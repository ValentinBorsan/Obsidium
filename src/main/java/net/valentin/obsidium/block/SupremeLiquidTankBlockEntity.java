package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;

public class SupremeLiquidTankBlockEntity extends LiquidTankBlockEntity {
    public SupremeLiquidTankBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.SUPREME_LIQUID_TANK_BE, pos, state);
        this.maxCapacity = 1600000; // 1600 buckets
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.supreme_liquid_tank");
    }
}
