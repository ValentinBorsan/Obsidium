package net.valentin.obsidium.block;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;
import net.valentin.obsidium.ObsidiumMod;

public class VolcaniteGeneratorMenu extends AbstractContainerMenu {
    private final SimpleContainer container;
    private final ContainerData data;

    public VolcaniteGeneratorMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(2), new net.minecraft.world.inventory.SimpleContainerData(5));
    }

    public VolcaniteGeneratorMenu(int syncId, Inventory playerInventory, SimpleContainer container, ContainerData data) {
        super(ObsidiumMod.VOLCANITE_GENERATOR_MENU, syncId);
        checkContainerSize(container, 2);
        checkContainerDataCount(data, 5);
        this.container = container;
        this.data = data;

        // Input Slot (Coal, Charcoal, Wood, Lava Bucket, Volcanite Bottle)
        this.addSlot(new Slot(container, 0, 44, 35));
        // Output Slot (Empty Bucket)
        this.addSlot(new Slot(container, 1, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }

        this.addDataSlots(data);
    }

    public int getBurnTime() {
        return this.data.get(0);
    }

    public int getMaxBurnTime() {
        return this.data.get(1);
    }

    public int getLavaAmount() {
        return this.data.get(2);
    }

    public int getEnergyAmount() {
        // Energy can be > 65535, so we split it across 2 ints
        return (this.data.get(3) << 16) | (this.data.get(4) & 0xFFFF);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();
            if (index < 2) {
                if (!this.moveItemStackTo(itemStack2, 2, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (VolcaniteGeneratorBlockEntity.isFuel(itemStack2)) {
                    if (!this.moveItemStackTo(itemStack2, 0, 1, false)) return ItemStack.EMPTY;
                } else {
                    return ItemStack.EMPTY;
                }
            }
            if (itemStack2.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return itemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}
