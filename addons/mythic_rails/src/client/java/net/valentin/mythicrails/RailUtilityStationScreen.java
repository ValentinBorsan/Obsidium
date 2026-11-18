package net.valentin.mythicrails;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class RailUtilityStationScreen extends AbstractContainerScreen<RailUtilityStationMenu> {
    private RailGui.RailButton toggle, left, right, release;

    public RailUtilityStationScreen(RailUtilityStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 208;
        imageHeight = 236;
        inventoryLabelY = 141;
    }

    private void send(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private Component text(String key) {
        return Component.translatable("gui.mythic_rails." + key);
    }

    @Override
    protected void init() {
        super.init();
        int accent = menu.value(2) == 1 ? RailGui.CYAN_ACCENT : 0xFF38BDF8;
        toggle = addRenderableWidget(new RailGui.RailButton(leftPos + 8, topPos + 94, 94, 18, text("pause"), b -> send(0), 0xFF10B981));
        left = addRenderableWidget(new RailGui.RailButton(leftPos + 108, topPos + 94, 44, 18, Component.literal("<"), b -> send(1), RailGui.GOLD_BORDER));
        right = addRenderableWidget(new RailGui.RailButton(leftPos + 156, topPos + 94, 44, 18, Component.literal(">"), b -> send(2), RailGui.GOLD_BORDER));
        release = addRenderableWidget(new RailGui.RailButton(leftPos + 8, topPos + 116, 192, 18, text("release"), b -> send(3), accent));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean enabled = menu.value(1) == 1;
        toggle.setMessage(text(enabled ? "pause" : "resume"));
        toggle.setAccentColor(enabled ? 0xFFF59E0B : 0xFF10B981);
        left.active = right.active = menu.value(9) == 0 || !enabled;
        release.active = menu.value(9) == 1 && menu.value(4) != 7 && menu.value(4) != 8;
        release.setAccentColor(menu.value(3) == 1 ? RailGui.CYAN_ACCENT : RailGui.ORANGE_ACCENT);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        boolean isFluid = menu.value(2) == 1;
        boolean isLoader = menu.value(3) == 1;
        int accent = isFluid ? (isLoader ? RailGui.CYAN_ACCENT : RailGui.ORANGE_ACCENT) : (isLoader ? 0xFF38BDF8 : 0xFFF59E0B);
        RailGui.frame(g, leftPos, topPos, imageWidth, imageHeight, accent);

        RailGui.panel(g, leftPos + 8, topPos + 24, imageWidth - 16, 64, 0xFF3E2D52, RailGui.CARD_BG);

        int status = Math.clamp(menu.value(4), 0, 8);
        int dotColor = switch (status) {
            case 1 -> 0xFF10B981;
            case 6, 7 -> accent;
            case 3 -> 0xFFF59E0B;
            case 2, 4, 5, 8 -> 0xFFEF4444;
            default -> 0xFF6B5F7A;
        };
        g.fill(leftPos + 13, topPos + 31, leftPos + 18, topPos + 36, dotColor);

        // Station buffer bar (Fluid or Energy)
        int barX = leftPos + 36;
        int barY = topPos + 53;
        int barW = imageWidth - 49;
        RailGui.panel(g, barX, barY, barW, 6, 0xFF352747, 0xFF0A0710);
        if (isFluid) {
            int cur = Math.clamp(menu.value(5), 0, 3200);
            int fw = (barW - 2) * cur / 3200;
            int col = menu.value(6) == 2 ? 0xFFFF6611 : 0xFF22CCFF;
            if (fw > 0) g.fill(barX + 1, barY + 1, barX + 1 + fw, barY + 5, col);
        } else {
            int cur = Math.clamp(menu.value(7), 0, 20000);
            int fw = (barW - 2) * cur / 20000;
            if (fw > 0) g.fill(barX + 1, barY + 1, barX + 1 + fw, barY + 5, 0xFF38BDF8);
        }

        // Redstone LED
        boolean held = menu.value(8) == 1;
        g.fill(leftPos + 36, topPos + 64, leftPos + 41, topPos + 69, held ? 0xFFEF4444 : 0xFF10B981);

        // Divider above player inventory
        g.fill(leftPos + 8, topPos + 138, leftPos + imageWidth - 8, topPos + 139, 0xFF3E2D52);

        for (var slot : menu.slots) {
            RailGui.slot(g, leftPos + slot.x, topPos + slot.y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        boolean isFluid = menu.value(2) == 1;
        boolean isLoader = menu.value(3) == 1;
        int accent = isFluid ? (isLoader ? RailGui.CYAN_ACCENT : RailGui.ORANGE_ACCENT) : (isLoader ? 0xFF38BDF8 : 0xFFF59E0B);
        String badge = (isFluid ? "FLUID " : "RF ") + (isLoader ? "IN" : "OUT");
        int titleMaxW = imageWidth - 20 - (font.width(badge) + 14);
        g.drawString(font, font.substrByWidth(title, Math.max(40, titleMaxW)).getString(), 8, 7, RailGui.GOLD_TEXT, false);
        RailGui.badge(g, font, imageWidth - 6, 5, badge, accent, 0xFFE2F7F4);

        int status = Math.clamp(menu.value(4), 0, 8);
        g.drawString(font, Component.translatable("gui.mythic_rails.status", text("status." + status)), 22, 30, 0xFFE2F7F4, false);

        String bufStr = isFluid
                ? ((menu.value(6) == 2 ? "Lava " : (menu.value(6) == 1 ? "Water " : "Tank: ")) + (menu.value(5) * 10) + " / 32000 mB")
                : ("Buffer: " + (menu.value(7) * 100 / 1000) + "k / 2000k RF");
        g.drawString(font, bufStr, 36, 42, 0xFFCDBFDC, false);

        boolean held = menu.value(8) == 1;
        g.drawString(font, Component.translatable("gui.mythic_rails.redstone", text(held ? "held" : "clear")), 45, 63, held ? 0xFFFCA5A5 : 0xFFA7F3D0, false);

        var dir = Direction.from2DDataValue(menu.value(0));
        g.drawString(font, Component.translatable("gui.mythic_rails.direction", text("direction." + dir.getName())), 36, 75, RailGui.GOLD_TEXT, false);

        g.drawString(font, playerInventoryTitle, 23, inventoryLabelY, 0xFFBCABC9, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        renderTooltip(g, mx, my);
    }
}
