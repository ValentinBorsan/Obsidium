package net.valentin.obsidium.fluid;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Adapts immediate, mB-based inventories to Fabric's nested transaction model. */
public final class TransactionalFluidAccess<S> extends SnapshotParticipant<S> {
    private final Supplier<S> snapshot;
    private final Consumer<S> restore;
    private final BiFunction<FluidVariant, Integer, Integer> insert;
    private final BiFunction<FluidVariant, Integer, Integer> extract;
    private final Runnable committed;

    public TransactionalFluidAccess(Supplier<S> snapshot, Consumer<S> restore,
                                    BiFunction<FluidVariant, Integer, Integer> insert,
                                    BiFunction<FluidVariant, Integer, Integer> extract, Runnable committed) {
        this.snapshot = snapshot;
        this.restore = restore;
        this.insert = insert;
        this.extract = extract;
        this.committed = committed;
    }

    public int transfer(FluidVariant fluid, int amount, boolean inserting, TransactionContext transaction) {
        if (fluid.isBlank() || amount <= 0) return 0;
        updateSnapshots(transaction);
        return (inserting ? insert : extract).apply(fluid, amount);
    }

    @Override
    protected S createSnapshot() { return snapshot.get(); }

    @Override
    protected void readSnapshot(S value) { restore.accept(value); }

    @Override
    protected void onFinalCommit() { committed.run(); }
}
