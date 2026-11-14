package net.valentin.obsidium.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Organic Dionysian Pomace (Tescovină Dionisiacă).
 * Concentrated fibrous byproduct of pressing mineral & mythical fruits.
 * Multifunctional resource:
 *  - Distillable into Volcanic Tuica (Grappa) in the Dionysian Distillery
 *  - High-yield compost input in the Demetrian Composter (4x Bone Meal)
 *  - Potent 2x agricultural fertilizer
 *  - Animal breeding feed & baby acceleration
 *  - Long-burning thermal fuel (1,600 ticks)
 */
public class DionysianPomaceItem extends Item {

    public DionysianPomaceItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand hand) {
        if (interactionTarget instanceof Animal animal) {
            Level level = animal.level();
            if (animal.isBaby()) {
                if (!level.isClientSide()) {
                    animal.ageUp((int) (Animal.getSpeedUpSecondsWhenFeeding(-animal.getAge()) * 0.1F), true);
                    if (level instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, animal.getX(), animal.getY() + 0.5, animal.getZ(), 8, 0.2, 0.2, 0.2, 0.02);
                    }
                    level.playSound(null, animal.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.6F, 1.2F);
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            } else if (animal.getAge() == 0 && !animal.isInLove()) {
                if (!level.isClientSide()) {
                    animal.setInLove(player);
                    if (level instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HEART, animal.getX(), animal.getY() + 0.7, animal.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
                    }
                    level.playSound(null, animal.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.6F, 1.0F);
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        return super.interactLivingEntity(stack, player, interactionTarget, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        if (applyPomaceFertilizer(stack, level, pos)) {
            if (!level.isClientSide()) {
                level.levelEvent(1505, pos, 0); // Bonemeal particles & sound
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.05);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return super.useOn(context);
    }

    public static boolean applyPomaceFertilizer(ItemStack stack, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BonemealableBlock bonemealable) {
            if (bonemealable.isValidBonemealTarget(level, pos, state)) {
                if (level instanceof ServerLevel serverLevel) {
                    if (bonemealable.isBonemealSuccess(level, level.random, pos, state)) {
                        bonemealable.performBonemeal(serverLevel, level.random, pos, state);
                        // Extra boost: 2nd growth burst for mythical pomace potency!
                        BlockState newState = level.getBlockState(pos);
                        if (newState.getBlock() instanceof BonemealableBlock second && second.isValidBonemealTarget(level, pos, newState)) {
                            second.performBonemeal(serverLevel, level.random, pos, newState);
                        }
                    }
                    stack.shrink(1);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6✦ Pure Fruit Pomace (Tescovină)"));
        tooltip.add(Component.literal("§7Concentrated organic byproduct from the §eDionysian Press§7."));
        tooltip.add(Component.literal("§8• §dDistillery: §fDistill into Volcanic Tuica (Grappa)"));
        tooltip.add(Component.literal("§8• §aComposter: §fYields 4x Bone Meal in Demetrian Composter"));
        tooltip.add(Component.literal("§8• §eAgriculture: §fRight-click crops for double growth burst"));
        tooltip.add(Component.literal("§8• §cAnimals: §fFeed livestock to breed or accelerate babies"));
        tooltip.add(Component.literal("§8• §6Fuel: §f1,600 burn ticks (smelts 8 items)"));
    }
}
