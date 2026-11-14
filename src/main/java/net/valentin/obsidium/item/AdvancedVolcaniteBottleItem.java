package net.valentin.obsidium.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.ChatFormatting;
import team.reborn.energy.api.base.SimpleEnergyItem;
import java.util.List;

public class AdvancedVolcaniteBottleItem extends VolcaniteBottleItem implements SimpleEnergyItem {
    public static final int MAX_ENERGY = 1000000;

    public AdvancedVolcaniteBottleItem(Properties properties, int capacity) {
        super(properties, capacity);
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return MAX_ENERGY;
    }

    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return 10000;
    }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return 10000; // Can be used to power things if needed
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, context, tooltipComponents, isAdvanced);
        long stored = getStoredEnergy(stack);
        tooltipComponents.add(Component.literal("Energy: " + stored + " / " + MAX_ENERGY + " RF").withStyle(ChatFormatting.GREEN));
    }
}
