package net.valentin.obsidium.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.block.DemetrianBiomassGeneratorMenu;

public class DemetrianBiomassGeneratorScreen extends AbstractContainerScreen<DemetrianBiomassGeneratorMenu> {

    private float helixAngle = 0f;
    private long lastFrameMs = System.currentTimeMillis();

    public DemetrianBiomassGeneratorScreen(DemetrianBiomassGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - lastFrameMs) / 1000f);
        lastFrameMs = now;

        boolean lit = this.menu.isLit();
        if (lit) {
            helixAngle = (helixAngle + 180f * dt) % 360f;
        }

        // 1. Main Background frame - Demetrian Emerald & Volcanic Bronze
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF101C14);
        guiGraphics.renderOutline(x, y, this.imageWidth, this.imageHeight, 0xFF10B981);
        guiGraphics.renderOutline(x + 1, y + 1, this.imageWidth - 2, this.imageHeight - 2, 0xFFF59E0B);

        // 2. Machine Panel (upper section)
        guiGraphics.fill(x + 6, y + 6, x + 170, y + 78, 0xFF16291C);
        guiGraphics.renderOutline(x + 6, y + 6, 164, 72, 0xFF065F46);

        // 3. Player Inventory Panel (lower section)
        guiGraphics.fill(x + 6, y + 80, x + 170, y + 162, 0xFF121E16);
        guiGraphics.renderOutline(x + 6, y + 80, 164, 82, 0xFF064E3B);

        // 4. Vertical Energy Bar on the left (x = 12, y = 18, w = 12, h = 50)
        int energyH = this.menu.getEnergyScaled(50);
        int barX = x + 12;
        int barY = y + 18;
        guiGraphics.fill(barX, barY, barX + 12, barY + 50, 0xFF0D1711);
        if (energyH > 0) {
            guiGraphics.fillGradient(barX + 1, barY + (50 - energyH), barX + 11, barY + 50, 0xFF34D399, 0xFF059669);
            if (lit) {
                int sparkY = barY + 50 - (int) ((now / 20) % Math.max(1, energyH));
                guiGraphics.fill(barX + 1, sparkY, barX + 11, sparkY + 1, 0xFFECFDF5);
            }
        }
        guiGraphics.renderOutline(barX, barY, 12, 50, 0xFF059669);

        // 4b. Vertical Biogas Fluid Reservoir (x = 28, y = 18, w = 12, h = 50)
        int bioH = this.menu.getBiogasScaled(50);
        int tankX = x + 28;
        guiGraphics.fill(tankX, barY, tankX + 12, barY + 50, 0xFF0A140F);
        if (bioH > 0) {
            guiGraphics.fillGradient(tankX + 1, barY + (50 - bioH), tankX + 11, barY + 50, 0xFF4ADE80, 0xFF15803D);
            if (lit) {
                for (int b = 0; b < 2; b++) {
                    int bx = tankX + 2 + (b * 4) % 8;
                    int by = barY + 48 - (int) (((now / 30) + b * 18) % Math.max(1, bioH));
                    guiGraphics.fill(bx, by, bx + 2, by + 2, 0xFFF0FDF4);
                }
            }
        }
        guiGraphics.renderOutline(tankX, barY, 12, 50, 0xFF166534);

        // 5. Slot backgrounds & borders
        for (var slot : this.menu.slots) {
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            guiGraphics.fill(sx, sy, sx + 18, sy + 18, 0xFF09140D);
            guiGraphics.renderOutline(sx, sy, 18, 18, 0xFF047857);
        }

        // Highlight fruit, ash and battery slots
        guiGraphics.renderOutline(x + 53, y + 34, 18, 18, 0xFF10B981); // Fruit input
        guiGraphics.renderOutline(x + 105, y + 34, 18, 18, 0xFF94A3B8); // Ash output
        guiGraphics.renderOutline(x + 146, y + 34, 18, 18, 0xFF38BDF8); // Charging slot

        // 6. Central Bio-Catalytic Digestion Chamber (x = 73, y = 20, w = 30, h = 48)
        int domeX = x + 73;
        int domeY = y + 20;
        guiGraphics.fill(domeX, domeY, domeX + 30, domeY + 48, 0xFF07150E);
        guiGraphics.renderOutline(domeX, domeY, 30, 48, lit ? 0xFF10B981 : 0xFF064E3B);

        // Rotating DNA Double Helix / Organic Enzymes
        int helixCX = domeX + 15;
        for (int step = 0; step < 8; step++) {
            float phase = (float) Math.toRadians(helixAngle + step * 45f);
            int hy = domeY + 6 + step * 5;
            int offset1 = (int) (Math.sin(phase) * 10);
            int offset2 = -offset1;

            // Rung connecting nodes
            int minX = Math.min(helixCX + offset1, helixCX + offset2);
            int maxX = Math.max(helixCX + offset1, helixCX + offset2);
            guiGraphics.fill(minX, hy + 1, maxX, hy + 2, lit ? 0xFF065F46 : 0xFF042F24);

            // Node 1 (Emerald Strand)
            int col1 = lit ? 0xFF34D399 : 0xFF059669;
            guiGraphics.fill(helixCX + offset1 - 1, hy, helixCX + offset1 + 2, hy + 3, col1);

            // Node 2 (Lime/Amber Strand)
            int col2 = lit ? 0xFFFEF08A : 0xFF65A30D;
            guiGraphics.fill(helixCX + offset2 - 1, hy, helixCX + offset2 + 2, hy + 3, col2);
        }

        // Flow of bio-spores from Fruit slot (x=71) into Dome (x=73) and into Ash slot (x=105)
        if (lit) {
            int spore1X = x + 71 + (int) ((now / 25) % 36);
            int spore1Y = y + 43 + (int) (Math.sin(now / 50.0) * 3);
            guiGraphics.fill(spore1X, spore1Y, spore1X + 2, spore1Y + 2, 0xFF86EFAC);

            // Energy arc to battery slot (x + 146)
            int arcY = y + 42 + (int) (Math.cos(now / 60.0) * 4);
            guiGraphics.fill(x + 125, arcY, x + 146, arcY + 1, 0xFF38BDF8);
        }

        // Lightning bolt icon above charging slot
        guiGraphics.drawString(this.font, "⚡", x + 152, y + 22, 0xFF38BDF8, false);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, 0xFFA7F3D0, false);
        guiGraphics.drawString(this.font, "Fuel", 58, 23, 0xFF6EE7B7, false);
        guiGraphics.drawString(this.font, "Ash", 107, 23, 0xFFCBD5E1, false);

        if (this.menu.isLit()) {
            guiGraphics.drawString(this.font, "+" + this.menu.getCurrentRfPerTick() + " RF/t", 70, 70, 0xFF4ADE80, false);
        } else {
            guiGraphics.drawString(this.font, "IDLE", 80, 70, 0xFF6B7280, false);
        }

        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xFF10B981, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Tooltip for Energy Bar
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        if (mouseX >= x + 12 && mouseX <= x + 24 && mouseY >= y + 18 && mouseY <= y + 68) {
            String energyText = String.format("%,d / %,d RF", this.menu.getEnergy(), this.menu.getCapacity());
            guiGraphics.renderTooltip(this.font, Component.literal("§a⚡ Bio-Energy Buffer: §f" + energyText), mouseX, mouseY);
        }

        // Tooltip for Biogas Reservoir
        if (mouseX >= x + 28 && mouseX <= x + 40 && mouseY >= y + 18 && mouseY <= y + 68) {
            String bioText = String.format("%,d / %,d mB", this.menu.getBiogasAmount(), this.menu.getMaxBiogas());
            guiGraphics.renderTooltip(this.font, Component.literal("§2🍃 Biogas Reservoir: §a" + bioText), mouseX, mouseY);
        }

        // Tooltip for Bio-Dome Reactor
        if (mouseX >= x + 73 && mouseX <= x + 103 && mouseY >= y + 20 && mouseY <= y + 68) {
            if (this.menu.isLit()) {
                guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                        Component.literal("§2Enzymatic Bio-Digester: §aACTIVE"),
                        Component.literal("§7Photosynthetic breakdown: §e+" + this.menu.getCurrentRfPerTick() + " RF/t")
                ), mouseX, mouseY);
            } else {
                guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                        Component.literal("§7Enzymatic Bio-Digester: §cIDLE"),
                        Component.literal("§8Insert fruit, crops, or mod tree leaves")
                ), mouseX, mouseY);
            }
        }
    }
}
