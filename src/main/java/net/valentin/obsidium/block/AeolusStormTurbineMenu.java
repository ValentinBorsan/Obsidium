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

public class AeolusStormTurbineMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public AeolusStormTurbineMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(AeolusStormTurbineBlockEntity.INVENTORY_SIZE), new SimpleContainerData(8));
    }

    public AeolusStormTurbineMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(ObsidiumMod.AEOLUS_STORM_TURBINE_MENU, syncId);
        this.container = container;
        this.data = data;
        checkContainerSize(container, AeolusStormTurbineBlockEntity.INVENTORY_SIZE);
        checkContainerDataCount(data, 8);
        container.startOpen(playerInventory.player);

        // Slot 0: Storm Catalyst / RF Charge Slot (x = 122, y = 48)
        this.addSlot(new Slot(container, 0, 122, 48));

        // Player Inventory (3 rows x 9 columns)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar (9 slots)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(data);
    }

    public int getEnergy() {
        int high = this.data.get(0) & 0xFFFF;
        int low = this.data.get(1) & 0xFFFF;
        return (high << 16) | low;
    }

    public int getMaxEnergy() {
        return AeolusStormTurbineBlockEntity.MAX_ENERGY;
    }

    public int getGenerationRate() {
        return this.data.get(2);
    }

    public int getAltitudePct() {
        return this.data.get(3);
    }

    public int getBiomePct() {
        return this.data.get(4);
    }

    public int getWeatherState() {
        return this.data.get(5);
    }

    public int getSurgeTicks() {
        return this.data.get(6);
    }

    public boolean hasLightningRod() {
        return this.data.get(7) == 1;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (invSlot == 0) {
                if (!this.moveItemStackTo(originalStack, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(originalStack, 0, 1, false)) {
                    if (invSlot < 28) {
                        if (!this.moveItemStackTo(originalStack, 28, 37, false)) return ItemStack.EMPTY;
                    } else if (!this.moveItemStackTo(originalStack, 1, 28, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (originalStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (originalStack.getCount() == newStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, originalStack);
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

