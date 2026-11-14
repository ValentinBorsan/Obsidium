package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class MagmaForgeDummyBlock extends Block {
    public MagmaForgeDummyBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    public static BlockPos findCore(Level level, BlockPos pos) {
        // Fast path: check horizontal 3x3 layer (y = 0) first, where the 3x3x1 Magma Forge core always sits
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                BlockPos checkPos = pos.offset(x, 0, z);
                if (level.getBlockEntity(checkPos) instanceof MagmaForgeCoreBlockEntity) {
                    return checkPos;
                }
            }
        }
        // Legacy fallback: check y = 1 and y = -1
        for (int y : new int[]{1, -1}) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    if (level.getBlockEntity(checkPos) instanceof MagmaForgeCoreBlockEntity) {
                        return checkPos;
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockPos corePos = findCore(level, pos);
            if (corePos != null) {
                BlockEntity be = level.getBlockEntity(corePos);
                if (be instanceof MagmaForgeCoreBlockEntity core) {
                    player.openMenu(core);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockPos corePos = findCore(level, pos);
            if (corePos != null) {
                BlockEntity be = level.getBlockEntity(corePos);
                if (be instanceof MagmaForgeCoreBlockEntity core) {
                    player.openMenu(core);
                }
            }
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockPos checkPos = pos.offset(x, y, z);
                        if (level.getBlockEntity(checkPos) instanceof MagmaForgeCoreBlockEntity core) {
                            core.unform(level, checkPos);
                        }
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
