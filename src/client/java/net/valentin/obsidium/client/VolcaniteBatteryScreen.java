package net.valentin.obsidium.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.VolcaniteBatteryMenu;
import net.valentin.obsidium.block.VolcaniteEnergyCellBlockEntity;
import net.valentin.obsidium.screen.MagmaForgeManualScreen;

import java.util.ArrayList;
import java.util.List;

public class VolcaniteBatteryScreen extends AbstractContainerScreen<VolcaniteBatteryMenu> {
    private static final String[] FACE_SHORT = { "D", "U", "N", "S", "W", "E" };

    public VolcaniteBatteryScreen(VolcaniteBatteryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 220;
        this.imageHeight = 208;
        this.inventoryLabelX = 30;
        this.inventoryLabelY = 115;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 12;
        this.titleLabelY = 8;
    }

    private static String formatCompactRf(long rf) {
        long abs = Math.abs(rf);
        String sign = rf < 0 ? "-" : "";
        if (abs >= 1_000_000_000L) return sign + String.format("%.2fG", abs / 1_000_000_000.0);
        if (abs >= 1_000_000L) return sign + String.format("%.1fM", abs / 1_000_000.0);
        if (abs >= 1_000L) return sign + String.format("%.1fk", abs / 1_000.0);
        return String.valueOf(rf);
    }

    private static String formatDurationSeconds(long secs) {
        if (secs < 60L) return secs + "s";
        if (secs < 3600L) return (secs / 60L) + "m " + (secs % 60L) + "s";
        if (secs < 86400L) return (secs / 3600L) + "h " + ((secs % 3600L) / 60L) + "m";
        return ">24h";
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
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawSlotFrame(GuiGraphics g, int sx, int sy, int borderColor) {
        g.fill(sx, sy, sx + 18, sy + 18, 0xFF0D0812);
        drawBorder(g, sx, sy, 18, 18, borderColor);
    }

    private void sendButtonClick(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null && this.minecraft.player != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
            this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.55F, 1.2F);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        int coreStatus = this.menu.getCoreStatus();
        boolean adv = this.menu.isAdvancedUnit();
        int titleColor = coreStatus > 0 ? 0xFFFFCC44 : (adv ? 0xFF00E5FF : 0xFFFFB74D);

        int maxTitlePx = coreStatus == 0 ? 116 : 176;
        int rawW = this.font.width(this.title);
        float scale = rawW > maxTitlePx ? ((float) maxTitlePx / (float) rawW) : 1.0F;
        g.pose().pushPose();
        g.pose().translate(this.titleLabelX, this.titleLabelY, 0);
        if (scale < 1.0F) {
            g.pose().scale(scale, scale, 1.0F);
        }
        g.drawString(this.font, this.title, 0, 0, titleColor, false);
        g.pose().popPose();

        g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFCECAC5, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        if (button == 0) {
            // [📖] Manual button (x+194..210, y+6..18)
            if (mouseX >= i + 194 && mouseX <= i + 210 && mouseY >= j + 6 && mouseY <= j + 18) {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new MagmaForgeManualScreen(2));
                }
                return true;
            }

            if (this.menu.getCoreStatus() == 0) {
                // Redstone Mode button (x+132..168, y+6..18)
                if (mouseX >= i + 132 && mouseX <= i + 168 && mouseY >= j + 6 && mouseY <= j + 18) {
                    sendButtonClick(1);
                    return true;
                }
                // Wireless Field Charge toggle (x+170..192, y+6..18)
                if (mouseX >= i + 170 && mouseX <= i + 192 && mouseY >= j + 6 && mouseY <= j + 18) {
                    sendButtonClick(2);
                    return true;
                }
                // 6-Face I/O tiles (x+51 + k*14, y+88..101)
                for (int k = 0; k < 6; k++) {
                    int bx = i + 51 + k * 14;
                    int by = j + 88;
                    if (mouseX >= bx && mouseX <= bx + 13 && mouseY >= by && mouseY <= by + 13) {
                        sendButtonClick(10 + k);
                        return true;
                    }
                }
                // Reset all faces button (x+137..150, y+88..101)
                if (mouseX >= i + 137 && mouseX <= i + 150 && mouseY >= j + 88 && mouseY <= j + 101) {
                    sendButtonClick(16);
                    return true;
                }
            }

