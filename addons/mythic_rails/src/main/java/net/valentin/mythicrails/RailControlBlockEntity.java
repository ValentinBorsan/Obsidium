package net.valentin.mythicrails;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Stores up to 8 linked Signal Connector positions and dispatches remote commands to them. */
public final class RailControlBlockEntity extends BlockEntity {
    public static final int MAX_LINKS = 8;
    private final List<BlockPos> links = new ArrayList<>();

    public RailControlBlockEntity(BlockPos pos, BlockState state) {
        super(MythicRails.RAIL_CONTROL_BE, pos, state);
    }

    public List<BlockPos> links() {
        return links;
    }

    public boolean linkConnector(BlockPos pos) {
        BlockPos immutable = pos.immutable();
        if (links.contains(immutable)) return true;
        if (links.size() >= MAX_LINKS) return false;
        links.add(immutable);
        setChanged();
        return true;
    }

    public void pruneMissing() {
        if (level == null) return;
        boolean removed = links.removeIf(p -> level.isLoaded(p) && !(level.getBlockEntity(p) instanceof SignalConnectorBlockEntity));
        if (removed) setChanged();
    }

    public SignalConnectorBlockEntity connectorAt(int index) {
        if (level == null || index < 0 || index >= links.size()) return null;
        BlockPos p = links.get(index);
        if (!level.isLoaded(p)) return null;
        return level.getBlockEntity(p) instanceof SignalConnectorBlockEntity be ? be : null;
    }

    public void cycleConnector(int index) {
        var conn = connectorAt(index);
        if (conn == null) return;
        int kind = conn.targetKind();
        int next = switch (kind) {
            case SignalConnectorBlockEntity.KIND_SEMAPHORE -> (conn.mode() + 1) % 3;
            case SignalConnectorBlockEntity.KIND_BUMPER, SignalConnectorBlockEntity.KIND_SWITCH -> (conn.mode() + 1) % 2;
            default -> 0;
        };
        conn.setMode(next);
    }

    public void setAllSemaphores(int mode) {
        for (int i = 0; i < links.size(); i++) {
            var conn = connectorAt(i);
            if (conn != null && conn.targetKind() == SignalConnectorBlockEntity.KIND_SEMAPHORE) {
                conn.setMode(mode);
            }
        }
    }

    public void toggleAllBumpers() {
        for (int i = 0; i < links.size(); i++) {
            var conn = connectorAt(i);
            if (conn != null && conn.targetKind() == SignalConnectorBlockEntity.KIND_BUMPER) {
                conn.setMode(conn.mode() == 1 ? 0 : 1);
            }
        }
    }

    public void toggleAllSwitches() {
        for (int i = 0; i < links.size(); i++) {
            var conn = connectorAt(i);
            if (conn != null && conn.targetKind() == SignalConnectorBlockEntity.KIND_SWITCH) {
                conn.setMode(conn.mode() == 1 ? 0 : 1);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (BlockPos p : links) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("X", p.getX());
            entry.putInt("Y", p.getY());
            entry.putInt("Z", p.getZ());
            list.add(entry);
        }
        tag.put("Links", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        links.clear();
        ListTag list = tag.getList("Links", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && i < MAX_LINKS; i++) {
            CompoundTag entry = list.getCompound(i);
            links.add(new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z")));
        }
    }
}
