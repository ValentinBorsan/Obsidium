package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.block.AdvancedDionysianPressBlockEntity;
import net.valentin.obsidium.block.AdvancedDionysianPressMenu;

import java.util.List;

public class AdvancedDionysianPressScreen extends AbstractContainerScreen<AdvancedDionysianPressMenu> {

    public AdvancedDionysianPressScreen(AdvancedDionysianPressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.titleLabelX = 1000;
        this.inventoryLabelX = 1000;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Outer Wine-Obsidian Chassis
        g.fill(x - 1, y - 1, x + this.imageWidth + 1, y + this.imageHeight + 1, 0xFF07030A);
        g.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF140914);

        g.fill(x, y, x + this.imageWidth, y + 1, 0xFFD97706);
        g.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF581C3C);
        g.fill(x, y, x + 1, y + this.imageHeight, 0xFFD97706);
        g.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, 0xFF581C3C);

        // Header bar
        g.fill(x + 1, y + 1, x + this.imageWidth - 1, y + 16, 0xFF210E1E);
        g.fill(x + 1, y + 16, x + this.imageWidth - 1, y + 17, 0xFF6B1E48);

        // Chamber panel
        g.fill(x + 7, y + 19, x + 169, y + 89, 0xFF1B0C18);
        g.renderOutline(x + 7, y + 19, 162, 70, 0xFF4A1838);

        // 1. High-Power RF Bar (x+11, y+22, w=14, h=60)
        long energy = this.menu.getEnergy();
        long maxEnergy = this.menu.getMaxEnergy();
        int energyH = (int) (58.0 * Math.min(1.0, (double) energy / (double) maxEnergy));
        g.fill(x + 11, y + 22, x + 25, y + 82, 0xFF0A0308);
        if (energyH > 0) {
            g.fill(x + 12, y + 81 - energyH, x + 24, y + 81, 0xFFE11D48);
            g.fill(x + 12, y + 81 - energyH, x + 24, Math.min(y + 81, y + 83 - energyH), 0xFFFDA4AF);
        }
        g.renderOutline(x + 11, y + 22, 14, 60, 0xFF6B1E48);

        // 2. Three Parallel Pressing Lanes
        int maxProg = this.menu.getMaxProgress();
        for (int lane = 0; lane < AdvancedDionysianPressBlockEntity.LANES; lane++) {
            int sy = y + 22 + lane * 22;

            // Fruit Input Frame (40, 22 + lane*22)
            drawSlotFrame(g, x + 40, sy, 0xFF22C55E, 0xFF0B050B);

            // Mini Horizontal Hydraulic Press Bar (x+62..100, width 38, height 14)
            int prog = this.menu.getLaneProgress(lane);
            int fillW = (prog > 0) ? Math.max(2, (36 * prog) / maxProg) : 0;
            g.fill(x + 62, sy + 1, x + 100, sy + 15, 0xFF0D050B);
            if (fillW > 0) {
                g.fill(x + 63, sy + 2, x + 63 + fillW, sy + 14, 0xFFF59E0B);
                g.fill(x + 63, sy + 2, x + 63 + fillW, sy + 4, 0xFFFDE047);
            }
            g.renderOutline(x + 62, sy + 1, 38, 14, prog > 0 ? 0xFFFBBF24 : 0xFF4A2038);

            // Mineral Output Frame (108, 22 + lane*22)
            drawSlotFrame(g, x + 108, sy, 0xFFFACC15, 0xFF0B050B);

            // Pomace collector pipe line from each mineral lane into shared Pomace slot (x=142, y=44)
            g.fill(x + 127, sy + 7, x + 134, sy + 9, prog > 0 ? 0xFF65A30D : 0xFF3A1C2E);
        }

        // Vertical Pomace collector manifold connecting lanes 0..2 to Slot 6 (x=142, y=44)
        g.fill(x + 133, y + 29, x + 135, y + 75, 0xFF4A2038);
        g.fill(x + 135, y + 51, x + 141, y + 53, 0xFF65A30D);

        // Shared Pomace Output Slot (x=142, y=44)
        drawSlotFrame(g, x + 142, y + 44, 0xFF84CC16, 0xFF0B050B);

        // Player Inventory (y = 104) & Hotbar (y = 162)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                drawSlotFrame(g, x + 8 + col * 18, y + 104 + row * 18, 0xFF4A2044, 0xFF0A040B);
            }
        }
        for (int col = 0; col < 9; ++col) {
            drawSlotFrame(g, x + 8 + col * 18, y + 162, 0xFF6A2E58, 0xFF0A040B);
        }
    }

    private static void drawSlotFrame(GuiGraphics g, int sx, int sy, int borderColor, int innerColor) {
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, borderColor);
        g.fill(sx, sy, sx + 16, sy + 16, innerColor);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, "§6🍇 §fAdv. Press", 8, 5, 0xFFFFFF, false);

        int active = this.menu.getActiveLanes();
        int rfDraw = active * AdvancedDionysianPressBlockEntity.RF_PER_TICK_PER_LANE;
        String drawStr = rfDraw > 0 ? ("§e-" + String.format("%,d", rfDraw) + " RF/t") : "§7Idle (0/3)";
        int w = this.font.width(drawStr);
        g.drawString(this.font, drawStr, 168 - w, 5, 0xFFFFFF, false);

        g.drawString(this.font, "§8INVENTORY", 8, 93, 0x94A3B8, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);

        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            return;
        }

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        if (mouseX >= x + 11 && mouseX <= x + 25 && mouseY >= y + 22 && mouseY <= y + 82) {
            int active = this.menu.getActiveLanes();
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("§c⚡ Triple-Press Energy Buffer"),
                    Component.literal(String.format("§f%,d / %,d RF", this.menu.getEnergy(), this.menu.getMaxEnergy())),
                    Component.literal("§7Consumption: §e600 RF/t × active lane §7(max 1,800 RF/t)"),
                    Component.literal("§7Active Lanes: §a" + active + " / 3")
            ), mouseX, mouseY);
        } else if (mouseX >= x + 141 && mouseX <= x + 159 && mouseY >= y + 43 && mouseY <= y + 61) {
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("§2🌿 Shared Pomace Collector"),
                    Component.literal("§7Collects Dionysian Pomace from all 3 lanes.")
            ), mouseX, mouseY);
        } else {
            for (int lane = 0; lane < AdvancedDionysianPressBlockEntity.LANES; lane++) {
                int sy = y + 22 + lane * 22;
                if (mouseX >= x + 39 && mouseX <= x + 57 && mouseY >= sy - 1 && mouseY <= sy + 17) {
                    g.renderComponentTooltip(this.font, List.of(
                            Component.literal("§a🍏 Fruit Lane #" + (lane + 1)),
                            Component.literal("§7Parallel high-speed crushing (1.25s per fruit).")
                    ), mouseX, mouseY);
                    break;
                }
                if (mouseX >= x + 107 && mouseX <= x + 125 && mouseY >= sy - 1 && mouseY <= sy + 17) {
                    g.renderComponentTooltip(this.font, List.of(
                            Component.literal("§e✨ Mineral Output #" + (lane + 1)),
                            Component.literal("§7Outputs extracted nuggets from Fruit Lane #" + (lane + 1) + ".")
                    ), mouseX, mouseY);
                    break;
                }
            }
        }
    }
}
