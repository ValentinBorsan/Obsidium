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

public final class AmbrosiaFeederMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public AmbrosiaFeederMenu(int syncId, Inventory inv) {
        this(syncId, inv, new SimpleContainer(9), new SimpleContainerData(AmbrosiaFeederBlockEntity.DATA_COUNT));
    }

    public AmbrosiaFeederMenu(int syncId, Inventory inv, Container container, ContainerData data) {
        super(ObsidiumMod.AMBROSIA_FEEDER_MENU, syncId);
        checkContainerSize(container, 9);
        checkContainerDataCount(data, AmbrosiaFeederBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        // 6 Food Input slots (2x3) on left
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 3; c++) {
                addSlot(new Slot(container, r * 3 + c, 18 + c * 18, 44 + r * 18) {
                    @Override public boolean mayPlace(ItemStack stack) { return AmbrosiaFeederBlockEntity.isBreedingFood(stack); }
                });
            }
        }
        // 3 Output slots (Wool/Eggs) on right
        for (int c = 0; c < 3; c++) {
            addSlot(new Slot(container, 6 + c, 108 + c * 18, 53) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, 9 + row * 9 + col, 8 + col * 18, 94 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 152));
        }
        addDataSlots(data);
    }

    public long energy() { return data.get(0) * 100L; }
    public int animalCount() { return data.get(1); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 9) {
            if (!moveItemStackTo(stack, 9, 45, true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 0, 6, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}
