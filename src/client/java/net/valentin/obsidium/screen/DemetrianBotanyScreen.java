package net.valentin.obsidium.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.DemetrianScionItem;

import java.util.List;

public class DemetrianBotanyScreen extends Screen {
    private static final int BOOK_WIDTH = 410;
    private static final int BOOK_HEIGHT = 230;
    private static final int SIDEBAR_W = 110;

    private static final String[] CHAPTERS = {
            "🌿 Intro",
            "🔪 Grafting",
            "🌳 Hybrids",
            "💎 Apex Trees",
            "🪴 Bonsai & Bees",
            "📦 Storage"
    };

    private int currentChapter = 0;
    private ItemStack hoveredStack = ItemStack.EMPTY;

    public DemetrianBotanyScreen() {
        super(Component.literal("Demetrian Botany"));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(g);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        this.hoveredStack = ItemStack.EMPTY;

        int x = (this.width - BOOK_WIDTH) / 2;
        int y = (this.height - BOOK_HEIGHT) / 2;

        // 1. Exterior Emerald Leather Binding & Gold Trim
        g.fill(x, y, x + BOOK_WIDTH, y + BOOK_HEIGHT, 0xFF0D2818);
        g.fill(x + 2, y + 2, x + BOOK_WIDTH - 2, y + BOOK_HEIGHT - 2, 0xFF163824);
        drawBorder(g, x, y, BOOK_WIDTH, BOOK_HEIGHT, 0xFF047857);
        drawBorder(g, x + 1, y + 1, BOOK_WIDTH - 2, BOOK_HEIGHT - 2, 0xFFB8860B);

        // 2. Left Sidebar (Chapters)
        g.fill(x + 4, y + 4, x + SIDEBAR_W, y + BOOK_HEIGHT - 4, 0xFF0B1F13);
        drawBorder(g, x + 4, y + 4, SIDEBAR_W - 4, BOOK_HEIGHT - 8, 0xFF065F46);

        // Sidebar Header
        g.drawString(this.font, "§6📖 CHAPTERS", x + 10, y + 10, 0xFBBF24, false);
        g.fill(x + 8, y + 22, x + SIDEBAR_W - 6, y + 23, 0xFFB8860B);

        // Chapter Buttons (84px wide, well padded)
        for (int i = 0; i < CHAPTERS.length; i++) {
            int cy = y + 28 + i * 29;
            boolean selected = (currentChapter == i);
            boolean hovered = mouseX >= x + 8 && mouseX <= x + SIDEBAR_W - 8 && mouseY >= cy && mouseY <= cy + 22;

            int bg = selected ? 0xFF047857 : (hovered ? 0xFF1F4D36 : 0xFF132D1E);
            int border = selected ? 0xFFFBBF24 : (hovered ? 0xFF34D399 : 0xFF065F46);
            int textCol = selected ? 0xFFFFFFFF : (hovered ? 0xFF86EFAC : 0xFFCECAC5);

            g.fill(x + 8, cy, x + SIDEBAR_W - 8, cy + 22, bg);
            drawBorder(g, x + 8, cy, SIDEBAR_W - 16, 22, border);
            g.drawString(this.font, CHAPTERS[i], x + 12, cy + 7, textCol, false);
        }

        // 3. Right Content Page (Parchment Tint, 272px wide)
        int px = x + SIDEBAR_W + 4;
        int py = y + 4;
        int pw = BOOK_WIDTH - SIDEBAR_W - 8;
        int ph = BOOK_HEIGHT - 8;

        g.fill(px, py, px + pw, py + ph, 0xFFFBF7EE);
        drawBorder(g, px, py, pw, ph, 0xFFD4AF37);
        drawBorder(g, px + 1, py + 1, pw - 2, ph - 2, 0xFFE2D9C8);

        // Render Active Chapter
        switch (currentChapter) {
            case 0 -> renderChapterIntroduction(g, px, py, pw, ph, mouseX, mouseY);
            case 1 -> renderChapterGrafting(g, px, py, pw, ph, mouseX, mouseY);
            case 2 -> renderChapterHybrids(g, px, py, pw, ph, mouseX, mouseY);
            case 3 -> renderChapterApex(g, px, py, pw, ph, mouseX, mouseY);
            case 4 -> renderChapterBonsai(g, px, py, pw, ph, mouseX, mouseY);
            case 5 -> renderChapterStorage(g, px, py, pw, ph, mouseX, mouseY);
        }

        // Render Tooltip if hovering over any item
        if (!this.hoveredStack.isEmpty()) {
            g.renderTooltip(this.font, this.hoveredStack, mouseX, mouseY);
        }
    }

