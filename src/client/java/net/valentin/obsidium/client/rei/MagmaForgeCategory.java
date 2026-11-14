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
import net.valentin.obsidium.block.MagmaForgeRecipe;

import java.util.ArrayList;
import java.util.List;

public class MagmaForgeCategory implements DisplayCategory<MagmaForgeDisplay> {
    @Override
    public CategoryIdentifier<? extends MagmaForgeDisplay> getCategoryIdentifier() {
        return MagmaForgeDisplay.CATEGORY;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Magma Forge Multiblock");
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(ObsidiumMod.MAGMA_FORGE_CORE_BLOCK);
    }

    @Override
    public int getDisplayWidth(MagmaForgeDisplay display) {
        return 170;
    }

    @Override
    public int getDisplayHeight() {
        return 75;
    }

    @Override
    public List<Widget> setupDisplay(MagmaForgeDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));

        MagmaForgeRecipe recipe = display.getRecipe();

        // 3 Input Slots
        for (int i = 0; i < 3; i++) {
            Point p = new Point(bounds.getX() + 15 + (i * 20), bounds.getY() + 20);
            if (i < display.getInputEntries().size()) {
                widgets.add(Widgets.createSlot(p)
                        .entries(display.getInputEntries().get(i))
                        .markInput());
            } else {
                widgets.add(Widgets.createSlot(p));
            }
        }

        // Arrow
        widgets.add(Widgets.createArrow(new Point(bounds.getX() + 80, bounds.getY() + 20))
                .animationDurationTicks(120));

        // Primary Output Slot
        widgets.add(Widgets.createSlot(new Point(bounds.getX() + 115, bounds.getY() + 20))
                .entries(display.getOutputEntries().get(0))
                .markOutput());

        // Bonus / Perfect Strike Output Slot
        if (display.getOutputEntries().size() > 1) {
            widgets.add(Widgets.createSlot(new Point(bounds.getX() + 138, bounds.getY() + 20))
                    .entries(display.getOutputEntries().get(1))
                    .markOutput());
        }

        // Category Banner
        widgets.add(Widgets.createLabel(
                new Point(bounds.getX() + 85, bounds.getY() + 4),
                Component.literal("§6★ " + recipe.getCategory().toUpperCase() + " ★")
        ).centered());

        // Costs (RF and Lava)
        widgets.add(Widgets.createLabel(
                new Point(bounds.getX() + 85, bounds.getY() + 58),
                Component.literal("§e⚡ " + String.format("%,d", recipe.energyCost) + " RF  §c🔥 " + recipe.getLavaCostMb() + " mB Lava")
        ).centered());

        return widgets;
    }
}
