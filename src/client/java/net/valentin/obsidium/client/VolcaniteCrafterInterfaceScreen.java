package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.block.VolcaniteCrafterInterfaceMenu;

import java.util.List;

public class VolcaniteCrafterInterfaceScreen extends AbstractContainerScreen<VolcaniteCrafterInterfaceMenu> {
    private Button autoCraftButton;
    private Button stockLimitButton;
    private Button refillToggleButton;
    private Button storageTerminalButton;

    public VolcaniteCrafterInterfaceScreen(VolcaniteCrafterInterfaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 202;
        this.imageHeight = 216;
        this.titleLabelX = 1000;
        this.inventoryLabelX = 1000;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Button 5: Switch to Storage Terminal (Top right)
        this.storageTerminalButton = this.addRenderableWidget(Button.builder(Component.literal("§6📦 Terminal"), b -> clickBtn(5))
                .bounds(x + 140, y + 4, 54, 14)
                .build());

        // Button 1: Clear 3x3 Grid to Storage Network
        this.addRenderableWidget(Button.builder(Component.literal("§e➔Net"), b -> clickBtn(1))
                .bounds(x + 12, y + 98, 30, 16)
                .build());

        // Button 4: Toggle Auto-Refill 3x3 Grid from Network
        this.refillToggleButton = this.addRenderableWidget(Button.builder(Component.literal("§a↻Sync"), b -> clickBtn(4))
                .bounds(x + 44, y + 98, 33, 16)
                .build());

        // Button 0: Save / Load Blueprint (under Slot 10)
        this.addRenderableWidget(Button.builder(Component.literal("§b💾BP"), b -> clickBtn(0))
                .bounds(x + 79, y + 98, 31, 16)
                .build());

        // Button 2: Toggle Auto-Crafter ON / OFF (Right panel)
        this.autoCraftButton = this.addRenderableWidget(Button.builder(Component.literal("§a⚡ Auto"), b -> clickBtn(2))
                .bounds(x + 122, y + 80, 70, 15)
                .build());

        // Button 3: Cycle Auto-Craft Stock Limit (64 / 256 / 1024 / ∞)
        this.stockLimitButton = this.addRenderableWidget(Button.builder(Component.literal("Limit: 256"), b -> clickBtn(3))
                .bounds(x + 122, y + 98, 70, 16)
                .build());

        updateButtonLabels();
    }

    private void clickBtn(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private void updateButtonLabels() {
        if (this.autoCraftButton != null) {
            boolean on = this.menu.isAutoCraftEnabled();
            this.autoCraftButton.setMessage(Component.literal(on ? "§a⚡ AUTO: ON" : "§c⚡ AUTO: OFF"));
        }
        if (this.stockLimitButton != null) {
            String stockStr = switch (this.menu.getStockMode()) {
                case 0 -> "§eLimit: 64";
                case 1 -> "§eLimit: 256";
                case 2 -> "§bLimit: 1024";
                default -> "§dLimit: ∞";
            };
            this.stockLimitButton.setMessage(Component.literal(stockStr));
        }
        if (this.refillToggleButton != null) {
            boolean refill = this.menu.isAutoRefillGrid();
            this.refillToggleButton.setMessage(Component.literal(refill ? "§a↻Sync" : "§7↻Sync"));
        }
        if (this.storageTerminalButton != null) {
            this.storageTerminalButton.active = this.menu.isNetworkOnline();
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        updateButtonLabels();

        // Title on the top bar
        guiGraphics.drawString(this.font, Component.literal("§6◈ §fCrafter Interface"), 10, 7, 0xFFFFFF, false);

        // Clean section headers with ample vertical breathing room
        guiGraphics.drawString(this.font, Component.literal("§8CRAFTING"), 14, 26, 0xFF94A3B8, false);
        guiGraphics.drawString(this.font, Component.literal("§3AUTOMATION"), 126, 26, 0xFF0284C7, false);

        // Player Inventory Section Title
        guiGraphics.drawString(this.font, Component.literal("§8INVENTORY"), 21, 125, 0xFF64748B, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Base window shadow and deep titanium charcoal body
        guiGraphics.fill(x - 1, y - 1, x + this.imageWidth + 1, y + this.imageHeight + 1, 0xFF080A0E);
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF12141A);

        // Sophisticated metallic perimeter bezel
        guiGraphics.fill(x, y, x + this.imageWidth, y + 1, 0xFF2E3544);
        guiGraphics.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF181C24);
        guiGraphics.fill(x, y, x + 1, y + this.imageHeight, 0xFF2A3140);
        guiGraphics.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, 0xFF1C212B);

        // Refined corner metallic amber pins (2x2)
        int pinColor = 0xFFD97706;
        guiGraphics.fill(x + 1, y + 1, x + 3, y + 3, pinColor);
        guiGraphics.fill(x + this.imageWidth - 3, y + 1, x + this.imageWidth - 1, y + 3, pinColor);
        guiGraphics.fill(x + 1, y + this.imageHeight - 3, x + 3, y + this.imageHeight - 1, pinColor);
        guiGraphics.fill(x + this.imageWidth - 3, y + this.imageHeight - 3, x + this.imageWidth - 1, y + this.imageHeight - 1, pinColor);

        // 1. Top Header Bar (0..22)
        guiGraphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + 22, 0xFF161922);
        guiGraphics.fill(x + 1, y + 22, x + this.imageWidth - 1, y + 23, 0xFF232A37);

