package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import team.reborn.energy.api.base.SimpleEnergyItem;

import java.util.List;

public class HephaesteanSwordItem extends SwordItem implements SimpleEnergyItem {

    public static final long CAPACITY = 1_000_000L;
    public static final long MAX_INPUT = 50_000L;
    public static final long ENERGY_PER_HIT = 250L;
    public static final long ENERGY_ERUPTION = 5_000L;

    public HephaesteanSwordItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) { return CAPACITY; }

    @Override
    public long getEnergyMaxInput(ItemStack stack) { return MAX_INPUT; }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) { return 0; }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.setDamageValue(0); // Zero durability wear

        if (getStoredEnergy(stack) >= ENERGY_PER_HIT) {
            tryUseEnergy(stack, ENERGY_PER_HIT);
            target.igniteForSeconds(15);
        } else if (attacker instanceof Player player && !player.level().isClientSide()) {
            player.displayClientMessage(Component.literal("§c⚡ Low Energy! Re-charge sword in a battery or charger.").withStyle(ChatFormatting.RED), true);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!player.getCooldowns().isOnCooldown(this)) {
            if (getStoredEnergy(stack) < ENERGY_ERUPTION) {
                if (!level.isClientSide()) {
                    player.displayClientMessage(Component.literal("§c⚡ Not enough energy for Magma Eruption! (Requires 5,000 RF)").withStyle(ChatFormatting.RED), true);
                }
                return InteractionResultHolder.fail(stack);
            }

            tryUseEnergy(stack, ENERGY_ERUPTION);
            player.getCooldowns().addCooldown(this, 40); // 2 second cooldown

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 0.8F);

            if (level instanceof ServerLevel serverLevel) {
                Vec3 look = player.getLookAngle();
                Vec3 origin = player.getEyePosition();

                for (int i = 1; i <= 10; i++) {
                    Vec3 pos = origin.add(look.scale(i));
                    serverLevel.sendParticles(ParticleTypes.LAVA, pos.x, pos.y, pos.z, 12, 0.4, 0.4, 0.4, 0.1);
                    serverLevel.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 15, 0.3, 0.3, 0.3, 0.08);

                    AABB box = new AABB(pos.x - 1.5, pos.y - 1.5, pos.z - 1.5, pos.x + 1.5, pos.y + 1.5, pos.z + 1.5);
                    List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive());
                    for (LivingEntity entity : targets) {
                        entity.hurt(serverLevel.damageSources().playerAttack(player), 18.0F);
                        entity.igniteForSeconds(15);
                        entity.setDeltaMovement(entity.getDeltaMovement().add(look.x * 0.8, 0.6, look.z * 0.8));
                    }
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        return super.use(level, player, hand);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) { return true; }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round((double) getStoredEnergy(stack) * 13.0 / (double) CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) { return 0x00E5FF; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        long stored = getStoredEnergy(stack);
        tooltip.add(Component.literal("§b⚡ Energy: §f" + String.format("%,d", stored) + " §b/ §f" + String.format("%,d", CAPACITY) + " RF").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("§6Magma Eruption: §7[Right-Click] Fires a devastating volcanic shockwave (5k RF).").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§eHephaestean Masterwork: §7Immortal blade powered by RF energy.").withStyle(ChatFormatting.YELLOW));
    }
}

