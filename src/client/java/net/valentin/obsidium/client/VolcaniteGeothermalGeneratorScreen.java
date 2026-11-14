package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.VolcaniteGeothermalGeneratorBlockEntity;
import net.valentin.obsidium.block.VolcaniteGeothermalGeneratorMenu;

public class VolcaniteGeothermalGeneratorScreen extends AbstractContainerScreen<VolcaniteGeothermalGeneratorMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/geothermal_generator_gui.png");
    private static final ResourceLocation LAVA_OVERLAY = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/lava_overlay.png");
    private static final ResourceLocation ENERGY_OVERLAY = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/energy_overlay.png");

    private Button boostButton;

    public VolcaniteGeothermalGeneratorScreen(VolcaniteGeothermalGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 73;
    }

    @Override
    protected void init() {
        super.init();
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        boostButton = Button.builder(Component.literal("ECO"), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            }
        }).bounds(i + 78, j + 48, 44, 18).build();

        this.addRenderableWidget(boostButton);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Render title with slight scale so it never touches borders or screws
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.titleLabelX, this.titleLabelY, 0);
        guiGraphics.pose().scale(0.92f, 0.92f, 1.0f);
        guiGraphics.drawString(this.font, this.title, 0, 0, 0xFF7A00, false);
        guiGraphics.pose().popPose();

        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xCECAC5, false);

        // Geothermal Status Text
        int passive = this.menu.getPassiveRate();
        int total = this.menu.getTotalRate();
        guiGraphics.drawString(this.font, "Heat: +" + passive, 78, 22, 0xFFBA08, false);
        guiGraphics.drawString(this.font, "Gen: " + total + " E/t", 78, 34, 0x55FF55, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (boostButton != null) {
            boolean active = this.menu.isBoostEnabled();
            // Clean ASCII text without broken emojis
            boostButton.setMessage(Component.literal(active ? "§cBOOST" : "§aECO"));
        }

        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        // Tooltip for Lava Tank (58, 20) 16x50
        if (mouseX >= i + 58 && mouseX <= i + 74 && mouseY >= j + 20 && mouseY <= j + 70) {
            guiGraphics.renderTooltip(this.font, Component.literal("§6Geothermal Lava Chamber: §e" + this.menu.getLavaAmount() + " / " + VolcaniteGeothermalGeneratorBlockEntity.MAX_LAVA + " mB"), mouseX, mouseY);
        }

        // Tooltip for Status Area (78, 20) 44x26
        if (mouseX >= i + 78 && mouseX <= i + 122 && mouseY >= j + 20 && mouseY <= j + 46) {
            String status = this.menu.isBoostEnabled() ? "§cActive (+25 RF/t with Lava)" : "§aDisabled (Passive Eco Mode)";
            guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("§6Thermopile Heat Grid"),
                    Component.literal("§ePassive Heat: §a+" + this.menu.getPassiveRate() + " RF/t"),
                    Component.literal("§6Lava Overclock: " + status),
                    Component.literal("§eTotal Output: §a" + this.menu.getTotalRate() + " RF/t")
            ), mouseX, mouseY);
        }

        // Tooltip for Energy (126, 20) 16x50
        if (mouseX >= i + 126 && mouseX <= i + 142 && mouseY >= j + 20 && mouseY <= j + 70) {
            guiGraphics.renderTooltip(this.font, Component.literal("§6⚡ Geothermal Buffer: §e" + String.format("%,d", this.menu.getEnergyAmount()) + " / " + String.format("%,d", VolcaniteGeothermalGeneratorBlockEntity.MAX_ENERGY) + " RF"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        long now = System.currentTimeMillis();

        VolcaniteGuiTheme.renderExteriorFrame(guiGraphics, i, j, this.imageWidth, this.imageHeight, true);
        guiGraphics.blit(GUI_TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        boolean boostActive = this.menu.isBoostEnabled() && this.menu.getLavaAmount() >= 10;
        int totalRate = this.menu.getTotalRate();
        boolean generating = totalRate > 0;

        // 1. Subtle Thermopile Heat Waves inside the status frame (i + 79..121, j + 21..45)
        if (generating) {
            for (int line = 0; line < 6; line++) {
                int lx = i + 82 + line * 7;
                float phase = (float) (now / 150.0 + line * 0.8);
                int waveShift = (int) (Math.sin(phase) * 2.5);
                int col = boostActive ? 0xAAFF7700 : 0x77B45309;
                guiGraphics.fill(lx, j + 22 + waveShift, lx + 1, j + 44 + waveShift, col);
            }
        }

        // 2. Render Lava Bar with convective magma circulation (58, 20) 16x50
        int lava = this.menu.getLavaAmount();
        if (lava > 0) {
            int height = (int) (50.0f * ((float) lava / VolcaniteGeothermalGeneratorBlockEntity.MAX_LAVA));
            height = Math.max(1, Math.min(50, height));
            int tankX = i + 58;
            int tankBaseY = j + 70;
            guiGraphics.blit(LAVA_OVERLAY, tankX, tankBaseY - height, 0, 50 - height, 16, height, 16, 50);

            int waveY = tankBaseY - height;
            int waveShift = (int) (Math.sin(now / 100.0) * 1.5);
            guiGraphics.fill(tankX + 1, waveY + waveShift, tankX + 15, waveY + waveShift + 1, 0xFFFDE047);

            for (int b = 0; b < 2; b++) {
                int bx = tankX + 3 + (b * 6);
                int by = tankBaseY - 2 - (int) (((now / 40) + b * 22) % Math.max(2, height));
                guiGraphics.fill(bx, by, bx + 2, by + 2, 0xFFFFFBEB);
            }
        }

        // 3. Render Energy Bar (126, 20) 16x50
        int energy = this.menu.getEnergyAmount();
        if (energy > 0) {
            int height = (int) (50.0f * ((float) energy / VolcaniteGeothermalGeneratorBlockEntity.MAX_ENERGY));
            height = Math.max(1, Math.min(50, height));
            guiGraphics.blit(ENERGY_OVERLAY, i + 126, j + 70 - height, 0, 50 - height, 16, height, 16, 50);

            if (generating) {
                int pulseY = j + 70 - (int) ((now / 20) % height);
                guiGraphics.fill(i + 127, pulseY, i + 141, pulseY + 1, 0xFFFEF08A);
            }
        }

        // 4. Plasma Stream when BOOST is active
        if (boostActive) {
            int glowPulse = (int) ((Math.sin(now / 80.0) + 1.0) * 127);
            int glowColor = 0xFF000000 | (0xEF << 16) | (glowPulse << 8) | 0x00;
            guiGraphics.renderOutline(i + 77, j + 47, 46, 20, glowColor);

            int streamY = j + 30 + (int) (Math.sin(now / 50.0) * 4);
            guiGraphics.fill(i + 74, streamY, i + 78, streamY + 2, 0xFFF97316);
            guiGraphics.fill(i + 122, streamY, i + 126, streamY + 2, 0xFFFBBF24);
        }
    }
}
