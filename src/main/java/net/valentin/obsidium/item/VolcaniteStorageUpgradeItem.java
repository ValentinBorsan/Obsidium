package net.valentin.obsidium.item;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class VolcaniteStorageUpgradeItem extends Item {
    public VolcaniteStorageUpgradeItem(Properties properties) {
        super(properties.fireResistant());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("§6Sorting Station Upgrade"));
        tooltipComponents.add(Component.literal("§7Configures face for §aITEMS §7(§aGreen§7)"));
        tooltipComponents.add(Component.literal("§8Extracts items from network into machines"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
