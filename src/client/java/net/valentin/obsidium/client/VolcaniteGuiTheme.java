package net.valentin.obsidium.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Shared Obsidian + Tech + Vulcan exterior frame renderer for all Obsidium machine screens.
 * Renders 3D obsidian armor plating, corner magma brackets, side thermal heat-sink fins,
 * and animated lava/RF conduit pulses around the exterior perimeter of machine GUIs.
 */
public final class VolcaniteGuiTheme {
    private VolcaniteGuiTheme() {}

    // Obsidian Armor Palette
    private static final int OBS_VOID      = 0xFF07050C;
    private static final int OBS_DARK      = 0xFF120C1C;
    private static final int OBS_MID       = 0xFF1E152E;
    private static final int OBS_HIGHLIGHT = 0xFF34254E;

    // Volcanic & Tech Accents
    private static final int MAGMA_DARK    = 0xFF7A1808;
    private static final int MAGMA_ORANGE  = 0xFFFF6600;
    private static final int MAGMA_AMBER   = 0xFFFFB700;
    private static final int MAGMA_HOT     = 0xFFFFEA79;

    private static final int GOLD_DARK     = 0xFF8C5E10;
    private static final int GOLD_MID      = 0xFFDCA220;
    private static final int GOLD_BRIGHT   = 0xFFFFDA55;

    private static final int CYAN_MID      = 0xFF00B8E6;
    private static final int CYAN_HOT      = 0xFF73F2FF;

