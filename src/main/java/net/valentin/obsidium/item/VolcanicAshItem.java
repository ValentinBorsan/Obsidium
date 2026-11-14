package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.block.VolcanicOreCropBlock;

import java.util.List;

public class VolcanicAshItem extends Item {
    public VolcanicAshItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (state.getBlock() instanceof VolcanicOreCropBlock crop) {
            int currentAge = crop.getAge(state);
            if (currentAge < crop.getMaxAge()) {
                if (!level.isClientSide()) {
                    int boost = 2 + level.random.nextInt(2);
                    int newAge = Math.min(crop.getMaxAge(), currentAge + boost);
                    level.setBlock(pos, crop.getStateForAge(newAge), 2);
                    if (player != null && !player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.85F, 1.4F);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 12, 0.2, 0.2, 0.2, 0.04);
                        serverLevel.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 8, 0.15, 0.15, 0.15, 0.02);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("§6★ Sacred Magmatic Fertilizer").withStyle(ChatFormatting.GOLD));
        tooltipComponents.add(Component.literal("§eRight-Click on Volcanic Crops: §fAdvances growth by 2-3 stages").withStyle(ChatFormatting.YELLOW));
        tooltipComponents.add(Component.literal("§7Volcanic crops also grow 2x faster near Lava & Magma Blocks").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