    private void renderChapterIntroduction(GuiGraphics g, int px, int py, int pw, int ph, int mx, int my) {
        g.drawString(this.font, "§2🌿 SACRED BOTANY OF DEMETER", px + 12, py + 12, 0x14532D, false);
        g.fill(px + 12, py + 24, px + pw - 12, py + 25, 0xFF15803D);

        int maxW = pw - 34; // Generous margin on right
        int curY = py + 32;

        curY += drawWrapped(g, "The blessing of goddess Demeter bestows a sacred craft upon fertile soil: §2Mineral Arboriculture§0.", px + 14, curY, maxW, 0x1F2937);
        curY += 6;

        curY += drawWrapped(g, "Every sacred tree consists of two living halves:", px + 14, curY, maxW, 0x1F2937);
        curY += 3;

        curY += drawWrapped(g, "§8• §2Rootstock (The Root): §0base sapling planted in soil.", px + 14, curY, maxW, 0x1F2937);
        curY += 3;

        curY += drawWrapped(g, "§8• §6Scion (The Crown): §0noble cutting bearing ore fruit.", px + 14, curY, maxW, 0x1F2937);
        curY += 6;

        curY += drawWrapped(g, "Through master grafting, you can cultivate metals, crystals, and gems without ever mining underground.", px + 14, curY, maxW, 0x1F2937);

        // Showcase Items
        drawItemSlot(g, px + 24, py + 172, new ItemStack(ObsidiumMod.DEMETRIAN_GRAFTING_KNIFE), mx, my);
        g.drawString(this.font, "§2Grafting Knife", px + 48, py + 176, 0x14532D, false);

        drawItemSlot(g, px + 150, py + 172, DemetrianScionItem.create("midas_gold", 3), mx, my);
        g.drawString(this.font, "§6Noble Scion", px + 174, py + 176, 0xB45309, false);
    }

    private void renderChapterGrafting(GuiGraphics g, int px, int py, int pw, int ph, int mx, int my) {
        g.drawString(this.font, "§2🔪 PRACTICAL GRAFTING GUIDE", px + 12, py + 12, 0x14532D, false);
        g.fill(px + 12, py + 24, px + pw - 12, py + 25, 0xFF15803D);

        int textW = pw - 58;

        // Step 1
        drawItemSlot(g, px + 14, py + 30, new ItemStack(ObsidiumMod.DEMETRIAN_GRAFTING_KNIFE), mx, my);
        g.drawString(this.font, "§2Step 1: Harvest Scion", px + 38, py + 30, 0x14532D, false);
        drawWrapped(g, "Sneak + right-click leaves with the Grafting Knife to harvest cuttings.", px + 38, py + 40, textW, 0x374151);

        // Step 2
        drawItemSlot(g, px + 14, py + 64, new ItemStack(ObsidiumMod.MIDAS_GOLD_SAPLING), mx, my);
        g.drawString(this.font, "§2Step 2: Incise Rootstock", px + 38, py + 64, 0x14532D, false);
        drawWrapped(g, "Plant rootstock sapling & right-click with Grafting Knife.", px + 38, py + 74, textW, 0x374151);

        // Step 3
        drawItemSlot(g, px + 14, py + 98, DemetrianScionItem.create("hermes_copper", 2), mx, my);
        g.drawString(this.font, "§2Step 3: Insert Scion", px + 38, py + 98, 0x14532D, false);
        drawWrapped(g, "Right-click the incised sapling with your harvested Scion.", px + 38, py + 108, textW, 0x374151);

        // Step 4
        drawItemSlot(g, px + 14, py + 132, new ItemStack(ObsidiumMod.DEMETRIAN_GRAFTING_TAPE), mx, my);
        g.drawString(this.font, "§2Step 4: Bind with Tape", px + 38, py + 132, 0x14532D, false);
        drawWrapped(g, "Right-click with Grafting Tape to trigger metamorphosis!", px + 38, py + 142, textW, 0x374151);

        // Bonus: Lens
        drawItemSlot(g, px + 14, py + 168, new ItemStack(ObsidiumMod.DEMETRIAN_BOTANIST_LENS), mx, my);
        g.drawString(this.font, "§6Botanist's Lens", px + 38, py + 168, 0xB45309, false);
        drawWrapped(g, "Right-click any plant to inspect genetics & hybrid outcomes.", px + 38, py + 178, textW, 0x6B7280);
    }

