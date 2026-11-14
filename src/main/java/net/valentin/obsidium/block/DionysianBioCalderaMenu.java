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

public class DionysianBioCalderaMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public DionysianBioCalderaMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(DionysianBioCalderaBlockEntity.INVENTORY_SIZE), new SimpleContainerData(9));
    }

    public DionysianBioCalderaMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(ObsidiumMod.DIONYSIAN_BIO_CALDERA_MENU, syncId);
        checkContainerSize(container, DionysianBioCalderaBlockEntity.INVENTORY_SIZE);
        checkContainerDataCount(data, 9);
        this.container = container;
        this.data = data;

        container.startOpen(playerInventory.player);

        // Slot 0: Fruit / Biomass input
        this.addSlot(new Slot(container, 0, 44, 21) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(0, stack);
            }
        });
        // Slot 1: Fertilizer byproduct output
        this.addSlot(new Slot(container, 1, 44, 49) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        // Slot 2: Battery / Tool charge slot
        this.addSlot(new Slot(container, 2, 150, 49) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(2, stack);
            }
        });

        // Player Inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(data);
    }

    public boolean isFermenting() {
        return this.data.get(0) > 0;
    }

    public long getEnergy() {
        return ((long) this.data.get(3) << 16) | (this.data.get(4) & 0xFFFFL);
    }

    public long getMaxEnergy() {
        return ((long) this.data.get(5) << 16) | (this.data.get(6) & 0xFFFFL);
    }

    public int getEnergyScaled(int pixels) {
        long cap = getMaxEnergy();
        if (cap <= 0) cap = DionysianBioCalderaBlockEntity.MAX_ENERGY;
        return (int) (getEnergy() * pixels / cap);
    }

    public int getBiogasAmount() {
        return this.data.get(7);
    }

    public int getMaxBiogas() {
        return this.data.get(8);
    }

    public int getBiogasScaled(int pixels) {
        int max = getMaxBiogas();
        if (max <= 0) max = DionysianBioCalderaBlockEntity.MAX_BIOGAS;
        return getBiogasAmount() * pixels / max;
    }

    public int getCurrentRfPerTick() {
        return this.data.get(2);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (invSlot < DionysianBioCalderaBlockEntity.INVENTORY_SIZE) {
                if (!this.moveItemStackTo(originalStack, DionysianBioCalderaBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(originalStack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
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

