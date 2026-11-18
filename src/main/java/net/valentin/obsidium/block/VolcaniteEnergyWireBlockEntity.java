package net.valentin.obsidium.block;

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
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.storage.QuantumLinkNetworkState;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

public class VolcaniteEnergyWireBlockEntity extends BlockEntity implements PipeFacadeHolder, PipeConfigurable, MenuProvider {
    public enum WireMode {
        NORMAL(0),
        EXTRACT(1),
        INSERT(2);

        private final int id;
        WireMode(int id) { this.id = id; }
        public int getId() { return id; }

        public WireMode cycle() {
            return switch (this) {
                case NORMAL -> EXTRACT;
                case EXTRACT -> INSERT;
                case INSERT -> NORMAL;
            };
        }

        public static WireMode fromOrdinal(int ord) {
            WireMode[] vals = values();
            if (ord >= 0 && ord < vals.length) return vals[ord];
            return NORMAL;
        }
    }

    public int getTier() {
        if (getBlockState().getBlock() instanceof VolcaniteEnergyWireBlock wireBlock) {
            return wireBlock.getTier();
        }
        return 1;
    }

    public long getMaxTransfer() {
        if (getBlockState().getBlock() instanceof VolcaniteEnergyWireBlock wireBlock) {
            return wireBlock.getMaxTransfer();
        }
        return 5_000L;
    }

    public long getCapacity() {
        if (getBlockState().getBlock() instanceof VolcaniteEnergyWireBlock wireBlock) {
            return wireBlock.getCapacity();
        }
        return 20_000L;
    }

    private static final int MAX_NETWORK_SCAN = 512;
    private static final ThreadLocal<Set<BlockPos>> ACTIVE_ROUTING = ThreadLocal.withInitial(HashSet::new);

    // Drain-only migration buffer: old saved energy remains available after upgrading.
    private final SimpleEnergyStorage legacyEnergy = new SimpleEnergyStorage(Long.MAX_VALUE, 0, Long.MAX_VALUE) {
        @Override protected void onFinalCommit() { setChanged(); }
    };

    private final TickTransferBudget transferBudget = new TickTransferBudget();
    private static long available(ConsumerEntry entry, Level level) {
        long available = entry.maxRate();
        for (var wire : entry.path()) available = Math.min(available,
                wire.transferBudget.remaining(level.getGameTime(), wire.getEffectiveMaxTransfer()));
        return available;
    }
    private static void consume(ConsumerEntry entry, long amount, TransactionContext transaction) {
        for (var wire : entry.path()) wire.transferBudget.consume(amount, transaction);
    }

    private record WireNode(BlockPos pos, long pathMaxRate, List<VolcaniteEnergyWireBlockEntity> path) {}
    private record ConsumerEntry(EnergyStorage storage, long maxRate, List<VolcaniteEnergyWireBlockEntity> path) {}
    private record SourceEntry(EnergyStorage storage, @Nullable BlockPos excludePos) {}

    public long getEffectiveMaxTransfer() {
        return Math.max(1L, (getMaxTransfer() * throughputPercent) / 100L);
    }

