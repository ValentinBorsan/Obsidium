package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.VolcaniteLiquidStorageTankMenu;

public class VolcaniteLiquidStorageTankScreen extends AbstractContainerScreen<VolcaniteLiquidStorageTankMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/liquid_storage_tank_gui.png");
    private static final ResourceLocation LAVA_OVERLAY = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/lava_overlay.png");
    private static final ResourceLocation WATER_OVERLAY = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/water_overlay.png");
    private static final ResourceLocation STEAM_OVERLAY = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/steam_overlay.png");
    private static final ResourceLocation ENERGY_OVERLAY = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/energy_overlay.png");

    public VolcaniteLiquidStorageTankScreen(VolcaniteLiquidStorageTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, 0xFFD166, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 0xCECAC5, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        // Tooltip for Fluid Gauge at (54, 20) size 16x50
        if (mouseX >= i + 54 && mouseX <= i + 70 && mouseY >= j + 20 && mouseY <= j + 70) {
            int amount = this.menu.getFluidAmount();
            int maxCap = this.menu.getMaxCapacity();
            int fluidType = this.menu.getFluidTypeId();
            String typeName = switch (fluidType) {
                case 1 -> "§6Lava";
                case 2 -> "§9Water";
                case 3 -> "§dPyroclastic Distillate";
                case 4 -> "§bHigh-Pressure Steam";
                default -> "§7Empty";
            };
            guiGraphics.renderTooltip(this.font, Component.literal(typeName + "§f: " + String.format("%,d", amount) + " / " + String.format("%,d", maxCap) + " mB"), mouseX, mouseY);
        }

        // Tooltip for Energy Gauge at (146, 20) size 16x50
        if (mouseX >= i + 146 && mouseX <= i + 162 && mouseY >= j + 20 && mouseY <= j + 70) {
            guiGraphics.renderTooltip(this.font, Component.literal(String.format("%,d", this.menu.getEnergy()) + " / 50,000 RF"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        VolcaniteGuiTheme.renderExteriorFrame(guiGraphics, i, j, this.imageWidth, this.imageHeight, true);

        guiGraphics.blit(GUI_TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        // Render Fluid Overlay
        int amount = this.menu.getFluidAmount();
        int maxCap = this.menu.getMaxCapacity();
        int fluidType = this.menu.getFluidTypeId();
        if (amount > 0 && maxCap > 0) {
            int height = (int) (50.0f * ((float) amount / maxCap));
            height = Math.max(1, Math.min(50, height));
            if (fluidType == 3) {
                guiGraphics.fill(i + 54, j + 70 - height, i + 70, j + 70, 0xFFD90467);
                guiGraphics.fill(i + 56, j + 70 - height, i + 68, j + 70, 0xFFFF4D94);
            } else {
                ResourceLocation overlay = (fluidType == 4) ? STEAM_OVERLAY : ((fluidType == 2) ? WATER_OVERLAY : LAVA_OVERLAY);
                guiGraphics.blit(overlay, i + 54, j + 70 - height, 0, 50 - height, 16, height, 16, 50);
            }
        }

        // Render Energy Overlay
        int energy = this.menu.getEnergy();
        if (energy > 0) {
            int height = (int) (50.0f * ((float) energy / 50000));
            height = Math.max(1, Math.min(50, height));
            guiGraphics.blit(ENERGY_OVERLAY, i + 146, j + 70 - height, 0, 50 - height, 16, height, 16, 50);
        }
    }
}
