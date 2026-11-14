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

public class VolcanicDistillationChamberMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public VolcanicDistillationChamberMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(5), new SimpleContainerData(10));
    }

    public VolcanicDistillationChamberMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(ObsidiumMod.VOLCANIC_DISTILLATION_CHAMBER_MENU, syncId);
        checkContainerSize(container, 5);
        checkContainerDataCount(data, 10);
        this.container = container;
        this.data = data;

        // Slot 0: Lava / Biofuel / Ethanol Bucket / Bottle input (8, 22)
        this.addSlot(new Slot(container, 0, 8, 22) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return VolcanicDistillationChamberBlockEntity.isValidInputBucket(stack);
            }
        });

        // Slot 1: Empty Container output (8, 52)
        this.addSlot(new Slot(container, 1, 8, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Slot 2: Chemical Reagent / Catalyst / Bio-Ethanol Bucket input (72, 20)
        this.addSlot(new Slot(container, 2, 72, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return VolcanicDistillationChamberBlockEntity.isValidReagent(stack);
            }
        });

        // Slot 3: Empty Bucket / Glass Bottle input for bottling Output (122, 22)
        this.addSlot(new Slot(container, 3, 122, 22) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.BUCKET) || stack.is(Items.GLASS_BOTTLE);
            }
        });

        // Slot 4: Filled Distillate / Biodiesel / Pyro-Diesel Bucket output (122, 52)
        this.addSlot(new Slot(container, 4, 122, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

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

    public int getLavaAmount() {
        return this.data.get(0);
    }

    public int getDistillateAmount() {
        return this.data.get(1);
    }

    public int getProgress() {
        return this.data.get(2);
    }

    public int getMaxProgress() {
        return Math.max(1, this.data.get(3));
    }

    public int getEnergyAmount() {
        return (this.data.get(5) << 16) | (this.data.get(4) & 0xFFFF);
    }

    public int getInputFluidTypeId() {
        return this.data.get(6);
    }

    public int getOutputFluidTypeId() {
        return this.data.get(7);
    }

    public int getEthanolAmount() {
        return this.data.get(8);
    }

    public int getSecondaryFluidTypeId() {
        return this.data.get(9);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemStack = slotStack.copy();

            if (index < 5) {
                if (!this.moveItemStackTo(slotStack, 5, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (VolcanicDistillationChamberBlockEntity.isValidInputBucket(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (VolcanicDistillationChamberBlockEntity.isValidReagent(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, 2, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotStack.is(Items.BUCKET) || slotStack.is(Items.GLASS_BOTTLE)) {
                    if (!this.moveItemStackTo(slotStack, 3, 4, false)) {
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

