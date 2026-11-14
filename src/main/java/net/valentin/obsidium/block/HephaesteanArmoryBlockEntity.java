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
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.HephaesteanPaxelItem;
import net.valentin.obsidium.menu.HephaesteanArmoryMenu;

public class HephaesteanArmoryBlockEntity extends BaseContainerBlockEntity {
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);

    public HephaesteanArmoryBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.HEPHAESTEAN_ARMORY_BE, pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.obsidium.hephaestean_armory");
    }

    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory inv) {
        return new HephaesteanArmoryMenu(syncId, inv, this);
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

    public static boolean canPlaceInSlot(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (slot == 0) return stack.getItem() instanceof ArmorItem a && a.getType() == ArmorItem.Type.HELMET;
        if (slot == 1) return stack.getItem() instanceof ArmorItem a && a.getType() == ArmorItem.Type.CHESTPLATE;
        if (slot == 2) return stack.getItem() instanceof ArmorItem a && a.getType() == ArmorItem.Type.LEGGINGS;
        if (slot == 3) return stack.getItem() instanceof ArmorItem a && a.getType() == ArmorItem.Type.BOOTS;
        if (slot == 4) return stack.getItem() instanceof SwordItem || stack.getItem() instanceof HephaesteanPaxelItem;
        if (slot == 5) return stack.getItem() instanceof PickaxeItem || stack.getItem() instanceof HephaesteanPaxelItem;
        if (slot == 6) return stack.getItem() instanceof AxeItem || stack.getItem() instanceof HephaesteanPaxelItem;
        if (slot == 7) return stack.getItem() instanceof ShovelItem || stack.getItem() instanceof HephaesteanPaxelItem;
        if (slot == 8) return stack.getItem() instanceof HoeItem || stack.getItem() instanceof HephaesteanPaxelItem;
        return slot >= 9 && slot < 27;
    }

    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return stack;
        ItemStack rem = stack.copy();

        // 1. Try dedicated armor & tool slots 0..8
        for (int i = 0; i <= 8; i++) {
            if (items.get(i).isEmpty() && canPlaceInSlot(i, rem)) {
                if (!simulate) {
                    items.set(i, rem.split(1));
                    setChanged();
                } else {
                    rem.shrink(1);
                }
                if (rem.isEmpty()) return ItemStack.EMPTY;
            }
        }

        // 2. Try general equipment slots 9..26 (empty slots first to preserve discrete damaged tools)
        for (int i = 9; i < 27; i++) {
            ItemStack inSlot = items.get(i);
            if (inSlot.isEmpty()) {
                if (!simulate) {
                    items.set(i, rem.copy());
                    setChanged();
                }
                return ItemStack.EMPTY;
            }
        }

        return rem;
    }

    public ItemStack extractSpecific(ItemStack target, boolean simulate) {
        if (target.isEmpty()) return ItemStack.EMPTY;
        for (int i = 0; i < 27; i++) {
            ItemStack inSlot = items.get(i);
            if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(inSlot, target)) {
                ItemStack res = inSlot.split(1);
                if (!simulate) {
                    if (inSlot.isEmpty()) items.set(i, ItemStack.EMPTY);
                    setChanged();
                }
                return res;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
    }
}
