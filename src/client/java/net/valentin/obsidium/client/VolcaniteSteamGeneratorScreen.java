package net.valentin.obsidium.client;

import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.block.VolcaniteSteamGeneratorBlockEntity;
import net.valentin.obsidium.block.VolcaniteSteamGeneratorMenu;


/**
 * Industrial "Steam Turbine" GUI: Water tank (left) -> spinning turbine (center) -> RF tank (right),
 * with a firebox fuel slot, a steam pressure gauge and a burn-time bar in the bottom panel.
 */
public class VolcaniteSteamGeneratorScreen extends AbstractContainerScreen<VolcaniteSteamGeneratorMenu> {

    // Layout constants (relative to GUI origin)
    private static final int WATER_X = 16, WATER_Y = 26, TANK_W = 28, TANK_H = 66;
    private static final int RF_X = 192, RF_Y = 26;
    private static final int TURB_X = 62, TURB_Y = 22, TURB_W = 112, TURB_H = 70;
    private static final int TURB_CX = 118, TURB_CY = 57, TURB_R = 30;
    private static final int PANEL_X = 62, PANEL_Y = 96, PANEL_W = 112, PANEL_H = 28;
    private static final int GAUGE_X = 90, GAUGE_Y = 100, GAUGE_W = 70, GAUGE_H = 20;
    private static final int BURN_X = 164, BURN_Y = 100, BURN_W = 6, BURN_H = 20;

    private float turbineAngle = 0f;
    private float turbineSpeed = 0f;
    private long lastFrameMs = System.currentTimeMillis();

