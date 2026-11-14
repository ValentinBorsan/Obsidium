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

public class VolcaniteNetworkNodeReceiverBlock extends BaseEntityBlock {
    public static final MapCodec<VolcaniteNetworkNodeReceiverBlock> CODEC = simpleCodec(VolcaniteNetworkNodeReceiverBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    private static final VoxelShape BASE = Block.box(3, 0, 3, 13, 4, 13);
    private static final VoxelShape CORE = Block.box(4, 4, 4, 12, 12, 12);
    private static final VoxelShape SATELLITE = Block.box(2, 12, 2, 14, 16, 14);
    private static final VoxelShape SHAPE = Shapes.or(BASE, CORE, SATELLITE);

    public VolcaniteNetworkNodeReceiverBlock(Properties properties) {
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
        return new VolcaniteNetworkNodeReceiverBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ObsidiumMod.VOLCANITE_NETWORK_NODE_RECEIVER_BE, VolcaniteNetworkNodeReceiverBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VolcaniteNetworkNodeReceiverBlockEntity receiver) {
                // Sneak + Right click toggles chunk loading
                if (player.isShiftKeyDown()) {
                    boolean enabled = receiver.toggleChunkLoading();
                    if (enabled) {
                        player.displayClientMessage(Component.literal("§6[Network Node Receiver] §a✔ Chunk Loader Module: §eACTIVE §7(Chunk " + (pos.getX() >> 4) + ", " + (pos.getZ() >> 4) + " kept 24/7 loaded)"), true);
                        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.6F);
                    } else {
                        player.displayClientMessage(Component.literal("§6[Network Node Receiver] §c✖ Chunk Loader Module: §7DISABLED"), true);
                        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BEACON_DEACTIVATE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.2F);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide());
                }

                VolcaniteNetworkNodeEmitterBlockEntity emitter = receiver.getConnectedEmitter();
                String chunkStatus = receiver.isChunkLoading() ? "§aACTIVE§r" : "§7OFF§r";
                if (emitter != null && emitter.isLinked()) {
                    BlockPos emitPos = emitter.getBlockPos();
                    int dist = (int) Math.sqrt(pos.distSqr(emitPos));
                    int signal = Math.max(10, 100 - (dist * 100 / VolcaniteNetworkNodeReceiverBlockEntity.SUPREME_RANGE));
                    player.displayClientMessage(Component.literal("§6[Network Node Receiver]§r §aONLINE§r | Emitter: §e" + emitPos.toShortString() + " §r(§b" + dist + "m§r) | Signal: §a" + signal + "%§r | Chunk Loader: " + chunkStatus + " §8[Sneak+R-Click to toggle]"), true);
                } else {
                    player.displayClientMessage(Component.literal("§6[Network Node Receiver]§r §cOFFLINE§r | Searching for Emitter within " + VolcaniteNetworkNodeReceiverBlockEntity.SUPREME_RANGE + "m... | Chunk Loader: " + chunkStatus + " §8[Sneak+R-Click to toggle]"), true);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VolcaniteNetworkNodeReceiverBlockEntity receiver) {
                receiver.disableChunkLoading();
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.95;
            double z = pos.getZ() + 0.5;
            if (random.nextFloat() < 0.35f) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, x + (random.nextDouble() - 0.5) * 0.4, y, z + (random.nextDouble() - 0.5) * 0.4, 0, 0.04, 0);
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VolcaniteNetworkNodeReceiverBlockEntity receiver && receiver.isChunkLoading()) {
                if (random.nextFloat() < 0.4f) {
                    level.addParticle(ParticleTypes.PORTAL, x + (random.nextDouble() - 0.5) * 0.3, y, z + (random.nextDouble() - 0.5) * 0.3, 0, 0.05, 0);
                }
            }
        }
    }
}