            // [⚡/🔋] Toggle Charge / Drain Mode Button (x+162..208, y+27..43)
            if (mouseX >= i + 162 && mouseX <= i + 208 && mouseY >= j + 27 && mouseY <= j + 43) {
                sendButtonClick(0);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static int getFaceColorARGB(int faceMode) {
        return switch (faceMode) {
            case VolcaniteEnergyCellBlockEntity.FACE_INPUT -> 0xFF00AAFF;
            case VolcaniteEnergyCellBlockEntity.FACE_OUTPUT -> 0xFFFF8800;
            case VolcaniteEnergyCellBlockEntity.FACE_NONE -> 0xFF3A3542;
            default -> 0xFFC066FF;
        };
    }

    private static String getFaceModeShortText(int faceMode) {
        return switch (faceMode) {
            case VolcaniteEnergyCellBlockEntity.FACE_INPUT -> "§9📥 INPUT (Receive & Pull)";
            case VolcaniteEnergyCellBlockEntity.FACE_OUTPUT -> "§6📤 OUTPUT (Push Only)";
            case VolcaniteEnergyCellBlockEntity.FACE_NONE -> "§8✖ DISABLED (Isolated)";
            default -> "§d⇄ BOTH (Auto-Balance)";
        };
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        VolcaniteGuiTheme.renderExteriorFrame(g, x, y, this.imageWidth, this.imageHeight, true);

        int coreStatus = this.menu.getCoreStatus();
        boolean adv = this.menu.isAdvancedUnit();
        int outerAccent = coreStatus > 0 ? 0xFFFF8800 : (adv ? 0xFF00D8FF : 0xFFFF9800);
        int innerAccent = coreStatus > 0 ? 0xFF552200 : (adv ? 0xFF004866 : 0xFF552600);

        // 1. Main Dark-Obsidian Chassis (220x208)
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF110B18);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF191122);
        drawBorder(g, x, y, imageWidth, imageHeight, outerAccent);
        drawBorder(g, x + 1, y + 1, imageWidth - 2, imageHeight - 2, innerAccent);

        // Header Bar
        g.fill(x + 8, y + 5, x + imageWidth - 8, y + 19, 0xFF24172E);
        drawBorder(g, x + 8, y + 5, imageWidth - 16, 14, innerAccent);

        if (coreStatus == 0) {
            // Redstone Mode Button (x+132..168, y+6..18)
            int rs = this.menu.getRedstoneMode();
            boolean hoverRs = mouseX >= x + 132 && mouseX <= x + 168 && mouseY >= y + 6 && mouseY <= y + 18;
            int rsBorder = switch (rs) {
                case 1 -> 0xFFFF5555;
                case 2 -> 0xFF44AAFF;
                default -> 0xFF44DD77;
            };
            g.fill(x + 132, y + 6, x + 168, y + 18, hoverRs ? 0xFF2E223D : 0xFF150E1E);
            drawBorder(g, x + 132, y + 6, 36, 12, hoverRs ? 0xFFFFFFFF : rsBorder);
            String rsLabel = switch (rs) {
                case 1 -> "§c⚡ HI";
                case 2 -> "§b🌙 LO";
                default -> "§a☀ ON";
            };
            drawScaledString(g, rsLabel, x + 136, y + 9, 0.72f, 0xFFFFFFFF);

            // Wireless Field Charge Toggle Button (x+170..192, y+6..18)
            boolean fc = this.menu.isFieldChargeEnabled();
            boolean hoverFc = mouseX >= x + 170 && mouseX <= x + 192 && mouseY >= y + 6 && mouseY <= y + 18;
            int fcBg = fc ? (hoverFc ? 0xFF13485E : 0xFF0C2E3D) : (hoverFc ? 0xFF2E223D : 0xFF150E1E);
            int fcBorder = fc ? 0xFF00E5FF : 0xFF524463;
            g.fill(x + 170, y + 6, x + 192, y + 18, fcBg);
            drawBorder(g, x + 170, y + 6, 22, 12, hoverFc ? 0xFFFFFFFF : fcBorder);
            drawScaledString(g, fc ? "§b🛜" : "§8🛜", x + 176, y + 8, 0.8f, 0xFFFFFFFF);
        }

