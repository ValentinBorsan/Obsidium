package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.VolcaniteGrinderMenu;

public class VolcaniteGrinderScreen extends AbstractContainerScreen<VolcaniteGrinderMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/grinder_gui.png");
    private static final ResourceLocation ENERGY_OVERLAY = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/energy_overlay.png");
    private static final ResourceLocation ARROW_OVERLAY = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/arrow_overlay.png");

    public VolcaniteGrinderScreen(VolcaniteGrinderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 30;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
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

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        
        // Tooltip for Energy (10, 20) size 16x50
        if (mouseX >= i + 10 && mouseX <= i + 26 && mouseY >= j + 20 && mouseY <= j + 70) {
            guiGraphics.renderTooltip(this.font, Component.literal(this.menu.getEnergyAmount() + " / 100000 RF"), mouseX, mouseY);
        }

        // Tooltip for Upgrade Slot (150, 35) size 18x18
        if (mouseX >= i + 149 && mouseX <= i + 167 && mouseY >= j + 34 && mouseY <= j + 52) {
            if (this.menu.slots.get(2).getItem().isEmpty()) {
                guiGraphics.renderTooltip(this.font, Component.literal("§6Overdrive Upgrade Socket\n§7Accepts Overdrive Modules & Conduit Upgrades."), mouseX, mouseY);
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

        // Render Energy Bar
        int maxEnergy = 100000;
        int energyAmount = this.menu.getEnergyAmount();
        if (energyAmount > 0) {
            int height = (int) (50.0f * ((float) energyAmount / maxEnergy));
            guiGraphics.blit(ENERGY_OVERLAY, i + 10, j + 70 - height, 0, 50 - height, 16, height, 16, 50);
        }

        // Render Progress Arrow
        int progress = this.menu.getProgress();
        if (progress > 0) {
            int width = (int) (24.0f * ((float) progress / 100.0f));
            guiGraphics.blit(ARROW_OVERLAY, i + 79, j + 35, 0, 0, width, 17, 24, 17);
        }

        // Render Upgrade Slot frame at (149, 34)
        boolean hasUpgrade = !this.menu.slots.get(2).getItem().isEmpty();
        VolcaniteGuiTheme.renderUpgradeSlot(guiGraphics, i + 149, j + 34, hasUpgrade);
    }
}
