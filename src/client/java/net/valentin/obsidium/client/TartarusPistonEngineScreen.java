package net.valentin.obsidium.client;

import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.block.TartarusPistonEngineBlockEntity;
import net.valentin.obsidium.block.TartarusPistonEngineMenu;

import java.util.List;

public class TartarusPistonEngineScreen extends AbstractContainerScreen<TartarusPistonEngineMenu> {

    private static final int MODE_BTN_X = 114;
    private static final int MODE_BTN_Y = 55;
    private static final int MODE_BTN_W = 31;
    private static final int MODE_BTN_H = 12;

    private float flywheelAngle = 0f;
    private float flywheelSpeed = 0f;
    private long lastFrameMs = System.currentTimeMillis();

    public TartarusPistonEngineScreen(TartarusPistonEngineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 73;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, "❄ Cryo-Thermic Piston Engine", this.titleLabelX, this.titleLabelY, 0xFF90E0EF, false);
        g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFCECAC5, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        if (button == 0 && VolcaniteGuiTheme.handleMuteClick(x, y, this.imageWidth, mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && mouseX >= x + MODE_BTN_X && mouseX <= x + MODE_BTN_X + MODE_BTN_W
                && mouseY >= y + MODE_BTN_Y && mouseY <= y + MODE_BTN_Y + MODE_BTN_H) {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.15F));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - lastFrameMs) / 1000f);
        lastFrameMs = now;

        boolean burning = this.menu.isBurning();
        int rpm = this.menu.getRpm();
        int temp = this.menu.getCylinderTemp();
        boolean cryoActive = this.menu.getCryoAmount() > 0 && temp <= 420;
        int mode = this.menu.getDriveMode();

        float targetSpeed = burning ? (180f + (rpm / 9000f) * 720f) : 0f;
        flywheelSpeed += (targetSpeed - flywheelSpeed) * Math.min(1f, dt * 3.0f);
        flywheelAngle = (flywheelAngle + flywheelSpeed * dt) % 360f;

        // 1. Obsidian Theme Window & Panels
        VolcaniteGuiTheme.renderWindow(g, x, y, this.imageWidth, this.imageHeight, true);
        VolcaniteGuiTheme.renderRecessedPanel(g, x + 6, y + 16, 164, 55);
        VolcaniteGuiTheme.renderPlayerInventory(g, x + 8, y + 84);

        // 2. Energy Bar (x = 9, y = 18, w = 11, h = 50)
        int energyH = this.menu.getEnergyScaled(48);
        drawGauge(g, x + 9, y + 18, 11, 50, energyH, 0xFFF97316, 0xFFDC2626, 0xFF991B1B);

        // 3. Fuel Fluid Tank (x = 23, y = 18, w = 11, h = 50)
        int fluidH = this.menu.getFluidScaled(48);
        drawGauge(g, x + 23, y + 18, 11, 50, fluidH, 0xFFFBBF24, 0xFFEA580C, 0xFFC2410C);

        // 4. Cryo-Ice Coolant Tank (x = 37, y = 18, w = 11, h = 50)
        int cryoH = this.menu.getCryoScaled(48);
        drawGauge(g, x + 37, y + 18, 11, 50, cryoH, 0xFF90E0EF, 0xFF0077B6, 0xFF00B4D8);
        if (cryoActive && cryoH > 2) {
            int frostY = y + 66 - (int) ((now / 45) % Math.max(1, cryoH));
            g.fill(x + 39, frostY, x + 46, frostY + 1, 0xFFFFFFFF);
        }

        // 5. Machine Slots
        VolcaniteGuiTheme.renderSlot(g, x + 54, y + 21, 0xFFFF9900); // Fuel In
        VolcaniteGuiTheme.renderSlot(g, x + 54, y + 49, 0xFF71717A); // Bucket Out
        VolcaniteGuiTheme.renderSlot(g, x + 150, y + 21, 0xFF00E5FF); // Cryo Ice In
        VolcaniteGuiTheme.renderSlot(g, x + 150, y + 49, 0xFFFFD54F); // Battery Charge

        // 6. Central 4-Piston Engine Viewport (x = 74, y = 18, w = 72, h = 35)
        int mechX = x + 74;
        int mechY = y + 18;
        g.fill(mechX, mechY, mechX + 72, mechY + 35, 0xFF09070F);
        g.renderOutline(mechX, mechY, 72, 35, cryoActive ? 0xFF00B4D8 : 0xFF7C2D12);

        // 4-cylinder firing pistons
        float rad = (float) Math.toRadians(flywheelAngle);
        float[] phases = {0f, (float) Math.PI, (float) (Math.PI * 0.5), (float) (Math.PI * 1.5)};
        for (int c = 0; c < 4; c++) {
            int cx = mechX + 4 + c * 10;
            float stroke = (float) Math.sin(rad + phases[c]) * 4.5f;
            int py = (int) (mechY + 9 + stroke);
            g.fill(cx, mechY + 3, cx + 8, mechY + 26, 0xFF14121D);
            g.renderOutline(cx, mechY + 3, 8, 23, 0xFF27272A);
            if (burning && stroke > 1.2f) {
                int flameTop = cryoActive ? 0xFF90E0EF : 0xFFFEF08A;
                int flameBot = cryoActive ? 0xFF0077B6 : 0xFFEA580C;
                g.fillGradient(cx + 1, mechY + 18, cx + 7, mechY + 25, flameTop, flameBot);
            }
            g.fill(cx + 1, py, cx + 7, py + 5, 0xFF9CA3AF);
            g.fill(cx + 3, py + 5, cx + 5, mechY + 31, 0xFF6B7280);
        }

        // Rotating Flywheel on the right of viewport
        int fwCX = mechX + 57;
        int fwCY = mechY + 17;
        g.fill(fwCX - 11, fwCY - 11, fwCX + 11, fwCY + 11, 0xFF15121C);
        g.renderOutline(fwCX - 11, fwCY - 11, 22, 22, cryoActive ? 0xFF00E5FF : (burning ? 0xFFEA580C : 0xFF3F3F46));

        g.pose().pushPose();
        g.pose().translate(fwCX, fwCY, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(flywheelAngle));
        int spokeCol = cryoActive ? 0xFF73F2FF : (burning ? 0xFFF97316 : 0xFF71717A);
        g.fill(-9, -1, 9, 1, spokeCol);
        g.fill(-1, -9, 1, 9, spokeCol);
        g.pose().popPose();
        g.fill(fwCX - 2, fwCY - 2, fwCX + 2, fwCY + 2, 0xFFFFD54F);

        // 7. Telemetry Strip (RPM & Temp) + Drive Mode Button (y = 55..67)
        g.fill(mechX, y + 55, mechX + 38, y + 67, 0xFF09070F);
        g.renderOutline(mechX, y + 55, 38, 12, 0xFF2D2240);
        int tempCol = temp >= 950 ? 0xFFFF4D4D : (cryoActive ? 0xFF73F2FF : 0xFFFFB74D);
        g.drawString(this.font, temp + "°C", mechX + 3, y + 57, tempCol, false);

        boolean btnHover = mouseX >= x + MODE_BTN_X && mouseX <= x + MODE_BTN_X + MODE_BTN_W
                && mouseY >= y + MODE_BTN_Y && mouseY <= y + MODE_BTN_Y + MODE_BTN_H;
        int btnBg = switch (mode) {
            case TartarusPistonEngineBlockEntity.MODE_ECO -> 0xFF102A1E;
            case TartarusPistonEngineBlockEntity.MODE_OVERDRIVE -> 0xFF3A1218;
            default -> 0xFF1B162C;
        };
        int btnBorder = btnHover ? 0xFFFFD54F : (mode == 2 ? 0xFFFF6B35 : (mode == 0 ? 0xFF2A9D8F : 0xFF00B4D8));
        g.fill(x + MODE_BTN_X, y + MODE_BTN_Y, x + MODE_BTN_X + MODE_BTN_W, y + MODE_BTN_Y + MODE_BTN_H, btnBg);
        g.renderOutline(x + MODE_BTN_X, y + MODE_BTN_Y, MODE_BTN_W, MODE_BTN_H, btnBorder);
        String modeLabel = switch (mode) {
            case TartarusPistonEngineBlockEntity.MODE_ECO -> "ECO";
            case TartarusPistonEngineBlockEntity.MODE_OVERDRIVE -> "TURBO";
            default -> "STD";
        };
        int mw = this.font.width(modeLabel);
        g.drawString(this.font, modeLabel, x + MODE_BTN_X + (MODE_BTN_W - mw) / 2, y + MODE_BTN_Y + 2, 0xFFF3F4F6, false);
    }

    private void drawGauge(GuiGraphics g, int x, int y, int w, int h, int filledH, int colTop, int colBot, int borderCol) {
        g.fill(x, y, x + w, y + h, 0xFF09060E);
        if (filledH > 0) {
            g.fillGradient(x + 1, y + h - 1 - filledH, x + w - 1, y + h - 1, colTop, colBot);
        }
        g.renderOutline(x, y, w, h, borderCol);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        VolcaniteGuiTheme.renderMuteTooltip(this.font, g, x, y, this.imageWidth, mouseX, mouseY);

        if (mouseX >= x + 9 && mouseX <= x + 20 && mouseY >= y + 18 && mouseY <= y + 68) {
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6⚡ Energy Buffer"),
                    Component.literal("§e" + String.format("%,d / %,d RF", this.menu.getEnergy(), this.menu.getMaxEnergy())),
                    Component.literal("§7Base Fuel Rate: §a+" + String.format("%,d RF/t", this.menu.getCurrentRfPerTick())),
                    Component.literal("§bRPM: §f" + this.menu.getRpm() + " RPM")
            ), mouseX, mouseY);
        }
        if (mouseX >= x + 23 && mouseX <= x + 34 && mouseY >= y + 18 && mouseY <= y + 68) {
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6🔥 Combustion Fuel Tank"),
                    Component.literal("§e" + this.menu.getFluidAmount() + " / " + this.menu.getMaxFluid() + " mB"),
                    Component.literal("§7Accepts: Lava, Bio-Oil, Biodiesel, Distillate, Pyro-Diesel")
            ), mouseX, mouseY);
        }
        if (mouseX >= x + 37 && mouseX <= x + 48 && mouseY >= y + 18 && mouseY <= y + 68) {
            boolean cryoActive = this.menu.getCryoAmount() > 0 && this.menu.getCylinderTemp() <= 420;
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("§b❄ Cryo-Ice Coolant Tank"),
                    Component.literal("§f" + this.menu.getCryoAmount() + " / " + TartarusPistonEngineBlockEntity.MAX_CRYO + " Cryo"),
                    Component.literal(cryoActive ? "§a★ Cryo-Supercharged: +35% RF/t & +25% Fuel Efficiency!" : "§7Insert Ice, Packed Ice, Blue Ice, or Dew Droplets")
            ), mouseX, mouseY);
        }
        if (mouseX >= x + MODE_BTN_X && mouseX <= x + MODE_BTN_X + MODE_BTN_W
                && mouseY >= y + MODE_BTN_Y && mouseY <= y + MODE_BTN_Y + MODE_BTN_H) {
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("§e⚙ Drive Mode Selector"),
                    Component.literal("§a• ECO§7: 75% RF/t, +45% Fuel Duration"),
                    Component.literal("§b• STD§7: 100% RF/t, Balanced RPM"),
                    Component.literal("§c• TURBO§7: 160% RF/t, 9,000 RPM (Requires Ice Cooling!)")
            ), mouseX, mouseY);
        }
    }
}
