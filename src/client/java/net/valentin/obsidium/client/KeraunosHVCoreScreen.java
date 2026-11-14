package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.block.KeraunosHVCoreMenu;

import java.util.List;

public class KeraunosHVCoreScreen extends AbstractContainerScreen<KeraunosHVCoreMenu> {

    public KeraunosHVCoreScreen(KeraunosHVCoreMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 190;
        this.titleLabelX = 1000;
        this.inventoryLabelX = 1000;
    }

    private static String formatEnergyShort(long rf) {
        if (rf >= 1_000_000_000L) return String.format("%.2fB", rf / 1_000_000_000.0);
        if (rf >= 1_000_000L) return String.format("%.2fM", rf / 1_000_000.0);
        if (rf >= 1_000L) return String.format("%.1fk", rf / 1_000.0);
        return String.valueOf(rf);
    }

    private void drawScaledString(GuiGraphics g, String text, int x, int y, float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0f);
        g.drawString(this.font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        boolean formed = this.menu.isFormed();
        boolean grounded = this.menu.isServiceMode();
        boolean overdrive = this.menu.isOvervoltageMode();
        int kv = this.menu.getVoltageKv();

        // Outer shadow & obsidian-steel substation chassis
        g.fill(x - 1, y - 1, x + this.imageWidth + 1, y + this.imageHeight + 1, 0xFF04060A);
        g.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF0C101B);

