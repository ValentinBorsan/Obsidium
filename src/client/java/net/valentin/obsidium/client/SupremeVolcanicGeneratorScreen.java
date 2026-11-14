package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.SupremeVolcanicGeneratorBlockEntity;
import net.valentin.obsidium.block.SupremeVolcanicGeneratorMenu;

import java.util.ArrayList;
import java.util.List;

public class SupremeVolcanicGeneratorScreen extends AbstractContainerScreen<SupremeVolcanicGeneratorMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/geothermal_generator_gui.png");
    private static final ResourceLocation ENERGY_OVERLAY = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/energy_overlay.png");

    private Button eruptButton;

    public SupremeVolcanicGeneratorScreen(SupremeVolcanicGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        eruptButton = Button.builder(Component.literal("ERUPT!"), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            }
        }).bounds(i + 62, j + 54, 58, 16).build();

        this.addRenderableWidget(eruptButton);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, "Supreme Volcanic Gen", 8, 6, 0xFF7A00, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xCECAC5, false);

        int phase = this.menu.getPhase();
        if (phase == SupremeVolcanicGeneratorBlockEntity.PHASE_ERUPTING) {
            int rate = this.menu.getCurrentBurstRate();
            int secs = (this.menu.getEruptionTicks() + 19) / 20;
            guiGraphics.drawString(this.font, "+" + (rate / 1000) + "k RF/t", 84, 23, 0xFF5555, false);
            guiGraphics.drawString(this.font, "Burst: " + secs + "s", 84, 33, 0xFFAA00, false);
        } else if (phase == SupremeVolcanicGeneratorBlockEntity.PHASE_COOLING) {
            int secs = (this.menu.getCooldownTicks() + 19) / 20;
            guiGraphics.drawString(this.font, "Cooling", 84, 23, 0x48CAE4, false);
            guiGraphics.drawString(this.font, "Wait: " + secs + "s", 84, 33, 0xAAAAAA, false);
        } else {
            int pressure = this.menu.getPressure();
            ItemStack cat = this.menu.getCatalystItem();
            int potRate = SupremeVolcanicGeneratorBlockEntity.getCatalystBurstRate(cat);
            guiGraphics.drawString(this.font, "Seismic:" + pressure + "%", 84, 23, 0xFFD166, false);
            String rateTxt = potRate > 0 ? ("+" + (potRate / 1000) + "k/t") : "Need StarCat";
            guiGraphics.drawString(this.font, rateTxt, 84, 33, potRate > 0 ? 0x55FF55 : 0xFF5555, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (eruptButton != null) {
            int phase = this.menu.getPhase();
            int pressure = this.menu.getPressure();
            boolean canManualErupt = (phase == SupremeVolcanicGeneratorBlockEntity.PHASE_PRESSURIZING
                    && pressure >= 50
                    && this.menu.getDistillateAmount() >= 1000
                    && SupremeVolcanicGeneratorBlockEntity.getCatalystBurstRate(this.menu.getCatalystItem()) > 0);
            eruptButton.active = canManualErupt;
            if (phase == SupremeVolcanicGeneratorBlockEntity.PHASE_ERUPTING) {
                eruptButton.setMessage(Component.literal("§cSURGING!"));
            } else if (phase == SupremeVolcanicGeneratorBlockEntity.PHASE_COOLING) {
                eruptButton.setMessage(Component.literal("§bDORMANT"));
            } else {
                eruptButton.setMessage(Component.literal("§6ERUPT!"));
            }
        }

        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        // Tooltip for Pyroclastic Distillate Chamber (38, 20) 16x50
        if (mouseX >= i + 38 && mouseX <= i + 54 && mouseY >= j + 20 && mouseY <= j + 70) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("§d⚗ Pyroclastic Distillate Chamber"));
            lines.add(Component.literal("§f" + this.menu.getDistillateAmount() + " / " + SupremeVolcanicGeneratorBlockEntity.MAX_DISTILLATE + " mB Distillate"));
            lines.add(Component.literal("§7Produced in the §dVolcanic Distillation Chamber"));
            lines.add(Component.literal("§7Consumes 1,000 mB Distillate per Eruption"));
            guiGraphics.renderTooltip(this.font, lines, java.util.Optional.empty(), mouseX, mouseY);
        }

        // Tooltip for Tectonic Pressure / Eruption Bar (62, 43) 76x8
        if (mouseX >= i + 62 && mouseX <= i + 138 && mouseY >= j + 42 && mouseY <= j + 52) {
            List<Component> lines = new ArrayList<>();
            int phase = this.menu.getPhase();
            if (phase == SupremeVolcanicGeneratorBlockEntity.PHASE_ERUPTING) {
                lines.add(Component.literal("§c§l🌋 VOLCANIC ERUPTION SURGE!"));
                lines.add(Component.literal("§6Generating: §e+" + this.menu.getCurrentBurstRate() + " RF/t"));
                lines.add(Component.literal("§7Burst Remaining: §f" + (this.menu.getEruptionTicks() / 20.0f) + "s"));
            } else if (phase == SupremeVolcanicGeneratorBlockEntity.PHASE_COOLING) {
                lines.add(Component.literal("§b❄ Magma Crust Cooling (Dormant)"));
                lines.add(Component.literal("§7Chamber stabilizing after eruption..."));
                lines.add(Component.literal("§7Ready in: §f" + (this.menu.getCooldownTicks() / 20.0f) + "s"));
            } else {
                lines.add(Component.literal("§6Seismic Pressure: §e" + this.menu.getPressure() + "%"));
                lines.add(Component.literal("§7Auto-erupts at 100% (or click ERUPT at 50%+)"));
                lines.add(Component.literal("§8Required Catalyst (Center Slot):"));
                lines.add(Component.literal(" §6✦ Astral Volcanic Catalyst §7(Nether Star + Ash):"));
                lines.add(Component.literal("   §e35,000 RF/t §7for 12s (§68,400,000 RF §7burst!)"));
            }
            guiGraphics.renderTooltip(this.font, lines, java.util.Optional.empty(), mouseX, mouseY);
        }

        // Tooltip for Energy Buffer (144, 20) 16x50
        if (mouseX >= i + 144 && mouseX <= i + 160 && mouseY >= j + 20 && mouseY <= j + 70) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("§b⚡ Supreme Capacitor Buffer"));
            lines.add(Component.literal("§f" + String.format("%,d", this.menu.getEnergyAmount()) + " / 10,000,000 RF"));
            lines.add(Component.literal("§7Max Output: §b100,000 RF/t"));
            guiGraphics.renderTooltip(this.font, lines, java.util.Optional.empty(), mouseX, mouseY);
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

        guiGraphics.fill(i + 12, j + 18, i + 164, j + 74, 0xFF141119);

        int phase = this.menu.getPhase();

        // 1. Slot frames
        drawSlotBox(guiGraphics, i + 15, j + 21, 0xFFD90467); // Slot 0: Distillate
        drawSlotBox(guiGraphics, i + 15, j + 51, 0xFF5A4E68); // Slot 1: Return
        drawSlotBox(guiGraphics, i + 63, j + 21, phase == SupremeVolcanicGeneratorBlockEntity.PHASE_ERUPTING ? 0xFFFF3B00 : 0xFFFFB703); // Catalyst

        // 2. Pyroclastic Distillate Chamber (38, 20) 16x50
        guiGraphics.fill(i + 37, j + 19, i + 55, j + 71, 0xFF5A1E5C);
        guiGraphics.fill(i + 38, j + 20, i + 54, j + 70, 0xFF0B090E);
        int dist = this.menu.getDistillateAmount();
        if (dist > 0) {
            int height = Math.max(1, (int) (50.0f * ((float) dist / SupremeVolcanicGeneratorBlockEntity.MAX_DISTILLATE)));
            int topY = j + 70 - height;
            guiGraphics.fillGradient(i + 38, topY, i + 54, j + 70, 0xFFFF4D94, 0xFFD90467);
            // Shimmering distillate bubbles
            for (int b = 0; b < 2; b++) {
                int by = j + 68 - (int) (((now / 35) + b * 20) % Math.max(1, height));
                guiGraphics.fill(i + 42 + b * 5, by, i + 44 + b * 5, by + 2, 0xFFFFD1E8);
            }
        }

        // 3. Tectonic Caldera & Dynamic Seismic / Eruption Animation (62, 43, 76x8)
        guiGraphics.fill(i + 62, j + 42, i + 138, j + 51, 0xFF3A2846);
        guiGraphics.fill(i + 63, j + 43, i + 137, j + 50, 0xFF09070C);

        if (phase == SupremeVolcanicGeneratorBlockEntity.PHASE_ERUPTING) {
            // == PHASE 1: MASSIVE CATACLYSMIC VOLCANIC ERUPTION ==
            int fillW = (int) (74.0f * ((float) this.menu.getEruptionTicks() / this.menu.getMaxEruptionTicks()));
            guiGraphics.fillGradient(i + 63, j + 43, i + 63 + fillW, j + 50, 0xFFFEF08A, 0xFFFF3B00);

            // Blazing vertical magma geyser column behind catalyst slot
            int geyserX = i + 72;
            int waveShift = (int) (Math.sin(now / 40.0) * 3);
            guiGraphics.fill(geyserX - 4 + waveShift, j + 18, geyserX + 4 + waveShift, j + 42, 0xFFFF3B00);
            guiGraphics.fill(geyserX - 2 + waveShift, j + 18, geyserX + 2 + waveShift, j + 42, 0xFFFFFBEB);

            // Spraying pyroclastic magma sparks
            for (int s = 0; s < 5; s++) {
                int sx = i + 64 + (int) (((now / 20) + s * 16) % 72);
                int sy = j + 40 - (int) (((now / 25) + s * 12) % 22);
                guiGraphics.fill(sx, sy, sx + 2, sy + 2, (s % 2 == 0) ? 0xFFFDE047 : 0xFFFF3B00);
            }

            // Power discharge lightning arcs connecting to Energy Capacitor (x=144)
            int arcY = j + 35 + (int) (Math.sin(now / 50.0) * 10);
            guiGraphics.fill(i + 138, arcY, i + 144, arcY + 2, 0xFFFBBF24);
            guiGraphics.fill(i + 138, arcY, i + 144, arcY + 1, 0xFFFFFFFF);

        } else if (phase == SupremeVolcanicGeneratorBlockEntity.PHASE_COOLING) {
            // == PHASE 2: COOLING CRYOGENIC OBSIDIAN CRUST ==
            int fillW = (int) (74.0f * ((float) this.menu.getCooldownTicks() / SupremeVolcanicGeneratorBlockEntity.COOLDOWN_DURATION));
            guiGraphics.fillGradient(i + 63, j + 43, i + 63 + fillW, j + 50, 0xFF38BDF8, 0xFF0284C7);

            // Cold smoke / cryogenic vapor wisps
            for (int w = 0; w < 3; w++) {
                int wx = i + 70 + w * 22;
                int wy = j + 40 - (int) (((now / 45) + w * 15) % 18);
                guiGraphics.fill(wx, wy, wx + 3, wy + 2, 0x88BAE6FD);
            }

        } else {
            // == PHASE 3: SEISMIC PRESSURE CHARGING ==
            int pressure = this.menu.getPressure();
            int fillW = (int) (74.0f * (pressure / 100.0f));
            guiGraphics.fillGradient(i + 63, j + 43, i + 63 + fillW, j + 50, 0xFFFBBF24, 0xFFEA580C);

            // Tectonic vibration cracks when pressure > 50%
            if (pressure >= 50) {
                int pulse = (int) ((Math.sin(now / 80.0) + 1.0) * 2);
                guiGraphics.fill(i + 63 + fillW - pulse, j + 43, i + 63 + fillW, j + 50, 0xFFFFFBEB);
            }
        }

        // 4. Tank 2: Supreme Energy Capacitor (144, 20) 16x50
        guiGraphics.fill(i + 143, j + 19, i + 161, j + 71, 0xFF1D3546);
        guiGraphics.fill(i + 144, j + 20, i + 160, j + 70, 0xFF0B090E);
        int energy = this.menu.getEnergyAmount();
        if (energy > 0) {
            int height = Math.max(1, (int) (50.0f * ((float) energy / SupremeVolcanicGeneratorBlockEntity.MAX_ENERGY)));
            guiGraphics.blit(ENERGY_OVERLAY, i + 144, j + 70 - height, 0, 50 - height, 16, height, 16, 50);

            if (phase == SupremeVolcanicGeneratorBlockEntity.PHASE_ERUPTING) {
                // High-voltage surge shimmer
                int sparkY = j + 70 - (int) ((now / 20) % height);
                guiGraphics.fill(i + 145, sparkY, i + 159, sparkY + 2, 0xFFFEF08A);
            }
        }
    }

    private void drawSlotBox(GuiGraphics g, int x, int y, int borderColor) {
        g.fill(x, y, x + 18, y + 18, borderColor);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF16131D);
    }
}
