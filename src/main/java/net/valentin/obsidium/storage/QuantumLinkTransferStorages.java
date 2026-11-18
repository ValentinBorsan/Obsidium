package net.valentin.obsidium.storage;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import team.reborn.energy.api.EnergyStorage;

import java.util.HashMap;
import java.util.Map;

/** One transaction participant per channel, shared by every endpoint in this SavedData. */
final class QuantumLinkTransferStorages {
    private final QuantumLinkNetworkState state;
    private final Map<String, FluidEndpoint> fluids = new HashMap<>();
    private final Map<String, EnergyEndpoint> energy = new HashMap<>();

    QuantumLinkTransferStorages(QuantumLinkNetworkState state) { this.state = state; }
    SingleSlotStorage<FluidVariant> fluid(String key) { return fluids.computeIfAbsent(key, FluidEndpoint::new); }
    EnergyStorage energy(String key) { return energy.computeIfAbsent(key, EnergyEndpoint::new); }

    private record FluidSnapshot(FluidVariant fluid, long amount) {}
    private final class FluidEndpoint extends SnapshotParticipant<FluidSnapshot> implements SingleSlotStorage<FluidVariant> {
        private final String key;
        FluidEndpoint(String key) { this.key = key; }
        @Override protected FluidSnapshot createSnapshot() { return new FluidSnapshot(state.getFluid(key), state.getFluidAmount(key)); }
        @Override protected void readSnapshot(FluidSnapshot snapshot) { state.setFluidState(key, snapshot.fluid(), snapshot.amount()); }
        @Override protected void onFinalCommit() { state.setDirty(); }
        @Override public boolean isResourceBlank() { return state.getFluid(key).isBlank(); }
        @Override public FluidVariant getResource() { return state.getFluid(key); }
        @Override public long getAmount() { return state.getFluidAmount(key) * 81L; }
        @Override public long getCapacity() { return QuantumLinkNetworkState.FLUID_VAULT_CAPACITY * 81L; }
        @Override public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount < 81L) return 0L;
            updateSnapshots(transaction);
            return state.insertFluid(key, resource, maxAmount / 81L, false) * 81L;
        }
        @Override public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount < 81L) return 0L;
            updateSnapshots(transaction);
            return state.extractFluid(key, resource, maxAmount / 81L, false) * 81L;
        }
    }

    private final class EnergyEndpoint extends SnapshotParticipant<Long> implements EnergyStorage {
        private final String key;
        EnergyEndpoint(String key) { this.key = key; }
        @Override protected Long createSnapshot() { return state.getEnergy(key); }
        @Override protected void readSnapshot(Long amount) { state.setEnergyState(key, amount); }
        @Override protected void onFinalCommit() { state.setDirty(); }
        @Override public long getAmount() { return state.getEnergy(key); }
        @Override public long getCapacity() { return QuantumLinkNetworkState.ENERGY_VAULT_CAPACITY; }
        @Override public long insert(long maxAmount, TransactionContext transaction) {
            if (maxAmount <= 0L) return 0L;
            updateSnapshots(transaction);
            return state.insertEnergy(key, maxAmount, false);
        }
        @Override public long extract(long maxAmount, TransactionContext transaction) {
            if (maxAmount <= 0L) return 0L;
            updateSnapshots(transaction);
            return state.extractEnergy(key, maxAmount, false);
        }
    }
}
