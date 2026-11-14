package net.valentin.obsidium.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class VolcanicArmorItem extends ArmorItem {

    public VolcanicArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide() && entity instanceof Player player) {
            if (hasFullSuitOfArmorOn(player)) {
                evaluateArmorEffects(player);
            }
        }
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    private boolean hasFullSuitOfArmorOn(Player player) {
        ItemStack boots = player.getInventory().getArmor(0);
        ItemStack leggings = player.getInventory().getArmor(1);
        ItemStack breastplate = player.getInventory().getArmor(2);
        ItemStack helmet = player.getInventory().getArmor(3);
        
        return !helmet.isEmpty() && !breastplate.isEmpty()
                && !leggings.isEmpty() && !boots.isEmpty()
                && helmet.getItem() instanceof VolcanicArmorItem
                && breastplate.getItem() instanceof VolcanicArmorItem
                && leggings.getItem() instanceof VolcanicArmorItem
                && boots.getItem() instanceof VolcanicArmorItem;
    }

    private void evaluateArmorEffects(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0, false, false, true));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag tooltipFlag) {
        tooltip.add(net.minecraft.network.chat.Component.literal("§6Typhonic Might: §e+3 Attack Damage §7(Full Set Bonus)"));
        tooltip.add(net.minecraft.network.chat.Component.literal("§7• Permanent Fire Resistance & Heat Immunity"));
        tooltip.add(net.minecraft.network.chat.Component.literal("§8Reinforced Volcanic Alloy | Superior protection to Diamond"));
        super.appendHoverText(stack, context, tooltip, tooltipFlag);
    }
}
