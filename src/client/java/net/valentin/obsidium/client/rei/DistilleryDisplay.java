package net.valentin.obsidium.client.rei;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.valentin.obsidium.ObsidiumMod;

import java.util.List;

public class DistilleryDisplay extends BasicDisplay {
    public static final CategoryIdentifier<DistilleryDisplay> CATEGORY =
            CategoryIdentifier.of(ObsidiumMod.MOD_ID, "plugins/distilling");

    public DistilleryDisplay(EntryIngredient fruitInput, EntryIngredient bottleInput, EntryIngredient drinkOutput, EntryIngredient ashOutput) {
        super(List.of(fruitInput, bottleInput), List.of(drinkOutput, ashOutput));
    }

    public DistilleryDisplay(ItemLike fruit, ItemLike bottle, ItemStack drink, ItemStack ash) {
        this(EntryIngredients.of(fruit), EntryIngredients.of(bottle), EntryIngredients.of(drink), EntryIngredients.of(ash));
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CATEGORY;
    }
}

