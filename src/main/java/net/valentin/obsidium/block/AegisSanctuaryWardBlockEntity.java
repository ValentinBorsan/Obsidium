package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Energy-driven spawn suppression ward draining 800 RF/t when active.
 */
public final class AegisSanctuaryWardBlockEntity extends BlockEntity {
    public static final long MAX_ENERGY = 500_000L;
    public static final long DRAIN_PER_TICK = 800L;
    public static final int RADIUS_HORIZONTAL = 48;
    public static final int RADIUS_VERTICAL = 24;

    private static final Map<ResourceKey<Level>, Set<BlockPos>> ACTIVE_WARDS = new ConcurrentHashMap<>();

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(MAX_ENERGY, 10_000L, 0L) {
        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    public static final int DATA_COUNT = 3;
    private boolean active;
    private boolean manualEnabled = true;

    public AegisSanctuaryWardBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.AEGIS_SANCTUARY_WARD_BE, pos, state);
    }

    public boolean isActive() {
        return active;
    }

    public boolean isManualEnabled() {
        return manualEnabled;
    }

    public void toggleManualEnabled() {
        manualEnabled = !manualEnabled;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AegisSanctuaryWardBlockEntity ward) {
        boolean canRun = ward.manualEnabled && !level.hasNeighborSignal(pos) && ward.energyStorage.amount >= DRAIN_PER_TICK;
        if (canRun) {
            ward.energyStorage.amount -= DRAIN_PER_TICK;
            if (level.getGameTime() % 20 == 0) {
                ward.setChanged();
            }
            ward.active = true;
            ACTIVE_WARDS.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
        } else {
            ward.active = false;
            Set<BlockPos> set = ACTIVE_WARDS.get(level.dimension());
            if (set != null) set.remove(pos);
        }

        if (state.getValue(AegisSanctuaryWardBlock.LIT) != ward.active) {
            level.setBlock(pos, state.setValue(AegisSanctuaryWardBlock.LIT, ward.active), Block.UPDATE_ALL);
        }
    }

    public static boolean isProtected(ServerLevel level, BlockPos spawnPos) {
        Set<BlockPos> wards = ACTIVE_WARDS.get(level.dimension());
        if (wards == null || wards.isEmpty()) return false;
        for (BlockPos wardPos : wards) {
            int dx = Math.abs(spawnPos.getX() - wardPos.getX());
            int dz = Math.abs(spawnPos.getZ() - wardPos.getZ());
            int dy = Math.abs(spawnPos.getY() - wardPos.getY());
            if (dx <= RADIUS_HORIZONTAL && dz <= RADIUS_HORIZONTAL && dy <= RADIUS_VERTICAL) {
                if (level.isLoaded(wardPos)
                        && level.getBlockEntity(wardPos) instanceof AegisSanctuaryWardBlockEntity ward
                        && ward.isActive()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null) {
            Set<BlockPos> set = ACTIVE_WARDS.get(level.dimension());
            if (set != null) set.remove(worldPosition);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("Energy", energyStorage.amount);
        tag.putBoolean("ManualEnabled", manualEnabled);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyStorage.amount = Math.clamp(tag.getLong("Energy"), 0L, MAX_ENERGY);
        manualEnabled = !tag.contains("ManualEnabled") || tag.getBoolean("ManualEnabled");
    }
}
