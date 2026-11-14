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
import net.valentin.obsidium.item.VolcaniteOverclockUpgradeItem;
import net.valentin.obsidium.item.VolcanitePipeEnergyUpgradeItem;

public class VolcaniteFurnaceMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public VolcaniteFurnaceMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(6), new SimpleContainerData(6));
    }

    public VolcaniteFurnaceMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(ObsidiumMod.VOLCANITE_FURNACE_MENU, syncId);
        this.container = container;
        this.data = data;
        checkContainerSize(container, 6);
        checkContainerDataCount(data, 6);
        container.startOpen(playerInventory.player);

        // 0: Input (48, 22)
        this.addSlot(new Slot(container, 0, 48, 22));
        // 1: Fuel (48, 52)
        this.addSlot(new Slot(container, 1, 48, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return VolcaniteFurnaceBlockEntity.isFuel(stack);
            }
        });
        // 2: Alloy Output (112, 24)
        this.addSlot(new Slot(container, 2, 112, 24) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });
        // 3: Ash Output (112, 48)
        this.addSlot(new Slot(container, 3, 112, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });

        // 4 & 5: Upgrade Slots (148, 22 & 148, 48)
        this.addSlot(new Slot(container, 4, 148, 22) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof VolcanitePipeEnergyUpgradeItem
                        || VolcaniteOverclockUpgradeItem.isOverclockUpgrade(stack);
            }
        });
        this.addSlot(new Slot(container, 5, 148, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof VolcanitePipeEnergyUpgradeItem
                        || VolcaniteOverclockUpgradeItem.isOverclockUpgrade(stack);
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

    public int getProgress() { return this.data.get(0); }
    public int getBurnTime() { return this.data.get(1); }
    public int getTotalBurnTime() { return this.data.get(2); }
    public int getEnergyAmount() { return (this.data.get(3) << 16) | (this.data.get(4) & 0xFFFF); }
    public boolean isThermal() { return this.data.get(5) == 1; }
    public boolean isBurning() { return getBurnTime() > 0 || isThermal() || (getProgress() > 0 && getEnergyAmount() >= 80); }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemStack = slotStack.copy();

            if (index == 2 || index == 3) {
                if (!this.moveItemStackTo(slotStack, 6, 42, true)) return ItemStack.EMPTY;
                slot.onQuickCraft(slotStack, itemStack);
            } else if (index < 6) {
                if (!this.moveItemStackTo(slotStack, 6, 42, false)) return ItemStack.EMPTY;
            } else {
                if (slotStack.getItem() instanceof VolcanitePipeEnergyUpgradeItem
                        || VolcaniteOverclockUpgradeItem.isOverclockUpgrade(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, 4, 6, false)) return ItemStack.EMPTY;
                } else if (VolcaniteFurnaceBlockEntity.isFuel(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, 1, 2, false)) {
                        if (!this.moveItemStackTo(slotStack, 0, 1, false)) return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.moveItemStackTo(slotStack, 0, 1, false)) return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();

            if (slotStack.getCount() == itemStack.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, slotStack);
        }
        return itemStack;
    }
}
