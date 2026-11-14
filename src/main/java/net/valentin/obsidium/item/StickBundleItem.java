package net.valentin.obsidium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class StickBundleItem extends Item {
    public StickBundleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6Dense Wooden Fuel"));
        tooltip.add(Component.literal("§7Burns for 8 items in furnaces & generators (1,600 ticks)."));
        tooltip.add(Component.literal("§bCrafted 3x3 from 9 Wooden Sticks."));
    }
}