    private void renderChapterHybrids(GuiGraphics g, int px, int py, int pw, int ph, int mx, int my) {
        g.drawString(this.font, "§2🌳 SACRED HYBRIDIZATION TREE", px + 12, py + 12, 0x14532D, false);
        g.fill(px + 12, py + 24, px + pw - 12, py + 25, 0xFF15803D);

        // Hybrid row 1: Iron + Coal -> Redstone
        renderHybridRecipeRow(g, px + 14, py + 30, new ItemStack(ObsidiumMod.ARES_IRON_SAPLING), DemetrianScionItem.create("hades_coal", 2), new ItemStack(ObsidiumMod.ASTRAPE_REDSTONE_SAPLING), "Astrape Redstone", mx, my);

        // Hybrid row 2: Gold + Copper -> Lapis
        renderHybridRecipeRow(g, px + 14, py + 62, new ItemStack(ObsidiumMod.MIDAS_GOLD_SAPLING), DemetrianScionItem.create("hermes_copper", 2), new ItemStack(ObsidiumMod.POSEIDON_LAPIS_SAPLING), "Poseidon Lapis", mx, my);

        // Hybrid row 3: Gold + Silver -> Electrum
        renderHybridRecipeRow(g, px + 14, py + 94, new ItemStack(ObsidiumMod.MIDAS_GOLD_SAPLING), DemetrianScionItem.create("artemis_silver", 2), new ItemStack(ObsidiumMod.ELECTRUM_SAPLING), "Electrum Tree", mx, my);

        // Hybrid row 4: Iron + Silver -> Quartz
        renderHybridRecipeRow(g, px + 14, py + 126, new ItemStack(ObsidiumMod.ARES_IRON_SAPLING), DemetrianScionItem.create("artemis_silver", 2), new ItemStack(ObsidiumMod.APOLLO_QUARTZ_SAPLING), "Apollo Quartz", mx, my);

        // Hybrid row 5: Redstone + Quartz -> Amethyst
        renderHybridRecipeRow(g, px + 14, py + 158, new ItemStack(ObsidiumMod.ASTRAPE_REDSTONE_SAPLING), DemetrianScionItem.create("apollo_quartz", 2), new ItemStack(ObsidiumMod.DIONYSIAN_AMETHYST_SAPLING), "Dionysian Amethyst", mx, my);
    }

