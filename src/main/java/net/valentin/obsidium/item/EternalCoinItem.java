package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import team.reborn.energy.api.base.SimpleEnergyItem;

import java.util.List;

public class EternalCoinItem extends Item implements SimpleEnergyItem {

    public static final long CAPACITY = 500_000L;
    public static final long MAX_INPUT = 25_000L;
    public static final long COST_PER_DEATH = 100_000L;

    public EternalCoinItem(Properties properties) {
        super(properties);
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) { return CAPACITY; }

    @Override
    public long getEnergyMaxInput(ItemStack stack) { return MAX_INPUT; }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) { return 0; }

    @Override
    public boolean isBarVisible(ItemStack stack) { return true; }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round((double) getStoredEnergy(stack) * 13.0 / (double) CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) { return 0x00E5FF; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        long energy = getStoredEnergy(stack);
        tooltip.add(Component.literal("§b⚡ Energy: §f" + String.format("%,d", energy) + " §b/ §f" + String.format("%,d", CAPACITY) + " RF").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("§6Eternal Soul Anchor: §7Never consumed on death!").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§7Consumes §e100,000 RF §7upon death to preserve").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7all gear in a §5Stygian Cenotaph§7 and returns with you on respawn.").withStyle(ChatFormatting.GRAY));
        if (energy < COST_PER_DEATH) {
            tooltip.add(Component.literal("§c⚠ Insufficient energy (requires at least 100,000 RF)!").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.literal("§a✔ Charged and ready for protection.").withStyle(ChatFormatting.GREEN));
        }
    }
}
