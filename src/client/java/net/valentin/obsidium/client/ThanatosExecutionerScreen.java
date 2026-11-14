package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.block.ThanatosExecutionerMenu;

import java.util.List;

public class ThanatosExecutionerScreen extends AbstractContainerScreen<ThanatosExecutionerMenu> {
    private Button targetModeButton;
    private Button claimXpButton;
    private Button spawnerButton;

    public ThanatosExecutionerScreen(ThanatosExecutionerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 1000; // Custom drawn
        this.inventoryLabelX = 1000;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Button 0: Target Filter (Hostile Mobs vs All Mobs)
        this.targetModeButton = this.addRenderableWidget(Button.builder(Component.literal("⚔ Hostiles"), b -> clickBtn(0))
                .bounds(x + 10, y + 54, 46, 14)
                .build());

        // Button 1: Claim Stored XP
        this.claimXpButton = this.addRenderableWidget(Button.builder(Component.literal("🧪 0 XP"), b -> clickBtn(1))
                .bounds(x + 130, y + 18, 38, 14)
                .build());

        // Button 2: Toggle Mob Lure Spawner
        this.spawnerButton = this.addRenderableWidget(Button.builder(Component.literal("☠ Lure"), b -> clickBtn(2))
                .bounds(x + 130, y + 35, 38, 14)
                .build());

        updateButtons();
    }

    private void clickBtn(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private void updateButtons() {
        if (this.targetModeButton != null) {
            boolean hostilesOnly = this.menu.getTargetMode() == 0;
            this.targetModeButton.setMessage(Component.literal(hostilesOnly ? "§c⚔ Hostile" : "§6⚔ All"));
        }
        if (this.claimXpButton != null) {
            int xp = this.menu.getStoredXp();
            this.claimXpButton.setMessage(Component.literal("§a🧪 " + (xp > 999 ? (xp / 1000 + "k") : xp)));
            this.claimXpButton.active = xp > 0;
        }
        if (this.spawnerButton != null) {
            boolean active = this.menu.getSpawnerEnabled() == 1;
            this.spawnerButton.setMessage(Component.literal(active ? "§d☠ Lure" : "§7○ Lure"));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        updateButtons();

        // Title on the top bar - clean and uncrowded
        guiGraphics.drawString(this.font, Component.literal("§4☠ §fThanatos"), 8, 5, 0xFFFFFF, false);

        // Section labels with ample clearance
        guiGraphics.drawString(this.font, Component.literal("§8SWORD"), 18, 23, 0x94A3B8, false);
        guiGraphics.drawString(this.font, Component.literal("§8INVENTORY"), 8, 73, 0x64748B, false);

        // Auto-Output Badge (Right side of header)
        guiGraphics.drawString(this.font, Component.literal("§a➔ Chest"), 126, 5, 0x10B981, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Base window shadow and deep titanium charcoal casing
        guiGraphics.fill(x - 1, y - 1, x + this.imageWidth + 1, y + this.imageHeight + 1, 0xFF080A0E);
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF14161D);

        // Metallic perimeter bezel
        guiGraphics.fill(x, y, x + this.imageWidth, y + 1, 0xFF2A3140);
        guiGraphics.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF181C24);
        guiGraphics.fill(x, y, x + 1, y + this.imageHeight, 0xFF2A3140);
        guiGraphics.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, 0xFF181C24);

        // Crimson Thanatos corner pins (2x2)
        int pinColor = 0xFFDC2626;
        guiGraphics.fill(x + 1, y + 1, x + 3, y + 3, pinColor);
        guiGraphics.fill(x + this.imageWidth - 3, y + 1, x + this.imageWidth - 1, y + 3, pinColor);
        guiGraphics.fill(x + 1, y + this.imageHeight - 3, x + 3, y + this.imageHeight - 1, pinColor);
        guiGraphics.fill(x + this.imageWidth - 3, y + this.imageHeight - 3, x + this.imageWidth - 1, y + this.imageHeight - 1, pinColor);

        // Top Header Bar
        guiGraphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + 16, 0xFF1B1E28);
        guiGraphics.fill(x + 1, y + 16, x + this.imageWidth - 1, y + 17, 0xFF282E3E);

        // Slot 9: Weapon / Sword Upgrade Slot (at x + 20, y + 35) - Crimson Death Frame
        drawSlotFrame(guiGraphics, x + 20, y + 35, 0xFFDC2626, 0xFF1A1214);

