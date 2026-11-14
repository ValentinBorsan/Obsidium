package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.block.VolcaniteLavaPumpMenu;

public class VolcaniteLavaPumpScreen extends AbstractContainerScreen<VolcaniteLavaPumpMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("obsidium", "textures/gui/pump_gui.png");

    public VolcaniteLavaPumpScreen(VolcaniteLavaPumpMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 73; // Move down slightly so it doesn't overlap with our custom text
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFD166, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xCECAC5, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        VolcaniteGuiTheme.renderExteriorFrame(guiGraphics, x, y, this.imageWidth, this.imageHeight, false);
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Lava Bar (Left)
        int maxLava = 16000;
        int lavaHeight = (int) ((this.menu.getLavaAmount() / (float) maxLava) * 50);
        guiGraphics.fill(x + 40, y + 70 - lavaHeight, x + 60, y + 70, 0xFFFF5500);

        // Energy Bar (Right)
        int maxEnergy = 100000;
        int energyHeight = (int) ((this.menu.getEnergyAmount() / (float) maxEnergy) * 50);
        guiGraphics.fill(x + 116, y + 70 - energyHeight, x + 136, y + 70, 0xFF00FF88);
        
        // Text labels (using inventoryLabelX/Y makes it clean, but we can also hardcode nice offsets)
        guiGraphics.drawString(this.font, this.menu.getLavaAmount() + " mB", x + 35, y + 74, 0xFF5500, false);
        guiGraphics.drawString(this.font, this.menu.getEnergyAmount() + " RF", x + 110, y + 74, 0x00FF88, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
