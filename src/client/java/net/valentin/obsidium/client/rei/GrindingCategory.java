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

public class GrindingCategory implements DisplayCategory<GrindingDisplay> {
    @Override
    public CategoryIdentifier<? extends GrindingDisplay> getCategoryIdentifier() {
        return GrindingDisplay.CATEGORY;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Volcanite Grinder");
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(ObsidiumMod.VOLCANITE_GRINDER_BLOCK);
    }

    @Override
    public int getDisplayWidth(GrindingDisplay display) {
        return 150;
    }

    @Override
    public int getDisplayHeight() {
        return 66;
    }

    @Override
    public List<Widget> setupDisplay(GrindingDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));

        Point inputPoint = new Point(bounds.getX() + 20, bounds.getY() + 24);
        Point arrowPoint = new Point(bounds.getX() + 56, bounds.getY() + 24);
        Point outputPoint = new Point(bounds.getX() + 104, bounds.getY() + 24);

        widgets.add(Widgets.createSlot(inputPoint)
                .entries(display.getInputEntries().get(0))
                .markInput());

        widgets.add(Widgets.createArrow(arrowPoint).animationDurationTicks(100));

        widgets.add(Widgets.createSlot(outputPoint)
                .entries(display.getOutputEntries().get(0))
                .markOutput());

        widgets.add(Widgets.createLabel(
                new Point(bounds.getX() + 75, bounds.getY() + 8),
                Component.literal("§7⚡ 250 RF/t • 5 sec")
        ).centered());

        return widgets;
    }
}

