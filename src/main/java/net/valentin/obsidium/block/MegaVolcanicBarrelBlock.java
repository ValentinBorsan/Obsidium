package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.sound.ModSounds;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MegaVolcanicBarrelBlock extends VolcanicWoodLiquidBarrelBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public MegaVolcanicBarrelBlock(Properties properties) {
        super(properties, false);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    public ItemInteractionResult interactFromPart(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hitResult) {
        return useItemOn(stack, state, level, pos, player, hand, hitResult);
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
                    level.setBlock(partPos, ObsidiumMod.MEGA_VOLCANIC_BARREL_PART.defaultBlockState()
                            .setValue(MegaVolcanicBarrelPartBlock.UPPER, dy == 1)
                            .setValue(MegaVolcanicBarrelPartBlock.POS_X, dx)
                            .setValue(MegaVolcanicBarrelPartBlock.POS_Z, dz), Block.UPDATE_ALL);
                }
            }
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LAVA, pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0,
                    15, 0.8, 0.8, 0.8, 0.02);
            serverLevel.sendParticles(ParticleTypes.FLAME, pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0,
                    20, 0.6, 0.6, 0.6, 0.02);
        }
    }

    /**
     * Checks if a block is a valid component of an unformed 2x2x2 Mega Volcanic Barrel structure.
     */
    public static boolean isMultiblockComponent(BlockState state) {
        Block b = state.getBlock();
        return b == ObsidiumMod.REINFORCED_VOLCANIC_LIQUID_BARREL_BLOCK || b == ObsidiumMod.VOLCANITE_CASING_BLOCK;
    }

    /**
     * Verifies if a 2x2x2 cube anchored at pos has at least 1 Reinforced Volcanic Liquid Barrel and remaining Volcanite Casings.
     */
    public static boolean isValid2x2x2Structure(Level level, BlockPos anchor) {
        int barrelCount = 0;
        int casingCount = 0;

        for (int dx = 0; dx < 2; dx++) {
            for (int dy = 0; dy < 2; dy++) {
                for (int dz = 0; dz < 2; dz++) {
                    BlockPos p = anchor.offset(dx, dy, dz);
                    if (level.isOutsideBuildHeight(p)) return false;
                    BlockState s = level.getBlockState(p);
                    if (s.is(ObsidiumMod.REINFORCED_VOLCANIC_LIQUID_BARREL_BLOCK)) {
                        barrelCount++;
                    } else if (s.is(ObsidiumMod.VOLCANITE_CASING_BLOCK)) {
                        casingCount++;
                    } else {
                        return false;
                    }
                }
            }
        }

        return barrelCount >= 1 && (barrelCount + casingCount == 8);
    }

    /**
     * Assembles the 2x2x2 multiblock from in-world blocks, preserving existing fluids.
     */
    public static void assembleMultiblock(Level level, BlockPos anchor, @Nullable Player player) {
        // 1. Preserve existing fluid from any of the existing barrels in the 2x2x2 footprint
        FluidVariant storedFluid = FluidVariant.blank();
        long storedAmount = 0;

        for (int dx = 0; dx < 2; dx++) {
            for (int dy = 0; dy < 2; dy++) {
                for (int dz = 0; dz < 2; dz++) {
                    BlockPos p = anchor.offset(dx, dy, dz);
                    if (level.getBlockEntity(p) instanceof LiquidTankBlockEntity tank) {
                        if (!tank.getResource().isBlank() && tank.getAmount() > 0) {
                            storedFluid = tank.getResource();
                            storedAmount = tank.getAmount();
                            break;
                        }
                    }
                }
                if (!storedFluid.isBlank()) break;
            }
            if (!storedFluid.isBlank()) break;
        }

        // 2. Controller facing
        Direction facing = Direction.NORTH;
        if (player != null) {
            Direction pFace = player.getDirection().getOpposite();
            if (pFace.getAxis() != Direction.Axis.Y) {
                facing = pFace;
            }
        }

        // 3. Set controller block at anchor (dy=0, dx=0, dz=0)
        level.setBlock(anchor, ObsidiumMod.MEGA_VOLCANIC_BARREL.defaultBlockState().setValue(FACING, facing), Block.UPDATE_ALL);

        // 4. Set dummy part blocks at the other 7 positions
        for (int dx = 0; dx < 2; dx++) {
            for (int dy = 0; dy < 2; dy++) {
                for (int dz = 0; dz < 2; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos partPos = anchor.offset(dx, dy, dz);
                    level.setBlock(partPos, ObsidiumMod.MEGA_VOLCANIC_BARREL_PART.defaultBlockState()
                            .setValue(MegaVolcanicBarrelPartBlock.UPPER, dy == 1)
                            .setValue(MegaVolcanicBarrelPartBlock.POS_X, dx)
                            .setValue(MegaVolcanicBarrelPartBlock.POS_Z, dz), Block.UPDATE_ALL);
                }
            }
        }

        // 5. Transfer preserved fluid to newly formed Mega Barrel
        if (!storedFluid.isBlank() && storedAmount > 0) {
            BlockEntity be = level.getBlockEntity(anchor);
            if (be instanceof MegaVolcanicBarrelBlockEntity megaBe) {
                megaBe.insertFluid(storedFluid, (int) storedAmount, false);
            }
        }

        // 6. Audio-visual fanfare
        level.playSound(null, anchor, ModSounds.MULTIBLOCK_FORM, SoundSource.BLOCKS, 1.0F, 1.0F);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LAVA, anchor.getX() + 1.0, anchor.getY() + 1.0, anchor.getZ() + 1.0,
                    25, 0.8, 0.8, 0.8, 0.05);
            serverLevel.sendParticles(ParticleTypes.FLAME, anchor.getX() + 1.0, anchor.getY() + 1.0, anchor.getZ() + 1.0,
                    35, 0.7, 0.7, 0.7, 0.04);
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, anchor.getX() + 1.0, anchor.getY() + 1.8, anchor.getZ() + 1.0,
                    15, 0.5, 0.5, 0.5, 0.02);
        }

        if (player != null) {
            player.displayClientMessage(Component.literal("§6[Mega Barrel] §aDionysian Mega Volcanic Barrel assembled! (Capacity: 1,024,000 mB)"), true);
        }
    }

    /**
     * Tries to assemble the 2x2x2 multiblock from a clicked block position.
     */
    public static boolean tryAssembleMultiblock(Level level, BlockPos clickedPos, @Nullable Player player, InteractionHand hand) {
        BlockState clickedState = level.getBlockState(clickedPos);
        if (!isMultiblockComponent(clickedState)) {
            return false;
        }

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

        if (!level.isClientSide() && player != null) {
            player.displayClientMessage(Component.literal("§6[Mega Barrel] §cIncomplete structure! Requires a 2×2×2 cube containing 1 Reinforced Volcanic Barrel and 7 Volcanite Casings."), true);
        }
        return false;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MegaVolcanicBarrelBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ObsidiumMod.MEGA_VOLCANIC_BARREL_BE
                ? (BlockEntityTicker<T>) (BlockEntityTicker<MegaVolcanicBarrelBlockEntity>) LiquidTankBlockEntity::tick
                : null;
    }

    public void dismantleFromPart(Level level, BlockPos controllerPos, BlockPos brokenPartPos) {
        if (level.isClientSide()) return;
        BlockEntity be = level.getBlockEntity(controllerPos);
        if (!(be instanceof MegaVolcanicBarrelBlockEntity)) return;

        level.setBlock(controllerPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        net.minecraft.world.Containers.dropItemStack(level, brokenPartPos.getX() + 0.5, brokenPartPos.getY() + 0.5,
                brokenPartPos.getZ() + 0.5, new ItemStack(ObsidiumMod.MEGA_VOLCANIC_BARREL_ITEM));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            for (int dx = 0; dx < 2; dx++) {
                for (int dy = 0; dy < 2; dy++) {
                    for (int dz = 0; dz < 2; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos partPos = pos.offset(dx, dy, dz);
                        if (level.getBlockState(partPos).is(ObsidiumMod.MEGA_VOLCANIC_BARREL_PART)) {
                            level.setBlock(partPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        }
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.obsidium.mega_volcanic_barrel.capacity", MegaVolcanicBarrelBlockEntity.CAPACITY));
        tooltip.add(Component.translatable("tooltip.obsidium.mega_volcanic_barrel.structure"));
    }
}
