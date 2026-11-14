package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Ultra-rare endgame alloy synthesized exclusively inside a formed 5x5 Olympian Hadron Collider
 * with a 10% quantum probability per high-energy singularity collision.
 */
public class SingularityHadronAlloyItem extends Item {
    public SingularityHadronAlloyItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal("✦ Collider-Exclusive Quantum Singularity Alloy").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("Synthesized ONLY in the Olympian Hadron Collider (10% Chance).").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("• Dedicated Recipe: 4× Supreme Volcanic Alloy + 4× Astral Volcanic Catalyst").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("• Cryo Bonus: 10% drop chance on any collision at >= 90% c & < 120 K!").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("• Used for: Core Teleporter, Warp Gauntlet & Singularity Astrape Wires").withStyle(ChatFormatting.YELLOW));
    }
}
