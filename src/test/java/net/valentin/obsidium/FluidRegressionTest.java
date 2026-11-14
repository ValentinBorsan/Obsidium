package net.valentin.obsidium;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.fabricmc.fabric.impl.transfer.fluid.FluidVariantImpl;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.fluid.FluidBucketMapping;
import net.valentin.obsidium.fluid.FluidNetworkTransfers;
import net.valentin.obsidium.fluid.TransactionalFluidAccess;

import java.util.List;

final class FluidRegressionTest {
    // The standalone JVM has no Fabric fluid-cache mixin. Only this test fixture uses the implementation constructor.
    private static FluidVariant variant(net.minecraft.world.level.material.Fluid fluid) {
        return new FluidVariantImpl(fluid, DataComponentPatch.EMPTY);
    }

    static void run() {
        FluidVariant water = variant(Fluids.WATER);
        TestTank small = new TestTank(500, 0);
        check(move(List.of(small.access::transfer), water, 1000, true, false, true) == 0,
                "An incomplete bucket deposit must fail");
        check(small.amount == 0 && small.commits == 0, "Failed bucket deposit changed storage");
        small.amount = 500;
        check(move(List.of(small.access::transfer), water, 1000, false, false, true) == 0,
                "An incomplete bucket withdrawal must fail");
        check(small.amount == 500 && small.commits == 0, "Failed withdrawal removed liquid");

        TestTank first = new TestTank(600, 0);
        TestTank second = new TestTank(600, 0);
        List<FluidNetworkTransfers.Endpoint> network = List.of(first.access::transfer, second.access::transfer);
        check(move(network, water, 1000, true, true, true) == 1000, "Distributed simulation failed");
        check(first.amount == 0 && second.amount == 0, "Simulation must roll back all endpoints");
        check(move(network, water, 1000, true, false, true) == 1000, "Distributed bucket deposit failed");
        check(first.amount == 600 && second.amount == 400, "Distributed deposit changed quantities");
        check(first.commits == 1 && second.commits == 1, "Successful deposit must finalize once per endpoint");
        check(move(network, water, 1000, false, false, true) == 1000, "Distributed bucket withdrawal failed");
        check(first.amount == 0 && second.amount == 0, "Distributed withdrawal left liquid behind");

        TestTank shared = new TestTank(500, 0);
        List<FluidNetworkTransfers.Endpoint> duplicate = List.of(shared.access::transfer, shared.access::transfer);
        check(move(duplicate, water, 1000, true, false, true) == 0,
                "The same storage must not be counted twice");
        check(shared.amount == 0, "Duplicate endpoint failure did not roll back");
        shared.amount = 500;
        check(move(duplicate, water, 1000, false, false, true) == 0, "Duplicate extraction must fail");
        check(shared.amount == 500, "Duplicate extraction lost liquid");
        shared.amount = 0;
        check(move(duplicate, water, 1000, true, false, false) == 500, "Partial bottle transfer should be allowed");
        check(shared.amount == 500, "Partial transfer did not conserve liquid");

        try (Transaction parent = Transaction.openOuter()) {
            shared.access.transfer(water, 200, false, parent);
            try (Transaction nested = parent.openNested()) {
                shared.access.transfer(water, 100, false, nested);
            }
            check(shared.amount == 300, "Nested abort must restore only the nested change");
        }
        check(shared.amount == 500, "Outer abort must restore the original snapshot");
        try {
            move(List.of(shared.access::transfer, (f, a, i, t) -> { throw new IllegalStateException("Test failure"); }),
                    water, 1000, false, false, true);
            throw new AssertionError("Expected endpoint failure");
        } catch (IllegalStateException expected) {
            check(shared.amount == 500, "Endpoint exception must roll back earlier endpoints");
        }

        DropletTank external = new DropletTank(40_540, 0); // 500 mB + 40 droplets
        FluidNetworkTransfers.Endpoint endpoint = (f, a, i, t) -> FluidNetworkTransfers.transferStorage(external, f, a, i, t);
        check(move(List.of(endpoint), water, 1000, true, false, true) == 0, "Fractional external storage filled a bucket");
        check(external.amount == 0, "External storage was not rolled back");
        check(move(List.of(endpoint), water, 1000, true, false, false) == 500, "Whole mB rounding failed");
        check(external.amount == 40_500, "Fractional droplets were committed without being accounted for");
        external.amount = 40_540;
        check(move(List.of(endpoint), water, 1000, false, false, false) == 500, "Whole mB extraction failed");
        check(external.amount == 40, "Fractional droplets disappeared on extraction");

        DropletTank deferred = new DropletTank(40_500, 0) {
            @Override public long insert(FluidVariant fluid, long max, TransactionContext transaction) {
                long moved = Math.min(max, capacity - amount);
                transaction.addCloseCallback((closed, result) -> {
                    if (result.wasCommitted()) amount += moved;
                });
                return moved;
            }
        };
        FluidNetworkTransfers.Endpoint deferredEndpoint = (f, a, i, t) ->
                FluidNetworkTransfers.transferStorage(deferred, f, a, i, t);
        check(move(List.of(deferredEndpoint), water, 1000, true, false, true) == 0,
                "Legacy incomplete transfer must fail");
        check(deferred.amount == 0, "A nested probe committed a legacy callback early");
        check(move(List.of(deferredEndpoint), water, 500, true, true, true) == 500,
                "Legacy simulation must report available capacity");
        check(deferred.amount == 0, "Legacy simulation changed storage");
        check(move(List.of(deferredEndpoint), water, 500, true, false, true) == 500,
                "Legacy complete transfer failed");
        check(deferred.amount == 40_500, "Legacy transfer was applied more than once");

        check(FluidBucketMapping.filledBucket(water) == Items.WATER_BUCKET, "Water bucket mapping failed");
        check(FluidBucketMapping.filledBucket(variant(Fluids.LAVA)) == Items.LAVA_BUCKET, "Lava bucket mapping failed");
        check(FluidBucketMapping.filledBucket(variant(Fluids.EMPTY)) == Items.AIR, "Empty fluid must not create water");
        FluidVariant namedWater = new FluidVariantImpl(Fluids.WATER,
                DataComponentPatch.builder().set(DataComponents.CUSTOM_NAME, Component.literal("Special water")).build());
        check(FluidBucketMapping.filledBucket(namedWater) == Items.AIR, "A bucket must not discard fluid components");
        System.out.println("Fluid regression checks passed");
    }

