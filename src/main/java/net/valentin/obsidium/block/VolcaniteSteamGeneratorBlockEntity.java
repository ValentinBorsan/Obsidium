package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import net.minecraft.world.WorldlyContainer;
import java.util.List;

public class VolcaniteSteamGeneratorBlockEntity extends BlockEntity implements MenuProvider, InventoryHolder, WorldlyContainer, MachineInventory {
    public static final int INVENTORY_SIZE = 3;
    // Slot 0: Water input (Water Bucket / Water Bottle)
    // Slot 1: Bucket / Bottle output (Empty container output)
    // Slot 2: Solid Fuel (Charcoal, Coal, Lava Bottle, etc.)
    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            VolcaniteSteamGeneratorBlockEntity.this.setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return VolcaniteSteamGeneratorBlockEntity.this.canPlaceItem(slot, stack);
        }
    };

    public static final int MAX_WATER = 10000;
    public static final int MAX_STEAM = 10000;
    public static final int MAX_ENERGY = 200000;
    public static final int GENERATION_RATE = 20; // Rebalanced/nerfed to 20 RF/t
    public static final int WATER_PER_TICK = 2; // mB/t water consumed
    public static final int STEAM_PER_TICK = 20; // mB/t steam produced
    public static final int MAX_OUTPUT = 1000;

    private static final long DROPLETS_PER_MILLIBUCKET = 81L;
    private final SingleFluidStorage waterFluidStorage = new SingleFluidStorage() {
        @Override
        protected long getCapacity(FluidVariant variant) {
            return variant.isBlank() || variant.isOf(net.minecraft.world.level.material.Fluids.WATER)
                    ? (long) MAX_WATER * DROPLETS_PER_MILLIBUCKET : 0;
        }

        @Override
        protected boolean canInsert(FluidVariant variant) {
            return variant.isOf(net.minecraft.world.level.material.Fluids.WATER);
        }

        @Override
        protected boolean canExtract(FluidVariant variant) {
            return false;
        }

        @Override
        public boolean supportsExtraction() {
            return false;
        }

        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };
    private final SingleFluidStorage steamFluidStorage = new SingleFluidStorage() {
        @Override
        protected long getCapacity(FluidVariant variant) {
            return variant.isBlank() || variant.isOf(ObsidiumMod.STEAM_FLUID)
                    ? (long) MAX_STEAM * DROPLETS_PER_MILLIBUCKET : 0;
        }

        @Override
        protected boolean canInsert(FluidVariant variant) {
            return false;
        }

        @Override
        protected boolean canExtract(FluidVariant variant) {
            return variant.isOf(ObsidiumMod.STEAM_FLUID);
        }

        @Override
        public boolean supportsInsertion() {
            return false;
        }

        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };
    public final Storage<FluidVariant> fluidStorage = new CombinedStorage<>(List.of(waterFluidStorage, steamFluidStorage));
    private int burnTime = 0;
    private int maxBurnTime = 0;

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(MAX_ENERGY, 0, MAX_OUTPUT) {
        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> getWaterAmount();
                case 1 -> getSteamAmount();
                case 2 -> burnTime;
                case 3 -> maxBurnTime;
                case 4 -> (int) (energyStorage.amount & 0xFFFF);
                case 5 -> (int) ((energyStorage.amount >> 16) & 0xFFFF);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> setStoredAmount(waterFluidStorage, net.minecraft.world.level.material.Fluids.WATER, value);
                case 1 -> setStoredAmount(steamFluidStorage, ObsidiumMod.STEAM_FLUID, value);
                case 2 -> burnTime = value;
                case 3 -> maxBurnTime = value;
                case 4 -> energyStorage.amount = (energyStorage.amount & 0xFFFF0000L) | (value & 0xFFFF);
                case 5 -> energyStorage.amount = (energyStorage.amount & 0xFFFFL) | ((long) value << 16);
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public VolcaniteSteamGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.VOLCANITE_STEAM_GENERATOR_BE, pos, state);
    }

    @Override
    public Container getInventory() {
        return this.inventory;
    }

    public int getWaterAmount() {
        return (int) (waterFluidStorage.getAmount() / DROPLETS_PER_MILLIBUCKET);
    }

    public int getSteamAmount() {
        return (int) (steamFluidStorage.getAmount() / DROPLETS_PER_MILLIBUCKET);
    }

    public int getBurnTime() {
        return burnTime;
    }

    public int getMaxBurnTime() {
        return maxBurnTime;
    }

    public int insertWater(int amount) {
        if (amount <= 0) return 0;
        try (Transaction transaction = Transaction.openOuter()) {
            long inserted = waterFluidStorage.insert(FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER), (long) amount * DROPLETS_PER_MILLIBUCKET, transaction);
            if (inserted > 0) transaction.commit();
            return (int) (inserted / DROPLETS_PER_MILLIBUCKET);
        }
    }

    public int extractWater(int amount) {
        if (amount <= 0) return 0;
        try (Transaction transaction = Transaction.openOuter()) {
            long extracted = waterFluidStorage.extract(FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER), (long) amount * DROPLETS_PER_MILLIBUCKET, transaction);
            if (extracted > 0) transaction.commit();
            return (int) (extracted / DROPLETS_PER_MILLIBUCKET);
        }
    }

    public int insertSteam(int amount) {
        if (amount <= 0) return 0;
        try (Transaction transaction = Transaction.openOuter()) {
            long inserted = steamFluidStorage.insert(FluidVariant.of(ObsidiumMod.STEAM_FLUID), (long) amount * DROPLETS_PER_MILLIBUCKET, transaction);
            if (inserted > 0) transaction.commit();
            return (int) (inserted / DROPLETS_PER_MILLIBUCKET);
        }
    }

    public int extractSteam(int amount) {
        if (amount <= 0) return 0;
        try (Transaction transaction = Transaction.openOuter()) {
            long extracted = steamFluidStorage.extract(FluidVariant.of(ObsidiumMod.STEAM_FLUID), (long) amount * DROPLETS_PER_MILLIBUCKET, transaction);
            if (extracted > 0) transaction.commit();
            return (int) (extracted / DROPLETS_PER_MILLIBUCKET);
        }
    }

    private void setStoredAmount(SingleFluidStorage storage, net.minecraft.world.level.material.Fluid fluid, int amountMb) {
        long requested = Math.max(0, (long) amountMb) * DROPLETS_PER_MILLIBUCKET;
        storage.variant = requested == 0 ? FluidVariant.blank() : FluidVariant.of(fluid);
        storage.amount = Math.min(requested, storage.getCapacity());
        if (storage.amount == 0) storage.variant = FluidVariant.blank();
    }

    public static int getFuelTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.is(ObsidiumMod.ASH_BRIQUETTE)) return 3200;
        if (stack.is(ObsidiumMod.STICK_BUNDLE)) return 1600;
        if (stack.is(ObsidiumMod.VOLCANIC_ASH)) return 800;
        Integer fabricFuel = net.fabricmc.fabric.api.registry.FuelRegistry.INSTANCE.get(stack.getItem());
        if (fabricFuel != null && fabricFuel > 0) return fabricFuel;
        return AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VolcaniteSteamGeneratorBlockEntity entity) {
        if (level.isClientSide()) return;

        boolean dirty = false;

        // 1. Water Bucket / Bottle input processing
        ItemStack waterInput = entity.inventory.getItem(0);
        ItemStack waterOutput = entity.inventory.getItem(1);
        if (!waterInput.isEmpty()) {
            if (waterInput.is(Items.WATER_BUCKET) && entity.getWaterAmount() + 1000 <= MAX_WATER) {
                if (waterOutput.isEmpty() || (waterOutput.is(Items.BUCKET) && waterOutput.getCount() < waterOutput.getMaxStackSize())) {
                    entity.insertWater(1000);
                    waterInput.shrink(1);
                    if (waterOutput.isEmpty()) {
                        entity.inventory.setItem(1, new ItemStack(Items.BUCKET));
                    } else {
                        waterOutput.grow(1);
                    }
                    dirty = true;
                }
            } else if (waterInput.is(Items.BUCKET) && entity.getSteamAmount() >= 1000) {
                if (waterOutput.isEmpty() || (waterOutput.is(ObsidiumMod.STEAM_BUCKET) && waterOutput.getCount() < waterOutput.getMaxStackSize())) {
                    entity.extractSteam(1000);
                    waterInput.shrink(1);
                    if (waterOutput.isEmpty()) {
                        entity.inventory.setItem(1, new ItemStack(ObsidiumMod.STEAM_BUCKET));
                    } else {
                        waterOutput.grow(1);
                    }
                    dirty = true;
                }
            } else if (waterInput.is(Items.POTION) && entity.getWaterAmount() + 250 <= MAX_WATER) {
                PotionContents contents = waterInput.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
                if (contents != null && contents.is(Potions.WATER)) {
                    if (waterOutput.isEmpty() || (waterOutput.is(Items.GLASS_BOTTLE) && waterOutput.getCount() < waterOutput.getMaxStackSize())) {
                        entity.insertWater(250);
                        waterInput.shrink(1);
                        if (waterOutput.isEmpty()) {
                            entity.inventory.setItem(1, new ItemStack(Items.GLASS_BOTTLE));
                        } else {
                            waterOutput.grow(1);
                        }
                        dirty = true;
                    }
                }
            }
        }

        // 2. Fuel burning & Steam/Energy Generation
        boolean canProduce = entity.getWaterAmount() >= WATER_PER_TICK &&
                (entity.getSteamAmount() < MAX_STEAM || entity.energyStorage.amount < entity.energyStorage.capacity);

        if (entity.burnTime <= 0 && canProduce) {
            ItemStack fuel = entity.inventory.getItem(2);
            if (fuel.getItem() instanceof net.valentin.obsidium.item.VolcaniteBottleItem) {
                int bottleLava = net.valentin.obsidium.item.VolcaniteBottleItem.getLava(fuel);
                if (bottleLava >= 100) {
                    net.valentin.obsidium.item.VolcaniteBottleItem.setLava(fuel, bottleLava - 100);
                    entity.burnTime = 2000;
                    entity.maxBurnTime = 2000;
                    dirty = true;
                }
            } else {
                int fuelDuration = getFuelTime(fuel);
                if (fuelDuration > 0) {
                    entity.burnTime = fuelDuration;
                    entity.maxBurnTime = fuelDuration;
                    if (fuel.is(Items.LAVA_BUCKET)) {
                        entity.inventory.setItem(2, new ItemStack(Items.BUCKET));
                    } else {
                        fuel.shrink(1);
                    }
                    dirty = true;
                }
            }
        }

        boolean isGenerating = false;
        if (entity.burnTime > 0 && entity.getWaterAmount() >= WATER_PER_TICK) {
            entity.burnTime--;
            entity.extractWater(WATER_PER_TICK);

            // Produce Steam
            entity.insertSteam(STEAM_PER_TICK);

            boolean isOverclocked = isThermalOverclocked(level, pos);
            int currentGen = isOverclocked ? (int)(GENERATION_RATE * 1.5) : GENERATION_RATE;

            // Produce Energy
            if (entity.energyStorage.amount < entity.energyStorage.capacity) {
                entity.energyStorage.amount = Math.min(entity.energyStorage.capacity, entity.energyStorage.amount + currentGen);
            }

            // Turbine running loop (~1.85s clip -> retrigger every 36 ticks)
            if (level.getGameTime() % 36 == 0) {
                level.playSound(null, pos, net.valentin.obsidium.sound.ModSounds.STEAM_GENERATOR_RUN, net.minecraft.sounds.SoundSource.BLOCKS, 0.45F, 1.0F);
            }

            // Steam hiss sound & exhaust particles
            if (level.random.nextInt(45) == 0) {
                level.playSound(null, pos, net.valentin.obsidium.sound.ModSounds.STEAM_HISS, net.minecraft.sounds.SoundSource.BLOCKS, 0.35F, 0.9F + level.random.nextFloat() * 0.2F);
            }
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel && level.random.nextFloat() < 0.3f) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 1, 0.05, 0.1, 0.05, 0.01);
                if (isOverclocked && level.random.nextFloat() < 0.2f) {
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }

            isGenerating = true;
            dirty = true;
        }

        // Sync WORKING blockstate
        if (state.getValue(VolcaniteSteamGeneratorBlock.WORKING) != isGenerating) {
            level.setBlock(pos, state.setValue(VolcaniteSteamGeneratorBlock.WORKING, isGenerating), 3);
            dirty = true;
        }

        // 3. Automatically pump high-pressure steam into adjacent Geothermal Arborist or Liquid Tanks
        if (entity.getSteamAmount() > 0) {
            for (Direction dir : Direction.values()) {
                if (entity.getSteamAmount() <= 0) break;
                BlockPos neighborPos = pos.relative(dir);
                BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                if (neighborBe instanceof GeothermalArboristBlockEntity arborist) {
                    int space = arborist.getRemainingFluidCapacity();
                    if (space > 0) {
                        int toMove = Math.min(entity.getSteamAmount(), Math.min(space, 50));
                        int moved = arborist.insertFluid(toMove);
                        entity.extractSteam(moved);
                        dirty = true;
                        if (level.random.nextFloat() < 0.25f && level instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.CLOUD,
                                    (pos.getX() + neighborPos.getX()) / 2.0 + 0.5,
                                    pos.getY() + 0.6,
                                    (pos.getZ() + neighborPos.getZ()) / 2.0 + 0.5,
                                    2, 0.1, 0.1, 0.1, 0.01);
                        }
                    }
                } else if (neighborBe instanceof LiquidTankBlockEntity tank) {
                    int moved = tank.insertFluid(FluidVariant.of(ObsidiumMod.STEAM_FLUID), Math.min(entity.getSteamAmount(), 50), false);
                    if (moved > 0) {
                        entity.extractSteam(moved);
                        dirty = true;
                    }
                } else if (neighborBe instanceof VolcaniteLiquidStorageTankBlockEntity storageTank) {
                    int moved = storageTank.insertFluid(FluidVariant.of(ObsidiumMod.STEAM_FLUID), Math.min(entity.getSteamAmount(), 50), false);
                    if (moved > 0) {
                        entity.extractSteam(moved);
                        dirty = true;
                    }
                }
            }
        }

        // 4. Output energy to neighbors
        if (entity.energyStorage.amount > 0) {
            for (Direction dir : Direction.values()) {
                EnergyStorage target = EnergyStorage.SIDED.find(level, pos.relative(dir), dir.getOpposite());
                if (target != null) {
                    long moved = EnergyStorageUtil.move(entity.energyStorage, target, MAX_OUTPUT, null);
                    if (moved > 0) {
                        dirty = true;
                    }
                }
            }
        }

        if (dirty) {
            entity.setChanged();
        }

        VolcaniteStoragePipeBlockEntity.autoEjectMachineOutputs(level, pos, entity);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("Water", getWaterAmount());
        tag.putInt("Steam", getSteamAmount());
        tag.putLong("WaterDroplets", waterFluidStorage.getAmount());
        tag.putLong("SteamDroplets", steamFluidStorage.getAmount());
        tag.putInt("BurnTime", burnTime);
        tag.putInt("MaxBurnTime", maxBurnTime);
        tag.putLong("Energy", energyStorage.amount);
        MachineInventory.saveInventory(tag, inventory, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("WaterDroplets")) {
            setFluidDroplets(waterFluidStorage, net.minecraft.world.level.material.Fluids.WATER, tag.getLong("WaterDroplets"));
        } else {
            setStoredAmount(waterFluidStorage, net.minecraft.world.level.material.Fluids.WATER, tag.getInt("Water"));
        }
        if (tag.contains("SteamDroplets")) {
            setFluidDroplets(steamFluidStorage, ObsidiumMod.STEAM_FLUID, tag.getLong("SteamDroplets"));
        } else {
            setStoredAmount(steamFluidStorage, ObsidiumMod.STEAM_FLUID, tag.getInt("Steam"));
        }
        burnTime = tag.getInt("BurnTime");
        maxBurnTime = tag.getInt("MaxBurnTime");
        energyStorage.amount = tag.getLong("Energy");
        MachineInventory.loadInventory(tag, inventory, provider);
    }

    private void setFluidDroplets(SingleFluidStorage storage, net.minecraft.world.level.material.Fluid fluid, long droplets) {
        storage.variant = droplets <= 0 ? FluidVariant.blank() : FluidVariant.of(fluid);
        storage.amount = Math.min(Math.max(0, droplets), storage.getCapacity());
        if (storage.amount == 0) storage.variant = FluidVariant.blank();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.volcanite_steam_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new VolcaniteSteamGeneratorMenu(syncId, playerInventory, inventory, data);
    }

    public static boolean isWaterContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(Items.WATER_BUCKET) || stack.is(Items.BUCKET)) return true;
        if (stack.is(Items.POTION)) {
            PotionContents contents = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
            return contents != null && contents.is(Potions.WATER);
        }
        return false;
    }

    public static boolean isFuel(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof net.valentin.obsidium.item.VolcaniteBottleItem) {
            return net.valentin.obsidium.item.VolcaniteBottleItem.getLava(stack) >= 100;
        }
        return getFuelTime(stack) > 0;
    }

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
        return this.inventory.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return this.inventory.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.inventory.setItem(slot, stack);
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
        if (stack.isEmpty()) return false;
        if (slot == 0) {
            return isWaterContainer(stack);
        }
        if (slot == 1) {
            return false; // Output slot only!
        }
        if (slot == 2) {
            return isFuel(stack);
        }
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return new int[] { 1 }; // Extract empty buckets/bottles from bottom
        } else if (side == Direction.UP) {
            return new int[] { 0, 2 }; // Insert water or fuel from top
        } else {
            return new int[] { 2, 0, 1 }; // Sides
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 1; // Only extract from output slot
    }

    @Override
    public boolean isInputSlot(int slot, ItemStack stack, @org.jetbrains.annotations.Nullable Direction side) {
        return slot == 0 || slot == 2;
    }

    @Override
    public boolean isOutputSlot(int slot, ItemStack stack, @org.jetbrains.annotations.Nullable Direction side) {
        return slot == 1;
    }

    public static boolean isThermalOverclocked(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
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
