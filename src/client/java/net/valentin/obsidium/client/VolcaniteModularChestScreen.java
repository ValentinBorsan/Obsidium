package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.block.VolcaniteModularChestMenu;

import java.util.List;

public class VolcaniteModularChestScreen extends AbstractContainerScreen<VolcaniteModularChestMenu> {

    public VolcaniteModularChestScreen(VolcaniteModularChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 206;
        this.imageHeight = 222;
        this.inventoryLabelY = 128;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        VolcaniteGuiTheme.renderExteriorFrame(guiGraphics, x, y, this.imageWidth, this.imageHeight, true);

        // Dark base background
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF140F1A);

        // Storage grid panel (54 slots per page)
        guiGraphics.fill(x + 6, y + 16, x + 172, y + 126, 0xFF1C1624);
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 9; c++) {
                int sx = x + 7 + c * 18;
                int sy = y + 17 + r * 18;
                guiGraphics.fill(sx, sy, sx + 18, sy + 18, 0xFF0D0A12);
                guiGraphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF221A2C);
            }
        }

        // Top toolbar buttons (Sort Quantity, Sort A-Z, Quick Stack)
        boolean desc = this.menu.isQuantitySortDescending();
        drawMiniButton(guiGraphics, x + 104, y + 4, 22, 11, desc ? "#↓" : "#↑", 0xFFFFAA00, mouseX, mouseY);
        drawMiniButton(guiGraphics, x + 128, y + 4, 22, 11, "A-Z", 0xFF00CED1, mouseX, mouseY);
        drawMiniButton(guiGraphics, x + 152, y + 4, 18, 11, "⬇", 0xFF66FF88, mouseX, mouseY);

        // Page navigation controls (between storage grid and player inventory: x + 98..170, y + 127..137)
        int curPage = this.menu.getCurrentPage() + 1;
        int maxPages = this.menu.getUnlockedPages();
        drawMiniButton(guiGraphics, x + 98, y + 127, 14, 10, "◀", curPage > 1 ? 0xFFFFCC44 : 0xFF555566, mouseX, mouseY);
        String pageLabel = curPage + "/" + maxPages;
        int pw = this.font.width(pageLabel);
        guiGraphics.drawString(this.font, pageLabel, x + 134 - pw / 2, y + 128, maxPages > 1 ? 0xFFFFCC44 : 0xFF9999AA, false);
        drawMiniButton(guiGraphics, x + 156, y + 127, 14, 10, "▶", curPage < maxPages ? 0xFFFFCC44 : 0xFF555566, mouseX, mouseY);

        // Universal Flex Module sidebar background (x: 174..200, y: 16..126)
        guiGraphics.fill(x + 174, y + 16, x + 200, y + 126, 0xFF1A1322);
        guiGraphics.fill(x + 174, y + 16, x + 175, y + 126, 0xFFDCA220); // Golden divider line

        // 4 Universal Flex Module Bays
        int[] bayColors = {0xFFD87093, 0xFF00CED1, 0xFFFF8C00, 0xFF9370DB};
        String[] bayIcons = {"✦", "✦", "✦", "✦"};
        for (int m = 0; m < 4; m++) {
            boolean hasMod = this.menu.getSlot(VolcaniteModularChestMenu.MENU_MOD_SLOT_START + m).hasItem();
            drawModuleBay(guiGraphics, x + 177, y + 19 + m * 24, bayColors[m], bayIcons[m], hasMod);
        }

        // Player Inventory Background
        guiGraphics.fill(x + 6, y + 138, x + 172, y + 218, 0xFF1C1624);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                int sx = x + 7 + c * 18;
                int sy = y + 139 + r * 18;
                guiGraphics.fill(sx, sy, sx + 18, sy + 18, 0xFF0D0A12);
                guiGraphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF201A28);
            }
        }
        for (int c = 0; c < 9; c++) {
            int sx = x + 7 + c * 18;
            int sy = y + 197;
            guiGraphics.fill(sx, sy, sx + 18, sy + 18, 0xFF0D0A12);
            guiGraphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF261E30);
        }
    }

    private void drawMiniButton(GuiGraphics g, int bx, int by, int bw, int bh, String text, int borderColor, int mouseX, int mouseY) {
        boolean hovered = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh;
        g.fill(bx, by, bx + bw, by + bh, hovered ? 0xFFFFFFFF : borderColor);
        g.fill(bx + 1, by + 1, bx + bw - 1, by + bh - 1, hovered ? 0xFF322642 : 0xFF1B1426);
        int tw = this.font.width(text);
        g.drawString(this.font, text, bx + (bw - tw) / 2, by + (bh - 7) / 2, hovered ? 0xFFFFFFFF : borderColor, false);
    }

    private void drawModuleBay(GuiGraphics g, int bx, int by, int color, String icon, boolean installed) {
        g.fill(bx, by, bx + 18, by + 18, installed ? color : 0xFF352B42);
        g.fill(bx + 1, by + 1, bx + 17, by + 17, 0xFF100C16);
        if (!installed) {
            g.drawString(this.font, icon, bx + 6, by + 5, color, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        if (this.minecraft != null && this.minecraft.gameMode != null) {
            // Sort by Quantity button (x + 104..126, y + 4..15) -> Left click = Descending (#↓) / toggle, Right click = Ascending (#↑)
            if (mouseX >= x + 104 && mouseX < x + 126 && mouseY >= y + 4 && mouseY < y + 15) {
                int actionId;
                if (button == 1) {
                    actionId = VolcaniteModularChestMenu.BTN_SORT_QUANTITY_ASC;
                } else {
                    actionId = this.menu.isQuantitySortDescending() && hasShiftDown()
                            ? VolcaniteModularChestMenu.BTN_SORT_QUANTITY_ASC
                            : VolcaniteModularChestMenu.BTN_SORT_QUANTITY_DESC;
                }
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, actionId);
                return true;
            }

            // Sort Alphabetically A-Z (x + 128..150, y + 4..15)
            if (mouseX >= x + 128 && mouseX < x + 150 && mouseY >= y + 4 && mouseY < y + 15) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, VolcaniteModularChestMenu.BTN_SORT_ALPHA);
                return true;
            }

            // Quick Stack (x + 152..170, y + 4..15)
            if (mouseX >= x + 152 && mouseX < x + 170 && mouseY >= y + 4 && mouseY < y + 15) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, VolcaniteModularChestMenu.BTN_QUICK_STACK);
                return true;
            }

            // Prev Page (x + 98..112, y + 127..137)
            if (mouseX >= x + 98 && mouseX < x + 112 && mouseY >= y + 127 && mouseY < y + 137) {
                int cur = this.menu.getCurrentPage();
                if (cur > 0) {
                    this.menu.setCurrentPageClient(cur - 1);
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, VolcaniteModularChestMenu.BTN_PREV_PAGE);
                }
                return true;
            }

            // Next Page (x + 156..170, y + 127..137)
            if (mouseX >= x + 156 && mouseX < x + 170 && mouseY >= y + 127 && mouseY < y + 137) {
                int cur = this.menu.getCurrentPage();
                if (cur + 1 < this.menu.getUnlockedPages()) {
                    this.menu.setCurrentPageClient(cur + 1);
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, VolcaniteModularChestMenu.BTN_NEXT_PAGE);
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, "Modular Chest", 8, 6, 0xFFD166, false);
        guiGraphics.drawString(this.font, "MODS", 175, 6, 0x00CED1, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xCECAC5, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Tooltips for top buttons
        if (mouseY >= y + 4 && mouseY < y + 15) {
            if (mouseX >= x + 104 && mouseX < x + 126) {
                guiGraphics.renderComponentTooltip(this.font, List.of(
                        Component.literal("§6📊 Sort by Quantity (Cantitate)"),
                        Component.literal("§7• §eLeft-Click: §fHighest Quantity First (#↓)"),
                        Component.literal("§7• §eRight-Click / Shift-Click: §fLowest Quantity First (#↑)"),
                        Component.literal("§8Automatically merges partial stacks across all pages.")
                ), mouseX, mouseY);
            } else if (mouseX >= x + 128 && mouseX < x + 150) {
                guiGraphics.renderComponentTooltip(this.font, List.of(
                        Component.literal("§b🔤 Sort Alphabetically (A–Z)"),
                        Component.literal("§7Merges partial stacks and arranges items by name.")
                ), mouseX, mouseY);
            } else if (mouseX >= x + 152 && mouseX < x + 170) {
                guiGraphics.renderComponentTooltip(this.font, List.of(
                        Component.literal("§a⬇ Quick Stack Matching Items"),
                        Component.literal("§7Deposits items from your inventory that already"),
                        Component.literal("§7exist inside this Modular Chest.")
                ), mouseX, mouseY);
            }
        }

        // Tooltip for Page Navigation
        if (mouseY >= y + 127 && mouseY < y + 137 && mouseX >= x + 98 && mouseX < x + 170) {
            int unlocked = this.menu.getUnlockedPages();
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§e📦 Storage Pages: §f" + (this.menu.getCurrentPage() + 1) + " / " + unlocked + " §7(" + (unlocked * 54) + " slots)"),
                    Component.literal("§7Install §dStack / Storage / Barrel Upgrades §7in the"),
                    Component.literal("§7Module Bays to unlock up to §64 Pages (216 slots)§7!")
            ), mouseX, mouseY);
        }

        // Tooltips for empty Universal Flex Module sockets
        if (mouseX >= x + 177 && mouseX <= x + 195) {
            for (int m = 0; m < 4; m++) {
                int by = y + 19 + m * 24;
                if (mouseY >= by && mouseY <= by + 18 && !this.menu.getSlot(VolcaniteModularChestMenu.MENU_MOD_SLOT_START + m).hasItem()) {
                    guiGraphics.renderComponentTooltip(this.font, List.of(
                            Component.literal("§6✦ Universal Flex Module Bay #" + (m + 1)),
                            Component.literal("§7Combine any of these modules in any slot:"),
                            Component.literal("  §d• Stack / Storage / Barrel Upgrade: §7+54 to +162 slots (Pages 2–4)"),
                            Component.literal("  §6• Hermes / Advanced Filter (9-Slot): §7Whitelist/Blacklist chest filter"),
                            Component.literal("  §b• Vacuum Module: §7Absorbs nearby items (stacks range)"),
                            Component.literal("  §6• Compactor / Precision Compactor: §7Auto-crafts 3×3 & 2×2 items"),
                            Component.literal("  §5• Void / Selective Void Upgrade: §7Voids overflow or chosen items"),
                            Component.literal("  §a• Logistics Module: §7Auto-pulls outputs from machine above"),
                            Component.literal("  §e• Overclock Upgrade: §72×/4×/8× module speed & range")
                    ), mouseX, mouseY);
                }
            }
        }
    }
}
