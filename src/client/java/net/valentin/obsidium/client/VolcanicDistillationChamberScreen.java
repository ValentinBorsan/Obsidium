package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.VolcanicDistillationChamberBlockEntity;
import net.valentin.obsidium.block.VolcanicDistillationChamberMenu;

import java.util.ArrayList;
import java.util.List;

public class VolcanicDistillationChamberScreen extends AbstractContainerScreen<VolcanicDistillationChamberMenu> {
    private static final ResourceLocation LAVA_OVERLAY = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/lava_overlay.png");
    private static final ResourceLocation ENERGY_OVERLAY = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/energy_overlay.png");

    public VolcanicDistillationChamberScreen(VolcanicDistillationChamberMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 1000;
        this.inventoryLabelX = 1000;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, "⚗ Volcanic Distillation", 8, 5, 0xFFE879F9, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, 74, 0xFFB8B0C2, false);

        int prog = (int) (100.0f * this.menu.getProgress() / this.menu.getMaxProgress());
        String statusText = prog > 0 ? (prog + "%") : "IDLE";
        int textColor = prog > 0 ? 0xFFF472B6 : 0xFF7D718C;
        int centerX = 81 - (this.font.width(statusText) / 2);
        guiGraphics.drawString(this.font, statusText, centerX, 58, textColor, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        // Tooltip for Primary Input Tank (30..44, 20..70)
        if (mouseX >= i + 30 && mouseX <= i + 44 && mouseY >= j + 20 && mouseY <= j + 70) {
            List<Component> lines = new ArrayList<>();
            int inType = this.menu.getInputFluidTypeId();
            String inName = switch (inType) {
                case 2 -> "§6⚗ Bio-Oil";
                case 3 -> "§a⚗ Biodiesel";
                default -> "§6🌋 Lava";
            };
            lines.add(Component.literal(inName + " §fInput Tank (Primary)"));
            lines.add(Component.literal("§e" + this.menu.getLavaAmount() + " / " + VolcanicDistillationChamberBlockEntity.MAX_LAVA + " mB"));
            lines.add(Component.literal("§7Feeds Lava, Bio-Oil, or Biodiesel"));
            guiGraphics.renderTooltip(this.font, lines, java.util.Optional.empty(), mouseX, mouseY);
        }

        // Tooltip for Secondary Input Tank (48..62, 20..70)
        if (mouseX >= i + 48 && mouseX <= i + 62 && mouseY >= j + 20 && mouseY <= j + 70) {
            List<Component> lines = new ArrayList<>();
            int secType = this.menu.getSecondaryFluidTypeId();
            String secName = switch (secType) {
                case 1 -> "§b⚗ Bio-Ethanol";
                case 2 -> "§d⚗ Pyroclastic Distillate";
                default -> "§b⚗ Ethanol / Reagent";
            };
            lines.add(Component.literal(secName + " §fTank (Secondary)"));
            lines.add(Component.literal("§b" + this.menu.getEthanolAmount() + " / " + VolcanicDistillationChamberBlockEntity.MAX_ETHANOL + " mB"));
            lines.add(Component.literal("§a✓ Auto-receives Bio-Ethanol from Dionysian Distillery"));
            lines.add(Component.literal("§7Used cleanly for Bio-Oil -> Biodiesel & Pyro-Diesel!"));
            guiGraphics.renderTooltip(this.font, lines, java.util.Optional.empty(), mouseX, mouseY);
        }

        // Tooltip for Chemical Distillation Progress (66..96, 42..66)
        if (mouseX >= i + 66 && mouseX <= i + 96 && mouseY >= j + 42 && mouseY <= j + 66) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("§d⚗ Chemical & Biofuel Refining"));
            lines.add(Component.literal("§7• Lava + Reagent -> §dPyroclastic Distillate"));
            lines.add(Component.literal("§7• 1000 mB Bio-Oil + 500 mB Bio-Ethanol -> §a1500 mB Biodiesel"));
            lines.add(Component.literal("§7• 1000 mB Biodiesel + 500 mB Pyro / Catalyst -> §cPyro-Diesel"));
            guiGraphics.renderTooltip(this.font, lines, java.util.Optional.empty(), mouseX, mouseY);
        }

        // Tooltip for Output Tank (100..116, 20..70)
        if (mouseX >= i + 100 && mouseX <= i + 116 && mouseY >= j + 20 && mouseY <= j + 70) {
            List<Component> lines = new ArrayList<>();
            int outType = this.menu.getOutputFluidTypeId();
            String outName = switch (outType) {
                case 2 -> "§a⚗ Biodiesel Output Tank";
                case 3 -> "§c⚗ Pyro-Diesel Output Tank";
                default -> "§d⚗ Pyroclastic Distillate Tank";
            };
            lines.add(Component.literal(outName));
            lines.add(Component.literal("§f" + this.menu.getDistillateAmount() + " / " + VolcanicDistillationChamberBlockEntity.MAX_DISTILLATE + " mB"));
            lines.add(Component.literal("§a✓ Auto-pumps to Generators, Chambers & Tanks!"));
            lines.add(Component.literal("§7Or place Bucket / Bottle in right slot to bottle."));
            guiGraphics.renderTooltip(this.font, lines, java.util.Optional.empty(), mouseX, mouseY);
        }

        // Tooltip for Energy Bar (148..164, 20..70)
        if (mouseX >= i + 148 && mouseX <= i + 164 && mouseY >= j + 20 && mouseY <= j + 70) {
            guiGraphics.renderTooltip(this.font, Component.literal("§bEnergy: " + this.menu.getEnergyAmount() + " / " + VolcanicDistillationChamberBlockEntity.MAX_ENERGY + " RF (250 RF/t)"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        VolcaniteGuiTheme.renderExteriorFrame(guiGraphics, i, j, this.imageWidth, this.imageHeight, true);

        // Main Obsidian Window Body
        guiGraphics.fill(i, j, i + this.imageWidth, j + this.imageHeight, 0xFF120E19);
        guiGraphics.fill(i + 1, j + 1, i + this.imageWidth - 1, j + 16, 0xFF1B1328);
        guiGraphics.fill(i + 1, j + 15, i + this.imageWidth - 1, j + 16, 0xFF4A255A);

        // Upper Laboratory Processing Bay
        guiGraphics.fill(i + 4, j + 17, i + this.imageWidth - 4, j + 72, 0xFF171220);
        guiGraphics.fill(i + 4, j + 72, i + this.imageWidth - 4, j + 73, 0xFF332345);

        // Subtle Manifold Conduit Links (Input Tanks -> Center Tube -> Output Tank)
        guiGraphics.fill(i + 44, j + 47, i + 48, j + 49, 0xFF4A255A);
        guiGraphics.fill(i + 62, j + 47, i + 66, j + 49, 0xFF4A255A);
        guiGraphics.fill(i + 96, j + 47, i + 100, j + 49, 0xFF5A1E5C);
        // Drain arrows between bucket slots (left and right)
        drawDownArrow(guiGraphics, i + 14, j + 41, 0xFFFF9E00);
        drawDownArrow(guiGraphics, i + 128, j + 41, 0xFFE879F9);

        // Machine Slot Boxes
        drawSlotBox(guiGraphics, i + 7, j + 21, 0xFFFF6D00);    // Slot 0: Primary/Secondary Bucket In
        drawSlotBox(guiGraphics, i + 7, j + 51, 0xFF5A4E68);    // Slot 1: Empty Bucket Out
        drawSlotBox(guiGraphics, i + 71, j + 19, 0xFFD946EF);   // Slot 2: Catalyst / Reagent In
        drawSlotBox(guiGraphics, i + 121, j + 21, 0xFF9B5DE5);  // Slot 3: Empty Bucket/Bottle In
        drawSlotBox(guiGraphics, i + 121, j + 51, 0xFFFF007F);  // Slot 4: Filled Output

        // Player Inventory + Hotbar Slot Boxes
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                drawSlotBox(guiGraphics, i + 7 + col * 18, j + 83 + row * 18, 0xFF2D223C);
            }
        }
        for (int col = 0; col < 9; ++col) {
            drawSlotBox(guiGraphics, i + 7 + col * 18, j + 141, 0xFF38294A);
        }

        // Tank 1: Primary Input Tank (30, 20) 14x50
        guiGraphics.fill(i + 29, j + 19, i + 45, j + 71, 0xFF4A251D);
        guiGraphics.fill(i + 30, j + 20, i + 44, j + 70, 0xFF0A080E);
        int lava = this.menu.getLavaAmount();
        if (lava > 0) {
            int height = Math.max(1, (int) (50.0f * ((float) lava / VolcanicDistillationChamberBlockEntity.MAX_LAVA)));
            int topY = j + 70 - height;
            int inType = this.menu.getInputFluidTypeId();
            if (inType == 2) {
                guiGraphics.fill(i + 30, topY, i + 44, j + 70, 0xFFB45309);
                guiGraphics.fill(i + 32, topY, i + 42, j + 70, 0xFFF59E0B);
                guiGraphics.fill(i + 30, topY, i + 44, topY + 1, 0xFFFDE68A);
            } else if (inType == 3) {
                guiGraphics.fill(i + 30, topY, i + 44, j + 70, 0xFF15803D);
                guiGraphics.fill(i + 32, topY, i + 42, j + 70, 0xFF4ADE80);
                guiGraphics.fill(i + 30, topY, i + 44, topY + 1, 0xFF86EFAC);
            } else {
                guiGraphics.blit(LAVA_OVERLAY, i + 30, topY, 0, 50 - height, 14, height, 16, 50);
            }
        }
        drawTankTicks(guiGraphics, i + 30, j + 20, 14);

        // Tank 2: Secondary Input Tank (48, 20) 14x50 (Bio-Ethanol / Pyroclastic Distillate)
        guiGraphics.fill(i + 47, j + 19, i + 63, j + 71, 0xFF164E63);
        guiGraphics.fill(i + 48, j + 20, i + 62, j + 70, 0xFF0A080E);
        int secAmt = this.menu.getEthanolAmount();
        if (secAmt > 0) {
            int height = Math.max(1, (int) (50.0f * ((float) secAmt / VolcanicDistillationChamberBlockEntity.MAX_ETHANOL)));
            int topY = j + 70 - height;
            int secType = this.menu.getSecondaryFluidTypeId();
            int cOuter = secType == 2 ? 0xFF9D174D : 0xFF0E7490;
            int cInner = secType == 2 ? 0xFFF43F5E : 0xFF22D3EE;
            int cTop   = secType == 2 ? 0xFFFDA4AF : 0xFFA5F3FC;
            guiGraphics.fill(i + 48, topY, i + 62, j + 70, cOuter);
            guiGraphics.fill(i + 50, topY, i + 60, j + 70, cInner);
            guiGraphics.fill(i + 48, topY, i + 62, topY + 1, cTop);
        }
        drawTankTicks(guiGraphics, i + 48, j + 20, 14);

        // Central Animated Distillation Progress Tube (66, 44) width 30, height 8
        guiGraphics.fill(i + 65, j + 43, i + 97, j + 53, 0xFF4A255A);
        guiGraphics.fill(i + 66, j + 44, i + 96, j + 52, 0xFF0A080E);
        int progress = this.menu.getProgress();
        if (progress > 0) {
            int fillW = Math.max(1, (int) (30.0f * ((float) progress / this.menu.getMaxProgress())));
            guiGraphics.fill(i + 66, j + 44, i + 66 + fillW, j + 52, 0xFFD946EF);
            guiGraphics.fill(i + 66, j + 44, i + 66 + fillW, j + 46, 0xFFFFB703);
            if (fillW >= 4) {
                int pulseX = (int) ((System.currentTimeMillis() / 60L) % Math.max(1, fillW - 2));
                guiGraphics.fill(i + 66 + pulseX, j + 45, i + 66 + pulseX + 2, j + 51, 0xFFFFFFFF);
            }
        }
        // Subtle status badge frame below progress tube
        guiGraphics.fill(i + 66, j + 56, i + 96, j + 68, 0xFF241A33);
        guiGraphics.fill(i + 67, j + 57, i + 95, j + 67, 0xFF0E0B14);

        // Tank 3: Output Distillate Tank (100, 20) 16x50
        guiGraphics.fill(i + 99, j + 19, i + 117, j + 71, 0xFF5A1E5C);
        guiGraphics.fill(i + 100, j + 20, i + 116, j + 70, 0xFF0A080E);
        int dist = this.menu.getDistillateAmount();
        if (dist > 0) {
            int height = Math.max(1, (int) (50.0f * ((float) dist / VolcanicDistillationChamberBlockEntity.MAX_DISTILLATE)));
            int topY = j + 70 - height;
            int outType = this.menu.getOutputFluidTypeId();
            int cOuter = outType == 2 ? 0xFF15803D : (outType == 3 ? 0xFFB91C1C : 0xFFD90467);
            int cInner = outType == 2 ? 0xFF4ADE80 : (outType == 3 ? 0xFFFB923C : 0xFFFF4D94);
            int cTop   = outType == 2 ? 0xFF86EFAC : (outType == 3 ? 0xFFFED7AA : 0xFFFBCFE8);
            guiGraphics.fill(i + 100, topY, i + 116, j + 70, cOuter);
            guiGraphics.fill(i + 102, topY, i + 114, j + 70, cInner);
            guiGraphics.fill(i + 100, topY, i + 116, topY + 1, cTop);
        }
        drawTankTicks(guiGraphics, i + 100, j + 20, 16);

        // Energy Bar (148, 20) 16x50
        guiGraphics.fill(i + 147, j + 19, i + 165, j + 71, 0xFF1D3546);
        guiGraphics.fill(i + 148, j + 20, i + 164, j + 70, 0xFF0A080E);
        int energy = this.menu.getEnergyAmount();
        if (energy > 0) {
            int height = Math.max(1, (int) (50.0f * ((float) energy / VolcanicDistillationChamberBlockEntity.MAX_ENERGY)));
            guiGraphics.blit(ENERGY_OVERLAY, i + 148, j + 70 - height, 0, 50 - height, 16, height, 16, 50);
        }
    }

    private void drawSlotBox(GuiGraphics g, int x, int y, int borderColor) {
        g.fill(x, y, x + 18, y + 18, borderColor);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF0E0B14);
    }

    private void drawTankTicks(GuiGraphics g, int x, int y, int width) {
        for (int ty = y + 10; ty < y + 48; ty += 10) {
            g.fill(x, ty, x + 4, ty + 1, 0x88FFFFFF);
            g.fill(x + width - 3, ty, x + width, ty + 1, 0x44FFFFFF);
        }
    }

    private void drawDownArrow(GuiGraphics g, int cx, int y, int color) {
        g.fill(cx + 1, y, cx + 3, y + 6, color);
        g.fill(cx, y + 5, cx + 4, y + 6, color);
        g.fill(cx + 1, y + 6, cx + 3, y + 7, color);
    }
}
