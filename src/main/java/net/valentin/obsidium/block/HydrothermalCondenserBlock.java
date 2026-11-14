package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
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
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;

public class HydrothermalCondenserBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public HydrothermalCondenserBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof HydrothermalCondenserBlockEntity condenser) {
            // Right-click with empty bucket to extract water directly
            if (stack.is(Items.BUCKET)) {
                if (condenser.getWaterAmount() >= 1000) {
                    if (!level.isClientSide()) {
                        condenser.extractWater(1000);
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                            ItemStack waterBucket = new ItemStack(Items.WATER_BUCKET);
                            if (stack.isEmpty()) {
                                player.setItemInHand(hand, waterBucket);
                            } else if (!player.getInventory().add(waterBucket)) {
                                player.drop(waterBucket, false);
                            }
                        }
                        level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                } else {
                    if (!level.isClientSide()) {
                        player.openMenu(condenser);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }

            // Normal right-click with any item in hand opens the machine
            if (!player.isShiftKeyDown()) {
                if (!level.isClientSide()) {
                    player.openMenu(condenser);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof HydrothermalCondenserBlockEntity condenser) {
                player.openMenu(condenser);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ObsidiumMod.HYDROTHERMAL_CONDENSER_BE ?
                (BlockEntityTicker<T>) (BlockEntityTicker<HydrothermalCondenserBlockEntity>) HydrothermalCondenserBlockEntity::tick : null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HydrothermalCondenserBlockEntity(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.85;
            double z = pos.getZ() + 0.5;

            // 1. Cool swirling vapor / mist from condenser intake
            if (random.nextDouble() < 0.45) {
                level.addParticle(ParticleTypes.CLOUD, x + (random.nextDouble() - 0.5) * 0.35, y + 0.1, z + (random.nextDouble() - 0.5) * 0.35, 0.0, 0.015, 0.0);
            }
            // 2. Condensed water dripping
            if (random.nextDouble() < 0.4) {
                level.addParticle(ParticleTypes.FALLING_DRIPSTONE_WATER, x + (random.nextDouble() - 0.5) * 0.4, y, z + (random.nextDouble() - 0.5) * 0.4, 0.0, -0.05, 0.0);
            }
            // 3. Splash droplets on cooling coils
            if (random.nextDouble() < 0.25) {
                level.addParticle(ParticleTypes.SPLASH, x + (random.nextDouble() - 0.5) * 0.3, y, z + (random.nextDouble() - 0.5) * 0.3, 0.0, 0.04, 0.0);
            }
            // 4. Subtle condensation sparkle
            if (random.nextDouble() < 0.15) {
                level.addParticle(ParticleTypes.WAX_OFF, x + (random.nextDouble() - 0.5) * 0.5, y + 0.1, z + (random.nextDouble() - 0.5) * 0.5, 0.0, 0.02, 0.0);
            }
            // 5. Ambient water drip sound
            if (random.nextDouble() < 0.09) {
                level.playLocalSound(x, y, z, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON, SoundSource.BLOCKS, 0.25F, 1.2F, false);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof HydrothermalCondenserBlockEntity condenser) {
                Containers.dropContents(level, pos, condenser);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}

