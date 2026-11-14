package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.CreativeWaterTankMenu;

import java.util.List;

public class CreativeWaterTankScreen extends AbstractContainerScreen<CreativeWaterTankMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/creative_water_tank_gui.png");

    public CreativeWaterTankScreen(CreativeWaterTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, Component.literal("Creative Water Tank"), 8, 6, 0x00E5FF, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xCECAC5, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        // Tooltip for Infinite Water Reservoir
        if (mouseX >= i + 68 && mouseX <= i + 107 && mouseY >= j + 16 && mouseY <= j + 72) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                Component.literal("§b§lInfinite Water Reservoir"),
                Component.literal("§3Stored: §b∞ / Infinite mB Water"),
                Component.literal("§7Flow Rate: §aUnlimited (Instant)"),
                Component.literal("§dCreative Source - Infinite Supply")
            ), mouseX, mouseY);
        } else if (mouseX >= i + 43 && mouseX <= i + 61 && mouseY >= j + 34 && mouseY <= j + 52) {
            if (!this.menu.getSlot(0).hasItem()) {
                guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§bInstant Fill Slot"),
                    Component.literal("§7Place any Bucket, Bottle, or Container to fill instantly")
                ), mouseX, mouseY);
            }
        } else if (mouseX >= i + 115 && mouseX <= i + 133 && mouseY >= j + 34 && mouseY <= j + 52) {
            if (!this.menu.getSlot(1).hasItem()) {
                guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§bOutput Slot"),
                    Component.literal("§7Collect filled containers here")
                ), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        VolcaniteGuiTheme.renderExteriorFrame(guiGraphics, i, j, this.imageWidth, this.imageHeight, true);

        guiGraphics.blit(GUI_TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
    }
}
