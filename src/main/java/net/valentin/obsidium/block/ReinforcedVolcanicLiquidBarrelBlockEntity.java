package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;

public class ReinforcedVolcanicLiquidBarrelBlockEntity extends LiquidTankBlockEntity {
    public static final int REINFORCED_BARREL_CAPACITY = 256000; // 256 Buckets (256,000 mB)

    public ReinforcedVolcanicLiquidBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.REINFORCED_VOLCANIC_LIQUID_BARREL_BE, pos, state);
        this.maxCapacity = REINFORCED_BARREL_CAPACITY;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.reinforced_volcanic_liquid_barrel");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new LiquidTankMenu(syncId, playerInventory, this.getInventory(), this.data);
    }
}

