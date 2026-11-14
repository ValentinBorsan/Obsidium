package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

public class VolcaniteEnergyWireBlockEntity extends BlockEntity implements PipeFacadeHolder {
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

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(2_000_000L, 500_000L, 500_000L) {
        @Override
        public long getCapacity() {
            return VolcaniteEnergyWireBlockEntity.this.getCapacity();
        }

        @Override
        public long insert(long maxAmount, net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction) {
            long space = Math.max(0, getCapacity() - this.amount);
            long allowed = Math.min(maxAmount, Math.min(space, getMaxTransfer()));
            return super.insert(allowed, transaction);
        }

        @Override
        public long extract(long maxAmount, net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction) {
            long allowed = Math.min(maxAmount, getMaxTransfer());
            return super.extract(allowed, transaction);
        }

        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    private final boolean[] disconnected = new boolean[6];
    private final WireMode[] wireModes = new WireMode[6];
    private net.minecraft.world.level.block.Block facadeBlock = null;

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

    public static void tick(Level level, BlockPos pos, BlockState state, VolcaniteEnergyWireBlockEntity entity) {
        if (level.isClientSide()) return;

        boolean didSomething = false;
        long maxTransfer = entity.getMaxTransfer();
        
        for (Direction dir : Direction.values()) {
            if (entity.isSideDisconnected(dir)) continue;

            BlockPos neighborPos = pos.relative(dir);
            if (!level.isLoaded(neighborPos)) continue;

            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (neighborBe instanceof VolcaniteEnergyWireBlockEntity otherWire && otherWire.isSideDisconnected(dir.getOpposite())) {
                continue;
            }

            WireMode mode = entity.wireModes[dir.get3DDataValue()];
            EnergyStorage target = EnergyStorage.SIDED.find(level, neighborPos, dir.getOpposite());
            if (target == null) continue;

            // 1. EXTRACT / Export Mode (Pull energy from adjacent generator/machine into wire)
            if (mode == WireMode.EXTRACT) {
                if (target.supportsExtraction() && entity.energyStorage.amount < entity.getCapacity()) {
                    long space = entity.getCapacity() - entity.energyStorage.amount;
                    long toMove = Math.min(maxTransfer, space);
                    long moved = EnergyStorageUtil.move(target, entity.energyStorage, toMove, null);
                    if (moved > 0) didSomething = true;
                }
            }
            // 2. INSERT / Import Mode (Push energy from wire into adjacent machine)
            else if (mode == WireMode.INSERT) {
                if (target.supportsInsertion() && entity.energyStorage.amount > 0) {
                    long toMove = Math.min(maxTransfer, entity.energyStorage.amount);
                    long moved = EnergyStorageUtil.move(entity.energyStorage, target, toMove, null);
                    if (moved > 0) didSomething = true;
                }
            }
            // 3. NORMAL Mode (Auto-balance and bidirectional push/pull)
            else {
                // Adjacent is another energy wire: balance energy smoothly without heavy diffusion drop-off (throttled every 2 ticks with hash phase offset)
                if (neighborBe instanceof VolcaniteEnergyWireBlockEntity otherWire) {
                    if ((level.getGameTime() + Math.abs(pos.hashCode())) % 2 == 0) {
                        if (entity.energyStorage.amount > otherWire.energyStorage.amount) {
                            long effectiveRate = Math.min(maxTransfer, otherWire.getMaxTransfer());
                            long diff = (entity.energyStorage.amount - otherWire.energyStorage.amount + 1) / 2;
                            long toMove = Math.min(diff, effectiveRate);
                            long moved = EnergyStorageUtil.move(entity.energyStorage, target, toMove, null);
                            if (moved > 0) didSomething = true;
                        }
                    }
                } else {
                    // Pull from pure producers (generators that don't accept insertion)
                    if (target.supportsExtraction() && !target.supportsInsertion() && entity.energyStorage.amount < entity.getCapacity()) {
                        long space = entity.getCapacity() - entity.energyStorage.amount;
                        long toMove = Math.min(maxTransfer, space);
                        long moved = EnergyStorageUtil.move(target, entity.energyStorage, toMove, null);
                        if (moved > 0) didSomething = true;
                    }

                    // Push to consumers (skip output/receiver Astrape Pylons so wires don't push energy back into the pylon feeding them)
                    if (target.supportsInsertion() && entity.energyStorage.amount > 0
                            && !(neighborBe instanceof AstrapeEnergyPylonBlockEntity pylon && pylon.isOutputPylon(level))) {
                        long toMove = Math.min(maxTransfer, entity.energyStorage.amount);
                        long moved = EnergyStorageUtil.move(entity.energyStorage, target, toMove, null);
                        if (moved > 0) didSomething = true;
                    }
                }
            }
        }

        if (didSomething) {
            entity.setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putLong("Energy", energyStorage.amount);
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.energyStorage.amount = tag.getLong("Energy");
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
