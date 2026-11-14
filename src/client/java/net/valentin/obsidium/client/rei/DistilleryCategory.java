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

public class DistilleryCategory implements DisplayCategory<DistilleryDisplay> {
    @Override
    public CategoryIdentifier<? extends DistilleryDisplay> getCategoryIdentifier() {
        return DistilleryDisplay.CATEGORY;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Dionysian Distillery");
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(ObsidiumMod.DIONYSIAN_DISTILLERY_BLOCK);
    }

    @Override
    public int getDisplayWidth(DistilleryDisplay display) {
        return 150;
    }

    @Override
    public int getDisplayHeight() {
        return 72;
    }

    @Override
    public List<Widget> setupDisplay(DistilleryDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));

        Point fruitPoint = new Point(bounds.getX() + 20, bounds.getY() + 16);
        Point bottlePoint = new Point(bounds.getX() + 20, bounds.getY() + 40);
        Point arrowPoint = new Point(bounds.getX() + 56, bounds.getY() + 28);
        Point drinkPoint = new Point(bounds.getX() + 104, bounds.getY() + 16);
        Point ashPoint = new Point(bounds.getX() + 104, bounds.getY() + 40);

        widgets.add(Widgets.createSlot(fruitPoint)
                .entries(display.getInputEntries().get(0))
                .markInput());

        widgets.add(Widgets.createSlot(bottlePoint)
                .entries(display.getInputEntries().get(1))
                .markInput());

        widgets.add(Widgets.createArrow(arrowPoint).animationDurationTicks(120));

        widgets.add(Widgets.createSlot(drinkPoint)
                .entries(display.getOutputEntries().get(0))
                .markOutput());

        widgets.add(Widgets.createSlot(ashPoint)
                .entries(display.getOutputEntries().get(1))
                .markOutput());

        widgets.add(Widgets.createLabel(
                new Point(bounds.getX() + 75, bounds.getY() + 6),
                Component.literal("§6Fermentation & Distillation")
        ).centered());

        return widgets;
    }
}

