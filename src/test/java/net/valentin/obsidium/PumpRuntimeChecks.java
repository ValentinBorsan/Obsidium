package net.valentin.obsidium;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.LevelResource;
import net.valentin.obsidium.block.LiquidTankBlockEntity;
import net.valentin.obsidium.storage.QuantumLinkNetworkState;

import java.util.HashMap;
import java.util.Map;

/** Runs only in an isolated Fabric test server, never in the distributed mod. */
public final class PumpRuntimeChecks implements ModInitializer {
    private final Map<BlockPos, Tank> targets = new HashMap<>();

    @Override public void onInitialize() {
        FluidStorage.SIDED.registerForBlocks((world, pos, state, entity, side) -> targets.get(pos), Blocks.GOLD_BLOCK);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                server.tickRateManager().setFrozen(true);
                ServerLevel level = server.overworld();
                int index = 0;
                for (boolean quantum : new boolean[]{false, true}) {
                    for (boolean pipe : new boolean[]{false, true}) {
                        BlockPos pos = new BlockPos(32 + index++ * 16, 100, 32);
                        level.setBlock(pos, ObsidiumMod.VOLCANIC_WOOD_LIQUID_BARREL_BLOCK.defaultBlockState(), 3);
                        var barrel = (LiquidTankBlockEntity) level.getBlockEntity(pos);
                        if (quantum) {
                            barrel.setQuantumChannel(850 + index);
                            barrel.setQuantumLinked(true, null);
                        }
                        FluidVariant water = FluidVariant.of(Fluids.WATER);
                        check(barrel.insertFluid(water, 1000, false) == 1000, "FILL_" + index);
                        check(barrel.extractFluid(water, 10, true) == 10 && barrel.getFluidAmount() == 1000,
                                "SIMULATION_PRESERVES_SOURCE_" + index);
                        BlockPos targetPos = pos.east(pipe ? 2 : 1);
                        if (pipe) level.setBlock(pos.east(), ObsidiumMod.VOLCANITE_STORAGE_PIPE_BLOCK.defaultBlockState(), 3);
                        level.setBlock(targetPos, Blocks.GOLD_BLOCK.defaultBlockState(), 3);
                        Tank target = new Tank(40);
                        targets.put(targetPos, target);
                        tick(level, barrel);
                        check(barrel.getFluidAmount() == 1000 && target.amount == 0, "FRACTION_REJECTED_" + index);
                        target.capacity = 283;
                        tick(level, barrel);
                        check(barrel.getFluidAmount() == 997 && target.amount == 243, "WHOLE_MB_CONSERVED_" + index);
                        for (int retry = 0; retry < 20; retry++) tick(level, barrel);
                        check(barrel.getFluidAmount() == 997 && target.amount == 243, "RETRIES_CONSERVE_" + index);
                        target.capacity = 100_000;
                        tick(level, barrel);
                        check(barrel.getFluidAmount() == 947 && target.amount == 4293, "TICK_BUDGET_" + index);
                        if (quantum) {
                            BlockPos peerPos = pos.north(4);
                            level.setBlock(peerPos, ObsidiumMod.VOLCANIC_WOOD_LIQUID_BARREL_BLOCK.defaultBlockState(), 3);
                            var peer = (LiquidTankBlockEntity) level.getBlockEntity(peerPos);
                            peer.setQuantumChannel(850 + index);
                            peer.setQuantumLinked(true, null);
                            check(peer.getFluidAmount() == 947 && peer.getStoredFluid().equals(water),
                                    "QUANTUM_PEER_SEES_DEBIT_" + index);
                            String key = barrel.getActiveQuantumKey();
                            level.getDataStorage().save();
                            var nbt = NbtIo.readCompressed(server.getWorldPath(LevelResource.ROOT)
                                    .resolve("data/obsidium_quantum_link_network.dat"), NbtAccounter.unlimitedHeap());
                            var reloaded = QuantumLinkNetworkState.load(nbt.getCompound("data"), server.registryAccess());
                            check(reloaded.getFluidAmount(key) == 947, "QUANTUM_DISK_RELOAD_" + index);
                        }
                        targets.remove(targetPos);
                    }
                }
                System.out.println("PUMP_ALL_PASSED");
            } catch (Throwable failure) {
                failure.printStackTrace();
                System.out.println("PUMP_FAILED");
            } finally {
                server.halt(false);
            }
        });
    }

    private static void tick(ServerLevel level, LiquidTankBlockEntity barrel) {
        LiquidTankBlockEntity.tick(level, barrel.getBlockPos(), barrel.getBlockState(), barrel);
    }

    private static void check(boolean success, String name) {
        if (!success) throw new AssertionError(name);
        System.out.println("PUMP_OK_" + name);
    }

    private static final class Tank extends SingleVariantStorage<FluidVariant> {
        long capacity;
        Tank(long capacity) { this.capacity = capacity; }
        @Override protected FluidVariant getBlankVariant() { return FluidVariant.blank(); }
        @Override protected long getCapacity(FluidVariant variant) { return capacity; }
    }
}
