package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.*;

public class VolcaniteUniversalConduitBlockEntity extends VolcaniteStoragePipeBlockEntity {
    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(400_000, 200_000, 200_000) {
        @Override
        protected void onFinalCommit() { setChanged(); }
    };

    public final SingleFluidStorage fluidStorage = SingleFluidStorage.withFixedCapacity(FluidConstants.BUCKET * 16, this::setChanged);
    private int conduitTickCounter = 0;

    public VolcaniteUniversalConduitBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.VOLCANITE_UNIVERSAL_CONDUIT_BE, pos, state);
        setAllFacesTransferType(TYPE_OMNI);
    }

    public FluidVariant getStoredFluidVariant() {
        return fluidStorage.variant != null ? fluidStorage.variant : FluidVariant.blank();
    }

    public int getStoredFluidMb() {
        return (int) (fluidStorage.amount / 81L);
    }

    public int getMaxFluidMb() {
        return (int) (fluidStorage.getCapacity() / 81L);
    }

    public long getStoredEnergy() {
        return energyStorage.amount;
    }

    public long getMaxEnergy() {
        return energyStorage.capacity;
    }

    public static boolean allowsItems(int type) {
        return type == TYPE_ITEM || type == TYPE_OMNI;
    }

    public static boolean allowsFluids(int type) {
        return type == TYPE_FLUID || type == TYPE_OMNI;
    }

    public static boolean allowsEnergy(int type) {
        return type == TYPE_ENERGY || type == TYPE_OMNI;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VolcaniteUniversalConduitBlockEntity entity) {
        if (level.isClientSide()) return;
        entity.conduitTickCounter++;

        // 0. Pipe Module Ticking: Controller-backed network or standalone conduit-to-conduit routing
        if (entity.hasAnyModule() || entity.hasAnyEnergyUpgrade()) {
            VolcaniteStorageControllerBlockEntity controller = entity.findConnectedController();
            if (controller != null && controller.isPowered()) {
                VolcaniteStoragePipeBlockEntity.tick(level, pos, state, entity);
            } else if (entity.conduitTickCounter % 5 == 0 && entity.hasAnyModule()) {
                tickStandaloneItems(level, pos, entity);
                tickStandaloneFluids(level, pos, entity);
            }
        }

        boolean didSomething = false;

        // 1. Energy Handling (Push & Pull, respecting channel type, Import/Export & Redstone)
        if (entity.energyStorage.amount > 0) {
            for (Direction dir : Direction.values()) {
                if (entity.isSideDisconnected(dir) || !entity.isFaceActiveByRedstone(dir)) continue;
                if (!allowsEnergy(entity.getTransferType(dir))) continue;
                ModuleType module = entity.getModule(dir);
                if (module == ModuleType.EXTRACT) continue; // Import mode only pulls

                BlockPos targetPos = pos.relative(dir);
                BlockEntity neighborBe = level.getBlockEntity(targetPos);
                if (neighborBe instanceof VolcaniteStoragePipeBlockEntity np && np.isSideDisconnected(dir.getOpposite())) continue;
                if (neighborBe instanceof VolcaniteUniversalConduitBlockEntity nc && !allowsEnergy(nc.getTransferType(dir.getOpposite()))) continue;

                EnergyStorage target = EnergyStorage.SIDED.find(level, targetPos, dir.getOpposite());
                if (target != null && target.supportsInsertion()) {
                    if (neighborBe instanceof VolcaniteUniversalConduitBlockEntity otherConduit) {
                        if (entity.energyStorage.amount > otherConduit.energyStorage.amount + 10) {
                            long diff = (entity.energyStorage.amount - otherConduit.energyStorage.amount) / 2;
                            long toMove = Math.min(diff, 200_000L);
                            long moved = EnergyStorageUtil.move(entity.energyStorage, target, toMove, null);
                            if (moved > 0) didSomething = true;
                        }
                    } else if (!(neighborBe instanceof AstrapeEnergyPylonBlockEntity pylon && pylon.isOutputPylon(level))) {
                        long maxTransfer = entity.getMaxEnergyTransfer(dir);
                        long moved = EnergyStorageUtil.move(entity.energyStorage, target, maxTransfer, null);
                        if (moved > 0) didSomething = true;
                    }
                }
            }
        }

        // Pull energy from external sources (generators, batteries)
        if (entity.energyStorage.amount < entity.energyStorage.capacity) {
            for (Direction dir : Direction.values()) {
                if (entity.isSideDisconnected(dir) || !entity.isFaceActiveByRedstone(dir)) continue;
                if (!allowsEnergy(entity.getTransferType(dir))) continue;
                ModuleType module = entity.getModule(dir);
                if (module == ModuleType.INSERT) continue; // Export mode only pushes

                BlockPos sourcePos = pos.relative(dir);
                BlockEntity neighborBe = level.getBlockEntity(sourcePos);
                if (neighborBe instanceof VolcaniteStoragePipeBlockEntity np && np.isSideDisconnected(dir.getOpposite())) continue;
                if (!(neighborBe instanceof VolcaniteUniversalConduitBlockEntity)) {
                    EnergyStorage source = EnergyStorage.SIDED.find(level, sourcePos, dir.getOpposite());
                    if (source != null && source.supportsExtraction()) {
                        if (module == ModuleType.EXTRACT || entity.getEnergyUpgradeTier(dir) > 0 || !source.supportsInsertion()) {
                            long maxTransfer = entity.getMaxEnergyTransfer(dir);
                            long moved = EnergyStorageUtil.move(source, entity.energyStorage, maxTransfer, null);
                            if (moved > 0) didSomething = true;
                        }
                    }
                }
            }
        }

        // 2. Fluid Handling (Push, Pull, and Balance with neighbor conduits, respecting Import/Export, Filter & Redstone)
        if (entity.fluidStorage.amount > 0) {
            for (Direction dir : Direction.values()) {
                if (entity.isSideDisconnected(dir) || !entity.isFaceActiveByRedstone(dir)) continue;
                if (!allowsFluids(entity.getTransferType(dir))) continue;
                ModuleType module = entity.getModule(dir);
                if (module == ModuleType.EXTRACT) continue; // Import mode only pulls

                BlockPos targetPos = pos.relative(dir);
                BlockEntity neighborBe = level.getBlockEntity(targetPos);
                if (neighborBe instanceof VolcaniteStoragePipeBlockEntity np && np.isSideDisconnected(dir.getOpposite())) continue;
                if (neighborBe instanceof VolcaniteUniversalConduitBlockEntity nc && !allowsFluids(nc.getTransferType(dir.getOpposite()))) continue;

                Storage<FluidVariant> target = FluidStorage.SIDED.find(level, targetPos, dir.getOpposite());
                if (target != null && target.supportsInsertion()) {
                    if (neighborBe instanceof VolcaniteUniversalConduitBlockEntity otherConduit) {
                        if (entity.fluidStorage.amount > otherConduit.fluidStorage.amount + 81L) {
                            long diff = (entity.fluidStorage.amount - otherConduit.fluidStorage.amount) / 2;
                            long toMove = Math.min(diff, FluidConstants.BUCKET * 32);
                            long moved = StorageUtil.move(entity.fluidStorage, target, fluid -> !fluid.isBlank(), toMove, null);
                            if (moved > 0) didSomething = true;
                        }
                    } else {
                        long maxFluidTransfer = entity.getMaxFluidTransfer(dir) * 81L;
                        long moved = StorageUtil.move(entity.fluidStorage, target,
                                fluid -> !fluid.isBlank() && entity.matchesFluidFilterForFace(dir, fluid),
                                maxFluidTransfer, null);
                        if (moved > 0) didSomething = true;
                    }
                }
            }
        }

        // Pull fluid from external sources (tanks, pumps, distilleries)
        if (entity.fluidStorage.amount < entity.fluidStorage.getCapacity()) {
            for (Direction dir : Direction.values()) {
                if (entity.isSideDisconnected(dir) || !entity.isFaceActiveByRedstone(dir)) continue;
                if (!allowsFluids(entity.getTransferType(dir))) continue;
                ModuleType module = entity.getModule(dir);
                if (module == ModuleType.INSERT) continue; // Export mode only pushes

                BlockPos sourcePos = pos.relative(dir);
                BlockEntity neighborBe = level.getBlockEntity(sourcePos);
                if (neighborBe instanceof VolcaniteStoragePipeBlockEntity np && np.isSideDisconnected(dir.getOpposite())) continue;
                if (!(neighborBe instanceof VolcaniteUniversalConduitBlockEntity)) {
                    Storage<FluidVariant> source = FluidStorage.SIDED.find(level, sourcePos, dir.getOpposite());
                    if (source != null && source.supportsExtraction()) {
                        long maxFluidTransfer = entity.getMaxFluidTransfer(dir) * 81L;
                        long moved = StorageUtil.move(source, entity.fluidStorage,
                                fluid -> !fluid.isBlank() && entity.matchesFluidFilterForFace(dir, fluid),
                                maxFluidTransfer, null);
                        if (moved > 0) didSomething = true;
                    }
                }
            }
        }

        if (didSomething) {
            entity.setChanged();
            if (entity.conduitTickCounter % 10 == 0) {
                level.sendBlockUpdated(pos, state, state, 3);
            }
        }
    }

    private static void tickStandaloneItems(Level level, BlockPos pos, VolcaniteUniversalConduitBlockEntity entity) {
        List<InsertTarget> insertTargets = null;

        for (Direction dir : Direction.values()) {
            if (entity.isSideDisconnected(dir) || !entity.isFaceActiveByRedstone(dir)) continue;
            if (entity.getModule(dir) != ModuleType.EXTRACT) continue;
            if (!allowsItems(entity.getTransferType(dir))) continue;

            BlockPos sourcePos = pos.relative(dir);
            Container sourceContainer = getContainerAt(level, sourcePos);
            if (sourceContainer == null) continue;

            if (insertTargets == null) {
                insertTargets = findConduitInsertTargets(level, pos);
            }
            if (insertTargets.isEmpty()) continue;

            int maxItems = entity.getMaxItemTransfer(dir);
            int size = sourceContainer.getContainerSize();
            int startSlot = entity.lastExtractedSlot[dir.get3DDataValue()];
            int targetCount = insertTargets.size();
            int rrStart = entity.isRoundRobin(dir) ? (entity.roundRobinCursor[dir.get3DDataValue()] % targetCount) : 0;

            for (int step = 0; step < size; step++) {
                int i = (startSlot + step) % size;
                ItemStack inSlot = sourceContainer.getItem(i);
                if (inSlot.isEmpty()) continue;
                if (!isOutputSlot(sourceContainer, i, inSlot, dir.getOpposite())) continue;
                if (!entity.matchesFilterForFace(dir, inSlot, level.registryAccess())) continue;

                boolean moved = false;
                for (int tStep = 0; tStep < targetCount; tStep++) {
                    int tIdx = (rrStart + tStep) % targetCount;
                    InsertTarget target = insertTargets.get(tIdx);
                    if (target.pos.equals(sourcePos)) continue;
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
                        if (entity.isRoundRobin(dir)) {
                            entity.roundRobinCursor[dir.get3DDataValue()] = (tIdx + 1) % targetCount;
                        }
                        moved = true;
                        break;
                    }
                }
                if (moved) break;
            }
        }
    }

    private static void tickStandaloneFluids(Level level, BlockPos pos, VolcaniteUniversalConduitBlockEntity entity) {
        List<InsertTargetFluid> fluidTargets = null;

        for (Direction dir : Direction.values()) {
            if (entity.isSideDisconnected(dir) || !entity.isFaceActiveByRedstone(dir)) continue;
            if (entity.getModule(dir) != ModuleType.EXTRACT) continue;
            if (!allowsFluids(entity.getTransferType(dir))) continue;

            BlockPos sourcePos = pos.relative(dir);
            Storage<FluidVariant> sourceStorage = FluidStorage.SIDED.find(level, sourcePos, dir.getOpposite());
            if (sourceStorage == null || !sourceStorage.supportsExtraction()) continue;

            if (fluidTargets == null) {
                fluidTargets = findConduitFluidInsertTargets(level, pos);
            }
            if (fluidTargets.isEmpty()) continue;

            long maxDroplets = entity.getMaxFluidTransfer(dir) * 81L;
            int targetCount = fluidTargets.size();
            int rrStart = entity.isRoundRobin(dir) ? (entity.roundRobinCursor[dir.get3DDataValue()] % targetCount) : 0;

            for (int tStep = 0; tStep < targetCount; tStep++) {
                int tIdx = (rrStart + tStep) % targetCount;
                InsertTargetFluid target = fluidTargets.get(tIdx);
                if (target.pos.equals(sourcePos)) continue;
                long moved = StorageUtil.move(sourceStorage, target.storage,
                        fluid -> !fluid.isBlank() && entity.matchesFluidFilterForFace(dir, fluid) && target.matches(fluid),
                        maxDroplets, null);
                if (moved > 0) {
                    if (entity.isRoundRobin(dir)) {
                        entity.roundRobinCursor[dir.get3DDataValue()] = (tIdx + 1) % targetCount;
                    }
                    entity.setChanged();
                    break;
                }
            }
        }
    }

    public static List<InsertTarget> findConduitInsertTargets(Level level, BlockPos startPos) {
        List<InsertTarget> targets = new ArrayList<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        Map<BlockPos, Integer> distanceMap = new HashMap<>();
        queue.add(startPos);
        distanceMap.put(startPos, 0);

        int steps = 0;
        int maxSteps = 4096;
        while (!queue.isEmpty() && steps++ < maxSteps) {
            BlockPos curr = queue.poll();
            if (!level.isLoaded(curr)) continue;
            int dist = distanceMap.getOrDefault(curr, 0);
            BlockEntity currBe = level.getBlockEntity(curr);

            if (currBe instanceof VolcaniteStoragePipeBlockEntity pipe) {
                for (Direction d : Direction.values()) {
                    if (pipe.isSideDisconnected(d) || !pipe.isFaceActiveByRedstone(d)) continue;
                    BlockPos neighborPos = curr.relative(d);
                    if (!level.isLoaded(neighborPos)) continue;
                    if (pipe.getModule(d) == ModuleType.INSERT && allowsItems(pipe.getTransferType(d))) {
                        Container c = getContainerAt(level, neighborPos);
                        if (c != null) {
                            targets.add(new InsertTarget(c, d.getOpposite(), neighborPos, pipe.getFilterItemsForFace(d), pipe.isFilterBlacklist(d), pipe.getPriority(d), dist + 1));
                        }
                    }
                }
            }

            for (Direction d : Direction.values()) {
                if (currBe instanceof VolcaniteStoragePipeBlockEntity pipe && pipe.isSideDisconnected(d)) continue;
                BlockPos next = curr.relative(d);
                if (!level.isLoaded(next)) continue;
                BlockState s = level.getBlockState(next);
                if (s.isAir()) continue;
                if (level.getBlockEntity(next) instanceof VolcaniteStoragePipeBlockEntity np && np.isSideDisconnected(d.getOpposite())) continue;
                if (!distanceMap.containsKey(next)) {
                    if (s.getBlock() instanceof VolcaniteUniversalConduitBlock || s.getBlock() instanceof VolcaniteStoragePipeBlock) {
                        distanceMap.put(next, dist + 1);
                        queue.add(next);
                    }
                }
            }
        }

        targets.sort(Comparator.comparingInt((InsertTarget t) -> -t.priority).thenComparingInt(t -> t.distance));
        return targets;
    }

    public static List<InsertTargetFluid> findConduitFluidInsertTargets(Level level, BlockPos startPos) {
        List<InsertTargetFluid> targets = new ArrayList<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        Map<BlockPos, Integer> distanceMap = new HashMap<>();
        queue.add(startPos);
        distanceMap.put(startPos, 0);

        int steps = 0;
        int maxSteps = 4096;
        while (!queue.isEmpty() && steps++ < maxSteps) {
            BlockPos curr = queue.poll();
            if (!level.isLoaded(curr)) continue;
            int dist = distanceMap.getOrDefault(curr, 0);
            BlockEntity currBe = level.getBlockEntity(curr);

            if (currBe instanceof VolcaniteStoragePipeBlockEntity pipe) {
                for (Direction d : Direction.values()) {
                    if (pipe.isSideDisconnected(d) || !pipe.isFaceActiveByRedstone(d)) continue;
                    BlockPos neighborPos = curr.relative(d);
                    if (!level.isLoaded(neighborPos)) continue;
                    if (pipe.getModule(d) == ModuleType.INSERT && allowsFluids(pipe.getTransferType(d))) {
                        BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                        if (!(neighborBe instanceof VolcaniteUniversalConduitBlockEntity)) {
                            Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, neighborPos, d.getOpposite());
                            if (storage != null && storage.supportsInsertion()) {
                                targets.add(new InsertTargetFluid(storage, neighborPos, pipe.getFilterItemsForFace(d), pipe.isFilterBlacklist(d), pipe.getPriority(d), dist + 1));
                            }
                        }
                    }
                }
            }

            for (Direction d : Direction.values()) {
                if (currBe instanceof VolcaniteStoragePipeBlockEntity pipe && pipe.isSideDisconnected(d)) continue;
                BlockPos next = curr.relative(d);
                if (!level.isLoaded(next)) continue;
                BlockState s = level.getBlockState(next);
                if (s.isAir()) continue;
                if (level.getBlockEntity(next) instanceof VolcaniteStoragePipeBlockEntity np && np.isSideDisconnected(d.getOpposite())) continue;
                if (!distanceMap.containsKey(next)) {
                    if (s.getBlock() instanceof VolcaniteUniversalConduitBlock || s.getBlock() instanceof VolcaniteStoragePipeBlock) {
                        distanceMap.put(next, dist + 1);
                        queue.add(next);
                    }
                }
            }
        }

        targets.sort(Comparator.comparingInt((InsertTargetFluid t) -> -t.priority).thenComparingInt(t -> t.distance));
        return targets;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("Energy", energyStorage.amount);
        CompoundTag fluidTag = new CompoundTag();
        fluidStorage.writeNbt(fluidTag, registries);
        tag.put("Fluid", fluidTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyStorage.amount = tag.getLong("Energy");
        if (tag.contains("Fluid")) {
            fluidStorage.readNbt(tag.getCompound("Fluid"), registries);
        }
    }
}
