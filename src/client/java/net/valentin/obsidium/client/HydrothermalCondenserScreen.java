package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.block.HydrothermalCondenserBlockEntity;
import net.valentin.obsidium.block.HydrothermalCondenserMenu;

import java.util.List;

public class HydrothermalCondenserScreen extends AbstractContainerScreen<HydrothermalCondenserMenu> {

    private static final int BTN_X = 64;
    private static final int BTN_Y = 54;
    private static final int BTN_W = 28;
    private static final int BTN_H = 14;

    public HydrothermalCondenserScreen(HydrothermalCondenserMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 73;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int mode = this.menu.getOutputMode();
        String titleStr = mode == HydrothermalCondenserBlockEntity.MODE_WATER
                ? "✦ Hydrothermal Condenser"
                : "❄ Hydro-Cryo Ice Synthesizer";
        int titleCol = mode == HydrothermalCondenserBlockEntity.MODE_WATER ? 0x00E5FF : 0xA8F0FF;
        guiGraphics.drawString(this.font, titleStr, this.titleLabelX, this.titleLabelY, titleCol, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xCECAC5, false);

        int eff = this.menu.getEfficiencyMultiplier();
        guiGraphics.drawString(this.font, "RATE", 12, 24, 0x5599AA, false);
        guiGraphics.drawString(this.font, (eff / 100.0) + "x", 13, 34, 0x90E0EF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        if (button == 0 && mouseX >= i + BTN_X && mouseX <= i + BTN_X + BTN_W
                && mouseY >= j + BTN_Y && mouseY <= j + BTN_Y + BTN_H) {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.15F));
                return true;
            }
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
        int mode = this.menu.getOutputMode();

