package net.valentin.obsidium.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class KeraunosHVPortBlock extends Block {
    public KeraunosHVPortBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal("⚡ High-Voltage Multiblock I/O Bushing").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Place in the outer 3x3x3 shell of a Keraunos High-Voltage Matrix.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Transfers up to 25M RF/t (50M RF/t in Overvoltage Mode) and opens the HV Core GUI.").withStyle(ChatFormatting.GOLD));
    }

    @Nullable
    public static KeraunosHVCoreBlockEntity findCore(Level level, BlockPos portPos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos checkPos = portPos.offset(dx, dy, dz);
                    if (level.getBlockEntity(checkPos) instanceof KeraunosHVCoreBlockEntity core) {
                        return core;
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            KeraunosHVCoreBlockEntity core = findCore(level, pos);
            if (core != null) {
                core.handlePlayerInteraction(level, pos, player);
            } else {
                player.displayClientMessage(Component.literal("⚠ No Keraunos High-Voltage Core found at the center of this 3x3x3 structure!").withStyle(ChatFormatting.RED), true);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            KeraunosHVCoreBlockEntity core = findCore(level, pos);
            if (core != null) {
                core.handlePlayerInteraction(level, pos, player);
            } else {
                player.displayClientMessage(Component.literal("⚠ No Keraunos High-Voltage Core found at the center of this 3x3x3 structure!").withStyle(ChatFormatting.RED), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            KeraunosHVCoreBlockEntity core = findCore(level, pos);
            if (core != null && core.isEnergized()) {
                core.triggerArcBlast(level, pos, player);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}

