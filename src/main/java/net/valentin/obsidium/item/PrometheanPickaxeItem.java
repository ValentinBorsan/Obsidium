package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.util.AutoSmeltHelper;

import java.util.List;

public class PrometheanPickaxeItem extends PickaxeItem {
    public PrometheanPickaxeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            if (level.random.nextFloat() < 0.35f) {
                List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, level.getBlockEntity(pos), miner, stack);
                level.removeBlock(pos, false);
                for (ItemStack drop : drops) {
                    ItemStack smelted = AutoSmeltHelper.getSmeltedDrop(serverLevel, drop);
                    Block.popResource(level, pos, smelted);
                }
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, 0.2, 0.2, 0.2, 0.02);
            }
        }
        return super.mineBlock(stack, level, state, pos, miner);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6Promethean Smelt: §735% chance to automatically smelt drops.").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§eDurable early-game bronze pickaxe.").withStyle(ChatFormatting.GRAY));
    }
}

