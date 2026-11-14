package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.DionysianPressMenu;

import java.util.List;

public class DionysianPressScreen extends AbstractContainerScreen<DionysianPressMenu> {

    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/geothermal_generator_gui.png");

    public DionysianPressScreen(DionysianPressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 74;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, "🍇 Dionysian Press", this.titleLabelX, this.titleLabelY, 0xFFA500, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xCECAC5, false);

        int prog = this.menu.getProgressScaled(100);
        if (prog > 0) {
            guiGraphics.drawString(this.font, prog + "%", 78, 52, 0xFFE082, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        // Tooltip: Energy Gauge (14, 18, 14x54)
        if (mouseX >= i + 14 && mouseX <= i + 28 && mouseY >= j + 18 && mouseY <= j + 72) {
            long energy = this.menu.getEnergy();
            long max = this.menu.getMaxEnergy();
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§c⚡ Dionysian Energy Storage"),
                    Component.literal("§f" + String.format("%,d", energy) + " / " + String.format("%,d", max) + " RF"),
                    Component.literal("§7Usage: §e200 RF/t §7while pressing"),
                    Component.literal("§8Powered via energy conduits or internal storage")
            ), mouseX, mouseY);
        }

        // Tooltip: Fruit Slot (49, 23, 18x18)
        if (this.menu.getSlot(0).getItem().isEmpty() && mouseX >= i + 49 && mouseX <= i + 67 && mouseY >= j + 23 && mouseY <= j + 41) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§a🍏 Fruit Input"),
                    Component.literal("§7Accepts mythical ore fruits or vanilla fruits"),
                    Component.literal("§8Extracts 5–8× Nuggets + Organic Pomace")
            ), mouseX, mouseY);
        }

        // Tooltip: Fuel Slot (49, 53, 18x18)
        if (this.menu.getSlot(3).getItem().isEmpty() && mouseX >= i + 49 && mouseX <= i + 67 && mouseY >= j + 53 && mouseY <= j + 71) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6🔥 Thermal Fuel (Optional)"),
                    Component.literal("§7Coal, Charcoal, Ash, Wood, Pomace"),
                    Component.literal("§8Used automatically when RF power is missing")
            ), mouseX, mouseY);
        }

        // Tooltip: Pressing Chamber & Progress (76, 22, 28x26)
        if (mouseX >= i + 74 && mouseX <= i + 104 && mouseY >= j + 20 && mouseY <= j + 48) {
            int prog = this.menu.getProgressScaled(100);
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6🍇 Hydraulic Press Mechanism"),
                    Component.literal("§7Crush progress: §e" + prog + "%"),
                    Component.literal("§8Cold-presses the fruit core for pure mineral nuggets")
            ), mouseX, mouseY);
        }

        // Tooltip: Mineral Output (111, 23, 18x18)
        if (this.menu.getSlot(1).getItem().isEmpty() && mouseX >= i + 111 && mouseX <= i + 129 && mouseY >= j + 23 && mouseY <= j + 41) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§e✨ Mineral Output (Nuggets)"),
                    Component.literal("§7Recovers 1-3 pure metallic nuggets from fruit sap")
            ), mouseX, mouseY);
        }

        // Tooltip: Pomace Output (135, 23, 18x18)
        if (this.menu.getSlot(2).getItem().isEmpty() && mouseX >= i + 135 && mouseX <= i + 153 && mouseY >= j + 23 && mouseY <= j + 41) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§2🌿 Pomace Output"),
                    Component.literal("§7Concentrated organic byproduct"),
                    Component.literal("§8High-yield fertilizer and 1,600-tick furnace fuel")
            ), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        VolcaniteGuiTheme.renderExteriorFrame(guiGraphics, i, j, this.imageWidth, this.imageHeight, false);
        guiGraphics.blit(GUI_TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        // Dionysian Wine/Bronze Chamber Background
        guiGraphics.fill(i + 7, j + 16, i + 169, j + 73, 0xFF1A0A14);
        border(guiGraphics, i + 7, j + 16, 162, 57, 0xFF4A1028);

        long time = System.currentTimeMillis();

        // 1. Energy Storage Bar (i + 14, j + 18, w = 14, h = 52)
        guiGraphics.fill(i + 14, j + 18, i + 28, j + 70, 0xFF0A0307);
        border(guiGraphics, i + 14, j + 18, 14, 52, 0xFF6B1A3A);
        int energyH = this.menu.getEnergyScaled(50);
        if (energyH > 0) {
            int topY = j + 19 + (50 - energyH);
            guiGraphics.fill(i + 15, topY, i + 27, j + 69, 0xFFC02040); // Wine-red RF fill
            guiGraphics.fill(i + 15, topY, i + 27, Math.min(topY + 2, j + 69), 0xFFFF7090);
        }

        // 2. Themed Slot Borders
        // Fruit input (50, 24)
        drawSlotFrame(guiGraphics, i + 49, j + 23, 0xFF2E8B57); // Fruit green
        // Fuel input (50, 54)
        drawSlotFrame(guiGraphics, i + 49, j + 53, 0xFFD2691E); // Warm bronze/orange
        // Nuggets output (112, 24)
        drawSlotFrame(guiGraphics, i + 111, j + 23, 0xFFFFD700); // Gold
        // Pomace output (136, 24)
        drawSlotFrame(guiGraphics, i + 135, j + 23, 0xFF556B2F); // Olive pomace green

        // 3. Flame indicator near fuel slot
        if (this.menu.getBurnTime() > 0) {
            int flameH = this.menu.getBurnTimeScaled(12);
            guiGraphics.fill(i + 69, j + 56 + (12 - flameH), i + 75, j + 68, 0xFFFF6600);
            guiGraphics.fill(i + 70, j + 58 + (12 - flameH), i + 74, j + 68, 0xFFFFFF00);
        } else {
            guiGraphics.fill(i + 69, j + 66, i + 75, j + 68, 0xFF331111);
        }

        // 4. Animated Hydraulic Press Mechanism (x = 74, y = 20, w = 30, h = 26)
        guiGraphics.fill(i + 74, j + 20, i + 104, j + 46, 0xFF0D050B);
        border(guiGraphics, i + 74, j + 20, 30, 26, 0xFF5A1E35);

        // Press Chamber Base Plate
        guiGraphics.fill(i + 76, j + 42, i + 102, j + 44, 0xFF704050);

        // Piston Head moving down with progress
        int maxTravel = 14;
        int travel = this.menu.getProgressScaled(maxTravel);
        int pistonY = j + 22 + travel;

        // Piston shaft
        guiGraphics.fill(i + 87, j + 21, i + 91, pistonY, 0xFF8A7080);
        // Heavy Bronze Pressing Weight
        guiGraphics.fill(i + 77, pistonY, i + 101, pistonY + 5, 0xFFB87333); // Bronze
        guiGraphics.fill(i + 78, pistonY + 1, i + 100, pistonY + 2, 0xFFFFC080); // Highlight
        guiGraphics.fill(i + 77, pistonY + 4, i + 101, pistonY + 5, 0xFF6E4020); // Shadow

        // Squishing juice / spark effect when actively pressing
        if (travel > 4) {
            int splashColor = ((time / 100) % 2 == 0) ? 0xFFFFD700 : 0xFF8B0000;
            guiGraphics.fill(i + 78, j + 41, i + 82, j + 43, splashColor);
            guiGraphics.fill(i + 96, j + 41, i + 100, j + 43, splashColor);
        }

        // Connecting arrow between chamber and output
        guiGraphics.fill(i + 105, j + 31, i + 110, j + 33, 0xFFB87333);
        guiGraphics.fill(i + 108, j + 29, i + 110, j + 35, 0xFFB87333);
    }

    private void drawSlotFrame(GuiGraphics g, int x, int y, int borderColor) {
        g.fill(x, y, x + 18, y + 18, 0xFF0E0610);
        border(g, x, y, 18, 18, borderColor);
    }

    private void border(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }
}

