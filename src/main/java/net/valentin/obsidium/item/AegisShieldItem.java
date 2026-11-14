package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class AegisShieldItem extends ShieldItem {

    public AegisShieldItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.add(Component.literal("§6🛡 Aegis of Athena (Scutul Zeiței Atena)").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§7• §eProjectile Reflection§7: Reflects incoming arrows, fireballs & spells back at attackers!"));
        tooltip.add(Component.literal("§7• §cVolcanic Shockwave§7: Melee attackers are repelled, ignited & dealt volcanic backlash."));
        tooltip.add(Component.literal("§8Forged by Hephaestus for Athena | Durability: 1,600"));
    }
}

