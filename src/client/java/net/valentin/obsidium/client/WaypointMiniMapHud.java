package net.valentin.obsidium.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.valentin.obsidium.item.HephaesteanResonatorItem;
import net.valentin.obsidium.item.PloutonRodItem;
import net.valentin.obsidium.item.VolcaniteWarpGauntletItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Small terrain map that plots all saved Plouton's Rod / Resonator waypoints. */
public final class WaypointMiniMapHud {
    private static final int MAP_PIXELS = 24;
    private static final int PIXEL_SIZE = 3;
    private static final int MAP_SIZE = MAP_PIXELS * PIXEL_SIZE;
    private static final int MAP_RANGE_BLOCKS = 64;
    private static final int PANEL_WIDTH = 142;
    private static final int PANEL_HEIGHT = 139;

    private static int[] terrainColors = new int[MAP_PIXELS * MAP_PIXELS];
    private static List<Waypoint> waypoints = List.of();
    private static BlockPos sampledCenter;
    private static String sampledDimension = "";
    private static long lastSampleTick = Long.MIN_VALUE;

    private WaypointMiniMapHud() {}

    public static void tick(Minecraft client) {
        if (client.level == null || client.player == null || client.screen != null) {
            waypoints = List.of();
            sampledCenter = null;
            lastSampleTick = Long.MIN_VALUE;
            return;
        }

        long gameTick = client.level.getGameTime();
        if (lastSampleTick == gameTick) return;
        lastSampleTick = gameTick;

        waypoints = collectWaypoints(client);
        BlockPos playerPos = client.player.blockPosition();
        String dimension = client.level.dimension().location().toString();
        if (sampledCenter == null || !sampledCenter.equals(playerPos) || !sampledDimension.equals(dimension)
                || gameTick % 5 == 0) {
            sampledCenter = playerPos;
            sampledDimension = dimension;
            sampleTerrain(client);
        }
    }

