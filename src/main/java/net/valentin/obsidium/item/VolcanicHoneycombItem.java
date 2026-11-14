package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class VolcanicHoneycombItem extends Item {
    public VolcanicHoneycombItem(Properties properties) {
        super(properties.rarity(Rarity.UNCOMMON));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6✦ Crystallized Pyromel Comb").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§7Harvested from Volcanic Beehives using Shears.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8Used for crafting Apiaries, Grafting Mastic & Ambrosia.").withStyle(ChatFormatting.DARK_GRAY));
    }
}

