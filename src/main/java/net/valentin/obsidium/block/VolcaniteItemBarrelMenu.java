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
import net.valentin.obsidium.item.VolcaniteBarrelUpgradeItem;

public class VolcaniteItemBarrelMenu extends AbstractContainerMenu {
    private final VolcaniteItemBarrelBlockEntity blockEntity;
    private final Container upgrades;
    private final ContainerData data;

    public VolcaniteItemBarrelMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, null, new SimpleContainer(4), new SimpleContainerData(7));
    }

    public VolcaniteItemBarrelMenu(int syncId, Inventory playerInventory, VolcaniteItemBarrelBlockEntity blockEntity, Container upgrades, ContainerData data) {
        super(ObsidiumMod.VOLCANITE_ITEM_BARREL_MENU, syncId);
        this.blockEntity = blockEntity;
        this.upgrades = upgrades;
        this.data = data;
        checkContainerSize(upgrades, 4);
        checkContainerDataCount(data, 7);

        // 4 Upgrade Slots (Vertical stack on right side: x = 150, y = 18, 36, 54, 72)
        for (int i = 0; i < 4; i++) {
            this.addSlot(new Slot(upgrades, i, 150, 18 + i * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof VolcaniteBarrelUpgradeItem;
                }
            });
        }

        // Player Inventory (8, 98)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 98 + row * 18));
            }
        }

        // Hotbar (8, 156)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 156));
        }

        this.addDataSlots(data);
    }

    public VolcaniteItemBarrelBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public long getStoredAmount() {
        int low = data.get(0);
        int high = data.get(1);
        return ((long) high << 16) | (low & 0xFFFFL);
    }

    public long getMaxCapacity() {
        int low = data.get(2);
        int high = data.get(3);
        return ((long) high << 16) | (low & 0xFFFFL);
    }

    public boolean isLocked() {
        return data.get(4) == 1;
    }

    public boolean hasVoidUpgrade() {
        return data.get(5) == 1;
    }

    public int getTierOrdinal() {
        return data.get(6);
    }

    public ItemStack getStoredItemSample() {
        if (blockEntity != null) {
            return blockEntity.getStoredItemSample();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && blockEntity != null) {
            blockEntity.toggleLock(player);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (invSlot < 4) { // Upgrade slot to player inventory
                if (!this.moveItemStackTo(originalStack, 4, 40, true)) {
                    return ItemStack.EMPTY;
                }
            } else { // Player inventory to upgrades or barrel
                if (originalStack.getItem() instanceof VolcaniteBarrelUpgradeItem) {
                    if (!this.moveItemStackTo(originalStack, 0, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (blockEntity != null) {
                    boolean deposited = blockEntity.depositStack(originalStack, false, player);
                    if (!deposited) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
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
        return upgrades.stillValid(player);
    }
}

