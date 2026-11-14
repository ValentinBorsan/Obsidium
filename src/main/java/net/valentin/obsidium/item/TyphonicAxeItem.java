package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class TyphonicAxeItem extends AxeItem {

    private static final int MAX_BLOCKS = 128;

    public TyphonicAxeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!level.isClientSide() && state.is(BlockTags.LOGS) && miner instanceof Player player) {
            if (!player.isShiftKeyDown()) {
                fellTree(level, pos, state, player, stack);
            }
        }
        return super.mineBlock(stack, level, state, pos, miner);
    }

    private void fellTree(Level level, BlockPos startPos, BlockState startState, Player player, ItemStack tool) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(startPos);
        visited.add(startPos);

        int felledCount = 0;

        while (!queue.isEmpty() && visited.size() <= MAX_BLOCKS) {
            BlockPos current = queue.poll();

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos neighbor = current.offset(dx, dy, dz);

                        if (!visited.contains(neighbor) && level.getBlockState(neighbor).is(BlockTags.LOGS)) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }

            if (!current.equals(startPos)) {
                BlockState s = level.getBlockState(current);
                if (s.is(BlockTags.LOGS)) {
                    level.destroyBlock(current, true, player);
                    felledCount++;
                }
            }
        }

        if (felledCount > 0) {
            tool.hurtAndBreak(felledCount, player, EquipmentSlot.MAINHAND);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6Typhonic Timber: §7Fells entire trees in a single strike.").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§8[Sneak to chop only a single log]").withStyle(ChatFormatting.DARK_GRAY));
    }
}

