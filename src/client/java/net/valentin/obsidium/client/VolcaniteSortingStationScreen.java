package net.valentin.obsidium.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.block.VolcaniteSortingStationBlockEntity;
import net.valentin.obsidium.block.VolcaniteSortingStationMenu;

public class VolcaniteSortingStationScreen extends AbstractContainerScreen<VolcaniteSortingStationMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("obsidium", "textures/gui/volcanite_sorting_station_gui.png");

    private final Button[] modeButtons = new Button[6];

    // Card positions matching GUI texture:
    // Col 1 (x = 8): UP (y=22), NORTH (y=52), WEST (y=82)
    // Col 2 (x = 92): DOWN (y=22), SOUTH (y=52), EAST (y=82)
    // Face indices (Direction.get3DDataValue()):
    // 0: DOWN, 1: UP, 2: NORTH, 3: SOUTH, 4: WEST, 5: EAST
    private static final int[][] CARD_POS = new int[][] {
        {90, 20}, // 0: DOWN
        {7, 20},  // 1: UP
        {7, 55},  // 2: NORTH
        {90, 55}, // 3: SOUTH
        {7, 90},  // 4: WEST
        {90, 90}  // 5: EAST
    };

    private static final String[] FACE_NAMES = new String[] {
        "↓ DOWN", "↑ UP", "↑ NORTH", "↓ SOUTH", "← WEST", "→ EAST"
    };

    public VolcaniteSortingStationScreen(VolcaniteSortingStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 228;
        this.inventoryLabelY = 132;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        for (int i = 0; i < 6; i++) {
            final int faceIndex = i;
            int btnX = x + CARD_POS[i][0] + 43;
            int btnY = y + CARD_POS[i][1] + 2;

            modeButtons[i] = this.addRenderableWidget(Button.builder(Component.literal("Mode"), b -> {
                if (this.minecraft != null && this.minecraft.gameMode != null) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, faceIndex);
                }
            }).bounds(btnX, btnY, 34, 11).build());
        }

        updateButtons();
    }

    private void updateButtons() {
        for (int i = 0; i < 6; i++) {
            if (modeButtons[i] == null) continue;
            int mode = this.menu.getFaceMode(i);
            if (mode == VolcaniteSortingStationBlockEntity.MODE_ITEM) {
                modeButtons[i].setMessage(Component.literal("§aITEM"));
            } else if (mode == VolcaniteSortingStationBlockEntity.MODE_LIQUID) {
                modeButtons[i].setMessage(Component.literal("§bLIQ"));
            } else if (mode == VolcaniteSortingStationBlockEntity.MODE_VOID) {
                modeButtons[i].setMessage(Component.literal("§dVOID"));
            } else {
                modeButtons[i].setMessage(Component.literal("§8OFF"));
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        VolcaniteGuiTheme.renderExteriorFrame(guiGraphics, x, y, this.imageWidth, this.imageHeight, true);
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Highlight active slots & sockets with neon glow borders & connection indicators
        for (int i = 0; i < 6; i++) {
            int mode = this.menu.getFaceMode(i);
            int conn = this.menu.getFaceConnection(i);
            boolean hasMod = this.menu.hasAdvancedModule(i);

            int baseSlot = i * 4;
            int modSlotX = x + this.menu.getSlot(baseSlot).x - 1;
            int modSlotY = y + this.menu.getSlot(baseSlot).y - 1;

            // Module slot outline: Golden if installed, dim slate if empty
            if (hasMod) {
                guiGraphics.renderOutline(modSlotX, modSlotY, 18, 18, 0xFFFFCC00);
            } else {
                guiGraphics.renderOutline(modSlotX, modSlotY, 18, 18, 0xFF443850);
            }

            // Outline color depending on face mode:
            int outlineColor;
            if (mode == VolcaniteSortingStationBlockEntity.MODE_ITEM) {
                outlineColor = 0xFF00FF66; // Emerald Green
            } else if (mode == VolcaniteSortingStationBlockEntity.MODE_LIQUID) {
                outlineColor = 0xFF00BFFF; // Cyan Blue
            } else if (mode == VolcaniteSortingStationBlockEntity.MODE_VOID) {
                outlineColor = 0xFFFF0077; // Neon Magenta
            } else {
                outlineColor = 0xFF353040; // Dim Slate
            }

            // Filter 1 is always active
            int s1X = x + this.menu.getSlot(baseSlot + 1).x - 1;
            int s1Y = y + this.menu.getSlot(baseSlot + 1).y - 1;
            guiGraphics.renderOutline(s1X, s1Y, 18, 18, outlineColor);

            // Filter 2 & Filter 3:
            int s2X = x + this.menu.getSlot(baseSlot + 2).x - 1;
            int s2Y = y + this.menu.getSlot(baseSlot + 2).y - 1;
            int s3X = x + this.menu.getSlot(baseSlot + 3).x - 1;
            int s3Y = y + this.menu.getSlot(baseSlot + 3).y - 1;

            if (hasMod) {
                guiGraphics.renderOutline(s2X, s2Y, 18, 18, outlineColor);
                guiGraphics.renderOutline(s3X, s3Y, 18, 18, outlineColor);
            } else {
                guiGraphics.renderOutline(s2X, s2Y, 18, 18, 0xFF2A2233);
                guiGraphics.renderOutline(s3X, s3Y, 18, 18, 0xFF2A2233);
                guiGraphics.fill(s2X + 1, s2Y + 1, s2X + 17, s2Y + 17, 0x88000000);
                guiGraphics.fill(s3X + 1, s3Y + 1, s3X + 17, s3Y + 17, 0x88000000);
            }

            // Connection indicator LED dot on card upper-left corner
            int cx = x + CARD_POS[i][0];
            int cy = y + CARD_POS[i][1];
            int ledColor = (conn == 1) ? 0xFF00FF66 :  // Green (Pipe)
                           (conn == 2) ? 0xFF00CCFF :  // Cyan (Liquid Tank)
                           (conn == 3) ? 0xFFFFCC00 :  // Gold (Machine)
                                         0xFF444050;   // Dark (Disconnected)

            guiGraphics.fill(cx + 3, cy + 4, cx + 5, cy + 10, ledColor);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        updateButtons();

        // Centered Header Title
        Component titleComponent = Component.literal("§6§lSORTING STATION");
        int titleWidth = this.font.width(titleComponent);
        guiGraphics.drawString(this.font, titleComponent, (this.imageWidth - titleWidth) / 2, 6, 0xFFCC44, false);

        // Face labels on each card
        for (int i = 0; i < 6; i++) {
            int cx = CARD_POS[i][0];
            int cy = CARD_POS[i][1];
            int mode = this.menu.getFaceMode(i);

            int nameColor = (mode == VolcaniteSortingStationBlockEntity.MODE_ITEM) ? 0x88FF88 :
                            (mode == VolcaniteSortingStationBlockEntity.MODE_LIQUID) ? 0x88DDFF :
                            (mode == VolcaniteSortingStationBlockEntity.MODE_VOID) ? 0xFFAAFF : 0xAAAAAA;

            // Face title next to LED dot
            guiGraphics.drawString(this.font, Component.literal(FACE_NAMES[i]), cx + 8, cy + 3, nameColor, false);
        }

        // Subtitle Status in center bar (y = 128)
        int pipeCount = 0;
        for (int f = 0; f < 6; f++) {
            if (this.menu.getFaceConnection(f) == 1) pipeCount++;
        }
        Component statusComponent = Component.literal("§eConnected Pipes: §a" + pipeCount + "§7/6 faces");
        int statusWidth = this.font.width(statusComponent);
        guiGraphics.drawString(this.font, statusComponent, (this.imageWidth - statusWidth) / 2, 128, 0xFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        for (int i = 0; i < 6; i++) {
            int baseSlot = i * 4;
            int cardX = x + CARD_POS[i][0];
            int cardY = y + CARD_POS[i][1];
            boolean hasMod = this.menu.hasAdvancedModule(i);

            // 1. Tooltip for Module Socket when hovered and slot is empty
            int modSlotX = x + this.menu.getSlot(baseSlot).x;
            int modSlotY = y + this.menu.getSlot(baseSlot).y;
            if (mouseX >= modSlotX && mouseX < modSlotX + 16 && mouseY >= modSlotY && mouseY < modSlotY + 16) {
                if (!this.menu.getSlot(baseSlot).hasItem()) {
                    java.util.List<Component> tooltip = new java.util.ArrayList<>();
                    tooltip.add(Component.literal("§6Socket: §7Advanced Filter Module"));
                    tooltip.add(Component.literal("§eInstalls Volcanite Advanced Filter Module"));
                    tooltip.add(Component.literal("§8Unlocks 2 extra filter slots (total 3)"));
                    guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                    return;
                }
            }

            // 2. Tooltip for locked Filter 2 & Filter 3 slots
            if (!hasMod) {
                int s2X = x + this.menu.getSlot(baseSlot + 2).x;
                int s2Y = y + this.menu.getSlot(baseSlot + 2).y;
                int s3X = x + this.menu.getSlot(baseSlot + 3).x;
                int s3Y = y + this.menu.getSlot(baseSlot + 3).y;

                if ((mouseX >= s2X && mouseX < s2X + 16 && mouseY >= s2Y && mouseY < s2Y + 16) ||
                    (mouseX >= s3X && mouseX < s3X + 16 && mouseY >= s3Y && mouseY < s3Y + 16)) {
                    java.util.List<Component> tooltip = new java.util.ArrayList<>();
                    tooltip.add(Component.literal("§c🔒 Locked Filter Slot"));
                    tooltip.add(Component.literal("§7Requires an §6Advanced Filter Module"));
                    tooltip.add(Component.literal("§8Install module into socket on the left!"));
                    guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                    return;
                }
            }

            // 3. Tooltip for card header (face title & connection status)
            if (mouseX >= cardX && mouseX <= cardX + 40 && mouseY >= cardY && mouseY <= cardY + 12) {
                int mode = this.menu.getFaceMode(i);
                int conn = this.menu.getFaceConnection(i);

                java.util.List<Component> tooltip = new java.util.ArrayList<>();
                tooltip.add(Component.literal("§6Face: §f" + FACE_NAMES[i]));

                if (conn == 1) {
                    tooltip.add(Component.literal("§a✔ Connected: Volcanite Pipe / Conduit"));
                } else if (conn == 2) {
                    tooltip.add(Component.literal("§b✔ Connected: Liquid Tank"));
                } else if (conn == 3) {
                    tooltip.add(Component.literal("§6✔ Connected: Machine / Container"));
                } else {
                    tooltip.add(Component.literal("§7○ Connection: §8Disconnected"));
                }

                if (mode == VolcaniteSortingStationBlockEntity.MODE_ITEM) {
                    tooltip.add(Component.literal("§aMode: ITEM"));
                    tooltip.add(Component.literal("§8Routes matching items to this face"));
                } else if (mode == VolcaniteSortingStationBlockEntity.MODE_LIQUID) {
                    tooltip.add(Component.literal("§bMode: LIQUID"));
                    tooltip.add(Component.literal("§8Routes matching liquid to this face"));
                } else if (mode == VolcaniteSortingStationBlockEntity.MODE_VOID) {
                    tooltip.add(Component.literal("§dMode: VOID"));
                    tooltip.add(Component.literal("§cDeletes excess matching items/fluids!"));
                } else {
                    tooltip.add(Component.literal("§7Mode: §8DISABLED (Off)"));
                }

                guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                return;
            }
        }
    }
}

