package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class FaradayArmorItem extends ArmorItem {
    public FaradayArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    /**
     * Returns true if the entity is wearing all 4 pieces of the Aether-Faraday Dielectric Suit.
     */
    public static boolean hasFullSuit(LivingEntity entity) {
        if (entity == null) return false;
        return entity.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof FaradayArmorItem
            && entity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof FaradayArmorItem
            && entity.getItemBySlot(EquipmentSlot.LEGS).getItem() instanceof FaradayArmorItem
            && entity.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof FaradayArmorItem;
    }

    /**
     * Returns how many pieces (0..4) of the Aether-Faraday Dielectric Suit the entity is wearing.
     */
    public static int getEquippedPieceCount(LivingEntity entity) {
        if (entity == null) return 0;
        int count = 0;
        if (entity.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof FaradayArmorItem) count++;
        if (entity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof FaradayArmorItem) count++;
        if (entity.getItemBySlot(EquipmentSlot.LEGS).getItem() instanceof FaradayArmorItem) count++;
        if (entity.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof FaradayArmorItem) count++;
        return count;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("⚡ Aether-Faraday Dielectric Suit").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Woven copper-electrum mesh lined with vulcanized rubber.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Full Set (4/4): 100% Immunity to High-Voltage Arcs & Lightning").withStyle(ChatFormatting.GOLD));
    }
}

