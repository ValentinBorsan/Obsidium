package net.valentin.mythicrails;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Fluid or Energy rail station below a straight track; docks and transfers with modular carts. */
public final class RailUtilityStationBlock extends BaseEntityBlock {
    public enum StationKind {
        FLUID_LOADER(true, true),
        FLUID_UNLOADER(true, false),
        ENERGY_LOADER(false, true),
        ENERGY_UNLOADER(false, false);

        public final boolean isFluid;
        public final boolean isLoader;

        StationKind(boolean isFluid, boolean isLoader) {
            this.isFluid = isFluid;
            this.isLoader = isLoader;
        }
    }

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    public static final MapCodec<RailUtilityStationBlock> CODEC = simpleCodec(
            p -> new RailUtilityStationBlock(p, StationKind.FLUID_LOADER));

    private final StationKind kind;

    public RailUtilityStationBlock(Properties properties, StationKind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ENABLED, true));
    }

    public StationKind kind() { return kind; }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection()).setValue(ENABLED, true);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RailUtilityStationBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, MythicRails.UTILITY_STATION_BE, RailUtilityStationBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.openMenu(getMenuProvider(state, level, pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState old, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!old.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof RailUtilityStationBlockEntity station) {
            if (!level.isClientSide()) net.minecraft.world.Containers.dropContents(level, pos, station);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(old, level, pos, replacement, moving);
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new net.minecraft.world.SimpleMenuProvider(
                (id, inventory, player) -> new RailUtilityStationMenu(id, inventory, level, pos),
                getName());
    }
}
