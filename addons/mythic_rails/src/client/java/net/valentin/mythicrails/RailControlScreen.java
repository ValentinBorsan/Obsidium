package net.valentin.mythicrails;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Dark obsidian-gold remote dispatch console UI for Olympus Dispatch Console. */
public final class RailControlScreen extends AbstractContainerScreen<RailControlMenu> {
    private static final int ROW_H = 18;
    private static final int TEXT_MAIN = 0xFFF3E8FF;
    private static final int TEXT_DIM = 0xFF9C8FB4;
    private final RailGui.RailButton[] rowButtons = new RailGui.RailButton[RailControlBlockEntity.MAX_LINKS];

    public RailControlScreen(RailControlMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 236;
        imageHeight = 224;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 12;
        titleLabelY = 7;

        for (int i = 0; i < RailControlBlockEntity.MAX_LINKS; i++) {
            final int slot = i;
            rowButtons[i] = addRenderableWidget(new RailGui.RailButton(
                    leftPos + 150, topPos + 30 + i * ROW_H, 74, 15,
                    Component.literal("Toggle"),
                    btn -> sendCmd(slot),
                    RailGui.CYAN_ACCENT));
        }

        int masterY = topPos + 186;
        addRenderableWidget(new RailGui.RailButton(leftPos + 12, masterY, 50, 18,
                Component.literal("ALL GO"), btn -> sendCmd(100), 0xFF3CE070));
        addRenderableWidget(new RailGui.RailButton(leftPos + 66, masterY, 52, 18,
                Component.literal("ALL STOP"), btn -> sendCmd(101), 0xFFE84040));
        addRenderableWidget(new RailGui.RailButton(leftPos + 122, masterY, 50, 18,
                Component.literal("LAUNCH"), btn -> sendCmd(103), RailGui.ORANGE_ACCENT));
        addRenderableWidget(new RailGui.RailButton(leftPos + 176, masterY, 48, 18,
                Component.literal("SWITCH"), btn -> sendCmd(104), RailGui.CYAN_ACCENT));
    }

    private void sendCmd(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int count = menu.linkCount();
        for (int i = 0; i < RailControlBlockEntity.MAX_LINKS; i++) {
            boolean active = i < count;
            rowButtons[i].visible = active;
            rowButtons[i].active = active && menu.slotKind(i) != SignalConnectorBlockEntity.KIND_NONE;
            if (active) {
                int kind = menu.slotKind(i);
                int mode = menu.slotMode(i);
                rowButtons[i].setMessage(Component.literal(modeLabel(kind, mode)));
                rowButtons[i].setAccentColor(dotColor(kind, mode));
            }
        }
    }

    private static String modeLabel(int kind, int mode) {
        return switch (kind) {
            case SignalConnectorBlockEntity.KIND_SEMAPHORE -> switch (mode) {
                case 1 -> "RED";
                case 2 -> "GREEN";
                default -> "AUTO";
            };
            case SignalConnectorBlockEntity.KIND_BUMPER -> mode == 1 ? "LAUNCH" : "HOLD";
            case SignalConnectorBlockEntity.KIND_SWITCH -> mode == 1 ? "CURVED" : "STRAIGHT";
            default -> "UNBOUND";
        };
    }

    private static String kindLabel(int kind) {
        return switch (kind) {
            case SignalConnectorBlockEntity.KIND_SEMAPHORE -> "Semaphore";
            case SignalConnectorBlockEntity.KIND_BUMPER -> "Buffer Stop";
            case SignalConnectorBlockEntity.KIND_SWITCH -> "Switch Rail";
            default -> "No Target";
        };
    }

    private static int dotColor(int kind, int mode) {
        return switch (kind) {
            case SignalConnectorBlockEntity.KIND_SEMAPHORE -> mode == 1 ? 0xFFE84040 : (mode == 2 ? 0xFF3CE070 : 0xFFE8B838);
            case SignalConnectorBlockEntity.KIND_BUMPER -> mode == 1 ? 0xFF3CE070 : 0xFFE84040;
            case SignalConnectorBlockEntity.KIND_SWITCH -> mode == 1 ? 0xFF44B4E8 : 0xFFE8B838;
            default -> 0xFF666677;
        };
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RailGui.frame(g, leftPos, topPos, imageWidth, imageHeight, RailGui.CYAN_ACCENT);
        RailGui.panel(g, leftPos + 8, topPos + 24, imageWidth - 16, 154, RailGui.GOLD_BORDER, RailGui.CARD_BG);
        int count = menu.linkCount();
        for (int i = 0; i < RailControlBlockEntity.MAX_LINKS; i++) {
            int y = topPos + 29 + i * ROW_H;
            int bg = (i < count) ? 0xFF231A33 : 0xFF140E1D;
            g.fill(leftPos + 12, y, leftPos + 144, y + 16, bg);
            if (i < count) {
                int color = dotColor(menu.slotKind(i), menu.slotMode(i));
                g.fill(leftPos + 15, y + 4, leftPos + 21, y + 12, color);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, RailGui.GOLD_TEXT, false);
        int count = menu.linkCount();
        String sub = "Linked: " + count + " / " + RailControlBlockEntity.MAX_LINKS;
        g.drawString(font, sub, imageWidth - font.width(sub) - 12, 7, TEXT_DIM, false);

        if (count == 0) {
            g.drawString(font, "Bind Caduceus Linker on Console,", 18, 76, TEXT_DIM, false);
            g.drawString(font, "then right-click Iris Signal Relays.", 18, 90, TEXT_DIM, false);
        } else {
            for (int i = 0; i < count; i++) {
                int y = 33 + i * ROW_H;
                g.drawString(font, "#" + (i + 1) + " " + kindLabel(menu.slotKind(i)), 26, y, TEXT_MAIN, false);
            }
        }
        g.drawString(font, "Master Controls", 12, 176, TEXT_DIM, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
