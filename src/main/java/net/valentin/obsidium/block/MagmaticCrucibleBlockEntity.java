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
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

public class MagmaticCrucibleBlockEntity extends BlockEntity implements MenuProvider, InventoryHolder, WorldlyContainer, MachineInventory {
    public static final int INVENTORY_SIZE = 4;
    // Slot 0: Rock Input (Cobblestone, Stone, Basalt, Smooth Basalt, Deepslate, Netherrack, etc.)
    // Slot 1: Empty Bucket In
    // Slot 2: Lava Bucket Out
    // Slot 3: Magma Droplets Out (subproduct)

    public static final int MAX_LAVA = 4000;
    public static final int LAVA_PER_ROCK = 250;
    public static final int BASE_MELT_TICKS = 300; // 15s per rock (4 rocks = 1000mB = exactly 60 seconds per bucket)

    private int lavaAmount = 0;
    private int progress = 0;
    private int heatMultiplier = 0; // 0 = none, 100 = 1.0x, 125 = 1.25x, 150 = 1.5x

    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            MagmaticCrucibleBlockEntity.this.setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            if (slot == 0) return isMeltableRock(stack);
            if (slot == 1) return stack.is(Items.BUCKET);
            return false;
        }
    };

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> lavaAmount;
                case 1 -> progress;
                case 2 -> BASE_MELT_TICKS;
                case 3 -> heatMultiplier;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> lavaAmount = value;
                case 1 -> progress = value;
                case 3 -> heatMultiplier = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public MagmaticCrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.MAGMATIC_CRUCIBLE_BE, pos, state);
    }

    public static boolean isMeltableRock(ItemStack stack) {
        return stack.is(Items.COBBLESTONE)
                || stack.is(Items.STONE)
                || stack.is(Items.SMOOTH_BASALT)
                || stack.is(Items.BASALT)
                || stack.is(Items.COBBLED_DEEPSLATE)
                || stack.is(Items.DEEPSLATE)
                || stack.is(Items.NETHERRACK)
                || stack.is(Items.BLACKSTONE)
                || stack.is(Items.MAGMA_BLOCK);
    }

    public int getLavaAmount() {
        return this.lavaAmount;
    }

    public void extractLava(int amount) {
        this.lavaAmount = Math.max(0, this.lavaAmount - amount);
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MagmaticCrucibleBlockEntity entity) {
        if (level.isClientSide()) return;

        boolean wasLit = state.getValue(MagmaticCrucibleBlock.LIT);
        boolean isLit = false;

        // 1. Detect heat source below
        int heat = getHeatSourceMultiplier(level, pos.below());
        entity.heatMultiplier = heat;

        // 2. Process automatic bucket filling (Slot 1 -> Slot 2)
        if (entity.lavaAmount >= 1000) {
            ItemStack emptyBucket = entity.inventory.getItem(1);
            ItemStack lavaOut = entity.inventory.getItem(2);
            if (!emptyBucket.isEmpty() && emptyBucket.is(Items.BUCKET)) {
                if (lavaOut.isEmpty()) {
                    emptyBucket.shrink(1);
                    entity.inventory.setItem(2, new ItemStack(Items.LAVA_BUCKET));
                    entity.lavaAmount -= 1000;
                    entity.setChanged();
                } else if (lavaOut.is(Items.LAVA_BUCKET) && lavaOut.getCount() < lavaOut.getMaxStackSize()) {
                    emptyBucket.shrink(1);
                    lavaOut.grow(1);
                    entity.lavaAmount -= 1000;
                    entity.setChanged();
                }
            }
        }

        // 3. Process rock melting if heat source is present and space in lava tank
        ItemStack rockSlot = entity.inventory.getItem(0);
        boolean hasRock = isMeltableRock(rockSlot);
        boolean hasTankSpace = entity.lavaAmount <= MAX_LAVA - LAVA_PER_ROCK;

        if (heat > 0 && hasRock && hasTankSpace) {
            isLit = true;
            // Progress increment scaled by heat multiplier (100 = 1 tick per tick, 150 = 1.5 ticks per tick)
            int step = heat >= 150 ? 2 : 1;
            entity.progress += step;

            if (entity.progress >= BASE_MELT_TICKS) {
                entity.progress = 0;

                // Consume 1 rock
                rockSlot.shrink(1);

                // Add 250 mB of lava (4 rocks = 1000 mB)
                entity.lavaAmount = Math.min(MAX_LAVA, entity.lavaAmount + LAVA_PER_ROCK);

                // Subproduct: 50% chance to crystallize a Magma Droplet in Slot 3
                if (level.random.nextFloat() < 0.50F) {
                    ItemStack dropSlot = entity.inventory.getItem(3);
                    if (dropSlot.isEmpty()) {
                        entity.inventory.setItem(3, new ItemStack(ObsidiumMod.MAGMA_DROPLET));
                    } else if (dropSlot.is(ObsidiumMod.MAGMA_DROPLET) && dropSlot.getCount() < dropSlot.getMaxStackSize()) {
                        dropSlot.grow(1);
                    }
                }

                level.playSound(null, pos, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.6F, 1.2F);
                entity.setChanged();
            }
        } else {
            if (entity.progress > 0) {
                entity.progress = Math.max(0, entity.progress - 2);
            }
        }

        if (wasLit != isLit) {
            level.setBlock(pos, state.setValue(MagmaticCrucibleBlock.LIT, isLit), 3);
        }

        // 4. Auto-output lava to any adjacent Fabric fluid storage.
        if (entity.lavaAmount > 0) {
            for (Direction dir : Direction.values()) {
                if (entity.lavaAmount <= 0) break;
                BlockPos neighborPos = pos.relative(dir);
                Storage<FluidVariant> target = FluidStorage.SIDED.find(level, neighborPos, dir.getOpposite());
                if (target == null || !target.supportsInsertion()) continue;

                long moved = StorageUtil.move(
                        entity.fluidStorage,
                        target,
                        fluid -> fluid.isOf(Fluids.LAVA),
                        100L * 81L,
                        null
                );
                if (moved > 0) entity.setChanged();
            }
        }

        VolcaniteStoragePipeBlockEntity.autoEjectMachineOutputs(level, pos, entity);
    }

    private static int getHeatSourceMultiplier(Level level, BlockPos belowPos) {
        BlockState belowState = level.getBlockState(belowPos);
        if (belowState.is(Blocks.LAVA)) {
            return 150; // 1.5x speed (40 seconds per bucket)
        }
        if (belowState.is(Blocks.SOUL_CAMPFIRE) && belowState.hasProperty(CampfireBlock.LIT) && belowState.getValue(CampfireBlock.LIT)) {
            return 125; // 1.25x speed (48 seconds per bucket)
        }
        if (belowState.is(Blocks.CAMPFIRE) && belowState.hasProperty(CampfireBlock.LIT) && belowState.getValue(CampfireBlock.LIT)) {
            return 100; // 1.0x speed (60 seconds per bucket)
        }
        if (belowState.is(Blocks.MAGMA_BLOCK)) {
            return 100; // 1.0x speed (60 seconds per bucket)
        }
        if (belowState.is(Blocks.FIRE) || belowState.is(Blocks.SOUL_FIRE)) {
            return 100;
        }
        return 0; // No valid heat source
    }

    // Fabric Transfer API for Fluids (Lava extraction via pipes)
    public final Storage<FluidVariant> fluidStorage = new Storage<FluidVariant>() {
        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return 0; // Crucible only generates and outputs lava
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (!resource.isOf(Fluids.LAVA) || lavaAmount <= 0) return 0;
            long maxExtract_mB = maxAmount / 81L;
            long toExtract = Math.min(maxExtract_mB, lavaAmount);
            if (toExtract > 0) {
                transaction.addCloseCallback((t, res) -> {
                    if (res.wasCommitted()) {
                        lavaAmount -= (int) toExtract;
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
                    return lavaAmount <= 0;
                }

                @Override
                public FluidVariant getResource() {
                    return FluidVariant.of(Fluids.LAVA);
                }

                @Override
                public long getAmount() {
                    return lavaAmount * 81L;
                }

                @Override
                public long getCapacity() {
                    return MAX_LAVA * 81L;
                }
            };
            return List.of(view).iterator();
        }
    };

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("LavaAmount", this.lavaAmount);
        tag.putInt("Progress", this.progress);
        tag.putInt("HeatMultiplier", this.heatMultiplier);

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
        this.lavaAmount = tag.getInt("LavaAmount");
        this.progress = tag.getInt("Progress");
        this.heatMultiplier = tag.getInt("HeatMultiplier");

        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (tag.contains("Slot" + i)) {
                inventory.setItem(i, ItemStack.parse(registries, tag.getCompound("Slot" + i)).orElse(ItemStack.EMPTY));
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Magmatic Crucible");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new MagmaticCrucibleMenu(syncId, playerInventory, this.inventory, this.data);
    }

    @Override
    public Container getInventory() {
        return this.inventory;
    }

    // WorldlyContainer implementation for automation (Hoppers / Pipes)
    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return new int[]{2, 3}; // Output: Lava Bucket, Magma Droplets
        } else if (side == Direction.UP) {
            return new int[]{0};    // Input: Rock
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

