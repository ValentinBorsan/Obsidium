package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class PyroclasticDistillateItem extends Item {
    private final int fluidAmountMb;

    public PyroclasticDistillateItem(Properties properties, int fluidAmountMb) {
        super(properties);
        this.fluidAmountMb = fluidAmountMb;
    }

    public int getFluidAmountMb() {
        return fluidAmountMb;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("⚗ Distilled Volcanic Fuel (" + fluidAmountMb + " mB)").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltipComponents.add(Component.literal("Synthesized from Lava + Reagents in the Volcanic Distillation Chamber.").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("Fuels the Supreme Volcanic Generator.").withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}

