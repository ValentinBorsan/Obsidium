package net.valentin.obsidium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
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

import java.util.List;

/**
 * Controller block for the 5x5 Olympian Hadron Collider toroidal particle accelerator multiblock.
 */
public class OlympianHadronColliderBlock extends BaseEntityBlock {
    public static final MapCodec<OlympianHadronColliderBlock> CODEC = simpleCodec(OlympianHadronColliderBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public OlympianHadronColliderBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ACTIVE, false)
                .setValue(FORMED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE, FORMED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(ACTIVE, false)
                .setValue(FORMED, false);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof OlympianHadronColliderBlockEntity collider) {
                player.openMenu(collider);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof OlympianHadronColliderBlockEntity collider) {
                collider.clearRingFormedState(level);
                Containers.dropContents(level, pos, collider.getInventory());
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OlympianHadronColliderBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ObsidiumMod.OLYMPIAN_HADRON_COLLIDER_BE, OlympianHadronColliderBlockEntity::tick);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof OlympianHadronColliderBlockEntity collider && collider.isFormed()) {
            double cx = pos.getX() + collider.getRingOffsetX() + 0.5;
            double cy = pos.getY() + 0.65;
            double cz = pos.getZ() + collider.getRingOffsetZ() + 0.5;
            if (collider.getBeamVelocity() > 50 && random.nextDouble() < 0.65) {
                double ang = random.nextDouble() * Math.PI * 2.0;
                double rad = 1.65;
                level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                        cx + Math.cos(ang) * rad, cy, cz + Math.sin(ang) * rad,
                        0.0, 0.02, 0.0);
            }
            if (collider.getCoilTemp() < 120 && random.nextDouble() < 0.25) {
                level.addParticle(ParticleTypes.SNOWFLAKE,
                        pos.getX() + 0.5 + (random.nextDouble() - 0.5),
                        pos.getY() + 1.05,
                        pos.getZ() + 0.5 + (random.nextDouble() - 0.5),
                        0.0, 0.01, 0.0);
            }
            if (collider.getBeamVelocity() > 750 && random.nextDouble() < 0.08) {
                level.playLocalSound(cx, cy, cz, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.25F, 1.6F, false);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal("⚛ 5×5 CERN-Style Toroidal Particle Accelerator & Reactor").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Requires 5×5 Ring: 12× Synchrotron Coils + 1× Energy, 1× Item & 1× Fluid Port.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("• Accelerates opposing beams (Beam A ↻ vs Beam B ↺) up to 99.9% c").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("• Uses Pyro-Diesel / Biodiesel / Distillate + Cryo Coolant (< 120 K = 1.5× Boost)").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("• Synthesizes Singularity Hadron Alloy (10% chance) & up to 328k RF/t!").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof OlympianHadronColliderBlockEntity collider) {
            return Math.min(15, (int) Math.round((collider.getBeamVelocity() / (double) OlympianHadronColliderBlockEntity.MAX_VELOCITY) * 15.0));
        }
        return 0;
    }
}

