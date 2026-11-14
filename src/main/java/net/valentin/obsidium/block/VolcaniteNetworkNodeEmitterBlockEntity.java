package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VolcaniteNetworkNodeEmitterBlockEntity extends BlockEntity {
    public static final int RANGE = 64;
    public static final int SUPREME_RANGE = 256;
    private static final int BASE_RF_DRAIN = 50;
    private static final int PER_RECEIVER_RF_DRAIN = 25;

    private static final Map<ResourceKey<Level>, Set<BlockPos>> ACTIVE_EMITTERS = new ConcurrentHashMap<>();

    public static void registerEmitter(Level level, BlockPos pos) {
        if (level != null && pos != null) {
            ACTIVE_EMITTERS.computeIfAbsent(level.dimension(), k -> Collections.synchronizedSet(new HashSet<>())).add(pos);
        }
    }

    public static void clearStaticRegistries() {
        ACTIVE_EMITTERS.clear();
    }

    public static void clearDimension(ResourceKey<Level> dim) {
        if (dim != null) {
            ACTIVE_EMITTERS.remove(dim);
        }
    }

    public static void unregisterEmitter(Level level, BlockPos pos) {
        if (level != null && pos != null) {
            Set<BlockPos> set = ACTIVE_EMITTERS.get(level.dimension());
            if (set != null) {
                set.remove(pos);
            }
        }
    }

    public static Set<BlockPos> getActiveEmittersInDimension(Level level) {
        if (level == null) return Collections.emptySet();
        Set<BlockPos> set = ACTIVE_EMITTERS.get(level.dimension());
        return (set != null) ? new HashSet<>(set) : Collections.emptySet();
    }

    private BlockPos controllerPos = null;
    private final Set<BlockPos> registeredReceivers = new HashSet<>();
    private boolean active = false;
    private int tickTimer = 0;
    private boolean searchingController = false;

    public VolcaniteNetworkNodeEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.VOLCANITE_NETWORK_NODE_EMITTER_BE, pos, state);
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level != null) {
            registerEmitter(level, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null) {
            unregisterEmitter(level, worldPosition);
        }
        super.setRemoved();
    }

    public boolean isLinked() {
        VolcaniteStorageControllerBlockEntity ctrl = getConnectedController();
        return ctrl != null && ctrl.isPowered();
    }

    public VolcaniteStorageControllerBlockEntity getConnectedController() {
        if (level == null || searchingController) return null;
        searchingController = true;
        try {
            if (controllerPos != null && level.isLoaded(controllerPos) && level.getBlockEntity(controllerPos) instanceof VolcaniteStorageControllerBlockEntity ctrl) {
                if (ctrl.isPowered()) return ctrl;
            }

            // Perform a network BFS up to 1024 nodes through pipes, conduits, drives, and storage units
            Queue<BlockPos> queue = new ArrayDeque<>();
            Set<BlockPos> visited = new HashSet<>();
            queue.add(worldPosition);
            visited.add(worldPosition);

            int steps = 0;
            while (!queue.isEmpty() && steps++ < 1024) {
                BlockPos curr = queue.poll();
                if (!level.isLoaded(curr)) continue;

                for (Direction d : Direction.values()) {
                    if (!PipeConnectionHelper.canTraverseNetwork(level, curr, d)) continue;
                    BlockPos next = curr.relative(d);
                    if (!level.isLoaded(next) || !visited.add(next)) continue;

                    BlockEntity be = level.getBlockEntity(next);
                    if (be instanceof VolcaniteStorageControllerBlockEntity ctrl) {
                        controllerPos = next;
                        if (ctrl.isPowered()) return ctrl;
                    }

                    BlockState bs = level.getBlockState(next);
                    if (PipeConnectionHelper.isLogisticsPipe(be) || PipeConnectionHelper.isStorageNetworkBlock(bs.getBlock())) {
                        queue.add(next);
                    }
                }
            }

            return null;
        } finally {
            searchingController = false;
        }
    }

    public void registerReceiver(BlockPos pos) {
        if (pos != null && worldPosition.distSqr(pos) <= (SUPREME_RANGE * SUPREME_RANGE)) {
            registeredReceivers.add(pos);
            setChanged();
        }
    }

    public List<BlockPos> getLinkedReceivers() {
        pruneReceivers();
        return new ArrayList<>(registeredReceivers);
    }

    public int getActiveReceiverCount() {
        pruneReceivers();
        return registeredReceivers.size();
    }

    private void pruneReceivers() {
        if (level == null) return;
        registeredReceivers.removeIf(p -> {
            if (p == null || worldPosition.distSqr(p) > (SUPREME_RANGE * SUPREME_RANGE)) return true;
            if (level.isLoaded(p)) {
                return !(level.getBlockEntity(p) instanceof VolcaniteNetworkNodeReceiverBlockEntity);
            }
            return false;
        });
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VolcaniteNetworkNodeEmitterBlockEntity entity) {
        if (level.isClientSide()) return;

        registerEmitter(level, pos);

        if (++entity.tickTimer >= 20) {
            entity.tickTimer = 0;
            entity.pruneReceivers();

            VolcaniteStorageControllerBlockEntity controller = entity.getConnectedController();
            boolean currentlyActive = (controller != null && controller.isPowered());

            if (currentlyActive) {
                int drain = BASE_RF_DRAIN + (entity.registeredReceivers.size() * PER_RECEIVER_RF_DRAIN);
                controller.consumeEnergy(drain);
            }

            if (entity.active != currentlyActive) {
                entity.active = currentlyActive;
                level.setBlock(pos, state.setValue(VolcaniteNetworkNodeEmitterBlock.LIT, currentlyActive), 3);
                entity.setChanged();
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putBoolean("Active", active);
        if (controllerPos != null) {
            tag.putInt("CtrlX", controllerPos.getX());
            tag.putInt("CtrlY", controllerPos.getY());
            tag.putInt("CtrlZ", controllerPos.getZ());
        }
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (BlockPos p : registeredReceivers) {
            CompoundTag pt = new CompoundTag();
            pt.putInt("X", p.getX());
            pt.putInt("Y", p.getY());
            pt.putInt("Z", p.getZ());
            list.add(pt);
        }
        tag.put("Receivers", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.active = tag.getBoolean("Active");
        if (tag.contains("CtrlX")) {
            this.controllerPos = new BlockPos(tag.getInt("CtrlX"), tag.getInt("CtrlY"), tag.getInt("CtrlZ"));
        }
        if (tag.contains("Receivers", 9)) {
            net.minecraft.nbt.ListTag list = tag.getList("Receivers", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag pt = list.getCompound(i);
                this.registeredReceivers.add(new BlockPos(pt.getInt("X"), pt.getInt("Y"), pt.getInt("Z")));
            }
        }
    }
}
