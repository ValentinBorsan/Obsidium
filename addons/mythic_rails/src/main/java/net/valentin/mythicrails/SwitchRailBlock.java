package net.valentin.mythicrails;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

/** Redstone-controlled switch rail that diverts between straight and curved track shapes. */
public final class SwitchRailBlock extends BaseRailBlock {
    public static final MapCodec<SwitchRailBlock> CODEC = simpleCodec(SwitchRailBlock::new);
    public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public SwitchRailBlock(Properties properties) {
        super(false, properties);
        registerDefaultState(stateDefinition.any()
                .setValue(SHAPE, RailShape.NORTH_SOUTH)
                .setValue(POWERED, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    public MapCodec<SwitchRailBlock> codec() {
        return CODEC;
    }

    @Override
    public Property<RailShape> getShapeProperty() {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE, POWERED, WATERLOGGED);
    }

    public void applyRemote(BlockState state, Level level, BlockPos pos) {
        var connector = SignalConnectorBlock.findAdjacentConnector(level, pos);
        boolean remoteOn = connector != null && connector.mode() == 1;
        boolean hasSignal = level.hasNeighborSignal(pos) || remoteOn;
        if (hasSignal != state.getValue(POWERED)) {
            RailShape next = toggledShape(state.getValue(SHAPE), level, pos, hasSignal);
            level.setBlock(pos, state.setValue(POWERED, hasSignal).setValue(SHAPE, next), Block.UPDATE_ALL);
        }
    }

    @Override
    protected BlockState updateState(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        var connector = SignalConnectorBlock.findAdjacentConnector(level, pos);
        boolean hasSignal = level.hasNeighborSignal(pos) || (connector != null && connector.mode() == 1);
        if (hasSignal != state.getValue(POWERED)) {
            RailShape next = toggledShape(state.getValue(SHAPE), level, pos, hasSignal);
            state = state.setValue(POWERED, hasSignal).setValue(SHAPE, next);
            level.setBlock(pos, state, Block.UPDATE_ALL);
        }
        return state;
    }

    @Override
    protected void updateState(BlockState state, Level level, BlockPos pos, Block neighborBlock) {
        applyRemote(state, level, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            RailShape next = cycleShape(state.getValue(SHAPE));
            level.setBlock(pos, state.setValue(SHAPE, next), Block.UPDATE_ALL);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static RailShape toggledShape(RailShape current, Level level, BlockPos pos, boolean powered) {
        if (powered) {
            if (current == RailShape.NORTH_SOUTH) {
                if (isRail(level, pos.east())) return RailShape.SOUTH_EAST;
                if (isRail(level, pos.west())) return RailShape.SOUTH_WEST;
                return RailShape.SOUTH_EAST;
            }
            if (current == RailShape.EAST_WEST) {
                if (isRail(level, pos.south())) return RailShape.SOUTH_EAST;
                if (isRail(level, pos.north())) return RailShape.NORTH_EAST;
                return RailShape.NORTH_EAST;
            }
            return current;
        } else {
            return switch (current) {
                case SOUTH_EAST, SOUTH_WEST, NORTH_EAST, NORTH_WEST ->
                        (isRail(level, pos.north()) && isRail(level, pos.south())) ? RailShape.NORTH_SOUTH
                                : (isRail(level, pos.east()) && isRail(level, pos.west())) ? RailShape.EAST_WEST
                                : RailShape.NORTH_SOUTH;
                default -> current;
            };
        }
    }

    private static RailShape cycleShape(RailShape current) {
        return switch (current) {
            case NORTH_SOUTH -> RailShape.EAST_WEST;
            case EAST_WEST -> RailShape.SOUTH_EAST;
            case SOUTH_EAST -> RailShape.SOUTH_WEST;
            case SOUTH_WEST -> RailShape.NORTH_WEST;
            case NORTH_WEST -> RailShape.NORTH_EAST;
            default -> RailShape.NORTH_SOUTH;
        };
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        RailShape shape = state.getValue(SHAPE);
        RailShape rotated = switch (rotation) {
            case CLOCKWISE_180 -> switch (shape) {
                case ASCENDING_EAST -> RailShape.ASCENDING_WEST;
                case ASCENDING_WEST -> RailShape.ASCENDING_EAST;
                case ASCENDING_NORTH -> RailShape.ASCENDING_SOUTH;
                case ASCENDING_SOUTH -> RailShape.ASCENDING_NORTH;
                case SOUTH_EAST -> RailShape.NORTH_WEST;
                case SOUTH_WEST -> RailShape.NORTH_EAST;
                case NORTH_WEST -> RailShape.SOUTH_EAST;
                case NORTH_EAST -> RailShape.SOUTH_WEST;
                default -> shape;
            };
            case COUNTERCLOCKWISE_90 -> switch (shape) {
                case NORTH_SOUTH -> RailShape.EAST_WEST;
                case EAST_WEST -> RailShape.NORTH_SOUTH;
                case SOUTH_EAST -> RailShape.NORTH_EAST;
                case SOUTH_WEST -> RailShape.SOUTH_EAST;
                case NORTH_WEST -> RailShape.SOUTH_WEST;
                case NORTH_EAST -> RailShape.NORTH_WEST;
                default -> shape;
            };
            case CLOCKWISE_90 -> switch (shape) {
                case NORTH_SOUTH -> RailShape.EAST_WEST;
                case EAST_WEST -> RailShape.NORTH_SOUTH;
                case SOUTH_EAST -> RailShape.SOUTH_WEST;
                case SOUTH_WEST -> RailShape.NORTH_WEST;
                case NORTH_WEST -> RailShape.NORTH_EAST;
                case NORTH_EAST -> RailShape.SOUTH_EAST;
                default -> shape;
            };
            default -> shape;
        };
        return state.setValue(SHAPE, rotated);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        RailShape shape = state.getValue(SHAPE);
        RailShape mirrored = switch (mirror) {
            case LEFT_RIGHT -> switch (shape) {
                case SOUTH_EAST -> RailShape.NORTH_EAST;
                case SOUTH_WEST -> RailShape.NORTH_WEST;
                case NORTH_WEST -> RailShape.SOUTH_WEST;
                case NORTH_EAST -> RailShape.SOUTH_EAST;
                default -> shape;
            };
            case FRONT_BACK -> switch (shape) {
                case SOUTH_EAST -> RailShape.SOUTH_WEST;
                case SOUTH_WEST -> RailShape.SOUTH_EAST;
                case NORTH_WEST -> RailShape.NORTH_EAST;
                case NORTH_EAST -> RailShape.NORTH_WEST;
                default -> shape;
            };
            default -> shape;
        };
        return state.setValue(SHAPE, mirrored);
    }
}
