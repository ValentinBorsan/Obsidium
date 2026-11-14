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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.ObsidiumMod;

public class LiquidTankMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public LiquidTankMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(2), new SimpleContainerData(5));
    }

    public LiquidTankMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(ObsidiumMod.LIQUID_TANK_MENU, syncId);
        checkContainerSize(container, 2);
        checkContainerDataCount(data, 5);
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
