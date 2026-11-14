package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

public class NaiadFluidPipeBlockEntity extends BlockEntity implements PipeFacadeHolder {
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

    public final SingleFluidStorage fluidStorage = SingleFluidStorage.withFixedCapacity(FluidConstants.BUCKET * 2, this::setChanged);
    private final ValveMode[] valves = new ValveMode[6];
    private final boolean[] disconnected = new boolean[6];
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
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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
        if (valves[idx] != ValveMode.NONE) {
            valves[idx] = ValveMode.NONE;
            if (level != null && !level.isClientSide()) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                        new ItemStack(ObsidiumMod.VOLCANITE_LOGISTICS_MODULE));
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
            if (valves[i] != ValveMode.NONE) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                        new ItemStack(ObsidiumMod.VOLCANITE_LOGISTICS_MODULE));
                valves[i] = ValveMode.NONE;
            }
        }
    }

    public Storage<FluidVariant> getFluidStorage(Direction dir) {
        if (dir != null && isSideDisconnected(dir)) {
            return null;
        }
        return fluidStorage;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, NaiadFluidPipeBlockEntity entity) {
        if (level.isClientSide()) return;

        boolean didSomething = false;
        final long transferRate = FluidConstants.BUCKET / 4; // 250 mB per tick

        // 1. Valve logic (Extract from / Insert into external fluid storages)
        for (Direction dir : Direction.values()) {
            if (entity.isSideDisconnected(dir)) continue;
            ValveMode mode = entity.valves[dir.get3DDataValue()];
            if (mode == ValveMode.NONE) continue;

            BlockPos targetPos = pos.relative(dir);
            if (!level.isLoaded(targetPos)) continue;
            BlockEntity neighborBe = level.getBlockEntity(targetPos);
            if (neighborBe instanceof NaiadFluidPipeBlockEntity) continue; // Valves only connect to external devices

            Storage<FluidVariant> targetStorage = FluidStorage.SIDED.find(level, targetPos, dir.getOpposite());
            if (targetStorage == null) continue;

            if (mode == ValveMode.EXTRACT) {
                if (targetStorage.supportsExtraction() && entity.fluidStorage.amount < entity.fluidStorage.getCapacity()) {
                    long moved = StorageUtil.move(targetStorage, entity.fluidStorage, fluid -> !fluid.isBlank(), transferRate, null);
                    if (moved > 0) didSomething = true;
                }
            } else if (mode == ValveMode.INSERT) {
                if (targetStorage.supportsInsertion() && entity.fluidStorage.amount > 0) {
                    long moved = StorageUtil.move(entity.fluidStorage, targetStorage, fluid -> !fluid.isBlank(), transferRate, null);
                    if (moved > 0) didSomething = true;
                }
            }
        }

        // 2. Spread / balance fluid across adjacent Naiad pipes (throttled to every 3 ticks, phase offset by pos)
        if (entity.fluidStorage.amount > 0 && (level.getGameTime() + Math.abs(pos.hashCode())) % 3 == 0) {
            for (Direction dir : Direction.values()) {
                if (entity.isSideDisconnected(dir)) continue;
                BlockPos targetPos = pos.relative(dir);
                if (!level.isLoaded(targetPos)) continue;
                BlockEntity neighborBe = level.getBlockEntity(targetPos);

                if (neighborBe instanceof NaiadFluidPipeBlockEntity otherPipe && !otherPipe.isSideDisconnected(dir.getOpposite())) {
                    if (entity.fluidStorage.amount > otherPipe.fluidStorage.amount + 81L) {
                        long diff = (entity.fluidStorage.amount - otherPipe.fluidStorage.amount) / 2;
                        long toMove = Math.min(diff, FluidConstants.BUCKET);
                        long moved = StorageUtil.move(entity.fluidStorage, otherPipe.fluidStorage, fluid -> !fluid.isBlank(), toMove, null);
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag fluidTag = new CompoundTag();
        fluidStorage.writeNbt(fluidTag, registries);
        tag.put("Fluid", fluidTag);

        byte[] valveBytes = new byte[6];
        byte[] discBytes = new byte[6];
        for (int i = 0; i < 6; i++) {
            valveBytes[i] = (byte) valves[i].ordinal();
            discBytes[i] = (byte) (disconnected[i] ? 1 : 0);
        }
        tag.putByteArray("Valves", valveBytes);
        tag.putByteArray("Disconnected", discBytes);
        if (facadeBlock != null) {
            tag.putString("FacadeBlock", net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(facadeBlock).toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Fluid")) {
            fluidStorage.readNbt(tag.getCompound("Fluid"), registries);
        }
        if (tag.contains("Valves")) {
            byte[] valveBytes = tag.getByteArray("Valves");
            for (int i = 0; i < Math.min(6, valveBytes.length); i++) {
                valves[i] = ValveMode.fromOrdinal(valveBytes[i]);
            }
        }
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