    private void renderChapterApex(GuiGraphics g, int px, int py, int pw, int ph, int mx, int my) {
        g.drawString(this.font, "§4💎 STERILE APEX HYBRIDS", px + 12, py + 12, 0x991B1B, false);
        g.fill(px + 12, py + 24, px + pw - 12, py + 25, 0xFFB91C1C);

        int maxW = pw - 34;
        int curY = py + 30;

        curY += drawWrapped(g, "Legendary trees (§4Netherite§0 & §bDiamond§0) are sterile! Their leaves never drop saplings — craft the first Netherite specimen in late game.", px + 14, curY, maxW, 0x1F2937);
        curY += 6;

        curY += drawWrapped(g, "§8✦ §4Tartarean Netherite Sapling:", px + 14, curY, maxW, 0x991B1B);
        curY += drawWrapped(g, "  • Rootstock: §cHephaestus Volcanite", px + 18, curY, maxW - 4, 0x374151);
        curY += drawWrapped(g, "  • Scion: §4Tartarean Netherite (late-game parent)", px + 18, curY, maxW - 4, 0x374151);
        curY += 6;

        curY += drawWrapped(g, "§8✦ §bAdamantine Diamond Sapling (Apex):", px + 14, curY, maxW, 0x0284C7);
        curY += drawWrapped(g, "  • Rootstock: §2Plouton Emerald", px + 18, curY, maxW - 4, 0x374151);
        curY += drawWrapped(g, "  • Scion: §4Tartarean Netherite", px + 18, curY, maxW - 4, 0x374151);
        curY += 8;

        // Diamond tree recipe showcase
        renderHybridRecipeRow(g, px + 14, curY, new ItemStack(ObsidiumMod.PLOUTON_EMERALD_SAPLING), DemetrianScionItem.create("tartarean_netherite", 3), new ItemStack(ObsidiumMod.ADAMANTINE_DIAMOND_SAPLING), "Adamantine Diamond", mx, my);
        curY += 28;

        drawWrapped(g, "§6★ Apex Yield: Produces pure sparkling mineral gemstones!", px + 14, curY, maxW, 0xB45309);
    }

    private void renderChapterBonsai(GuiGraphics g, int px, int py, int pw, int ph, int mx, int my) {
        g.drawString(this.font, "§2🪴 BONSAI & VOLCANIC BEES", px + 12, py + 12, 0x14532D, false);
        g.fill(px + 12, py + 24, px + pw - 12, py + 25, 0xFF15803D);

        int maxW = pw - 34;
        int textW = pw - 58;
        int curY = py + 30;

        // Bonsai Planter
        drawItemSlot(g, px + 14, curY, new ItemStack(ObsidiumMod.DEMETRIAN_BONSAI_PLANTER_BLOCK), mx, my);
        g.drawString(this.font, "§2Demetrian Bonsai Planter", px + 38, curY + 1, 0x14532D, false);
        drawWrapped(g, "Plant any mineral sapling to harvest miniature wood & ore drops.", px + 38, curY + 11, textW, 0x374151);
        curY += 32;

        // Volcanic Beehive
        drawItemSlot(g, px + 14, curY, new ItemStack(ObsidiumMod.VOLCANIC_BEEHIVE_BLOCK), mx, my);
        g.drawString(this.font, "§6Volcanic Beehive & Honey", px + 38, curY + 1, 0xB45309, false);
        drawWrapped(g, "Houses fiery bees. Shear for honeycombs or bottle for glowing honey.", px + 38, curY + 11, textW, 0x374151);
        curY += 32;

        // Pollinating Brush
        drawItemSlot(g, px + 14, curY, new ItemStack(ObsidiumMod.POLLINATING_BRUSH), mx, my);
        g.drawString(this.font, "§2Pollinating Brush", px + 38, curY + 1, 0x14532D, false);
        drawWrapped(g, "Harvest royal pollen from leaves or hives to pollinate saplings.", px + 38, curY + 11, textW, 0x374151);
        curY += 32;

        // Honey bottle
        drawItemSlot(g, px + 14, curY, new ItemStack(ObsidiumMod.VOLCANIC_HONEY_BOTTLE), mx, my);
        g.drawString(this.font, "§eVolcanic Honey Bottle", px + 38, curY + 1, 0xD97706, false);
        drawWrapped(g, "Drink for Fire Res & Haste II, or use on crops & bonsai to accelerate!", px + 38, curY + 11, textW, 0x374151);
        curY += 30;

        // Arborist Automation Tip
        drawWrapped(g, "§8• Geothermal Arborist: §2Automatically plants, fertilizes, & extracts drops from Bonsai Planters in range!", px + 14, curY, maxW, 0x15803D);
    }

