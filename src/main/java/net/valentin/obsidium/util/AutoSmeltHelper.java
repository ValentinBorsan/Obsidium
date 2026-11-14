package net.valentin.obsidium.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;

import java.util.Optional;

public class AutoSmeltHelper {

    public static ItemStack getSmeltedDrop(ServerLevel level, ItemStack input) {
        if (input.isEmpty()) return input;

        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        Optional<RecipeHolder<SmeltingRecipe>> match = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, recipeInput, level);

        if (match.isPresent()) {
            ItemStack result = match.get().value().assemble(recipeInput, level.registryAccess());
            if (!result.isEmpty()) {
                result.setCount(result.getCount() * input.getCount());
                return result;
            }
        }
        return input;
    }
}
