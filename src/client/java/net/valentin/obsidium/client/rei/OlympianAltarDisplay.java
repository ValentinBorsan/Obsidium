package net.valentin.obsidium.client.rei;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.mythology.DivinePact;
import net.valentin.obsidium.mythology.DivinePactHelper;

import java.util.List;

public class OlympianAltarDisplay extends BasicDisplay {
    public static final CategoryIdentifier<OlympianAltarDisplay> CATEGORY =
            CategoryIdentifier.of(ObsidiumMod.MOD_ID, "plugins/olympian_altar");

    private final DivinePact pact;
    private final String weatherCondition;

    public OlympianAltarDisplay(ItemStack targetItem, ItemStack scroll, ItemStack papyrus, ItemStack sacrifice, DivinePact pact, String weatherCondition) {
        super(
                List.of(
                        EntryIngredients.of(targetItem),
                        EntryIngredients.of(scroll),
                        EntryIngredients.of(papyrus),
                        EntryIngredients.of(sacrifice)
                ),
                List.of(
                        EntryIngredients.of(createPactResult(targetItem, pact))
                )
        );
        this.pact = pact;
        this.weatherCondition = weatherCondition;
    }

    private static ItemStack createPactResult(ItemStack base, DivinePact pact) {
        ItemStack result = base.copy();
        DivinePactHelper.setPact(result, pact);
        return result;
    }

    public DivinePact getPact() {
        return pact;
    }

    public String getWeatherCondition() {
        return weatherCondition;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CATEGORY;
    }
}

