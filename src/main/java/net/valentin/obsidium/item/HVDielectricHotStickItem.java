package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class HVDielectricHotStickItem extends Item {
    public HVDielectricHotStickItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("⚡ Insulated High-Voltage Operating Pole").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("• Right-Click HV Multiblock: Safely open HV Control Panel").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("• Shift + Right-Click HV Multiblock: Toggle Grounded Service Mode (0 kV)").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("Required to safely operate or dismantle the Keraunos HV Matrix!").withStyle(ChatFormatting.RED));
    }
}

