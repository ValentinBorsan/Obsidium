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
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.VolcaniteItemBarrelMenu;

import java.util.List;

public class VolcaniteItemBarrelScreen extends AbstractContainerScreen<VolcaniteItemBarrelMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/geothermal_generator_gui.png");

    public VolcaniteItemBarrelScreen(VolcaniteItemBarrelMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 180;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 88;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFAA33, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xCECAC5, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        // Tooltip: Lock Button (x = 24, y = 62, w = 54, h = 18)
        if (mouseX >= i + 24 && mouseX <= i + 78 && mouseY >= j + 62 && mouseY <= j + 80) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal(this.menu.isLocked() ? "§a🔒 Filter Locked" : "§c🔓 Filter Unlocked"),
                    Component.literal("§7When locked, the barrel keeps this item type"),
                    Component.literal("§7even when count reaches 0 items."),
                    Component.literal("§eClick to Toggle")
            ), mouseX, mouseY);
        }

        // Tooltip: Capacity Bar (x = 24, y = 48, w = 114, h = 10)
        if (mouseX >= i + 24 && mouseX <= i + 138 && mouseY >= j + 48 && mouseY <= j + 58) {
            long stored = this.menu.getStoredAmount();
            long max = this.menu.getMaxCapacity();
            int pct = max > 0 ? (int) (100.0 * stored / max) : 0;
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6✦ Barrel Capacity"),
                    Component.literal("§f" + String.format("%,d", stored) + " / " + String.format("%,d", max) + " items §7(" + pct + "%)"),
                    Component.literal(this.menu.hasVoidUpgrade() ? "§c✔ Void Overflow Active" : "§8Install Void Upgrade to discard overflow")
            ), mouseX, mouseY);
        }

        // Tooltip: Upgrade Slots (x = 149, y = 17 + i*18)
        for (int s = 0; s < 4; s++) {
            int slotY = j + 17 + s * 18;
            if (this.menu.getSlot(s).getItem().isEmpty() && mouseX >= i + 149 && mouseX <= i + 167 && mouseY >= slotY && mouseY <= slotY + 18) {
                guiGraphics.renderComponentTooltip(this.font, List.of(
                        Component.literal("§eBarrel Upgrade Slot " + (s + 1)),
                        Component.literal("§7Accepts: §6Basic, Advanced, Supreme Capacity Upgrades"),
                        Component.literal("§7or §cBarrel Void Upgrade"),
                        Component.literal("§8Multiplies barrel storage capacity")
                ), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        VolcaniteGuiTheme.renderExteriorFrame(guiGraphics, i, j, this.imageWidth, this.imageHeight, false);
        guiGraphics.blit(GUI_TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        // Clear machine upper interior with dark volcanic background
        guiGraphics.fill(i + 7, j + 16, i + 169, j + 86, 0xFF140E1B);
        border(guiGraphics, i + 7, j + 16, 162, 70, 0xFF2A1C36);

        // Central Showcase Panel (i + 14, j + 18, width 128, height 66)
        guiGraphics.fill(i + 14, j + 18, i + 142, j + 84, 0xFF0D0813);
        border(guiGraphics, i + 14, j + 18, 128, 66, 0xFF3D254C);

        ItemStack sample = this.menu.getStoredItemSample();
        long stored = this.menu.getStoredAmount();
        long max = this.menu.getMaxCapacity();

        if (!sample.isEmpty()) {
            // Render sample item
            guiGraphics.renderFakeItem(sample, i + 24, j + 24);

            // Item Name
            String name = sample.getHoverName().getString();
            if (name.length() > 16) name = name.substring(0, 14) + "...";
            guiGraphics.drawString(this.font, name, i + 46, j + 24, 0xFFFFDD44, false);

            // Count text
            String countText = String.format("%,d", stored) + " / " + String.format("%,d", max);
            guiGraphics.drawString(this.font, countText, i + 46, j + 35, 0xFF00E5FF, false);
        } else {
            guiGraphics.drawString(this.font, "§7[Empty Barrel]", i + 24, j + 26, 0xFF888888, false);
            guiGraphics.drawString(this.font, "§8Max: " + String.format("%,d", max) + " items", i + 24, j + 36, 0xFF666666, false);
        }

        // Fill Progress Bar (i + 24, j + 48, w = 112, h = 8)
        guiGraphics.fill(i + 24, j + 48, i + 136, j + 56, 0xFF08050C);
        border(guiGraphics, i + 24, j + 48, 112, 8, 0xFF4A255A);
        int fillW = max > 0 ? (int) (110.0 * Math.min(stored, max) / max) : 0;
        if (fillW > 0) {
            guiGraphics.fill(i + 25, j + 49, i + 25 + fillW, j + 55, 0xFFFF7700);
            guiGraphics.fill(i + 25, j + 49, i + 25 + fillW, j + 51, 0xFFFFDD44);
        }

        // Lock Toggle Button (i + 24, j + 62, w = 54, h = 18)
        boolean lockHovered = mouseX >= i + 24 && mouseX <= i + 78 && mouseY >= j + 62 && mouseY <= j + 80;
        int lockBg = this.menu.isLocked() ? 0xFF352B1C : (lockHovered ? 0xFF2A1C36 : 0xFF181022);
        int lockBorder = this.menu.isLocked() ? 0xFFFFCC00 : (lockHovered ? 0xFF888888 : 0xFF444444);
        guiGraphics.fill(i + 24, j + 62, i + 78, j + 80, lockBg);
        border(guiGraphics, i + 24, j + 62, 54, 18, lockBorder);
        String lockBtnText = this.menu.isLocked() ? "🔒 LOCKED" : "🔓 UNLOCK";
        int lockTextCol = this.menu.isLocked() ? 0xFFFFCC00 : 0xFFAAAAAA;
        guiGraphics.drawString(this.font, lockBtnText, i + 27, j + 67, lockTextCol, false);

        // Void Status Indicator (i + 82, j + 62, w = 54, h = 18)
        if (this.menu.hasVoidUpgrade()) {
            guiGraphics.fill(i + 82, j + 62, i + 136, j + 80, 0xFF3B1522);
            border(guiGraphics, i + 82, j + 62, 54, 18, 0xFFFF3366);
            guiGraphics.drawString(this.font, "☠ VOID ON", i + 86, j + 67, 0xFFFF6688, false);
        }

        // 4 Upgrade Slots (Vertical column on the right: x = 149, y = 17, 35, 53, 71)
        for (int s = 0; s < 4; s++) {
            int slotX = i + 149;
            int slotY = j + 17 + s * 18;
            guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF0B0711);
            border(guiGraphics, slotX, slotY, 18, 18, 0xFF00AAFF);

            if (this.menu.getSlot(s).getItem().isEmpty()) {
                guiGraphics.renderFakeItem(new ItemStack(ObsidiumMod.VOLCANITE_BASIC_BARREL_UPGRADE), slotX + 1, slotY + 1);
                guiGraphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xAA140E1B);
            }
        }
    }

    private void border(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        // Lock button click
        if (mouseX >= i + 24 && mouseX <= i + 78 && mouseY >= j + 62 && mouseY <= j + 80) {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}

