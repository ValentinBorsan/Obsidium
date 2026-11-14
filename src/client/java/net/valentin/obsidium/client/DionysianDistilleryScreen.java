package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.block.DionysianDistilleryMenu;

import java.util.List;

public class DionysianDistilleryScreen extends AbstractContainerScreen<DionysianDistilleryMenu> {

    public DionysianDistilleryScreen(DionysianDistilleryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 1000;
        this.inventoryLabelX = 1000;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Base window shadow and deep titanium charcoal casing
        guiGraphics.fill(x - 1, y - 1, x + this.imageWidth + 1, y + this.imageHeight + 1, 0xFF080A0E);
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF14121A);

        // Metallic perimeter bezel
        guiGraphics.fill(x, y, x + this.imageWidth, y + 1, 0xFF2F263E);
        guiGraphics.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF181422);
        guiGraphics.fill(x, y, x + 1, y + this.imageHeight, 0xFF2F263E);
        guiGraphics.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, 0xFF181422);

        // Amethyst corner pins (2x2)
        int pinColor = 0xFFA855F7;
        guiGraphics.fill(x + 1, y + 1, x + 3, y + 3, pinColor);
        guiGraphics.fill(x + this.imageWidth - 3, y + 1, x + this.imageWidth - 1, y + 3, pinColor);
        guiGraphics.fill(x + 1, y + this.imageHeight - 3, x + 3, y + this.imageHeight - 1, pinColor);
        guiGraphics.fill(x + this.imageWidth - 3, y + this.imageHeight - 3, x + this.imageWidth - 1, y + this.imageHeight - 1, pinColor);

        // Top Header Bar (0..16)
        guiGraphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + 16, 0xFF1B1626);
        guiGraphics.fill(x + 1, y + 16, x + this.imageWidth - 1, y + 17, 0xFF282038);

        // Machine Panel Section (upper body)
        guiGraphics.fill(x + 6, y + 18, x + 170, y + 74, 0xFF1A1424);
        guiGraphics.fill(x + 6, y + 18, x + 170, y + 19, 0xFF2D223E);

        // 4. Slot backgrounds & slot borders
        for (var slot : this.menu.slots) {
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            guiGraphics.fill(sx, sy, sx + 18, sy + 18, 0xFF292036);
            guiGraphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF0D0B12);
        }

        // Output Slot Highlights
        guiGraphics.fill(x + 115, y + 35, x + 133, y + 53, 0xFFF59E0B);
        guiGraphics.fill(x + 116, y + 36, x + 132, y + 52, 0xFF18140E);

        guiGraphics.fill(x + 141, y + 35, x + 159, y + 53, 0xFF94A3B8);
        guiGraphics.fill(x + 142, y + 36, x + 158, y + 52, 0xFF14161A);

        // 5. Animated Fuel Flame between Fruit and Fuel
        int flameH = this.menu.getBurnTimeScaled(14);
        int flameX = x + 45;
        int flameY = y + 37;
        guiGraphics.fill(flameX, flameY, flameX + 14, flameY + 14, 0xFF2A1518);
        if (this.menu.isLit()) {
            guiGraphics.fill(flameX, flameY + (14 - flameH), flameX + 14, flameY + 14, 0xFFEF4444);
            guiGraphics.fill(flameX + 2, flameY + (14 - flameH) + 2, flameX + 12, flameY + 14, 0xFFF59E0B);
        }

        // 6. Animated Distillation Progress Tube
        int progressW = this.menu.getProgressScaled(24);
        int progX = x + 76;
        int progY = y + 42;
        guiGraphics.fill(progX, progY, progX + 24, progY + 6, 0xFF2E1065);
        if (progressW > 0) {
            guiGraphics.fill(progX, progY, progX + progressW, progY + 6, 0xFFC084FC);
            guiGraphics.fill(progX, progY + 1, progX + progressW, progY + 4, 0xFFFCD34D);
        }
        guiGraphics.renderOutline(progX, progY, 24, 6, 0xFF7E22CE);

        // 7. Biofuel Internal Fluid Tank Bar (x + 161 .. x + 168, y + 21 .. y + 69)
        int tankX = x + 161;
        int tankY = y + 21;
        int tankW = 7;
        int tankH = 48;
        guiGraphics.fill(tankX - 1, tankY - 1, tankX + tankW + 1, tankY + tankH + 1, 0xFF2D223E);
        guiGraphics.fill(tankX, tankY, tankX + tankW, tankY + tankH, 0xFF0A0810);
        int fluidAmt = this.menu.getFluidAmount();
        if (fluidAmt > 0) {
            int fillH = Math.max(1, (int) ((long) fluidAmt * tankH / net.valentin.obsidium.block.DionysianDistilleryBlockEntity.MAX_FLUID));
            int fType = this.menu.getFluidTypeId();
            int cOuter = fType == 1 ? 0xFFB45309 : 0xFF0284C7;
            int cInner = fType == 1 ? 0xFFF59E0B : 0xFF38BDF8;
            guiGraphics.fill(tankX, tankY + tankH - fillH, tankX + tankW, tankY + tankH, cOuter);
            guiGraphics.fill(tankX + 1, tankY + tankH - fillH, tankX + tankW - 1, tankY + tankH, cInner);
        }

        // Player Inventory Section (y = 84..160)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int sx = x + 8 + col * 18;
                int sy = y + 84 + row * 18;
                guiGraphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF242030);
                guiGraphics.fill(sx, sy, sx + 16, sy + 16, 0xFF0C0A12);
            }
        }
        for (int col = 0; col < 9; ++col) {
            int sx = x + 8 + col * 18;
            int sy = y + 142;
            guiGraphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF242030);
            guiGraphics.fill(sx, sy, sx + 16, sy + 16, 0xFF0C0A12);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Title in top bar
        guiGraphics.drawString(this.font, Component.literal("§d🍇 §fDionysian Distillery"), 8, 5, 0xFFFFFF, false);

        // Output section labels cleanly placed above output slots
        guiGraphics.drawString(this.font, "§6Țuică", 114, 23, 0xFFFBBF24, false);
        guiGraphics.drawString(this.font, "§7Ash", 143, 23, 0xFFCBD5E1, false);

        // Player Inventory Title placed cleanly above inventory slots (y = 74)
        guiGraphics.drawString(this.font, Component.literal("§8INVENTORY"), 8, 74, 0x64748B, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Hover Tooltip on Progress Arrow
        if (mouseX >= x + 76 && mouseX <= x + 100 && mouseY >= y + 40 && mouseY <= y + 48) {
            int pct = (this.menu.getMaxProgress() > 0) ? (this.menu.getProgress() * 100 / this.menu.getMaxProgress()) : 0;
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§d🍇 Distillation: §f" + pct + "%"),
                    Component.literal("§7Fermenting fruits into premium Țuică")
            ), mouseX, mouseY);
        }
        // Tooltip for Slot 0 (Fruit Input)
        else if (mouseX >= x + 43 && mouseX <= x + 61 && mouseY >= y + 19 && mouseY <= y + 37 && this.menu.getSlot(0).getItem().isEmpty()) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§dFruit / Mash Input Slot"),
                    Component.literal("§7Accepts Dionysian Pomace, Sweet Berries, Apples,"),
                    Component.literal("§7or any volcanic/hybrid fruits.")
            ), mouseX, mouseY);
        }
        // Tooltip for Slot 1 (Fuel Input)
        else if (mouseX >= x + 43 && mouseX <= x + 61 && mouseY >= y + 51 && mouseY <= y + 69 && this.menu.getSlot(1).getItem().isEmpty()) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§cFuel Slot"),
                    Component.literal("§7Accepts Coal, Charcoal, Lava Buckets,"),
                    Component.literal("§7or Volcanic Ash Briquettes.")
            ), mouseX, mouseY);
        }
        // Tooltip for Slot 2 (Bottle / Bucket Slot)
        else if (mouseX >= x + 79 && mouseX <= x + 97 && mouseY >= y + 19 && mouseY <= y + 37 && this.menu.getSlot(2).getItem().isEmpty()) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§bBottle / Bucket Slot"),
                    Component.literal("§7Glass/1L Bottle: Brews Țuică/Cider."),
                    Component.literal("§7Empty: Distills Bio-Oil (Seeds/Pomace) or Bio-Ethanol (Fruits)."),
                    Component.literal("§7Empty Bucket: Fills Bio-Oil / Bio-Ethanol Bucket.")
            ), mouseX, mouseY);
        }
        // Tooltip for Biofuel Tank Bar
        else if (mouseX >= x + 160 && mouseX <= x + 169 && mouseY >= y + 20 && mouseY <= y + 70) {
            int amt = this.menu.getFluidAmount();
            int fType = this.menu.getFluidTypeId();
            String fName = fType == 1 ? "§6Bio-Oil" : (fType == 2 ? "§bBio-Ethanol" : "§7Empty");
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal(fName + " §f(" + String.format("%,d", amt) + " / 16,000 mB)"),
                    Component.literal("§7Leave Bottle slot empty to distill Biofuel."),
                    Component.literal("§7Extract with Bucket or Fluid Pipes.")
            ), mouseX, mouseY);
        }
    }
}
