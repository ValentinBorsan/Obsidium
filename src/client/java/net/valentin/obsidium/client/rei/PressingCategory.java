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

public class PressingCategory implements DisplayCategory<PressingDisplay> {
    @Override
    public CategoryIdentifier<? extends PressingDisplay> getCategoryIdentifier() {
        return PressingDisplay.CATEGORY;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Dionysian Press");
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(ObsidiumMod.DIONYSIAN_PRESS_BLOCK);
    }

    @Override
    public int getDisplayWidth(PressingDisplay display) {
        return 150;
    }

    @Override
    public int getDisplayHeight() {
        return 66;
    }

    @Override
    public List<Widget> setupDisplay(PressingDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));

        Point fruitPoint = new Point(bounds.getX() + 20, bounds.getY() + 24);
        Point arrowPoint = new Point(bounds.getX() + 54, bounds.getY() + 24);
        Point mineralPoint = new Point(bounds.getX() + 92, bounds.getY() + 24);
        Point pomacePoint = new Point(bounds.getX() + 116, bounds.getY() + 24);

        widgets.add(Widgets.createSlot(fruitPoint)
                .entries(display.getInputEntries().get(0))
                .markInput());

        widgets.add(Widgets.createArrow(arrowPoint).animationDurationTicks(80));

        widgets.add(Widgets.createSlot(mineralPoint)
                .entries(display.getOutputEntries().get(0))
                .markOutput());

        widgets.add(Widgets.createSlot(pomacePoint)
                .entries(display.getOutputEntries().get(1))
                .markOutput());

        widgets.add(Widgets.createLabel(
                new Point(bounds.getX() + 75, bounds.getY() + 8),
                Component.literal("§6Fruit Pressing • 200 RF/t")
        ).centered());

        return widgets;
    }
}
