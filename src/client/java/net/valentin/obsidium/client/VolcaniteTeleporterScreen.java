package net.valentin.obsidium.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.VolcaniteTeleporterMenu;
import net.valentin.obsidium.network.TeleportActionPayload;
import net.valentin.obsidium.network.TeleportNetworkSyncPayload;
import net.valentin.obsidium.network.TeleportNetworkSyncPayload.DestinationEntry;
import net.valentin.obsidium.screen.MagmaForgeManualScreen;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Quantum Teleporter Network Matrix UI shared by Basic Volcanic Teleporters,
 * Volcanic Core Teleporters, and the portable Volcanite Warp Gauntlet.
 */
public class VolcaniteTeleporterScreen extends AbstractContainerScreen<VolcaniteTeleporterMenu> {
    private static final int VISIBLE_ROWS = 5;
    private static final int ROW_HEIGHT = 25;

    private static final int FILTER_ALL = 0;
    private static final int FILTER_PADS = 1;
    private static final int FILTER_SAVED = 2;

    private EditBox topEditBox;
    private EditBox searchBox;

    private int filterMode = FILTER_ALL;
    private int scrollOffset = 0;
    private int lastSyncedVersion = -1;

    private int editIcon = 0;
    private boolean editPublic = true;
    private boolean initializedTopBox = false;

    public VolcaniteTeleporterScreen(VolcaniteTeleporterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 212;
        this.titleLabelX = 1000;
        this.inventoryLabelX = 1000;
    }

    @Override
    protected void init() {
        super.init();
        int i = this.leftPos;
        int j = this.topPos;

        TeleportNetworkSyncPayload sync = ClientTeleportCache.getLatest();
        boolean fromBlock = sync != null ? sync.fromBlock() : (this.menu.getBlockEntity() != null);

        String prevTop = this.topEditBox != null ? this.topEditBox.getValue() : "";
        String prevSearch = this.searchBox != null ? this.searchBox.getValue() : "";

        int topW = fromBlock ? 128 : 152;
        this.topEditBox = new EditBox(this.font, i + 32, j + 29, topW, 11, Component.literal("Name"));
        this.topEditBox.setMaxLength(32);
        this.topEditBox.setBordered(false);
        this.topEditBox.setTextColor(0xFFF4F6FA);
        this.topEditBox.setHint(Component.literal(fromBlock ? "Pad Name..." : "New Waypoint Name..."));
        if (!prevTop.isEmpty()) {
            this.topEditBox.setValue(prevTop);
        }
        this.addRenderableWidget(this.topEditBox);

        this.searchBox = new EditBox(this.font, i + 134, j + 50, 110, 11, Component.literal("Search"));
        this.searchBox.setMaxLength(32);
        this.searchBox.setBordered(false);
        this.searchBox.setTextColor(0xFF9EEFFF);
        this.searchBox.setHint(Component.literal("Filter destinations..."));
        this.searchBox.setValue(prevSearch);
        this.searchBox.setResponder(s -> this.scrollOffset = 0);
        this.addRenderableWidget(this.searchBox);

        pullSyncIfUpdated(true);
    }

    private void pullSyncIfUpdated(boolean forceInit) {
        int ver = ClientTeleportCache.getVersion();
        TeleportNetworkSyncPayload sync = ClientTeleportCache.getLatest();
        if (sync != null && (ver != this.lastSyncedVersion || forceInit)) {
            this.lastSyncedVersion = ver;
            if (sync.fromBlock()) {
                this.editIcon = sync.sourceIcon();
                this.editPublic = sync.sourcePublic();
                if (this.topEditBox != null && (!this.topEditBox.isFocused() || !this.initializedTopBox)) {
                    this.topEditBox.setValue(sync.sourceName());
                    this.initializedTopBox = true;
                }
            }
        }
    }

