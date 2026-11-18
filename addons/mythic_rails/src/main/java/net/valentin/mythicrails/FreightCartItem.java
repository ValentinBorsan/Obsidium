package net.valentin.mythicrails;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gameevent.GameEvent;

public final class FreightCartItem extends Item {
    public FreightCartItem(Properties properties) { super(properties); }

    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context,
            java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.mythic_rails.cart.capacity")
                .withStyle(net.minecraft.ChatFormatting.GOLD));
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.mythic_rails.coupling").withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.mythic_rails.cart.placement")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.mythic_rails.cart.stations")
                .withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        if (!state.is(BlockTags.RAILS)) return InteractionResult.FAIL;
        if (level instanceof ServerLevel server) {
            var shape = state.getBlock() instanceof BaseRailBlock rail
                    ? state.getValue(rail.getShapeProperty()) : RailShape.NORTH_SOUTH;
            var cart = new HermesFreightCart(MythicRails.FREIGHT_CART, server);
            cart.setPos(pos.getX() + 0.5, pos.getY() + 0.0625 + (shape.isAscending() ? 0.5 : 0), pos.getZ() + 0.5);
            var stack = context.getItemInHand();
            if (stack.has(DataComponents.CUSTOM_NAME)) cart.setCustomName(stack.get(DataComponents.CUSTOM_NAME));
            if (!server.addFreshEntity(cart)) return InteractionResult.FAIL;
            server.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, pos);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
