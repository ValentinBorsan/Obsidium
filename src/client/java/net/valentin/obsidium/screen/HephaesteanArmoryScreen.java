package net.valentin.obsidium.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.menu.HephaesteanArmoryMenu;

import java.util.List;

public class HephaesteanArmoryScreen extends AbstractContainerScreen<HephaesteanArmoryMenu> {

    public HephaesteanArmoryScreen(HephaesteanArmoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 200;
        this.imageHeight = 180;
        this.titleLabelX = 1000; // Custom drawn
        this.inventoryLabelX = 1000;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Base window shadow and deep titanium charcoal casing
        guiGraphics.fill(x - 1, y - 1, x + this.imageWidth + 1, y + this.imageHeight + 1, 0xFF080A0E);
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF14161D);

        // Metallic perimeter bezel
        guiGraphics.fill(x, y, x + this.imageWidth, y + 1, 0xFF2A3140);
        guiGraphics.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF181C24);
        guiGraphics.fill(x, y, x + 1, y + this.imageHeight, 0xFF2A3140);
        guiGraphics.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, 0xFF181C24);

        // Hephaestean gold corner pins (2x2)
        int pinColor = 0xFFF59E0B;
        guiGraphics.fill(x + 1, y + 1, x + 3, y + 3, pinColor);
        guiGraphics.fill(x + this.imageWidth - 3, y + 1, x + this.imageWidth - 1, y + 3, pinColor);
        guiGraphics.fill(x + 1, y + this.imageHeight - 3, x + 3, y + this.imageHeight - 1, pinColor);
        guiGraphics.fill(x + this.imageWidth - 3, y + this.imageHeight - 3, x + this.imageWidth - 1, y + this.imageHeight - 1, pinColor);

        // Top Header Bar
        guiGraphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + 15, 0xFF1B1E28);
        guiGraphics.fill(x + 1, y + 15, x + this.imageWidth - 1, y + 16, 0xFF282E3E);

        // Armor Section Casing (Slots 0..3 at x+12, y=18,36,54,72)
        guiGraphics.fill(x + 10, y + 16, x + 30, y + 90, 0xFF191322);
        guiGraphics.fill(x + 10, y + 16, x + 30, y + 17, 0xFF7C3AED);

        // Tool Section Casing (Slots 4..8 at x+40/58, y=18,36,54)
        guiGraphics.fill(x + 38, y + 16, x + 76, y + 72, 0xFF101924);
        guiGraphics.fill(x + 38, y + 16, x + 76, y + 17, 0xFF0284C7);

        // Vault Section Casing (Slots 9..26 at x+84..192, y=18..72)
        guiGraphics.fill(x + 82, y + 16, x + 194, y + 72, 0xFF161922);
        guiGraphics.fill(x + 82, y + 16, x + 194, y + 17, 0xFFF59E0B);

        // Slot Backgrounds & Borders
        for (var slot : this.menu.slots) {
            int sx = x + slot.x;
            int sy = y + slot.y;
            int borderColor = 0xFF242A36;
            int innerColor = 0xFF0C0E12;

            if (slot.index < 4) {
                // Armor slots
                borderColor = 0xFF6D28D9;
                innerColor = 0xFF171120;
            } else if (slot.index < 9) {
                // Tool slots
                borderColor = 0xFF0369A1;
                innerColor = 0xFF0E1720;
            } else if (slot.index < 27) {
                // Vault slots
                borderColor = 0xFF334155;
                innerColor = 0xFF0F1117;
            }

            drawSlotFrame(guiGraphics, sx, sy, borderColor, innerColor);
        }
    }

    private static void drawSlotFrame(GuiGraphics g, int sx, int sy, int borderColor, int innerColor) {
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, borderColor);
        g.fill(sx, sy, sx + 16, sy + 16, innerColor);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Top Header Title - zero collisions
        guiGraphics.drawString(this.font, Component.literal("§6⚒ §fHephaestean Armory"), 8, 4, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, Component.literal("§e27 Slots"), 150, 4, 0xFBBF24, false);

        // Inventory section label positioned comfortably above the player inventory slots (y=98)
        guiGraphics.drawString(this.font, Component.literal("§8INVENTORY"), 18, 87, 0x64748B, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Tooltips on empty dedicated slots
        if (this.menu.getSlot(0).getItem().isEmpty() && isHovering(12, 18, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal("§dHelmet Slot")), mouseX, mouseY);
        } else if (this.menu.getSlot(1).getItem().isEmpty() && isHovering(12, 36, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal("§dChestplate Slot")), mouseX, mouseY);
        } else if (this.menu.getSlot(2).getItem().isEmpty() && isHovering(12, 54, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal("§dLeggings Slot")), mouseX, mouseY);
        } else if (this.menu.getSlot(3).getItem().isEmpty() && isHovering(12, 72, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal("§dBoots Slot")), mouseX, mouseY);
        } else if (this.menu.getSlot(4).getItem().isEmpty() && isHovering(40, 18, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal("§bSword / Weapon Slot")), mouseX, mouseY);
        } else if (this.menu.getSlot(5).getItem().isEmpty() && isHovering(40, 36, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal("§bPickaxe Slot")), mouseX, mouseY);
        } else if (this.menu.getSlot(6).getItem().isEmpty() && isHovering(40, 54, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal("§bAxe Slot")), mouseX, mouseY);
        } else if (this.menu.getSlot(7).getItem().isEmpty() && isHovering(58, 36, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal("§bShovel Slot")), mouseX, mouseY);
        } else if (this.menu.getSlot(8).getItem().isEmpty() && isHovering(58, 54, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal("§bHoe / Paxel Slot")), mouseX, mouseY);
        }
    }
}
