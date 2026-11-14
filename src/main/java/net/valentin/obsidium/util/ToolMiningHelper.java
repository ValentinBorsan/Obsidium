package net.valentin.obsidium.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

public class ToolMiningHelper {

    private static final ThreadLocal<Boolean> IS_BREAKING = ThreadLocal.withInitial(() -> false);

    public static List<BlockPos> getSurroundingBlocks(BlockPos origin, Player player, int radiusX, int radiusY,
            int depth) {
        List<BlockPos> positions = new ArrayList<>();
        HitResult hit = player.pick(6.0D, 1.0F, false);
        Direction facing = Direction.DOWN;
        if (hit instanceof BlockHitResult blockHit) {
            facing = blockHit.getDirection();
        }

        // Determine axis
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int y = -radiusY; y <= radiusY; y++) {
                for (int d = 0; d < depth; d++) {
                    if (x == 0 && y == 0 && d == 0)
                        continue; // Skip original block (it breaks normally)

                    BlockPos target;
                    switch (facing.getAxis()) {
                        case Y -> target = origin.offset(x, -d * facing.getStepY(), y);
                        case Z -> target = origin.offset(x, y, -d * facing.getStepZ());
                        case X -> target = origin.offset(-d * facing.getStepX(), y, x);
                        default -> target = origin.offset(x, y, d);
                    }
                    positions.add(target);
                }
            }
        }
        return positions;
    }

    public static int breakBlocksInArea(ServerLevel level, ServerPlayer player, ItemStack tool, List<BlockPos> blocks,
            boolean autoSmelt) {
        if (IS_BREAKING.get())
            return 0;
        IS_BREAKING.set(true);

        int broken = 0;
        try {
            for (BlockPos pos : blocks) {
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || state.getDestroySpeed(level, pos) < 0)
                    continue; // Unbreakable (Bedrock)

                // Only mine blocks the tool is suitable for or can harvest
                if (tool.isCorrectToolForDrops(state) || player.hasCorrectToolForDrops(state)) {
                    BlockEntity blockEntity = level.getBlockEntity(pos);

                    if (autoSmelt) {
                        List<ItemStack> drops = Block.getDrops(state, level, pos, blockEntity, player, tool);
                        level.removeBlock(pos, false);
                        for (ItemStack drop : drops) {
                            ItemStack smelted = AutoSmeltHelper.getSmeltedDrop(level, drop);
                            Block.popResource(level, pos, smelted);
                        }
                    } else {
                        Block.dropResources(state, level, pos, blockEntity, player, tool);
                        level.removeBlock(pos, false);
                    }

                    level.levelEvent(2001, pos, Block.getId(state));
                    broken++;
                }
            }
        } finally {
            IS_BREAKING.set(false);
        }
        return broken;
    }
}
