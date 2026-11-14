package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class VolcaniteCompactorUpgradeItem extends Item {
    public VolcaniteCompactorUpgradeItem(Properties properties) {
        super(properties.fireResistant());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("§6★ Volcanite Compactor Module").withStyle(ChatFormatting.BOLD));
        tooltipComponents.add(Component.literal("§7Hydraulic volcanic compression matrix:"));
        tooltipComponents.add(Component.literal("  §6🔨 Auto-Compression: §f9 Nuggets → 1 Ingot"));
        tooltipComponents.add(Component.literal("  §c🧱 Super-Compression: §f9 Ingots → 1 Block"));
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.literal("§8Socket into a Volcanite Modular Chest to activate."));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}

