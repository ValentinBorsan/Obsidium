package net.valentin.obsidium.item;

import net.minecraft.core.NonNullList;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Precision Compactor Module for the Volcanite Modular Chest.
 * Auto-crafts 3x3 and 2x2 recipes only for items permitted by its 9-slot Whitelist/Blacklist filter.
 */
public class VolcaniteFilteredCompactorUpgradeItem extends Item {

    public VolcaniteFilteredCompactorUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider(
                    (syncId, inv, p) -> new HermesFilterMenu(syncId, inv, stack, hand),
                    Component.literal("Precision Compactor Filter")
            ));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§eFiltered Auto-Compactor (3×3 & 2×2)"));
        tooltip.add(Component.literal("§7Right-click in hand to configure 9-slot filter"));
        tooltip.add(Component.literal("§8Does not block unfiltered items from entering chest"));

        boolean blacklist = HermesFilterUpgradeItem.isBlacklist(stack);
        tooltip.add(Component.literal(blacklist ? "§cMode: Blacklist (Skip Listed)" : "§aMode: Whitelist (Compact Only Listed)"));

        if (context.registries() != null) {
            NonNullList<ItemStack> filterItems = HermesFilterUpgradeItem.getFilterItems(stack, context.registries());
            List<String> names = new ArrayList<>();
            for (ItemStack fi : filterItems) {
                if (!fi.isEmpty()) {
                    names.add(fi.getHoverName().getString());
                }
            }
            if (names.isEmpty()) {
                tooltip.add(Component.literal(blacklist
                        ? "§8Configured: None (compacts all eligible)"
                        : "§8Configured: None (compacts nothing)"));
            } else {
                tooltip.add(Component.literal("§7Filter (" + names.size() + "/9): §f" + String.join(", ", names)));
            }
        }
    }
}