    private static ItemStack getPresetIcon(int index) {
        return switch (Math.floorMod(index, 8)) {
            case 0 -> new ItemStack(ObsidiumMod.VOLCANIC_CORE_TELEPORTER_ITEM);
            case 1 -> new ItemStack(Items.RED_BED);
            case 2 -> new ItemStack(ObsidiumMod.BASIC_VOLCANIC_TELEPORTER_ITEM);
            case 3 -> new ItemStack(ObsidiumMod.OLYMPIAN_HADRON_COLLIDER_ITEM);
            case 4 -> new ItemStack(Items.BLAST_FURNACE);
            case 5 -> new ItemStack(Items.ENDER_PEARL);
            case 6 -> new ItemStack(Items.COMPASS);
            default -> new ItemStack(ObsidiumMod.VOLCANITE_WARP_GAUNTLET);
        };
    }

    private static String formatCompactRf(long rf) {
        if (rf >= 1_000_000L) return String.format("%.1fM", rf / 1_000_000.0);
        if (rf >= 1_000L) return String.format("%.1fk", rf / 1_000.0);
        return String.valueOf(rf);
    }

    private static String shortDimensionName(String dimId) {
        return switch (dimId) {
            case "minecraft:overworld" -> "OW";
            case "minecraft:the_nether" -> "NETHER";
            case "minecraft:the_end" -> "END";
            default -> {
                int colon = dimId.indexOf(':');
                String raw = colon >= 0 ? dimId.substring(colon + 1) : dimId;
                yield raw.length() > 8 ? raw.substring(0, 8).toUpperCase(Locale.ROOT) : raw.toUpperCase(Locale.ROOT);
            }
        };
    }

    private static int dimBadgeColor(String dimId) {
        return switch (dimId) {
            case "minecraft:overworld" -> 0xFF39D98A;
            case "minecraft:the_nether" -> 0xFFFF6644;
            case "minecraft:the_end" -> 0xFFD96BFF;
            default -> 0xFF66E8FF;
        };
    }

    private List<DestinationEntry> getFilteredEntries() {
        TeleportNetworkSyncPayload sync = ClientTeleportCache.getLatest();
        if (sync == null) return List.of();
        String query = this.searchBox != null ? this.searchBox.getValue().trim().toLowerCase(Locale.ROOT) : "";

        List<DestinationEntry> out = new ArrayList<>();
        for (DestinationEntry entry : sync.entries()) {
            if (this.filterMode == FILTER_PADS && entry.category() != TeleportNetworkSyncPayload.CAT_PAD) continue;
            if (this.filterMode == FILTER_SAVED
                    && entry.category() != TeleportNetworkSyncPayload.CAT_WAYPOINT
                    && entry.category() != TeleportNetworkSyncPayload.CAT_SYSTEM) {
                continue;
            }
            if (!query.isEmpty()) {
                String hay = (entry.name() + " " + entry.dimension()).toLowerCase(Locale.ROOT);
                if (!hay.contains(query)) continue;
            }
            out.add(entry);
        }
        return out;
    }

