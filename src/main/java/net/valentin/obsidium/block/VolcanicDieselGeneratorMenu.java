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
import team.reborn.energy.api.base.SimpleEnergyItem;

public class VolcanicDieselGeneratorMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    public VolcanicDieselGeneratorMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(VolcanicDieselGeneratorBlockEntity.INVENTORY_SIZE), new SimpleContainerData(11));
    }

    public VolcanicDieselGeneratorMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(ObsidiumMod.VOLCANIC_DIESEL_GENERATOR_MENU, syncId);
        this.container = container;
        this.data = data;
        checkContainerSize(container, VolcanicDieselGeneratorBlockEntity.INVENTORY_SIZE);
        checkContainerDataCount(data, 11);
        container.startOpen(playerInventory.player);

        this.addSlot(new Slot(container, 0, 26, 22) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return VolcanicDieselGeneratorBlockEntity.getFluidFromBucket(stack) != null;
            }
        });

        this.addSlot(new Slot(container, 1, 26, 50) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        this.addSlot(new Slot(container, 2, 134, 50) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof SimpleEnergyItem;
            }
        });

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(data);
    }

    public int getBurnTime() { return data.get(0); }
    public int getMaxBurnTime() { return data.get(1); }
    public int getCurrentRfPerTick() { return data.get(2); }
    public long getEnergy() { return ((long) data.get(3) << 16) | (data.get(4) & 0xFFFFL); }
    public long getMaxEnergy() {
        long cap = ((long) data.get(5) << 16) | (data.get(6) & 0xFFFFL);
        return cap > 0 ? cap : VolcanicDieselGeneratorBlockEntity.MAX_ENERGY;
    }
    public int getFluidAmount() { return data.get(7); }
    public int getMaxFluid() {
        int max = data.get(8);
        return max > 0 ? max : VolcanicDieselGeneratorBlockEntity.MAX_FLUID;
    }
    public int getRpm() { return data.get(9); }
    public int getFluidTypeId() { return data.get(10); }

    public int getEffectiveRfPerTick() {
        int base = getCurrentRfPerTick();
        if (base <= 0) return 0;
        double bonus = 1.0 + 0.25 * ((double) getRpm() / (double) VolcanicDieselGeneratorBlockEntity.MAX_RPM);
        return (int) Math.round(base * bonus);
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack original = slot.getItem();
            newStack = original.copy();
            if (index < VolcanicDieselGeneratorBlockEntity.INVENTORY_SIZE) {
                if (!this.moveItemStackTo(original, VolcanicDieselGeneratorBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (VolcanicDieselGeneratorBlockEntity.getFluidFromBucket(original) != null) {
                    if (!this.moveItemStackTo(original, 0, 1, false)) return ItemStack.EMPTY;
                } else if (original.getItem() instanceof SimpleEnergyItem) {
                    if (!this.moveItemStackTo(original, 2, 3, false)) return ItemStack.EMPTY;
                } else if (index < VolcanicDieselGeneratorBlockEntity.INVENTORY_SIZE + 27) {
                    if (!this.moveItemStackTo(original, VolcanicDieselGeneratorBlockEntity.INVENTORY_SIZE + 27, this.slots.size(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(original, VolcanicDieselGeneratorBlockEntity.INVENTORY_SIZE, VolcanicDieselGeneratorBlockEntity.INVENTORY_SIZE + 27, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (original.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return newStack;
    }
}

