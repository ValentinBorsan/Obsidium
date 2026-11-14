package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.ObsidiumMod;

public class DemetrianSaplingBarrelMenu extends AbstractContainerMenu {
    private final DemetrianSaplingBarrelBlockEntity blockEntity;
    private final ContainerData data;
    private final SimpleContainer dummyChambers;

    // Slot coordinates (18x18):
    // Chamber 0: x = 27, y = 33
    // Chamber 1: x = 79, y = 33
    // Chamber 2: x = 131, y = 33
    // Player Inventory (3 rows): x = 8, y = 112
    // Player Hotbar (1 row): x = 8, y = 170

    public DemetrianSaplingBarrelMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, null, new SimpleContainerData(9));
    }

    public DemetrianSaplingBarrelMenu(int syncId, Inventory playerInventory, DemetrianSaplingBarrelBlockEntity blockEntity, ContainerData data) {
        super(ObsidiumMod.DEMETRIAN_SAPLING_BARREL_MENU, syncId);
        this.blockEntity = blockEntity;
        this.data = data;
        checkContainerDataCount(data, 9);

        this.dummyChambers = new SimpleContainer(3);

        if (blockEntity != null) {
            for (int i = 0; i < 3; i++) {
                DemetrianSaplingBarrelBlockEntity.Chamber c = blockEntity.getChamber(i);
                if (!c.getVariant().isBlank() && c.getAmount() > 0) {
                    this.dummyChambers.setItem(i, c.getVariant().toStack(1));
                }
            }
        }

        // Chamber Slots 0, 1, 2 (centered inside 18x18 frame at 27/79/131, 33)
        this.addSlot(new Slot(this.dummyChambers, 0, 28, 34) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return DemetrianSaplingBarrelBlockEntity.isSapling(stack);
            }
            @Override
            public boolean mayPickup(Player player) {
                return false; // Handled custom in clicked()
            }
        });

        this.addSlot(new Slot(this.dummyChambers, 1, 80, 34) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return DemetrianSaplingBarrelBlockEntity.isSapling(stack);
            }
            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });

        this.addSlot(new Slot(this.dummyChambers, 2, 132, 34) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return DemetrianSaplingBarrelBlockEntity.isSapling(stack);
            }
            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });

        // Player Inventory (3 rows x 9 columns) at y = 112
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 112 + row * 18));
            }
        }

        // Hotbar (1 row x 9 columns) at y = 170
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 170));
        }

        this.addDataSlots(data);
    }

    @Override
    public void broadcastChanges() {
        if (blockEntity != null) {
            for (int i = 0; i < 3; i++) {
                DemetrianSaplingBarrelBlockEntity.Chamber c = blockEntity.getChamber(i);
                ItemStack sample = (!c.getVariant().isBlank() && c.getAmount() > 0) ? c.getVariant().toStack(1) : ItemStack.EMPTY;
                if (!ItemStack.matches(this.dummyChambers.getItem(i), sample)) {
                    this.dummyChambers.setItem(i, sample);
                }
            }
        }
        super.broadcastChanges();
    }

    public DemetrianSaplingBarrelBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public long getChamberAmount(int chamberIndex) {
        if (chamberIndex < 0 || chamberIndex >= 3) return 0;
        int low = data.get(chamberIndex * 3);
        int high = data.get(chamberIndex * 3 + 1);
        return ((long) high << 16) | (low & 0xFFFFL);
    }

    public boolean isChamberLocked(int chamberIndex) {
        if (chamberIndex < 0 || chamberIndex >= 3) return false;
        return data.get(chamberIndex * 3 + 2) == 1;
    }

    public ItemStack getSampleStack(int chamberIndex) {
        if (chamberIndex >= 0 && chamberIndex < 3) {
            ItemStack inSlot = this.dummyChambers.getItem(chamberIndex);
            if (!inSlot.isEmpty()) return inSlot;
            if (blockEntity != null) {
                DemetrianSaplingBarrelBlockEntity.Chamber c = blockEntity.getChamber(chamberIndex);
                if (!c.getVariant().isBlank() && c.getAmount() > 0) {
                    return c.getVariant().toStack(1);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < 3) {
            if (blockEntity != null) {
                int ch = slotId;
            DemetrianSaplingBarrelBlockEntity.Chamber chamber = blockEntity.getChamber(ch);
            ItemStack carried = getCarried();

            // 1. Placing carried saplings into chamber
            if (!carried.isEmpty() && DemetrianSaplingBarrelBlockEntity.isSapling(carried)) {
                ItemVariant v = ItemVariant.of(carried);
                if (chamber.getVariant().isBlank() || chamber.getVariant().equals(v)) {
                    chamber.setVariant(v);
                    long space = DemetrianSaplingBarrelBlockEntity.MAX_CHAMBER_CAPACITY - chamber.getAmount();
                    int toAdd = (button == 1) ? 1 : (int) Math.min(carried.getCount(), space);
                    if (toAdd > 0) {
                        carried.shrink(toAdd);
                        chamber.setAmount(chamber.getAmount() + toAdd);
                        this.dummyChambers.setItem(ch, v.toStack(1));
                        blockEntity.sync();
                        this.broadcastChanges();
                        return;
                    }
                }
            }

            // 2. Extracting from chamber when cursor is empty
            if (carried.isEmpty() && !chamber.getVariant().isBlank() && chamber.getAmount() > 0) {
                int maxStack = chamber.getVariant().toStack().getMaxStackSize();
                int toTake = (button == 1) ? 1 : (int) Math.min(maxStack, chamber.getAmount());
                if (toTake <= 0) toTake = 1;

                if (clickType == ClickType.QUICK_MOVE) {
                    // Shift-click directly into player inventory
                    ItemStack out = chamber.getVariant().toStack(toTake);
                    if (this.moveItemStackTo(out, 3, 39, true)) {
                        int moved = toTake - out.getCount();
                        chamber.setAmount(chamber.getAmount() - moved);
                        if (chamber.getAmount() <= 0 && !chamber.isLocked()) {
                            chamber.setVariant(ItemVariant.blank());
                            this.dummyChambers.setItem(ch, ItemStack.EMPTY);
                        }
                        blockEntity.sync();
                        this.broadcastChanges();
                        return;
                    }
                } else {
                    // Pick up onto cursor
                    ItemStack picked = chamber.getVariant().toStack(toTake);
                    chamber.setAmount(chamber.getAmount() - toTake);
                    if (chamber.getAmount() <= 0 && !chamber.isLocked()) {
                        chamber.setVariant(ItemVariant.blank());
                        this.dummyChambers.setItem(ch, ItemStack.EMPTY);
                    }
                    setCarried(picked);
                    blockEntity.sync();
                    this.broadcastChanges();
                    return;
                }
            }
            }

            return;
        }

        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack currentStack = slot.getItem();
        ItemStack copy = currentStack.copy();

        if (slotIndex < 3) {
            // Chamber slot extraction via shift-click handled in clicked()
            return ItemStack.EMPTY;
        } else {
            // Player Inventory -> Chamber
            if (DemetrianSaplingBarrelBlockEntity.isSapling(currentStack)) {
                if (blockEntity != null) {
                    int before = currentStack.getCount();
                    if (blockEntity.insertSapling(currentStack, player) && currentStack.getCount() < before) {
                        this.broadcastChanges();
                        if (currentStack.isEmpty()) {
                            slot.setByPlayer(ItemStack.EMPTY);
                        } else {
                            slot.setChanged();
                        }
                        return copy;
                    }
                }
                return ItemStack.EMPTY;
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.blockEntity != null && this.blockEntity.stillValid(player);
    }
}
