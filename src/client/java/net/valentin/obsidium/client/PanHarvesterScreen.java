package net.valentin.obsidium.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.block.PanHarvesterBlockEntity;
import net.valentin.obsidium.block.PanHarvesterMenu;

public final class PanHarvesterScreen extends AbstractContainerScreen<PanHarvesterMenu> {
    private Button keepBtn;

    public PanHarvesterScreen(PanHarvesterMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 168;
        inventoryLabelY = 75;
    }

    @Override
    protected void init() {
        super.init();
        keepBtn = addRenderableWidget(Button.builder(Component.translatable("gui.obsidium.pastoral.keep", menu.keepAdults()), btn -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            }
        }).bounds(leftPos + 8, topPos + 33, 160, 18).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        keepBtn.setMessage(Component.translatable("gui.obsidium.pastoral.keep", menu.keepAdults()));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        ObsidiumMachineGui.drawFrame(g, leftPos, topPos, imageWidth, imageHeight, 0xFFFF8822);
        ObsidiumMachineGui.drawEnergyBar(g, leftPos + 8, topPos + 23, 160, 7,
                menu.energy(), PanHarvesterBlockEntity.MAX_ENERGY, 0xFFFF8822);

        for (var slot : menu.slots) {
            ObsidiumMachineGui.drawSlot(g, leftPos + slot.x, topPos + slot.y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        ObsidiumMachineGui.drawFittedText(g, font, title, 8, 7, 100, 0xFFF4C438);
        String rf = ObsidiumMachineGui.formatRf(menu.energy());
        g.drawString(font, rf, imageWidth - font.width(rf) - 8, 7, 0xFF9C8FB4, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xFF9C8FB4, false);
        ObsidiumMachineGui.drawFittedText(g, font, Component.translatable("gui.obsidium.pastoral.pending", menu.pendingItems()), 94, inventoryLabelY, 72, 0xFF28BEB9);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
