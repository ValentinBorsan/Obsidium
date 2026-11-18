package net.valentin.mythicrails;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

final class RailGui {
    static final int GOLD_BORDER = 0xFFD49B35;
    static final int GOLD_TEXT = 0xFFE9B94B;
    static final int CYAN_ACCENT = 0xFF28BEB9;
    static final int ORANGE_ACCENT = 0xFFFF8822;
    static final int PANEL_BG = 0xFF140E1D;
    static final int CARD_BG = 0xFF1B1328;

    static void frame(GuiGraphics g, int x, int y, int width, int height) {
        frame(g, x, y, width, height, CYAN_ACCENT);
    }

    static void frame(GuiGraphics g, int x, int y, int width, int height, int accentColor) {
        // Outer gold bevel + inner dark rim
        g.fill(x, y, x + width, y + height, GOLD_BORDER);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF0A0612);
        g.fill(x + 2, y + 2, x + width - 2, y + height - 2, PANEL_BG);

        // Header bar
        g.fill(x + 3, y + 3, x + width - 3, y + 19, 0xFF241833);
        g.fill(x + 3, y + 19, x + width - 3, y + 20, accentColor);

        // 4 corner gold rivets
        rivet(g, x + 3, y + 3);
        rivet(g, x + width - 6, y + 3);
        rivet(g, x + 3, y + height - 6);
        rivet(g, x + width - 6, y + height - 6);
    }

    private static void rivet(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 2, y + 2, 0xFFFFE184);
        g.fill(x + 1, y + 1, x + 2, y + 2, 0xFF8C5E1A);
    }

    static void panel(GuiGraphics g, int x, int y, int width, int height, int borderColor, int fillColor) {
        g.fill(x, y, x + width, y + height, borderColor);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, fillColor);
    }

    static void slot(GuiGraphics g, int slotX, int slotY) {
        g.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF4B3759);
        g.fill(slotX, slotY + 16, slotX + 17, slotY + 17, 0xFF7B5C2F);
        g.fill(slotX + 16, slotY, slotX + 17, slotY + 17, 0xFF7B5C2F);
        g.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF0A0710);
    }

    static void badge(GuiGraphics g, Font font, int rightX, int y, String text, int borderColor, int textColor) {
        int bw = font.width(text) + 10;
        int bx = rightX - bw;
        panel(g, bx, y, bw, 12, borderColor, 0xFF100A16);
        g.drawString(font, text, bx + 5, y + 2, textColor, false);
    }

    static final class RailButton extends Button {
        private int accentColor;

        RailButton(int x, int y, int w, int h, Component message, OnPress onPress, int accentColor) {
            super(x, y, w, h, message, onPress, DEFAULT_NARRATION);
            this.accentColor = accentColor;
        }

        void setAccentColor(int accentColor) {
            this.accentColor = accentColor;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int border = !this.active ? 0xFF3A2D4A : (this.isHoveredOrFocused() ? 0xFFFFE184 : this.accentColor);
            int bg = !this.active ? 0xFF120D1A : (this.isHoveredOrFocused() ? 0xFF2B1E3D : 0xFF1C1429);
            panel(g, getX(), getY(), getWidth(), getHeight(), border, bg);
            int textColor = !this.active ? 0xFF6B5F7A : (this.isHoveredOrFocused() ? 0xFFFFFFFF : 0xFFF3E8FF);
            var font = Minecraft.getInstance().font;
            String label = font.substrByWidth(getMessage(), Math.max(8, getWidth() - 6)).getString();
            int tw = font.width(label);
            g.drawString(font, label, getX() + (getWidth() - tw) / 2, getY() + (getHeight() - 8) / 2, textColor, false);
        }
    }
}
