package net.valentin.obsidium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class DemetrianGraftingTapeItem extends Item {

    public DemetrianGraftingTapeItem(Properties properties) {
        super(properties.stacksTo(64).rarity(Rarity.UNCOMMON));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6✦ Organic Grafting Tape"));
        tooltip.add(Component.literal("§7Used to bind and seal a rootstock sapling"));
        tooltip.add(Component.literal("§7after inserting a Noble Scion."));
        tooltip.add(Component.literal("§8Triggers hybrid metamorphosis & fusion."));
    }
}
