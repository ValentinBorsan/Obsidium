package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;

public class DionysianAgingBarrelBlockEntity extends BlockEntity implements MenuProvider, InventoryHolder, MachineInventory, WorldlyContainer {

    public static final int INVENTORY_SIZE = 2;
    // Slot 0: Input Bottle of Tuica
    // Slot 1: Output Aged Tuica

    public static final int AGING_TIME = 2400; // ~2 real minutes (balanced duration per aging stage)

    private final NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private int agingProgress = 0;

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> agingProgress;
                case 1 -> AGING_TIME;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) agingProgress = value;
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public DionysianAgingBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.DIONYSIAN_AGING_BARREL_BE, pos, state);
    }

    public static boolean canAge(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(ObsidiumMod.VOLCANIC_TUICA) || stack.is(ObsidiumMod.VINTAGE_VOLCANIC_TUICA);
    }

    public static ItemStack getAgedResult(ItemStack input) {
        if (input.is(ObsidiumMod.VOLCANIC_TUICA)) {
            return new ItemStack(ObsidiumMod.VINTAGE_VOLCANIC_TUICA);
        } else if (input.is(ObsidiumMod.VINTAGE_VOLCANIC_TUICA)) {
            return new ItemStack(ObsidiumMod.GRAND_RESERVE_TUICA);
        }
        return ItemStack.EMPTY;
    }

    public void tickServer(Level level, BlockPos pos, BlockState state) {
        boolean changed = false;

        ItemStack input = items.get(0);
        ItemStack output = items.get(1);

        boolean isAging = false;
        if (canAge(input)) {
            ItemStack targetResult = getAgedResult(input);
            boolean canOutput = output.isEmpty() || (output.is(targetResult.getItem()) && output.getCount() < output.getMaxStackSize());

            if (canOutput) {
                agingProgress++;
                isAging = true;
                changed = true;

                if (agingProgress >= AGING_TIME) {
                    agingProgress = 0;
                    input.shrink(1);

                    if (output.isEmpty()) {
                        items.set(1, targetResult.copy());
                    } else {
                        output.grow(1);
                    }

                    level.playSound(null, pos, net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.BLOCKS, 0.7F, 1.2F);
                }
            } else {
                if (agingProgress > 0) {
                    agingProgress = 0;
                    changed = true;
                }
            }
        } else {
            if (agingProgress > 0) {
                agingProgress = 0;
                changed = true;
            }
        }

        boolean currentAgingState = state.getValue(DionysianAgingBarrelBlock.AGING);
        if (currentAgingState != isAging) {
            level.setBlock(pos, state.setValue(DionysianAgingBarrelBlock.AGING, isAging), 3);
            changed = true;
        }

        if (changed) {
            setChanged();
        }

        VolcaniteStoragePipeBlockEntity.autoEjectMachineOutputs(level, pos, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putInt("AgingProgress", this.agingProgress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items.clear();
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.agingProgress = tag.getInt("AgingProgress");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.dionysian_aging_barrel");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new DionysianAgingBarrelMenu(syncId, playerInventory, this, this.dataAccess);
    }

    @Override
    public Container getInventory() {
        return this;
    }

    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && canAge(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return new int[]{ 1 };
        } else {
            return new int[]{ 0 };
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 0 && canAge(stack);
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
}

