package net.valentin.obsidium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.valentin.obsidium.menu.HermesFilterMenu;

import java.util.List;

public class VolcaniteAdvancedFilterModuleItem extends Item {
    public VolcaniteAdvancedFilterModuleItem(Properties properties) {
        super(properties.fireResistant());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider(
                    (syncId, inv, p) -> new HermesFilterMenu(syncId, inv, stack, hand),
                    Component.literal("Advanced 9-Slot Filter Configuration")
            ));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        boolean blacklist = HermesFilterUpgradeItem.isBlacklist(stack);
        tooltipComponents.add(Component.literal("§d§lAdvanced Filter Module"));
        tooltipComponents.add(Component.literal("§7Mode: " + (blacklist ? "§c[BLACKLIST - Block listed]" : "§a[WHITELIST - Allow listed]")));
        tooltipComponents.add(Component.literal("§7• Sorting Station: expands face filter to §e3 items/liquids"));
        tooltipComponents.add(Component.literal("§7• Modular Chest: §e9-slot Whitelist/Blacklist §7(Right-click in hand)"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
