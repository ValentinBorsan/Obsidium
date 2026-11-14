package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.Optional;

public class ApolloSolarGeneratorBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, MachineInventory {
    public static final int INVENTORY_SIZE = 2; // Slot 0: Solar Smelt Input, Slot 1: Output
    public static final int MAX_ENERGY = 300_000;
    public static final int MAX_OUTPUT = 1_000;
    public static final int SMELT_DURATION = 100; // 5 seconds per solar smelt

    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            ApolloSolarGeneratorBlockEntity.this.setChanged();
        }
    };

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(MAX_ENERGY, 0, MAX_OUTPUT) {
        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    private int sunIrradiance = 0; // 0..100%
    private int generationRate = 0; // RF/t currently generated
    private int smeltProgress = 0;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) energyStorage.amount;
                case 1 -> (int) energyStorage.capacity;
                case 2 -> sunIrradiance;
                case 3 -> generationRate;
                case 4 -> smeltProgress;
                case 5 -> SMELT_DURATION;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energyStorage.amount = value;
                case 2 -> sunIrradiance = value;
                case 3 -> generationRate = value;
                case 4 -> smeltProgress = value;
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public ApolloSolarGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.APOLLO_SOLAR_GENERATOR_BE, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ApolloSolarGeneratorBlockEntity entity) {
        if (level.isClientSide()) return;
        ServerLevel serverLevel = (ServerLevel) level;

        boolean wasActive = state.getValue(ApolloSolarGeneratorBlock.ACTIVE);
        boolean isActive = false;

        // 1. Calculate Solar Irradiance
        boolean canSeeSky = level.canSeeSky(pos.above());
        boolean isDay = level.isDay();
        int rate = 0;
        int irradiance = 0;

        if (canSeeSky && isDay) {
            // Midday peaks in Minecraft is around noon (6000 ticks)
            boolean isRaining = level.isRaining();
            boolean isThundering = level.isThundering();

            if (isThundering) {
                irradiance = 20;
                rate = 25;
            } else if (isRaining) {
                irradiance = 45;
                rate = 40;
            } else {
                irradiance = 100;
                rate = net.valentin.obsidium.config.ModConfig.get().solarGeneratorMiddayRate;
                // Boost in volcanic/hot biomes (Caldera, Nether, Desert, Badlands)
                var biome = level.getBiome(pos);
                if (biome.is(net.minecraft.tags.BiomeTags.IS_NETHER) || biome.is(net.minecraft.tags.BiomeTags.IS_BADLANDS)) {
                    irradiance = 125;
                    rate = (int) (rate * 1.75F);
                }
            }
        }

        entity.sunIrradiance = irradiance;
        entity.generationRate = rate;

        // 2. Generate Energy
        if (rate > 0 && entity.energyStorage.amount < entity.energyStorage.capacity) {
            entity.energyStorage.amount = Math.min(entity.energyStorage.capacity, entity.energyStorage.amount + rate);
            isActive = true;
            entity.setChanged();
        }

        // 3. Solar Smelting / Ray Infusion
        ItemStack input = entity.inventory.getItem(0);
        if (!input.isEmpty() && (irradiance > 0 || entity.energyStorage.amount >= 40)) {
            SingleRecipeInput recipeInput = new SingleRecipeInput(input);
            Optional<RecipeHolder<SmeltingRecipe>> match = serverLevel.getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, recipeInput, serverLevel);

            if (match.isPresent()) {
                ItemStack result = match.get().value().assemble(recipeInput, level.registryAccess());
                if (entity.canOutput(result)) {
                    isActive = true;
                    // Deduct secondary power if sun is dim
                    if (irradiance < 50) {
                        entity.energyStorage.amount = Math.max(0, entity.energyStorage.amount - 20);
                    }

                    if (++entity.smeltProgress >= SMELT_DURATION) {
                        entity.smeltProgress = 0;
                        entity.produceSmelt(result);
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                                6, 0.15, 0.15, 0.15, 0.05);
                        serverLevel.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.4F, 1.4F);
                    }
                    entity.setChanged();
                } else {
                    entity.smeltProgress = 0;
                }
            } else {
                entity.smeltProgress = 0;
            }
        } else {
            entity.smeltProgress = 0;
        }

        // 4. Auto-Push RF Energy to Adjacent Cables & Machines
        if (entity.energyStorage.amount > 0) {
            for (Direction dir : Direction.values()) {
                EnergyStorage neighbor = EnergyStorage.SIDED.find(level, pos.relative(dir), dir.getOpposite());
                if (neighbor != null && neighbor.supportsInsertion()) {
                    EnergyStorageUtil.move(entity.energyStorage, neighbor, MAX_OUTPUT, null);
                }
            }
        }

        // 5. Update State
        if (wasActive != isActive) {
            level.setBlock(pos, state.setValue(ApolloSolarGeneratorBlock.ACTIVE, isActive), 3);
            entity.setChanged();
        }

        // Occasional ambient solar flare
        if (isActive && level.random.nextInt(60) == 0) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5,
                    2, 0.2, 0.1, 0.2, 0.02);
        }
    }

    private boolean canOutput(ItemStack result) {
        if (result.isEmpty()) return false;
        ItemStack currentOut = inventory.getItem(1);
        if (currentOut.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(currentOut, result)) return false;
        return currentOut.getCount() + result.getCount() <= currentOut.getMaxStackSize();
    }

    private void produceSmelt(ItemStack result) {
        ItemStack input = inventory.getItem(0);
        input.shrink(1);
        if (input.isEmpty()) {
            inventory.setItem(0, ItemStack.EMPTY);
        }

        ItemStack currentOut = inventory.getItem(1);
        if (currentOut.isEmpty()) {
            inventory.setItem(1, result.copy());
        } else {
            currentOut.grow(result.getCount());
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Apollo Helio-Concentrator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new ApolloSolarGeneratorMenu(syncId, playerInventory, this, this.data);
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return inventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventory.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return inventory.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return inventory.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory.setItem(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
    }

    @Override
    public boolean isInputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 0;
    }

    @Override
    public boolean isOutputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 1;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP) return new int[]{ 0 }; // Input from top
        return new int[]{ 1 }; // Output from sides/bottom
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == 0;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == 1;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("Energy", energyStorage.amount);
        tag.putInt("SmeltProgress", smeltProgress);
        ContainerHelper.saveAllItems(tag, inventory.getItems(), registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyStorage.amount = tag.getLong("Energy");
        smeltProgress = tag.getInt("SmeltProgress");
        inventory.clearContent();
        ContainerHelper.loadAllItems(tag, inventory.getItems(), registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }
}
