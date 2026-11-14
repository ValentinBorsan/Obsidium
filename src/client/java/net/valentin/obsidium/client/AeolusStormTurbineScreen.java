package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.block.AeolusStormTurbineBlockEntity;
import net.valentin.obsidium.block.AeolusStormTurbineMenu;

import java.util.List;

public class AeolusStormTurbineScreen extends AbstractContainerScreen<AeolusStormTurbineMenu> {

    public AeolusStormTurbineScreen(AeolusStormTurbineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 1000;
        this.inventoryLabelX = 1000;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Outer shadow and storm-slate casing
        g.fill(x - 1, y - 1, x + this.imageWidth + 1, y + this.imageHeight + 1, 0xFF06090F);
        g.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF0F1522);

        // Metallic cyan-slate bezel
        g.fill(x, y, x + this.imageWidth, y + 1, 0xFF284160);
        g.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF131D2E);
        g.fill(x, y, x + 1, y + this.imageHeight, 0xFF284160);
        g.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, 0xFF131D2E);

        // Electric Cyan/Gold corner pins
        int pinColor = (this.menu.getWeatherState() >= 3) ? 0xFFFACC15 : 0xFF38BDF8;
        g.fill(x + 1, y + 1, x + 3, y + 3, pinColor);
        g.fill(x + this.imageWidth - 3, y + 1, x + this.imageWidth - 1, y + 3, pinColor);
        g.fill(x + 1, y + this.imageHeight - 3, x + 3, y + this.imageHeight - 1, pinColor);
        g.fill(x + this.imageWidth - 3, y + this.imageHeight - 3, x + this.imageWidth - 1, y + this.imageHeight - 1, pinColor);

        // Header Bar
        g.fill(x + 1, y + 1, x + this.imageWidth - 1, y + 16, 0xFF152033);
        g.fill(x + 1, y + 16, x + this.imageWidth - 1, y + 17, 0xFF233856);

        // Telemetry Dashboard Panel
        g.fill(x + 8, y + 20, x + 168, y + 73, 0xFF121A2B);
        g.fill(x + 8, y + 20, x + 168, y + 21, 0xFF1E304C);

        // 1. Weather Status Box (x + 12, y + 24, w = 102, h = 16)
        int weather = this.menu.getWeatherState();
        int weatherBorder = switch (weather) {
            case 4 -> 0xFFFACC15;
            case 3 -> 0xFFA855F7;
            case 2 -> 0xFF38BDF8;
            case 1 -> 0xFF34D399;
            default -> 0xFF475569;
        };
        g.fill(x + 12, y + 24, x + 114, y + 40, 0xFF0A101A);
        g.renderOutline(x + 12, y + 24, 102, 16, weatherBorder);

        String weatherText = switch (weather) {
            case 4 -> "§e⚡ ASTRAPE SURGE!";
            case 3 -> "§d⛈ Storm (4.5×)";
            case 2 -> "§b🌧 Rain Gale (2.2×)";
            case 1 -> "§a🍃 Clear Wind (1.0×)";
            default -> "§c✖ Sky Blocked";
        };
        g.drawString(this.font, weatherText, x + 16, y + 28, 0xFFFFFF, false);

        // 2. Altitude Wind Bar (x + 12, y + 44, w = 102, h = 10)
        int altPct = this.menu.getAltitudePct();
        int altFill = Math.min(100, altPct * 100 / 250);
        g.fill(x + 12, y + 44, x + 114, y + 54, 0xFF0A101A);
        g.fill(x + 13, y + 45, x + 13 + altFill, y + 53, 0xFF0284C7);
        g.renderOutline(x + 12, y + 44, 102, 10, 0xFF1E3A5F);
        g.drawString(this.font, "§7Alt: §f" + altPct + "%", x + 16, y + 45, 0xFFFFFF, false);

        // 3. Biome Resonance Bar (x + 12, y + 58, w = 102, h = 10)
        int bioPct = this.menu.getBiomePct();
        int bioFill = Math.min(100, bioPct * 100 / 175);
        int bioColor = bioPct >= 140 ? 0xFF10B981 : (bioPct >= 100 ? 0xFF3B82F6 : 0xFFF59E0B);
        g.fill(x + 12, y + 58, x + 114, y + 68, 0xFF0A101A);
        g.fill(x + 13, y + 59, x + 13 + bioFill, y + 67, bioColor);
        g.renderOutline(x + 12, y + 58, 102, 10, 0xFF1E3A5F);
        g.drawString(this.font, "§7Biome: §f" + bioPct + "%", x + 16, y + 59, 0xFFFFFF, false);

        // 4. Astrape Surge / Rod Badge (x + 118, y + 24, w = 26, h = 18)
        int surge = this.menu.getSurgeTicks();
        boolean rod = this.menu.hasLightningRod();
        g.fill(x + 118, y + 24, x + 144, y + 42, 0xFF0A101A);
        g.renderOutline(x + 118, y + 24, 26, 18, surge > 0 ? 0xFFFACC15 : (rod ? 0xFF38BDF8 : 0xFF334155));
        String badgeLabel = surge > 0 ? ("§e" + (surge / 20 + 1) + "s") : (rod ? "§bRod" : "§8--");
        g.drawString(this.font, badgeLabel, x + 118 + (26 - this.font.width(badgeLabel)) / 2, y + 29, 0xFFFFFF, false);

        // 5. Slot 0: Catalyst / RF Charge Slot (x + 122, y + 48)
        drawSlotFrame(g, x + 122, y + 48, surge > 0 ? 0xFFFACC15 : 0xFF38BDF8, 0xFF0A0F18);

        // 6. Energy Capacitor Gauge (x + 148, y + 24, w = 14, h = 44)
        int energy = this.menu.getEnergy();
        int maxEnergy = this.menu.getMaxEnergy();
        int barH = (maxEnergy > 0) ? (int) (42.0 * Math.min(1.0, (double) energy / maxEnergy)) : 0;
        g.fill(x + 148, y + 24, x + 162, y + 68, 0xFF090D14);
        if (barH > 0) {
            g.fill(x + 149, y + 67 - barH, x + 161, y + 67, surge > 0 ? 0xFFFACC15 : 0xFF38BDF8);
        }
        g.renderOutline(x + 148, y + 24, 14, 44, 0xFF334155);

        // Player Inventory (3 rows x 9 columns at y = 84)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                drawSlotFrame(g, x + 8 + col * 18, y + 84 + row * 18, 0xFF1E2D44, 0xFF090D14);
            }
        }

        // Player Hotbar (9 slots at y = 142)
        for (int col = 0; col < 9; ++col) {
            drawSlotFrame(g, x + 8 + col * 18, y + 142, 0xFF1E2D44, 0xFF090D14);
        }
    }

    private static void drawSlotFrame(GuiGraphics g, int sx, int sy, int borderColor, int innerColor) {
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, borderColor);
        g.fill(sx, sy, sx + 16, sy + 16, innerColor);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, Component.literal("§b⚡ §fAeolus Dynamo"), 8, 5, 0xFFFFFF, false);

        int rate = this.menu.getGenerationRate();
        String rateText = (rate > 0) ? String.format("§b+%,d RF/t", rate) : "§7Idle";
        int textW = this.font.width(rateText);
        g.drawString(this.font, rateText, 168 - textW, 5, 0xFFFFFF, false);

        g.drawString(this.font, Component.literal("§8INVENTORY"), 8, 74, 0x64748B, false);
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

        // Energy Capacitor Tooltip
        if (mouseX >= x + 148 && mouseX <= x + 162 && mouseY >= y + 24 && mouseY <= y + 68) {
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("§b⚡ Aetherial Storm Capacitor"),
                    Component.literal(String.format("§7Stored: §f%,d / %,d RF", this.menu.getEnergy(), this.menu.getMaxEnergy())),
                    Component.literal("§7Current Output: §a+" + String.format("%,d", this.menu.getGenerationRate()) + " RF/t"),
                    Component.literal("§8Max Transfer: 10,000 RF/t")
            ), mouseX, mouseY);
        }
        // Weather Box Tooltip
        else if (mouseX >= x + 12 && mouseX <= x + 114 && mouseY >= y + 24 && mouseY <= y + 40) {
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("§b🌩 Atmospheric Telemetry"),
                    Component.literal("§7Clear Sky: §f1.0× Base Wind"),
                    Component.literal("§7Rain / Snow: §b2.2× Gale Boost"),
                    Component.literal("§7Thunderstorm: §d4.5× Tempest Boost"),
                    Component.literal("§eLightning Strike: §f+75,000 RF burst & +2,000 RF/t for 10s")
            ), mouseX, mouseY);
        }
        // Altitude Wind Bar Tooltip
        else if (mouseX >= x + 12 && mouseX <= x + 114 && mouseY >= y + 44 && mouseY <= y + 54) {
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("§b🍃 Altitude & Airflow: §f" + this.menu.getAltitudePct() + "%"),
                    Component.literal("§7Higher Y-levels (up to Y=180+) and open"),
                    Component.literal("§7sides around the rotor increase wind speed up to 250%.")
            ), mouseX, mouseY);
        }
        // Biome Resonance Bar Tooltip
        else if (mouseX >= x + 12 && mouseX <= x + 114 && mouseY >= y + 58 && mouseY <= y + 68) {
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("§a⛰ Biome Wind Resonance: §f" + this.menu.getBiomePct() + "%"),
                    Component.literal("§7• Peaks / Mountains / Windswept: §a175%"),
                    Component.literal("§7• Ocean / Coast / River: §b140%"),
                    Component.literal("§7• Plains / Meadow / Savanna: §e120%"),
                    Component.literal("§7• Desert / Badlands / Nether: §c40%")
            ), mouseX, mouseY);
        }
        // Lightning Rod / Surge Badge Tooltip
        else if (mouseX >= x + 118 && mouseX <= x + 144 && mouseY >= y + 24 && mouseY <= y + 42) {
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("§e⚡ Astrape Lightning Rod"),
                    Component.literal(this.menu.hasLightningRod()
                            ? "§a✔ Lightning Rod Installed (+20% RF/t & 2× Strikes)"
                            : "§7Place a vanilla §fLightning Rod §7directly on top"),
                    Component.literal("§7to gain §a+20% output §7and attract storm bolts!"),
                    Component.literal(this.menu.getSurgeTicks() > 0
                            ? "§e★ Surge Active: +" + AeolusStormTurbineBlockEntity.SURGE_BONUS_RATE + " RF/t (" + (this.menu.getSurgeTicks() / 20 + 1) + "s)"
                            : "§8Waiting for thunderstorm strike...")
            ), mouseX, mouseY);
        }
        // Catalyst / Charge Slot Tooltip (when empty)
        else if (this.menu.getSlot(0).getItem().isEmpty()
                && mouseX >= x + 121 && mouseX <= x + 139 && mouseY >= y + 47 && mouseY <= y + 65) {
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("§b⚡ Storm Catalyst / Charge Slot"),
                    Component.literal("§7• Insert §fCopper Ingot, Amethyst, Electrum Fruit,"),
                    Component.literal("§7  or §fZeus Tin Fruit §7to attract lightning in storms!"),
                    Component.literal("§7• Or insert any §eRF Tool / Battery §7to charge it.")
            ), mouseX, mouseY);
        }
    }
}
