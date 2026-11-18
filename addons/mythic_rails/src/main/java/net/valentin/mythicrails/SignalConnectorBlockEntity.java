package net.valentin.mythicrails;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Stores remote link to RailControlBlockEntity and current remote command mode for adjacent rail hardware. */
public final class SignalConnectorBlockEntity extends BlockEntity {
    public static final int KIND_NONE = 0;
    public static final int KIND_SEMAPHORE = 1;
    public static final int KIND_BUMPER = 2;
    public static final int KIND_SWITCH = 3;

    private BlockPos controlPos;
    private int mode; // Semaphore: 0=AUTO, 1=RED, 2=GREEN | Bumper: 0=HOLD, 1=LAUNCH | Switch: 0=STRAIGHT, 1=CURVED

    public SignalConnectorBlockEntity(BlockPos pos, BlockState state) {
        super(MythicRails.SIGNAL_CONNECTOR_BE, pos, state);
    }

    public BlockPos controlPos() { return controlPos; }
    public int mode() { return mode; }

    public void setControlPos(BlockPos pos) {
        this.controlPos = pos == null ? null : pos.immutable();
        setChanged();
        if (level != null) {
            boolean linked = this.controlPos != null;
            if (getBlockState().getValue(SignalConnectorBlock.LINKED) != linked) {
                level.setBlock(worldPosition, getBlockState().setValue(SignalConnectorBlock.LINKED, linked), Block.UPDATE_ALL);
            }
        }
    }

    public void setMode(int mode) {
        this.mode = mode;
        setChanged();
        applyToNeighbor();
    }

    public int targetKind() {
        if (level == null) return KIND_NONE;
        for (Direction dir : Direction.values()) {
            Block block = level.getBlockState(worldPosition.relative(dir)).getBlock();
            if (block instanceof RailSemaphoreBlock) return KIND_SEMAPHORE;
            if (block instanceof RailBumperBlock) return KIND_BUMPER;
            if (block instanceof SwitchRailBlock) return KIND_SWITCH;
        }
        return KIND_NONE;
    }

    public void applyToNeighbor() {
        if (level == null || level.isClientSide) return;
        for (Direction dir : Direction.values()) {
            BlockPos targetPos = worldPosition.relative(dir);
            BlockState targetState = level.getBlockState(targetPos);
            Block block = targetState.getBlock();
            if (block instanceof RailSemaphoreBlock sem) {
                sem.applyRemote(targetState, level, targetPos);
            } else if (block instanceof RailBumperBlock bumper) {
                bumper.applyRemote(targetState, level, targetPos);
            } else if (block instanceof SwitchRailBlock sw) {
                sw.applyRemote(targetState, level, targetPos);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Mode", mode);
        if (controlPos != null) {
            tag.putInt("CtrlX", controlPos.getX());
            tag.putInt("CtrlY", controlPos.getY());
            tag.putInt("CtrlZ", controlPos.getZ());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mode = tag.getInt("Mode");
        if (tag.contains("CtrlX")) {
            controlPos = new BlockPos(tag.getInt("CtrlX"), tag.getInt("CtrlY"), tag.getInt("CtrlZ"));
        } else {
            controlPos = null;
        }
    }
}
