package net.valentin.obsidium.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.valentin.obsidium.ObsidiumMod;

/**
 * C2S: sends the Storage Terminal search text to the server, which owns the network inventory
 * and filters the displayed slots.
 */
public record TerminalSearchPayload(String query) implements CustomPacketPayload {
    public static final Type<TerminalSearchPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "terminal_search")
    );

    public static final StreamCodec<FriendlyByteBuf, TerminalSearchPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUtf(payload.query(), 128),
            buf -> new TerminalSearchPayload(buf.readUtf(128))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

