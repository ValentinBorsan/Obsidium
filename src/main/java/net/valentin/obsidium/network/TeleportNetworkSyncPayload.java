package net.valentin.obsidium.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.valentin.obsidium.ObsidiumMod;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C: Synchronizes available teleporter pads, waypoints, RF costs, and pad configuration
 * to the client for the VolcaniteTeleporterScreen.
 */
public record TeleportNetworkSyncPayload(
        boolean fromBlock,
        BlockPos blockPos,
        boolean supremeSource,
        String sourceName,
        int sourceIcon,
        boolean sourcePublic,
        String pinnedTargetId,
        List<DestinationEntry> entries
) implements CustomPacketPayload {

    public static final int CAT_SYSTEM = 0;
    public static final int CAT_PAD = 1;
    public static final int CAT_WAYPOINT = 2;

    public record DestinationEntry(
            String id,
            String name,
            String dimension,
            int x,
            int y,
            int z,
            int iconIndex,
            int category,
            int distance,
            long rfCost,
            boolean crossDim,
            boolean canAfford
    ) {}

    public static final Type<TeleportNetworkSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "teleport_network_sync")
    );

    public static final StreamCodec<FriendlyByteBuf, TeleportNetworkSyncPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBoolean(payload.fromBlock());
                buf.writeBlockPos(payload.blockPos());
                buf.writeBoolean(payload.supremeSource());
                buf.writeUtf(payload.sourceName(), 64);
                buf.writeVarInt(payload.sourceIcon());
                buf.writeBoolean(payload.sourcePublic());
                buf.writeUtf(payload.pinnedTargetId(), 160);
                buf.writeVarInt(payload.entries().size());
                for (DestinationEntry e : payload.entries()) {
                    buf.writeUtf(e.id(), 160);
                    buf.writeUtf(e.name(), 64);
                    buf.writeUtf(e.dimension(), 96);
                    buf.writeInt(e.x());
                    buf.writeInt(e.y());
                    buf.writeInt(e.z());
                    buf.writeVarInt(e.iconIndex());
                    buf.writeVarInt(e.category());
                    buf.writeInt(e.distance());
                    buf.writeLong(e.rfCost());
                    buf.writeBoolean(e.crossDim());
                    buf.writeBoolean(e.canAfford());
                }
            },
            buf -> {
                boolean fromBlock = buf.readBoolean();
                BlockPos blockPos = buf.readBlockPos();
                boolean supreme = buf.readBoolean();
                String sourceName = buf.readUtf(64);
                int sourceIcon = buf.readVarInt();
                boolean sourcePublic = buf.readBoolean();
                String pinned = buf.readUtf(160);
                int size = buf.readVarInt();
                if (size < 0 || size > 512) {
                    throw new IllegalArgumentException("Invalid teleport destination count: " + size);
                }
                List<DestinationEntry> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    String id = buf.readUtf(160);
                    String name = buf.readUtf(64);
                    String dim = buf.readUtf(96);
                    int x = buf.readInt();
                    int y = buf.readInt();
                    int z = buf.readInt();
                    int icon = buf.readVarInt();
                    int cat = buf.readVarInt();
                    int dist = buf.readInt();
                    long cost = buf.readLong();
                    boolean crossDim = buf.readBoolean();
                    boolean canAfford = buf.readBoolean();
                    list.add(new DestinationEntry(id, name, dim, x, y, z, icon, cat, dist, cost, crossDim, canAfford));
                }
                return new TeleportNetworkSyncPayload(fromBlock, blockPos, supreme, sourceName, sourceIcon, sourcePublic, pinned, list);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

