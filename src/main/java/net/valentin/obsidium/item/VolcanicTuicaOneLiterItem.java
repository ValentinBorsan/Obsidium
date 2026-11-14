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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.valentin.obsidium.ObsidiumMod;
import team.reborn.energy.api.base.SimpleEnergyItem;

import java.util.List;

/**
 * 1-Liter Custom Bottle of Volcanic Țuică.
 * Contains 4 authentic hearty sips (250 ml each).
 * Each sip grants high-tier buffs and recharges 100,000 RF into held & carried batteries/tools.
 * Upon finishing the 4th sip, returns an Empty 1L Bottle.
 */
public class VolcanicTuicaOneLiterItem extends Item {

    public static final int MAX_SIPS = 4;
    public static final long ENERGY_RESTORE_PER_SIP = 100_000L;

    public VolcanicTuicaOneLiterItem(Properties properties) {
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
            // 1. Grand Dionysian Buffs
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 20 * 240, 1)); // Haste II (4 min)
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 240, 1)); // Speed II (4 min)
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 240, 1)); // Strength II (4 min)
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 360, 0)); // Fire Resistance (6 min)
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 * 4, 0)); // 4 sec authentic Țuică Kick!

            // 2. Recharge carried energy items (up to 100,000 RF per sip)
            long remainingEnergy = ENERGY_RESTORE_PER_SIP;
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
                serverLevel.sendParticles(
                        ParticleTypes.FLAME,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        25, 0.45, 0.5, 0.45, 0.09
                );
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0F, 0.9F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.9F, 1.3F);

            int currentDamage = stack.getDamageValue();
            int remainingSipsAfter = MAX_SIPS - (currentDamage + 1);

            if (remainingSipsAfter > 0) {
                player.displayClientMessage(
                        Component.literal("§6[Volcanic Demi-John] §eYou took a hearty sip! (+100,000 RF). §a" 
                                + remainingSipsAfter + "§e / 4 sips left.")
                                .withStyle(ChatFormatting.GOLD),
                        true
                );
            } else {
                player.displayClientMessage(
                        Component.literal("§6[Volcanic Demi-John] §eYou emptied the 1-Liter demi-john! The volcanic fire burns within you!")
                                .withStyle(ChatFormatting.GOLD),
                        true
                );
            }

            // Durability handling: 4 uses
            if (currentDamage + 1 >= MAX_SIPS) {
                return new ItemStack(ObsidiumMod.EMPTY_TUICA_BOTTLE_1L);
            } else {
                ItemStack updated = stack.copy();
                updated.setDamageValue(currentDamage + 1);
                return updated;
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int remaining = MAX_SIPS - stack.getDamageValue();
        tooltip.add(Component.literal("§6✦ Traditional Wicker Demi-John"));
        tooltip.add(Component.literal("§eCapacity: §f1 Liter (1,000 mB)"));
        tooltip.add(Component.literal("§eRemaining Sips: §a" + remaining + " / " + MAX_SIPS + " §8(250 ml / sip)"));
        tooltip.add(Component.literal("§7Each sip grants:"));
        tooltip.add(Component.literal("  §8• §bHaste II & Speed II §8(4 min)"));
        tooltip.add(Component.literal("  §8• §cStrength II & Fire Resistance §8(4-6 min)"));
        tooltip.add(Component.literal("  §8• §e+100,000 RF §7recharge in carried tools & batteries"));
        tooltip.add(Component.literal("§8Refill in Dionysian Distillery with an empty demi-john."));
    }
}

