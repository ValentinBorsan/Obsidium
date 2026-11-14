package net.valentin.obsidium.client.rei;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.valentin.obsidium.ObsidiumMod;

import java.util.List;

public class GrindingDisplay extends BasicDisplay {
    public static final CategoryIdentifier<GrindingDisplay> CATEGORY =
            CategoryIdentifier.of(ObsidiumMod.MOD_ID, "plugins/grinding");

    public GrindingDisplay(EntryIngredient input, EntryIngredient output) {
        super(List.of(input), List.of(output));
    }

    public GrindingDisplay(ItemLike input, ItemStack output) {
        this(EntryIngredients.of(input), EntryIngredients.of(output));
    }

    public GrindingDisplay(ItemStack input, ItemStack output) {
        this(EntryIngredients.of(input), EntryIngredients.of(output));
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CATEGORY;
    }
}

