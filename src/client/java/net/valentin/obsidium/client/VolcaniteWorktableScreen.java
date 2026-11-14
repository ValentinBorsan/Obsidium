package net.valentin.obsidium.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.block.VolcaniteWorktableMenu;
import net.minecraft.resources.ResourceLocation;

public class VolcaniteWorktableScreen extends AbstractContainerScreen<VolcaniteWorktableMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(net.valentin.obsidium.ObsidiumMod.MOD_ID, "textures/gui/volcanite_worktable.png");
    private static final ResourceLocation CHEST_ADDON = ResourceLocation.fromNamespaceAndPath(net.valentin.obsidium.ObsidiumMod.MOD_ID, "textures/gui/chest_addon.png");

    private Button autoOutputButton;
    private Button antiMixButton;
    private Button copyRecipeButton;

    public VolcaniteWorktableScreen(VolcaniteWorktableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        this.copyRecipeButton = this.addRenderableWidget(Button.builder(Component.literal("Copy"), b -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 2);
            }
        }).bounds(i + 122, j + 16, 36, 14).build());

        this.autoOutputButton = this.addRenderableWidget(Button.builder(Component.literal("Auto"), b -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            }
        }).bounds(i + 104, j + 56, 32, 14).build());

        this.antiMixButton = this.addRenderableWidget(Button.builder(Component.literal("Mix"), b -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
            }
        }).bounds(i + 138, j + 56, 30, 14).build());

        updateButtonText();
    }

    private void updateButtonText() {
        if (this.copyRecipeButton != null) {
            boolean hasBp = this.menu.getSlot(11).hasItem();
            this.copyRecipeButton.setMessage(Component.literal(hasBp ? "§eCopy" : "§7Copy"));
        }
        if (this.autoOutputButton != null) {
            boolean enabled = this.menu.autoOutput.get() == 1;
            this.autoOutputButton.setMessage(Component.literal(enabled ? "§aAuto" : "§7Auto"));
        }
        if (this.antiMixButton != null) {
            boolean enabled = this.menu.antiMix.get() == 1;
            this.antiMixButton.setMessage(Component.literal(enabled ? "§bMix" : "§7Mix"));
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        VolcaniteGuiTheme.renderExteriorFrame(guiGraphics, i, j, this.imageWidth, this.imageHeight, false);
        guiGraphics.blit(TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight);

        // Blueprint Slot (Slot 11) background frame at (i + 8, j + 16)
        int bpX = i + 8;
        int bpY = j + 16;
        guiGraphics.fill(bpX, bpY, bpX + 18, bpY + 18, 0xFF120C1C);
        guiGraphics.fill(bpX, bpY, bpX + 18, bpY + 1, 0xFF00B8E6);
        guiGraphics.fill(bpX, bpY + 17, bpX + 18, bpY + 18, 0xFF007799);
        guiGraphics.fill(bpX, bpY, bpX + 1, bpY + 18, 0xFF00B8E6);
        guiGraphics.fill(bpX + 17, bpY, bpX + 18, bpY + 18, 0xFF007799);

        // Ghost schematic icon inside empty blueprint slot
        if (!this.menu.getSlot(11).hasItem()) {
            guiGraphics.drawString(this.font, "BP", bpX + 4, bpY + 5, 0x4400B8E6, false);
        }
        
        if (this.menu.hasChest.get() == 1) {
            guiGraphics.blit(CHEST_ADDON, i + 176, j + 13, 0.0F, 0.0F, 62, 62, 62, 62);
        }
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        updateButtonText();
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFD166, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xCECAC5, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        // Tooltip over Copy Blueprint button
        if (mouseX >= i + 122 && mouseX <= i + 158 && mouseY >= j + 16 && mouseY <= j + 30) {
            guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("§6Copy Recipe to Blueprint"),
                    Component.literal("§7Saves the current 3x3 crafting grid"),
                    Component.literal("§7into the Blueprint in slot 11."),
                    Component.literal("§eClick to encode.")
            ), mouseX, mouseY);
        }
        // Tooltip over Auto-Output button
        else if (mouseX >= i + 104 && mouseX <= i + 136 && mouseY >= j + 56 && mouseY <= j + 70) {
            boolean enabled = this.menu.autoOutput.get() == 1;
            guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("§6Auto-Output: " + (enabled ? "§aENABLED" : "§cDISABLED")),
                    Component.literal("§7Automatically pushes crafted items into"),
                    Component.literal("§7adjacent chests, barrels, or machines."),
                    Component.literal("§eClick to toggle.")
            ), mouseX, mouseY);
        }
        // Tooltip over Anti-Mix button
        else if (mouseX >= i + 138 && mouseX <= i + 168 && mouseY >= j + 56 && mouseY <= j + 70) {
            boolean enabled = this.menu.antiMix.get() == 1;
            guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("§6Anti-Mix Batching: " + (enabled ? "§aENABLED" : "§cDISABLED")),
                    Component.literal("§7Blocks different items from entering the 3x3"),
                    Component.literal("§7grid until the current material finishes."),
                    Component.literal("§bAuto-balances identical items & requires 9+ items!"),
                    Component.literal("§eClick to toggle.")
            ), mouseX, mouseY);
        }
        // Tooltip over empty Blueprint slot
        else if (!this.menu.getSlot(11).hasItem() && mouseX >= i + 8 && mouseX <= i + 26 && mouseY >= j + 16 && mouseY <= j + 34) {
            guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("§bBlueprint Slot"),
                    Component.literal("§7Insert a Volcanite Blueprint here."),
                    Component.literal("§7Automates custom patterns or 3x3 recipes"),
                    Component.literal("§7and pulls ingredients from adjacent chest.")
            ), mouseX, mouseY);
        }
    }
}
