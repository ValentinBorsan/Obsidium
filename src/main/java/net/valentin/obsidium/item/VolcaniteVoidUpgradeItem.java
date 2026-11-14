package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class VolcaniteVoidUpgradeItem extends Item {
    public VolcaniteVoidUpgradeItem(Properties properties) {
        super(properties.fireResistant());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("§5★ Volcanite Void Module").withStyle(ChatFormatting.BOLD));
        tooltipComponents.add(Component.literal("§7Abyssal singularity overflow regulator:"));
        tooltipComponents.add(Component.literal("  §d🌌 Safe Overflow: §fPrevents chest locking by voiding excess items"));
        tooltipComponents.add(Component.literal("  §8🛡 Cobblestone / Dirt Overflow Filter"));
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.literal("§8Socket into a Volcanite Modular Chest to activate."));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}

