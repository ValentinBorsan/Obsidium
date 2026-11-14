package net.valentin.obsidium.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.block.DionysianAgingBarrelMenu;

public class DionysianAgingBarrelScreen extends AbstractContainerScreen<DionysianAgingBarrelMenu> {

    public DionysianAgingBarrelScreen(DionysianAgingBarrelMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 1. Dark Oak & Dionysian Wine Wood Frame
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF1C120C);
        guiGraphics.renderOutline(x, y, this.imageWidth, this.imageHeight, 0xFF92400E); // Warm Oak outline
        guiGraphics.renderOutline(x + 1, y + 1, this.imageWidth - 2, this.imageHeight - 2, 0xFFB45309);

        // 2. Machine Panel (upper section)
        guiGraphics.fill(x + 6, y + 6, x + 170, y + 78, 0xFF2B1810);
        guiGraphics.renderOutline(x + 6, y + 6, 164, 72, 0xFF78350F);

        // 3. Player Inventory Panel (lower section)
        guiGraphics.fill(x + 6, y + 80, x + 170, y + 162, 0xFF170F0A);
        guiGraphics.renderOutline(x + 6, y + 80, 164, 82, 0xFF451A03);

        // 4. Slots
        for (var slot : this.menu.slots) {
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            guiGraphics.fill(sx, sy, sx + 18, sy + 18, 0xFF120B07);
            guiGraphics.renderOutline(sx, sy, 18, 18, 0xFF78350F);
        }
        // Output slot highlight
        guiGraphics.renderOutline(x + 115, y + 35, 18, 18, 0xFFF59E0B);

        // 5. Animated Aging Progress Track (oak cask staves)
        int progW = this.menu.getAgingProgressScaled(28);
        int progX = x + 79;
        int progY = y + 42;
        guiGraphics.fill(progX, progY, progX + 28, progY + 6, 0xFF3B1D11);
        if (progW > 0) {
            guiGraphics.fill(progX, progY, progX + progW, progY + 6, 0xFFD97706);
            guiGraphics.fill(progX, progY + 1, progX + progW, progY + 4, 0xFFFBBF24);
        }
        guiGraphics.renderOutline(progX, progY, 28, 6, 0xFF92400E);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, 0xFFFDE68A, false);
        guiGraphics.drawString(this.font, "§7Tuică", 55, 24, 0xFFFCD34D, false);
        guiGraphics.drawString(this.font, "§6Aged", 116, 24, 0xFFF59E0B, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xFFD97706, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        if (mouseX >= x + 79 && mouseX <= x + 107 && mouseY >= y + 40 && mouseY <= y + 50) {
            int max = this.menu.getMaxAgingTime();
            int pct = max > 0 ? (this.menu.getAgingProgress() * 100 / max) : 0;
            guiGraphics.renderTooltip(this.font, Component.literal("Învechire: " + pct + "%"), mouseX, mouseY);
        }
    }
}

