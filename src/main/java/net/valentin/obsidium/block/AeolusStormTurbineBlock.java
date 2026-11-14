package net.valentin.obsidium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AeolusStormTurbineBlock extends BaseEntityBlock {
    public static final MapCodec<AeolusStormTurbineBlock> CODEC = simpleCodec(AeolusStormTurbineBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty STORM = BooleanProperty.create("storm");

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0),
            Block.box(1.0, 4.0, 1.0, 15.0, 13.0, 15.0),
            Block.box(0.0, 13.0, 0.0, 16.0, 15.0, 16.0),
            Block.box(5.0, 15.0, 5.0, 11.0, 16.0, 11.0)
    );

    public AeolusStormTurbineBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ACTIVE, false)
                .setValue(STORM, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE, STORM);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) return;

        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.95;
        double cz = pos.getZ() + 0.5;

        if (state.getValue(STORM)) {
            for (int i = 0; i < 3; i++) {
                double ox = (random.nextDouble() - 0.5) * 0.9;
                double oy = (random.nextDouble() - 0.3) * 0.6;
                double oz = (random.nextDouble() - 0.5) * 0.9;
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, cx + ox, cy + oy, cz + oz, 0.0, 0.05, 0.0);
            }
        } else if (random.nextFloat() < 0.45f) {
            double ox = (random.nextDouble() - 0.5) * 0.8;
            double oz = (random.nextDouble() - 0.5) * 0.8;
            level.addParticle(ParticleTypes.CLOUD, cx + ox, cy, cz + oz, 0.02, 0.01, 0.02);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§b⚡ Atmospheric Wind & Lightning Generator"));
        tooltip.add(Component.literal("§7Harnesses wind currents, altitude, rain & thunder:"));
        tooltip.add(Component.literal("  §a• Altitude Wind: §fUp to §e2.5× §fat high Y-levels"));
        tooltip.add(Component.literal("  §b• Biome Bonus: §fPeaks & Windswept (§a1.75×§f), Ocean (§a1.4×§f)"));
        tooltip.add(Component.literal("  §9• Weather Multipliers: §fRain (§b2.2×§f) | Thunderstorm (§d4.5×§f)"));
        tooltip.add(Component.literal("  §e• Astrape Lightning Surge: §f+75,000 RF burst & §e+2,000 RF/t"));
        tooltip.add(Component.literal("§8Tip: Place a Lightning Rod on top to attract more strikes!"));
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AeolusStormTurbineBlockEntity turbine && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(turbine);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof AeolusStormTurbineBlockEntity turbine) {
                Containers.dropContents(level, pos, turbine.getInventory());
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AeolusStormTurbineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ObsidiumMod.AEOLUS_STORM_TURBINE_BE, AeolusStormTurbineBlockEntity::tick);
    }
}

