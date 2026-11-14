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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.LiquidTankMenu;

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
    }
}
