package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.minecraft.core.Direction;
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
import team.reborn.energy.api.EnergyStorage;

public class VolcaniteBatteryMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 16;

    private final ContainerData data;
    private final Container container;

    public VolcaniteBatteryMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainerData(DATA_COUNT), new SimpleContainer(2));
    }

    public VolcaniteBatteryMenu(int syncId, Inventory playerInventory, ContainerData data, Container container) {
        super(ObsidiumMod.VOLCANITE_BATTERY_MENU, syncId);
        checkContainerDataCount(data, DATA_COUNT);
        checkContainerSize(container, 1);
        this.data = data;
        this.container = container;

        // Slot 0: Item Charge / Discharge Slot (Right Panel: 166, 57)
        this.addSlot(new Slot(container, 0, 166, 57) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !stack.isEmpty() && EnergyStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack)) != null;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // Slot 1: Upgrade Slot (Right Panel: 188, 57) - only active on standalone Energy Cells
        Container upgradeHost = container.getContainerSize() >= 2 ? container : new SimpleContainer(2);
        this.addSlot(new Slot(upgradeHost, 1, 188, 57) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isActive() && VolcaniteEnergyCellBlockEntity.isUpgradeItem(stack);
            }

            @Override
            public boolean isActive() {
                return getCoreStatus() == 0;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // Player Inventory (Centered in 220px wide UI: startX = 30, startY = 126)
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 30 + j * 18, 126 + i * 18));
            }
        }
        // Hotbar (startY = 184)
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 30 + i * 18, 184));
        }
        this.addDataSlots(data);
    }

    public long getEnergyAmount() {
        long p2 = this.data.get(0) & 0xFFFFL;
        long p1 = this.data.get(1) & 0xFFFFL;
        long p0 = this.data.get(2) & 0xFFFFL;
        return (p2 << 32) | (p1 << 16) | p0;
    }

    public long getMaxEnergy() {
        long p2 = this.data.get(3) & 0xFFFFL;
        long p1 = this.data.get(4) & 0xFFFFL;
        long p0 = this.data.get(5) & 0xFFFFL;
        return (p2 << 32) | (p1 << 16) | p0;
    }

    /** 0 = Standalone Cell, 1 = Unformed Energy Core, 2 = Formed 3x3 Energy Core */
    public int getCoreStatus() {
        return this.data.get(6) & 0xFFFF;
    }

    public int getBasicCellsCount() {
        return this.data.get(7) & 0xFF;
    }

    public int getAdvancedCellsCount() {
        return (this.data.get(7) >> 8) & 0xFF;
    }

    public boolean isAdvancedUnit() {
        return getCoreStatus() == 0 && (this.data.get(7) & 1) == 1;
    }

    public int getUpgradeTier() {
        if (getCoreStatus() != 0) return 0;
        return (this.data.get(7) >> 1) & 0x7;
    }

    /** 0 = Charge Item, 1 = Discharge Item into battery */
    public int getTransferMode() {
        return this.data.get(8);
    }

    public int getFaceMode(Direction dir) {
        if (dir == null) return VolcaniteEnergyCellBlockEntity.FACE_BOTH;
        int packed = this.data.get(9);
        return (packed >> (dir.get3DDataValue() * 2)) & 0x3;
    }

    public int getRedstoneMode() {
        return this.data.get(10);
    }

    public boolean isFieldChargeEnabled() {
        return this.data.get(11) != 0;
    }

    public long getAvgInPerTick() {
        long hi = this.data.get(12) & 0xFFFFL;
        long lo = this.data.get(13) & 0xFFFFL;
        return (hi << 16) | lo;
    }

    public long getAvgOutPerTick() {
        long hi = this.data.get(14) & 0xFFFFL;
        long lo = this.data.get(15) & 0xFFFFL;
        return (hi << 16) | lo;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            int current = this.data.get(8);
            this.data.set(8, current == 0 ? 1 : 0);
            broadcastChanges();
            return true;
        } else if (id == 1 && getCoreStatus() == 0) {
            int next = (this.data.get(10) + 1) % 3;
            this.data.set(10, next);
            broadcastChanges();
            return true;
        } else if (id == 2 && getCoreStatus() == 0) {
            this.data.set(11, this.data.get(11) == 0 ? 1 : 0);
            broadcastChanges();
            return true;
        } else if (id >= 10 && id < 16 && getCoreStatus() == 0) {
            int faceIdx = id - 10;
            int packed = this.data.get(9);
            int cur = (packed >> (faceIdx * 2)) & 0x3;
            int next = (cur + 1) % 4;
            int updated = (packed & ~(0x3 << (faceIdx * 2))) | (next << (faceIdx * 2));
            this.data.set(9, updated);
            broadcastChanges();
            return true;
        } else if (id == 16 && getCoreStatus() == 0) {
            int allBoth = 0;
            for (int i = 0; i < 6; i++) {
                allBoth |= (VolcaniteEnergyCellBlockEntity.FACE_BOTH << (i * 2));
            }
            this.data.set(9, allBoth);
            broadcastChanges();
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();
            if (index == 0 || index == 1) {
                if (!this.moveItemStackTo(itemStack2, 2, 38, true)) return ItemStack.EMPTY;
            } else {
                if (getCoreStatus() == 0 && VolcaniteEnergyCellBlockEntity.isUpgradeItem(itemStack2)) {
                    if (!this.moveItemStackTo(itemStack2, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemStack2, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (itemStack2.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            if (itemStack2.getCount() == itemStack.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, itemStack2);
        }
        return itemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}
