package net.valentin.obsidium.block;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.ObsidiumMod;

/**
 * 108-Slot Menu for the Volcanite Grand Storage Chest.
 */
public class VolcaniteStorageUnitMenu extends AbstractContainerMenu {
    public static final int CHEST_SLOTS = 108;
    private final Container container;

    public VolcaniteStorageUnitMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(CHEST_SLOTS));
    }

    public VolcaniteStorageUnitMenu(int syncId, Inventory playerInventory, Container container) {
        super(ObsidiumMod.VOLCANITE_STORAGE_UNIT_MENU, syncId);
        checkContainerSize(container, CHEST_SLOTS);
        this.container = container;
        container.startOpen(playerInventory.player);

        // 108 Storage Slots (9 rows of 12 columns)
        for (int row = 0; row < 9; ++row) {
            for (int col = 0; col < 12; ++col) {
                int slotIndex = col + row * 12;
                int x = 8 + col * 18;
                int y = 18 + row * 18;
                this.addSlot(new Slot(container, slotIndex, x + 1, y + 1));
            }
        }

        // Player Inventory (centered at X=35)
        int playerInvX = 35;
        int playerInvY = 188;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, playerInvX + j * 18, playerInvY + i * 18));
            }
        }

        // Player Hotbar
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, playerInvX + i * 18, 246));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemStack = slotStack.copy();

            if (index < CHEST_SLOTS) {
                // From chest into player inventory
                if (!this.moveItemStackTo(slotStack, CHEST_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player inventory into chest
                if (!this.moveItemStackTo(slotStack, 0, CHEST_SLOTS, false)) {
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
