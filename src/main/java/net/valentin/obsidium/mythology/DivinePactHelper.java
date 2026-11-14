package net.valentin.obsidium.mythology;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

public final class DivinePactHelper {
    public static final String PACT_KEY = "DivinePact";

    private DivinePactHelper() {}

    public static DivinePact getPact(ItemStack stack) {
        if (stack.isEmpty()) return DivinePact.NONE;
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        if (tag.contains(PACT_KEY)) {
            return DivinePact.fromId(tag.getString(PACT_KEY));
        }
        return DivinePact.NONE;
    }

    public static void setPact(ItemStack stack, DivinePact pact) {
        if (stack.isEmpty()) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (pact == null || pact == DivinePact.NONE) {
                tag.remove(PACT_KEY);
            } else {
                tag.putString(PACT_KEY, pact.getId());
            }
        });
    }

    public static boolean hasPact(ItemStack stack) {
        return getPact(stack) != DivinePact.NONE;
    }

    public static void appendPactTooltip(ItemStack stack, List<Component> tooltip) {
        DivinePact pact = getPact(stack);
        if (pact != DivinePact.NONE) {
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("§6✦ OLYMPIAN PACT ✦").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            tooltip.add(pact.getFormattedBadge());
            for (String benefit : pact.getBenefits()) {
                tooltip.add(Component.literal(benefit));
            }
        }
    }
}

