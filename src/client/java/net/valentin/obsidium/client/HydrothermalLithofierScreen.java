package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.HydrothermalLithofierBlockEntity;
import net.valentin.obsidium.block.HydrothermalLithofierMenu;

import java.util.List;

public class HydrothermalLithofierScreen extends AbstractContainerScreen<HydrothermalLithofierMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/geothermal_generator_gui.png");

    public HydrothermalLithofierScreen(HydrothermalLithofierMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 73;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, "✦ Hydrothermal Lithofier", this.titleLabelX, this.titleLabelY, 0xFFAA33, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xCECAC5, false);

        int max = this.menu.getMaxProgress();
        int prog = max > 0 ? (int) (100.0f * this.menu.getProgress() / max) : 0;
        if (prog > 0) {
            String progText = prog + "%";
            int tw = this.font.width(progText);
            guiGraphics.drawString(this.font, progText, 87 - tw / 2, 41, 0xFFDD66, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        // Tooltip: Water Tank (10, 20) 14x50
        if (mouseX >= i + 10 && mouseX <= i + 24 && mouseY >= j + 20 && mouseY <= j + 70) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§b💧 Hydrothermal Water Tank"),
                    Component.literal("§f" + this.menu.getWater() + " / " + HydrothermalLithofierBlockEntity.MAX_WATER + " mB"),
                    Component.literal("§7Dew Droplet: §b+250 mB §7each"),
                    Component.literal("§7Water Bucket: §b+1,000 mB"),
                    Component.literal("§8Piped automatically via Naiad Fluid Pipes")
            ), mouseX, mouseY);
        }

        // Tooltip: Lava Tank (152, 20) 14x50
        if (mouseX >= i + 152 && mouseX <= i + 166 && mouseY >= j + 20 && mouseY <= j + 70) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6🌋 Magma / Lava Tank"),
                    Component.literal("§f" + this.menu.getLava() + " / " + HydrothermalLithofierBlockEntity.MAX_LAVA + " mB"),
                    Component.literal("§7Magma Droplet: §6+250 mB §7each"),
                    Component.literal("§7Lava Bucket: §6+1,000 mB"),
                    Component.literal("§8Piped automatically via Naiad Fluid Pipes")
            ), mouseX, mouseY);
        }

        // Tooltip: Water Input Slot (28, 23)
        if (this.menu.getSlot(0).getItem().isEmpty() && mouseX >= i + 27 && mouseX <= i + 45 && mouseY >= j + 22 && mouseY <= j + 40) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§bWater Input"),
                    Component.literal("§7Accepts: §bDew Droplets §7(+250 mB) or §bWater Buckets"),
                    Component.literal("§8Drains automatically into internal water tank")
            ), mouseX, mouseY);
        }

        // Tooltip: Water Bucket Output Slot (28, 51)
        if (this.menu.getSlot(1).getItem().isEmpty() && mouseX >= i + 27 && mouseX <= i + 45 && mouseY >= j + 50 && mouseY <= j + 68) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§bEmpty Bucket Output"),
                    Component.literal("§8Returns empty buckets after water extraction")
            ), mouseX, mouseY);
        }

        // Tooltip: Lava Input Slot (132, 23)
        if (this.menu.getSlot(2).getItem().isEmpty() && mouseX >= i + 131 && mouseX <= i + 149 && mouseY >= j + 22 && mouseY <= j + 40) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6Lava Input"),
                    Component.literal("§7Accepts: §6Magma Droplets §7(+250 mB) or §6Lava Buckets"),
                    Component.literal("§8Drains automatically into internal lava tank")
            ), mouseX, mouseY);
        }

        // Tooltip: Lava Bucket Output Slot (132, 51)
        if (this.menu.getSlot(3).getItem().isEmpty() && mouseX >= i + 131 && mouseX <= i + 149 && mouseY >= j + 50 && mouseY <= j + 68) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6Empty Bucket Output"),
                    Component.literal("§8Returns empty buckets after lava extraction")
            ), mouseX, mouseY);
        }

        // Tooltip: Mode Buttons (52, 70, 88, 106 at y=21)
        String[] modeNames = {"§eCobblestone Mode", "§fStone Mode", "§7Smooth Basalt Mode", "§8Deepslate Mode"};
        String[] modeCosts = {"25 mB Water + 25 mB Lava", "35 mB Water + 35 mB Lava", "50 mB Water + 50 mB Lava", "75 mB Water + 75 mB Lava"};
        for (int m = 0; m < 4; m++) {
            int btnX = i + 52 + m * 18;
            int btnY = j + 21;
            if (mouseX >= btnX && mouseX < btnX + 18 && mouseY >= btnY && mouseY < btnY + 18) {
                guiGraphics.renderComponentTooltip(this.font, List.of(
                        Component.literal(modeNames[m]),
                        Component.literal("§7Cost: §b" + modeCosts[m]),
                        Component.literal(this.menu.getMode() == m ? "§a✔ Currently Active" : "§eClick to Select")
                ), mouseX, mouseY);
            }
        }

        // Tooltip: Speed Upgrade Slot (56, 48)
        if (this.menu.getSlot(5).getItem().isEmpty() && mouseX >= i + 55 && mouseX <= i + 73 && mouseY >= j + 47 && mouseY <= j + 65) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§eSpeed / Conduit Upgrade Slot"),
                    Component.literal("§7Accepts: §6Conduit Energy Upgrades (Tiers I - III)"),
                    Component.literal("§8Dramatically accelerates stone formation speed")
            ), mouseX, mouseY);
        }

        // Tooltip: Output Slot (102, 48)
        if (this.menu.getSlot(4).getItem().isEmpty() && mouseX >= i + 101 && mouseX <= i + 119 && mouseY >= j + 47 && mouseY <= j + 65) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§aFormed Rock Output"),
                    Component.literal("§7Synthesizes cobblestone, stone, basalt or deepslate"),
                    Component.literal("§8Auto-eject to adjacent inventories when configured")
            ), mouseX, mouseY);
        }

        // Tooltip: Progress Arrow (75, 51) 24x10
        if (mouseX >= i + 75 && mouseX <= i + 99 && mouseY >= j + 51 && mouseY <= j + 61) {
            int max = this.menu.getMaxProgress();
            int prog = max > 0 ? (int) (100.0f * this.menu.getProgress() / max) : 0;
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6✦ Hydrothermal Lithification"),
                    Component.literal("§7Progress: §e" + prog + "%"),
                    Component.literal("§7RF Energy: §e" + this.menu.getEnergy() + " / " + this.menu.getMaxEnergy() + " RF"),
                    Component.literal("§8Passive mode: 0 RF (Accelerated when powered or upgraded)")
            ), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        VolcaniteGuiTheme.renderExteriorFrame(guiGraphics, i, j, this.imageWidth, this.imageHeight, false);
        guiGraphics.blit(GUI_TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        // Clear machine upper interior with dark vulcan industrial background
        guiGraphics.fill(i + 7, j + 18, i + 169, j + 71, 0xFF140E1B);
        border(guiGraphics, i + 7, j + 18, 162, 53, 0xFF2A1C36);

        // 1. Water Fluid Tank (i + 10, j + 20, width 14, height 50)
        drawTankGauge(guiGraphics, i + 10, j + 20, 14, 50, this.menu.getWater(), HydrothermalLithofierBlockEntity.MAX_WATER, 0xFF0077B6, 0xCC00B4D8, 0xFF90E0EF);

        // 2. Lava Fluid Tank (i + 152, j + 20, width 14, height 50)
        drawTankGauge(guiGraphics, i + 152, j + 20, 14, 50, this.menu.getLava(), HydrothermalLithofierBlockEntity.MAX_LAVA, 0xFF9E2A2B, 0xCCD9381E, 0xFFFF8C00);

        // 3. Render Themed Slot Frames
        // Water Droplet/Bucket In (28, 23) -> frame (27, 22)
        drawSlotFrame(guiGraphics, i + 27, j + 22, 0xFF00B4D8);
        if (this.menu.getSlot(0).getItem().isEmpty()) {
            guiGraphics.renderFakeItem(new ItemStack(ObsidiumMod.DEW_DROPLET), i + 28, j + 23);
            guiGraphics.fill(i + 28, j + 23, i + 44, j + 39, 0x99140E1B);
        }

        // Water Bucket Out (28, 51) -> frame (27, 50)
        drawSlotFrame(guiGraphics, i + 27, j + 50, 0xFF0077B6);
        if (this.menu.getSlot(1).getItem().isEmpty()) {
            guiGraphics.renderFakeItem(new ItemStack(Items.BUCKET), i + 28, j + 51);
            guiGraphics.fill(i + 28, j + 51, i + 44, j + 67, 0x99140E1B);
        }

        // Lava Droplet/Bucket In (132, 23) -> frame (131, 22)
        drawSlotFrame(guiGraphics, i + 131, j + 22, 0xFFFF5500);
        if (this.menu.getSlot(2).getItem().isEmpty()) {
            guiGraphics.renderFakeItem(new ItemStack(ObsidiumMod.MAGMA_DROPLET), i + 132, j + 23);
            guiGraphics.fill(i + 132, j + 23, i + 148, j + 39, 0x99140E1B);
        }

        // Lava Bucket Out (132, 51) -> frame (131, 50)
        drawSlotFrame(guiGraphics, i + 131, j + 50, 0xFF9E2A2B);
        if (this.menu.getSlot(3).getItem().isEmpty()) {
            guiGraphics.renderFakeItem(new ItemStack(Items.BUCKET), i + 132, j + 51);
            guiGraphics.fill(i + 132, j + 51, i + 148, j + 67, 0x99140E1B);
        }

        // Slot 5: Upgrade Slot (56, 48) -> frame (55, 47)
        drawSlotFrame(guiGraphics, i + 55, j + 47, 0xFF00AAFF);
        if (this.menu.getSlot(5).getItem().isEmpty()) {
            guiGraphics.renderFakeItem(new ItemStack(ObsidiumMod.VOLCANITE_BASIC_PIPE_ENERGY_UPGRADE), i + 56, j + 48);
            guiGraphics.fill(i + 56, j + 48, i + 72, j + 64, 0xAA140E1B);
        }

        // Slot 4: Output Slot (102, 48) -> frame (101, 47) with golden highlight
        guiGraphics.fill(i + 101, j + 47, i + 119, j + 65, 0xFF2A2035);
        border(guiGraphics, i + 101, j + 47, 18, 18, 0xFFFFCC00);
        border(guiGraphics, i + 102, j + 48, 16, 16, 0xFF5A3E1B);

        // 4. Mode Selector Buttons (52, 70, 88, 106) at y=21
        ItemStack[] modeIcons = {
                new ItemStack(Blocks.COBBLESTONE),
                new ItemStack(Blocks.STONE),
                new ItemStack(Blocks.SMOOTH_BASALT),
                new ItemStack(Blocks.DEEPSLATE)
        };

        int activeMode = this.menu.getMode();
        for (int m = 0; m < 4; m++) {
            int btnX = i + 52 + m * 18;
            int btnY = j + 21;

            boolean hovered = mouseX >= btnX && mouseX < btnX + 18 && mouseY >= btnY && mouseY < btnY + 18;
            int bgCol = (m == activeMode) ? 0xFF4A3520 : (hovered ? 0xFF352B3C : 0xFF1D1624);
            guiGraphics.fill(btnX, btnY, btnX + 18, btnY + 18, bgCol);

            int borderCol = (m == activeMode) ? 0xFFFFCC00 : (hovered ? 0xFFAAAAAA : 0xFF3D2A48);
            border(guiGraphics, btnX, btnY, 18, 18, borderCol);

            guiGraphics.renderFakeItem(modeIcons[m], btnX + 1, btnY + 1);
        }

        // 5. Lithification Progress Channel (i + 75, j + 51, width 24, height 10)
        long time = System.currentTimeMillis();
        int maxProg = this.menu.getMaxProgress();
        int progWidth = maxProg > 0 ? (int) (22.0f * this.menu.getProgress() / maxProg) : 0;
        guiGraphics.fill(i + 75, j + 51, i + 99, j + 61, 0xFF0A0610);
        border(guiGraphics, i + 75, j + 51, 24, 10, 0xFF4A255A);
        if (progWidth > 0) {
            guiGraphics.fill(i + 76, j + 52, i + 76 + progWidth, j + 60, 0xFFFF6600);
            guiGraphics.fill(i + 76, j + 52, i + 76 + progWidth, j + 54, 0xFFFFCC00);
            int shockPos = (int) ((time / 60) % Math.max(1, progWidth));
            guiGraphics.fill(i + 76 + shockPos, j + 52, Math.min(i + 76 + shockPos + 3, i + 76 + progWidth), j + 60, 0xFF00E5FF);
        }
        guiGraphics.drawString(this.font, "➔", i + 83, j + 52, 0xFFFFCC00, false);

        // 6. Dual elemental convergence stream pulses
        int streamStep = (int) ((time / 120) % 5);
        // Water stream from left to center
        guiGraphics.fill(i + 46 + streamStep, j + 55, i + 48 + streamStep, j + 57, 0xFF00E5FF);
        // Lava stream from right to center
        guiGraphics.fill(i + 128 - streamStep, j + 55, i + 130 - streamStep, j + 57, 0xFFFF5500);
    }

    private void drawSlotFrame(GuiGraphics g, int x, int y, int borderColor) {
        g.fill(x, y, x + 18, y + 18, 0xFF0B0711);
        border(g, x, y, 18, 18, borderColor);
    }

    private void drawTankGauge(GuiGraphics g, int x, int y, int w, int h, int amount, int max, int borderCol, int fluidCol, int surfaceCol) {
        g.fill(x, y, x + w, y + h, 0xFF08050C);
        border(g, x, y, w, h, borderCol);

        if (max > 0 && amount > 0) {
            int innerH = h - 2;
            int innerW = w - 2;
            int filledH = (int) ((float) innerH * Math.min(amount, max) / max);
            if (filledH > 0) {
                int topY = y + 1 + (innerH - filledH);
                g.fill(x + 1, topY, x + 1 + innerW, y + h - 1, fluidCol);
                g.fill(x + 1, topY, x + 1 + innerW, Math.min(topY + 2, y + h - 1), surfaceCol);

                // Animated fluid surface shimmer
                long time = System.currentTimeMillis();
                int ripple = (int) ((time / 250) % Math.max(1, innerW));
                g.fill(x + 1 + ripple, topY, Math.min(x + 3 + ripple, x + 1 + innerW), Math.min(topY + 1, y + h - 1), 0xAAFFFFFF);
            }
        }

        // Subtle graduation tick marks
        for (int tick = 1; tick <= 3; tick++) {
            int tickY = y + h - 1 - (h - 2) * tick / 4;
            g.fill(x + 1, tickY, x + 3, tickY + 1, 0x44FFFFFF);
            g.fill(x + w - 3, tickY, x + w - 1, tickY + 1, 0x44FFFFFF);
        }
    }

    private void border(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        for (int m = 0; m < 4; m++) {
            int btnX = i + 52 + m * 18;
            int btnY = j + 21;
            if (mouseX >= btnX && mouseX < btnX + 18 && mouseY >= btnY && mouseY < btnY + 18) {
                if (this.minecraft != null && this.minecraft.gameMode != null) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, m);
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
