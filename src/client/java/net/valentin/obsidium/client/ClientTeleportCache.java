package net.valentin.obsidium.client;

import net.valentin.obsidium.network.TeleportNetworkSyncPayload;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the most recent S2C TeleportNetworkSyncPayload received from the server
 * so VolcaniteTeleporterScreen stays reactive across live updates.
 */
public final class ClientTeleportCache {
    @Nullable
    private static TeleportNetworkSyncPayload latest;
    private static int version = 0;

    private ClientTeleportCache() {}

    public static void update(TeleportNetworkSyncPayload payload) {
        latest = payload;
        version++;
    }

    @Nullable
    public static TeleportNetworkSyncPayload getLatest() {
        return latest;
    }

    public static int getVersion() {
        return version;
    }

    public static void clear() {
        latest = null;
        version = 0;
    }
}

