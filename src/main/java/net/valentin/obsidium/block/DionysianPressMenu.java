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

public class DionysianPressMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    public DionysianPressMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(DionysianPressBlockEntity.INVENTORY_SIZE), new SimpleContainerData(8));
    }

    public DionysianPressMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(ObsidiumMod.DIONYSIAN_PRESS_MENU, syncId);
        this.container = container;
        this.data = data;
        checkContainerSize(container, DionysianPressBlockEntity.INVENTORY_SIZE);
        checkContainerDataCount(data, 8);
        container.startOpen(playerInventory.player);

        // Slot 0: Fruit Input (x = 50, y = 24)
        this.addSlot(new Slot(container, 0, 50, 24) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return DionysianPressBlockEntity.isFruit(stack);
            }
        });

        // Slot 1: Mineral Output (Nuggets) (x = 112, y = 24)
        this.addSlot(new Slot(container, 1, 112, 24) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Slot 2: Pomace Output (x = 136, y = 24)
        this.addSlot(new Slot(container, 2, 136, 24) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Slot 3: Optional Fuel Input (x = 50, y = 54)
        this.addSlot(new Slot(container, 3, 50, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return DionysianPressBlockEntity.isFuel(stack);
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

    public int getProgress() {
        return this.data.get(0);
    }

    public int getMaxProgress() {
        return this.data.get(1);
    }

    public int getBurnTime() {
        return this.data.get(2);
    }

    public int getTotalBurnTime() {
        return this.data.get(3);
    }

    public long getEnergy() {
        return ((long) this.data.get(4) << 16) | (this.data.get(5) & 0xFFFFL);
    }

    public long getMaxEnergy() {
        return ((long) this.data.get(6) << 16) | (this.data.get(7) & 0xFFFFL);
    }

    public boolean isLit() {
        return getBurnTime() > 0 || getEnergy() >= DionysianPressBlockEntity.RF_PER_TICK;
    }

    public int getProgressScaled(int pixels) {
        int progress = getProgress();
        int maxProgress = getMaxProgress();
        return (maxProgress != 0 && progress != 0) ? (progress * pixels / maxProgress) : 0;
    }

    public int getEnergyScaled(int pixels) {
        long energy = getEnergy();
        long maxEnergy = getMaxEnergy();
        return (maxEnergy > 0) ? (int) (energy * pixels / maxEnergy) : 0;
    }

    public int getBurnTimeScaled(int pixels) {
        int total = getTotalBurnTime();
        if (total == 0) total = 200;
        return (getBurnTime() * pixels / total);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemStack = stackInSlot.copy();

            // Machine output slots (1, 2) -> player inventory
            if (index == 1 || index == 2) {
                if (!this.moveItemStackTo(stackInSlot, 4, 40, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, itemStack);
            }
            // Machine input slots (0, 3) -> player inventory
            else if (index < 4) {
                if (!this.moveItemStackTo(stackInSlot, 4, 40, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // Player inventory -> machine slots
            else {
                if (DionysianPressBlockEntity.isFruit(stackInSlot)) {
                    if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (DionysianPressBlockEntity.isFuel(stackInSlot)) {
                    if (!this.moveItemStackTo(stackInSlot, 3, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < 31) {
                    if (!this.moveItemStackTo(stackInSlot, 31, 40, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 31 && !this.moveItemStackTo(stackInSlot, 4, 31, false)) {
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

