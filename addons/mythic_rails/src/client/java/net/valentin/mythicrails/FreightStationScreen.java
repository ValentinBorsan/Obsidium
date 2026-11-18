package net.valentin.mythicrails;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class FreightStationScreen extends AbstractContainerScreen<FreightStationMenu> {
    private RailGui.RailButton toggle, left, right, release;

    public FreightStationScreen(FreightStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 208;
        imageHeight = 278;
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
        int accent = menu.value(2) == 1 ? RailGui.CYAN_ACCENT : RailGui.ORANGE_ACCENT;
        toggle = addRenderableWidget(new RailGui.RailButton(leftPos + 8, topPos + 92, 94, 18, text("pause"), b -> send(0), 0xFF10B981));
        left = addRenderableWidget(new RailGui.RailButton(leftPos + 108, topPos + 92, 44, 18, Component.literal("<"), b -> send(1), RailGui.GOLD_BORDER));
        right = addRenderableWidget(new RailGui.RailButton(leftPos + 156, topPos + 92, 44, 18, Component.literal(">"), b -> send(2), RailGui.GOLD_BORDER));
        release = addRenderableWidget(new RailGui.RailButton(leftPos + 8, topPos + 114, 192, 18, text("release"), b -> send(3), accent));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean enabled = menu.value(1) == 1;
        toggle.setMessage(text(enabled ? "pause" : "resume"));
        toggle.setAccentColor(enabled ? 0xFFF59E0B : 0xFF10B981);
        left.active = right.active = menu.value(6) == 0 || !enabled;
        release.active = menu.value(6) == 1 && menu.value(3) != 7 && menu.value(3) != 8;
        release.setAccentColor(menu.value(2) == 1 ? RailGui.CYAN_ACCENT : RailGui.ORANGE_ACCENT);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        boolean isLoader = menu.value(2) == 1;
        int accent = isLoader ? RailGui.CYAN_ACCENT : RailGui.ORANGE_ACCENT;
        RailGui.frame(g, leftPos, topPos, imageWidth, imageHeight, accent);

        // Status & telemetry card
        RailGui.panel(g, leftPos + 8, topPos + 24, imageWidth - 16, 64, 0xFF3E2D52, RailGui.CARD_BG);

        // Status indicator dot
        int status = Math.clamp(menu.value(3), 0, 8);
        int dotColor = switch (status) {
            case 1 -> 0xFF10B981; // Transferring
            case 6, 7 -> accent;  // Ready / Departing
            case 3 -> 0xFFF59E0B; // Paused
            case 2, 4, 5, 8 -> 0xFFEF4444; // Held / Blocked / Misaligned
            default -> 0xFF6B5F7A; // No cart
        };
        g.fill(leftPos + 13, topPos + 31, leftPos + 18, topPos + 36, dotColor);

        // Cargo progress bar (0..54 slots)
        int cargo = Math.clamp(menu.value(4), 0, 54);
        int barX = leftPos + 13;
        int barY = topPos + 53;
        int barW = imageWidth - 26;
        RailGui.panel(g, barX, barY, barW, 7, 0xFF352747, 0xFF0B0712);
        int fillW = (barW - 2) * cargo / 54;
        if (fillW > 0) {
            g.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + 6, accent);
        }

        // Redstone LED
        boolean held = menu.value(5) == 1;
        g.fill(leftPos + 13, topPos + 65, leftPos + 18, topPos + 70, held ? 0xFFEF4444 : 0xFF10B981);

        // Divider above station buffer & player inventory
        g.fill(leftPos + 8, topPos + 137, leftPos + imageWidth - 8, topPos + 138, 0xFF3E2D52);
        g.fill(leftPos + 8, topPos + 177, leftPos + imageWidth - 8, topPos + 178, 0xFF3E2D52);

        for (var slot : menu.slots) {
            RailGui.slot(g, leftPos + slot.x, topPos + slot.y);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        renderTooltip(g, mx, my);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        boolean isLoader = menu.value(2) == 1;
        int accent = isLoader ? RailGui.CYAN_ACCENT : RailGui.ORANGE_ACCENT;
        String modeLabel = isLoader ? "LOADER" : "UNLOADER";
        int titleMaxW = imageWidth - 20 - (font.width(modeLabel) + 14);
        g.drawString(font, font.substrByWidth(title, Math.max(40, titleMaxW)).getString(), 8, 7, RailGui.GOLD_TEXT, false);
        RailGui.badge(g, font, imageWidth - 6, 5, modeLabel, accent, isLoader ? 0xFF9DF7FF : 0xFFFFC580);

        int status = Math.clamp(menu.value(3), 0, 8);
        g.drawString(font, Component.translatable("gui.mythic_rails.status", text("status." + status)), 22, 30, 0xFFE2F7F4, false);
        g.drawString(font, Component.translatable("gui.mythic_rails.cargo", menu.value(4)), 13, 42, 0xFFCDBFDC, false);

        boolean held = menu.value(5) == 1;
        g.drawString(font, Component.translatable("gui.mythic_rails.redstone", text(held ? "held" : "clear")), 22, 64, held ? 0xFFFCA5A5 : 0xFFA7F3D0, false);

        var direction = Direction.from2DDataValue(menu.value(0));
        g.drawString(font, Component.translatable("gui.mythic_rails.direction", text("direction." + direction.getName())), 13, 75, RailGui.GOLD_TEXT, false);

        g.drawString(font, text("station_buffer"), 23, 143, 0xFFCDBFDC, false);
        g.drawString(font, playerInventoryTitle, 23, 182, 0xFFBCABC9, false);
    }
}
