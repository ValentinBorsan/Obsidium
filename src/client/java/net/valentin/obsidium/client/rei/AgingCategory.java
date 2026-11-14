package net.valentin.obsidium.client.rei;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.valentin.obsidium.ObsidiumMod;

import java.util.ArrayList;
import java.util.List;

public class AgingCategory implements DisplayCategory<AgingDisplay> {
    @Override
    public CategoryIdentifier<? extends AgingDisplay> getCategoryIdentifier() {
        return AgingDisplay.CATEGORY;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Dionysian Aging Barrel");
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(ObsidiumMod.DIONYSIAN_AGING_BARREL_BLOCK);
    }

    @Override
    public int getDisplayWidth(AgingDisplay display) {
        return 150;
    }

    @Override
    public int getDisplayHeight() {
        return 66;
    }

    @Override
    public List<Widget> setupDisplay(AgingDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));

        Point inputPoint = new Point(bounds.getX() + 25, bounds.getY() + 24);
        Point arrowPoint = new Point(bounds.getX() + 60, bounds.getY() + 24);
        Point outputPoint = new Point(bounds.getX() + 105, bounds.getY() + 24);

        widgets.add(Widgets.createSlot(inputPoint)
                .entries(display.getInputEntries().get(0))
                .markInput());

        widgets.add(Widgets.createArrow(arrowPoint).animationDurationTicks(140));

        widgets.add(Widgets.createSlot(outputPoint)
                .entries(display.getOutputEntries().get(0))
                .markOutput());

        widgets.add(Widgets.createLabel(
                new Point(bounds.getX() + 75, bounds.getY() + 8),
                Component.literal("§dBarrel Aging • 2 min")
        ).centered());

        return widgets;
    }
}
