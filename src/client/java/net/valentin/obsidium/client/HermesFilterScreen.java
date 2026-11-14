package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.menu.HermesFilterMenu;

import java.util.List;

public class HermesFilterScreen extends AbstractContainerScreen<HermesFilterMenu> {
    private Button toggleModeButton;

    public HermesFilterScreen(HermesFilterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 74;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Toggle Whitelist / Blacklist (Button ID 0)
        this.toggleModeButton = this.addRenderableWidget(Button.builder(Component.literal("Mode"), b -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                updateButtons();
            }
        }).bounds(x + 10, y + 24, 46, 20).build());

        // Clear All (Button ID 1)
        this.addRenderableWidget(Button.builder(Component.literal("Clear"), b -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
            }
        }).bounds(x + 10, y + 48, 46, 20).build());

        updateButtons();
    }

    private void updateButtons() {
        if (toggleModeButton != null) {
            boolean bl = this.menu.isBlacklist();
            toggleModeButton.setMessage(Component.literal(bl ? "§cBlack" : "§aWhite"));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        updateButtons();
        String label = this.title.getString();
        guiGraphics.drawString(this.font, label.startsWith("✦") ? label : "✦ " + label, this.titleLabelX, this.titleLabelY, 0xFFD700, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xCECAC5, false);

        boolean bl = this.menu.isBlacklist();
        guiGraphics.drawString(this.font, bl ? "§c[Blacklist]" : "§a[Whitelist]", 120, 30, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "§79 Slots", 120, 44, 0xAAAAAA, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Tooltip over Mode Button
        if ((this.hoveredSlot == null || !this.hoveredSlot.hasItem()) && mouseX >= x + 10 && mouseX <= x + 56 && mouseY >= y + 24 && mouseY <= y + 44) {
            boolean bl = this.menu.isBlacklist();
            String titleStr = this.title.getString();
            String desc;
            if (titleStr.contains("Void")) {
                desc = bl ? "§7Keeps listed items; voids all others." : "§7Eliminates (voids) ONLY listed items.";
            } else if (titleStr.contains("Compactor")) {
                desc = bl ? "§7Skips listed items; compacts others." : "§7Auto-compacts ONLY listed items.";
            } else {
                desc = bl ? "§7Blocks listed items from passing." : "§7Allows ONLY listed items to pass.";
            }
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal(bl ? "§cMode: BLACKLIST" : "§aMode: WHITELIST"),
                    Component.literal(desc),
                    Component.literal("§eClick to switch mode.")
            ), mouseX, mouseY);
        }
        // Tooltip over Clear Button
        else if ((this.hoveredSlot == null || !this.hoveredSlot.hasItem()) && mouseX >= x + 10 && mouseX <= x + 56 && mouseY >= y + 48 && mouseY <= y + 68) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§eClear Filter"),
                    Component.literal("§7Removes all 9 configured filter items.")
            ), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        VolcaniteGuiTheme.renderExteriorFrame(guiGraphics, x, y, this.imageWidth, this.imageHeight, false);

        // Hermes Deep Cyan / Gold Theme Panel
        guiGraphics.fill(x + 6, y + 16, x + this.imageWidth - 6, y + 74, 0xFF0D1820);
        border(guiGraphics, x + 6, y + 16, this.imageWidth - 12, 58, 0xFF1B4055);

        // 3x3 Filter Slots Grid Frame
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int sx = x + 61 + c * 18;
                int sy = y + 19 + r * 18;
                guiGraphics.fill(sx, sy, sx + 18, sy + 18, 0xFF060B0F);
                border(guiGraphics, sx, sy, 18, 18, 0xFF2A607C);
            }
        }
        // Outer gold border around 3x3 grid
        border(guiGraphics, x + 60, y + 18, 56, 56, 0xFFFFCC00);

        // Player Inventory slots background
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int sx = x + 7 + col * 18;
                int sy = y + 85 + row * 18;
                guiGraphics.fill(sx, sy, sx + 18, sy + 18, 0xFF0B0810);
                border(guiGraphics, sx, sy, 18, 18, 0xFF282030);
            }
        }

        // Hotbar slots background
        for (int col = 0; col < 9; ++col) {
            int sx = x + 7 + col * 18;
            int sy = y + 143;
            guiGraphics.fill(sx, sy, sx + 18, sy + 18, 0xFF0B0810);
            border(guiGraphics, sx, sy, 18, 18, 0xFF352B40);
        }
    }

    private void border(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }
}

