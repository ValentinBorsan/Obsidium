package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class BiofuelBucketItem extends Item {
    private final String subtitle;
    private final String sourceHint;
    private final String generatorHint;
    private final ChatFormatting accentColor;

    public BiofuelBucketItem(Properties properties, String subtitle, String sourceHint, String generatorHint, ChatFormatting accentColor) {
        super(properties);
        this.subtitle = subtitle;
        this.sourceHint = sourceHint;
        this.generatorHint = generatorHint;
        this.accentColor = accentColor;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("⚗ " + subtitle + " (1,000 mB)").withStyle(accentColor));
        tooltipComponents.add(Component.literal(sourceHint).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal(generatorHint).withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}

