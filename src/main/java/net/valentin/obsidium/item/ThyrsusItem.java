package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import team.reborn.energy.api.base.SimpleEnergyItem;

import java.util.ArrayList;
import java.util.List;

public class ThyrsusItem extends Item implements SimpleEnergyItem {

    public static final long CAPACITY = 250_000L;
    public static final long FRENZY_COST = 2_500L;
    public static final long FEAST_COST = 10_000L;

    public ThyrsusItem(Properties properties) {
        super(properties);
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return CAPACITY;
    }

    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return 10_000L;
    }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return 0L;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            // Shift + Right-Click: Sacramental Feast
            if (getStoredEnergy(stack) >= FEAST_COST) {
                if (!level.isClientSide()) {
                    setStoredEnergy(stack, getStoredEnergy(stack) - FEAST_COST);

                    // Cleanse harmful effects
                    List<MobEffectInstance> toRemove = new ArrayList<>();
                    for (MobEffectInstance effect : player.getActiveEffects()) {
                        if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                            toRemove.add(effect);
                        }
                    }
                    for (MobEffectInstance effect : toRemove) {
                        player.removeEffect(effect.getEffect());
                    }

                    // Restore hunger & saturation
                    player.getFoodData().eat(10, 10.0F);
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 10, 1));
                    player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 60, 1));

                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.2, player.getZ(), 16, 0.4, 0.4, 0.4, 0.1);
                        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 0.8, player.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
                    }
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.8F, 1.2F);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.0F);

                    player.displayClientMessage(Component.literal("§d[Thyrsus of Dionysus] §aBanchetul Sacru: efectele negative au fost alungate și foamea potolită!").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            } else {
                if (!level.isClientSide()) {
                    player.displayClientMessage(Component.literal("§c[Thyrsus] Energie insuficientă pentru Banchet (Necesită 10,000 RF)!").withStyle(ChatFormatting.RED), true);
                }
                return InteractionResultHolder.fail(stack);
            }
        } else {
            // Normal Right-Click: Dionysian Frenzy Wave
            if (getStoredEnergy(stack) >= FRENZY_COST) {
                if (!level.isClientSide()) {
                    setStoredEnergy(stack, getStoredEnergy(stack) - FRENZY_COST);

                    double radius = 10.0;
                    AABB box = player.getBoundingBox().inflate(radius);
                    List<Monster> monsters = level.getEntitiesOfClass(Monster.class, box, m -> m.isAlive() && !m.isAlliedTo(player));

                    for (int i = 0; i < monsters.size(); i++) {
                        Monster target = monsters.get(i);
                        // Confuse and target nearest other monster
                        if (monsters.size() > 1) {
                            Monster other = monsters.get((i + 1) % monsters.size());
                            target.setTarget(other);
                        }
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 12, 1));
                        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 12, 1));
                        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 12, 0));
                    }

                    if (level instanceof ServerLevel serverLevel) {
                        for (double theta = 0; theta < Math.PI * 4; theta += 0.3) {
                            double r = (theta / (Math.PI * 4)) * radius;
                            double px = player.getX() + r * Math.cos(theta);
                            double pz = player.getZ() + r * Math.sin(theta);
                            serverLevel.sendParticles(ParticleTypes.WITCH, px, player.getY() + 0.5, pz, 1, 0, 0, 0, 0);
                        }
                    }

                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.2F);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.8F, 1.4F);

                    player.displayClientMessage(Component.literal("§d[Thyrsus of Dionysus] §eFrenezie dionisiacă dezlănțuită peste " + monsters.size() + " inamici!").withStyle(ChatFormatting.GOLD), true);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            } else {
                if (!level.isClientSide()) {
                    player.displayClientMessage(Component.literal("§c[Thyrsus] Energie insuficientă (Necesită 2,500 RF)!").withStyle(ChatFormatting.RED), true);
                }
                return InteractionResultHolder.fail(stack);
            }
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round((double) getStoredEnergy(stack) * 13.0 / (double) CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xC084FC; // Dionysian Purple
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        long stored = getStoredEnergy(stack);
        tooltip.add(Component.literal("§d🍇 Tirul lui Dionysos §7(Thyrsus of Dionysus)").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("§e⚡ Energie: §f" + String.format("%,d", stored) + " §e/ §f" + String.format("%,d", CAPACITY) + " RF").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§7• Click Dreapta: Frenezie dionisiacă (inamicii se atacă între ei, 2,500 RF).").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7• Shift + Click Dreapta: Banchet sacru (elimină efectele negative și satură, 10,000 RF).").withStyle(ChatFormatting.GRAY));
    }
}

