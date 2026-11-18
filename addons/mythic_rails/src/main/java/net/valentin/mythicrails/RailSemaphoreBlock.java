package net.valentin.mythicrails;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Railway semaphore signal that holds carts when redstone-locked or when a cart occupies the track ahead. */
public final class RailSemaphoreBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<RailSemaphoreBlock> CODEC = simpleCodec(RailSemaphoreBlock::new);
    public static final BooleanProperty RED = BooleanProperty.create("red");
    private static final VoxelShape SHAPE = box(5, 0, 5, 11, 16, 11);
    private static final int SCAN_DISTANCE = 16;

    public RailSemaphoreBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(RED, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, RED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection()).setValue(RED, false);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !oldState.is(this)) {
            updateSignal(state, level, pos);
            level.scheduleTick(pos, this, 10);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            updateSignal(state, level, pos);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        updateSignal(state, level, pos);
        level.scheduleTick(pos, this, 10);
    }

    public void applyRemote(BlockState state, Level level, BlockPos pos) {
        updateSignal(state, level, pos);
    }

    private void updateSignal(BlockState state, Level level, BlockPos pos) {
        boolean red;
        var connector = SignalConnectorBlock.findAdjacentConnector(level, pos);
        if (connector != null && connector.mode() == 1) {
            red = true;
        } else if (connector != null && connector.mode() == 2) {
            red = false;
        } else {
            red = level.hasNeighborSignal(pos) || isTrackAheadOccupied(level, pos, state.getValue(FACING));
        }
        if (red != state.getValue(RED)) {
            level.setBlock(pos, state.setValue(RED, red), Block.UPDATE_ALL);
        }
    }

    private static boolean isTrackAheadOccupied(Level level, BlockPos semaphorePos, Direction facing) {
        BlockPos railPos = findAdjacentRail(level, semaphorePos);
        if (railPos == null) return false;
        for (int step = 1; step <= SCAN_DISTANCE; step++) {
            BlockPos checkPos = railPos.relative(facing, step);
            if (!BaseRailBlock.isRail(level, checkPos)) {
                if (BaseRailBlock.isRail(level, checkPos.above())) checkPos = checkPos.above();
                else if (BaseRailBlock.isRail(level, checkPos.below())) checkPos = checkPos.below();
                else break;
            }
            AABB box = new AABB(checkPos).inflate(0.15, 0.5, 0.15);
            if (!level.getEntitiesOfClass(AbstractMinecart.class, box, EntitySelector.ENTITY_STILL_ALIVE).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static BlockPos findAdjacentRail(Level level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = pos.relative(dir);
            if (BaseRailBlock.isRail(level, candidate)) return candidate;
        }
        return null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
