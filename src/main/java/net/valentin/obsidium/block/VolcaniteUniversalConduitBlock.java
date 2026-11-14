package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import net.valentin.obsidium.ObsidiumMod;

public class VolcaniteUniversalConduitBlock extends VolcaniteStoragePipeBlock {
    public VolcaniteUniversalConduitBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VolcaniteUniversalConduitBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ObsidiumMod.VOLCANITE_UNIVERSAL_CONDUIT_BE ? 
            (BlockEntityTicker<T>) (BlockEntityTicker<VolcaniteUniversalConduitBlockEntity>) VolcaniteUniversalConduitBlockEntity::tick : null;
    }
}
