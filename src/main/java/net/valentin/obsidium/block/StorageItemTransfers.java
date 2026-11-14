package net.valentin.obsidium.block;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Shared extraction rules for storage inventories that return a single item variant. */
public final class StorageItemTransfers {
    private StorageItemTransfers() {}

    public static ItemStack extract(List<ItemStack> items, int limit, Item item, int amount, boolean simulate) {
        return extract(items, limit, item, amount, simulate, ItemStack.EMPTY);
    }

    public static ItemStack extractMatching(List<ItemStack> items, int limit, ItemStack template, int amount, boolean simulate) {
        if (template.isEmpty()) return ItemStack.EMPTY;
        return extract(items, limit, template.getItem(), amount, simulate, template);
    }

    private static ItemStack extract(List<ItemStack> items, int limit, Item item, int amount,
                                     boolean simulate, ItemStack template) {
        if (item == null || amount <= 0) return ItemStack.EMPTY;
        int needed = amount;
        ItemStack result = ItemStack.EMPTY;
        for (int i = 0; i < Math.min(limit, items.size()); i++) {
            ItemStack current = items.get(i);
            if (current.isEmpty() || !current.is(item)
                    || (!template.isEmpty() && !ItemStack.isSameItemSameComponents(template, current))
                    || (!result.isEmpty() && !ItemStack.isSameItemSameComponents(result, current))) continue;
            int taken = Math.min(needed, current.getCount());
            if (result.isEmpty()) result = current.copyWithCount(taken);
            else result.grow(taken);
            if (!simulate) {
                current.shrink(taken);
                if (current.isEmpty()) items.set(i, ItemStack.EMPTY);
            }
            needed -= taken;
            if (needed == 0) break;
        }
        return result;
    }
}
