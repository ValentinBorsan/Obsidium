package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.sound.ModSounds;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DionysianBioCalderaBlock extends BaseEntityBlock {
    public static final com.mojang.serialization.MapCodec<DionysianBioCalderaBlock> CODEC = simpleCodec(DionysianBioCalderaBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public DionysianBioCalderaBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos anchor = context.getClickedPos();
        Level level = context.getLevel();
        for (int dx = 0; dx < 2; dx++) {
            for (int dy = 0; dy < 2; dy++) {
                for (int dz = 0; dz < 2; dz++) {
                    BlockPos pos = anchor.offset(dx, dy, dz);
                    if (level.isOutsideBuildHeight(pos) || (dx != 0 || dy != 0 || dz != 0) && !level.getBlockState(pos).isAir()) {
                        return null;
                    }
                }
            }
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;

        for (int dx = 0; dx < 2; dx++) {
            for (int dy = 0; dy < 2; dy++) {
                for (int dz = 0; dz < 2; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos partPos = pos.offset(dx, dy, dz);
                    level.setBlock(partPos, ObsidiumMod.DIONYSIAN_BIO_CALDERA_PART.defaultBlockState()
                            .setValue(DionysianBioCalderaPartBlock.UPPER, dy == 1)
                            .setValue(DionysianBioCalderaPartBlock.POS_X, dx)
                            .setValue(DionysianBioCalderaPartBlock.POS_Z, dz), Block.UPDATE_ALL);
                }
            }
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 1.0, pos.getY() + 1.8, pos.getZ() + 1.0,
                    15, 0.5, 0.5, 0.5, 0.05);
            serverLevel.sendParticles(ParticleTypes.COMPOSTER, pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0,
                    15, 0.6, 0.6, 0.6, 0.02);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DionysianBioCalderaBlockEntity caldera && caldera.isFermenting()) {
            double cx = pos.getX() + 1.0;
            double cy = pos.getY() + 2.05;
            double cz = pos.getZ() + 1.0;
            if (random.nextDouble() < 0.6) {
                level.addParticle(ParticleTypes.CLOUD, cx + (random.nextDouble() - 0.5) * 0.4, cy, cz + (random.nextDouble() - 0.5) * 0.4, 0.0, 0.04, 0.0);
                level.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, cx + (random.nextDouble() - 0.5) * 1.2, cy - 0.4, cz + (random.nextDouble() - 0.5) * 1.2, 0.0, 0.02, 0.0);
            }
            if (random.nextDouble() < 0.35) {
                level.addParticle(ParticleTypes.COMPOSTER, cx + (random.nextDouble() - 0.5) * 0.8, pos.getY() + 0.8 + random.nextDouble() * 0.8, cz + (random.nextDouble() - 0.5) * 0.8, 0.0, 0.02, 0.0);
            }
            if (random.nextDouble() < 0.15) {
                level.playLocalSound(cx, pos.getY() + 1.0, cz, net.minecraft.sounds.SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.4F, 0.8F + random.nextFloat() * 0.3F, false);
            }
        }
    }

    public static boolean isMultiblockComponent(BlockState state) {
        Block b = state.getBlock();
        return b == ObsidiumMod.VOLCANIC_WOOD_LIQUID_BARREL_BLOCK || b == ObsidiumMod.REINFORCED_VOLCANIC_LIQUID_BARREL_BLOCK
                || b == ObsidiumMod.VOLCANITE_CASING_BLOCK || b == ObsidiumMod.ADVANCED_VOLCANITE_CASING_BLOCK
                || b == ObsidiumMod.DEMETRIAN_BIOMASS_GENERATOR_BLOCK;
    }

    public static boolean isValid2x2x2Structure(Level level, BlockPos anchor) {
        int generatorOrBarrelCount = 0;
        int totalValid = 0;

        for (int dx = 0; dx < 2; dx++) {
            for (int dy = 0; dy < 2; dy++) {
                for (int dz = 0; dz < 2; dz++) {
                    BlockPos p = anchor.offset(dx, dy, dz);
                    if (level.isOutsideBuildHeight(p)) return false;
                    BlockState s = level.getBlockState(p);
                    if (s.is(ObsidiumMod.DEMETRIAN_BIOMASS_GENERATOR_BLOCK) || s.is(ObsidiumMod.REINFORCED_VOLCANIC_LIQUID_BARREL_BLOCK) || s.is(ObsidiumMod.VOLCANIC_WOOD_LIQUID_BARREL_BLOCK)) {
                        generatorOrBarrelCount++;
                        totalValid++;
                    } else if (s.is(ObsidiumMod.VOLCANITE_CASING_BLOCK) || s.is(ObsidiumMod.ADVANCED_VOLCANITE_CASING_BLOCK)) {
                        totalValid++;
                    } else {
                        return false;
                    }
                }
            }
        }

        return generatorOrBarrelCount >= 1 && totalValid == 8;
    }

    public static void assembleMultiblock(Level level, BlockPos anchor, @Nullable Player player) {
        Direction facing = Direction.NORTH;
        if (player != null) {
            Direction pFace = player.getDirection().getOpposite();
            if (pFace.getAxis() != Direction.Axis.Y) facing = pFace;
        }

        level.setBlock(anchor, ObsidiumMod.DIONYSIAN_BIO_CALDERA.defaultBlockState().setValue(FACING, facing), Block.UPDATE_ALL);

        for (int dx = 0; dx < 2; dx++) {
            for (int dy = 0; dy < 2; dy++) {
                for (int dz = 0; dz < 2; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos partPos = anchor.offset(dx, dy, dz);
                    level.setBlock(partPos, ObsidiumMod.DIONYSIAN_BIO_CALDERA_PART.defaultBlockState()
                            .setValue(DionysianBioCalderaPartBlock.UPPER, dy == 1)
                            .setValue(DionysianBioCalderaPartBlock.POS_X, dx)
                            .setValue(DionysianBioCalderaPartBlock.POS_Z, dz), Block.UPDATE_ALL);
                }
            }
        }

        level.playSound(null, anchor, ModSounds.MULTIBLOCK_FORM, SoundSource.BLOCKS, 1.0F, 1.0F);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, anchor.getX() + 1.0, anchor.getY() + 1.8, anchor.getZ() + 1.0, 25, 0.7, 0.7, 0.7, 0.05);
            serverLevel.sendParticles(ParticleTypes.FLAME, anchor.getX() + 1.0, anchor.getY() + 0.8, anchor.getZ() + 1.0, 20, 0.6, 0.6, 0.6, 0.03);
        }

        if (player != null) {
            player.displayClientMessage(Component.literal("§6[Bio-Caldera] §aDionysian Bio-Caldera assembled! (Output: up to 1,800 RF/t)"), true);
        }
    }

    public static boolean tryAssembleMultiblock(Level level, BlockPos clickedPos, @Nullable Player player, InteractionHand hand) {
        BlockState clickedState = level.getBlockState(clickedPos);
        if (!isMultiblockComponent(clickedState)) return false;

        for (int ox = -1; ox <= 0; ox++) {
            for (int oy = -1; oy <= 0; oy++) {
                for (int oz = -1; oz <= 0; oz++) {
                    BlockPos anchor = clickedPos.offset(ox, oy, oz);
                    if (isValid2x2x2Structure(level, anchor)) {
                        if (!level.isClientSide()) {
                            assembleMultiblock(level, anchor, player);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void dismantleFromPart(Level level, BlockPos controllerPos, BlockPos brokenPartPos) {
        if (level.isClientSide()) return;
        BlockEntity be = level.getBlockEntity(controllerPos);
        if (!(be instanceof DionysianBioCalderaBlockEntity)) return;

        level.setBlock(controllerPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        net.minecraft.world.Containers.dropItemStack(level, brokenPartPos.getX() + 0.5, brokenPartPos.getY() + 0.5,
                brokenPartPos.getZ() + 0.5, new ItemStack(ObsidiumMod.DIONYSIAN_BIO_CALDERA_ITEM));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            for (int dx = 0; dx < 2; dx++) {
                for (int dy = 0; dy < 2; dy++) {
                    for (int dz = 0; dz < 2; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos partPos = pos.offset(dx, dy, dz);
                        if (level.getBlockState(partPos).is(ObsidiumMod.DIONYSIAN_BIO_CALDERA_PART)) {
                            level.setBlock(partPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        }
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(net.minecraft.world.item.Items.BUCKET)) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DionysianBioCalderaBlockEntity caldera) {
                if (caldera.extractBioOil(1000, true) == 1000) {
                    caldera.extractBioOil(1000, false);
                    net.minecraft.world.item.ItemStack oilBucket = new net.minecraft.world.item.ItemStack(ObsidiumMod.BIO_OIL_BUCKET);
                    if (!player.isCreative()) {
                        stack.shrink(1);
                        if (stack.isEmpty()) {
                            player.setItemInHand(hand, oilBucket);
                        } else if (!player.getInventory().add(oilBucket)) {
                            player.drop(oilBucket, false);
                        }
                    } else if (!player.getInventory().add(oilBucket)) {
                        player.drop(oilBucket, false);
                    }
                    level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BUCKET_FILL, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                    return net.minecraft.world.ItemInteractionResult.SUCCESS;
                }
            }
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DionysianBioCalderaBlockEntity caldera) {
                player.openMenu(caldera);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DionysianBioCalderaBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ObsidiumMod.DIONYSIAN_BIO_CALDERA_BE, DionysianBioCalderaBlockEntity::tick);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.obsidium.dionysian_bio_caldera.desc"));
        tooltip.add(Component.translatable("tooltip.obsidium.dionysian_bio_caldera.output"));
        tooltip.add(Component.translatable("tooltip.obsidium.dionysian_bio_caldera.structure"));
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DionysianBioCalderaBlockEntity caldera) {
            long amt = caldera.energyStorage.amount;
            long cap = caldera.energyStorage.capacity;
            if (cap <= 0 || amt <= 0) return 0;
            return (int) Math.floor((double) amt / cap * 14.0) + 1;
        }
        return 0;
    }
}
