package net.valentin.obsidium.block;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.storage.QuantumLinkNetworkState;

public class LiquidTankMenu extends AbstractContainerMenu {
    public static final int BTN_PREV_COLOR = 0;
    public static final int BTN_NEXT_COLOR = 1;
    public static final int BTN_DEC_CH_1 = 2;
    public static final int BTN_INC_CH_1 = 3;
    public static final int BTN_DEC_CH_10 = 4;
    public static final int BTN_INC_CH_10 = 5;
    public static final int BTN_TOGGLE_PRIVATE = 6;

    private final Container container;
    private final ContainerData data;

    public LiquidTankMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(2), new SimpleContainerData(9));
    }

    public LiquidTankMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(ObsidiumMod.LIQUID_TANK_MENU, syncId);
        checkContainerSize(container, 2);
        checkContainerDataCount(data, 9);
        this.container = container;
        this.data = data;

        container.startOpen(playerInventory.player);

        // Input Slot 0 at (100, 24)
        this.addSlot(new Slot(container, 0, 100, 24));
        // Output Slot 1 at (100, 52)
        this.addSlot(new Slot(container, 1, 100, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Player Inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(data);
    }

    public int getFluidAmount() {
        return (data.get(0) << 16) | (data.get(1) & 0xFFFF);
    }

    public int getMaxCapacity() {
        int cap = (data.get(2) << 16) | (data.get(3) & 0xFFFF);
        return cap > 0 ? cap : 16000;
    }

    public Fluid getFluid() {
        int id = data.get(4);
        if (id <= 0) return Fluids.EMPTY;
        Fluid f = BuiltInRegistries.FLUID.byId(id);
        return f != null ? f : Fluids.EMPTY;
    }

    public boolean isQuantumLinked() {
        return data.get(5) != 0;
    }

    public DyeColor getQuantumColor() {
        return DyeColor.byId(Math.floorMod(data.get(6), 16));
    }

    public int getQuantumChannel() {
        return QuantumLinkNetworkState.clampChannel(data.get(7));
    }

    public boolean isQuantumPrivate() {
        return data.get(8) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!isQuantumLinked()) return false;
        switch (id) {
            case BTN_PREV_COLOR -> data.set(6, Math.floorMod(getQuantumColor().getId() - 1, 16));
            case BTN_NEXT_COLOR -> data.set(6, Math.floorMod(getQuantumColor().getId() + 1, 16));
            case BTN_DEC_CH_1 -> data.set(7, QuantumLinkNetworkState.clampChannel(getQuantumChannel() - 1));
            case BTN_INC_CH_1 -> data.set(7, QuantumLinkNetworkState.clampChannel(getQuantumChannel() + 1));
            case BTN_DEC_CH_10 -> data.set(7, QuantumLinkNetworkState.clampChannel(getQuantumChannel() - 10));
            case BTN_INC_CH_10 -> data.set(7, QuantumLinkNetworkState.clampChannel(getQuantumChannel() + 10));
            case BTN_TOGGLE_PRIVATE -> data.set(8, isQuantumPrivate() ? 0 : 1);
            default -> {
                return false;
            }
        }
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (invSlot < 2) {
                // Moving from tank to player inventory
                if (!this.moveItemStackTo(originalStack, 2, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to input slot 0
                if (!this.moveItemStackTo(originalStack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
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
