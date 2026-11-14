package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.menu.AdvancedBackpackMenu;

import java.util.ArrayList;
import java.util.List;

public class AdvancedBackpackScreen extends AbstractContainerScreen<AdvancedBackpackMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/advanced_backpack_gui.png");

    public AdvancedBackpackScreen(AdvancedBackpackMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 234;
        this.imageHeight = 222;
        this.titleLabelX = 65;
        this.titleLabelY = 9;
        this.inventoryLabelX = 65;
        this.inventoryLabelY = 129;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFD700, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xCECAC5, false);

        // Sidebar section labels
        guiGraphics.drawString(this.font, "ARM", 13, 9, 0xBA68C8, false);
        guiGraphics.drawString(this.font, "TOOL", 33, 9, 0x4FC3F7, false);
        guiGraphics.drawString(this.font, "RF", 28, 105, 0x00E5FF, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Energy Gauge Tooltip
        if (mouseX >= x + 11 && mouseX <= x + 55 && mouseY >= y + 114 && mouseY <= y + 124) {
            long stored = this.menu.getEnergyAmount();
            long max = this.menu.getMaxEnergy();
            float pct = max > 0 ? (float) stored / max * 100.0f : 0.0f;

            List<Component> energyTooltip = new ArrayList<>();
            energyTooltip.add(Component.literal("§b⚡ Quantum Buffer"));
            energyTooltip.add(Component.literal("§e" + String.format("%,d", stored) + " / " + String.format("%,d", max) + " RF §7(" + String.format("%.1f", pct) + "%)"));
            energyTooltip.add(Component.literal("§7Input/Output: §a25,000 RF/t"));
            energyTooltip.add(Component.literal("§8Place a Portable Battery above to charge"));
            guiGraphics.renderComponentTooltip(this.font, energyTooltip, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        VolcaniteGuiTheme.renderExteriorFrame(guiGraphics, x, y, this.imageWidth, this.imageHeight, true);

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        // Render dynamic energy gauge bar (x=12 to 54, y=115 to 123, width 43, height 8)
        long stored = this.menu.getEnergyAmount();
        long max = this.menu.getMaxEnergy();
        if (stored > 0 && max > 0) {
            int barWidth = (int) (43.0f * ((float) stored / max));
            barWidth = Math.max(1, Math.min(43, barWidth));
            // Cyan/electric blue fill with gold gradient tip
            int color1 = 0xFF00B4D8;
            int color2 = 0xFF90E0EF;
            guiGraphics.fillGradient(x + 12, y + 115, x + 12 + barWidth, y + 123, color1, color2);
        }
    }
}

