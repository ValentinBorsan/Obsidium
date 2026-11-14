package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import team.reborn.energy.api.base.SimpleEnergyItem;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TalariaOfHermesItem extends ArmorItem implements SimpleEnergyItem {

    public static final long CAPACITY = 2_000_000L; // 2,000,000 RF
    public static final long MAX_INPUT = 50_000L;
    public static final long FLIGHT_COST_PER_TICK = 30L;

    public static final Set<UUID> HERMES_FLYING_PLAYERS = new HashSet<>();

    public TalariaOfHermesItem(Properties properties) {
        super(SupremeArmorMaterial.INSTANCE, Type.BOOTS, properties);
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return CAPACITY;
    }

    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return MAX_INPUT;
    }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return 0;
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
        return 0xFCD34D; // Celestial Golden Glow
    }

    public static void tickServerFlight(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isCreative() || player.isSpectator()) {
                HERMES_FLYING_PLAYERS.remove(player.getUUID());
                continue;
            }

            ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);
            ItemStack offhand = player.getOffhandItem();

            ItemStack talaria = ItemStack.EMPTY;
            if (feet.getItem() instanceof TalariaOfHermesItem) {
                talaria = feet;
            } else if (offhand.getItem() instanceof TalariaOfHermesItem) {
                talaria = offhand;
            }

            if (!talaria.isEmpty() && SimpleEnergyItem.getStoredEnergyUnchecked(talaria) >= FLIGHT_COST_PER_TICK) {
                HERMES_FLYING_PLAYERS.add(player.getUUID());

                if (!player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = true;
                    player.onUpdateAbilities();
                }

                // Negate any fall damage
                player.fallDistance = 0.0F;

                if (player.getAbilities().flying) {
                    // Consume energy
                    if (talaria.getItem() instanceof SimpleEnergyItem sei) {
                        sei.tryUseEnergy(talaria, FLIGHT_COST_PER_TICK);
                    }

                    // Golden divine feathers particle trail
                    if (player.level() instanceof ServerLevel sl && player.tickCount % 3 == 0) {
                        sl.sendParticles(ParticleTypes.WAX_OFF, player.getX(), player.getY() + 0.1, player.getZ(), 2, 0.2, 0.05, 0.2, 0.02);
                        sl.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 0.1, player.getZ(), 1, 0.1, 0.02, 0.1, 0.01);
                    }
                }
            } else {
                if (HERMES_FLYING_PLAYERS.remove(player.getUUID())) {
                    player.getAbilities().flying = false;
                    player.getAbilities().mayfly = false;
                    player.onUpdateAbilities();
                    player.fallDistance = 0.0F;
                    // Safe descent: bestow slow falling for 8 seconds
                    player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 8, 0, false, false, true));
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        long stored = getStoredEnergy(stack);
        tooltip.add(Component.literal("§6🪽 Talaria of Hermes §7(Winged Sandals of the Messenger)").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§e⚡ Energy: §f" + String.format("%,d", stored) + " §e/ §f" + String.format("%,d", CAPACITY) + " RF").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§b• Creative Flight: §7Active when worn on feet or held in offhand.").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("§a• Flight Consumption: §f30 RF/tick §7(~55 mins on full charge).").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("§6• Divine Protection: §7Complete fall damage immunity & safe slow descent.").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§7§oForged by Hephaestus from imperishable gold and celestial wind.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
