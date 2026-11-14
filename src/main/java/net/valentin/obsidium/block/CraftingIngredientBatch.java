package net.valentin.obsidium.block;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/** Owns the real ingredients until a craft commits; every unsuccessful batch is refunded. */
public final class CraftingIngredientBatch implements AutoCloseable {
    private final List<ItemStack> input = new ArrayList<>();
    private final Consumer<ItemStack> refund;
    private boolean complete = true;
    private boolean consumed;

    public CraftingIngredientBatch(List<ItemStack> pattern, Function<ItemStack, ItemStack> extract,
                                    Consumer<ItemStack> refund) {
        this.refund = refund;
        try {
            for (ItemStack required : pattern) {
                ItemStack taken = required.isEmpty() ? ItemStack.EMPTY : extract.apply(required.copyWithCount(1));
                input.add(taken);
                if (!required.isEmpty() && (taken.getCount() != 1 || !ItemStack.isSameItemSameComponents(required, taken))) {
                    complete = false;
                    break;
                }
            }
        } catch (RuntimeException | Error failure) {
            close();
            throw failure;
        }
    }

    public boolean isComplete() { return complete; }

    public List<ItemStack> input() { return input.stream().map(ItemStack::copy).toList(); }

    public void consume() {
        if (!complete) throw new IllegalStateException("Incomplete crafting batch");
        consumed = true;
    }

    @Override
    public void close() {
        if (consumed) return;
        consumed = true;
        for (ItemStack stack : input) if (!stack.isEmpty()) refund.accept(stack);
    }
}
