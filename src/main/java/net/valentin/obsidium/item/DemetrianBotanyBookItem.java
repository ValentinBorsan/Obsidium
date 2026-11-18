package net.valentin.obsidium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class DemetrianBotanyBookItem extends Item {

    public DemetrianBotanyBookItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            try {
                Class<?> clazz = Class.forName("net.valentin.obsidium.screen.ClientHooks");
                clazz.getMethod("openBotanyBookScreen").invoke(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.obsidium.botany_book.title").withStyle(net.minecraft.ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.obsidium.botany_book.description").withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.obsidium.botany_book.chapters_grafting").withStyle(net.minecraft.ChatFormatting.GREEN));
        tooltip.add(Component.translatable("tooltip.obsidium.botany_book.chapters_resources").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.obsidium.botany_book.open").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
