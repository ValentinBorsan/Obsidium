package net.valentin.obsidium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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

public class VolcaniteNetworkNodeEmitterBlock extends BaseEntityBlock {
    public static final MapCodec<VolcaniteNetworkNodeEmitterBlock> CODEC = simpleCodec(VolcaniteNetworkNodeEmitterBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    private static final VoxelShape BASE = Block.box(3, 0, 3, 13, 4, 13);
    private static final VoxelShape PILLAR = Block.box(5, 4, 5, 11, 12, 11);
    private static final VoxelShape DISH = Block.box(2, 12, 2, 14, 16, 14);
    private static final VoxelShape SHAPE = Shapes.or(BASE, PILLAR, DISH);

    public VolcaniteNetworkNodeEmitterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        return this.defaultBlockState().setValue(FACING, facing).setValue(LIT, false);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VolcaniteNetworkNodeEmitterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ObsidiumMod.VOLCANITE_NETWORK_NODE_EMITTER_BE, VolcaniteNetworkNodeEmitterBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VolcaniteNetworkNodeEmitterBlockEntity emitter) {
                VolcaniteStorageControllerBlockEntity ctrl = emitter.getConnectedController();
                if (ctrl != null && ctrl.isPowered()) {
                    int receivers = emitter.getActiveReceiverCount();
                    player.displayClientMessage(Component.literal("§6[Network Node Emitter]§r §aONLINE§r | Controller: §e" + ctrl.getBlockPos().toShortString() + "§r | Active Receivers: §b" + receivers + "§r | Range: §d" + VolcaniteNetworkNodeEmitterBlockEntity.RANGE + "m§r"), true);
                } else {
                    player.displayClientMessage(Component.literal("§6[Network Node Emitter]§r §cOFFLINE§r | No powered Storage Controller connected! Place adjacent to a Controller."), true);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.95;
            double z = pos.getZ() + 0.5;
            if (random.nextFloat() < 0.4f) {
                level.addParticle(ParticleTypes.PORTAL, x + (random.nextDouble() - 0.5) * 0.3, y, z + (random.nextDouble() - 0.5) * 0.3, 0, 0.05, 0);
            }
            if (random.nextFloat() < 0.2f) {
                level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.02, 0);
            }
        }
    }
}
