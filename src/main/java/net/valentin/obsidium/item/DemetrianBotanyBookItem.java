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
        tooltip.add(Component.literal("§6📖 Herbarul Sacru al Demetrei"));
        tooltip.add(Component.literal("§7Codex complet de botanică, altoire și hibridare."));
        tooltip.add(Component.literal("§eCapitole: §aAltoire Pas cu Pas§7, §6Arborele Genealogic§7,"));
        tooltip.add(Component.literal("§dPomii de Minereu§7 și §2Butoiul de Puieți"));
        tooltip.add(Component.literal("§8Click-dreapta pentru a deschide cartea"));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