        // [📖] Manual Button (x+194..210, y+6..18)
        boolean hoverBook = mouseX >= x + 194 && mouseX <= x + 210 && mouseY >= y + 6 && mouseY <= y + 18;
        g.fill(x + 194, y + 6, x + 210, y + 18, hoverBook ? 0xFF4A2511 : 0xFF180F22);
        drawBorder(g, x + 194, y + 6, 16, 12, hoverBook ? 0xFFFFCC00 : 0xFF884400);
        g.drawString(this.font, "📖", x + 198, y + 8, 0xFFFFCC44, false);

        long energyAmount = this.menu.getEnergyAmount();
        long maxEnergy = Math.max(1L, this.menu.getMaxEnergy());
        float pct = Math.min(1.0f, Math.max(0.0f, (float) ((double) energyAmount / (double) maxEnergy)));

        // 2. Left Panel: Twin Plasma Energy Columns (x+8..44, y+23..111)
        g.fill(x + 8, y + 23, x + 44, y + 111, 0xFF140D1C);
        drawBorder(g, x + 8, y + 23, 36, 88, 0xFF3D2452);

        int barH = 70;
        int fillH = (int) Math.round(pct * barH);
        g.fill(x + 12, y + 27, x + 24, y + 27 + barH, 0xFF0A060E);
        g.fill(x + 28, y + 27, x + 40, y + 27 + barH, 0xFF0A060E);
        drawBorder(g, x + 12, y + 27, 12, barH, 0xFF1F4E66);
        drawBorder(g, x + 28, y + 27, 12, barH, 0xFF1F4E66);

        if (fillH > 0) {
            int topColor = (coreStatus > 0 || adv) ? 0xFF00E5FF : 0xFFFFB703;
            int botColor = (coreStatus > 0 || adv) ? 0xFF0055AA : 0xFFD9381E;
            g.fillGradient(x + 13, y + 27 + barH - fillH, x + 23, y + 27 + barH - 1, topColor, botColor);
            g.fillGradient(x + 29, y + 27 + barH - fillH, x + 39, y + 27 + barH - 1, topColor, botColor);
        }
        for (int ty = 37; ty < 94; ty += 12) {
            g.fill(x + 13, y + ty, x + 17, y + ty + 1, 0x55FFFFFF);
            g.fill(x + 29, y + ty, x + 33, y + ty + 1, 0x55FFFFFF);
        }
        String pctStr = String.format("%.0f%%", pct * 100.0f);
        drawScaledString(g, pctStr, x + 16, y + 100, 0.78f, adv || coreStatus > 0 ? 0xFF00E5FF : 0xFFFFCC44);

        // 3. Center Panel: Matrix Dashboard (x+48..154, y+23..111)
        g.fill(x + 48, y + 23, x + 154, y + 111, 0xFF140D1C);
        drawBorder(g, x + 48, y + 23, 106, 88,
                coreStatus == 2 ? 0xFF00AA88 : (coreStatus == 1 ? 0xFFAA3333 : (adv ? 0xFF006D8A : 0xFF553311)));

