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

public class DionysianAgingBarrelMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    public DionysianAgingBarrelMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(DionysianAgingBarrelBlockEntity.INVENTORY_SIZE), new SimpleContainerData(2));
    }

    public DionysianAgingBarrelMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(ObsidiumMod.DIONYSIAN_AGING_BARREL_MENU, syncId);
        this.container = container;
        this.data = data;
        checkContainerSize(container, DionysianAgingBarrelBlockEntity.INVENTORY_SIZE);
        checkContainerDataCount(data, 2);
        container.startOpen(playerInventory.player);

        // Machine Slots
        // 0: Input Bottle of Tuica (left)
        this.addSlot(new Slot(container, 0, 56, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return DionysianAgingBarrelBlockEntity.canAge(stack);
            }
        });

        // 1: Output Aged Tuica (right)
        this.addSlot(new Slot(container, 1, 116, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Player Inventory (3x9)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar (1x9)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(data);
    }

    public int getAgingProgress() {
        return this.data.get(0);
    }

    public int getMaxAgingTime() {
        return this.data.get(1);
    }

    public int getAgingProgressScaled(int pixels) {
        int max = getMaxAgingTime();
        if (max == 0) max = 2400;
        return getAgingProgress() * pixels / max;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemStack = stackInSlot.copy();

            // Output slot 1 -> player inventory
            if (index == 1) {
                if (!this.moveItemStackTo(stackInSlot, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, itemStack);
            }
            // Machine slot 0 -> player inventory
            else if (index == 0) {
                if (!this.moveItemStackTo(stackInSlot, 2, 38, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // Player inventory -> machine slot 0
            else {
                if (DionysianAgingBarrelBlockEntity.canAge(stackInSlot)) {
                    if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < 29) {
                    if (!this.moveItemStackTo(stackInSlot, 29, 38, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 29 && !this.moveItemStackTo(stackInSlot, 2, 29, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stackInSlot);
        }

        return itemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}

