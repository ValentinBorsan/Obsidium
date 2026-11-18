package net.valentin.mythicrails;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class FreightStationBlock extends HorizontalDirectionalBlock implements net.minecraft.world.level.block.EntityBlock {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty ENABLED = net.minecraft.world.level.block.state.properties.BooleanProperty.create("enabled");
    public static final MapCodec<FreightStationBlock> CODEC = simpleCodec(FreightStationBlock::new);
    public FreightStationBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ENABLED, true));
    }
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING, ENABLED);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,
            net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if(!level.isClientSide())player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, inventory, opener)->new FreightStationMenu(id,inventory,level,pos),getName()));
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());
    }
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(net.minecraft.core.BlockPos pos,BlockState state){return new FreightStationBlockEntity(pos,state);}
    @Override public <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            net.minecraft.world.level.Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type){
        return !level.isClientSide() && type==MythicRails.STATION_BE?(world,pos,block,entity)->FreightStationBlockEntity.serverTick(world,pos,block,(FreightStationBlockEntity)entity):null;
    }
    @Override protected void onRemove(BlockState old,net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,BlockState replacement,boolean moving){
        if(!old.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof FreightStationBlockEntity station){
            if(!level.isClientSide())net.minecraft.world.Containers.dropContents(level,pos,station);
            level.updateNeighbourForOutputSignal(pos,this);
        }
        super.onRemove(old,level,pos,replacement,moving);
    }
    public static boolean hasDepartureSignal(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos){
        for(var side:net.minecraft.core.Direction.values()){
            var other=pos.relative(side);
            if(!level.getChunkSource().hasChunk(other.getX()>>4,other.getZ()>>4))continue;
            var state=level.getBlockState(other);
            if(state.getSignal(level,other,side)>0)return true;
            if(state.isRedstoneConductor(level,other))for(var direct:net.minecraft.core.Direction.values()){
                var source=other.relative(direct);
                if(!source.equals(pos)&&level.getChunkSource().hasChunk(source.getX()>>4,source.getZ()>>4)
                        &&level.getBlockState(source).getDirectSignal(level,source,direct)>0)return true;
            }
        }
        return false;
    }
    public boolean isLoader() { return this == MythicRails.LOADING_STATION; }
}
