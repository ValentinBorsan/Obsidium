package net.valentin.obsidium.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.block.AdvancedVolcaniteFurnaceBlockEntity;
import net.valentin.obsidium.block.AdvancedVolcaniteFurnaceMenu;

import java.util.List;

public class AdvancedVolcaniteFurnaceScreen extends AbstractContainerScreen<AdvancedVolcaniteFurnaceMenu> {

    private static final int SPLIT_BTN_X = 114;
    private static final int SPLIT_BTN_Y = 20;
    private static final int EJECT_BTN_X = 114;
    private static final int EJECT_BTN_Y = 36;
    private static final int BTN_W = 32;
    private static final int BTN_H = 13;

    public AdvancedVolcaniteFurnaceScreen(AdvancedVolcaniteFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 184;
        this.imageHeight = 186;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 12;
        this.inventoryLabelY = 91;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, "⚡ Advanced Tri-Smelter", this.titleLabelX, this.titleLabelY, 0xFF00E5FF, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFCECAC5, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        if (button == 0 && VolcaniteGuiTheme.handleMuteClick(i, j, this.imageWidth, mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && mouseX >= i + SPLIT_BTN_X && mouseX <= i + SPLIT_BTN_X + BTN_W
                && mouseY >= j + SPLIT_BTN_Y && mouseY <= j + SPLIT_BTN_Y + BTN_H) {
            clickServerButton(0);
            return true;
        }
        if (button == 0 && mouseX >= i + EJECT_BTN_X && mouseX <= i + EJECT_BTN_X + BTN_W
                && mouseY >= j + EJECT_BTN_Y && mouseY <= j + EJECT_BTN_Y + BTN_H) {
            clickServerButton(1);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void clickServerButton(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.15F));
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        VolcaniteGuiTheme.renderMuteTooltip(this.font, g, i, j, this.imageWidth, mouseX, mouseY);

        if (mouseX >= i + 10 && mouseX <= i + 22 && mouseY >= j + 18 && mouseY <= j + 80) {
            int rf = this.menu.getEnergyAmount();
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("§b⚡ Tri-Phase Plasma Capacitor"),
                    Component.literal("§fCharge: §b" + String.format("%,d / 250,000 RF", rf)),
                    Component.literal("§7Draws §e80 RF/t §7per active chamber (§c240 RF/t §7at 3×)"),
                    this.menu.isThermal() ? Component.literal("§c★ Geothermal Heat: Free Power & +50% Speed!") : Component.literal("§7Place next to Lava/Core for free heat")
            ), mouseX, mouseY);
        }

