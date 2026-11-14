package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.valentin.obsidium.mythology.DivinePact;

import java.util.List;

public class MythicScrollItem extends Item {
    private final DivinePact pact;

    public MythicScrollItem(Properties properties, DivinePact pact) {
        super(properties);
        this.pact = pact;
    }

    public DivinePact getPact() {
        return pact;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.add(Component.literal("§6📜 Olympian Ritual Scroll").withStyle(ChatFormatting.GOLD));
        tooltip.add(pact.getFormattedBadge());
        for (String benefit : pact.getBenefits()) {
            tooltip.add(Component.literal(benefit));
        }
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§eRitual Use:").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§7Place upon an §6Astrape Ritual Obelisk §7surrounding the"));
        tooltip.add(Component.literal("§6Olympian Altar §7to bind this exclusive Divine Pact."));
        tooltip.add(Component.literal("§8(Each weapon/armor may hold only 1 Divine Pact)"));
        super.appendHoverText(stack, context, tooltip, tooltipFlag);
    }
}

