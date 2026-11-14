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
import net.minecraft.world.item.SwordItem;
import net.valentin.obsidium.ObsidiumMod;

public class ThanatosExecutionerMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    private final ThanatosExecutionerBlockEntity blockEntity;

    public ThanatosExecutionerMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(10), new SimpleContainerData(6), null);
    }

    public ThanatosExecutionerMenu(int syncId, Inventory playerInventory, ThanatosExecutionerBlockEntity blockEntity, ContainerData data) {
        this(syncId, playerInventory, blockEntity.getInventory(), data, blockEntity);
    }

    public ThanatosExecutionerMenu(int syncId, Inventory playerInventory, Container container, ContainerData data, ThanatosExecutionerBlockEntity blockEntity) {
        super(ObsidiumMod.THANATOS_EXECUTIONER_MENU, syncId);
        this.container = container;
        this.data = data;
        this.blockEntity = blockEntity;
        checkContainerSize(container, 10);
        checkContainerDataCount(data, 6);

        // Slot 9: Weapon / Sword Upgrade Slot (at x = 20, y = 35)
        this.addSlot(new Slot(container, ThanatosExecutionerBlockEntity.WEAPON_SLOT, 20, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof SwordItem;
            }
        });

        // Slots 0..8: 3x3 Loot Buffer Grid (at x = 70 + col*18, y = 17 + row*18)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = col + row * 3;
                this.addSlot(new Slot(container, index, 70 + col * 18, 17 + row * 18));
            }
        }

        // Player Inventory (3 rows x 9 columns at x = 8, y = 84)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar (9 slots at x = 8, y = 142)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(data);
    }

    public int getEnergy() {
        return (this.data.get(0) & 0xFFFF) | ((this.data.get(1) & 0xFFFF) << 16);
    }

    public int getStoredXp() {
        return this.data.get(2);
    }

    public int getTargetMode() {
        return this.data.get(3);
    }

    public int getKillCount() {
        return this.data.get(4);
    }

    public int getSpawnerEnabled() {
        return this.data.get(5);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (blockEntity == null) return false;
        if (id == 0) {
            blockEntity.cycleTargetMode();
            return true;
        } else if (id == 1) {
            blockEntity.extractXp(player);
            return true;
        } else if (id == 2) {
            blockEntity.cycleSpawner();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            itemstack = current.copy();

            // Machine slots (0..9) -> move to player inventory (10..45)
            if (invSlot < 10) {
                if (!this.moveItemStackTo(current, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Player inventory -> move to weapon slot if sword, otherwise try output buffer
                if (current.getItem() instanceof SwordItem) {
                    if (!this.moveItemStackTo(current, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(current, 1, 10, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (current.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (current.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, current);
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}