        if (mouseX >= i + 25 && mouseX <= i + 34 && mouseY >= j + 18 && mouseY <= j + 80) {
            int temp = this.menu.getCoilTemp();
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6🔥 Thermal Momentum"),
                    Component.literal("§eCoil Temp: §f" + temp + "°C / 2,500°C"),
                    Component.literal(temp >= 1500 ? "§a★ Momentum Active: +25% Speed!" : "§7Reaches +25% Speed at 1,500°C")
            ), mouseX, mouseY);
        }

        if (mouseX >= i + SPLIT_BTN_X && mouseX <= i + SPLIT_BTN_X + BTN_W
                && mouseY >= j + SPLIT_BTN_Y && mouseY <= j + SPLIT_BTN_Y + BTN_H) {
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("§b⇄ Auto-Split: " + (this.menu.isAutoSplit() ? "§aON" : "§cOFF")),
                    Component.literal("§7Automatically distributes stacks across all 3 chambers!")
            ), mouseX, mouseY);
        }

        if (mouseX >= i + EJECT_BTN_X && mouseX <= i + EJECT_BTN_X + BTN_W
                && mouseY >= j + EJECT_BTN_Y && mouseY <= j + EJECT_BTN_Y + BTN_H) {
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("§e📤 Auto-Eject: " + (this.menu.isAutoEject() ? "§aON" : "§cOFF")),
                    Component.literal("§7Automatically pushes finished ingots into adjacent chests")
            ), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        long now = System.currentTimeMillis();

        VolcaniteGuiTheme.renderWindow(g, i, j, this.imageWidth, this.imageHeight, true);
        VolcaniteGuiTheme.renderRecessedPanel(g, i + 7, j + 16, 170, 69);
        VolcaniteGuiTheme.renderPlayerInventory(g, i + 12, j + 102);

        // 1. Energy Capacitor Gauge (10, 18, 12x62)
        int rf = this.menu.getEnergyAmount();
        int rfH = Math.min(60, (int) (rf / 250000.0f * 60));
        g.fill(i + 10, j + 18, i + 22, j + 80, 0xFF09060E);
        if (rfH > 0) {
            g.fillGradient(i + 11, j + 79 - rfH, i + 21, j + 79, 0xFF00E5FF, 0xFF0284C7);
            int arcY = j + 78 - (int) ((now / 35) % Math.max(1, rfH));
            g.fill(i + 12, arcY, i + 20, arcY + 1, 0xFFE0F2FE);
        }
        g.renderOutline(i + 10, j + 18, 12, 62, 0xFF0369A1);

        // 2. Thermal Momentum Gauge (25, 18, 9x62)
        int temp = this.menu.getCoilTemp();
        int tempH = Math.min(60, Math.max(0, (int) ((temp - 300) / 2200.0f * 60)));
        g.fill(i + 25, j + 18, i + 34, j + 80, 0xFF09060E);
        if (tempH > 0) {
            int topCol = temp >= 1500 ? 0xFFFFEA79 : 0xFFF97316;
            g.fillGradient(i + 26, j + 79 - tempH, i + 33, j + 79, topCol, 0xFF991B1B);
        }
        g.renderOutline(i + 25, j + 18, 9, 62, temp >= 1500 ? 0xFFFFD54F : 0xFF7C2D12);

        // 3. 3 Parallel Induction Chambers (x = 42, 66, 90)
        for (int c = 0; c < 3; c++) {
            int sx = i + 42 + c * 24;
            int prog = this.menu.getProgress(c);
            boolean active = prog > 0;

            VolcaniteGuiTheme.renderSlot(g, sx, j + 20, active ? 0xFF00E5FF : 0xFF0077B6);
            VolcaniteGuiTheme.renderSlot(g, sx, j + 60, 0xFFFFD54F);

            // Vertical Plasma Induction Channel (sx + 4, j + 38, 8x19)
            int chX = sx + 4;
            int chY = j + 38;
            g.fill(chX, chY, chX + 8, chY + 19, 0xFF09060E);
            g.renderOutline(chX, chY, 8, 19, active ? 0xFF00E5FF : 0xFF2B2040);

            int fillH = Math.min(17, (int) (17.0f * prog / AdvancedVolcaniteFurnaceBlockEntity.MAX_PROGRESS));
            if (fillH > 0) {
                g.fillGradient(chX + 1, chY + 1, chX + 7, chY + 1 + fillH, 0xFF00E5FF, 0xFFF97316);
                int sparkY = chY + 1 + (int) ((now / 40 + c * 5) % Math.max(1, fillH));
                g.fill(chX + 2, sparkY, chX + 6, sparkY + 1, 0xFFFFFFFF);
            }
        }

        // 4. Shared Ash Slot (120, 60) & Upgrade Slots (152, 20 & 42)
        VolcaniteGuiTheme.renderSlot(g, i + 120, j + 60, 0xFF71717A);
        VolcaniteGuiTheme.renderUpgradeSlot(g, i + 151, j + 19, this.menu.getSlot(7).hasItem());
        VolcaniteGuiTheme.renderUpgradeSlot(g, i + 151, j + 41, this.menu.getSlot(8).hasItem());

        // 5. Auto-Split & Auto-Eject Toggle Buttons
        renderToggleBtn(g, i + SPLIT_BTN_X, j + SPLIT_BTN_Y, "SPLIT", this.menu.isAutoSplit(), mouseX, mouseY, i, j, SPLIT_BTN_X, SPLIT_BTN_Y);
        renderToggleBtn(g, i + EJECT_BTN_X, j + EJECT_BTN_Y, "EJECT", this.menu.isAutoEject(), mouseX, mouseY, i, j, EJECT_BTN_X, EJECT_BTN_Y);
    }

    private void renderToggleBtn(GuiGraphics g, int x, int y, String label, boolean on, int mouseX, int mouseY, int i, int j, int rx, int ry) {
        boolean hover = mouseX >= i + rx && mouseX <= i + rx + BTN_W && mouseY >= j + ry && mouseY <= j + ry + BTN_H;
        g.fill(x, y, x + BTN_W, y + BTN_H, on ? 0xFF0E2A2F : 0xFF1A1423);
        g.renderOutline(x, y, BTN_W, BTN_H, hover ? 0xFFFFD54F : (on ? 0xFF00E5FF : 0xFF4B3863));
        int tw = this.font.width(label);
        g.drawString(this.font, label, x + (BTN_W - tw) / 2, y + 3, on ? 0xFF73F2FF : 0xFF9CA3AF, false);
    }
}
