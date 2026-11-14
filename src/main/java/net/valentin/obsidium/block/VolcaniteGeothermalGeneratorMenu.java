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

public class VolcaniteGeothermalGeneratorMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public VolcaniteGeothermalGeneratorMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(2), new SimpleContainerData(5));
    }

    public VolcaniteGeothermalGeneratorMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(ObsidiumMod.VOLCANITE_GEOTHERMAL_GENERATOR_MENU, syncId);
        checkContainerSize(container, 2);
        checkContainerDataCount(data, 5);
        this.container = container;
        this.data = data;

        // Slot 0: Lava Bucket input
        this.addSlot(new Slot(container, 0, 34, 25));
        // Slot 1: Bucket output
        this.addSlot(new Slot(container, 1, 34, 56) {
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

    public int getPassiveRate() {
        return this.data.get(1);
    }

    public boolean isBoostEnabled() {
        return this.data.get(2) == 1;
    }

    public int getEnergyAmount() {
        return (this.data.get(4) << 16) | (this.data.get(3) & 0xFFFF);
    }

    public int getTotalRate() {
        return getPassiveRate() + (isBoostEnabled() && getLavaAmount() >= 2 ? VolcaniteGeothermalGeneratorBlockEntity.ACTIVE_BOOST_RATE : 0);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            this.data.set(2, this.isBoostEnabled() ? 0 : 1);
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemStack = slotStack.copy();

            if (index < 2) {
                if (!this.moveItemStackTo(slotStack, 2, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (slotStack.is(Items.LAVA_BUCKET) || slotStack.getItem() instanceof net.valentin.obsidium.item.VolcaniteBottleItem) {
                    if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
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
