package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
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
import team.reborn.energy.api.EnergyStorage;

public class OlympianHadronColliderMenu extends AbstractContainerMenu {
    private final ContainerData data;
    @Nullable
    private final OlympianHadronColliderBlockEntity blockEntity;

    public OlympianHadronColliderMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory,
                new SimpleContainerData(OlympianHadronColliderBlockEntity.DATA_COUNT),
                new SimpleContainer(OlympianHadronColliderBlockEntity.INVENTORY_SIZE),
                null);
    }

    public OlympianHadronColliderMenu(int syncId, Inventory playerInventory, ContainerData data, Container container,
                                      @Nullable OlympianHadronColliderBlockEntity blockEntity) {
        super(ObsidiumMod.OLYMPIAN_HADRON_COLLIDER_MENU, syncId);
        checkContainerDataCount(data, OlympianHadronColliderBlockEntity.DATA_COUNT);
        checkContainerSize(container, OlympianHadronColliderBlockEntity.INVENTORY_SIZE);
        this.data = data;
        this.blockEntity = blockEntity;

        // Slot 0: Beam A Particle Input (48, 42)
        this.addSlot(new Slot(container, OlympianHadronColliderBlockEntity.SLOT_BEAM_A, 48, 42) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !OlympianHadronColliderBlockEntity.isFuelContainerItem(stack)
                        && !OlympianHadronColliderBlockEntity.isCoolantContainerItem(stack);
            }
        });

        // Slot 1: Beam B Particle Input (172, 42)
        this.addSlot(new Slot(container, OlympianHadronColliderBlockEntity.SLOT_BEAM_B, 172, 42) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !OlympianHadronColliderBlockEntity.isFuelContainerItem(stack)
                        && !OlympianHadronColliderBlockEntity.isCoolantContainerItem(stack);
            }
        });

        // Slot 2: Fusion Output 1 (98, 92)
        this.addSlot(new Slot(container, OlympianHadronColliderBlockEntity.SLOT_OUTPUT_MAIN, 98, 92) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Slot 3: Fusion Output 2 (122, 92)
        this.addSlot(new Slot(container, OlympianHadronColliderBlockEntity.SLOT_OUTPUT_SECOND, 122, 92) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Slot 4: Propulsion Fuel Input (12, 24)
        this.addSlot(new Slot(container, OlympianHadronColliderBlockEntity.SLOT_FUEL_IN, 12, 24) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return OlympianHadronColliderBlockEntity.isFuelContainerItem(stack);
            }
        });

        // Slot 5: Cryo-Coolant Input (208, 24)
        this.addSlot(new Slot(container, OlympianHadronColliderBlockEntity.SLOT_COOLANT_IN, 208, 24) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return OlympianHadronColliderBlockEntity.isCoolantContainerItem(stack)
                        || OlympianHadronColliderBlockEntity.isSolidCryoItem(stack);
            }
        });

        // Slot 6: Container Return / Battery Charge (12, 92)
        this.addSlot(new Slot(container, OlympianHadronColliderBlockEntity.SLOT_CONTAINER_OUT, 12, 92) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !stack.isEmpty() && EnergyStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack)) != null;
            }
        });

        // Player Inventory (38, 134)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 38 + col * 18, 134 + row * 18));
            }
        }
        // Hotbar (38, 192)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 38 + col * 18, 192));
        }

        this.addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.blockEntity == null || !stillValid(player)) return false;
        switch (id) {
            case 0 -> {
                this.blockEntity.toggleOperatingMode();
                return true;
            }
            case 1 -> {
                return this.blockEntity.triggerCollision(player);
            }
            case 2 -> {
                this.blockEntity.toggleHologram();
                return true;
            }
            case 3 -> {
                this.blockEntity.tryAutoBuildRing(player);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public long getEnergyAmount() {
        long p2 = this.data.get(0) & 0xFFFFL;
        long p1 = this.data.get(1) & 0xFFFFL;
        long p0 = this.data.get(2) & 0xFFFFL;
        return (p2 << 32) | (p1 << 16) | p0;
    }

    public boolean isFormed() { return this.data.get(3) != 0; }
    public int getCoilCount() { return this.data.get(4); }
    public int getBeamVelocity() { return this.data.get(5); }
    public int getCoilTemp() { return this.data.get(6); }
    public int getFuelAmount() { return this.data.get(7); }
    public int getFuelType() { return this.data.get(8); }
    public int getCoolantAmount() { return this.data.get(9); }
    public int getCoolantType() { return this.data.get(10); }

    public int getCurrentRfPerTick() {
        return (this.data.get(11) & 0xFFFF) | ((this.data.get(12) & 0xFFFF) << 16);
    }

    public int getOperatingMode() { return this.data.get(13); }
    public boolean isShowHologram() { return this.data.get(14) != 0; }
    public int getCollisionFlashTicks() { return this.data.get(15); }

    public int getTotalCollisions() {
        return (this.data.get(16) & 0xFFFF) | ((this.data.get(17) & 0xFFFF) << 16);
    }

    @Override
    public boolean stillValid(Player player) {
        // The client constructs a placeholder inventory; the server must validate the real block entity.
        if (this.blockEntity == null) return player.level().isClientSide();
        return !this.blockEntity.isRemoved()
                && this.blockEntity.getLevel() == player.level()
                && this.blockEntity.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= this.slots.size()) return ItemStack.EMPTY;
        ItemStack copied = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack raw = slot.getItem();
            copied = raw.copy();
            int machineSlots = OlympianHadronColliderBlockEntity.INVENTORY_SIZE;

            if (index < machineSlots) {
                if (!this.moveItemStackTo(raw, machineSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (OlympianHadronColliderBlockEntity.isFuelContainerItem(raw)) {
                    if (!this.moveItemStackTo(raw, OlympianHadronColliderBlockEntity.SLOT_FUEL_IN, OlympianHadronColliderBlockEntity.SLOT_FUEL_IN + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (OlympianHadronColliderBlockEntity.isCoolantContainerItem(raw) || OlympianHadronColliderBlockEntity.isSolidCryoItem(raw)) {
                    if (!this.moveItemStackTo(raw, OlympianHadronColliderBlockEntity.SLOT_COOLANT_IN, OlympianHadronColliderBlockEntity.SLOT_COOLANT_IN + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (EnergyStorage.ITEM.find(raw, ContainerItemContext.withConstant(raw)) != null) {
                    if (!this.moveItemStackTo(raw, OlympianHadronColliderBlockEntity.SLOT_CONTAINER_OUT, OlympianHadronColliderBlockEntity.SLOT_CONTAINER_OUT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.moveItemStackTo(raw, OlympianHadronColliderBlockEntity.SLOT_BEAM_A, OlympianHadronColliderBlockEntity.SLOT_BEAM_B + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (raw.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (raw.getCount() == copied.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, raw);
        }
        return copied;
    }
}

