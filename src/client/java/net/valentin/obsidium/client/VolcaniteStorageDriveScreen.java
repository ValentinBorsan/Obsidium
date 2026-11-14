package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.VolcaniteStorageDriveMenu;
import net.valentin.obsidium.item.VolcaniteLiquidStorageDiskItem;
import net.valentin.obsidium.item.VolcaniteStorageDiskItem;

public class VolcaniteStorageDriveScreen extends AbstractContainerScreen<VolcaniteStorageDriveMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/storage_disk_drive_gui.png");

    public VolcaniteStorageDriveScreen(VolcaniteStorageDriveMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // High-tech header
        guiGraphics.drawString(this.font, Component.literal("§6§lDISK DRIVE"), 8, 7, 0xFFAA00, false);

        // Disks in bay counter
        int diskCount = 0;
        for (int i = 0; i < 8; i++) {
            if (!this.menu.getSlot(i).getItem().isEmpty()) {
                diskCount++;
            }
        }
        guiGraphics.drawString(this.font, Component.literal("§7Disks: §f" + diskCount + "§8/§f8"), 114, 7, 0xAAAAAA, false);

        // Player Inventory label (cleanly spaced without overlapping drive bay)
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, 72, 0xCECAC5, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Tooltip for empty drive bays
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        for (int i = 0; i < 8; i++) {
            int r = i / 4;
            int c = i % 4;
            int sx = x + 49 + c * 20;
            int sy = y + 24 + r * 22;
            if (mouseX >= sx && mouseX <= sx + 18 && mouseY >= sy && mouseY <= sy + 18) {
                if (this.menu.getSlot(i).getItem().isEmpty()) {
                    guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                            Component.literal("§eDrive Bay #" + (i + 1)),
                            Component.literal("§8[Empty]"),
                            Component.literal("§7Insert an Item or Fluid Storage Disk.")
                    ), mouseX, mouseY);
                }
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        VolcaniteGuiTheme.renderExteriorFrame(guiGraphics, x, y, this.imageWidth, this.imageHeight, true);
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        // Render dynamic LED status lights on each server blade sled
        for (int i = 0; i < 8; i++) {
            int r = i / 4;
            int c = i % 4;
            int sx = x + 49 + c * 20;
            int sy = y + 24 + r * 22;

            ItemStack disk = this.menu.getSlot(i).getItem();
            int ledColor = 0xFF2A2830; // dim grey for empty slot

            if (!disk.isEmpty()) {
                if (disk.getItem() instanceof VolcaniteStorageDiskItem itemDisk) {
                    int count = VolcaniteStorageDiskItem.getTotalCount(disk);
                    int cap = itemDisk.getCapacity();
                    float pct = (float) count / Math.max(1, cap);
                    ledColor = (pct >= 1.0F) ? 0xFFFF3333 : (pct >= 0.75F) ? 0xFFFFAA00 : 0xFF00FFCC;
                } else if (disk.getItem() instanceof VolcaniteLiquidStorageDiskItem liquidDisk) {
                    int amt = VolcaniteLiquidStorageDiskItem.getStoredAmount(disk);
                    int cap = liquidDisk.getCapacity();
                    float pct = (float) amt / Math.max(1, cap);
                    ledColor = (pct >= 1.0F) ? 0xFFFF3333 : (pct >= 0.75F) ? 0xFFFFAA00 : 0xFF33AAFF;
                }
            }

            // Draw glowing LED bar
            guiGraphics.fill(sx + 6, sy - 3, sx + 12, sy - 1, ledColor);
            if (!disk.isEmpty()) {
                // Subtle bright center specular
                guiGraphics.fill(sx + 8, sy - 3, sx + 10, sy - 2, 0xFFFFFFFF);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode))) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
