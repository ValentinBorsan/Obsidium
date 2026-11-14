package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class CharonCoinItem extends Item {

    public CharonCoinItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6Ferryman's Toll: §7Carried in your inventory,").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§7upon death it is consumed to safely anchor").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7all your gear and armor into a §5Stygian Cenotaph§7.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§c⚠ Consumable (Single-use).").withStyle(ChatFormatting.RED));
    }
}
