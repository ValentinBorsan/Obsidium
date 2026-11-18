package net.valentin.obsidium.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.block.AegisSanctuaryWardBlockEntity;
import net.valentin.obsidium.block.AegisSanctuaryWardMenu;

public final class AegisSanctuaryWardScreen extends AbstractContainerScreen<AegisSanctuaryWardMenu> {
    private Button toggleBtn;

    public AegisSanctuaryWardScreen(AegisSanctuaryWardMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 212;
        imageHeight = 132;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 10;
        titleLabelY = 7;
        toggleBtn = addRenderableWidget(Button.builder(Component.translatable("gui.obsidium.ward.toggle"), btn -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            }
        }).bounds(leftPos + 12, topPos + 98, 188, 20).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        toggleBtn.setMessage(Component.translatable(menu.isManualEnabled() ? "gui.obsidium.ward.enabled" : "gui.obsidium.ward.paused"));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        ObsidiumMachineGui.drawFrame(g, leftPos, topPos, imageWidth, imageHeight, 0xFF28BEB9);
        ObsidiumMachineGui.drawCard(g, leftPos + 10, topPos + 26, imageWidth - 20, 64, 0xFF4B3759);
        ObsidiumMachineGui.drawEnergyBar(g, leftPos + 16, topPos + 34, imageWidth - 32, 12,
                menu.energy(), AegisSanctuaryWardBlockEntity.MAX_ENERGY, 0xFF28BEB9);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        ObsidiumMachineGui.drawFittedText(g, font, title, titleLabelX, titleLabelY, imageWidth - 20, 0xFFF4C438);
        String rfText = ObsidiumMachineGui.formatRf(menu.energy()) + " / 500k RF";
        g.drawString(font, rfText, 16, 51, 0xFFF3E8FF, false);
        Component stateText = Component.translatable(!menu.isManualEnabled() ? "gui.obsidium.ward.state_paused"
                : menu.isActive() ? "gui.obsidium.ward.state_active" : "gui.obsidium.ward.state_idle");
        int stateCol = menu.isActive() ? 0xFF3CE070 : 0xFFE86040;
        ObsidiumMachineGui.drawFittedText(g, font, stateText, 16, 64, imageWidth - 32, stateCol);
        ObsidiumMachineGui.drawFittedText(g, font, Component.translatable("gui.obsidium.ward.area"), 16, 76, imageWidth - 32, 0xFF9C8FB4);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
