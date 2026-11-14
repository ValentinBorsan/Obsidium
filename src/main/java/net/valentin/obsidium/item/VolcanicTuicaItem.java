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

public class VolcanicTuicaItem extends Item {

    public static final long ENERGY_RESTORE_AMOUNT = 100_000L;

    public VolcanicTuicaItem(Properties properties) {
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
            // 1. Energetic Buffs
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 20 * 180, 1)); // Haste II (3 min)
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 180, 1)); // Speed II (3 min)
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 180, 1)); // Strength II (3 min)
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 300, 0)); // Fire Resistance (5 min)
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 * 4, 0)); // 4 sec authentic Țuică Kick!

            // 2. "Iti da energie": Recharge carried energy items (up to 100,000 RF)
            long remainingEnergy = ENERGY_RESTORE_AMOUNT;

            // Search player main inventory, armor, and offhand
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
                        20, 0.4, 0.5, 0.4, 0.08
                );
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.9F, 1.0F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.4F);

            player.displayClientMessage(
                    Component.literal("§6[Volcanic Spirit] §eYou felt the Dionysian surge! +100,000 RF recharged into tools & batteries!")
                            .withStyle(ChatFormatting.GOLD),
                    true
            );

            // Handle empty glass bottle return
            stack.shrink(1);
            ItemStack emptyBottle = new ItemStack(Items.GLASS_BOTTLE);
            if (stack.isEmpty()) {
                return emptyBottle;
            } else {
                if (!player.getInventory().add(emptyBottle)) {
                    player.drop(emptyBottle, false);
                }
                return stack;
            }
        }

        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§d🍇 Dionysian Sacred Spirit §7(52% Volcanic Alc.)").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("§e⚡ Recharges up to 100,000 RF into carried items & batteries.").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§a⚔ Grants Haste II, Strength II, Speed II & Fire Resistance.").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("§7§oDistilled from sacred mineral fruits in the Dionysian Distillery.").withStyle(ChatFormatting.DARK_GRAY));
    }
}

