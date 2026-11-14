package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.valentin.obsidium.ObsidiumMod;

import java.util.List;

public class ArtemisBowItem extends BowItem {

    public ArtemisBowItem(Properties properties) {
        super(properties);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;

        boolean hasSilverFruit = player.getInventory().contains(new ItemStack(ObsidiumMod.ARTEMIS_SILVER_FRUIT));
        boolean creative = player.getAbilities().instabuild;
        ItemStack ammo = player.getProjectile(stack);

        if (ammo.isEmpty() && !hasSilverFruit && !creative) {
            return;
        }

        int useDuration = this.getUseDuration(stack, entity) - timeLeft;
        float power = getPowerForTime(useDuration);
        if ((double) power < 0.1D) return;

        if (!level.isClientSide()) {
            ItemStack projectileStack = ammo.isEmpty() ? new ItemStack(Items.ARROW) : ammo;
            net.minecraft.world.item.ArrowItem arrowItem = (projectileStack.getItem() instanceof net.minecraft.world.item.ArrowItem ai)
                    ? ai : (net.minecraft.world.item.ArrowItem) Items.ARROW;
            AbstractArrow arrow = arrowItem.createArrow(level, projectileStack, player, stack);
            
            // Artemis Lunar Bow: balanced velocity & moonlight damage (never 1-shots regular mobs)
            float velocity = power * 3.15F;
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, velocity, 0.4F);
            arrow.setBaseDamage(arrow.getBaseDamage() + 0.6);

            if (power == 1.0F && level.getRandom().nextFloat() < 0.25F) {
                arrow.setCritArrow(true);
            }

            // Silver fruit grants infinite lunar arrows
            if (hasSilverFruit || creative) {
                arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            } else if (!ammo.isEmpty()) {
                ammo.shrink(1);
                if (ammo.isEmpty()) {
                    player.getInventory().removeItem(ammo);
                }
            }

            level.addFreshEntity(arrow);

            // Lunar release particle trail & silver resonance chime
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        player.getX(), player.getEyeY() - 0.1, player.getZ(),
                        10, 0.3, 0.3, 0.3, 0.05);
            }
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                1.0F, 1.35F / (level.getRandom().nextFloat() * 0.4F + 1.2F));

        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean hasSilverFruit = player.getInventory().contains(new ItemStack(ObsidiumMod.ARTEMIS_SILVER_FRUIT));
        boolean hasAmmo = !player.getProjectile(stack).isEmpty();

        if (player.getAbilities().instabuild || hasAmmo || hasSilverFruit) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.add(Component.literal("§d🏹 Bow of Artemis (Arcul Zeiței Artemis)").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("§7• Infuses arrows with §fLunar Moonlight §7(+Lunar velocity & bonus damage)."));
        tooltip.add(Component.literal("§7• §bInfinite Arrows§7: Carried §eArtemis Silver Fruit §7grants endless lunar arrows!"));
        tooltip.add(Component.literal("§8Divine Relic of the Huntress | Durability: 1,150"));
    }
}
