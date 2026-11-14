package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class AstralVolcanicCatalystItem extends Item {
    public AstralVolcanicCatalystItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("✦ Nether Star + Volcanic Ash Catalyst").withStyle(ChatFormatting.GOLD));
        tooltipComponents.add(Component.literal("Triggers a short-duration Cataclysmic Eruption (35,000 RF/t)").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("inside the Supreme Volcanic Generator with Pyroclastic Distillate.").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}

