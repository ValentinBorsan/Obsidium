package net.valentin.obsidium.block;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.PyrogenicBloomInfuserItem;

public class GeothermalArboristMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    public GeothermalArboristMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(GeothermalArboristBlockEntity.INVENTORY_SIZE), new SimpleContainerData(9));
    }

    public GeothermalArboristMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ObsidiumMod.GEOTHERMAL_ARBORIST_MENU, containerId);
        checkContainerSize(container, GeothermalArboristBlockEntity.INVENTORY_SIZE);
        this.container = container;
        this.data = data;
        container.startOpen(playerInventory.player);

        // Slots:
        // 0, 1: Seeds / Saplings inputs
        this.addSlot(new Slot(container, 0, 36, 17));
        this.addSlot(new Slot(container, 1, 56, 17));

        // 2, 3: Lava Bucket input / output
        this.addSlot(new Slot(container, 2, 36, 36));
        this.addSlot(new Slot(container, 3, 56, 36));

        // 4..8: Harvest outputs; 9: reserved hybrid sapling, manually collectable only
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(container, 4 + col + row * 3, 116 + col * 18, 26 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }

        // 10: Auto-Hoe Upgrade
        this.addSlot(new Slot(container, 10, 85, 82) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof HoeItem;
            }
        });

        // 11: Bio-Thermal Catalyst (Pyrogenic Bloom Infuser)
        this.addSlot(new Slot(container, 11, 56, 82) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof PyrogenicBloomInfuserItem;
            }
        });

        // 12: Dedicated Fertilizer Bay (Accepts Bone Meal, Volcanic Ash, etc.)
        this.addSlot(new Slot(container, 12, 36, 82) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return GeothermalArboristBlockEntity.isFertilizer(stack);
            }
        });

        // Player Inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 108 + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 166));
        }

        this.addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            // Radius button
            int current = data.get(3);
            int next = current + 1;
            if (next > 4) next = 1;
            data.set(3, next);
            return true;
        } else if (id == 1) {
            // Mode button
            int current = data.get(4);
            int next = (current + 1) % 4;
            data.set(4, next);
            return true;
        } else if (id == 2) {
            data.set(7, (data.get(7) + 1) % 2);
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    public int getLavaAmount() {
        return this.data.get(0);
    }

    public long getEnergy() {
        return ((long) this.data.get(2) << 16) | ((long) this.data.get(1) & 0xFFFFL);
    }

    public int getRadius() {
        return this.data.get(3);
    }

    public int getMode() {
        return this.data.get(4);
    }

    public boolean hasTillerUpgrade() {
        return this.data.get(5) == 1;
    }

    public boolean hasBloomInfuser() {
        return this.data.get(6) == 1;
    }

    public int getMutationSelection() {
        return this.data.get(7);
    }

    public int getMutationProgress() {
        return this.data.get(8);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            int machineSlotCount = GeothermalArboristBlockEntity.INVENTORY_SIZE; // 13

            if (index < machineSlotCount) {
                // Move from machine to player
                if (!this.moveItemStackTo(itemstack1, machineSlotCount, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from player to appropriate machine slots
                if (itemstack1.getItem() instanceof HoeItem) {
                    if (!this.moveItemStackTo(itemstack1, 10, 11, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (itemstack1.getItem() instanceof PyrogenicBloomInfuserItem) {
                    if (!this.moveItemStackTo(itemstack1, 11, 12, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (hasBloomInfuser() && GeothermalArboristBlockEntity.isFertilizer(itemstack1)) {
                    if (!this.moveItemStackTo(itemstack1, 12, 13, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (itemstack1.is(Items.LAVA_BUCKET) || itemstack1.is(Items.BUCKET) || itemstack1.getItem() instanceof net.valentin.obsidium.item.VolcaniteBottleItem) {
                    if (!this.moveItemStackTo(itemstack1, 2, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // Seed / sapling inputs
                    if (!this.moveItemStackTo(itemstack1, 0, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}

