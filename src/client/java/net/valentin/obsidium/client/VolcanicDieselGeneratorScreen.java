package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.block.VolcanicDieselGeneratorBlockEntity;
import net.valentin.obsidium.block.VolcanicDieselGeneratorMenu;

import java.util.List;

public class VolcanicDieselGeneratorScreen extends AbstractContainerScreen<VolcanicDieselGeneratorMenu> {

    public VolcanicDieselGeneratorScreen(VolcanicDieselGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 1000;
        this.inventoryLabelX = 1000;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.fill(x - 1, y - 1, x + this.imageWidth + 1, y + this.imageHeight + 1, 0xFF080A0E);
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF141218);

        guiGraphics.fill(x, y, x + this.imageWidth, y + 1, 0xFF3A2622);
        guiGraphics.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF1E1412);
        guiGraphics.fill(x, y, x + 1, y + this.imageHeight, 0xFF3A2622);
        guiGraphics.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, 0xFF1E1412);

        int pin = 0xFFF97316;
        guiGraphics.fill(x + 1, y + 1, x + 3, y + 3, pin);
        guiGraphics.fill(x + this.imageWidth - 3, y + 1, x + this.imageWidth - 1, y + 3, pin);
        guiGraphics.fill(x + 1, y + this.imageHeight - 3, x + 3, y + this.imageHeight - 1, pin);
        guiGraphics.fill(x + this.imageWidth - 3, y + this.imageHeight - 3, x + this.imageWidth - 1, y + this.imageHeight - 1, pin);

        guiGraphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + 16, 0xFF1D1514);
        guiGraphics.fill(x + 1, y + 16, x + this.imageWidth - 1, y + 17, 0xFF382420);

        guiGraphics.fill(x + 6, y + 18, x + 170, y + 74, 0xFF1A1416);

        for (var slot : this.menu.slots) {
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            guiGraphics.fill(sx, sy, sx + 18, sy + 18, 0xFF382824);
            guiGraphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF0D0B0E);
        }

        int tankX = x + 50;
        int tankY = y + 21;
        int tankW = 14;
        int tankH = 46;
        guiGraphics.fill(tankX - 1, tankY - 1, tankX + tankW + 1, tankY + tankH + 1, 0xFF382824);
        guiGraphics.fill(tankX, tankY, tankX + tankW, tankY + tankH, 0xFF0A090D);

        int fluidAmt = this.menu.getFluidAmount();
        int maxFluid = this.menu.getMaxFluid();
        if (fluidAmt > 0 && maxFluid > 0) {
            int fillH = Math.max(1, (int) ((long) fluidAmt * tankH / maxFluid));
            int[] colors = getFluidColors(this.menu.getFluidTypeId());
            guiGraphics.fill(tankX, tankY + tankH - fillH, tankX + tankW, tankY + tankH, colors[0]);
            guiGraphics.fill(tankX + 2, tankY + tankH - fillH, tankX + tankW - 2, tankY + tankH, colors[1]);
        }

        int rpmX = x + 72;
        int rpmY = y + 30;
        int rpmW = 54;
        int rpmH = 8;
        guiGraphics.fill(rpmX - 1, rpmY - 1, rpmX + rpmW + 1, rpmY + rpmH + 1, 0xFF382824);
        guiGraphics.fill(rpmX, rpmY, rpmX + rpmW, rpmY + rpmH, 0xFF0A090D);
        int rpm = this.menu.getRpm();
        if (rpm > 0) {
            int fillW = Math.max(1, rpm * rpmW / VolcanicDieselGeneratorBlockEntity.MAX_RPM);
            int rpmColor = rpm >= 800 ? 0xFF22C55E : (rpm >= 400 ? 0xFFF59E0B : 0xFFEF4444);
            guiGraphics.fill(rpmX, rpmY, rpmX + fillW, rpmY + rpmH, rpmColor);
        }

        int burn = this.menu.getBurnTime();
        int maxBurn = Math.max(1, this.menu.getMaxBurnTime());
        int flameX = x + 72;
        int flameY = y + 46;
        guiGraphics.fill(flameX, flameY, flameX + 14, flameY + 14, 0xFF221414);
        if (burn > 0) {
            int flameH = Math.max(1, burn * 14 / maxBurn);
            guiGraphics.fill(flameX, flameY + (14 - flameH), flameX + 14, flameY + 14, 0xFFEF4444);
            guiGraphics.fill(flameX + 2, flameY + (14 - flameH) + 2, flameX + 12, flameY + 14, 0xFFFACC15);
        }

        int eX = x + 154;
        int eY = y + 21;
        int eW = 12;
        int eH = 46;
        guiGraphics.fill(eX - 1, eY - 1, eX + eW + 1, eY + eH + 1, 0xFF382824);
        guiGraphics.fill(eX, eY, eX + eW, eY + eH, 0xFF0A090D);
        long energy = this.menu.getEnergy();
        long maxEnergy = this.menu.getMaxEnergy();
        if (energy > 0 && maxEnergy > 0) {
            int fillH = Math.max(1, (int) (energy * eH / maxEnergy));
            guiGraphics.fill(eX, eY + eH - fillH, eX + eW, eY + eH, 0xFFD97706);
            guiGraphics.fill(eX + 2, eY + eH - fillH, eX + eW - 2, eY + eH, 0xFFFDE047);
        }

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int sx = x + 8 + col * 18;
                int sy = y + 84 + row * 18;
                guiGraphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF2A2020);
                guiGraphics.fill(sx, sy, sx + 16, sy + 16, 0xFF0C0A0E);
            }
        }
        for (int col = 0; col < 9; ++col) {
            int sx = x + 8 + col * 18;
            int sy = y + 142;
            guiGraphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF2A2020);
            guiGraphics.fill(sx, sy, sx + 16, sy + 16, 0xFF0C0A0E);
        }
    }

    private static int[] getFluidColors(int typeId) {
        return switch (typeId) {
            case 1 -> new int[]{0xFFB45309, 0xFFF59E0B};
            case 2 -> new int[]{0xFF0284C7, 0xFF38BDF8};
            case 3 -> new int[]{0xFF15803D, 0xFF4ADE80};
            case 4 -> new int[]{0xFFB91C1C, 0xFFFB923C};
            default -> new int[]{0xFF4B5563, 0xFF9CA3AF};
        };
    }

    private static String getFluidName(int typeId) {
        return switch (typeId) {
            case 1 -> "§6Bio-Oil";
            case 2 -> "§bBio-Ethanol";
            case 3 -> "§aBiodiesel";
            case 4 -> "§cPyro-Diesel";
            default -> "§7Empty";
        };
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, Component.literal("§6⚙ §fVolcanic Diesel Gen"), 8, 5, 0xFFFFFF, false);

        int rpmPct = this.menu.getRpm() / 10;
        int bonusPct = (int) Math.round(25.0 * this.menu.getRpm() / VolcanicDieselGeneratorBlockEntity.MAX_RPM);
        guiGraphics.drawString(this.font, "§7RPM: §f" + rpmPct + "% §a(+" + bonusPct + "%)", 71, 20, 0xFFFFFF, false);

        int effRf = this.menu.getEffectiveRfPerTick();
        guiGraphics.drawString(this.font, effRf > 0 ? ("§e" + effRf + " RF/t") : "§8Idle", 90, 49, 0xFFFFFF, false);

        guiGraphics.drawString(this.font, Component.literal("§8INVENTORY"), 8, 74, 0x64748B, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        if (mouseX >= x + 49 && mouseX <= x + 65 && mouseY >= y + 20 && mouseY <= y + 68) {
            int amt = this.menu.getFluidAmount();
            int max = this.menu.getMaxFluid();
            String name = getFluidName(this.menu.getFluidTypeId());
            guiGraphics.renderTooltip(this.font, List.of(
                    Component.literal(name + " §f(" + String.format("%,d", amt) + " / " + String.format("%,d", max) + " mB)"),
                    Component.literal("§7Accepts Bio-Oil, Bio-Ethanol, Biodiesel, Pyro-Diesel")
            ), java.util.Optional.empty(), mouseX, mouseY);
        }

        if (mouseX >= x + 153 && mouseX <= x + 167 && mouseY >= y + 20 && mouseY <= y + 68) {
            guiGraphics.renderTooltip(this.font, Component.literal(
                    "§eEnergy: §f" + String.format("%,d", this.menu.getEnergy()) + " / " + String.format("%,d", this.menu.getMaxEnergy()) + " FE"
            ), mouseX, mouseY);
        }
    }
}

