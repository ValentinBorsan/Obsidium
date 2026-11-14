package net.valentin.obsidium.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.block.MagmaForgeCoreBlockEntity;
import net.valentin.obsidium.block.MagmaForgeRecipe;
import net.valentin.obsidium.client.VolcaniteGuiTheme;
import net.valentin.obsidium.menu.MagmaForgeMenu;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MagmaForgeScreen extends AbstractContainerScreen<MagmaForgeMenu> {

    // 2 columns x 3 rows = 6 icon buttons per page (super clean, zero text clutter!)
    private static final int GRID_COLS = 2;
    private static final int GRID_ROWS = 3;
    private static final int RECIPES_PER_PAGE = GRID_COLS * GRID_ROWS;
    private int recipePage = 0;

    // Mini-game visual feedback state
    private long lastStrikeTimeMs = 0;
    private boolean lastStrikePerfect = false;

    public MagmaForgeScreen(MagmaForgeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 276;
        this.imageHeight = 230;
        this.titleLabelX = 98;
        this.titleLabelY = 8;
        this.inventoryLabelX = 58;
        this.inventoryLabelY = 137;
    }

    private float getNeedlePosition() {
        double cycle = (System.currentTimeMillis() % 1500L) / 1500.0;
        return (float) (cycle < 0.5 ? cycle * 2.0 : (1.0 - cycle) * 2.0);
    }

    private boolean isNeedleInSweetSpot(float needle) {
        return needle >= 0.36f && needle <= 0.64f;
    }

    private void triggerHammerStrike() {
        if (this.minecraft == null || this.minecraft.gameMode == null) return;
        float needle = getNeedlePosition();
        boolean perfect = isNeedleInSweetSpot(needle);
        this.lastStrikeTimeMs = System.currentTimeMillis();
        this.lastStrikePerfect = perfect;
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, perfect ? 1 : 0);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        VolcaniteGuiTheme.renderExteriorFrame(g, x, y, imageWidth, imageHeight, true);

        // 1. Main Outer Dark-Obsidian Frame (276x230)
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF120C18);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF1A1222);
        drawBorder(g, x, y, imageWidth, imageHeight, 0xFFFF6600);
        drawBorder(g, x + 1, y + 1, imageWidth - 2, imageHeight - 2, 0xFF552200);

        // Corner accents
        g.fill(x + 4, y + 4, x + 6, y + 6, 0xFFFFAA00);
        g.fill(x + imageWidth - 6, y + 4, x + imageWidth - 4, y + 6, 0xFFFFAA00);
        g.fill(x + 4, y + imageHeight - 6, x + 6, y + imageHeight - 4, 0xFFFFAA00);
        g.fill(x + imageWidth - 6, y + imageHeight - 6, x + imageWidth - 4, y + imageHeight - 4, 0xFFFFAA00);

        // Header Bar
        g.fill(x + 8, y + 5, x + imageWidth - 8, y + 19, 0xFF24172E);
        drawBorder(g, x + 8, y + 5, imageWidth - 16, 14, 0xFF662A00);

        // [📖] Manual button in header bar
        boolean hoverBook = mouseX >= x + imageWidth - 26 && mouseX < x + imageWidth - 10 && mouseY >= y + 6 && mouseY < y + 18;
        g.fill(x + imageWidth - 26, y + 6, x + imageWidth - 10, y + 18, hoverBook ? 0xFF4A2511 : 0xFF1A1024);
        drawBorder(g, x + imageWidth - 26, y + 6, 16, 12, hoverBook ? 0xFFFFCC00 : 0xFF884400);
        g.drawString(this.font, "📖", x + imageWidth - 21, y + 8, 0xFFFFCC44, false);

        // 2. Left Panel: Lava & RF Bars (x+8..52, y+23..132)
        g.fill(x + 8, y + 23, x + 52, y + 132, 0xFF150E1C);
        drawBorder(g, x + 8, y + 23, 44, 109, 0xFF4A2200);

        // Lava Tank (x+14..30, y+28..104)
        g.fill(x + 14, y + 28, x + 30, y + 104, 0xFF0D0811);
        drawBorder(g, x + 14, y + 28, 16, 76, 0xFFAA4400);
        long fluidMb = this.menu.getFluidMb();
        int fluidH = (int) Math.min(74, (fluidMb * 74L) / 64000L);
        if (fluidH > 0) {
            g.fillGradient(x + 15, y + 103 - fluidH, x + 29, y + 103, 0xFFFFAA00, 0xFFDD3300);
        }
        for (int gy = 42; gy < 98; gy += 16) {
            g.fill(x + 15, y + gy, x + 19, y + gy + 1, 0x66FFFFFF);
        }

        // RF Energy Bar (x+34..46, y+28..126)
        g.fill(x + 34, y + 28, x + 46, y + 126, 0xFF0D0811);
        drawBorder(g, x + 34, y + 28, 12, 98, 0xFF225588);
        long energy = this.menu.getEnergy();
        int energyH = (int) Math.min(96, (energy * 96L) / 10_000_000L);
        if (energyH > 0) {
            g.fillGradient(x + 35, y + 125 - energyH, x + 45, y + 125, 0xFF00E5FF, 0xFF0055CC);
        }
        for (int gy = 44; gy < 120; gy += 18) {
            g.fill(x + 35, y + gy, x + 38, y + gy + 1, 0x66FFFFFF);
        }

        // 3. Center Panel: Forge Crucible & Mini-Game (x+56..192, y+23..132)
        g.fill(x + 56, y + 23, x + 192, y + 132, 0xFF150E1C);
        drawBorder(g, x + 56, y + 23, 136, 109, 0xFF552600);

        // Conduits from 3 Input slots to Progress Bar
        int prog = this.menu.getCraftProgress();
        int conduitColor = prog > 0 ? 0xFFFF7700 : 0xFF3D1E10;
        g.fill(x + 75, y + 49, x + 77, y + 57, conduitColor);
        g.fill(x + 105, y + 45, x + 107, y + 57, conduitColor);
        g.fill(x + 135, y + 49, x + 137, y + 57, conduitColor);
        g.fill(x + 75, y + 55, x + 137, y + 57, conduitColor);

        // Slot Frames
        drawSlotFrame(g, x + 67, y + 31, 0xFFAA5500);  // Input 1 (68, 32)
        drawSlotFrame(g, x + 97, y + 27, 0xFFFF8800);  // Input 2 (98, 28)
        drawSlotFrame(g, x + 127, y + 31, 0xFFAA5500); // Input 3 (128, 32)
        drawSlotFrame(g, x + 13, y + 109, 0xFFFF6600); // Lava Fuel (14, 110)
        drawSlotFrame(g, x + 163, y + 53, 0xFFFFCC00); // Main Output (164, 54)
        drawSlotFrame(g, x + 163, y + 89, 0xFF00E5FF); // Bonus Output (164, 90)

        // Progress Bar (x+64..148, y+58..66)
        int barX = x + 64;
        int barY = y + 58;
        int barW = 84;
        g.fill(barX, barY, barX + barW, barY + 8, 0xFF09060C);
        drawBorder(g, barX, barY, barW, 8, 0xFF774400);
        int maxProg = this.menu.getMaxProgress();
        int progW = maxProg > 0 ? (int) Math.min(barW - 2, (prog * (barW - 2L)) / maxProg) : 0;
        if (progW > 0) {
            g.fillGradient(barX + 1, barY + 1, barX + 1 + progW, barY + 7, 0xFFFFDD44, 0xFFFF5500);
        }
        g.fill(x + 150, y + 61, x + 161, y + 63, prog > 0 ? 0xFFFFCC00 : 0xFF442211);

        // 4. Tempering Bar (x+64..148, y+72..83)
        int mgX = x + 64;
        int mgY = y + 72;
        int mgW = 84;
        int mgH = 11;
        g.fill(mgX, mgY, mgX + mgW, mgY + mgH, 0xFF09060C);
        drawBorder(g, mgX, mgY, mgW, mgH, 0xFF773A1A);

        g.fill(mgX + 1, mgY + 1, mgX + mgW - 1, mgY + mgH - 1, 0xFF331212);
        int warmStart = mgX + (int) (mgW * 0.22f);
        int warmEnd = mgX + (int) (mgW * 0.78f);
        g.fill(warmStart, mgY + 1, warmEnd, mgY + mgH - 1, 0xFF663800);
        int sweetStart = mgX + (int) (mgW * 0.36f);
        int sweetEnd = mgX + (int) (mgW * 0.64f);
        g.fill(sweetStart, mgY + 1, sweetEnd, mgY + mgH - 1, 0xFF22AA44);
        g.fill(sweetStart + 3, mgY + 2, sweetEnd - 3, mgY + mgH - 2, 0xFF55FF77);

        float needle = getNeedlePosition();
        int needleX = mgX + 2 + (int) (needle * (mgW - 5));
        boolean inSweet = isNeedleInSweetSpot(needle);
        g.fill(needleX - 1, mgY - 1, needleX + 2, mgY + mgH + 1, inSweet ? 0xFFFFFFFF : 0xFFFFCC00);
        g.fill(needleX, mgY, needleX + 1, mgY + mgH, 0xFFFF2200);

        // 5. Strike Button (x+64..148, y+89..107)
        int btnX = x + 64;
        int btnY = y + 89;
        int btnW = 84;
        int btnH = 18;
        boolean hoverStrike = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        long sinceStrike = System.currentTimeMillis() - this.lastStrikeTimeMs;
        int btnBg = (sinceStrike < 180)
                ? (this.lastStrikePerfect ? 0xFF1E7A3A : 0xFF885500)
                : (hoverStrike ? 0xFF52240E : 0xFF331507);
        int btnBorder = inSweet ? 0xFF55FF77 : (hoverStrike ? 0xFFFFCC00 : 0xFF994400);
        g.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnBg);
        drawBorder(g, btnX, btnY, btnW, btnH, btnBorder);
        String btnText = inSweet ? "🔨 STRIKE!" : "STRIKE [SPACE]";
        g.drawString(this.font, btnText, btnX + (btnW - this.font.width(btnText)) / 2, btnY + 5,
                inSweet ? 0xFF55FF77 : 0xFFFFDD88, false);

        // Minimal, centered Masterwork stars below button (y+114)
        int strikes = this.menu.getPerfectStrikes();
        String starStr = (strikes >= 1 ? "★" : "☆") + "  " + (strikes >= 2 ? "★" : "☆") + "  " + (strikes >= 3 ? "★" : "☆");
        if (strikes >= 2) {
            starStr += "  (-50%)";
        }
        int starColor = strikes >= 2 ? 0xFF00E5FF : (strikes == 1 ? 0xFFFFCC00 : 0xFF777777);
        g.drawString(this.font, starStr, btnX + (btnW - this.font.width(starStr)) / 2, y + 114, starColor, false);

        // Small star badge above Bonus Output slot
        g.drawString(this.font, "★", x + 169, y + 79, 0xFF00E5FF, false);

        // 6. Right Panel: Clean 2x3 Recipe Icon Grid (x+196..268, y+23..132)
        int rpX = x + 196;
        int rpY = y + 23;
        int rpW = 72;
        int rpH = 109;
        g.fill(rpX, rpY, rpX + rpW, rpY + rpH, 0xFF150E1C);
        drawBorder(g, rpX, rpY, rpW, rpH, 0xFF552600);

        String recHeader = "Recipes";
        g.drawString(this.font, recHeader, rpX + (rpW - this.font.width(recHeader)) / 2, rpY + 5, 0xFFFFB74D, false);

        List<MagmaForgeRecipe> recipes = MagmaForgeCoreBlockEntity.RECIPES;
        int totalPages = Math.max(1, (recipes.size() + RECIPES_PER_PAGE - 1) / RECIPES_PER_PAGE);
        int startIdx = this.recipePage * RECIPES_PER_PAGE;

        int gridStartX = rpX + 11;
        int gridStartY = rpY + 18;
        int cellGap = 26;

        for (int i = 0; i < RECIPES_PER_PAGE; i++) {
            int rIdx = startIdx + i;
            if (rIdx >= recipes.size()) break;
            MagmaForgeRecipe rec = recipes.get(rIdx);
            int col = i % GRID_COLS;
            int row = i / GRID_COLS;
            int cx = gridStartX + col * cellGap;
            int cy = gridStartY + row * 24;

            boolean hover = mouseX >= cx && mouseX < cx + 22 && mouseY >= cy && mouseY < cy + 22;
            boolean canCraft = playerHasIngredients(rec);

            int cellBg = hover ? 0xFF3E254B : (canCraft ? 0xFF1B2E22 : 0xFF21172B);
            int cellBorder = hover ? 0xFFFFCC00 : (canCraft ? 0xFF44AA55 : 0xFF442E54);
            g.fill(cx, cy, cx + 22, cy + 22, cellBg);
            drawBorder(g, cx, cy, 22, 22, cellBorder);

            g.renderItem(rec.getResult(), cx + 3, cy + 3);
            g.renderItemDecorations(this.font, rec.getResult(), cx + 3, cy + 3);
        }

        // Pagination Controls at bottom of Right Panel (rpY + 93)
        int navY = rpY + 93;
        boolean prevHover = mouseX >= rpX + 6 && mouseX < rpX + 20 && mouseY >= navY && mouseY < navY + 12;
        boolean nextHover = mouseX >= rpX + rpW - 20 && mouseX < rpX + rpW - 6 && mouseY >= navY && mouseY < navY + 12;

        g.fill(rpX + 6, navY, rpX + 20, navY + 12, prevHover ? 0xFF553311 : 0xFF261830);
        drawBorder(g, rpX + 6, navY, 14, 12, 0xFF884400);
        g.drawString(this.font, "<", rpX + 10, navY + 2, 0xFFFFCC00, false);

        String pageStr = (this.recipePage + 1) + "/" + totalPages;
        g.drawString(this.font, pageStr, rpX + (rpW - this.font.width(pageStr)) / 2, navY + 2, 0xFFCCCCCC, false);

        g.fill(rpX + rpW - 20, navY, rpX + rpW - 6, navY + 12, nextHover ? 0xFF553311 : 0xFF261830);
        drawBorder(g, rpX + rpW - 20, navY, 14, 12, 0xFF884400);
        g.drawString(this.font, ">", rpX + rpW - 15, navY + 2, 0xFFFFCC00, false);

        // 7. Player Inventory & Hotbar Slot Frames
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                drawSlotFrame(g, x + 57 + col * 18, y + 147 + row * 18, 0xFF4A355A);
            }
        }
        for (int col = 0; col < 9; ++col) {
            drawSlotFrame(g, x + 57 + col * 18, y + 205, 0xFF6A457A);
        }
    }

    private boolean playerHasIngredients(MagmaForgeRecipe recipe) {
        if (this.minecraft == null || this.minecraft.player == null) return false;
        Inventory inv = this.minecraft.player.getInventory();
        for (ItemStack req : recipe.getIngredients()) {
            int found = 0;
            for (int i = 0; i < 3; i++) {
                ItemStack inForge = this.menu.getContainer().getItem(i);
                if (ItemStack.isSameItemSameComponents(inForge, req)) {
                    found += inForge.getCount();
                }
            }
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack inInv = inv.getItem(i);
                if (ItemStack.isSameItemSameComponents(inInv, req)) {
                    found += inInv.getCount();
                }
            }
            if (found < req.getCount()) return false;
        }
        return true;
    }

    private void drawSlotFrame(GuiGraphics g, int sx, int sy, int borderColor) {
        g.fill(sx, sy, sx + 18, sy + 18, 0xFF0D0812);
        drawBorder(g, sx, sy, 18, 18, borderColor);
    }

    private void drawBorder(GuiGraphics g, int bx, int by, int bw, int bh, int color) {
        g.fill(bx, by, bx + bw, by + 1, color);
        g.fill(bx, by + bh - 1, bx + bw, by + bh, color);
        g.fill(bx, by, bx + 1, by + bh, color);
        g.fill(bx + bw - 1, by, bx + bw, by + bh, color);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, Component.literal("🔥 Magma Forge"), this.titleLabelX, this.titleLabelY, 0xFFFFB74D, false);
        g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFCECAC5, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Manual button tooltip
        if (mouseX >= x + imageWidth - 26 && mouseX < x + imageWidth - 10 && mouseY >= y + 6 && mouseY < y + 18) {
            List<Component> tip = List.of(
                    Component.literal("📖 Open Obsidium Manual").withStyle(ChatFormatting.GOLD),
                    Component.literal("Jump to Chapter 2: Magma Forge 3×3").withStyle(ChatFormatting.GRAY)
            );
            g.renderTooltip(this.font, tip, Optional.empty(), mouseX, mouseY);
        }

        // Lava Tank Tooltip
        if (mouseX >= x + 14 && mouseX <= x + 30 && mouseY >= y + 28 && mouseY <= y + 104) {
            List<Component> tip = List.of(
                    Component.literal("Lava Tank").withStyle(ChatFormatting.GOLD),
                    Component.literal(String.format("%,d / 64,000 mB", this.menu.getFluidMb())).withStyle(ChatFormatting.YELLOW),
                    Component.literal("Insert Lava Bucket or Volcanite Bottle below.").withStyle(ChatFormatting.GRAY)
            );
            g.renderTooltip(this.font, tip, Optional.empty(), mouseX, mouseY);
        }

        // RF Energy Tooltip
        if (mouseX >= x + 34 && mouseX <= x + 46 && mouseY >= y + 28 && mouseY <= y + 126) {
            List<Component> tip = List.of(
                    Component.literal("RF Energy").withStyle(ChatFormatting.AQUA),
                    Component.literal(String.format("%,d / 10,000,000 RF", this.menu.getEnergy())).withStyle(ChatFormatting.WHITE),
                    Component.literal("2+ ★ Masterwork: -50% Energy Cost").withStyle(ChatFormatting.GREEN)
            );
            g.renderTooltip(this.font, tip, Optional.empty(), mouseX, mouseY);
        }

        // Mini-game bar / Strike button Tooltip
        if (mouseX >= x + 64 && mouseX <= x + 148 && mouseY >= y + 72 && mouseY <= y + 122) {
            List<Component> tip = List.of(
                    Component.literal("🔨 Magma Tempering").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.literal("Hit SPACE or Click in the GREEN zone:").withStyle(ChatFormatting.YELLOW),
                    Component.literal("• Perfect Hit: +35% progress & +1 ★").withStyle(ChatFormatting.GREEN),
                    Component.literal("• 2+ ★ Masterwork: -50% Cost + Bonus Item!").withStyle(ChatFormatting.AQUA)
            );
            g.renderTooltip(this.font, tip, Optional.empty(), mouseX, mouseY);
        }

        // Bonus Output Slot Tooltip when empty
        Slot bonusSlot = this.menu.getSlot(5);
        if (!bonusSlot.hasItem() && mouseX >= x + 163 && mouseX <= x + 181 && mouseY >= y + 89 && mouseY <= y + 107) {
            List<Component> tip = List.of(
                    Component.literal("★ Masterwork Bonus Slot").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                    Component.literal("Reach 2+ ★ while forging to get bonus items.").withStyle(ChatFormatting.GRAY)
            );
            g.renderTooltip(this.font, tip, Optional.empty(), mouseX, mouseY);
        }

        // Recipe Grid Tooltips
        int rpX = x + 196;
        int rpY = y + 23;
        int gridStartX = rpX + 11;
        int gridStartY = rpY + 18;
        int cellGap = 26;
        List<MagmaForgeRecipe> recipes = MagmaForgeCoreBlockEntity.RECIPES;
        int startIdx = this.recipePage * RECIPES_PER_PAGE;

        for (int i = 0; i < RECIPES_PER_PAGE; i++) {
            int rIdx = startIdx + i;
            if (rIdx >= recipes.size()) break;
            int col = i % GRID_COLS;
            int row = i / GRID_COLS;
            int cx = gridStartX + col * cellGap;
            int cy = gridStartY + row * 24;

            if (mouseX >= cx && mouseX < cx + 22 && mouseY >= cy && mouseY < cy + 22) {
                MagmaForgeRecipe rec = recipes.get(rIdx);
                List<Component> tip = new ArrayList<>();
                tip.add(rec.getResult().getHoverName().copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                        .append(Component.literal(" x" + rec.getResult().getCount()).withStyle(ChatFormatting.WHITE)));
                for (ItemStack ing : rec.getIngredients()) {
                    tip.add(Component.literal(" • " + ing.getCount() + "x " + ing.getHoverName().getString()).withStyle(ChatFormatting.GRAY));
                }
                if (!rec.getBonusResult().isEmpty()) {
                    tip.add(Component.literal("★ Bonus (2+ ★): +" + rec.getBonusResult().getCount() + "x "
                            + rec.getBonusResult().getHoverName().getString()).withStyle(ChatFormatting.AQUA));
                }
                tip.add(Component.literal(String.format("Cost: %,dk RF | %,d mB Lava", rec.getEnergyCost() / 1000, rec.getLavaCost())).withStyle(ChatFormatting.GOLD));
                tip.add(Component.literal("Click to auto-fill ingredients").withStyle(ChatFormatting.GREEN));
                g.renderTooltip(this.font, tip, Optional.empty(), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        if (button == 0) {
            if (mouseX >= x + imageWidth - 26 && mouseX < x + imageWidth - 10 && mouseY >= y + 6 && mouseY < y + 18) {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new MagmaForgeManualScreen(1));
                }
                return true;
            }
            // Check Hammer Strike Button (x+64..148, y+89..107)
            if (mouseX >= x + 64 && mouseX < x + 148 && mouseY >= y + 89 && mouseY < y + 107) {
                triggerHammerStrike();
                return true;
            }

            // Check Recipe Icon Grid & Pagination
            int rpX = x + 196;
            int rpY = y + 23;
            int rpW = 72;
            int gridStartX = rpX + 11;
            int gridStartY = rpY + 18;
            int cellGap = 26;

            List<MagmaForgeRecipe> recipes = MagmaForgeCoreBlockEntity.RECIPES;
            int totalPages = Math.max(1, (recipes.size() + RECIPES_PER_PAGE - 1) / RECIPES_PER_PAGE);
            int startIdx = this.recipePage * RECIPES_PER_PAGE;

            for (int i = 0; i < RECIPES_PER_PAGE; i++) {
                int rIdx = startIdx + i;
                if (rIdx >= recipes.size()) break;
                int col = i % GRID_COLS;
                int row = i / GRID_COLS;
                int cx = gridStartX + col * cellGap;
                int cy = gridStartY + row * 24;

                if (mouseX >= cx && mouseX < cx + 22 && mouseY >= cy && mouseY < cy + 22) {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 100 + rIdx);
                    }
                    return true;
                }
            }

            int navY = rpY + 93;
            if (mouseX >= rpX + 6 && mouseX < rpX + 20 && mouseY >= navY && mouseY < navY + 12) {
                this.recipePage = (this.recipePage - 1 + totalPages) % totalPages;
                return true;
            }
            if (mouseX >= rpX + rpW - 20 && mouseX < rpX + rpW - 6 && mouseY >= navY && mouseY < navY + 12) {
                this.recipePage = (this.recipePage + 1) % totalPages;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            triggerHammerStrike();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
