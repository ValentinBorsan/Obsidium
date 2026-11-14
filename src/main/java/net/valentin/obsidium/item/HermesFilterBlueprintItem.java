package net.valentin.obsidium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Calibration blueprint used to imbue a Compactor Module with a 9-slot item filter.
 */
public class HermesFilterBlueprintItem extends Item {

    public HermesFilterBlueprintItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§79-Slot Whitelist / Blacklist schematic"));
        tooltip.add(Component.literal("§8Combine with §fCompactor §8or §fVoid Module"));
        tooltip.add(Component.literal("§8for §ePrecision Compactor §8or §5Selective Void§8."));
    }
}

