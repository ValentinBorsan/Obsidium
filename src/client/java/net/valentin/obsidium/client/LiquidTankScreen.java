package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.LiquidTankMenu;
import net.valentin.obsidium.item.QuantumLinkModuleItem;

import java.util.List;

public class LiquidTankScreen extends AbstractContainerScreen<LiquidTankMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/liquid_tank_gui.png");
    private static final ResourceLocation LAVA_OVERLAY = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/lava_overlay.png");
    private static final ResourceLocation WATER_OVERLAY = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/water_overlay.png");
    private static final ResourceLocation STEAM_OVERLAY = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/steam_overlay.png");

    public LiquidTankScreen(LiquidTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFD166, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xCECAC5, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Tooltip for Fluid Gauge at (36, 20) with width 24 and height 52
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        if (mouseX >= i + 36 && mouseX <= i + 60 && mouseY >= j + 20 && mouseY <= j + 72) {
            Fluid fluid = this.menu.getFluid();
            int amount = this.menu.getFluidAmount();
            int maxCap = this.menu.getMaxCapacity();

            if (fluid == Fluids.EMPTY || amount <= 0) {
                guiGraphics.renderTooltip(this.font, Component.literal("§7Empty §8(0 / " + String.format("%,d", maxCap) + " mB)"), mouseX, mouseY);
            } else if (fluid == ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID) {
                guiGraphics.renderTooltip(this.font, Component.literal("§d⚗ Pyroclastic Distillate §f(" + String.format("%,d", amount) + " / " + String.format("%,d", maxCap) + " mB)"), mouseX, mouseY);
            } else if (fluid == ObsidiumMod.STEAM_FLUID) {
                guiGraphics.renderTooltip(this.font, Component.literal("§b☁ High-Pressure Steam §f(" + String.format("%,d", amount) + " / " + String.format("%,d", maxCap) + " mB)"), mouseX, mouseY);
            } else if (fluid == ObsidiumMod.BIO_OIL_FLUID) {
                guiGraphics.renderTooltip(this.font, Component.literal("§6⚗ Bio-Oil §f(" + String.format("%,d", amount) + " / " + String.format("%,d", maxCap) + " mB)"), mouseX, mouseY);
            } else if (fluid == ObsidiumMod.BIO_ETHANOL_FLUID) {
                guiGraphics.renderTooltip(this.font, Component.literal("§b⚗ Bio-Ethanol §f(" + String.format("%,d", amount) + " / " + String.format("%,d", maxCap) + " mB)"), mouseX, mouseY);
            } else if (fluid == ObsidiumMod.BIODIESEL_FLUID) {
                guiGraphics.renderTooltip(this.font, Component.literal("§a⚗ Biodiesel §f(" + String.format("%,d", amount) + " / " + String.format("%,d", maxCap) + " mB)"), mouseX, mouseY);
            } else if (fluid == ObsidiumMod.PYRO_DIESEL_FLUID) {
                guiGraphics.renderTooltip(this.font, Component.literal("§c⚗ Pyro-Diesel §f(" + String.format("%,d", amount) + " / " + String.format("%,d", maxCap) + " mB)"), mouseX, mouseY);
            } else {
                FluidVariant variant = FluidVariant.of(fluid);
                List<Component> tooltip = FluidVariantRendering.getTooltip(variant);
                String fluidName = (!tooltip.isEmpty()) ? tooltip.get(0).getString() : (fluid == Fluids.LAVA ? "Lava" : "Water");
                String colorPrefix = (fluid == Fluids.LAVA) ? "§6" : (fluid == Fluids.WATER ? "§9" : "§b");
                guiGraphics.renderTooltip(this.font, Component.literal(colorPrefix + fluidName + " §f(" + String.format("%,d", amount) + " / " + String.format("%,d", maxCap) + " mB)"), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        VolcaniteGuiTheme.renderExteriorFrame(guiGraphics, i, j, this.imageWidth, this.imageHeight, false);

        guiGraphics.blit(GUI_TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        int maxCap = this.menu.getMaxCapacity();
        int amount = Math.min(this.menu.getFluidAmount(), maxCap);
        Fluid fluid = this.menu.getFluid();

        if (amount > 0 && maxCap > 0 && fluid != Fluids.EMPTY) {
            int height = (int) (52.0f * ((float) amount / maxCap));
            if (height > 0) {
                int gaugeX = i + 36;
                int gaugeY = j + 72 - height;
                int gaugeW = 24;

                if (fluid == Fluids.LAVA) {
                    guiGraphics.blit(LAVA_OVERLAY, gaugeX, gaugeY, 0, 52 - height, gaugeW, height, 24, 52);
                } else if (fluid == Fluids.WATER) {
                    guiGraphics.blit(WATER_OVERLAY, gaugeX, gaugeY, 0, 52 - height, gaugeW, height, 24, 52);
                } else if (fluid == ObsidiumMod.STEAM_FLUID) {
                    guiGraphics.blit(STEAM_OVERLAY, gaugeX, gaugeY, 0, 52 - height, gaugeW, height, 24, 52);
                } else if (fluid == ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID) {
                    guiGraphics.fill(gaugeX, gaugeY, gaugeX + gaugeW, j + 72, 0xFFD90467);
                    guiGraphics.fill(gaugeX + 3, gaugeY, gaugeX + gaugeW - 3, j + 72, 0xFFFF4D94);
                } else if (fluid == ObsidiumMod.BIO_OIL_FLUID) {
                    guiGraphics.fill(gaugeX, gaugeY, gaugeX + gaugeW, j + 72, 0xFFB45309);
                    guiGraphics.fill(gaugeX + 3, gaugeY, gaugeX + gaugeW - 3, j + 72, 0xFFF59E0B);
                } else if (fluid == ObsidiumMod.BIO_ETHANOL_FLUID) {
                    guiGraphics.fill(gaugeX, gaugeY, gaugeX + gaugeW, j + 72, 0xFF0284C7);
                    guiGraphics.fill(gaugeX + 3, gaugeY, gaugeX + gaugeW - 3, j + 72, 0xFF38BDF8);
                } else if (fluid == ObsidiumMod.BIODIESEL_FLUID) {
                    guiGraphics.fill(gaugeX, gaugeY, gaugeX + gaugeW, j + 72, 0xFF15803D);
                    guiGraphics.fill(gaugeX + 3, gaugeY, gaugeX + gaugeW - 3, j + 72, 0xFF4ADE80);
                } else if (fluid == ObsidiumMod.PYRO_DIESEL_FLUID) {
                    guiGraphics.fill(gaugeX, gaugeY, gaugeX + gaugeW, j + 72, 0xFFB91C1C);
                    guiGraphics.fill(gaugeX + 3, gaugeY, gaugeX + gaugeW - 3, j + 72, 0xFFFB923C);
                } else {
                    // Modded/custom fluid sprite rendering
                    FluidVariant variant = FluidVariant.of(fluid);
                    TextureAtlasSprite sprite = FluidVariantRendering.getSprite(variant);
                    if (sprite != null) {
                        int color = FluidVariantRendering.getColor(variant);
                        float r = ((color >> 16) & 0xFF) / 255.0f;
                        float g = ((color >> 8) & 0xFF) / 255.0f;
                        float b = (color & 0xFF) / 255.0f;
                        RenderSystem.setShaderColor(r, g, b, 1.0f);
                        guiGraphics.blit(gaugeX, gaugeY, 0, gaugeW, height, sprite);
                        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                    }
                }
            }
        }

        // Quantum Link Channel Controls (x: 122..169, y: 20..73)
        if (this.menu.isQuantumLinked()) {
            int px = i + 122;
            int py = j + 20;
            guiGraphics.fill(px, py, px + 48, py + 54, 0xFF15101D);
            guiGraphics.fill(px + 1, py + 1, px + 47, py + 53, 0xFF20182C);

            DyeColor color = this.menu.getQuantumColor();
            int rgb = 0xFF000000 | QuantumLinkModuleItem.getChannelRgb(color);
            // Color row: [<] [swatch] [>]
            drawMiniBtn(guiGraphics, px + 2, py + 3, 10, 11, "<", 0xFFD088FF, mouseX, mouseY);
            guiGraphics.fill(px + 14, py + 3, px + 34, py + 14, 0xFF000000);
            guiGraphics.fill(px + 15, py + 4, px + 33, py + 13, rgb);
            drawMiniBtn(guiGraphics, px + 36, py + 3, 10, 11, ">", 0xFFD088FF, mouseX, mouseY);

            // Channel label & buttons: [-][-10][+10][+]
            String chText = "#" + this.menu.getQuantumChannel();
            int cw = this.font.width(chText);
            guiGraphics.drawString(this.font, chText, px + 24 - cw / 2, py + 17, 0xFFE6C8FF, false);

            drawMiniBtn(guiGraphics, px + 2, py + 27, 10, 11, "-", 0xFF99CCFF, mouseX, mouseY);
            drawMiniBtn(guiGraphics, px + 13, py + 27, 10, 11, "--", 0xFF77AADD, mouseX, mouseY);
            drawMiniBtn(guiGraphics, px + 25, py + 27, 10, 11, "++", 0xFF77AADD, mouseX, mouseY);
            drawMiniBtn(guiGraphics, px + 36, py + 27, 10, 11, "+", 0xFF99CCFF, mouseX, mouseY);

            // Public / Private toggle
            boolean priv = this.menu.isQuantumPrivate();
            drawMiniBtn(guiGraphics, px + 2, py + 40, 44, 11, priv ? "PRIVATE" : "PUBLIC", priv ? 0xFFFF6688 : 0xFF66FFAA, mouseX, mouseY);
        }
    }

    private void drawMiniBtn(GuiGraphics g, int bx, int by, int bw, int bh, String text, int borderColor, int mouseX, int mouseY) {
        boolean hovered = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh;
        g.fill(bx, by, bx + bw, by + bh, hovered ? 0xFFFFFFFF : borderColor);
        g.fill(bx + 1, by + 1, bx + bw - 1, by + bh - 1, hovered ? 0xFF342648 : 0xFF1A1424);
        int tw = this.font.width(text);
        g.drawString(this.font, text, bx + (bw - tw) / 2, by + (bh - 7) / 2, hovered ? 0xFFFFFFFF : borderColor, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.menu.isQuantumLinked() && this.minecraft != null && this.minecraft.gameMode != null) {
            int i = (this.width - this.imageWidth) / 2;
            int j = (this.height - this.imageHeight) / 2;
            int px = i + 122;
            int py = j + 20;
            int btnId = -1;
            if (inBox(mouseX, mouseY, px + 2, py + 3, 10, 11)) btnId = LiquidTankMenu.BTN_PREV_COLOR;
            else if (inBox(mouseX, mouseY, px + 36, py + 3, 10, 11)) btnId = LiquidTankMenu.BTN_NEXT_COLOR;
            else if (inBox(mouseX, mouseY, px + 2, py + 27, 10, 11)) btnId = LiquidTankMenu.BTN_DEC_CH_1;
            else if (inBox(mouseX, mouseY, px + 13, py + 27, 10, 11)) btnId = LiquidTankMenu.BTN_DEC_CH_10;
            else if (inBox(mouseX, mouseY, px + 25, py + 27, 10, 11)) btnId = LiquidTankMenu.BTN_INC_CH_10;
            else if (inBox(mouseX, mouseY, px + 36, py + 27, 10, 11)) btnId = LiquidTankMenu.BTN_INC_CH_1;
            else if (inBox(mouseX, mouseY, px + 2, py + 40, 44, 11)) btnId = LiquidTankMenu.BTN_TOGGLE_PRIVATE;

            if (btnId >= 0) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, btnId);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean inBox(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
