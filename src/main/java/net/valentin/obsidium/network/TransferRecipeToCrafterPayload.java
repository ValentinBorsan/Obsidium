package net.valentin.obsidium.network;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.valentin.obsidium.ObsidiumMod;

import java.util.ArrayList;
import java.util.List;

public record TransferRecipeToCrafterPayload(List<List<Item>> candidates, boolean maxCraft) implements CustomPacketPayload {
    public static final Type<TransferRecipeToCrafterPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "transfer_recipe_to_crafter")
    );

    public static final StreamCodec<FriendlyByteBuf, TransferRecipeToCrafterPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBoolean(payload.maxCraft());
                buf.writeVarInt(payload.candidates().size());
                for (List<Item> slotCandidates : payload.candidates()) {
                    buf.writeVarInt(slotCandidates.size());
                    for (Item item : slotCandidates) {
                        buf.writeVarInt(BuiltInRegistries.ITEM.getId(item));
                    }
                }
            },
            buf -> {
                boolean max = buf.readBoolean();
                int size = buf.readVarInt();
                if (size < 0 || size > 9) {
                    throw new IllegalArgumentException("Invalid crafter recipe slot count: " + size);
                }
                List<List<Item>> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    int count = buf.readVarInt();
                    if (count < 0 || count > 32) {
                        throw new IllegalArgumentException("Invalid crafter recipe candidate count: " + count);
                    }
                    List<Item> slotList = new ArrayList<>(count);
                    for (int j = 0; j < count; j++) {
                        slotList.add(BuiltInRegistries.ITEM.byId(buf.readVarInt()));
                    }
                    list.add(slotList);
                }
                return new TransferRecipeToCrafterPayload(list, max);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
