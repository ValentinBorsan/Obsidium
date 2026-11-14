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

public class DemetrianBiomassGeneratorMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    public DemetrianBiomassGeneratorMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(DemetrianBiomassGeneratorBlockEntity.INVENTORY_SIZE), new SimpleContainerData(9));
    }

    public DemetrianBiomassGeneratorMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(ObsidiumMod.DEMETRIAN_BIOMASS_GENERATOR_MENU, syncId);
        this.container = container;
        this.data = data;
        checkContainerSize(container, DemetrianBiomassGeneratorBlockEntity.INVENTORY_SIZE);
        checkContainerDataCount(data, 9);
        container.startOpen(playerInventory.player);

        // Machine Slots (176x166 standard layout)
        // 0: Fruit / Biomass Input (left)
        this.addSlot(new Slot(container, 0, 54, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return DemetrianBiomassGeneratorBlockEntity.isBiomassFruit(stack);
            }
        });

        // 1: Byproduct Output (center-right)
        this.addSlot(new Slot(container, 1, 106, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // 2: Battery / Tool Charging Slot (far right)
        this.addSlot(new Slot(container, 2, 148, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof SimpleEnergyItem;
            }
        });

        // Player Inventory (3x9)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar (1x9)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(data);
    }

    public int getBurnTime() {
        return this.data.get(0);
    }

    public int getMaxBurnTime() {
        return this.data.get(1);
    }

    public int getCurrentRfPerTick() {
        return this.data.get(2);
    }

    public long getEnergy() {
        return ((long) this.data.get(3) << 16) | ((long) this.data.get(4) & 0xFFFFL);
    }

    public long getCapacity() {
        return ((long) this.data.get(5) << 16) | ((long) this.data.get(6) & 0xFFFFL);
    }

    public int getBiogasAmount() {
        return this.data.get(7);
    }

    public int getMaxBiogas() {
        return this.data.get(8);
    }

    public int getBiogasScaled(int pixels) {
        int max = getMaxBiogas();
        if (max == 0) max = DemetrianBiomassGeneratorBlockEntity.MAX_BIOGAS;
        return getBiogasAmount() * pixels / max;
    }

    public boolean isLit() {
        return getBurnTime() > 0;
    }

    public int getBurnTimeScaled(int pixels) {
        int max = getMaxBurnTime();
        if (max == 0) max = 200;
        return getBurnTime() * pixels / max;
    }

    public int getEnergyScaled(int pixels) {
        long cap = getCapacity();
        if (cap == 0) cap = DemetrianBiomassGeneratorBlockEntity.MAX_ENERGY;
        return (int) (getEnergy() * pixels / cap);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemStack = stackInSlot.copy();

            // Output slot 1 -> move to player inventory
            if (index == 1) {
                if (!this.moveItemStackTo(stackInSlot, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, itemStack);
            }
            // Machine slots 0 or 2 -> move to player inventory
            else if (index < 3) {
                if (!this.moveItemStackTo(stackInSlot, 3, 39, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // Player inventory -> machine slots
            else {
                if (DemetrianBiomassGeneratorBlockEntity.isBiomassFruit(stackInSlot)) {
                    if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (stackInSlot.getItem() instanceof SimpleEnergyItem) {
                    if (!this.moveItemStackTo(stackInSlot, 2, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < 30) {
                    if (!this.moveItemStackTo(stackInSlot, 30, 39, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 30 && !this.moveItemStackTo(stackInSlot, 3, 30, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stackInSlot);
        }

        return itemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}