        // Beveled HV Perimeter Frame
        int accentTop = !formed ? 0xFFEF4444 : (grounded ? 0xFF10B981 : (overdrive ? 0xFFF59E0B : 0xFF38BDF8));
        g.fill(x, y, x + this.imageWidth, y + 1, accentTop);
        g.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF1E293B);
        g.fill(x, y, x + 1, y + this.imageHeight, accentTop);
        g.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, 0xFF1E293B);

        // Corner rivets
        g.fill(x + 2, y + 2, x + 4, y + 4, accentTop);
        g.fill(x + this.imageWidth - 4, y + 2, x + this.imageWidth - 2, y + 4, accentTop);

        // Header Bar
        g.fill(x + 1, y + 1, x + this.imageWidth - 1, y + 17, 0xFF141B2D);
        g.fill(x + 1, y + 17, x + this.imageWidth - 1, y + 18, 0xFF24324E);

        // Main Telemetry Backdrop
        g.fill(x + 7, y + 20, x + 169, y + 94, 0xFF101726);
        g.renderOutline(x + 7, y + 20, 162, 74, 0xFF22304C);

        // --- 1. Status & Output Card (x+10, y+23, w=111, h=23) ---
        int statusBorder = !formed ? 0xFFEF4444 : (grounded ? 0xFF10B981 : (overdrive ? 0xFFFACC15 : 0xFF38BDF8));
        g.fill(x + 10, y + 23, x + 121, y + 46, 0xFF080C14);
        g.renderOutline(x + 10, y + 23, 111, 23, statusBorder);

        String line1 = !formed
                ? "§c✖ UNFORMED (3×3×3)"
                : (grounded ? "§a⏚ GROUNDED (0 kV)" : ("§b● " + kv + " kV" + (overdrive ? " §6[OD]" : " §7[STD]")));
        String line2 = !formed
                ? ("§7Bad: §f" + this.menu.getInvalidBlocksCount() + " §7| Ports: §f" + this.menu.getPortsCount())
                : ("§7Ports: §f" + this.menu.getPortsCount() + " §7| §e" + formatEnergyShort(this.menu.getLastThroughputRft()) + "/t");
        drawScaledString(g, line1, x + 14, y + 26, 0.90f, 0xFFFFFF);
        drawScaledString(g, line2, x + 14, y + 36, 0.88f, 0xFFFFFF);

        // --- 2. Stored Capacity & Shell Card (x+10, y+49, w=111, h=22) ---
        g.fill(x + 10, y + 49, x + 121, y + 71, 0xFF080C14);
        g.renderOutline(x + 10, y + 49, 111, 22, 0xFF1E293B);

        long energy = this.menu.getEnergy();
        long maxEnergy = this.menu.getMaxEnergy();
        String capLine1 = "§f" + formatEnergyShort(energy) + " §7/ §b" + formatEnergyShort(maxEnergy) + " RF";
        String capLine2 = "§7Cells: §f" + this.menu.getBasicCellsCount() + " §7| Units: §d" + this.menu.getAdvancedUnitsCount();
        drawScaledString(g, capLine1, x + 14, y + 52, 0.88f, 0xFFFFFF);
        drawScaledString(g, capLine2, x + 14, y + 61, 0.85f, 0xFFFFFF);

        // --- 3. Two Spacious Control Buttons (y+74..y+90, 54x16 each) ---
        boolean hoverGround = mouseX >= x + 10 && mouseX <= x + 64 && mouseY >= y + 74 && mouseY <= y + 90;
        g.fill(x + 10, y + 74, x + 64, y + 90, grounded ? 0xFF065F46 : 0xFF162033);
        g.renderOutline(x + 10, y + 74, 54, 16, hoverGround ? 0xFFFFFFFF : (grounded ? 0xFF34D399 : 0xFF475569));
        String gLabel = grounded ? "§a⏚ Safe" : "§e⏚ Ground";
        g.drawString(this.font, gLabel, x + 10 + (54 - this.font.width(gLabel)) / 2, y + 78, 0xFFFFFF, false);

        boolean hoverOD = mouseX >= x + 67 && mouseX <= x + 121 && mouseY >= y + 74 && mouseY <= y + 90;
        g.fill(x + 67, y + 74, x + 121, y + 90, overdrive ? 0xFF78350F : 0xFF162033);
        g.renderOutline(x + 67, y + 74, 54, 16, hoverOD ? 0xFFFFFFFF : (overdrive ? 0xFFFACC15 : 0xFF475569));
        String odLabel = overdrive ? "§62000 kV" : "§71000 kV";
        g.drawString(this.font, odLabel, x + 67 + (54 - this.font.width(odLabel)) / 2, y + 78, 0xFFFFFF, false);

        // --- 4. Supercharge Control & Slot (x+124..148) ---
        int tMode = this.menu.getTransferMode();
        boolean hoverTransfer = mouseX >= x + 124 && mouseX <= x + 148 && mouseY >= y + 23 && mouseY <= y + 39;
        g.fill(x + 124, y + 23, x + 148, y + 39, 0xFF080C14);
        g.renderOutline(x + 124, y + 23, 24, 16, hoverTransfer ? 0xFFFFFFFF : (tMode == 0 ? 0xFF38BDF8 : 0xFFF59E0B));
        String tLabel = tMode == 0 ? "§bCHG" : "§6DSC";
        g.drawString(this.font, tLabel, x + 124 + (24 - this.font.width(tLabel)) / 2, y + 27, 0xFFFFFF, false);

        // Slot frame aligned cleanly to Menu slot (128, 45)
        drawSlotFrame(g, x + 128, y + 45, tMode == 0 ? 0xFF38BDF8 : 0xFFF59E0B, 0xFF080C14);

        // Rate label under slot
        drawScaledString(g, "§810M/t", x + 126, y + 66, 0.80f, 0x94A3B8);

        // --- 5. Tall HV Dielectric Capacitor Gauge (x+151, y+23, w=15, h=67) ---
        int barH = (maxEnergy > 0) ? (int) (65.0 * Math.min(1.0, (double) energy / (double) maxEnergy)) : 0;
        g.fill(x + 151, y + 23, x + 166, y + 90, 0xFF080C14);
        if (barH > 0) {
            int barColor = grounded ? 0xFF10B981 : (overdrive ? 0xFFFACC15 : 0xFF38BDF8);
            g.fill(x + 152, y + 89 - barH, x + 165, y + 89, barColor);
        }
        // Gauge measurement ticks
        for (int ty = 30; ty <= 84; ty += 9) {
            g.fill(x + 152, y + ty, x + 156, y + ty + 1, 0x66000000);
        }
        g.renderOutline(x + 151, y + 23, 15, 67, 0xFF334155);

        // --- 6. Player Inventory & Hotbar (y = 108 & 166) ---
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                drawSlotFrame(g, x + 8 + col * 18, y + 108 + row * 18, 0xFF1E2D44, 0xFF080C14);
            }
        }
        for (int col = 0; col < 9; ++col) {
            drawSlotFrame(g, x + 8 + col * 18, y + 166, 0xFF1E2D44, 0xFF080C14);
        }
    }

    private static void drawSlotFrame(GuiGraphics g, int sx, int sy, int borderColor, int innerColor) {
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, borderColor);
        g.fill(sx, sy, sx + 16, sy + 16, innerColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        if (button == 0 && this.minecraft != null && this.minecraft.gameMode != null) {
            if (mouseX >= x + 10 && mouseX <= x + 64 && mouseY >= y + 74 && mouseY <= y + 90) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                return true;
            }
            if (mouseX >= x + 67 && mouseX <= x + 121 && mouseY >= y + 74 && mouseY <= y + 90) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
                return true;
            }
            if (mouseX >= x + 124 && mouseX <= x + 148 && mouseY >= y + 23 && mouseY <= y + 39) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 2);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, Component.literal("§b⚡ §fKeraunos HV Matrix"), 8, 5, 0xFFFFFF, false);

        String fillStr = String.format("§e%.1f%%", 100.0 * this.menu.getEnergy() / Math.max(1L, this.menu.getMaxEnergy()));
        int w = this.font.width(fillStr);
        g.drawString(this.font, fillStr, 168 - w, 5, 0xFFFFFF, false);

        g.drawString(this.font, Component.literal("§8INVENTORY"), 8, 97, 0x64748B, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        if (mouseX >= x + 151 && mouseX <= x + 166 && mouseY >= y + 23 && mouseY <= y + 90) {
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("§b⚡ Keraunos HV Dielectric Capacitor"),
                    Component.literal(String.format("§7Stored: §f%,d / %,d RF", this.menu.getEnergy(), this.menu.getMaxEnergy())),
                    Component.literal("§7Voltage: §e" + this.menu.getVoltageKv() + " kV"),
                    Component.literal("§7Shell: §f" + this.menu.getBasicCellsCount() + " Basic §7| §d" + this.menu.getAdvancedUnitsCount() + " Adv Units")
            ), mouseX, mouseY);
        } else if (mouseX >= x + 10 && mouseX <= x + 64 && mouseY >= y + 74 && mouseY <= y + 90) {
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("§a⏚ Grounded Service Lockout"),
                    Component.literal("§7Drops voltage to §a0 kV§7, disables arc field,"),
                    Component.literal("§7halts I/O, and allows safe dismantling.")
            ), mouseX, mouseY);
        } else if (mouseX >= x + 67 && mouseX <= x + 121 && mouseY >= y + 74 && mouseY <= y + 90) {
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6⚡ Overvoltage Transformer Mode"),
                    Component.literal("§7Doubles voltage up to §e2,000 kV§7 and raises"),
                    Component.literal("§7Port rate from §b25M §7to §650M RF/t§7,"),
                    Component.literal("§cbut expands proximity arc radius to 6.5m!")
            ), mouseX, mouseY);
        } else if (mouseX >= x + 124 && mouseX <= x + 148 && mouseY >= y + 23 && mouseY <= y + 39) {
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("§b⇄ Supercharge Port Mode"),
                    Component.literal(this.menu.getTransferMode() == 0
                            ? "§bCurrently: §fCharge Item (10M RF/t)"
                            : "§6Currently: §fDrain Item into Core (10M RF/t)")
            ), mouseX, mouseY);
        } else if (this.menu.getSlot(0).getItem().isEmpty()
                && mouseX >= x + 127 && mouseX <= x + 145 && mouseY >= y + 44 && mouseY <= y + 62) {
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("§b⚡ High-Voltage Item Port"),
                    Component.literal("§7Insert any RF tool, armor, or battery to charge/drain at §e10M RF/t§7.")
            ), mouseX, mouseY);
        }
    }
}
