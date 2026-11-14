package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
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

public class HydrothermalVitrifierBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public HydrothermalVitrifierBlock(Properties properties) {
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
        if (be instanceof HydrothermalVitrifierBlockEntity vitrifier) {
            // Direct bucket filling with Water
            if (stack.is(Items.WATER_BUCKET)) {
                if (vitrifier.getWaterAmount() <= HydrothermalVitrifierBlockEntity.MAX_WATER - 1000) {
                    if (!level.isClientSide()) {
                        vitrifier.addWater(1000);
                        if (!player.getAbilities().instabuild) {
                            player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                        }
                        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                } else {
                    if (!level.isClientSide()) {
                        player.openMenu(vitrifier);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
            // Direct bucket filling with Lava
            if (stack.is(Items.LAVA_BUCKET)) {
                if (vitrifier.getLavaAmount() <= HydrothermalVitrifierBlockEntity.MAX_LAVA - 1000) {
                    if (!level.isClientSide()) {
                        vitrifier.addLava(1000);
                        if (!player.getAbilities().instabuild) {
                            player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                        }
                        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                } else {
                    if (!level.isClientSide()) {
                        player.openMenu(vitrifier);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }

            // Normal right-click with any item in hand (when not sneaking) opens the machine interface
            if (!player.isShiftKeyDown()) {
                if (!level.isClientSide()) {
                    player.openMenu(vitrifier);
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
            if (blockEntity instanceof HydrothermalVitrifierBlockEntity vitrifier) {
                player.openMenu(vitrifier);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ObsidiumMod.HYDROTHERMAL_VITRIFIER_BE ?
                (BlockEntityTicker<T>) (BlockEntityTicker<HydrothermalVitrifierBlockEntity>) HydrothermalVitrifierBlockEntity::tick : null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HydrothermalVitrifierBlockEntity(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + 0.5;

            // 1. Billowing dense steam clouds from thermal quenching reaction
            if (random.nextDouble() < 0.5) {
                level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x + (random.nextDouble() - 0.5) * 0.25, y, z + (random.nextDouble() - 0.5) * 0.25, 0.0, 0.04, 0.0);
            }
            if (random.nextDouble() < 0.3) {
                level.addParticle(ParticleTypes.CLOUD, x + (random.nextDouble() - 0.5) * 0.3, y + 0.1, z + (random.nextDouble() - 0.5) * 0.3, 0.0, 0.02, 0.0);
            }
            // 2. Obsidian crystalline formation resonance: purple portal sparks & magic runes
            if (random.nextDouble() < 0.35) {
                level.addParticle(ParticleTypes.PORTAL, x + (random.nextDouble() - 0.5) * 0.6, y - 0.2 + random.nextDouble() * 0.5, z + (random.nextDouble() - 0.5) * 0.6, (random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.2);
            }
            if (random.nextDouble() < 0.2) {
                level.addParticle(ParticleTypes.ENCHANT, x + (random.nextDouble() - 0.5) * 0.5, y + 0.2, z + (random.nextDouble() - 0.5) * 0.5, 0.0, 0.1, 0.0);
            }
            // 3. Dual element flash: occasional splash + lava spark
            if (random.nextDouble() < 0.15) {
                level.addParticle(ParticleTypes.SPLASH, x + (random.nextDouble() - 0.5) * 0.2, y, z + (random.nextDouble() - 0.5) * 0.2, 0.0, 0.03, 0.0);
                level.addParticle(ParticleTypes.LAVA, x + (random.nextDouble() - 0.5) * 0.2, y, z + (random.nextDouble() - 0.5) * 0.2, 0.0, 0.02, 0.0);
            }
            // 4. Hissing thermal quench & crystalline resonance sound
            if (random.nextDouble() < 0.12) {
                level.playLocalSound(x, y, z, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.25F, 1.2F + random.nextFloat() * 0.2F, false);
            }
            if (random.nextDouble() < 0.05) {
                level.playLocalSound(x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.2F, 0.8F, false);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof HydrothermalVitrifierBlockEntity vitrifier) {
                Containers.dropContents(level, pos, vitrifier);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
