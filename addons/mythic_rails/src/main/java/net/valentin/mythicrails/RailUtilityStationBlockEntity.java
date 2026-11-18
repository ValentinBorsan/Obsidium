package net.valentin.mythicrails;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/** Persistent Fluid (32,000 mB) or Energy (2,000,000 RF) rail station buffer with Fabric Sided API. */
public final class RailUtilityStationBlockEntity extends BlockEntity implements Container {
    public static final int MAX_FLUID_MB = 32_000;
    public static final long MAX_FLUID_DROPLETS = 32L * FluidConstants.BUCKET; // 1 mB = 81 droplets
    public static final long MAX_ENERGY = 2_000_000L;

    private final SimpleContainer auxSlot = new SimpleContainer(1);

    private final SingleFluidStorage fluidStorage = SingleFluidStorage.withFixedCapacity(
            MAX_FLUID_DROPLETS, this::setChanged);

    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(MAX_ENERGY, 50_000L, 50_000L) {
        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    public RailUtilityStationBlockEntity(BlockPos pos, BlockState state) {
        super(MythicRails.UTILITY_STATION_BE, pos, state);
    }

    public RailUtilityStationBlock.StationKind kind() {
        return getBlockState().getBlock() instanceof RailUtilityStationBlock b
                ? b.kind() : RailUtilityStationBlock.StationKind.FLUID_LOADER;
    }

    public SingleFluidStorage fluidStorage() { return fluidStorage; }
    public SimpleEnergyStorage energyStorage() { return energyStorage; }

    public EnergyStorage sidedEnergyStorage() {
        return new EnergyStorage() {
            @Override
            public boolean supportsInsertion() { return kind().isLoader; }

            @Override
            public boolean supportsExtraction() { return !kind().isLoader; }

            @Override
            public long insert(long maxAmount, TransactionContext tx) {
                return kind().isLoader ? energyStorage.insert(maxAmount, tx) : 0L;
            }

            @Override
            public long extract(long maxAmount, TransactionContext tx) {
                return !kind().isLoader ? energyStorage.extract(maxAmount, tx) : 0L;
            }

            @Override
            public long getAmount() { return energyStorage.getAmount(); }

            @Override
            public long getCapacity() { return energyStorage.getCapacity(); }
        };
    }

    public int fluidMb() { return (int) (fluidStorage.amount / 81L); }

    public int fluidTypeCode() {
        if (fluidStorage.amount <= 0 || fluidStorage.variant.isBlank()) return 0;
        if (fluidStorage.variant.getFluid() == Fluids.LAVA) return 2;
        if (fluidStorage.variant.getFluid() == Fluids.WATER) return 1;
        return 1;
    }

    public int energyAmount() { return (int) Math.min(Integer.MAX_VALUE, energyStorage.amount); }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RailUtilityStationBlockEntity be) {
        var kind = be.kind();
        if (kind.isFluid) {
            be.processAuxBucket();
            if (level.getGameTime() % 4 == 0 && state.getValue(RailUtilityStationBlock.ENABLED)) {
                be.bridgeAdjacentFluid(level, pos, kind.isLoader);
            }
        } else {
            be.processAuxEnergy();
            if (state.getValue(RailUtilityStationBlock.ENABLED)) {
                be.bridgeAdjacentEnergy(level, pos, kind.isLoader);
            }
        }
    }

