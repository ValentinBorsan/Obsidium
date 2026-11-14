package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

public class HydrothermalVitrifierBlockEntity extends BlockEntity implements MenuProvider, InventoryHolder, WorldlyContainer, MachineInventory {
    public static final int INVENTORY_SIZE = 8;
    // Slot 0: Stone Input (Stone, Smooth Basalt, Cobblestone, Deepslate)
    // Slot 1: Volcanic Ash Input (Volcanic Ash, Ash Briquette)
    // Slot 2: Water Bucket Input (Water Bucket)
    // Slot 3: Water Bucket Output (Empty Bucket)
    // Slot 4: Lava Bucket Input (Lava Bucket)
    // Slot 5: Lava Bucket Output (Empty Bucket)
    // Slot 6: Obsidian Output
    // Slot 7: Rare Bonus Output (Volcanite / Crying Obsidian)

    public static final int MAX_WATER = 4000;
    public static final int MAX_LAVA = 4000;
    public static final int WATER_PER_OPERATION = 250;
    public static final int LAVA_PER_OPERATION = 250;
    public static final int MAX_PROGRESS = 240; // 12 seconds per obsidian block (slow, steady hydrothermal synthesis)

    private int waterAmount = 0;
    private int lavaAmount = 0;
    private int progress = 0;

    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            HydrothermalVitrifierBlockEntity.this.setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            if (slot == 0) return isStoneIngredient(stack);
            if (slot == 1) return isAshIngredient(stack);
            if (slot == 2) return stack.is(Items.WATER_BUCKET) || stack.is(ObsidiumMod.DEW_DROPLET);
            if (slot == 4) return stack.is(Items.LAVA_BUCKET) || stack.is(ObsidiumMod.MAGMA_DROPLET);
            return false;
        }
    };

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> waterAmount;
                case 1 -> lavaAmount;
                case 2 -> progress;
                case 3 -> MAX_PROGRESS;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> waterAmount = value;
                case 1 -> lavaAmount = value;
                case 2 -> progress = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public HydrothermalVitrifierBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.HYDROTHERMAL_VITRIFIER_BE, pos, state);
    }

    public static boolean isStoneIngredient(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(Items.STONE)
                || stack.is(Items.SMOOTH_BASALT)
                || stack.is(Items.BASALT)
                || stack.is(Items.COBBLESTONE)
                || stack.is(Items.COBBLED_DEEPSLATE)
                || stack.is(Items.DEEPSLATE)
                || stack.is(ItemTags.STONE_CRAFTING_MATERIALS);
    }

    public static boolean isAshIngredient(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(ObsidiumMod.VOLCANIC_ASH) || stack.is(ObsidiumMod.ASH_BRIQUETTE);
    }

    public int getWaterAmount() {
        return waterAmount;
    }

    public void addWater(int amount) {
        this.waterAmount = Math.min(MAX_WATER, this.waterAmount + amount);
        setChanged();
    }

    public int getLavaAmount() {
        return lavaAmount;
    }

    public void addLava(int amount) {
        this.lavaAmount = Math.min(MAX_LAVA, this.lavaAmount + amount);
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HydrothermalVitrifierBlockEntity entity) {
        if (level.isClientSide()) return;

        boolean wasLit = state.getValue(HydrothermalVitrifierBlock.LIT);
        boolean isLit = false;

        // 1. Process Water bucket or Dew Droplet in slot 2
        ItemStack waterBucketIn = entity.inventory.getItem(2);
        if (!waterBucketIn.isEmpty()) {
            if (waterBucketIn.is(Items.WATER_BUCKET) && entity.waterAmount <= MAX_WATER - 1000) {
                if (entity.canOutputBucket(3)) {
                    waterBucketIn.shrink(1);
                    entity.depositBucketOutput(3);
                    entity.waterAmount += 1000;
                    entity.setChanged();
                }
            } else if (waterBucketIn.is(ObsidiumMod.DEW_DROPLET) && entity.waterAmount <= MAX_WATER - 250) {
                waterBucketIn.shrink(1);
                entity.waterAmount += 250;
                entity.setChanged();
            }
        }

        // 2. Process Lava bucket or Magma Droplet in slot 4
        ItemStack lavaBucketIn = entity.inventory.getItem(4);
        if (!lavaBucketIn.isEmpty()) {
            if (lavaBucketIn.is(Items.LAVA_BUCKET) && entity.lavaAmount <= MAX_LAVA - 1000) {
                if (entity.canOutputBucket(5)) {
                    lavaBucketIn.shrink(1);
                    entity.depositBucketOutput(5);
                    entity.lavaAmount += 1000;
                    entity.setChanged();
                }
            } else if (lavaBucketIn.is(ObsidiumMod.MAGMA_DROPLET) && entity.lavaAmount <= MAX_LAVA - 250) {
                lavaBucketIn.shrink(1);
                entity.lavaAmount += 250;
                entity.setChanged();
            }
        }

        // 3. Check if machine can vitrify obsidian
        boolean hasFluids = entity.waterAmount >= WATER_PER_OPERATION && entity.lavaAmount >= LAVA_PER_OPERATION;
        ItemStack stoneSlot = entity.inventory.getItem(0);
        ItemStack ashSlot = entity.inventory.getItem(1);
        boolean hasItems = isStoneIngredient(stoneSlot) && isAshIngredient(ashSlot);

        ItemStack outputSlot = entity.inventory.getItem(6);
        boolean canOutput = outputSlot.isEmpty() || (outputSlot.is(Items.OBSIDIAN) && outputSlot.getCount() < outputSlot.getMaxStackSize());

        if (hasFluids && hasItems && canOutput) {
            isLit = true;
            entity.progress++;

            if (entity.progress >= MAX_PROGRESS) {
                entity.progress = 0;

                // Deduct fluids
                entity.waterAmount -= WATER_PER_OPERATION;
                entity.lavaAmount -= LAVA_PER_OPERATION;

                // Deduct ingredients
                stoneSlot.shrink(1);
                ashSlot.shrink(1);

                // Deposit Obsidian
                if (outputSlot.isEmpty()) {
                    entity.inventory.setItem(6, new ItemStack(Items.OBSIDIAN));
                } else {
                    outputSlot.grow(1);
                }

                // 5% bonus chance: Volcanite or Crying Obsidian
                if (level.random.nextFloat() < 0.05F) {
                    ItemStack bonus = level.random.nextBoolean() ? new ItemStack(ObsidiumMod.VOLCANITE) : new ItemStack(Items.CRYING_OBSIDIAN);
                    ItemStack bonusSlot = entity.inventory.getItem(7);
                    if (bonusSlot.isEmpty()) {
                        entity.inventory.setItem(7, bonus);
                    } else if (bonusSlot.is(bonus.getItem()) && bonusSlot.getCount() < bonusSlot.getMaxStackSize()) {
                        bonusSlot.grow(1);
                    }
                }

                level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.7F, 0.9F);
                entity.setChanged();
            }
        } else {
            if (entity.progress > 0) {
                entity.progress = Math.max(0, entity.progress - 2);
            }
        }

        if (wasLit != isLit) {
            level.setBlock(pos, state.setValue(HydrothermalVitrifierBlock.LIT, isLit), 3);
        }

        VolcaniteStoragePipeBlockEntity.autoEjectMachineOutputs(level, pos, entity);
    }

    private boolean canOutputBucket(int slot) {
        ItemStack out = inventory.getItem(slot);
        return out.isEmpty() || (out.is(Items.BUCKET) && out.getCount() < out.getMaxStackSize());
    }

    private void depositBucketOutput(int slot) {
        ItemStack out = inventory.getItem(slot);
        if (out.isEmpty()) {
            inventory.setItem(slot, new ItemStack(Items.BUCKET));
        } else {
            out.grow(1);
        }
    }

    // Fabric Transfer API for Fluids (Dual Water & Lava acceptance)
    public final Storage<FluidVariant> fluidStorage = new Storage<FluidVariant>() {
        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isOf(Fluids.WATER)) {
                long maxInsert_mB = maxAmount / 81L;
                long space_mB = MAX_WATER - waterAmount;
                long toInsert = Math.min(maxInsert_mB, space_mB);
                if (toInsert > 0) {
                    transaction.addCloseCallback((t, res) -> {
                        if (res.wasCommitted()) {
                            waterAmount += (int) toInsert;
                            setChanged();
                        }
                    });
                    return toInsert * 81L;
                }
            } else if (resource.isOf(Fluids.LAVA)) {
                long maxInsert_mB = maxAmount / 81L;
                long space_mB = MAX_LAVA - lavaAmount;
                long toInsert = Math.min(maxInsert_mB, space_mB);
                if (toInsert > 0) {
                    transaction.addCloseCallback((t, res) -> {
                        if (res.wasCommitted()) {
                            lavaAmount += (int) toInsert;
                            setChanged();
                        }
                    });
                    return toInsert * 81L;
                }
            }
            return 0;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return 0; // Machine consumes fluids internally
        }

        @Override
        public Iterator<StorageView<FluidVariant>> iterator() {
            return List.<StorageView<FluidVariant>>of(
                    new FluidStorageView(Fluids.WATER, waterAmount, MAX_WATER),
                    new FluidStorageView(Fluids.LAVA, lavaAmount, MAX_LAVA)
            ).iterator();
        }
    };

    private static class FluidStorageView implements StorageView<FluidVariant> {
        private final net.minecraft.world.level.material.Fluid fluid;
        private final int amount_mB;
        private final int cap_mB;

        FluidStorageView(net.minecraft.world.level.material.Fluid fluid, int amount_mB, int cap_mB) {
            this.fluid = fluid;
            this.amount_mB = amount_mB;
            this.cap_mB = cap_mB;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public boolean isResourceBlank() {
            return amount_mB <= 0;
        }

        @Override
        public FluidVariant getResource() {
            return amount_mB > 0 ? FluidVariant.of(fluid) : FluidVariant.blank();
        }

        @Override
        public long getAmount() {
            return amount_mB * 81L;
        }

        @Override
        public long getCapacity() {
            return cap_mB * 81L;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Water", waterAmount);
        tag.putInt("Lava", lavaAmount);
        tag.putInt("Progress", progress);

        CompoundTag invTag = new CompoundTag();
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (!inventory.getItem(i).isEmpty()) {
                invTag.put("Slot" + i, inventory.getItem(i).save(registries));
            }
        }
        tag.put("Inventory", invTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        waterAmount = tag.getInt("Water");
        lavaAmount = tag.getInt("Lava");
        progress = tag.getInt("Progress");

        CompoundTag invTag = tag.getCompound("Inventory");
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (invTag.contains("Slot" + i)) {
                inventory.setItem(i, ItemStack.parse(registries, invTag.getCompound("Slot" + i)).orElse(ItemStack.EMPTY));
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Hydrothermal Vitrifier");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new HydrothermalVitrifierMenu(syncId, playerInventory, this.inventory, this.data);
    }

    @Override
    public Container getInventory() {
        return this.inventory;
    }

    // WorldlyContainer implementation for sided automation
    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return new int[]{6, 7, 3, 5}; // Output Obsidian, Bonus, Empty Buckets
        } else if (side == Direction.UP) {
            return new int[]{0};          // Stone Input
        } else {
            return new int[]{1, 2, 4};    // Ash Input, Water Bucket, Lava Bucket
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 6 || slot == 7 || slot == 3 || slot == 5;
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
        return slot == 0 || slot == 1 || slot == 2 || slot == 4;
    }

    @Override
    public boolean isOutputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 6 || slot == 7 || slot == 3 || slot == 5;
    }
}
