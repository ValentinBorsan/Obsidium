package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.valentin.obsidium.fluid.TickTransferBudget;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.VolcaniteBottleItem;
import net.valentin.obsidium.storage.QuantumLinkNetworkState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

public class NaiadFluidPipeBlockEntity extends BlockEntity implements PipeFacadeHolder, PipeConfigurable, MenuProvider {
    public enum ValveMode {
        NONE,
        EXTRACT,
        INSERT;

        public ValveMode cycle() {
            if (this == EXTRACT) return INSERT;
            return EXTRACT;
        }

        public static ValveMode fromOrdinal(int ord) {
            ValveMode[] values = values();
            if (ord >= 0 && ord < values.length) {
                return values[ord];
            }
            return NONE;
        }
    }

    private static final int MAX_NETWORK_SCAN = 512;
    private static final ThreadLocal<Set<BlockPos>> ACTIVE_ROUTING = ThreadLocal.withInitial(HashSet::new);

    // Only pre-upgrade fluid is stored here; new fluid is routed directly.
    private final SingleFluidStorage legacyFluid = new SingleFluidStorage() {
        @Override protected long getCapacity(FluidVariant variant) { return Math.max(162_000L, amount); }
        @Override protected void onFinalCommit() { setChanged(); }
    };

    private final TickTransferBudget transferBudget = new TickTransferBudget();
    private static long available(FluidConsumerEntry entry, Level level) {
        long available = entry.maxRate();
        for (var pipe : entry.path()) available = Math.min(available,
                pipe.transferBudget.remaining(level.getGameTime(), pipe.getEffectiveTransferRate()));
        return available;
    }
    private static void consume(FluidConsumerEntry entry, long amount, TransactionContext transaction) {
        for (var pipe : entry.path()) pipe.transferBudget.consume(amount, transaction);
    }

    private record PipeNode(BlockPos pos, long pathMaxRate, List<NaiadFluidPipeBlockEntity> path) {}
    private record FluidConsumerEntry(Storage<FluidVariant> storage, long maxRate, FluidVariant filterFluid, List<NaiadFluidPipeBlockEntity> path) {}
    private record FluidSourceEntry(Storage<FluidVariant> storage, @Nullable BlockPos excludePos) {}

    private final ValveMode[] valves = new ValveMode[6];
    private final boolean[] hasValveModule = new boolean[6];
    private final boolean[] disconnected = new boolean[6];
    private net.minecraft.world.level.block.Block facadeBlock = null;

    private int pipeColor = -1; // -1 = default, 0..15 = DyeColor
    private int redstoneMode = REDSTONE_ALWAYS;
    private boolean roundRobin = false;
    private int throughputPercent = 100;
    private int rrIndex = 0;

    private boolean quantumLinked = false;
    private DyeColor quantumColor = DyeColor.PURPLE;
    private int quantumChannel = 1;
    private boolean quantumPrivate = false;
    private @Nullable UUID quantumOwner = null;

