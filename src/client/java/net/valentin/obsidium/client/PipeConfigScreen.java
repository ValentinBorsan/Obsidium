package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.valentin.obsidium.block.PipeConfigMenu;
import net.valentin.obsidium.block.PipeConfigurable;
import net.valentin.obsidium.item.QuantumLinkModuleItem;

public class PipeConfigScreen extends AbstractContainerScreen<PipeConfigMenu> {
    private static final String[] FACE_SHORT = {"D", "U", "N", "S", "W", "E"};

    public PipeConfigScreen(PipeConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 190;
        this.inventoryLabelY = 97;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        String header = this.menu.getPipeKind() == PipeConfigurable.KIND_FLUID ? "Naiad Fluid Pipe" : "Astrape Energy Wire";
        g.drawString(this.font, header, 8, 6, 0xFFD166, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xCECAC5, false);
        if (this.menu.getPipeKind() == PipeConfigurable.KIND_FLUID) {
            g.drawString(this.font, "Filter", 143, 31, 0x77DDFF, false);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        VolcaniteGuiTheme.renderExteriorFrame(g, x, y, this.imageWidth, this.imageHeight, true);
        g.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF140F1A);

        // Face tabs (D,U,N,S,W,E) at y + 18
        int selFace = this.menu.getSelectedFace();
        for (int f = 0; f < 6; f++) {
            int bx = x + 8 + f * 26;
            int col = (f == selFace) ? 0xFFFFCC44 : 0xFF556688;
            drawBtn(g, bx, y + 18, 24, 12, FACE_SHORT[f], col, mouseX, mouseY);
        }

        // Row 1 (y + 34): Face Mode & Pipe Color
        int fMode = this.menu.getFaceModeOrdinal();
        String modeTxt = switch (fMode) {
            case 1 -> "EXPORT";
            case 2 -> "IMPORT";
            default -> "NORMAL";
        };
        int modeCol = switch (fMode) {
            case 1 -> 0xFFFFAA22;
            case 2 -> 0xFF22DDFF;
            default -> 0xFFCCFF66;
        };
        drawBtn(g, x + 8, y + 34, 62, 13, modeTxt, modeCol, mouseX, mouseY);

        int pColor = this.menu.getPipeColor();
        drawBtn(g, x + 74, y + 34, 12, 13, "<", 0xFFAAAACC, mouseX, mouseY);
        int pRgb = (pColor >= 0) ? (0xFF000000 | QuantumLinkModuleItem.getChannelRgb(DyeColor.byId(pColor))) : 0xFF332D3F;
        g.fill(x + 87, y + 34, x + 123, y + 47, 0xFF000000);
        g.fill(x + 88, y + 35, x + 122, y + 46, pRgb);
        String pLabel = pColor >= 0 ? "DYE" : "NONE";
        int plw = this.font.width(pLabel);
        g.drawString(this.font, pLabel, x + 105 - plw / 2, y + 37, pColor == DyeColor.WHITE.getId() || pColor == DyeColor.YELLOW.getId() ? 0xFF111111 : 0xFFFFFFFF, false);
        drawBtn(g, x + 124, y + 34, 12, 13, ">", 0xFFAAAACC, mouseX, mouseY);

        // Fluid Filter slot box
        if (this.menu.getPipeKind() == PipeConfigurable.KIND_FLUID) {
            g.fill(x + 147, y + 41, x + 165, y + 59, 0xFF0D0A12);
            g.fill(x + 148, y + 42, x + 164, y + 58, 0xFF232C3C);
        }

        // Row 2 (y + 51): Redstone, Round-Robin, Throughput
        String rsTxt = switch (this.menu.getRedstoneMode()) {
            case PipeConfigurable.REDSTONE_HIGH -> "RS:HIGH";
            case PipeConfigurable.REDSTONE_LOW -> "RS:LOW";
            case PipeConfigurable.REDSTONE_OFF -> "RS:OFF";
            default -> "RS:ON";
        };
        int rsCol = this.menu.getRedstoneMode() == PipeConfigurable.REDSTONE_OFF ? 0xFFFF5566 : 0xFFFF9944;
        drawBtn(g, x + 8, y + 51, 46, 13, rsTxt, rsCol, mouseX, mouseY);

        boolean rr = this.menu.isRoundRobin();
        drawBtn(g, x + 56, y + 51, 42, 13, rr ? "RR:ON" : "NEAR", rr ? 0xFF66FFAA : 0xFF88AAFF, mouseX, mouseY);

        int pct = this.menu.getThroughputPercent();
        drawBtn(g, x + 100, y + 51, 36, 13, pct + "%", 0xFFE6C8FF, mouseX, mouseY);

        // Row 3 (y + 68): Quantum Link Channel Controls (if linked)
        if (this.menu.isQuantumLinked()) {
            DyeColor qCol = this.menu.getQuantumColor();
            int qRgb = 0xFF000000 | QuantumLinkModuleItem.getChannelRgb(qCol);
            drawBtn(g, x + 8, y + 68, 12, 13, "<", 0xFFD088FF, mouseX, mouseY);
            g.fill(x + 21, y + 68, x + 37, y + 81, 0xFF000000);
            g.fill(x + 22, y + 69, x + 36, y + 80, qRgb);
            drawBtn(g, x + 38, y + 68, 12, 13, ">", 0xFFD088FF, mouseX, mouseY);

            drawBtn(g, x + 53, y + 68, 14, 13, "--", 0xFF77AADD, mouseX, mouseY);
            drawBtn(g, x + 68, y + 68, 12, 13, "-", 0xFF99CCFF, mouseX, mouseY);
            String chTxt = "#" + this.menu.getQuantumChannel();
            int chw = this.font.width(chTxt);
            g.drawString(this.font, chTxt, x + 95 - chw / 2, y + 71, 0xFFE6C8FF, false);
            drawBtn(g, x + 111, y + 68, 12, 13, "+", 0xFF99CCFF, mouseX, mouseY);
            drawBtn(g, x + 124, y + 68, 14, 13, "++", 0xFF77AADD, mouseX, mouseY);

            boolean priv = this.menu.isQuantumPrivate();
            drawBtn(g, x + 141, y + 68, 27, 13, priv ? "PRV" : "PUB", priv ? 0xFFFF6688 : 0xFF66FFAA, mouseX, mouseY);
        } else {
            g.drawString(this.font, "§8Quantum Link: Not Installed", x + 8, y + 71, 0xFF777788, false);
        }

        // Player Inventory Background
        g.fill(x + 6, y + 106, x + 170, y + 186, 0xFF1C1624);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                int sx = x + 7 + c * 18;
                int sy = y + 107 + r * 18;
                g.fill(sx, sy, sx + 18, sy + 18, 0xFF0D0A12);
                g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF201A28);
            }
        }
        for (int c = 0; c < 9; c++) {
            int sx = x + 7 + c * 18;
            int sy = y + 165;
            g.fill(sx, sy, sx + 18, sy + 18, 0xFF0D0A12);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF261E30);
        }
    }

    private void drawBtn(GuiGraphics g, int bx, int by, int bw, int bh, String text, int col, int mx, int my) {
        boolean hov = mx >= bx && mx < bx + bw && my >= by && my < by + bh;
        g.fill(bx, by, bx + bw, by + bh, hov ? 0xFFFFFFFF : col);
        g.fill(bx + 1, by + 1, bx + bw - 1, by + bh - 1, hov ? 0xFF342648 : 0xFF1A1424);
        int tw = this.font.width(text);
        g.drawString(this.font, text, bx + (bw - tw) / 2, by + (bh - 7) / 2, hov ? 0xFFFFFFFF : col, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.minecraft != null && this.minecraft.gameMode != null) {
            int x = (this.width - this.imageWidth) / 2;
            int y = (this.height - this.imageHeight) / 2;
            int btn = -1;
            for (int f = 0; f < 6; f++) {
                if (inBox(mouseX, mouseY, x + 8 + f * 26, y + 18, 24, 12)) {
                    btn = PipeConfigMenu.BTN_FACE_BASE + f;
                    break;
                }
            }
            if (btn < 0) {
                if (inBox(mouseX, mouseY, x + 8, y + 34, 62, 13)) btn = PipeConfigMenu.BTN_CYCLE_FACE_MODE;
                else if (inBox(mouseX, mouseY, x + 74, y + 34, 12, 13)) btn = PipeConfigMenu.BTN_PREV_PIPE_COLOR;
                else if (inBox(mouseX, mouseY, x + 124, y + 34, 12, 13)) btn = PipeConfigMenu.BTN_NEXT_PIPE_COLOR;
                else if (inBox(mouseX, mouseY, x + 8, y + 51, 46, 13)) btn = PipeConfigMenu.BTN_CYCLE_REDSTONE;
                else if (inBox(mouseX, mouseY, x + 56, y + 51, 42, 13)) btn = PipeConfigMenu.BTN_TOGGLE_ROUND_ROBIN;
                else if (inBox(mouseX, mouseY, x + 100, y + 51, 36, 13)) btn = PipeConfigMenu.BTN_CYCLE_THROUGHPUT;
                else if (this.menu.isQuantumLinked()) {
                    if (inBox(mouseX, mouseY, x + 8, y + 68, 12, 13)) btn = PipeConfigMenu.BTN_Q_PREV_COLOR;
                    else if (inBox(mouseX, mouseY, x + 38, y + 68, 12, 13)) btn = PipeConfigMenu.BTN_Q_NEXT_COLOR;
                    else if (inBox(mouseX, mouseY, x + 53, y + 68, 14, 13)) btn = PipeConfigMenu.BTN_Q_DEC_10;
                    else if (inBox(mouseX, mouseY, x + 68, y + 68, 12, 13)) btn = PipeConfigMenu.BTN_Q_DEC_1;
                    else if (inBox(mouseX, mouseY, x + 111, y + 68, 12, 13)) btn = PipeConfigMenu.BTN_Q_INC_1;
                    else if (inBox(mouseX, mouseY, x + 124, y + 68, 14, 13)) btn = PipeConfigMenu.BTN_Q_INC_10;
                    else if (inBox(mouseX, mouseY, x + 141, y + 68, 27, 13)) btn = PipeConfigMenu.BTN_Q_TOGGLE_PRIVATE;
                }
            }
            if (btn >= 0) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, btn);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }

    private static boolean inBox(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
