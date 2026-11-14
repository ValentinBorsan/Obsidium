package net.valentin.obsidium.mythology;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.valentin.obsidium.ObsidiumMod;

/**
 * Grants the code-driven criteria of the "Obsidium: Olympian Odyssey" quest tree
 * (data/obsidium/advancement/odyssey). Criteria use the {@code minecraft:impossible}
 * trigger, so they can only be awarded from here.
 */
public final class OdysseyAdvancements {
    private static final ResourceLocation WRATH_OF_THE_THUNDERER =
            ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "odyssey/wrath_of_the_thunderer");
    private static final ResourceLocation OLYMPIAN_MASTERY =
            ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "odyssey/olympian_mastery");

    private OdysseyAdvancements() {}

    public static void awardPact(ServerPlayer player, DivinePact pact) {
        if (pact == null || pact == DivinePact.NONE) return;
        if (pact == DivinePact.ZEUS) {
            award(player, WRATH_OF_THE_THUNDERER, "zeus");
        }
        award(player, OLYMPIAN_MASTERY, pact.getId());
    }

    private static void award(ServerPlayer player, ResourceLocation id, String criterion) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        AdvancementHolder holder = server.getAdvancements().get(id);
        if (holder != null) {
            player.getAdvancements().award(holder, criterion);
        }
    }
}
