package net.valentin.obsidium.item;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.valentin.obsidium.menu.HermesFilterMenu;

import java.util.List;

public class HermesFilterUpgradeItem extends Item {
    public static final int FILTER_SLOTS = 9;

    public HermesFilterUpgradeItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    public static boolean isBlacklist(ItemStack filterStack) {
        CustomData data = filterStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getBoolean("Blacklist");
    }

    public static void setBlacklist(ItemStack filterStack, boolean blacklist) {
        CustomData.update(DataComponents.CUSTOM_DATA, filterStack, tag -> tag.putBoolean("Blacklist", blacklist));
    }

    public static NonNullList<ItemStack> getFilterItems(ItemStack filterStack, HolderLookup.Provider registries) {
        NonNullList<ItemStack> list = NonNullList.withSize(FILTER_SLOTS, ItemStack.EMPTY);
        CustomData data = filterStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        if (tag.contains("Items") && registries != null) {
            ContainerHelper.loadAllItems(tag, list, registries);
        }
        return list;
    }

    public static void saveFilterItems(ItemStack filterStack, NonNullList<ItemStack> list, HolderLookup.Provider registries) {
        CustomData.update(DataComponents.CUSTOM_DATA, filterStack, tag -> ContainerHelper.saveAllItems(tag, list, registries));
    }

    public static boolean matches(ItemStack filterStack, ItemStack candidate, HolderLookup.Provider registries) {
        if (filterStack.isEmpty() || candidate.isEmpty()) return true;
        NonNullList<ItemStack> filters = getFilterItems(filterStack, registries);
        boolean blacklist = isBlacklist(filterStack);

        boolean hasAnyFilter = false;
        boolean matched = false;
        for (ItemStack f : filters) {
            if (!f.isEmpty()) {
                hasAnyFilter = true;
                if (ItemStack.isSameItem(f, candidate)) {
                    matched = true;
                    break;
                }
            }
        }

        if (!hasAnyFilter) {
            return true; // Empty filter allows all items
        }

        return blacklist ? !matched : matched;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider(
                    (syncId, inv, p) -> new HermesFilterMenu(syncId, inv, stack, hand),
                    Component.literal("Hermes Filter Configuration")
            ));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        boolean blacklist = isBlacklist(stack);
        tooltip.add(Component.literal("§6✦ Hermes 9-Slot Intelligent Filter"));
        tooltip.add(Component.literal("§7Mode: " + (blacklist ? "§c[BLACKLIST - Block listed]" : "§a[WHITELIST - Allow listed]")));
        if (context.registries() != null) {
            NonNullList<ItemStack> filters = getFilterItems(stack, context.registries());
            int count = 0;
            StringBuilder sb = new StringBuilder();
            for (ItemStack f : filters) {
                if (!f.isEmpty()) {
                    if (count > 0) sb.append("§7, ");
                    sb.append("§f").append(f.getHoverName().getString());
                    count++;
                }
            }
            if (count > 0) {
                tooltip.add(Component.literal("§eFiltered (" + count + "/9): " + sb));
            } else {
                tooltip.add(Component.literal("§8Filtered (0/9): Empty (Allows all)"));
            }
        }
        tooltip.add(Component.literal("§8• Right-click in hand to configure 9 filter slots"));
        tooltip.add(Component.literal("§8• Works in Hermes Pipes & Volcanite Modular Chest"));
    }
}
