package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.HydrothermalVitrifierBlockEntity;
import net.valentin.obsidium.block.HydrothermalVitrifierMenu;

import java.util.List;

public class HydrothermalVitrifierScreen extends AbstractContainerScreen<HydrothermalVitrifierMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/geothermal_generator_gui.png");

    public HydrothermalVitrifierScreen(HydrothermalVitrifierMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 73;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, "✦ Hydrothermal Vitrifier", this.titleLabelX, this.titleLabelY, 0x00E5FF, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xCECAC5, false);

        int max = this.menu.getMaxProgress();
        int prog = max > 0 ? (int) (100.0f * this.menu.getProgress() / max) : 0;
        if (prog > 0) {
            String progText = prog + "%";
            int tw = this.font.width(progText);
            guiGraphics.drawString(this.font, progText, 86 - tw / 2, 26, 0xCCA0FF, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        // Tooltip: Water Tank (8, 21) 12x48
        if (mouseX >= i + 8 && mouseX <= i + 20 && mouseY >= j + 21 && mouseY <= j + 69) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§b💧 Hydrothermal Water Tank"),
                    Component.literal("§f" + this.menu.getWaterAmount() + " / " + HydrothermalVitrifierBlockEntity.MAX_WATER + " mB"),
                    Component.literal("§7Consumption: §b250 mB §7per obsidian"),
                    Component.literal("§8Insert water bucket in slot or pipe from sides")
            ), mouseX, mouseY);
        }

        // Tooltip: Lava Tank (156, 21) 12x48
        if (mouseX >= i + 156 && mouseX <= i + 168 && mouseY >= j + 21 && mouseY <= j + 69) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6🌋 Magma / Lava Tank"),
                    Component.literal("§f" + this.menu.getLavaAmount() + " / " + HydrothermalVitrifierBlockEntity.MAX_LAVA + " mB"),
                    Component.literal("§7Consumption: §6250 mB §7per obsidian"),
                    Component.literal("§8Insert lava bucket in slot or pipe from sides")
            ), mouseX, mouseY);
        }

        // Tooltip: Progress Arrow (73, 37) 26x10
        if (mouseX >= i + 73 && mouseX <= i + 99 && mouseY >= j + 37 && mouseY <= j + 47) {
            int max = this.menu.getMaxProgress();
            int prog = max > 0 ? (int) (100.0f * this.menu.getProgress() / max) : 0;
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§5✦ Hydrothermal Synthesis"),
                    Component.literal("§7Slow quenching & crystallization: §e" + prog + "%"),
                    Component.literal("§8Recipe: 1 Stone + 1 Ash + 250mB Lava + 250mB Water"),
                    Component.literal("§dOutputs: 1 Obsidian (5% bonus Volcanite/Crying Obsidian)")
            ), mouseX, mouseY);
        }

        // Tooltip: Water Bucket Input (24, 22)
        if (this.menu.getSlot(2).getItem().isEmpty() && mouseX >= i + 23 && mouseX <= i + 41 && mouseY >= j + 21 && mouseY <= j + 39) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§bWater Bucket Input"),
                    Component.literal("§7Accepts Water Buckets to fill internal tank")
            ), mouseX, mouseY);
        }

        // Tooltip: Water Bucket Output (24, 52)
        if (this.menu.getSlot(3).getItem().isEmpty() && mouseX >= i + 23 && mouseX <= i + 41 && mouseY >= j + 51 && mouseY <= j + 69) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§7Empty Bucket Output (Water)"),
                    Component.literal("§8Drains empty buckets automatically")
            ), mouseX, mouseY);
        }

        // Tooltip: Lava Bucket Input (134, 22)
        if (this.menu.getSlot(4).getItem().isEmpty() && mouseX >= i + 133 && mouseX <= i + 151 && mouseY >= j + 21 && mouseY <= j + 39) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6Lava Bucket Input"),
                    Component.literal("§7Accepts Lava Buckets to fill internal tank")
            ), mouseX, mouseY);
        }

        // Tooltip: Lava Bucket Output (134, 52)
        if (this.menu.getSlot(5).getItem().isEmpty() && mouseX >= i + 133 && mouseX <= i + 151 && mouseY >= j + 51 && mouseY <= j + 69) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§7Empty Bucket Output (Lava)"),
                    Component.literal("§8Drains empty buckets automatically")
            ), mouseX, mouseY);
        }

        // Tooltip: Stone Input (52, 22)
        if (this.menu.getSlot(0).getItem().isEmpty() && mouseX >= i + 51 && mouseX <= i + 69 && mouseY >= j + 21 && mouseY <= j + 39) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§7Rock Matrix Input"),
                    Component.literal("§8Accepts: Stone, Smooth Basalt, Cobblestone, Deepslate")
            ), mouseX, mouseY);
        }

        // Tooltip: Ash Input (52, 52)
        if (this.menu.getSlot(1).getItem().isEmpty() && mouseX >= i + 51 && mouseX <= i + 69 && mouseY >= j + 51 && mouseY <= j + 69) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§eVolcanic Mineral Catalyst"),
                    Component.literal("§8Accepts: Volcanic Ash or Ash Briquette")
            ), mouseX, mouseY);
        }

        // Tooltip: Obsidian Output (104, 26)
        if (this.menu.getSlot(6).getItem().isEmpty() && mouseX >= i + 103 && mouseX <= i + 121 && mouseY >= j + 25 && mouseY <= j + 43) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§dObsidian Output"),
                    Component.literal("§8Synthesized Obsidian Block")
            ), mouseX, mouseY);
        }

        // Tooltip: Bonus Output (104, 50)
        if (this.menu.getSlot(7).getItem().isEmpty() && mouseX >= i + 103 && mouseX <= i + 121 && mouseY >= j + 49 && mouseY <= j + 67) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6Rare Bonus Output"),
                    Component.literal("§85% chance of Volcanite or Crying Obsidian")
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

        // Clear machine upper interior with dark vulcan industrial background
        guiGraphics.fill(i + 7, j + 18, i + 169, j + 71, 0xFF140E1B);
        border(guiGraphics, i + 7, j + 18, 162, 53, 0xFF2A1C36);

        // 1. Water Fluid Tank (i + 8, j + 21, width 12, height 48)
        drawTankGauge(guiGraphics, i + 8, j + 21, 12, 48, this.menu.getWaterAmount(), HydrothermalVitrifierBlockEntity.MAX_WATER, 0xFF0077B6, 0xCC00B4D8, 0xFF90E0EF);

        // 2. Lava Fluid Tank (i + 156, j + 21, width 12, height 48)
        drawTankGauge(guiGraphics, i + 156, j + 21, 12, 48, this.menu.getLavaAmount(), HydrothermalVitrifierBlockEntity.MAX_LAVA, 0xFF9E2A2B, 0xCCD9381E, 0xFFFF8C00);

        // 3. Render Themed Slot Frames
        // Water Bucket In & Out
        drawSlotFrame(guiGraphics, i + 23, j + 21, 0xFF00B4D8);
        drawSlotFrame(guiGraphics, i + 23, j + 51, 0xFF0077B6);

        // Stone & Ash Input
        drawSlotFrame(guiGraphics, i + 51, j + 21, 0xFF888888);
        drawSlotFrame(guiGraphics, i + 51, j + 51, 0xFFFF7A00);

        // Lava Bucket In & Out
        drawSlotFrame(guiGraphics, i + 133, j + 21, 0xFFFF4500);
        drawSlotFrame(guiGraphics, i + 133, j + 51, 0xFF9E2A2B);

        // Obsidian Output (primary) & Bonus Output (secondary)
        drawSlotFrame(guiGraphics, i + 103, j + 25, 0xFF9D4EDD);
        drawSlotFrame(guiGraphics, i + 103, j + 49, 0xFFFFD166);

        // 4. Quenching Progress Bar (i + 73, j + 37, width 26, height 10)
        long time = System.currentTimeMillis();
        int maxProg = this.menu.getMaxProgress();
        int progWidth = maxProg > 0 ? (int) (24.0f * this.menu.getProgress() / maxProg) : 0;
        guiGraphics.fill(i + 73, j + 37, i + 99, j + 47, 0xFF0A0610);
        border(guiGraphics, i + 73, j + 37, 26, 10, 0xFF4A255A);
        if (progWidth > 0) {
            guiGraphics.fill(i + 74, j + 38, i + 74 + progWidth, j + 46, 0xFF5A189A); // Obsidian purple fill
            guiGraphics.fill(i + 74, j + 38, i + 74 + progWidth, j + 40, 0xFF9D4EDD); // Violet crystal edge
            // Thermal quenching cyan steam pulse
            int shockPos = (int) ((time / 65) % Math.max(1, progWidth));
            guiGraphics.fill(i + 74 + shockPos, j + 38, Math.min(i + 74 + shockPos + 3, i + 74 + progWidth), j + 46, 0xFF4CC9F0);
        }

        // Dual elemental convergence indicators
        // Cyan cold water stream into synthesis chamber
        int cyanX = (int) ((time / 100) % 6);
        guiGraphics.fill(i + 43 + cyanX, j + 39, i + 45 + cyanX, j + 41, 0xFF00E5FF);
        // Fiery molten lava stream into synthesis chamber
        int lavaX = (int) ((time / 100) % 6);
        guiGraphics.fill(i + 129 - lavaX, j + 39, i + 131 - lavaX, j + 41, 0xFFFF5500);
    }

    private void drawSlotFrame(GuiGraphics g, int x, int y, int borderColor) {
        g.fill(x, y, x + 18, y + 18, 0xFF0B0711);
        border(g, x, y, 18, 18, borderColor);
    }

    private void drawTankGauge(GuiGraphics g, int x, int y, int w, int h, int amount, int max, int borderCol, int fluidCol, int surfaceCol) {
        g.fill(x, y, x + w, y + h, 0xFF08050C);
        border(g, x, y, w, h, borderCol);

        if (max > 0 && amount > 0) {
            int innerH = h - 2;
            int innerW = w - 2;
            int filledH = (int) ((float) innerH * Math.min(amount, max) / max);
            if (filledH > 0) {
                int topY = y + 1 + (innerH - filledH);
                g.fill(x + 1, topY, x + 1 + innerW, y + h - 1, fluidCol);
                g.fill(x + 1, topY, x + 1 + innerW, Math.min(topY + 2, y + h - 1), surfaceCol);

                // Animated fluid surface shimmer
                long time = System.currentTimeMillis();
                int ripple = (int) ((time / 250) % innerW);
                g.fill(x + 1 + ripple, topY, Math.min(x + 3 + ripple, x + 1 + innerW), Math.min(topY + 1, y + h - 1), 0xAAFFFFFF);
            }
        }
    }

    private void border(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
