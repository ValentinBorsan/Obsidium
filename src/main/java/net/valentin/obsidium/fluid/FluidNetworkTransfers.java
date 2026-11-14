package net.valentin.obsidium.fluid;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import java.util.List;

public final class FluidNetworkTransfers {
    public static final long DROPLETS_PER_MB = 81L;

    private FluidNetworkTransfers() {}

    @FunctionalInterface
    public interface Endpoint {
        int transfer(FluidVariant fluid, int amount, boolean inserting, TransactionContext transaction);
    }

    /** One shared transaction, so both simulation and incomplete exact transfers roll back. */
    public static int transfer(List<Endpoint> endpoints, FluidVariant fluid, int amount,
                               boolean inserting, boolean simulate, boolean exact) {
        if (fluid.isBlank() || amount <= 0) return 0;
        try (Transaction transaction = Transaction.openOuter()) {
            int remaining = amount;
            for (Endpoint endpoint : endpoints) {
                int moved = endpoint.transfer(fluid, remaining, inserting, transaction);
                if (moved < 0 || moved > remaining) throw new IllegalStateException("Invalid fluid transfer amount");
                remaining -= moved;
                if (remaining == 0) break;
            }
            int moved = amount - remaining;
            if (exact && remaining != 0) return 0;
            if (!simulate && moved > 0) transaction.commit();
            return moved;
        }
    }

    /** Never commit fractional mB that cannot be represented by the caller. */
    public static int transferStorage(Storage<FluidVariant> storage, FluidVariant fluid, int amount,
                                      boolean inserting, TransactionContext parent) {
        long requested = amount * DROPLETS_PER_MB;
        long moved;
        try (Transaction attempt = parent.openNested()) {
            moved = inserting ? storage.insert(fluid, requested, attempt) : storage.extract(fluid, requested, attempt);
            if (moved < 0 || moved > requested) throw new IllegalStateException("Invalid storage fluid amount");
            // Always abort the probe. Some legacy adapters apply their changes in
            // a close callback, so committing a nested probe would apply them early.
        }
        long whole = moved - moved % DROPLETS_PER_MB;
        if (whole == 0) return 0;
        long actual = inserting ? storage.insert(fluid, whole, parent) : storage.extract(fluid, whole, parent);
        if (actual != whole) throw new IllegalStateException("Fluid storage disagreed with its simulation");
        return (int) (whole / DROPLETS_PER_MB);
    }
}
