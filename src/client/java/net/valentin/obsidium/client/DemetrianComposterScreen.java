package net.valentin.obsidium.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.block.DemetrianComposterMenu;
import net.valentin.obsidium.screen.MagmaForgeManualScreen;

import java.util.ArrayList;
import java.util.List;

public class DemetrianComposterScreen extends AbstractContainerScreen<DemetrianComposterMenu> {

    public DemetrianComposterScreen(DemetrianComposterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 220;
        this.imageHeight = 188;
        this.inventoryLabelX = 30;
        this.inventoryLabelY = 93;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = 7;
    }

    private void drawScaledString(GuiGraphics g, String text, int x, int y, float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0f);
        g.drawString(this.font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private void drawSlotFrame(GuiGraphics g, int sx, int sy, int borderColor) {
        g.fill(sx, sy, sx + 18, sy + 18, 0xFF0D0A14);
        g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF1C1524);
        drawBorder(g, sx, sy, 18, 18, borderColor);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFD166, false);
        g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFCECAC5, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        if (button == 0) {
            // [📖] Manual button in top-right of header bar
            if (mouseX >= i + this.imageWidth - 26 && mouseX <= i + this.imageWidth - 10 && mouseY >= j + 5 && mouseY <= j + 17) {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new MagmaForgeManualScreen(2));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        long now = System.currentTimeMillis();

        // 1. Exterior Obsidian & Demetrian Nature Frame
        VolcaniteGuiTheme.renderExteriorFrame(g, x, y, this.imageWidth, this.imageHeight, true);

        // Main Chassis (220x188)
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF120E1A);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF1C1628);
        drawBorder(g, x, y, imageWidth, imageHeight, 0xFF7BC950);
        drawBorder(g, x + 1, y + 1, imageWidth - 2, imageHeight - 2, 0xFF355E1C);

        // Header Bar
        g.fill(x + 8, y + 4, x + imageWidth - 8, y + 18, 0xFF241C33);
        drawBorder(g, x + 8, y + 4, imageWidth - 16, 14, 0xFF5A8F29);

        // [📖] Manual Button
        boolean hoverBook = mouseX >= x + imageWidth - 26 && mouseX <= x + imageWidth - 10 && mouseY >= y + 5 && mouseY <= y + 17;
        g.fill(x + imageWidth - 26, y + 5, x + imageWidth - 10, y + 17, hoverBook ? 0xFF384A1A : 0xFF181224);
        drawBorder(g, x + imageWidth - 26, y + 5, 16, 12, hoverBook ? 0xFFFFD166 : 0xFF6D9A3B);
        g.drawString(this.font, "📖", x + imageWidth - 21, y + 7, 0xFFFFCC44, false);

        int progress = Math.max(0, Math.min(40, this.menu.getProgress()));
        boolean isWorking = progress > 0;
        boolean isBlocked = this.menu.isOutputBlocked();

        // =========================================================================
        // 2. LEFT PANEL: ORGANIC & ASH INPUTS (x+10..62, y+22..91)
        // =========================================================================
        g.fill(x + 10, y + 22, x + 62, y + 91, 0xFF14101E);
        drawBorder(g, x + 10, y + 22, 52, 69, 0xFF384C24);

        // Slot 0 (Organic Feed: x+38, y+44 -> frame at x+37, y+43)
        drawScaledString(g, "🌿 ORGANIC", x + 14, y + 29, 0.72f, 0xFFA8E063);
        drawSlotFrame(g, x + 37, y + 43, 0xFF62A838);

        // Slot 1 (Volcanic Ash: x+38, y+72 -> frame at x+37, y+71)
        drawScaledString(g, "🌋 ASH", x + 14, y + 63, 0.72f, 0xFFFFB74D);
        drawSlotFrame(g, x + 37, y + 71, 0xFFE07A5F);

        // Connecting feed conduits leading into the crucible
        g.fill(x + 56, y + 51, x + 68, y + 53, 0xFF3A5A20);
        g.fill(x + 56, y + 79, x + 68, y + 81, 0xFF6A3B18);
        if (isWorking) {
            float pulse = (float) ((Math.sin(now / 150.0) + 1.0) * 0.5);
            g.fill(x + 58, y + 51, x + 66, y + 53, pulse > 0.5f ? 0xFF8AE040 : 0xFF4A8025);
            g.fill(x + 58, y + 79, x + 66, y + 81, pulse > 0.5f ? 0xFFFFB030 : 0xFFC05515);
        }

        // =========================================================================
        // 3. CENTER PANEL: ANIMATED BIO-FERMENTATION CRUCIBLE (x+66..150, y+22..91)
        // =========================================================================
        g.fill(x + 66, y + 22, x + 150, y + 91, 0xFF14101E);
        int centerBorderColor = isBlocked ? 0xFFE05050 : (isWorking ? 0xFF6BB535 : 0xFF2F4522);
        drawBorder(g, x + 66, y + 22, 84, 69, centerBorderColor);

        // Crucible Title Bar
        g.fill(x + 68, y + 24, x + 148, y + 33, 0xFF1E2616);
        String statusText;
        int statusColor;
        if (isBlocked) {
            statusText = "⚠ FULL";
            statusColor = (now / 300 % 2 == 0) ? 0xFFFF5555 : 0xFFFFAA00;
        } else if (isWorking) {
            int pct = (progress * 100) / 40;
            statusText = "⟳ " + pct + "%";
            statusColor = 0xFF76FF03;
        } else {
            statusText = "💤 IDLE";
            statusColor = 0xFF888888;
        }
        drawScaledString(g, "FERMENTER", x + 71, y + 26, 0.68f, 0xFFD8EAA0);
        drawScaledString(g, statusText, x + 120, y + 26, 0.68f, statusColor);

        // Crucible Inner Chamber (Vat: x+70..146, y+35..74 -> 76x39)
        g.fill(x + 70, y + 35, x + 146, y + 74, 0xFF0D0B12);
        drawBorder(g, x + 70, y + 35, 76, 39, 0xFF283618);

        // Compost Fill Liquid (scales with progress 0..40)
        int fillMaxH = 35;
        int fillH = isWorking ? (fillMaxH * progress / 40) : (isBlocked ? fillMaxH : 4);
        if (fillH > 0) {
            int fillY = y + 73 - fillH;
            // Rich soil/ash gradient
            g.fillGradient(x + 72, fillY, x + 144, y + 73, 0xFF588135, 0xFF2E1F12);
            // Foam / biological active froth surface
            int foamColor = isWorking ? 0xFF98D84A : 0xFF4A6B25;
            g.fill(x + 72, fillY, x + 144, fillY + 2, foamColor);
        }

        // =========================================================================
        // LIVE COMPOSTING ANIMATION (Rising bio-bubbles, spores, & heat sparks)
        // =========================================================================
        if (isWorking) {
            for (int b = 0; b < 6; b++) {
                // Sine wave cycle for each bubble
                double seed = b * 1.618;
                double cycle = ((now / 800.0) + seed) % 1.0; // 0.0 to 1.0 rising
                int bubbleX = x + 76 + (int) ((Math.sin(cycle * Math.PI * 4 + b) * 0.5 + 0.5) * 60);
                int bubbleY = y + 70 - (int) (cycle * 32);

                int bubbleCol = (b % 2 == 0) ? 0xFFB4F064 : 0xFFFFD166;
                // Draw 2x2 glowing bubble
                g.fill(bubbleX, bubbleY, bubbleX + 2, bubbleY + 2, bubbleCol);
            }
        }

        // Horizontal Progress Bar / Transfer Conduit (x+72..144, y+79..85)
        g.fill(x + 72, y + 79, x + 144, y + 85, 0xFF0A070E);
        drawBorder(g, x + 72, y + 79, 72, 6, 0xFF354E1F);
        int barFill = 70 * progress / 40;
        if (barFill > 0) {
            g.fillGradient(x + 73, y + 80, x + 73 + barFill, y + 84, 0xFF99E23D, 0xFF43761E);
        }

        // Animated Transfer Chevrons into Output (x+147..157)
        int step = (int) ((now / 200) % 3);
        int chCol1 = (step == 0 && isWorking) ? 0xFF76FF03 : 0xFF3D5523;
        int chCol2 = (step == 1 && isWorking) ? 0xFF76FF03 : 0xFF3D5523;
        int chCol3 = (step == 2 && isWorking) ? 0xFF76FF03 : 0xFF3D5523;
        drawScaledString(g, "▶", x + 148, y + 62, 0.75f, chCol1);
        drawScaledString(g, "▶", x + 151, y + 62, 0.75f, chCol2);
        drawScaledString(g, "▶", x + 154, y + 62, 0.75f, chCol3);

        // =========================================================================
        // 4. RIGHT PANEL: FERTILIZER OUTPUT (x+154..210, y+22..91)
        // =========================================================================
        g.fill(x + 154, y + 22, x + 210, y + 91, 0xFF14101E);
        drawBorder(g, x + 154, y + 22, 56, 69, 0xFF4A3A1A);

        drawScaledString(g, "🌾 HARVEST", x + 158, y + 29, 0.72f, 0xFFFFE082);
        drawScaledString(g, "FERTILIZER", x + 158, y + 38, 0.68f, 0xFFC8B070);

        // Slot 2 (Fertilizer Output: x+158, y+58 -> frame at x+157, y+57)
        int outBorder = isWorking ? 0xFF8AE040 : (isBlocked ? 0xFFFF5555 : 0xFFFFD166);
        drawSlotFrame(g, x + 157, y + 57, outBorder);

        drawScaledString(g, "2x Yield", x + 162, y + 79, 0.72f, 0xFFB6D96A);

        // =========================================================================
        // 5. PLAYER INVENTORY & HOTBAR SLOTS
        // =========================================================================
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                drawSlotFrame(g, x + 29 + col * 18, y + 101 + row * 18, 0xFF352B44);
            }
        }
        for (int col = 0; col < 9; ++col) {
            drawSlotFrame(g, x + 29 + col * 18, y + 159, 0xFF4E3D60);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);

        int x = this.leftPos;
        int y = this.topPos;

        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            return;
        }

        // Manual button tooltip
        if (mouseX >= x + imageWidth - 26 && mouseX <= x + imageWidth - 10 && mouseY >= y + 5 && mouseY <= y + 17) {
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6📖 Demetrian Manual"),
                    Component.literal("§7Click to view Composting Recipes & Yields")
            ), mouseX, mouseY);
        }
        // Slot 0 (Organic Feed) Tooltip
        else if (this.menu.getSlot(0).getItem().isEmpty() && mouseX >= x + 37 && mouseX <= x + 55 && mouseY >= y + 43 && mouseY <= y + 61) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal("§a🌱 Organic Feed Slot"));
            tip.add(Component.literal("§7Accepts leaves, saplings, flowers, and metal tree fruits."));
            g.renderComponentTooltip(this.font, tip, mouseX, mouseY);
        }
        // Slot 1 (Ash Input) Tooltip
        else if (this.menu.getSlot(1).getItem().isEmpty() && mouseX >= x + 37 && mouseX <= x + 55 && mouseY >= y + 71 && mouseY <= y + 89) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal("§6🌋 Volcanic Ash Catalyst"));
            tip.add(Component.literal("§7Required as mineral substrate to enrich compost."));
            g.renderComponentTooltip(this.font, tip, mouseX, mouseY);
        }
        // Center Crucible Tooltip
        else if (mouseX >= x + 66 && mouseX <= x + 150 && mouseY >= y + 22 && mouseY <= y + 91) {
            int progress = Math.max(0, Math.min(40, this.menu.getProgress()));
            int pct = (progress * 100) / 40;
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal("§2🌿 Demetrian Fermentation Crucible"));
            tip.add(Component.literal("§7Progress: §a" + pct + "% §8(" + progress + "/40 ticks)"));
            tip.add(Component.literal("§eRecipe: §71 Organic + 1 Volcanic Ash §a➔ 2 Bone Meal"));
            if (this.menu.isOutputBlocked()) {
                tip.add(Component.literal("§c⚠ Output slot full! Clear space to resume."));
            }
            g.renderComponentTooltip(this.font, tip, mouseX, mouseY);
        }
        // Output Slot Tooltip
        else if (this.menu.getSlot(2).getItem().isEmpty() && mouseX >= x + 157 && mouseX <= x + 175 && mouseY >= y + 57 && mouseY <= y + 75) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal("§e🌾 Enriched Bone Meal Output"));
            tip.add(Component.literal("§7Produces double yield (2x Bone Meal per craft)."));
            tip.add(Component.literal("§7Auto-ejects into adjacent Arborists & containers."));
            g.renderComponentTooltip(this.font, tip, mouseX, mouseY);
        }
    }
}