    public static void renderExteriorFrame(GuiGraphics g, int x, int y, int w, int h, boolean advanced) {
        long now = System.currentTimeMillis();
        float pulse = (float) ((Math.sin(now / 320.0) + 1.0) * 0.5); // 0.0 .. 1.0

        int mainTrim = advanced ? GOLD_MID : MAGMA_ORANGE;
        int brightTrim = advanced ? GOLD_BRIGHT : MAGMA_AMBER;
        int darkTrim = advanced ? GOLD_DARK : MAGMA_DARK;
        int techNode = advanced ? CYAN_HOT : MAGMA_HOT;
        int techDim  = advanced ? CYAN_MID : MAGMA_ORANGE;

        // 1. Outer 4px Obsidian Armor Casing Halo around the entire window
        // Top & Bottom exterior armor strips
        g.fill(x - 3, y - 4, x + w + 3, y, OBS_VOID);
        g.fill(x - 2, y - 3, x + w + 2, y, OBS_DARK);
        g.fill(x - 2, y - 2, x + w + 2, y - 1, OBS_HIGHLIGHT);

        g.fill(x - 3, y + h, x + w + 3, y + h + 4, OBS_VOID);
        g.fill(x - 2, y + h, x + w + 2, y + h + 3, OBS_DARK);
        g.fill(x - 2, y + h + 1, x + w + 2, y + h + 2, OBS_MID);

        // Left & Right exterior armor strips
        g.fill(x - 4, y - 3, x, y + h + 3, OBS_VOID);
        g.fill(x - 3, y - 2, x, y + h + 2, OBS_DARK);
        g.fill(x - 2, y - 2, x - 1, y + h + 2, OBS_HIGHLIGHT);

        g.fill(x + w, y - 3, x + w + 4, y + h + 3, OBS_VOID);
        g.fill(x + w, y - 2, x + w + 3, y + h + 2, OBS_DARK);
        g.fill(x + w + 1, y - 2, x + w + 2, y + h + 2, OBS_MID);

        // 2. Top & Bottom Centered Volcanic Conduit Crests
        int midX = x + w / 2;
        int crestHalf = Math.min(42, w / 3);
        // Top crest
        g.fill(midX - crestHalf - 2, y - 5, midX + crestHalf + 2, y - 1, OBS_VOID);
        g.fill(midX - crestHalf, y - 4, midX + crestHalf, y - 1, OBS_MID);
        g.fill(midX - crestHalf, y - 4, midX + crestHalf, y - 3, mainTrim);
        g.fill(midX - 18, y - 3, midX + 18, y - 2, pulse > 0.5f ? MAGMA_HOT : MAGMA_ORANGE);
        g.fill(midX - crestHalf + 3, y - 3, midX - crestHalf + 6, y - 2, techNode);
        g.fill(midX + crestHalf - 6, y - 3, midX + crestHalf - 3, y - 2, techNode);

        // Bottom crest
        g.fill(midX - crestHalf - 2, y + h + 1, midX + crestHalf + 2, y + h + 5, OBS_VOID);
        g.fill(midX - crestHalf, y + h + 1, midX + crestHalf, y + h + 4, OBS_DARK);
        g.fill(midX - crestHalf, y + h + 3, midX + crestHalf, y + h + 4, darkTrim);
        g.fill(midX - 16, y + h + 2, midX + 16, y + h + 3, MAGMA_ORANGE);

        // 3. Left & Right Exterior Thermal Heat-Sink Fins (Upper Workspace Area)
        int finTop = y + 18;
        int finBot = Math.min(y + h - 24, y + 68);
        if (finBot - finTop >= 24) {
            // Left Fin Housing (protrudes 7px to the left)
            g.fill(x - 7, finTop - 1, x, finBot + 1, OBS_VOID);
            g.fill(x - 6, finTop, x - 1, finBot, OBS_MID);
            g.fill(x - 6, finTop, x - 5, finBot, mainTrim);

            // Right Fin Housing (protrudes 7px to the right)
            g.fill(x + w, finTop - 1, x + w + 7, finBot + 1, OBS_VOID);
            g.fill(x + w + 1, finTop, x + w + 6, finBot, OBS_MID);
            g.fill(x + w + 5, finTop, x + w + 6, finBot, mainTrim);

            // Glowing Magma / Tech Louvers inside both fins
            int louverGlow = (pulse > 0.4f) ? MAGMA_AMBER : MAGMA_ORANGE;
            for (int ly = finTop + 3; ly <= finBot - 4; ly += 6) {
                // Left louver
                g.fill(x - 4, ly, x - 1, ly + 3, OBS_VOID);
                g.fill(x - 4, ly + 1, x - 2, ly + 2, louverGlow);
                // Right louver
                g.fill(x + w + 1, ly, x + w + 4, ly + 3, OBS_VOID);
                g.fill(x + w + 2, ly + 1, x + w + 4, ly + 2, louverGlow);
            }

            // Secondary lower side conduit tabs (Player Inventory level)
            int invFinTop = finBot + 18;
            int invFinBot = Math.min(y + h - 12, invFinTop + 36);
            if (invFinBot - invFinTop >= 16) {
                g.fill(x - 6, invFinTop, x, invFinBot, OBS_VOID);
                g.fill(x - 5, invFinTop + 1, x - 1, invFinBot - 1, OBS_DARK);
                g.fill(x - 4, invFinTop + 3, x - 3, invFinBot - 3, techDim);

                g.fill(x + w, invFinTop, x + w + 6, invFinBot, OBS_VOID);
                g.fill(x + w + 1, invFinTop + 1, x + w + 5, invFinBot - 1, OBS_DARK);
                g.fill(x + w + 3, invFinTop + 3, x + w + 4, invFinBot - 3, techDim);
            }
        }

        // 4. Heavy 4-Corner Obsidian + Tech + Vulcan Armor Brackets
        drawCornerBracket(g, x - 4, y - 4, 1, 1, mainTrim, brightTrim, techNode);
        drawCornerBracket(g, x + w + 3, y - 4, -1, 1, mainTrim, brightTrim, techNode);
        drawCornerBracket(g, x - 4, y + h + 3, 1, -1, mainTrim, brightTrim, techNode);
        drawCornerBracket(g, x + w + 3, y + h + 3, -1, -1, mainTrim, brightTrim, techNode);

        // 5. Machine Sound Mute / Unmute Button on top-right corner
        renderMuteButton(g, x, y, w, h);
    }

    public static int getMuteButtonX(int x, int w) {
        return x + w - 16;
    }

    public static int getMuteButtonY(int y) {
        return y - 13;
    }

    public static boolean isMuteButtonHovered(int x, int y, int w, double mouseX, double mouseY) {
        int bx = getMuteButtonX(x, w);
        int by = getMuteButtonY(y);
        return mouseX >= bx && mouseX < bx + 13 && mouseY >= by && mouseY < by + 12;
    }

    public static boolean handleMuteClick(int x, int y, int w, double mouseX, double mouseY) {
        if (isMuteButtonHovered(x, y, w, mouseX, mouseY)) {
            net.valentin.obsidium.sound.MachineSoundConfig.toggleMute();
            return true;
        }
        return false;
    }

