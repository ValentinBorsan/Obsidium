package net.valentin.obsidium.block;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;
import net.valentin.obsidium.ObsidiumMod;

import net.minecraft.world.Container;

public class VolcaniteGrinderMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public VolcaniteGrinderMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(3), new net.minecraft.world.inventory.SimpleContainerData(3));
    }

    public VolcaniteGrinderMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(ObsidiumMod.VOLCANITE_GRINDER_MENU, syncId);
        checkContainerSize(container, 3);
        checkContainerDataCount(data, 3);
        this.container = container;
        this.data = data;

        // Input Slot (Ore)
        this.addSlot(new Slot(container, 0, 44, 35));
        // Output Slot (Dust)
        this.addSlot(new Slot(container, 1, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });
        // Upgrade Slot (Overdrive / Conduit Upgrade)
        this.addSlot(new Slot(container, 2, 150, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return net.valentin.obsidium.item.VolcaniteOverclockUpgradeItem.isOverclockUpgrade(stack);
            }
        });

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }

        this.addDataSlots(data);
    }

    public int getProgress() {
        return this.data.get(0);
    }

    public int getEnergyAmount() {
        return (this.data.get(1) << 16) | (this.data.get(2) & 0xFFFF);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();
            if (index < 3) {
                if (!this.moveItemStackTo(itemStack2, 3, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (net.valentin.obsidium.item.VolcaniteOverclockUpgradeItem.isOverclockUpgrade(itemStack2)) {
                    if (!this.moveItemStackTo(itemStack2, 2, 3, false)) {
                        if (!this.moveItemStackTo(itemStack2, 0, 1, false)) return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemStack2, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (itemStack2.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return itemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}