    private void sendAction(int action, String targetId, String name, int icon, boolean isPublic) {
        TeleportNetworkSyncPayload sync = ClientTeleportCache.getLatest();
        boolean fromBlock = sync != null ? sync.fromBlock() : (this.menu.getBlockEntity() != null);
        BlockPos pos = sync != null ? sync.blockPos()
                : (this.menu.getBlockEntity() != null ? this.menu.getBlockEntity().getBlockPos() : BlockPos.ZERO);
        ClientPlayNetworking.send(new TeleportActionPayload(action, fromBlock, pos, targetId, name, icon, isPublic));
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawSmallString(GuiGraphics g, String text, int x, int y, float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0f);
        g.drawString(this.font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        pullSyncIfUpdated(false);

        int i = this.leftPos;
        int j = this.topPos;

        VolcaniteGuiTheme.renderExteriorFrame(g, i, j, this.imageWidth, this.imageHeight, true);

        TeleportNetworkSyncPayload sync = ClientTeleportCache.getLatest();
        boolean fromBlock = sync != null ? sync.fromBlock() : (this.menu.getBlockEntity() != null);
        boolean supreme = sync != null ? sync.supremeSource() : this.menu.isSupreme();

        // Header divider & Title
        g.fill(i + 7, j + 20, i + this.imageWidth - 7, j + 21, 0xFF28344A);
        String titleStr = fromBlock
                ? (supreme ? "§b⚛ CORE TELEPORTER" : "§6⛩ VOLCANIC TELEPORTER")
                : "§d◈ WARP GAUNTLET MATRIX";
        g.drawString(this.font, titleStr, i + 9, j + 8, 0xFFFFFFFF, false);

        // Header RF meter pill (138..233, 6..18)
        long storedRf = this.menu.getStoredRf();
        long maxRf = Math.max(1L, this.menu.getMaxRf());
        double rfRatio = Math.min(1.0, (double) storedRf / maxRf);
        g.fill(i + 138, j + 6, i + 233, j + 18, 0xFF0A0C14);
        drawBorder(g, i + 138, j + 6, 95, 12, supreme ? 0xFF0099CC : 0xFFB06818);
        int fillW = (int) Math.round(91 * rfRatio);
        if (fillW > 0) {
            g.fill(i + 140, j + 15, i + 140 + fillW, j + 16, supreme ? 0xFF00E5FF : 0xFFFFAA00);
        }
        String rfTxt = formatCompactRf(storedRf) + " / " + formatCompactRf(maxRf) + " RF";
        drawSmallString(g, rfTxt, i + 142, j + 8, 0.72f, 0xFFE6F4FA);

        // [?] Manual button (236..248, 6..18)
        boolean hoverHelp = mouseX >= i + 236 && mouseX <= i + 248 && mouseY >= j + 6 && mouseY <= j + 18;
        g.fill(i + 236, j + 6, i + 248, j + 18, hoverHelp ? 0xFF253C55 : 0xFF141E2E);
        drawBorder(g, i + 236, j + 6, 12, 12, hoverHelp ? 0xFF00E5FF : 0xFF557799);
        g.drawString(this.font, "?", i + 240, j + 8, hoverHelp ? 0xFFFFFFFF : 0xFF88CCFF, false);

        // Context bar background (8..248, 24..42)
        g.fill(i + 8, j + 24, i + 248, j + 42, 0xFF0F131E);
        drawBorder(g, i + 8, j + 24, 240, 18, 0xFF2B354B);

        // Icon selector slot (10..27, 25..41)
        boolean hoverIcon = mouseX >= i + 10 && mouseX <= i + 27 && mouseY >= j + 25 && mouseY <= j + 41;
        g.fill(i + 10, j + 25, i + 27, j + 41, hoverIcon ? 0xFF222E44 : 0xFF151C2B);
        drawBorder(g, i + 10, j + 25, 17, 16, hoverIcon ? 0xFF00E5FF : 0xFF40506E);
        g.renderItem(getPresetIcon(this.editIcon), i + 10, j + 25);

        // Top EditBox box
        int boxW = fromBlock ? 132 : 156;
        g.fill(i + 29, j + 26, i + 29 + boxW, j + 40, 0xFF07090F);
        drawBorder(g, i + 29, j + 26, boxW, 14, this.topEditBox != null && this.topEditBox.isFocused() ? 0xFF00E5FF : 0xFF2A3448);

        if (fromBlock) {
            // Privacy toggle (163..206, 26..40)
            boolean hoverPub = mouseX >= i + 163 && mouseX <= i + 206 && mouseY >= j + 26 && mouseY <= j + 40;
            int pubCol = this.editPublic ? 0xFF2CE67A : 0xFFFFB020;
            g.fill(i + 163, j + 26, i + 206, j + 40, hoverPub ? 0xFF1E2D3D : 0xFF121926);
            drawBorder(g, i + 163, j + 26, 43, 14, pubCol);
            drawSmallString(g, this.editPublic ? "PUBLIC" : "PRIVATE", i + 168, j + 30, 0.78f, pubCol);

            // Save button (209..246, 26..40)
            boolean hoverSave = mouseX >= i + 209 && mouseX <= i + 246 && mouseY >= j + 26 && mouseY <= j + 40;
            g.fill(i + 209, j + 26, i + 246, j + 40, hoverSave ? 0xFF194455 : 0xFF112936);
            drawBorder(g, i + 209, j + 26, 37, 14, hoverSave ? 0xFF00E5FF : 0xFF2E859E);
            drawSmallString(g, "SAVE", i + 218, j + 30, 0.8f, 0xFFE6FAFF);
        } else {
            // Bookmark button (+Mark Here) (188..246, 26..40)
            boolean hoverMark = mouseX >= i + 188 && mouseX <= i + 246 && mouseY >= j + 26 && mouseY <= j + 40;
            g.fill(i + 188, j + 26, i + 246, j + 40, hoverMark ? 0xFF1A4638 : 0xFF102A22);
            drawBorder(g, i + 188, j + 26, 58, 14, hoverMark ? 0xFF42FFAA : 0xFF288B63);
            drawSmallString(g, "+MARK HERE", i + 193, j + 30, 0.78f, 0xFFB8FFE2);
        }

        // Filter tabs (All / Pads / Saved)
        drawFilterTab(g, i + 8, j + 45, 34, "ALL", this.filterMode == FILTER_ALL, mouseX, mouseY);
        drawFilterTab(g, i + 44, j + 45, 38, "PADS", this.filterMode == FILTER_PADS, mouseX, mouseY);
        drawFilterTab(g, i + 84, j + 45, 44, "SAVED", this.filterMode == FILTER_SAVED, mouseX, mouseY);

        // Search box frame (131..248, 45..60)
        g.fill(i + 131, j + 45, i + 248, j + 60, 0xFF07090F);
        drawBorder(g, i + 131, j + 45, 117, 15, this.searchBox != null && this.searchBox.isFocused() ? 0xFF00E5FF : 0xFF2A3448);

        // Destination List Area
        g.fill(i + 8, j + 63, i + 248, j + 191, 0xFF090B12);
        drawBorder(g, i + 8, j + 63, 240, 128, 0xFF232C3F);

        List<DestinationEntry> filtered = getFilteredEntries();
        int maxScroll = Math.max(0, filtered.size() - VISIBLE_ROWS);
        if (this.scrollOffset > maxScroll) this.scrollOffset = maxScroll;
        String pinnedId = sync != null ? sync.pinnedTargetId() : "";

        if (filtered.isEmpty()) {
            String msg = sync == null ? "Synchronizing Quantum Matrix..." : "No matching destinations found.";
            g.drawString(this.font, msg, i + 44, j + 122, 0xFF708098, false);
        } else {
            for (int r = 0; r < VISIBLE_ROWS; r++) {
                int idx = this.scrollOffset + r;
                if (idx >= filtered.size()) break;
                DestinationEntry entry = filtered.get(idx);

                int rx = i + 10;
                int ry = j + 65 + r * ROW_HEIGHT;
                int rw = 230;
                int rh = 23;
                boolean isPinned = entry.id().equals(pinnedId);
                boolean rowHover = mouseX >= rx && mouseX <= rx + rw && mouseY >= ry && mouseY <= ry + rh;

                int bg = isPinned ? 0xFF102836 : (rowHover ? 0xFF151C2B : 0xFF0E131E);
                int border = isPinned ? 0xFF00E5FF : (rowHover ? 0xFF3E5278 : 0xFF222B3E);
                g.fill(rx, ry, rx + rw, ry + rh, bg);
                drawBorder(g, rx, ry, rw, rh, border);

                // Row Icon
                g.renderItem(getPresetIcon(entry.iconIndex()), rx + 3, ry + 3);

                // Name (clipped cleanly)
                String dispName = this.font.plainSubstrByWidth(entry.name(), 96);
                if (isPinned) dispName = "★ " + dispName;
                drawSmallString(g, dispName, rx + 22, ry + 4, 0.88f, isPinned ? 0xFF73F2FF : 0xFFF2F5FA);

                // Dimension pill on top right of info column
                String dimShort = shortDimensionName(entry.dimension());
                int dimCol = dimBadgeColor(entry.dimension());
                drawSmallString(g, dimShort, rx + 22, ry + 13, 0.72f, dimCol);

                // Distance & RF cost
                String distStr = entry.crossDim() ? "X-DIM" : (entry.distance() + "m");
                String costStr = formatCompactRf(entry.rfCost()) + " RF";
                int metaCol = entry.canAfford() ? 0xFFA8C0D8 : 0xFFFF5E6C;
                drawSmallString(g, "• " + distStr + " • " + costStr, rx + 58, ry + 13, 0.72f, metaCol);

                // Delete button for personal waypoints (rx + 148..161, ry + 4..19)
                if (entry.category() == TeleportNetworkSyncPayload.CAT_WAYPOINT) {
                    boolean hoverDel = mouseX >= rx + 148 && mouseX <= rx + 161 && mouseY >= ry + 4 && mouseY <= ry + 19;
                    g.fill(rx + 148, ry + 4, rx + 161, ry + 19, hoverDel ? 0xFF551A22 : 0xFF281218);
                    drawBorder(g, rx + 148, ry + 4, 13, 15, hoverDel ? 0xFFFF5566 : 0xFF883344);
                    drawSmallString(g, "X", rx + 152, ry + 8, 0.8f, 0xFFFF8899);
                }

                // [PIN] button (rx + 164..185, ry + 4..19)
                boolean hoverPin = mouseX >= rx + 164 && mouseX <= rx + 185 && mouseY >= ry + 4 && mouseY <= ry + 19;
                g.fill(rx + 164, ry + 4, rx + 185, ry + 19, isPinned ? 0xFF144B5C : (hoverPin ? 0xFF24334C : 0xFF141B29));
                drawBorder(g, rx + 164, ry + 4, 21, 15, isPinned ? 0xFF00E5FF : (hoverPin ? 0xFF66B2FF : 0xFF394966));
                drawSmallString(g, "PIN", rx + 168, ry + 8, 0.72f, isPinned ? 0xFF88FFFF : 0xFFB0C4DE);

                // [WARP] button (rx + 188..227, ry + 4..19)
                boolean hoverWarp = mouseX >= rx + 188 && mouseX <= rx + 227 && mouseY >= ry + 4 && mouseY <= ry + 19;
                int wBg = !entry.canAfford() ? 0xFF1E161B : (hoverWarp ? 0xFF1F5D4A : 0xFF143C32);
                int wBorder = !entry.canAfford() ? 0xFF663340 : (hoverWarp ? 0xFF55FFBA : 0xFF2CA878);
                int wTxtCol = !entry.canAfford() ? 0xFFAA6670 : 0xFFD2FFEE;
                g.fill(rx + 188, ry + 4, rx + 227, ry + 19, wBg);
                drawBorder(g, rx + 188, ry + 4, 39, 15, wBorder);
                drawSmallString(g, "WARP", rx + 195, ry + 8, 0.8f, wTxtCol);
            }
        }

        // Scrollbar track (242..246, 65..190)
        g.fill(i + 242, j + 65, i + 246, j + 190, 0xFF121826);
        if (filtered.size() > VISIBLE_ROWS) {
            int trackH = 125;
            int thumbH = Math.max(18, (VISIBLE_ROWS * trackH) / filtered.size());
            int thumbY = j + 65 + (maxScroll == 0 ? 0 : (this.scrollOffset * (trackH - thumbH)) / maxScroll);
            g.fill(i + 242, thumbY, i + 246, thumbY + thumbH, 0xFF00E5FF);
        }

        // Footer hint
        String pinHint = fromBlock
                ? "★ Pin target -> Sneak 1.25s on pad for Quick-Warp"
                : "★ Pin target -> Shift+Right-Click air for Quick-Warp";
        drawSmallString(g, pinHint, i + 9, j + 197, 0.72f, 0xFF8AA2BE);
        String rangeCap = supreme ? "∞ Cross-Dim" : "Max 2,500m";
        drawSmallString(g, rangeCap, i + 206, j + 197, 0.72f, supreme ? 0xFF66F2FF : 0xFFFFB84D);
    }

    private void drawFilterTab(GuiGraphics g, int x, int y, int w, String label, boolean selected, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 15;
        g.fill(x, y, x + w, y + 15, selected ? 0xFF19354A : (hover ? 0xFF1C2333 : 0xFF101521));
        drawBorder(g, x, y, w, 15, selected ? 0xFF00E5FF : (hover ? 0xFF4D668E : 0xFF283247));
        int tx = x + (w - (int) (this.font.width(label) * 0.78f)) / 2;
        drawSmallString(g, label, tx, y + 5, 0.78f, selected ? 0xFF99F6FF : 0xFFA4B4CC);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Rendered cleanly inside renderBg/render
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderHoveredTooltips(g, mouseX, mouseY);
    }

    private void renderHoveredTooltips(GuiGraphics g, int mouseX, int mouseY) {
        int i = this.leftPos;
        int j = this.topPos;
        TeleportNetworkSyncPayload sync = ClientTeleportCache.getLatest();
        boolean fromBlock = sync != null ? sync.fromBlock() : (this.menu.getBlockEntity() != null);
        boolean supreme = sync != null ? sync.supremeSource() : this.menu.isSupreme();

        if (mouseX >= i + 236 && mouseX <= i + 248 && mouseY >= j + 6 && mouseY <= j + 18) {
            g.renderTooltip(this.font, Component.literal("§bOpen Teleportation & Collider Manual §7[?]"), mouseX, mouseY);
            return;
        }
        if (mouseX >= i + 138 && mouseX <= i + 233 && mouseY >= j + 6 && mouseY <= j + 18) {
            g.renderTooltip(this.font, Component.literal(String.format("§6Buffer: %,d / %,d RF",
                    this.menu.getStoredRf(), this.menu.getMaxRf())), mouseX, mouseY);
            return;
        }
        if (mouseX >= i + 10 && mouseX <= i + 27 && mouseY >= j + 25 && mouseY <= j + 41) {
            g.renderTooltip(this.font, Component.literal(fromBlock
                    ? "§eClick to cycle Teleporter Icon (remember to SAVE)"
                    : "§eClick to cycle Icon for new Waypoint"), mouseX, mouseY);
            return;
        }

        List<DestinationEntry> filtered = getFilteredEntries();
        for (int r = 0; r < VISIBLE_ROWS; r++) {
            int idx = this.scrollOffset + r;
            if (idx >= filtered.size()) break;
            DestinationEntry entry = filtered.get(idx);
            int rx = i + 10;
            int ry = j + 65 + r * ROW_HEIGHT;
            if (mouseX >= rx && mouseX <= rx + 230 && mouseY >= ry && mouseY <= ry + 23) {
                List<Component> lines = new ArrayList<>();
                lines.add(Component.literal("§b" + entry.name()));
                lines.add(Component.literal(String.format("§7Coords: §f%d, %d, %d §8(%s)",
                        entry.x(), entry.y(), entry.z(), entry.dimension())));
                lines.add(Component.literal(String.format("§6Jump Cost: §e%,d RF", entry.rfCost())));
                if (!entry.canAfford()) {
                    if (!supreme && (entry.crossDim() || entry.distance() > 2500)) {
                        lines.add(Component.literal("§cRequires Volcanic Core Teleporter (Cross-Dim / >2,500m)"));
                    } else {
                        lines.add(Component.literal("§cInsufficient RF energy stored!"));
                    }
                } else {
                    lines.add(Component.literal("§aClick [WARP] to teleport, or [PIN] for Quick-Warp"));
                }
                g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<DestinationEntry> filtered = getFilteredEntries();
        int maxScroll = Math.max(0, filtered.size() - VISIBLE_ROWS);
        if (maxScroll > 0) {
            int dir = scrollY > 0 ? -1 : 1;
            this.scrollOffset = Math.max(0, Math.min(maxScroll, this.scrollOffset + dir));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int i = this.leftPos;
            int j = this.topPos;
            TeleportNetworkSyncPayload sync = ClientTeleportCache.getLatest();
            boolean fromBlock = sync != null ? sync.fromBlock() : (this.menu.getBlockEntity() != null);

            // [?] Manual
            if (mouseX >= i + 236 && mouseX <= i + 248 && mouseY >= j + 6 && mouseY <= j + 18) {
                playClick();
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new MagmaForgeManualScreen(7, 3));
                }
                return true;
            }

            // Icon cycle
            if (mouseX >= i + 10 && mouseX <= i + 27 && mouseY >= j + 25 && mouseY <= j + 41) {
                playClick();
                this.editIcon = (this.editIcon + 1) % 8;
                return true;
            }

            if (fromBlock) {
                if (mouseX >= i + 163 && mouseX <= i + 206 && mouseY >= j + 26 && mouseY <= j + 40) {
                    playClick();
                    this.editPublic = !this.editPublic;
                    sendAction(TeleportActionPayload.ACTION_UPDATE_PAD, "", this.topEditBox.getValue(), this.editIcon, this.editPublic);
                    return true;
                }
                if (mouseX >= i + 209 && mouseX <= i + 246 && mouseY >= j + 26 && mouseY <= j + 40) {
                    playClick();
                    sendAction(TeleportActionPayload.ACTION_UPDATE_PAD, "", this.topEditBox.getValue(), this.editIcon, this.editPublic);
                    return true;
                }
            } else {
                if (mouseX >= i + 188 && mouseX <= i + 246 && mouseY >= j + 26 && mouseY <= j + 40) {
                    playClick();
                    sendAction(TeleportActionPayload.ACTION_SAVE_WAYPOINT, "", this.topEditBox.getValue(), this.editIcon, true);
                    this.topEditBox.setValue("");
                    return true;
                }
            }

            // Filter tabs
            if (mouseY >= j + 45 && mouseY <= j + 60) {
                if (mouseX >= i + 8 && mouseX <= i + 42) {
                    playClick();
                    this.filterMode = FILTER_ALL;
                    this.scrollOffset = 0;
                    return true;
                }
                if (mouseX >= i + 44 && mouseX <= i + 82) {
                    playClick();
                    this.filterMode = FILTER_PADS;
                    this.scrollOffset = 0;
                    return true;
                }
                if (mouseX >= i + 84 && mouseX <= i + 128) {
                    playClick();
                    this.filterMode = FILTER_SAVED;
                    this.scrollOffset = 0;
                    return true;
                }
            }

            // Row buttons
            List<DestinationEntry> filtered = getFilteredEntries();
            for (int r = 0; r < VISIBLE_ROWS; r++) {
                int idx = this.scrollOffset + r;
                if (idx >= filtered.size()) break;
                DestinationEntry entry = filtered.get(idx);
                int rx = i + 10;
                int ry = j + 65 + r * ROW_HEIGHT;

                if (entry.category() == TeleportNetworkSyncPayload.CAT_WAYPOINT
                        && mouseX >= rx + 148 && mouseX <= rx + 161 && mouseY >= ry + 4 && mouseY <= ry + 19) {
                    playClick();
                    sendAction(TeleportActionPayload.ACTION_DELETE_WAYPOINT, entry.id(), "", 0, true);
                    return true;
                }
                if (mouseX >= rx + 164 && mouseX <= rx + 185 && mouseY >= ry + 4 && mouseY <= ry + 19) {
                    playClick();
                    sendAction(TeleportActionPayload.ACTION_PIN, entry.id(), "", 0, true);
                    return true;
                }
                if (mouseX >= rx + 188 && mouseX <= rx + 227 && mouseY >= ry + 4 && mouseY <= ry + 19) {
                    playClick();
                    sendAction(TeleportActionPayload.ACTION_WARP, entry.id(), "", 0, true);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (this.topEditBox != null && this.topEditBox.isFocused()) {
                this.topEditBox.setFocused(false);
                return true;
            }
            if (this.searchBox != null && this.searchBox.isFocused()) {
                this.searchBox.setFocused(false);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (this.topEditBox != null && this.topEditBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                TeleportNetworkSyncPayload sync = ClientTeleportCache.getLatest();
                boolean fromBlock = sync != null ? sync.fromBlock() : (this.menu.getBlockEntity() != null);
                if (fromBlock) {
                    sendAction(TeleportActionPayload.ACTION_UPDATE_PAD, "", this.topEditBox.getValue(), this.editIcon, this.editPublic);
                } else {
                    sendAction(TeleportActionPayload.ACTION_SAVE_WAYPOINT, "", this.topEditBox.getValue(), this.editIcon, true);
                    this.topEditBox.setValue("");
                }
                this.topEditBox.setFocused(false);
                return true;
            }
            this.topEditBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (this.searchBox != null && this.searchBox.isFocused()) {
            this.searchBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void playClick() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5F, 1.25F);
        }
    }
}
