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

public class AdvancedDionysianPressMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public AdvancedDionysianPressMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(AdvancedDionysianPressBlockEntity.INVENTORY_SIZE),
                new SimpleContainerData(AdvancedDionysianPressBlockEntity.DATA_COUNT));
    }

    public AdvancedDionysianPressMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(ObsidiumMod.ADVANCED_DIONYSIAN_PRESS_MENU, syncId);
        this.container = container;
        this.data = data;
        checkContainerSize(container, AdvancedDionysianPressBlockEntity.INVENTORY_SIZE);
        checkContainerDataCount(data, AdvancedDionysianPressBlockEntity.DATA_COUNT);
        container.startOpen(playerInventory.player);

        // Slots 0..2: 3 Parallel Fruit Input Slots (x = 40, y = 22, 44, 66)
        for (int i = 0; i < AdvancedDionysianPressBlockEntity.LANES; i++) {
            this.addSlot(new Slot(container, i, 40, 22 + i * 22) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return DionysianPressBlockEntity.isFruit(stack);
                }
            });
        }

        // Slots 3..5: 3 Mineral Output Slots (x = 108, y = 22, 44, 66)
        for (int i = 0; i < AdvancedDionysianPressBlockEntity.LANES; i++) {
            this.addSlot(new Slot(container, 3 + i, 108, 22 + i * 22) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        // Slot 6: Shared Organic Pomace Output (x = 142, y = 44)
        this.addSlot(new Slot(container, 6, 142, 44) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Player Inventory (y = 104)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
            }
        }

        // Player Hotbar (y = 162)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 162));
        }

        this.addDataSlots(data);
    }

    public int getLaneProgress(int lane) {
        return this.data.get(lane);
    }

    public int getMaxProgress() {
        return Math.max(1, this.data.get(3));
    }

    public int getActiveLanes() {
        return this.data.get(4);
    }

    public long getEnergy() {
        return ((long) this.data.get(5) << 16) | (this.data.get(6) & 0xFFFFL);
    }

    public long getMaxEnergy() {
        return Math.max(1L, ((long) this.data.get(7) << 16) | (this.data.get(8) & 0xFFFFL));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int machineSlots = AdvancedDionysianPressBlockEntity.INVENTORY_SIZE;

            if (index < machineSlots) {
                if (!this.moveItemStackTo(stack, machineSlots, machineSlots + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (DionysianPressBlockEntity.isFruit(stack)) {
                    if (!this.moveItemStackTo(stack, 0, AdvancedDionysianPressBlockEntity.LANES, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < machineSlots + 27) {
                    if (!this.moveItemStackTo(stack, machineSlots + 27, machineSlots + 36, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(stack, machineSlots, machineSlots + 27, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();

            if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}
