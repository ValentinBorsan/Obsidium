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

public class AgedTuicaItem extends Item {

    private final int tier; // 1 = Vintage (Învechită), 2 = Grand Reserve (Arhontească)

    public AgedTuicaItem(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 36;
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
            long energyRecharge = (tier >= 2) ? 1_000_000L : 250_000L;

            if (tier >= 2) {
                // Grand Reserve / Țuică Arhontească
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 20 * 300, 3)); // Haste IV (5 min)
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 300, 3)); // Speed IV (5 min)
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 300, 3)); // Strength IV (5 min)
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 300, 2)); // Resistance III (5 min)
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 60, 2)); // Regeneration III (1 min)
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 600, 0)); // Fire Resistance (10 min)
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 * 5, 0)); // 5 sec kick
            } else {
                // Vintage / Țuică Învechită
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 20 * 240, 2)); // Haste III (4 min)
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 240, 2)); // Speed III (4 min)
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 240, 2)); // Strength III (4 min)
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 240, 1)); // Resistance II (4 min)
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 480, 0)); // Fire Resistance (8 min)
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 * 4, 0));
            }

            // Recharge carried items
            long remainingEnergy = energyRecharge;
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

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        tier >= 2 ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.FLAME,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        35, 0.5, 0.6, 0.5, 0.1
                );
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0F, 0.9F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);

            String title = (tier >= 2) ? "§6[Grand Reserve Spirit] §dThe fire of the gods awakes! +1,000,000 RF recharged!"
                    : "§6[Vintage Volcanic Spirit] §eAged to perfection! +250,000 RF recharged!";
            player.displayClientMessage(Component.literal(title).withStyle(ChatFormatting.GOLD), true);

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
        if (tier >= 2) {
            tooltip.add(Component.literal("§6👑 Dionysian Grand Reserve").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal("§7Aged over extended time in Dionysian Oak Barrels.").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("§e⚡ Full Recharge: +1,000,000 RF into equipped tools & armor.").withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal("§a⚔ Haste IV, Speed IV, Strength IV, Resistance III, Regen III.").withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.literal("§d🍷 Vintage Volcanic Spirit").withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.add(Component.literal("§7Matured in oak barrels for rich volcanic notes.").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("§e⚡ Recharges: +250,000 RF into tools & batteries.").withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal("§a⚔ Haste III, Speed III, Strength III, Resistance II.").withStyle(ChatFormatting.GREEN));
        }
    }
}

