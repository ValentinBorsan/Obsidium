package net.valentin.obsidium.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.block.OlympianHadronColliderBlockEntity;
import net.valentin.obsidium.block.OlympianHadronColliderMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OlympianHadronColliderScreen extends AbstractContainerScreen<OlympianHadronColliderMenu> {
    private float clientBeamAngle = 0.0f;
    private long lastFrameMs = System.currentTimeMillis();

    public OlympianHadronColliderScreen(OlympianHadronColliderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 236;
        this.imageHeight = 216;
        this.titleLabelX = 1000;
        this.inventoryLabelX = 38;
        this.inventoryLabelY = 123;
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null && this.minecraft.player != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
            this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.55F, 1.25F);
        }
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawSlotBox(GuiGraphics g, int sx, int sy, int borderColor) {
        g.fill(sx, sy, sx + 18, sy + 18, 0xFF0A0812);
        drawBorder(g, sx, sy, 18, 18, borderColor);
    }

    private void drawScaledString(GuiGraphics g, String text, int x, int y, float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0f);
        g.drawString(this.font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private static String formatCompactRf(long rf) {
        if (rf >= 1_000_000_000L) return String.format("%.2fG", rf / 1_000_000_000.0);
        if (rf >= 1_000_000L) return String.format("%.1fM", rf / 1_000_000.0);
        if (rf >= 1_000L) return String.format("%.1fk", rf / 1_000.0);
        return String.valueOf(rf);
    }

    private int getFuelColor(int fuelType) {
        return switch (fuelType) {
            case OlympianHadronColliderBlockEntity.FUEL_PYRO_DIESEL -> 0xFFFF2A44;
            case OlympianHadronColliderBlockEntity.FUEL_DISTILLATE -> 0xFFE84DFF;
            case OlympianHadronColliderBlockEntity.FUEL_BIODIESEL -> 0xFF2CE67A;
            case OlympianHadronColliderBlockEntity.FUEL_BIO_OIL -> 0xFFFFB020;
            case OlympianHadronColliderBlockEntity.FUEL_LAVA -> 0xFFFF6600;
            default -> 0xFF332844;
        };
    }

    private String getFuelName(int fuelType) {
        return switch (fuelType) {
            case OlympianHadronColliderBlockEntity.FUEL_PYRO_DIESEL -> "§cPyro-Diesel (2.5×)";
            case OlympianHadronColliderBlockEntity.FUEL_DISTILLATE -> "§dPyroclastic Distillate (1.8×)";
            case OlympianHadronColliderBlockEntity.FUEL_BIODIESEL -> "§aBiodiesel (1.4×)";
            case OlympianHadronColliderBlockEntity.FUEL_BIO_OIL -> "§6Bio-Oil (1.0×)";
            case OlympianHadronColliderBlockEntity.FUEL_LAVA -> "§6Lava (1.0×)";
            default -> "§7Empty";
        };
    }

    private String getCoolantName(int coolType) {
        return switch (coolType) {
            case OlympianHadronColliderBlockEntity.COOLANT_BIO_ETHANOL -> "§bBio-Ethanol (Cryo 2.0×)";
            case OlympianHadronColliderBlockEntity.COOLANT_WATER -> "§9Water / Ice (1.0×)";
            default -> "§7Empty";
        };
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, "§b⚛ HADRON COLLIDER", 8, 7, 0xFF73F2FF, false);

        // Header right buttons / status
        if (!this.menu.isFormed()) {
            boolean holo = this.menu.isShowHologram();
            drawScaledString(g, holo ? "Holo:ON" : "Holo:OFF", 136, 9, 0.72f, holo ? 0xFF55FFFF : 0xFF9E9E9E);
            drawScaledString(g, "Auto-Build", 174, 9, 0.72f, 0xFFFFD54F);
        } else {
            drawScaledString(g, "5×5 LOCKED (" + this.menu.getCoilCount() + " Coils)", 136, 9, 0.72f, 0xFF55FF99);
        }
        g.drawString(this.font, "?", 221, 7, 0xFFFFD54F, false);

        // Slot mini labels
        drawScaledString(g, "Beam A↻", 46, 33, 0.70f, 0xFF66EEFF);
        drawScaledString(g, "Beam B↺", 170, 33, 0.70f, 0xFFFF9955);
        drawScaledString(g, "Fuel", 14, 16, 0.70f, 0xFFFF8844);
        drawScaledString(g, "Cryo", 209, 16, 0.70f, 0xFF66DDFF);
        drawScaledString(g, "Out", 112, 83, 0.70f, 0xFFCEB8FF);

        // Center Speed Readout under radar
        int vel = this.menu.getBeamVelocity();
        double cPct = Math.min(99.9, (vel / 1000.0) * 99.9);
        String cStr = String.format("%.1f%% c", cPct);
        int cw = (int) (this.font.width(cStr) * 0.85f);
        drawScaledString(g, cStr, 118 - cw / 2, 73, 0.85f, vel >= 999 ? 0xFF55FFAA : 0xFF73F2FF);

        // Buttons text
        String modeTxt = this.menu.getOperatingMode() == 0 ? "AutoSmash" : "HoldRing";
        drawScaledString(g, modeTxt, 51, 87, 0.72f, 0xFFE6DDFF);

        boolean canSmash = this.menu.isFormed() && vel >= 300
                && !this.menu.getSlot(0).getItem().isEmpty()
                && !this.menu.getSlot(1).getItem().isEmpty();
        drawScaledString(g, "SMASH!", 150, 87, 0.75f, canSmash ? 0xFFFFEE55 : 0xFF887799);

        // Temperature & RF Telemetry
        int temp = this.menu.getCoilTemp();
        int tempColor = temp < 120 ? 0xFF55FFFF : (temp >= 550 ? 0xFFFF4455 : 0xFFFFAA44);
        drawScaledString(g, temp + " K", 52, 102, 0.75f, tempColor);
        if (temp < 120) {
            drawScaledString(g, "CRYO LOCK", 47, 111, 0.65f, 0xFF55FFAA);
        }

        String rft = "+" + formatCompactRf(this.menu.getCurrentRfPerTick()) + "/t";
        drawScaledString(g, rft, 145, 102, 0.75f, 0xFF55FF99);
        drawScaledString(g, "Hits:" + this.menu.getTotalCollisions(), 145, 111, 0.65f, 0xFFD0C4E0);

        g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFB8B0C8, false);
        String storedLabel = formatCompactRf(this.menu.getEnergyAmount()) + " / 100M RF";
        drawScaledString(g, storedLabel, 134, 124, 0.72f, 0xFF73F2FF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int i = (this.width - this.imageWidth) / 2;
            int j = (this.height - this.imageHeight) / 2;

            // Manual [?] button (216..228, 6..18)
            if (mouseX >= i + 216 && mouseX <= i + 228 && mouseY >= j + 6 && mouseY <= j + 18) {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.55F, 1.25F);
                    this.minecraft.setScreen(new net.valentin.obsidium.screen.MagmaForgeManualScreen(7, 0));
                }
                return true;
            }

            // Mode toggle button (48..93, 84..97)
            if (mouseX >= i + 48 && mouseX <= i + 93 && mouseY >= j + 84 && mouseY <= j + 97) {
                sendButton(0);
                return true;
            }
            // SMASH! button (143..188, 84..97)
            if (mouseX >= i + 143 && mouseX <= i + 188 && mouseY >= j + 84 && mouseY <= j + 97) {
                sendButton(1);
                return true;
            }
            if (!this.menu.isFormed()) {
                // Holo toggle (133..169, 6..18)
                if (mouseX >= i + 133 && mouseX <= i + 169 && mouseY >= j + 6 && mouseY <= j + 18) {
                    sendButton(2);
                    return true;
                }
                // Auto-Build 5x5 (171..214, 6..18)
                if (mouseX >= i + 171 && mouseX <= i + 214 && mouseY >= j + 6 && mouseY <= j + 18) {
                    sendButton(3);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        long now = System.currentTimeMillis();
        float dtSec = Math.min(0.1f, Math.max(0.0f, (now - lastFrameMs) / 1000.0f));
        lastFrameMs = now;

        int vel = this.menu.getBeamVelocity();
        if (vel > 0) {
            clientBeamAngle = (clientBeamAngle + dtSec * (0.8f + (vel / 1000.0f) * 14.0f)) % ((float) (Math.PI * 2.0));
        }

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        VolcaniteGuiTheme.renderExteriorFrame(g, i, j, this.imageWidth, this.imageHeight, true);

        // Obsidian main panel
        g.fill(i, j, i + this.imageWidth, j + this.imageHeight, 0xFF100C18);
        g.fill(i + 4, j + 4, i + this.imageWidth - 4, j + 20, 0xFF181226);
        g.fill(i + 4, j + 20, i + this.imageWidth - 4, j + 21, 0xFF3E2D5E);

        // Recipe [?] badge (216..228, 6..18)
        g.fill(i + 216, j + 6, i + 228, j + 18, 0xFF281F3D);
        drawBorder(g, i + 216, j + 6, 12, 12, 0xFFDCA220);

        // Unformed Hologram & Auto-Build buttons
        if (!this.menu.isFormed()) {
            boolean holo = this.menu.isShowHologram();
            g.fill(i + 133, j + 6, i + 169, j + 18, holo ? 0xFF113344 : 0xFF221B30);
            drawBorder(g, i + 133, j + 6, 36, 12, holo ? 0xFF55FFFF : 0xFF554A6A);

            g.fill(i + 171, j + 6, i + 214, j + 18, 0xFF332612);
            drawBorder(g, i + 171, j + 6, 43, 12, 0xFFFFB74D);
        } else {
            g.fill(i + 133, j + 6, i + 214, j + 18, 0xFF0E2B1E);
            drawBorder(g, i + 133, j + 6, 81, 12, 0xFF35C47C);
        }

        // Slots
        drawSlotBox(g, i + 47, j + 41, 0xFF00D4FF); // Beam A
        drawSlotBox(g, i + 171, j + 41, 0xFFFF8833); // Beam B
        drawSlotBox(g, i + 97, j + 91, 0xFFDCA220); // Out 1
        drawSlotBox(g, i + 121, j + 91, 0xFFDCA220); // Out 2
        drawSlotBox(g, i + 11, j + 23, 0xFFFF6622); // Fuel In
        drawSlotBox(g, i + 207, j + 23, 0xFF33CCFF); // Cryo In
        drawSlotBox(g, i + 11, j + 91, 0xFF7A6A99); // Container Return / Charge

        // Propulsion Fuel Tank (32, 24, 12x84)
        g.fill(i + 32, j + 24, i + 44, j + 108, 0xFF09070E);
        drawBorder(g, i + 31, j + 23, 14, 86, 0xFF5B3B28);
        int fuelAmt = this.menu.getFuelAmount();
        if (fuelAmt > 0) {
            int fillH = Math.max(2, (int) ((fuelAmt / (double) OlympianHadronColliderBlockEntity.MAX_FUEL) * 84));
            g.fill(i + 32, j + 108 - fillH, i + 44, j + 108, getFuelColor(this.menu.getFuelType()));
        }

        // Cryo-Coolant Tank (192, 24, 12x84)
        g.fill(i + 192, j + 24, i + 204, j + 108, 0xFF09070E);
        drawBorder(g, i + 191, j + 23, 14, 86, 0xFF28506B);
        int coolAmt = this.menu.getCoolantAmount();
        if (coolAmt > 0) {
            int fillH = Math.max(2, (int) ((coolAmt / (double) OlympianHadronColliderBlockEntity.MAX_COOLANT) * 84));
            int coolColor = this.menu.getCoolantType() == OlympianHadronColliderBlockEntity.COOLANT_BIO_ETHANOL ? 0xFF00E5CC : 0xFF3399FF;
            g.fill(i + 192, j + 108 - fillH, i + 204, j + 108, coolColor);
        }

        // Stored Energy Vertical Bar (209, 48, 14x60)
        g.fill(i + 209, j + 48, i + 223, j + 108, 0xFF09070E);
        drawBorder(g, i + 208, j + 47, 16, 62, 0xFF2A5866);
        long energyAmt = this.menu.getEnergyAmount();
        if (energyAmt > 0) {
            int fillH = Math.max(2, (int) ((energyAmt / (double) OlympianHadronColliderBlockEntity.MAX_ENERGY) * 60));
            for (int row = 0; row < fillH; row += 2) {
                int yPos = j + 107 - row;
                int col = (row % 4 == 0) ? 0xFF00E5FF : 0xFF0099CC;
                g.fill(i + 210, yPos, i + 222, yPos + 1, col);
            }
        }

        // Injector beam lines from Beam A & B slots to Synchrotron Radar
        int lineColorA = vel > 0 ? 0xFF00EEFF : 0xFF243A4A;
        int lineColorB = vel > 0 ? 0xFFFF8833 : 0xFF4A2E24;
        g.fill(i + 66, j + 49, i + 94, j + 51, lineColorA);
        g.fill(i + 142, j + 49, i + 170, j + 51, lineColorB);

        // Live Synchrotron Radar (center = i+118, j+48, radius = 21)
        int rcx = i + 118;
        int rcy = j + 48;
        g.fill(rcx - 26, rcy - 25, rcx + 26, rcy + 25, 0xFF0A0712);
        drawBorder(g, rcx - 26, rcy - 25, 52, 50, 0xFF2B2242);

        // 16 coil ring nodes in circular orbit
        for (int step = 0; step < 32; step++) {
            double ang = step * (Math.PI * 2.0 / 32.0);
            int px = rcx + (int) Math.round(Math.cos(ang) * 20.0);
            int py = rcy + (int) Math.round(Math.sin(ang) * 20.0);
            boolean coilNode = (step % 2 == 0);
            int nodeCol = coilNode ? (this.menu.isFormed() ? 0xFF4488BB : 0xFF332844) : 0xFF1D182C;
            g.fill(px - 1, py - 1, px + 1, py + 1, nodeCol);
        }

        // Orbiting Beam A (Cyan CW) and Beam B (Orange/Magenta CCW)
        if (this.menu.isFormed() && vel > 0) {
            for (int t = 4; t >= 0; t--) {
                float tailOffset = t * 0.22f;
                int size = t == 0 ? 2 : 1;
                int alphaA = t == 0 ? 0xFFFFFFFF : 0xFF00D4FF;
                int ax = rcx + (int) Math.round(Math.cos(clientBeamAngle - tailOffset) * 18.0);
                int ay = rcy + (int) Math.round(Math.sin(clientBeamAngle - tailOffset) * 18.0);
                g.fill(ax - size, ay - size, ax + size, ay + size, alphaA);

                int alphaB = t == 0 ? 0xFFFFEE88 : 0xFFFF55AA;
                int bx = rcx + (int) Math.round(Math.cos(-clientBeamAngle + tailOffset) * 18.0);
                int by = rcy + (int) Math.round(Math.sin(-clientBeamAngle + tailOffset) * 18.0);
                g.fill(bx - size, by - size, bx + size, by + size, alphaB);
            }
        }

        // Collision Flash Shockwave in radar
        int flash = this.menu.getCollisionFlashTicks();
        if (flash > 0) {
            int rad = (30 - flash) / 2 + 3;
            int flashCol = (flash > 18) ? 0xFFFFFFFF : 0xFF66FFFF;
            g.fill(rcx - rad / 2, rcy - rad / 2, rcx + rad / 2, rcy + rad / 2, flashCol);
        }

        // Mode & SMASH Buttons
        g.fill(i + 48, j + 84, i + 93, j + 97, 0xFF211834);
        drawBorder(g, i + 48, j + 84, 45, 13, 0xFF7D58B8);

        boolean canSmash = this.menu.isFormed() && vel >= 300
                && !this.menu.getSlot(0).getItem().isEmpty()
                && !this.menu.getSlot(1).getItem().isEmpty();
        g.fill(i + 143, j + 84, i + 188, j + 97, canSmash ? 0xFF4A2414 : 0xFF1E1828);
        drawBorder(g, i + 143, j + 84, 45, 13, canSmash ? 0xFFFFAA00 : 0xFF443A54);

        // Relativistic Speed Bar (48, 118 -> 188, 121)
        g.fill(i + 48, j + 118, i + 188, j + 121, 0xFF0A0812);
        int speedW = (int) ((vel / 1000.0) * 140);
        if (speedW > 0) {
            int barCol = vel >= 999 ? 0xFF44FFAA : (vel >= 300 ? 0xFF00D4FF : 0xFFFF9933);
            g.fill(i + 48, j + 118, i + 48 + speedW, j + 121, barCol);
        }

        // Player Inventory Backing
        g.fill(i + 37, j + 133, i + 200, j + 188, 0xFF161121);
        g.fill(i + 37, j + 191, i + 200, j + 210, 0xFF161121);
        drawBorder(g, i + 36, j + 132, 165, 57, 0xFF2E2445);
        drawBorder(g, i + 36, j + 190, 165, 21, 0xFF2E2445);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        // Fuel Tank Tooltip (31..45, 23..109)
        if (mouseX >= i + 31 && mouseX <= i + 45 && mouseY >= j + 23 && mouseY <= j + 109) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal("§6⚛ Propulsion Fuel Tank"));
            tip.add(Component.literal("§fContent: " + getFuelName(this.menu.getFuelType())));
            tip.add(Component.literal("§e" + this.menu.getFuelAmount() + " / " + OlympianHadronColliderBlockEntity.MAX_FUEL + " mB"));
            tip.add(Component.literal("§7Accepts Pyro-Diesel, Distillate, Biodiesel, Bio-Oil, Lava."));
            g.renderTooltip(this.font, tip, Optional.empty(), mouseX, mouseY);
        }

        // Cryo Coolant Tooltip (191..205, 23..109)
        if (mouseX >= i + 191 && mouseX <= i + 205 && mouseY >= j + 23 && mouseY <= j + 109) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal("§b❄ Superconducting Cryo Tank"));
            tip.add(Component.literal("§fCoolant: " + getCoolantName(this.menu.getCoolantType())));
            tip.add(Component.literal("§b" + this.menu.getCoolantAmount() + " / " + OlympianHadronColliderBlockEntity.MAX_COOLANT + " mB"));
            tip.add(Component.literal("§fCoil Temp: §b" + this.menu.getCoilTemp() + " K §7(< 120 K = 1.5× RF & 2.5× Accel)"));
            tip.add(Component.literal("§7Accepts Water, Bio-Ethanol, Ice, Packed Ice, Blue Ice, Dew."));
            g.renderTooltip(this.font, tip, Optional.empty(), mouseX, mouseY);
        }

        // Stored Energy Bar Tooltip (208..224, 47..109)
        if (mouseX >= i + 208 && mouseX <= i + 224 && mouseY >= j + 47 && mouseY <= j + 109) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal("§b⚡ Synchrotron Energy Buffer"));
            tip.add(Component.literal(String.format("§f%,d / %,d RF", this.menu.getEnergyAmount(), OlympianHadronColliderBlockEntity.MAX_ENERGY)));
            tip.add(Component.literal(String.format("§aOutput: +%,d RF/t", this.menu.getCurrentRfPerTick())));
            tip.add(Component.literal("§7Auto-pushes RF out of all 16 ring blocks."));
            g.renderTooltip(this.font, tip, Optional.empty(), mouseX, mouseY);
        }

        // Fusion Recipes Tooltip on [?] (216..228, 6..18)
        if (mouseX >= i + 216 && mouseX <= i + 228 && mouseY >= j + 6 && mouseY <= j + 18) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal("§e⚛ CERN Minecraft-Physics Collisions"));
            tip.add(Component.literal("§d• 4× Supreme + 4× Astral ➔ §5Singularity Alloy (10% Chance!)"));
            tip.add(Component.literal("§7• §8Coal Block§7 + §fIron Block §7➔ §b2× Diamond + Volcanic Ash"));
            tip.add(Component.literal("§7• §bDiamond§7 + §6Gold Block §7➔ §51× Netherite Ingot"));
            tip.add(Component.literal("§7• §6Copper Block§7 + §6Gold §7➔ §c2× Volcanic Alloy + 2× Volcanite"));
            tip.add(Component.literal("§7• §cVolcanic Alloy§7 + §4Cinder Core §7➔ §dSupreme Volcanic Alloy"));
            tip.add(Component.literal("§7• §3Ender Pearl§7 + §4Cinder Core §7➔ §dAstral Catalyst"));
            tip.add(Component.literal("§b• Cryo <120K & >=90% c ➔ §d+10% Bonus Singularity Alloy!"));
            tip.add(Component.literal("§7• §eIdentical Items §7➔ §c2× Antimatter RF Burst!"));
            tip.add(Component.literal("§6• Ring Requires: §f12 Coils + 1 Energy + 1 Item + 1 Fluid Port"));
            g.renderTooltip(this.font, tip, Optional.empty(), mouseX, mouseY);
        }

        // SMASH button tooltip
        if (mouseX >= i + 143 && mouseX <= i + 188 && mouseY >= j + 84 && mouseY <= j + 97) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal("§e💥 Force Hadron Collision"));
            tip.add(Component.literal("§7Requires Formed 5×5 Ring, >= 30.0% c velocity, and both Beams."));
            tip.add(Component.literal("§bGenerates 3M–30M RF Burst + Fused Items!"));
            g.renderTooltip(this.font, tip, Optional.empty(), mouseX, mouseY);
        }

        // Mode button tooltip
        if (mouseX >= i + 48 && mouseX <= i + 93 && mouseY >= j + 84 && mouseY <= j + 97) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal("§d⚙ Collider Operating Mode"));
            tip.add(Component.literal("§fAutoSmash: §7Automatically collides at 99.9% c."));
            tip.add(Component.literal("§fHoldRing: §7Locks at 99.9% c for peak continuous RF/t until manual SMASH."));
            g.renderTooltip(this.font, tip, Optional.empty(), mouseX, mouseY);
        }
    }
}

