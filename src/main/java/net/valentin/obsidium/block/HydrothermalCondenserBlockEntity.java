package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

public class HydrothermalCondenserBlockEntity extends BlockEntity implements MenuProvider, InventoryHolder, WorldlyContainer, MachineInventory {
    public static final int INVENTORY_SIZE = 4;
    // Slot 0: Moisture / Biomass / Ice Input
    // Slot 1: Empty Bucket OR Cryo-Catalyst In (Snowball, Dew Droplet, Ice, Packed Ice)
    // Slot 2: Water Bucket / Ice / Packed Ice / Blue Ice Out
    // Slot 3: Dew Droplets Out (subproduct)

    public static final int DATA_COUNT = 5;
    public static final int MODE_WATER = 0;
    public static final int MODE_ICE = 1;
    public static final int MODE_PACKED_ICE = 2;
    public static final int MODE_BLUE_ICE = 3;

    public static final int MAX_WATER = 4000;
    public static final int WATER_PER_OPERATION = 250;
    public static final int BASE_CONDENSE_TICKS = 160; // 8s base per 250mB (faster with ice/rain)

    private int waterAmount = 0;
    private int progress = 0;
    private int efficiencyMultiplier = 100; // 100 = 1.0x, 150 = 1.5x, 200 = 2.0x
    private int outputMode = MODE_WATER;

    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            HydrothermalCondenserBlockEntity.this.setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            if (slot == 0) return isMoistureItem(stack);
            if (slot == 1) return isCatalystOrBucketItem(stack);
            return false;
        }
    };

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> waterAmount;
                case 1 -> progress;
                case 2 -> BASE_CONDENSE_TICKS;
                case 3 -> efficiencyMultiplier;
                case 4 -> outputMode;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> waterAmount = value;
                case 1 -> progress = value;
                case 3 -> efficiencyMultiplier = value;
                case 4 -> outputMode = value;
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public HydrothermalCondenserBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.HYDROTHERMAL_CONDENSER_BE, pos, state);
    }

    public static boolean isCatalystOrBucketItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(Items.BUCKET)
                || stack.is(ObsidiumMod.DEW_DROPLET)
                || stack.is(Items.SNOWBALL)
                || stack.is(Items.SNOW_BLOCK)
                || stack.is(Items.ICE)
                || stack.is(Items.PACKED_ICE);
    }

    public void cycleOutputMode() {
        this.outputMode = (this.outputMode + 1) % 4;
        setChanged();
    }

    public int getOutputMode() {
        return this.outputMode;
    }

    public static boolean isMoistureItem(ItemStack stack) {
        return stack.is(ItemTags.LEAVES)
                || stack.is(ItemTags.SAPLINGS)
                || stack.is(Items.ICE)
                || stack.is(Items.PACKED_ICE)
                || stack.is(Items.BLUE_ICE)
                || stack.is(Items.SNOWBALL)
                || stack.is(Items.SNOW_BLOCK)
                || stack.is(Items.CACTUS)
                || stack.is(Items.VINE)
                || stack.is(Items.KELP)
                || stack.is(Items.SEAGRASS)
                || stack.is(Items.MOSS_BLOCK)
                || stack.is(Items.APPLE)
                || stack.is(Items.WHEAT);
    }

    public int getWaterAmount() {
        return this.waterAmount;
    }

    public void extractWater(int amount) {
        this.waterAmount = Math.max(0, this.waterAmount - amount);
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HydrothermalCondenserBlockEntity entity) {
        if (level.isClientSide()) return;

        boolean wasLit = state.getValue(HydrothermalCondenserBlock.LIT);
        boolean isLit = false;

        // 1. Detect environmental efficiency
        int efficiency = getEnvironmentalEfficiency(level, pos);
        entity.efficiencyMultiplier = efficiency;

        // 2. Process automatic bucket filling or Cryo-Freezing (Water + Slot 1 -> Slot 2)
        processWaterAndCryoOutput(level, pos, entity);

        // 3. Process moisture condensation if space in water tank
        ItemStack moistureSlot = entity.inventory.getItem(0);
        boolean hasMoistureItem = isMoistureItem(moistureSlot);
        boolean hasTankSpace = entity.waterAmount <= MAX_WATER - WATER_PER_OPERATION;

        // Either uses a moisture item, or slowly condenses ambient humidity if raining/high efficiency
        if (hasTankSpace && (hasMoistureItem || efficiency >= 125)) {
            isLit = true;
            int step = Math.max(1, efficiency / 75);
            entity.progress += step;

            if (entity.progress >= BASE_CONDENSE_TICKS) {
                entity.progress = 0;

                if (hasMoistureItem) {
                    moistureSlot.shrink(1);
                }

                // Add 250 mB of water
                entity.waterAmount = Math.min(MAX_WATER, entity.waterAmount + WATER_PER_OPERATION);

                // Subproduct: 50% chance to crystallize a Dew Droplet in Slot 3
                if (level.random.nextFloat() < 0.50F) {
                    ItemStack dropSlot = entity.inventory.getItem(3);
                    if (dropSlot.isEmpty()) {
                        entity.inventory.setItem(3, new ItemStack(ObsidiumMod.DEW_DROPLET));
                    } else if (dropSlot.is(ObsidiumMod.DEW_DROPLET) && dropSlot.getCount() < dropSlot.getMaxStackSize()) {
                        dropSlot.grow(1);
                    }
                }

                level.playSound(null, pos, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON, SoundSource.BLOCKS, 0.5F, 1.1F);
                entity.setChanged();
            }
        } else {
            if (entity.progress > 0) {
                entity.progress = Math.max(0, entity.progress - 2);
            }
        }

        if (wasLit != isLit) {
            level.setBlock(pos, state.setValue(HydrothermalCondenserBlock.LIT, isLit), 3);
        }

        // 4. Auto-output water to any adjacent Fabric fluid storage (when in MODE_WATER).
        if (entity.outputMode == MODE_WATER && entity.waterAmount > 0) {
            for (Direction dir : Direction.values()) {
                if (entity.waterAmount <= 0) break;
                BlockPos neighborPos = pos.relative(dir);
                Storage<FluidVariant> target = FluidStorage.SIDED.find(level, neighborPos, dir.getOpposite());
                if (target == null || !target.supportsInsertion()) continue;

                long moved = StorageUtil.move(
                        entity.fluidStorage,
                        target,
                        fluid -> fluid.isOf(Fluids.WATER),
                        100L * 81L,
                        null
                );
                if (moved > 0) entity.setChanged();
            }
        }

        VolcaniteStoragePipeBlockEntity.autoEjectMachineOutputs(level, pos, entity);
    }

    private static boolean canOutputStack(SimpleContainer inv, int slot, ItemStack result) {
        ItemStack existing = inv.getItem(slot);
        if (existing.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(existing, result)
                && existing.getCount() + result.getCount() <= existing.getMaxStackSize();
    }

    private static void addOutputStack(SimpleContainer inv, int slot, ItemStack result) {
        ItemStack existing = inv.getItem(slot);
        if (existing.isEmpty()) {
            inv.setItem(slot, result.copy());
        } else {
            existing.grow(result.getCount());
        }
    }

    private static void processWaterAndCryoOutput(Level level, BlockPos pos, HydrothermalCondenserBlockEntity entity) {
        ItemStack catalyst = entity.inventory.getItem(1);

        // Even in MODE_WATER, if the player puts an Empty Bucket in Slot 1, fill it at 1000 mB
        if (!catalyst.isEmpty() && catalyst.is(Items.BUCKET) && entity.waterAmount >= 1000) {
            ItemStack bucketResult = new ItemStack(Items.WATER_BUCKET);
            if (canOutputStack(entity.inventory, 2, bucketResult)) {
                catalyst.shrink(1);
                addOutputStack(entity.inventory, 2, bucketResult);
                entity.waterAmount -= 1000;
                entity.setChanged();
                return;
            }
        }

        // Catalyst upgrade in Slot 1 regardless of mode if Ice/Packed Ice is inserted
        if (entity.outputMode == MODE_WATER && !catalyst.isEmpty()) {
            if (catalyst.is(Items.PACKED_ICE) && entity.waterAmount >= 1000) {
                ItemStack res = new ItemStack(Items.BLUE_ICE);
                if (canOutputStack(entity.inventory, 2, res)) {
                    catalyst.shrink(1);
                    addOutputStack(entity.inventory, 2, res);
                    entity.waterAmount -= 1000;
                    level.playSound(null, pos, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.BLOCKS, 0.5F, 1.35F);
                    entity.setChanged();
                }
            } else if (catalyst.is(Items.ICE) && entity.waterAmount >= 500) {
                ItemStack res = new ItemStack(Items.PACKED_ICE);
                if (canOutputStack(entity.inventory, 2, res)) {
                    catalyst.shrink(1);
                    addOutputStack(entity.inventory, 2, res);
                    entity.waterAmount -= 500;
                    level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 0.5F, 1.4F);
                    entity.setChanged();
                }
            } else if ((catalyst.is(ObsidiumMod.DEW_DROPLET) || catalyst.is(Items.SNOWBALL) || catalyst.is(Items.SNOW_BLOCK)) && entity.waterAmount >= 250) {
                ItemStack res = new ItemStack(Items.ICE);
                if (canOutputStack(entity.inventory, 2, res)) {
                    catalyst.shrink(1);
                    addOutputStack(entity.inventory, 2, res);
                    entity.waterAmount -= 250;
                    level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 0.5F, 1.5F);
                    entity.setChanged();
                }
            }
            return;
        }

        // Dedicated Cryo-Freezing Modes (no bucket needed!)
        if (entity.outputMode == MODE_ICE) {
            boolean hasSeed = !catalyst.isEmpty() && (catalyst.is(ObsidiumMod.DEW_DROPLET) || catalyst.is(Items.SNOWBALL) || catalyst.is(Items.SNOW_BLOCK));
            int waterCost = hasSeed ? 250 : 500;
            ItemStack res = new ItemStack(Items.ICE);
            if (entity.waterAmount >= waterCost && canOutputStack(entity.inventory, 2, res)) {
                if (hasSeed) catalyst.shrink(1);
                entity.waterAmount -= waterCost;
                addOutputStack(entity.inventory, 2, res);
                level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 0.45F, 1.5F);
                entity.setChanged();
            }
        } else if (entity.outputMode == MODE_PACKED_ICE) {
            boolean hasIceSeed = !catalyst.isEmpty() && catalyst.is(Items.ICE);
            int waterCost = hasIceSeed ? 500 : 1500;
            ItemStack res = new ItemStack(Items.PACKED_ICE);
            if (entity.waterAmount >= waterCost && canOutputStack(entity.inventory, 2, res)) {
                if (hasIceSeed) catalyst.shrink(1);
                entity.waterAmount -= waterCost;
                addOutputStack(entity.inventory, 2, res);
                level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 0.5F, 1.35F);
                entity.setChanged();
            }
        } else if (entity.outputMode == MODE_BLUE_ICE) {
            boolean hasPackedSeed = !catalyst.isEmpty() && catalyst.is(Items.PACKED_ICE);
            int waterCost = hasPackedSeed ? 1000 : 3000;
            ItemStack res = new ItemStack(Items.BLUE_ICE);
            if (entity.waterAmount >= waterCost && canOutputStack(entity.inventory, 2, res)) {
                if (hasPackedSeed) catalyst.shrink(1);
                entity.waterAmount -= waterCost;
                addOutputStack(entity.inventory, 2, res);
                level.playSound(null, pos, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.BLOCKS, 0.55F, 1.3F);
                entity.setChanged();
            }
        }
    }

    private static int getEnvironmentalEfficiency(Level level, BlockPos pos) {
        int eff = 100; // Base ambient condensation (1.0x)

        BlockState below = level.getBlockState(pos.below());
        if (below.is(Blocks.BLUE_ICE) || below.is(Blocks.PACKED_ICE) || below.is(Blocks.ICE)) {
            eff += 100; // +1.0x (total 2.0x)
        } else if (below.is(Blocks.WATER)) {
            eff += 50;  // +0.5x (total 1.5x)
        } else if (below.is(Blocks.MOSS_BLOCK) || below.is(Blocks.SPONGE) || below.is(Blocks.WET_SPONGE)) {
            eff += 25;  // +0.25x
        }

        if (level.isRainingAt(pos)) {
            eff += 50; // Rain boost
        }

        return eff;
    }

    // Fabric Transfer API for Fluids (Water extraction via pipes)
    public final Storage<FluidVariant> fluidStorage = new Storage<FluidVariant>() {
        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return 0; // Condenser only outputs water
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (!resource.isOf(Fluids.WATER) || waterAmount <= 0) return 0;
            long maxExtract_mB = maxAmount / 81L;
            long toExtract = Math.min(maxExtract_mB, waterAmount);
            if (toExtract > 0) {
                transaction.addCloseCallback((t, res) -> {
                    if (res.wasCommitted()) {
                        waterAmount -= (int) toExtract;
                        setChanged();
                    }
                });
                return toExtract * 81L;
            }
            return 0;
        }

        @Override
        public Iterator<StorageView<FluidVariant>> iterator() {
            StorageView<FluidVariant> view = new StorageView<FluidVariant>() {
                @Override
                public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
                    return fluidStorage.extract(resource, maxAmount, transaction);
                }

                @Override
                public boolean isResourceBlank() {
                    return waterAmount <= 0;
                }

                @Override
                public FluidVariant getResource() {
                    return FluidVariant.of(Fluids.WATER);
                }

                @Override
                public long getAmount() {
                    return waterAmount * 81L;
                }

                @Override
                public long getCapacity() {
                    return MAX_WATER * 81L;
                }
            };
            return List.of(view).iterator();
        }
    };

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("WaterAmount", this.waterAmount);
        tag.putInt("Progress", this.progress);
        tag.putInt("EfficiencyMultiplier", this.efficiencyMultiplier);
        tag.putInt("OutputMode", this.outputMode);

        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                tag.put("Slot" + i, stack.save(registries));
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.waterAmount = tag.getInt("WaterAmount");
        this.progress = tag.getInt("Progress");
        this.efficiencyMultiplier = tag.getInt("EfficiencyMultiplier");
        this.outputMode = tag.getInt("OutputMode");

        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (tag.contains("Slot" + i)) {
                inventory.setItem(i, ItemStack.parse(registries, tag.getCompound("Slot" + i)).orElse(ItemStack.EMPTY));
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Hydrothermal Cryo-Condenser");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new HydrothermalCondenserMenu(syncId, playerInventory, this.inventory, this.data, this);
    }

    @Override
    public Container getInventory() {
        return this.inventory;
    }

    // WorldlyContainer implementation for automation (Hoppers / Pipes)
    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return new int[]{2, 3}; // Output: Water Bucket, Dew Droplets
        } else if (side == Direction.UP) {
            return new int[]{0};    // Input: Moisture Items
        } else {
            return new int[]{1};    // Side: Empty Buckets
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 2 || slot == 3;
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
        return inventory.stillValid(player);
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return inventory.canPlaceItem(slot, stack);
    }

    @Override
    public boolean isInputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 0 || slot == 1;
    }

    @Override
    public boolean isOutputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 2 || slot == 3;
    }
}

