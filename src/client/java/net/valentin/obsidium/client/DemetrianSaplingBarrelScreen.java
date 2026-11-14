package net.valentin.obsidium.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.block.DemetrianSaplingBarrelBlockEntity;
import net.valentin.obsidium.block.DemetrianSaplingBarrelMenu;

import java.util.ArrayList;
import java.util.List;

public class DemetrianSaplingBarrelScreen extends AbstractContainerScreen<DemetrianSaplingBarrelMenu> {

    public DemetrianSaplingBarrelScreen(DemetrianSaplingBarrelMenu menu, Inventory playerInventory, Component title) {
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

        // 1. Exterior Casing (Warm Demetrian Cypress & Brass Wood Frame)
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF19130D);
        guiGraphics.renderOutline(x, y, this.imageWidth, this.imageHeight, 0xFF84532A);
        guiGraphics.renderOutline(x + 1, y + 1, this.imageWidth - 2, this.imageHeight - 2, 0xFFB8860B);

        // 2. Upper Planter Section (y = 16 to 94)
        guiGraphics.fill(x + 6, y + 16, x + 170, y + 94, 0xFF241A12);
        guiGraphics.renderOutline(x + 6, y + 16, 164, 78, 0xFF5C3A21);

        // 3. Render 3 Dedicated Chamber Cards
        int[] cardX = {14, 66, 118};

        for (int i = 0; i < 3; i++) {
            int cx = x + cardX[i];
            int cy = y + 18;
            int cw = 44;
            int ch = 74;

            // Chamber card background
            guiGraphics.fill(cx, cy, cx + cw, cy + ch, 0xFF120E09);
            guiGraphics.renderOutline(cx, cy, cw, ch, 0xFF2D5A27);

            // Chamber slot border (18x18 at cardX + 13, y + 33)
            int sx = cx + 13;
            int sy = y + 33;
            guiGraphics.fill(sx, sy, sx + 18, sy + 18, 0xFF080603);
            guiGraphics.renderOutline(sx, sy, 18, 18, 0xFF4D7C0F);

            // Directly render the item icon inside the slot!
            ItemStack sample = this.menu.getSlot(i).getItem();
            if (sample.isEmpty()) {
                sample = this.menu.getSampleStack(i);
            }
            if (!sample.isEmpty()) {
                guiGraphics.renderFakeItem(sample, sx + 1, sy + 1);
            }

            // Gauge capacity bar (30px wide, 4px high)
            int bx = cx + 7;
            int by = y + 54;
            long amount = this.menu.getChamberAmount(i);
            long max = DemetrianSaplingBarrelBlockEntity.MAX_CHAMBER_CAPACITY;
            int fillW = max > 0 ? (int) Math.min(30, (amount * 30L) / max) : 0;

            guiGraphics.fill(bx, by, bx + 30, by + 4, 0xFF1F2937);
            if (fillW > 0) {
                guiGraphics.fill(bx, by, bx + fillW, by + 4, 0xFF22C55E);
                guiGraphics.fill(bx, by, bx + fillW, by + 1, 0xFF86EFAC);
            }
            guiGraphics.renderOutline(bx, by, 30, 4, 0xFF374151);

            // Amount / Empty text
            String countStr = amount > 0 ? String.format("%,d", amount) : "§8Empty";
            int textW = this.font.width(countStr);
            guiGraphics.drawString(this.font, countStr, cx + (cw - textW) / 2, y + 62, amount > 0 ? 0x86EFAC : 0x6B7280, false);
        }

        // 4. Botanical Divider Line
        guiGraphics.fill(x + 10, y + 96, x + 166, y + 97, 0xFF4D7C0F);
        guiGraphics.fill(x + 10, y + 97, x + 166, y + 98, 0xFF1E3A1A);

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
        guiGraphics.drawString(this.font, "§eSlot I", 25, 21, 0xFBBF24, false);
        guiGraphics.drawString(this.font, "§eSlot II", 77, 21, 0xFBBF24, false);
        guiGraphics.drawString(this.font, "§eSlot III", 129, 21, 0xFBBF24, false);

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
        int[] cardX = {14, 66, 118};
        for (int i = 0; i < 3; i++) {
            int cx = x + cardX[i];
            int cy = y + 18;
            int cw = 44;
            int ch = 74;

            if (mouseX >= cx && mouseX <= cx + cw && mouseY >= cy && mouseY <= cy + ch) {
                long amount = this.menu.getChamberAmount(i);
                long max = DemetrianSaplingBarrelBlockEntity.MAX_CHAMBER_CAPACITY;
                int pct = max > 0 ? (int) ((amount * 100L) / max) : 0;
                ItemStack sample = this.menu.getSampleStack(i);

                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.literal("§6✦ Demetrian Nursery Chamber " + (i + 1)));

                if (!sample.isEmpty() && amount > 0) {
                    tooltip.add(Component.literal("§e" + sample.getHoverName().getString()));
                    tooltip.add(Component.literal("§f" + String.format("%,d", amount) + " / " + String.format("%,d", max) + " §7(" + pct + "%)"));
                    tooltip.add(Component.literal("§8Left-click: Take 64"));
                    tooltip.add(Component.literal("§8Right-click: Take 1"));
                    tooltip.add(Component.literal("§8Shift-click: Move to inventory"));
                } else {
                    tooltip.add(Component.literal("§8[Empty Chamber]"));
                    tooltip.add(Component.literal("§7Accepts any sapling or scion cutting"));
                    tooltip.add(Component.literal("§7Capacity: §a4,096 saplings"));
                }

                guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                break;
            }
        }
    }
}
