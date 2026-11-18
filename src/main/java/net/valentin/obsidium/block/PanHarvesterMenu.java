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
import org.jetbrains.annotations.Nullable;

public final class PanHarvesterMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    private final @Nullable PanHarvesterBlockEntity blockEntity;

    public PanHarvesterMenu(int syncId, Inventory inv) {
        this(syncId, inv, new SimpleContainer(9), new SimpleContainerData(PanHarvesterBlockEntity.DATA_COUNT), null);
    }

    public PanHarvesterMenu(int syncId, Inventory inv, Container container, ContainerData data, @Nullable PanHarvesterBlockEntity blockEntity) {
        super(ObsidiumMod.PAN_HARVESTER_MENU, syncId);
        checkContainerSize(container, 9);
        checkContainerDataCount(data, PanHarvesterBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockEntity = blockEntity;

        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(container, col, 8 + col * 18, 56) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, 9 + row * 9 + col, 8 + col * 18, 86 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 144));
        }
        addDataSlots(data);
    }

    public long energy() { return data.get(0) * 100L; }
    public int pendingItems() { return data.get(2); }
    public int keepAdults() { return data.get(1); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && blockEntity != null && stillValid(player)) {
            blockEntity.cycleKeepAdults();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 9) {
            if (!moveItemStackTo(stack, 9, 45, true)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
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
