package net.valentin.obsidium.fluid;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

/** Shared by active routing and passive insertion; aborted probes consume no throughput. */
public final class TickTransferBudget extends SnapshotParticipant<Long> {
    private long tick = Long.MIN_VALUE;
    private long used;
    public long remaining(long currentTick, long limit) {
        if (tick != currentTick) { tick = currentTick; used = 0; }
        return Math.max(0L, limit - used);
    }
    public void consume(long amount, TransactionContext transaction) {
        if (amount <= 0L) return;
        updateSnapshots(transaction);
        used += amount;
    }
    @Override protected Long createSnapshot() { return used; }
    @Override protected void readSnapshot(Long snapshot) { used = snapshot; }
}