        // Network Status Energy Badge (compact icon badge at x + 118, y + 4, w=18, h=14)
        boolean online = this.menu.isNetworkOnline();
        int pillX = x + 118;
        int pillY = y + 4;
        int pillW = 18;
        int pillH = 14;
        int pillBg = online ? 0xFF0B291D : 0xFF261012;
        int pillBorder = online ? 0xFF10B981 : 0xFFEF4444;
        guiGraphics.fill(pillX, pillY, pillX + pillW, pillY + pillH, pillBg);
        guiGraphics.fill(pillX, pillY, pillX + pillW, pillY + 1, pillBorder);
        guiGraphics.fill(pillX, pillY + pillH - 1, pillX + pillW, pillY + pillH, pillBorder);
        guiGraphics.fill(pillX, pillY, pillX + 1, pillY + pillH, pillBorder);
        guiGraphics.fill(pillX + pillW - 1, pillY, pillX + pillW, pillY + pillH, pillBorder);

        String pillText = online ? "§a⚡" : "§c⚡";
        int textX = pillX + (pillW - this.font.width(pillText)) / 2;
        guiGraphics.drawString(this.font, pillText, textX, pillY + 3, 0xFFFFFF, false);

        // 2. Left Card: Manual Crafting & Pattern Encoder (x+8..114, y+24..118)
        guiGraphics.fill(x + 8, y + 23, x + 114, y + 118, 0xFF161922);
        guiGraphics.fill(x + 8, y + 23, x + 114, y + 24, 0xFF242C3B);
        guiGraphics.fill(x + 8, y + 24, x + 114, y + 34, 0xFF1B202B);
        guiGraphics.fill(x + 8, y + 34, x + 114, y + 35, 0xFF222835);

