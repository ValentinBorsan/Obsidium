package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;

import java.util.*;

public class HermesItemPipeBlockEntity extends VolcaniteStoragePipeBlockEntity {
    private int itemPipeTickCounter = 0;
    private final int[] lastExtractedSlot = new int[6];

    public HermesItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.HERMES_ITEM_PIPE_BE, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HermesItemPipeBlockEntity entity) {
        if (level.isClientSide()) return;
        entity.itemPipeTickCounter++;

        // Fast O(1) early-exit: item pipes only act every 5 ticks and only if a Logistics Module is mounted
        if (entity.itemPipeTickCounter % 5 != 0 || !entity.hasAnyModule()) {
            return;
        }

        VolcaniteStorageControllerBlockEntity controller = entity.findConnectedController();

        // 1. Digital Storage Network: if connected to a powered controller, route into storage system
        if (controller != null && controller.isPowered()) {
            entity.tickCounter = 4; // Ensure parent tick executes its 5-tick transfer cycle immediately
            VolcaniteStoragePipeBlockEntity.tick(level, pos, state, entity);
        } else {
            // 2. Early-game standalone item transport between containers
            tickStandaloneItems(level, pos, entity);
        }
    }

    private static void tickStandaloneItems(Level level, BlockPos pos, HermesItemPipeBlockEntity entity) {
        List<InsertTarget> insertTargets = null;

        for (Direction dir : Direction.values()) {
            if (entity.isSideDisconnected(dir)) continue;
            if (entity.getModule(dir) != ModuleType.EXTRACT) continue;
            if (entity.getTransferType(dir) != TYPE_ITEM) continue;

            BlockPos sourcePos = pos.relative(dir);
            Container sourceContainer = getContainerAt(level, sourcePos);
            if (sourceContainer == null) continue;

            if (insertTargets == null) {
                insertTargets = findItemPipeInsertTargets(level, pos);
            }
            if (insertTargets.isEmpty()) continue;

            int maxItems = entity.getMaxItemTransfer(dir);

            int size = sourceContainer.getContainerSize();
            int startSlot = entity.lastExtractedSlot[dir.get3DDataValue()];
            for (int step = 0; step < size; step++) {
                int i = (startSlot + step) % size;
                ItemStack inSlot = sourceContainer.getItem(i);
                if (inSlot.isEmpty()) continue;
                if (!isOutputSlot(sourceContainer, i, inSlot, dir.getOpposite())) continue;
                if (!entity.matchesFilterForFace(dir, inSlot, level.registryAccess())) continue;

                // Route into the first insert target that can accept it
                boolean moved = false;
                for (InsertTarget target : insertTargets) {
                    if (target.pos.equals(sourcePos)) continue; // don't loop back into source container
                    if (target.hasFilters() && !target.matches(inSlot, level.registryAccess())) continue;

                    int toMoveCount = Math.min(inSlot.getCount(), maxItems);
                    ItemStack toMove = inSlot.copyWithCount(toMoveCount);
                    int accepted = insertIntoContainer(target.container, target.side, toMove, true);

                    if (accepted > 0) {
                        ItemStack extracted = (sourceContainer instanceof VolcaniteItemBarrelBlockEntity barrel) ?
                            barrel.extractItemForPipe(accepted, null, dir.getOpposite(), false) :
                            sourceContainer.removeItem(i, accepted);
                        sourceContainer.setChanged();
                        if (!extracted.isEmpty()) {
                            int realInserted = insertIntoContainer(target.container, target.side, extracted, false);
                            if (realInserted < extracted.getCount()) {
                                ItemStack leftover = extracted.copyWithCount(extracted.getCount() - realInserted);
                                insertIntoContainer(sourceContainer, dir.getOpposite(), leftover, false);
                            }
                        }
                        entity.lastExtractedSlot[dir.get3DDataValue()] = (i + 1) % size;
                        moved = true;
                        break;
                    }
                }
                if (moved) break;
            }
        }
    }

    public static List<InsertTarget> findItemPipeInsertTargets(Level level, BlockPos startPos) {
        List<InsertTarget> targets = new ArrayList<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(startPos);
        visited.add(startPos);

        int steps = 0;
        int maxSteps = 4096;
        while (!queue.isEmpty() && steps++ < maxSteps) {
            BlockPos curr = queue.poll();
            if (!level.isLoaded(curr)) continue;
            BlockEntity currBe = level.getBlockEntity(curr);

            if (currBe instanceof VolcaniteStoragePipeBlockEntity pipe) {
                for (Direction d : Direction.values()) {
                    if (pipe.isSideDisconnected(d)) continue;
                    BlockPos neighborPos = curr.relative(d);
                    if (!level.isLoaded(neighborPos)) continue;
                    if (pipe.getModule(d) == ModuleType.INSERT && pipe.getTransferType(d) == TYPE_ITEM) {
                        Container c = getContainerAt(level, neighborPos);
                        if (c != null) {
                            targets.add(new InsertTarget(c, d.getOpposite(), neighborPos, pipe.getFilterItemsForFace(d), pipe.isFilterBlacklist(d)));
                        }
                    }
                }
            }

            for (Direction d : Direction.values()) {
                if (!PipeConnectionHelper.canTraverseNetwork(level, curr, d)) continue;
                BlockPos next = curr.relative(d);
                if (!level.isLoaded(next)) continue;
                BlockState s = level.getBlockState(next);
                if (s.isAir()) continue;
                if (visited.add(next)) {
                    if (PipeConnectionHelper.isLogisticsPipe(s.getBlock())) {
                        queue.add(next);
                    }
                }
            }
        }
        return targets;
    }

    @Override
    public boolean canSwitchTransferType() {
        return false;
    }

    @Override
    public int getTransferType(Direction dir) {
        return TYPE_ITEM;
    }
}
