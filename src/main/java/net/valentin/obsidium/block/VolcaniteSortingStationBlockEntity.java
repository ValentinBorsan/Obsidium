package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.VolcaniteBottleItem;
import net.valentin.obsidium.item.VolcaniteLiquidStorageDiskItem;
import net.valentin.obsidium.item.VolcaniteLiquidUpgradeItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class VolcaniteSortingStationBlockEntity extends BlockEntity implements MenuProvider {
    public static final int MODE_DISABLED = 0;
    public static final int MODE_ITEM = 1; // Green (Item extraction)
    public static final int MODE_LIQUID = 2; // Blue (Liquid extraction)
    public static final int MODE_VOID = 3; // Magenta (Void/Trash extra items or fluids)

    // 6 faces * 4 slots per face = 24 slots:
    // Slot f * 4 + 0: Advanced Module Slot (for VolcaniteAdvancedFilterModuleItem)
    // Slot f * 4 + 1: Filter Slot 1 (Base filter, always active)
    // Slot f * 4 + 2: Filter Slot 2 (Active when Advanced Module is installed)
    // Slot f * 4 + 3: Filter Slot 3 (Active when Advanced Module is installed)
    public static final int INVENTORY_SIZE = 24;

    private final int[] faceModes = new int[6];
    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            VolcaniteSortingStationBlockEntity.this.setChanged();
            VolcaniteSortingStationBlockEntity.this.syncToClient();
        }
    };

    private int tickCounter = 0;
    private int roundRobinCounter = 0;

    public boolean hasAdvancedModule(int faceIndex) {
        if (faceIndex < 0 || faceIndex >= 6)
            return false;
        ItemStack modStack = inventory.getItem(faceIndex * 4 + 0);
        return !modStack.isEmpty() && modStack.is(ObsidiumMod.VOLCANITE_ADVANCED_FILTER_MODULE);
    }

    public List<ItemStack> getActiveFilterStacks(int faceIndex) {
        List<ItemStack> list = new ArrayList<>();
        if (faceIndex < 0 || faceIndex >= 6)
            return list;

        // Slot 1 is always active
        ItemStack s1 = inventory.getItem(faceIndex * 4 + 1);
        if (!s1.isEmpty())
            list.add(s1);

        // Slots 2 & 3 are active only when Advanced Module is installed
        if (hasAdvancedModule(faceIndex)) {
            ItemStack s2 = inventory.getItem(faceIndex * 4 + 2);
            if (!s2.isEmpty())
                list.add(s2);

            ItemStack s3 = inventory.getItem(faceIndex * 4 + 3);
            if (!s3.isEmpty())
                list.add(s3);
        }
        return list;
    }

    public int getFaceConnection(int index) {
        if (level == null || index < 0 || index >= 6)
            return 0;
        Direction dir = Direction.from3DDataValue(index);
        BlockPos neighborPos = worldPosition.relative(dir);
        BlockState neighborState = level.getBlockState(neighborPos);
        if (neighborState.isAir())
            return 0;
        net.minecraft.world.level.block.Block b = neighborState.getBlock();
        if (b instanceof VolcaniteStoragePipeBlock
                || b instanceof VolcaniteUniversalConduitBlock) {
            return 1; // Pipe connected
        }
        BlockEntity be = level.getBlockEntity(neighborPos);
        if (b instanceof LiquidTankBlock || be instanceof LiquidTankBlockEntity
                || FluidStorage.SIDED.find(level, neighborPos, dir.getOpposite()) != null) {
            return 2; // Tank connected
        }
        if (be instanceof Container || PipeConnectionHelper.canConnect(level, neighborPos, dir)) {
            return 3; // Machine/Container connected
        }
        return 0;
    }

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= 0 && index < 6)
                return faceModes[index];
            if (index >= 6 && index < 12)
                return getFaceConnection(index - 6);
            return 0;
        }

        @Override
        public void set(int index, int value) {
            if (index >= 0 && index < 6) {
                faceModes[index] = value;
                setChanged();
                syncToClient();
            }
        }

        @Override
        public int getCount() {
            return 12;
        }
    };

    public VolcaniteSortingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.VOLCANITE_SORTING_STATION_BE, pos, state);
        Arrays.fill(faceModes, MODE_DISABLED);

        this.inventory.addListener(container -> {
            for (int i = 0; i < 6; i++) {
                List<ItemStack> active = getActiveFilterStacks(i);
                if (active.isEmpty()) {
                    if (faceModes[i] != MODE_VOID) {
                        faceModes[i] = MODE_DISABLED;
                    }
                } else if (faceModes[i] == MODE_DISABLED) {
                    boolean hasFluid = false;
                    for (ItemStack s : active) {
                        if (isFluidContainer(s)) {
                            hasFluid = true;
                            break;
                        }
                    }
                    faceModes[i] = hasFluid ? MODE_LIQUID : MODE_ITEM;
                }
            }
            setChanged();
            syncToClient();
        });
    }

    public Container getInventory() {
        return inventory;
    }

    public int getFaceMode(Direction dir) {
        return faceModes[dir.get3DDataValue()];
    }

    public void setFaceMode(Direction dir, int mode) {
        faceModes[dir.get3DDataValue()] = mode;
        setChanged();
        syncToClient();
    }

    public void cycleFaceMode(int faceIndex) {
        if (faceIndex >= 0 && faceIndex < 6) {
            faceModes[faceIndex] = (faceModes[faceIndex] + 1) % 4;
            setChanged();
            syncToClient();
        }
    }

    public void spawnVoidParticles(Level level, BlockPos pos, Direction dir) {
        if (level instanceof ServerLevel serverLevel) {
            double px = pos.getX() + 0.5 + dir.getStepX() * 0.55;
            double py = pos.getY() + 0.5 + dir.getStepY() * 0.55;
            double pz = pos.getZ() + 0.5 + dir.getStepZ() * 0.55;
            serverLevel.sendParticles(ParticleTypes.PORTAL, px, py, pz, 4, 0.05, 0.05, 0.05, 0.02);
        }
    }

    public ItemStack getFilterItem(Direction dir) {
        return inventory.getItem(dir.get3DDataValue() * 4 + 1);
    }

    public ItemStack getModuleItem(Direction dir) {
        return inventory.getItem(dir.get3DDataValue() * 4 + 0);
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static class TargetDestination {
        public final Container container;
        public final Direction side;
        public final BlockPos pos;

        public TargetDestination(Container container, Direction side, BlockPos pos) {
            this.container = container;
            this.side = side;
            this.pos = pos;
        }
    }

    public static class FaceItemRoute {
        public final Direction dir;
        public final Item item;
        public final List<TargetDestination> destinations;
        public int currentCount;
        public int neededCount;

        public FaceItemRoute(Direction dir, Item item, List<TargetDestination> destinations, int currentCount,
                int neededCount) {
            this.dir = dir;
            this.item = item;
            this.destinations = destinations;
            this.currentCount = currentCount;
            this.neededCount = neededCount;
        }
    }

    public static class TargetFluidDestination {
        public final Storage<FluidVariant> storage;
        public final Direction side;
        public final BlockPos pos;

        public TargetFluidDestination(Storage<FluidVariant> storage, Direction side, BlockPos pos) {
            this.storage = storage;
            this.side = side;
            this.pos = pos;
        }
    }

    public static class FaceFluidRoute {
        public final Direction dir;
        public final FluidVariant fluid;
        public final List<TargetFluidDestination> destinations;
        public int current_mB;
        public int needed_mB;

        public FaceFluidRoute(Direction dir, FluidVariant fluid, List<TargetFluidDestination> destinations,
                int current_mB, int needed_mB) {
            this.dir = dir;
            this.fluid = fluid;
            this.destinations = destinations;
            this.current_mB = current_mB;
            this.needed_mB = needed_mB;
        }
    }

    public static boolean isBulkStorage(Container container) {
        if (container == null)
            return false;
        return container instanceof net.minecraft.world.level.block.entity.ChestBlockEntity ||
                container instanceof VolcaniteStorageUnitBlockEntity ||
                container instanceof VolcaniteModularChestBlockEntity ||
                container instanceof VolcaniteChestBlockEntity;
    }

    public static boolean canContainerAcceptItem(Container container, Direction side, Item item) {
        if (container == null || item == null)
            return false;
        ItemStack sample = new ItemStack(item);
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (VolcaniteStoragePipeBlockEntity.isInputSlot(container, i, sample, side)) {
                return true;
            }
        }
        return false;
    }

    public static int getItemCountInContainer(Container container, Direction side, Item item) {
        if (container == null || item == null)
            return 0;
        int count = 0;
        ItemStack sample = new ItemStack(item);
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (!VolcaniteStoragePipeBlockEntity.isInputSlot(container, slot, sample, side)) {
                continue;
            }
            ItemStack inSlot = container.getItem(slot);
            if (!inSlot.isEmpty() && inSlot.is(item)) {
                count += inSlot.getCount();
            }
        }
        return count;
    }

    public static int getFreeSpaceInContainer(Container container, Direction side, Item item) {
        if (container == null || item == null)
            return 0;
        int space = 0;
        ItemStack sample = new ItemStack(item);
        int maxStack = Math.min(container.getMaxStackSize(sample), sample.getMaxStackSize());

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (!VolcaniteStoragePipeBlockEntity.isInputSlot(container, slot, sample, side))
                continue;

            ItemStack inSlot = container.getItem(slot);
            if (inSlot.isEmpty()) {
                space += maxStack;
            } else if (ItemStack.isSameItemSameComponents(inSlot, sample)) {
                space += Math.max(0, maxStack - inSlot.getCount());
            }
        }
        return space;
    }

    public static int insertFilteredItemIntoContainer(Container container, Direction side, ItemStack stack,
            boolean isMachine, boolean simulate) {
        if (container == null || stack.isEmpty())
            return 0;
        int remaining = stack.getCount();
        int size = container.getContainerSize();

        // Pass 1: Add to existing matching stacks in valid input slots
        for (int i = 0; i < size; i++) {
            if (!VolcaniteStoragePipeBlockEntity.isInputSlot(container, i, stack, side))
                continue;

            ItemStack inSlot = container.getItem(i);
            if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(inSlot, stack)) {
                int maxStack = Math.min(container.getMaxStackSize(stack), inSlot.getMaxStackSize());
                if (inSlot.getCount() < maxStack) {
                    int space = maxStack - inSlot.getCount();
                    int canAdd = Math.min(remaining, space);
                    if (!simulate) {
                        inSlot.grow(canAdd);
                        container.setChanged();
                    }
                    remaining -= canAdd;
                    if (remaining <= 0)
                        break;
                }
            }
        }

        // Pass 2: Empty input slots (For machines, ONLY if machine doesn't already have
        // a stack of this item!)
        if (remaining > 0) {
            boolean machineAlreadyHasStack = false;
            if (isMachine) {
                for (int i = 0; i < size; i++) {
                    if (!VolcaniteStoragePipeBlockEntity.isInputSlot(container, i, stack, side))
                        continue;
                    ItemStack inSlot = container.getItem(i);
                    if (!inSlot.isEmpty() && inSlot.is(stack.getItem())) {
                        machineAlreadyHasStack = true;
                        break;
                    }
                }
            }

            if (!machineAlreadyHasStack) {
                for (int i = 0; i < size; i++) {
                    if (!VolcaniteStoragePipeBlockEntity.isInputSlot(container, i, stack, side))
                        continue;

                    ItemStack inSlot = container.getItem(i);
                    if (inSlot.isEmpty()) {
                        int maxStack = Math.min(container.getMaxStackSize(stack), stack.getMaxStackSize());
                        int canAdd = Math.min(remaining, maxStack);
                        if (!simulate) {
                            container.setItem(i, stack.copyWithCount(canAdd));
                            container.setChanged();
                        }
                        remaining -= canAdd;
                        if (remaining <= 0)
                            break;
                    }
                }
            }
        }

        return stack.getCount() - remaining;
    }

    public static List<TargetDestination> findTargetsForBranch(Level level, BlockPos startPipePos, BlockPos stationPos,
            Item filterItem) {
        List<TargetDestination> destinations = new ArrayList<>();
        if (level == null || startPipePos == null)
            return destinations;

        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(startPipePos);
        visited.add(startPipePos);
        visited.add(stationPos); // Never backtrack into the Sorting Station!

        int maxBlocks = 256;
        while (!queue.isEmpty() && visited.size() < maxBlocks) {
            BlockPos curr = queue.poll();
            if (!level.isLoaded(curr)) continue;
            BlockEntity currBe = level.getBlockEntity(curr);

            if (currBe instanceof VolcaniteStoragePipeBlockEntity pipe) {
                for (Direction d : Direction.values()) {
                    BlockPos neighborPos = curr.relative(d);
                    if (neighborPos.equals(stationPos) || !level.isLoaded(neighborPos))
                        continue;

                    VolcaniteStoragePipeBlockEntity.ModuleType mod = pipe.getModule(d);
                    if (mod == VolcaniteStoragePipeBlockEntity.ModuleType.INSERT) {
                        Container c = VolcaniteStoragePipeBlockEntity.getContainerAt(level, neighborPos);
                        if (c != null) {
                            if (filterItem == null || pipe.matchesFilterForFace(d, new ItemStack(filterItem), level.registryAccess())) {
                                if (canContainerAcceptItem(c, d.getOpposite(), filterItem)) {
                                    destinations.add(new TargetDestination(c, d.getOpposite(), neighborPos));
                                }
                            }
                        }
                    }
                }
            }

            for (Direction d : Direction.values()) {
                BlockPos next = curr.relative(d);
                if (!level.isLoaded(next)) continue;
                if (visited.add(next)) {
                    BlockState s = level.getBlockState(next);
                    BlockEntity be = level.getBlockEntity(next);

                    if (s.getBlock() instanceof VolcaniteStoragePipeBlock ||
                            s.getBlock() instanceof VolcaniteStoragePipeBlock ||
                            s.getBlock() instanceof VolcaniteUniversalConduitBlock) {
                        queue.add(next);
                    } else if (!(s.getBlock() instanceof VolcaniteSortingStationBlock) &&
                            !(be instanceof VolcaniteStorageControllerBlockEntity) &&
                            !(be instanceof VolcaniteStorageDriveBlockEntity)) {
                        Container c = VolcaniteStoragePipeBlockEntity.getContainerAt(level, next);
                        if (c != null && canContainerAcceptItem(c, d.getOpposite(), filterItem)) {
                            destinations.add(new TargetDestination(c, d.getOpposite(), next));
                            if (s.getBlock() instanceof net.minecraft.world.level.block.ChestBlock) {
                                for (Direction cd : Direction.Plane.HORIZONTAL) {
                                    BlockPos p2 = next.relative(cd);
                                    if (level.getBlockState(p2).is(s.getBlock()))
                                        visited.add(p2);
                                }
                            }
                        }
                    }
                }
            }
        }
        return destinations;
    }

    public static List<TargetFluidDestination> findFluidTargetsForBranch(Level level, BlockPos startPipePos,
            BlockPos stationPos, FluidVariant filterFluid) {
        List<TargetFluidDestination> destinations = new ArrayList<>();
        if (level == null || startPipePos == null)
            return destinations;

        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(startPipePos);
        visited.add(startPipePos);
        visited.add(stationPos);

        int maxBlocks = 256;
        while (!queue.isEmpty() && visited.size() < maxBlocks) {
            BlockPos curr = queue.poll();

            for (Direction d : Direction.values()) {
                BlockPos next = curr.relative(d);
                if (!level.isLoaded(next)) continue;
                if (visited.add(next)) {
                    BlockState s = level.getBlockState(next);
                    BlockEntity be = level.getBlockEntity(next);

                    if (s.getBlock() instanceof VolcaniteStoragePipeBlock ||
                            s.getBlock() instanceof VolcaniteStoragePipeBlock ||
                            s.getBlock() instanceof VolcaniteUniversalConduitBlock) {
                        queue.add(next);
                    } else if (!(s.getBlock() instanceof VolcaniteSortingStationBlock) &&
                            !(be instanceof VolcaniteStorageControllerBlockEntity) &&
                            !(be instanceof VolcaniteStorageDriveBlockEntity)) {
                        Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, next, d.getOpposite());
                        if (storage != null) {
                            try (Transaction testTx = Transaction.openOuter()) {
                                long accepted = storage.insert(filterFluid, 81L, testTx);
                                if (accepted > 0) {
                                    destinations.add(new TargetFluidDestination(storage, d.getOpposite(), next));
                                }
                            }
                        }
                    }
                }
            }
        }
        return destinations;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide())
            return;
        tickCounter++;
        if (tickCounter % 5 != 0)
            return; // Run every 0.25s for fast, responsive sorting

        boolean anyActive = false;
        for (int m : faceModes) {
            if (m != MODE_DISABLED) {
                anyActive = true;
                break;
            }
        }
        if (!anyActive)
            return;

        VolcaniteStorageControllerBlockEntity controller = findConnectedController();

        // 1. VOID MODE (MAGENTA / INCINERATOR)
        for (Direction dir : Direction.values()) {
            int mode = faceModes[dir.get3DDataValue()];
            if (mode != MODE_VOID)
                continue;

            List<ItemStack> activeFilters = getActiveFilterStacks(dir.get3DDataValue());
            for (ItemStack filterStack : activeFilters) {
                if (isFluidContainer(filterStack)) {
                    FluidVariant fluidToVoid = getFluidFromStack(filterStack);
                    if (fluidToVoid != null && !fluidToVoid.isBlank()) {
                        int voided_mB = (controller != null) ? controller.extractFluid(fluidToVoid, 2000, false)
                                : extractFluidFromNetwork(level, pos, fluidToVoid, 2000, false);
                        if (voided_mB > 0) {
                            spawnVoidParticles(level, pos, dir);
                        }
                    }
                } else {
                    Item itemToVoid = filterStack.getItem();
                    ItemStack voided = (controller != null) ? controller.extractItem(itemToVoid, 64, false)
                            : VolcaniteStoragePipeBlockEntity.extractItemFromNetwork(level, pos, itemToVoid, 64, false);
                    if (!voided.isEmpty()) {
                        spawnVoidParticles(level, pos, dir);
                    }
                }
            }
        }

        // 2. ITEM MODE (GREEN)
        // Group faces by filtered item so duplicate filters can be balanced and
        // distributed equally!
        Map<Item, List<FaceItemRoute>> itemRoutesByItem = new LinkedHashMap<>();
        List<Direction> generalItemFaces = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            int mode = faceModes[dir.get3DDataValue()];
            if (mode != MODE_ITEM)
                continue;

            List<ItemStack> activeFilters = getActiveFilterStacks(dir.get3DDataValue());
            if (activeFilters.isEmpty())
                continue;

            BlockPos targetPos = pos.relative(dir);
            Direction opposite = dir.getOpposite();

            for (ItemStack filterStack : activeFilters) {
                if (filterStack.is(ObsidiumMod.VOLCANITE_STORAGE_UPGRADE)) {
                    if (!generalItemFaces.contains(dir))
                        generalItemFaces.add(dir);
                    continue;
                }

                Item filterItem = filterStack.getItem();

                List<TargetDestination> dests = new ArrayList<>();
                Container directContainer = VolcaniteStoragePipeBlockEntity.getContainerAt(level, targetPos);
                if (directContainer != null) {
                    if (canContainerAcceptItem(directContainer, opposite, filterItem)) {
                        dests.add(new TargetDestination(directContainer, opposite, targetPos));
                    }
                } else {
                    BlockState targetState = level.getBlockState(targetPos);
                    if (targetState.getBlock() instanceof VolcaniteStoragePipeBlock ||
                            targetState.getBlock() instanceof VolcaniteStoragePipeBlock ||
                            targetState.getBlock() instanceof VolcaniteUniversalConduitBlock) {
                        dests = findTargetsForBranch(level, targetPos, pos, filterItem);
                    }
                }

                if (dests.isEmpty())
                    continue;

                int totalCurrent = 0;
                int totalNeeded = 0;
                int maxStack = filterItem.getDefaultInstance().getMaxStackSize();

                for (TargetDestination dest : dests) {
                    boolean isBulk = isBulkStorage(dest.container);
                    int current = getItemCountInContainer(dest.container, dest.side, filterItem);
                    totalCurrent += current;
                    int needed = isBulk ? getFreeSpaceInContainer(dest.container, dest.side, filterItem)
                            : Math.max(0, maxStack - current);
                    totalNeeded += needed;
                }

                itemRoutesByItem.computeIfAbsent(filterItem, k -> new ArrayList<>())
                        .add(new FaceItemRoute(dir, filterItem, dests, totalCurrent, totalNeeded));
            }
        }

        // Process item routes with duplicate filter equal distribution & max 1 stack
        // limit
        for (Map.Entry<Item, List<FaceItemRoute>> entry : itemRoutesByItem.entrySet()) {
            Item item = entry.getKey();
            List<FaceItemRoute> routes = entry.getValue();

            // Filter out faces whose destination machines/slots are already full!
            List<FaceItemRoute> hungryRoutes = new ArrayList<>();
            for (FaceItemRoute r : routes) {
                if (r.neededCount > 0) {
                    hungryRoutes.add(r);
                }
            }
            if (hungryRoutes.isEmpty()) {
                // All connected machines for this item already have a full stack (or full
                // chest)!
                // Do NOT extract anything!
                continue;
            }

            // Distribute equally:
            // The route with the lowest current item count gets items first!
            // When counts are equal, alternate with roundRobinCounter so items are
            // distributed evenly!
            final int rr = this.roundRobinCounter;
            hungryRoutes.sort((a, b) -> {
                int c = Integer.compare(a.currentCount, b.currentCount);
                if (c != 0)
                    return c;
                return Integer.compare((a.dir.get3DDataValue() + rr) % 6, (b.dir.get3DDataValue() + rr) % 6);
            });

            FaceItemRoute selected = hungryRoutes.get(0);

            // Extract ONLY what is needed to make a stack (up to 16 per tick)
            int toExtract = Math.min(16, selected.neededCount);
            if (toExtract <= 0)
                continue;

            ItemStack extracted = (controller != null) ? controller.extractItem(item, toExtract, false)
                    : VolcaniteStoragePipeBlockEntity.extractItemFromNetwork(level, pos, item, toExtract, false);

            if (!extracted.isEmpty()) {
                ItemStack remainingToInsert = extracted.copy();
                for (TargetDestination dest : selected.destinations) {
                    boolean isMach = !isBulkStorage(dest.container);
                    int accepted = insertFilteredItemIntoContainer(dest.container, dest.side, remainingToInsert, isMach,
                            false);
                    if (accepted > 0) {
                        remainingToInsert.shrink(accepted);
                        if (remainingToInsert.isEmpty())
                            break;
                    }
                }

                if (!remainingToInsert.isEmpty()) {
                    if (controller != null) {
                        controller.insertItem(remainingToInsert, false);
                    } else {
                        VolcaniteStoragePipeBlockEntity.routeItemIntoNetwork(level, pos, null, remainingToInsert,
                                Collections.singleton(pos), false);
                    }
                }
            }
        }

        // Process general (unfiltered) item upgrade faces
        for (Direction dir : generalItemFaces) {
            BlockPos targetPos = pos.relative(dir);
            Direction opposite = dir.getOpposite();
            BlockState targetState = level.getBlockState(targetPos);
            Container targetContainer = VolcaniteStoragePipeBlockEntity.getContainerAt(level, targetPos);
            boolean isPipe = targetState.getBlock() instanceof VolcaniteStoragePipeBlock ||
                    targetState.getBlock() instanceof VolcaniteStoragePipeBlock ||
                    targetState.getBlock() instanceof VolcaniteUniversalConduitBlock;

            if (targetContainer != null || isPipe) {
                Map<Item, Integer> networkItems = (controller != null) ? controller.getUnifiedInventory()
                        : getNetworkItems(level, pos);

                for (Map.Entry<Item, Integer> entry : networkItems.entrySet()) {
                    Item available = entry.getKey();
                    if (entry.getValue() > 0) {
                        int toMove = 16;
                        int accepted = 0;
                        if (targetContainer != null) {
                            accepted = insertFilteredItemIntoContainer(targetContainer, opposite,
                                    new ItemStack(available, toMove), !isBulkStorage(targetContainer), true);
                        } else {
                            ItemStack leftover = VolcaniteStoragePipeBlockEntity.routeItemIntoNetwork(level, targetPos,
                                    null, new ItemStack(available, toMove), Collections.singleton(pos), true);
                            accepted = toMove - leftover.getCount();
                        }

                        if (accepted > 0) {
                            ItemStack extracted = (controller != null)
                                    ? controller.extractItem(available, accepted, false)
                                    : VolcaniteStoragePipeBlockEntity.extractItemFromNetwork(level, pos, available,
                                            accepted, false);

                            if (!extracted.isEmpty()) {
                                if (targetContainer != null) {
                                    insertFilteredItemIntoContainer(targetContainer, opposite, extracted,
                                            !isBulkStorage(targetContainer), false);
                                } else {
                                    VolcaniteStoragePipeBlockEntity.routeItemIntoNetwork(level, targetPos, null,
                                            extracted, Collections.singleton(pos), false);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }

        // 3. LIQUID MODE (BLUE)
        // Group faces by filtered fluid so duplicate fluid filters are distributed
        // equally!
        Map<FluidVariant, List<FaceFluidRoute>> fluidRoutesByFluid = new LinkedHashMap<>();
        List<Direction> generalLiquidFaces = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            int mode = faceModes[dir.get3DDataValue()];
            if (mode != MODE_LIQUID)
                continue;

            List<ItemStack> activeFilters = getActiveFilterStacks(dir.get3DDataValue());
            if (activeFilters.isEmpty())
                continue;

            BlockPos targetPos = pos.relative(dir);
            Direction opposite = dir.getOpposite();

            for (ItemStack filterStack : activeFilters) {
                FluidVariant fluidToExtract = getFluidFromStack(filterStack);
                if (fluidToExtract == null || fluidToExtract.isBlank()) {
                    if (!generalLiquidFaces.contains(dir))
                        generalLiquidFaces.add(dir);
                    continue;
                }

                List<TargetFluidDestination> dests = new ArrayList<>();
                Storage<FluidVariant> targetStorage = FluidStorage.SIDED.find(level, targetPos, opposite);
                if (targetStorage != null) {
                    try (Transaction testTx = Transaction.openOuter()) {
                        long accepted = targetStorage.insert(fluidToExtract, 81L, testTx);
                        if (accepted > 0) {
                            dests.add(new TargetFluidDestination(targetStorage, opposite, targetPos));
                        }
                    }
                } else {
                    BlockState targetState = level.getBlockState(targetPos);
                    if (targetState.getBlock() instanceof VolcaniteStoragePipeBlock ||
                            targetState.getBlock() instanceof VolcaniteStoragePipeBlock ||
                            targetState.getBlock() instanceof VolcaniteUniversalConduitBlock) {
                        dests = findFluidTargetsForBranch(level, targetPos, pos, fluidToExtract);
                    }
                }

                if (dests.isEmpty())
                    continue;

                int totalCurrent_mB = 0;
                int totalNeeded_mB = 0;
                for (TargetFluidDestination dest : dests) {
                    try (Transaction testTx = Transaction.openOuter()) {
                        long canAcceptDroplets = dest.storage.insert(fluidToExtract, (long) 1000 * 81L, testTx);
                        int canAccept_mB = (int) (canAcceptDroplets / 81L);
                        totalNeeded_mB += canAccept_mB;
                    }
                }

                fluidRoutesByFluid.computeIfAbsent(fluidToExtract, k -> new ArrayList<>())
                        .add(new FaceFluidRoute(dir, fluidToExtract, dests, totalCurrent_mB, totalNeeded_mB));
            }
        }

        // Process fluid routes with equal distribution
        for (Map.Entry<FluidVariant, List<FaceFluidRoute>> entry : fluidRoutesByFluid.entrySet()) {
            FluidVariant fv = entry.getKey();
            List<FaceFluidRoute> routes = entry.getValue();

            List<FaceFluidRoute> hungryRoutes = new ArrayList<>();
            for (FaceFluidRoute r : routes) {
                if (r.needed_mB > 0) {
                    hungryRoutes.add(r);
                }
            }
            if (hungryRoutes.isEmpty())
                continue;

            final int rr = this.roundRobinCounter;
            hungryRoutes.sort(
                    (a, b) -> Integer.compare((a.dir.get3DDataValue() + rr) % 6, (b.dir.get3DDataValue() + rr) % 6));

            FaceFluidRoute selected = hungryRoutes.get(0);
            int toExtract_mB = Math.min(1000, selected.needed_mB);
            if (toExtract_mB <= 0)
                continue;

            int extracted = (controller != null) ? controller.extractFluid(fv, toExtract_mB, false)
                    : extractFluidFromNetwork(level, pos, fv, toExtract_mB, false);

            if (extracted > 0) {
                int remaining_mB = extracted;
                for (TargetFluidDestination dest : selected.destinations) {
                    try (Transaction insTx = Transaction.openOuter()) {
                        long insertedDroplets = dest.storage.insert(fv, (long) remaining_mB * 81L, insTx);
                        if (insertedDroplets > 0) {
                            insTx.commit();
                            remaining_mB -= (int) (insertedDroplets / 81L);
                            if (remaining_mB <= 0)
                                break;
                        }
                    }
                }
                if (remaining_mB > 0) {
                    if (controller != null) {
                        controller.insertFluid(fv, remaining_mB, false);
                    } else {
                        VolcaniteStoragePipeBlockEntity.routeFluidIntoNetwork(level, pos, fv, remaining_mB, false);
                    }
                }
            }
        }

        // Process general (unfiltered) liquid upgrade faces
        for (Direction dir : generalLiquidFaces) {
            BlockPos targetPos = pos.relative(dir);
            Direction opposite = dir.getOpposite();
            Storage<FluidVariant> targetStorage = FluidStorage.SIDED.find(level, targetPos, opposite);
            BlockState targetState = level.getBlockState(targetPos);
            boolean isPipe = targetState.getBlock() instanceof VolcaniteStoragePipeBlock ||
                    targetState.getBlock() instanceof VolcaniteStoragePipeBlock ||
                    targetState.getBlock() instanceof VolcaniteUniversalConduitBlock;

            if (targetStorage != null || isPipe) {
                Map<FluidVariant, Integer> networkFluids = (controller != null) ? controller.getUnifiedFluids()
                        : getNetworkFluids(level, pos);

                for (Map.Entry<FluidVariant, Integer> entry : networkFluids.entrySet()) {
                    FluidVariant fv = entry.getKey();
                    int available_mB = entry.getValue();
                    if (available_mB > 0) {
                        int toPush_mB = Math.min(available_mB, 1000);
                        int canAccept_mB = 0;
                        if (targetStorage != null) {
                            try (Transaction testTx = Transaction.openOuter()) {
                                long acceptedDroplets = targetStorage.insert(fv, (long) toPush_mB * 81L, testTx);
                                canAccept_mB = (int) (acceptedDroplets / 81L);
                            }
                        } else {
                            canAccept_mB = VolcaniteStoragePipeBlockEntity.routeFluidIntoNetwork(level, targetPos, fv,
                                    toPush_mB, true);
                        }

                        if (canAccept_mB > 0) {
                            int extracted = (controller != null) ? controller.extractFluid(fv, canAccept_mB, false)
                                    : extractFluidFromNetwork(level, pos, fv, canAccept_mB, false);

                            if (extracted > 0) {
                                if (targetStorage != null) {
                                    try (Transaction insTx = Transaction.openOuter()) {
                                        targetStorage.insert(fv, (long) extracted * 81L, insTx);
                                        insTx.commit();
                                    }
                                } else {
                                    VolcaniteStoragePipeBlockEntity.routeFluidIntoNetwork(level, targetPos, fv,
                                            extracted, false);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }

        roundRobinCounter++;
    }

    public static boolean isFluidContainer(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        if (stack.is(Items.LAVA_BUCKET) || stack.is(Items.WATER_BUCKET))
            return true;
        if (stack.is(Items.LAVA_BUCKET) || stack.is(Items.WATER_BUCKET) ||
                stack.is(ObsidiumMod.PYROCLASTIC_DISTILLATE_BUCKET) || stack.is(ObsidiumMod.PYROCLASTIC_FLASK) ||
                stack.is(ObsidiumMod.STEAM_BUCKET))
            return true;
        if (stack.getItem() instanceof VolcaniteBottleItem)
            return true;
        if (stack.getItem() instanceof VolcaniteLiquidStorageDiskItem)
            return true;
        if (stack.getItem() instanceof VolcaniteLiquidUpgradeItem)
            return true;
        return false;
    }

    public static FluidVariant getFluidFromStack(ItemStack stack) {
        if (stack.isEmpty())
            return FluidVariant.blank();
        if (stack.is(Items.LAVA_BUCKET)) {
            return FluidVariant.of(Fluids.LAVA);
        }
        if (stack.is(Items.WATER_BUCKET)) {
            return FluidVariant.of(Fluids.WATER);
        }
        if (stack.is(ObsidiumMod.PYROCLASTIC_DISTILLATE_BUCKET) || stack.is(ObsidiumMod.PYROCLASTIC_FLASK)) {
            return FluidVariant.of(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID);
        }
        if (stack.is(ObsidiumMod.STEAM_BUCKET)) {
            return FluidVariant.of(ObsidiumMod.STEAM_FLUID);
        }
        if (stack.is(ObsidiumMod.BIO_OIL_BUCKET)) {
            return FluidVariant.of(ObsidiumMod.BIO_OIL_FLUID);
        }
        if (stack.is(ObsidiumMod.BIO_ETHANOL_BUCKET)) {
            return FluidVariant.of(ObsidiumMod.BIO_ETHANOL_FLUID);
        }
        if (stack.is(ObsidiumMod.BIODIESEL_BUCKET)) {
            return FluidVariant.of(ObsidiumMod.BIODIESEL_FLUID);
        }
        if (stack.is(ObsidiumMod.PYRO_DIESEL_BUCKET)) {
            return FluidVariant.of(ObsidiumMod.PYRO_DIESEL_FLUID);
        }
        if (stack.getItem() instanceof VolcaniteBottleItem) {
            return VolcaniteBottleItem.getFluid(stack);
        }
        if (stack.getItem() instanceof VolcaniteLiquidStorageDiskItem) {
            return VolcaniteLiquidStorageDiskItem.getStoredFluid(stack);
        }
        return FluidVariant.blank();
    }

    private VolcaniteStorageControllerBlockEntity cachedController = null;
    private long controllerCacheTick = -100;
    private static final int CONTROLLER_CACHE_TTL = 40;

    public void invalidateControllerCache() {
        this.cachedController = null;
        this.controllerCacheTick = -100;
    }

    public VolcaniteStorageControllerBlockEntity findConnectedController() {
        if (level == null)
            return null;
        long currentTick = level.getGameTime();
        if (currentTick - controllerCacheTick < CONTROLLER_CACHE_TTL) {
            if (cachedController != null && !cachedController.isRemoved() && cachedController.getLevel() == level) {
                return cachedController;
            } else if (cachedController == null) {
                return null;
            }
        }
        controllerCacheTick = currentTick;
        cachedController = scanForController();
        return cachedController;
    }

    private VolcaniteStorageControllerBlockEntity scanForController() {
        if (level == null || !level.isLoaded(worldPosition))
            return null;
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(worldPosition);
        visited.add(worldPosition);

        int steps = 0;
        int maxSteps = 4096;
        while (!queue.isEmpty() && steps++ < maxSteps) {
            BlockPos curr = queue.poll();
            if (!level.isLoaded(curr)) continue;
            for (Direction d : Direction.values()) {
                if (!PipeConnectionHelper.canTraverseNetwork(level, curr, d)) continue;
                BlockPos next = curr.relative(d);
                if (!level.isLoaded(next)) continue;
                BlockState s = level.getBlockState(next);
                if (s.isAir()) continue;
                if (visited.add(next)) {
                    BlockEntity be = level.getBlockEntity(next);
                    if (be instanceof VolcaniteStorageControllerBlockEntity controller) {
                        return controller;
                    }
                    if (be instanceof VolcaniteNetworkNodeReceiverBlockEntity receiver && receiver.isLinked()) {
                        VolcaniteStorageControllerBlockEntity c = receiver.getConnectedController();
                        if (c != null)
                            return c;
                        BlockPos ePos = receiver.getLinkedEmitterPos();
                        if (ePos != null && level.isLoaded(ePos) && visited.add(ePos))
                            queue.add(ePos);
                    }
                    if (be instanceof VolcaniteNetworkNodeEmitterBlockEntity emitter && emitter.isLinked()) {
                        VolcaniteStorageControllerBlockEntity c = emitter.getConnectedController();
                        if (c != null)
                            return c;
                        for (BlockPos rPos : emitter.getLinkedReceivers()) {
                            if (level.isLoaded(rPos) && visited.add(rPos))
                                queue.add(rPos);
                        }
                    }
                    if (PipeConnectionHelper.isLogisticsPipe(s.getBlock()) ||
                        PipeConnectionHelper.isStorageNetworkBlock(s.getBlock())) {
                        queue.add(next);
                    }
                }
            }
        }
        return null;
    }

    public static ItemStack extractItemFromNetwork(Level level, BlockPos startPos, Item item, int amount,
            boolean simulate) {
        if (level == null || item == null || amount <= 0)
            return ItemStack.EMPTY;
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(startPos);
        visited.add(startPos);

        List<VolcaniteStorageDriveBlockEntity> connectedDrives = new ArrayList<>();
        List<VolcaniteStorageUnitBlockEntity> connectedUnits = new ArrayList<>();

        int max = 256;
        while (!queue.isEmpty() && visited.size() < max) {
            BlockPos curr = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos next = curr.relative(d);
                if (!level.isLoaded(next)) continue;
                if (visited.add(next)) {
                    BlockEntity be = level.getBlockEntity(next);
                    BlockState s = level.getBlockState(next);
                    if (be instanceof VolcaniteStorageDriveBlockEntity drive) {
                        connectedDrives.add(drive);
                        queue.add(next);
                    } else if (be instanceof VolcaniteStorageUnitBlockEntity unit) {
                        connectedUnits.add(unit);
                        queue.add(next);
                    } else if (s.getBlock() instanceof VolcaniteStoragePipeBlock ||
                            s.getBlock() instanceof VolcaniteStorageInterfaceBlock ||
                            s.getBlock() instanceof VolcaniteStorageControllerBlock ||
                            s.getBlock() instanceof VolcaniteSortingStationBlock) {
                        queue.add(next);
                    }
                }
            }
        }

        int needed = amount;
        ItemStack result = ItemStack.EMPTY;

        for (VolcaniteStorageDriveBlockEntity drive : connectedDrives) {
            ItemStack ext = drive.extractItem(item, needed, simulate);
            if (!ext.isEmpty()) {
                if (result.isEmpty())
                    result = ext.copy();
                else
                    result.grow(ext.getCount());
                needed -= ext.getCount();
                if (needed <= 0)
                    return result;
            }
        }

        for (VolcaniteStorageUnitBlockEntity unit : connectedUnits) {
            ItemStack ext = unit.extractItem(item, needed, simulate);
            if (!ext.isEmpty()) {
                if (result.isEmpty())
                    result = ext.copy();
                else
                    result.grow(ext.getCount());
                needed -= ext.getCount();
                if (needed <= 0)
                    return result;
            }
        }

        return result;
    }

    public static int extractFluidFromNetwork(Level level, BlockPos startPos, FluidVariant fluid, int amount_mB,
            boolean simulate) {
        if (level == null || fluid.isBlank() || amount_mB <= 0)
            return 0;
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(startPos);
        visited.add(startPos);

        List<VolcaniteStorageDriveBlockEntity> connectedDrives = new ArrayList<>();
        List<VolcaniteLiquidStorageTankBlockEntity> connectedTanks = new ArrayList<>();
        List<LiquidTankBlockEntity> connectedLiquidTanks = new ArrayList<>();

        int max = 256;
        while (!queue.isEmpty() && visited.size() < max) {
            BlockPos curr = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos next = curr.relative(d);
                if (!level.isLoaded(next)) continue;
                if (visited.add(next)) {
                    BlockEntity be = level.getBlockEntity(next);
                    BlockState s = level.getBlockState(next);
                    if (be instanceof VolcaniteStorageDriveBlockEntity drive) {
                        connectedDrives.add(drive);
                        queue.add(next);
                    } else if (be instanceof VolcaniteLiquidStorageTankBlockEntity tank) {
                        connectedTanks.add(tank);
                        queue.add(next);
                    } else if (be instanceof LiquidTankBlockEntity liquidTank) {
                        connectedLiquidTanks.add(liquidTank);
                        queue.add(next);
                    } else if (s.getBlock() instanceof VolcaniteStoragePipeBlock ||
                            s.getBlock() instanceof VolcaniteStorageInterfaceBlock ||
                            s.getBlock() instanceof VolcaniteStorageControllerBlock ||
                            s.getBlock() instanceof VolcaniteSortingStationBlock) {
                        queue.add(next);
                    }
                }
            }
        }

        int remaining = amount_mB;
        int totalExtracted = 0;

        for (VolcaniteStorageDriveBlockEntity drive : connectedDrives) {
            int ext = drive.extractFluid(fluid, remaining, simulate);
            remaining -= ext;
            totalExtracted += ext;
            if (remaining <= 0)
                return totalExtracted;
        }

        for (LiquidTankBlockEntity liquidTank : connectedLiquidTanks) {
            int ext = liquidTank.extractFluid(fluid, remaining, simulate);
            remaining -= ext;
            totalExtracted += ext;
            if (remaining <= 0)
                return totalExtracted;
        }

        for (VolcaniteLiquidStorageTankBlockEntity tank : connectedTanks) {
            int ext = tank.extractFluid(fluid, remaining, simulate);
            remaining -= ext;
            totalExtracted += ext;
            if (remaining <= 0)
                return totalExtracted;
        }

        return totalExtracted;
    }

    public static Map<Item, Integer> getNetworkItems(Level level, BlockPos startPos) {
        Map<Item, Integer> map = new HashMap<>();
        if (level == null)
            return map;
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(startPos);
        visited.add(startPos);

        int max = 256;
        while (!queue.isEmpty() && visited.size() < max) {
            BlockPos curr = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos next = curr.relative(d);
                if (!level.isLoaded(next)) continue;
                if (visited.add(next)) {
                    BlockEntity be = level.getBlockEntity(next);
                    BlockState s = level.getBlockState(next);
                    if (be instanceof VolcaniteStorageDriveBlockEntity drive) {
                        drive.collectStoredItems(map);
                        queue.add(next);
                    } else if (be instanceof VolcaniteStorageUnitBlockEntity unit) {
                        for (int i = 0; i < unit.getContainerSize(); i++) {
                            ItemStack st = unit.getItem(i);
                            if (!st.isEmpty()) {
                                map.put(st.getItem(), map.getOrDefault(st.getItem(), 0) + st.getCount());
                            }
                        }
                        queue.add(next);
                    } else if (s.getBlock() instanceof VolcaniteStoragePipeBlock ||
                            s.getBlock() instanceof VolcaniteStorageInterfaceBlock ||
                            s.getBlock() instanceof VolcaniteSortingStationBlock) {
                        queue.add(next);
                    }
                }
            }
        }
        return map;
    }

    public static Map<FluidVariant, Integer> getNetworkFluids(Level level, BlockPos startPos) {
        Map<FluidVariant, Integer> map = new HashMap<>();
        if (level == null)
            return map;
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(startPos);
        visited.add(startPos);

        int max = 256;
        while (!queue.isEmpty() && visited.size() < max) {
            BlockPos curr = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos next = curr.relative(d);
                if (!level.isLoaded(next)) continue;
                if (visited.add(next)) {
                    BlockEntity be = level.getBlockEntity(next);
                    BlockState s = level.getBlockState(next);
                    if (be instanceof VolcaniteStorageDriveBlockEntity drive) {
                        drive.collectStoredFluids(map);
                        queue.add(next);
                    } else if (be instanceof VolcaniteLiquidStorageTankBlockEntity tank) {
                        if (tank.getTotalFluidAmount() > 0 && tank.getActiveFluid() != Fluids.EMPTY) {
                            FluidVariant fv = FluidVariant.of(tank.getActiveFluid());
                            map.put(fv, map.getOrDefault(fv, 0) + tank.getTotalFluidAmount());
                        }
                        queue.add(next);
                    } else if (be instanceof LiquidTankBlockEntity liquidTank) {
                        if (liquidTank.getFluidAmount() > 0 && !liquidTank.getStoredFluid().isBlank()) {
                            FluidVariant fv = liquidTank.getStoredFluid();
                            map.put(fv, map.getOrDefault(fv, 0) + liquidTank.getFluidAmount());
                        }
                        queue.add(next);
                    } else if (s.getBlock() instanceof VolcaniteStoragePipeBlock ||
                            s.getBlock() instanceof VolcaniteStorageInterfaceBlock ||
                            s.getBlock() instanceof VolcaniteSortingStationBlock) {
                        queue.add(next);
                    }
                }
            }
        }
        return map;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("InventoryVersion", 2);
        byte[] modes = new byte[6];
        for (int i = 0; i < 6; i++)
            modes[i] = (byte) faceModes[i];
        tag.putByteArray("FaceModes", modes);

        NonNullList<ItemStack> list = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
        for (int i = 0; i < INVENTORY_SIZE; i++)
            list.set(i, inventory.getItem(i));
        ContainerHelper.saveAllItems(tag, list, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("FaceModes")) {
            byte[] modes = tag.getByteArray("FaceModes");
            for (int i = 0; i < Math.min(6, modes.length); i++) {
                faceModes[i] = modes[i];
            }
        }
        int version = tag.getInt("InventoryVersion");
        if (version >= 2) {
            NonNullList<ItemStack> list = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, list, provider);
            for (int i = 0; i < INVENTORY_SIZE; i++)
                inventory.setItem(i, list.get(i));
        } else {
            // Backward compatibility for old 6-slot saves: migrate face filters to Filter 1
            // (slot f * 4 + 1)
            NonNullList<ItemStack> list = NonNullList.withSize(6, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, list, provider);
            for (int i = 0; i < 6; i++) {
                ItemStack oldFilter = list.get(i);
                if (!oldFilter.isEmpty()) {
                    inventory.setItem(i * 4 + 1, oldFilter);
                }
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, provider);
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.obsidium.volcanite_sorting_station");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new VolcaniteSortingStationMenu(syncId, playerInventory, this, this.data);
    }
}
