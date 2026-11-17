package net.valentin.obsidium.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.MagmaForgeCoreBlockEntity;
import net.valentin.obsidium.block.MagmaForgeRecipe;

import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class MagmaForgeManualScreen extends Screen {
    private static final int BOOK_WIDTH = 356;
    private static final int BOOK_HEIGHT = 220;
    private static final int SIDEBAR_W = 98;

    private static final String[] CHAPTER_TITLES = {
        "Power",
        "Magma Forge",
        "Energy Core",
        "Storage",
        "Pipes",
        "Caldera",
        "Mythology",
        "Collider"
    };

    private static final String[][] SUB_TABS = {
        {"Generators", "Multiblocks", "Chemistry"},
        {"3×3 Layout", "Tempering", "Recipes"},
        {"3×3 Layout", "Scaling", "HV 3×3×3", "HV Safety"},
        {"Network", "Item Barrels", "Fluid Barrels", "Core", "Build", "Crafter"},
        {"Items", "Fluids", "Conduits", "Upgrades", "Facades", "Poseidon"},
        {"Caldera Biome", "Volcanite Ore", "Living Trees"},
        {"Ritual", "Relics", "Solar", "Distillery", "Pylons", "Thanatos"},
        {"5×5 Ring", "Cryo & Fuel", "Collisions", "Teleport"}
    };

    private int currentChapter = 0;
    private int currentSubTab = 0;
    private int powerMultiblockPage = 0;
    private int recipePage = 0;
    private int colliderCollisionPage = 0;
    private static final int RECIPES_PER_PAGE = 4;

    private ItemStack hoveredStack = ItemStack.EMPTY;

    public MagmaForgeManualScreen() {
        this(0, 0);
    }

    public MagmaForgeManualScreen(int initialChapter) {
        this(initialChapter, 0);
    }

    public MagmaForgeManualScreen(int initialChapter, int initialSubTab) {
        super(Component.literal("Obsidium Manual"));
        this.currentChapter = Math.max(0, Math.min(CHAPTER_TITLES.length - 1, initialChapter));
        String[] tabs = SUB_TABS[this.currentChapter];
        this.currentSubTab = Math.max(0, Math.min(tabs.length - 1, initialSubTab));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Clean dark translucent tint without Minecraft 1.21's blur shader
        this.renderTransparentBackground(g);
    }

    private void drawBadge(GuiGraphics g, int rightX, int y, String text, int bgColor, int borderColor, int textColor) {
        int w = this.font.width(text) + 10;
        int bx = rightX - w;
        g.fill(bx, y, rightX, y + 13, bgColor);
        drawBorder(g, bx, y, w, 13, borderColor);
        g.drawString(this.font, text, bx + 5, y + 3, textColor, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        this.hoveredStack = ItemStack.EMPTY;

        int x = (this.width - BOOK_WIDTH) / 2;
        int y = (this.height - BOOK_HEIGHT) / 2;

        // 1. Outer Obsidian-Gold Frame
        g.fill(x, y, x + BOOK_WIDTH, y + BOOK_HEIGHT, 0xFF100B16);
        g.fill(x + 2, y + 2, x + BOOK_WIDTH - 2, y + BOOK_HEIGHT - 2, 0xFF171020);
        drawBorder(g, x, y, BOOK_WIDTH, BOOK_HEIGHT, 0xFFFF6600);
        drawBorder(g, x + 1, y + 1, BOOK_WIDTH - 2, BOOK_HEIGHT - 2, 0xFF552200);

        // 2. Top Title Banner
        g.fill(x + 8, y + 6, x + BOOK_WIDTH - 8, y + 22, 0xFF22152C);
        drawBorder(g, x + 8, y + 6, BOOK_WIDTH - 16, 16, 0xFF773300);
        String mainTitle = "📖 OBSIDIUM MANUAL";
        g.drawString(this.font, mainTitle, x + (BOOK_WIDTH - this.font.width(mainTitle)) / 2, y + 10, 0xFFFFCC44, false);

        // 3. Left Chapter Sidebar
        int sbX = x + 8;
        int sbY = y + 26;
        int sbH = BOOK_HEIGHT - 34;
        g.fill(sbX, sbY, sbX + SIDEBAR_W, sbY + sbH, 0xFF130D1B);
        drawBorder(g, sbX, sbY, SIDEBAR_W, sbH, 0xFF4A2511);

        g.drawString(this.font, "CHAPTERS", sbX + 24, sbY + 6, 0xFFFF9933, false);
        g.fill(sbX + 8, sbY + 17, sbX + SIDEBAR_W - 8, sbY + 18, 0xFF552800);

        ItemStack[] chapterIcons = {
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_GENERATOR_BLOCK),
            new ItemStack(ObsidiumMod.MAGMA_FORGE_CORE_BLOCK),
            new ItemStack(ObsidiumMod.VOLCANITE_ENERGY_CORE_BLOCK),
            new ItemStack(ObsidiumMod.VOLCANITE_STORAGE_CONTROLLER_BLOCK),
            new ItemStack(ObsidiumMod.HERMES_ITEM_PIPE_BLOCK),
            new ItemStack(ObsidiumMod.VOLCANITE_ORE),
            new ItemStack(ObsidiumMod.THANATOS_EXECUTIONER_BLOCK),
            new ItemStack(ObsidiumMod.OLYMPIAN_HADRON_COLLIDER_BLOCK)
        };

        for (int c = 0; c < CHAPTER_TITLES.length; c++) {
            int cy = sbY + 19 + c * 20;
            boolean active = (this.currentChapter == c);
            boolean hover = mouseX >= sbX + 5 && mouseX < sbX + SIDEBAR_W - 5 && mouseY >= cy && mouseY < cy + 19;

            int bg = active ? 0xFF3E1E08 : (hover ? 0xFF281934 : 0xFF1A1123);
            int brd = active ? 0xFFFF9900 : (hover ? 0xFF884400 : 0xFF352347);
            g.fill(sbX + 5, cy, sbX + SIDEBAR_W - 5, cy + 19, bg);
            drawBorder(g, sbX + 5, cy, SIDEBAR_W - 10, 19, brd);

            g.renderItem(chapterIcons[c], sbX + 7, cy + 1);
            g.drawString(this.font, CHAPTER_TITLES[c], sbX + 26, cy + 6, active ? 0xFFFFDD66 : 0xFFEEEEEE, false);
        }

        // 4. Right Content Area
        int contentX = sbX + SIDEBAR_W + 6;
        int contentW = BOOK_WIDTH - (contentX - x) - 8;

        // Sub-tabs row at top of Right Content Area (y+26 .. y+42)
        String[] subTabs = SUB_TABS[this.currentChapter];
        int subW = (contentW - (subTabs.length - 1) * 4) / subTabs.length;
        for (int t = 0; t < subTabs.length; t++) {
            int tx = contentX + t * (subW + 4);
            int ty = y + 26;
            boolean active = (this.currentSubTab == t);
            boolean hover = mouseX >= tx && mouseX < tx + subW && mouseY >= ty && mouseY < ty + 16;

            g.fill(tx, ty, tx + subW, ty + 16, active ? 0xFF552600 : (hover ? 0xFF351D42 : 0xFF1E1328));
            drawBorder(g, tx, ty, subW, 16, active ? 0xFFFFAA00 : 0xFF4A2E56);
            int tw = this.font.width(subTabs[t]);
            int maxTw = Math.max(8, subW - 4);
            if (tw > maxTw) {
                float s = (float) maxTw / tw;
                g.pose().pushPose();
                g.pose().translate(tx + 2, ty + 4 + (1.0f - s) * 3, 0);
                g.pose().scale(s, s, 1.0f);
                g.drawString(this.font, subTabs[t], 0, 0, active ? 0xFFFFEE88 : 0xFFBBBBCC, false);
                g.pose().popPose();
            } else {
                g.drawString(this.font, subTabs[t], tx + (subW - tw) / 2, ty + 4, active ? 0xFFFFEE88 : 0xFFBBBBCC, false);
            }
        }

        // Main Content Box (y+46 .. y+BOOK_HEIGHT-8)
        int boxY = y + 46;
        int boxH = BOOK_HEIGHT - 54;
        g.fill(contentX, boxY, contentX + contentW, boxY + boxH, 0xFF130D1B);
        drawBorder(g, contentX, boxY, contentX + contentW - contentX, boxH, 0xFF4A2511);

        switch (this.currentChapter) {
            case 0 -> renderPowerChapter(g, contentX, boxY, contentW, boxH, mouseX, mouseY);
            case 1 -> renderForgeChapter(g, contentX, boxY, contentW, boxH, mouseX, mouseY);
            case 2 -> renderEnergyCoreChapter(g, contentX, boxY, contentW, boxH, mouseX, mouseY);
            case 3 -> renderStorageChapter(g, contentX, boxY, contentW, boxH, mouseX, mouseY);
            case 4 -> renderPipesChapter(g, contentX, boxY, contentW, boxH, mouseX, mouseY);
            case 5 -> renderDimensionChapter(g, contentX, boxY, contentW, boxH, mouseX, mouseY);
            case 6 -> renderMythologyChapter(g, contentX, boxY, contentW, boxH, mouseX, mouseY);
            case 7 -> renderColliderChapter(g, contentX, boxY, contentW, boxH, mouseX, mouseY);
        }

        if (!this.hoveredStack.isEmpty()) {
            g.renderTooltip(this.font, this.hoveredStack, mouseX, mouseY);
        }
    }

    // ==================== CHAPTER 0: POWER & CHEMISTRY ====================
    private void renderPowerChapter(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        if (this.currentSubTab == 0) {
            ItemStack[] icons = {
                new ItemStack(ObsidiumMod.VOLCANITE_GENERATOR_BLOCK),
                new ItemStack(ObsidiumMod.VOLCANITE_STEAM_GENERATOR_BLOCK),
                new ItemStack(ObsidiumMod.VOLCANITE_GEOTHERMAL_GENERATOR_BLOCK),
                new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_GENERATOR_BLOCK),
                new ItemStack(ObsidiumMod.DEMETRIAN_BIOMASS_GENERATOR_ITEM)
            };
            String[] names = {
                "Basic Generator",
                "Steam Turbine",
                "Geothermal Gen",
                "Supreme Volcanic",
                "Demetrian Biomass"
            };
            String[] subtitles = {
                "Lava (10 mB/s)",
                "Water + Lava ➔ Steam",
                "Passive + Lava Boost",
                "Pyroclastic Eruption",
                "Fruit ➔ Biogas + energy"
            };
            String[] badges = {
                "5 RF/t",
                "20 RF/t",
                "25 RF/t",
                "2,500 RF/t",
                "800 RF/t max"
            };
            int[] colors = {0xFFFFAA00, 0xFF00E5FF, 0xFF55FF77, 0xFFFF44AA, 0xFF88DD66};

            for (int i = 0; i < icons.length; i++) {
                int cy = y + 3 + i * 32;
                g.fill(x + 8, cy, x + w - 8, cy + 29, 0xFF1B1224);
                drawBorder(g, x + 8, cy, w - 16, 29, colors[i]);

                drawSlotWithItem(g, icons[i], x + 13, cy + 4, mouseX, mouseY, colors[i]);
                g.drawString(this.font, names[i], x + 38, cy + 4, colors[i], false);
                g.drawString(this.font, subtitles[i], x + 38, cy + 16, 0xFFCCCCCC, false);

                drawBadge(g, x + w - 14, cy + 3, badges[i], 0xFF100A16, colors[i], colors[i]);
            }
        } else if (this.currentSubTab == 1) {
            renderPowerMultiblocks(g, x, y, w, mouseX, mouseY);
        } else {
            // Card 1: Catalyst Recipe
            int c1Y = y + 8;
            g.fill(x + 8, c1Y, x + w - 8, c1Y + 46, 0xFF1B1224);
            drawBorder(g, x + 8, c1Y, w - 16, 46, 0xFFFFAA00);
            g.drawString(this.font, "1. Astral Catalyst", x + 14, c1Y + 6, 0xFFFFCC44, false);
            drawBadge(g, x + w - 14, c1Y + 4, "4× in Forge", 0xFF100A16, 0xFF55FF77, 0xFF55FF77);

            drawSlotWithItem(g, new ItemStack(Items.NETHER_STAR, 1), x + 16, c1Y + 21, mouseX, mouseY, 0xFF664400);
            g.drawString(this.font, "+", x + 40, c1Y + 27, 0xFFAAAAAA, false);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANIC_ASH, 4), x + 50, c1Y + 21, mouseX, mouseY, 0xFF664400);
            g.drawString(this.font, "+", x + 74, c1Y + 27, 0xFFAAAAAA, false);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 2), x + 84, c1Y + 21, mouseX, mouseY, 0xFF664400);
            g.drawString(this.font, "➔", x + 112, c1Y + 27, 0xFFFFCC00, false);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.ASTRAL_VOLCANIC_CATALYST, 2), x + 128, c1Y + 21, mouseX, mouseY, 0xFFFFCC00);

            // Card 2: Distillation Chamber
            int c2Y = y + 60;
            g.fill(x + 8, c2Y, x + w - 8, c2Y + 46, 0xFF1B1224);
            drawBorder(g, x + 8, c2Y, w - 16, 46, 0xFFFF3388);
            g.drawString(this.font, "2. Pyroclastic Fuel", x + 14, c2Y + 6, 0xFFFF66B2, false);
            drawBadge(g, x + w - 14, c2Y + 4, "1,000 mB", 0xFF100A16, 0xFFFF3388, 0xFFFF66B2);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANIC_DISTILLATION_CHAMBER_BLOCK), x + 16, c2Y + 21, mouseX, mouseY, 0xFFFF3388);
            drawSlotWithItem(g, new ItemStack(Items.LAVA_BUCKET), x + 46, c2Y + 21, mouseX, mouseY, 0xFFAA4400);
            g.drawString(this.font, "+", x + 70, c2Y + 27, 0xFFAAAAAA, false);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.ASTRAL_VOLCANIC_CATALYST), x + 80, c2Y + 21, mouseX, mouseY, 0xFFFFCC00);
            g.drawString(this.font, "➔", x + 108, c2Y + 27, 0xFFFF3388, false);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.PYROCLASTIC_DISTILLATE_BUCKET), x + 124, c2Y + 21, mouseX, mouseY, 0xFFFF3388);

            // Card 3: Geothermal Fluid Alchemy (Crucible, Condenser, Vitrifier & Lithofier)
            int c3Y = y + 108;
            int c3H = 50;
            g.fill(x + 8, c3Y, x + w - 8, c3Y + c3H, 0xFF1B1224);
            drawBorder(g, x + 8, c3Y, w - 16, c3H, 0xFF00E5FF);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.MAGMATIC_CRUCIBLE_ITEM), x + 14, c3Y + 6, mouseX, mouseY, 0xFFFF7A00);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.HYDROTHERMAL_CONDENSER_ITEM), x + 34, c3Y + 6, mouseX, mouseY, 0xFF00B4D8);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.HYDROTHERMAL_VITRIFIER_ITEM), x + 54, c3Y + 6, mouseX, mouseY, 0xFF00E5FF);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.HYDROTHERMAL_LITHOFIER_ITEM), x + 74, c3Y + 6, mouseX, mouseY, 0xFFFFAA00);
            drawSmallString(g, "Fluid Alchemy & Synthesis", x + 98, c3Y + 6, 0xFF00E5FF);
            drawBadge(g, x + w - 14, c3Y + 4, "Synthesis", 0xFF100A16, 0xFF00E5FF, 0xFF80EEFF);
            drawSmallString(g, "• Crucible: Lava | Condenser: Water", x + 98, c3Y + 18, 0xFFFFAA00);
            drawSmallString(g, "• Vitrifier: Obsidian | Lithofier: Rocks", x + 98, c3Y + 28, 0xFF80EEFF);
            drawSmallString(g, "• 100% passive thermal automation with zero fuel", x + 14, c3Y + 38, 0xFFCCCCCC);
        }
    }

    private void drawSmallString(GuiGraphics g, String text, int x, int y, int color) {
        drawSmallString(g, text, x, y, color, 185);
    }

    private void drawSmallString(GuiGraphics g, String text, int x, int y, int color, int maxW) {
        int strW = this.font.width(text);
        float baseScale = 0.70f;
        float actualScale = baseScale;
        if (strW * baseScale > maxW && maxW > 0) {
            actualScale = (float) maxW / strW;
        }
        g.pose().pushPose();
        g.pose().translate(x, y + (baseScale - actualScale) * 2, 0);
        g.pose().scale(actualScale, actualScale, 1.0f);
        g.drawString(this.font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private void renderPowerMultiblocks(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY) {
        // 1. Selector tabs at top (3 multiblocks)
        int btnW = (w - 24) / 3;
        int btnY = y + 4;
        String[] labels = {"Tartarus Piston", "Bio-Caldera", "Hadron Ring"};
        int[] activeBg = {0xFF3D2012, 0xFF12351E, 0xFF102A3A};
        int[] activeBorder = {0xFFFF7700, 0xFF00E577, 0xFF00E5FF};
        int[] activeTxt = {0xFFFFCC44, 0xFF88FF88, 0xFF88F6FF};

        for (int p = 0; p < 3; p++) {
            int bx = x + 8 + p * (btnW + 4);
            boolean sel = this.powerMultiblockPage == p;
            g.fill(bx, btnY, bx + btnW, btnY + 16, sel ? activeBg[p] : 0xFF1B1224);
            drawBorder(g, bx, btnY, btnW, 16, sel ? activeBorder[p] : 0xFF3A2B48);
            int tw = this.font.width(labels[p]);
            int maxTw = Math.max(8, btnW - 4);
            if (tw > maxTw) {
                float s = (float) maxTw / tw;
                g.pose().pushPose();
                g.pose().translate(bx + 2, btnY + 4 + (1.0f - s) * 3, 0);
                g.pose().scale(s, s, 1.0f);
                g.drawString(this.font, labels[p], 0, 0, sel ? activeTxt[p] : 0xFFAAAAAA, false);
                g.pose().popPose();
            } else {
                g.drawString(this.font, labels[p], bx + (btnW - tw) / 2, btnY + 4, sel ? activeTxt[p] : 0xFFAAAAAA, false);
            }
        }

        if (this.powerMultiblockPage == 0) {
            renderTartarusEngineFormation(g, x, y, w, mouseX, mouseY);
        } else if (this.powerMultiblockPage == 1) {
            renderBioCalderaFormation(g, x, y, w, mouseX, mouseY);
        } else {
            renderHadronRingSummary(g, x, y, w, mouseX, mouseY);
        }
    }

    private void renderHadronRingSummary(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY) {
        drawSmallString(g, "5×5 Toroidal Accelerator Ring", x + 10, y + 24, 0xFF73F2FF);
        drawBadge(g, x + w - 10, y + 21, "100M RF Buffer", 0xFF100A16, 0xFF00E5FF, 0xFF73F2FF);

        ItemStack ctrl = new ItemStack(ObsidiumMod.OLYMPIAN_HADRON_COLLIDER_BLOCK);
        ItemStack coil = new ItemStack(ObsidiumMod.SYNCHROTRON_RING_COIL_BLOCK);
        ItemStack energyPort = new ItemStack(ObsidiumMod.HADRON_ENERGY_PORT_BLOCK);
        ItemStack itemPort = new ItemStack(ObsidiumMod.HADRON_ITEM_PORT_BLOCK);
        ItemStack fluidPort = new ItemStack(ObsidiumMod.HADRON_FLUID_PORT_BLOCK);

        // Compact 5x5 top-down blueprint on left (cell 18x18 -> 90x90)
        int gx = x + 12;
        int gy = y + 38;
        int step = 18;
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                int sx = gx + c * step;
                int sy = gy + r * step;
                boolean edge = (r == 0 || r == 4 || c == 0 || c == 4);
                if (!edge) {
                    g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF090710);
                    continue;
                }
                if (r == 4 && c == 2) {
                    drawSlotWithItem(g, ctrl, sx - 1, sy - 1, mouseX, mouseY, 0xFF00E5FF);
                } else if (r == 0 && c == 2) {
                    drawSlotWithItem(g, energyPort, sx - 1, sy - 1, mouseX, mouseY, 0xFFFF9900);
                } else if (r == 2 && c == 0) {
                    drawSlotWithItem(g, itemPort, sx - 1, sy - 1, mouseX, mouseY, 0xFF33FF77);
                } else if (r == 2 && c == 4) {
                    drawSlotWithItem(g, fluidPort, sx - 1, sy - 1, mouseX, mouseY, 0xFF00B8FF);
                } else {
                    drawSlotWithItem(g, coil, sx - 1, sy - 1, mouseX, mouseY, 0xFF336699);
                }
            }
        }

        // Right-column details
        int rx = x + 108;
        int maxW = w - 114;
        drawSmallString(g, "• 1× Hadron Collider Controller", rx, y + 40, 0xFF73F2FF, maxW);
        drawSmallString(g, "• 12× Ring Coils (min 9)", rx, y + 52, 0xFFEEEEEE, maxW);
        drawSmallString(g, "• Req: 1 Energy, 1 Item, 1 Fluid Port", rx, y + 64, 0xFFFFCC66, maxW);
        drawSmallString(g, "• 3×3 inner ring stays hollow air", rx, y + 76, 0xFFCCCCCC, maxW);
        drawSmallString(g, "• Output: Up to 328k RF/t + 30M Burst", rx, y + 88, 0xFF55FF77, maxW);
        drawSmallString(g, "★ Open GUI for Holo 5×5 & Auto-Build!", rx, y + 102, 0xFF00E5FF, maxW);

        // Bottom note bar
        g.fill(x + 10, y + 132, x + w - 10, y + 158, 0xFF10202C);
        drawBorder(g, x + 10, y + 132, w - 20, 26, 0xFF00AACC);
        drawSmallString(g, "Synthesizes Singularity Hadron Alloy (10% chance) & generates", x + 15, y + 137, 0xFFDDF4FF, w - 30);
        drawSmallString(g, "massive RF! Auto-outputs into Modular Chests. See 'Collider' tab.", x + 15, y + 147, 0xFF73F2FF, w - 30);
    }

    private void renderTartarusEngineFormation(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY) {
        drawSmallString(g, "2×2×2 Engine · Example Build", x + 10, y + 24, 0xFFFFB74D);
        drawBadge(g, x + w - 10, y + 21, "1,000–1,500 RF/t", 0xFF100A16, 0xFFFF9900, 0xFFFFCC33);

        ItemStack gen = new ItemStack(ObsidiumMod.VOLCANITE_GENERATOR_BLOCK);
        ItemStack casing = new ItemStack(ObsidiumMod.ADVANCED_VOLCANITE_CASING_BLOCK);

        int slotSize = 20;
        int gap = 3;
        int gridSize = slotSize * 2 + gap; // 43px
        int firstGridX = x + 44;
        int secondGridX = x + 144;
        int gridY = y + 48;

        drawCenteredLabel(g, "Bottom (Y=0)", firstGridX, y + 36, gridSize, 0xFFFFCC55);
        drawCenteredLabel(g, "Top (Y=1)", secondGridX, y + 36, gridSize, 0xFFFFCC55);

        // Lower layer: 1 Volcanite Generator + 3 Adv. Casings
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                int sx = firstGridX + col * (slotSize + gap);
                int sy = gridY + row * (slotSize + gap);
                if (row == 0 && col == 0) {
                    drawSlotWithItem(g, gen, sx, sy, mouseX, mouseY, 0xFFFF9900);
                } else {
                    drawSlotWithItem(g, casing, sx, sy, mouseX, mouseY, 0xFF884488);
                }

                // Upper layer: 4 Adv. Casings
                int ux = secondGridX + col * (slotSize + gap);
                drawSlotWithItem(g, casing, ux, sy, mouseX, mouseY, 0xFF884488);
            }
        }

        // Legend
        int legY = gridY + gridSize + 5;
        drawSlotWithItem(g, gen, x + 24, legY, mouseX, mouseY, 0xFFFF9900);
        drawSmallString(g, "1× Generator", x + 48, legY + 7, 0xFFFFCC44);

        drawSlotWithItem(g, casing, x + 128, legY, mouseX, mouseY, 0xFF884488);
        drawSmallString(g, "7× Adv. Casing", x + 152, legY + 7, 0xFFFF8833);

        // Activation & Cryo-Cooling card
        int actY = legY + 22;
        int actH = 38;
        g.fill(x + 10, actY, x + w - 10, actY + actH, 0xFF1B1224);
        drawBorder(g, x + 10, actY, w - 20, actH, 0xFFFF7700);

        ItemStack wrench = new ItemStack(ObsidiumMod.VOLCANITE_WRENCH);
        drawSlotWithItem(g, wrench, x + 14, actY + 9, mouseX, mouseY, 0xFFFF7700);

        int actMaxW = w - 54;
        drawSmallString(g, "Wrench forms · Basic/Adv casings accepted · Shift-Wrench unforms", x + 40, actY + 5, 0xFFFFDD66, actMaxW);
        drawSmallString(g, "Cooling: Ice, Snow, Water or Bio-Ethanol prevents overheat", x + 40, actY + 16, 0xFF73F2FF, actMaxW);
        drawSmallString(g, "Cryo <= 420°C: +35% RF/t Boost | >= 950°C: -35% Throttle", x + 40, actY + 27, 0xFF55FF77, actMaxW);
    }

    private void renderBioCalderaFormation(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY) {
        drawSmallString(g, "2×2×2 Caldera · Example Build", x + 10, y + 24, 0xFF88FF88);
        drawBadge(g, x + w - 10, y + 21, "1,200–1,800 RF/t", 0xFF100A16, 0xFF55DD66, 0xFFAAEEAA);

        ItemStack demetrian = new ItemStack(ObsidiumMod.DEMETRIAN_BIOMASS_GENERATOR_ITEM);
        ItemStack barrel = new ItemStack(ObsidiumMod.REINFORCED_VOLCANIC_LIQUID_BARREL_BLOCK);
        ItemStack casing = new ItemStack(ObsidiumMod.ADVANCED_VOLCANITE_CASING_BLOCK);

        int slotSize = 20;
        int gap = 3;
        int gridSize = slotSize * 2 + gap; // 43px
        int firstGridX = x + 44;
        int secondGridX = x + 144;
        int gridY = y + 48;

        drawCenteredLabel(g, "Bottom (Y=0)", firstGridX, y + 36, gridSize, 0xFF88FF88);
        drawCenteredLabel(g, "Top (Y=1)", secondGridX, y + 36, gridSize, 0xFF88FF88);

        // Lower layer: 1 Demetrian + 1 Reinforced Barrel + 2 Adv. Casings
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                int sx = firstGridX + col * (slotSize + gap);
                int sy = gridY + row * (slotSize + gap);
                if (row == 0 && col == 0) {
                    drawSlotWithItem(g, demetrian, sx, sy, mouseX, mouseY, 0xFF55DD66);
                } else if (row == 0 && col == 1) {
                    drawSlotWithItem(g, barrel, sx, sy, mouseX, mouseY, 0xFF00E5FF);
                } else {
                    drawSlotWithItem(g, casing, sx, sy, mouseX, mouseY, 0xFF884488);
                }

                // Upper layer: 4 Adv. Casings
                int ux = secondGridX + col * (slotSize + gap);
                drawSlotWithItem(g, casing, ux, sy, mouseX, mouseY, 0xFF884488);
            }
        }

        // Legend (compact & perfectly spaced)
        int legY = gridY + gridSize + 7;
        drawSlotWithItem(g, demetrian, x + 8, legY, mouseX, mouseY, 0xFF55DD66);
        drawSmallString(g, "1× Demetrian", x + 30, legY + 7, 0xFF88FF88);

        drawSlotWithItem(g, barrel, x + 80, legY, mouseX, mouseY, 0xFF00E5FF);
        drawSmallString(g, "1× Barrel", x + 102, legY + 7, 0xFF00E5FF);

        drawSlotWithItem(g, casing, x + 150, legY, mouseX, mouseY, 0xFF884488);
        drawSmallString(g, "6× Adv. Casing", x + 172, legY + 7, 0xFFFF8833);

        // Activation card
        int actY = legY + 25;
        g.fill(x + 10, actY, x + w - 10, actY + 32, 0xFF1B1224);
        drawBorder(g, x + 10, actY, w - 20, 32, 0xFF55DD66);

        ItemStack wrench = new ItemStack(ObsidiumMod.VOLCANITE_WRENCH);
        drawSlotWithItem(g, wrench, x + 14, actY + 6, mouseX, mouseY, 0xFF55DD66);

        drawSmallString(g, "Wrench: 1+ Biomass Gen / Wood or Reinf. Barrel", x + 40, actY + 7, 0xFF88FF88);
        drawSmallString(g, "Rest: Basic/Adv Casings · Shift-Wrench unforms", x + 40, actY + 18, 0xFF99AABB);
    }

    // ==================== CHAPTER 1: MAGMA FORGE 3x3 ====================
    private void renderForgeChapter(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        if (this.currentSubTab == 0) {
            g.drawString(this.font, "🔥 3×3 Magma Forge Blueprint", x + 10, y + 8, 0xFFFFB74D, false);

            ItemStack core = new ItemStack(ObsidiumMod.MAGMA_FORGE_CORE_BLOCK);
            ItemStack casing = new ItemStack(ObsidiumMod.VOLCANITE_CASING_BLOCK);
            ItemStack corner = new ItemStack(ObsidiumMod.VOLCANITE_ENERGY_UNIT_BLOCK);

            int gridX = x + 12;
            int gridY = y + 24;
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    int sx = gridX + c * 25;
                    int sy = gridY + r * 25;
                    ItemStack st = (r == 1 && c == 1) ? core : ((r != 1 && c != 1) ? corner : casing);
                    int col = (r == 1 && c == 1) ? 0xFFFFCC00 : ((r != 1 && c != 1) ? 0xFF00E5FF : 0xFFFF6600);
                    drawSlotWithItem(g, st, sx, sy, mouseX, mouseY, col);
                }
            }

            int lx = x + 94;
            drawSlotWithItem(g, core, lx, y + 24, mouseX, mouseY, 0xFFFFCC00);
            g.drawString(this.font, "1× Forge Core", lx + 24, y + 29, 0xFFFFCC00, false);

            drawSlotWithItem(g, casing, lx, y + 50, mouseX, mouseY, 0xFFFF6600);
            g.drawString(this.font, "4× Casings (sides)", lx + 24, y + 55, 0xFFFF8833, false);

            drawSlotWithItem(g, corner, lx, y + 76, mouseX, mouseY, 0xFF00E5FF);
            drawSmallString(g, "4× Cells / Units / Cores", lx + 24, y + 81, 0xFF00E5FF, w - 124);

            g.fill(x + 10, y + 110, x + w - 10, y + 148, 0xFF1B1224);
            drawBorder(g, x + 10, y + 110, w - 20, 38, 0xFF44AA55);
            g.drawString(this.font, "✔ Build flat. Right-click the center core.", x + 16, y + 118, 0xFF66FF88, false);
            g.drawString(this.font, "The 8 outer blocks become forge casings.", x + 16, y + 131, 0xFFCCCCCC, false);
        } else if (this.currentSubTab == 1) {
            g.drawString(this.font, "🔨 Magma Tempering", x + 10, y + 8, 0xFFFFB74D, false);

            int bx = x + 10;
            int by = y + 24;
            int bw = w - 20;
            g.fill(bx, by, bx + bw, by + 16, 0xFF331212);
            g.fill(bx + (int)(bw * 0.22f), by, bx + (int)(bw * 0.78f), by + 16, 0xFF663800);
            g.fill(bx + (int)(bw * 0.36f), by, bx + (int)(bw * 0.64f), by + 16, 0xFF22AA44);
            drawBorder(g, bx, by, bw, 16, 0xFFFF8800);
            g.drawString(this.font, "HIT [SPACE] IN GREEN", bx + 42, by + 4, 0xFFFFFFFF, false);

            String[][] cards = {
                {"★ Perfect Strike", "+35% Progress & +1 ★", "0xFF55FF77"},
                {"★★ Masterwork (2+ ★)", "-50% Cost + Bonus Item!", "0xFF00E5FF"},
                {"⚡ 1-Click Auto-Fill", "Click recipe to pull items", "0xFFFFCC44"}
            };
            for (int i = 0; i < 3; i++) {
                int cy = y + 48 + i * 36;
                int col = Integer.parseUnsignedInt(cards[i][2].substring(2), 16);
                g.fill(x + 10, cy, x + w - 10, cy + 31, 0xFF1B1224);
                drawBorder(g, x + 10, cy, w - 20, 31, col);
                g.drawString(this.font, cards[i][0], x + 16, cy + 5, col, false);
                g.drawString(this.font, cards[i][1], x + 16, cy + 17, 0xFFDDDDDD, false);
            }
        } else {
            renderRecipesSubTab(g, x, y, w, h, mouseX, mouseY);
        }
    }

    // ==================== CHAPTER 2: ENERGY CORE 3x3 ====================
    private void renderEnergyCoreChapter(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        if (this.currentSubTab == 0) {
            g.drawString(this.font, "🔋 3×3 Energy Core Matrix", x + 10, y + 8, 0xFF00E5FF, false);

            ItemStack core = new ItemStack(ObsidiumMod.VOLCANITE_ENERGY_CORE_BLOCK);
            ItemStack advCell = new ItemStack(ObsidiumMod.VOLCANITE_ENERGY_UNIT_BLOCK);
            ItemStack casing = new ItemStack(ObsidiumMod.ADVANCED_VOLCANITE_CASING_BLOCK);

            int gridX = x + 12;
            int gridY = y + 24;
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    int sx = gridX + c * 25;
                    int sy = gridY + r * 25;
                    ItemStack st = (r == 1 && c == 1) ? core : ((r != 1 && c != 1) ? casing : advCell);
                    int col = (r == 1 && c == 1) ? 0xFFFFCC00 : ((r != 1 && c != 1) ? 0xFFFF8800 : 0xFF00E5FF);
                    drawSlotWithItem(g, st, sx, sy, mouseX, mouseY, col);
                }
            }

            int lx = x + 94;
            drawSlotWithItem(g, core, lx, y + 24, mouseX, mouseY, 0xFFFFCC00);
            g.drawString(this.font, "1× Energy Core", lx + 24, y + 29, 0xFFFFCC00, false);

            drawSlotWithItem(g, advCell, lx, y + 50, mouseX, mouseY, 0xFF00E5FF);
            g.drawString(this.font, "4–8× Cells / Units", lx + 24, y + 55, 0xFF00E5FF, false);

            drawSlotWithItem(g, casing, lx, y + 76, mouseX, mouseY, 0xFFFF8800);
            g.drawString(this.font, "0–4× Casings", lx + 24, y + 81, 0xFFFF8833, false);

            g.fill(x + 10, y + 110, x + w - 10, y + 148, 0xFF12222B);
            drawBorder(g, x + 10, y + 110, w - 20, 38, 0xFF00E5FF);
            g.drawString(this.font, "⚡ Auto-forms with 8 valid ring blocks.", x + 16, y + 118, 0xFF55FF99, false);
            g.drawString(this.font, "4+ Cells / Units; rest: Casings.", x + 16, y + 131, 0xFFCCCCCC, false);
        } else if (this.currentSubTab == 1) {
            ItemStack[] icons = {
                new ItemStack(ObsidiumMod.VOLCANITE_ENERGY_CELL_BLOCK),
                new ItemStack(ObsidiumMod.VOLCANITE_ENERGY_UNIT_BLOCK),
                new ItemStack(ObsidiumMod.VOLCANITE_ENERGY_CORE_BLOCK)
            };
            String[] titles = {
                "Basic Cell",
                "Advanced Cell",
                "Max 3×3 Core"
            };
            String[] lines = {
                "1×1: 2M RF (25k/t)",
                "1×1: 15M RF (150k/t)",
                "Core + 8× Adv Cells"
            };
            String[] badges = {
                "+25M in 3×3",
                "+100M in 3×3",
                "900M RF"
            };
            int[] cols = {0xFFFFAA00, 0xFF00E5FF, 0xFF55FF77};

            for (int i = 0; i < 3; i++) {
                int cy = y + 10 + i * 48;
                g.fill(x + 8, cy, x + w - 8, cy + 40, 0xFF1B1224);
                drawBorder(g, x + 8, cy, w - 16, 40, cols[i]);

                drawSlotWithItem(g, icons[i], x + 14, cy + 10, mouseX, mouseY, cols[i]);
                g.drawString(this.font, titles[i], x + 40, cy + 9, cols[i], false);
                g.drawString(this.font, lines[i], x + 40, cy + 23, 0xFFCCCCCC, false);

                drawBadge(g, x + w - 14, cy + 7, badges[i], 0xFF100A16, cols[i], cols[i]);
            }
        } else if (this.currentSubTab == 2) {
            // HV 3x3x3 Substation Layout — All 3 Layers Explicitly Shown
            g.drawString(this.font, "⚡ Keraunos HV Matrix (3×3×3)", x + 10, y + 6, 0xFF38BDF8, false);
            drawBadge(g, x + w - 10, y + 4, "1.0B – 15.0B+ RF", 0xFF100A16, 0xFFFACC15, 0xFFFACC15);

            ItemStack hvCore = new ItemStack(ObsidiumMod.KERAUNOS_HV_CORE_BLOCK);
            ItemStack hvPort = new ItemStack(ObsidiumMod.KERAUNOS_HV_PORT_BLOCK);
            ItemStack casing = new ItemStack(ObsidiumMod.ADVANCED_VOLCANITE_CASING_BLOCK);
            ItemStack glass = new ItemStack(ObsidiumMod.VOLCANITE_GLASS_BLOCK);
            ItemStack unit = new ItemStack(ObsidiumMod.VOLCANITE_ENERGY_UNIT_BLOCK);

            int gY = y + 30;
            int step = 21; // 20px slot + 1px gap -> 62px grid width

            // Layer 1: Bottom (Y = -1)
            int g1X = x + 12;
            drawCenteredLabel(g, "1. Bottom (Y-1)", g1X, y + 20, 62, 0xFFFFAA00);
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    ItemStack st = (r == 1 && c == 1) ? unit : ((r == 0 || r == 2) && (c == 0 || c == 2) ? casing : glass);
                    int col = (st == unit) ? 0xFF38BDF8 : (st == glass ? 0xFF818CF8 : 0xFFFF8800);
                    drawSlotWithItem(g, st, g1X + c * step, gY + r * step, mouseX, mouseY, col);
                }
            }

            // Layer 2: Middle (Y = 0)
            int g2X = x + 87;
            drawCenteredLabel(g, "2. Middle (Y=0)", g2X, y + 20, 62, 0xFF38BDF8);
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    ItemStack st = (r == 1 && c == 1) ? hvCore : ((r == 1 || c == 1) ? hvPort : casing);
                    int col = (st == hvCore) ? 0xFFFACC15 : (st == hvPort ? 0xFF38BDF8 : 0xFFFF8800);
                    drawSlotWithItem(g, st, g2X + c * step, gY + r * step, mouseX, mouseY, col);
                }
            }

            // Layer 3: Top (Y = +1)
            int g3X = x + 162;
            drawCenteredLabel(g, "3. Top (Y+1)", g3X, y + 20, 62, 0xFF34D399);
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    ItemStack st = (r == 1 && c == 1) ? unit : ((r == 0 || r == 2) && (c == 0 || c == 2) ? casing : glass);
                    int col = (st == unit) ? 0xFF38BDF8 : (st == glass ? 0xFF818CF8 : 0xFFFF8800);
                    drawSlotWithItem(g, st, g3X + c * step, gY + r * step, mouseX, mouseY, col);
                }
            }

            // Bottom summary box
            g.fill(x + 10, y + 98, x + w - 10, y + 150, 0xFF0E1726);
            drawBorder(g, x + 10, y + 98, w - 20, 52, 0xFF38BDF8);
            drawSmallString(g, "• Center = 1× Keraunos HV Core; 26 Shell blocks around it.", x + 16, y + 104, 0xFFFACC15, w - 32);
            drawSmallString(g, "• Valid Shell: Basic/Adv Casings, supported Glass, Cells/Units & ≥1 HV Port.", x + 16, y + 115, 0xFFE2E8F0, w - 32);
            drawSmallString(g, "• Capacity: 1.0 BRF base (+150M Basic Cell / +600M Adv Unit).", x + 16, y + 126, 0xFF34D399, w - 32);
            drawSmallString(g, "• Not replacing 3×3 Core — this is the High-Voltage tier!", x + 16, y + 137, 0xFF38BDF8, w - 32);
        } else {
            // HV Safety Protocols: Faraday Suit & Hot-Stick
            g.drawString(this.font, "⚠ High-Voltage Safety Protocols", x + 10, y + 6, 0xFFFACC15, false);

            // Card 1: Aether-Faraday Dielectric Suit
            int c1Y = y + 22;
            g.fill(x + 8, c1Y, x + w - 8, c1Y + 46, 0xFF141B29);
            drawBorder(g, x + 8, c1Y, w - 16, 46, 0xFF38BDF8);
            drawSmallString(g, "1. Aether-Faraday Suit (4/4 Set Required)", x + 14, c1Y + 5, 0xFF38BDF8);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.FARADAY_HELMET), x + 14, c1Y + 18, mouseX, mouseY, 0xFF38BDF8);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.FARADAY_CHESTPLATE), x + 36, c1Y + 18, mouseX, mouseY, 0xFF38BDF8);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.FARADAY_LEGGINGS), x + 58, c1Y + 18, mouseX, mouseY, 0xFF38BDF8);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.FARADAY_BOOTS), x + 80, c1Y + 18, mouseX, mouseY, 0xFF38BDF8);
            drawSmallString(g, "• Blocks Proximity HV Arcs (4.5–6.5m)", x + 106, c1Y + 20, 0xFFE2E8F0, 125);
            drawSmallString(g, "• Grants 100% Lightning Immunity.", x + 106, c1Y + 32, 0xFF34D399, 125);

            // Card 2: HV Dielectric Hot-Stick
            int c2Y = y + 73;
            g.fill(x + 8, c2Y, x + w - 8, c2Y + 38, 0xFF141B29);
            drawBorder(g, x + 8, c2Y, w - 16, 38, 0xFFFACC15);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.HV_DIELECTRIC_HOT_STICK), x + 14, c2Y + 9, mouseX, mouseY, 0xFFFACC15);
            drawSmallString(g, "2. HV Dielectric Hot-Stick", x + 38, c2Y + 6, 0xFFFACC15);
            drawSmallString(g, "• Right-Click: Open GUI (bare hand shocks!)", x + 38, c2Y + 17, 0xFFE2E8F0, 195);
            drawSmallString(g, "• Shift+Right-Click: Toggle Grounded Mode (0 kV)", x + 38, c2Y + 27, 0xFF34D399, 195);

            // Card 3: Dismantle Warning
            int c3Y = y + 116;
            g.fill(x + 8, c3Y, x + w - 8, c3Y + 36, 0xFF261218);
            drawBorder(g, x + 8, c3Y, w - 16, 36, 0xFFEF4444);
            drawSmallString(g, "3. Mandatory Grounding Before Dismantling", x + 14, c3Y + 6, 0xFFF87171);
            drawSmallString(g, "Breaking ANY live block without Grounding causes an Arc Blast", x + 14, c3Y + 17, 0xFFE2E8F0, w - 28);
            drawSmallString(g, "(heavy blast damage + 25% stored energy discharged)!", x + 14, c3Y + 26, 0xFFFCA5A5, w - 28);
        }
    }

    // ==================== CHAPTER 3: STORAGE & BARRELS ====================
    private void renderStorageChapter(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        if (this.currentSubTab == 0) {
            ItemStack[] icons = {
                new ItemStack(ObsidiumMod.VOLCANITE_STORAGE_CONTROLLER_BLOCK),
                new ItemStack(ObsidiumMod.VOLCANITE_STORAGE_DISK_DRIVE_BLOCK),
                new ItemStack(ObsidiumMod.VOLCANITE_SORTING_STATION_BLOCK)
            };
            String[] titles = {
                "Controller & UI",
                "8-Bay Disk Drive",
                "Sorting & Nodes"
            };
            String[] subs = {
                "[Items] [Fluids] [⚔ Tools] Armory",
                "Item & Fluid Disks (32k–256k)",
                "Auto-filters & Wireless"
            };
            String[] badges = {"Core", "8 Bays", "Wireless"};
            int[] cols = {0xFFFFAA00, 0xFF00E5FF, 0xFF55FF77};

            for (int i = 0; i < 3; i++) {
                int cy = y + 10 + i * 48;
                g.fill(x + 8, cy, x + w - 8, cy + 40, 0xFF1B1224);
                drawBorder(g, x + 8, cy, w - 16, 40, cols[i]);

                drawSlotWithItem(g, icons[i], x + 14, cy + 10, mouseX, mouseY, cols[i]);
                g.drawString(this.font, titles[i], x + 40, cy + 9, cols[i], false);
                g.drawString(this.font, subs[i], x + 40, cy + 23, 0xFFCCCCCC, false);

                drawBadge(g, x + w - 14, cy + 7, badges[i], 0xFF100A16, cols[i], cols[i]);
            }
        } else if (this.currentSubTab == 1) {
            renderItemBarrelsSubTab(g, x, y, w, h, mouseX, mouseY);
        } else if (this.currentSubTab == 2) {
            g.drawString(this.font, "Capacities & recipes", x + 10, y + 5, 0xFFFFB74D, false);
            ItemStack[] outputs = {
                new ItemStack(ObsidiumMod.VOLCANIC_WOOD_LIQUID_BARREL_ITEM),
                new ItemStack(ObsidiumMod.REINFORCED_VOLCANIC_LIQUID_BARREL_ITEM),
                new ItemStack(ObsidiumMod.MEGA_VOLCANIC_BARREL_ITEM),
                new ItemStack(ObsidiumMod.SUPREME_LIQUID_TANK_ITEM)
            };
            String[] titles = {
                "Volcanic Barrel · 64,000 mB",
                "Reinforced · 256,000 mB",
                "Dionysian Barrel · 1,024,000 mB",
                "Supreme Tank · 1,600,000 mB"
            };
            String[][] ingredients = {
                {"4 logs · 4 plates · barrel"},
                {"Barrel · 4 bronze · 4 obsidian"},
                {"Reinf. Barrel · Core · 7 Casings", "2×2×2 · auto-forms"},
                {"2 Supreme Alloys · Thermo Core", "2 Supreme Glass · Reinforced Barrel", "2 Lead Blocks · Advanced Casing"}
            };
            int[] cardHeights = {25, 25, 35, 43};
            int[] colors = {0xFFFFAA00, 0xFF00E5FF, 0xFFFF55AA, 0xFFFF44AA};
            int cy = y + 22;
            for (int i = 0; i < outputs.length; i++) {
                int cardHeight = cardHeights[i];
                g.fill(x + 8, cy, x + w - 8, cy + cardHeight, 0xFF1B1224);
                drawBorder(g, x + 8, cy, w - 16, cardHeight, colors[i]);
                drawSlotWithItem(g, outputs[i], x + 10, cy + (cardHeight - 20) / 2, mouseX, mouseY, colors[i]);
                g.drawString(this.font, titles[i], x + 34, cy + 2, colors[i], false);
                for (int line = 0; line < ingredients[i].length; line++) {
                    g.drawString(this.font, ingredients[i][line], x + 34, cy + 13 + line * 10, 0xFFCCCCCC, false);
                }
                cy += cardHeight + 3;
            }
        } else if (this.currentSubTab == 3) {
            renderDionysianBarrelCoreRecipe(g, x, y, w, mouseX, mouseY);
        } else if (this.currentSubTab == 4) {
            renderDionysianBarrelFormation(g, x, y, w, mouseX, mouseY);
        } else {
            renderCrafterSubTab(g, x, y, w, mouseX, mouseY);
        }
    }

    private void renderItemBarrelsSubTab(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        int c1Y = y + 6;
        int c1H = 76;
        g.fill(x + 8, c1Y, x + w - 8, c1Y + c1H, 0xFF1B1224);
        drawBorder(g, x + 8, c1Y, w - 16, c1H, 0xFFFFAA00);

        drawSlotWithItem(g, new ItemStack(ObsidiumMod.BASIC_VOLCANITE_ITEM_BARREL_ITEM), x + 14, c1Y + 8, mouseX, mouseY, 0xFFFFAA00);
        drawSlotWithItem(g, new ItemStack(ObsidiumMod.ADVANCED_VOLCANITE_ITEM_BARREL_ITEM), x + 38, c1Y + 8, mouseX, mouseY, 0xFF00E5FF);
        drawSlotWithItem(g, new ItemStack(ObsidiumMod.SUPREME_VOLCANITE_ITEM_BARREL_ITEM), x + 62, c1Y + 8, mouseX, mouseY, 0xFFFF3399);

        drawSmallString(g, "Single-Item Barrels", x + 90, c1Y + 9, 0xFFFFAA00);
        drawBadge(g, x + w - 14, c1Y + 6, "Tiers I–III", 0xFF100A16, 0xFFFFAA00, 0xFFFFAA00);

        drawSmallString(g, "• Basic: 4k items | Advanced: 16k | Supreme: 65k", x + 12, c1Y + 34, 0xFFCCCCCC);
        drawSmallString(g, "• Right-click: Deposit stack | Double-click: Deposit all", x + 12, c1Y + 46, 0xFFFFCC44);
        drawSmallString(g, "• Left-click: Extract 1 | Shift+Left: Extract full stack", x + 12, c1Y + 56, 0xFF00E5FF);
        drawSmallString(g, "• Wrench: Lock/Unlock filter | Shift+Right: Open Menu", x + 12, c1Y + 66, 0xFF55FF77);

        int c2Y = c1Y + c1H + 6;
        int c2H = 74;
        g.fill(x + 8, c2Y, x + w - 8, c2Y + c2H, 0xFF1B1224);
        drawBorder(g, x + 8, c2Y, w - 16, c2H, 0xFF00E5FF);

        drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANITE_BASIC_BARREL_UPGRADE), x + 14, c2Y + 8, mouseX, mouseY, 0xFFFFAA00);
        drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANITE_ADVANCED_BARREL_UPGRADE), x + 38, c2Y + 8, mouseX, mouseY, 0xFF00E5FF);
        drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANITE_SUPREME_BARREL_UPGRADE), x + 62, c2Y + 8, mouseX, mouseY, 0xFFFF3399);
        drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANITE_BARREL_VOID_UPGRADE), x + 86, c2Y + 8, mouseX, mouseY, 0xFFFF3366);

        drawSmallString(g, "Barrel Upgrades", x + 114, c2Y + 9, 0xFF00E5FF);
        drawBadge(g, x + w - 14, c2Y + 6, "Up to 64×", 0xFF100A16, 0xFF00E5FF, 0xFF00E5FF);

        drawSmallString(g, "• Basic Upgrade: Multiplies capacity by +4×", x + 12, c2Y + 34, 0xFFFFAA00);
        drawSmallString(g, "• Advanced Upgrade: Multiplies capacity by +16×", x + 12, c2Y + 45, 0xFF00E5FF);
        drawSmallString(g, "• Supreme Upgrade: Multiplies capacity by +64×", x + 12, c2Y + 55, 0xFFFF3399);
        drawSmallString(g, "★ Void Upgrade: Destroys incoming overflow when full", x + 12, c2Y + 65, 0xFFFF3366);
    }

    private void renderDionysianBarrelCoreRecipe(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY) {
        g.drawString(this.font, "Dionysian Barrel Core", x + 10, y + 6, 0xFFFFB74D, false);
        g.drawString(this.font, "Crafting recipe", x + 10, y + 20, 0xFFCCCCCC, false);

        ItemStack obsidian = new ItemStack(Items.OBSIDIAN);
        ItemStack bronze = new ItemStack(ObsidiumMod.PYRIC_BRONZE_INGOT);
        ItemStack mechanism = new ItemStack(ObsidiumMod.VOLCANITE_FLUX_MECHANISM);
        int gridX = x + 18;
        int gridY = y + 43;
        int spacing = 23;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                ItemStack ingredient = row == 1 && col == 1 ? mechanism
                        : row == 1 || col == 1 ? bronze : obsidian;
                drawSlotWithItem(g, ingredient, gridX + col * spacing, gridY + row * spacing,
                        mouseX, mouseY, 0xFF9C5826);
            }
        }

        g.drawString(this.font, "➜", x + 104, y + 79, 0xFFFFCC44, false);
        drawSlotWithItem(g, new ItemStack(ObsidiumMod.DIONYSIAN_BARREL_CORE), x + 128, y + 74,
                mouseX, mouseY, 0xFFFFAA33);
        g.drawString(this.font, "Corners · Obsidian", x + 18, y + 123, 0xFFDDCCBB, false);
        g.drawString(this.font, "Sides · Pyric Bronze", x + 18, y + 136, 0xFFDDCCBB, false);
        g.drawString(this.font, "Center · Flux Mechanism", x + 18, y + 149, 0xFFDDCCBB, false);
    }

    private void renderDionysianBarrelFormation(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY) {
        g.drawString(this.font, "Mega Barrel · 2×2×2 Example", x + 10, y + 6, 0xFFFFB74D, false);
        drawBadge(g, x + w - 10, y + 4, "1,024k mB", 0xFF100A16, 0xFFFF9900, 0xFFFFBB33);

        ItemStack reinf = new ItemStack(ObsidiumMod.REINFORCED_VOLCANIC_LIQUID_BARREL_BLOCK);
        ItemStack casing = new ItemStack(ObsidiumMod.VOLCANITE_CASING_BLOCK);

        int slotSize = 20;
        int gap = 3;
        int gridSize = slotSize * 2 + gap; // 43px
        int firstGridX = x + 44;
        int secondGridX = x + 144;
        int gridY = y + 33;

        drawCenteredLabel(g, "Bottom (Y=0)", firstGridX, y + 20, gridSize, 0xFFFFCC55);
        drawCenteredLabel(g, "Top (Y=1)", secondGridX, y + 20, gridSize, 0xFFFFCC55);

        // Lower layer: 1 Reinforced Barrel + 3 Casings
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                int sx = firstGridX + col * (slotSize + gap);
                int sy = gridY + row * (slotSize + gap);
                if (row == 0 && col == 0) {
                    drawSlotWithItem(g, reinf, sx, sy, mouseX, mouseY, 0xFF00E5FF);
                } else {
                    drawSlotWithItem(g, casing, sx, sy, mouseX, mouseY, 0xFFFF8833);
                }

                // Upper layer: 4 Casings
                int ux = secondGridX + col * (slotSize + gap);
                drawSlotWithItem(g, casing, ux, sy, mouseX, mouseY, 0xFFFF8833);
            }
        }

        // Legend
        int legY = gridY + gridSize + 7;
        drawSlotWithItem(g, reinf, x + 24, legY, mouseX, mouseY, 0xFF00E5FF);
        g.drawString(this.font, "1× Reinf. Barrel", x + 48, legY + 6, 0xFF00E5FF, false);

        drawSlotWithItem(g, casing, x + 140, legY, mouseX, mouseY, 0xFFFF8833);
        g.drawString(this.font, "7× Casings", x + 164, legY + 6, 0xFFFF8833, false);

        // Activation card (compact & clean)
        int actY = legY + 26;
        g.fill(x + 10, actY, x + w - 10, actY + 36, 0xFF1B1224);
        drawBorder(g, x + 10, actY, w - 20, 36, 0xFF44AA55);

        ItemStack core = new ItemStack(ObsidiumMod.DIONYSIAN_BARREL_CORE);
        drawSlotWithItem(g, core, x + 16, actY + 8, mouseX, mouseY, 0xFFFFCC00);

        drawSmallString(g, "Core forms · 1+ Reinf. Barrel", x + 42, actY + 9, 0xFFFFDD66, w - 54);
        drawSmallString(g, "Rest: Basic Casings · Shift-Wrench unforms", x + 42, actY + 21, 0xFF88CC88, w - 54);
    }

    private void drawCenteredLabel(GuiGraphics g, String text, int x, int y, int width, int color) {
        g.drawString(this.font, text, x + (width - this.font.width(text)) / 2, y, color, false);
    }

    private void renderRecipesSubTab(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        List<MagmaForgeRecipe> recipes = MagmaForgeCoreBlockEntity.RECIPES;
        int totalPages = Math.max(1, (recipes.size() + RECIPES_PER_PAGE - 1) / RECIPES_PER_PAGE);
        int startIdx = this.recipePage * RECIPES_PER_PAGE;

        int cardW = (w - 20) / 2;
        int cardH = 62;

        for (int i = 0; i < RECIPES_PER_PAGE; i++) {
            int rIdx = startIdx + i;
            if (rIdx >= recipes.size()) break;
            MagmaForgeRecipe rec = recipes.get(rIdx);

            int col = i % 2;
            int row = i / 2;
            int cx = x + 8 + col * (cardW + 4);
            int cy = y + 8 + row * (cardH + 4);

            g.fill(cx, cy, cx + cardW, cy + cardH, 0xFF1C1326);
            drawBorder(g, cx, cy, cardW, cardH, 0xFF663300);

            String rName = rec.getResult().getHoverName().getString();
            if (rName.length() > 14) rName = rName.substring(0, 13) + "…";
            g.drawString(this.font, rName, cx + 5, cy + 4, 0xFFFFCC44, false);

            List<ItemStack> ings = rec.getIngredients();
            for (int j = 0; j < ings.size() && j < 3; j++) {
                drawSlotWithItem(g, ings.get(j), cx + 4 + j * 21, cy + 16, mouseX, mouseY, 0xFF553322);
            }

            g.drawString(this.font, "➔", cx + 69, cy + 22, 0xFFFFAA00, false);
            drawSlotWithItem(g, rec.getResult(), cx + 80, cy + 16, mouseX, mouseY, 0xFFFFCC00);

            String stats = (rec.getEnergyCost() / 1000) + "k RF | " + rec.getLavaCost() + "mB";
            g.drawString(this.font, stats, cx + 5, cy + 40, 0xFF00E5FF, false);

            if (!rec.getBonusResult().isEmpty()) {
                g.drawString(this.font, "★+" + rec.getBonusResult().getCount() + " Bonus", cx + 5, cy + 50, 0xFF55FF77, false);
            }
        }

        int navY = y + h - 18;
        boolean prevHover = mouseX >= x + 10 && mouseX < x + 52 && mouseY >= navY && mouseY < navY + 14;
        boolean nextHover = mouseX >= x + w - 52 && mouseX < x + w - 10 && mouseY >= navY && mouseY < navY + 14;

        g.fill(x + 10, navY, x + 52, navY + 14, prevHover ? 0xFF553311 : 0xFF24162E);
        drawBorder(g, x + 10, navY, 42, 14, 0xFF884400);
        g.drawString(this.font, "< Prev", x + 15, navY + 3, 0xFFFFCC00, false);

        String pageStr = (this.recipePage + 1) + " / " + totalPages;
        g.drawString(this.font, pageStr, x + (w - this.font.width(pageStr)) / 2, navY + 3, 0xFFCCCCCC, false);

        g.fill(x + w - 52, navY, x + w - 10, navY + 14, nextHover ? 0xFF553311 : 0xFF24162E);
        drawBorder(g, x + w - 52, navY, 42, 14, 0xFF884400);
        g.drawString(this.font, "Next >", x + w - 47, navY + 3, 0xFFFFCC00, false);
    }

    // ==================== CHAPTER 4: PIPES & LOGISTICS ====================
    private void renderPipesChapter(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        if (this.currentSubTab == 0) {
            // Sub-tab 0: Item Pipes (Hermes Item Pipe & Volcanite Storage Pipe)
            int c1Y = y + 4;
            int c1H = 76;
            g.fill(x + 6, c1Y, x + w - 6, c1Y + c1H, 0xFF1B1224);
            drawBorder(g, x + 6, c1Y, w - 12, c1H, 0xFFFFAA00);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.HERMES_ITEM_PIPE_BLOCK), x + 12, c1Y + 6, mouseX, mouseY, 0xFFFFAA00);
            drawSmallString(g, "Hermes Item Pipe", x + 36, c1Y + 6, 0xFFFFCC44);
            drawBadge(g, x + w - 10, c1Y + 4, "Standalone & Grid", 0xFF100A16, 0xFFFFAA00, 0xFFFFDD66);

            drawSmallString(g, "• High-speed point-to-point item logistics.", x + 36, c1Y + 18, 0xFFEEEEEE);
            drawSmallString(g, "• Right-click face to cycle EXTRACT / INSERT mode.", x + 12, c1Y + 30, 0xFFAAAAAA);
            drawSmallString(g, "• Auto-routes to Storage Controller on grid.", x + 12, c1Y + 40, 0xFFFFB74D);

            int r1Y = c1Y + 52;
            drawSlotWithItem(g, new ItemStack(Items.COPPER_INGOT, 3), x + 12, r1Y, mouseX, mouseY, 0xFF664400);
            drawSlotWithItem(g, new ItemStack(Items.REDSTONE, 2), x + 34, r1Y, mouseX, mouseY, 0xFF882200);
            drawSlotWithItem(g, new ItemStack(Blocks.GLASS, 1), x + 56, r1Y, mouseX, mouseY, 0xFF446688);
            g.drawString(this.font, "➔", x + 79, r1Y + 6, 0xFFFFCC00, false);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.HERMES_ITEM_PIPE_ITEM, 8), x + 92, r1Y, mouseX, mouseY, 0xFFFFAA00);
            drawSmallString(g, "Yields 8× Hermes Pipes", x + 116, r1Y + 6, 0xFF88DD66);

            // Card 2: Volcanite Storage Pipe
            int c2Y = y + 84;
            int c2H = 76;
            g.fill(x + 6, c2Y, x + w - 6, c2Y + c2H, 0xFF1B1224);
            drawBorder(g, x + 6, c2Y, w - 12, c2H, 0xFFFF6622);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANITE_STORAGE_PIPE_BLOCK), x + 12, c2Y + 6, mouseX, mouseY, 0xFFFF6622);
            drawSmallString(g, "Volcanite Optic Fiber", x + 36, c2Y + 6, 0xFFFF8844);
            drawBadge(g, x + w - 10, c2Y + 4, "Digital Network", 0xFF100A16, 0xFFFF6622, 0xFFFF8844);

            drawSmallString(g, "• Connects external chests, barrels & units to grid.", x + 36, c2Y + 18, 0xFFEEEEEE);
            drawSmallString(g, "• Mount Extract / Insert modules for dedicated routing.", x + 12, c2Y + 30, 0xFFAAAAAA);
            drawSmallString(g, "• Shift-right-click with Wrench to configure.", x + 12, c2Y + 40, 0xFFFFB74D);

            int r2Y = c2Y + 52;
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANITE_INGOT, 3), x + 12, r2Y, mouseX, mouseY, 0xFF664400);
            drawSlotWithItem(g, new ItemStack(Items.REDSTONE, 2), x + 34, r2Y, mouseX, mouseY, 0xFF882200);
            drawSlotWithItem(g, new ItemStack(Blocks.GLASS, 1), x + 56, r2Y, mouseX, mouseY, 0xFF446688);
            g.drawString(this.font, "➔", x + 79, r2Y + 6, 0xFFFFCC00, false);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANITE_STORAGE_PIPE_ITEM, 8), x + 92, r2Y, mouseX, mouseY, 0xFFFF6622);
            drawSmallString(g, "Central Digital Link", x + 116, r2Y + 6, 0xFF88DD66);

        } else if (this.currentSubTab == 1) {
            // Sub-tab 1: Fluid Pipes (Naiad Fluid Pipe & Fluid Network)
            int c1Y = y + 4;
            int c1H = 76;
            g.fill(x + 6, c1Y, x + w - 6, c1Y + c1H, 0xFF1B1224);
            drawBorder(g, x + 6, c1Y, w - 12, c1H, 0xFF00E5FF);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.NAIAD_FLUID_PIPE_BLOCK), x + 12, c1Y + 6, mouseX, mouseY, 0xFF00E5FF);
            drawSmallString(g, "Naiad Fluid Pipe", x + 36, c1Y + 6, 0xFF80EEFF);
            drawBadge(g, x + w - 10, c1Y + 4, "1,000 mB/t", 0xFF100A16, 0xFF00E5FF, 0xFF80EEFF);

            drawSmallString(g, "• High-velocity pressurised fluid transfer network.", x + 36, c1Y + 18, 0xFFEEEEEE);
            drawSmallString(g, "• Carries Water, Lava, Steam & Pyroclastic Fuel.", x + 12, c1Y + 30, 0xFFAAAAAA);
            drawSmallString(g, "• Equip Naiad Valve Module to auto-pump fluids.", x + 12, c1Y + 40, 0xFF00B4D8);

            int r1Y = c1Y + 52;
            drawSlotWithItem(g, new ItemStack(Items.COPPER_INGOT, 3), x + 12, r1Y, mouseX, mouseY, 0xFF664400);
            drawSlotWithItem(g, new ItemStack(Blocks.GLASS, 2), x + 34, r1Y, mouseX, mouseY, 0xFF446688);
            drawSlotWithItem(g, new ItemStack(Items.LAPIS_LAZULI, 1), x + 56, r1Y, mouseX, mouseY, 0xFF0055AA);
            g.drawString(this.font, "➔", x + 79, r1Y + 6, 0xFFFFCC00, false);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.NAIAD_FLUID_PIPE_ITEM, 8), x + 92, r1Y, mouseX, mouseY, 0xFF00E5FF);
            drawSmallString(g, "Yields 8× Naiad Pipes", x + 116, r1Y + 6, 0xFF00E5FF);

            // Card 2: Liquid Storage & Valve Integration
            int c2Y = y + 84;
            int c2H = 76;
            g.fill(x + 6, c2Y, x + w - 6, c2Y + c2H, 0xFF1B1224);
            drawBorder(g, x + 6, c2Y, w - 12, c2H, 0xFF3399FF);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANITE_LOGISTICS_MODULE), x + 12, c2Y + 6, mouseX, mouseY, 0xFF3399FF);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANITE_LIQUID_STORAGE_TANK_BLOCK), x + 34, c2Y + 6, mouseX, mouseY, 0xFF00E5FF);
            drawSmallString(g, "Valves & Fluid Tanks", x + 58, c2Y + 6, 0xFF66B2FF);
            drawBadge(g, x + w - 10, c2Y + 4, "Auto-Pumping", 0xFF100A16, 0xFF3399FF, 0xFF66B2FF);

            drawSmallString(g, "• Enforces strict one-way auto-pumping flow.", x + 58, c2Y + 18, 0xFFEEEEEE);
            drawSmallString(g, "• Connects with Liquid Tanks & Caldera machines.", x + 12, c2Y + 30, 0xFFAAAAAA);
            drawSmallString(g, "• Feeds Distillation Chambers & generators.", x + 12, c2Y + 40, 0xFFCCCCCC);
            drawSmallString(g, "★ Zero leakage or fluid cross-contamination.", x + 12, c2Y + 54, 0xFF88DD66);

        } else if (this.currentSubTab == 2) {
            // Sub-tab 2: Conduits (4 Tiers of Astrape Energy Wires & Volcanite Universal Conduit)
            int c1Y = y + 4;
            int c1H = 76;
            g.fill(x + 6, c1Y, x + w - 6, c1Y + c1H, 0xFF1B1224);
            drawBorder(g, x + 6, c1Y, w - 12, c1H, 0xFF00E5FF);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANITE_ENERGY_WIRE_ITEM), x + 12, c1Y + 6, mouseX, mouseY, 0xFFFF9922);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.ADVANCED_VOLCANITE_ENERGY_WIRE_ITEM), x + 34, c1Y + 6, mouseX, mouseY, 0xFF00CCFF);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.SUPREME_VOLCANITE_ENERGY_WIRE_ITEM), x + 56, c1Y + 6, mouseX, mouseY, 0xFFD96BFF);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.SINGULARITY_VOLCANITE_ENERGY_WIRE_ITEM), x + 78, c1Y + 6, mouseX, mouseY, 0xFF55FFFF);
            drawSmallString(g, "Astrape Energy Wires (T1–T4)", x + 102, c1Y + 6, 0xFF73F2FF);
            drawBadge(g, x + w - 10, c1Y + 4, "Up to 25M RF/t", 0xFF100A16, 0xFF00E5FF, 0xFF88FFFF);

            drawSmallString(g, "• Zero-Loss Instant BFS Grid: 25k / 250k / 2.5M / 25M RF/t!", x + 12, c1Y + 29, 0xFFEEEEEE, w - 24);
            drawSmallString(g, "• Right-Click wire with higher tier to upgrade in-place!", x + 12, c1Y + 40, 0xFF55FF99, w - 24);

            int r1Y = c1Y + 52;
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.SUPREME_VOLCANITE_ENERGY_WIRE_ITEM, 8), x + 12, r1Y, mouseX, mouseY, 0xFFD96BFF);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.SINGULARITY_HADRON_ALLOY, 1), x + 34, r1Y, mouseX, mouseY, 0xFF00E5FF);
            g.drawString(this.font, "➔", x + 58, r1Y + 6, 0xFFFFCC00, false);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.SINGULARITY_VOLCANITE_ENERGY_WIRE_ITEM, 8), x + 72, r1Y, mouseX, mouseY, 0xFF55FFFF);
            drawSmallString(g, "8× Singularity Wires (25M/t)", x + 96, r1Y + 6, 0xFF73F2FF, w - 104);

            // Card 2: Volcanite Universal Conduit
            int c2Y = y + 84;
            int c2H = 76;
            g.fill(x + 6, c2Y, x + w - 6, c2Y + c2H, 0xFF1B1224);
            drawBorder(g, x + 6, c2Y, w - 12, c2H, 0xFFFFD700);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANITE_UNIVERSAL_CONDUIT_BLOCK), x + 12, c2Y + 6, mouseX, mouseY, 0xFFFFD700);
            drawSmallString(g, "Volcanite Universal Conduit", x + 36, c2Y + 6, 0xFFFFE066);
            drawBadge(g, x + w - 10, c2Y + 4, "3-in-1 Unified", 0xFF100A16, 0xFFFFD700, 0xFFFFE066);

            drawSmallString(g, "• Power, Fluids & Items in 1 single block!", x + 36, c2Y + 18, 0xFFEEEEEE);
            drawSmallString(g, "• Transmits: 100k RF/t + 10k mB/t + fast items.", x + 12, c2Y + 30, 0xFFFFDD44);
            drawSmallString(g, "• Maximum compactness for compact factories.", x + 12, c2Y + 40, 0xFFAAAAAA);

            int r2Y = c2Y + 52;
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANITE_ENERGY_WIRE_ITEM), x + 12, r2Y, mouseX, mouseY, 0xFFFF3333);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.NAIAD_FLUID_PIPE_ITEM), x + 34, r2Y, mouseX, mouseY, 0xFF00E5FF);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.HERMES_ITEM_PIPE_ITEM), x + 56, r2Y, mouseX, mouseY, 0xFFFFAA00);
            g.drawString(this.font, "➔", x + 79, r2Y + 6, 0xFFFFCC00, false);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANITE_UNIVERSAL_CONDUIT_ITEM, 3), x + 92, r2Y, mouseX, mouseY, 0xFFFFD700);
            drawSmallString(g, "Triple Conduit (3×)", x + 116, r2Y + 6, 0xFFFFE066);

        } else if (this.currentSubTab == 3) {
            // Sub-tab 3: Dedicated Upgrades Tab!
            // Card 1: Hermes Filter Upgrade
            int c1Y = y + 4;
            int c1H = 50;
            g.fill(x + 6, c1Y, x + w - 6, c1Y + c1H, 0xFF1B1224);
            drawBorder(g, x + 6, c1Y, w - 12, c1H, 0xFF00E5FF);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.HERMES_FILTER_UPGRADE), x + 12, c1Y + 7, mouseX, mouseY, 0xFF00E5FF);
            drawSmallString(g, "Hermes Filter Upgrade", x + 36, c1Y + 6, 0xFF80EEFF);
            drawBadge(g, x + w - 10, c1Y + 4, "9 Ghost Slots", 0xFF100A16, 0xFF00E5FF, 0xFF80EEFF);

            drawSmallString(g, "• Right-click in hand to configure filter.", x + 36, c1Y + 18, 0xFFEEEEEE);
            drawSmallString(g, "• Toggle WHITELIST or BLACKLIST mode.", x + 12, c1Y + 28, 0xFF55FF77);
            drawSmallString(g, "• Fits Hermes Pipe & Volcanite Optic Fiber.", x + 12, c1Y + 38, 0xFFAAAAAA);

            // Card 2: Conduit Energy Upgrades (Tiers I - III)
            int c2Y = y + 57;
            int c2H = 51;
            g.fill(x + 6, c2Y, x + w - 6, c2Y + c2H, 0xFF1B1224);
            drawBorder(g, x + 6, c2Y, w - 12, c2H, 0xFFFFAA00);

            drawSmallString(g, "Energy Upgrades", x + 12, c2Y + 6, 0xFFFFCC44);
            drawBadge(g, x + w - 10, c2Y + 4, "Tiers I - III", 0xFF100A16, 0xFFFFAA00, 0xFFFFCC44);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANITE_BASIC_PIPE_ENERGY_UPGRADE), x + 12, c2Y + 18, mouseX, mouseY, 0xFF88DD66);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANITE_ADVANCED_PIPE_ENERGY_UPGRADE), x + 34, c2Y + 18, mouseX, mouseY, 0xFF00E5FF);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANITE_SUPREME_PIPE_ENERGY_UPGRADE), x + 56, c2Y + 18, mouseX, mouseY, 0xFFFF44AA);

            drawSmallString(g, "• Tier I Basic: +500 RF/t · +4 items/t", x + 80, c2Y + 19, 0xFFCCCCCC);
            drawSmallString(g, "• Tier II: +5,000 RF/t · +16 items/t", x + 80, c2Y + 29, 0xFF00E5FF);
            drawSmallString(g, "★ Tier III: +50k RF/t · +64 items/t", x + 80, c2Y + 39, 0xFFFF9944);

            // Card 3: Pipe Modules & Attachments
            int c3Y = y + 111;
            int c3H = 51;
            g.fill(x + 6, c3Y, x + w - 6, c3Y + c3H, 0xFF1B1224);
            drawBorder(g, x + 6, c3Y, w - 12, c3H, 0xFF55FF77);

            drawSmallString(g, "Universal Pipe Logistics Module", x + 12, c3Y + 6, 0xFF88FF99);
            drawBadge(g, x + w - 10, c3Y + 4, "Unified 3-in-1", 0xFF100A16, 0xFF55FF77, 0xFF88FF99);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANITE_LOGISTICS_MODULE), x + 16, c3Y + 18, mouseX, mouseY, 0xFF55FF77);

            drawSmallString(g, "• Unified module replaces separate Extract / Insert.", x + 46, c3Y + 16, 0xFF88FF99);
            drawSmallString(g, "• Toggle EXTRACT (Pull) or INSERT (Push) in UI.", x + 46, c3Y + 26, 0xFF00E5FF);
            drawSmallString(g, "★ Built-in 3-slot Whitelist/Blacklist ghost filter!", x + 46, c3Y + 36, 0xFFFFAA00);

        } else if (this.currentSubTab == 4) {
            // Sub-tab 4: Universal Pipe Concealment Facade
            int c1Y = y + 4;
            int c1H = 76;
            g.fill(x + 6, c1Y, x + w - 6, c1Y + c1H, 0xFF1B1224);
            drawBorder(g, x + 6, c1Y, w - 12, c1H, 0xFFFFAA00);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.PIPE_FACADE), x + 12, c1Y + 6, mouseX, mouseY, 0xFFFFAA00);
            drawSmallString(g, "Universal Pipe Facade", x + 36, c1Y + 6, 0xFFFFCC44);
            drawBadge(g, x + w - 10, c1Y + 4, "Any Block", 0xFF100A16, 0xFFFFAA00, 0xFFFFCC44);

            drawSmallString(g, "• Right-click any block in the world to copy its look.", x + 36, c1Y + 18, 0xFFEEEEEE);
            drawSmallString(g, "• Right-click pipe with Facade (or block in Offhand).", x + 12, c1Y + 30, 0xFFCCCCCC);
            drawSmallString(g, "• Shift + Right-Click with empty hand/wrench to remove.", x + 12, c1Y + 42, 0xFF88FF88);
            drawSmallString(g, "★ Glass Facades keep internal pipe & flow visible!", x + 12, c1Y + 56, 0xFF00E5FF);

            // Card 2: Universal Facade Crafting & Usage
            int c2Y = y + 84;
            int c2H = 76;
            g.fill(x + 6, c2Y, x + w - 6, c2Y + c2H, 0xFF1B1224);
            drawBorder(g, x + 6, c2Y, w - 12, c2H, 0xFF9966FF);

            drawSmallString(g, "Universal Facade Crafting & Usage", x + 12, c2Y + 6, 0xFFCC99FF);
            drawBadge(g, x + w - 10, c2Y + 4, "Yields 4×", 0xFF100A16, 0xFF9966FF, 0xFFCC99FF);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANIC_ASH), x + 16, c2Y + 24, mouseX, mouseY, 0xFFFFAA00);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANITE_CASING_ITEM), x + 44, c2Y + 24, mouseX, mouseY, 0xFFFFAA00);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.PIPE_FACADE, 4), x + 80, c2Y + 24, mouseX, mouseY, 0xFF00E5FF);

            int tX = x + 114;
            drawSmallString(g, "Recipe: 4× Ash + Casing", tX, c2Y + 24, 0xFFEEEEEE);
            drawSmallString(g, "• Hermes Item Pipes", tX, c2Y + 36, 0xFFFFAA00);
            drawSmallString(g, "• Naiad Fluid Pipes", tX, c2Y + 46, 0xFF00E5FF);
            drawSmallString(g, "• Volcanite Wires", tX, c2Y + 56, 0xFFFF6666);
        } else if (this.currentSubTab == 5) {
            // Sub-tab 5: Poseidon Abyssal Pump (2x2x2 End-Game Multiblock)
            int c1Y = y + 4;
            int c1H = 84;
            g.fill(x + 6, c1Y, x + w - 6, c1Y + c1H, 0xFF0E1A26);
            drawBorder(g, x + 6, c1Y, w - 12, c1H, 0xFF00D4FF);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.POSEIDON_ABYSSAL_PUMP_BLOCK), x + 12, c1Y + 6, mouseX, mouseY, 0xFF00D4FF);
            drawSmallString(g, "Poseidon Abyssal Pump (2×2×2)", x + 36, c1Y + 6, 0xFF67E8F9);
            drawBadge(g, x + w - 10, c1Y + 4, "16,000 mB/t", 0xFF09121B, 0xFF00D4FF, 0xFF73F2FF);

            drawSmallString(g, "• 1× Controller + 7× Abyssal Casing · oriented footprint (Holo/Auto-Build).", x + 36, c1Y + 18, 0xFFEEEEEE);
            drawSmallString(g, "• Infinite 2×2 Water (Zero Lag) & Area Drain (8–48r + Auto-Replace).", x + 12, c1Y + 31, 0xFFBAE6FD);
            drawSmallString(g, "• Speeds: 1k / 4k / 16k mB/t • Integrated Quantum Link & Auto-Eject.", x + 12, c1Y + 41, 0xFF93C5FD);

            int r1Y = c1Y + 56;
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY), x + 12, r1Y, mouseX, mouseY, 0xFFD946EF);
            drawSlotWithItem(g, new ItemStack(Items.HEART_OF_THE_SEA), x + 34, r1Y, mouseX, mouseY, 0xFF00E5FF);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.ASTRAL_VOLCANIC_CATALYST), x + 56, r1Y, mouseX, mouseY, 0xFFA855F7);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANITE_LAVA_PUMP_BLOCK), x + 78, r1Y, mouseX, mouseY, 0xFFFF8800);
            drawSlotWithItem(g, new ItemStack(Items.NETHERITE_INGOT), x + 100, r1Y, mouseX, mouseY, 0xFF8888AA);
            g.drawString(this.font, "➔", x + 123, r1Y + 6, 0xFF67E8F9, false);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.POSEIDON_ABYSSAL_PUMP_ITEM), x + 136, r1Y, mouseX, mouseY, 0xFF00D4FF);

            // Card 2: Poseidon Abyssal Casing
            int c2Y = y + 92;
            int c2H = 68;
            g.fill(x + 6, c2Y, x + w - 6, c2Y + c2H, 0xFF0E1A26);
            drawBorder(g, x + 6, c2Y, w - 12, c2H, 0xFF0284C7);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.POSEIDON_ABYSSAL_CASING_BLOCK), x + 12, c2Y + 6, mouseX, mouseY, 0xFF0284C7);
            drawSmallString(g, "Poseidon Abyssal Casing (7× Required)", x + 36, c2Y + 6, 0xFF38BDF8);
            drawBadge(g, x + w - 10, c2Y + 4, "Yields 4×", 0xFF09121B, 0xFF0284C7, 0xFFBAE6FD);

            drawSmallString(g, "• All 8 blocks accept RF input & auto-eject fluids to Naiad Pipes.", x + 36, c2Y + 19, 0xFFEEEEEE);
            int r2Y = c2Y + 38;
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANIC_ALLOY), x + 12, r2Y, mouseX, mouseY, 0xFFFF6600);
            drawSlotWithItem(g, new ItemStack(Blocks.PRISMARINE_BRICKS), x + 34, r2Y, mouseX, mouseY, 0xFF2DD4BF);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.SUPREME_NAIAD_FLUID_PIPE_BLOCK), x + 56, r2Y, mouseX, mouseY, 0xFF00E5FF);
            g.drawString(this.font, "➔", x + 79, r2Y + 6, 0xFF38BDF8, false);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.POSEIDON_ABYSSAL_CASING_ITEM, 4), x + 92, r2Y, mouseX, mouseY, 0xFF0284C7);
            drawSmallString(g, "Use Hologram / Auto-Build in GUI", x + 116, r2Y + 6, 0xFF88DD66);
        }
    }

    // ==================== CHAPTER 5: VOLCANIC CALDERA BIOME ====================
    private void renderDimensionChapter(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        if (this.currentSubTab == 0) {
            // Sub-tab 0: Overworld Volcanic Caldera Biome
            int c1Y = y + 4;
            int c1H = 74;
            g.fill(x + 6, c1Y, x + w - 6, c1Y + c1H, 0xFF1B1224);
            drawBorder(g, x + 6, c1Y, w - 12, c1H, 0xFFFF7700);

            drawSmallString(g, "1. Caldera Biome", x + 10, c1Y + 6, 0xFFFFCC44);
            drawBadge(g, x + w - 10, c1Y + 3, "Overworld", 0xFF100A16, 0xFFFF9900, 0xFFFFCC33);

            drawSlotWithItem(g, new ItemStack(Blocks.SMOOTH_BASALT), x + 12, c1Y + 18, mouseX, mouseY, 0xFFFF7700);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.TELLURIC_ASTROLABE), x + 12, c1Y + 44, mouseX, mouseY, 0xFFFFAA00);

            int t1X = x + 38;
            drawSmallString(g, "• Hot, arid volcanic plateaus & highlands.", t1X, c1Y + 20, 0xFFEEEEEE);
            drawSmallString(g, "• Terraced Smooth Basalt & Blackstone.", t1X, c1Y + 31, 0xFFDDDDDD);
            drawSmallString(g, "• Pure Overworld: No portals needed!", t1X, c1Y + 43, 0xFF55FF77);
            drawSmallString(g, "★ Astrolabe scans biomes up to 6.4 km.", t1X, c1Y + 55, 0xFFFFCC33);

            // Card 2: Volcanic Cones & Basalt Features
            int c2Y = y + 82;
            int c2H = 78;
            g.fill(x + 6, c2Y, x + w - 6, c2Y + c2H, 0xFF1B1224);
            drawBorder(g, x + 6, c2Y, w - 12, c2H, 0xFFFF5500);

            drawSmallString(g, "2. Volcanic Cones", x + 10, c2Y + 6, 0xFFFF8844);
            drawBadge(g, x + w - 10, c2Y + 3, "Surface Cones", 0xFF100A16, 0xFFFF5500, 0xFFFFAA66);

            drawSlotWithItem(g, new ItemStack(Blocks.MAGMA_BLOCK), x + 12, c2Y + 20, mouseX, mouseY, 0xFFFF5500);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANITE_ORE), x + 12, c2Y + 46, mouseX, mouseY, 0xFFFF3333);

            int t2X = x + 38;
            drawSmallString(g, "• Basalt vents with active magma craters.", t2X, c2Y + 21, 0xFFEEEEEE);
            drawSmallString(g, "• Rich Volcanite deposits encrust slopes.", t2X, c2Y + 33, 0xFFFFCC66);
            drawSmallString(g, "• Mine Volcanite directly on the surface!", t2X, c2Y + 46, 0xFF55FF77);
            drawSmallString(g, "• Safely scoop lava from crater vents.", t2X, c2Y + 58, 0xFFFFAA33);
        } else if (this.currentSubTab == 1) {
            // Sub-tab 1: Volcanite Ore & Mining
            int c1Y = y + 4;
            int c1H = 74;
            g.fill(x + 6, c1Y, x + w - 6, c1Y + c1H, 0xFF1B1224);
            drawBorder(g, x + 6, c1Y, w - 12, c1H, 0xFF55DD66);

            drawSmallString(g, "1. Volcanite Mining", x + 10, c1Y + 6, 0xFF88FF88);
            drawBadge(g, x + w - 10, c1Y + 3, "Iron Pickaxe+", 0xFF100A16, 0xFF55DD66, 0xFFAAEEAA);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANITE_ORE), x + 12, c1Y + 18, mouseX, mouseY, 0xFFFF5500);
            drawSlotWithItem(g, new ItemStack(Items.IRON_PICKAXE), x + 12, c1Y + 44, mouseX, mouseY, 0xFFD8D8D8);

            int t1X = x + 38;
            drawSmallString(g, "★ Mines with Iron Pickaxe+ (no diamond).", t1X, c1Y + 20, 0xFF55FF77);
            drawSmallString(g, "★ Low hardness: Mines fast and easily.", t1X, c1Y + 32, 0xFFEEEEEE);
            drawSmallString(g, "★ Drops Volcanite crystals (Fortune boosted).", t1X, c1Y + 44, 0xFFFFCC44);
            drawSmallString(g, "★ Silk Touch drops raw Ore intact.", t1X, c1Y + 56, 0xFF00E5FF);

            // Card 2: Spawning Locations & Thermal Veins
            int c2Y = y + 82;
            int c2H = 78;
            g.fill(x + 6, c2Y, x + w - 6, c2Y + c2H, 0xFF1B1224);
            drawBorder(g, x + 6, c2Y, w - 12, c2H, 0xFF00E5FF);

            drawSmallString(g, "2. Vein Locations", x + 10, c2Y + 6, 0xFF00E5FF);
            drawBadge(g, x + w - 10, c2Y + 3, "Surface + Deep", 0xFF100A16, 0xFF00E5FF, 0xFF88FFFF);

            drawSlotWithItem(g, new ItemStack(Items.LAVA_BUCKET), x + 12, c2Y + 20, mouseX, mouseY, 0xFFFF7700);
            drawSlotWithItem(g, new ItemStack(Blocks.OBSIDIAN), x + 12, c2Y + 46, mouseX, mouseY, 0xFF553377);

            int t2X = x + 38;
            drawSmallString(g, "• Surface: Volcanic Cones in Calderas.", t2X, c2Y + 21, 0xFFEEEEEE);
            drawSmallString(g, "• Underground: Thermal veins near Lava.", t2X, c2Y + 33, 0xFFFFCC66);
            drawSmallString(g, "• Deepslate: Veins between Y: -64 & -10.", t2X, c2Y + 46, 0xFFDDDDDD);
            drawSmallString(g, "• Nether: Abundant near lava seas & deltas.", t2X, c2Y + 58, 0xFFFF9933);
        } else {
            // Sub-tab 2: Caldera Ecosystem & Living Trees
            // Card 1: Living Ore Trees
            int c1Y = y + 4;
            int c1H = 74;
            g.fill(x + 6, c1Y, x + w - 6, c1Y + c1H, 0xFF1B1224);
            drawBorder(g, x + 6, c1Y, w - 12, c1H, 0xFFFFCC00);

            drawSmallString(g, "1. Living Ore Trees", x + 10, c1Y + 6, 0xFFFFDD44);
            drawBadge(g, x + w - 10, c1Y + 3, "Renewable", 0xFF100A16, 0xFFFFCC00, 0xFFFFEE66);

            int sY = c1Y + 18;
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.MIDAS_GOLD_SAPLING), x + 12, sY, mouseX, mouseY, 0xFFFFDD44);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.ARES_IRON_SAPLING), x + 34, sY, mouseX, mouseY, 0xFFD8D8D8);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.HERMES_COPPER_SAPLING), x + 56, sY, mouseX, mouseY, 0xFFFF8844);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.HEPHAESTUS_VOLCANITE_SAPLING), x + 78, sY, mouseX, mouseY, 0xFFFF3333);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.TARTAREAN_NETHERITE_SAPLING), x + 100, sY, mouseX, mouseY, 0xFF8844FF);

            drawSmallString(g, "• 5 species: Iron, Gold, Copper, Volcanite, Netherite.", x + 12, c1Y + 41, 0xFFEEEEEE);
            drawSmallString(g, "• Harvest metal nuggets & hybrid saplings.", x + 12, c1Y + 51, 0xFFAAAAAA);
            drawSmallString(g, "★ Breed in Arborist or with Grafting Knife!", x + 12, c1Y + 62, 0xFFFF9944);

            // Card 2: Caldera Wildlife & Cinder Stalkers
            int c2Y = y + 82;
            int c2H = 78;
            g.fill(x + 6, c2Y, x + w - 6, c2Y + c2H, 0xFF1B1224);
            drawBorder(g, x + 6, c2Y, w - 12, c2H, 0xFFCC33FF);

            drawSmallString(g, "2. Cinder Stalkers", x + 10, c2Y + 6, 0xFFDD66FF);
            drawBadge(g, x + w - 10, c2Y + 3, "Caldera Fauna", 0xFF100A16, 0xFFCC33FF, 0xFFEE88FF);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.CINDER_CORE), x + 12, c2Y + 20, mouseX, mouseY, 0xFFCC33FF);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.HEPHAESTEAN_CHESTPLATE), x + 12, c2Y + 46, mouseX, mouseY, 0xFFFF6600);

            int t2X = x + 38;
            drawSmallString(g, "• Pack hunters roaming basalt plateaus.", t2X, c2Y + 21, 0xFFEEEEEE);
            drawSmallString(g, "• Slay for Cinder Cores (furnace fuel/catalysts).", t2X, c2Y + 33, 0xFFFFAA77);
            drawSmallString(g, "• Equip Volcanic armor to resist fire damage.", t2X, c2Y + 46, 0xFFFFCC66);
            drawSmallString(g, "• Contained spawns keep encounters fair.", t2X, c2Y + 58, 0xFF55FF77);
        }
    }

    private void renderMythologyChapter(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        if (this.currentSubTab == 0) {
            // Sub-tab 0: Olympian Altar & Astrape Ritual Obelisks
            int c1Y = y + 2;
            int c1H = 92;
            g.fill(x + 6, c1Y, x + w - 6, c1Y + c1H, 0xFF1B1224);
            drawBorder(g, x + 6, c1Y, w - 12, c1H, 0xFFFFB300);

            drawSmallString(g, "1. Olympian Altar · Recommended Cross", x + 10, c1Y + 5, 0xFFFFD54F);
            drawBadge(g, x + w - 10, c1Y + 3, "4-Block Scan", 0xFF100A16, 0xFFFFB300, 0xFFFFE082);

            // Multiblock visual top-down representation:
            // Background box for schematic
            g.fill(x + 10, c1Y + 16, x + 88, c1Y + 88, 0xFF120B1C);
            drawBorder(g, x + 10, c1Y + 16, 78, 72, 0xFF4A2800);

            int cx = x + 39;
            int cy = c1Y + 42;

            // Cardinal direction labels
            g.drawString(this.font, "§bN", cx + 6, c1Y + 18, 0xFF00E5FF, false);
            g.drawString(this.font, "§bW", x + 12, cy + 6, 0xFF00E5FF, false);
            g.drawString(this.font, "§bE", x + 76, cy + 6, 0xFF00E5FF, false);
            g.drawString(this.font, "§bS", cx + 6, c1Y + 76, 0xFF00E5FF, false);

            // Dotted beam lines connecting obelisks to center altar
            g.fill(cx + 9, c1Y + 33, cx + 11, cy, 0xFF00E5FF);
            g.fill(cx + 9, cy + 20, cx + 11, c1Y + 68, 0xFF00E5FF);
            g.fill(x + 29, cy + 9, cx, cy + 11, 0xFF00E5FF);
            g.fill(cx + 20, cy + 9, x + 69, cy + 11, 0xFF00E5FF);

            // North Obelisk (radius 3)
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.ASTRAPE_RITUAL_OBELISK_BLOCK), cx, c1Y + 18, mouseX, mouseY, 0xFF00E5FF);
            // South Obelisk (radius 3)
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.ASTRAPE_RITUAL_OBELISK_BLOCK), cx, c1Y + 66, mouseX, mouseY, 0xFF00E5FF);
            // West Obelisk (radius 3)
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.ASTRAPE_RITUAL_OBELISK_BLOCK), x + 18, cy, mouseX, mouseY, 0xFF00E5FF);
            // East Obelisk (radius 3)
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.ASTRAPE_RITUAL_OBELISK_BLOCK), x + 60, cy, mouseX, mouseY, 0xFF00E5FF);
            // Center Olympian Altar (0, 0)
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.OLYMPIAN_ALTAR_BLOCK), cx, cy, mouseX, mouseY, 0xFFFFB300);

            // Right text instructions
            int tx = x + 92;
            int maxTw = w - 100;
            drawSmallString(g, "• Center: Olympian Altar (0,0)", tx, c1Y + 17, 0xFFFFD54F, maxTw);
            drawSmallString(g, "• 4+ Obelisks within X/Z ±4", tx, c1Y + 29, 0xFF80EEFF, maxTw);
            drawSmallString(g, "• Example: N/S/E/W at distance 3", tx, c1Y + 41, 0xFF55FF77, maxTw);
            drawSmallString(g, "• Y offset ±1; diagonals accepted", tx, c1Y + 53, 0xFFFFCC44, maxTw);
            drawSmallString(g, "• Scroll on Obelisk, Item on Altar", tx, c1Y + 65, 0xFFEEEEEE, maxTw);
            drawSmallString(g, "★ Shift + empty-hand right-click retrieves item (idle)", tx, c1Y + 77, 0xFFFFCC44, maxTw);

            int c2Y = y + 96;
            int c2H = h - 98;
            g.fill(x + 6, c2Y, x + w - 6, c2Y + c2H, 0xFF1B1224);
            drawBorder(g, x + 6, c2Y, w - 12, c2H, 0xFFA855F7);

            drawSmallString(g, "2. The 7 Divine Pacts (Combat Powers)", x + 10, c2Y + 5, 0xFFC084FC);
            drawBadge(g, x + w - 10, c2Y + 3, "Limit 1/Weapon", 0xFF100A16, 0xFFA855F7, 0xFFE9D5FF);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.SCROLL_OF_ZEUS), x + 12, c2Y + 18, mouseX, mouseY, 0xFFA855F7);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANIC_PAPYRUS), x + 12, c2Y + 41, mouseX, mouseY, 0xFFFF9900);

            int t2X = x + 38;
            int maxPactW = w - 46;
            drawSmallString(g, "• Zeus: Chain Lightning arcs (4.0 dmg to 3 foes)", t2X, c2Y + 16, 0xFFEEEEEE, maxPactW);
            drawSmallString(g, "• Apollo: Celestial fire (3.5 true dmg ignores armor)", t2X, c2Y + 26, 0xFFFDE68A, maxPactW);
            drawSmallString(g, "• Artemis: +40% speed/crit | Athena: 35% damage nullify", t2X, c2Y + 36, 0xFF80EEFF, maxPactW);
            drawSmallString(g, "• Hephaestus: Auto-Smelt | Poseidon: +6.0 water dmg", t2X, c2Y + 46, 0xFF55FF77, maxPactW);
            drawSmallString(g, "• Thanatos: Soul Siphon (+2.0 hearts heal on kill)", t2X, c2Y + 56, 0xFFFFCC44, maxPactW);

        } else if (this.currentSubTab == 1) {
            // Sub-tab 1: Divine Relics (Bow of Artemis & Aegis of Athena)
            int c1Y = y + 4;
            int c1H = 74;
            g.fill(x + 6, c1Y, x + w - 6, c1Y + c1H, 0xFF1B1224);
            drawBorder(g, x + 6, c1Y, w - 12, c1H, 0xFFC084FC);

            drawSmallString(g, "1. Bow of Artemis", x + 10, c1Y + 6, 0xFFE879F9);
            drawBadge(g, x + w - 10, c1Y + 3, "Lunar Hunt", 0xFF100A16, 0xFFC084FC, 0xFFF5D0FE);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.BOW_OF_ARTEMIS), x + 12, c1Y + 20, mouseX, mouseY, 0xFFC084FC);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.ARTEMIS_SILVER_FRUIT), x + 12, c1Y + 46, mouseX, mouseY, 0xFFE879F9);

            int t1X = x + 38;
            drawSmallString(g, "• Forged with Silver Ingots & Artemis Silver Fruit.", t1X, c1Y + 21, 0xFFEEEEEE);
            drawSmallString(g, "• Lunar Infusion: +3.0 Base Damage & +40% arrow speed.", t1X, c1Y + 33, 0xFFF5D0FE);
            drawSmallString(g, "• Infinite Ammo: carried Silver Fruit grants endless arrows!", t1X, c1Y + 45, 0xFF55FF77);
            drawSmallString(g, "★ Piercing moonlight damage against demons & undead.", t1X, c1Y + 57, 0xFFFFDD44);

            int c2Y = y + 82;
            int c2H = 78;
            g.fill(x + 6, c2Y, x + w - 6, c2Y + c2H, 0xFF1B1224);
            drawBorder(g, x + 6, c2Y, w - 12, c2H, 0xFFF59E0B);

            drawSmallString(g, "2. Aegis of Athena", x + 10, c2Y + 6, 0xFFFBBF24);
            drawBadge(g, x + w - 10, c2Y + 3, "Divine Shield", 0xFF100A16, 0xFFF59E0B, 0xFFFEF08A);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.AEGIS_OF_ATHENA), x + 12, c2Y + 20, mouseX, mouseY, 0xFFF59E0B);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.CINDER_CORE), x + 12, c2Y + 46, mouseX, mouseY, 0xFFDC2626);

            int t2X = x + 38;
            drawSmallString(g, "• Volcanite shield forged with Plates & Cinder Core.", t2X, c2Y + 21, 0xFFEEEEEE);
            drawSmallString(g, "• Projectile Reflection: deflects arrows & spells back!", t2X, c2Y + 33, 0xFFFDE68A);
            drawSmallString(g, "• Volcanic Shockwave: repels & ignites melee attackers.", t2X, c2Y + 46, 0xFF55FF77);
            drawSmallString(g, "★ Durability: 1,600 & completely fire resistant.", t2X, c2Y + 58, 0xFFFFCC44);

        } else if (this.currentSubTab == 2) {
            // Sub-tab 2: Apollo Helio-Concentrator
            int c1Y = y + 4;
            int c1H = 74;
            g.fill(x + 6, c1Y, x + w - 6, c1Y + c1H, 0xFF1B1224);
            drawBorder(g, x + 6, c1Y, w - 12, c1H, 0xFFF59E0B);

            drawSmallString(g, "1. Apollo Helio-Concentrator", x + 10, c1Y + 6, 0xFFFBBF24);
            drawBadge(g, x + w - 10, c1Y + 3, "Solar Power", 0xFF100A16, 0xFFF59E0B, 0xFFFDE68A);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.APOLLO_SOLAR_GENERATOR_ITEM), x + 12, c1Y + 20, mouseX, mouseY, 0xFFF59E0B);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.MIDAS_GOLD_FRUIT), x + 12, c1Y + 46, mouseX, mouseY, 0xFFFFDD44);

            int t1X = x + 38;
            drawSmallString(g, "• Harnesses celestial sunlight into high-density RF energy.", t1X, c1Y + 21, 0xFFEEEEEE);
            drawSmallString(g, "• Generates up to 140 RF/t under direct sky (boosted in hot biomes).", t1X, c1Y + 33, 0xFFFDE68A);
            drawSmallString(g, "• 300,000 RF internal buffer with automatic power export.", t1X, c1Y + 45, 0xFF55FF77);
            drawSmallString(g, "★ Zero fuel cost: clean, continuous daylight power.", t1X, c1Y + 57, 0xFFFFDD44);

            int c2Y = y + 82;
            int c2H = 78;
            g.fill(x + 6, c2Y, x + w - 6, c2Y + c2H, 0xFF1B1224);
            drawBorder(g, x + 6, c2Y, w - 12, c2H, 0xFF10B981);

            drawSmallString(g, "2. Solar Smelting Ray", x + 10, c2Y + 6, 0xFF34D399);
            drawBadge(g, x + w - 10, c2Y + 3, "Fuel-Free", 0xFF100A16, 0xFF10B981, 0xFF6EE7B7);

            drawSlotWithItem(g, new ItemStack(Items.RAW_IRON), x + 12, c2Y + 20, mouseX, mouseY, 0xFFDDDDDD);
            drawSlotWithItem(g, new ItemStack(Items.IRON_INGOT), x + 12, c2Y + 46, mouseX, mouseY, 0xFFEEEEEE);

            int t2X = x + 38;
            drawSmallString(g, "• Integrated optical focal point inside solar dish.", t2X, c2Y + 21, 0xFFEEEEEE);
            drawSmallString(g, "• Smelts raw ores and metals using concentrated rays.", t2X, c2Y + 33, 0xFF6EE7B7);
            drawSmallString(g, "• Requires only open sunlight or buffered solar power.", t2X, c2Y + 46, 0xFF55FF77);
            drawSmallString(g, "★ Fully automated with hopper, pipes & crafters!", t2X, c2Y + 58, 0xFFFFCC44);

        } else if (this.currentSubTab == 3) {
            // Sub-tab 3: Dionysian Distillery & Beverages
            int c1Y = y + 4;
            int c1H = 74;
            g.fill(x + 6, c1Y, x + w - 6, c1Y + c1H, 0xFF1B1224);
            drawBorder(g, x + 6, c1Y, w - 12, c1H, 0xFFD946EF);

            drawSmallString(g, "1. Dionysian Distillery", x + 10, c1Y + 6, 0xFFF0ABFC);
            drawBadge(g, x + w - 10, c1Y + 3, "Spirits & Mash", 0xFF100A16, 0xFFD946EF, 0xFFF5D0FE);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.DIONYSIAN_DISTILLERY_BLOCK), x + 12, c1Y + 20, mouseX, mouseY, 0xFFD946EF);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANIC_TUICA), x + 12, c1Y + 46, mouseX, mouseY, 0xFFF59E0B);

            int t1X = x + 38;
            drawSmallString(g, "• Alchemical still powered by Volcanic Ash or Coal.", t1X, c1Y + 21, 0xFFEEEEEE);
            drawSmallString(g, "• Pomace/Fruits -> Volcanic Spirit (+Haste, Speed, 100k RF).", t1X, c1Y + 33, 0xFFFCD34D);
            drawSmallString(g, "• Apples -> Volcanic Apple Cider (+Absorption, 60k RF).", t1X, c1Y + 45, 0xFF6EE7B7);
            drawSmallString(g, "• Concentrates mineral nuggets (Gold/Iron/Copper) as byproduct.", t1X, c1Y + 57, 0xFFDDDDDD);

            int c2Y = y + 82;
            int c2H = 78;
            g.fill(x + 6, c2Y, x + w - 6, c2Y + c2H, 0xFF1B1224);
            drawBorder(g, x + 6, c2Y, w - 12, c2H, 0xFFF59E0B);

            drawSmallString(g, "2. Aging Barrel & Reserve", x + 10, c2Y + 6, 0xFFFBBF24);
            drawBadge(g, x + w - 10, c2Y + 3, "Vintage", 0xFF100A16, 0xFFF59E0B, 0xFFFDE68A);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.DIONYSIAN_AGING_BARREL_BLOCK), x + 12, c2Y + 20, mouseX, mouseY, 0xFFF59E0B);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VINTAGE_VOLCANIC_TUICA), x + 12, c2Y + 46, mouseX, mouseY, 0xFF8844FF);

            int t2X = x + 38;
            drawSmallString(g, "• Place bottles in Aging Barrel over time.", t2X, c2Y + 21, 0xFFEEEEEE);
            drawSmallString(g, "• Ages into Vintage & Grand Reserve spirits.", t2X, c2Y + 33, 0xFFFCD34D);
            drawSmallString(g, "• Volcanic Spirit 1L: large demi-john with 4 servings.", t2X, c2Y + 46, 0xFF55FF77);
            drawSmallString(g, "• Volcanic Ash byproduct fuels furnaces & recipes.", t2X, c2Y + 58, 0xFFAAAAAA);

        } else if (this.currentSubTab == 4) {
            // Sub-tab 4: Wireless Power Pylons & Charging Plate
            int c1Y = y + 4;
            int c1H = 74;
            g.fill(x + 6, c1Y, x + w - 6, c1Y + c1H, 0xFF1B1224);
            drawBorder(g, x + 6, c1Y, w - 12, c1H, 0xFF3B82F6);

            drawSmallString(g, "1. Astrape Energy Pylons", x + 10, c1Y + 6, 0xFF60A5FA);
            drawBadge(g, x + w - 10, c1Y + 3, "Wireless Power", 0xFF100A16, 0xFF3B82F6, 0xFF93C5FD);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.ASTRAPE_ENERGY_PYLON_BLOCK), x + 12, c1Y + 20, mouseX, mouseY, 0xFF3B82F6);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.HEPHAESTEAN_RESONATOR), x + 12, c1Y + 46, mouseX, mouseY, 0xFFFFCC00);

            int t1X = x + 38;
            drawSmallString(g, "• Beam power wirelessly across your entire base.", t1X, c1Y + 21, 0xFFEEEEEE);
            drawSmallString(g, "• Link using Hephaestean Resonator (Shift+RMB).", t1X, c1Y + 33, 0xFF93C5FD);
            drawSmallString(g, "• Massive transfer rate: up to 50,000 RF/t per link.", t1X, c1Y + 45, 0xFF55FF77);
            drawSmallString(g, "• Can beam to batteries, cores, generators & pylons.", t1X, c1Y + 57, 0xFFFFDD44);

            int c2Y = y + 82;
            int c2H = 78;
            g.fill(x + 6, c2Y, x + w - 6, c2Y + c2H, 0xFF1B1224);
            drawBorder(g, x + 6, c2Y, w - 12, c2H, 0xFFEF4444);

            drawSmallString(g, "2. Hephaestean Charging Plate", x + 10, c2Y + 6, 0xFFF87171);
            drawBadge(g, x + w - 10, c2Y + 3, "Inductive Pad", 0xFF100A16, 0xFFEF4444, 0xFFFCA5A5);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.HEPHAESTEAN_CHARGING_PLATE_BLOCK), x + 12, c2Y + 20, mouseX, mouseY, 0xFFEF4444);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANITE_PORTABLE_BATTERY), x + 12, c2Y + 46, mouseX, mouseY, 0xFFFFCC00);

            int t2X = x + 38;
            drawSmallString(g, "• Inductive floor plate: step on it to recharge.", t2X, c2Y + 21, 0xFFEEEEEE);
            drawSmallString(g, "• Rapidly powers backpacks, batteries, armor & tools.", t2X, c2Y + 33, 0xFF55FF77);
            drawSmallString(g, "• Shift+RMB with battery toggles auto-charge mode.", t2X, c2Y + 46, 0xFFFFAA00);
            drawSmallString(g, "★ Step onto the plate to top up all equipment in seconds!", t2X, c2Y + 58, 0xFFFFDD44);

        } else {
            // Sub-tab 5: Thanatos Executioner & Mob Lure
            int c1Y = y + 4;
            int c1H = 74;
            g.fill(x + 6, c1Y, x + w - 6, c1Y + c1H, 0xFF1B1224);
            drawBorder(g, x + 6, c1Y, w - 12, c1H, 0xFFDC2626);

            drawSmallString(g, "1. Thanatos Executioner", x + 10, c1Y + 6, 0xFFFF4444);
            drawBadge(g, x + w - 10, c1Y + 3, "Mob Slayer", 0xFF100A16, 0xFFDC2626, 0xFFFF6666);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.THANATOS_EXECUTIONER_BLOCK), x + 12, c1Y + 20, mouseX, mouseY, 0xFFDC2626);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.REINFORCED_TINTED_GLASS_BLOCK), x + 12, c1Y + 46, mouseX, mouseY, 0xFF8844FF);

            int t1X = x + 38;
            drawSmallString(g, "• Builds a 5x5 reinforced chamber facing forward.", t1X, c1Y + 21, 0xFFEEEEEE);
            drawSmallString(g, "• Reinforced Tinted Glass: 1200 blast res & darkens.", t1X, c1Y + 33, 0xFFCC88FF);
            drawSmallString(g, "• Mobs CANNOT see player through this glass!", t1X, c1Y + 45, 0xFF55FF77);
            drawSmallString(g, "★ [☠ Lure]: Summons Cinder Stalkers & Tartarean Hounds!", t1X, c1Y + 57, 0xFFFFDD44);

            int c2Y = y + 82;
            int c2H = 78;
            g.fill(x + 6, c2Y, x + w - 6, c2Y + c2H, 0xFF1B1224);
            drawBorder(g, x + 6, c2Y, w - 12, c2H, 0xFF10B981);

            drawSmallString(g, "2. Soul XP & Automation", x + 10, c2Y + 6, 0xFF34D399);
            drawBadge(g, x + w - 10, c2Y + 3, "Auto-Export", 0xFF100A16, 0xFF10B981, 0xFF6EE7B7);

            drawSlotWithItem(g, new ItemStack(Items.NETHERITE_SWORD), x + 12, c2Y + 20, mouseX, mouseY, 0xFFFFCC00);
            drawSlotWithItem(g, new ItemStack(Items.EXPERIENCE_BOTTLE), x + 12, c2Y + 46, mouseX, mouseY, 0xFF34D399);

            int t2X = x + 38;
            drawSmallString(g, "• Sword slot: applies strikes (consumes RF/durability).", t2X, c2Y + 21, 0xFFEEEEEE);
            drawSmallString(g, "• 9-slot loot buffer vacuums all drops inside chamber.", t2X, c2Y + 33, 0xFFDDDDDD);
            drawSmallString(g, "• [➔Chest]: Auto-pushes loot to adjacent chests/pipes.", t2X, c2Y + 46, 0xFF55FF77);
            drawSmallString(g, "★ [🧪 XP]: Click button in GUI to claim stored levels!", t2X, c2Y + 58, 0xFFFFCC44);
        }
    }

    private void renderCrafterSubTab(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY) {
        int c1Y = y + 4;
        int c1H = 74;
        g.fill(x + 6, c1Y, x + w - 6, c1Y + c1H, 0xFF1B1224);
        drawBorder(g, x + 6, c1Y, w - 12, c1H, 0xFF0284C7);

        drawSmallString(g, "1. Crafter Interface", x + 10, c1Y + 6, 0xFF38BDF8);
        drawBadge(g, x + w - 10, c1Y + 3, "Auto-Crafting", 0xFF100A16, 0xFF0284C7, 0xFF7DD3FC);

        drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANITE_CRAFTER_INTERFACE_BLOCK), x + 12, c1Y + 20, mouseX, mouseY, 0xFF0284C7);
        drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANITE_BLUEPRINT), x + 12, c1Y + 46, mouseX, mouseY, 0xFF38BDF8);

        int t1X = x + 38;
        drawSmallString(g, "• Connects directly to Storage Network cables/bus.", t1X, c1Y + 21, 0xFFEEEEEE);
        drawSmallString(g, "• Insert Volcanite Blueprint to encode 3x3 recipes.", t1X, c1Y + 33, 0xFF7DD3FC);
        drawSmallString(g, "• Automated crafting pulls materials right from network.", t1X, c1Y + 45, 0xFF55FF77);
        drawSmallString(g, "★ Stock Limits: 64, 256, 1024 or infinite production.", t1X, c1Y + 57, 0xFFFFDD44);

        int c2Y = y + 82;
        int c2H = 78;
        g.fill(x + 6, c2Y, x + w - 6, c2Y + c2H, 0xFF1B1224);
        drawBorder(g, x + 6, c2Y, w - 12, c2H, 0xFFEAB308);

        drawSmallString(g, "2. REI & Terminal Sync", x + 10, c2Y + 6, 0xFFFACC15);
        drawBadge(g, x + w - 10, c2Y + 3, "Integration", 0xFF100A16, 0xFFEAB308, 0xFFFEF08A);

        drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANITE_STORAGE_CONTROLLER_BLOCK), x + 12, c2Y + 20, mouseX, mouseY, 0xFFEAB308);
        drawSlotWithItem(g, new ItemStack(Items.CRAFTING_TABLE), x + 12, c2Y + 46, mouseX, mouseY, 0xFF38BDF8);

        int t2X = x + 38;
        drawSmallString(g, "• Full REI Support: Click [+] in recipe view to fill grid.", t2X, c2Y + 21, 0xFFEEEEEE);
        drawSmallString(g, "• [↻Sync]: Auto-refills 3x3 inputs as you craft.", t2X, c2Y + 33, 0xFF55FF77);
        drawSmallString(g, "• [➔Net]: Clears crafting grid back to central storage.", t2X, c2Y + 46, 0xFFDDDDDD);
        drawSmallString(g, "★ [📦 Terminal]: Switch between Terminal and Crafter.", t2X, c2Y + 58, 0xFFFFCC44);
    }

    // ==================== CHAPTER 7: COLLIDER & TELEPORTATION ====================
    private void renderColliderChapter(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        if (this.currentSubTab == 0) {
            drawSmallString(g, "⚛ Olympian Hadron Collider (5×5)", x + 10, y + 6, 0xFF73F2FF);
            drawBadge(g, x + w - 8, y + 4, "100M RF", 0xFF100A16, 0xFF00E5FF, 0xFF73F2FF);

            ItemStack ctrl = new ItemStack(ObsidiumMod.OLYMPIAN_HADRON_COLLIDER_BLOCK);
            ItemStack coil = new ItemStack(ObsidiumMod.SYNCHROTRON_RING_COIL_BLOCK);
            ItemStack energyPort = new ItemStack(ObsidiumMod.HADRON_ENERGY_PORT_BLOCK);
            ItemStack itemPort = new ItemStack(ObsidiumMod.HADRON_ITEM_PORT_BLOCK);
            ItemStack fluidPort = new ItemStack(ObsidiumMod.HADRON_FLUID_PORT_BLOCK);

            // Top-down 5x5 ring blueprint
            int gx = x + 12;
            int gy = y + 22;
            int step = 20;
            for (int r = 0; r < 5; r++) {
                for (int c = 0; c < 5; c++) {
                    int sx = gx + c * step;
                    int sy = gy + r * step;
                    boolean edge = (r == 0 || r == 4 || c == 0 || c == 4);
                    if (!edge) {
                        g.fill(sx + 1, sy + 1, sx + 19, sy + 19, 0xFF090710);
                        drawBorder(g, sx + 1, sy + 1, 18, 18, 0xFF1D1528);
                        continue;
                    }
                    if (r == 4 && c == 2) {
                        drawSlotWithItem(g, ctrl, sx, sy, mouseX, mouseY, 0xFF00E5FF);
                    } else if (r == 0 && c == 2) {
                        drawSlotWithItem(g, energyPort, sx, sy, mouseX, mouseY, 0xFFFFAA00);
                    } else if (r == 2 && c == 0) {
                        drawSlotWithItem(g, itemPort, sx, sy, mouseX, mouseY, 0xFF55FF77);
                    } else if (r == 2 && c == 4) {
                        drawSlotWithItem(g, fluidPort, sx, sy, mouseX, mouseY, 0xFF00CCFF);
                    } else {
                        drawSlotWithItem(g, coil, sx, sy, mouseX, mouseY, 0xFF3377AA);
                    }
                }
            }

            // Right specs
            int rx = x + 118;
            int maxW = w - 124;
            drawSmallString(g, "Ring Assembly:", rx, y + 23, 0xFFFFCC44, maxW);
            drawSmallString(g, "• 1× Collider Controller", rx, y + 35, 0xFF73F2FF, maxW);
            drawSmallString(g, "• 12× Ring Coils (min 9)", rx, y + 46, 0xFFEEEEEE, maxW);
            drawSmallString(g, "• Req: 1 Energy, 1 Item, 1 Fluid", rx, y + 57, 0xFFFFB74D, maxW);
            drawSmallString(g, "• 3×3 inner ring = Air", rx, y + 68, 0xFFBBBBCC, maxW);

            drawSmallString(g, "Formed Behavior:", rx, y + 83, 0xFF55FF77, maxW);
            drawSmallString(g, "• Whole ring transforms & glows", rx, y + 95, 0xFFCCCCCC, maxW);
            drawSmallString(g, "• [Auto-Build] & [Holo 5×5]", rx, y + 106, 0xFFCCCCCC, maxW);
            drawSmallString(g, "• Up to 5M RF/t Port I/O", rx, y + 117, 0xFF73F2FF, maxW);

            // Footer
            g.fill(x + 8, y + 128, x + w - 8, y + h - 6, 0xFF1B1224);
            drawBorder(g, x + 8, y + 128, w - 16, h - 134, 0xFF00AACC);
            drawSmallString(g, "Item Ports & Coils auto-output into Modular Chests & pipes!", x + 13, y + 134, 0xFFE6FAFF, w - 26);
            drawSmallString(g, "☢ Danger: Entering the 3×3 core deals heavy radiation damage!", x + 13, y + 145, 0xFFFF6677, w - 26);
        } else if (this.currentSubTab == 1) {
            // Card 1: Thermal & Cryo States
            int c1Y = y + 4;
            int c1H = 72;
            g.fill(x + 6, c1Y, x + w - 6, c1Y + c1H, 0xFF131D2B);
            drawBorder(g, x + 6, c1Y, w - 12, c1H, 0xFF00CCFF);
            drawSmallString(g, "1. Cryo Cooling & Quench", x + 10, c1Y + 5, 0xFF73F2FF);
            drawBadge(g, x + w - 10, c1Y + 3, "<120 K Cryo", 0xFF0B1420, 0xFF00CCFF, 0xFF73F2FF);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.BIO_ETHANOL_BUCKET), x + 11, c1Y + 19, mouseX, mouseY, 0xFF00E5FF);
            drawSlotWithItem(g, new ItemStack(Items.BLUE_ICE), x + 11, c1Y + 44, mouseX, mouseY, 0xFF55BBFF);

            int t1X = x + 36;
            int maxW = w - 44;
            drawSmallString(g, "• <120 K Superconducting: 1.5× RF & 2.5× Accel!", t1X, c1Y + 20, 0xFF66F2FF, maxW);
            drawSmallString(g, "• >=650 K Thermal Quench halts acceleration (Max 800 K)", t1X, c1Y + 31, 0xFFFF6677, maxW);
            drawSmallString(g, "• Coolants: Bio-Ethanol (2× Cryo), Water, Ice / Blue Ice", t1X, c1Y + 43, 0xFFB8E2FF, maxW);
            drawSmallString(g, "• SYNCHROTRON locks max speed for steady RF generation", t1X, c1Y + 54, 0xFFCCCCCC, maxW);

            // Card 2: Fuels & Multipliers
            int c2Y = y + 80;
            int c2H = h - 86;
            g.fill(x + 6, c2Y, x + w - 6, c2Y + c2H, 0xFF1E1420);
            drawBorder(g, x + 6, c2Y, w - 12, c2H, 0xFFFF7700);
            drawSmallString(g, "2. Plasma Fuel Multipliers", x + 10, c2Y + 5, 0xFFFFB74D);
            drawBadge(g, x + w - 10, c2Y + 3, "Up to 2.5×", 0xFF100A16, 0xFFFF7700, 0xFFFFCC44);

            drawSlotWithItem(g, new ItemStack(ObsidiumMod.PYRO_DIESEL_BUCKET), x + 11, c2Y + 20, mouseX, mouseY, 0xFFFF3344);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.PYROCLASTIC_DISTILLATE_BUCKET), x + 33, c2Y + 20, mouseX, mouseY, 0xFFD96BFF);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.BIODIESEL_BUCKET), x + 11, c2Y + 44, mouseX, mouseY, 0xFF2CE67A);
            drawSlotWithItem(g, new ItemStack(Items.LAVA_BUCKET), x + 33, c2Y + 44, mouseX, mouseY, 0xFFFF8800);

            int t2X = x + 58;
            int maxW2 = w - 66;
            drawSmallString(g, "• Pyro-Diesel: 2.5× RF & Velocity", t2X, c2Y + 21, 0xFFFF6677, maxW2);
            drawSmallString(g, "• Distillate: 1.8×  |  Biodiesel: 1.4×", t2X, c2Y + 33, 0xFFE8A4FF, maxW2);
            drawSmallString(g, "• Bio-Oil / Lava: 1.0× Baseline", t2X, c2Y + 45, 0xFFFFCC66, maxW2);
            drawSmallString(g, "★ COLLIDER Mode: Press [SMASH!] at high speed!", t2X, c2Y + 57, 0xFF73F2FF, maxW2);
        } else if (this.currentSubTab == 2) {
            drawSmallString(g, "⚛ Particle Collisions", x + 10, y + 6, 0xFF73F2FF);

            ItemStack[][] recipes = {
                {new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 4), new ItemStack(ObsidiumMod.ASTRAL_VOLCANIC_CATALYST, 4), new ItemStack(ObsidiumMod.SINGULARITY_HADRON_ALLOY, 1), new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 2)},
                {new ItemStack(Items.COAL_BLOCK, 4), new ItemStack(Items.IRON_BLOCK, 2), new ItemStack(Items.DIAMOND, 2), new ItemStack(ObsidiumMod.VOLCANIC_ASH, 2)},
                {new ItemStack(Items.DIAMOND, 4), new ItemStack(Items.GOLD_BLOCK, 2), new ItemStack(Items.NETHERITE_INGOT, 1), new ItemStack(ObsidiumMod.VOLCANIC_ASH, 2)},
                {new ItemStack(Items.COPPER_BLOCK, 4), new ItemStack(Items.GOLD_INGOT, 4), new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 2), new ItemStack(ObsidiumMod.VOLCANITE_INGOT, 2)},
                {new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 4), new ItemStack(ObsidiumMod.VOLCANITE_THERMO_CORE, 2), new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 1), new ItemStack(ObsidiumMod.VOLCANITE_DUST, 2)},
                {new ItemStack(Items.ENDER_EYE, 4), new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 2), new ItemStack(ObsidiumMod.ASTRAL_VOLCANIC_CATALYST, 1), new ItemStack(Items.DIAMOND, 2)},
                {new ItemStack(Items.IRON_INGOT, 4), new ItemStack(Items.REDSTONE_BLOCK, 4), new ItemStack(ObsidiumMod.VOLCANITE_INGOT, 2), new ItemStack(ObsidiumMod.SILVER_INGOT, 2)},
                {new ItemStack(ObsidiumMod.TIN_INGOT, 4), new ItemStack(Items.COPPER_INGOT, 4), new ItemStack(ObsidiumMod.PYRIC_BRONZE_INGOT, 4), new ItemStack(ObsidiumMod.LEAD_INGOT, 1)},
                {new ItemStack(ObsidiumMod.LEAD_INGOT, 4), new ItemStack(Items.IRON_INGOT, 4), new ItemStack(ObsidiumMod.FEROPLUMB_INGOT, 4), new ItemStack(ObsidiumMod.SILVER_INGOT, 1)}
            };
            String[] burstTags = {
                "2.5× RF · 10% Chance!", "1.0× Burst", "1.0× Burst", "1.0× Burst", "1.25× RF",
                "1.5× RF", "1.0× Burst", "1.0× Burst", "1.0× Burst"
            };
            String[] notes = {
                "✦ Singularity Alloy (10%)", "Carbon-Iron Synthesis", "Noble Transmutation", "Copper-Gold Fusion", "Supreme Core Synthesis",
                "Astral Catalyst Fusion", "Redstone Ionization", "Bronze-Lead Split", "Feroplumb Transmutation"
            };

            int totalPages = (recipes.length + 3) / 4;
            int page = Math.floorMod(this.colliderCollisionPage, totalPages);
            String pgStr = "Page " + (page + 1) + "/" + totalPages;
            drawBadge(g, x + w - 8, y + 4, pgStr, 0xFF100A16, 0xFF00CCFF, 0xFF9EEFFF);

            int start = page * 4;
            for (int r = 0; r < 4; r++) {
                int idx = start + r;
                if (idx >= recipes.length) break;
                int ry = y + 20 + r * 31;
                boolean isSingularity = idx == 0;
                g.fill(x + 6, ry, x + w - 6, ry + 29, isSingularity ? 0xFF23112E : 0xFF181224);
                drawBorder(g, x + 6, ry, w - 12, 29, isSingularity ? 0xFFD96BFF : 0xFF2D405C);

                drawSlotWithItem(g, recipes[idx][0], x + 10, ry + 4, mouseX, mouseY, 0xFF00CCFF);
                drawSmallString(g, "+", x + 32, ry + 11, 0xFF88CCFF);
                drawSlotWithItem(g, recipes[idx][1], x + 39, ry + 4, mouseX, mouseY, 0xFF00CCFF);
                drawSmallString(g, "➜", x + 61, ry + 11, 0xFFFFCC44);
                drawSlotWithItem(g, recipes[idx][2], x + 70, ry + 4, mouseX, mouseY, isSingularity ? 0xFFD96BFF : 0xFFFF9900);
                drawSlotWithItem(g, recipes[idx][3], x + 92, ry + 4, mouseX, mouseY, 0xFF66DD88);

                drawSmallString(g, notes[idx], x + 116, ry + 6, isSingularity ? 0xFFE899FF : 0xFFEEEEEE, w - 124);
                drawSmallString(g, burstTags[idx], x + 116, ry + 17, isSingularity ? 0xFFFFCC44 : 0xFF73F2FF, w - 124);
            }

            // Bottom Pagination + Identical Items tip
            int navY = y + h - 18;
            boolean hPrev = mouseX >= x + 10 && mouseX < x + 52 && mouseY >= navY && mouseY < navY + 14;
            g.fill(x + 10, navY, x + 52, navY + 14, hPrev ? 0xFF283A52 : 0xFF141D2C);
            drawBorder(g, x + 10, navY, 42, 14, 0xFF00CCFF);
            drawSmallString(g, "< Prev", x + 16, navY + 4, 0xFF99F0FF);

            String tip = "Same=2×RF | >=90%c Cryo=+10%!";
            int tipMaxW = w - 116;
            int tipW = Math.min((int) (this.font.width(tip) * 0.70f), tipMaxW);
            drawSmallString(g, tip, x + 58 + Math.max(0, (tipMaxW - tipW) / 2), navY + 4, 0xFFFFCC44, tipMaxW);

            boolean hNext = mouseX >= x + w - 52 && mouseX < x + w - 10 && mouseY >= navY && mouseY < navY + 14;
            g.fill(x + w - 52, navY, x + w - 10, navY + 14, hNext ? 0xFF283A52 : 0xFF141D2C);
            drawBorder(g, x + w - 52, navY, 42, 14, 0xFF00CCFF);
            drawSmallString(g, "Next >", x + w - 45, navY + 4, 0xFF99F0FF);
        } else {
            // Sub-tab 3: Quantum Teleportation System
            int c1Y = y + 4;
            g.fill(x + 6, c1Y, x + w - 6, c1Y + 48, 0xFF1D1322);
            drawBorder(g, x + 6, c1Y, w - 12, 48, 0xFFFF9900);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.BASIC_VOLCANIC_TELEPORTER_ITEM), x + 10, c1Y + 14, mouseX, mouseY, 0xFFFF9900);
            drawSmallString(g, "Basic Volcanic Teleporter (2M RF)", x + 34, c1Y + 6, 0xFFFFB74D, w - 42);
            drawSmallString(g, "• Same-dimension jumps up to 2,500 blocks", x + 34, c1Y + 18, 0xFFEEEEEE, w - 42);
            drawSmallString(g, "• Sneak 1.25s on pad -> Quick-Warp to Pinned target", x + 34, c1Y + 29, 0xFFCCCCCC, w - 42);
            drawSmallString(g, "• Passively recharges inventory RF items", x + 34, c1Y + 38, 0xFF88CCFF, w - 42);

            int c2Y = y + 56;
            g.fill(x + 6, c2Y, x + w - 6, c2Y + 48, 0xFF101C2B);
            drawBorder(g, x + 6, c2Y, w - 12, 48, 0xFF00E5FF);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANIC_CORE_TELEPORTER_ITEM), x + 10, c2Y + 14, mouseX, mouseY, 0xFF00E5FF);
            drawSmallString(g, "Volcanic Core Teleporter (20M RF)", x + 34, c2Y + 6, 0xFF73F2FF, w - 42);
            drawSmallString(g, "• Infinite distance & Cross-Dimensional jumps", x + 34, c2Y + 18, 0xFFEEEEEE, w - 42);
            drawSmallString(g, "• Accepts up to 250k RF/t & teleports tamed pets", x + 34, c2Y + 29, 0xFF55FF77, w - 42);
            drawSmallString(g, "• Custom Name, Icon, Public/Private lock & Bed/Spawn", x + 34, c2Y + 38, 0xFFCCCCCC, w - 42);

            int c3Y = y + 108;
            int c3H = h - 114;
            g.fill(x + 6, c3Y, x + w - 6, c3Y + c3H, 0xFF1A1228);
            drawBorder(g, x + 6, c3Y, w - 12, c3H, 0xFFD96BFF);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.VOLCANITE_WARP_GAUNTLET), x + 10, c3Y + 6, mouseX, mouseY, 0xFFD96BFF);
            drawSlotWithItem(g, new ItemStack(ObsidiumMod.SUPREME_LINK_TOOL), x + 10, c3Y + 28, mouseX, mouseY, 0xFFFFCC44);
            drawSmallString(g, "Warp Gauntlet (5M RF) & Pairing", x + 34, c3Y + 6, 0xFFE899FF, w - 42);
            drawSmallString(g, "• RClick: Open Matrix + 16 personal bookmarks", x + 34, c3Y + 18, 0xFFEEEEEE, w - 42);
            drawSmallString(g, "• Shift+Click Pad: Pin | Shift+RClick Air: Quick-Warp", x + 34, c3Y + 29, 0xFF73F2FF, w - 42);
            drawSmallString(g, "• Supreme Link Tool: Pair 2 Pads bids + Mini-Map HUD", x + 34, c3Y + 39, 0xFFFFCC44, w - 42);
        }
    }

    private void drawSlotWithItem(GuiGraphics g, ItemStack stack, int sx, int sy, int mouseX, int mouseY, int borderColor) {
        g.fill(sx, sy, sx + 20, sy + 20, 0xFF0E0914);
        drawBorder(g, sx, sy, 20, 20, borderColor);
        g.renderItem(stack, sx + 2, sy + 2);
        g.renderItemDecorations(this.font, stack, sx + 2, sy + 2);
        if (mouseX >= sx && mouseX < sx + 20 && mouseY >= sy && mouseY < sy + 20) {
            this.hoveredStack = stack;
        }
    }

    private void drawBorder(GuiGraphics g, int bx, int by, int bw, int bh, int color) {
        g.fill(bx, by, bx + bw, by + 1, color);
        g.fill(bx, by + bh - 1, bx + bw, by + bh, color);
        g.fill(bx, by, bx + 1, by + bh, color);
        g.fill(bx + bw - 1, by, bx + bw, by + bh, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - BOOK_WIDTH) / 2;
        int y = (this.height - BOOK_HEIGHT) / 2;

        if (button == 0) {
            int sbX = x + 8;
            int sbY = y + 26;
            for (int c = 0; c < CHAPTER_TITLES.length; c++) {
                int cy = sbY + 19 + c * 20;
                if (mouseX >= sbX + 5 && mouseX < sbX + SIDEBAR_W - 5 && mouseY >= cy && mouseY < cy + 19) {
                    this.currentChapter = c;
                    this.currentSubTab = 0;
                    return true;
                }
            }

            int contentX = sbX + SIDEBAR_W + 6;
            int contentW = BOOK_WIDTH - (contentX - x) - 8;
            String[] subTabs = SUB_TABS[this.currentChapter];
            int subW = (contentW - (subTabs.length - 1) * 4) / subTabs.length;
            for (int t = 0; t < subTabs.length; t++) {
                int tx = contentX + t * (subW + 4);
                int ty = y + 26;
                if (mouseX >= tx && mouseX < tx + subW && mouseY >= ty && mouseY < ty + 16) {
                    this.currentSubTab = t;
                    return true;
                }
            }

            if (this.currentChapter == 0 && this.currentSubTab == 1) {
                int boxY = y + 46;
                int btnY = boxY + 4;
                int btnW = (contentW - 24) / 3;
                if (mouseY >= btnY && mouseY <= btnY + 16) {
                    for (int p = 0; p < 3; p++) {
                        int bx = contentX + 8 + p * (btnW + 4);
                        if (mouseX >= bx && mouseX < bx + btnW) {
                            this.powerMultiblockPage = p;
                            return true;
                        }
                    }
                }
            }

            if (this.currentChapter == 1 && this.currentSubTab == 2) {
                int boxY = y + 46;
                int boxH = BOOK_HEIGHT - 54;
                int navY = boxY + boxH - 18;
                int totalPages = Math.max(1, (MagmaForgeCoreBlockEntity.RECIPES.size() + RECIPES_PER_PAGE - 1) / RECIPES_PER_PAGE);
                if (mouseX >= contentX + 10 && mouseX < contentX + 52 && mouseY >= navY && mouseY < navY + 14) {
                    this.recipePage = (this.recipePage - 1 + totalPages) % totalPages;
                    return true;
                }
                if (mouseX >= contentX + contentW - 52 && mouseX < contentX + contentW - 10 && mouseY >= navY && mouseY < navY + 14) {
                    this.recipePage = (this.recipePage + 1) % totalPages;
                    return true;
                }
            }

            if (this.currentChapter == 7 && this.currentSubTab == 2) {
                int boxY = y + 46;
                int boxH = BOOK_HEIGHT - 54;
                int navY = boxY + boxH - 18;
                if (mouseX >= contentX + 10 && mouseX < contentX + 52 && mouseY >= navY && mouseY < navY + 14) {
                    this.colliderCollisionPage = (this.colliderCollisionPage + 2) % 3;
                    return true;
                }
                if (mouseX >= contentX + contentW - 52 && mouseX < contentX + contentW - 10 && mouseY >= navY && mouseY < navY + 14) {
                    this.colliderCollisionPage = (this.colliderCollisionPage + 1) % 3;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
