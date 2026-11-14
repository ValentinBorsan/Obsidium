package net.valentin.obsidium.block;

import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Reserves an entire transfer round before modifying any ingredient slot. */
public final class RecipeGridTransfer {
    private RecipeGridTransfer() {}

    public interface Network {
        ItemStack extract(Item item, ItemStack existing);
        ItemStack insert(ItemStack stack);
    }

    private record Reservation(int gridSlot, int sourceSlot, ItemStack stack) {}

    public static int transfer(Container grid, Container source, List<List<Item>> candidates,
                               int maxRounds, Network network, Consumer<ItemStack> fallback) {
        for (int i = 0; i < 9; i++) {
            ItemStack current = grid.getItem(i);
            if (!current.isEmpty() && !choices(candidates, i).contains(current.getItem())) {
                ItemStack returned = current.copy();
                grid.setItem(i, ItemStack.EMPTY);
                refund(returned, network, fallback);
            }
        }
        if (candidates.stream().allMatch(List::isEmpty)) return 0;

        int rounds = 0;
        for (; rounds < Math.min(64, maxRounds); rounds++) {
            List<Reservation> reserved = new ArrayList<>();
            List<ItemStack> previousGrid = new ArrayList<>();
            for (int i = 0; i < 9; i++) previousGrid.add(grid.getItem(i).copy());
            boolean complete = false;
            boolean applying = false;
            try {
                boolean possible = true;
                for (int i = 0; i < 9; i++) {
                    List<Item> choices = choices(candidates, i);
                    if (choices.isEmpty()) continue;
                    ItemStack current = grid.getItem(i);
                    if (!current.isEmpty() && current.getCount() >= Math.min(current.getMaxStackSize(), grid.getMaxStackSize())) {
                        possible = false;
                        break;
                    }
                    ItemStack taken = ItemStack.EMPTY;
                    int sourceSlot = -1;
                    for (Item item : choices) {
                        if (!current.isEmpty() && !current.is(item)) continue;
                        for (int slot = 0; slot < source.getContainerSize(); slot++) {
                            ItemStack available = source.getItem(slot);
                            if (available.is(item) && (current.isEmpty() || ItemStack.isSameItemSameComponents(current, available))) {
                                taken = source.removeItem(slot, 1);
                                sourceSlot = slot;
                                break;
                            }
                        }
                        if (!taken.isEmpty()) break;
                    }
                    if (taken.isEmpty() && network != null) {
                        for (Item item : choices) {
                            if (!current.isEmpty() && !current.is(item)) continue;
                            taken = network.extract(item, current);
                            if (!taken.isEmpty()) break;
                        }
                    }
                    if (!taken.isEmpty()) reserved.add(new Reservation(i, sourceSlot, taken));
                    if (taken.getCount() != 1 || !choices.contains(taken.getItem())
                            || (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, taken))) {
                        possible = false;
                        break;
                    }
                }
                if (!possible) break;
                applying = true;
                for (Reservation reservation : reserved) {
                    ItemStack current = grid.getItem(reservation.gridSlot());
                    grid.setItem(reservation.gridSlot(), current.isEmpty() ? reservation.stack()
                            : current.copyWithCount(current.getCount() + reservation.stack().getCount()));
                }
                grid.setChanged();
                complete = true;
            } finally {
                if (!complete) {
                    if (applying) {
                        for (int i = 0; i < 9; i++) grid.setItem(i, previousGrid.get(i));
                    }
                    for (Reservation reservation : reserved) {
                        ItemStack remaining = reservation.stack();
                        if (reservation.sourceSlot() >= 0) {
                            remaining = restoreSourceSlot(source, reservation.sourceSlot(), remaining);
                        }
                        refund(remaining, network, fallback);
                    }
                }
            }
        }
        return rounds;
    }

    private static List<Item> choices(List<List<Item>> candidates, int index) {
        return index < candidates.size() ? candidates.get(index) : List.of();
    }

    private static ItemStack restoreSourceSlot(Container source, int slot, ItemStack stack) {
        ItemStack current = source.getItem(slot);
        if (!source.canPlaceItem(slot, stack)
                || (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, stack))) return stack;
        int limit = Math.min(source.getMaxStackSize(), stack.getMaxStackSize());
        int moved = Math.min(stack.getCount(), Math.max(0, limit - current.getCount()));
        if (moved > 0) {
            source.setItem(slot, stack.copyWithCount(current.getCount() + moved));
            stack = stack.copyWithCount(stack.getCount() - moved);
        }
        return stack;
    }

    private static void refund(ItemStack stack, Network network, Consumer<ItemStack> fallback) {
        if (stack.isEmpty()) return;
        ItemStack remaining = network == null ? stack : network.insert(stack);
        if (!remaining.isEmpty()) fallback.accept(remaining);
    }
}
