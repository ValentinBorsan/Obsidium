package net.valentin.obsidium.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.valentin.obsidium.block.GeothermalArboristBlockEntity;
import net.valentin.obsidium.block.GeothermalArboristMenu;

import java.util.List;

public class GeothermalArboristScreen extends AbstractContainerScreen<GeothermalArboristMenu> {

    private Button rangeButton;
    private Button modeButton;
    private Button mutationButton;

    public GeothermalArboristScreen(GeothermalArboristMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 190;
        this.inventoryLabelY = 98;
    }

    @Override
    protected void init() {
        super.init();
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        rangeButton = this.addRenderableWidget(Button.builder(
                Component.literal(getAreaString()),
                button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                    }
                })
                .bounds(x + 76, y + 17, 36, 16)
                .build());

        modeButton = this.addRenderableWidget(Button.builder(
                Component.literal(getModeString()),
                button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
                    }
                })
                .bounds(x + 76, y + 35, 36, 16)
                .build());

        mutationButton = this.addRenderableWidget(Button.builder(
                getMutationButtonText(),
                button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 2);
                    }
                })
                .bounds(x + 76, y + 54, 36, 16)
                .build());
    }

    private String getAreaString() {
        int r = this.menu.getRadius();
        int dim = r * 2 + 1;
        return dim + "x" + dim;
    }

    private String getModeString() {
        return switch (this.menu.getMode()) {
            case 1 -> "Crops";
            case 2 -> "Trees";
            case 3 -> "Leaves";
            default -> "All";
        };
    }

    private Component getMutationButtonText() {
        return Component.translatable(this.menu.getMutationSelection() == 0
                ? "gui.obsidium.arborist.mutation.electrum_short"
                : "gui.obsidium.arborist.mutation.tartarean_short");
    }

    private Component getMutationName() {
        return Component.translatable(this.menu.getMutationSelection() == 0
                ? "gui.obsidium.arborist.mutation.electrum"
                : "gui.obsidium.arborist.mutation.tartarean");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (rangeButton != null) {
            rangeButton.setMessage(Component.literal(getAreaString()));
        }
        if (modeButton != null) {
            modeButton.setMessage(Component.literal(getModeString()));
        }
        if (mutationButton != null) {
            mutationButton.setMessage(getMutationButtonText());
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, Component.literal("Geothermal Arborist"), this.titleLabelX, this.titleLabelY, 0xFFD166, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xCECAC5, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        VolcaniteGuiTheme.renderExteriorFrame(guiGraphics, x, y, this.imageWidth, this.imageHeight, true);

        // Dark obsidian panel background
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF141218);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF1C1922);
        guiGraphics.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF24202B);

        // Header bar
        guiGraphics.fill(x + 6, y + 5, x + imageWidth - 6, y + 16, 0xFF18151D);

        // 1. Lava / Steam Tank (Left)
        guiGraphics.fill(x + 10, y + 18, x + 24, y + 74, 0xFF120E15);
        int maxLava = GeothermalArboristBlockEntity.MAX_LAVA;
        int lavaHeight = (int) ((this.menu.getLavaAmount() / (float) maxLava) * 54);
        if (lavaHeight > 0) {
            guiGraphics.fillGradient(x + 11, y + 73 - lavaHeight, x + 23, y + 73, 0xFFFF7700, 0xFFDD2200);
        }

        // 2. Energy Bar (Right of tank)
        guiGraphics.fill(x + 25, y + 18, x + 33, y + 74, 0xFF0E1210);
        long maxEnergy = 200000;
        int energyHeight = (int) ((this.menu.getEnergy() / (float) maxEnergy) * 54);
        if (energyHeight > 0) {
            guiGraphics.fillGradient(x + 26, y + 73 - energyHeight, x + 32, y + 73, 0xFF00FF88, 0xFF00AA55);
        }

        // 3. Slot outlines
        // Seeds inputs (Slots 0, 1)
        drawSlotFrame(guiGraphics, x + 35, y + 16);
        drawSlotFrame(guiGraphics, x + 55, y + 16);

        // Lava Bucket input/output (Slots 2, 3)
        drawSlotFrame(guiGraphics, x + 35, y + 35);
        drawSlotFrame(guiGraphics, x + 55, y + 35);

        // Dedicated Fertilizer Slot (Slot 12)
        boolean bloomUnlocked = this.menu.hasBloomInfuser();
        if (bloomUnlocked) {
            // Catalyzed frame with vibrant green glow
            guiGraphics.fill(x + 35, y + 81, x + 53, y + 99, 0xFF00AA44);
            guiGraphics.fill(x + 36, y + 82, x + 52, y + 98, 0xFF1B2E22);
        } else {
            // Standard fertilizer slot frame
            drawSlotFrame(guiGraphics, x + 35, y + 81);
        }

        // Catalyst Upgrade (Slot 11 - Pyrogenic Bloom Infuser)
        boolean hasBloom = this.menu.hasBloomInfuser();
        int bloomBorder = hasBloom ? 0xFFFF8800 : 0xFF35254A;
        int bloomBg = hasBloom ? 0xFF2A1C14 : 0xFF221A2E;
        guiGraphics.fill(x + 55, y + 81, x + 73, y + 99, bloomBorder);
        guiGraphics.fill(x + 56, y + 82, x + 72, y + 98, bloomBg);

        // Auto-Hoe Upgrade (Slot 10 - Thermic Soil Tiller)
        boolean hasTiller = this.menu.hasTillerUpgrade();
        int tillerBorder = hasTiller ? 0xFF00B4D8 : 0xFF242C3A;
        int tillerBg = hasTiller ? 0xFF14222E : 0xFF1B202B;
        guiGraphics.fill(x + 84, y + 81, x + 102, y + 99, tillerBorder);
        guiGraphics.fill(x + 85, y + 82, x + 101, y + 98, tillerBg);

        guiGraphics.fill(x + 76, y + 73, x + 112, y + 80, 0xFF100E14);
        int mutationWidth = (int) (34.0F * this.menu.getMutationProgress() / 100.0F);
        if (mutationWidth > 0) {
            int startColor = this.menu.getMutationSelection() == 0 ? 0xFFFFD166 : 0xFFB000FF;
            int endColor = this.menu.getMutationSelection() == 0 ? 0xFFFF9F1C : 0xFF5400A8;
            guiGraphics.fillGradient(x + 77, y + 74, x + 77 + mutationWidth, y + 79, startColor, endColor);
        }

        // Outputs (Slots 4..9)
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                drawSlotFrame(guiGraphics, x + 115 + col * 18, y + 25 + row * 18);
            }
        }

        // Player Inventory slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotFrame(guiGraphics, x + 7 + col * 18, y + 107 + row * 18);
            }
        }

        // Hotbar slots
        for (int col = 0; col < 9; col++) {
            drawSlotFrame(guiGraphics, x + 7 + col * 18, y + 165);
        }
    }

    private void drawSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, 0xFF141217);
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF2A2633);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Lava tank tooltip
        if (mouseX >= x + 10 && mouseX <= x + 24 && mouseY >= y + 18 && mouseY <= y + 74) {
            renderWrappedTooltip(guiGraphics, List.of(
                    Component.literal("§6Geothermal Lava / Steam Tank"),
                    Component.literal("§e" + this.menu.getLavaAmount() + " §7/ " + GeothermalArboristBlockEntity.MAX_LAVA + " mB"),
                    Component.literal("§7Accepts Lava or Steam from Steam Generator")
            ), mouseX, mouseY);
        }

        // Energy bar tooltip
        if (mouseX >= x + 25 && mouseX <= x + 33 && mouseY >= y + 18 && mouseY <= y + 74) {
            renderWrappedTooltip(guiGraphics, List.of(
                    Component.literal("§aEnergy Storage"),
                    Component.literal("§2" + this.menu.getEnergy() + " §7/ 200,000 RF")
            ), mouseX, mouseY);
        }

        // Range button tooltip
        if (mouseX >= x + 76 && mouseX <= x + 112 && mouseY >= y + 17 && mouseY <= y + 33) {
            renderWrappedTooltip(guiGraphics, List.of(
                    Component.literal("§eWorking Area: §f" + getAreaString()),
                    Component.literal("§7Click to toggle: 3x3, 5x5, 7x7, 9x9")
            ), mouseX, mouseY);
        }

        // Mode button tooltip
        if (mouseX >= x + 76 && mouseX <= x + 112 && mouseY >= y + 35 && mouseY <= y + 51) {
            renderWrappedTooltip(guiGraphics, List.of(
                    Component.literal("§eOperation Mode: §f" + getModeString()),
                    Component.literal("§7Click to toggle: All, Crops, Trees, Leaves")
            ), mouseX, mouseY);
        }

        if (mouseX >= x + 76 && mouseX <= x + 112 && mouseY >= y + 54 && mouseY <= y + 70) {
            Component ingredients = Component.translatable(this.menu.getMutationSelection() == 0
                    ? "gui.obsidium.arborist.mutation.electrum_leaves"
                    : "gui.obsidium.arborist.mutation.tartarean_leaves");
            renderWrappedTooltip(guiGraphics, List.of(
                    Component.translatable("gui.obsidium.arborist.mutation.name", getMutationName()),
                    Component.translatable("gui.obsidium.arborist.mutation.leaves", ingredients),
                    Component.translatable("gui.obsidium.arborist.mutation.fruit_progress"),
                    Component.translatable("gui.obsidium.arborist.mutation.cost"),
                    Component.translatable("gui.obsidium.arborist.mutation.progress", this.menu.getMutationProgress())
            ), mouseX, mouseY);
        }

        // Auto-Hoe Upgrade Slot tooltip
        if (mouseX >= x + 84 && mouseX <= x + 102 && mouseY >= y + 81 && mouseY <= y + 99) {
            boolean active = this.menu.hasTillerUpgrade();
            renderWrappedTooltip(guiGraphics, List.of(
                    Component.literal("§bThermic Soil Tiller Bay " + (active ? "§a[ACTIVE]" : "§7[EMPTY]")),
                    Component.literal("§7Insert §bThermic Soil Tiller §7(or Hoe)"),
                    Component.literal("§8Automatically tills dirt/grass into farmland in the active radius.")
            ), mouseX, mouseY);
        }

        // Bloom Infuser Upgrade Slot tooltip
        if (mouseX >= x + 55 && mouseX <= x + 73 && mouseY >= y + 81 && mouseY <= y + 99) {
            boolean active = this.menu.hasBloomInfuser();
            renderWrappedTooltip(guiGraphics, List.of(
                    Component.literal("§6Bloom Infuser Catalyst Bay " + (active ? "§a[INSTALLED]" : "§7[EMPTY]")),
                    Component.literal("§7Insert §6Pyrogenic Bloom Infuser"),
                    Component.literal("§8Unlocks dedicated Fertilizer slot & enables super-charged crop blooming.")
            ), mouseX, mouseY);
        }

        // Fertilizer Slot tooltip
        if (mouseX >= x + 35 && mouseX <= x + 53 && mouseY >= y + 81 && mouseY <= y + 99) {
            boolean unlocked = this.menu.hasBloomInfuser();
            if (unlocked) {
                renderWrappedTooltip(guiGraphics, List.of(
                        Component.literal("§aDedicated Fertilizer Bay §2[UNLOCKED]"),
                        Component.literal("§7Accepts §fBone Meal§7, §cVolcanic Ash§7, and §dVolcanite Dust"),
                        Component.literal("§8Consumes 1 fertilizer to instantly boost crop growth by multiple stages.")
                ), mouseX, mouseY);
            } else {
                renderWrappedTooltip(guiGraphics, List.of(
                        Component.literal("§cDedicated Fertilizer Bay §4[LOCKED]"),
                        Component.literal("§eRequires: §6Pyrogenic Bloom Infuser §ein catalyst bay"),
                        Component.literal("§7Insert the upgrade in the slot next to this to unlock automated fertilization!")
                ), mouseX, mouseY);
            }
        }
    }

    private void renderWrappedTooltip(GuiGraphics graphics, List<Component> lines, int mouseX, int mouseY) {
        int maxWidth = Math.max(120, Math.min(220, width - 20));
        List<FormattedCharSequence> wrapped = new java.util.ArrayList<>();
        for (Component line : lines) {
            wrapped.addAll(this.font.split(line, maxWidth));
        }
        graphics.renderTooltip(this.font, wrapped, mouseX, mouseY);
    }
}