    public static void renderMuteTooltip(net.minecraft.client.gui.Font font, GuiGraphics g, int x, int y, int w, int mouseX, int mouseY) {
        if (isMuteButtonHovered(x, y, w, mouseX, mouseY)) {
            boolean muted = net.valentin.obsidium.sound.MachineSoundConfig.isMuted();
            g.renderComponentTooltip(font, java.util.List.of(
                net.minecraft.network.chat.Component.literal(muted ? "§c🔇 Machine Sounds: MUTED" : "§a🔊 Machine Sounds: ACTIVE"),
                net.minecraft.network.chat.Component.literal("§7Click to " + (muted ? "unmute all machine SFX" : "mute all machine SFX"))
            ), mouseX, mouseY);
        }
    }

    private static void renderMuteButton(GuiGraphics g, int x, int y, int w, int h) {
        renderMuteButton(g, getMuteButtonX(x, w), getMuteButtonY(y));
    }

    public static void renderMuteButton(GuiGraphics g, int bx, int by) {
        boolean muted = net.valentin.obsidium.sound.MachineSoundConfig.isMuted();

        // Background chip
        g.fill(bx, by, bx + 13, by + 12, 0xFF140C1A);
        g.fill(bx + 1, by + 1, bx + 12, by + 11, muted ? 0xFF2A1015 : 0xFF12241D);

        // Border
        int borderCol = muted ? 0xFFE63946 : 0xFF2A9D8F;
        g.fill(bx, by, bx + 13, by + 1, borderCol);
        g.fill(bx, by + 11, bx + 13, by + 12, borderCol);
        g.fill(bx, by, bx + 1, by + 12, borderCol);
        g.fill(bx + 12, by, bx + 13, by + 12, borderCol);

        // Mini Speaker Icon Pixel Art (7x7 pixels inside)
        // Speaker body:
        int iconCol = muted ? 0xFFFF6B6B : 0xFF52B788;
        g.fill(bx + 3, by + 4, bx + 5, by + 8, iconCol);
        g.fill(bx + 5, by + 3, bx + 6, by + 9, iconCol);
        g.fill(bx + 6, by + 2, bx + 7, by + 10, iconCol);

        if (muted) {
            // Red "X" slash next to speaker
            int xCol = 0xFFFF3333;
            g.fill(bx + 8, by + 4, bx + 9, by + 5, xCol);
            g.fill(bx + 10, by + 4, bx + 11, by + 5, xCol);
            g.fill(bx + 9, by + 5, bx + 10, by + 7, xCol);
            g.fill(bx + 8, by + 7, bx + 9, by + 8, xCol);
            g.fill(bx + 10, by + 7, bx + 11, by + 8, xCol);
        } else {
            // Sound wave arcs
            g.fill(bx + 8, by + 3, bx + 9, by + 4, iconCol);
            g.fill(bx + 9, by + 4, bx + 10, by + 8, iconCol);
            g.fill(bx + 8, by + 8, bx + 9, by + 9, iconCol);
        }
    }

    private static void drawCornerBracket(GuiGraphics g, int cx, int cy, int dx, int dy, int trim, int hi, int node) {
        // Horizontal arm (12px) and Vertical arm (12px)
        int hx1 = Math.min(cx, cx + 11 * dx);
        int hx2 = Math.max(cx, cx + 11 * dx) + 1;
        int hy1 = Math.min(cy, cy + 2 * dy);
        int hy2 = Math.max(cy, cy + 2 * dy) + 1;
        g.fill(hx1, hy1, hx2, hy2, trim);

        int vx1 = Math.min(cx, cx + 2 * dx);
        int vx2 = Math.max(cx, cx + 2 * dx) + 1;
        int vy1 = Math.min(cy, cy + 11 * dy);
        int vy2 = Math.max(cy, cy + 11 * dy) + 1;
        g.fill(vx1, vy1, vx2, vy2, trim);

        // Specular corner edge
        int sx1 = Math.min(cx, cx + 5 * dx);
        int sx2 = Math.max(cx, cx + 5 * dx) + 1;
        g.fill(sx1, Math.min(cy, cy + dy), sx2, Math.max(cy, cy + dy) + 1, hi);
        g.fill(Math.min(cx, cx + dx), Math.min(cy, cy + 5 * dy), Math.max(cx, cx + dx) + 1, Math.max(cy, cy + 5 * dy) + 1, hi);

        // Glowing 2x2 Power Rivet at corner bend
        int rx = cx + 2 * dx;
        int ry = cy + 2 * dy;
        g.fill(Math.min(rx, rx + dx), Math.min(ry, ry + dy), Math.max(rx, rx + dx) + 1, Math.max(ry, ry + dy) + 1, node);
    }

