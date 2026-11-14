package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.fluid.FluidNetworkTransfers;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.*;

public class VolcaniteStorageControllerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int MAX_ENERGY = 200000;
    private int scanTimer = 0;

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(MAX_ENERGY, 50000, 50000) {
        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    public SimpleEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }

    public long consumeEnergy(long amount) {
        if (amount <= 0) return 0;
        try (net.fabricmc.fabric.api.transfer.v1.transaction.Transaction tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            long extracted = this.energyStorage.extract(amount, tx);
            if (extracted > 0) {
                tx.commit();
                setChanged();
                return extracted;
            }
        }
        return 0;
    }

    private final Set<BlockPos> connectedUnits = new HashSet<>();
    private final Set<BlockPos> connectedContainers = new HashSet<>();

    public int viewMode = 0;
    public int sortMode = 0; // 0 = Quantity Descending (Highest First), 1 = Quantity Ascending, 2 = Name A-Z, 3 = Name Z-A

    private long inventoryVersion = 0;

    public long getInventoryVersion() {
        return this.inventoryVersion;
    }

    public void markInventoryDirty() {
        this.inventoryVersion++;
    }

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index == 0) return (int) (energyStorage.amount >> 16);
            if (index == 1) return (int) (energyStorage.amount & 0xFFFF);
            if (index == 2) return (int) (getTotalStored() >> 16);
            if (index == 3) return (int) (getTotalStored() & 0xFFFF);
            if (index == 4) return viewMode;
            if (index == 5) return sortMode;
            return 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 4) viewMode = value;
            if (index == 5) sortMode = value;
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    private boolean networkDirty = true;

    public void markNetworkDirty() {
        this.networkDirty = true;
    }

    public VolcaniteStorageControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.VOLCANITE_STORAGE_CONTROLLER_BE, pos, state);
    }

    public boolean isPowered() {
        return this.energyStorage.amount > 0;
    }

    public int getEnergy() {
        return (int) this.energyStorage.amount;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VolcaniteStorageControllerBlockEntity entity) {
        if (level.isClientSide()) return;

        // Idle network power (15 RF/t when connected to units/containers)
        if (entity.energyStorage.amount > 0 && (!entity.connectedUnits.isEmpty() || !entity.connectedContainers.isEmpty())) {
            entity.energyStorage.amount = Math.max(0, entity.energyStorage.amount - 15);
        }

        boolean powered = entity.isPowered();
        if (state.getValue(VolcaniteStorageControllerBlock.WORKING) != powered) {
            level.setBlock(pos, state.setValue(VolcaniteStorageControllerBlock.WORKING, powered), 3);
            entity.setChanged();
        }

        if (!powered) {
            return; // Out of power, suspend network scanning and operation
        }

        // Optimized network scan: immediately when dirty / empty, or fallback every 100 ticks (5s)
        entity.scanTimer++;
        if (entity.networkDirty || entity.scanTimer >= 100 || (entity.connectedUnits.isEmpty() && entity.connectedContainers.isEmpty())) {
            entity.scanTimer = 0;
            entity.networkDirty = false;
            entity.scanNetwork(level, pos);
        }
    }

    public void scanNetwork(Level level, BlockPos startPos) {
        connectedUnits.clear();
        connectedContainers.clear();

        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(startPos);
        visited.add(startPos);

        int steps = 0;
        int maxSteps = 4096;
        while (!queue.isEmpty() && steps++ < maxSteps) {
            BlockPos current = queue.poll();
            if (!level.isLoaded(current)) continue;
            BlockEntity currentBe = level.getBlockEntity(current);

            for (Direction dir : Direction.values()) {
                if (!PipeConnectionHelper.canTraverseNetwork(level, current, dir)) continue;
                BlockPos next = current.relative(dir);
                if (!level.isLoaded(next)) continue;
                BlockState nextState = level.getBlockState(next);
                if (nextState.isAir()) continue;

                if (visited.add(next)) {
                    Block nextBlock = nextState.getBlock();
                    if (nextBlock instanceof VolcaniteStorageUnitBlock ||
                        nextBlock instanceof VolcaniteModularChestBlock ||
                        nextBlock instanceof VolcaniteChestBlock ||
                        nextBlock instanceof VolcaniteStorageDiskDriveBlock ||
                        nextBlock instanceof VolcaniteLiquidStorageTankBlock ||
                        nextBlock instanceof LiquidTankBlock ||
                        nextBlock instanceof HephaesteanArmoryBlock) {
                        connectedUnits.add(next);
                        queue.add(next);
                    } else if (nextBlock instanceof VolcaniteNetworkNodeEmitterBlock) {
                        BlockEntity be = level.getBlockEntity(next);
                        if (be instanceof VolcaniteNetworkNodeEmitterBlockEntity emitter && emitter.isLinked()) {
                            for (BlockPos receiverPos : emitter.getLinkedReceivers()) {
                                if (level.isLoaded(receiverPos) && visited.add(receiverPos)) {
                                    queue.add(receiverPos);
                                }
                            }
                            // Auto-discover active receivers in range (standard emitter range 64m)
                            for (BlockPos rPos : VolcaniteNetworkNodeReceiverBlockEntity.getActiveReceiversInDimension(level)) {
                                if (next.distSqr(rPos) <= (VolcaniteNetworkNodeEmitterBlockEntity.RANGE * VolcaniteNetworkNodeEmitterBlockEntity.RANGE)) {
                                    emitter.registerReceiver(rPos);
                                    if (level.isLoaded(rPos) && visited.add(rPos)) {
                                        queue.add(rPos);
                                    }
                                }
                            }
                        }
                        queue.add(next);
                    } else if (nextBlock instanceof VolcaniteNetworkNodeReceiverBlock) {
                        BlockEntity be = level.getBlockEntity(next);
                        if (be instanceof VolcaniteNetworkNodeReceiverBlockEntity receiver && receiver.isLinked()) {
                            BlockPos ePos = receiver.getLinkedEmitterPos();
                            if (ePos != null && level.isLoaded(ePos) && visited.add(ePos)) {
                                queue.add(ePos);
                            }
                        }
                        queue.add(next);
                    } else if (PipeConnectionHelper.isLogisticsPipe(nextBlock) ||
                               nextBlock instanceof VolcaniteStorageInterfaceBlock ||
                               nextBlock instanceof VolcaniteCrafterInterfaceBlock ||
                               nextBlock instanceof VolcaniteSortingStationBlock) {
                        queue.add(next);
                    } else {
                        // External container or fluid tank: if stepping from an Optic Fiber, require a connector on that face
                        if (currentBe instanceof VolcaniteStoragePipeBlockEntity pipe && pipe.isOpticFiber()) {
                            if (pipe.getModule(dir) == VolcaniteStoragePipeBlockEntity.ModuleType.NONE) {
                                continue;
                            }
                        }
                        BlockEntity be = level.getBlockEntity(next);
                        if (be instanceof Container c && VolcaniteStoragePipeBlockEntity.isPureStorage(c)
                                && !(be instanceof VolcaniteStorageUnitBlockEntity)
                                && !(be instanceof VolcaniteModularChestBlockEntity)
                                && !(be instanceof VolcaniteChestBlockEntity)
                                && !(be instanceof VolcaniteStorageDriveBlockEntity)) {
                            connectedContainers.add(next);
                        } else if (FluidStorage.SIDED.find(level, next, dir.getOpposite()) != null) {
                            connectedContainers.add(next);
                        }
                    }
                }
            }
        }
        markInventoryDirty();
    }

    public VolcaniteCrafterInterfaceBlockEntity findConnectedCrafterInterface() {
        if (level == null || !isPowered()) return null;
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
                    if (be instanceof VolcaniteCrafterInterfaceBlockEntity crafter) {
                        return crafter;
                    } else if (be instanceof VolcaniteNetworkNodeEmitterBlockEntity emitter && emitter.isLinked()) {
                        for (BlockPos rPos : emitter.getLinkedReceivers()) {
                            if (level.isLoaded(rPos) && visited.add(rPos)) queue.add(rPos);
                        }
                        queue.add(next);
                    } else if (be instanceof VolcaniteNetworkNodeReceiverBlockEntity receiver && receiver.isLinked()) {
                        BlockPos ePos = receiver.getLinkedEmitterPos();
                        if (ePos != null && level.isLoaded(ePos) && visited.add(ePos)) queue.add(ePos);
                        queue.add(next);
                    } else if (PipeConnectionHelper.isLogisticsPipe(nextState.getBlock())
                            || PipeConnectionHelper.isStorageNetworkBlock(nextState.getBlock())) {
                        queue.add(next);
                    }
                }
            }
        }
        return null;
    }

    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        return insertItemExcluding(stack, (Set<BlockPos>) null, simulate);
    }

    public ItemStack insertItemExcluding(ItemStack stack, BlockPos excludePos, boolean simulate) {
        Set<BlockPos> excludeSet = null;
        if (excludePos != null) {
            excludeSet = new HashSet<>();
            excludeSet.add(excludePos);
            if (level != null) {
                BlockState s = level.getBlockState(excludePos);
                if (s.getBlock() instanceof net.minecraft.world.level.block.ChestBlock) {
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        BlockPos p2 = excludePos.relative(d);
                        if (level.getBlockState(p2).is(s.getBlock())) {
                            excludeSet.add(p2);
                        }
                    }
                }
            }
        }
        return insertItemExcluding(stack, excludeSet, simulate);
    }

    public ItemStack insertItemExcluding(ItemStack stack, Set<BlockPos> excludePositions, boolean simulate) {
        if (stack.isEmpty() || !isPowered() || level == null) return stack;
        if (connectedUnits.isEmpty() && connectedContainers.isEmpty()) {
            scanNetwork(level, worldPosition);
        }
        ItemStack remaining = stack.copy();

        // 0. PRIORITY 0: PIPES WITH INSERT MODULES / EXPORT CONNECTORS (Filtered first, then unfiltered)
        VolcaniteStoragePipeBlockEntity.PipeNetworkScan scan = VolcaniteStoragePipeBlockEntity.scanNetwork(level, worldPosition);
        for (VolcaniteStoragePipeBlockEntity.InsertTarget target : scan.insertTargets) {
            if (excludePositions != null && excludePositions.contains(target.pos)) continue;
            if (target.hasFilters() && target.matches(remaining, level.registryAccess())) {
                int accepted = VolcaniteStoragePipeBlockEntity.insertIntoContainer(target.container, target.side, remaining, simulate);
                if (accepted > 0) {
                    remaining.shrink(accepted);
                    if (remaining.isEmpty()) {
                        if (!simulate) markInventoryDirty();
                        return ItemStack.EMPTY;
                    }
                }
            }
        }
        for (VolcaniteStoragePipeBlockEntity.InsertTarget target : scan.insertTargets) {
            if (excludePositions != null && excludePositions.contains(target.pos)) continue;
            if (!target.hasFilters()) {
                int accepted = VolcaniteStoragePipeBlockEntity.insertIntoContainer(target.container, target.side, remaining, simulate);
                if (accepted > 0) {
                    remaining.shrink(accepted);
                    if (remaining.isEmpty()) {
                        if (!simulate) markInventoryDirty();
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        // 1. PRIORITY 1: FILTERED MODULAR CHESTS (Modular Chests with active Whitelist 9-slot Filter Modules)
        for (BlockPos pos : connectedUnits) {
            if (excludePositions != null && excludePositions.contains(pos)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VolcaniteModularChestBlockEntity modularChest && modularChest.hasActiveWhitelistFilter()) {
                remaining = modularChest.insertItem(remaining, simulate);
                if (remaining.isEmpty()) {
                    if (!simulate) markInventoryDirty();
                    return ItemStack.EMPTY;
                }
            }
        }

        // 1B. PRIORITY 1B: CONNECTED TOOL CHEST / HEPHAESTEAN ARMORY (Dedicated Equipment & Tool Storage)
        if (isEquipment(remaining)) {
            for (BlockPos pos : connectedUnits) {
                if (excludePositions != null && excludePositions.contains(pos)) continue;
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof HephaesteanArmoryBlockEntity armory) {
                    remaining = armory.insertItem(remaining, simulate);
                    if (remaining.isEmpty()) {
                        if (!simulate) markInventoryDirty();
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        // 2. PRIORITY 2: DISK DRIVES (Volcanite Storage Disks) - Only store non-damageable stackable items to preserve durability!
        if (!remaining.isDamageableItem()) {
            for (BlockPos pos : connectedUnits) {
                if (excludePositions != null && excludePositions.contains(pos)) continue;
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof VolcaniteStorageDriveBlockEntity drive) {
                    remaining = drive.insertItem(remaining, simulate);
                    if (remaining.isEmpty()) {
                        if (!simulate) markInventoryDirty();
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        // 3. PRIORITY 3: VOLCANITE STORAGE UNITS (Grand Chests, Modular Chests, Volcanite Chests)
        for (BlockPos pos : connectedUnits) {
            if (excludePositions != null && excludePositions.contains(pos)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VolcaniteStorageUnitBlockEntity unit) {
                remaining = unit.insertItem(remaining, simulate);
                if (remaining.isEmpty()) {
                    if (!simulate) markInventoryDirty();
                    return ItemStack.EMPTY;
                }
            } else if (be instanceof VolcaniteModularChestBlockEntity modularChest) {
                remaining = modularChest.insertItem(remaining, simulate);
                if (remaining.isEmpty()) {
                    if (!simulate) markInventoryDirty();
                    return ItemStack.EMPTY;
                }
            } else if (be instanceof VolcaniteChestBlockEntity volcaniteChest) {
                remaining = volcaniteChest.insertItem(remaining, simulate);
                if (remaining.isEmpty()) {
                    if (!simulate) markInventoryDirty();
                    return ItemStack.EMPTY;
                }
            }
        }

        // 4. PRIORITY 4: EXTERNAL CHESTS (Connected vanilla chests / containers)
        for (BlockPos pos : connectedContainers) {
            if (excludePositions != null && excludePositions.contains(pos)) continue;
            Container container = VolcaniteStoragePipeBlockEntity.getContainerAt(level, pos);
            if (container != null) {
                int accepted = VolcaniteStoragePipeBlockEntity.insertIntoContainer(container, null, remaining, simulate);
                if (accepted > 0) {
                    remaining.shrink(accepted);
                    if (remaining.isEmpty()) {
                        if (!simulate) markInventoryDirty();
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        if (!simulate && remaining.getCount() < stack.getCount()) {
            markInventoryDirty();
        }

        return remaining;
    }

    public ItemStack extractItem(Item item, int amount, boolean simulate) {
        return extractItemExcluding(item, amount, null, simulate);
    }

    public ItemStack extractItemExcluding(Item item, int amount, @org.jetbrains.annotations.Nullable BlockPos excludePos, boolean simulate) {
        return extractItemExcluding(item, amount, excludePos, simulate, ItemStack.EMPTY);
    }

    public ItemStack extractMatchingItem(ItemStack template, int amount, boolean simulate) {
        if (template.isEmpty()) return ItemStack.EMPTY;
        return extractItemExcluding(template.getItem(), amount, null, simulate, template);
    }

    private ItemStack extractItemExcluding(Item item, int amount, @org.jetbrains.annotations.Nullable BlockPos excludePos,
                                          boolean simulate, ItemStack template) {
        if (item == null || amount <= 0 || !isPowered() || level == null) return ItemStack.EMPTY;
        if (connectedUnits.isEmpty() && connectedContainers.isEmpty()) {
            scanNetwork(level, worldPosition);
        }
        int needed = amount;
        ItemStack result = ItemStack.EMPTY;

        // 1. Extract from Storage Disks first
        for (BlockPos pos : connectedUnits) {
            if (excludePos != null && excludePos.equals(pos)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VolcaniteStorageDriveBlockEntity drive) {
                // Inspect before mutating: a single returned stack must have one variant.
                ItemStack preview = template.isEmpty() ? drive.extractItem(item, needed, true)
                        : drive.extractMatchingItem(template, needed, true);
                if (!result.isEmpty() && !ItemStack.isSameItemSameComponents(result, preview)) continue;
                ItemStack extracted = simulate ? preview : template.isEmpty() ? drive.extractItem(item, needed, false)
                        : drive.extractMatchingItem(template, needed, false);
                if (!extracted.isEmpty()) {
                    if (result.isEmpty()) result = extracted.copy();
                    else result.grow(extracted.getCount());
                    needed -= extracted.getCount();
                    if (needed <= 0) {
                        if (!simulate && !result.isEmpty()) markInventoryDirty();
                        return result;
                    }
                }
            }
        }

        // 1B. Extract from connected Hephaestean Armories / Tool Chests
        for (BlockPos pos : connectedUnits) {
            if (excludePos != null && excludePos.equals(pos)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HephaesteanArmoryBlockEntity armory) {
                for (int i = 0; i < armory.getContainerSize(); i++) {
                    ItemStack inSlot = armory.getItem(i);
                    if (!inSlot.isEmpty() && inSlot.is(item)
                            && (template.isEmpty() || ItemStack.isSameItemSameComponents(template, inSlot))
                            && (result.isEmpty() || ItemStack.isSameItemSameComponents(result, inSlot))) {
                        int toTake = Math.min(needed, inSlot.getCount());
                        ItemStack extracted = simulate ? inSlot.copyWithCount(toTake) : armory.removeItem(i, toTake);
                        if (!extracted.isEmpty()) {
                            if (result.isEmpty()) result = extracted.copy();
                            else result.grow(extracted.getCount());
                            needed -= extracted.getCount();
                            if (needed <= 0) {
                                if (!simulate && !result.isEmpty()) markInventoryDirty();
                                return result;
                            }
                        }
                    }
                }
            }
        }

        // 2. Extract from Volcanite Storage Units (Grand Chests, Modular Chests, Volcanite Chests)
        for (BlockPos pos : connectedUnits) {
            if (excludePos != null && excludePos.equals(pos)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VolcaniteStorageUnitBlockEntity unit) {
                // Inspect before mutating: a single returned stack must have one variant.
                ItemStack preview = template.isEmpty() ? unit.extractItem(item, needed, true)
                        : unit.extractMatchingItem(template, needed, true);
                if (!result.isEmpty() && !ItemStack.isSameItemSameComponents(result, preview)) continue;
                ItemStack extracted = simulate ? preview : template.isEmpty() ? unit.extractItem(item, needed, false)
                        : unit.extractMatchingItem(template, needed, false);
                if (!extracted.isEmpty()) {
                    if (result.isEmpty()) result = extracted.copy();
                    else result.grow(extracted.getCount());
                    needed -= extracted.getCount();
                    if (needed <= 0) {
                        if (!simulate && !result.isEmpty()) markInventoryDirty();
                        return result;
                    }
                }
            } else if (be instanceof VolcaniteModularChestBlockEntity modularChest) {
                // Inspect before mutating: a single returned stack must have one variant.
                ItemStack preview = template.isEmpty() ? modularChest.extractItem(item, needed, true)
                        : modularChest.extractMatchingItem(template, needed, true);
                if (!result.isEmpty() && !ItemStack.isSameItemSameComponents(result, preview)) continue;
                ItemStack extracted = simulate ? preview : template.isEmpty() ? modularChest.extractItem(item, needed, false)
                        : modularChest.extractMatchingItem(template, needed, false);
                if (!extracted.isEmpty()) {
                    if (result.isEmpty()) result = extracted.copy();
                    else result.grow(extracted.getCount());
                    needed -= extracted.getCount();
                    if (needed <= 0) {
                        if (!simulate && !result.isEmpty()) markInventoryDirty();
                        return result;
                    }
                }
            } else if (be instanceof VolcaniteChestBlockEntity volcaniteChest) {
                // Inspect before mutating: a single returned stack must have one variant.
                ItemStack preview = template.isEmpty() ? volcaniteChest.extractItem(item, needed, true)
                        : volcaniteChest.extractMatchingItem(template, needed, true);
                if (!result.isEmpty() && !ItemStack.isSameItemSameComponents(result, preview)) continue;
                ItemStack extracted = simulate ? preview : template.isEmpty() ? volcaniteChest.extractItem(item, needed, false)
                        : volcaniteChest.extractMatchingItem(template, needed, false);
                if (!extracted.isEmpty()) {
                    if (result.isEmpty()) result = extracted.copy();
                    else result.grow(extracted.getCount());
                    needed -= extracted.getCount();
                    if (needed <= 0) {
                        if (!simulate && !result.isEmpty()) markInventoryDirty();
                        return result;
                    }
                }
            }
        }

        // 3. Extract from connected external Containers
        for (BlockPos pos : connectedContainers) {
            if (excludePos != null && excludePos.equals(pos)) continue;
            Container container = VolcaniteStoragePipeBlockEntity.getContainerAt(level, pos);
            if (container != null) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack inSlot = container.getItem(i);
                    if (!inSlot.isEmpty() && inSlot.is(item)
                            && (template.isEmpty() || ItemStack.isSameItemSameComponents(template, inSlot))
                            && (result.isEmpty() || ItemStack.isSameItemSameComponents(result, inSlot))
                            && VolcaniteStoragePipeBlockEntity.isOutputSlot(container, i, inSlot, null)) {
                        int toTake = Math.min(needed, inSlot.getCount());
                        if (result.isEmpty()) {
                            result = inSlot.copyWithCount(toTake);
                        } else {
                            result.grow(toTake);
                        }
                        if (!simulate) {
                            container.removeItem(i, toTake);
                            container.setChanged();
                        }
                        needed -= toTake;
                        if (needed <= 0) {
                            if (!simulate && !result.isEmpty()) markInventoryDirty();
                            return result;
                        }
                    }
                }
            }
        }

        if (!simulate && !result.isEmpty()) {
            markInventoryDirty();
        }

        return result;
    }

    public Map<Item, Integer> getUnifiedInventory() {
        Map<Item, Integer> map = new LinkedHashMap<>();
        if (!isPowered() || level == null) return map;
        if (connectedUnits.isEmpty() && connectedContainers.isEmpty()) {
            scanNetwork(level, worldPosition);
        }

        // Collect from units, modular chests, volcanite chests & drives
        for (BlockPos pos : connectedUnits) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VolcaniteStorageDriveBlockEntity drive) {
                drive.collectStoredItems(map);
            } else if (be instanceof VolcaniteStorageUnitBlockEntity unit) {
                unit.collectStoredItems(map);
            } else if (be instanceof VolcaniteModularChestBlockEntity modularChest) {
                modularChest.collectStoredItems(map);
            } else if (be instanceof VolcaniteChestBlockEntity volcaniteChest) {
                volcaniteChest.collectStoredItems(map);
            }
        }

        // Collect from external containers
        for (BlockPos pos : connectedContainers) {
            Container container = VolcaniteStoragePipeBlockEntity.getContainerAt(level, pos);
            if (container != null) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack s = container.getItem(i);
                    if (!s.isEmpty() && VolcaniteStoragePipeBlockEntity.isOutputSlot(container, i, s, null)) {
                        map.put(s.getItem(), map.getOrDefault(s.getItem(), 0) + s.getCount());
                    }
                }
            }
        }

        return map;
    }

    public Map<FluidVariant, Integer> getUnifiedFluids() {
        Map<FluidVariant, Integer> map = new LinkedHashMap<>();
        if (!isPowered() || level == null) return map;

        // 1. Collect from liquid storage disks
        for (BlockPos pos : connectedUnits) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VolcaniteStorageDriveBlockEntity drive) {
                drive.collectStoredFluids(map);
            }
        }

        // 2. Collect from liquid storage tanks
        for (BlockPos pos : connectedUnits) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VolcaniteLiquidStorageTankBlockEntity tank) {
                Fluid active = tank.getActiveFluid();
                int amt = tank.getTotalFluidAmount();
                if (active != Fluids.EMPTY && amt > 0) {
                    FluidVariant fv = FluidVariant.of(active);
                    map.put(fv, map.getOrDefault(fv, 0) + amt);
                }
            } else if (be instanceof LiquidTankBlockEntity liquidTank) {
                FluidVariant fv = liquidTank.getStoredFluid();
                int amt = liquidTank.getFluidAmount();
                if (!fv.isBlank() && amt > 0) {
                    map.put(fv, map.getOrDefault(fv, 0) + amt);
                }
            }
        }

        // 3. Collect from external fluid containers
        for (BlockPos pos : connectedContainers) {
            for (Direction dir : Direction.values()) {
                Storage<FluidVariant> extStorage = FluidStorage.SIDED.find(level, pos, dir);
                if (extStorage != null) {
                    for (var view : extStorage) {
                        if (!view.isResourceBlank() && view.getAmount() > 0) {
                            FluidVariant fv = view.getResource();
                            long raw = view.getAmount();
                            int amt_mB;
                            if (raw >= Long.MAX_VALUE / 2 || raw / 81L >= Integer.MAX_VALUE) {
                                amt_mB = Integer.MAX_VALUE;
                            } else {
                                amt_mB = (int) (raw / 81L);
                            }
                            long sum = (long) map.getOrDefault(fv, 0) + (long) amt_mB;
                            map.put(fv, (int) Math.min((long) Integer.MAX_VALUE, sum));
                        }
                    }
                }
            }
        }

        return map;
    }

    public static boolean isEquipment(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.isDamageableItem()) return true;
        Item item = stack.getItem();
        return item instanceof net.minecraft.world.item.ArmorItem ||
               item instanceof net.minecraft.world.item.TieredItem ||
               item instanceof net.minecraft.world.item.BowItem ||
               item instanceof net.minecraft.world.item.CrossbowItem ||
               item instanceof net.minecraft.world.item.ShieldItem ||
               item instanceof net.minecraft.world.item.TridentItem ||
               item instanceof net.minecraft.world.item.FishingRodItem ||
               item instanceof net.minecraft.world.item.ShearsItem;
    }

    public List<ItemStack> getUnifiedEquipment() {
        List<ItemStack> list = new ArrayList<>();
        if (!isPowered() || level == null) return list;

        // 1. Collect from connected Hephaestean Armory / Tool Chests
        for (BlockPos pos : connectedUnits) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HephaesteanArmoryBlockEntity armory) {
                for (int i = 0; i < armory.getContainerSize(); i++) {
                    ItemStack s = armory.getItem(i);
                    if (!s.isEmpty()) {
                        list.add(s.copy());
                    }
                }
            } else if (be instanceof VolcaniteModularChestBlockEntity modularChest) {
                for (int i = 0; i < modularChest.getContainerSize(); i++) {
                    ItemStack s = modularChest.getItem(i);
                    if (!s.isEmpty() && isEquipment(s)) {
                        list.add(s.copy());
                    }
                }
            } else if (be instanceof VolcaniteChestBlockEntity volcaniteChest) {
                for (int i = 0; i < volcaniteChest.getContainerSize(); i++) {
                    ItemStack s = volcaniteChest.getItem(i);
                    if (!s.isEmpty() && isEquipment(s)) {
                        list.add(s.copy());
                    }
                }
            }
        }

        // 2. Also collect any equipment from connected external containers
        for (BlockPos pos : connectedContainers) {
            Container container = VolcaniteStoragePipeBlockEntity.getContainerAt(level, pos);
            if (container != null) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack s = container.getItem(i);
                    if (!s.isEmpty() && isEquipment(s) && VolcaniteStoragePipeBlockEntity.isOutputSlot(container, i, s, null)) {
                        list.add(s.copy());
                    }
                }
            }
        }

        return list;
    }

    public ItemStack extractSpecificEquipment(ItemStack target, boolean simulate) {
        if (target.isEmpty() || !isPowered() || level == null) return ItemStack.EMPTY;

        // 1. Check Hephaestean Armories
        for (BlockPos pos : connectedUnits) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HephaesteanArmoryBlockEntity armory) {
                ItemStack extracted = armory.extractSpecific(target, simulate);
                if (!extracted.isEmpty()) {
                    if (!simulate) markInventoryDirty();
                    return extracted;
                }
            } else if (be instanceof VolcaniteModularChestBlockEntity modularChest) {
                for (int i = 0; i < modularChest.getContainerSize(); i++) {
                    ItemStack inSlot = modularChest.getItem(i);
                    if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(inSlot, target)) {
                        ItemStack res = inSlot.split(1);
                        if (!simulate) {
                            modularChest.setChanged();
                            markInventoryDirty();
                        }
                        return res;
                    }
                }
            } else if (be instanceof VolcaniteChestBlockEntity volcaniteChest) {
                for (int i = 0; i < volcaniteChest.getContainerSize(); i++) {
                    ItemStack inSlot = volcaniteChest.getItem(i);
                    if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(inSlot, target)) {
                        ItemStack res = inSlot.split(1);
                        if (!simulate) {
                            volcaniteChest.setChanged();
                            markInventoryDirty();
                        }
                        return res;
                    }
                }
            }
        }

        // 2. Check external containers
        for (BlockPos pos : connectedContainers) {
            Container container = VolcaniteStoragePipeBlockEntity.getContainerAt(level, pos);
            if (container != null) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack inSlot = container.getItem(i);
                    if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(inSlot, target) && VolcaniteStoragePipeBlockEntity.isOutputSlot(container, i, inSlot, null)) {
                        ItemStack res = inSlot.split(1);
                        if (!simulate) {
                            container.setChanged();
                            markInventoryDirty();
                        }
                        return res;
                    }
                }
            }
        }

        return ItemStack.EMPTY;
    }

    public int insertFluid(FluidVariant fluid, int amount_mB, boolean simulate) {
        return transferFluid(fluid, amount_mB, true, simulate, false);
    }

    public int extractFluid(FluidVariant fluid, int amount_mB, boolean simulate) {
        return transferFluid(fluid, amount_mB, false, simulate, false);
    }

    public boolean insertFluidExactly(FluidVariant fluid, int amount_mB) {
        return amount_mB > 0 && transferFluid(fluid, amount_mB, true, false, true) == amount_mB;
    }

    public boolean extractFluidExactly(FluidVariant fluid, int amount_mB) {
        return amount_mB > 0 && transferFluid(fluid, amount_mB, false, false, true) == amount_mB;
    }

    private int transferFluid(FluidVariant fluid, int amount, boolean inserting, boolean simulate, boolean exact) {
        if (fluid.isBlank() || amount <= 0 || !isPowered() || level == null) return 0;
        if (connectedUnits.isEmpty() && connectedContainers.isEmpty()) scanNetwork(level, worldPosition);
        List<FluidNetworkTransfers.Endpoint> endpoints = new ArrayList<>();

        // Keep disk priority, and share one transaction across disks, tanks and external storage.
        for (BlockPos pos : connectedUnits) {
            if (!level.isLoaded(pos)) continue;
            if (level.getBlockEntity(pos) instanceof VolcaniteStorageDriveBlockEntity drive) {
                endpoints.add(drive::transferFluid);
            }
        }
        for (BlockPos pos : connectedUnits) {
            if (!level.isLoaded(pos)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            Storage<FluidVariant> storage = be instanceof VolcaniteLiquidStorageTankBlockEntity tank ? tank
                    : be instanceof LiquidTankBlockEntity tank ? tank : null;
            if (storage != null) endpoints.add((variant, requested, insert, transaction) ->
                    FluidNetworkTransfers.transferStorage(storage, variant, requested, insert, transaction));
        }
        for (BlockPos pos : connectedContainers) {
            if (!level.isLoaded(pos) || connectedUnits.contains(pos)) continue;
            endpoints.add((variant, requested, insert, transaction) -> {
                // Different faces often expose the same inventory. Use one accepting face per block.
                for (Direction direction : Direction.values()) {
                    Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, pos, direction);
                    if (storage == null) continue;
                    int moved = FluidNetworkTransfers.transferStorage(
                            storage, variant, requested, insert, transaction);
                    if (moved > 0) return moved;
                }
                return 0;
            });
        }
        int moved = FluidNetworkTransfers.transfer(
                endpoints, fluid, amount, inserting, simulate, exact);
        if (!simulate && moved > 0) markInventoryDirty();
        return moved;
    }

    public int getTotalStored() {
        int total = 0;
        if (level == null) return 0;
        for (BlockPos pos : connectedUnits) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VolcaniteStorageDriveBlockEntity drive) {
                total += drive.getTotalStored();
            } else if (be instanceof VolcaniteStorageUnitBlockEntity unit) {
                total += unit.getTotalStored();
            } else if (be instanceof VolcaniteModularChestBlockEntity modularChest) {
                total += modularChest.getTotalStored();
            } else if (be instanceof VolcaniteChestBlockEntity volcaniteChest) {
                total += volcaniteChest.getTotalStored();
            }
        }
        for (BlockPos pos : connectedContainers) {
            Container c = VolcaniteStoragePipeBlockEntity.getContainerAt(level, pos);
            if (c != null) {
                for (int i = 0; i < c.getContainerSize(); i++) {
                    ItemStack s = c.getItem(i);
                    if (!s.isEmpty() && VolcaniteStoragePipeBlockEntity.isOutputSlot(c, i, s, null)) {
                        total += s.getCount();
                    }
                }
            }
        }
        return total;
    }

    public int getMaxCapacity() {
        int total = 0;
        if (level == null) return 0;
        for (BlockPos pos : connectedUnits) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VolcaniteStorageDriveBlockEntity drive) {
                total += drive.getMaxCapacity();
            } else if (be instanceof VolcaniteStorageUnitBlockEntity unit) {
                total += unit.getMaxCapacity();
            } else if (be instanceof VolcaniteModularChestBlockEntity modularChest) {
                total += modularChest.getMaxCapacity();
            } else if (be instanceof VolcaniteChestBlockEntity volcaniteChest) {
                total += volcaniteChest.getMaxCapacity();
            }
        }
        for (BlockPos pos : connectedContainers) {
            Container c = VolcaniteStoragePipeBlockEntity.getContainerAt(level, pos);
            if (c != null) {
                total += c.getContainerSize() * 64;
            }
        }
        return total;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putLong("Energy", energyStorage.amount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.energyStorage.amount = Math.min(this.energyStorage.capacity, Math.max(0, tag.getLong("Energy")));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.volcanite_storage_controller");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        if (level != null && !level.isClientSide() && isPowered()) {
            scanNetwork(level, worldPosition);
        }
        return new VolcaniteStorageTerminalMenu(syncId, inv, this, this.data);
    }
}
