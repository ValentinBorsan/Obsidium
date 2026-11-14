package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.block.ApolloSolarGeneratorMenu;

import java.util.List;

public class ApolloSolarGeneratorScreen extends AbstractContainerScreen<ApolloSolarGeneratorMenu> {

    public ApolloSolarGeneratorScreen(ApolloSolarGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 1000; // Custom drawn
        this.inventoryLabelX = 1000;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Base window shadow and deep titanium charcoal casing
        guiGraphics.fill(x - 1, y - 1, x + this.imageWidth + 1, y + this.imageHeight + 1, 0xFF080A0E);
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF14131A);

        // Metallic perimeter bezel
        guiGraphics.fill(x, y, x + this.imageWidth, y + 1, 0xFF352A40);
        guiGraphics.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF1A1522);
        guiGraphics.fill(x, y, x + 1, y + this.imageHeight, 0xFF352A40);
        guiGraphics.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, 0xFF1A1522);

        // Apollo Golden Sun corner pins (2x2)
        int pinColor = 0xFFF59E0B;
        guiGraphics.fill(x + 1, y + 1, x + 3, y + 3, pinColor);
        guiGraphics.fill(x + this.imageWidth - 3, y + 1, x + this.imageWidth - 1, y + 3, pinColor);
        guiGraphics.fill(x + 1, y + this.imageHeight - 3, x + 3, y + this.imageHeight - 1, pinColor);
        guiGraphics.fill(x + this.imageWidth - 3, y + this.imageHeight - 3, x + this.imageWidth - 1, y + this.imageHeight - 1, pinColor);

        // Top Header Bar
        guiGraphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + 16, 0xFF1E1828);
        guiGraphics.fill(x + 1, y + 16, x + this.imageWidth - 1, y + 17, 0xFF2F243E);

        // Solar Collector Panel section
        guiGraphics.fill(x + 8, y + 20, x + 168, y + 74, 0xFF191624);
        guiGraphics.fill(x + 8, y + 20, x + 168, y + 21, 0xFF2C223C);

        // Slot 0 (Input): x + 56, y + 35
        drawSlotFrame(guiGraphics, x + 56, y + 35, 0xFF3B2F50, 0xFF0D0B12);

        // Slot 1 (Output): x + 116, y + 35 with Golden Sun ring
        drawSlotFrame(guiGraphics, x + 116, y + 35, 0xFFF59E0B, 0xFF17130E);

        // Animated Solar Ray Progress Arrow (between 76 and 108)
        int prog = (this.menu.getSmeltDuration() > 0)
                ? (this.menu.getSmeltProgress() * 24 / this.menu.getSmeltDuration())
                : 0;
        int arrX = x + 82;
        int arrY = y + 40;
        guiGraphics.fill(arrX, arrY, arrX + 24, arrY + 6, 0xFF2A1F38);
        if (prog > 0) {
            guiGraphics.fill(arrX, arrY, arrX + prog, arrY + 6, 0xFFF59E0B);
            guiGraphics.fill(arrX, arrY + 1, arrX + prog, arrY + 4, 0xFFFDE68A);
        }
        guiGraphics.renderOutline(arrX, arrY, 24, 6, 0xFFB45309);

        // Sun Irradiance Gauge Badge (x + 16, y + 32, w = 28, h = 24)
        int irrad = this.menu.getSunIrradiance();
        guiGraphics.fill(x + 16, y + 32, x + 44, y + 56, 0xFF120E1C);
        guiGraphics.renderOutline(x + 16, y + 32, 28, 24, irrad > 0 ? 0xFFF59E0B : 0xFF475569);
        guiGraphics.drawString(this.font, irrad > 0 ? "§6☀" : "§8○", x + 25, y + 36, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, irrad + "%", x + 19, y + 46, irrad > 0 ? 0xFFFDE68A : 0xFF64748B, false);

        // Energy Indicator Gauge (x + 148, y + 25, w = 14, h = 44)
        int energy = this.menu.getEnergy();
        int maxEnergy = this.menu.getMaxEnergy();
        int barH = (maxEnergy > 0) ? (int) (42.0 * Math.min(1.0, (double) energy / maxEnergy)) : 0;
        guiGraphics.fill(x + 148, y + 25, x + 162, y + 69, 0xFF0D1217);
        guiGraphics.fill(x + 149, y + 68 - barH, x + 161, y + 68, (energy > 0) ? 0xFFF59E0B : 0xFFEF4444);
        guiGraphics.renderOutline(x + 148, y + 25, 14, 44, 0xFF334155);

        // Player Inventory Section (3 rows x 9 columns at y = 84)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int sx = x + 8 + col * 18;
                int sy = y + 84 + row * 18;
                drawSlotFrame(guiGraphics, sx, sy, 0xFF262132, 0xFF0C0A10);
            }
        }

        // Player Hotbar (9 slots at y = 142)
        for (int col = 0; col < 9; ++col) {
            int sx = x + 8 + col * 18;
            int sy = y + 142;
            drawSlotFrame(guiGraphics, sx, sy, 0xFF262132, 0xFF0C0A10);
        }
    }

    private static void drawSlotFrame(GuiGraphics g, int sx, int sy, int borderColor, int innerColor) {
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, borderColor);
        g.fill(sx, sy, sx + 16, sy + 16, innerColor);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Clean Header Title
        guiGraphics.drawString(this.font, Component.literal("§6☀ §fApollo Helio-Concentrator"), 8, 5, 0xFFFFFF, false);

        // Dynamic generation status
        int rate = this.menu.getGenerationRate();
        String rateText = (rate > 0) ? String.format("§a+%,d RF/t", rate) : "§7Idle";
        guiGraphics.drawString(this.font, rateText, 120, 5, 0xFFFFFF, false);

        // Player Inventory Title (y = 74)
        guiGraphics.drawString(this.font, Component.literal("§8INVENTORY"), 8, 74, 0x64748B, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Energy Gauge Tooltip (x + 148..162, y + 25..69)
        if (mouseX >= x + 148 && mouseX <= x + 162 && mouseY >= y + 25 && mouseY <= y + 69) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6⚡ Apollo Solar Battery"),
                    Component.literal(String.format("§7Stored Energy: §e%,d / %,d RF", this.menu.getEnergy(), this.menu.getMaxEnergy())),
                    Component.literal(this.menu.getEnergy() > 0 ? "§a● Grid Status: ONLINE" : "§c○ Grid Status: DEPLETED")
            ), mouseX, mouseY);
        }
        // Sun Irradiance Tooltip (x + 16..44, y + 32..56)
        else if (mouseX >= x + 16 && mouseX <= x + 44 && mouseY >= y + 32 && mouseY <= y + 56) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6☀ Solar Irradiance"),
                    Component.literal("§7Direct Sunlight: §e" + this.menu.getSunIrradiance() + "%"),
                    Component.literal("§7Output Generation: §a+" + this.menu.getGenerationRate() + " RF/t"),
                    Component.literal("§8Must face open sky during day. Generates extra in hot biomes!")
            ), mouseX, mouseY);
        }
        // Smelt Arrow Tooltip
        else if (mouseX >= x + 82 && mouseX <= x + 106 && mouseY >= y + 38 && mouseY <= y + 48) {
            int pct = (this.menu.getSmeltDuration() > 0)
                    ? (this.menu.getSmeltProgress() * 100 / this.menu.getSmeltDuration())
                    : 0;
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6Solar Smelting Ray: §e" + pct + "%"),
                    Component.literal("§7Concentrates solar beams to smelt ores"),
                    Component.literal("§7and metals without consuming any fuel!")
            ), mouseX, mouseY);
        }
    }
}

