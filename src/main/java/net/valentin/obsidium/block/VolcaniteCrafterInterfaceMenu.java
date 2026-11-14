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
import net.valentin.obsidium.item.VolcaniteBlueprintItem;
import net.valentin.obsidium.item.VolcaniteOverclockUpgradeItem;
import net.valentin.obsidium.item.VolcanitePipeEnergyUpgradeItem;
import net.valentin.obsidium.item.VolcaniteStackUpgradeItem;

public class VolcaniteCrafterInterfaceMenu extends AbstractContainerMenu {
    private final Container container;
    private final VolcaniteCrafterInterfaceBlockEntity crafterBe;
    private final ContainerData data;

    public VolcaniteCrafterInterfaceMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(VolcaniteCrafterInterfaceBlockEntity.CONTAINER_SIZE), new SimpleContainerData(6));
    }

    public VolcaniteCrafterInterfaceMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(ObsidiumMod.VOLCANITE_CRAFTER_INTERFACE_MENU, syncId);
        this.container = container;
        this.crafterBe = (container instanceof VolcaniteCrafterInterfaceBlockEntity be) ? be : null;
        this.data = data;
        checkContainerSize(container, VolcaniteCrafterInterfaceBlockEntity.CONTAINER_SIZE);
        checkContainerDataCount(data, 6);

        // Slots 0..8: 3x3 Interactive Crafting Grid at (13, 38)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int idx = col + row * 3;
                this.addSlot(new Slot(container, idx, 13 + col * 18, 38 + row * 18) {
                    @Override
                    public void setChanged() {
                        super.setChanged();
                        if (crafterBe != null) {
                            crafterBe.updateRecipeOutput();
                        }
                    }
                });
            }
        }

        // Slot 9: Crafting Result Output Slot at (88, 47)
        this.addSlot(new Slot(container, 9, 88, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                if (crafterBe != null) {
                    crafterBe.onCraftFromGrid(player);
                }
                super.onTake(player, stack);
            }
        });

        // Slot 10: Blueprint Encoder / Loader Slot at (88, 74)
        this.addSlot(new Slot(container, 10, 88, 74) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof VolcaniteBlueprintItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // Slots 11..14: 4 Automated Network Blueprint Slots (2x2 grid on the right panel at x=126, y=38)
        for (int i = 0; i < 4; i++) {
            int col = i % 2;
            int row = i / 2;
            this.addSlot(new Slot(container, 11 + i, 126 + col * 20, 38 + row * 20) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof VolcaniteBlueprintItem;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        // Slot 15: Auto-Crafter Speed / Batch Upgrade Slot at (171, 48)
        this.addSlot(new Slot(container, 15, 171, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof VolcaniteStackUpgradeItem
                        || stack.getItem() instanceof VolcaniteOverclockUpgradeItem
                        || stack.getItem() instanceof VolcanitePipeEnergyUpgradeItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // Player Inventory (Slots 16..42) at (20, 136)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 20 + col * 18, 136 + row * 18));
            }
        }

        // Player Hotbar (Slots 43..51) at (20, 194)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 20 + col * 18, 194));
        }

        this.addDataSlots(data);
    }

    public VolcaniteCrafterInterfaceBlockEntity getCrafterBlockEntity() {
        return this.crafterBe;
    }

    public boolean isNetworkOnline() {
        return this.data.get(0) == 1;
    }

    public int getNetworkEnergyPercent() {
        return this.data.get(1);
    }

    public boolean isAutoCraftEnabled() {
        return this.data.get(2) == 1;
    }

    public int getStockMode() {
        return this.data.get(3);
    }

    public boolean isAutoRefillGrid() {
        return this.data.get(4) == 1;
    }

    public int getUpgradeTier() {
        return this.data.get(5);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (crafterBe == null) return false;
        switch (id) {
            case 0 -> {
                // Encode or Load Blueprint in Slot 10
                crafterBe.encodeOrLoadBlueprint(player);
                broadcastChanges();
                return true;
            }
            case 1 -> {
                // Clear 3x3 Grid to Storage Network
                crafterBe.clearGridToNetwork(player);
                broadcastChanges();
                return true;
            }
            case 2 -> {
                // Toggle Auto-Craft ON / OFF
                crafterBe.toggleAutoCraft();
                broadcastChanges();
                return true;
            }
            case 3 -> {
                // Cycle Stock Limit (64 -> 256 -> 1024 -> Infinite)
                crafterBe.cycleStockMode();
                broadcastChanges();
                return true;
            }
            case 4 -> {
                // Toggle Auto-Refill 3x3 Grid from Network
                crafterBe.toggleAutoRefillGrid();
                broadcastChanges();
                return true;
            }
            case 5 -> {
                // Switch directly to the connected Storage Controller Terminal GUI
                VolcaniteStorageControllerBlockEntity ctrl = crafterBe.findController();
                if (ctrl != null && ctrl.isPowered()) {
                    player.openMenu(ctrl);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack slotStack = slot.getItem();
        ItemStack copy = slotStack.copy();

        if (index == 9) {
            // Shift-click craft up to 64 times (auto-refilling from the Storage Network on each craft!)
            // IMPORTANT: always return EMPTY here. Vanilla's doClick() re-invokes quickMoveStack in a loop
            // while the returned stack is non-empty and the slot still holds the same item. Since the
            // output slot is refilled after each craft (and never changes on the client), returning
            // the copy caused an infinite loop -> game freeze / crash on Shift + Left Click.
            if (crafterBe == null || player.level().isClientSide()) {
                return ItemStack.EMPTY;
            }
            int maxCrafts = Math.max(1, copy.getMaxStackSize() / Math.max(1, copy.getCount()));
            for (int c = 0; c < maxCrafts; c++) {
                ItemStack currentResult = crafterBe.getItem(9);
                if (currentResult.isEmpty() || !ItemStack.isSameItemSameComponents(currentResult, copy)) {
                    break;
                }
                ItemStack toGive = currentResult.copy();
                if (!this.moveItemStackTo(toGive, 16, 52, true)) {
                    break; // inventory full
                }
                if (!toGive.isEmpty()) {
                    // Partially inserted: never lose the remainder
                    player.drop(toGive, false);
                }
                currentResult.onCraftedBy(player.level(), player, currentResult.getCount());
                crafterBe.onCraftFromGrid(player);
            }
            broadcastChanges();
            return ItemStack.EMPTY;
        } else if (index < 16) {
            // From Crafter slots (0..8, 10..15) -> Player Inventory (16..52)
            if (!this.moveItemStackTo(slotStack, 16, 52, false)) {
                return ItemStack.EMPTY;
            }
            if (index <= 8 && crafterBe != null) {
                crafterBe.updateRecipeOutput();
            }
        } else {
            // From Player Inventory (16..52) -> Blueprint slots, Upgrade slot, or 3x3 Grid
            if (slotStack.getItem() instanceof VolcaniteBlueprintItem) {
                if (VolcaniteBlueprintItem.hasRecipe(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, 11, 15, false) && !this.moveItemStackTo(slotStack, 10, 11, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.moveItemStackTo(slotStack, 10, 11, false) && !this.moveItemStackTo(slotStack, 11, 15, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (slotStack.getItem() instanceof VolcaniteStackUpgradeItem
                    || slotStack.getItem() instanceof VolcaniteOverclockUpgradeItem
                    || slotStack.getItem() instanceof VolcanitePipeEnergyUpgradeItem) {
                if (!this.moveItemStackTo(slotStack, 15, 16, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(slotStack, 0, 9, false)) {
                    return ItemStack.EMPTY;
                }
                if (crafterBe != null) {
                    crafterBe.updateRecipeOutput();
                }
            }
        }

        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}
