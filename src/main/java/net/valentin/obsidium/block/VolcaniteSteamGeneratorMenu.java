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

public class VolcaniteSteamGeneratorMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public VolcaniteSteamGeneratorMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(3), new SimpleContainerData(6));
    }

    public VolcaniteSteamGeneratorMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(ObsidiumMod.VOLCANITE_STEAM_GENERATOR_MENU, syncId);
        checkContainerSize(container, 3);
        checkContainerDataCount(data, 6);
        this.container = container;
        this.data = data;

        // Slot 0: Water input (under left Water tank)
        this.addSlot(new Slot(container, 0, 22, 101) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return VolcaniteSteamGeneratorBlockEntity.isWaterContainer(stack);
            }
        });
        // Slot 1: Output bucket (under right Energy tank)
        this.addSlot(new Slot(container, 1, 198, 101) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        // Slot 2: Solid Fuel (bottom firebox panel)
        this.addSlot(new Slot(container, 2, 68, 102) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return VolcaniteSteamGeneratorBlockEntity.isFuel(stack);
            }
        });

        // Player Inventory (centered in 236px wide UI)
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 38 + j * 18, 140 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 38 + i * 18, 198));
        }

        this.addDataSlots(data);
    }

    public int getWaterAmount() {
        return this.data.get(0);
    }

    public int getSteamAmount() {
        return this.data.get(1);
    }

    public int getBurnTime() {
        return this.data.get(2);
    }

    public int getMaxBurnTime() {
        return this.data.get(3);
    }

    public int getEnergyAmount() {
        return (this.data.get(5) << 16) | (this.data.get(4) & 0xFFFF);
    }

    public boolean isBurning() {
        return this.getBurnTime() > 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemStack = slotStack.copy();

            if (index < 3) {
                if (!this.moveItemStackTo(slotStack, 3, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Check if water container
                if (VolcaniteSteamGeneratorBlockEntity.isWaterContainer(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (VolcaniteSteamGeneratorBlockEntity.isFuel(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, 2, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}
