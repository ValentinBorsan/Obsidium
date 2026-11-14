package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class VolcaniteStorageInterfaceBlockEntity extends BlockEntity {
    private int scanTimer = 0;
    private BlockPos cachedControllerPos = null;

    public VolcaniteStorageInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.VOLCANITE_STORAGE_INTERFACE_BE, pos, state);
    }

    public VolcaniteStorageControllerBlockEntity findController() {
        if (level == null) return null;
        if (cachedControllerPos != null && level.getBlockEntity(cachedControllerPos) instanceof VolcaniteStorageControllerBlockEntity controller && controller.isPowered()) {
            return controller;
        }

        // Direct check on adjacent blocks first for immediate response
        for (Direction dir : Direction.values()) {
            BlockPos p = worldPosition.relative(dir);
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof VolcaniteStorageControllerBlockEntity ctrl && ctrl.isPowered()) {
                cachedControllerPos = p;
                return ctrl;
            }
            if (be instanceof VolcaniteNetworkNodeReceiverBlockEntity receiver && receiver.isLinked()) {
                VolcaniteStorageControllerBlockEntity ctrl = receiver.getConnectedController();
                if (ctrl != null && ctrl.isPowered()) {
                    cachedControllerPos = ctrl.getBlockPos();
                    return ctrl;
                }
            }
            if (be instanceof VolcaniteNetworkNodeEmitterBlockEntity emitter && emitter.isLinked()) {
                VolcaniteStorageControllerBlockEntity ctrl = emitter.getConnectedController();
                if (ctrl != null && ctrl.isPowered()) {
                    cachedControllerPos = ctrl.getBlockPos();
                    return ctrl;
                }
            }
        }

        // BFS traversal through pipes, conduits, wireless nodes, and network blocks
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(worldPosition);
        visited.add(worldPosition);

        int steps = 0;
        int maxSteps = 4096;
        while (!queue.isEmpty() && steps++ < maxSteps) {
            BlockPos current = queue.poll();
            if (!level.isLoaded(current)) continue;

            for (Direction dir : Direction.values()) {
                if (!PipeConnectionHelper.canTraverseNetwork(level, current, dir)) continue;
                BlockPos next = current.relative(dir);
                if (!level.isLoaded(next)) continue;
                BlockState nextState = level.getBlockState(next);
                if (nextState.isAir()) continue;
                if (visited.add(next)) {
                    BlockEntity be = level.getBlockEntity(next);
                    if (be instanceof VolcaniteStorageControllerBlockEntity controller && controller.isPowered()) {
                        cachedControllerPos = next;
                        return controller;
                    } else if (be instanceof VolcaniteNetworkNodeReceiverBlockEntity receiver && receiver.isLinked()) {
                        VolcaniteStorageControllerBlockEntity ctrl = receiver.getConnectedController();
                        if (ctrl != null && ctrl.isPowered()) {
                            cachedControllerPos = ctrl.getBlockPos();
                            return ctrl;
                        }
                        BlockPos ePos = receiver.getLinkedEmitterPos();
                        if (ePos != null && visited.add(ePos)) {
                            queue.add(ePos);
                        }
                        queue.add(next);
                    } else if (be instanceof VolcaniteNetworkNodeEmitterBlockEntity emitter && emitter.isLinked()) {
                        VolcaniteStorageControllerBlockEntity ctrl = emitter.getConnectedController();
                        if (ctrl != null && ctrl.isPowered()) {
                            cachedControllerPos = ctrl.getBlockPos();
                            return ctrl;
                        }
                        for (BlockPos rPos : emitter.getLinkedReceivers()) {
                            if (visited.add(rPos)) {
                                queue.add(rPos);
                            }
                        }
                        queue.add(next);
                    } else {
                        if (PipeConnectionHelper.isLogisticsPipe(nextState.getBlock()) ||
                            PipeConnectionHelper.isStorageNetworkBlock(nextState.getBlock())) {
                            queue.add(next);
                        }
                    }
                }
            }
        }

        cachedControllerPos = null;
        return null;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VolcaniteStorageInterfaceBlockEntity entity) {
        if (level.isClientSide()) return;

        if (++entity.scanTimer >= 20) {
            entity.scanTimer = 0;
            VolcaniteStorageControllerBlockEntity ctrl = entity.findController();
            boolean isOnline = ctrl != null && ctrl.isPowered();

            if (state.getValue(VolcaniteStorageInterfaceBlock.WORKING) != isOnline) {
                level.setBlock(pos, state.setValue(VolcaniteStorageInterfaceBlock.WORKING, isOnline), 3);
            }
        }
    }
}
