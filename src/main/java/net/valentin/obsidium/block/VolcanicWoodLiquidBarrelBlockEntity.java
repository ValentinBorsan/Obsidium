package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;

public class VolcanicWoodLiquidBarrelBlockEntity extends LiquidTankBlockEntity {
    public static final int VOLCANIC_BARREL_CAPACITY = 64000; // 64 Buckets (64,000 mB)

    public VolcanicWoodLiquidBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.VOLCANIC_WOOD_LIQUID_BARREL_BE, pos, state);
        this.maxCapacity = VOLCANIC_BARREL_CAPACITY;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.volcanic_wood_liquid_barrel");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new LiquidTankMenu(syncId, playerInventory, this.getInventory(), this.data);
    }
}

