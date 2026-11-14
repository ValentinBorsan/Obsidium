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

public class OlympianAltarCategory implements DisplayCategory<OlympianAltarDisplay> {
    @Override
    public CategoryIdentifier<? extends OlympianAltarDisplay> getCategoryIdentifier() {
        return OlympianAltarDisplay.CATEGORY;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Olympian Altar Ritual");
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(ObsidiumMod.OLYMPIAN_ALTAR_BLOCK);
    }

    @Override
    public int getDisplayWidth(OlympianAltarDisplay display) {
        return 176;
    }

    @Override
    public int getDisplayHeight() {
        return 96;
    }

    @Override
    public List<Widget> setupDisplay(OlympianAltarDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));

        // Obelisk North (Scroll)
        Point scrollPoint = new Point(bounds.getX() + 40, bounds.getY() + 12);
        // Obelisk South (Papyrus)
        Point papyrusPoint = new Point(bounds.getX() + 40, bounds.getY() + 58);
        // Obelisk West (Offering 1)
        Point westPoint = new Point(bounds.getX() + 17, bounds.getY() + 35);
        // Obelisk East (Offering 2)
        Point eastPoint = new Point(bounds.getX() + 63, bounds.getY() + 35);
        // Center Altar (Base weapon/tool/armor)
        Point centerPoint = new Point(bounds.getX() + 40, bounds.getY() + 35);

        // Arrow & Output
        Point arrowPoint = new Point(bounds.getX() + 92, bounds.getY() + 35);
        Point resultPoint = new Point(bounds.getX() + 128, bounds.getY() + 35);

        // Center item
        widgets.add(Widgets.createSlot(centerPoint)
                .entries(display.getInputEntries().get(0))
                .markInput());

        // North Scroll
        widgets.add(Widgets.createSlot(scrollPoint)
                .entries(display.getInputEntries().get(1))
                .markInput());

        // South Papyrus
        widgets.add(Widgets.createSlot(papyrusPoint)
                .entries(display.getInputEntries().get(2))
                .markInput());

        // West Offering
        widgets.add(Widgets.createSlot(westPoint)
                .entries(display.getInputEntries().get(3))
                .markInput());

        // East Offering
        widgets.add(Widgets.createSlot(eastPoint)
                .entries(display.getInputEntries().get(3))
                .markInput());

        widgets.add(Widgets.createArrow(arrowPoint).animationDurationTicks(130));

        widgets.add(Widgets.createSlot(resultPoint)
                .entries(display.getOutputEntries().get(0))
                .markOutput());

        // Title / Pact badge
        widgets.add(Widgets.createLabel(
                new Point(bounds.getX() + 88, bounds.getY() + 4),
                display.getPact().getFormattedBadge()
        ).centered());

        // Weather / Celestial requirement
        widgets.add(Widgets.createLabel(
                new Point(bounds.getX() + 88, bounds.getY() + 82),
                Component.literal(display.getWeatherCondition())
        ).centered());

        return widgets;
    }
}