    public static void renderUpgradeSlot(GuiGraphics g, int slotX, int slotY, boolean hasItem) {
        // Outer 18x18 recessed socket
        g.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF0D0A14);
        g.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF1C1328);
        g.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 2, 0xFF3D2A54);
        g.fill(slotX + 1, slotY + 1, slotX + 2, slotY + 17, 0xFF3D2A54);
        g.fill(slotX + 16, slotY + 1, slotX + 17, slotY + 17, 0xFF09060E);
        g.fill(slotX + 1, slotY + 16, slotX + 17, slotY + 17, 0xFF09060E);

        // Tech corner brackets (Gold accents)
        g.fill(slotX, slotY, slotX + 3, slotY + 1, 0xFFFFB700);
        g.fill(slotX, slotY, slotX + 1, slotY + 3, 0xFFFFB700);
        g.fill(slotX + 15, slotY, slotX + 18, slotY + 1, 0xFFFFB700);
        g.fill(slotX + 17, slotY, slotX + 18, slotY + 3, 0xFFFFB700);
        g.fill(slotX, slotY + 17, slotX + 3, slotY + 18, 0xFFFFB700);
        g.fill(slotX, slotY + 15, slotX + 1, slotY + 18, 0xFFFFB700);
        g.fill(slotX + 15, slotY + 17, slotX + 18, slotY + 18, 0xFFFFB700);
        g.fill(slotX + 17, slotY + 15, slotX + 18, slotY + 18, 0xFFFFB700);

        // Ghost lightning bolt icon if empty
        if (!hasItem) {
            g.fill(slotX + 9, slotY + 4, slotX + 11, slotY + 8, 0x55FFB700);
            g.fill(slotX + 7, slotY + 8, slotX + 12, slotY + 10, 0x55FFB700);
            g.fill(slotX + 8, slotY + 10, slotX + 10, slotY + 14, 0x55FFB700);
        }
    }

    /** Renders a complete dark obsidian window with exterior armor frame and recessed interior. */
    public static void renderWindow(GuiGraphics g, int x, int y, int w, int h, boolean advanced) {
        renderExteriorFrame(g, x, y, w, h, advanced);
        g.fill(x, y, x + w, y + h, OBS_DARK);
        g.renderOutline(x, y, w, h, advanced ? GOLD_MID : MAGMA_ORANGE);
        g.renderOutline(x + 1, y + 1, w - 2, h - 2, OBS_HIGHLIGHT);
    }

    /** Renders a recessed dark obsidian machine panel. */
    public static void renderRecessedPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF0E0916);
        g.renderOutline(x, y, w, h, OBS_HIGHLIGHT);
    }

    /** Renders a 18x18 slot frame centered around a 16x16 slot at (slotX, slotY). */
    public static void renderSlot(GuiGraphics g, int slotX, int slotY, int borderColor) {
        int sx = slotX - 1;
        int sy = slotY - 1;
        g.fill(sx, sy, sx + 18, sy + 18, 0xFF09060E);
        g.renderOutline(sx, sy, 18, 18, borderColor);
    }

    /** Renders the standard 9x3 inventory + 9x1 hotbar slot grid starting at (invX, invY). */
    public static void renderPlayerInventory(GuiGraphics g, int invX, int invY) {
        int panelX = invX - 3;
        int panelY = invY - 3;
        int panelW = 9 * 18 + 4;
        int panelH = 4 * 18 + 8;
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF0E0916);
        g.renderOutline(panelX, panelY, panelW, panelH, 0xFF281D38);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                renderSlot(g, invX + col * 18, invY + row * 18, 0xFF332645);
            }
        }
        int hotbarY = invY + 58;
        for (int col = 0; col < 9; col++) {
            renderSlot(g, invX + col * 18, hotbarY, 0xFF423159);
        }
    }
}

