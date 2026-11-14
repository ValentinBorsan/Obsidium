package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import team.reborn.energy.api.base.SimpleEnergyStorage;
import net.minecraft.world.MenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import org.jetbrains.annotations.Nullable;

public class VolcaniteGrinderBlockEntity extends BlockEntity implements MenuProvider, InventoryHolder, WorldlyContainer, MachineInventory {
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> C_ORES_IRON = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ores/iron"));
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> C_RAW_IRON = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "raw_materials/iron"));
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> C_INGOTS_IRON = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ingots/iron"));

    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> C_ORES_GOLD = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ores/gold"));
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> C_RAW_GOLD = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "raw_materials/gold"));
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> C_INGOTS_GOLD = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ingots/gold"));

    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> C_ORES_COPPER = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ores/copper"));
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> C_RAW_COPPER = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "raw_materials/copper"));
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> C_INGOTS_COPPER = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ingots/copper"));

    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> C_ORES_TIN = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ores/tin"));
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> C_RAW_TIN = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "raw_materials/tin"));
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> C_INGOTS_TIN = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ingots/tin"));

    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> C_ORES_LEAD = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ores/lead"));
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> C_RAW_LEAD = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "raw_materials/lead"));
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> C_INGOTS_LEAD = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ingots/lead"));

    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> C_ORES_SILVER = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ores/silver"));
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> C_RAW_SILVER = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "raw_materials/silver"));
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> C_INGOTS_SILVER = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ingots/silver"));

    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> C_ORES_VOLCANITE = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ores/volcanite"));
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> C_INGOTS_VOLCANITE = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ingots/volcanite"));
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> C_INGOTS_BRONZE = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ingots/bronze"));

    private int progress = 0;
    private final int MAX_PROGRESS = 100;
    private final int RF_PER_TICK = 250;

    private final SimpleContainer inventory = new SimpleContainer(3) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            if (slot == 0) return true;
            if (slot == 2) return net.valentin.obsidium.item.VolcaniteOverclockUpgradeItem.isOverclockUpgrade(stack);
            return false;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            VolcaniteGrinderBlockEntity.this.setChanged();
        }
    };

    @Override
    public int getContainerSize() {
        return this.inventory.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return this.inventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.inventory.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack res = this.inventory.removeItem(slot, amount);
        if (!res.isEmpty()) setChanged();
        return res;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack res = this.inventory.removeItemNoUpdate(slot);
        if (!res.isEmpty()) setChanged();
        return res;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.inventory.setItem(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }

    @Override
    public void clearContent() {
        this.inventory.clearContent();
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return new int[]{ 1 };
        } else if (side == Direction.UP) {
            return new int[]{ 0 };
        } else {
            return new int[]{ 1, 0 };
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 0 && side != Direction.DOWN;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 1;
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
    public net.minecraft.world.Container getInventory() {
        return this;
    }

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index == 0) return progress;
            if (index == 1) return (int) (energyStorage.amount >> 16);
            if (index == 2) return (int) (energyStorage.amount & 0xFFFF);
            return 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(100000, 10000, 0) {
        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    public VolcaniteGrinderBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.VOLCANITE_GRINDER_BE, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VolcaniteGrinderBlockEntity entity) {
        if (level.isClientSide()) return;

        boolean isGrinding = false;
        boolean didSomething = false;

        ItemStack input = entity.inventory.getItem(0);
        ItemStack output = entity.inventory.getItem(1);
        ItemStack upgrade = entity.inventory.getItem(2);
        
        ItemStack recipeResult = getRecipeResult(input);
        
        boolean isThermalBoost = isThermalSurrounding(level, pos);
        float speedMult = 1.0f;
        float energyMult = 1.0f;
        if (!upgrade.isEmpty() && net.valentin.obsidium.item.VolcaniteOverclockUpgradeItem.isOverclockUpgrade(upgrade)) {
            speedMult = net.valentin.obsidium.item.VolcaniteOverclockUpgradeItem.getSpeedBonus(upgrade);
            energyMult = net.valentin.obsidium.item.VolcaniteOverclockUpgradeItem.getEnergyCostMultiplier(upgrade);
        }

        int speedBoost = Math.max(1, (int) ((isThermalBoost ? 2 : 1) * speedMult));
        int effectiveRF = (int) (entity.RF_PER_TICK * energyMult);
        boolean hasPower = entity.energyStorage.amount >= effectiveRF || isThermalBoost;

        if (!recipeResult.isEmpty() && hasPower && 
            (output.isEmpty() || (output.is(recipeResult.getItem()) && output.getCount() + recipeResult.getCount() <= output.getMaxStackSize()))) {
            
            if (entity.energyStorage.amount >= effectiveRF) {
                entity.energyStorage.amount -= effectiveRF;
            }
            entity.progress += speedBoost;
            isGrinding = true;
            didSomething = true;
            
            // Grinding audio during operation (faster pitch if overclocked)
            if (entity.progress % 24 == 0) {
                float pitch = 0.85F + (speedMult - 1.0f) * 0.15F + level.random.nextFloat() * 0.15F;
                level.playSound(null, pos, net.valentin.obsidium.sound.ModSounds.GRINDER_RUN, net.minecraft.sounds.SoundSource.BLOCKS, 0.45F, pitch);
            }

            if (speedMult > 1.0f && level instanceof net.minecraft.server.level.ServerLevel serverLevel && level.random.nextFloat() < 0.35f) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.65, pos.getZ() + 0.5, 2, 0.15, 0.15, 0.15, 0.03);
            } else if (isThermalBoost && level instanceof net.minecraft.server.level.ServerLevel serverLevel && level.random.nextFloat() < 0.2f) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 2, 0.1, 0.1, 0.1, 0.02);
            }

            if (entity.progress >= entity.MAX_PROGRESS) {
                entity.progress = 0;
                input.shrink(1);
                if (output.isEmpty()) {
                    entity.inventory.setItem(1, recipeResult.copy());
                } else {
                    output.grow(recipeResult.getCount());
                }

                // Completion crunch audio
                level.playSound(null, pos, net.valentin.obsidium.sound.ModSounds.MACHINE_CLICK, net.minecraft.sounds.SoundSource.BLOCKS, 0.7F, 1.0F);
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 6, 0.15, 0.15, 0.15, 0.05);
                }
            }
        } else {
            if (entity.progress > 0) {
                entity.progress = 0;
                didSomething = true;
            }
        }

        boolean currentLitState = state.getValue(VolcaniteGrinderBlock.LIT);
        if (currentLitState != isGrinding) {
            level.setBlock(pos, state.setValue(VolcaniteGrinderBlock.LIT, isGrinding), 3);
            didSomething = true;
        }

        if (didSomething) {
            entity.setChanged();
        }

        VolcaniteStoragePipeBlockEntity.autoEjectMachineOutputs(level, pos, entity);
    }

    private static ItemStack getRecipeResult(ItemStack input) {
        if (input.isEmpty()) return ItemStack.EMPTY;
        
        // Raw Ores & Ores -> 2 Dust (Supports Conventional Tags from other mods!)
        if (input.is(C_ORES_IRON) || input.is(C_RAW_IRON) || input.is(Items.RAW_IRON) || input.is(Items.IRON_ORE) || input.is(Items.DEEPSLATE_IRON_ORE)) {
            return new ItemStack(ObsidiumMod.IRON_DUST, 2);
        }
        if (input.is(C_ORES_GOLD) || input.is(C_RAW_GOLD) || input.is(Items.RAW_GOLD) || input.is(Items.GOLD_ORE) || input.is(Items.DEEPSLATE_GOLD_ORE)) {
            return new ItemStack(ObsidiumMod.GOLD_DUST, 2);
        }
        if (input.is(C_ORES_COPPER) || input.is(C_RAW_COPPER) || input.is(Items.RAW_COPPER) || input.is(Items.COPPER_ORE) || input.is(Items.DEEPSLATE_COPPER_ORE)) {
            return new ItemStack(ObsidiumMod.COPPER_DUST, 2);
        }
        if (input.is(C_ORES_TIN) || input.is(C_RAW_TIN) || input.is(ObsidiumMod.RAW_TIN) || input.is(ObsidiumMod.TIN_ORE_ITEM) || input.is(ObsidiumMod.DEEPSLATE_TIN_ORE_ITEM)) {
            return new ItemStack(ObsidiumMod.TIN_DUST, 2);
        }
        if (input.is(C_ORES_LEAD) || input.is(C_RAW_LEAD) || input.is(ObsidiumMod.RAW_LEAD) || input.is(ObsidiumMod.LEAD_ORE_ITEM) || input.is(ObsidiumMod.DEEPSLATE_LEAD_ORE_ITEM)) {
            return new ItemStack(ObsidiumMod.LEAD_DUST, 2);
        }
        if (input.is(C_ORES_SILVER) || input.is(C_RAW_SILVER) || input.is(ObsidiumMod.RAW_SILVER) || input.is(ObsidiumMod.SILVER_ORE_ITEM) || input.is(ObsidiumMod.DEEPSLATE_SILVER_ORE_ITEM)) {
            return new ItemStack(ObsidiumMod.SILVER_DUST, 2);
        }
        if (input.is(ObsidiumMod.ARTEMIS_SILVER_FRUIT)) {
            return new ItemStack(ObsidiumMod.SILVER_DUST, 2);
        }
        if (input.is(ObsidiumMod.RAW_SILVER_BLOCK_ITEM)) {
            return new ItemStack(ObsidiumMod.SILVER_DUST, 18);
        }
        if (input.is(ObsidiumMod.SILVER_BLOCK_ITEM)) {
            return new ItemStack(ObsidiumMod.SILVER_DUST, 9);
        }
        if (input.is(C_ORES_VOLCANITE) || input.is(ObsidiumMod.VOLCANITE_ORE_ITEM) || input.is(ObsidiumMod.VOLCANITE)) {
            return new ItemStack(ObsidiumMod.VOLCANITE_DUST, 2);
        }

        // Ingots -> 1 Dust (Supports Conventional Tags from other mods!)
        if (input.is(C_INGOTS_IRON) || input.is(Items.IRON_INGOT)) {
            return new ItemStack(ObsidiumMod.IRON_DUST, 1);
        }
        if (input.is(C_INGOTS_GOLD) || input.is(Items.GOLD_INGOT)) {
            return new ItemStack(ObsidiumMod.GOLD_DUST, 1);
        }
        if (input.is(C_INGOTS_COPPER) || input.is(Items.COPPER_INGOT)) {
            return new ItemStack(ObsidiumMod.COPPER_DUST, 1);
        }
        if (input.is(C_INGOTS_TIN) || input.is(ObsidiumMod.TIN_INGOT)) {
            return new ItemStack(ObsidiumMod.TIN_DUST, 1);
        }
        if (input.is(C_INGOTS_LEAD) || input.is(ObsidiumMod.LEAD_INGOT)) {
            return new ItemStack(ObsidiumMod.LEAD_DUST, 1);
        }
        if (input.is(C_INGOTS_SILVER) || input.is(ObsidiumMod.SILVER_INGOT)) {
            return new ItemStack(ObsidiumMod.SILVER_DUST, 1);
        }
        if (input.is(C_INGOTS_BRONZE) || input.is(ObsidiumMod.PYRIC_BRONZE_INGOT)) {
            return new ItemStack(ObsidiumMod.PYRIC_BRONZE_DUST, 1);
        }
        if (input.is(ObsidiumMod.FEROPLUMB_INGOT)) {
            return new ItemStack(ObsidiumMod.FEROPLUMB_DUST, 1);
        }
        if (input.is(C_INGOTS_VOLCANITE) || input.is(ObsidiumMod.VOLCANITE_INGOT) || input.is(ObsidiumMod.VOLCANIC_ALLOY)) {
            return new ItemStack(ObsidiumMod.VOLCANITE_DUST, 1);
        }

        // Common Materials
        if (input.is(Items.COBBLESTONE) || input.is(Items.COBBLED_DEEPSLATE) || input.is(Items.STONE) || input.is(Items.DEEPSLATE)) {
            return new ItemStack(Items.GRAVEL, 1);
        }
        if (input.is(Items.GRAVEL)) {
            return new ItemStack(Items.SAND, 1);
        }
        if (input.is(Items.SANDSTONE) || input.is(Items.RED_SANDSTONE)) {
            return new ItemStack(Items.SAND, 2);
        }
        if (input.is(Items.COAL)) {
            return new ItemStack(ObsidiumMod.COAL_NUGGET, 4);
        }
        if (input.is(Items.CHARCOAL)) {
            return new ItemStack(ObsidiumMod.VOLCANIC_ASH, 2);
        }
        if (input.is(Items.NETHERRACK)) {
            return new ItemStack(ObsidiumMod.VOLCANIC_ASH, 2);
        }
        if (input.is(Items.BLAZE_ROD)) {
            return new ItemStack(Items.BLAZE_POWDER, 4);
        }
        if (input.is(Items.BONE)) {
            return new ItemStack(Items.BONE_MEAL, 6);
        }
        if (input.is(Items.MAGMA_BLOCK)) {
            return new ItemStack(Items.MAGMA_CREAM, 2);
        }

        return ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("Progress", progress);
        tag.putLong("Energy", energyStorage.amount);
        MachineInventory.saveInventory(tag, inventory, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.progress = tag.getInt("Progress");
        this.energyStorage.amount = tag.getLong("Energy");
        MachineInventory.loadInventory(tag, inventory, provider);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.volcanite_grinder");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new VolcaniteGrinderMenu(syncId, playerInventory, this, this.data);
    }

    public static boolean isThermalSurrounding(Level level, BlockPos pos) {
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            BlockState adj = level.getBlockState(pos.relative(dir));
            if (adj.is(net.minecraft.world.level.block.Blocks.LAVA) || 
                adj.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK) || 
                adj.getBlock() == ObsidiumMod.VOLCANITE_ENERGY_CORE_BLOCK || 
                adj.getFluidState().is(net.minecraft.world.level.material.Fluids.LAVA)) {
                return true;
            }
        }
        return false;
    }
}
