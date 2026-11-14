package net.valentin.obsidium.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.block.VolcaniteFurnaceMenu;

import java.util.List;

public class VolcaniteFurnaceScreen extends AbstractContainerScreen<VolcaniteFurnaceMenu> {

    public VolcaniteFurnaceScreen(VolcaniteFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 73;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, "🔥 Volcanite Furnace", this.titleLabelX, this.titleLabelY, 0xFFFFB74D, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFCECAC5, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        if (button == 0 && VolcaniteGuiTheme.handleMuteClick(i, j, this.imageWidth, mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        VolcaniteGuiTheme.renderMuteTooltip(this.font, guiGraphics, i, j, this.imageWidth, mouseX, mouseY);

        // Energy / Fuel Tank Tooltip (10, 18, 12x50)
        if (mouseX >= i + 10 && mouseX <= i + 22 && mouseY >= j + 18 && mouseY <= j + 68) {
            int burnSeconds = this.menu.getBurnTime() / 20;
            int rf = this.menu.getEnergyAmount();
            boolean thermal = this.menu.isThermal();
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6🔥 Hybrid Thermal Buffer"),
                    Component.literal("§bRF Energy: §f" + String.format("%,d / 50,000 RF", rf) + " §7(80 RF/t)"),
                    Component.literal("§eSolid Fuel: §f" + burnSeconds + "s remaining"),
                    thermal ? Component.literal("§c★ Geothermal Heat: +100% Speed (Free!)") : Component.literal("§7Place next to Lava/Magma for free heat")
            ), mouseX, mouseY);
        }

        // Hearth Tooltip (70, 22, 34x44)
        if (mouseX >= i + 70 && mouseX <= i + 104 && mouseY >= j + 22 && mouseY <= j + 66) {
            int prog = this.menu.getProgress();
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6🔥 Volcanic Blast Hearth"),
                    Component.literal("§eProgress: §f" + prog + "% §7(100 ticks base)"),
                    this.menu.isBurning() ? Component.literal("§aCombustion Active") : Component.literal("§7Idle")
            ), mouseX, mouseY);
        }

        // Upgrade Slots Tooltip (147, 21, 18x46)
        if (mouseX >= i + 147 && mouseX <= i + 165 && mouseY >= j + 21 && mouseY <= j + 67) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§b⚡ Upgrade Slots"),
                    Component.literal("§7Accepts §ePipe Energy §7& §bOverclock Upgrades")
            ), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        long now = System.currentTimeMillis();

        VolcaniteGuiTheme.renderWindow(g, i, j, this.imageWidth, this.imageHeight, false);
        VolcaniteGuiTheme.renderRecessedPanel(g, i + 7, j + 16, 162, 55);
        VolcaniteGuiTheme.renderPlayerInventory(g, i + 8, j + 84);

        boolean burning = this.menu.isBurning();
        int progress = this.menu.getProgress();

        // 1. Left Hybrid Energy/Lava Tank (10, 18, 12x50)
        int rf = this.menu.getEnergyAmount();
        float rfRatio = Math.min(1.0f, rf / 50000.0f);
        int rfH = (int) (rfRatio * 48);
        g.fill(i + 10, j + 18, i + 22, j + 68, 0xFF09060E);
        if (rfH > 0) {
            g.fillGradient(i + 11, j + 67 - rfH, i + 21, j + 67, 0xFFF97316, 0xFFB91C1C);
            if (burning) {
                int bubY = j + 66 - (int) ((now / 45) % Math.max(1, rfH));
                g.fill(i + 13, bubY, i + 19, bubY + 1, 0xFFFDE047);
            }
        }
        g.renderOutline(i + 10, j + 18, 12, 50, 0xFF991B1B);

        // 2. Burn Time Bar (26, 18, 10x50)
        int totalBurn = Math.max(1, this.menu.getTotalBurnTime());
        int burn = this.menu.getBurnTime();
        int burnH = this.menu.isThermal() ? 48 : Math.min(48, (int) ((float) burn / totalBurn * 48));
        g.fill(i + 26, j + 18, i + 36, j + 68, 0xFF09060E);
        if (burnH > 0) {
            g.fillGradient(i + 27, j + 67 - burnH, i + 35, j + 67, 0xFFFDE047, 0xFFEA580C);
        }
        g.renderOutline(i + 26, j + 18, 10, 50, this.menu.isThermal() ? 0xFFFFEA79 : 0xFF7C2D12);

        // 3. Machine Slots
        VolcaniteGuiTheme.renderSlot(g, i + 48, j + 22, 0xFFFF9900);  // Input
        VolcaniteGuiTheme.renderSlot(g, i + 48, j + 52, 0xFFEA580C);  // Fuel
        VolcaniteGuiTheme.renderSlot(g, i + 112, j + 24, 0xFFFFD54F); // Alloy Out
        VolcaniteGuiTheme.renderSlot(g, i + 112, j + 48, 0xFF71717A); // Ash Out
        VolcaniteGuiTheme.renderUpgradeSlot(g, i + 147, j + 21, this.menu.getSlot(4).hasItem());
        VolcaniteGuiTheme.renderUpgradeSlot(g, i + 147, j + 47, this.menu.getSlot(5).hasItem());

        // 4. Animated Flame between Input & Fuel (52, 40)
        if (burning) {
            int flameH = 4 + (int) ((Math.sin(now / 95.0) + 1.0) * 3.0);
            g.fill(i + 52, j + 50 - flameH, i + 60, j + 50, 0xFFEA580C);
            g.fill(i + 54, j + 50 - Math.max(2, flameH - 2), i + 58, j + 50, 0xFFFDE047);
        }

        // 5. Central Blast Hearth & Molten Pour Progress (70, 22, 34x44)
        g.fill(i + 70, j + 22, i + 104, j + 66, 0xFF0A0710);
        g.renderOutline(i + 70, j + 22, 34, 44, burning ? 0xFFEA580C : 0xFF332645);

        // Glowing ember bed at bottom of hearth
        if (burning) {
            float glow = (float) ((Math.sin(now / 150.0) + 1.0) * 0.5);
            int emberCol = glow > 0.5f ? 0xFFF97316 : 0xFFDC2626;
            g.fill(i + 73, j + 58, i + 101, j + 63, emberCol);
            // Rising sparks
            for (int s = 0; s < 3; s++) {
                int sx = i + 76 + ((s * 9 + (int) (now / 140)) % 22);
                int sy = j + 58 - (int) (((now / 40) + s * 11) % 24);
                g.fill(sx, sy, sx + 2, sy + 2, 0xFFFDE047);
            }
        }

        // Progress Bar across center of hearth (73, 38, 28x8)
        g.fill(i + 73, j + 38, i + 101, j + 46, 0xFF140E1C);
        g.renderOutline(i + 73, j + 38, 28, 8, 0xFF4B3863);
        int progW = Math.min(26, (int) (26.0f * progress / 100.0f));
        if (progW > 0) {
            g.fillGradient(i + 74, j + 39, i + 74 + progW, j + 45, 0xFFFDE047, 0xFFEA580C);
            int pulseX = (int) ((now / 45) % Math.max(1, progW));
            g.fill(i + 74 + pulseX, j + 39, Math.min(i + 74 + progW, i + 76 + pulseX), j + 45, 0xFFFFFFFF);
        }

        // Geothermal indicator badge
        if (this.menu.isThermal()) {
            g.drawString(this.font, "GEO", i + 79, j + 26, 0xFFFFEA79, false);
        }
    }
}