    private void processAuxBucket() {
        ItemStack stack = auxSlot.getItem(0);
        if (stack.isEmpty()) return;
        if (stack.is(Items.WATER_BUCKET) && (fluidStorage.isResourceBlank() || fluidStorage.variant.getFluid() == Fluids.WATER)
                && fluidStorage.amount + FluidConstants.BUCKET <= MAX_FLUID_DROPLETS) {
            try (var tx = Transaction.openOuter()) {
                if (fluidStorage.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, tx) == FluidConstants.BUCKET) {
                    tx.commit();
                    auxSlot.setItem(0, new ItemStack(Items.BUCKET));
                    setChanged();
                }
            }
        } else if (stack.is(Items.LAVA_BUCKET) && (fluidStorage.isResourceBlank() || fluidStorage.variant.getFluid() == Fluids.LAVA)
                && fluidStorage.amount + FluidConstants.BUCKET <= MAX_FLUID_DROPLETS) {
            try (var tx = Transaction.openOuter()) {
                if (fluidStorage.insert(FluidVariant.of(Fluids.LAVA), FluidConstants.BUCKET, tx) == FluidConstants.BUCKET) {
                    tx.commit();
                    auxSlot.setItem(0, new ItemStack(Items.BUCKET));
                    setChanged();
                }
            }
        } else if (stack.is(Items.BUCKET) && stack.getCount() == 1 && fluidStorage.amount >= FluidConstants.BUCKET) {
            var fluid = fluidStorage.variant.getFluid();
            var filled = fluid == Fluids.LAVA ? Items.LAVA_BUCKET : (fluid == Fluids.WATER ? Items.WATER_BUCKET : null);
            if (filled != null) {
                try (var tx = Transaction.openOuter()) {
                    if (fluidStorage.extract(fluidStorage.variant, FluidConstants.BUCKET, tx) == FluidConstants.BUCKET) {
                        tx.commit();
                        auxSlot.setItem(0, new ItemStack(filled));
                        setChanged();
                    }
                }
            }
        }
    }

    private void processAuxEnergy() {
        ItemStack stack = auxSlot.getItem(0);
        if (stack.isEmpty()) return;

        var itemEnergy = EnergyStorage.ITEM.find(stack, ContainerItemContext.ofSingleSlot(
                InventoryStorage.of(auxSlot, null).getSlot(0)));
        if (itemEnergy != null) {
            boolean isLoader = kind().isLoader;
            long moved = isLoader
                    ? EnergyStorageUtil.move(itemEnergy, energyStorage, 25_000L, null)
                    : EnergyStorageUtil.move(energyStorage, itemEnergy, 25_000L, null);
            if (moved > 0) setChanged();
            return;
        }

        if (energyStorage.amount >= MAX_ENERGY) return;
        long gain = 0;
        if (stack.is(Items.REDSTONE)) gain = 8_000L;
        else if (stack.is(Items.REDSTONE_BLOCK)) gain = 72_000L;
        else if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) gain = 4_000L;
        if (gain > 0) {
            energyStorage.amount = Math.min(MAX_ENERGY, energyStorage.amount + gain);
            stack.shrink(1);
            setChanged();
        }
    }

    private void bridgeAdjacentFluid(Level level, BlockPos pos, boolean isLoader) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos other = pos.relative(dir);
            if (!level.getChunkSource().hasChunk(other.getX() >> 4, other.getZ() >> 4)) continue;
            var ext = FluidStorage.SIDED.find(level, other, dir.getOpposite());
            if (ext == null) continue;
            try (var tx = Transaction.openOuter()) {
                long moved = StorageUtil.move(isLoader ? ext : fluidStorage,
                        isLoader ? fluidStorage : ext, v -> true, FluidConstants.BUCKET, tx);
                if (moved > 0) {
                    tx.commit();
                    break;
                }
            }
        }
    }

    private void bridgeAdjacentEnergy(Level level, BlockPos pos, boolean isLoader) {
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP) continue;
            BlockPos other = pos.relative(dir);
            if (!level.getChunkSource().hasChunk(other.getX() >> 4, other.getZ() >> 4)) continue;
            var ext = EnergyStorage.SIDED.find(level, other, dir.getOpposite());
            if (ext == null) continue;
            long moved = isLoader
                    ? EnergyStorageUtil.move(ext, energyStorage, 25_000L, null)
                    : EnergyStorageUtil.move(energyStorage, ext, 25_000L, null);
            if (moved > 0) setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        fluidStorage.writeNbt(tag, registries);
        tag.putLong("Energy", energyStorage.amount);
        ItemStack aux = auxSlot.getItem(0);
        if (!aux.isEmpty()) tag.put("Aux", aux.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fluidStorage.readNbt(tag, registries);
        energyStorage.amount = Math.clamp(tag.getLong("Energy"), 0L, MAX_ENERGY);
        auxSlot.setItem(0, tag.contains("Aux")
                ? ItemStack.parse(registries, tag.getCompound("Aux")).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY);
    }

    @Override
    public void setChanged() {
        if (level != null) level.blockEntityChanged(worldPosition);
    }

    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return auxSlot.isEmpty(); }
    @Override public ItemStack getItem(int slot) { return auxSlot.getItem(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { var r = auxSlot.removeItem(slot, amount); setChanged(); return r; }
    @Override public ItemStack removeItemNoUpdate(int slot) { var r = auxSlot.removeItemNoUpdate(slot); setChanged(); return r; }
    @Override public void setItem(int slot, ItemStack stack) { auxSlot.setItem(slot, stack); setChanged(); }
    @Override public void clearContent() { auxSlot.clearContent(); setChanged(); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
}
