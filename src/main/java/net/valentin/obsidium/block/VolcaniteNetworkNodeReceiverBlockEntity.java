package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import team.reborn.energy.api.EnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VolcaniteNetworkNodeReceiverBlockEntity extends BlockEntity {
    public static final int RANGE = 64;
    public static final int SUPREME_RANGE = 256;

    private static final Map<ResourceKey<Level>, Set<BlockPos>> ACTIVE_RECEIVERS = new ConcurrentHashMap<>();

    public static void registerReceiver(Level level, BlockPos pos) {
        if (level != null && pos != null) {
            ACTIVE_RECEIVERS.computeIfAbsent(level.dimension(), k -> Collections.synchronizedSet(new HashSet<>())).add(pos);
        }
    }

    public static void clearStaticRegistries() {
        ACTIVE_RECEIVERS.clear();
    }

    public static void clearDimension(ResourceKey<Level> dim) {
        if (dim != null) {
            ACTIVE_RECEIVERS.remove(dim);
        }
    }

    public static void unregisterReceiver(Level level, BlockPos pos) {
        if (level != null && pos != null) {
            Set<BlockPos> set = ACTIVE_RECEIVERS.get(level.dimension());
            if (set != null) {
                set.remove(pos);
            }
        }
    }

    public static Set<BlockPos> getActiveReceiversInDimension(Level level) {
        if (level == null) return Collections.emptySet();
        Set<BlockPos> set = ACTIVE_RECEIVERS.get(level.dimension());
        return (set != null) ? new HashSet<>(set) : Collections.emptySet();
    }

    private BlockPos linkedEmitterPos = null;
    private boolean active = false;
    private boolean chunkLoading = false;
    private boolean chunkForcedRegistered = false;
    private int tickTimer = 0;

    public static final long WIRELESS_ENERGY_TRANSFER_RATE = 50_000L;

    // Bidirectional energy forwarding to/from the linked controller
    public final EnergyStorage energyForwarder = new EnergyStorage() {
        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            VolcaniteStorageControllerBlockEntity ctrl = getConnectedController();
            if (ctrl != null) {
                long maxTransfer = Math.min(maxAmount, WIRELESS_ENERGY_TRANSFER_RATE);
                return ctrl.getEnergyStorage().insert(maxTransfer, transaction);
            }
            return 0;
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            VolcaniteStorageControllerBlockEntity ctrl = getConnectedController();
            if (ctrl != null && ctrl.isPowered()) {
                long maxTransfer = Math.min(maxAmount, WIRELESS_ENERGY_TRANSFER_RATE);
                return ctrl.getEnergyStorage().extract(maxTransfer, transaction);
            }
            return 0;
        }

        @Override
        public long getAmount() {
            VolcaniteStorageControllerBlockEntity ctrl = getConnectedController();
            return (ctrl != null) ? ctrl.getEnergyStorage().getAmount() : 0;
        }

        @Override
        public long getCapacity() {
            VolcaniteStorageControllerBlockEntity ctrl = getConnectedController();
            return (ctrl != null) ? ctrl.getEnergyStorage().getCapacity() : 0;
        }

        @Override
        public boolean supportsExtraction() {
            return true;
        }

        @Override
        public boolean supportsInsertion() {
            return true;
        }
    };

    public VolcaniteNetworkNodeReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.VOLCANITE_NETWORK_NODE_RECEIVER_BE, pos, state);
    }

    public boolean isChunkLoading() {
        return chunkLoading;
    }

    public boolean toggleChunkLoading() {
        this.chunkLoading = !this.chunkLoading;
        if (this.chunkLoading) {
            VolcaniteStorageControllerBlockEntity ctrl = getConnectedController();
            if (ctrl == null || !ctrl.isPowered()) {
                this.chunkLoading = false;
                applyChunkLoadingState();
                setChanged();
                return false;
            }
        }
        applyChunkLoadingState();
        setChanged();
        return this.chunkLoading;
    }

    public void applyChunkLoadingState() {
        if (level instanceof ServerLevel sl) {
            ChunkPos cp = new ChunkPos(worldPosition);
            VolcaniteStorageControllerBlockEntity ctrl = getConnectedController();
            boolean canForce = chunkLoading && ctrl != null && ctrl.isPowered() && ctrl.getEnergyStorage().amount >= 5;
            if (canForce) {
                ctrl.getEnergyStorage().amount -= 5;
                if (!chunkForcedRegistered) {
                    sl.setChunkForced(cp.x, cp.z, true);
                    chunkForcedRegistered = true;
                }
            } else if (chunkForcedRegistered) {
                sl.setChunkForced(cp.x, cp.z, false);
                chunkForcedRegistered = false;
            }
        }
    }

    public void disableChunkLoading() {
        if (chunkForcedRegistered && level instanceof ServerLevel sl) {
            ChunkPos cp = new ChunkPos(worldPosition);
            sl.setChunkForced(cp.x, cp.z, false);
            chunkForcedRegistered = false;
        }
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level != null) {
            registerReceiver(level, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        disableChunkLoading();
        if (level != null) {
            unregisterReceiver(level, worldPosition);
        }
        super.setRemoved();
    }

    public boolean isLinked() {
        return active && getConnectedEmitter() != null;
    }

    public BlockPos getLinkedEmitterPos() {
        return linkedEmitterPos;
    }

    public void setLinkedEmitter(BlockPos pos) {
        this.linkedEmitterPos = pos;
        this.active = true;
        this.setChanged();
        if (level != null) {
            level.setBlock(worldPosition, getBlockState().setValue(VolcaniteNetworkNodeReceiverBlock.LIT, true), 3);
        }
    }

    public VolcaniteNetworkNodeEmitterBlockEntity getConnectedEmitter() {
        if (level == null) return null;
        if (linkedEmitterPos != null && level.isLoaded(linkedEmitterPos) && worldPosition.distSqr(linkedEmitterPos) <= (SUPREME_RANGE * SUPREME_RANGE)) {
            if (level.getBlockEntity(linkedEmitterPos) instanceof VolcaniteNetworkNodeEmitterBlockEntity emitter) {
                if (emitter.isLinked()) return emitter;
            }
        }

        // Query active emitters in the dimension within SUPREME_RANGE
        VolcaniteNetworkNodeEmitterBlockEntity closest = null;
        double closestDistSq = Double.MAX_VALUE;

        Set<BlockPos> emitterPositions = VolcaniteNetworkNodeEmitterBlockEntity.getActiveEmittersInDimension(level);
        for (BlockPos emitPos : emitterPositions) {
            if (emitPos.equals(worldPosition)) continue;
            if (!level.isLoaded(emitPos)) continue;
            double d = worldPosition.distSqr(emitPos);
            if (d <= (SUPREME_RANGE * SUPREME_RANGE) && d < closestDistSq) {
                BlockEntity be = level.getBlockEntity(emitPos);
                if (be instanceof VolcaniteNetworkNodeEmitterBlockEntity emitter && emitter.isLinked()) {
                    closestDistSq = d;
                    closest = emitter;
                }
            }
        }

        if (closest != null) {
            linkedEmitterPos = closest.getBlockPos();
            return closest;
        }

        return null;
    }

    public VolcaniteStorageControllerBlockEntity getConnectedController() {
        VolcaniteNetworkNodeEmitterBlockEntity emitter = getConnectedEmitter();
        return emitter != null ? emitter.getConnectedController() : null;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VolcaniteNetworkNodeReceiverBlockEntity entity) {
        if (level.isClientSide()) return;

        registerReceiver(level, pos);

        // Ensure forced chunk state on server tied to power
        if (entity.chunkLoading) {
            VolcaniteStorageControllerBlockEntity ctrl = entity.getConnectedController();
            if (ctrl == null || !ctrl.isPowered() || ctrl.getEnergyStorage().amount < 5) {
                if (entity.chunkForcedRegistered && level instanceof ServerLevel sl) {
                    ChunkPos cp = new ChunkPos(pos);
                    sl.setChunkForced(cp.x, cp.z, false);
                    entity.chunkForcedRegistered = false;
                }
            } else {
                ctrl.getEnergyStorage().amount -= 5;
                if (!entity.chunkForcedRegistered && level instanceof ServerLevel sl) {
                    ChunkPos cp = new ChunkPos(pos);
                    sl.setChunkForced(cp.x, cp.z, true);
                    entity.chunkForcedRegistered = true;
                }
            }
        } else if (entity.chunkForcedRegistered && level instanceof ServerLevel sl) {
            ChunkPos cp = new ChunkPos(pos);
            sl.setChunkForced(cp.x, cp.z, false);
            entity.chunkForcedRegistered = false;
        }

        if (++entity.tickTimer >= 20) {
            entity.tickTimer = 0;
            VolcaniteNetworkNodeEmitterBlockEntity emitter = entity.getConnectedEmitter();
            boolean currentlyLinked = (emitter != null && emitter.isLinked());

            if (currentlyLinked) {
                emitter.registerReceiver(pos);
            }

            if (entity.active != currentlyLinked) {
                entity.active = currentlyLinked;
                level.setBlock(pos, state.setValue(VolcaniteNetworkNodeReceiverBlock.LIT, currentlyLinked), 3);
                entity.setChanged();
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putBoolean("Active", active);
        tag.putBoolean("ChunkLoading", chunkLoading);
        if (linkedEmitterPos != null) {
            tag.putInt("EmitX", linkedEmitterPos.getX());
            tag.putInt("EmitY", linkedEmitterPos.getY());
            tag.putInt("EmitZ", linkedEmitterPos.getZ());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.active = tag.getBoolean("Active");
        this.chunkLoading = tag.getBoolean("ChunkLoading");
        if (tag.contains("EmitX")) {
            this.linkedEmitterPos = new BlockPos(tag.getInt("EmitX"), tag.getInt("EmitY"), tag.getInt("EmitZ"));
        }
    }
}
