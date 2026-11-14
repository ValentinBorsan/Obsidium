package net.valentin.obsidium.client.rei;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.valentin.obsidium.ObsidiumMod;

import java.util.List;

public class PressingDisplay extends BasicDisplay {
    public static final CategoryIdentifier<PressingDisplay> CATEGORY =
            CategoryIdentifier.of(ObsidiumMod.MOD_ID, "plugins/pressing");

    public PressingDisplay(ItemLike fruit, ItemStack mineralOutput, ItemStack pomaceOutput) {
        super(
                List.of(EntryIngredients.of(fruit)),
                List.of(EntryIngredients.of(mineralOutput), EntryIngredients.of(pomaceOutput))
        );
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CATEGORY;
    }
}

