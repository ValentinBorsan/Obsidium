package net.valentin.obsidium.item;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class VolcaniteLiquidUpgradeItem extends Item {
    public VolcaniteLiquidUpgradeItem(Properties properties) {
        super(properties.fireResistant());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("§6Sorting Station Upgrade"));
        tooltipComponents.add(Component.literal("§7Configures face for §bLIQUIDS §7(§bBlue§7)"));
        tooltipComponents.add(Component.literal("§8Extracts fluids from network into tanks/machines"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