    private final SimpleContainer filterContainer = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            NaiadFluidPipeBlockEntity.this.setChanged();
        }
    };

    @Override public int getPipeKind() { return KIND_FLUID; }
    @Override public int getPipeColor() { return pipeColor; }
    @Override public void setPipeColor(int color) {
        int clamped = (color >= 0 && color < 16) ? color : -1;
        if (this.pipeColor != clamped) {
            this.pipeColor = clamped;
            setChanged();
            if (level != null && !level.isClientSide()) {
                BlockState st = getBlockState();
                if (st.getBlock() instanceof NaiadFluidPipeBlock pipeBlock) {
                    BlockState newSt = pipeBlock.makeConnections(level, worldPosition)
                            .setValue(NaiadFluidPipeBlock.WATERLOGGED, st.getValue(NaiadFluidPipeBlock.WATERLOGGED));
                    level.setBlock(worldPosition, newSt, 3);
                }
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }
    @Override public int getRedstoneMode() { return redstoneMode; }
    @Override public void setRedstoneMode(int mode) { this.redstoneMode = Math.floorMod(mode, 4); setChanged(); }
    @Override public boolean isRoundRobin() { return roundRobin; }
    @Override public void setRoundRobin(boolean rr) { this.roundRobin = rr; setChanged(); }
    @Override public int getThroughputPercent() { return throughputPercent; }
    @Override public void setThroughputPercent(int pct) { this.throughputPercent = pct; setChanged(); }
    @Override public int getFaceModeOrdinal(Direction dir) { return getValveMode(dir).ordinal(); }
    @Override public void cycleFaceMode(Direction dir) { cycleValveModeWithNone(dir); }
    @Override public boolean isQuantumLinked() { return quantumLinked; }
    public void setQuantumLinked(boolean linked, @Nullable UUID owner) {
        this.quantumLinked = linked;
        if (owner != null) this.quantumOwner = owner;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    @Override public DyeColor getQuantumColor() { return quantumColor; }
    @Override public void setQuantumColor(DyeColor color) { if (color != null) { this.quantumColor = color; setChanged(); } }
    @Override public int getQuantumChannel() { return quantumChannel; }
    @Override public void setQuantumChannel(int ch) { this.quantumChannel = QuantumLinkNetworkState.clampChannel(ch); setChanged(); }
    @Override public boolean isQuantumPrivate() { return quantumPrivate; }
    @Override public @Nullable UUID getQuantumOwner() { return quantumOwner; }
    @Override public void setQuantumPrivate(boolean priv) { this.quantumPrivate = priv; setChanged(); }
    @Override public void ensureQuantumOwner(@Nullable UUID owner) { if (this.quantumOwner == null && owner != null) { this.quantumOwner = owner; setChanged(); } }
    @Override public Container getFilterContainer() { return filterContainer; }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new PipeConfigMenu(syncId, playerInventory, this);
    }

    public FluidVariant getFilteredFluid() {
        ItemStack stack = filterContainer.getItem(0);
        if (stack.isEmpty()) return FluidVariant.blank();
        if (stack.is(Items.WATER_BUCKET)) return FluidVariant.of(Fluids.WATER);
        if (stack.is(Items.LAVA_BUCKET)) return FluidVariant.of(Fluids.LAVA);
        if (stack.is(ObsidiumMod.PYROCLASTIC_DISTILLATE_BUCKET)) return FluidVariant.of(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID);
        if (stack.is(ObsidiumMod.STEAM_BUCKET)) return FluidVariant.of(ObsidiumMod.STEAM_FLUID);
        if (stack.getItem() instanceof VolcaniteBottleItem) {
            return VolcaniteBottleItem.getFluid(stack);
        }
        return FluidVariant.blank();
    }

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

    public NaiadFluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.NAIAD_FLUID_PIPE_BE, pos, state);
        Arrays.fill(valves, ValveMode.NONE);
        Arrays.fill(disconnected, false);
    }

    public ValveMode getValveMode(Direction dir) {
        if (dir == null) return ValveMode.NONE;
        return valves[dir.get3DDataValue()];
    }

    public void setValveMode(Direction dir, ValveMode mode) {
        if (dir == null) return;
        valves[dir.get3DDataValue()] = mode;
        // Installed modules are physical items; changing their mode must not discard them.
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void setHasValveModule(Direction dir, boolean mounted) {
        if (dir != null) {
            hasValveModule[dir.get3DDataValue()] = mounted;
            setChanged();
        }
    }

    public ValveMode cycleValveMode(Direction dir) {
        if (dir == null) return ValveMode.NONE;
        ValveMode current = getValveMode(dir);
        ValveMode next = current.cycle();
        setValveMode(dir, next);
        return next;
    }

    public boolean isSideDisconnected(Direction dir) {
        if (dir == null) return false;
        return disconnected[dir.get3DDataValue()];
    }

    public void setSideDisconnected(Direction dir, boolean disc) {
        if (dir == null) return;
        disconnected[dir.get3DDataValue()] = disc;
        if (disc && valves[dir.get3DDataValue()] != ValveMode.NONE) {
            dismantleValve(dir);
        }
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            if (state.getBlock() instanceof NaiadFluidPipeBlock pipeBlock) {
                BlockState newState = pipeBlock.makeConnections(level, worldPosition)
                        .setValue(NaiadFluidPipeBlock.WATERLOGGED, state.getValue(NaiadFluidPipeBlock.WATERLOGGED));
                level.setBlock(worldPosition, newState, 3);
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

    public boolean dismantleValve(Direction dir) {
        if (dir == null) return false;
        int idx = dir.get3DDataValue();
        if (valves[idx] != ValveMode.NONE || hasValveModule[idx]) {
            boolean dropModule = hasValveModule[idx];
            valves[idx] = ValveMode.NONE;
            hasValveModule[idx] = false;
            if (level != null && !level.isClientSide()) {
                if (dropModule) {
                    Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                            new ItemStack(ObsidiumMod.VOLCANITE_LOGISTICS_MODULE));
                }
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
            setChanged();
            return true;
        }
        return false;
    }

    public void dropAllValves() {
        if (level == null || level.isClientSide()) return;
        for (int i = 0; i < 6; i++) {
            if (valves[i] != ValveMode.NONE || hasValveModule[i]) {
                if (hasValveModule[i]) {
                    Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                            new ItemStack(ObsidiumMod.VOLCANITE_LOGISTICS_MODULE));
                }
                valves[i] = ValveMode.NONE;
                hasValveModule[i] = false;
            }
        }
    }

    public ValveMode cycleValveModeWithNone(Direction dir) {
        if (dir == null) return ValveMode.NONE;
        ValveMode current = getValveMode(dir);
        ValveMode next = switch (current) {
            case NONE -> ValveMode.EXTRACT;
            case EXTRACT -> ValveMode.INSERT;
            case INSERT -> ValveMode.NONE;
        };
        setValveMode(dir, next);
        return next;
    }

    public long getEffectiveTransferRate() {
        long tierMb = (getBlockState().getBlock() instanceof NaiadFluidPipeBlock pipeBlock) ? pipeBlock.getTransferRateMb() : 250L;
        long effectiveMb = Math.max(1L, (tierMb * throughputPercent) / 100L);
        return effectiveMb * 81L; // 1 mB = 81 droplets
    }

    private boolean hasInsertValve() {
        for (ValveMode m : valves) {
            if (m == ValveMode.INSERT) return true;
        }
        return false;
    }

    public Storage<FluidVariant> getFluidStorage(Direction dir) {
        if (dir != null && isSideDisconnected(dir)) {
            return null;
        }
        ValveMode mode = dir != null ? getValveMode(dir) : ValveMode.NONE;
        return new Storage<>() {
            @Override
            public boolean supportsInsertion() {
                return mode != ValveMode.INSERT;
            }

            @Override
            public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
                if (mode == ValveMode.INSERT || resource.isBlank() || maxAmount <= 0L) return 0L;
                if (level == null || level.isClientSide() || !isRedstoneAllowed(level, worldPosition)) return 0L;
                FluidVariant selfFilter = getFilteredFluid();
                if (!selfFilter.isBlank() && !selfFilter.equals(resource)) return 0L;

                Set<BlockPos> active = ACTIVE_ROUTING.get();
                if (!active.add(worldPosition)) return 0L;
                try {
                    long toSend = Math.min(maxAmount, getEffectiveTransferRate());
                    BlockPos excludePos = dir != null ? worldPosition.relative(dir) : null;
                    List<FluidConsumerEntry> consumers = collectFluidConsumers(level, worldPosition, excludePos, resource);
                    if (consumers.isEmpty()) return 0L;

                    if (roundRobin && consumers.size() > 1) {
                        int offset = Math.floorMod(rrIndex, consumers.size());
                        if (offset > 0) {
                            List<FluidConsumerEntry> rotated = new ArrayList<>(consumers.size());
                            rotated.addAll(consumers.subList(offset, consumers.size()));
                            rotated.addAll(consumers.subList(0, offset));
                            consumers = rotated;
                        }
                    }

                    long totalInserted = 0L;
                    for (FluidConsumerEntry c : consumers) {
                        if (!c.filterFluid().isBlank() && !c.filterFluid().equals(resource)) continue;
                        long allowed = Math.min(toSend - totalInserted, available(c, level));
                        if (allowed <= 0L) continue;
                        long moved = c.storage().insert(resource, allowed, transaction);
                        consume(c, moved, transaction);
                        totalInserted += moved;
                        if (totalInserted >= toSend) break;
                    }
                    if (roundRobin && totalInserted > 0L) {
                        rrIndex = (rrIndex + 1) % consumers.size();
                    }
                    return totalInserted;
                } finally {
                    active.remove(worldPosition);
                }
            }

            @Override
            public boolean supportsExtraction() {
                return legacyFluid.amount > 0L;
            }

            @Override
            public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
                return extractLegacyFluid(resource, maxAmount, transaction);
            }

            @Override
            public Iterator<StorageView<FluidVariant>> iterator() {
                if (legacyFluid.amount <= 0L) return Collections.emptyIterator();
                return List.<StorageView<FluidVariant>>of(new StorageView<>() {
                    @Override public long extract(FluidVariant fluid, long amount, TransactionContext transaction) {
                        return extractLegacyFluid(fluid, amount, transaction);
                    }
                    @Override public boolean isResourceBlank() { return legacyFluid.isResourceBlank(); }
                    @Override public FluidVariant getResource() { return legacyFluid.variant; }
                    @Override public long getAmount() { return legacyFluid.amount; }
                    @Override public long getCapacity() { return legacyFluid.amount; }
                }).iterator();
            }
        };
    }

    private long extractLegacyFluid(FluidVariant resource, long amount, TransactionContext transaction) {
        if (level == null || !isRedstoneAllowed(level, worldPosition)) return 0L;
        long allowed = Math.min(amount, transferBudget.remaining(level.getGameTime(), getEffectiveTransferRate()));
        long moved = legacyFluid.extract(resource, allowed, transaction);
        transferBudget.consume(moved, transaction);
        return moved;
    }

    private static Storage<FluidVariant> createQuantumFluidInsertStorage(QuantumLinkNetworkState netState, String qKey) {
        return netState.getFluidStorage(qKey);
    }

    private static Storage<FluidVariant> createQuantumFluidExtractStorage(QuantumLinkNetworkState netState, String qKey) {
        return netState.getFluidStorage(qKey);
    }

    private List<FluidConsumerEntry> collectFluidConsumers(Level level, BlockPos startPos, @Nullable BlockPos excludePos, @Nullable FluidVariant requiredFluid) {
        List<FluidConsumerEntry> consumers = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<PipeNode> queue = new ArrayDeque<>();
        visited.add(startPos);
        queue.add(new PipeNode(startPos, getEffectiveTransferRate(), List.of(this)));
        QuantumLinkNetworkState netState = QuantumLinkNetworkState.get(level);

        while (!queue.isEmpty()) {
            PipeNode node = queue.poll();
            BlockEntity be = level.getBlockEntity(node.pos());
            if (!(be instanceof NaiadFluidPipeBlockEntity pipe)) continue;
            if (!pipe.isRedstoneAllowed(level, node.pos())) continue;

            FluidVariant pipeFilter = pipe.getFilteredFluid();
            if (requiredFluid != null && !requiredFluid.isBlank() && !pipeFilter.isBlank() && !pipeFilter.equals(requiredFluid)) {
                continue;
            }

            if (pipe.quantumLinked && netState != null && !pipe.hasInsertValve()) {
                String qKey = QuantumLinkNetworkState.buildChannelKey(pipe.quantumPrivate, pipe.quantumOwner, pipe.quantumColor, pipe.quantumChannel);
                consumers.add(new FluidConsumerEntry(createQuantumFluidInsertStorage(netState, qKey), node.pathMaxRate(), pipeFilter, node.path()));
            }

            for (Direction dir : Direction.values()) {
                if (pipe.isSideDisconnected(dir)) continue;
                BlockPos np = node.pos().relative(dir);
                if (!level.isLoaded(np)) continue;

                BlockEntity nbe = level.getBlockEntity(np);
                if (nbe instanceof NaiadFluidPipeBlockEntity otherPipe) {
                    if (!otherPipe.isSideDisconnected(dir.getOpposite()) && pipe.pipeColor == otherPipe.pipeColor) {
                        if (visited.size() < MAX_NETWORK_SCAN && visited.add(np)) {
                            long nextRate = Math.min(node.pathMaxRate(), otherPipe.getEffectiveTransferRate());
                            var path = new ArrayList<>(node.path());
                            path.add(otherPipe);
                            queue.add(new PipeNode(np, nextRate, path));
                        }
                    }
                    continue;
                }

                if (excludePos != null && np.equals(excludePos)) continue;
                ValveMode mode = pipe.getValveMode(dir);
                if (mode == ValveMode.EXTRACT) continue;

                Storage<FluidVariant> target = FluidStorage.SIDED.find(level, np, dir.getOpposite());
                if (target != null && target.supportsInsertion()) {
                    consumers.add(new FluidConsumerEntry(target, node.pathMaxRate(), pipeFilter, node.path()));
                }
            }
        }
        return consumers;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, NaiadFluidPipeBlockEntity entity) {
        if (level.isClientSide()) return;
        if (!entity.isRedstoneAllowed(level, pos)) return;

        List<FluidSourceEntry> sources = new ArrayList<>();
        if (entity.legacyFluid.amount > 0L) sources.add(new FluidSourceEntry(entity.legacyFluid, null));
        for (Direction dir : Direction.values()) {
            if (entity.isSideDisconnected(dir)) continue;
            if (entity.getValveMode(dir) != ValveMode.EXTRACT) continue;

            BlockPos np = pos.relative(dir);
            if (!level.isLoaded(np)) continue;
            BlockEntity nbe = level.getBlockEntity(np);
            if (nbe instanceof NaiadFluidPipeBlockEntity) continue;

            Storage<FluidVariant> targetStorage = FluidStorage.SIDED.find(level, np, dir.getOpposite());
            if (targetStorage != null && targetStorage.supportsExtraction()) {
                sources.add(new FluidSourceEntry(targetStorage, np));
            }
        }

        if (entity.quantumLinked && entity.hasInsertValve()) {
            QuantumLinkNetworkState netState = QuantumLinkNetworkState.get(level);
            if (netState != null) {
                String qKey = QuantumLinkNetworkState.buildChannelKey(entity.quantumPrivate, entity.quantumOwner, entity.quantumColor, entity.quantumChannel);
                sources.add(new FluidSourceEntry(createQuantumFluidExtractStorage(netState, qKey), null));
            }
        }

        if (sources.isEmpty()) return;

        Set<BlockPos> active = ACTIVE_ROUTING.get();
        if (!active.add(pos)) return;
        boolean didSomething = false;
        try {
            FluidVariant selfFilter = entity.getFilteredFluid();
            long pipeMax = entity.getEffectiveTransferRate();

            for (FluidSourceEntry src : sources) {
                List<FluidConsumerEntry> consumers = entity.collectFluidConsumers(level, pos, src.excludePos(), null);
                if (consumers.isEmpty()) continue;

                if (entity.roundRobin && consumers.size() > 1) {
                    int offset = Math.floorMod(entity.rrIndex, consumers.size());
                    if (offset > 0) {
                        List<FluidConsumerEntry> rotated = new ArrayList<>(consumers.size());
                        rotated.addAll(consumers.subList(offset, consumers.size()));
                        rotated.addAll(consumers.subList(0, offset));
                        consumers = rotated;
                    }
                }

                long remaining = pipeMax;
                for (FluidConsumerEntry c : consumers) {
                    long toMove = Math.min(remaining, available(c, level));
                    if (toMove <= 0L) continue;
                    long moved;
                    try (Transaction transaction = Transaction.openOuter()) {
                        moved = StorageUtil.move(src.storage(), c.storage(),
                                fluid -> !fluid.isBlank()
                                        && (selfFilter.isBlank() || selfFilter.equals(fluid))
                                        && (c.filterFluid().isBlank() || c.filterFluid().equals(fluid)),
                                toMove, transaction);
                        consume(c, moved, transaction);
                        if (moved > 0L) transaction.commit();
                    }
                    if (moved > 0L) {
                        remaining -= moved;
                        didSomething = true;
                    }
                    if (remaining <= 0L) break;
                }

                if (entity.roundRobin) {
                    entity.rrIndex = (entity.rrIndex + 1) % consumers.size();
                }
            }
        } finally {
            active.remove(pos);
        }

        if (didSomething) {
            entity.setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag legacy = new CompoundTag();
        legacyFluid.writeNbt(legacy, registries);
        tag.put("Fluid", legacy);

        byte[] valveBytes = new byte[6];
        byte[] discBytes = new byte[6];
        for (int i = 0; i < 6; i++) {
            valveBytes[i] = (byte) valves[i].ordinal();
            discBytes[i] = (byte) (disconnected[i] ? 1 : 0);
        }
        tag.putByteArray("Valves", valveBytes);
        byte[] modules = new byte[6];
        for (int i = 0; i < 6; i++) modules[i] = (byte) (hasValveModule[i] ? 1 : 0);
        tag.putByteArray("ValveModules", modules);
        tag.putByteArray("Disconnected", discBytes);
        if (facadeBlock != null) {
            tag.putString("FacadeBlock", net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(facadeBlock).toString());
        }
        tag.putInt("PipeColor", pipeColor);
        tag.putInt("RedstoneMode", redstoneMode);
        tag.putBoolean("RoundRobin", roundRobin);
        tag.putInt("Throughput", throughputPercent);
        tag.putBoolean("QuantumLinked", quantumLinked);
        tag.putInt("QuantumColor", quantumColor.getId());
        tag.putInt("QuantumChannel", quantumChannel);
        tag.putBoolean("QuantumPrivate", quantumPrivate);
        if (quantumOwner != null) tag.putUUID("QuantumOwner", quantumOwner);
        ContainerHelper.saveAllItems(tag, filterContainer.getItems(), registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Fluid")) legacyFluid.readNbt(tag.getCompound("Fluid"), registries);
        if (tag.contains("Valves")) {
            byte[] valveBytes = tag.getByteArray("Valves");
            for (int i = 0; i < Math.min(6, valveBytes.length); i++) {
                valves[i] = ValveMode.fromOrdinal(valveBytes[i]);
            }
        }
        Arrays.fill(hasValveModule, false);
        byte[] modules = tag.getByteArray("ValveModules");
        for (int i = 0; i < Math.min(6, modules.length); i++) hasValveModule[i] = modules[i] != 0;
        if (tag.contains("Disconnected")) {
            byte[] discBytes = tag.getByteArray("Disconnected");
            for (int i = 0; i < Math.min(6, discBytes.length); i++) {
                disconnected[i] = discBytes[i] == 1;
            }
        }
        if (tag.contains("FacadeBlock")) {
            facadeBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse(tag.getString("FacadeBlock")));
        } else {
            facadeBlock = null;
        }
        pipeColor = tag.contains("PipeColor") ? tag.getInt("PipeColor") : -1;
        redstoneMode = tag.getInt("RedstoneMode");
        roundRobin = tag.getBoolean("RoundRobin");
        if (tag.contains("Throughput")) throughputPercent = tag.getInt("Throughput");
        quantumLinked = tag.getBoolean("QuantumLinked");
        if (tag.contains("QuantumColor")) quantumColor = DyeColor.byId(Math.floorMod(tag.getInt("QuantumColor"), 16));
        if (tag.contains("QuantumChannel")) quantumChannel = QuantumLinkNetworkState.clampChannel(tag.getInt("QuantumChannel"));
        quantumPrivate = tag.getBoolean("QuantumPrivate");
        quantumOwner = tag.hasUUID("QuantumOwner") ? tag.getUUID("QuantumOwner") : null;
        filterContainer.clearContent();
        ContainerHelper.loadAllItems(tag, filterContainer.getItems(), registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