        if (coreStatus > 0 && energyAmount < 900_000_000L) {
            // 3x3 Energy Core Multiblock Dashboard
            boolean formed = (coreStatus == 2);
            g.fill(x + 50, y + 25, x + 152, y + 36, formed ? 0xFF112A24 : 0xFF2E1418);
            String badge = formed ? "✔ 3×3 MATRIX ONLINE" : "⚠ NEED 3×3 RING";
            drawScaledString(g, badge, x + 54, y + 28, 0.8f, formed ? 0xFF55FF99 : 0xFFFF6666);

            ItemStack coreIcon = new ItemStack(ObsidiumMod.VOLCANITE_ENERGY_CORE_BLOCK);
            ItemStack cellIcon = new ItemStack(ObsidiumMod.VOLCANITE_ENERGY_UNIT_BLOCK);
            ItemStack casingIcon = new ItemStack(ObsidiumMod.ADVANCED_VOLCANITE_CASING_BLOCK);

            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    int gx = x + 53 + c * 13;
                    int gy = y + 43 + r * 13;
                    boolean isCenter = (r == 1 && c == 1);
                    boolean isCorner = (r != 1 && c != 1);
                    int cellBorder = isCenter ? 0xFFFFCC00 : (formed ? 0xFF00E5FF : 0xFF663333);
                    g.fill(gx, gy, gx + 12, gy + 12, 0xFF0B0710);
                    drawBorder(g, gx, gy, 12, 12, cellBorder);

                    ItemStack icon = isCenter ? coreIcon : (isCorner ? casingIcon : cellIcon);
                    g.pose().pushPose();
                    g.pose().translate(gx + 1, gy + 1, 0);
                    g.pose().scale(0.625f, 0.625f, 1.0f);
                    g.renderItem(icon, 0, 0);
                    g.pose().popPose();
                }
            }

            drawScaledString(g, "STORED RF:", x + 95, y + 43, 0.72f, 0xFFAAAAAA);
            drawScaledString(g, formatCompactRf(energyAmount) + " / " + formatCompactRf(maxEnergy), x + 95, y + 51, 0.78f, 0xFFFFDD55);

            if (formed) {
                drawScaledString(g, "Basic: " + this.menu.getBasicCellsCount() + " (+25M)", x + 95, y + 64, 0.66f, 0xFF88FFAA);
                drawScaledString(g, "Adv: " + this.menu.getAdvancedCellsCount() + " (+100M)", x + 95, y + 74, 0.66f, 0xFF00E5FF);
                drawScaledString(g, "Out: 5M RF/t", x + 95, y + 88, 0.72f, 0xFFFFAA44);
            } else {
                drawScaledString(g, "Place 4+ Cells", x + 95, y + 65, 0.70f, 0xFFFF8888);
                drawScaledString(g, "& Casings around", x + 95, y + 74, 0.66f, 0xFFCCCCCC);
                drawScaledString(g, "Core (Click 📖)", x + 95, y + 84, 0.70f, 0xFFFFCC44);
            }
        } else if (coreStatus > 0) {
            // Creative Energy Core
            g.fill(x + 50, y + 25, x + 152, y + 36, 0xFF2A1438);
            drawScaledString(g, "★ CREATIVE CORE", x + 58, y + 28, 0.8f, 0xFFFF66FF);
            drawScaledString(g, "Infinite Output", x + 56, y + 50, 0.8f, 0xFF00E5FF);
            drawScaledString(g, "Rate: 10M RF/t", x + 56, y + 64, 0.76f, 0xFFFFDD55);
        } else {
            // Standalone Upgraded Cell / Advanced Unit Dashboard
            int tier = this.menu.getUpgradeTier();
            long baseRate = adv ? 150_000L : 25_000L;
            long maxRate = baseRate * (1L + tier);

            g.fill(x + 50, y + 25, x + 152, y + 36, adv ? 0xFF0E212E : 0xFF261712);
            String hdr = (adv ? "✦ ADV UNIT" : "⚡ ENERGY CELL") + (tier > 0 ? " §dT" + tier : "");
            drawScaledString(g, hdr, x + 54, y + 28, 0.76f, adv ? 0xFF00E5FF : 0xFFFFB74D);

            drawScaledString(g, "§f" + formatCompactRf(energyAmount) + " §7/ §e" + formatCompactRf(maxEnergy) + " RF", x + 52, y + 40, 0.76f, 0xFFFFFFFF);
            drawScaledString(g, "§7Max I/O: §b" + formatCompactRf(maxRate) + " RF/t", x + 52, y + 49, 0.70f, 0xFF00E5FF);

            // Live Telemetry Box (y+58..76)
            g.fill(x + 51, y + 58, x + 151, y + 76, 0xFF0C0912);
            drawBorder(g, x + 51, y + 58, 100, 18, 0xFF2F2342);

            long inRate = this.menu.getAvgInPerTick();
            long outRate = this.menu.getAvgOutPerTick();
            drawScaledString(g, "§a+" + formatCompactRf(inRate) + "  §c-" + formatCompactRf(outRate) + " §7/t", x + 54, y + 60, 0.70f, 0xFFFFFFFF);

            long net = inRate - outRate;
            String etaStr;
            if (net > 0 && energyAmount < maxEnergy) {
                etaStr = "§aFull: " + formatDurationSeconds(((maxEnergy - energyAmount) / net) / 20L);
            } else if (net < 0 && energyAmount > 0) {
                etaStr = "§6Empty: " + formatDurationSeconds((energyAmount / (-net)) / 20L);
            } else if (energyAmount >= maxEnergy) {
                etaStr = "§b100% Charged";
            } else if (energyAmount == 0) {
                etaStr = "§8Depleted";
            } else {
                etaStr = "§7Balanced (0 Δ)";
            }
            drawScaledString(g, etaStr, x + 54, y + 68, 0.68f, 0xFFCCCCCC);

            // 6-Face I/O Configurator (y+79..109)
            drawScaledString(g, "§7FACE I/O CONFIG:", x + 52, y + 79, 0.65f, 0xFFAAAAAA);
            for (int k = 0; k < 6; k++) {
                int bx = x + 51 + k * 14;
                int by = y + 88;
                int fMode = this.menu.getFaceMode(Direction.from3DDataValue(k));
                boolean hovered = mouseX >= bx && mouseX <= bx + 13 && mouseY >= by && mouseY <= by + 13;
                int fColor = getFaceColorARGB(fMode);
                g.fill(bx, by, bx + 13, by + 13, hovered ? 0xFF282038 : 0xFF0E0A14);
                drawBorder(g, bx, by, 13, 13, hovered ? 0xFFFFFFFF : fColor);
                drawScaledString(g, FACE_SHORT[k], bx + 4, by + 3, 0.78f, fColor);
            }
            boolean hoverReset = mouseX >= x + 137 && mouseX <= x + 150 && mouseY >= y + 88 && mouseY <= y + 101;
            g.fill(x + 137, y + 88, x + 150, y + 101, hoverReset ? 0xFF332645 : 0xFF171022);
            drawBorder(g, x + 137, y + 88, 13, 13, hoverReset ? 0xFFFFCC00 : 0xFF6B4F8A);
            drawScaledString(g, "⟳", x + 140, y + 91, 0.75f, 0xFFD8B4FF);

            drawScaledString(g, "§9■In  §6■Out  §d■All  §8■Off", x + 53, y + 103, 0.62f, 0xFFCCCCCC);
        }

        // 4. Right Panel: Item Charger & Upgrade Slots (x+158..212, y+23..111)
        g.fill(x + 158, y + 23, x + 212, y + 111, 0xFF140D1C);
        drawBorder(g, x + 158, y + 23, 54, 88, innerAccent);

        int mode = this.menu.getTransferMode();
        boolean hoverMode = mouseX >= x + 162 && mouseX <= x + 208 && mouseY >= y + 27 && mouseY <= y + 43;
        int btnBg = hoverMode ? (mode == 0 ? 0xFF4A2511 : 0xFF103040) : (mode == 0 ? 0xFF24142B : 0xFF14202E);
        int btnBorder = mode == 0 ? (hoverMode ? 0xFFFFCC00 : 0xFFFFAA00) : (hoverMode ? 0xFF80FFFF : 0xFF00C8FF);
        g.fill(x + 162, y + 27, x + 208, y + 43, btnBg);
        drawBorder(g, x + 162, y + 27, 46, 16, btnBorder);
        drawScaledString(g, mode == 0 ? "⚡ CHARGE" : "🔋 DRAIN", x + (mode == 0 ? 166 : 168), y + 32, 0.75f, mode == 0 ? 0xFFFFB74D : 0xFF00E5FF);

        // Slot 0 Frame (165, 56 -> Slot 166, 57)
        drawSlotFrame(g, x + 165, y + 56, mode == 0 ? 0xFFFFAA00 : 0xFF00C8FF);
        drawScaledString(g, mode == 0 ? "§6Chg" : "§bDrn", x + 167, y + 76, 0.68f, 0xFFCCCCCC);

        if (coreStatus == 0) {
            // Slot 1 Upgrade Frame (187, 56 -> Slot 188, 57)
            int upgBorder = switch (this.menu.getUpgradeTier()) {
                case 1 -> 0xFFFFAA00;
                case 2 -> 0xFF00E5FF;
                case 3 -> 0xFFFF55FF;
                default -> 0xFF5D4375;
            };
            drawSlotFrame(g, x + 187, y + 56, upgBorder);
            if (this.menu.getSlot(1).getItem().isEmpty()) {
                drawScaledString(g, "✦", x + 193, y + 61, 0.9f, 0xFF7B5799);
            }
            drawScaledString(g, "§dUpg", x + 189, y + 76, 0.68f, 0xFFCCCCCC);

            String fieldLine = this.menu.isFieldChargeEnabled()
                    ? ("§b🛜 " + (adv ? "8m" : "5m") + " ON")
                    : "§8🛜 Field Off";
            drawScaledString(g, fieldLine, x + 163, y + 96, 0.68f, 0xFFFFFFFF);
        } else {
            drawScaledString(g, mode == 0 ? "§aAuto Chg" : "§bAuto Drn", x + 167, y + 96, 0.72f, 0xFFAAAAAA);
        }

        // 5. Player Inventory & Hotbar Slot Frames (y+125, y+183)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                drawSlotFrame(g, x + 29 + col * 18, y + 125 + row * 18, 0xFF4A355A);
            }
        }
        for (int col = 0; col < 9; ++col) {
            drawSlotFrame(g, x + 29 + col * 18, y + 183, 0xFF6A457A);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            return;
        }

        // Manual button tooltip
        if (mouseX >= x + 194 && mouseX <= x + 210 && mouseY >= y + 6 && mouseY <= y + 18) {
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6📖 Obsidium Manual"),
                    Component.literal("§7Open Chapter 3: Energy Core 3×3")
            ), mouseX, mouseY);
            return;
        }

        if (this.menu.getCoreStatus() == 0) {
            // Redstone button tooltip
            if (mouseX >= x + 132 && mouseX <= x + 168 && mouseY >= y + 6 && mouseY <= y + 18) {
                int rs = this.menu.getRedstoneMode();
                String rsTitle = switch (rs) {
                    case 1 -> "§c⚡ Redstone: HIGH ONLY";
                    case 2 -> "§b🌙 Redstone: LOW ONLY";
                    default -> "§a☀ Redstone: ALWAYS ACTIVE";
                };
                g.renderComponentTooltip(this.font, List.of(
                        Component.literal(rsTitle),
                        Component.literal("§7Click to cycle Redstone control mode.")
                ), mouseX, mouseY);
                return;
            }

            // Wireless Field Charge tooltip
            if (mouseX >= x + 170 && mouseX <= x + 192 && mouseY >= y + 6 && mouseY <= y + 18) {
                boolean fc = this.menu.isFieldChargeEnabled();
                int r = this.menu.isAdvancedUnit() ? 8 : 5;
                g.renderComponentTooltip(this.font, List.of(
                        Component.literal("§b🛜 Wireless Field Charge: " + (fc ? "§aENABLED" : "§cDISABLED")),
                        Component.literal("§7Charges tools & batteries inside nearby players'"),
                        Component.literal("§7inventories within a §e" + r + "-block §7radius."),
                        Component.literal("§eClick to toggle.")
                ), mouseX, mouseY);
                return;
            }

            // 6-Face I/O tooltips
            for (int k = 0; k < 6; k++) {
                int bx = x + 51 + k * 14;
                int by = y + 88;
                if (mouseX >= bx && mouseX <= bx + 13 && mouseY >= by && mouseY <= by + 13) {
                    Direction d = Direction.from3DDataValue(k);
                    int fMode = this.menu.getFaceMode(d);
                    g.renderComponentTooltip(this.font, List.of(
                            Component.literal("§eFace: §f" + d.getName().toUpperCase()),
                            Component.literal(getFaceModeShortText(fMode)),
                            Component.literal("§7Smart BOTH auto-balances adjacent cells w/o ping-pong."),
                            Component.literal("§eClick to cycle I/O mode.")
                    ), mouseX, mouseY);
                    return;
                }
            }

            // Reset faces tooltip
            if (mouseX >= x + 137 && mouseX <= x + 150 && mouseY >= y + 88 && mouseY <= y + 101) {
                g.renderComponentTooltip(this.font, List.of(
                        Component.literal("§d⟳ Reset All 6 Faces"),
                        Component.literal("§7Sets all faces to §dBOTH (Auto-Balance)§7.")
                ), mouseX, mouseY);
                return;
            }

            // Upgrade slot empty tooltip
            if (mouseX >= x + 187 && mouseX <= x + 205 && mouseY >= y + 56 && mouseY <= y + 74) {
                g.renderComponentTooltip(this.font, List.of(
                        Component.literal("§d✦ Cell Upgrade Slot"),
                        Component.literal("§7Accepts §6Energy§7, §bOverclock§7, or §dStack Upgrades§7:"),
                        Component.literal("§e• T1: §a+25% Cap, 2× Transfer"),
                        Component.literal("§b• T2: §a+50% Cap, 3× Transfer"),
                        Component.literal("§d• T3 / Stack: §a+100% Cap, 4× Transfer")
                ), mouseX, mouseY);
                return;
            }
        }

        // Mode toggle button tooltip
        if (mouseX >= x + 162 && mouseX <= x + 208 && mouseY >= y + 27 && mouseY <= y + 43) {
            int mode = this.menu.getTransferMode();
            List<Component> tooltip = new ArrayList<>();
            if (mode == 0) {
                tooltip.add(Component.literal("§6⚡ Mode: Charge Item"));
                tooltip.add(Component.literal("§7Pushes energy from buffer into item in left slot."));
            } else {
                tooltip.add(Component.literal("§b🔋 Mode: Drain / Discharge Item"));
                tooltip.add(Component.literal("§7Pulls energy from item into buffer."));
            }
            tooltip.add(Component.literal("§eClick to toggle mode."));
            g.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
            return;
        }

        // Energy Column Tooltip
        if (mouseX >= x + 8 && mouseX <= x + 44 && mouseY >= y + 23 && mouseY <= y + 111) {
            long energyAmount = this.menu.getEnergyAmount();
            long maxEnergy = Math.max(1L, this.menu.getMaxEnergy());
            float pct = (float) ((double) energyAmount / (double) maxEnergy * 100.0);
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("§6⚡ Energy Matrix Buffer"));
            tooltip.add(Component.literal("§e" + String.format("%,d", energyAmount) + " / " + String.format("%,d", maxEnergy) + " RF §7(" + String.format("%.1f%%", pct) + ")"));
            if (this.menu.getCoreStatus() == 2) {
                tooltip.add(Component.literal("§a✔ 3×3 Multiblock Active (Max 5M RF/t)"));
            }
            g.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }
}