        // Tooltip: Mode Button
        if (mouseX >= i + BTN_X && mouseX <= i + BTN_X + BTN_W && mouseY >= j + BTN_Y && mouseY <= j + BTN_Y + BTN_H) {
            String modeName = switch (mode) {
                case HydrothermalCondenserBlockEntity.MODE_ICE -> "§b❄ Cryo-Freeze: ICE";
                case HydrothermalCondenserBlockEntity.MODE_PACKED_ICE -> "§9🧊 Cryo-Freeze: PACKED ICE";
                case HydrothermalCondenserBlockEntity.MODE_BLUE_ICE -> "§3💎 Cryo-Freeze: BLUE ICE";
                default -> "§b💧 Liquid Water & Catalyst";
            };
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§e⚙ Synthesis Mode: " + modeName),
                    Component.literal("§7• §bWATER§7: Fills Buckets (1,000 mB) or upgrades Ice in Slot 2"),
                    Component.literal("§7• §bICE§7: Auto-freezes §f500 mB Water ➔ 1× Ice §7(250 mB with Dew/Snow)"),
                    Component.literal("§7• §9PACKED§7: Auto-freezes §f1,500 mB ➔ 1× Packed Ice §7(500 mB with Ice)"),
                    Component.literal("§7• §3BLUE ICE§7: Auto-freezes §f3,000 mB ➔ 1× Blue Ice §7(1,000 mB with Packed Ice)"),
                    Component.literal("§aClick to cycle output mode!")
            ), mouseX, mouseY);
        }

        // Tooltip: Condensation Efficiency Gauge (10, 22) 28x46
        if (mouseX >= i + 10 && mouseX <= i + 38 && mouseY >= j + 22 && mouseY <= j + 68) {
            int eff = this.menu.getEfficiencyMultiplier();
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§b⚡ Condensation & Cryo Efficiency"),
                    Component.literal("§7Current Speed: §a" + (eff / 100.0) + "x"),
                    Component.literal("§8Place Ice / Blue Ice below for +1.0x speed!")
            ), mouseX, mouseY);
        }

        // Tooltip: Water Tank (148, 20) 14x48
        if (mouseX >= i + 148 && mouseX <= i + 162 && mouseY >= j + 20 && mouseY <= j + 68) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§b💧 Hydrothermal Water Buffer"),
                    Component.literal("§f" + this.menu.getWaterAmount() + " / " + HydrothermalCondenserBlockEntity.MAX_WATER + " mB"),
                    Component.literal("§7Production: §e250 mB per cycle"),
                    Component.literal("§7Used for: §bWater Buckets, Ice, Packed Ice & Blue Ice")
            ), mouseX, mouseY);
        }

        // Tooltip: Condensation Efficiency Status Indicator (44, 55) 18x12
        if (mouseX >= i + 44 && mouseX <= i + 60 && mouseY >= j + 55 && mouseY <= j + 67) {
            int eff = this.menu.getEfficiencyMultiplier();
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§b💧 Environmental Humidity: §aACTIVE"),
                    Component.literal("§eMultiplier: " + (eff / 100.0) + "x speed"),
                    Component.literal("§8• Ice / Packed Ice / Blue Ice below (§b+1.0x§8)"),
                    Component.literal("§8• Water block below (§3+0.5x§8)"),
                    Component.literal("§8• Moss / Sponge below (§a+0.25x§8)"),
                    Component.literal("§8• Rain weather (§9+0.5x§8)")
            ), mouseX, mouseY);
        }

        // Tooltip: Condensation Progress Bar (66, 37) 24x10
        if (mouseX >= i + 66 && mouseX <= i + 90 && mouseY >= j + 37 && mouseY <= j + 47) {
            int max = this.menu.getMaxProgress();
            int prog = max > 0 ? (int) (100.0f * this.menu.getProgress() / max) : 0;
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§b✦ Atmospheric Condensation"),
                    Component.literal("§7Progress: §e" + prog + "%"),
                    Component.literal("§8Yield: 250 mB Water + 50% chance of Dew Droplet")
            ), mouseX, mouseY);
        }

        // Tooltip: Moisture Input (44, 35)
        if (this.menu.getSlot(0).getItem().isEmpty() && mouseX >= i + 43 && mouseX <= i + 61 && mouseY >= j + 34 && mouseY <= j + 52) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§aMoisture & Biomass Input"),
                    Component.literal("§8Accepts: Leaves, Saplings, Vines, Cactus, Kelp, Snow, Ice")
            ), mouseX, mouseY);
        }

        // Tooltip: Dew Droplets Output (96, 22)
        if (this.menu.getSlot(3).getItem().isEmpty() && mouseX >= i + 95 && mouseX <= i + 113 && mouseY >= j + 21 && mouseY <= j + 39) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§b✦ Dew Droplets (Byproduct)"),
                    Component.literal("§7Crystallizes during hydrothermal condensation"),
                    Component.literal("§8Use as Cryo-Seed in Slot 2 or coolant in Piston Engine!")
            ), mouseX, mouseY);
        }

        // Tooltip: Empty Bucket / Cryo-Seed In (96, 52)
        if (this.menu.getSlot(1).getItem().isEmpty() && mouseX >= i + 95 && mouseX <= i + 113 && mouseY >= j + 51 && mouseY <= j + 69) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§bEmpty Bucket / Cryo-Catalyst Seed"),
                    Component.literal("§7Accepts: §fEmpty Bucket, Dew Droplet, Snowball, Ice, Packed Ice"),
                    Component.literal("§8• Seed items cut the water cost of freezing Ice / Packed / Blue Ice by 50–66%!")
            ), mouseX, mouseY);
        }

        // Tooltip: Output (122, 52)
        if (this.menu.getSlot(2).getItem().isEmpty() && mouseX >= i + 121 && mouseX <= i + 139 && mouseY >= j + 51 && mouseY <= j + 69) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§bWater Bucket / Cryo-Ice Output"),
                    Component.literal("§7Outputs: §fWater Bucket, Ice, Packed Ice, or Blue Ice"),
                    Component.literal("§8Automatically extracted by pipes into Piston Engine or Collider!")
            ), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        VolcaniteGuiTheme.renderWindow(guiGraphics, i, j, this.imageWidth, this.imageHeight, false);
        VolcaniteGuiTheme.renderRecessedPanel(guiGraphics, i + 7, j + 18, 162, 53);
        VolcaniteGuiTheme.renderPlayerInventory(guiGraphics, i + 8, j + 84);

        long time = System.currentTimeMillis();
        int mode = this.menu.getOutputMode();

        // 0. Condenser Efficiency Gauge Panel (i + 10, j + 22, w = 28, h = 46)
        guiGraphics.fill(i + 10, j + 22, i + 38, j + 68, 0xFF0B141C);
        border(guiGraphics, i + 10, j + 22, 28, 46, mode == 0 ? 0xFF005577 : 0xFF3A88CC);
        guiGraphics.fill(i + 12, j + 45, i + 36, j + 46, 0xFF003850);

        // Animated Condensation Droplets or Frost Crystals in the Rate Gauge Panel
        int dripY = (int) ((time / 75) % 18);
        guiGraphics.fill(i + 23, j + 46 + dripY, i + 25, j + 48 + dripY, mode == 0 ? 0xFF90E0EF : 0xFFD8F8FF);
        guiGraphics.fill(i + 24, j + 45 + dripY, i + 25, j + 46 + dripY, 0xAA00B4D8);
        // Water / Ice pool at bottom of gauge
        guiGraphics.fill(i + 13, j + 64, i + 35, j + 67, mode == 0 ? 0xFF0077B6 : 0xFF48CAE4);
        guiGraphics.fill(i + 13, j + 64, i + 35, j + 65, 0xFFD8F8FF);

        // 1. Water Tank (i + 148, j + 20, width 14, height 48)
        int tankFluidCol = mode == 0 ? 0xCC00B4D8 : 0xCC48CAE4;
        int tankSurfCol = mode == 0 ? 0xFF90E0EF : 0xFFE0FBFC;
        drawTankGauge(guiGraphics, i + 148, j + 20, 14, 48, this.menu.getWaterAmount(),
                HydrothermalCondenserBlockEntity.MAX_WATER, 0xFF0077B6, tankFluidCol, tankSurfCol);

        // 2. Themed Slot Frames
        VolcaniteGuiTheme.renderSlot(guiGraphics, i + 44, j + 35, 0xFF2E8B57);
        VolcaniteGuiTheme.renderSlot(guiGraphics, i + 96, j + 22, 0xFF00B4D8);
        VolcaniteGuiTheme.renderSlot(guiGraphics, i + 96, j + 52, 0xFF5599CC);
        VolcaniteGuiTheme.renderSlot(guiGraphics, i + 122, j + 52, mode == 0 ? 0xFF0077B6 : 0xFF80E5FF);

        // Arrow between Slot 1 (96, 52) and Slot 2 (122, 52)
        guiGraphics.fill(i + 115, j + 59, i + 120, j + 62, mode == 0 ? 0xFF0077B6 : 0xFF90E0EF);

        // 3. Humidity indicator beneath Moisture Slot (i + 44, j + 55, w = 16, h = 12)
        double wave = (Math.sin(time / 220.0) + 1.0) / 2.0;
        int dropBlue = 0xB4 + (int) (0x4B * wave);
        guiGraphics.fill(i + 44, j + 55, i + 60, j + 67, 0xFF003355);
        guiGraphics.fill(i + 46, j + 57, i + 58, j + 65, 0xFF000000 | (0x35 << 16) | (dropBlue << 8) | 0xFF);
        border(guiGraphics, i + 44, j + 55, 16, 12, 0xFF00B4D8);

        // 4. Interactive Mode Toggle Button (i + 64, j + 54, w = 28, h = 14)
        boolean hovered = mouseX >= i + BTN_X && mouseX <= i + BTN_X + BTN_W && mouseY >= j + BTN_Y && mouseY <= j + BTN_Y + BTN_H;
        int btnBg = switch (mode) {
            case HydrothermalCondenserBlockEntity.MODE_ICE -> 0xFF10344A;
            case HydrothermalCondenserBlockEntity.MODE_PACKED_ICE -> 0xFF142A52;
            case HydrothermalCondenserBlockEntity.MODE_BLUE_ICE -> 0xFF1A1E5A;
            default -> 0xFF0C2233;
        };
        int btnBorder = hovered ? 0xFFFFD54F : (mode == 0 ? 0xFF00B4D8 : 0xFF90E0EF);
        guiGraphics.fill(i + BTN_X, j + BTN_Y, i + BTN_X + BTN_W, j + BTN_Y + BTN_H, btnBg);
        border(guiGraphics, i + BTN_X, j + BTN_Y, BTN_W, BTN_H, btnBorder);
        String btnLabel = switch (mode) {
            case HydrothermalCondenserBlockEntity.MODE_ICE -> "❄ICE";
            case HydrothermalCondenserBlockEntity.MODE_PACKED_ICE -> "🧊PKD";
            case HydrothermalCondenserBlockEntity.MODE_BLUE_ICE -> "💎BLU";
            default -> "💧H2O";
        };
        int textW = this.font.width(btnLabel);
        guiGraphics.drawString(this.font, btnLabel, i + BTN_X + (BTN_W - textW) / 2, j + BTN_Y + 3, 0xE0FBFC, false);

        // 5. Condensation Progress Bar (i + 66, j + 37, width 24, height 10)
        int maxProg = this.menu.getMaxProgress();
        int progWidth = maxProg > 0 ? (int) (22.0f * this.menu.getProgress() / maxProg) : 0;
        guiGraphics.fill(i + 66, j + 37, i + 90, j + 47, 0xFF0A0610);
        border(guiGraphics, i + 66, j + 37, 24, 10, 0xFF005577);
        if (progWidth > 0) {
            guiGraphics.fill(i + 67, j + 38, i + 67 + progWidth, j + 46, 0xFF0077B6);
            guiGraphics.fill(i + 67, j + 38, i + 67 + progWidth, j + 40, 0xFF90E0EF);
            int pulsePos = (int) ((time / 55) % Math.max(1, progWidth));
            guiGraphics.fill(i + 67 + pulsePos, j + 38, Math.min(i + 67 + pulsePos + 2, i + 67 + progWidth), j + 46, 0xCCFFFFFF);
        }
    }

    private void drawTankGauge(GuiGraphics g, int x, int y, int w, int h, int amount, int max, int borderCol, int fluidCol, int surfaceCol) {
        g.fill(x, y, x + w, y + h, 0xFF08050C);
        border(g, x, y, w, h, borderCol);

        if (max > 0 && amount > 0) {
            int innerH = h - 2;
            int innerW = w - 2;
            int filledH = (int) ((float) innerH * Math.min(amount, max) / max);
            if (filledH > 0) {
                int topY = y + 1 + (innerH - filledH);
                g.fill(x + 1, topY, x + 1 + innerW, y + h - 1, fluidCol);
                g.fill(x + 1, topY, x + 1 + innerW, Math.min(topY + 2, y + h - 1), surfaceCol);
                long time = System.currentTimeMillis();
                int ripple = (int) ((time / 200) % innerW);
                g.fill(x + 1 + ripple, topY, Math.min(x + 3 + ripple, x + 1 + innerW), Math.min(topY + 1, y + h - 1), 0xFFFFFFFF);
            }
        }
    }

    private void border(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
