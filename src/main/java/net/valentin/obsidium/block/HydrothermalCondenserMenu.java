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

public class HydrothermalCondenserMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    @Nullable
    private final HydrothermalCondenserBlockEntity blockEntity;

    public HydrothermalCondenserMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(HydrothermalCondenserBlockEntity.INVENTORY_SIZE),
                new SimpleContainerData(HydrothermalCondenserBlockEntity.DATA_COUNT), null);
    }

    public HydrothermalCondenserMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        this(syncId, playerInventory, container, data, null);
    }

    public HydrothermalCondenserMenu(int syncId, Inventory playerInventory, Container container, ContainerData data,
                                     @Nullable HydrothermalCondenserBlockEntity blockEntity) {
        super(ObsidiumMod.HYDROTHERMAL_CONDENSER_MENU, syncId);
        checkContainerSize(container, HydrothermalCondenserBlockEntity.INVENTORY_SIZE);
        checkContainerDataCount(data, HydrothermalCondenserBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockEntity = blockEntity;

        // Slot 0: Moisture Input (44, 35)
        this.addSlot(new Slot(container, 0, 44, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return HydrothermalCondenserBlockEntity.isMoistureItem(stack);
            }
        });

        // Slot 1: Empty Bucket OR Cryo-Seed In (96, 52)
        this.addSlot(new Slot(container, 1, 96, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return HydrothermalCondenserBlockEntity.isCatalystOrBucketItem(stack);
            }
        });

        // Slot 2: Water Bucket / Ice / Packed Ice / Blue Ice Out (122, 52)
        this.addSlot(new Slot(container, 2, 122, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Slot 3: Dew Droplets Out (96, 22)
        this.addSlot(new Slot(container, 3, 96, 22) {
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

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.blockEntity == null || !stillValid(player)) return false;
        if (id == 0) {
            this.blockEntity.cycleOutputMode();
            return true;
        }
        return false;
    }

    public int getWaterAmount() {
        return this.data.get(0);
    }

    public int getProgress() {
        return this.data.get(1);
    }

    public int getMaxProgress() {
        return this.data.get(2);
    }

    public int getEfficiencyMultiplier() {
        return this.data.get(3);
    }

    public int getOutputMode() {
        return this.data.get(4);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (invSlot < HydrothermalCondenserBlockEntity.INVENTORY_SIZE) {
                if (!this.moveItemStackTo(originalStack, HydrothermalCondenserBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (HydrothermalCondenserBlockEntity.isCatalystOrBucketItem(originalStack)) {
                    if (!this.moveItemStackTo(originalStack, 1, 2, false)) {
                        if (!HydrothermalCondenserBlockEntity.isMoistureItem(originalStack)
                                || !this.moveItemStackTo(originalStack, 0, 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else if (HydrothermalCondenserBlockEntity.isMoistureItem(originalStack)) {
                    if (!this.moveItemStackTo(originalStack, 0, 1, false)) return ItemStack.EMPTY;
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

