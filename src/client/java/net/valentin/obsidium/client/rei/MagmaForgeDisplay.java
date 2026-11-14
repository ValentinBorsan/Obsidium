package net.valentin.obsidium.client.rei;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.MagmaForgeRecipe;

import java.util.ArrayList;
import java.util.List;

public class MagmaForgeDisplay extends BasicDisplay {
    public static final CategoryIdentifier<MagmaForgeDisplay> CATEGORY =
            CategoryIdentifier.of(ObsidiumMod.MOD_ID, "plugins/magma_forge");

    private final MagmaForgeRecipe recipe;

    public MagmaForgeDisplay(MagmaForgeRecipe recipe) {
        super(convertInputs(recipe), convertOutputs(recipe));
        this.recipe = recipe;
    }

    private static List<EntryIngredient> convertInputs(MagmaForgeRecipe recipe) {
        List<EntryIngredient> list = new ArrayList<>();
        for (ItemStack in : recipe.getIngredients()) {
            list.add(EntryIngredients.of(in));
        }
        return list;
    }

    private static List<EntryIngredient> convertOutputs(MagmaForgeRecipe recipe) {
        List<EntryIngredient> list = new ArrayList<>();
        list.add(EntryIngredients.of(recipe.getResult()));
        if (!recipe.getBonusResult().isEmpty()) {
            list.add(EntryIngredients.of(recipe.getBonusResult()));
        }
        return list;
    }

    public MagmaForgeRecipe getRecipe() {
        return recipe;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CATEGORY;
    }
}
