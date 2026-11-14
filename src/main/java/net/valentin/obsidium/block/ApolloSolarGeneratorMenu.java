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

public class ApolloSolarGeneratorMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public ApolloSolarGeneratorMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(ApolloSolarGeneratorBlockEntity.INVENTORY_SIZE), new SimpleContainerData(6));
    }

    public ApolloSolarGeneratorMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(ObsidiumMod.APOLLO_SOLAR_GENERATOR_MENU, syncId);
        this.container = container;
        this.data = data;
        checkContainerSize(container, ApolloSolarGeneratorBlockEntity.INVENTORY_SIZE);
        checkContainerDataCount(data, 6);
        container.startOpen(playerInventory.player);

        // Slot 0: Solar Input (x = 56, y = 35)
        this.addSlot(new Slot(container, 0, 56, 35));

        // Slot 1: Smelted Output (x = 116, y = 35)
        this.addSlot(new Slot(container, 1, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Player Inventory (3 rows x 9 columns)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar (9 slots)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(data);
    }

    public int getEnergy() {
        return this.data.get(0);
    }

    public int getMaxEnergy() {
        return this.data.get(1);
    }

    public int getSunIrradiance() {
        return this.data.get(2);
    }

    public int getGenerationRate() {
        return this.data.get(3);
    }

    public int getSmeltProgress() {
        return this.data.get(4);
    }

    public int getSmeltDuration() {
        return this.data.get(5);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (invSlot == 1) { // Take output
                if (!this.moveItemStackTo(originalStack, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(originalStack, newStack);
            } else if (invSlot == 0) { // Take input
                if (!this.moveItemStackTo(originalStack, 2, 38, false)) {
                    return ItemStack.EMPTY;
                }
            } else { // From player inventory
                if (!this.moveItemStackTo(originalStack, 0, 1, false)) {
                    if (invSlot < 29) {
                        if (!this.moveItemStackTo(originalStack, 29, 38, false)) return ItemStack.EMPTY;
                    } else if (!this.moveItemStackTo(originalStack, 2, 29, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (originalStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (originalStack.getCount() == newStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, originalStack);
        }
        return newStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}

