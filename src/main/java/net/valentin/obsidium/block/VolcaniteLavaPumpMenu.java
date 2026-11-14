package net.valentin.obsidium.block;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.ObsidiumMod;

public class VolcaniteLavaPumpMenu extends AbstractContainerMenu {
    private final ContainerData data;

    public VolcaniteLavaPumpMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainerData(3));
    }

    public VolcaniteLavaPumpMenu(int syncId, Inventory playerInventory, ContainerData data) {
        super(ObsidiumMod.VOLCANITE_LAVA_PUMP_MENU, syncId);
        this.data = data;
        this.addDataSlots(data);
        
        // Player Inventory
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, k, 8 + k * 18, 142));
        }
    }

    public int getLavaAmount() { return this.data.get(0); }
    public int getEnergyAmount() { return (this.data.get(1) & 0xFFFF) | (this.data.get(2) << 16); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
