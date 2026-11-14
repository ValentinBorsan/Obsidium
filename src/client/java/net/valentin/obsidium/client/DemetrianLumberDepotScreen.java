package net.valentin.obsidium.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.block.DemetrianLumberDepotBlockEntity;
import net.valentin.obsidium.block.DemetrianLumberDepotMenu;

import java.util.ArrayList;
import java.util.List;

public class DemetrianLumberDepotScreen extends AbstractContainerScreen<DemetrianLumberDepotMenu> {

    public DemetrianLumberDepotScreen(DemetrianLumberDepotMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 194;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 100;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 1. Exterior Casing (Warm Demetrian Redwood & Bronze Wood Frame)
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF19130D);
        guiGraphics.renderOutline(x, y, this.imageWidth, this.imageHeight, 0xFF84532A);
        guiGraphics.renderOutline(x + 1, y + 1, this.imageWidth - 2, this.imageHeight - 2, 0xFFB8860B);

        // 2. Upper Timber Storage Section (y = 16 to 94)
        guiGraphics.fill(x + 6, y + 16, x + 170, y + 94, 0xFF241A12);
        guiGraphics.renderOutline(x + 6, y + 16, 164, 78, 0xFF5C3A21);

        // 3. Render 4 Dedicated Timber Chamber Cards
        int[] cardX = {10, 50, 90, 130};

        for (int i = 0; i < 4; i++) {
            int cx = x + cardX[i];
            int cy = y + 18;
            int cw = 36;
            int ch = 74;

            // Chamber card background
            guiGraphics.fill(cx, cy, cx + cw, cy + ch, 0xFF120E09);
            guiGraphics.renderOutline(cx, cy, cw, ch, 0xFF3D2616);

            // Chamber slot border (18x18 at cardX + 9, y + 33)
            int sx = cx + 9;
            int sy = y + 33;
            guiGraphics.fill(sx, sy, sx + 18, sy + 18, 0xFF080603);
            guiGraphics.renderOutline(sx, sy, 18, 18, 0xFF84532A);

            // Render item icon inside the slot
            ItemStack sample = this.menu.getSlot(i).getItem();
            if (sample.isEmpty()) {
                sample = this.menu.getSampleStack(i);
            }
            if (!sample.isEmpty()) {
                guiGraphics.renderFakeItem(sample, sx + 1, sy + 1);
            }

            // Gauge capacity bar (28px wide, 4px high)
            int bx = cx + 4;
            int by = y + 54;
            long amount = this.menu.getChamberAmount(i);
            long max = DemetrianLumberDepotBlockEntity.MAX_CHAMBER_CAPACITY;
            int fillW = max > 0 ? (int) Math.min(28, (amount * 28L) / max) : 0;

            guiGraphics.fill(bx, by, bx + 28, by + 4, 0xFF1F2937);
            if (fillW > 0) {
                guiGraphics.fill(bx, by, bx + fillW, by + 4, 0xFFD97706);
                guiGraphics.fill(bx, by, bx + fillW, by + 1, 0xFFFBBF24);
            }
            guiGraphics.renderOutline(bx, by, 28, 4, 0xFF374151);

            // Amount / Empty text
            String countStr;
            if (amount <= 0) {
                countStr = "§8Empty";
            } else if (amount >= 1000) {
                countStr = String.format("%.1fK", amount / 1000.0);
            } else {
                countStr = String.valueOf(amount);
            }
            int textW = this.font.width(countStr);
            guiGraphics.drawString(this.font, countStr, cx + (cw - textW) / 2, y + 62, amount > 0 ? 0xFDE68A : 0x6B7280, false);
        }

        // 4. Bronze Divider Line
        guiGraphics.fill(x + 10, y + 96, x + 166, y + 97, 0xFF84532A);
        guiGraphics.fill(x + 10, y + 97, x + 166, y + 98, 0xFF422817);

        // 5. Lower Player Inventory Frame (y = 98 to 190)
        guiGraphics.fill(x + 6, y + 99, x + 170, y + 190, 0xFF17100B);
        guiGraphics.renderOutline(x + 6, y + 99, 164, 91, 0xFF422817);

        // Player Inventory Slots (3 rows of 9)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int sx = x + 8 + col * 18 - 1;
                int sy = y + 112 + row * 18 - 1;
                guiGraphics.fill(sx, sy, sx + 18, sy + 18, 0xFF0B0704);
                guiGraphics.renderOutline(sx, sy, 18, 18, 0xFF3D2616);
            }
        }

        // Hotbar Slots (1 row of 9)
        for (int col = 0; col < 9; ++col) {
            int sx = x + 8 + col * 18 - 1;
            int sy = y + 170 - 1;
            guiGraphics.fill(sx, sy, sx + 18, sy + 18, 0xFF0B0704);
            guiGraphics.renderOutline(sx, sy, 18, 18, 0xFF3D2616);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Clean Title without any overlapping text!
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFDE68A, false);

        // Chamber labels (cleanly centered above each slot)
        String[] labels = {"§eBay I", "§eBay II", "§eBay III", "§eBay IV"};
        int[] cardRelX = {10, 50, 90, 130};
        for (int i = 0; i < 4; i++) {
            int tw = this.font.width(labels[i]);
            guiGraphics.drawString(this.font, labels[i], cardRelX[i] + (36 - tw) / 2, 21, 0xFBBF24, false);
        }

        // Player inventory label
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xD97706, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Custom Chamber Tooltips when hovering over Chamber cards
        int[] cardX = {10, 50, 90, 130};
        for (int i = 0; i < 4; i++) {
            int cx = x + cardX[i];
            int cy = y + 18;
            int cw = 36;
            int ch = 74;

            if (mouseX >= cx && mouseX <= cx + cw && mouseY >= cy && mouseY <= cy + ch) {
                long amount = this.menu.getChamberAmount(i);
                long max = DemetrianLumberDepotBlockEntity.MAX_CHAMBER_CAPACITY;
                int pct = max > 0 ? (int) ((amount * 100L) / max) : 0;
                ItemStack sample = this.menu.getSlot(i).getItem();
                if (sample.isEmpty()) sample = this.menu.getSampleStack(i);

                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.literal("§6✦ Demetrian Timber Bay " + (i + 1)));
                if (!sample.isEmpty()) {
                    tooltip.add(Component.literal("§fStored: " + sample.getHoverName().getString()));
                    tooltip.add(Component.literal(String.format("§7Count: §a%,d §7/ §e%,d §8(%d%%)", amount, max, pct)));
                    tooltip.add(Component.literal(String.format("§8Equivalent to: §6%.1f stacks", amount / 64.0)));
                } else {
                    tooltip.add(Component.literal("§8Status: Empty"));
                    tooltip.add(Component.literal(String.format("§7Capacity: §e%,d logs", max)));
                    tooltip.add(Component.literal("§8Accepts any tree trunk, log, or wood"));
                }
                tooltip.add(Component.literal("§8─────────────────"));
                tooltip.add(Component.literal("§eLeft-Click: §7Extract 1 log"));
                tooltip.add(Component.literal("§eShift + Left-Click: §7Extract 64 logs"));
                tooltip.add(Component.literal("§eRight-Click (held log): §7Deposit logs"));

                guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
            }
        }
    }
}
