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

public class HydrothermalVitrifierMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public HydrothermalVitrifierMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(HydrothermalVitrifierBlockEntity.INVENTORY_SIZE), new SimpleContainerData(4));
    }

    public HydrothermalVitrifierMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(ObsidiumMod.HYDROTHERMAL_VITRIFIER_MENU, syncId);
        checkContainerSize(container, HydrothermalVitrifierBlockEntity.INVENTORY_SIZE);
        checkContainerDataCount(data, 4);
        this.container = container;
        this.data = data;

        // Slot 0: Stone Input (52, 22)
        this.addSlot(new Slot(container, 0, 52, 22) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return HydrothermalVitrifierBlockEntity.isStoneIngredient(stack);
            }
        });

        // Slot 1: Volcanic Ash Input (52, 52)
        this.addSlot(new Slot(container, 1, 52, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return HydrothermalVitrifierBlockEntity.isAshIngredient(stack);
            }
        });

        // Slot 2: Water Bucket or Dew Droplet In (24, 22)
        this.addSlot(new Slot(container, 2, 24, 22) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.WATER_BUCKET) || stack.is(ObsidiumMod.DEW_DROPLET);
            }
        });

        // Slot 3: Water Bucket Out (24, 52)
        this.addSlot(new Slot(container, 3, 24, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Slot 4: Lava Bucket or Magma Droplet In (134, 22)
        this.addSlot(new Slot(container, 4, 134, 22) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.LAVA_BUCKET) || stack.is(ObsidiumMod.MAGMA_DROPLET);
            }
        });

        // Slot 5: Lava Bucket Out (134, 52)
        this.addSlot(new Slot(container, 5, 134, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Slot 6: Obsidian Output (104, 26)
        this.addSlot(new Slot(container, 6, 104, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Slot 7: Rare Bonus Output (104, 50)
        this.addSlot(new Slot(container, 7, 104, 50) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Player Inventory (8, 84)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar (8, 142)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(data);
    }

    public int getWaterAmount() {
        return this.data.get(0);
    }

    public int getLavaAmount() {
        return this.data.get(1);
    }

    public int getProgress() {
        return this.data.get(2);
    }

    public int getMaxProgress() {
        return this.data.get(3);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (invSlot < HydrothermalVitrifierBlockEntity.INVENTORY_SIZE) {
                // Moving from machine to player
                if (!this.moveItemStackTo(originalStack, HydrothermalVitrifierBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player to machine
                if (originalStack.is(Items.WATER_BUCKET) || originalStack.is(ObsidiumMod.DEW_DROPLET)) {
                    if (!this.moveItemStackTo(originalStack, 2, 3, false)) return ItemStack.EMPTY;
                } else if (originalStack.is(Items.LAVA_BUCKET) || originalStack.is(ObsidiumMod.MAGMA_DROPLET)) {
                    if (!this.moveItemStackTo(originalStack, 4, 5, false)) return ItemStack.EMPTY;
                } else if (HydrothermalVitrifierBlockEntity.isStoneIngredient(originalStack)) {
                    if (!this.moveItemStackTo(originalStack, 0, 1, false)) return ItemStack.EMPTY;
                } else if (HydrothermalVitrifierBlockEntity.isAshIngredient(originalStack)) {
                    if (!this.moveItemStackTo(originalStack, 1, 2, false)) return ItemStack.EMPTY;
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return newStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}
