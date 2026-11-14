package net.valentin.obsidium.block;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.ObsidiumMod;

public class DemetrianComposterMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public DemetrianComposterMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainer(DemetrianComposterBlockEntity.INVENTORY_SIZE), new SimpleContainerData(2));
    }

    public DemetrianComposterMenu(int id, Inventory playerInventory, Container container, ContainerData data) {
        super(ObsidiumMod.DEMETRIAN_COMPOSTER_MENU, id);
        checkContainerSize(container, DemetrianComposterBlockEntity.INVENTORY_SIZE);
        checkContainerDataCount(data, 2);
        this.container = container;
        this.data = data;
        container.startOpen(playerInventory.player);

        this.addSlot(new Slot(container, 0, 38, 44) {
            @Override public boolean mayPlace(ItemStack stack) { return DemetrianComposterBlockEntity.isCompostable(stack); }
        });
        this.addSlot(new Slot(container, 1, 38, 72) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(ObsidiumMod.VOLCANIC_ASH); }
        });
        this.addSlot(new Slot(container, 2, 158, 58) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 30 + col * 18, 102 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) this.addSlot(new Slot(playerInventory, col, 30 + col * 18, 160));
        addDataSlots(data);
    }

    public int getProgress() { return data.get(0); }
    public boolean isOutputBlocked() { return data.get(1) != 0; }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;
        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index == 2) {
            if (!moveItemStackTo(stack, 3, 39, true)) return ItemStack.EMPTY;
        } else if (index < 3) {
            if (!moveItemStackTo(stack, 3, 39, true)) return ItemStack.EMPTY;
        } else if (stack.is(ObsidiumMod.VOLCANIC_ASH)) {
            if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
        } else if (DemetrianComposterBlockEntity.isCompostable(stack)) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return result;
    }

    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
}