    private void renderChapterStorage(GuiGraphics g, int px, int py, int pw, int ph, int mx, int my) {
        g.drawString(this.font, "§2📦 BOTANICAL STORAGE & BIO-POWER", px + 12, py + 12, 0x14532D, false);
        g.fill(px + 12, py + 24, px + pw - 12, py + 25, 0xFF15803D);

        int maxW = pw - 34;
        int textW = pw - 58;
        int curY = py + 30;

        // Demetrian Nursery Barrel
        drawItemSlot(g, px + 14, curY, new ItemStack(ObsidiumMod.DEMETRIAN_SAPLING_BARREL_BLOCK), mx, my);
        g.drawString(this.font, "§2Demetrian Nursery Barrel §8(12,288)", px + 38, curY + 1, 0x14532D, false);
        drawWrapped(g, "3 deep chambers (4,096 each) for rootstocks, scions, & hybrids.", px + 38, curY + 11, textW, 0x374151);
        curY += 30;

        // Demetrian Lumber Depot
        drawItemSlot(g, px + 14, curY, new ItemStack(ObsidiumMod.DEMETRIAN_LUMBER_DEPOT_BLOCK), mx, my);
        g.drawString(this.font, "§6Demetrian Lumber Depot §8(16,384)", px + 38, curY + 1, 0xB45309, false);
        drawWrapped(g, "4 deep bays (4,096 each) for logs & Timber Bundles.", px + 38, curY + 11, textW, 0x374151);
        curY += 30;

        // Foliage Biomass Bale
        drawItemSlot(g, px + 14, curY, new ItemStack(ObsidiumMod.FOLIAGE_BALE_BLOCK), mx, my);
        g.drawString(this.font, "§2Foliage Biomass Bale §8(9 Leaves)", px + 38, curY + 1, 0x14532D, false);
        drawWrapped(g, "Supercharged fuel for Biomass Generator (+20% energy & biogas).", px + 38, curY + 11, textW, 0x374151);
        curY += 30;

        // Interaction tip
        drawWrapped(g, "§8• Quick Feed: Right-click directly onto generator with foliage bales.", px + 14, curY, maxW, 0x4B5563);
        curY += 12;
        drawWrapped(g, "§8• Depot/Barrel: Right-click to deposit, Left-click to extract (Shift: 64).", px + 14, curY, maxW, 0x4B5563);
    }

    private void renderHybridRecipeRow(GuiGraphics g, int x, int y, ItemStack base, ItemStack scion, ItemStack result, String label, int mx, int my) {
        drawItemSlot(g, x, y, base, mx, my);
        g.drawString(this.font, "+", x + 24, y + 6, 0x047857, false);
        drawItemSlot(g, x + 34, y, scion, mx, my);
        g.drawString(this.font, "➜", x + 58, y + 6, 0xB8860B, false);
        drawItemSlot(g, x + 72, y, result, mx, my);
        g.drawString(this.font, "§2" + label, x + 96, y + 6, 0x14532D, false);
    }

    private void drawItemSlot(GuiGraphics g, int x, int y, ItemStack stack, int mx, int my) {
        g.fill(x - 1, y - 1, x + 19, y + 19, 0xFFE2D9C8);
        drawBorder(g, x - 1, y - 1, 20, 20, 0xFF047857);
        g.renderFakeItem(stack, x + 1, y + 1);

        if (mx >= x && mx <= x + 18 && my >= y && my <= y + 18) {
            this.hoveredStack = stack;
        }
    }

    /**
     * Draws text that automatically wraps into multiple lines if it exceeds maxWidth.
     * Returns total vertical height consumed by the rendered lines.
     */
    private int drawWrapped(GuiGraphics g, String text, int x, int y, int maxWidth, int color) {
        List<FormattedCharSequence> lines = this.font.split(Component.literal(text), maxWidth);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(this.font, lines.get(i), x, y + i * 11, color, false);
        }
        return lines.size() * 11;
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - BOOK_WIDTH) / 2;
        int y = (this.height - BOOK_HEIGHT) / 2;

        // Check chapter button clicks
        for (int i = 0; i < CHAPTERS.length; i++) {
            int cy = y + 28 + i * 29;
            if (mouseX >= x + 8 && mouseX <= x + SIDEBAR_W - 8 && mouseY >= cy && mouseY <= cy + 22) {
                if (currentChapter != i) {
                    currentChapter = i;
                    if (minecraft != null && minecraft.player != null) {
                        minecraft.player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.7F, 1.0F);
                    }
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