    private static List<Waypoint> collectWaypoints(Minecraft client) {
        List<Waypoint> found = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ItemStack stack : client.player.getInventory().items) addWaypoint(stack, client, found, seen);
        for (ItemStack stack : client.player.getInventory().offhand) addWaypoint(stack, client, found, seen);
        return List.copyOf(found);
    }

    private static void addWaypoint(ItemStack stack, Minecraft client, List<Waypoint> found, Set<String> seen) {
        if (!(stack.getItem() instanceof PloutonRodItem)
                && !(stack.getItem() instanceof HephaesteanResonatorItem)
                && !(stack.getItem() instanceof VolcaniteWarpGauntletItem)) return;

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (!tag.contains("WpX")) return;

        BlockPos pos = new BlockPos(tag.getInt("WpX"), tag.getInt("WpY"), tag.getInt("WpZ"));
        String dimension = tag.getString("WpDim");
        if (dimension.isBlank()) dimension = client.level.dimension().location().toString();
        String key = dimension + ":" + pos.asLong();
        if (!seen.add(key)) return;

        found.add(new Waypoint(pos, dimension));
    }

    private static void sampleTerrain(Minecraft client) {
        int centerX = sampledCenter.getX();
        int centerZ = sampledCenter.getZ();
        int index = 0;
        for (int mapZ = 0; mapZ < MAP_PIXELS; mapZ++) {
            for (int mapX = 0; mapX < MAP_PIXELS; mapX++) {
                int worldX = centerX + (mapX - MAP_PIXELS / 2) * MAP_RANGE_BLOCKS / (MAP_PIXELS / 2);
                int worldZ = centerZ + (mapZ - MAP_PIXELS / 2) * MAP_RANGE_BLOCKS / (MAP_PIXELS / 2);
                int color = 0xFF171020;
                if (client.level.getChunkSource().hasChunk(worldX >> 4, worldZ >> 4)) {
                    int surfaceY = client.level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ);
                    BlockPos surfacePos = new BlockPos(worldX, surfaceY - 1, worldZ);
                    BlockState surface = client.level.getBlockState(surfacePos);
                    int mapColor = surface.getMapColor(client.level, surfacePos).col;
                    color = mapColor == 0 ? 0xFF25202A : 0xFF000000 | mapColor;
                }
                terrainColors[index++] = color;
            }
        }
    }

    public static void render(GuiGraphics graphics, Minecraft client) {
        if (client.player == null || client.level == null || client.screen != null || waypoints.isEmpty()) return;

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int left = screenWidth - PANEL_WIDTH - 8;
        int top = 8;
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xD9100B16);
        border(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFFFF7A18);
        graphics.drawString(client.font, "WAYPOINT MAP", left + 7, top + 5, 0xFFFFCC55, false);

        int mapLeft = left + (PANEL_WIDTH - MAP_SIZE) / 2;
        int mapTop = top + 18;
        graphics.fill(mapLeft - 2, mapTop - 2, mapLeft + MAP_SIZE + 2, mapTop + MAP_SIZE + 2, 0xFF09070C);
        for (int z = 0; z < MAP_PIXELS; z++) {
            for (int x = 0; x < MAP_PIXELS; x++) {
                graphics.fill(mapLeft + x * PIXEL_SIZE, mapTop + z * PIXEL_SIZE,
                        mapLeft + (x + 1) * PIXEL_SIZE, mapTop + (z + 1) * PIXEL_SIZE,
                        terrainColors[z * MAP_PIXELS + x]);
            }
        }

        // Subtle map grid and north marker.
        for (int i = 1; i < 4; i++) {
            int grid = i * MAP_SIZE / 4;
            graphics.fill(mapLeft + grid, mapTop, mapLeft + grid + 1, mapTop + MAP_SIZE, 0x443C3545);
            graphics.fill(mapLeft, mapTop + grid, mapLeft + MAP_SIZE, mapTop + grid + 1, 0x443C3545);
        }
        graphics.drawString(client.font, "N", mapLeft + MAP_SIZE / 2 - 2, mapTop + 1, 0xFFFFFFFF, true);

        int centerX = mapLeft + MAP_SIZE / 2;
        int centerY = mapTop + MAP_SIZE / 2;
        Waypoint nearest = nearestWaypoint(client);
        for (Waypoint waypoint : waypoints) {
            if (!waypoint.dimension.equals(sampledDimension)) continue;
            boolean selected = waypoint == nearest;
            int color = selected ? 0xFFFFD34D : 0xFFE58BFF;
            int markerX = mapX(centerX, waypoint.pos.getX() - sampledCenter.getX());
            int markerY = mapY(centerY, waypoint.pos.getZ() - sampledCenter.getZ());
            graphics.fill(markerX - (selected ? 2 : 1), markerY - (selected ? 2 : 1),
                    markerX + (selected ? 3 : 2), markerY + (selected ? 3 : 2), color);
        }

        // Player position and facing arrow.
        graphics.fill(centerX - 2, centerY - 2, centerX + 3, centerY + 3, 0xFF35F1FF);
        double yaw = Math.toRadians(client.player.getYRot());
        int facingX = centerX - (int) Math.round(Math.sin(yaw) * 7.0);
        int facingY = centerY + (int) Math.round(Math.cos(yaw) * 7.0);
        line(graphics, centerX, centerY, facingX, facingY, 0xFFFFFFFF);

        int textY = mapTop + MAP_SIZE + 5;
        graphics.drawString(client.font, "You  " + client.player.getBlockX() + ", " + client.player.getBlockZ(),
                left + 7, textY, 0xFF8DEFFF, false);
        if (nearest != null) {
            double horizontal = Math.sqrt(client.player.distanceToSqr(nearest.pos.getX() + 0.5,
                    client.player.getY(), nearest.pos.getZ() + 0.5));
            String direction = PloutonRodItem.getDirectionString(client.player.blockPosition(), nearest.pos);
            graphics.drawString(client.font, "Target  " + Math.round(horizontal) + "m  " + direction,
                    left + 7, textY + 11, 0xFFFFD34D, false);
            graphics.drawString(client.font, "XYZ  " + nearest.pos.getX() + ", " + nearest.pos.getY() + ", " + nearest.pos.getZ(),
                    left + 7, textY + 22, 0xFFE8DDF0, false);
        } else {
            graphics.drawString(client.font, "Waypoint is in another dimension", left + 7, textY + 11, 0xFFE8DDF0, false);
        }
    }

    private static Waypoint nearestWaypoint(Minecraft client) {
        Waypoint nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        String dimension = client.level.dimension().location().toString();
        for (Waypoint waypoint : waypoints) {
            if (!waypoint.dimension.equals(dimension)) continue;
            double distance = client.player.distanceToSqr(waypoint.pos.getX() + 0.5,
                    waypoint.pos.getY() + 0.5, waypoint.pos.getZ() + 0.5);
            if (distance < nearestDistance) {
                nearest = waypoint;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static int mapX(int center, int delta) {
        return clamp(center + delta * MAP_SIZE / (MAP_RANGE_BLOCKS * 2), center - MAP_SIZE / 2 + 1, center + MAP_SIZE / 2 - 2);
    }

    private static int mapY(int center, int delta) {
        return clamp(center + delta * MAP_SIZE / (MAP_RANGE_BLOCKS * 2), center - MAP_SIZE / 2 + 1, center + MAP_SIZE / 2 - 2);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void line(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int error = dx - dy;
        while (true) {
            graphics.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int twiceError = error * 2;
            if (twiceError > -dy) { error -= dy; x0 += sx; }
            if (twiceError < dx) { error += dx; y0 += sy; }
        }
    }

    private static void border(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private record Waypoint(BlockPos pos, String dimension) {}
}
