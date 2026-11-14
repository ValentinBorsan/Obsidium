package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;

import java.util.Map;

public class VolcaniteChestBlockEntity extends BaseContainerBlockEntity {
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);

    public VolcaniteChestBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.VOLCANITE_CHEST_BE, pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.obsidium.volcanite_chest");
    }

    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory inv) {
        return ChestMenu.threeRows(syncId, inv, this);
    }

    @Override
    public int getContainerSize() {
        return 27;
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

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
        setChanged();
    }

    // --- Network Storage Methods ---

    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return stack;
        ItemStack remaining = stack.copy();
        int size = getContainerSize();

        for (int i = 0; i < size; i++) {
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

        for (int i = 0; i < size; i++) {
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
        ItemStack result = StorageItemTransfers.extract(items, getContainerSize(), item, amount, simulate);
        if (!simulate && !result.isEmpty()) setChanged();
        return result;
    }

    public ItemStack extractMatchingItem(ItemStack template, int amount, boolean simulate) {
        ItemStack result = StorageItemTransfers.extractMatching(items, getContainerSize(), template, amount, simulate);
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
        return getContainerSize() * 64;
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
