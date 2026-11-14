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

public class VolcaniteSortingStationMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    private final VolcaniteSortingStationBlockEntity blockEntity;

    public VolcaniteSortingStationMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, null, new SimpleContainerData(12));
    }

    public VolcaniteSortingStationMenu(int syncId, Inventory playerInventory, VolcaniteSortingStationBlockEntity blockEntity, ContainerData data) {
        super(ObsidiumMod.VOLCANITE_SORTING_STATION_MENU, syncId);
        this.blockEntity = blockEntity;
        this.container = (blockEntity != null) ? blockEntity.getInventory() : new SimpleContainer(24);
        this.data = data;

        checkContainerSize(this.container, 24);
        checkContainerDataCount(data, 12);

        // 6 Sorting Station Cards:
        // 0: DOWN, 1: UP, 2: NORTH, 3: SOUTH, 4: WEST, 5: EAST
        // Each card has 4 slots:
        // Slot 0 (cx + 3, cy + 14): Advanced Filter Module Slot
        // Slot 1 (cx + 22, cy + 14): Filter 1 (Base - always active)
        // Slot 2 (cx + 41, cy + 14): Filter 2 (Advanced - active if module installed)
        // Slot 3 (cx + 60, cy + 14): Filter 3 (Advanced - active if module installed)
        int[][] cardCoords = new int[][] {
            {90, 20}, // 0: DOWN
            {7, 20},  // 1: UP
            {7, 55},  // 2: NORTH
            {90, 55}, // 3: SOUTH
            {7, 90},  // 4: WEST
            {90, 90}  // 5: EAST
        };

        for (int i = 0; i < 6; i++) {
            final int faceIndex = i;
            int cx = cardCoords[i][0];
            int cy = cardCoords[i][1];

            // Slot 0: Module Socket
            this.addSlot(new Slot(this.container, i * 4 + 0, cx + 3, cy + 14) {
                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(ObsidiumMod.VOLCANITE_ADVANCED_FILTER_MODULE);
                }
            });

            // Slot 1: Filter 1 (Base)
            this.addSlot(new Slot(this.container, i * 4 + 1, cx + 22, cy + 14) {
                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return !stack.is(ObsidiumMod.VOLCANITE_ADVANCED_FILTER_MODULE);
                }
            });

            // Slot 2: Filter 2 (Advanced)
            this.addSlot(new Slot(this.container, i * 4 + 2, cx + 41, cy + 14) {
                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return hasAdvancedModule(faceIndex) && !stack.is(ObsidiumMod.VOLCANITE_ADVANCED_FILTER_MODULE);
                }
            });

            // Slot 3: Filter 3 (Advanced)
            this.addSlot(new Slot(this.container, i * 4 + 3, cx + 60, cy + 14) {
                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return hasAdvancedModule(faceIndex) && !stack.is(ObsidiumMod.VOLCANITE_ADVANCED_FILTER_MODULE);
                }
            });
        }

        // Player Inventory (3 x 9) at (8, 144)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 144 + row * 18));
            }
        }

        // Player Hotbar (9 slots) at (8, 204)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 204));
        }

        this.addDataSlots(data);
    }

    public boolean hasAdvancedModule(int faceIndex) {
        if (faceIndex < 0 || faceIndex >= 6) return false;
        if (this.slots.size() > faceIndex * 4) {
            Slot slot = this.slots.get(faceIndex * 4);
            return slot != null && slot.hasItem() && slot.getItem().is(ObsidiumMod.VOLCANITE_ADVANCED_FILTER_MODULE);
        }
        return false;
    }

    public int getFaceMode(int faceIndex) {
        if (faceIndex >= 0 && faceIndex < 6) {
            return this.data.get(faceIndex);
        }
        return 0;
    }

    public int getFaceConnection(int faceIndex) {
        if (faceIndex >= 0 && faceIndex < 6) {
            return this.data.get(6 + faceIndex);
        }
        return 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id < 6) {
            if (this.blockEntity != null) {
                this.blockEntity.cycleFaceMode(id);
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 24) {
                // Moving from station (slots 0..23) to player inventory (slots 24..59)
                if (!this.moveItemStackTo(itemstack1, 24, 60, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to station
                boolean moved = false;
                if (itemstack1.is(ObsidiumMod.VOLCANITE_ADVANCED_FILTER_MODULE)) {
                    // Try to place into an empty module socket (face 0..5, slot f * 4 + 0)
                    for (int f = 0; f < 6; f++) {
                        Slot modSlot = this.slots.get(f * 4 + 0);
                        if (!modSlot.hasItem() && modSlot.mayPlace(itemstack1)) {
                            ItemStack single = itemstack1.split(1);
                            modSlot.set(single);
                            moved = true;
                            break;
                        }
                    }
                } else {
                    // Regular filter item: first try Filter 1 slots (f * 4 + 1)
                    for (int f = 0; f < 6; f++) {
                        Slot filterSlot = this.slots.get(f * 4 + 1);
                        if (!filterSlot.hasItem() && filterSlot.mayPlace(itemstack1)) {
                            ItemStack single = itemstack1.split(1);
                            filterSlot.set(single);
                            moved = true;
                            break;
                        }
                    }
                    // Next, if face has advanced module, try Filter 2 and Filter 3 slots
                    if (!moved) {
                        for (int f = 0; f < 6; f++) {
                            if (hasAdvancedModule(f)) {
                                Slot slot2 = this.slots.get(f * 4 + 2);
                                if (!slot2.hasItem() && slot2.mayPlace(itemstack1)) {
                                    ItemStack single = itemstack1.split(1);
                                    slot2.set(single);
                                    moved = true;
                                    break;
                                }
                                Slot slot3 = this.slots.get(f * 4 + 3);
                                if (!slot3.hasItem() && slot3.mayPlace(itemstack1)) {
                                    ItemStack single = itemstack1.split(1);
                                    slot3.set(single);
                                    moved = true;
                                    break;
                                }
                            }
                        }
                    }
                }

                if (!moved) {
                    if (index < 51) {
                        if (!this.moveItemStackTo(itemstack1, 51, 60, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (index < 60 && !this.moveItemStackTo(itemstack1, 24, 51, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}
