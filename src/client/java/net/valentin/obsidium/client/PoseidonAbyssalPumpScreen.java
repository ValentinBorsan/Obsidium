package net.valentin.obsidium.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.PipeConfigurable;
import net.valentin.obsidium.block.PoseidonAbyssalPumpBlockEntity;
import net.valentin.obsidium.block.PoseidonAbyssalPumpMenu;
import net.valentin.obsidium.item.QuantumLinkModuleItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PoseidonAbyssalPumpScreen extends AbstractContainerScreen<PoseidonAbyssalPumpMenu> {
    private static final int[] RADII = {8, 16, 32, 48};

    public PoseidonAbyssalPumpScreen(PoseidonAbyssalPumpMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 236;
        this.imageHeight = 166;
        this.titleLabelX = 1000;
        this.inventoryLabelX = 1000;
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null && this.minecraft.player != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
            this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.55F, 1.2F);
        }
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawScaled(GuiGraphics g, String text, int x, int y, float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0f);
        g.drawString(this.font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private void drawCenteredScaled(GuiGraphics g, String text, int centerX, int y, float scale, int color) {
        int sw = Math.round(this.font.width(text) * scale);
        drawScaled(g, text, centerX - sw / 2, y, scale, color);
    }

    private static String formatMb(long mb) {
        if (mb >= 1_000_000L) return String.format("%.2fM mB", mb / 1_000_000.0);
        if (mb >= 1_000L) return String.format("%.1fk mB", mb / 1_000.0);
        return mb + " mB";
    }

    private static String formatKrf(long krf) {
        if (krf >= 1_000L) return String.format("%.2fM RF", krf / 1_000.0);
        return krf + "k RF";
    }

    private int getFluidBarColor(Fluid fluid) {
        if (fluid == Fluids.LAVA) return 0xFFFF6611;
        if (fluid == Fluids.WATER) return 0xFF00C8FF;
        if (fluid == ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID) return 0xFFD946EF;
        if (fluid == ObsidiumMod.STEAM_FLUID) return 0xFFCBD5E1;
        return 0xFF38BDF8;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        // Main Abyssal Panel
        g.fill(i, j, i + this.imageWidth, j + this.imageHeight, 0xFF0A121C);
        drawBorder(g, i, j, this.imageWidth, this.imageHeight, 0xFF0E7490);
        drawBorder(g, i + 1, j + 1, this.imageWidth - 2, this.imageHeight - 2, 0xFFB8860B);

        // Header bar (y = 4..20)
        g.fill(i + 4, j + 4, i + this.imageWidth - 4, j + 20, 0xFF0F1D2C);
        g.fill(i + 4, j + 20, i + this.imageWidth - 4, j + 21, 0xFF164E63);

        if (!this.menu.isFormed()) {
            boolean holo = this.menu.isShowHologram();
            g.fill(i + 136, j + 6, i + 180, j + 18, holo ? 0xFF0C3547 : 0xFF1A2230);
            drawBorder(g, i + 136, j + 6, 44, 12, holo ? 0xFF22D3EE : 0xFF475569);

            g.fill(i + 182, j + 6, i + 230, j + 18, 0xFF332612);
            drawBorder(g, i + 182, j + 6, 48, 12, 0xFFFBBF24);
        } else {
            g.fill(i + 148, j + 6, i + 230, j + 18, 0xFF0B3027);
            drawBorder(g, i + 148, j + 6, 82, 12, 0xFF34D399);
        }

        // Left Gauges: Energy (10..24, 30..124) & Fluid (30..44, 30..124)
        g.fill(i + 10, j + 30, i + 24, j + 124, 0xFF060B12);
        drawBorder(g, i + 9, j + 29, 16, 96, 0xFF155E75);
        long krf = this.menu.getEnergyKrf();
        if (krf > 0) {
            int eh = Math.max(2, (int) Math.min(94, (krf * 94L) / (PoseidonAbyssalPumpBlockEntity.MAX_ENERGY / 1000L)));
            g.fill(i + 11, j + 123 - eh, i + 23, j + 123, 0xFF00E5FF);
        }

        g.fill(i + 30, j + 30, i + 44, j + 124, 0xFF060B12);
        drawBorder(g, i + 29, j + 29, 16, 96, 0xFF0284C7);
        long mb = this.menu.getFluidMb();
        if (mb > 0) {
            int fh = Math.max(2, (int) Math.min(94, (mb * 94L) / PoseidonAbyssalPumpBlockEntity.MAX_FLUID_MB));
            g.fill(i + 31, j + 123 - fh, i + 43, j + 123, getFluidBarColor(this.menu.getCurrentFluid()));
        }

        // Center Status Card (52..136, 26..126)
        g.fill(i + 52, j + 26, i + 136, j + 126, 0xFF0D1724);
        drawBorder(g, i + 52, j + 26, 84, 100, 0xFF1E3A5F);

        // Rescan button inside center card (60..128, 108..121)
        g.fill(i + 60, j + 108, i + 128, j + 121, 0xFF15293E);
        drawBorder(g, i + 60, j + 108, 68, 13, 0xFF38BDF8);

        // Right Control Buttons (142..228)
        int[] btnY = {26, 46, 66, 86, 106};
        for (int by : btnY) {
            g.fill(i + 142, j + by, i + 228, j + by + 17, 0xFF112031);
            drawBorder(g, i + 142, j + by, 86, 17, 0xFF1E4E6E);
        }

        // Bottom Quantum Link Bar (8..228, 133..159)
        g.fill(i + 8, j + 133, i + 228, j + 159, 0xFF0E1624);
        drawBorder(g, i + 8, j + 133, 220, 26, this.menu.isQuantumEnabled() ? 0xFFA855F7 : 0xFF334155);

        // Quantum sub-buttons: Toggle (12..62), Color (66..118), Ch- (122..136), Ch+ (162..176), Privacy (180..224)
        g.fill(i + 12, j + 138, i + 62, j + 154, this.menu.isQuantumEnabled() ? 0xFF3B1D5E : 0xFF1E293B);
        drawBorder(g, i + 12, j + 138, 50, 16, this.menu.isQuantumEnabled() ? 0xFFC084FC : 0xFF475569);

        DyeColor qc = this.menu.getQuantumColor();
        int qRgb = 0xFF000000 | QuantumLinkModuleItem.getChannelRgb(qc);
        g.fill(i + 66, j + 138, i + 118, j + 154, 0xFF131D2B);
        drawBorder(g, i + 66, j + 138, 52, 16, qRgb);
        g.fill(i + 69, j + 141, i + 77, j + 151, qRgb);

        g.fill(i + 122, j + 138, i + 136, j + 154, 0xFF1A2536);
        drawBorder(g, i + 122, j + 138, 14, 16, 0xFF475569);
        g.fill(i + 162, j + 138, i + 176, j + 154, 0xFF1A2536);
        drawBorder(g, i + 162, j + 138, 14, 16, 0xFF475569);

        g.fill(i + 180, j + 138, i + 224, j + 154, this.menu.isQuantumPrivate() ? 0xFF3F1D2B : 0xFF162A2C);
        drawBorder(g, i + 180, j + 138, 44, 16, this.menu.isQuantumPrivate() ? 0xFFF472B6 : 0xFF2DD4BF);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        drawScaled(g, "§b🔱 POSEIDON ABYSSAL PUMP", 8, 8, 0.85f, 0xFF67E8F9);

        if (!this.menu.isFormed()) {
            boolean holo = this.menu.isShowHologram();
            drawCenteredScaled(g, holo ? "Holo:ON" : "Holo:OFF", 158, 10, 0.68f, holo ? 0xFF22D3EE : 0xFF94A3B8);
            drawCenteredScaled(g, "Auto-Build", 206, 10, 0.68f, 0xFFFBBF24);
        } else {
            drawCenteredScaled(g, "2×2×2 FORMED", 189, 10, 0.70f, 0xFF34D399);
        }

        drawCenteredScaled(g, "RF", 17, 22, 0.65f, 0xFF22D3EE);
        drawCenteredScaled(g, "FL", 37, 22, 0.65f, 0xFF38BDF8);

        // Center Status Card
        int status = this.menu.getPumpStatus();
        String statusStr = !this.menu.isFormed() ? "UNFORMED" : switch (status) {
            case PoseidonAbyssalPumpBlockEntity.PUMP_INFINITE_WATER -> "INFINITE 2×2";
            case PoseidonAbyssalPumpBlockEntity.PUMP_AREA_DRAIN -> "AREA DRAIN";
            case PoseidonAbyssalPumpBlockEntity.PUMP_DRAINED -> "AREA DRAINED";
            default -> "STANDBY";
        };
        int statusCol = !this.menu.isFormed() ? 0xFFFF5566 : switch (status) {
            case PoseidonAbyssalPumpBlockEntity.PUMP_INFINITE_WATER -> 0xFF22D3EE;
            case PoseidonAbyssalPumpBlockEntity.PUMP_AREA_DRAIN -> 0xFFFBBF24;
            case PoseidonAbyssalPumpBlockEntity.PUMP_DRAINED -> 0xFF34D399;
            default -> 0xFF94A3B8;
        };
        drawCenteredScaled(g, statusStr, 94, 32, 0.72f, statusCol);

        Fluid curFluid = this.menu.getCurrentFluid();
        String fluidName = curFluid == Fluids.EMPTY ? "Empty" : curFluid.getBucket().getDescription().getString().replace(" Bucket", "");
        if (fluidName.length() > 13) fluidName = fluidName.substring(0, 13);
        drawCenteredScaled(g, fluidName, 94, 46, 0.70f, 0xFFE2E8F0);
        drawCenteredScaled(g, formatMb(this.menu.getFluidMb()), 94, 57, 0.70f, 0xFF38BDF8);
        drawCenteredScaled(g, formatKrf(this.menu.getEnergyKrf()), 94, 70, 0.68f, 0xFF67E8F9);

        String yLabel = (status == PoseidonAbyssalPumpBlockEntity.PUMP_AREA_DRAIN)
                ? ("Scan Y: " + this.menu.getScanY())
                : ("2×2×2 Intake");
        drawCenteredScaled(g, yLabel, 94, 83, 0.66f, 0xFF94A3B8);

        drawCenteredScaled(g, "Rescan Area", 94, 112, 0.68f, 0xFFBAE6FD);

        // Right Control Buttons
        String speedTxt = switch (this.menu.getSpeedTier()) {
            case PoseidonAbyssalPumpBlockEntity.SPEED_TURBO -> "Speed: 4k/t";
            case PoseidonAbyssalPumpBlockEntity.SPEED_OVERDRIVE -> "Speed: 16k/t";
            default -> "Speed: 1k/t";
        };
        drawCenteredScaled(g, speedTxt, 185, 32, 0.70f, 0xFF67E8F9);

        int rad = RADII[Math.floorMod(this.menu.getRadiusIndex(), RADII.length)];
        drawCenteredScaled(g, "Radius: " + rad + "b", 185, 52, 0.70f, 0xFFE2E8F0);

        String repTxt = switch (this.menu.getReplaceMode()) {
            case PoseidonAbyssalPumpBlockEntity.REPLACE_COBBLE -> "Replace: Cobble";
            case PoseidonAbyssalPumpBlockEntity.REPLACE_NETHERRACK -> "Replace: Nether";
            case PoseidonAbyssalPumpBlockEntity.REPLACE_GLASS -> "Replace: Glass";
            default -> "Replace: Air";
        };
        drawCenteredScaled(g, repTxt, 185, 72, 0.68f, 0xFFFDE68A);

        String fltTxt = switch (this.menu.getFilterMode()) {
            case PoseidonAbyssalPumpBlockEntity.FILTER_WATER -> "Filter: Water";
            case PoseidonAbyssalPumpBlockEntity.FILTER_LAVA -> "Filter: Lava";
            default -> "Filter: All";
        };
        drawCenteredScaled(g, fltTxt, 185, 92, 0.70f, 0xFF93C5FD);

        String rsTxt = switch (this.menu.getRedstoneMode()) {
            case PipeConfigurable.REDSTONE_HIGH -> "RS: High";
            case PipeConfigurable.REDSTONE_LOW -> "RS: Low";
            case PipeConfigurable.REDSTONE_OFF -> "RS: Off";
            default -> "RS: Always";
        };
        drawCenteredScaled(g, rsTxt, 185, 112, 0.70f, 0xFFFCA5A5);

        // Bottom Quantum Link Bar
        drawCenteredScaled(g, this.menu.isQuantumEnabled() ? "Q-Link:ON" : "Q-Link:OFF", 37, 143, 0.66f, this.menu.isQuantumEnabled() ? 0xFFE9D5FF : 0xFF94A3B8);
        String cName = this.menu.getQuantumColor().getName().toUpperCase();
        if (cName.length() > 6) cName = cName.substring(0, 6);
        drawCenteredScaled(g, cName, 98, 143, 0.65f, 0xFFE2E8F0);
        drawCenteredScaled(g, "-", 129, 143, 0.75f, 0xFFE2E8F0);
        drawCenteredScaled(g, "Ch " + this.menu.getQuantumChannel(), 149, 143, 0.68f, 0xFFC084FC);
        drawCenteredScaled(g, "+", 169, 143, 0.75f, 0xFFE2E8F0);
        drawCenteredScaled(g, this.menu.isQuantumPrivate() ? "Private" : "Public", 202, 143, 0.66f, this.menu.isQuantumPrivate() ? 0xFFF9A8D4 : 0xFF5EEAD4);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int i = (this.width - this.imageWidth) / 2;
            int j = (this.height - this.imageHeight) / 2;

            if (!this.menu.isFormed()) {
                if (mouseX >= i + 136 && mouseX <= i + 180 && mouseY >= j + 6 && mouseY <= j + 18) { sendButton(0); return true; }
                if (mouseX >= i + 182 && mouseX <= i + 230 && mouseY >= j + 6 && mouseY <= j + 18) { sendButton(1); return true; }
            }
            // Right buttons
            if (mouseX >= i + 142 && mouseX <= i + 228) {
                if (mouseY >= j + 26 && mouseY <= j + 43) { sendButton(2); return true; }
                if (mouseY >= j + 46 && mouseY <= j + 63) { sendButton(3); return true; }
                if (mouseY >= j + 66 && mouseY <= j + 83) { sendButton(4); return true; }
                if (mouseY >= j + 86 && mouseY <= j + 103) { sendButton(5); return true; }
                if (mouseY >= j + 106 && mouseY <= j + 123) { sendButton(6); return true; }
            }
            // Rescan button
            if (mouseX >= i + 60 && mouseX <= i + 128 && mouseY >= j + 108 && mouseY <= j + 121) { sendButton(12); return true; }
            // Quantum Link bar
            if (mouseY >= j + 138 && mouseY <= j + 154) {
                if (mouseX >= i + 12 && mouseX <= i + 62) { sendButton(7); return true; }
                if (mouseX >= i + 66 && mouseX <= i + 118) { sendButton(8); return true; }
                if (mouseX >= i + 122 && mouseX <= i + 136) { sendButton(10); return true; }
                if (mouseX >= i + 162 && mouseX <= i + 176) { sendButton(9); return true; }
                if (mouseX >= i + 180 && mouseX <= i + 224) { sendButton(11); return true; }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        if (mouseX >= i + 9 && mouseX <= i + 25 && mouseY >= j + 29 && mouseY <= j + 125) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal("§b⚡ Abyssal Pump Energy"));
            tip.add(Component.literal(String.format("§f%,d / %,d RF", this.menu.getEnergyKrf() * 1000L, PoseidonAbyssalPumpBlockEntity.MAX_ENERGY)));
            g.renderTooltip(this.font, tip, Optional.empty(), mouseX, mouseY);
        } else if (mouseX >= i + 29 && mouseX <= i + 45 && mouseY >= j + 29 && mouseY <= j + 125) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal("§3🌊 Internal Fluid Buffer"));
            tip.add(Component.literal(String.format("§f%,d / %,d mB", this.menu.getFluidMb(), PoseidonAbyssalPumpBlockEntity.MAX_FLUID_MB)));
            tip.add(Component.literal("§7Auto-ejects to adjacent pipes & tanks from all 8 blocks."));
            g.renderTooltip(this.font, tip, Optional.empty(), mouseX, mouseY);
        }
    }
}