        // Slots 0..8: 3x3 Loot Buffer (at x + 70 + col*18, y + 17 + row*18)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int sx = x + 70 + col * 18;
                int sy = y + 17 + row * 18;
                drawSlotFrame(guiGraphics, sx, sy, 0xFF2E3748, 0xFF0D0F14);
            }
        }

        // Energy Indicator Gauge (Vertical bar at x + 130, y + 52, w = 38, h = 16)
        int energy = this.menu.getEnergy();
        int maxEnergy = 200_000;
        int barW = (int) (36.0 * Math.min(1.0, (double) energy / maxEnergy));
        guiGraphics.fill(x + 130, y + 52, x + 168, y + 68, 0xFF0D1217);
        guiGraphics.fill(x + 131, y + 53, x + 131 + barW, y + 67, (energy > 0) ? 0xFFF59E0B : 0xFFEF4444);
        guiGraphics.drawString(this.font, "§e⚡", x + 133, y + 56, 0xFFFFFF, false);

        // Player Inventory (3 rows x 9 columns at x = 8, y = 84)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int sx = x + 8 + col * 18;
                int sy = y + 84 + row * 18;
                drawSlotFrame(guiGraphics, sx, sy, 0xFF242A36, 0xFF0C0E12);
            }
        }

        // Player Hotbar (9 slots at x = 8, y = 142)
        for (int col = 0; col < 9; ++col) {
            int sx = x + 8 + col * 18;
            int sy = y + 142;
            drawSlotFrame(guiGraphics, sx, sy, 0xFF242A36, 0xFF0C0E12);
        }
    }

    private static void drawSlotFrame(GuiGraphics g, int sx, int sy, int borderColor, int innerColor) {
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, borderColor);
        g.fill(sx, sy, sx + 16, sy + 16, innerColor);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Spawner / Lure Button Tooltip
        if (mouseX >= x + 130 && mouseX <= x + 168 && mouseY >= y + 35 && mouseY <= y + 49) {
            boolean active = this.menu.getSpawnerEnabled() == 1;
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§d☠ Thanatos Mob Lure"),
                    Component.literal("§7Status: " + (active ? "§aActive (Spawning Mobs)" : "§cDisabled")),
                    Component.literal("§7Automatically summons monsters inside"),
                    Component.literal("§7the 5x5 chamber every 7 seconds."),
                    Component.literal("§8Cost: 4,000 RF per spawn | Max 5 mobs")
            ), mouseX, mouseY);
        }
        // Claim XP Button Tooltip
        else if (mouseX >= x + 130 && mouseX <= x + 168 && mouseY >= y + 18 && mouseY <= y + 32) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§a🧪 Thanatos Soul XP"),
                    Component.literal(String.format("§7Stored: §a%,d XP points", this.menu.getStoredXp())),
                    Component.literal("§eClick to withdraw XP into your level bar!")
            ), mouseX, mouseY);
        }
        // Target Mode Button Tooltip
        else if (mouseX >= x + 10 && mouseX <= x + 56 && mouseY >= y + 54 && mouseY <= y + 68) {
            boolean hostiles = this.menu.getTargetMode() == 0;
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§c⚔ Target Filter"),
                    Component.literal("§7Mode: " + (hostiles ? "§cHostiles Only" : "§6All Mobs")),
                    Component.literal("§7Click to toggle between monsters only"),
                    Component.literal("§7and all entities (excluding tamed pets).")
            ), mouseX, mouseY);
        }
        // Energy Gauge Tooltip
        else if (mouseX >= x + 130 && mouseX <= x + 168 && mouseY >= y + 52 && mouseY <= y + 68) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6⚡ Volcanite Power Buffer"),
                    Component.literal(String.format("§7Stored Energy: §e%,d / 200,000 RF", this.menu.getEnergy())),
                    Component.literal("§8Consumes 1,500 RF per strike, 4,000 RF per summon")
            ), mouseX, mouseY);
        }
        // Auto-Output Badge Tooltip
        else if (mouseX >= x + 124 && mouseX <= x + 172 && mouseY >= y + 2 && mouseY <= y + 16) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§a✓ Automatic Chest Output"),
                    Component.literal("§7Any chest, barrel, modular chest or pipe connected"),
                    Component.literal("§7to this machine automatically receives all collected loot!")
            ), mouseX, mouseY);
        }
        // Weapon Slot Tooltip
        else if (mouseX >= x + 20 && mouseX <= x + 36 && mouseY >= y + 35 && mouseY <= y + 51 && this.menu.getSlot(0).getItem().isEmpty()) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§cExecutioner Sword Slot"),
                    Component.literal("§7Insert a sword (e.g. Netherite with Looting III)"),
                    Component.literal("§7to greatly amplify loot drop rates and strike damage!")
            ), mouseX, mouseY);
        }
    }
}