    @Nullable
    public EnergyStorage getEnergyStorage(@Nullable Direction side) {
        if (side != null && isSideDisconnected(side)) {
            return null;
        }
        WireMode mode = side != null ? getWireMode(side) : WireMode.NORMAL;
        return new EnergyStorage() {
            @Override
            public boolean supportsInsertion() {
                return mode != WireMode.INSERT;
            }

            @Override
            public long insert(long maxAmount, TransactionContext transaction) {
                if (mode == WireMode.INSERT || maxAmount <= 0L) return 0L;
                if (level == null || level.isClientSide() || !isRedstoneAllowed(level, worldPosition)) return 0L;
                Set<BlockPos> active = ACTIVE_ROUTING.get();
                if (!active.add(worldPosition)) return 0L;
                try {
                    long toSend = Math.min(maxAmount, getEffectiveMaxTransfer());
                    BlockPos excludePos = side != null ? worldPosition.relative(side) : null;
                    List<ConsumerEntry> consumers = collectConsumers(level, worldPosition, excludePos);
                    if (consumers.isEmpty()) return 0L;
                    if (roundRobin && consumers.size() > 1) {
                        int offset = Math.floorMod(rrIndex, consumers.size());
                        if (offset > 0) {
                            List<ConsumerEntry> rotated = new ArrayList<>(consumers.size());
                            rotated.addAll(consumers.subList(offset, consumers.size()));
                            rotated.addAll(consumers.subList(0, offset));
                            consumers = rotated;
                        }
                    }
                    long totalInserted = 0L;
                    for (ConsumerEntry c : consumers) {
                        long allowed = Math.min(toSend - totalInserted, available(c, level));
                        if (allowed <= 0L) continue;
                        long moved = c.storage().insert(allowed, transaction);
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
                return legacyEnergy.amount > 0L;
            }

            @Override
            public long extract(long maxAmount, TransactionContext transaction) {
                if (level == null || !isRedstoneAllowed(level, worldPosition)) return 0L;
                long allowed = Math.min(maxAmount, transferBudget.remaining(level.getGameTime(), getEffectiveMaxTransfer()));
                long moved = legacyEnergy.extract(allowed, transaction);
                transferBudget.consume(moved, transaction);
                return moved;
            }

            @Override
            public long getAmount() {
                return legacyEnergy.amount;
            }

            @Override
            public long getCapacity() {
                return legacyEnergy.amount;
            }
        };
    }

    private final boolean[] disconnected = new boolean[6];
    private final WireMode[] wireModes = new WireMode[6];
    private net.minecraft.world.level.block.Block facadeBlock = null;

    private int pipeColor = -1;
    private int redstoneMode = REDSTONE_ALWAYS;
    private boolean roundRobin = false;
    private int throughputPercent = 100;
    private int rrIndex = 0;

    private boolean quantumLinked = false;
    private DyeColor quantumColor = DyeColor.PURPLE;
    private int quantumChannel = 1;
    private boolean quantumPrivate = false;
    private @Nullable UUID quantumOwner = null;

    @Override public int getPipeKind() { return KIND_ENERGY; }
    @Override public int getPipeColor() { return pipeColor; }
    @Override public void setPipeColor(int color) {
        int clamped = (color >= 0 && color < 16) ? color : -1;
        if (this.pipeColor != clamped) {
            this.pipeColor = clamped;
            setChanged();
            if (level != null && !level.isClientSide()) {
                BlockState state = getBlockState();
                if (state.getBlock() instanceof VolcaniteEnergyWireBlock wireBlock) {
                    BlockState newState = wireBlock.makeConnections(level, worldPosition)
                            .setValue(VolcaniteEnergyWireBlock.WATERLOGGED, state.getValue(VolcaniteEnergyWireBlock.WATERLOGGED));
                    level.setBlock(worldPosition, newState, 3);
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
    @Override public int getFaceModeOrdinal(Direction dir) { return getWireMode(dir).getId(); }
    @Override public void cycleFaceMode(Direction dir) { cycleWireMode(dir); }
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
    @Override public @Nullable Container getFilterContainer() { return null; }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new PipeConfigMenu(syncId, playerInventory, this);
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

    public VolcaniteEnergyWireBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.VOLCANITE_ENERGY_WIRE_BE, pos, state);
        Arrays.fill(disconnected, false);
        Arrays.fill(wireModes, WireMode.NORMAL);
    }

    public boolean isSideDisconnected(Direction dir) {
        if (dir == null) return false;
        return disconnected[dir.get3DDataValue()];
    }

    public void setSideDisconnected(Direction dir, boolean disc) {
        if (dir == null) return;
        disconnected[dir.get3DDataValue()] = disc;
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            if (state.getBlock() instanceof VolcaniteEnergyWireBlock wireBlock) {
                BlockState newState = wireBlock.makeConnections(level, worldPosition);
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

    public WireMode getWireMode(Direction dir) {
        if (dir == null) return WireMode.NORMAL;
        return wireModes[dir.get3DDataValue()];
    }

    public void setWireMode(Direction dir, WireMode mode) {
        if (dir == null) return;
        wireModes[dir.get3DDataValue()] = mode;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public WireMode cycleWireMode(Direction dir) {
        if (dir == null) return WireMode.NORMAL;
        WireMode next = getWireMode(dir).cycle();
        setWireMode(dir, next);
        return next;
    }

    private boolean hasInsertFace() {
        for (WireMode m : wireModes) {
            if (m == WireMode.INSERT) return true;
        }
        return false;
    }

    private static EnergyStorage createQuantumInsertStorage(QuantumLinkNetworkState netState, String qKey) {
        return netState.getEnergyStorage(qKey);
    }

    private static EnergyStorage createQuantumExtractStorage(QuantumLinkNetworkState netState, String qKey) {
        return netState.getEnergyStorage(qKey);
    }

    private List<ConsumerEntry> collectConsumers(Level level, BlockPos startPos, @Nullable BlockPos excludePos) {
        List<ConsumerEntry> consumers = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<WireNode> queue = new ArrayDeque<>();
        visited.add(startPos);
        queue.add(new WireNode(startPos, getEffectiveMaxTransfer(), List.of(this)));
        QuantumLinkNetworkState netState = QuantumLinkNetworkState.get(level);

        while (!queue.isEmpty()) {
            WireNode node = queue.poll();
            BlockEntity be = level.getBlockEntity(node.pos());
            if (!(be instanceof VolcaniteEnergyWireBlockEntity wire)) continue;
            if (!wire.isRedstoneAllowed(level, node.pos())) continue;

            if (wire.quantumLinked && netState != null && !wire.hasInsertFace()) {
                String qKey = QuantumLinkNetworkState.buildChannelKey(wire.quantumPrivate, wire.quantumOwner, wire.quantumColor, wire.quantumChannel);
                consumers.add(new ConsumerEntry(createQuantumInsertStorage(netState, qKey), node.pathMaxRate(), node.path()));
            }

            for (Direction dir : Direction.values()) {
                if (wire.isSideDisconnected(dir)) continue;
                BlockPos np = node.pos().relative(dir);
                if (!level.isLoaded(np)) continue;

                BlockEntity nbe = level.getBlockEntity(np);
                if (nbe instanceof VolcaniteEnergyWireBlockEntity otherWire) {
                    if (!otherWire.isSideDisconnected(dir.getOpposite()) && wire.pipeColor == otherWire.pipeColor) {
                        if (visited.size() < MAX_NETWORK_SCAN && visited.add(np)) {
                            long nextRate = Math.min(node.pathMaxRate(), otherWire.getEffectiveMaxTransfer());
                            var path = new ArrayList<>(node.path());
                            path.add(otherWire);
                            queue.add(new WireNode(np, nextRate, path));
                        }
                    }
                    continue;
                }

                if (excludePos != null && np.equals(excludePos)) continue;
                WireMode mode = wire.getWireMode(dir);
                if (mode == WireMode.EXTRACT) continue;
                if (nbe instanceof AstrapeEnergyPylonBlockEntity pylon && pylon.isOutputPylon(level)) continue;

                EnergyStorage target = EnergyStorage.SIDED.find(level, np, dir.getOpposite());
                if (target != null && target.supportsInsertion()) {
                    consumers.add(new ConsumerEntry(target, node.pathMaxRate(), node.path()));
                }
            }
        }
        return consumers;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VolcaniteEnergyWireBlockEntity entity) {
        if (level.isClientSide()) return;
        if (!entity.isRedstoneAllowed(level, pos)) return;

        List<SourceEntry> sources = new ArrayList<>();
        if (entity.legacyEnergy.amount > 0L) sources.add(new SourceEntry(entity.legacyEnergy, null));
        for (Direction dir : Direction.values()) {
            if (entity.isSideDisconnected(dir)) continue;
            BlockPos np = pos.relative(dir);
            if (!level.isLoaded(np)) continue;
            BlockEntity nbe = level.getBlockEntity(np);
            if (nbe instanceof VolcaniteEnergyWireBlockEntity) continue;

            WireMode mode = entity.getWireMode(dir);
            if (mode == WireMode.INSERT) continue;

            EnergyStorage target = EnergyStorage.SIDED.find(level, np, dir.getOpposite());
            if (target == null || !target.supportsExtraction()) continue;

            if (mode == WireMode.EXTRACT || !target.supportsInsertion()) {
                sources.add(new SourceEntry(target, np));
            }
        }

        if (entity.quantumLinked && entity.hasInsertFace()) {
            QuantumLinkNetworkState netState = QuantumLinkNetworkState.get(level);
            if (netState != null) {
                String qKey = QuantumLinkNetworkState.buildChannelKey(entity.quantumPrivate, entity.quantumOwner, entity.quantumColor, entity.quantumChannel);
                sources.add(new SourceEntry(createQuantumExtractStorage(netState, qKey), null));
            }
        }

        if (sources.isEmpty()) return;

        Set<BlockPos> active = ACTIVE_ROUTING.get();
        if (!active.add(pos)) return;
        boolean didSomething = false;
        try {
            long wireMax = entity.getEffectiveMaxTransfer();
            for (SourceEntry src : sources) {
                List<ConsumerEntry> consumers = entity.collectConsumers(level, pos, src.excludePos());
                if (consumers.isEmpty()) continue;

                if (entity.roundRobin && consumers.size() > 1) {
                    int offset = Math.floorMod(entity.rrIndex, consumers.size());
                    if (offset > 0) {
                        List<ConsumerEntry> rotated = new ArrayList<>(consumers.size());
                        rotated.addAll(consumers.subList(offset, consumers.size()));
                        rotated.addAll(consumers.subList(0, offset));
                        consumers = rotated;
                    }
                }

                long remaining = wireMax;
                for (ConsumerEntry c : consumers) {
                    long toMove = Math.min(remaining, available(c, level));
                    if (toMove <= 0L) continue;
                    long moved;
                    try (Transaction transaction = Transaction.openOuter()) {
                        moved = EnergyStorageUtil.move(src.storage(), c.storage(), toMove, transaction);
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putLong("Energy", legacyEnergy.amount);
        byte[] discBytes = new byte[6];
        byte[] modeBytes = new byte[6];
        for (int i = 0; i < 6; i++) {
            discBytes[i] = (byte) (disconnected[i] ? 1 : 0);
            modeBytes[i] = (byte) wireModes[i].ordinal();
        }
        tag.putByteArray("Disconnected", discBytes);
        tag.putByteArray("WireModes", modeBytes);
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        legacyEnergy.amount = Math.max(0L, tag.getLong("Energy"));
        if (tag.contains("Disconnected")) {
            byte[] discBytes = tag.getByteArray("Disconnected");
            for (int i = 0; i < Math.min(6, discBytes.length); i++) {
                disconnected[i] = discBytes[i] == 1;
            }
        }
        if (tag.contains("WireModes")) {
            byte[] modeBytes = tag.getByteArray("WireModes");
            for (int i = 0; i < Math.min(6, modeBytes.length); i++) {
                wireModes[i] = WireMode.fromOrdinal(modeBytes[i]);
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
