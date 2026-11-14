package net.valentin.obsidium.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.valentin.obsidium.ObsidiumMod;

public record GeyserJumpPayload() implements CustomPacketPayload {
    public static final Type<GeyserJumpPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "geyser_jump"));

    public static final StreamCodec<FriendlyByteBuf, GeyserJumpPayload> CODEC = StreamCodec.of(
        (buf, payload) -> {},
        buf -> new GeyserJumpPayload()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
