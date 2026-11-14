package net.valentin.obsidium.client;

import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.block.DionysianBioCalderaMenu;

import java.util.List;

public class DionysianBioCalderaScreen extends AbstractContainerScreen<DionysianBioCalderaMenu> {

    private float impellerAngle = 0f;
    private float impellerSpeed = 0f;
    private long lastFrameMs = System.currentTimeMillis();

    public DionysianBioCalderaScreen(DionysianBioCalderaMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 73;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        Component titleComp = this.title;
        if (titleComp == null || titleComp.getString().startsWith("block.obsidium.")) {
            titleComp = Component.literal("Dionysian Bio-Caldera");
        }
        g.drawString(this.font, titleComp, this.titleLabelX, this.titleLabelY, 0xFF88FF88, false);
        g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFCCCCCC, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - lastFrameMs) / 1000f);
        lastFrameMs = now;

        boolean fermenting = this.menu.isFermenting();
        float targetSpeed = fermenting ? 260f : 0f;
        impellerSpeed += (targetSpeed - impellerSpeed) * Math.min(1f, dt * 1.8f);
        impellerAngle = (impellerAngle + impellerSpeed * dt) % 360f;

        // 1. Exterior chassis - Dionysian Emerald & Amber Bronze
        g.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF0F1A14);
        g.renderOutline(x, y, this.imageWidth, this.imageHeight, 0xFF10B981);
        g.renderOutline(x + 1, y + 1, this.imageWidth - 2, this.imageHeight - 2, 0xFF065F46);

        // 2. Machine Panel
        g.fill(x + 6, y + 16, x + 170, y + 71, 0xFF14241B);
        g.renderOutline(x + 6, y + 16, 164, 55, 0xFF064E3B);

        // 3. Player Inventory Panel
        g.fill(x + 6, y + 81, x + 170, y + 160, 0xFF111D16);
        g.renderOutline(x + 6, y + 81, 164, 79, 0xFF064E3B);

        // 4. Vertical Energy Bar on the left (x = 9, y = 18, w = 12, h = 50) - Distinct Amber / Gold
        int energyH = this.menu.getEnergyScaled(50);
        int barX = x + 9;
        int barY = y + 18;
        g.fill(barX, barY, barX + 12, barY + 50, 0xFF18120B);
        if (energyH > 0) {
            g.fillGradient(barX + 1, barY + (50 - energyH), barX + 11, barY + 50, 0xFFFBBF24, 0xFFD97706);
            if (fermenting) {
                int sparkY = barY + 50 - (int) ((now / 22) % Math.max(1, energyH));
                g.fill(barX + 1, sparkY, barX + 11, sparkY + 1, 0xFFFFFBEB);
            }
        }
        g.renderOutline(barX, barY, 12, 50, 0xFFB45309);

        // 4b. Vertical Biogas Tank (x = 24, y = 18, w = 12, h = 50) - Lush Emerald Green
        int bioH = this.menu.getBiogasScaled(50);
        int tankX = x + 24;
        g.fill(tankX, barY, tankX + 12, barY + 50, 0xFF0B140E);
        if (bioH > 0) {
            g.fillGradient(tankX + 1, barY + (50 - bioH), tankX + 11, barY + 50, 0xFF34D399, 0xFF059669);
            if (fermenting) {
                for (int b = 0; b < 3; b++) {
                    int bx = tankX + 2 + (b * 3) % 8;
                    int by = barY + 48 - (int) (((now / 30) + b * 16) % Math.max(1, bioH));
                    g.fill(bx, by, bx + 2, by + 2, 0xFFF0FDF4);
                }
            }
        }
        g.renderOutline(tankX, barY, 12, 50, 0xFF047857);

        // 5. Draw all slot backgrounds with exact slot alignment
        for (var slot : this.menu.slots) {
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, 0xFF0C1610);
            g.renderOutline(sx, sy, 18, 18, 0xFF2D5A40);
        }

        // Custom borders around machine slots
        g.renderOutline(x + 43, y + 20, 18, 18, 0xFF10B981); // Slot 0: Biomass Input (Green)
        g.renderOutline(x + 43, y + 48, 18, 18, 0xFFF59E0B); // Slot 1: Fertilizer Output (Amber Gold)
        g.renderOutline(x + 149, y + 48, 18, 18, 0xFF00E5FF); // Slot 2: Battery slot (Cyan)

        // Tiny charge icon if battery slot is empty
        if (!this.menu.slots.get(2).hasItem()) {
            g.drawString(this.font, "⚡", x + 155, y + 53, 0x5500E5FF, false);
        }

        // 6. Central Biothermal Fermentation Viewport (x = 66, y = 18, w = 78, h = 50)
        int mechX = x + 66;
        int mechY = y + 18;
        int mechW = 78;
        int mechH = 50;
        g.fill(mechX, mechY, mechX + mechW, mechY + mechH, 0xFF07150E);
        g.renderOutline(mechX, mechY, mechW, mechH, 0xFF065F46);

        // Circular vat porthole / glass rim (diameter 32px, centered at mechX + 22, mechY + 25)
        int centerVX = mechX + 22;
        int centerVY = mechY + 25;
        g.fill(centerVX - 16, centerVY - 16, centerVX + 16, centerVY + 16, 0xFF092415);
        g.renderOutline(centerVX - 16, centerVY - 16, 32, 32, fermenting ? 0xFF10B981 : 0xFF2D5A40);

        // Dynamic bubbling organic bio-slurry when fermenting
        if (fermenting) {
            g.fillGradient(centerVX - 14, centerVY - 4, centerVX + 14, centerVY + 14, 0xFF14532D, 0xFF166534);
            for (int b = 0; b < 4; b++) {
                int bx = centerVX - 10 + ((b * 7) % 20);
                int by = centerVY + 12 - (int) (((now / 35) + b * 13) % 16);
                g.fill(bx, by, bx + 2, by + 2, 0xFFBBF7D0);
            }
        }

        // Rotating 4-blade impeller turbine
        g.pose().pushPose();
        g.pose().translate(centerVX, centerVY, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(impellerAngle));
        for (int i = 0; i < 4; i++) {
            g.pose().pushPose();
            g.pose().mulPose(Axis.ZP.rotationDegrees(i * 90f));
            int bladeCol = fermenting ? 0xFF34D399 : 0xFF059669;
            int bladeEdge = fermenting ? 0xFF6EE7B7 : 0xFF047857;
            g.fill(2, -2, 12, 1, bladeCol);
            g.fill(9, 1, 14, 3, bladeEdge);
            g.pose().popPose();
        }
        // Amber Bronze hub pin
        g.fill(-3, -3, 3, 3, 0xFFD97706);
        g.fill(-1, -1, 1, 1, 0xFFFFFFFF);
        g.pose().popPose();

        // Right side of viewport: Biogas Pressure Gauge & Generation Text
        int dashVX = mechX + 43;
        g.drawString(this.font, "Biogas", dashVX, y + 21, 0xFF86EFAC, false);

        int gaugeW = 31;
        int gaugeH = 6;
        g.fill(dashVX, y + 31, dashVX + gaugeW, y + 31 + gaugeH, 0xFF0A1B12);
        g.renderOutline(dashVX, y + 31, gaugeW, gaugeH, 0xFF047857);

        float gasPct = this.menu.getMaxBiogas() > 0 ? (float) this.menu.getBiogasAmount() / this.menu.getMaxBiogas() : 0f;
        int litW = (int) (gasPct * (gaugeW - 2));
        if (litW > 0) {
            g.fill(dashVX + 1, y + 32, dashVX + 1 + litW, y + 30 + gaugeH, litW > 22 ? 0xFFF59E0B : 0xFF22C55E);
        }

        int rf = this.menu.getCurrentRfPerTick();
        String rfText = rf > 0 ? "+" + rf : "IDLE";
        int rfColor = rf > 0 ? 0xFF34D399 : 0xFF6B7280;
        g.drawString(this.font, rfText, dashVX, y + 41, rfColor, false);

        // 7. Right Dashboard: BIO indicator above battery slot (x = 147, y = 17)
        int dashX = x + 147;
        g.drawString(this.font, "BIO", dashX + 2, y + 17, 0xFF34D399, false);

        // Mini 4-cell Bio-activity gauge
        for (int c = 0; c < 4; c++) {
            int cx0 = dashX + c * 5;
            boolean lit = fermenting && (c < 3 || Math.sin(now / 150.0) > 0);
            g.fill(cx0, y + 27, cx0 + 4, y + 33, lit ? 0xFF10B981 : 0xFF1F2937);
        }

        g.drawString(this.font, "RF/t", dashX + 1, y + 36, rf > 0 ? 0xFF88FF88 : 0xFF71717A, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Tooltip for Energy bar
        int barX = x + 9;
        int barY = y + 18;
        if (mouseX >= barX && mouseX <= barX + 12 && mouseY >= barY && mouseY <= barY + 50) {
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6Bio-Energy Buffer:"),
                    Component.literal("§e" + String.format("%,d", this.menu.getEnergy()) + " §7/ §6" + String.format("%,d", this.menu.getMaxEnergy()) + " RF")
            ), mouseX, mouseY);
        }

        // Tooltip for Biogas tank
        int tankX = x + 24;
        if (mouseX >= tankX && mouseX <= tankX + 12 && mouseY >= barY && mouseY <= barY + 50) {
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("§2Internal Biogas Tank:"),
                    Component.literal("§a" + String.format("%,d", this.menu.getBiogasAmount()) + " §7/ §2" + String.format("%,d", this.menu.getMaxBiogas()) + " mB")
            ), mouseX, mouseY);
        }

        // Tooltip for Fermentation Vat viewport
        if (mouseX >= x + 66 && mouseX <= x + 144 && mouseY >= y + 18 && mouseY <= y + 68) {
            if (this.menu.isFermenting()) {
                g.renderComponentTooltip(this.font, List.of(
                        Component.literal("§2🌀 Biothermal Impeller: §aCHURNING"),
                        Component.literal("§7Digestion Rate: §a+" + this.menu.getCurrentRfPerTick() + " RF/t"),
                        Component.literal("§8Thermophilic digestion producing Biogas & Fertilizer")
                ), mouseX, mouseY);
            } else {
                g.renderComponentTooltip(this.font, List.of(
                        Component.literal("§7🌀 Biothermal Impeller: §cIDLE"),
                        Component.literal("§8Insert biomass (crops, fruit, plants) to ferment")
                ), mouseX, mouseY);
            }
        }

        // Tooltip for Battery slot (Slot 2)
        int batX = x + 149;
        int batY = y + 48;
        if (mouseX >= batX && mouseX <= batX + 18 && mouseY >= batY && mouseY <= batY + 18 && !this.menu.slots.get(2).hasItem()) {
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("§bEnergy Storage Slot"),
                    Component.literal("§7Insert Battery or rechargeable item")
            ), mouseX, mouseY);
        }
    }
}