    public VolcaniteSteamGeneratorScreen(VolcaniteSteamGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 236;
        this.imageHeight = 222;
        this.titleLabelX = 10;
        this.titleLabelY = 8;
        this.inventoryLabelX = 38;
        this.inventoryLabelY = 129;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF48CAE4, false);
        String rate = this.menu.isBurning() ? "20 RF/t" : "IDLE";
        g.drawString(this.font, rate, this.imageWidth - 10 - this.font.width(rate), this.titleLabelY,
                this.menu.isBurning() ? 0xFFFFCC33 : 0xFF777777, false);
        g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFCECAC5, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        if (in(mouseX, mouseY, x + WATER_X, y + WATER_Y, TANK_W, TANK_H)) {
            tip(g, mouseX, mouseY, "§b💧 Water Tank",
                    "§f" + String.format("%,d / %,d mB", this.menu.getWaterAmount(), VolcaniteSteamGeneratorBlockEntity.MAX_WATER),
                    "§7Insert Water Bucket below");
        } else if (in(mouseX, mouseY, x + RF_X, y + RF_Y, TANK_W, TANK_H)) {
            tip(g, mouseX, mouseY, "§6⚡ Energy Buffer",
                    "§e" + String.format("%,d / %,d RF", this.menu.getEnergyAmount(), VolcaniteSteamGeneratorBlockEntity.MAX_ENERGY),
                    "§7Auto-outputs to adjacent wires");
        } else if (in(mouseX, mouseY, x + TURB_CX - TURB_R, y + TURB_CY - TURB_R, TURB_R * 2, TURB_R * 2)) {
            if (this.menu.isBurning()) {
                tip(g, mouseX, mouseY, "§a⚙ Turbine Spinning", "§f+20 RF/t & Steam", "§7Fuel left: " + (this.menu.getBurnTime() / 20) + "s");
            } else {
                tip(g, mouseX, mouseY, "§7⚙ Turbine Idle", "§cNeeds Fuel + Water");
            }
        } else if (in(mouseX, mouseY, x + GAUGE_X, y + GAUGE_Y, GAUGE_W, GAUGE_H)) {
            tip(g, mouseX, mouseY, "§f☁ Steam Pressure",
                    "§f" + String.format("%,d / %,d mB", this.menu.getSteamAmount(), VolcaniteSteamGeneratorBlockEntity.MAX_STEAM),
                    "§bAuto-pumps to Tanks & Arborist");
        } else if (in(mouseX, mouseY, x + BURN_X, y + BURN_Y, BURN_W, BURN_H)) {
            tip(g, mouseX, mouseY, "§6🔥 Firebox", "§7Burn: " + (this.menu.getBurnTime() / 20) + "s");
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - lastFrameMs) / 1000f);
        lastFrameMs = now;

        VolcaniteGuiTheme.renderExteriorFrame(g, x, y, imageWidth, imageHeight, true);

        // ===== Chassis =====
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF0F0A15);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF17111F);
        border(g, x, y, imageWidth, imageHeight, 0xFFFF7A00);
        border(g, x + 1, y + 1, imageWidth - 2, imageHeight - 2, 0xFF4A1E00);
        g.fill(x + 6, y + 4, x + imageWidth - 6, y + 19, 0xFF201629);
        border(g, x + 6, y + 4, imageWidth - 12, 15, 0xFF5A2A00);

        boolean burning = this.menu.isBurning();

        // ===== Left: Water tank =====
        drawVents(g, x + 6, y + WATER_Y + 8, 0xFF1E88E5);
        drawTankFrame(g, x + WATER_X, y + WATER_Y, 0xFF1E88E5);
        float waterPct = pct(this.menu.getWaterAmount(), VolcaniteSteamGeneratorBlockEntity.MAX_WATER);
        int wh = Math.round(waterPct * (TANK_H - 4));
        if (wh > 0) {
            int top = y + WATER_Y + TANK_H - 2 - wh;
            g.fillGradient(x + WATER_X + 2, top, x + WATER_X + TANK_W - 2, y + WATER_Y + TANK_H - 2, 0xFF6FD3FF, 0xFF0B6FC2);
            g.fill(x + WATER_X + 2, top, x + WATER_X + TANK_W - 2, top + 1, 0xFFCFF4FF);
            // Rising bubbles
            for (int b = 0; b < 6; b++) {
                int bx = x + WATER_X + 5 + (b * 7) % (TANK_W - 9);
                int span = Math.max(1, wh - 3);
                int by = y + WATER_Y + TANK_H - 4 - (int) (((now / 25) + b * 17) % span);
                g.fill(bx, by, bx + 2, by + 2, 0xCCFFFFFF);
            }
        }
        drawTankTicks(g, x + WATER_X, y + WATER_Y);
        drawSlotFrame(g, x + 21, y + 100, 0xFF1E88E5);

        // ===== Right: RF tank =====
        drawVents(g, x + imageWidth - 12, y + RF_Y + 8, 0xFFFF6A00);
        drawTankFrame(g, x + RF_X, y + RF_Y, 0xFFFF5522);
        float rfPct = pct(this.menu.getEnergyAmount(), VolcaniteSteamGeneratorBlockEntity.MAX_ENERGY);
        int rh = Math.round(rfPct * (TANK_H - 4));
        if (rh > 0) {
            int top = y + RF_Y + TANK_H - 2 - rh;
            g.fillGradient(x + RF_X + 2, top, x + RF_X + TANK_W - 2, y + RF_Y + TANK_H - 2, 0xFFFFE066, 0xFFE0300B);
            g.fill(x + RF_X + 8, top, x + RF_X + TANK_W - 8, y + RF_Y + TANK_H - 2, 0x55FFFFFF);
        }
        drawTankTicks(g, x + RF_X, y + RF_Y);
        drawSlotFrame(g, x + 197, y + 100, 0xFFFF5522);

        // ===== Pipes (tanks <-> turbine) =====
        drawPipe(g, x + 46, y + 38, burning);
        drawPipe(g, x + 46, y + 74, burning);
        drawPipe(g, x + 176, y + 38, burning);
        drawPipe(g, x + 176, y + 74, burning);

        // ===== Center: Turbine housing =====
        g.fill(x + TURB_X, y + TURB_Y, x + TURB_X + TURB_W, y + TURB_Y + TURB_H, 0xFF120C19);
        border(g, x + TURB_X, y + TURB_Y, TURB_W, TURB_H, 0xFF3A2747);
        drawBrackets(g, x + TURB_X + 4, y + TURB_Y + 3, TURB_W - 8, TURB_H - 6, 0xFFFFB300);

        // Spin physics: accelerate while burning, coast down when idle
        float target = burning ? 540f : 0f;
        turbineSpeed += (target - turbineSpeed) * Math.min(1f, dt * 1.6f);
        turbineAngle = (turbineAngle + turbineSpeed * dt) % 360f;

        int cx = x + TURB_CX, cy = y + TURB_CY;
        drawDisc(g, cx, cy, TURB_R, 0xFF0A0710);
        drawRing(g, cx, cy, TURB_R, 2, burning ? 0xFFFF8A00 : 0xFF7A3A10);
        drawRing(g, cx, cy, TURB_R - 3, 1, 0xFF3A1A08);

        g.pose().pushPose();
        g.pose().translate(cx + 0.5f, cy + 0.5f, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(turbineAngle));
        for (int i = 0; i < 8; i++) {
            g.pose().pushPose();
            g.pose().mulPose(Axis.ZP.rotationDegrees(i * 45f));
            int bladeMain = (i % 2 == 0) ? 0xFFE8E8EE : 0xFF9C3A16;
            int bladeEdge = (i % 2 == 0) ? 0xFF9FA3AE : 0xFF5E200B;
            // Curved blade made of 3 offset segments (swirl look)
            g.fill(5, -2, 13, 2, bladeMain);
            g.fill(12, -1, 20, 3, bladeMain);
            g.fill(19, 1, 25, 5, bladeMain);
            g.fill(5, 2, 13, 3, bladeEdge);
            g.fill(12, 3, 20, 4, bladeEdge);
            g.pose().popPose();
        }
        g.pose().popPose();

        // Hub
        drawDisc(g, cx, cy, 6, 0xFF7A4A00);
        drawDisc(g, cx, cy, 4, burning ? 0xFFFFD54F : 0xFFAA8833);
        g.fill(cx - 1, cy - 1, cx + 1, cy + 1, 0xFFFFFFFF);

        // ===== Bottom panel: firebox + steam gauge + burn bar =====
        g.fill(x + PANEL_X, y + PANEL_Y, x + PANEL_X + PANEL_W, y + PANEL_Y + PANEL_H, 0xFF140E1B);
        border(g, x + PANEL_X, y + PANEL_Y, PANEL_W, PANEL_H, 0xFF3A2747);
        drawSlotFrame(g, x + 67, y + 101, burning ? 0xFFFF8A00 : 0xFFAA5500);

        // Steam gauge: 10 vertical cells
        g.fill(x + GAUGE_X, y + GAUGE_Y, x + GAUGE_X + GAUGE_W, y + GAUGE_Y + GAUGE_H, 0xFF09060C);
        border(g, x + GAUGE_X, y + GAUGE_Y, GAUGE_W, GAUGE_H, 0xFFFF7A00);
        float steamPct = pct(this.menu.getSteamAmount(), VolcaniteSteamGeneratorBlockEntity.MAX_STEAM);
        int litCells = Math.round(steamPct * 10f);
        for (int c = 0; c < 10; c++) {
            int cx0 = x + GAUGE_X + 2 + c * 7;
            boolean lit = c < litCells;
            int colTop = lit ? 0xFFFFE27A : 0xFF2A1A10;
            int colBot = lit ? 0xFFFF7A00 : 0xFF1C110A;
            g.fillGradient(cx0, y + GAUGE_Y + 2, cx0 + 5, y + GAUGE_Y + GAUGE_H - 2, colTop, colBot);
        }

        // Burn-time bar
        g.fill(x + BURN_X, y + BURN_Y, x + BURN_X + BURN_W, y + BURN_Y + BURN_H, 0xFF09060C);
        border(g, x + BURN_X, y + BURN_Y, BURN_W, BURN_H, 0xFF5A3A10);
        if (burning) {
            float bp = pct(this.menu.getBurnTime(), Math.max(1, this.menu.getMaxBurnTime()));
            int bh = Math.max(1, Math.round(bp * (BURN_H - 2)));
            g.fillGradient(x + BURN_X + 1, y + BURN_Y + BURN_H - 1 - bh, x + BURN_X + BURN_W - 1, y + BURN_Y + BURN_H - 1, 0xFFFFF59D, 0xFFFFB300);
        }

        // ===== Player inventory frames =====
        for (int r = 0; r < 3; ++r) {
            for (int c = 0; c < 9; ++c) {
                drawSlotFrame(g, x + 37 + c * 18, y + 139 + r * 18, 0xFF4A355A);
            }
        }
        for (int c = 0; c < 9; ++c) {
            drawSlotFrame(g, x + 37 + c * 18, y + 197, 0xFF6A457A);
        }
    }

    // ---------- helpers ----------

    private static float pct(long v, long max) {
        return max <= 0 ? 0f : Math.max(0f, Math.min(1f, (float) v / (float) max));
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void tip(GuiGraphics g, int mx, int my, String... lines) {
        g.renderComponentTooltip(this.font, java.util.Arrays.stream(lines).map(s -> (Component) Component.literal(s)).toList(), mx, my);
    }

    private void border(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + 1, c);
        g.fill(x, y + h - 1, x + w, y + h, c);
        g.fill(x, y, x + 1, y + h, c);
        g.fill(x + w - 1, y, x + w, y + h, c);
    }

    private void drawSlotFrame(GuiGraphics g, int x, int y, int c) {
        g.fill(x, y, x + 18, y + 18, 0xFF0D0812);
        border(g, x, y, 18, 18, c);
    }

    private void drawTankFrame(GuiGraphics g, int x, int y, int accent) {
        g.fill(x - 3, y - 4, x + TANK_W + 3, y + TANK_H + 3, 0xFF1A1322);
        border(g, x - 3, y - 4, TANK_W + 6, TANK_H + 7, 0xFF33263F);
        g.fill(x + 4, y - 7, x + TANK_W - 4, y - 4, 0xFF2A1F35); // top cap
        g.fill(x, y, x + TANK_W, y + TANK_H, 0xFF07050A);
        border(g, x, y, TANK_W, TANK_H, accent);
    }

    private void drawTankTicks(GuiGraphics g, int x, int y) {
        for (int t = 1; t < 6; t++) {
            int ty = y + 2 + t * (TANK_H - 4) / 6;
            g.fill(x + 2, ty, x + 7, ty + 1, 0x66FFFFFF);
        }
    }

    private void drawVents(GuiGraphics g, int x, int y, int c) {
        for (int i = 0; i < 7; i++) {
            g.fill(x, y + i * 7, x + 6, y + i * 7 + 2, c);
        }
    }

    private void drawPipe(GuiGraphics g, int x, int y, boolean active) {
        g.fill(x, y, x + 14, y + 6, 0xFF3A1A08);
        g.fill(x, y + 1, x + 14, y + 5, active ? 0xFFFF8A00 : 0xFF8A4410);
        g.fill(x, y + 2, x + 14, y + 3, active ? 0xFFFFD27A : 0xFFB06020);
        g.fill(x + 4, y - 1, x + 6, y + 7, 0xFF241016);
        g.fill(x + 9, y - 1, x + 11, y + 7, 0xFF241016);
    }

    private void drawBrackets(GuiGraphics g, int x, int y, int w, int h, int c) {
        int l = 8;
        g.fill(x, y, x + l, y + 2, c);           g.fill(x, y, x + 2, y + l, c);
        g.fill(x + w - l, y, x + w, y + 2, c);   g.fill(x + w - 2, y, x + w, y + l, c);
        g.fill(x, y + h - 2, x + l, y + h, c);   g.fill(x, y + h - l, x + 2, y + h, c);
        g.fill(x + w - l, y + h - 2, x + w, y + h, c); g.fill(x + w - 2, y + h - l, x + w, y + h, c);
    }

    private void drawDisc(GuiGraphics g, int cx, int cy, int r, int c) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.floor(Math.sqrt(r * r - dy * dy));
            g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, c);
        }
    }

    private void drawRing(GuiGraphics g, int cx, int cy, int r, int thickness, int c) {
        int rIn = r - thickness;
        for (int dy = -r; dy <= r; dy++) {
            int outer = (int) Math.floor(Math.sqrt(r * r - dy * dy));
            int inner = Math.abs(dy) < rIn ? (int) Math.floor(Math.sqrt(rIn * rIn - dy * dy)) : -1;
            if (inner < 0) {
                g.fill(cx - outer, cy + dy, cx + outer + 1, cy + dy + 1, c);
            } else {
                g.fill(cx - outer, cy + dy, cx - inner, cy + dy + 1, c);
                g.fill(cx + inner + 1, cy + dy, cx + outer + 1, cy + dy + 1, c);
            }
        }
    }
}
