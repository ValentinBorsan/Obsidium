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

public class AdvancedVolcaniteFurnaceMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    @Nullable
    private final AdvancedVolcaniteFurnaceBlockEntity blockEntity;

    public AdvancedVolcaniteFurnaceMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(AdvancedVolcaniteFurnaceBlockEntity.INVENTORY_SIZE),
                new SimpleContainerData(AdvancedVolcaniteFurnaceBlockEntity.DATA_COUNT), null);
    }

    public AdvancedVolcaniteFurnaceMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        this(syncId, playerInventory, container, data, null);
    }

    public AdvancedVolcaniteFurnaceMenu(int syncId, Inventory playerInventory, Container container, ContainerData data,
                                        @Nullable AdvancedVolcaniteFurnaceBlockEntity blockEntity) {
        super(ObsidiumMod.ADVANCED_VOLCANITE_FURNACE_MENU, syncId);
        checkContainerSize(container, AdvancedVolcaniteFurnaceBlockEntity.INVENTORY_SIZE);
        checkContainerDataCount(data, AdvancedVolcaniteFurnaceBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockEntity = blockEntity;
        container.startOpen(playerInventory.player);

        // 0, 1, 2: 3 Parallel Input Slots (42, 66, 90) at y = 20
        for (int c = 0; c < 3; c++) {
            this.addSlot(new Slot(container, c, 42 + c * 24, 20));
        }

        // 3, 4, 5: 3 Parallel Output Slots (42, 66, 90) at y = 60
        for (int c = 0; c < 3; c++) {
            this.addSlot(new Slot(container, 3 + c, 42 + c * 24, 60) {
                @Override
                public boolean mayPlace(ItemStack stack) { return false; }
            });
        }

        // 6: Shared Volcanic Ash Output (120, 60)
        this.addSlot(new Slot(container, 6, 120, 60) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });

        // 7 & 8: Upgrade Slots (152, 20 & 152, 42)
        for (int u = 0; u < 2; u++) {
            this.addSlot(new Slot(container, 7 + u, 152, 20 + u * 22) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return AdvancedVolcaniteFurnaceBlockEntity.isValidUpgrade(stack);
                }
            });
        }

        // Player Inventory (3x9) at (12, 102)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 12 + col * 18, 102 + row * 18));
            }
        }

        // Hotbar (1x9) at y = 160
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 12 + col * 18, 160));
        }

        this.addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.blockEntity == null || !stillValid(player)) return false;
        if (id == 0) {
            this.blockEntity.toggleAutoSplit();
            return true;
        }
        if (id == 1) {
            this.blockEntity.toggleAutoEject();
            return true;
        }
        return false;
    }

    public int getProgress(int chamber) { return this.data.get(chamber); }
    public int getEnergyAmount() { return (this.data.get(3) << 16) | (this.data.get(4) & 0xFFFF); }
    public boolean isThermal() { return this.data.get(5) == 1; }
    public int getCoilTemp() { return this.data.get(6); }
    public boolean isAutoSplit() { return this.data.get(7) != 0; }
    public boolean isAutoEject() { return this.data.get(8) != 0; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();

            if (index >= 3 && index <= 6) {
                if (!this.moveItemStackTo(itemStack2, 9, 45, true)) return ItemStack.EMPTY;
                slot.onQuickCraft(itemStack2, itemStack);
            } else if (index < 9) {
                if (!this.moveItemStackTo(itemStack2, 9, 45, false)) return ItemStack.EMPTY;
            } else {
                if (AdvancedVolcaniteFurnaceBlockEntity.isValidUpgrade(itemStack2)) {
                    if (!this.moveItemStackTo(itemStack2, 7, 9, false)) return ItemStack.EMPTY;
                } else if (!this.moveItemStackTo(itemStack2, 0, 3, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemStack2.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();

            if (itemStack2.getCount() == itemStack.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, itemStack2);
        }
        return itemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}
