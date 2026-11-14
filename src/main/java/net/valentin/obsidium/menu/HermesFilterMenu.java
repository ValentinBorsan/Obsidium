package net.valentin.obsidium.menu;

import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.HermesFilterUpgradeItem;

public class HermesFilterMenu extends AbstractContainerMenu {
    private final ItemStack filterStack;
    private final InteractionHand hand;
    private final SimpleContainer filterContainer;
    private boolean isBlacklist;

    public HermesFilterMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new ItemStack(ObsidiumMod.HERMES_FILTER_UPGRADE), InteractionHand.MAIN_HAND);
    }

    public HermesFilterMenu(int syncId, Inventory playerInventory, ItemStack filterStack, InteractionHand hand) {
        super(ObsidiumMod.HERMES_FILTER_MENU, syncId);
        this.filterStack = filterStack;
        this.hand = hand;
        this.isBlacklist = HermesFilterUpgradeItem.isBlacklist(filterStack);

        NonNullList<ItemStack> saved = HermesFilterUpgradeItem.getFilterItems(filterStack, playerInventory.player.level().registryAccess());
        this.filterContainer = new SimpleContainer(HermesFilterUpgradeItem.FILTER_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                saveToItem(playerInventory.player);
            }
        };

        for (int i = 0; i < saved.size(); i++) {
            this.filterContainer.setItem(i, saved.get(i).copy());
        }

        // 9 Filter Ghost Slots (3x3 grid at x=62, y=20)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = col + row * 3;
                this.addSlot(new Slot(filterContainer, index, 62 + col * 18, 20 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return true;
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return false;
                    }
                });
            }
        }

        // Player Inventory (3x9)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 86 + row * 18));
            }
        }

        // Hotbar (1x9)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 144));
        }
    }

    private void saveToItem(Player player) {
        NonNullList<ItemStack> list = NonNullList.withSize(HermesFilterUpgradeItem.FILTER_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < HermesFilterUpgradeItem.FILTER_SLOTS; i++) {
            list.set(i, filterContainer.getItem(i));
        }
        HermesFilterUpgradeItem.saveFilterItems(filterStack, list, player.level().registryAccess());
        HermesFilterUpgradeItem.setBlacklist(filterStack, isBlacklist);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            // Toggle Whitelist / Blacklist
            this.isBlacklist = !this.isBlacklist;
            saveToItem(player);
            return true;
        } else if (id == 1) {
            // Clear all 9 slots
            for (int i = 0; i < filterContainer.getContainerSize(); i++) {
                filterContainer.setItem(i, ItemStack.EMPTY);
            }
            saveToItem(player);
            return true;
        }
        return false;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Ghost slot logic for filter grid (slots 0..8)
        if (slotId >= 0 && slotId < HermesFilterUpgradeItem.FILTER_SLOTS) {
            ItemStack cursorStack = this.getCarried();
            if (cursorStack.isEmpty()) {
                filterContainer.setItem(slotId, ItemStack.EMPTY);
            } else {
                ItemStack ghost = cursorStack.copyWithCount(1);
                filterContainer.setItem(slotId, ghost);
            }
            saveToItem(player);
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // If clicking from player inventory, find first empty ghost slot
        if (index >= HermesFilterUpgradeItem.FILTER_SLOTS) {
            Slot slot = this.slots.get(index);
            if (slot != null && slot.hasItem()) {
                ItemStack stack = slot.getItem();
                for (int i = 0; i < HermesFilterUpgradeItem.FILTER_SLOTS; i++) {
                    if (filterContainer.getItem(i).isEmpty()) {
                        filterContainer.setItem(i, stack.copyWithCount(1));
                        saveToItem(player);
                        break;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public boolean isBlacklist() {
        return this.isBlacklist;
    }

    public void setBlacklist(boolean blacklist, Player player) {
        this.isBlacklist = blacklist;
        saveToItem(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return !player.isRemoved() && (player.getItemInHand(hand) == filterStack || ItemStack.isSameItemSameComponents(player.getItemInHand(hand), filterStack));
    }
}

