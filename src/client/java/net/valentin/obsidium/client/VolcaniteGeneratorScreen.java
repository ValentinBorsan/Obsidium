package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.VolcaniteGeneratorMenu;

public class VolcaniteGeneratorScreen extends AbstractContainerScreen<VolcaniteGeneratorMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/generator_gui.png");
    private static final ResourceLocation LAVA_OVERLAY = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/lava_overlay.png");
    private static final ResourceLocation ENERGY_OVERLAY = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/energy_overlay.png");

    private float rotorAngle = 0f;
    private long lastFrameMs = System.currentTimeMillis();

    public VolcaniteGeneratorScreen(VolcaniteGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 73;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Clean Title in Gold - never overlaps anything
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFD166, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xCECAC5, false);

        // Status text positioned cleanly below Slot 1 (Output) at x=116, y=56 with zero title overlap
        boolean active = this.menu.getBurnTime() > 0 || this.menu.getLavaAmount() > 0;
        String status = active ? "5 RF/t" : "IDLE";
        int col = active ? 0xFFFFCC33 : 0xFF71717A;
        int statusX = 116 + (18 - this.font.width(status)) / 2;
        guiGraphics.drawString(this.font, status, statusX, 56, col, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        
        // Tooltip for Fuel / Lava Tank (70, 20) size 16x50
        if (mouseX >= i + 70 && mouseX <= i + 86 && mouseY >= j + 20 && mouseY <= j + 70) {
            int lava = this.menu.getLavaAmount();
            int burnTime = this.menu.getBurnTime();
            int maxBurnTime = this.menu.getMaxBurnTime();
            if (lava > 0) {
                guiGraphics.renderTooltip(this.font, Component.literal("§6Lava Reservoir: §e" + lava + " / 16,000 mB"), mouseX, mouseY);
            } else if (burnTime > 0 && maxBurnTime > 0) {
                int percent = (int) (((float) burnTime / maxBurnTime) * 100);
                guiGraphics.renderTooltip(this.font, Component.literal("§cCombustion Hearth: §e" + (burnTime / 20) + "s left (" + percent + "%)"), mouseX, mouseY);
            } else {
                guiGraphics.renderTooltip(this.font, Component.literal("§7Fuel: Empty (Coal, Charcoal, Briquettes, Lava)"), mouseX, mouseY);
            }
        }
        
        // Tooltip for Energy (90, 20) size 16x50
        if (mouseX >= i + 90 && mouseX <= i + 106 && mouseY >= j + 20 && mouseY <= j + 70) {
            guiGraphics.renderTooltip(this.font, Component.literal("§6⚡ Internal Battery: §e" + String.format("%,d", this.menu.getEnergyAmount()) + " / 100,000 RF §7(5 RF/t)"), mouseX, mouseY);
        }

        // Tooltip for Mini Dynamo (above Slot 1, 117..133, 17..31)
        if (mouseX >= i + 117 && mouseX <= i + 133 && mouseY >= j + 17 && mouseY <= j + 31) {
            boolean active = this.menu.getBurnTime() > 0 || this.menu.getLavaAmount() > 0;
            guiGraphics.renderTooltip(this.font, Component.literal(active ? "§6⚙ Magnetic Induction Dynamo: §a5 RF/t" : "§7⚙ Magnetic Induction Dynamo: §cIDLE"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - lastFrameMs) / 1000f);
        lastFrameMs = now;

        boolean isBurning = this.menu.getBurnTime() > 0;
        boolean hasLava = this.menu.getLavaAmount() > 0;
        boolean active = isBurning || hasLava;

        float targetSpeed = active ? 360f : 0f;
        rotorAngle = (rotorAngle + targetSpeed * dt) % 360f;

        VolcaniteGuiTheme.renderExteriorFrame(guiGraphics, i, j, this.imageWidth, this.imageHeight, false);
        guiGraphics.blit(GUI_TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        // 1. Animated Hearth Flames under Input Slot (i + 44, j + 55 to j + 68)
        if (isBurning) {
            for (int f = 0; f < 3; f++) {
                int fx = i + 46 + f * 4;
                int waveH = (int) (5 + Math.sin((now / 80.0) + f * 1.5) * 2.5);
                guiGraphics.fill(fx, j + 67 - waveH, fx + 3, j + 67, 0xFFDC2626);
                guiGraphics.fill(fx, j + 67 - Math.max(2, waveH - 2), fx + 3, j + 67, 0xFFFBBF24);
            }
            int sparkX = i + 46 + (int) ((now / 40) % 12);
            int sparkY = j + 60 - (int) ((now / 30) % 8);
            guiGraphics.fill(sparkX, sparkY, sparkX + 1, sparkY + 1, 0xFFFFFBEB);
        }

        // 2. Render Fuel / Lava Bar with undulating surface and thermal bubbles
        int lava = this.menu.getLavaAmount();
        int burnTime = this.menu.getBurnTime();
        int maxBurnTime = this.menu.getMaxBurnTime();
        int fuelH = 0;
        if (lava > 0) {
            fuelH = (int) (50.0f * ((float) lava / 16000));
        } else if (burnTime > 0 && maxBurnTime > 0) {
            fuelH = (int) (50.0f * ((float) burnTime / maxBurnTime));
        }
        fuelH = Math.max(0, Math.min(50, fuelH));

        if (fuelH > 0) {
            int tankX = i + 70;
            int tankBaseY = j + 70;
            guiGraphics.blit(LAVA_OVERLAY, tankX, tankBaseY - fuelH, 0, 50 - fuelH, 16, fuelH, 16, 50);

            if (active) {
                int waveY = tankBaseY - fuelH;
                int waveShift = (int) (Math.sin(now / 120.0) * 1.5);
                guiGraphics.fill(tankX + 1, waveY + waveShift, tankX + 15, waveY + waveShift + 1, 0xFFFEF08A);

                for (int b = 0; b < 2; b++) {
                    int bx = tankX + 3 + (b * 7);
                    int by = tankBaseY - 2 - (int) (((now / 35) + b * 20) % Math.max(2, fuelH));
                    guiGraphics.fill(bx, by, bx + 2, by + 2, 0xFFFDE047);
                }
            }
        }
        
        // 3. Render Energy Bar with dynamic electrical sparkline
        int maxEnergy = 100000;
        int energyAmount = this.menu.getEnergyAmount();
        if (energyAmount > 0) {
            int height = (int) (50.0f * ((float) energyAmount / maxEnergy));
            height = Math.max(1, Math.min(50, height));
            guiGraphics.blit(ENERGY_OVERLAY, i + 90, j + 70 - height, 0, 50 - height, 16, height, 16, 50);

            if (active) {
                int sparkY = j + 70 - (int) ((now / 25) % height);
                guiGraphics.fill(i + 91, sparkY, i + 105, sparkY + 1, 0xFFFEF08A);
            }
        }

        // 4. Thermoelectric Arc Bridge between Lava Tank (x=86) and Energy Bar (x=90)
        if (active) {
            int arcY = j + 35 + (int) (Math.sin(now / 60.0) * 12);
            int arcCol = ((now / 50) % 2 == 0) ? 0xFF00E5FF : 0xFFFBBF24;
            guiGraphics.fill(i + 86, arcY, i + 90, arcY + 2, arcCol);
            guiGraphics.fill(i + 87, arcY, i + 89, arcY + 1, 0xFFFFFFFF);
        }

        // 5. Compact Spinning Dynamo Rotor directly above Slot 1 (centered at i + 125, j + 24)
        int dynoCX = i + 125;
        int dynoCY = j + 24;
        guiGraphics.fill(dynoCX - 8, dynoCY - 8, dynoCX + 8, dynoCY + 8, 0xFF14121A);
        guiGraphics.renderOutline(dynoCX - 8, dynoCY - 8, 16, 16, active ? 0xFFEA580C : 0xFF3F3F46);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(dynoCX, dynoCY, 0);
        guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(rotorAngle));
        for (int p = 0; p < 4; p++) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(p * 90f));
            guiGraphics.fill(-1, -6, 1, -2, active ? 0xFFF97316 : 0xFF71717A);
            guiGraphics.fill(-2, -7, 2, -5, active ? 0xFFFBBF24 : 0xFF52525B);
            guiGraphics.pose().popPose();
        }
        guiGraphics.fill(-2, -2, 2, 2, 0xFFD97706);
        guiGraphics.fill(-1, -1, 1, 1, 0xFFFFFFFF);
        guiGraphics.pose().popPose();
    }
}
