package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;

import java.util.HashSet;
import java.util.Set;

public class VolcaniteChunkLoaderBlockEntity extends BlockEntity {
    private int radius = 1; // 1 = 1x1 chunk, 2 = 3x3 chunks
    private final Set<Long> forcedChunks = new HashSet<>();
    private boolean enabled = true;
    private int tickTimer = 0;

    public VolcaniteChunkLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.VOLCANITE_CHUNK_LOADER_BE, pos, state);
    }

    public int getRadius() {
        return radius;
    }

    public int toggleRadius() {
        disableChunkLoading();
        if (radius == 1) {
            radius = 2; // 3x3 chunks
        } else {
            radius = 1; // 1x1 chunk
        }
        applyChunkLoading();
        setChanged();
        return radius;
    }

    public void applyChunkLoading() {
        if (!(level instanceof ServerLevel sl) || !enabled) return;

        ChunkPos center = new ChunkPos(worldPosition);
        int r = radius - 1; // 0 for 1x1, 1 for 3x3

        Set<Long> targetChunks = new HashSet<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int cx = center.x + dx;
                int cz = center.z + dz;
                targetChunks.add(ChunkPos.asLong(cx, cz));
                sl.setChunkForced(cx, cz, true);
            }
        }

        // Unforce any old chunks that are no longer in radius
        for (long oldChunk : forcedChunks) {
            if (!targetChunks.contains(oldChunk)) {
                sl.setChunkForced(ChunkPos.getX(oldChunk), ChunkPos.getZ(oldChunk), false);
            }
        }

        forcedChunks.clear();
        forcedChunks.addAll(targetChunks);
        setChanged();
    }

    public void disableChunkLoading() {
        if (level instanceof ServerLevel sl) {
            for (long chunkLong : forcedChunks) {
                sl.setChunkForced(ChunkPos.getX(chunkLong), ChunkPos.getZ(chunkLong), false);
            }
            forcedChunks.clear();
        }
    }

    @Override
    public void setRemoved() {
        disableChunkLoading();
        super.setRemoved();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VolcaniteChunkLoaderBlockEntity entity) {
        if (level.isClientSide()) return;

        if (++entity.tickTimer >= 40) {
            entity.tickTimer = 0;
            if (entity.enabled && entity.forcedChunks.isEmpty()) {
                entity.applyChunkLoading();
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("Radius", radius);
        tag.putBoolean("Enabled", enabled);
        tag.putLongArray("ForcedChunks", forcedChunks.stream().mapToLong(Long::longValue).toArray());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.radius = Math.max(1, Math.min(2, tag.contains("Radius") ? tag.getInt("Radius") : 1));
        this.enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
        forcedChunks.clear();
        if (tag.contains("ForcedChunks")) {
            for (long chunk : tag.getLongArray("ForcedChunks")) {
                forcedChunks.add(chunk);
            }
        } else if (enabled) {
            // Older saves did not store the ticket list. Reconstruct this block's previous radius
            // so removing it after upgrading can release the chunks it used to force.
            ChunkPos center = new ChunkPos(worldPosition);
            int r = radius - 1;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    forcedChunks.add(ChunkPos.asLong(center.x + dx, center.z + dz));
                }
            }
        }
    }
}

