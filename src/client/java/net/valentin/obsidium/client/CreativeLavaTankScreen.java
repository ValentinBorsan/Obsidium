package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.CreativeLavaTankMenu;

import java.util.List;

public class CreativeLavaTankScreen extends AbstractContainerScreen<CreativeLavaTankMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/creative_lava_tank_gui.png");

    public CreativeLavaTankScreen(CreativeLavaTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, Component.literal("Creative Lava Tank"), 8, 6, 0xFFD700, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xCECAC5, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        // Tooltip for Infinite Lava Chamber (68, 16) to (107, 72)
        if (mouseX >= i + 68 && mouseX <= i + 107 && mouseY >= j + 16 && mouseY <= j + 72) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                Component.literal("§6§lInfinite Lava Reservoir"),
                Component.literal("§eStored: §6∞ / Infinite mB Lava"),
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
                    Component.literal("§6Output Slot"),
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

        // Render main GUI texture
        guiGraphics.blit(GUI_TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        // Render animated pulsing glow over infinite lava reservoir
        long time = System.currentTimeMillis();
        float pulse = (float) ((Math.sin(time / 250.0) + 1.0) / 2.0); // 0.0 to 1.0
        int alpha = (int) (160 + 95 * pulse); // 160 to 255
        int glowColor = (alpha << 24) | 0xFF8800;

        // Render stylized glowing infinity symbol '∞' in the center of the chamber (at x=84, y=41)
        String inf = "∞";
        int infWidth = this.font.width(inf);
        int cx = i + 88 - (infWidth / 2);
        int cy = j + 40;

        // Shadow
        guiGraphics.drawString(this.font, inf, cx + 1, cy + 1, 0xAA000000, false);
        // Glowing symbol
        guiGraphics.drawString(this.font, inf, cx, cy, glowColor, false);
    }
}
