package net.valentin.obsidium.client;

import net.minecraft.client.gui.GuiGraphics;

/** Shared dark obsidian-gold GUI drawing helpers for Obsidium machines. */
final class ObsidiumMachineGui {
    private ObsidiumMachineGui() {}

    static void drawFittedText(GuiGraphics g, net.minecraft.client.gui.Font font, net.minecraft.network.chat.Component text, int x, int y, int maxWidth, int color) {
        float scale = Math.min(1f, (float) maxWidth / Math.max(1, font.width(text)));
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    static void drawFrame(GuiGraphics g, int x, int y, int w, int h, int accentColor) {
        g.fill(x, y, x + w, y + h, 0xFFD49B35);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF09060E);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF140E1D);

        // Header strip
        g.fill(x + 3, y + 3, x + w - 3, y + 19, 0xFF221732);
        g.fill(x + 3, y + 19, x + w - 3, y + 20, accentColor);

        // Corner gold rivets
        rivet(g, x + 3, y + 3);
        rivet(g, x + w - 6, y + 3);
        rivet(g, x + 3, y + h - 6);
        rivet(g, x + w - 6, y + h - 6);
    }

    private static void rivet(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 2, y + 2, 0xFFFFE184);
        g.fill(x + 1, y + 1, x + 2, y + 2, 0xFF8C5E1A);
    }

    static void drawCard(GuiGraphics g, int x, int y, int w, int h, int borderColor) {
        g.fill(x, y, x + w, y + h, borderColor);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF1A1226);
    }

    static void drawSlot(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF4B3759);
        g.fill(x, y + 16, x + 17, y + 17, 0xFF8C5E1A);
        g.fill(x + 16, y, x + 17, y + 17, 0xFF8C5E1A);
        g.fill(x, y, x + 16, y + 16, 0xFF0A0710);
    }

    static void drawEnergyBar(GuiGraphics g, int x, int y, int w, int h, long energy, long maxEnergy, int fillColor) {
        g.fill(x, y, x + w, y + h, 0xFF4B3759);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF08050E);
        if (maxEnergy <= 0 || energy <= 0) return;
        int innerW = w - 2;
        int fill = (int) Math.min(innerW, (energy * innerW) / maxEnergy);
        if (fill > 0) {
            g.fill(x + 1, y + 1, x + 1 + fill, y + h - 1, fillColor);
            g.fill(x + 1, y + 1, x + 1 + fill, y + 2, 0x55FFFFFF);
        }
    }

    static String formatRf(long rf) {
        if (rf >= 1_000_000L) return String.format("%.2fM RF", rf / 1_000_000.0);
        if (rf >= 1_000L) return String.format("%.1fk RF", rf / 1_000.0);
        return rf + " RF";
    }
}
