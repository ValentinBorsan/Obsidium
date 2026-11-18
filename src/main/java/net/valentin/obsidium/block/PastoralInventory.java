package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/** Server-thread inventory helpers; full insertions either fit completely or change nothing. */
final class PastoralInventory {
    private PastoralInventory() {}
    static AABB workArea(BlockPos pos) { return new AABB(pos).inflate(4.0, 1.5, 4.0); }

    static int insert(NonNullList<ItemStack> items, int[] slots, ItemStack input) {
        int remaining = input.getCount();
        for (int pass = 0; pass < 2; pass++) {
            for (int slot : slots) {
                ItemStack current = items.get(slot);
                if (pass == 0 && !current.isEmpty() && ItemStack.isSameItemSameComponents(current, input)) {
                    int moved = Math.min(remaining, Math.max(0, Math.min(64, current.getMaxStackSize()) - current.getCount()));
                    current.grow(moved); remaining -= moved;
                } else if (pass == 1 && current.isEmpty()) {
                    int moved = Math.min(remaining, Math.min(64, input.getMaxStackSize()));
                    items.set(slot, input.copyWithCount(moved)); remaining -= moved;
                }
                if (remaining == 0) return 0;
            }
        }
        return remaining;
    }

    static boolean insertAll(NonNullList<ItemStack> items, int[] slots, ItemStack input) {
        NonNullList<ItemStack> snapshot = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) snapshot.set(i, items.get(i).copy());
        if (insert(snapshot, slots, input) != 0) return false;
        for (int slot : slots) items.set(slot, snapshot.get(slot));
        return true;
    }
}
