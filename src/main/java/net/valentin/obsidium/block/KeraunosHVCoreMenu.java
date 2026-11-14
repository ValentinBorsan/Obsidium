package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
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
import org.jetbrains.annotations.Nullable;

public class KeraunosHVCoreMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    @Nullable
    private final KeraunosHVCoreBlockEntity blockEntity;

    public KeraunosHVCoreMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(KeraunosHVCoreBlockEntity.INVENTORY_SIZE), new SimpleContainerData(KeraunosHVCoreBlockEntity.DATA_COUNT), null);
    }

    public KeraunosHVCoreMenu(int syncId, Inventory playerInventory, Container container, ContainerData data, @Nullable BlockPos pos) {
        super(ObsidiumMod.KERAUNOS_HV_CORE_MENU, syncId);
        this.container = container;
        this.data = data;
        this.blockEntity = (pos != null && playerInventory.player.level().getBlockEntity(pos) instanceof KeraunosHVCoreBlockEntity be) ? be : null;
        checkContainerSize(container, KeraunosHVCoreBlockEntity.INVENTORY_SIZE);
        checkContainerDataCount(data, KeraunosHVCoreBlockEntity.DATA_COUNT);
        container.startOpen(playerInventory.player);

        // Slot 0: High-Voltage Supercharge / Discharge Slot (x = 128, y = 45)
        this.addSlot(new Slot(container, 0, 128, 45));

        // Player Inventory (3 rows x 9 columns)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 108 + row * 18));
            }
        }

        // Player Hotbar (9 slots)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 166));
        }

        this.addDataSlots(data);
    }

    public long getEnergy() {
        long high = this.data.get(0) & 0xFFFFL;
        long mid = this.data.get(1) & 0xFFFFL;
        long low = this.data.get(2) & 0xFFFFL;
        return (high << 32) | (mid << 16) | low;
    }

    public long getMaxEnergy() {
        long high = this.data.get(3) & 0xFFFFL;
        long mid = this.data.get(4) & 0xFFFFL;
        long low = this.data.get(5) & 0xFFFFL;
        return Math.max(1L, (high << 32) | (mid << 16) | low);
    }

    public int getVoltageKv() {
        return this.data.get(6) & 0xFFFF;
    }

    public int getPortsCount() {
        return this.data.get(7) & 0xFFFF;
    }

    public int getBasicCellsCount() {
        return this.data.get(8) & 0xFF;
    }

    public int getAdvancedUnitsCount() {
        return (this.data.get(8) >> 8) & 0xFF;
    }

    public boolean isFormed() {
        return (this.data.get(9) & 1) != 0;
    }

    public boolean isServiceMode() {
        return (this.data.get(9) & 2) != 0;
    }

    public boolean isOvervoltageMode() {
        return (this.data.get(9) & 4) != 0;
    }

    /** 0 = Charge Item, 1 = Discharge Item into HV Core */
    public int getTransferMode() {
        return (this.data.get(9) & 8) != 0 ? 1 : 0;
    }

    public int getLastThroughputRft() {
        int high = this.data.get(10) & 0xFFFF;
        int low = this.data.get(11) & 0xFFFF;
        return (high << 16) | low;
    }

    public int getInvalidBlocksCount() {
        return this.data.get(12) & 0xFFFF;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.blockEntity != null) {
            if (id == 0) {
                this.blockEntity.toggleServiceMode(player);
                return true;
            } else if (id == 1) {
                this.blockEntity.toggleOvervoltageMode();
                return true;
            } else if (id == 2) {
                this.blockEntity.toggleTransferMode();
                return true;
            }
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();
            if (invSlot == 0) {
                if (!this.moveItemStackTo(originalStack, 1, 37, true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(originalStack, 0, 1, false)) {
                if (invSlot < 28) {
                    if (!this.moveItemStackTo(originalStack, 28, 37, false)) return ItemStack.EMPTY;
                } else if (!this.moveItemStackTo(originalStack, 1, 28, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (originalStack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return newStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }
}
