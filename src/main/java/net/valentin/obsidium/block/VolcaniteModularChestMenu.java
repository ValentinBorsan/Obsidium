package net.valentin.obsidium.block;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

public class VolcaniteModularChestMenu extends AbstractContainerMenu {
    public static final int BTN_SORT_QUANTITY_DESC = 0;
    public static final int BTN_SORT_QUANTITY_ASC = 1;
    public static final int BTN_SORT_ALPHA = 2;
    public static final int BTN_QUICK_STACK = 3;
    public static final int BTN_PREV_PAGE = 4;
    public static final int BTN_NEXT_PAGE = 5;

    public static final int MENU_STORAGE_SLOTS = VolcaniteModularChestBlockEntity.MAX_STORAGE_SLOTS; // 216 (4 pages x 54)
    public static final int MENU_MOD_SLOT_START = VolcaniteModularChestBlockEntity.SLOT_MOD_0; // 216
    public static final int MENU_MOD_SLOT_END = VolcaniteModularChestBlockEntity.TOTAL_SLOTS; // 220 (216, 217, 218, 219)

    private final Container container;
    private final ContainerData data;

    public VolcaniteModularChestMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(VolcaniteModularChestBlockEntity.TOTAL_SLOTS), new SimpleContainerData(3));
    }

    public VolcaniteModularChestMenu(int syncId, Inventory playerInventory, Container container) {
        this(syncId, playerInventory, container,
                container instanceof VolcaniteModularChestBlockEntity chest ? chest.data : new SimpleContainerData(3));
    }

    public VolcaniteModularChestMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(ObsidiumMod.VOLCANITE_MODULAR_CHEST_MENU, syncId);
        this.container = container;
        this.data = data;
        checkContainerSize(container, VolcaniteModularChestBlockEntity.TOTAL_SLOTS);
        container.startOpen(playerInventory.player);
        this.addDataSlots(data);

        // 216 Fixed-Index Page-Aware Storage Slots (4 pages x 54 slots -> menu slots 0..215)
        for (int page = 0; page < VolcaniteModularChestBlockEntity.MAX_PAGES; page++) {
            for (int row = 0; row < 6; ++row) {
                for (int col = 0; col < 9; ++col) {
                    int containerSlot = page * VolcaniteModularChestBlockEntity.SLOTS_PER_PAGE + (col + row * 9);
                    this.addSlot(new PagedStorageSlot(container, page, containerSlot, 8 + col * 18, 18 + row * 18));
                }
            }
        }

        // 4 Universal Flex Module Bays (menu slots 54..57 -> container slots 216..219)
        for (int m = 0; m < 4; m++) {
            final int containerModSlot = VolcaniteModularChestBlockEntity.SLOT_MOD_0 + m;
            this.addSlot(new Slot(container, containerModSlot, 178, 20 + m * 24) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return VolcaniteModularChestBlockEntity.isValidModule(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    if (container instanceof VolcaniteModularChestBlockEntity chest) {
                        // Clamp current page if a capacity upgrade was removed
                        chest.getCurrentPage();
                    }
                    broadcastChanges();
                }
            });
        }

        // Player Inventory (3 rows of 9 -> menu slots 58..84)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }

        // Player Hotbar (menu slots 85..93)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }

    public int getCurrentPage() {
        int unlocked = Math.max(1, getUnlockedPages());
        return Math.max(0, Math.min(this.data.get(0), unlocked - 1));
    }

    public void setCurrentPageClient(int page) {
        int unlocked = Math.max(1, getUnlockedPages());
        this.data.set(0, Math.max(0, Math.min(page, unlocked - 1)));
    }

    public int getUnlockedPages() {
        int val = this.data.get(1);
        return val > 0 ? val : 1;
    }

    public boolean isQuantitySortDescending() {
        return this.data.get(2) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.container instanceof VolcaniteModularChestBlockEntity chest) {
            switch (id) {
                case BTN_SORT_QUANTITY_DESC -> {
                    chest.sortByQuantity(true);
                    playClickSound(player, 1.2F);
                    broadcastChanges();
                    return true;
                }
                case BTN_SORT_QUANTITY_ASC -> {
                    chest.sortByQuantity(false);
                    playClickSound(player, 1.3F);
                    broadcastChanges();
                    return true;
                }
                case BTN_SORT_ALPHA -> {
                    chest.sortAlphabetically();
                    playClickSound(player, 1.4F);
                    broadcastChanges();
                    return true;
                }
                case BTN_QUICK_STACK -> {
                    chest.quickStackFromPlayer(player);
                    playClickSound(player, 1.0F);
                    broadcastChanges();
                    return true;
                }
                case BTN_PREV_PAGE -> {
                    int cur = chest.getCurrentPage();
                    if (cur > 0) {
                        chest.setCurrentPage(cur - 1);
                        playClickSound(player, 0.9F);
                        broadcastChanges();
                        return true;
                    }
                }
                case BTN_NEXT_PAGE -> {
                    int cur = chest.getCurrentPage();
                    if (cur + 1 < chest.getUnlockedPages()) {
                        chest.setCurrentPage(cur + 1);
                        playClickSound(player, 1.1F);
                        broadcastChanges();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void playClickSound(Player player, float pitch) {
        if (player != null && player.level() != null) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.4F, pitch);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemStack = stackInSlot.copy();

            if (index < MENU_MOD_SLOT_END) {
                // Moving from chest (storage or module slots) to player inventory
                if (!this.moveItemStackTo(stackInSlot, MENU_MOD_SLOT_END, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to chest
                int unlockedSlots = Math.min(MENU_STORAGE_SLOTS,
                        getUnlockedPages() * VolcaniteModularChestBlockEntity.SLOTS_PER_PAGE);
                int curPageStart = getCurrentPage() * VolcaniteModularChestBlockEntity.SLOTS_PER_PAGE;
                int curPageEnd = Math.min(unlockedSlots, curPageStart + VolcaniteModularChestBlockEntity.SLOTS_PER_PAGE);
                if (VolcaniteModularChestBlockEntity.isValidModule(stackInSlot)) {
                    if (!this.moveItemStackTo(stackInSlot, MENU_MOD_SLOT_START, MENU_MOD_SLOT_END, false)) {
                        if (!this.moveItemStackTo(stackInSlot, curPageStart, curPageEnd, false)
                                && !this.moveItemStackTo(stackInSlot, 0, unlockedSlots, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else {
                    if (!this.moveItemStackTo(stackInSlot, curPageStart, curPageEnd, false)) {
                        // If current page is full, try inserting into remaining unlocked pages
                        if (!this.moveItemStackTo(stackInSlot, 0, unlockedSlots, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    /**
     * Slot tied to a fixed container index (0..215) whose visibility & interaction
     * are active only when its page is selected and unlocked.
     */
    private class PagedStorageSlot extends Slot {
        private final int pageIndex;

        public PagedStorageSlot(Container container, int pageIndex, int containerSlot, int x, int y) {
            super(container, containerSlot, x, y);
            this.pageIndex = pageIndex;
        }

        @Override
        public boolean isActive() {
            return this.pageIndex == getCurrentPage() && this.pageIndex < getUnlockedPages();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (this.pageIndex >= getUnlockedPages()) return false;
            if (this.container instanceof VolcaniteModularChestBlockEntity chest) {
                return chest.matchesChestFilters(stack);
            }
            return true;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return Math.min(this.getMaxStackSize(), stack.getMaxStackSize());
        }
    }
}
