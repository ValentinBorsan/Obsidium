package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import team.reborn.energy.api.base.SimpleEnergyItem;

import java.util.List;

public class VolcanicCiderItem extends Item {

    public static final long ENERGY_RESTORE_AMOUNT = 60_000L;

    public VolcanicCiderItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(itemStack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof Player player) {
            // 1. Crisp Cider Buffs
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 180, 1)); // Absorption II (3 min)
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 45, 1)); // Regeneration II (45s)
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 180, 1)); // Speed II (3 min)
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 20 * 180, 1)); // Haste II (3 min)
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 300, 0)); // Fire Resistance (5 min)

            // 2. Recharge carried energy equipment (up to 60,000 RF)
            long remainingEnergy = ENERGY_RESTORE_AMOUNT;
            for (int i = 0; i < player.getInventory().getContainerSize() && remainingEnergy > 0; i++) {
                ItemStack slotStack = player.getInventory().getItem(i);
                if (slotStack.isEmpty()) continue;

                if (slotStack.getItem() instanceof SimpleEnergyItem energyItem) {
                    long stored = energyItem.getStoredEnergy(slotStack);
                    long capacity = energyItem.getEnergyCapacity(slotStack);
                    long needed = capacity - stored;
                    if (needed > 0) {
                        long toAdd = Math.min(needed, remainingEnergy);
                        energyItem.setStoredEnergy(slotStack, stored + toAdd);
                        remainingEnergy -= toAdd;
                    }
                }
            }

            // Visual and audio feedback
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 16, 0.4, 0.5, 0.4, 0.1);
                serverLevel.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.8, player.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.7f, 1.15f);

            // Empty glass bottle return
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                ItemStack emptyBottle = new ItemStack(Items.GLASS_BOTTLE);
                if (stack.isEmpty()) {
                    return emptyBottle;
                } else if (!player.getInventory().add(emptyBottle)) {
                    player.drop(emptyBottle, false);
                }
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.add(Component.literal("§6🍎 Dionysian Apple Distillate").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§7Crisp sparkling cider distilled from fermented apples.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§eConsuming grants:").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  §6• Absorption II (3:00)"));
        tooltip.add(Component.literal("  §d• Regeneration II (0:45)"));
        tooltip.add(Component.literal("  §b• Speed II (3:00)"));
        tooltip.add(Component.literal("  §e• Haste II (3:00)"));
        tooltip.add(Component.literal("  §c• Fire Resistance (5:00)"));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§a⚡ Instantly restores 60,000 RF to carried items!").withStyle(ChatFormatting.GREEN));
    }
}

