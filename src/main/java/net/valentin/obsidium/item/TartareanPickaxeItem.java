package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class TartareanPickaxeItem extends PickaxeItem {
    public TartareanPickaxeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (state.getDestroySpeed(level, pos) >= 1.5f && level.random.nextBoolean()) {
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
        }
        return super.mineBlock(stack, level, state, pos, miner);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§9Tartarean Density: §750% durability resistance on hard stone.").withStyle(ChatFormatting.BLUE));
    }
}

