package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.VolcaniteStorageTerminalMenu;

import java.util.ArrayList;
import java.util.List;

public class VolcaniteStorageTerminalScreen extends AbstractContainerScreen<VolcaniteStorageTerminalMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/gui/storage_terminal_gui.png");
    private EditBox searchBox;
    private Button itemsTabButton;
    private Button fluidsTabButton;
    private Button toolsTabButton;
    private Button sortButton;

    // Scrollbar geometry (relative to GUI origin), right of the 9x6 grid
    private static final int SCROLL_X = 175;
    private static final int SCROLL_Y = 20;
    private static final int SCROLL_W = 10;
    private static final int SCROLL_H = 108;
    private static final int THUMB_H = 15;
    private int clientScrollRow = 0;
    private long lastScrollInputMs = 0;
    private boolean draggingScrollbar = false;

    public VolcaniteStorageTerminalScreen(VolcaniteStorageTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 194;
        this.imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        // View Mode Tab Buttons (Compact Icons: 📦, 💧, ⚔, ⚒)
        this.itemsTabButton = this.addRenderableWidget(Button.builder(Component.literal("📦"), b -> switchTab(100))
            .bounds(i + 8, j + 4, 16, 13)
            .build());

        this.fluidsTabButton = this.addRenderableWidget(Button.builder(Component.literal("💧"), b -> switchTab(101))
            .bounds(i + 26, j + 4, 16, 13)
            .build());

        this.toolsTabButton = this.addRenderableWidget(Button.builder(Component.literal("⚔"), b -> switchTab(104))
            .bounds(i + 44, j + 4, 16, 13)
            .build());

        // Craft Interface Tab Button
        this.addRenderableWidget(Button.builder(Component.literal("⚒"), b -> switchTab(103))
            .bounds(i + 62, j + 4, 16, 13)
            .build());

        // Sort Mode Button
        this.sortButton = this.addRenderableWidget(Button.builder(getSortButtonComponent(), b -> switchTab(102))
            .bounds(i + 80, j + 4, 18, 13)
            .build());

        // Search Box (Expanded to 70px wide for airy comfortable search)
        this.searchBox = new EditBox(this.font, i + 103, j + 7, 68, 9, Component.literal("Search"));
        this.searchBox.setMaxLength(100);
        this.searchBox.setBordered(false);
        this.searchBox.setVisible(true);
        this.searchBox.setTextColor(0xFFFFFF);
        this.searchBox.setResponder(text -> {
            // Filtering happens on the server (it owns the network inventory)
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                    new net.valentin.obsidium.network.TerminalSearchPayload(text));
            this.clientScrollRow = 0;
            this.lastScrollInputMs = System.currentTimeMillis();
        });
        this.addWidget(this.searchBox);

        // Close Button (✕)
        this.addRenderableWidget(Button.builder(Component.literal("✕"), b -> this.onClose())
            .bounds(i + 176, j + 4, 13, 13)
            .build());
    }

    private Component getSortButtonComponent() {
        int sort = this.menu.getSortMode();
        return switch (sort) {
            case 0 -> Component.literal("§6↓#"); // Quantity Descending
            case 1 -> Component.literal("§e↑#"); // Quantity Ascending
            case 2 -> Component.literal("§bAZ"); // Name A-Z
            case 3 -> Component.literal("§dZA"); // Name Z-A
            default -> Component.literal("§6↓#");
        };
    }

    private void switchTab(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private void requestScrollRow(int row) {
        int clamped = Math.max(0, Math.min(row, this.menu.getMaxScrollRow()));
        this.lastScrollInputMs = System.currentTimeMillis();
        if (clamped != this.clientScrollRow) {
            this.clientScrollRow = clamped;
            switchTab(VolcaniteStorageTerminalMenu.SCROLL_BUTTON_OFFSET + clamped);
        }
    }

    private int currentScrollRow() {
        // Use the locally predicted row right after user input, otherwise trust the server
        if (draggingScrollbar || System.currentTimeMillis() - lastScrollInputMs < 600) {
            return clientScrollRow;
        }
        clientScrollRow = this.menu.getScrollRow();
        return clientScrollRow;
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        int i = this.leftPos, j = this.topPos;
        return mouseX >= i + SCROLL_X && mouseX < i + SCROLL_X + SCROLL_W
                && mouseY >= j + SCROLL_Y && mouseY < j + SCROLL_Y + SCROLL_H;
    }

    private void scrollToMouse(double mouseY) {
        int max = this.menu.getMaxScrollRow();
        if (max <= 0) return;
        double rel = (mouseY - (this.topPos + SCROLL_Y) - THUMB_H / 2.0) / (double) (SCROLL_H - THUMB_H);
        rel = Math.max(0.0, Math.min(1.0, rel));
        requestScrollRow((int) Math.round(rel * max));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && this.menu.getMaxScrollRow() > 0) {
            int step = hasShiftDown() ? 6 : 1;
            requestScrollRow(currentScrollRow() - (int) Math.signum(scrollY) * step);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int i = this.leftPos;
        int j = this.topPos;

        // Expanded clickable search bar region (covers i + 100..174, j + 3..18)
        if (mouseX >= i + 100 && mouseX <= i + 174 && mouseY >= j + 3 && mouseY <= j + 18) {
            this.searchBox.setFocused(true);
            this.setFocused(this.searchBox);
            if (button == 1) {
                // Right-click instantly clears the search query
                this.searchBox.setValue("");
            }
            return true;
        } else {
            this.searchBox.setFocused(false);
        }

        if (button == 0 && isOverScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            scrollToMouse(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar) {
            scrollToMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingScrollbar && button == 0) {
            draggingScrollbar = false;
            lastScrollInputMs = System.currentTimeMillis();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void renderScrollbar(GuiGraphics g, int i, int j) {
        int x = i + SCROLL_X, y = j + SCROLL_Y;
        g.fill(x, y, x + SCROLL_W, y + SCROLL_H, 0xFF120A1A);
        int max = this.menu.getMaxScrollRow();
        int thumbY = y;
        if (max > 0) {
            thumbY = y + (int) ((SCROLL_H - THUMB_H) * (currentScrollRow() / (double) max));
        }
        int color = max > 0 ? 0xFFFF8A1E : 0xFF555555;
        int highlight = max > 0 ? 0xFFFFC266 : 0xFF777777;
        g.fill(x + 1, thumbY, x + SCROLL_W - 1, thumbY + THUMB_H, color);
        g.fill(x + 1, thumbY, x + SCROLL_W - 1, thumbY + 1, highlight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int mode = this.menu.getViewMode();
        this.itemsTabButton.active = (mode != 0);
        this.fluidsTabButton.active = (mode != 1);
        if (this.toolsTabButton != null) {
            this.toolsTabButton.active = (mode != 2);
        }
        if (this.sortButton != null) {
            this.sortButton.setMessage(getSortButtonComponent());
        }

        // Inventory label at Y = 128
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, 128, 0xCECAC5, false);

        // Header status: cleanly formatted stored count + static energy icon badge (prevents number jitter)
        int total = this.menu.getTotalStored();
        int energy = this.menu.getEnergy();
        String modeName = (mode == 2) ? "§c[Tools]§7" : ((mode == 1) ? "§b[Fluids]§7" : "§6[Items]§7");
        String countStr = String.format("%s §f%,d", modeName, Math.max(0, total));
        int badgeX = this.imageWidth - 24;
        int countW = this.font.width(countStr);
        int countX = badgeX - countW - 6;
        guiGraphics.drawString(this.font, countStr, countX, 128, 0x94A3B8, false);

        // Compact Energy Badge Icon at Y = 125, X = imageWidth - 24
        int badgeY = 125;
        int badgeBg = (energy > 0) ? 0xFF0D251C : 0xFF281114;
        int badgeBorder = (energy > 0) ? 0xFF10B981 : 0xFFEF4444;
        guiGraphics.fill(badgeX, badgeY, badgeX + 16, badgeY + 12, badgeBg);
        guiGraphics.fill(badgeX, badgeY, badgeX + 16, badgeY + 1, badgeBorder);
        guiGraphics.fill(badgeX, badgeY + 11, badgeX + 16, badgeY + 12, badgeBorder);
        guiGraphics.fill(badgeX, badgeY, badgeX + 1, badgeY + 12, badgeBorder);
        guiGraphics.fill(badgeX + 15, badgeY, badgeX + 16, badgeY + 12, badgeBorder);
        guiGraphics.drawString(this.font, (energy > 0) ? "§a⚡" : "§c⚡", badgeX + 4, badgeY + 2, 0xFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.searchBox.render(guiGraphics, mouseX, mouseY, partialTick);

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        renderScrollbar(guiGraphics, i, j);
        if (this.searchBox.getValue().isEmpty() && !this.searchBox.isFocused()) {
            guiGraphics.drawString(this.font, "§8Search...", i + 104, j + 7, 0x666666, false);
        }

        // Render actual real quantities over the 54 storage slots in compact normal white font
        for (int k = 0; k < 54; k++) {
            Slot slot = this.menu.slots.get(k);
            if (slot != null && slot.hasItem()) {
                ItemStack stack = slot.getItem();
                CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                CompoundTag tag = data.copyTag();

                if (tag.contains("RealCount")) {
                    int count = tag.getInt("RealCount");
                    boolean isFluid = tag.getBoolean("IsFluid");

                    if (count <= 1 && !isFluid) continue;

                    String text = formatCount(count, isFluid);
                    float scale = (text.length() >= 4) ? 0.62f : 0.72f;

                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(0, 0, 250);
                    guiGraphics.pose().scale(scale, scale, 1.0f);

                    float targetRight = i + slot.x + 17;
                    float targetBottom = j + slot.y + 16;
                    float rx = (targetRight / scale) - this.font.width(text);
                    float ry = (targetBottom / scale) - this.font.lineHeight;

                    // Clean normal Minecraft white color with shadow
                    guiGraphics.drawString(this.font, text, (int) rx, (int) ry, 0xFFFFFFFF, true);
                    guiGraphics.pose().popPose();
                }
            }
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Tooltip for Items tab button (i + 8..24)
        if (mouseX >= i + 8 && mouseX <= i + 24 && mouseY >= j + 4 && mouseY <= j + 17) {
            guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("§6📦 Storage Items"),
                    Component.literal("§7View and search all stored items & materials.")
            ), mouseX, mouseY);
        }
        // Tooltip for Fluids tab button (i + 26..42)
        else if (mouseX >= i + 26 && mouseX <= i + 42 && mouseY >= j + 4 && mouseY <= j + 17) {
            guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("§b💧 Fluid & Liquid Tanks"),
                    Component.literal("§7View connected fluid volumes & essence pools.")
            ), mouseX, mouseY);
        }
        // Tooltip for Tools tab button (i + 44..60)
        else if (mouseX >= i + 44 && mouseX <= i + 60 && mouseY >= j + 4 && mouseY <= j + 17) {
            guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("§c⚔ Equipment & Armory"),
                    Component.literal("§7Displays tools, weapons & armor from connected"),
                    Component.literal("§7Hephaestean Armories & Tool Chests with durability bars!")
            ), mouseX, mouseY);
        }
        // Tooltip for Crafter button (i + 62..78)
        else if (mouseX >= i + 62 && mouseX <= i + 78 && mouseY >= j + 4 && mouseY <= j + 17) {
            guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("§e⚒ Auto-Crafter Interface"),
                    Component.literal("§7Quick-open crafting blueprints and recipe grids.")
            ), mouseX, mouseY);
        }
        // Tooltip for Sort Mode button (i + 80..98)
        else if (mouseX >= i + 80 && mouseX <= i + 98 && mouseY >= j + 4 && mouseY <= j + 17) {
            int sort = this.menu.getSortMode();
            int mode = this.menu.getViewMode();
            String title;
            if (mode == 2) {
                title = switch (sort) {
                    case 0 -> "§6Sort: Durability (Fullest First)";
                    case 1 -> "§eSort: Durability (Damaged First)";
                    case 2 -> "§bSort: Name (A to Z)";
                    case 3 -> "§dSort: Name (Z to A)";
                    default -> "Sort Mode";
                };
            } else {
                title = switch (sort) {
                    case 0 -> "§6Sort: Quantity (Highest First)";
                    case 1 -> "§eSort: Quantity (Lowest First)";
                    case 2 -> "§bSort: Name (A to Z)";
                    case 3 -> "§dSort: Name (Z to A)";
                    default -> "Sort Mode";
                };
            }
            guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal(title),
                    Component.literal("§8Click to cycle sort modes.")
            ), mouseX, mouseY);
        }
    }

    private String formatCount(int count, boolean isFluid) {
        if (count < 0 || count == Integer.MAX_VALUE) return "∞";
        if (isFluid) {
            if (count >= 1000000) return String.format("%.1fM", count / 1000000.0);
            if (count >= 1000) return (count / 1000) + "B";
            return count + "m";
        } else {
            if (count < 1000) return String.valueOf(count);
            if (count < 10000) return String.format("%.1fk", count / 1000.0);
            if (count < 1000000) return (count / 1000) + "k";
            return String.format("%.1fM", count / 1000000.0);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem() && this.hoveredSlot.index < 54) {
            ItemStack stack = this.hoveredSlot.getItem();
            CustomData cd = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = cd.copyTag();

            if (tag.contains("RealCount")) {
                int count = tag.getInt("RealCount");
                boolean isFluid = tag.getBoolean("IsFluid");
                List<Component> tooltip = new ArrayList<>();

                if (isFluid) {
                    String fluidId = tag.getString("FluidId");
                    Fluid fluid = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(fluidId));
                    String fluidName = (fluid != null) ? fluid.defaultFluidState().createLegacyBlock().getBlock().getName().getString() : "Fluid";
                    tooltip.add(Component.literal("§b" + fluidName));
                    if (count < 0 || count == Integer.MAX_VALUE) {
                        tooltip.add(Component.literal("§7Stored: §eInfinite (Creative Tank)"));
                    } else {
                        tooltip.add(Component.literal("§7Stored: §e" + String.format("%,d", count) + " mB (" + (count / 1000) + " Buckets)"));
                    }
                    tooltip.add(Component.literal("§8[Left-Click with Bucket/Bottle to Extract]"));
                    tooltip.add(Component.literal("§8[Left-Click with Filled Bucket/Bottle to Deposit]"));
                } else {
                    tooltip.add(Component.literal("§f" + stack.getHoverName().getString()));
                    tooltip.add(Component.literal("§7Network Stored: §a" + String.format("%,d", count)));
                    tooltip.add(Component.literal("§8[Left-Click: Take Stack | Right-Click: Take 1]"));
                    tooltip.add(Component.literal("§8[Shift-Click: Move to Inventory]"));
                }

                guiGraphics.renderComponentTooltip(this.font, tooltip, x, y);
                return;
            }
        }
        // Energy Badge Tooltip
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        int badgeX = i + this.imageWidth - 24;
        int badgeY = j + 125;
        if (x >= badgeX && x <= badgeX + 16 && y >= badgeY && y <= badgeY + 12) {
            int energy = this.menu.getEnergy();
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§6⚡ Volcanite Power Grid"),
                    Component.literal(String.format("§7Stored Energy: §e%,d RF", energy)),
                    Component.literal(energy > 0 ? "§a● Grid Status: ONLINE" : "§c○ Grid Status: NO POWER")
            ), x, y);
            return;
        }

        super.renderTooltip(guiGraphics, x, y);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(GUI_TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        // Polished search bar frame (recessed dark plate with responsive border)
        int sbX = i + 100;
        int sbY = j + 4;
        int sbW = 73;
        int sbH = 13;
        guiGraphics.fill(sbX, sbY, sbX + sbW, sbY + sbH, 0xFF0D0A14);
        int borderCol = (this.searchBox != null && this.searchBox.isFocused()) ? 0xFF38BDF8 : 0xFF2A2038;
        guiGraphics.renderOutline(sbX, sbY, sbW, sbH, borderCol);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBox.isFocused() && this.searchBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox.isFocused()) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                this.searchBox.setFocused(false);
                return true;
            }
            if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            // Consume all keystrokes while search box is focused so 'E' (inventory) or REI shortcuts don't intercept!
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode))) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
