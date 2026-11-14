package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class VolcaniteVacuumUpgradeItem extends Item {
    public VolcaniteVacuumUpgradeItem(Properties properties) {
        super(properties.fireResistant());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("§6★ Volcanite Vacuum Module").withStyle(ChatFormatting.BOLD));
        tooltipComponents.add(Component.literal("§7Forged with Volcanite & Ender Resonance:"));
        tooltipComponents.add(Component.literal("  §e🧲 Gravitational Attraction: §f6-block radius"));
        tooltipComponents.add(Component.literal("  §b⚡ Absorption Cycle: §fCollects dropped items every 10 ticks"));
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.literal("§8Socket into a Volcanite Modular Chest to activate."));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}

