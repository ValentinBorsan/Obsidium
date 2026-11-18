package net.valentin.mythicrails;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/** Handheld linker to bind Signal Connectors to a Rail Control System. */
public final class SignalLinkerItem extends Item {
    public SignalLinkerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        var player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (level.getBlockEntity(pos) instanceof RailControlBlockEntity) {
            if (!level.isClientSide) {
                CompoundTag tag = new CompoundTag();
                tag.putInt("CtrlX", pos.getX());
                tag.putInt("CtrlY", pos.getY());
                tag.putInt("CtrlZ", pos.getZ());
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                if (player != null) {
                    player.displayClientMessage(Component.literal("Bound Signal Linker to Rail Control System at " + pos.toShortString())
                            .withStyle(ChatFormatting.GOLD), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (level.getBlockEntity(pos) instanceof SignalConnectorBlockEntity connector) {
            if (!level.isClientSide) {
                CustomData data = stack.get(DataComponents.CUSTOM_DATA);
                if (data == null || !data.copyTag().contains("CtrlX")) {
                    if (player != null) {
                        player.displayClientMessage(Component.literal("Right-click a Rail Control System first to bind the linker!")
                                .withStyle(ChatFormatting.RED), true);
                    }
                    return InteractionResult.FAIL;
                }
                CompoundTag tag = data.copyTag();
                BlockPos ctrlPos = new BlockPos(tag.getInt("CtrlX"), tag.getInt("CtrlY"), tag.getInt("CtrlZ"));
                if (level.getBlockEntity(ctrlPos) instanceof RailControlBlockEntity ctrl) {
                    if (ctrl.linkConnector(pos)) {
                        connector.setControlPos(ctrlPos);
                        if (player != null) {
                            player.displayClientMessage(Component.literal("Linked Signal Connector at " + pos.toShortString())
                                    .withStyle(ChatFormatting.GREEN), true);
                        }
                    } else if (player != null) {
                        player.displayClientMessage(Component.literal("Rail Control System is full (max 8 connectors)!")
                                .withStyle(ChatFormatting.RED), true);
                    }
                } else if (player != null) {
                    player.displayClientMessage(Component.literal("Bound Rail Control System not found or unloaded!")
                            .withStyle(ChatFormatting.RED), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mythic_rails.signal_linker.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mythic_rails.signal_linker.2").withStyle(ChatFormatting.GRAY));
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.copyTag().contains("CtrlX")) {
            CompoundTag tag = data.copyTag();
            tooltip.add(Component.literal("Bound: " + tag.getInt("CtrlX") + ", " + tag.getInt("CtrlY") + ", " + tag.getInt("CtrlZ"))
                    .withStyle(ChatFormatting.GOLD));
        }
    }
}
