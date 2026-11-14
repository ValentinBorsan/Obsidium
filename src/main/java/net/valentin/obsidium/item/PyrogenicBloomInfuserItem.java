package net.valentin.obsidium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class PyrogenicBloomInfuserItem extends Item {
    public PyrogenicBloomInfuserItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("§6Geothermal Arborist Upgrade"));
        tooltipComponents.add(Component.literal("§7Bio-thermal catalyst matrix."));
        tooltipComponents.add(Component.literal("§eUnlocks: §fFertilizer Bay (Bone Meal & Volcanic Ash)."));
        tooltipComponents.add(Component.literal("§aEffect: §fConsumes fertilizer for instant full floral blooms."));
        tooltipComponents.add(Component.literal("§8Place in the Infuser upgrade slot in the Arborist."));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
