package net.valentin.obsidium.client.rei;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.ObsidiumMod;

import java.util.List;

public class AgingDisplay extends BasicDisplay {
    public static final CategoryIdentifier<AgingDisplay> CATEGORY =
            CategoryIdentifier.of(ObsidiumMod.MOD_ID, "plugins/aging");

    public AgingDisplay(ItemStack input, ItemStack agedOutput) {
        super(
                List.of(EntryIngredients.of(input)),
                List.of(EntryIngredients.of(agedOutput))
        );
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CATEGORY;
    }
}
