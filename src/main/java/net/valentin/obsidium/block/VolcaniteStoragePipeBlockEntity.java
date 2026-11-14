package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;

import net.minecraft.world.WorldlyContainer;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class VolcaniteStoragePipeBlockEntity extends BlockEntity implements MenuProvider, PipeFacadeHolder {
    public enum ModuleType {
        NONE(0),
        EXTRACT(1),
        INSERT(2);

        private final int id;
        ModuleType(int id) { this.id = id; }
        public int getId() { return id; }

        public static ModuleType byId(int id) {
            for (ModuleType type : values()) {
                if (type.id == id) return type;
            }
            return NONE;
        }
    }

    public static final int TYPE_ITEM = 0;
    public static final int TYPE_FLUID = 1;
    public static final int TYPE_ENERGY = 2;
    public static final int TYPE_OMNI = 3;

    private final ModuleType[] modules = new ModuleType[6];
    private final int[] transferTypes = new int[6];
    private final int[] priorities = new int[6];
    private final boolean[] roundRobin = new boolean[6];
    private final int[] redstoneModes = new int[6];
    protected final int[] roundRobinCursor = new int[6];
    private final NonNullList<ItemStack> filterItems = NonNullList.withSize(18, ItemStack.EMPTY);
    private final boolean[] filterBlacklist = new boolean[6];
    private final int[] energyUpgradeTiers = new int[6];
    private final NonNullList<ItemStack> energyUpgradeItems = NonNullList.withSize(6, ItemStack.EMPTY);
    private final boolean[] disconnectedSides = new boolean[6];
    private final boolean[] isValveModule = new boolean[6];
    protected final int[] lastExtractedSlot = new int[6];
    protected final int[] lastFilterIndex = new int[6];
    private net.minecraft.world.level.block.Block facadeBlock = null;
    protected int tickCounter = 0;
    private Direction selectedFaceForMenu = Direction.NORTH;

    @Override
    @Nullable
    public net.minecraft.world.level.block.Block getFacadeBlock() {
        return facadeBlock;
    }

    @Override
    public void setFacadeBlock(@Nullable net.minecraft.world.level.block.Block block) {
        this.facadeBlock = block;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean isValveModule(Direction dir) {
        if (dir == null) return false;
        return isValveModule[dir.get3DDataValue()];
    }

    public void setValveModule(Direction dir, boolean valve) {
        if (dir != null) {
            isValveModule[dir.get3DDataValue()] = valve;
            setChanged();
        }
    }

    public VolcaniteStoragePipeBlockEntity(BlockPos pos, BlockState state) {
        this(ObsidiumMod.VOLCANITE_STORAGE_PIPE_BE, pos, state);
    }

    public VolcaniteStoragePipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        Arrays.fill(modules, ModuleType.NONE);
        Arrays.fill(transferTypes, TYPE_ITEM);
        Arrays.fill(energyUpgradeTiers, 0);
        Arrays.fill(priorities, 0);
        Arrays.fill(roundRobin, true);
        Arrays.fill(redstoneModes, 0);
    }

    public int getEnergyUpgradeTier(Direction dir) {
        if (dir == null) return 0;
        return energyUpgradeTiers[dir.get3DDataValue()];
    }

    public int getHighestEnergyUpgradeTier() {
        int max = 0;
        for (int t : energyUpgradeTiers) {
            if (t > max) max = t;
        }
        return max;
    }

    public ItemStack getEnergyUpgradeItem(Direction dir) {
        if (dir == null) return ItemStack.EMPTY;
        return energyUpgradeItems.get(dir.get3DDataValue());
    }

    public void setEnergyUpgrade(Direction dir, int tier, ItemStack stack) {
        if (dir == null) return;
        int idx = dir.get3DDataValue();
        energyUpgradeTiers[idx] = tier;
        energyUpgradeItems.set(idx, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    public long getMaxEnergyTransfer(Direction dir) {
        int tier = Math.max(getEnergyUpgradeTier(dir), (this instanceof VolcaniteUniversalConduitBlockEntity) ? getHighestEnergyUpgradeTier() : 0);
        if (this instanceof VolcaniteUniversalConduitBlockEntity) {
            return switch (tier) {
                case 1 -> 20_000L;
                case 2 -> 60_000L;
                case 3 -> 200_000L;
                default -> 5_000L;
            };
        }
        return switch (tier) {
            case 1 -> 2500L;
            case 2 -> 15000L;
            case 3 -> 60000L;
            default -> 500L;
        };
    }

    public long getMaxFluidTransfer(Direction dir) {
        int tier = Math.max(getEnergyUpgradeTier(dir), (this instanceof VolcaniteUniversalConduitBlockEntity) ? getHighestEnergyUpgradeTier() : 0);
        return switch (tier) {
            case 1 -> 2000L;
            case 2 -> 8000L;
            case 3 -> 32000L;
            default -> (this instanceof VolcaniteUniversalConduitBlockEntity) ? 1000L : 250L;
        };
    }

    public int getMaxItemTransfer(Direction dir) {
        ItemStack upgrade = getEnergyUpgradeItem(dir);
        if (!upgrade.isEmpty() && upgrade.getItem() instanceof net.valentin.obsidium.item.VolcaniteStackUpgradeItem) {
            return 64;
        }
        int tier = Math.max(getEnergyUpgradeTier(dir), (this instanceof VolcaniteUniversalConduitBlockEntity) ? getHighestEnergyUpgradeTier() : 0);
        return switch (tier) {
            case 1 -> 16;
            case 2 -> 32;
            case 3 -> 64;
            default -> (this instanceof VolcaniteUniversalConduitBlockEntity) ? 4 : 1;
        };
    }

    public boolean isSideDisconnected(Direction dir) {
        if (dir == null) return false;
        return disconnectedSides[dir.get3DDataValue()];
    }

    public void setSideDisconnected(Direction dir, boolean disconnected) {
        if (dir == null) return;
        int idx = dir.get3DDataValue();
        disconnectedSides[idx] = disconnected;

        if (disconnected && level != null && !level.isClientSide()) {
            if (modules[idx] != ModuleType.NONE) {
                if (!(this instanceof VolcaniteUniversalConduitBlockEntity)) {
                    Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, new ItemStack(ObsidiumMod.VOLCANITE_LOGISTICS_MODULE));
                }
                clearFilters(dir);
                modules[idx] = ModuleType.NONE;
            }

            if (energyUpgradeTiers[idx] > 0) {
                ItemStack upg = energyUpgradeItems.get(idx);
                if (!upg.isEmpty()) {
                    Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, upg.copy());
                }
                energyUpgradeTiers[idx] = 0;
                energyUpgradeItems.set(idx, ItemStack.EMPTY);
            }
        }

        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            if (state.getBlock() instanceof VolcaniteStoragePipeBlock pipeBlock) {
                BlockState newState = pipeBlock.makeConnections(level, worldPosition)
                    .setValue(VolcaniteStoragePipeBlock.WATERLOGGED, state.getValue(VolcaniteStoragePipeBlock.WATERLOGGED));
                level.setBlock(worldPosition, newState, 3);

                BlockPos neighborPos = worldPosition.relative(dir);
                level.neighborChanged(neighborPos, pipeBlock, worldPosition);
            }
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    public boolean toggleSideDisconnected(Direction dir) {
        if (dir == null) return false;
        boolean newVal = !isSideDisconnected(dir);
        setSideDisconnected(dir, newVal);
        return newVal;
    }

    public static Component getConnectedMachineName(Level level, BlockPos pipePos, Direction dir) {
        if (level == null || pipePos == null || dir == null) return Component.literal("None");
        BlockPos targetPos = pipePos.relative(dir);
        BlockState state = level.getBlockState(targetPos);
        if (state.isAir()) return Component.literal("None");
        return state.getBlock().getName();
    }

    public static boolean isConnectedToMachine(Level level, BlockPos pipePos, Direction dir) {
        if (level == null || pipePos == null || dir == null) return false;
        BlockPos targetPos = pipePos.relative(dir);
        return team.reborn.energy.api.EnergyStorage.SIDED.find(level, targetPos, dir.getOpposite()) != null;
    }

    public int getTransferType(Direction dir) {
        if (dir == null) return TYPE_ITEM;
        return transferTypes[dir.get3DDataValue()];
    }

    public void setTransferType(Direction dir, int type) {
        if (dir == null) return;
        transferTypes[dir.get3DDataValue()] = type;
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    public void setAllFacesTransferType(int type) {
        Arrays.fill(transferTypes, type);
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    public int getPriority(Direction dir) {
        if (dir == null) return 0;
        return priorities[dir.get3DDataValue()];
    }

    public void setPriority(Direction dir, int priority) {
        if (dir == null) return;
        priorities[dir.get3DDataValue()] = Math.max(-5, Math.min(5, priority));
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int adjustPriority(Direction dir, int delta) {
        if (dir == null) return 0;
        int next = Math.max(-5, Math.min(5, getPriority(dir) + delta));
        setPriority(dir, next);
        return next;
    }

    public boolean isRoundRobin(Direction dir) {
        if (dir == null) return true;
        return roundRobin[dir.get3DDataValue()];
    }

    public void setRoundRobin(Direction dir, boolean rr) {
        if (dir == null) return;
        roundRobin[dir.get3DDataValue()] = rr;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean toggleRoundRobin(Direction dir) {
        if (dir == null) return true;
        boolean next = !isRoundRobin(dir);
        setRoundRobin(dir, next);
        return next;
    }

    public int getRedstoneMode(Direction dir) {
        if (dir == null) return 0;
        return redstoneModes[dir.get3DDataValue()];
    }

    public void setRedstoneMode(Direction dir, int mode) {
        if (dir == null) return;
        redstoneModes[dir.get3DDataValue()] = ((mode % 3) + 3) % 3;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int cycleRedstoneMode(Direction dir) {
        if (dir == null) return 0;
        int next = (getRedstoneMode(dir) + 1) % 3;
        setRedstoneMode(dir, next);
        return next;
    }

    public boolean isFaceActiveByRedstone(Direction dir) {
        int mode = getRedstoneMode(dir);
        if (mode == 0) return true;
        if (level == null) return true;
        boolean hasSignal = level.hasNeighborSignal(worldPosition);
        return mode == 1 ? hasSignal : !hasSignal;
    }

    public boolean supportsEnergyInGui() {
        return this instanceof VolcaniteUniversalConduitBlockEntity;
    }

    public boolean isOpticFiber() {
        return this.getClass() == VolcaniteStoragePipeBlockEntity.class;
    }

    public boolean canSwitchTransferType() {
        return !(this instanceof HermesItemPipeBlockEntity);
    }

    public boolean hasAnyModule() {
        for (ModuleType m : modules) {
            if (m != ModuleType.NONE) return true;
        }
        return false;
    }

    public boolean hasAnyEnergyUpgrade() {
        for (int tier : energyUpgradeTiers) {
            if (tier > 0) return true;
        }
        return false;
    }

    public int getPrimaryTransferType() {
        for (int i = 0; i < 6; i++) {
            if (modules[i] != ModuleType.NONE) {
                return transferTypes[i];
            }
        }
        if (this instanceof VolcaniteUniversalConduitBlockEntity) {
            for (int i = 0; i < 6; i++) {
                if (!disconnectedSides[i] && transferTypes[i] != TYPE_OMNI) {
                    return transferTypes[i];
                }
            }
            return transferTypes[0];
        }
        return TYPE_ITEM;
    }

    public ModuleType getModule(Direction dir) {
        if (dir == null) return ModuleType.NONE;
        return modules[dir.get3DDataValue()];
    }

    public void setModule(Direction dir, ModuleType type) {
        modules[dir.get3DDataValue()] = type;
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    public void setSelectedFaceForMenu(Direction dir) {
        this.selectedFaceForMenu = dir;
    }

    public Direction getSelectedFaceForMenu() {
        return selectedFaceForMenu;
    }

    public ItemStack getFilterItem(Direction dir, int slot) {
        if (dir == null || slot < 0 || slot >= 3) return ItemStack.EMPTY;
        return filterItems.get(dir.get3DDataValue() * 3 + slot);
    }

    public void setFilterItem(Direction dir, int slot, ItemStack stack) {
        if (dir == null || slot < 0 || slot >= 3) return;
        filterItems.set(dir.get3DDataValue() * 3 + slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void clearFilters(Direction dir) {
        if (dir == null) return;
        int base = dir.get3DDataValue() * 3;
        for (int i = 0; i < 3; i++) {
            filterItems.set(base + i, ItemStack.EMPTY);
        }
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean isFilterBlacklist(Direction dir) {
        if (dir == null) return false;
        return filterBlacklist[dir.get3DDataValue()];
    }

    public void setFilterBlacklist(Direction dir, boolean blacklist) {
        if (dir == null) return;
        filterBlacklist[dir.get3DDataValue()] = blacklist;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean toggleFilterBlacklist(Direction dir) {
        if (dir == null) return false;
        boolean newVal = !isFilterBlacklist(dir);
        setFilterBlacklist(dir, newVal);
        return newVal;
    }

    public boolean hasFilterConfigured(Direction dir) {
        if (dir == null) return false;
        int base = dir.get3DDataValue() * 3;
        for (int i = 0; i < 3; i++) {
            if (!filterItems.get(base + i).isEmpty()) return true;
        }
        return false;
    }

    public List<ItemStack> getFilterItemsForFace(Direction dir) {
        if (dir == null) return List.of();
        int base = dir.get3DDataValue() * 3;
        List<ItemStack> list = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            list.add(filterItems.get(base + i));
        }
        return list;
    }

    public boolean matchesFilterForFace(Direction dir, ItemStack candidate, HolderLookup.Provider provider) {
        if (dir == null || candidate == null || candidate.isEmpty()) return true;
        int base = dir.get3DDataValue() * 3;
        boolean blacklist = filterBlacklist[dir.get3DDataValue()];
        boolean hasAny = false;
        boolean matchedAny = false;
        for (int i = 0; i < 3; i++) {
            ItemStack f = filterItems.get(base + i);
            if (!f.isEmpty()) {
                hasAny = true;
                if (matchesFilter(f, candidate, provider)) {
                    matchedAny = true;
                    break;
                }
            }
        }
        if (!hasAny) return true;
        return blacklist ? !matchedAny : matchedAny;
    }

    public boolean matchesFluidFilterForFace(Direction dir, FluidVariant candidate) {
        if (dir == null || candidate == null || candidate.isBlank()) return true;
        int base = dir.get3DDataValue() * 3;
        boolean blacklist = filterBlacklist[dir.get3DDataValue()];
        boolean hasAny = false;
        boolean matchedAny = false;
        for (int i = 0; i < 3; i++) {
            ItemStack f = filterItems.get(base + i);
            if (!f.isEmpty()) {
                hasAny = true;
                FluidVariant fFluid = VolcaniteSortingStationBlockEntity.getFluidFromStack(f);
                if (fFluid != null && !fFluid.isBlank() && fFluid.equals(candidate)) {
                    matchedAny = true;
                    break;
                }
            }
        }
        if (!hasAny) return true;
        return blacklist ? !matchedAny : matchedAny;
    }

    public static boolean matchesFilterList(List<ItemStack> filterList, boolean blacklist, ItemStack candidate, HolderLookup.Provider registries) {
        if (candidate == null || candidate.isEmpty()) return false;
        boolean hasAny = false;
        boolean matchedAny = false;
        for (ItemStack f : filterList) {
            if (!f.isEmpty()) {
                hasAny = true;
                if (matchesFilter(f, candidate, registries)) {
                    matchedAny = true;
                    break;
                }
            }
        }
        if (!hasAny) return true;
        return blacklist ? !matchedAny : matchedAny;
    }

    public static boolean matchesFluidFilterList(List<ItemStack> filterList, boolean blacklist, FluidVariant candidate) {
        if (candidate == null || candidate.isBlank()) return false;
        boolean hasAny = false;
        boolean matchedAny = false;
        for (ItemStack f : filterList) {
            if (!f.isEmpty()) {
                hasAny = true;
                FluidVariant fFluid = VolcaniteSortingStationBlockEntity.getFluidFromStack(f);
                if (fFluid != null && !fFluid.isBlank() && fFluid.equals(candidate)) {
                    matchedAny = true;
                    break;
                }
            }
        }
        if (!hasAny) return true;
        return blacklist ? !matchedAny : matchedAny;
    }

    public ItemStack getFilterItem(Direction dir) {
        return getFilterItem(dir, 0);
    }

    public void setFilterItem(Direction dir, ItemStack stack) {
        setFilterItem(dir, 0, stack);
    }

    public static class InsertTarget {
        public final Container container;
        public final Direction side;
        public final BlockPos pos;
        public final ItemStack filter;
        public final List<ItemStack> filters;
        public final boolean blacklist;
        public final int priority;
        public final int distance;

        public InsertTarget(Container container, Direction side, BlockPos pos, ItemStack filter) {
            this(container, side, pos, (filter == null || filter.isEmpty()) ? List.of() : List.of(filter), false, 0, 0);
        }

        public InsertTarget(Container container, Direction side, BlockPos pos, List<ItemStack> filters, boolean blacklist) {
            this(container, side, pos, filters, blacklist, 0, 0);
        }

        public InsertTarget(Container container, Direction side, BlockPos pos, List<ItemStack> filters, boolean blacklist, int priority, int distance) {
            this.container = container;
            this.side = side;
            this.pos = pos;
            this.filters = filters != null ? filters : List.of();
            this.blacklist = blacklist;
            this.priority = priority;
            this.distance = distance;
            this.filter = this.filters.isEmpty() ? ItemStack.EMPTY : this.filters.get(0);
        }

        public boolean hasFilters() {
            for (ItemStack f : filters) {
                if (!f.isEmpty()) return true;
            }
            return false;
        }

        public boolean matches(ItemStack candidate, HolderLookup.Provider registries) {
            return matchesFilterList(filters, blacklist, candidate, registries);
        }
    }

    public static class ExtractSource {
        public final Container container;
        public final Direction side;
        public final BlockPos pos;
        public final ItemStack filter;
        public final List<ItemStack> filters;
        public final boolean blacklist;

        public ExtractSource(Container container, Direction side, BlockPos pos, ItemStack filter) {
            this(container, side, pos, (filter == null || filter.isEmpty()) ? List.of() : List.of(filter), false);
        }

        public ExtractSource(Container container, Direction side, BlockPos pos, List<ItemStack> filters, boolean blacklist) {
            this.container = container;
            this.side = side;
            this.pos = pos;
            this.filters = filters != null ? filters : List.of();
            this.blacklist = blacklist;
            this.filter = this.filters.isEmpty() ? ItemStack.EMPTY : this.filters.get(0);
        }

        public boolean hasFilters() {
            for (ItemStack f : filters) {
                if (!f.isEmpty()) return true;
            }
            return false;
        }

        public boolean matches(ItemStack candidate, HolderLookup.Provider registries) {
            return matchesFilterList(filters, blacklist, candidate, registries);
        }
    }

    public static class InsertTargetFluid {
        public final Storage<FluidVariant> storage;
        public final BlockPos pos;
        public final ItemStack filter;
        public final List<ItemStack> filters;
        public final boolean blacklist;
        public final int priority;
        public final int distance;

        public InsertTargetFluid(Storage<FluidVariant> storage, ItemStack filter) {
            this(storage, BlockPos.ZERO, (filter == null || filter.isEmpty()) ? List.of() : List.of(filter), false, 0, 0);
        }

        public InsertTargetFluid(Storage<FluidVariant> storage, List<ItemStack> filters, boolean blacklist) {
            this(storage, BlockPos.ZERO, filters, blacklist, 0, 0);
        }

        public InsertTargetFluid(Storage<FluidVariant> storage, BlockPos pos, List<ItemStack> filters, boolean blacklist, int priority, int distance) {
            this.storage = storage;
            this.pos = pos;
            this.filters = filters != null ? filters : List.of();
            this.blacklist = blacklist;
            this.priority = priority;
            this.distance = distance;
            this.filter = this.filters.isEmpty() ? ItemStack.EMPTY : this.filters.get(0);
        }

        public boolean hasFilters() {
            for (ItemStack f : filters) {
                if (!f.isEmpty()) return true;
            }
            return false;
        }

        public boolean matches(FluidVariant candidate) {
            return matchesFluidFilterList(filters, blacklist, candidate);
        }
    }

    public static class PipeNetworkScan {
        public final List<InsertTarget> insertTargets = new ArrayList<>();
        public final List<ExtractSource> extractSources = new ArrayList<>();
        public final List<VolcaniteStorageDriveBlockEntity> connectedDrives = new ArrayList<>();
        public final List<VolcaniteStorageUnitBlockEntity> connectedUnits = new ArrayList<>();
        public final List<VolcaniteModularChestBlockEntity> connectedModularChests = new ArrayList<>();
        public final List<VolcaniteChestBlockEntity> connectedVolcaniteChests = new ArrayList<>();
        public final List<InsertTarget> fallbackContainers = new ArrayList<>();
        public VolcaniteStorageControllerBlockEntity controller = null;
    }

    public static Container getContainerAt(Level level, BlockPos pos) {
        if (level == null || pos == null) return null;
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof net.minecraft.world.level.block.ChestBlock chestBlock) {
            return net.minecraft.world.level.block.ChestBlock.getContainer(chestBlock, state, level, pos, true);
        }
        if (block instanceof DionysianBioCalderaPartBlock) {
            DionysianBioCalderaBlockEntity controller = DionysianBioCalderaPartBlock.findController(level, pos);
            if (controller != null) return controller;
        }
        if (block instanceof TartarusPistonEnginePartBlock) {
            TartarusPistonEngineBlockEntity controller = TartarusPistonEnginePartBlock.findController(level, pos);
            if (controller != null) return controller.getInventory();
        }
        if (block instanceof MegaVolcanicBarrelPartBlock) {
            MegaVolcanicBarrelBlockEntity controller = MegaVolcanicBarrelPartBlock.findController(level, pos);
            if (controller != null) return controller.getInventory();
        }
        if (block == ObsidiumMod.MAGMA_FORGE_DUMMY_BLOCK) {
            BlockPos corePos = MagmaForgeDummyBlock.findCore(level, pos);
            if (corePos != null && level.getBlockEntity(corePos) instanceof MagmaForgeCoreBlockEntity core) {
                return core.getInventory();
            }
        }
        if (block == ObsidiumMod.SYNCHROTRON_RING_COIL_BLOCK || block == ObsidiumMod.HADRON_ITEM_PORT_BLOCK) {
            OlympianHadronColliderBlockEntity controller = SynchrotronRingCoilBlock.findController(level, pos);
            if (controller != null && controller.isFormed()) {
                return controller;
            }
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof Container c) {
            return c;
        }
        if (be instanceof InventoryHolder holder) {
            return holder.getInventory();
        }
        return null;
    }

    private PipeNetworkScan cachedNetworkScan = null;
    private long networkScanCacheTick = -100;

    public PipeNetworkScan getCachedNetworkScan(Level level, BlockPos startPos) {
        if (level == null || startPos == null) return new PipeNetworkScan();
        long currentTick = level.getGameTime();
        if (cachedNetworkScan != null && currentTick - networkScanCacheTick < 20) {
            return cachedNetworkScan;
        }
        networkScanCacheTick = currentTick;
        cachedNetworkScan = scanNetwork(level, startPos);
        return cachedNetworkScan;
    }

    public static PipeNetworkScan scanNetwork(Level level, BlockPos startPos) {
        PipeNetworkScan result = new PipeNetworkScan();
        if (level == null || startPos == null) return result;

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
                    ModuleType mod = pipe.getModule(d);
                    if (mod == ModuleType.INSERT) {
                        Container c = getContainerAt(level, neighborPos);
                        if (c != null) {
                            result.insertTargets.add(new InsertTarget(c, d.getOpposite(), neighborPos, pipe.getFilterItemsForFace(d), pipe.isFilterBlacklist(d)));
                        }
                    } else if (mod == ModuleType.EXTRACT) {
                        Container c = getContainerAt(level, neighborPos);
                        if (c != null) {
                            result.extractSources.add(new ExtractSource(c, d.getOpposite(), neighborPos, pipe.getFilterItemsForFace(d), pipe.isFilterBlacklist(d)));
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
                    BlockEntity be = level.getBlockEntity(next);

                    if (be instanceof VolcaniteStorageDriveBlockEntity drive) {
                        result.connectedDrives.add(drive);
                        queue.add(next);
                    } else if (be instanceof VolcaniteStorageUnitBlockEntity unit) {
                        result.connectedUnits.add(unit);
                        queue.add(next);
                    } else if (be instanceof VolcaniteModularChestBlockEntity modularChest) {
                        result.connectedModularChests.add(modularChest);
                        queue.add(next);
                    } else if (be instanceof VolcaniteChestBlockEntity volcaniteChest) {
                        result.connectedVolcaniteChests.add(volcaniteChest);
                        queue.add(next);
                    } else if (be instanceof VolcaniteStorageControllerBlockEntity ctrl) {
                        if (result.controller == null) result.controller = ctrl;
                        queue.add(next);
                    } else if (be instanceof VolcaniteNetworkNodeReceiverBlockEntity receiver && receiver.isLinked()) {
                        VolcaniteStorageControllerBlockEntity ctrl = receiver.getConnectedController();
                        if (ctrl != null && result.controller == null) result.controller = ctrl;
                        BlockPos ePos = receiver.getLinkedEmitterPos();
                        if (ePos != null && level.isLoaded(ePos) && visited.add(ePos)) {
                            queue.add(ePos);
                        }
                        queue.add(next);
                    } else if (be instanceof VolcaniteNetworkNodeEmitterBlockEntity emitter && emitter.isLinked()) {
                        VolcaniteStorageControllerBlockEntity ctrl = emitter.getConnectedController();
                        if (ctrl != null && result.controller == null) result.controller = ctrl;
                        for (BlockPos rPos : emitter.getLinkedReceivers()) {
                            if (level.isLoaded(rPos) && visited.add(rPos)) {
                                queue.add(rPos);
                            }
                        }
                        for (BlockPos rPos : VolcaniteNetworkNodeReceiverBlockEntity.getActiveReceiversInDimension(level)) {
                            if (next.distSqr(rPos) <= (VolcaniteNetworkNodeEmitterBlockEntity.RANGE * VolcaniteNetworkNodeEmitterBlockEntity.RANGE)) {
                                emitter.registerReceiver(rPos);
                                if (level.isLoaded(rPos) && visited.add(rPos)) {
                                    queue.add(rPos);
                                }
                            }
                        }
                        queue.add(next);
                    } else if (PipeConnectionHelper.isLogisticsPipe(s.getBlock()) ||
                               PipeConnectionHelper.isStorageNetworkBlock(s.getBlock())) {
                        queue.add(next);
                    } else {
                        if (currBe instanceof VolcaniteStoragePipeBlockEntity pipe && pipe.isOpticFiber()) {
                            if (pipe.getModule(d) == ModuleType.NONE) {
                                continue;
                            }
                        }
                        Container c = getContainerAt(level, next);
                        if (c != null) {
                            result.fallbackContainers.add(new InsertTarget(c, d.getOpposite(), next, List.of(), false));
                            if (s.getBlock() instanceof net.minecraft.world.level.block.ChestBlock) {
                                for (Direction cd : Direction.Plane.HORIZONTAL) {
                                    BlockPos p2 = next.relative(cd);
                                    if (level.getBlockState(p2).is(s.getBlock())) visited.add(p2);
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VolcaniteStoragePipeBlockEntity entity) {
        if (level.isClientSide()) return;
        entity.tickCounter++;

        boolean hasModule = entity.hasAnyModule();

        // Fast O(1) early-exit: passive optic fiber cables with no Import/Export Connectors do zero BFS work!
        if (!hasModule || entity.tickCounter % 5 != 0) {
            return;
        }

        VolcaniteStorageControllerBlockEntity controller = entity.findConnectedController();

        // Storage Network requires an active powered Controller to process items & fluids
        if (controller == null || !controller.isPowered()) return;

        for (Direction dir : Direction.values()) {
            if (entity.isSideDisconnected(dir)) continue;
            BlockPos targetPos = pos.relative(dir);
            Direction opposite = dir.getOpposite();
            BlockState targetState = level.getBlockState(targetPos);
            ModuleType module = entity.getModule(dir);

            int transferType = entity.getTransferType(dir);

            if (module == ModuleType.EXTRACT) {
                // EXTRACT: Pull from adjacent machine/container -> Insert into Network
                if (transferType == TYPE_ITEM) {
                    Container targetContainer = getContainerAt(level, targetPos);
                    Set<BlockPos> excludePositions = new HashSet<>();
                    excludePositions.add(targetPos);

                    if (targetState.getBlock() instanceof net.minecraft.world.level.block.ChestBlock) {
                        for (Direction d : Direction.Plane.HORIZONTAL) {
                            BlockPos checkPos = targetPos.relative(d);
                            if (level.getBlockState(checkPos).is(targetState.getBlock())) {
                                excludePositions.add(checkPos);
                            }
                        }
                    }

                    if (targetContainer != null) {
                        int size = targetContainer.getContainerSize();
                        int startSlot = entity.lastExtractedSlot[dir.get3DDataValue()];
                        PipeNetworkScan scan = null;
                        for (int step = 0; step < size; step++) {
                            int i = (startSlot + step) % size;
                            ItemStack inSlot = targetContainer.getItem(i);
                            if (inSlot.isEmpty()) continue;
                            if (!isOutputSlot(targetContainer, i, inSlot, opposite)) continue;
                            if (!entity.matchesFilterForFace(dir, inSlot, level.registryAccess())) continue;

                            if (scan == null) {
                                scan = entity.getCachedNetworkScan(level, pos);
                            }

                            int toExtractCount = Math.min(inSlot.getCount(), entity.getMaxItemTransfer(dir));
                            ItemStack testExtract = inSlot.copyWithCount(toExtractCount);
                            ItemStack leftover = routeItemIntoNetwork(level, pos, controller, scan, testExtract, excludePositions, true);
                            int actuallyInserted = toExtractCount - leftover.getCount();

                            if (actuallyInserted > 0) {
                                ItemStack extracted = (targetContainer instanceof VolcaniteItemBarrelBlockEntity barrel) ?
                                    barrel.extractItemForPipe(actuallyInserted, null, opposite, false) :
                                    targetContainer.removeItem(i, actuallyInserted);
                                targetContainer.setChanged();
                                if (!extracted.isEmpty()) {
                                    ItemStack leftoverReal = routeItemIntoNetwork(level, pos, controller, scan, extracted, excludePositions, false);
                                    if (!leftoverReal.isEmpty()) {
                                        insertIntoContainer(targetContainer, opposite, leftoverReal, false);
                                    }
                                    int realCount = extracted.getCount() - leftoverReal.getCount();
                                    if (controller != null && realCount > 0) {
                                        controller.consumeEnergy(10L * realCount);
                                    }
                                }
                                entity.lastExtractedSlot[dir.get3DDataValue()] = (i + 1) % size;
                                break;
                            }
                        }
                    }
                } else if (transferType == TYPE_FLUID) {
                    // Fluid Extract
                    Storage<FluidVariant> fluidStorage = FluidStorage.SIDED.find(level, targetPos, opposite);
                    if (fluidStorage != null) {
                        for (var view : fluidStorage) {
                            if (!view.isResourceBlank() && view.getAmount() > 0) {
                                FluidVariant variant = view.getResource();
                                if (!entity.matchesFluidFilterForFace(dir, variant)) {
                                    continue;
                                }
                                long canExtract = Math.min(view.getAmount(), entity.getMaxFluidTransfer(dir) * 81L);
                                int canExtract_mB = (int) (canExtract / 81L);
                                int simulatedInserted = (controller != null) ?
                                    controller.insertFluid(variant, canExtract_mB, true) :
                                    routeFluidIntoNetwork(level, pos, variant, canExtract_mB, true);

                                if (simulatedInserted > 0) {
                                    try (Transaction tx = Transaction.openOuter()) {
                                        long extractedDroplets = fluidStorage.extract(variant, simulatedInserted * 81L, tx);
                                        if (extractedDroplets > 0) {
                                            tx.commit();
                                            if (controller != null) {
                                                controller.insertFluid(variant, (int) (extractedDroplets / 81L), false);
                                                controller.consumeEnergy(Math.max(5, (int)(extractedDroplets / 1620L)));
                                            } else {
                                                routeFluidIntoNetwork(level, pos, variant, (int) (extractedDroplets / 81L), false);
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            } else if (module == ModuleType.INSERT) {
                // INSERT: Pull from Network -> Push into adjacent machine/container
                if (transferType == TYPE_ITEM) {
                    Container targetContainer = getContainerAt(level, targetPos);

                    if (targetContainer != null) {
                        if (entity.hasFilterConfigured(dir)) {
                            if (!entity.isFilterBlacklist(dir)) {
                                int startFilter = entity.lastFilterIndex[dir.get3DDataValue()];
                                for (int step = 0; step < 3; step++) {
                                    int s = (startFilter + step) % 3;
                                    ItemStack filter = entity.getFilterItem(dir, s);
                                    if (filter.isEmpty()) continue;
                                    int maxAcceptable = insertIntoContainer(targetContainer, opposite, filter.copyWithCount(entity.getMaxItemTransfer(dir)), true);
                                    if (maxAcceptable > 0) {
                                        ItemStack extracted = (controller != null) ?
                                            controller.extractItemExcluding(filter.getItem(), maxAcceptable, targetPos, false) :
                                            extractItemFromNetworkExcluding(level, pos, filter.getItem(), maxAcceptable, targetPos, false);
                                        if (!extracted.isEmpty()) {
                                            int inserted = insertIntoContainer(targetContainer, opposite, extracted, false);
                                            if (inserted < extracted.getCount()) {
                                                ItemStack uninserted = extracted.copyWithCount(extracted.getCount() - inserted);
                                                if (controller != null) {
                                                    controller.insertItem(uninserted, false);
                                                } else {
                                                    routeItemIntoNetwork(level, pos, null, uninserted, null, false);
                                                }
                                            }
                                            if (controller != null && inserted > 0) {
                                                controller.consumeEnergy(10L * inserted);
                                            }
                                            entity.lastFilterIndex[dir.get3DDataValue()] = (s + 1) % 3;
                                            break;
                                        }
                                    }
                                }
                            } else {
                                ItemStack extracted = extractFirstAcceptableItemFromNetwork(level, pos, targetContainer, targetPos, opposite, entity.getMaxItemTransfer(dir), stack -> entity.matchesFilterForFace(dir, stack, level.registryAccess()));
                                if (!extracted.isEmpty()) {
                                    int inserted = insertIntoContainer(targetContainer, opposite, extracted, false);
                                    if (inserted < extracted.getCount()) {
                                        ItemStack uninserted = extracted.copyWithCount(extracted.getCount() - inserted);
                                        if (controller != null) {
                                            controller.insertItem(uninserted, false);
                                        } else {
                                            routeItemIntoNetwork(level, pos, null, uninserted, null, false);
                                        }
                                    }
                                    if (controller != null && inserted > 0) {
                                        controller.consumeEnergy(10L * inserted);
                                    }
                                }
                            }
                        } else if (!(targetContainer instanceof VolcaniteStorageUnitBlockEntity)
                                && !(targetContainer instanceof VolcaniteModularChestBlockEntity)
                                && !(targetContainer instanceof VolcaniteChestBlockEntity)) {
                            ItemStack extracted = extractFirstAcceptableItemFromNetwork(level, pos, targetContainer, targetPos, opposite, entity.getMaxItemTransfer(dir), null);
                            if (!extracted.isEmpty()) {
                                int inserted = insertIntoContainer(targetContainer, opposite, extracted, false);
                                if (inserted < extracted.getCount()) {
                                    ItemStack uninserted = extracted.copyWithCount(extracted.getCount() - inserted);
                                    if (controller != null) {
                                        controller.insertItem(uninserted, false);
                                    } else {
                                        routeItemIntoNetwork(level, pos, null, uninserted, null, false);
                                    }
                                }
                                if (controller != null && inserted > 0) {
                                    controller.consumeEnergy(10L * inserted);
                                }
                            }
                        }
                    }
                } else if (transferType == TYPE_FLUID) {
                    // Fluid Insert
                    Storage<FluidVariant> fluidStorage = FluidStorage.SIDED.find(level, targetPos, opposite);
                    if (fluidStorage != null) {
                        Map<FluidVariant, Integer> networkFluids = (controller != null) ? 
                            controller.getUnifiedFluids() : 
                            VolcaniteSortingStationBlockEntity.getNetworkFluids(level, pos);

                        for (Map.Entry<FluidVariant, Integer> entry : networkFluids.entrySet()) {
                            FluidVariant fv = entry.getKey();
                            if (!entity.matchesFluidFilterForFace(dir, fv)) {
                                continue;
                            }
                            int available_mB = entry.getValue();
                            if (available_mB > 0) {
                                int toPush_mB = Math.min(available_mB, (int) entity.getMaxFluidTransfer(dir));
                                try (Transaction tx = Transaction.openOuter()) {
                                    long insertedDroplets = fluidStorage.insert(fv, (long) toPush_mB * 81L, tx);
                                    if (insertedDroplets > 0) {
                                        tx.commit();
                                        int inserted_mB = (int) (insertedDroplets / 81L);
                                        if (controller != null) {
                                            controller.extractFluid(fv, inserted_mB, false);
                                            controller.consumeEnergy(Math.max(5, inserted_mB / 20));
                                        } else {
                                            VolcaniteSortingStationBlockEntity.extractFluidFromNetwork(level, pos, fv, inserted_mB, false);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static boolean isPureStorage(Container container) {
        if (container == null) return false;
        if (container instanceof VolcaniteItemBarrelBlockEntity
                || container instanceof VolcaniteStorageDriveBlockEntity
                || container instanceof DemetrianSaplingBarrelBlockEntity
                || container instanceof DionysianAgingBarrelBlockEntity
                || container instanceof VolcanicWoodLiquidBarrelBlockEntity
                || container instanceof MegaVolcanicBarrelBlockEntity
                || container instanceof ReinforcedVolcanicLiquidBarrelBlockEntity
                || container instanceof VolcaniteLiquidStorageTankBlockEntity
                || container instanceof LiquidTankBlockEntity
                || container instanceof DemetrianBonsaiPlanterBlockEntity
                || container instanceof VolcaniteEnergyCoreBlockEntity) {
            return false;
        }
        if (container instanceof net.minecraft.world.level.block.entity.ChestBlockEntity ||
            container instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity ||
            container instanceof net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity ||
            container instanceof net.minecraft.world.level.block.entity.HopperBlockEntity ||
            container instanceof VolcaniteStorageUnitBlockEntity ||
            container instanceof VolcaniteModularChestBlockEntity ||
            container instanceof HephaesteanArmoryBlockEntity ||
            container instanceof VolcaniteChestBlockEntity) {
            return true;
        }
        String name = container.getClass().getSimpleName();
        if (name.contains("VolcaniteItemBarrel") || name.contains("SaplingBarrel")
                || name.contains("AgingBarrel") || name.contains("Liquid")
                || name.contains("Tank") || name.contains("Drive")
                || name.contains("Energy") || name.contains("Bonsai")) {
            return false;
        }
        return name.contains("Chest") || name.contains("Barrel") || name.contains("ShulkerBox") || name.contains("Hopper");
    }

    public static boolean isBlockedAutoOutputTarget(@Nullable Level level, @Nullable BlockPos targetPos,
            @Nullable BlockEntity targetBe, @Nullable Container targetContainer) {
        if (targetBe instanceof VolcaniteEnergyCoreBlockEntity
                || targetBe instanceof VolcaniteEnergyCellBlockEntity
                || targetBe instanceof VolcaniteEnergyUnitBlockEntity
                || targetBe instanceof HephaesteanChargingPlateBlockEntity
                || targetBe instanceof AstrapeEnergyPylonBlockEntity
                || targetBe instanceof VolcaniteEnergyWireBlockEntity
                || targetBe instanceof DemetrianBonsaiPlanterBlockEntity
                || targetBe instanceof VolcaniteStorageDriveBlockEntity
                || targetBe instanceof VolcaniteLiquidStorageTankBlockEntity
                || targetBe instanceof LiquidTankBlockEntity
                || targetBe instanceof VolcanicWoodLiquidBarrelBlockEntity
                || targetBe instanceof MegaVolcanicBarrelBlockEntity
                || targetBe instanceof ReinforcedVolcanicLiquidBarrelBlockEntity
                || targetBe instanceof VolcaniteStorageControllerBlockEntity
                || targetBe instanceof VolcaniteNetworkNodeEmitterBlockEntity
                || targetBe instanceof VolcaniteNetworkNodeReceiverBlockEntity
                || targetBe instanceof GraftedSaplingBlockEntity
                || targetBe instanceof VolcanicBeehiveBlockEntity
                || targetBe instanceof OlympianAltarBlockEntity
                || targetBe instanceof StygianCenotaphBlockEntity
                || targetBe instanceof VolcaniteChunkLoaderBlockEntity
                || targetBe instanceof VolcaniteCrafterInterfaceBlockEntity
                || targetBe instanceof VolcaniteSortingStationBlockEntity
                || targetBe instanceof VolcaniteStorageInterfaceBlockEntity) {
            return true;
        }
        if (level != null && targetPos != null
                && level.getBlockState(targetPos).getBlock() instanceof MegaVolcanicBarrelPartBlock) {
            return true;
        }
        return targetContainer instanceof VolcaniteEnergyCoreBlockEntity
                || targetContainer instanceof DemetrianBonsaiPlanterBlockEntity
                || targetContainer instanceof VolcaniteStorageDriveBlockEntity
                || targetContainer instanceof VolcaniteLiquidStorageTankBlockEntity
                || targetContainer instanceof LiquidTankBlockEntity;
    }

    public static boolean canAutoOutputTargetAccept(@Nullable BlockEntity sourceBe, @Nullable Level level,
            @Nullable BlockPos targetPos, @Nullable BlockEntity targetBe, @Nullable Container targetContainer,
            @Nullable Direction side, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (targetBe != null && targetBe == sourceBe) return false;
        if (targetContainer != null && (targetContainer == sourceBe
                || (sourceBe instanceof InventoryHolder ih && targetContainer == ih.getInventory()))) {
            return false;
        }
        if (sourceBe instanceof OlympianHadronColliderBlockEntity collider && targetPos != null && collider.isPosOnRing(targetPos)) {
            return false;
        }
        if (isBlockedAutoOutputTarget(level, targetPos, targetBe, targetContainer)) return false;

        // Arborist harvests must never auto-export into other arborists or unrelated processing machines.
        if (sourceBe instanceof GeothermalArboristBlockEntity) {
            if (targetBe instanceof GeothermalArboristBlockEntity) return false;
            if (targetBe instanceof VolcaniteItemBarrelBlockEntity
                    || targetBe instanceof VolcaniteStoragePipeBlockEntity
                    || targetBe instanceof HermesItemPipeBlockEntity
                    || targetBe instanceof VolcaniteUniversalConduitBlockEntity
                    || isPureStorage(targetContainer)) {
                return true;
            }
            if (targetBe instanceof DemetrianLumberDepotBlockEntity depot) {
                return depot.canPlaceItem(0, stack);
            }
            if (targetBe instanceof DemetrianSaplingBarrelBlockEntity saplingBarrel) {
                for (int s = 0; s <= 2; s++) {
                    if (saplingBarrel.canPlaceItem(s, stack)) return true;
                }
                return false;
            }
            if (targetBe instanceof DemetrianComposterBlockEntity composter) {
                for (int s = 0; s < composter.getContainerSize(); s++) {
                    if (composter.canPlaceItem(s, stack)) return true;
                }
                return false;
            }
            if (targetBe instanceof DemetrianBiomassGeneratorBlockEntity bioGen) {
                return bioGen.canPlaceItem(0, stack);
            }
            return false;
        }
        return true;
    }

    public static int tryAutoOutputStack(Level level, BlockPos sourcePos, @Nullable BlockEntity sourceBe, ItemStack stack) {
        if (level == null || sourcePos == null || stack.isEmpty()) return 0;
        int totalMoved = 0;
        Container sourceContainer = (sourceBe instanceof InventoryHolder ih) ? ih.getInventory()
                : (sourceBe instanceof Container c ? c : null);

        for (Direction dir : Direction.values()) {
            BlockPos targetPos = sourcePos.relative(dir);
            Direction opposite = dir.getOpposite();
            BlockEntity targetBe = level.getBlockEntity(targetPos);
            Container targetContainer = getContainerAt(level, targetPos);

            if (!canAutoOutputTargetAccept(sourceBe, level, targetPos, targetBe, targetContainer, opposite, stack)) {
                continue;
            }

            if (targetContainer != null && targetContainer != sourceContainer && targetContainer != sourceBe) {
                int inserted = insertIntoContainer(targetContainer, opposite, stack, false);
                if (inserted > 0) {
                    stack.shrink(inserted);
                    totalMoved += inserted;
                    if (stack.isEmpty()) break;
                }
            }

            if (!stack.isEmpty()) {
                Storage<ItemVariant> targetStorage = ItemStorage.SIDED.find(level, targetPos, opposite);
                if (targetStorage != null) {
                    try (Transaction tx = Transaction.openOuter()) {
                        long inserted = targetStorage.insert(ItemVariant.of(stack), stack.getCount(), tx);
                        if (inserted > 0) {
                            tx.commit();
                            stack.shrink((int) inserted);
                            totalMoved += (int) inserted;
                            if (stack.isEmpty()) break;
                        }
                    }
                }
            }
        }
        return totalMoved;
    }

    public static boolean isOutputSlot(Container container, int slot, ItemStack stack, @Nullable Direction side) {
        if (container == null) return false;
        if (container instanceof VolcaniteItemBarrelBlockEntity) return slot == 1;
        if (container instanceof VolcaniteModularChestBlockEntity modularChest) {
            return slot >= 0 && slot < modularChest.getUnlockedSlots();
        }
        if (isPureStorage(container)) return true;

        if (container instanceof MachineInventory mi) {
            return mi.isOutputSlot(slot, stack, side);
        }

        // Vanilla Machines
        if (container instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity) {
            return slot == 2; // Smelted product
        }
        if (container instanceof net.minecraft.world.level.block.entity.BrewingStandBlockEntity) {
            return slot >= 0 && slot <= 2; // Potions
        }

        // Fallback for mod machines
        if (container instanceof VolcaniteGrinderBlockEntity) return slot == 1;
        if (container instanceof VolcaniteFurnaceBlockEntity) return slot == 2 || slot == 3;
        if (container instanceof AdvancedVolcaniteFurnaceBlockEntity) return slot == 1 || slot == 2;
        if (container instanceof VolcaniteWorktableBlockEntity) return slot == 10;
        if (container instanceof VolcaniteGeneratorBlockEntity || 
            container instanceof VolcaniteGeothermalGeneratorBlockEntity || 
            container instanceof LiquidTankBlockEntity) return slot == 1;
        if (container instanceof VolcaniteSteamGeneratorBlockEntity) return slot == 1;
        if (container instanceof MagmaForgeCoreBlockEntity) return slot == 2;
        if (container instanceof GeothermalArboristBlockEntity) return (slot >= 4 && slot <= 9) || slot == 3;

        if (container instanceof WorldlyContainer worldly) {
            if (side != null) {
                return worldly.canTakeItemThroughFace(slot, stack, side);
            }
            for (Direction d : Direction.values()) {
                if (worldly.canTakeItemThroughFace(slot, stack, d)) return true;
            }
            return false;
        }
        return true;
    }

    public static boolean isInputSlot(Container container, int slot, ItemStack stack, @Nullable Direction side) {
        if (container == null) return false;
        if (container instanceof VolcaniteItemBarrelBlockEntity barrel) {
            return barrel.canPlaceItem(slot, stack);
        }
        if (container instanceof VolcaniteModularChestBlockEntity modularChest) {
            return slot >= 0 && slot < modularChest.getUnlockedSlots() && modularChest.matchesChestFilters(stack);
        }
        if (isPureStorage(container)) return container.canPlaceItem(slot, stack);

        if (container instanceof MachineInventory mi) {
            return mi.isInputSlot(slot, stack, side) && container.canPlaceItem(slot, stack);
        }

        // Vanilla Machines
        if (container instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity) {
            if (slot == 2) return false; // Output slot
            if (slot == 0) return true; // Cookable input
            if (slot == 1) return net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.isFuel(stack); // Fuel
            return false;
        }
        if (container instanceof net.minecraft.world.level.block.entity.BrewingStandBlockEntity) {
            return container.canPlaceItem(slot, stack);
        }

        // Fallback for mod machines
        if (container instanceof VolcaniteGrinderBlockEntity) return slot == 0 && container.canPlaceItem(slot, stack);
        if (container instanceof VolcaniteFurnaceBlockEntity) return (slot == 0 || slot == 1) && container.canPlaceItem(slot, stack);
        if (container instanceof AdvancedVolcaniteFurnaceBlockEntity) return slot == 0 && container.canPlaceItem(slot, stack);
        if (container instanceof VolcaniteWorktableBlockEntity) return slot >= 0 && slot <= 9 && container.canPlaceItem(slot, stack);
        if (container instanceof VolcaniteGeneratorBlockEntity || 
            container instanceof VolcaniteGeothermalGeneratorBlockEntity || 
            container instanceof LiquidTankBlockEntity) return slot == 0 && container.canPlaceItem(slot, stack);
        if (container instanceof VolcaniteSteamGeneratorBlockEntity) return (slot == 0 || slot == 2) && container.canPlaceItem(slot, stack);
        if (container instanceof MagmaForgeCoreBlockEntity) return (slot == 0 || slot == 1) && container.canPlaceItem(slot, stack);
        if (container instanceof GeothermalArboristBlockEntity) return (slot == 0 || slot == 1 || slot == 2 || slot == 10 || slot == 11 || slot == 12) && container.canPlaceItem(slot, stack);

        if (container instanceof WorldlyContainer worldly) {
            if (side != null) {
                return worldly.canPlaceItemThroughFace(slot, stack, side) && container.canPlaceItem(slot, stack);
            }
            for (Direction d : Direction.values()) {
                if (worldly.canPlaceItemThroughFace(slot, stack, d) && container.canPlaceItem(slot, stack)) return true;
            }
            return false;
        }
        return container.canPlaceItem(slot, stack);
    }

    public static int insertIntoContainer(Container container, @Nullable Direction side, ItemStack stack, boolean simulate) {
        if (container == null || stack.isEmpty()) return 0;
        if (container instanceof VolcaniteItemBarrelBlockEntity barrel) {
            return barrel.insertItemFromPipe(stack, side, simulate);
        }
        if (container instanceof VolcaniteModularChestBlockEntity modularChest) {
            ItemStack rem = modularChest.insertItem(stack, simulate);
            return stack.getCount() - rem.getCount();
        }
        if (container instanceof VolcaniteStorageUnitBlockEntity unit) {
            ItemStack rem = unit.insertItem(stack, simulate);
            return stack.getCount() - rem.getCount();
        }
        int remaining = stack.getCount();
        int size = container.getContainerSize();

        // Pass 1: Existing stacks matching the item in valid input slots
        for (int i = 0; i < size; i++) {
            if (!isInputSlot(container, i, stack, side)) continue;

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
                    if (remaining <= 0) break;
                }
            }
        }

        // Pass 2: Empty valid input slots
        if (remaining > 0) {
            for (int i = 0; i < size; i++) {
                if (!isInputSlot(container, i, stack, side)) continue;

                ItemStack inSlot = container.getItem(i);
                if (inSlot.isEmpty()) {
                    int maxStack = Math.min(container.getMaxStackSize(stack), stack.getMaxStackSize());
                    int canAdd = Math.min(remaining, maxStack);
                    if (!simulate) {
                        container.setItem(i, stack.copyWithCount(canAdd));
                        container.setChanged();
                    }
                    remaining -= canAdd;
                    if (remaining <= 0) break;
                }
            }
        }

        return stack.getCount() - remaining;
    }

    public static boolean autoEjectMachineOutputs(Level level, BlockPos pos, BlockEntity be) {
        if (level == null || level.isClientSide() || be == null) return false;
        if (level.getGameTime() % 2 != 0) return false;

        Container container = null;
        if (be instanceof InventoryHolder ih) {
            container = ih.getInventory();
        } else if (be instanceof Container c) {
            container = c;
        }
        if (container == null) return false;

        MachineInventory machineInv = (be instanceof MachineInventory mi) ? mi : (container instanceof MachineInventory cmi ? cmi : null);

        boolean changed = false;
        int size = container.getContainerSize();
        for (int slot = 0; slot < size; slot++) {
            ItemStack ejectStack = container.getItem(slot);
            if (ejectStack.isEmpty()) continue;

            boolean isOutput = (machineInv != null) ? machineInv.isOutputSlot(slot, ejectStack, null) : isOutputSlot(container, slot, ejectStack, null);
            if (!isOutput) continue;

            if (tryAutoOutputStack(level, pos, be, ejectStack) > 0) {
                changed = true;
            }
        }
        if (changed) {
            be.setChanged();
        }
        return changed;
    }

    public static boolean matchesFilter(ItemStack filter, ItemStack candidate, net.minecraft.core.HolderLookup.Provider registries) {
        if (filter == null || filter.isEmpty() || candidate == null || candidate.isEmpty()) return true;
        if (filter.getItem() instanceof net.valentin.obsidium.item.HermesFilterUpgradeItem) {
            return net.valentin.obsidium.item.HermesFilterUpgradeItem.matches(filter, candidate, registries);
        }
        return ItemStack.isSameItem(candidate, filter);
    }

    public static ItemStack extractFromContainer(Container container, @Nullable Direction side, @Nullable ItemStack filter, int maxAmount, boolean simulate) {
        if (container == null || maxAmount <= 0) return ItemStack.EMPTY;
        if (container instanceof VolcaniteItemBarrelBlockEntity barrel) {
            return barrel.extractItemForPipe(maxAmount, filter, side, simulate);
        }

        int size = container.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack inSlot = container.getItem(i);
            if (!inSlot.isEmpty()) {
                if (!isOutputSlot(container, i, inSlot, side)) {
                    continue;
                }
                if (filter != null && !filter.isEmpty() && !ItemStack.isSameItem(inSlot, filter)) {
                    continue;
                }
                int toExtract = Math.min(inSlot.getCount(), maxAmount);
                if (simulate) {
                    return inSlot.copyWithCount(toExtract);
                } else {
                    ItemStack res = container.removeItem(i, toExtract);
                    container.setChanged();
                    return res;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack routeItemIntoNetwork(Level level, BlockPos pipePos, VolcaniteStorageControllerBlockEntity controller, ItemStack stack, Set<BlockPos> excludePositions, boolean simulate) {
        return routeItemIntoNetwork(level, pipePos, controller, null, stack, excludePositions, simulate);
    }

    public static ItemStack routeItemIntoNetwork(Level level, BlockPos pipePos, VolcaniteStorageControllerBlockEntity controller, @org.jetbrains.annotations.Nullable PipeNetworkScan cachedScan, ItemStack stack, Set<BlockPos> excludePositions, boolean simulate) {
        if (level == null || stack.isEmpty()) return stack;
        ItemStack remaining = stack.copy();

        PipeNetworkScan scan = (cachedScan != null) ? cachedScan : scanNetwork(level, pipePos);
        if (controller == null) {
            controller = scan.controller;
        }

        // 1. PRIORITY 1: INSERT Modules with matching filter!
        for (InsertTarget target : scan.insertTargets) {
            if (excludePositions != null && excludePositions.contains(target.pos)) continue;
            if (target.hasFilters() && target.matches(remaining, level.registryAccess())) {
                int accepted = insertIntoContainer(target.container, target.side, remaining, simulate);
                if (accepted > 0) {
                    remaining.shrink(accepted);
                    if (remaining.isEmpty()) return ItemStack.EMPTY;
                }
            }
        }

        // 2. PRIORITY 2: INSERT Modules without filter (universal accept)!
        for (InsertTarget target : scan.insertTargets) {
            if (excludePositions != null && excludePositions.contains(target.pos)) continue;
            if (!target.hasFilters()) {
                int accepted = insertIntoContainer(target.container, target.side, remaining, simulate);
                if (accepted > 0) {
                    remaining.shrink(accepted);
                    if (remaining.isEmpty()) return ItemStack.EMPTY;
                }
            }
        }

        // 3. PRIORITY 3: Storage Drives / Controller Disks & Units
        if (controller != null) {
            remaining = controller.insertItemExcluding(remaining, excludePositions, simulate);
            if (remaining.isEmpty()) return ItemStack.EMPTY;
        } else {
            for (VolcaniteModularChestBlockEntity modularChest : scan.connectedModularChests) {
                if (excludePositions != null && excludePositions.contains(modularChest.getBlockPos())) continue;
                if (modularChest.hasActiveWhitelistFilter()) {
                    remaining = modularChest.insertItem(remaining, simulate);
                    if (remaining.isEmpty()) return ItemStack.EMPTY;
                }
            }
            for (VolcaniteStorageDriveBlockEntity drive : scan.connectedDrives) {
                remaining = drive.insertItem(remaining, simulate);
                if (remaining.isEmpty()) return ItemStack.EMPTY;
            }
        }

        // 4. PRIORITY 4: Storage Units (Grand Chests, Modular Chests, Volcanite Chests on network)
        if (controller == null) {
            for (VolcaniteStorageUnitBlockEntity unit : scan.connectedUnits) {
                if (excludePositions != null && excludePositions.contains(unit.getBlockPos())) continue;
                remaining = unit.insertItem(remaining, simulate);
                if (remaining.isEmpty()) return ItemStack.EMPTY;
            }
            for (VolcaniteModularChestBlockEntity modularChest : scan.connectedModularChests) {
                if (excludePositions != null && excludePositions.contains(modularChest.getBlockPos())) continue;
                remaining = modularChest.insertItem(remaining, simulate);
                if (remaining.isEmpty()) return ItemStack.EMPTY;
            }
            for (VolcaniteChestBlockEntity volcaniteChest : scan.connectedVolcaniteChests) {
                if (excludePositions != null && excludePositions.contains(volcaniteChest.getBlockPos())) continue;
                remaining = volcaniteChest.insertItem(remaining, simulate);
                if (remaining.isEmpty()) return ItemStack.EMPTY;
            }
        }

        // 5. PRIORITY 5: Fallback connected containers without modules
        if (controller == null) {
            for (InsertTarget target : scan.fallbackContainers) {
                if (excludePositions != null && excludePositions.contains(target.pos)) continue;
                int accepted = insertIntoContainer(target.container, target.side, remaining, simulate);
                if (accepted > 0) {
                    remaining.shrink(accepted);
                    if (remaining.isEmpty()) return ItemStack.EMPTY;
                }
            }
        }

        return remaining;
    }

    public static int routeFluidIntoNetwork(Level level, BlockPos pipePos, FluidVariant fluid, int amount_mB, boolean simulate) {
        if (level == null || fluid.isBlank() || amount_mB <= 0) return 0;
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(pipePos);
        visited.add(pipePos);

        List<InsertTargetFluid> insertTargets = new ArrayList<>();
        List<VolcaniteStorageDriveBlockEntity> connectedDrives = new ArrayList<>();
        List<VolcaniteLiquidStorageTankBlockEntity> connectedTanks = new ArrayList<>();
        List<LiquidTankBlockEntity> connectedLiquidTanks = new ArrayList<>();
        List<Storage<FluidVariant>> externalTanks = new ArrayList<>();

        int steps = 0;
        int maxSteps = 4096;
        while (!queue.isEmpty() && steps++ < maxSteps) {
            BlockPos curr = queue.poll();
            if (!level.isLoaded(curr)) continue;
            BlockEntity currBe = level.getBlockEntity(curr);

            if (currBe instanceof VolcaniteStoragePipeBlockEntity pipe) {
                for (Direction d : Direction.values()) {
                    if (pipe.isSideDisconnected(d)) continue;
                    if (pipe.getModule(d) == ModuleType.INSERT) {
                        BlockPos targetPos = curr.relative(d);
                        if (!level.isLoaded(targetPos)) continue;
                        Storage<FluidVariant> st = FluidStorage.SIDED.find(level, targetPos, d.getOpposite());
                        if (st != null) {
                            insertTargets.add(new InsertTargetFluid(st, pipe.getFilterItemsForFace(d), pipe.isFilterBlacklist(d)));
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
                    BlockEntity be = level.getBlockEntity(next);

                    if (be instanceof VolcaniteStorageDriveBlockEntity drive) {
                        connectedDrives.add(drive);
                        queue.add(next);
                    } else if (be instanceof VolcaniteLiquidStorageTankBlockEntity tank) {
                        connectedTanks.add(tank);
                        queue.add(next);
                    } else if (be instanceof LiquidTankBlockEntity liquidTank) {
                        connectedLiquidTanks.add(liquidTank);
                        queue.add(next);
                    } else if (PipeConnectionHelper.isLogisticsPipe(s.getBlock()) ||
                               PipeConnectionHelper.isStorageNetworkBlock(s.getBlock())) {
                        queue.add(next);
                    } else {
                        Storage<FluidVariant> ext = FluidStorage.SIDED.find(level, next, d.getOpposite());
                        if (ext != null) externalTanks.add(ext);
                    }
                }
            }
        }

        int remaining = amount_mB;
        int totalInserted = 0;

        // 1. PRIORITY 1: Pipes with INSERT module facing fluid storage!
        for (InsertTargetFluid target : insertTargets) {
            if (!target.matches(fluid)) continue;

            try (Transaction tx = Transaction.openOuter()) {
                long insertedDroplets = target.storage.insert(fluid, remaining * 81L, tx);
                int ins_mB = (int) (insertedDroplets / 81L);
                if (ins_mB > 0) {
                    if (!simulate) tx.commit();
                    remaining -= ins_mB;
                    totalInserted += ins_mB;
                    if (remaining <= 0) return totalInserted;
                }
            }
        }

        for (VolcaniteStorageDriveBlockEntity drive : connectedDrives) {
            int ins = drive.insertFluid(fluid, remaining, simulate);
            remaining -= ins;
            totalInserted += ins;
            if (remaining <= 0) return totalInserted;
        }

        for (LiquidTankBlockEntity liquidTank : connectedLiquidTanks) {
            int ins = liquidTank.insertFluid(fluid, remaining, simulate);
            remaining -= ins;
            totalInserted += ins;
            if (remaining <= 0) return totalInserted;
        }

        for (VolcaniteLiquidStorageTankBlockEntity tank : connectedTanks) {
            int ins = tank.insertFluid(fluid, remaining, simulate);
            remaining -= ins;
            totalInserted += ins;
            if (remaining <= 0) return totalInserted;
        }

        for (Storage<FluidVariant> ext : externalTanks) {
            try (Transaction tx = Transaction.openOuter()) {
                long insertedDroplets = ext.insert(fluid, remaining * 81L, tx);
                int ins_mB = (int) (insertedDroplets / 81L);
                if (ins_mB > 0) {
                    if (!simulate) tx.commit();
                    remaining -= ins_mB;
                    totalInserted += ins_mB;
                    if (remaining <= 0) return totalInserted;
                }
            }
        }

        return totalInserted;
    }

    public static ItemStack extractItemFromNetwork(Level level, BlockPos pipePos, net.minecraft.world.item.Item item, int amount, boolean simulate) {
        return extractItemFromNetworkExcluding(level, pipePos, item, amount, null, simulate);
    }

    public static ItemStack extractItemFromNetworkExcluding(Level level, BlockPos pipePos, net.minecraft.world.item.Item item, int amount, @Nullable BlockPos excludePos, boolean simulate) {
        if (level == null || item == null || amount <= 0) return ItemStack.EMPTY;
        int needed = amount;
        ItemStack result = ItemStack.EMPTY;

        PipeNetworkScan scan = scanNetwork(level, pipePos);

        // 1. Containers connected to EXTRACT modules in the network
        for (ExtractSource src : scan.extractSources) {
            if (src.container == null) continue;
            if (excludePos != null && excludePos.equals(src.pos)) continue;
            if (src.hasFilters() && !src.matches(new ItemStack(item), level.registryAccess())) continue;

            ItemStack extracted = extractFromContainer(src.container, src.side, new ItemStack(item), needed, simulate);
            if (!extracted.isEmpty()) {
                if (result.isEmpty()) result = extracted.copy();
                else result.grow(extracted.getCount());
                needed -= extracted.getCount();
                if (needed <= 0) return result;
            }
        }

        // 2. Storage Disks / Controller
        if (scan.controller != null) {
            ItemStack extracted = scan.controller.extractItemExcluding(item, needed, excludePos, simulate);
            if (!extracted.isEmpty()) {
                if (result.isEmpty()) result = extracted.copy();
                else result.grow(extracted.getCount());
                needed -= extracted.getCount();
                if (needed <= 0) return result;
            }
        } else {
            for (VolcaniteStorageDriveBlockEntity drive : scan.connectedDrives) {
                if (excludePos != null && excludePos.equals(drive.getBlockPos())) continue;
                ItemStack ext = drive.extractItem(item, needed, simulate);
                if (!ext.isEmpty()) {
                    if (result.isEmpty()) result = ext.copy();
                    else result.grow(ext.getCount());
                    needed -= ext.getCount();
                    if (needed <= 0) return result;
                }
            }
        }

        // 3. Storage Units (Grand Chests, Modular Chests, Volcanite Chests)
        if (scan.controller == null) {
            for (VolcaniteStorageUnitBlockEntity unit : scan.connectedUnits) {
                if (excludePos != null && excludePos.equals(unit.getBlockPos())) continue;
                ItemStack ext = unit.extractItem(item, needed, simulate);
                if (!ext.isEmpty()) {
                    if (result.isEmpty()) result = ext.copy();
                    else result.grow(ext.getCount());
                    needed -= ext.getCount();
                    if (needed <= 0) return result;
                }
            }
            for (VolcaniteModularChestBlockEntity modularChest : scan.connectedModularChests) {
                if (excludePos != null && excludePos.equals(modularChest.getBlockPos())) continue;
                ItemStack ext = modularChest.extractItem(item, needed, simulate);
                if (!ext.isEmpty()) {
                    if (result.isEmpty()) result = ext.copy();
                    else result.grow(ext.getCount());
                    needed -= ext.getCount();
                    if (needed <= 0) return result;
                }
            }
            for (VolcaniteChestBlockEntity volcaniteChest : scan.connectedVolcaniteChests) {
                if (excludePos != null && excludePos.equals(volcaniteChest.getBlockPos())) continue;
                ItemStack ext = volcaniteChest.extractItem(item, needed, simulate);
                if (!ext.isEmpty()) {
                    if (result.isEmpty()) result = ext.copy();
                    else result.grow(ext.getCount());
                    needed -= ext.getCount();
                    if (needed <= 0) return result;
                }
            }
        }

        return result;
    }

    public static ItemStack extractFirstAcceptableItemFromNetwork(Level level, BlockPos pipePos, Container targetContainer, Direction targetSide, int maxAmount) {
        return extractFirstAcceptableItemFromNetwork(level, pipePos, targetContainer, null, targetSide, maxAmount, null);
    }

    public static ItemStack extractFirstAcceptableItemFromNetwork(Level level, BlockPos pipePos, Container targetContainer, Direction targetSide, int maxAmount, @Nullable java.util.function.Predicate<ItemStack> filterPredicate) {
        return extractFirstAcceptableItemFromNetwork(level, pipePos, targetContainer, null, targetSide, maxAmount, filterPredicate);
    }

    public static ItemStack extractFirstAcceptableItemFromNetwork(Level level, BlockPos pipePos, Container targetContainer, @Nullable BlockPos targetPos, Direction targetSide, int maxAmount, @Nullable java.util.function.Predicate<ItemStack> filterPredicate) {
        if (level == null || targetContainer == null || maxAmount <= 0) return ItemStack.EMPTY;
        PipeNetworkScan scan = scanNetwork(level, pipePos);

        // 1. Check containers connected to EXTRACT modules
        for (ExtractSource src : scan.extractSources) {
            if (src.container == null || src.container == targetContainer) continue;
            if (targetPos != null && targetPos.equals(src.pos)) continue;
            int size = src.container.getContainerSize();

            for (int slot = 0; slot < size; slot++) {
                ItemStack inSlot = src.container.getItem(slot);
                if (!inSlot.isEmpty()) {
                    if (!isOutputSlot(src.container, slot, inSlot, src.side)) continue;
                    if (src.hasFilters() && !src.matches(inSlot, level.registryAccess())) continue;
                    if (filterPredicate != null && !filterPredicate.test(inSlot)) continue;

                    int canAccept = insertIntoContainer(targetContainer, targetSide, inSlot.copyWithCount(Math.min(inSlot.getCount(), maxAmount)), true);
                    if (canAccept > 0) {
                        ItemStack extracted = src.container.removeItem(slot, canAccept);
                        src.container.setChanged();
                        return extracted;
                    }
                }
            }
        }

        // 2. Check Storage Disks or Controller
        if (scan.controller != null) {
            for (Map.Entry<net.minecraft.world.item.Item, Integer> entry : scan.controller.getUnifiedInventory().entrySet()) {
                if (entry.getValue() > 0) {
                    ItemStack testStack = new ItemStack(entry.getKey(), Math.min(entry.getValue(), maxAmount));
                    if (filterPredicate != null && !filterPredicate.test(testStack)) continue;
                    int canAccept = insertIntoContainer(targetContainer, targetSide, testStack, true);
                    if (canAccept > 0) {
                        ItemStack extracted = scan.controller.extractItemExcluding(entry.getKey(), canAccept, targetPos, false);
                        if (!extracted.isEmpty()) return extracted;
                    }
                }
            }
        } else {
            for (VolcaniteStorageDriveBlockEntity drive : scan.connectedDrives) {
                for (int slot = 0; slot < drive.getContainerSize(); slot++) {
                    ItemStack disk = drive.getItem(slot);
                    if (disk.getItem() instanceof net.valentin.obsidium.item.VolcaniteStorageDiskItem) {
                        for (net.valentin.obsidium.item.VolcaniteStorageDiskItem.StoredEntry entry : net.valentin.obsidium.item.VolcaniteStorageDiskItem.getStoredEntries(disk)) {
                            if (entry.count > 0) {
                                ItemStack testStack = new ItemStack(entry.item, Math.min(entry.count, maxAmount));
                                if (filterPredicate != null && !filterPredicate.test(testStack)) continue;
                                int canAccept = insertIntoContainer(targetContainer, targetSide, testStack, true);
                                if (canAccept > 0) {
                                    return drive.extractItem(entry.item, canAccept, false);
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Check Storage Units (Grand Chests & Modular Chests)
        if (scan.controller == null) {
            for (VolcaniteStorageUnitBlockEntity unit : scan.connectedUnits) {
                if (unit == targetContainer || (targetPos != null && targetPos.equals(unit.getBlockPos()))) continue;
                for (int i = 0; i < unit.getContainerSize(); i++) {
                    ItemStack inSlot = unit.getItem(i);
                    if (!inSlot.isEmpty()) {
                        if (filterPredicate != null && !filterPredicate.test(inSlot)) continue;
                        int canAccept = insertIntoContainer(targetContainer, targetSide, inSlot.copyWithCount(Math.min(inSlot.getCount(), maxAmount)), true);
                        if (canAccept > 0) {
                            ItemStack extracted = unit.removeItem(i, canAccept);
                            unit.setChanged();
                            return extracted;
                        }
                    }
                }
            }
            for (VolcaniteModularChestBlockEntity modularChest : scan.connectedModularChests) {
                if (modularChest == targetContainer || (targetPos != null && targetPos.equals(modularChest.getBlockPos()))) continue;
                int limit = modularChest.getUnlockedSlots();
                for (int i = 0; i < limit; i++) {
                    ItemStack inSlot = modularChest.getItem(i);
                    if (!inSlot.isEmpty()) {
                        if (filterPredicate != null && !filterPredicate.test(inSlot)) continue;
                        int canAccept = insertIntoContainer(targetContainer, targetSide, inSlot.copyWithCount(Math.min(inSlot.getCount(), maxAmount)), true);
                        if (canAccept > 0) {
                            ItemStack extracted = modularChest.removeItem(i, canAccept);
                            modularChest.setChanged();
                            return extracted;
                        }
                    }
                }
            }
        }

        return ItemStack.EMPTY;
    }

    public static boolean isExternalContainer(Level level, BlockPos pos, BlockEntity be) {
        if (be instanceof VolcaniteStoragePipeBlockEntity) return false;
        if (be instanceof VolcaniteStorageControllerBlockEntity ||
            be instanceof VolcaniteStorageDriveBlockEntity) {
            return false;
        }
        if (be instanceof LiquidTankBlockEntity) return true;
        if (be instanceof Container) return true;
        if (be instanceof InventoryHolder) return true;
        if (level != null && pos != null) {
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            if (PipeConnectionHelper.isLogisticsPipe(block)) {
                return false;
            }
            if (PipeConnectionHelper.canConnect(level, pos, null)) return true;
            for (Direction d : Direction.values()) {
                if (FluidStorage.SIDED.find(level, pos, d) != null) return true;
                if (net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(level, pos, d) != null) return true;
                if (team.reborn.energy.api.EnergyStorage.SIDED.find(level, pos, d) != null) return true;
            }
        }
        return false;
    }

    private VolcaniteStorageControllerBlockEntity cachedController = null;
    private long controllerCacheTick = -100;
    private static final int CONTROLLER_CACHE_TTL = 40; // Cache valid controller lookup for 2s (40 ticks)

    public void invalidateControllerCache() {
        this.cachedController = null;
        this.controllerCacheTick = -100;
    }

    public VolcaniteStorageControllerBlockEntity findConnectedController() {
        if (level == null) return null;
        long currentTick = level.getGameTime();
        if (cachedController != null && !cachedController.isRemoved() && cachedController.getLevel() == level) {
            if (currentTick - controllerCacheTick < CONTROLLER_CACHE_TTL) {
                return cachedController;
            }
        } else if (cachedController == null && currentTick - controllerCacheTick < 10) {
            return null;
        }
        controllerCacheTick = currentTick;
        cachedController = scanForController();
        return cachedController;
    }

    private VolcaniteStorageControllerBlockEntity scanForController() {
        if (level == null || !level.isLoaded(worldPosition)) return null;
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
                    if (s.getBlock() instanceof VolcaniteStorageControllerBlock) {
                        BlockEntity be = level.getBlockEntity(next);
                        if (be instanceof VolcaniteStorageControllerBlockEntity c) return c;
                    } else if (s.getBlock() instanceof VolcaniteNetworkNodeReceiverBlock) {
                        BlockEntity be = level.getBlockEntity(next);
                        if (be instanceof VolcaniteNetworkNodeReceiverBlockEntity receiver) {
                            VolcaniteStorageControllerBlockEntity ctrl = receiver.getConnectedController();
                            if (ctrl != null) return ctrl;
                            BlockPos ePos = receiver.getLinkedEmitterPos();
                            if (ePos != null && level.isLoaded(ePos) && visited.add(ePos)) queue.add(ePos);
                        }
                        queue.add(next);
                    } else if (s.getBlock() instanceof VolcaniteNetworkNodeEmitterBlock) {
                        BlockEntity be = level.getBlockEntity(next);
                        if (be instanceof VolcaniteNetworkNodeEmitterBlockEntity emitter) {
                            VolcaniteStorageControllerBlockEntity ctrl = emitter.getConnectedController();
                            if (ctrl != null) return ctrl;
                            for (BlockPos rPos : emitter.getLinkedReceivers()) {
                                if (level.isLoaded(rPos) && visited.add(rPos)) queue.add(rPos);
                            }
                        }
                        queue.add(next);
                    } else if (PipeConnectionHelper.isLogisticsPipe(s.getBlock()) ||
                               PipeConnectionHelper.isStorageNetworkBlock(s.getBlock())) {
                        queue.add(next);
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        byte[] modBytes = new byte[6];
        byte[] typeBytes = new byte[6];
        byte[] discBytes = new byte[6];
        byte[] valveBytes = new byte[6];
        byte[] blBytes = new byte[6];
        byte[] prioBytes = new byte[6];
        byte[] rrBytes = new byte[6];
        byte[] rsBytes = new byte[6];
        for (int i = 0; i < 6; i++) {
            modBytes[i] = (byte) modules[i].getId();
            typeBytes[i] = (byte) transferTypes[i];
            discBytes[i] = (byte) (disconnectedSides[i] ? 1 : 0);
            valveBytes[i] = (byte) (isValveModule[i] ? 1 : 0);
            blBytes[i] = (byte) (filterBlacklist[i] ? 1 : 0);
            prioBytes[i] = (byte) priorities[i];
            rrBytes[i] = (byte) (roundRobin[i] ? 1 : 0);
            rsBytes[i] = (byte) redstoneModes[i];
        }
        tag.putByteArray("Modules", modBytes);
        tag.putByteArray("TransferTypes", typeBytes);
        tag.putByteArray("DisconnectedSides", discBytes);
        tag.putByteArray("ValveModules", valveBytes);
        tag.putByteArray("FilterBlacklist", blBytes);
        tag.putByteArray("Priorities", prioBytes);
        tag.putByteArray("RoundRobin", rrBytes);
        tag.putByteArray("RedstoneModes", rsBytes);
        ContainerHelper.saveAllItems(tag, this.filterItems, provider);

        tag.putIntArray("EnergyUpgradeTiers", energyUpgradeTiers);
        CompoundTag energyTag = new CompoundTag();
        ContainerHelper.saveAllItems(energyTag, this.energyUpgradeItems, provider);
        tag.put("EnergyUpgrades", energyTag);
        if (facadeBlock != null) {
            tag.putString("FacadeBlock", net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(facadeBlock).toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("Modules")) {
            byte[] modBytes = tag.getByteArray("Modules");
            for (int i = 0; i < Math.min(6, modBytes.length); i++) {
                modules[i] = ModuleType.byId(modBytes[i]);
            }
        }
        if (tag.contains("TransferTypes")) {
            byte[] typeBytes = tag.getByteArray("TransferTypes");
            for (int i = 0; i < Math.min(6, typeBytes.length); i++) {
                transferTypes[i] = typeBytes[i];
            }
        }
        if (tag.contains("DisconnectedSides")) {
            byte[] discBytes = tag.getByteArray("DisconnectedSides");
            for (int i = 0; i < Math.min(6, discBytes.length); i++) {
                disconnectedSides[i] = (discBytes[i] == 1);
            }
        }
        if (tag.contains("ValveModules")) {
            byte[] valveBytes = tag.getByteArray("ValveModules");
            for (int i = 0; i < Math.min(6, valveBytes.length); i++) {
                isValveModule[i] = (valveBytes[i] == 1);
            }
        }
        if (tag.contains("FilterBlacklist")) {
            byte[] blBytes = tag.getByteArray("FilterBlacklist");
            for (int i = 0; i < Math.min(6, blBytes.length); i++) {
                filterBlacklist[i] = (blBytes[i] == 1);
            }
        }
        if (tag.contains("Priorities")) {
            byte[] prioBytes = tag.getByteArray("Priorities");
            for (int i = 0; i < Math.min(6, prioBytes.length); i++) {
                priorities[i] = Math.max(-5, Math.min(5, prioBytes[i]));
            }
        }
        if (tag.contains("RoundRobin")) {
            byte[] rrBytes = tag.getByteArray("RoundRobin");
            for (int i = 0; i < Math.min(6, rrBytes.length); i++) {
                roundRobin[i] = (rrBytes[i] == 1);
            }
        }
        if (tag.contains("RedstoneModes")) {
            byte[] rsBytes = tag.getByteArray("RedstoneModes");
            for (int i = 0; i < Math.min(6, rsBytes.length); i++) {
                redstoneModes[i] = ((rsBytes[i] % 3) + 3) % 3;
            }
        }
        ContainerHelper.loadAllItems(tag, this.filterItems, provider);

        if (tag.contains("EnergyUpgradeTiers")) {
            int[] tiers = tag.getIntArray("EnergyUpgradeTiers");
            for (int i = 0; i < Math.min(6, tiers.length); i++) {
                energyUpgradeTiers[i] = tiers[i];
            }
        }
        if (tag.contains("EnergyUpgrades")) {
            ContainerHelper.loadAllItems(tag.getCompound("EnergyUpgrades"), this.energyUpgradeItems, provider);
        }
        if (tag.contains("FacadeBlock")) {
            facadeBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse(tag.getString("FacadeBlock")));
        } else {
            facadeBlock = null;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, provider);
        return tag;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Pipe Module: " + selectedFaceForMenu.getName().toUpperCase());
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        invalidateControllerCache();
        return new VolcanitePipeModuleMenu(syncId, playerInventory, this, selectedFaceForMenu);
    }
}