    private static int move(List<FluidNetworkTransfers.Endpoint> endpoints, FluidVariant fluid, int amount,
                            boolean inserting, boolean simulate, boolean exact) {
        return FluidNetworkTransfers.transfer(endpoints, fluid, amount, inserting, simulate, exact);
    }

    private static final class TestTank {
        final int capacity;
        int amount;
        int commits;
        final TransactionalFluidAccess<Integer> access;

        TestTank(int capacity, int amount) {
            this.capacity = capacity;
            this.amount = amount;
            access = new TransactionalFluidAccess<>(() -> this.amount, saved -> this.amount = saved,
                    (fluid, requested) -> {
                        int moved = Math.min(requested, this.capacity - this.amount);
                        this.amount += moved;
                        return moved;
                    }, (fluid, requested) -> {
                        int moved = Math.min(requested, this.amount);
                        this.amount -= moved;
                        return moved;
                    }, () -> commits++);
        }
    }

    private static class DropletTank extends SnapshotParticipant<Long> implements SingleSlotStorage<FluidVariant> {
        final long capacity;
        long amount;
        DropletTank(long capacity, long amount) { this.capacity = capacity; this.amount = amount; }
        @Override protected Long createSnapshot() { return amount; }
        @Override protected void readSnapshot(Long saved) { amount = saved; }
        @Override public long insert(FluidVariant fluid, long max, TransactionContext transaction) {
            updateSnapshots(transaction);
            long moved = Math.min(max, capacity - amount);
            amount += moved;
            return moved;
        }
        @Override public long extract(FluidVariant fluid, long max, TransactionContext transaction) {
            updateSnapshots(transaction);
            long moved = Math.min(max, amount);
            amount -= moved;
            return moved;
        }
        @Override public boolean isResourceBlank() { return amount == 0; }
        @Override public FluidVariant getResource() { return variant(Fluids.WATER); }
        @Override public long getAmount() { return amount; }
        @Override public long getCapacity() { return capacity; }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
