package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import net.valentin.obsidium.ObsidiumMod;

public class CreativeLavaTankBlock extends LiquidTankBlock {
    public CreativeLavaTankBlock(Properties properties) { super(properties); }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeLavaTankBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ObsidiumMod.CREATIVE_LAVA_TANK_BE ? 
            (BlockEntityTicker<T>) (BlockEntityTicker<CreativeLavaTankBlockEntity>) CreativeLavaTankBlockEntity::creativeTick : null;
    }
}
