package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyItem;
import team.reborn.energy.api.base.SimpleEnergyStorage;

public class VolcanicDieselGeneratorBlockEntity extends BlockEntity implements MenuProvider, InventoryHolder, MachineInventory, WorldlyContainer {

    public static final int INVENTORY_SIZE = 3;
    public static final int MAX_ENERGY = 2_000_000;
    public static final int MAX_EXTRACT_RATE = 5_000;
    public static final int MAX_FLUID = 16_000;
    public static final int MAX_RPM = 1_000;

    public record DieselFuelProfile(int rfPerTick, int durationPer100Mb, int fluidTypeId) {}

    private final NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private int burnTime = 0;
    private int maxBurnTime = 0;
    private int currentRfPerTick = 0;
    private int fluidAmount = 0;
    private int rpm = 0;
    private FluidVariant storedFluid = FluidVariant.blank();

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(MAX_ENERGY, 0, MAX_EXTRACT_RATE) {
        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnTime;
                case 1 -> maxBurnTime;
                case 2 -> currentRfPerTick;
                case 3 -> (int) (energyStorage.amount >> 16);
                case 4 -> (int) (energyStorage.amount & 0xFFFF);
                case 5 -> (int) (energyStorage.capacity >> 16);
                case 6 -> (int) (energyStorage.capacity & 0xFFFF);
                case 7 -> fluidAmount;
                case 8 -> MAX_FLUID;
                case 9 -> rpm;
                case 10 -> getFluidTypeId(storedFluid);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> burnTime = value;
                case 1 -> maxBurnTime = value;
                case 2 -> currentRfPerTick = value;
                case 7 -> fluidAmount = value;
                case 9 -> rpm = value;
            }
        }

        @Override
        public int getCount() {
            return 11;
        }
    };

    public VolcanicDieselGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.VOLCANIC_DIESEL_GENERATOR_BE, pos, state);
    }

    @Nullable
    public static Fluid getFluidFromBucket(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.is(ObsidiumMod.BIO_OIL_BUCKET)) return ObsidiumMod.BIO_OIL_FLUID;
        if (stack.is(ObsidiumMod.BIO_ETHANOL_BUCKET)) return ObsidiumMod.BIO_ETHANOL_FLUID;
        if (stack.is(ObsidiumMod.BIODIESEL_BUCKET)) return ObsidiumMod.BIODIESEL_FLUID;
        if (stack.is(ObsidiumMod.PYRO_DIESEL_BUCKET)) return ObsidiumMod.PYRO_DIESEL_FLUID;
        return null;
    }

    @Nullable
    public static DieselFuelProfile getFuelProfile(FluidVariant variant) {
        if (variant == null || variant.isBlank()) return null;
        if (variant.isOf(ObsidiumMod.BIO_OIL_FLUID)) {
            return new DieselFuelProfile(200, 100, 1);
        } else if (variant.isOf(ObsidiumMod.BIO_ETHANOL_FLUID)) {
            return new DieselFuelProfile(400, 60, 2);
        } else if (variant.isOf(ObsidiumMod.BIODIESEL_FLUID)) {
            return new DieselFuelProfile(750, 80, 3);
        } else if (variant.isOf(ObsidiumMod.PYRO_DIESEL_FLUID)) {
            return new DieselFuelProfile(1600, 90, 4);
        }
        return null;
    }

    public static int getFluidTypeId(FluidVariant variant) {
        DieselFuelProfile profile = getFuelProfile(variant);
        return profile != null ? profile.fluidTypeId() : 0;
    }

    public FluidVariant getStoredFluid() {
        return storedFluid;
    }

    public int getFluidAmount() {
        return fluidAmount;
    }

    public int getRpm() {
        return rpm;
    }

    public boolean isActive() {
        return burnTime > 0;
    }

    public int insertFluidInternal(FluidVariant variant, int maxInsert, boolean simulate) {
        if (getFuelProfile(variant) == null || maxInsert <= 0) return 0;
        if (fluidAmount > 0 && !storedFluid.equals(variant)) return 0;
        int space = MAX_FLUID - fluidAmount;
        int toInsert = Math.min(maxInsert, space);
        if (toInsert > 0 && !simulate) {
            if (fluidAmount == 0) {
                storedFluid = variant;
            }
            fluidAmount += toInsert;
            setChanged();
            syncToClient();
        }
        return toInsert;
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void tickServer(Level level, BlockPos pos, BlockState state) {
        boolean changed = false;
        boolean needClientSync = false;

        ItemStack fuelInput = items.get(0);
        ItemStack emptyOut = items.get(1);
        Fluid bucketFluid = getFluidFromBucket(fuelInput);
        if (bucketFluid != null) {
            FluidVariant fv = FluidVariant.of(bucketFluid);
            if (insertFluidInternal(fv, 1000, true) == 1000) {
                if (emptyOut.isEmpty() || (emptyOut.is(Items.BUCKET) && emptyOut.getCount() < emptyOut.getMaxStackSize())) {
                    insertFluidInternal(fv, 1000, false);
                    fuelInput.shrink(1);
                    if (emptyOut.isEmpty()) {
                        items.set(1, new ItemStack(Items.BUCKET));
                    } else {
                        emptyOut.grow(1);
                    }
                    changed = true;
                }
            }
        }

        boolean activelyGenerating = false;
        if (burnTime > 0) {
            burnTime--;
            if (energyStorage.amount < energyStorage.capacity) {
                double rpmBonus = 1.0 + 0.25 * ((double) rpm / (double) MAX_RPM);
                int effectiveRf = (int) Math.round(currentRfPerTick * rpmBonus);
                long space = energyStorage.capacity - energyStorage.amount;
                energyStorage.amount += Math.min(space, effectiveRf);
                activelyGenerating = true;
            }
            changed = true;
        } else {
            currentRfPerTick = 0;
        }

        if (burnTime <= 0 && fluidAmount >= 100 && energyStorage.amount < energyStorage.capacity) {
            DieselFuelProfile profile = getFuelProfile(storedFluid);
            if (profile != null) {
                fluidAmount -= 100;
                if (fluidAmount <= 0) {
                    fluidAmount = 0;
                    storedFluid = FluidVariant.blank();
                }
                burnTime = maxBurnTime = profile.durationPer100Mb();
                currentRfPerTick = profile.rfPerTick();
                activelyGenerating = true;
                changed = true;
                needClientSync = true;
            }
        }

        int prevRpm = rpm;
        if (activelyGenerating || burnTime > 0) {
            rpm = Math.min(MAX_RPM, rpm + 4);
        } else {
            rpm = Math.max(0, rpm - 6);
        }
        if (rpm != prevRpm) {
            changed = true;
            if (level.getGameTime() % 5 == 0 || rpm == 0 || rpm == MAX_RPM) {
                needClientSync = true;
            }
        }

        boolean isLitNow = burnTime > 0;
        if (state.hasProperty(VolcanicDieselGeneratorBlock.LIT) && state.getValue(VolcanicDieselGeneratorBlock.LIT) != isLitNow) {
            level.setBlock(pos, state.setValue(VolcanicDieselGeneratorBlock.LIT, isLitNow), 3);
            needClientSync = true;
        }

        ItemStack batteryStack = items.get(2);
        if (!batteryStack.isEmpty() && energyStorage.amount > 0) {
            if (batteryStack.getItem() instanceof SimpleEnergyItem energyItem) {
                long stored = energyItem.getStoredEnergy(batteryStack);
                long capacity = energyItem.getEnergyCapacity(batteryStack);
                long maxIn = energyItem.getEnergyMaxInput(batteryStack);
                long transfer = Math.min(Math.min(energyStorage.amount, MAX_EXTRACT_RATE), Math.min(capacity - stored, maxIn));
                if (transfer > 0) {
                    energyStorage.amount -= transfer;
                    energyItem.setStoredEnergy(batteryStack, stored + transfer);
                    changed = true;
                }
            } else {
                EnergyStorage itemEnergy = ContainerItemContext.withConstant(batteryStack).find(EnergyStorage.ITEM);
                if (itemEnergy != null && itemEnergy.supportsInsertion()) {
                    try (Transaction tx = Transaction.openOuter()) {
                        long maxPush = Math.min(energyStorage.amount, MAX_EXTRACT_RATE);
                        long accepted = itemEnergy.insert(maxPush, tx);
                        if (accepted > 0) {
                            energyStorage.amount -= accepted;
                            tx.commit();
                            changed = true;
                        }
                    }
                }
            }
        }

        if (energyStorage.amount > 0) {
            for (Direction dir : Direction.values()) {
                EnergyStorage target = EnergyStorage.SIDED.find(level, pos.relative(dir), dir.getOpposite());
                if (target != null && target.supportsInsertion()) {
                    long moved = EnergyStorageUtil.move(energyStorage, target, MAX_EXTRACT_RATE, null);
                    if (moved > 0) changed = true;
                }
            }
        }

        if (changed) {
            setChanged();
        }
        if (needClientSync) {
            syncToClient();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putInt("BurnTime", this.burnTime);
        tag.putInt("MaxBurnTime", this.maxBurnTime);
        tag.putInt("CurrentRfPerTick", this.currentRfPerTick);
        tag.putInt("FluidAmount", this.fluidAmount);
        tag.putInt("Rpm", this.rpm);
        tag.putInt("FluidType", getFluidTypeId(this.storedFluid));
        tag.putLong("Energy", this.energyStorage.amount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items.clear();
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.burnTime = tag.getInt("BurnTime");
        this.maxBurnTime = tag.getInt("MaxBurnTime");
        this.currentRfPerTick = tag.getInt("CurrentRfPerTick");
        this.fluidAmount = tag.getInt("FluidAmount");
        this.rpm = tag.getInt("Rpm");
        this.energyStorage.amount = tag.getLong("Energy");
        int fType = tag.getInt("FluidType");
        this.storedFluid = switch (fType) {
            case 1 -> FluidVariant.of(ObsidiumMod.BIO_OIL_FLUID);
            case 2 -> FluidVariant.of(ObsidiumMod.BIO_ETHANOL_FLUID);
            case 3 -> FluidVariant.of(ObsidiumMod.BIODIESEL_FLUID);
            case 4 -> FluidVariant.of(ObsidiumMod.PYRO_DIESEL_FLUID);
            default -> FluidVariant.blank();
        };
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.volcanic_diesel_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new VolcanicDieselGeneratorMenu(syncId, playerInventory, this, this.dataAccess);
    }

    @Override
    public Container getInventory() {
        return this;
    }

    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : items) {
            if (!s.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack res = ContainerHelper.removeItem(items, slot, amount);
        if (!res.isEmpty()) setChanged();
        return res;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == 0) return getFluidFromBucket(stack) != null;
        if (slot == 2) return stack.getItem() instanceof SimpleEnergyItem;
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) return new int[]{1};
        return new int[]{0};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 0 && getFluidFromBucket(stack) != null;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 1 && stack.is(Items.BUCKET);
    }

    @Override
    public boolean isInputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 0;
    }

    @Override
    public boolean isOutputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 1;
    }
}

