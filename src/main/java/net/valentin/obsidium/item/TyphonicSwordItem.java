package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class TyphonicSwordItem extends SwordItem {
    public TyphonicSwordItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.igniteForSeconds(10);
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!player.getCooldowns().isOnCooldown(this)) {
            player.getCooldowns().addCooldown(this, 60);

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.9F);

            if (level instanceof ServerLevel serverLevel) {
                Vec3 look = player.getLookAngle();
                Vec3 origin = player.getEyePosition();

                for (int i = 1; i <= 6; i++) {
                    Vec3 pos = origin.add(look.scale(i));
                    serverLevel.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 8, 0.3, 0.3, 0.3, 0.05);
                    serverLevel.sendParticles(ParticleTypes.LAVA, pos.x, pos.y, pos.z, 2, 0.2, 0.2, 0.2, 0.02);

                    AABB box = new AABB(pos.x - 1.2, pos.y - 1.2, pos.z - 1.2, pos.x + 1.2, pos.y + 1.2, pos.z + 1.2);
                    List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive());
                    for (LivingEntity entity : targets) {
                        entity.hurt(serverLevel.damageSources().playerAttack(player), 7.0F);
                        entity.igniteForSeconds(8);
                        entity.setDeltaMovement(entity.getDeltaMovement().add(look.x * 0.5, 0.3, look.z * 0.5));
                    }
                }
                stack.hurtAndBreak(2, player, EquipmentSlot.MAINHAND);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6Typhonic Eruption: §7Ignites targets for 10 seconds.").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§cFlame Wave: §7[Right-Click] Shoots a piercing fire arc (3s CD).").withStyle(ChatFormatting.RED));
    }
}

