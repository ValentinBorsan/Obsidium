package net.valentin.obsidium.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.valentin.obsidium.ObsidiumMod;

/**
 * C2S: Player interactions from the VolcaniteTeleporterScreen (warp, pin, rename pad, bookmark/delete waypoint).
 */
public record TeleportActionPayload(
        int action,
        boolean fromBlock,
        BlockPos blockPos,
        String targetId,
        String name,
        int iconIndex,
        boolean isPublic
) implements CustomPacketPayload {

    public static final int ACTION_WARP = 0;
    public static final int ACTION_PIN = 1;
    public static final int ACTION_UPDATE_PAD = 2;
    public static final int ACTION_SAVE_WAYPOINT = 3;
    public static final int ACTION_DELETE_WAYPOINT = 4;

    public static final Type<TeleportActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "teleport_action")
    );

    public static final StreamCodec<FriendlyByteBuf, TeleportActionPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.action());
                buf.writeBoolean(payload.fromBlock());
                buf.writeBlockPos(payload.blockPos());
                buf.writeUtf(payload.targetId(), 160);
                buf.writeUtf(payload.name(), 64);
                buf.writeVarInt(payload.iconIndex());
                buf.writeBoolean(payload.isPublic());
            },
            buf -> new TeleportActionPayload(
                    buf.readVarInt(),
                    buf.readBoolean(),
                    buf.readBlockPos(),
                    buf.readUtf(160),
                    buf.readUtf(64),
                    buf.readVarInt(),
                    buf.readBoolean()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

