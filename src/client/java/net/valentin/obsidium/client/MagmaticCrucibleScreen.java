package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.MagmaticCrucibleBlockEntity;
import net.valentin.obsidium.block.MagmaticCrucibleMenu;

import java.util.List;

public class MagmaticCrucibleScreen extends AbstractContainerScreen<MagmaticCrucibleMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/geothermal_generator_gui.png");

    public MagmaticCrucibleScreen(MagmaticCrucibleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 73;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, "✦ Magmatic Crucible", this.titleLabelX, this.titleLabelY, 0xFF7A00, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xCECAC5, false);

        int heat = this.menu.getHeatMultiplier();
        guiGraphics.drawString(this.font, "HEAT", 14, 25, 0xAA6633, false);
        if (heat > 0) {
            String heatVal = (heat / 100.0) + "x";
            guiGraphics.drawString(this.font, heatVal, 14, 35, 0xFFFF77, false);
        } else {
            guiGraphics.drawString(this.font, "OFF", 14, 35, 0xFF7777, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        // Tooltip: Thermal Heat Gauge (10, 22) 28x46
        if (mouseX >= i + 10 && mouseX <= i + 38 && mouseY >= j + 22 && mouseY <= j + 68) {
            int heat = this.menu.getHeatMultiplier();
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6🔥 Thermal Heat Source"),
                    Component.literal("§7Status: " + (heat > 0 ? "§aACTIVE (" + (heat / 100.0) + "x speed)" : "§cNO HEAT")),
                    Component.literal("§8Place Lava, Fire, Magma or Campfire beneath crucible")
            ), mouseX, mouseY);
        }

        // Tooltip: Lava Tank (148, 20) 14x48
        if (mouseX >= i + 148 && mouseX <= i + 162 && mouseY >= j + 20 && mouseY <= j + 68) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6🌋 Molten Lava Buffer"),
                    Component.literal("§f" + this.menu.getLavaAmount() + " / " + MagmaticCrucibleBlockEntity.MAX_LAVA + " mB"),
                    Component.literal("§7Production: §e250 mB per rock block"),
                    Component.literal("§7Speed: §a1 Bucket (1,000 mB) per minute"),
                    Component.literal("§8Extract with buckets or connect fluid pipes")
            ), mouseX, mouseY);
        }

        // Tooltip: Heat Source Status Indicator (44, 55) 18x12
        if (mouseX >= i + 44 && mouseX <= i + 62 && mouseY >= j + 55 && mouseY <= j + 67) {
            int heat = this.menu.getHeatMultiplier();
            if (heat > 0) {
                guiGraphics.renderComponentTooltip(this.font, List.of(
                        Component.literal("§6🔥 Thermal Heat Source: §aACTIVE"),
                        Component.literal("§eMultiplier: " + (heat / 100.0) + "x speed"),
                        Component.literal("§7Base rate: 1 bucket every " + (6000 / heat) + " seconds")
                ), mouseX, mouseY);
            } else {
                guiGraphics.renderComponentTooltip(this.font, List.of(
                        Component.literal("§c🔥 Thermal Heat Source: §4OFFLINE"),
                        Component.literal("§7Place crucible directly over a heat source:"),
                        Component.literal("§8• Campfire / Magma Block (§e1.0x§8)"),
                        Component.literal("§8• Soul Campfire (§b1.25x§8)"),
                        Component.literal("§8• Lava (§61.5x§8)")
                ), mouseX, mouseY);
            }
        }

        // Tooltip: Melting Progress Bar (66, 37) 24x10
        if (mouseX >= i + 66 && mouseX <= i + 90 && mouseY >= j + 37 && mouseY <= j + 47) {
            int max = this.menu.getMaxProgress();
            int prog = max > 0 ? (int) (100.0f * this.menu.getProgress() / max) : 0;
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6✦ Magma Liquefaction"),
                    Component.literal("§7Melting rock into molten lava: §e" + prog + "%"),
                    Component.literal("§8Yield: 250 mB Lava + 50% chance of Magma Droplet")
            ), mouseX, mouseY);
        }

        // Tooltip: Rock Input (44, 35)
        if (this.menu.getSlot(0).getItem().isEmpty() && mouseX >= i + 43 && mouseX <= i + 61 && mouseY >= j + 34 && mouseY <= j + 52) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§7Meltable Rock Input"),
                    Component.literal("§8Accepts: Cobblestone, Stone, Basalt, Deepslate, Netherrack")
            ), mouseX, mouseY);
        }

        // Tooltip: Magma Droplets Output (96, 22)
        if (this.menu.getSlot(3).getItem().isEmpty() && mouseX >= i + 95 && mouseX <= i + 113 && mouseY >= j + 21 && mouseY <= j + 39) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6✦ Magma Droplets (Byproduct)"),
                    Component.literal("§7Condenses during melting process"),
                    Component.literal("§8Craft 4x + Bucket ➔ Lava Bucket, or feed directly into Vitrifier (+250 mB)")
            ), mouseX, mouseY);
        }

        // Tooltip: Empty Bucket In (96, 52)
        if (this.menu.getSlot(1).getItem().isEmpty() && mouseX >= i + 95 && mouseX <= i + 113 && mouseY >= j + 51 && mouseY <= j + 69) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§7Empty Bucket Input"),
                    Component.literal("§8Fills bucket when buffer has ≥ 1,000 mB lava")
            ), mouseX, mouseY);
        }

        // Tooltip: Lava Bucket Out (122, 52)
        if (this.menu.getSlot(2).getItem().isEmpty() && mouseX >= i + 121 && mouseX <= i + 139 && mouseY >= j + 51 && mouseY <= j + 69) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6Lava Bucket Output"),
                    Component.literal("§8Filled Lava Bucket")
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

        // Dark industrial background
        guiGraphics.fill(i + 7, j + 18, i + 169, j + 71, 0xFF140E1B);
        border(guiGraphics, i + 7, j + 18, 162, 53, 0xFF2A1C36);

        long time = System.currentTimeMillis();
        int heat = this.menu.getHeatMultiplier();

        // 0. Heat Readout Gauge Panel (i + 10, j + 22, w = 28, h = 46)
        guiGraphics.fill(i + 10, j + 22, i + 38, j + 68, 0xFF1A0A06);
        border(guiGraphics, i + 10, j + 22, 28, 46, heat > 0 ? 0xFFFF7700 : 0xFF553333);
        guiGraphics.fill(i + 12, j + 45, i + 36, j + 46, heat > 0 ? 0xFF882200 : 0xFF331111);

        // Animated Fiery Embers in the Heat Gauge Panel
        if (heat > 0) {
            double f1 = (Math.sin(time / 140.0) + 1.0) / 2.0;
            double f2 = (Math.cos(time / 95.0) + 1.0) / 2.0;
            int col1 = f1 > 0.5 ? 0xFFFFCC00 : 0xFFFF4500;
            int col2 = f2 > 0.5 ? 0xFFFF6600 : 0xFFDD1100;
            guiGraphics.fill(i + 17, j + 50, i + 23, j + 56, col1);
            guiGraphics.fill(i + 24, j + 54, i + 31, j + 61, col2);
            guiGraphics.fill(i + 19, j + 57, i + 27, j + 64, 0xFFFF9900);
            // Magma molten bed
            guiGraphics.fill(i + 13, j + 64, i + 35, j + 67, 0xFFD9381E);
            guiGraphics.fill(i + 13, j + 64, i + 35, j + 65, 0xFFFFCC00);
        } else {
            guiGraphics.fill(i + 13, j + 64, i + 35, j + 67, 0xFF261914);
        }

        // 1. Lava Tank (i + 148, j + 20, width 14, height 48)
        drawTankGauge(guiGraphics, i + 148, j + 20, 14, 48, this.menu.getLavaAmount(), MagmaticCrucibleBlockEntity.MAX_LAVA, 0xFF9E2A2B, 0xCCD9381E, 0xFFFF8C00);

        // 2. Themed Slot Frames
        // Rock In (44, 35)
        drawSlotFrame(guiGraphics, i + 43, j + 34, 0xFF888888);

        // Magma Droplets Out (96, 22)
        drawSlotFrame(guiGraphics, i + 95, j + 21, 0xFFFF7A00);

        // Empty Bucket In (96, 52) & Lava Bucket Out (122, 52)
        drawSlotFrame(guiGraphics, i + 95, j + 51, 0xFF666666);
        drawSlotFrame(guiGraphics, i + 121, j + 51, 0xFFFF4500);

        // 3. Heat source indicator beneath Rock Slot (i + 44, j + 55, w = 16, h = 10)
        if (heat > 0) {
            double pulse = (Math.sin(time / 180.0) + 1.0) / 2.0;
            int flameOrange = 0x33 + (int) (0x55 * pulse);
            guiGraphics.fill(i + 44, j + 55, i + 60, j + 65, 0xFFFF3300);
            guiGraphics.fill(i + 46, j + 57, i + 58, j + 63, 0xFF000000 | (0xFF << 16) | (flameOrange << 8));
        } else {
            guiGraphics.fill(i + 44, j + 55, i + 60, j + 65, 0xFF221111);
            guiGraphics.fill(i + 46, j + 57, i + 58, j + 63, 0xFF442222);
        }
        border(guiGraphics, i + 44, j + 55, 16, 10, heat > 0 ? 0xFFFF7700 : 0xFF553333);

        // 4. Melting Progress Bar with animated molten magma flow
        int maxProg = this.menu.getMaxProgress();
        int progWidth = maxProg > 0 ? (int) (22.0f * this.menu.getProgress() / maxProg) : 0;
        guiGraphics.fill(i + 66, j + 37, i + 90, j + 47, 0xFF0A0610);
        border(guiGraphics, i + 66, j + 37, 24, 10, 0xFF4A1A00);
        if (progWidth > 0) {
            guiGraphics.fill(i + 67, j + 38, i + 67 + progWidth, j + 46, 0xFFD9381E); // Lava red
            guiGraphics.fill(i + 67, j + 38, i + 67 + progWidth, j + 40, 0xFFFFB700); // Amber heat surface
            // Flowing magma spark animation
            int emberPos = (int) ((time / 45) % Math.max(1, progWidth));
            guiGraphics.fill(i + 67 + emberPos, j + 39, Math.min(i + 67 + emberPos + 3, i + 67 + progWidth), j + 44, 0xFFFFF555);
        }
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

                // Bubbling molten lava animation
                long time = System.currentTimeMillis();
                int bubbleX = (int) ((time / 180) % innerW);
                g.fill(x + 1 + bubbleX, topY, Math.min(x + 3 + bubbleX, x + 1 + innerW), Math.min(topY + 1, y + h - 1), 0xFFFFFF66);
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

