package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AstrapeEnergyPylonBlockEntity extends BlockEntity {
    public static final long MAX_CAPACITY = 200_000L;
    public static final long MAX_IO = 50_000L;
    public static final int MAX_LINKS = 6;
    public static final double MAX_RANGE = 48.0;

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(MAX_CAPACITY, MAX_IO, MAX_IO) {
        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    private final List<BlockPos> linkedPylons = new ArrayList<>();
    private boolean isTransferring = false;
    private int tickCount = 0;
    private long lastWirelessReceivedTick = -100L;

    public AstrapeEnergyPylonBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.ASTRAPE_ENERGY_PYLON_BE, pos, state);
    }

    public void markWirelessReceived(long gameTime) {
        this.lastWirelessReceivedTick = gameTime;
    }

    public boolean isReceivingWireless(Level level) {
        return level != null && (level.getGameTime() - this.lastWirelessReceivedTick) <= 20L;
    }

    public boolean isOutputPylon(Level level) {
        return isReceivingWireless(level) || (!hasLocalGenerator(level) && anyLinkedPylonHasGenerator(level));
    }

    public boolean hasLocalGenerator(Level level) {
        if (level == null) return false;
        for (Direction dir : Direction.values()) {
            BlockPos np = this.worldPosition.relative(dir);
            if (level.getBlockEntity(np) instanceof AstrapeEnergyPylonBlockEntity) continue;
            EnergyStorage s = EnergyStorage.SIDED.find(level, np, dir.getOpposite());
            if (s != null && s.supportsExtraction() && !s.supportsInsertion()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasLocalConsumer(Level level) {
        if (level == null) return false;
        for (Direction dir : Direction.values()) {
            BlockPos np = this.worldPosition.relative(dir);
            if (level.getBlockEntity(np) instanceof AstrapeEnergyPylonBlockEntity) continue;
            EnergyStorage s = EnergyStorage.SIDED.find(level, np, dir.getOpposite());
            if (s != null && s.supportsInsertion() && !s.supportsExtraction()) {
                return true;
            }
        }
        return false;
    }

    public boolean anyLinkedPylonHasGenerator(Level level) {
        if (level == null) return false;
        for (BlockPos targetPos : this.linkedPylons) {
            if (level.isLoaded(targetPos) && level.getBlockEntity(targetPos) instanceof AstrapeEnergyPylonBlockEntity other) {
                if (other.hasLocalGenerator(level)) return true;
            }
        }
        return false;
    }

    private static boolean isBatteryBlockEntity(Level level, BlockPos pos, BlockEntity be) {
        return be instanceof VolcaniteEnergyCellBlockEntity
                || be instanceof VolcaniteEnergyUnitBlockEntity
                || be instanceof VolcaniteEnergyCoreBlockEntity
                || be instanceof KeraunosHVCoreBlockEntity
                || (level != null && level.getBlockState(pos).getBlock() instanceof KeraunosHVPortBlock);
    }

    private static boolean hasGeneratorAdjacentTo(Level level, BlockPos center) {
        for (Direction dir : Direction.values()) {
            BlockPos np = center.relative(dir);
            if (level.getBlockEntity(np) instanceof AstrapeEnergyPylonBlockEntity) continue;
            EnergyStorage s = EnergyStorage.SIDED.find(level, np, dir.getOpposite());
            if (s != null && s.supportsExtraction() && !s.supportsInsertion()) {
                return true;
            }
        }
        return false;
    }

    public long getAdjacentBatteryEnergy(Level level) {
        if (level == null) return -1L;
        for (Direction dir : Direction.values()) {
            BlockPos np = this.worldPosition.relative(dir);
            BlockEntity be = level.getBlockEntity(np);
            if (isBatteryBlockEntity(level, np, be)) {
                EnergyStorage s = EnergyStorage.SIDED.find(level, np, dir.getOpposite());
                if (s != null) return s.getAmount();
            }
        }
        return -1L;
    }

    public boolean isPullingFromLocalBattery(Level level) {
        if (level == null) return false;
        for (Direction dir : Direction.values()) {
            BlockPos np = this.worldPosition.relative(dir);
            BlockEntity be = level.getBlockEntity(np);
            if (isBatteryBlockEntity(level, np, be)) {
                EnergyStorage s = EnergyStorage.SIDED.find(level, np, dir.getOpposite());
                if (s != null && shouldPullFromBattery(level, np, s)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean shouldPullFromBattery(Level level, BlockPos batteryPos, EnergyStorage batteryStorage) {
        if (batteryStorage.getAmount() <= 0) return false;
        if (isReceivingWireless(level) || hasLocalGenerator(level) || anyLinkedPylonHasGenerator(level)) {
            return false;
        }
        if (hasGeneratorAdjacentTo(level, batteryPos)) {
            return true;
        }
        long myBattEnergy = batteryStorage.getAmount();
        for (BlockPos targetPos : this.linkedPylons) {
            if (!level.isLoaded(targetPos)) continue;
            if (level.getBlockEntity(targetPos) instanceof AstrapeEnergyPylonBlockEntity other) {
                if (other.hasLocalConsumer(level) && other.getAdjacentBatteryEnergy(level) < 0) {
                    return true;
                }
                long otherBattEnergy = other.getAdjacentBatteryEnergy(level);
                if (otherBattEnergy >= 0 && myBattEnergy > otherBattEnergy + 10_000L) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<BlockPos> getLinkedPylons() {
        return Collections.unmodifiableList(linkedPylons);
    }

    public boolean isTransferring() {
        return isTransferring;
    }

    public boolean toggleLink(BlockPos targetPos) {
        if (targetPos.equals(this.worldPosition)) return false;

        if (linkedPylons.contains(targetPos)) {
            linkedPylons.remove(targetPos);
            syncToClient();
            setChanged();
            return false; // Removed
        } else {
            if (linkedPylons.size() >= MAX_LINKS) {
                return false; // Limit reached
            }
            linkedPylons.add(targetPos);
            syncToClient();
            setChanged();
            return true; // Added
        }
    }

    public void removeLink(BlockPos targetPos) {
        if (linkedPylons.remove(targetPos)) {
            syncToClient();
            setChanged();
        }
    }

    public void clearAllLinks() {
        if (!linkedPylons.isEmpty()) {
            if (level != null && !level.isClientSide()) {
                for (BlockPos target : new ArrayList<>(linkedPylons)) {
                    if (level.isLoaded(target) && level.getBlockEntity(target) instanceof AstrapeEnergyPylonBlockEntity other) {
                        other.removeLink(this.worldPosition);
                    }
                }
            }
            linkedPylons.clear();
            syncToClient();
            setChanged();
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AstrapeEnergyPylonBlockEntity entity) {
        if (level.isClientSide()) return;

        entity.tickCount++;
        boolean transferredAny = false;

        // 1. Periodically validate links (every 10 ticks = 0.5s) to purge removed pylons
        if (entity.tickCount % 10 == 0) {
            boolean listChanged = false;
            var iterator = entity.linkedPylons.iterator();
            while (iterator.hasNext()) {
                BlockPos target = iterator.next();
                if (level.isLoaded(target)) {
                    BlockEntity targetBe = level.getBlockEntity(target);
                    if (!(targetBe instanceof AstrapeEnergyPylonBlockEntity)) {
                        iterator.remove();
                        listChanged = true;
                    }
                }
            }
            if (listChanged) {
                entity.syncToClient();
                entity.setChanged();
            }
        }

        // 2. Local IO: Pull from generators/source-batteries and push to machines/target-batteries/wires
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            EnergyStorage neighborStorage = EnergyStorage.SIDED.find(level, neighborPos, dir.getOpposite());
            if (neighborStorage == null) continue;

            // Don't pull/push to another pylon via face contact to prevent 1-block oscillation loops
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (neighborBe instanceof AstrapeEnergyPylonBlockEntity) continue;

            // A. Pure energy producers (supports extraction but not insertion) -> Always PULL
            if (neighborStorage.supportsExtraction() && !neighborStorage.supportsInsertion()) {
                if (entity.energyStorage.amount < entity.energyStorage.capacity) {
                    long moved = EnergyStorageUtil.move(neighborStorage, entity.energyStorage, MAX_IO, null);
                    if (moved > 0) transferredAny = true;
                }
            }
            // B. Pure energy consumers / machines (supports insertion but not extraction) -> Always PUSH
            else if (neighborStorage.supportsInsertion() && !neighborStorage.supportsExtraction()) {
                if (entity.energyStorage.amount > 0) {
                    long moved = EnergyStorageUtil.move(entity.energyStorage, neighborStorage, MAX_IO, null);
                    if (moved > 0) transferredAny = true;
                }
            }
            // C. Bidirectional Energy Cells / Units / Cores
            else if (isBatteryBlockEntity(level, neighborPos, neighborBe)) {
                if (entity.shouldPullFromBattery(level, neighborPos, neighborStorage)) {
                    if (entity.energyStorage.amount < entity.energyStorage.capacity) {
                        long moved = EnergyStorageUtil.move(neighborStorage, entity.energyStorage, MAX_IO, null);
                        if (moved > 0) transferredAny = true;
                    }
                } else if (entity.energyStorage.amount > 0) {
                    long moved = EnergyStorageUtil.move(entity.energyStorage, neighborStorage, MAX_IO, null);
                    if (moved > 0) transferredAny = true;
                }
            }
            // D. Energy Wires / Universal Conduits -> Push into them when this pylon is an output/receiver pylon
            else if (neighborStorage.supportsInsertion() && entity.energyStorage.amount > 0 && entity.isOutputPylon(level)) {
                long moved = EnergyStorageUtil.move(entity.energyStorage, neighborStorage, MAX_IO, null);
                if (moved > 0) transferredAny = true;
            }
        }

        // 3. Wireless Transfer across linked pylons
        if (!entity.linkedPylons.isEmpty()) {
            boolean iAmSource = entity.hasLocalGenerator(level) || entity.isPullingFromLocalBattery(level);
            for (BlockPos targetPos : entity.linkedPylons) {
                if (!level.isLoaded(targetPos)) continue;

                BlockEntity targetBe = level.getBlockEntity(targetPos);
                if (targetBe instanceof AstrapeEnergyPylonBlockEntity otherPylon) {
                    long myEnergy = entity.energyStorage.amount;
                    long otherEnergy = otherPylon.energyStorage.amount;
                    if (myEnergy <= 0 || otherEnergy >= otherPylon.energyStorage.capacity) continue;

                    boolean otherIsSource = otherPylon.hasLocalGenerator(level) || otherPylon.isPullingFromLocalBattery(level);
                    long sendAmount = 0;

                    if (iAmSource && !otherIsSource) {
                        // Direct source transmission: send up to full MAX_IO without stopping at 50% equilibrium
                        sendAmount = Math.min(myEnergy, MAX_IO);
                    } else if (!otherPylon.isReceivingWireless(level) || iAmSource) {
                        if (myEnergy > otherEnergy) {
                            long diff = (myEnergy - otherEnergy + 1) / 2;
                            sendAmount = Math.min(diff, MAX_IO);
                        }
                    }

                    if (sendAmount > 0) {
                        long moved = EnergyStorageUtil.move(entity.energyStorage, otherPylon.energyStorage, sendAmount, null);
                        if (moved > 0) {
                            otherPylon.markWirelessReceived(level.getGameTime());
                            transferredAny = true;
                        }
                    }
                }
            }
        }

        // 4. Update blockstate LIT
        boolean shouldBeLit = transferredAny || entity.energyStorage.amount > 0;
        boolean currentLit = state.getValue(AstrapeEnergyPylonBlock.LIT);
        if (currentLit != shouldBeLit) {
            level.setBlock(pos, state.setValue(AstrapeEnergyPylonBlock.LIT, shouldBeLit), 3);
            entity.setChanged();
        }

        boolean prevTransferring = entity.isTransferring;
        entity.isTransferring = transferredAny;
        if (prevTransferring != transferredAny) {
            entity.syncToClient();
        }
    }

    public void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putLong("Energy", energyStorage.amount);
        tag.putBoolean("Transferring", isTransferring);

        ListTag list = new ListTag();
        for (BlockPos p : linkedPylons) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("X", p.getX());
            posTag.putInt("Y", p.getY());
            posTag.putInt("Z", p.getZ());
            list.add(posTag);
        }
        tag.put("LinkedPylons", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        energyStorage.amount = tag.getLong("Energy");
        isTransferring = tag.getBoolean("Transferring");

        linkedPylons.clear();
        if (tag.contains("LinkedPylons", Tag.TAG_LIST)) {
            ListTag list = tag.getList("LinkedPylons", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag posTag = list.getCompound(i);
                linkedPylons.add(new BlockPos(posTag.getInt("X"), posTag.getInt("Y"), posTag.getInt("Z")));
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        saveAdditional(tag, provider);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