        // 3x3 Grid Slots at (13 + col*18, 38 + row*18)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int sx = x + 13 + col * 18;
                int sy = y + 38 + row * 18;
                drawSlotFrame(guiGraphics, sx, sy, 0xFF2A3140, 0xFF0B0D12);
            }
        }

        // Crafting Arrow between 3x3 grid and Output Slot 9
        guiGraphics.drawString(this.font, "➔", x + 71, y + 51, 0xFFF59E0B, false);

        // Slot 9: Output Slot at (88, 47) - Golden Amber Frame
        guiGraphics.fill(x + 86, y + 45, x + 106, y + 65, 0xFFF59E0B);
        guiGraphics.fill(x + 87, y + 46, x + 105, y + 64, 0xFF16130E);

        // Slot 10: Blueprint Encoder Slot at (88, 74) - Ice Cyan Frame
        guiGraphics.fill(x + 87, y + 73, x + 105, y + 91, 0xFF0284C7);
        guiGraphics.fill(x + 88, y + 74, x + 104, y + 90, 0xFF0A121A);

        // 3. Right Card: Automated Blueprint Crafter (x+118..194, y+23..118)
        guiGraphics.fill(x + 118, y + 23, x + 194, y + 118, 0xFF141720);
        guiGraphics.fill(x + 118, y + 23, x + 194, y + 24, 0xFF222B3B);
        guiGraphics.fill(x + 118, y + 24, x + 194, y + 34, 0xFF1A212E);
        guiGraphics.fill(x + 118, y + 34, x + 194, y + 35, 0xFF1F2736);

        // Slots 11..14: 4 Blueprint Bays at (126 + col*20, 38 + row*20)
        boolean autoOn = this.menu.isAutoCraftEnabled() && online;
        int bpBorder = autoOn ? 0xFF0284C7 : 0xFF243040;
        for (int i = 0; i < 4; i++) {
            int col = i % 2;
            int row = i / 2;
            int sx = x + 126 + col * 20;
            int sy = y + 38 + row * 20;
            drawSlotFrame(guiGraphics, sx, sy, bpBorder, 0xFF090D14);
        }

        // Slot 15: Speed / Batch Upgrade Slot at (171, 48) - Violet Amethyst Frame
        int upgBorder = switch (this.menu.getUpgradeTier()) {
            case 1 -> 0xFFF59E0B;
            case 2 -> 0xFF38BDF8;
            case 3 -> 0xFFEC4899;
            default -> 0xFF8B5CF6;
        };
        drawSlotFrame(guiGraphics, x + 171, y + 48, upgBorder, 0xFF120E1A);

        // 4. Divider Line at y = 122
        guiGraphics.fill(x + 8, y + 122, x + this.imageWidth - 8, y + 123, 0xFF222834);

        // 5. Lower Section: Player Inventory (20 + col*18, 136 + row*18) & Hotbar (20 + col*18, 194)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int sx = x + 20 + col * 18;
                int sy = y + 136 + row * 18;
                drawSlotFrame(guiGraphics, sx, sy, 0xFF262C38, 0xFF0C0E12);
            }
        }
        for (int col = 0; col < 9; ++col) {
            int sx = x + 20 + col * 18;
            int sy = y + 194;
            drawSlotFrame(guiGraphics, sx, sy, 0xFF262C38, 0xFF0C0E12);
        }
    }

    private static void drawSlotFrame(GuiGraphics g, int sx, int sy, int borderColor, int innerColor) {
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, borderColor);
        g.fill(sx, sy, sx + 16, sy + 16, innerColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        if (VolcaniteGuiTheme.handleMuteClick(x, y, this.imageWidth, mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Machine Sound Mute Tooltip
        VolcaniteGuiTheme.renderMuteTooltip(this.font, guiGraphics, x, y, this.imageWidth, mouseX, mouseY);

        // Network Status Energy Badge Tooltip
        if (mouseX >= x + 118 && mouseX <= x + 136 && mouseY >= y + 4 && mouseY <= y + 18) {
            boolean online = this.menu.isNetworkOnline();
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6⚡ Volcanite Network Power"),
                    Component.literal(online ? "§a● Grid Status: ONLINE" : "§c○ Grid Status: OFFLINE"),
                    Component.literal("§7Grid Energy Buffer: §e" + this.menu.getNetworkEnergyPercent() + "% RF"),
                    Component.literal(online ? "§7Connected Controller: §aActive" : "§7Connected Controller: §cMissing / Unpowered")
            ), mouseX, mouseY);
        }
        // Custom Tooltip for Blueprint Encoder Slot (Slot 10)
        else if (mouseX >= x + 87 && mouseX <= x + 105 && mouseY >= y + 73 && mouseY <= y + 91 && this.menu.getSlot(10).getItem().isEmpty()) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§bBlueprint Encoder / Loader Slot"),
                    Component.literal("§7Place a §bVolcanite Blueprint §7here and click §b[💾BP]§7:"),
                    Component.literal("§8• If 3x3 grid has a recipe: §aSaves recipe to Blueprint"),
                    Component.literal("§8• If 3x3 grid is empty: §ePulls Blueprint ingredients from Network")
            ), mouseX, mouseY);
        }
        // Custom Tooltip for Speed Upgrade Slot (Slot 15)
        else if (mouseX >= x + 170 && mouseX <= x + 188 && mouseY >= y + 47 && mouseY <= y + 65 && this.menu.getSlot(15).getItem().isEmpty()) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§d★ Auto-Crafter Speed Upgrade Slot"),
                    Component.literal("§7Accepts §dStack Upgrade§7, §bOverclock Upgrade§7, or §6Conduit Upgrade"),
                    Component.literal("§7to accelerate auto-crafting up to §a8x batch @ 10x speed§7!")
            ), mouseX, mouseY);
        }
        // Custom Tooltip for Output Slot (Slot 9)
        else if (mouseX >= x + 86 && mouseX <= x + 106 && mouseY >= y + 45 && mouseY <= y + 65 && this.menu.getSlot(9).getItem().isEmpty()) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6Crafting Result Slot"),
                    Component.literal("§7Click or Shift-click to craft directly from the 3x3 grid.")
            ), mouseX, mouseY);
        }
        // Button Tooltips
        else if (mouseX >= x + 44 && mouseX <= x + 77 && mouseY >= y + 98 && mouseY <= y + 114) {
            boolean refill = this.menu.isAutoRefillGrid();
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§aAuto-Refill Grid from Network " + (refill ? "§2[ON]" : "§8[OFF]")),
                    Component.literal("§7When ON, crafting items from the 3x3 grid automatically"),
                    Component.literal("§7replenishes consumed ingredients directly from your Storage Network!")
            ), mouseX, mouseY);
        } else if (mouseX >= x + 12 && mouseX <= x + 42 && mouseY >= y + 98 && mouseY <= y + 114) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§eClear Grid -> Storage Network"),
                    Component.literal("§7Returns all items currently in the 3x3 grid directly into your Storage Network.")
            ), mouseX, mouseY);
        } else if (mouseX >= x + 79 && mouseX <= x + 110 && mouseY >= y + 98 && mouseY <= y + 114) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§bEncode / Load Blueprint"),
                    Component.literal("§7Saves the 3x3 grid recipe to the Blueprint in the slot above,"),
                    Component.literal("§7or loads the Blueprint's ingredients into the grid.")
            ), mouseX, mouseY);
        } else if (mouseX >= x + 122 && mouseX <= x + 192 && mouseY >= y + 80 && mouseY <= y + 95) {
            boolean on = this.menu.isAutoCraftEnabled();
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§aAuto-Crafter Toggle " + (on ? "§2[ACTIVE]" : "§c[PAUSED]")),
                    Component.literal("§7When active, automatically crafts recipes from installed Blueprints"),
                    Component.literal("§7whenever storage levels drop below the configured stock limit.")
            ), mouseX, mouseY);
        } else if (mouseX >= x + 122 && mouseX <= x + 192 && mouseY >= y + 98 && mouseY <= y + 114) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§eAuto-Craft Stock Limit"),
                    Component.literal("§7Cycles the stock threshold: §e64 §7➔ §e256 §7➔ §b1024 §7➔ §dInfinite§7."),
                    Component.literal("§7Stops auto-crafting when the network already has this amount stored.")
            ), mouseX, mouseY);
        } else if (mouseX >= x + 140 && mouseX <= x + 196 && mouseY >= y + 4 && mouseY <= y + 18) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6Open Storage Terminal"),
                    Component.literal("§7Instantly opens the connected Volcanite Storage Terminal.")
            ), mouseX, mouseY);
        }
    }

    public int getLeft() {
        return this.leftPos;
    }

    public int getTop() {
        return this.topPos;
    }
}
