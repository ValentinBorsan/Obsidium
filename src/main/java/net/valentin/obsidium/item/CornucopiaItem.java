package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.valentin.obsidium.ObsidiumMod;

import java.util.List;

public class CornucopiaItem extends Item {

    public CornucopiaItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean isSelected) {
        if (!level.isClientSide() && entity instanceof Player player) {
            // Check if held in offhand or main hand
            boolean isHeld = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
            if (isHeld) {
                // Every 8 seconds (160 ticks), passively replenish hunger if starving or running low
                if (level.getGameTime() % 160 == 0) {
                    if (player.getFoodData().getFoodLevel() < 20) {
                        player.getFoodData().eat(1, 0.5F);
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 0.3F, 1.5F);
                    }
                }
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            player.getCooldowns().addCooldown(this, 20 * 45); // 45 sec cooldown

            // Gift of Demeter: Manifests orchard fruits
            double r = level.random.nextDouble();
            ItemStack bounty;
            if (r < 0.25) {
                bounty = new ItemStack(ObsidiumMod.HERMES_COPPER_FRUIT, 2);
            } else if (r < 0.50) {
                bounty = new ItemStack(ObsidiumMod.ARES_IRON_FRUIT, 1);
            } else if (r < 0.70) {
                bounty = new ItemStack(Items.GOLDEN_APPLE, 1);
            } else if (r < 0.90) {
                bounty = new ItemStack(Items.SWEET_BERRIES, 4);
            } else {
                bounty = new ItemStack(ObsidiumMod.MIDAS_GOLD_FRUIT, 1);
            }

            if (!player.getInventory().add(bounty)) {
                player.drop(bounty, false);
            }

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.4, 0.4, 0.4, 0.1);
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.4F);
            player.displayClientMessage(Component.literal("§6[Cornul Abundenței] §eAi primit darul Demetrei: §a" + bounty.getHoverName().getString() + " x" + bounty.getCount()).withStyle(ChatFormatting.GOLD), true);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6🌾 Cornul Abundenței §7(Cornucopia of Demeter)").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§e• În mână/offhand: Saturează pasiv jucătorul la fiecare câteva secunde.").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§a• Click Dreapta: Invocă fructe proaspete și sacre din belșug (cooldown 45s).").withStyle(ChatFormatting.GREEN));
    }
}

