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

public class MagmaticCrucibleBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public MagmaticCrucibleBlock(Properties properties) {
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
        if (be instanceof MagmaticCrucibleBlockEntity crucible) {
            // Right-click with empty bucket to extract lava directly
            if (stack.is(Items.BUCKET)) {
                if (crucible.getLavaAmount() >= 1000) {
                    if (!level.isClientSide()) {
                        crucible.extractLava(1000);
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                            ItemStack lavaBucket = new ItemStack(Items.LAVA_BUCKET);
                            if (stack.isEmpty()) {
                                player.setItemInHand(hand, lavaBucket);
                            } else if (!player.getInventory().add(lavaBucket)) {
                                player.drop(lavaBucket, false);
                            }
                        }
                        level.playSound(null, pos, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                } else {
                    if (!level.isClientSide()) {
                        player.openMenu(crucible);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }

            // Normal right-click with any item in hand opens the machine
            if (!player.isShiftKeyDown()) {
                if (!level.isClientSide()) {
                    player.openMenu(crucible);
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
            if (blockEntity instanceof MagmaticCrucibleBlockEntity crucible) {
                player.openMenu(crucible);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ObsidiumMod.MAGMATIC_CRUCIBLE_BE ?
                (BlockEntityTicker<T>) (BlockEntityTicker<MagmaticCrucibleBlockEntity>) MagmaticCrucibleBlockEntity::tick : null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MagmaticCrucibleBlockEntity(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.95;
            double z = pos.getZ() + 0.5;

            // 1. Boiling lava bursts and crackling sparks
            if (random.nextDouble() < 0.35) {
                level.addParticle(ParticleTypes.LAVA, x + (random.nextDouble() - 0.5) * 0.35, y, z + (random.nextDouble() - 0.5) * 0.35, 0.0, 0.04, 0.0);
            }
            // 2. Swirling intense crucible flames
            if (random.nextDouble() < 0.45) {
                level.addParticle(ParticleTypes.FLAME, x + (random.nextDouble() - 0.5) * 0.3, y, z + (random.nextDouble() - 0.5) * 0.3, 0.0, 0.015, 0.0);
            }
            // 3. Molten heat smoke
            if (random.nextDouble() < 0.4) {
                level.addParticle(ParticleTypes.SMOKE, x + (random.nextDouble() - 0.5) * 0.4, y + 0.1, z + (random.nextDouble() - 0.5) * 0.4, 0.0, 0.03, 0.0);
            }
            // 4. Fiery magma drip from underside/edges
            if (random.nextDouble() < 0.15) {
                level.addParticle(ParticleTypes.DRIPPING_LAVA, x + (random.nextDouble() - 0.5) * 0.4, pos.getY() + 0.2, z + (random.nextDouble() - 0.5) * 0.4, 0.0, -0.02, 0.0);
            }
            // 5. Sizzling ambient lava sound & pops
            if (random.nextDouble() < 0.1) {
                level.playLocalSound(x, y, z, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 0.25F, 1.1F, false);
            }
            if (random.nextDouble() < 0.05) {
                level.playLocalSound(x, y, z, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.2F, 1.2F, false);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MagmaticCrucibleBlockEntity crucible) {
                Containers.dropContents(level, pos, crucible);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}

