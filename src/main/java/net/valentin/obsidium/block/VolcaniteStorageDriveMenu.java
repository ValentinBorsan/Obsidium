package net.valentin.obsidium.block;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.VolcaniteLiquidStorageDiskItem;
import net.valentin.obsidium.item.VolcaniteStorageDiskItem;

public class VolcaniteStorageDriveMenu extends AbstractContainerMenu {
    private final Container container;

    public VolcaniteStorageDriveMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(8));
    }

    public VolcaniteStorageDriveMenu(int syncId, Inventory playerInventory, Container container) {
        super(ObsidiumMod.VOLCANITE_STORAGE_DRIVE_MENU, syncId);
        checkContainerSize(container, 8);
        this.container = container;
        container.startOpen(playerInventory.player);

        // 8 Storage Disk slots (2 rows x 4 columns)
        for (int row = 0; row < 2; ++row) {
            for (int col = 0; col < 4; ++col) {
                int slotIndex = col + row * 4;
                int x = 49 + col * 20;
                int y = 24 + row * 22;
                this.addSlot(new Slot(container, slotIndex, x + 1, y + 1) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.getItem() instanceof VolcaniteStorageDiskItem || stack.getItem() instanceof VolcaniteLiquidStorageDiskItem;
                    }

                    @Override
                    public int getMaxStackSize() {
                        return 1;
                    }
                });
            }
        }

        // Player Inventory
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemStack = slotStack.copy();

            if (index < 8) {
                if (!this.moveItemStackTo(slotStack, 8, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (slotStack.getItem() instanceof VolcaniteStorageDiskItem || slotStack.getItem() instanceof VolcaniteLiquidStorageDiskItem) {
                    if (!this.moveItemStackTo(slotStack, 0, 8, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}
