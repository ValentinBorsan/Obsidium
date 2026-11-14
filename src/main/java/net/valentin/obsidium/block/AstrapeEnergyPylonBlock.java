package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AstrapeEnergyPylonBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    // Voxel shapes oriented to each direction
    protected static final VoxelShape SHAPE_UP = Shapes.or(
            Block.box(3.0, 0.0, 3.0, 13.0, 3.0, 13.0),
            Block.box(5.0, 3.0, 5.0, 11.0, 15.0, 11.0)
    );
    protected static final VoxelShape SHAPE_DOWN = Shapes.or(
            Block.box(3.0, 13.0, 3.0, 13.0, 16.0, 13.0),
            Block.box(5.0, 1.0, 5.0, 11.0, 13.0, 11.0)
    );
    protected static final VoxelShape SHAPE_NORTH = Shapes.or(
            Block.box(3.0, 3.0, 13.0, 13.0, 13.0, 16.0),
            Block.box(5.0, 5.0, 1.0, 11.0, 11.0, 13.0)
    );
    protected static final VoxelShape SHAPE_SOUTH = Shapes.or(
            Block.box(3.0, 3.0, 0.0, 13.0, 13.0, 3.0),
            Block.box(5.0, 5.0, 3.0, 11.0, 11.0, 15.0)
    );
    protected static final VoxelShape SHAPE_WEST = Shapes.or(
            Block.box(13.0, 3.0, 3.0, 16.0, 13.0, 13.0),
            Block.box(1.0, 5.0, 5.0, 13.0, 11.0, 11.0)
    );
    protected static final VoxelShape SHAPE_EAST = Shapes.or(
            Block.box(0.0, 3.0, 3.0, 3.0, 13.0, 13.0),
            Block.box(3.0, 5.0, 5.0, 15.0, 11.0, 11.0)
    );

    public AstrapeEnergyPylonBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.UP)
                .setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> SHAPE_DOWN;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            default -> SHAPE_UP;
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AstrapeEnergyPylonBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ObsidiumMod.ASTRAPE_ENERGY_PYLON_BE ?
                (lvl, pos, st, be) -> {
                    if (be instanceof AstrapeEnergyPylonBlockEntity pylonBe) {
                        AstrapeEnergyPylonBlockEntity.tick(lvl, pos, st, pylonBe);
                    }
                } : null;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AstrapeEnergyPylonBlockEntity pylonBe) {
                pylonBe.clearAllLinks();
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof AstrapeEnergyPylonBlockEntity pylon) {
            long amt = pylon.energyStorage.amount;
            long cap = pylon.energyStorage.capacity;
            if (cap <= 0 || amt <= 0) return 0;
            return (int) Math.floor((double) amt / cap * 14.0) + 1;
        }
        return 0;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6★ Astrape Wireless Energy Pylon"));
        tooltip.add(Component.literal("§7Transmits high-voltage laser flux across open air."));
        tooltip.add(Component.literal("§eMax Range: §f48 blocks §7| §eMax Links: §f6 pylons"));
        tooltip.add(Component.literal("§aTransfer Rate: §f50,000 RF/t per link"));
        tooltip.add(Component.literal("§8Link together using the Supreme Link Tool."));
    }
}

