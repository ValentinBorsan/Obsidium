package net.valentin.mythicrails;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class FreightCartScreen extends AbstractContainerScreen<FreightCartMenu> {
    private RailGui.RailButton toggle, left, right;

    public FreightCartScreen(FreightCartMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 208;
        imageHeight = 278;
        inventoryLabelY = 188;
    }

    private void send(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void init() {
        super.init();
        toggle = addRenderableWidget(new RailGui.RailButton(leftPos + 56, topPos + 25, 68, 16,
                Component.translatable("gui.mythic_rails.start"), b -> send(0), 0xFF10B981));
        left = addRenderableWidget(new RailGui.RailButton(leftPos + 128, topPos + 25, 34, 16,
                Component.literal("<"), b -> send(1), RailGui.GOLD_BORDER));
        right = addRenderableWidget(new RailGui.RailButton(leftPos + 166, topPos + 25, 34, 16,
                Component.literal(">"), b -> send(2), RailGui.GOLD_BORDER));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean running = menu.value(0) == 1;
        boolean hasMotor = menu.value(2) != 0;
        toggle.active = hasMotor || running;
        toggle.setMessage(Component.translatable("gui.mythic_rails." + (running ? "stop" : "start")));
        toggle.setAccentColor(running ? 0xFFEF4444 : 0xFF10B981);
        left.active = right.active = !running;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int payload = menu.value(3);
        int accent = payload == CartModuleItem.PayloadType.LIQUID_BARREL.id ? RailGui.CYAN_ACCENT
                : (payload == CartModuleItem.PayloadType.ENERGY_CELL.id ? 0xFF38BDF8 : RailGui.GOLD_BORDER);
        RailGui.frame(g, leftPos, topPos, imageWidth, imageHeight, accent);

        // Top modular control card
        RailGui.panel(g, leftPos + 6, topPos + 22, imageWidth - 12, 58, 0xFF3E2D52, RailGui.CARD_BG);

        // Energy / Fuel bar at (leftPos + 56, topPos + 54)
        int barX = leftPos + 56;
        int barY = topPos + 54;
        int barW = 144;
        RailGui.panel(g, barX, barY, barW, 6, 0xFF352747, 0xFF0A0710);
        int motor = menu.value(2);
        if (motor == CartModuleItem.MotorType.ELECTRIC.id || payload == CartModuleItem.PayloadType.ENERGY_CELL.id) {
            int maxE = Math.max(1, menu.value(7));
            int curE = Math.clamp(menu.value(6), 0, maxE);
            int fw = (barW - 2) * curE / maxE;
            if (fw > 0) g.fill(barX + 1, barY + 1, barX + 1 + fw, barY + 5, 0xFF38BDF8);
        } else {
            int maxF = Math.max(1, menu.value(5));
            int curF = Math.clamp(menu.value(4), 0, maxF);
            int fw = menu.value(5) == 0 ? 0 : (barW - 2) * curF / maxF;
            if (fw > 0) g.fill(barX + 1, barY + 1, barX + 1 + fw, barY + 5, 0xFFFF731C);
        }

        // Secondary module bar (Fluid or Work progress) at (leftPos + 56, topPos + 71)
        int bar2Y = topPos + 71;
        RailGui.panel(g, barX, bar2Y, barW, 5, 0xFF352747, 0xFF0A0710);
        if (payload == CartModuleItem.PayloadType.LIQUID_BARREL.id) {
            int fluid = Math.clamp(menu.value(8), 0, 3200);
            int fw = (barW - 2) * fluid / 3200;
            int col = menu.value(9) == 2 ? 0xFFFF6611 : 0xFF22CCFF;
            if (fw > 0) g.fill(barX + 1, bar2Y + 1, barX + 1 + fw, bar2Y + 4, col);
        } else if (payload >= CartModuleItem.PayloadType.ARBORIST.id) {
            int prog = Math.clamp(menu.value(10), 0, 100);
            int fw = (barW - 2) * prog / 100;
            if (fw > 0) g.fill(barX + 1, bar2Y + 1, barX + 1 + fw, bar2Y + 4, 0xFF10B981);
        }

        // Divider above player inventory
        g.fill(leftPos + 8, topPos + 194, leftPos + imageWidth - 8, topPos + 195, 0xFF3E2D52);

        for (var slot : menu.slots) {
            RailGui.slot(g, leftPos + slot.x, topPos + slot.y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        var payload = CartModuleItem.PayloadType.byId(menu.value(3));
        String badge = switch (payload) {
            case CARGO -> "CARGO";
            case LIQUID_BARREL -> "BARREL";
            case ENERGY_CELL -> "ENERGY";
            case ARBORIST -> "ARBORIST";
            case SMELTER -> "SMELTER";
            case GRINDER -> "GRINDER";
            default -> "MODULAR";
        };
        g.drawString(font, font.substrByWidth(title, imageWidth - 76).getString(), 8, 7, RailGui.GOLD_TEXT, false);
        RailGui.badge(g, font, imageWidth - 6, 5, badge, RailGui.CYAN_ACCENT, 0xFF9DF7FF);

        // Direction + motor telemetry
        var dir = Direction.from2DDataValue(menu.value(1));
        int motor = menu.value(2);
        String motorStr = motor == 1 ? ("Coal " + menu.value(4) + "s")
                : (motor == 2 ? (menu.value(6) * 100 / 1000 + "k RF") : "No Motor");
        g.drawString(font, motorStr + " · " + text("direction." + dir.getName()).getString(), 56, 44, 0xFFE2F7F4, false);

        String modStr = switch (payload) {
            case LIQUID_BARREL -> (menu.value(9) == 2 ? "Lava " : (menu.value(9) == 1 ? "Water " : "Empty ")) + (menu.value(8) * 10) + " mB";
            case ENERGY_CELL -> "Cell: " + (menu.value(6) * 100 / 1000) + "k / 2000k RF";
            case ARBORIST -> "Arborist: " + menu.value(10) + "%";
            case SMELTER -> "Smelter: " + menu.value(10) + "%";
            case GRINDER -> "Grinder: " + menu.value(10) + "%";
            default -> "Cargo: 54 slots";
        };
        g.drawString(font, modStr, 56, 62, 0xFFCDBFDC, false);
        g.drawString(font, playerInventoryTitle, 23, inventoryLabelY, 0xFFBCABC9, false);
    }

    private Component text(String key) {
        return Component.translatable("gui.mythic_rails." + key);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
