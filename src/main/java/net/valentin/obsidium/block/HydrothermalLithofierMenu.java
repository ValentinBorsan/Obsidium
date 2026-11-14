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
import net.valentin.obsidium.item.VolcanitePipeEnergyUpgradeItem;

public class HydrothermalLithofierMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public HydrothermalLithofierMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(HydrothermalLithofierBlockEntity.INVENTORY_SIZE), new SimpleContainerData(7));
    }

    public HydrothermalLithofierMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(ObsidiumMod.HYDROTHERMAL_LITHOFIER_MENU, syncId);
        checkContainerSize(container, HydrothermalLithofierBlockEntity.INVENTORY_SIZE);
        checkContainerDataCount(data, 7);
        this.container = container;
        this.data = data;

        // Slot 0: Water Droplet / Bucket In (28, 23)
        this.addSlot(new Slot(container, 0, 28, 23) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.WATER_BUCKET) || stack.is(ObsidiumMod.DEW_DROPLET);
            }
        });

        // Slot 1: Water Bucket Out (28, 51)
        this.addSlot(new Slot(container, 1, 28, 51) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Slot 2: Lava Droplet / Bucket In (132, 23)
        this.addSlot(new Slot(container, 2, 132, 23) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.LAVA_BUCKET) || stack.is(ObsidiumMod.MAGMA_DROPLET);
            }
        });

        // Slot 3: Lava Bucket Out (132, 51)
        this.addSlot(new Slot(container, 3, 132, 51) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Slot 4: Rock Output (102, 48)
        this.addSlot(new Slot(container, 4, 102, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Slot 5: Energy / Speed Upgrade (56, 48)
        this.addSlot(new Slot(container, 5, 56, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof VolcanitePipeEnergyUpgradeItem;
            }
        });

        // Player Inventory (8, 84)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar (8, 142)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id <= 3) {
            this.data.set(6, id);
            return true;
        }
        return false;
    }

    public int getWater() { return data.get(0); }
    public int getLava() { return data.get(1); }
    public int getProgress() { return data.get(2); }
    public int getMaxProgress() { return Math.max(1, data.get(3)); }
    public int getEnergy() { return data.get(4); }
    public int getMaxEnergy() { return Math.max(1, data.get(5)); }
    public int getMode() { return data.get(6); }

    public int getScaledProgress() {
        int p = getProgress();
        int max = getMaxProgress();
        return max != 0 && p != 0 ? p * 24 / max : 0;
    }

    public int getScaledWater() {
        return getWater() * 52 / HydrothermalLithofierBlockEntity.MAX_WATER;
    }

    public int getScaledLava() {
        return getLava() * 52 / HydrothermalLithofierBlockEntity.MAX_LAVA;
    }

    public int getScaledEnergy() {
        return (int) ((long) getEnergy() * 52L / (long) getMaxEnergy());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (invSlot < HydrothermalLithofierBlockEntity.INVENTORY_SIZE) {
                // Moving from machine to player
                if (!this.moveItemStackTo(originalStack, HydrothermalLithofierBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player to machine
                if (originalStack.getItem() instanceof VolcanitePipeEnergyUpgradeItem) {
                    if (!this.moveItemStackTo(originalStack, 5, 6, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (originalStack.is(Items.WATER_BUCKET) || originalStack.is(ObsidiumMod.DEW_DROPLET)) {
                    if (!this.moveItemStackTo(originalStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (originalStack.is(Items.LAVA_BUCKET) || originalStack.is(ObsidiumMod.MAGMA_DROPLET)) {
                    if (!this.moveItemStackTo(originalStack, 2, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= HydrothermalLithofierBlockEntity.INVENTORY_SIZE && invSlot < 33 + HydrothermalLithofierBlockEntity.INVENTORY_SIZE) {
                    if (!this.moveItemStackTo(originalStack, 33 + HydrothermalLithofierBlockEntity.INVENTORY_SIZE, this.slots.size(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= 33 + HydrothermalLithofierBlockEntity.INVENTORY_SIZE && invSlot < this.slots.size()) {
                    if (!this.moveItemStackTo(originalStack, HydrothermalLithofierBlockEntity.INVENTORY_SIZE, 33 + HydrothermalLithofierBlockEntity.INVENTORY_SIZE, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (originalStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
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
}

