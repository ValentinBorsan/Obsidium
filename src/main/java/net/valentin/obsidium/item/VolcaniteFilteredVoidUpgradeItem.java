package net.valentin.obsidium.item;

import net.minecraft.core.HolderLookup;
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
 * Selective Void Module for the Volcanite Modular Chest.
 * Eliminates chosen items inside the Modular Chest based on its 9-slot Whitelist/Blacklist filter.
 */
public class VolcaniteFilteredVoidUpgradeItem extends Item {

    public VolcaniteFilteredVoidUpgradeItem(Properties properties) {
        super(properties.fireResistant());
    }

    /**
     * Evaluates whether {@code candidate} should be voided by the given Selective Void Module stack.
     * An unconfigured (empty) filter never voids items in either mode to prevent accidental item loss.
     */
    public static boolean shouldVoid(ItemStack moduleStack, ItemStack candidate, HolderLookup.Provider registries) {
        if (moduleStack.isEmpty() || candidate.isEmpty() || registries == null) return false;
        NonNullList<ItemStack> filterItems = HermesFilterUpgradeItem.getFilterItems(moduleStack, registries);
        boolean blacklist = HermesFilterUpgradeItem.isBlacklist(moduleStack);

        boolean hasAnyFilter = false;
        boolean listed = false;
        for (ItemStack fi : filterItems) {
            if (!fi.isEmpty()) {
                hasAnyFilter = true;
                if (ItemStack.isSameItem(fi, candidate)) {
                    listed = true;
                    break;
                }
            }
        }

        if (!hasAnyFilter) {
            return false;
        }
        return blacklist ? !listed : listed;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider(
                    (syncId, inv, p) -> new HermesFilterMenu(syncId, inv, stack, hand),
                    Component.literal("Selective Void Filter")
            ));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§5★ Selective Void Upgrade (Filtered)"));
        tooltip.add(Component.literal("§7Eliminates selected items in a §eVolcanite Modular Chest"));
        tooltip.add(Component.literal("§7Right-click in hand to configure 9-slot filter"));

        boolean blacklist = HermesFilterUpgradeItem.isBlacklist(stack);
        tooltip.add(Component.literal(blacklist
                ? "§cMode: Blacklist (Keep Listed, Void Others)"
                : "§aMode: Whitelist (Void Listed Items)"));

        if (context.registries() != null) {
            NonNullList<ItemStack> filterItems = HermesFilterUpgradeItem.getFilterItems(stack, context.registries());
            List<String> names = new ArrayList<>();
            for (ItemStack fi : filterItems) {
                if (!fi.isEmpty()) {
                    names.add(fi.getHoverName().getString());
                }
            }
            if (names.isEmpty()) {
                tooltip.add(Component.literal("§8Configured: None (voids nothing)"));
            } else {
                tooltip.add(Component.literal("§7Filter (" + names.size() + "/9): §f" + String.join(", ", names)));
            }
        }
        tooltip.add(Component.literal("§8Socket into a Volcanite Modular Chest to activate."));
    }
}
