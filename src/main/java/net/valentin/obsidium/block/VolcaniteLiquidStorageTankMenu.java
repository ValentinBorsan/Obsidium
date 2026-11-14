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
import net.minecraft.world.item.Items;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.VolcaniteBottleItem;
import net.valentin.obsidium.item.VolcaniteLiquidStorageDiskItem;

public class VolcaniteLiquidStorageTankMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public VolcaniteLiquidStorageTankMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(6), new SimpleContainerData(7));
    }

    public VolcaniteLiquidStorageTankMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(ObsidiumMod.VOLCANITE_LIQUID_STORAGE_TANK_MENU, syncId);
        checkContainerSize(container, 6);
        checkContainerDataCount(data, 7);
        this.container = container;
        this.data = data;
        container.startOpen(playerInventory.player);

        // Slot 0: Fluid input at (28, 24)
        this.addSlot(new Slot(container, 0, 29, 25));
        // Slot 1: Fluid output at (28, 54)
        this.addSlot(new Slot(container, 1, 29, 55) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });

        // Slots 2..5: 4 Fluid Disk slots (2x2) at (88, 26), (116, 26), (88, 52), (116, 52)
        int[][] diskCoords = {{89, 27}, {117, 27}, {89, 53}, {117, 53}};
        for (int i = 0; i < 4; i++) {
            this.addSlot(new Slot(container, i + 2, diskCoords[i][0], diskCoords[i][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof VolcaniteLiquidStorageDiskItem;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        // Player Inventory
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

    public int getFluidAmount() {
        return (this.data.get(0) << 16) | (this.data.get(1) & 0xFFFF);
    }

    public int getMaxCapacity() {
        return (this.data.get(2) << 16) | (this.data.get(3) & 0xFFFF);
    }

    public int getFluidTypeId() {
        return this.data.get(4);
    }

    public int getEnergy() {
        return (this.data.get(5) << 16) | (this.data.get(6) & 0xFFFF);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemStack = slotStack.copy();

            if (index < 6) {
                if (!this.moveItemStackTo(slotStack, 6, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (slotStack.getItem() instanceof VolcaniteLiquidStorageDiskItem) {
                    if (!this.moveItemStackTo(slotStack, 2, 6, false)) return ItemStack.EMPTY;
                } else if (slotStack.is(Items.LAVA_BUCKET) || slotStack.is(Items.WATER_BUCKET) ||
                           slotStack.is(ObsidiumMod.PYROCLASTIC_DISTILLATE_BUCKET) || slotStack.is(ObsidiumMod.PYROCLASTIC_FLASK) ||
                           slotStack.is(ObsidiumMod.STEAM_BUCKET) || slotStack.is(Items.GLASS_BOTTLE) ||
                           slotStack.is(Items.BUCKET) || slotStack.getItem() instanceof VolcaniteBottleItem) {
                    if (!this.moveItemStackTo(slotStack, 0, 1, false)) return ItemStack.EMPTY;
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
