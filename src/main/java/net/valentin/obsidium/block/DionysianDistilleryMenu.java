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

public class DionysianDistilleryMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    public DionysianDistilleryMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(DionysianDistilleryBlockEntity.INVENTORY_SIZE), new SimpleContainerData(6));
    }

    public DionysianDistilleryMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(ObsidiumMod.DIONYSIAN_DISTILLERY_MENU, syncId);
        this.container = container;
        this.data = data;
        checkContainerSize(container, DionysianDistilleryBlockEntity.INVENTORY_SIZE);
        checkContainerDataCount(data, 6);
        container.startOpen(playerInventory.player);

        // Machine Slots (Standard 176x166 layout)
        // 0: Fruit / Biomass Input
        this.addSlot(new Slot(container, 0, 44, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return DionysianDistilleryBlockEntity.isAnyDistillableInput(stack);
            }
        });

        // 1: Fuel Input
        this.addSlot(new Slot(container, 1, 44, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return DionysianDistilleryBlockEntity.isFuel(stack);
            }
        });

        // 2: Empty Glass Bottle, 1L Bottle, or Empty Bucket Input
        this.addSlot(new Slot(container, 2, 80, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.GLASS_BOTTLE) || stack.is(ObsidiumMod.EMPTY_TUICA_BOTTLE_1L) || stack.is(Items.BUCKET);
            }
        });

        // 3: Volcanic Țuică Output
        this.addSlot(new Slot(container, 3, 116, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // 4: Volcanic Ash Byproduct Output
        this.addSlot(new Slot(container, 4, 142, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
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

    public int getProgress() {
        return this.data.get(0);
    }

    public int getMaxProgress() {
        return this.data.get(1);
    }

    public int getBurnTime() {
        return this.data.get(2);
    }

    public int getTotalBurnTime() {
        return this.data.get(3);
    }

    public int getFluidAmount() {
        return this.data.get(4);
    }

    public int getFluidTypeId() {
        return this.data.get(5);
    }

    public boolean isLit() {
        return getBurnTime() > 0;
    }

    public int getProgressScaled(int pixels) {
        int progress = getProgress();
        int maxProgress = getMaxProgress();
        return (maxProgress != 0 && progress != 0) ? (progress * pixels / maxProgress) : 0;
    }

    public int getBurnTimeScaled(int pixels) {
        int total = getTotalBurnTime();
        if (total == 0) total = 200;
        return (getBurnTime() * pixels / total);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemStack = stackInSlot.copy();

            // Machine output slots -> move to player inventory
            if (index == 3 || index == 4) {
                if (!this.moveItemStackTo(stackInSlot, 5, 41, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, itemStack);
            }
            // Machine input slots -> move to player inventory
            else if (index < 5) {
                if (!this.moveItemStackTo(stackInSlot, 5, 41, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // Player inventory -> try move to machine input slots
            else {
                if (DionysianDistilleryBlockEntity.isAnyDistillableInput(stackInSlot)) {
                    if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (stackInSlot.is(Items.GLASS_BOTTLE) || stackInSlot.is(ObsidiumMod.EMPTY_TUICA_BOTTLE_1L) || stackInSlot.is(Items.BUCKET)) {
                    if (!this.moveItemStackTo(stackInSlot, 2, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (DionysianDistilleryBlockEntity.isFuel(stackInSlot)) {
                    if (!this.moveItemStackTo(stackInSlot, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < 32) {
                    if (!this.moveItemStackTo(stackInSlot, 32, 41, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 32 && !this.moveItemStackTo(stackInSlot, 5, 32, false)) {
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

