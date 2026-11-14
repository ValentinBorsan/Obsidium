package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class VolcaniteOverclockUpgradeItem extends Item {
    private final int tier;
    private final float speedMultiplier;
    private final float energyMultiplier;

    public VolcaniteOverclockUpgradeItem(Properties properties, int tier, float speedMultiplier, float energyMultiplier) {
        super(properties.fireResistant().stacksTo(16));
        this.tier = tier;
        this.speedMultiplier = speedMultiplier;
        this.energyMultiplier = energyMultiplier;
    }

    public int getTier() {
        return tier;
    }

    public float getSpeedMultiplier() {
        return speedMultiplier;
    }

    public float getEnergyMultiplier() {
        return energyMultiplier;
    }

    public String getTierName() {
        return switch (tier) {
            case 1 -> "Overdrive Module";
            case 2 -> "Hyperdrive Module";
            case 3 -> "Tartarean Singularity Overdrive";
            default -> "Overclock Module";
        };
    }

    public static boolean isOverclockUpgrade(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.getItem() instanceof VolcaniteOverclockUpgradeItem || stack.getItem() instanceof VolcanitePipeEnergyUpgradeItem;
    }

    public static float getSpeedBonus(ItemStack stack) {
        if (stack.getItem() instanceof VolcaniteOverclockUpgradeItem o) {
            return o.getSpeedMultiplier();
        }
        if (stack.getItem() instanceof VolcanitePipeEnergyUpgradeItem p) {
            return 1.0f + p.getTier() * 0.75f;
        }
        return 1.0f;
    }

    public static float getEnergyCostMultiplier(ItemStack stack) {
        if (stack.getItem() instanceof VolcaniteOverclockUpgradeItem o) {
            return o.getEnergyMultiplier();
        }
        if (stack.getItem() instanceof VolcanitePipeEnergyUpgradeItem p) {
            return 1.0f + p.getTier() * 0.5f;
        }
        return 1.0f;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String tierColor = switch (tier) {
            case 1 -> "§6";
            case 2 -> "§b";
            case 3 -> "§d";
            default -> "§e";
        };

        tooltip.add(Component.literal(tierColor + "★ " + getTierName()).withStyle(ChatFormatting.BOLD));
        tooltip.add(Component.literal("§7Installs into Volcanite processing machines:"));
        tooltip.add(Component.literal("  §a⚡ Processing Speed: §f+" + (int)((speedMultiplier - 1.0f) * 100) + "% (" + speedMultiplier + "x)"));
        tooltip.add(Component.literal("  §c⚡ RF Consumption: §f+" + (int)((energyMultiplier - 1.0f) * 100) + "% (" + energyMultiplier + "x)"));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§8Place in Grinder, Furnace, or Forge upgrade slot."));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}

