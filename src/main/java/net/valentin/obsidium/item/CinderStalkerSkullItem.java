package net.valentin.obsidium.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.entity.CinderStalker;

import java.util.List;

public class CinderStalkerSkullItem extends Item {
    private static final int SKULLS_REQUIRED = 3;

    public CinderStalkerSkullItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel serverLevel) || context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }

        BlockPos topCenter = context.getClickedPos();
        if (!isRitualBlock(level, topCenter)) return InteractionResult.PASS;

        Direction armDirection = findRitualDirection(level, topCenter);
        if (armDirection == null) return InteractionResult.PASS;

        if (context.getItemInHand().getCount() < SKULLS_REQUIRED) {
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.literal("The ritual requires three Cinder Stalker Skulls."), true);
            }
            return InteractionResult.FAIL;
        }

        CinderStalker stalker = ObsidiumMod.CINDER_STALKER.create(serverLevel);
        if (stalker == null) return InteractionResult.FAIL;

        stalker.moveTo(topCenter.getX() + 0.5, topCenter.getY() + 1.0, topCenter.getZ() + 0.5,
                context.getPlayer() == null ? 0.0F : context.getPlayer().getYRot(), 0.0F);
        stalker.awakenFromRitual();
        if (!serverLevel.addFreshEntity(stalker)) return InteractionResult.FAIL;

        BlockPos stem = topCenter.below();
        level.removeBlock(stem, false);
        level.removeBlock(topCenter, false);
        level.removeBlock(topCenter.relative(armDirection), false);
        level.removeBlock(topCenter.relative(armDirection.getOpposite()), false);

        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(SKULLS_REQUIRED);
        }
        level.playSound(null, topCenter, SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS, 1.0F, 0.8F);
        return InteractionResult.CONSUME;
    }

    private static Direction findRitualDirection(Level level, BlockPos topCenter) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (isRitualBlock(level, topCenter.below())
                    && isRitualBlock(level, topCenter.relative(direction))
                    && isRitualBlock(level, topCenter.relative(direction.getOpposite()))) {
                return direction;
            }
        }
        return null;
    }

    private static boolean isRitualBlock(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(ObsidiumMod.VOLCANITE_BLOCK);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Use 3 on the center of a T-shaped Volcanite Block shrine."));
    }
}
