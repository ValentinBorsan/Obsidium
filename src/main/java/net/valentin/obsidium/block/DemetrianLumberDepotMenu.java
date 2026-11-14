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

public class DemetrianLumberDepotMenu extends AbstractContainerMenu {
    private final DemetrianLumberDepotBlockEntity blockEntity;
    private final ContainerData data;
    private final SimpleContainer dummyChambers;

    public DemetrianLumberDepotMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, null, new SimpleContainerData(12));
    }

    public DemetrianLumberDepotMenu(int syncId, Inventory playerInventory, DemetrianLumberDepotBlockEntity blockEntity, ContainerData data) {
        super(ObsidiumMod.DEMETRIAN_LUMBER_DEPOT_MENU, syncId);
        this.blockEntity = blockEntity;
        this.data = data;
        checkContainerDataCount(data, 12);

        this.dummyChambers = new SimpleContainer(4);

        if (blockEntity != null) {
            for (int i = 0; i < 4; i++) {
                DemetrianLumberDepotBlockEntity.Chamber c = blockEntity.getChamber(i);
                if (!c.getVariant().isBlank() && c.getAmount() > 0) {
                    this.dummyChambers.setItem(i, c.getVariant().toStack(1));
                }
            }
        }

        // 4 Chamber Slots (20, 60, 100, 140 at y = 34, centered inside cards)
        int[] slotX = {20, 60, 100, 140};
        for (int i = 0; i < 4; i++) {
            this.addSlot(new Slot(this.dummyChambers, i, slotX[i], 34) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return DemetrianLumberDepotBlockEntity.isLog(stack);
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false; // Handled custom in clicked()
                }
            });
        }

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
            for (int i = 0; i < 4; i++) {
                DemetrianLumberDepotBlockEntity.Chamber c = blockEntity.getChamber(i);
                ItemStack sample = (!c.getVariant().isBlank() && c.getAmount() > 0) ? c.getVariant().toStack(1) : ItemStack.EMPTY;
                if (!ItemStack.matches(this.dummyChambers.getItem(i), sample)) {
                    this.dummyChambers.setItem(i, sample);
                }
            }
        }
        super.broadcastChanges();
    }

    public DemetrianLumberDepotBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public long getChamberAmount(int chamberIndex) {
        if (chamberIndex < 0 || chamberIndex >= 4) return 0;
        int low = data.get(chamberIndex * 3);
        int high = data.get(chamberIndex * 3 + 1);
        return ((long) high << 16) | (low & 0xFFFFL);
    }

    public boolean isChamberLocked(int chamberIndex) {
        if (chamberIndex < 0 || chamberIndex >= 4) return false;
        return data.get(chamberIndex * 3 + 2) == 1;
    }

    public ItemStack getSampleStack(int chamberIndex) {
        if (chamberIndex >= 0 && chamberIndex < 4) {
            ItemStack inSlot = this.dummyChambers.getItem(chamberIndex);
            if (!inSlot.isEmpty()) return inSlot;
            if (blockEntity != null) {
                DemetrianLumberDepotBlockEntity.Chamber c = blockEntity.getChamber(chamberIndex);
                if (!c.getVariant().isBlank() && c.getAmount() > 0) {
                    return c.getVariant().toStack(1);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < 4) {
            if (blockEntity != null) {
                int ch = slotId;
                DemetrianLumberDepotBlockEntity.Chamber chamber = blockEntity.getChamber(ch);
                ItemStack carried = getCarried();

                // 1. Placing carried logs into chamber
                if (!carried.isEmpty() && DemetrianLumberDepotBlockEntity.isLog(carried)) {
                    ItemVariant v = ItemVariant.of(carried);
                    if (chamber.getVariant().isBlank() || chamber.getVariant().equals(v)) {
                        chamber.setVariant(v);
                        long space = DemetrianLumberDepotBlockEntity.MAX_CHAMBER_CAPACITY - chamber.getAmount();
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
                if (carried.isEmpty() && !chamber.isEmpty()) {
                    int toTake = (button == 1) ? 1 : (clickType == ClickType.QUICK_MOVE ? 64 : (int) Math.min(64, chamber.getAmount()));
                    if (blockEntity.extractLog(ch, toTake, player)) {
                        this.dummyChambers.setItem(ch, chamber.isEmpty() ? ItemStack.EMPTY : chamber.getVariant().toStack(1));
                        blockEntity.sync();
                        this.broadcastChanges();
                        return;
                    }
                }
            }
            return; // Never pass chamber slots to super.clicked on client
        }

        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        Slot slot = this.slots.get(invSlot);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack originalStack = slot.getItem();
        ItemStack copy = originalStack.copy();

        if (invSlot < 4) {
            // Chamber slot extraction via shift-click handled in clicked()
            return ItemStack.EMPTY;
        } else {
            // Player Inventory -> Depot Chamber
            if (DemetrianLumberDepotBlockEntity.isLog(originalStack)) {
                if (blockEntity != null) {
                    int beforeCount = originalStack.getCount();
                    boolean inserted = blockEntity.insertLog(originalStack, player);
                    if (inserted && originalStack.getCount() < beforeCount) {
                        this.broadcastChanges();
                        if (originalStack.isEmpty()) {
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
        return this.blockEntity == null || this.blockEntity.stillValid(player);
    }
}
