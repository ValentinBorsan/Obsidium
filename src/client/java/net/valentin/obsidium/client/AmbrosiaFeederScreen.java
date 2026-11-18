package net.valentin.obsidium.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.block.AmbrosiaFeederBlockEntity;
import net.valentin.obsidium.block.AmbrosiaFeederMenu;

public final class AmbrosiaFeederScreen extends AbstractContainerScreen<AmbrosiaFeederMenu> {
    public AmbrosiaFeederScreen(AmbrosiaFeederMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 176;
        inventoryLabelY = 83;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        ObsidiumMachineGui.drawFrame(g, leftPos, topPos, imageWidth, imageHeight, 0xFF3CE070);
        ObsidiumMachineGui.drawEnergyBar(g, leftPos + 8, topPos + 23, 160, 6,
                menu.energy(), AmbrosiaFeederBlockEntity.MAX_ENERGY, 0xFF3CE070);

        ObsidiumMachineGui.drawCard(g, leftPos + 12, topPos + 32, 66, 50, 0xFF4B3759);
        ObsidiumMachineGui.drawCard(g, leftPos + 102, topPos + 32, 62, 44, 0xFF4B3759);

        for (var slot : menu.slots) {
            ObsidiumMachineGui.drawSlot(g, leftPos + slot.x, topPos + slot.y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        ObsidiumMachineGui.drawFittedText(g, font, title, 8, 7, 94, 0xFFF4C438);
        g.drawString(font, Component.translatable("gui.obsidium.pastoral.food"), 16, 34, 0xFF9C8FB4, false);
        g.drawString(font, Component.translatable("gui.obsidium.pastoral.output"), 107, 34, 0xFF28BEB9, false);
        String pen = Component.translatable("gui.obsidium.pastoral.pen", menu.animalCount()).getString();
        g.drawString(font, pen, imageWidth - font.width(pen) - 8, 7, 0xFF3CE070, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xFF9C8FB4, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
