package net.valentin.obsidium.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.valentin.obsidium.ObsidiumMod;

public record SortContainerPayload(boolean descending) implements CustomPacketPayload {
    public static final Type<SortContainerPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "sort_container")
    );

    public static final StreamCodec<FriendlyByteBuf, SortContainerPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeBoolean(payload.descending()),
            buf -> new SortContainerPayload(buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

