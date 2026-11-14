package net.valentin.obsidium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class MagmaForgeManualItem extends Item {
    public MagmaForgeManualItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            try {
                Class<?> clazz = Class.forName("net.valentin.obsidium.screen.ClientHooks");
                clazz.getMethod("openManualScreen").invoke(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6📖 Unified Obsidium Guidebook"));
        tooltip.add(Component.literal("§7Chapters: §ePower & Chemistry§7, §6Magma Forge 3×3§7,"));
        tooltip.add(Component.literal("§bEnergy Core 3×3 §7& §eKeraunos HV 3×3×3§7, §aStorage"));
        tooltip.add(Component.literal("§8Right-click to open the interactive codex"));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
