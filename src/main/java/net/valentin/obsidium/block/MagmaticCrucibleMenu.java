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
import net.minecraft.world.item.Items;
import net.valentin.obsidium.ObsidiumMod;

public class MagmaticCrucibleMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public MagmaticCrucibleMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(MagmaticCrucibleBlockEntity.INVENTORY_SIZE), new SimpleContainerData(4));
    }

    public MagmaticCrucibleMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(ObsidiumMod.MAGMATIC_CRUCIBLE_MENU, syncId);
        checkContainerSize(container, MagmaticCrucibleBlockEntity.INVENTORY_SIZE);
        checkContainerDataCount(data, 4);
        this.container = container;
        this.data = data;

        // Slot 0: Rock Input (44, 35)
        this.addSlot(new Slot(container, 0, 44, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return MagmaticCrucibleBlockEntity.isMeltableRock(stack);
            }
        });

        // Slot 1: Empty Bucket In (96, 52)
        this.addSlot(new Slot(container, 1, 96, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.BUCKET);
            }
        });

        // Slot 2: Lava Bucket Out (122, 52)
        this.addSlot(new Slot(container, 2, 122, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Slot 3: Magma Droplets Out (96, 22)
        this.addSlot(new Slot(container, 3, 96, 22) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Player Inventory (8, 84)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar (8, 142)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(data);
    }

    public int getLavaAmount() {
        return this.data.get(0);
    }

    public int getProgress() {
        return this.data.get(1);
    }

    public int getMaxProgress() {
        return this.data.get(2);
    }

    public int getHeatMultiplier() {
        return this.data.get(3);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (invSlot < MagmaticCrucibleBlockEntity.INVENTORY_SIZE) {
                // Moving from machine to player
                if (!this.moveItemStackTo(originalStack, MagmaticCrucibleBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player to machine
                if (MagmaticCrucibleBlockEntity.isMeltableRock(originalStack)) {
                    if (!this.moveItemStackTo(originalStack, 0, 1, false)) return ItemStack.EMPTY;
                } else if (originalStack.is(Items.BUCKET)) {
                    if (!this.moveItemStackTo(originalStack, 1, 2, false)) return ItemStack.EMPTY;
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return newStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}

