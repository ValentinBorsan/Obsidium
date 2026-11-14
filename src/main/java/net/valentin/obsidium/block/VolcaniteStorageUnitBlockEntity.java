package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;

import java.util.Map;

/**
 * Grand Storage Chest (108 slots)
 * Functions as a massive standalone chest and seamlessly integrates with Volcanite Storage networks & pipes.
 */
public class VolcaniteStorageUnitBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int CONTAINER_SIZE = 108;
    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

    public VolcaniteStorageUnitBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.VOLCANITE_STORAGE_UNIT_BE, pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.volcanite_storage_unit");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return new VolcaniteStorageUnitMenu(syncId, inv, this);
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = ContainerHelper.removeItem(items, slot, amount);
        if (!stack.isEmpty()) setChanged();
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
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

    // --- Network Storage Methods ---

    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return stack;
        ItemStack remaining = stack.copy();

        // 1. First try existing matching stacks
        for (int i = 0; i < CONTAINER_SIZE; i++) {
            ItemStack current = items.get(i);
            if (!current.isEmpty() && ItemStack.isSameItemSameComponents(current, remaining)) {
                int space = Math.min(current.getMaxStackSize(), this.getMaxStackSize()) - current.getCount();
                if (space > 0) {
                    int toAdd = Math.min(space, remaining.getCount());
                    if (!simulate) {
                        current.grow(toAdd);
                        setChanged();
                    }
                    remaining.shrink(toAdd);
                    if (remaining.isEmpty()) return ItemStack.EMPTY;
                }
            }
        }

        // 2. Insert into empty slots
        for (int i = 0; i < CONTAINER_SIZE; i++) {
            ItemStack current = items.get(i);
            if (current.isEmpty()) {
                int toAdd = Math.min(remaining.getMaxStackSize(), remaining.getCount());
                if (!simulate) {
                    items.set(i, remaining.copyWithCount(toAdd));
                    setChanged();
                }
                remaining.shrink(toAdd);
                if (remaining.isEmpty()) return ItemStack.EMPTY;
            }
        }

        return remaining;
    }

    public ItemStack extractItem(Item item, int amount, boolean simulate) {
        ItemStack result = StorageItemTransfers.extract(items, CONTAINER_SIZE, item, amount, simulate);
        if (!simulate && !result.isEmpty()) setChanged();
        return result;
    }

    public ItemStack extractMatchingItem(ItemStack template, int amount, boolean simulate) {
        ItemStack result = StorageItemTransfers.extractMatching(items, CONTAINER_SIZE, template, amount, simulate);
        if (!simulate && !result.isEmpty()) setChanged();
        return result;
    }

    public void collectStoredItems(Map<Item, Integer> map) {
        for (ItemStack current : items) {
            if (!current.isEmpty()) {
                map.put(current.getItem(), map.getOrDefault(current.getItem(), 0) + current.getCount());
            }
        }
    }

    public int getTotalStored() {
        int total = 0;
        for (ItemStack current : items) {
            if (!current.isEmpty()) {
                total += current.getCount();
            }
        }
        return total;
    }

    public int getMaxCapacity() {
        return CONTAINER_SIZE * 64;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, this.items, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, provider);
    }
}
