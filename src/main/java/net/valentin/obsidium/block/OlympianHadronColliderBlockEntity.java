package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.VolcaniteBottleItem;
import net.valentin.obsidium.sound.ModSounds;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * BlockEntity for the 5x5 Olympian Hadron Collider (CERN-style particle
 * accelerator & fusion reactor).
 * Accelerates opposing particle beams (Beam A & Beam B) up to 99.9% c while
 * generating continuous
 * synchrotron RF/t, then collides them for massive RF bursts + Minecraft
 * physics transmutation outputs.
 */
public class OlympianHadronColliderBlockEntity extends BlockEntity
        implements MenuProvider, InventoryHolder, MachineInventory, WorldlyContainer {
    public static final int INVENTORY_SIZE = 7;
    public static final int SLOT_BEAM_A = 0;
    public static final int SLOT_BEAM_B = 1;
    public static final int SLOT_OUTPUT_MAIN = 2;
    public static final int SLOT_OUTPUT_SECOND = 3;
    public static final int SLOT_FUEL_IN = 4;
    public static final int SLOT_COOLANT_IN = 5;
    public static final int SLOT_CONTAINER_OUT = 6;

    public static final int MAX_FUEL = 16_000;
    public static final int MAX_COOLANT = 16_000;
    public static final long MAX_ENERGY = 100_000_000L;
    public static final long MAX_EXTRACT = 5_000_000L;
    public static final int MAX_VELOCITY = 1000;
    public static final int DATA_COUNT = 19;

    public static final int FUEL_NONE = 0;
    public static final int FUEL_LAVA = 1;
    public static final int FUEL_BIO_OIL = 2;
    public static final int FUEL_BIODIESEL = 3;
    public static final int FUEL_DISTILLATE = 4;
    public static final int FUEL_PYRO_DIESEL = 5;

    public static final int COOLANT_NONE = 0;
    public static final int COOLANT_WATER = 1;
    public static final int COOLANT_BIO_ETHANOL = 2;

    public static final int MODE_AUTO_COLLIDE = 0;
    public static final int MODE_STORAGE_RING = 1;

    private static final int[] ALL_SLOTS = { 0, 1, 2, 3, 4, 5, 6 };

    private boolean isFormed = false;
    private int ringOffsetX = 0;
    private int ringOffsetZ = 2;
    private int coilCount = 0;

    private int beamVelocity = 0;
    private int coilTemp = 40;
    private int fuelAmount = 0;
    private int fuelType = FUEL_NONE;
    private int coolantAmount = 0;
    private int coolantType = COOLANT_NONE;
    private int currentRfPerTick = 0;
    private int operatingMode = MODE_AUTO_COLLIDE;
    private boolean showHologram = true;
    private int collisionFlashTicks = 0;
    private int totalCollisions = 0;
    private int syncTicker = 0;
    private int lastSyncedVel = 0;
    private int lastSyncedFlash = 0;
    private int formationStatusMask = 0;

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(MAX_ENERGY, 0, MAX_EXTRACT) {
        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            OlympianHadronColliderBlockEntity.this.setChanged();
            syncToClient();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_BEAM_A, SLOT_BEAM_B -> !isFuelContainerItem(stack) && !isCoolantContainerItem(stack);
                case SLOT_FUEL_IN -> isFuelContainerItem(stack);
                case SLOT_COOLANT_IN -> isCoolantContainerItem(stack) || isSolidCryoItem(stack);
                case SLOT_CONTAINER_OUT ->
                    EnergyStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack)) != null;
                default -> false;
            };
        }
    };

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            long amt = energyStorage.amount;
            return switch (index) {
                case 0 -> (int) ((amt >> 32) & 0xFFFF);
                case 1 -> (int) ((amt >> 16) & 0xFFFF);
                case 2 -> (int) (amt & 0xFFFF);
                case 3 -> isFormed ? 1 : 0;
                case 4 -> coilCount;
                case 5 -> beamVelocity;
                case 6 -> coilTemp;
                case 7 -> fuelAmount;
                case 8 -> fuelType;
                case 9 -> coolantAmount;
                case 10 -> coolantType;
                case 11 -> currentRfPerTick & 0xFFFF;
                case 12 -> (currentRfPerTick >> 16) & 0xFFFF;
                case 13 -> operatingMode;
                case 14 -> showHologram ? 1 : 0;
                case 15 -> collisionFlashTicks;
                case 16 -> totalCollisions & 0xFFFF;
                case 17 -> (totalCollisions >> 16) & 0xFFFF;
                case 18 -> formationStatusMask;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 3 -> isFormed = value != 0;
                case 4 -> coilCount = value;
                case 5 -> beamVelocity = value;
                case 6 -> coilTemp = value;
                case 7 -> fuelAmount = value;
                case 8 -> fuelType = value;
                case 9 -> coolantAmount = value;
                case 10 -> coolantType = value;
                case 13 -> operatingMode = value;
                case 14 -> showHologram = value != 0;
                case 15 -> collisionFlashTicks = value;
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public OlympianHadronColliderBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.OLYMPIAN_HADRON_COLLIDER_BE, pos, state);
        Direction facing = state.hasProperty(OlympianHadronColliderBlock.FACING)
                ? state.getValue(OlympianHadronColliderBlock.FACING)
                : Direction.NORTH;
        Direction toCenter = facing.getOpposite();
        this.ringOffsetX = toCenter.getStepX() * 2;
        this.ringOffsetZ = toCenter.getStepZ() * 2;
    }

    public boolean isFormed() {
        return isFormed;
    }

    public int getRingOffsetX() {
        return ringOffsetX;
    }

    public int getRingOffsetZ() {
        return ringOffsetZ;
    }

    public int getCoilCount() {
        return coilCount;
    }

    public int getBeamVelocity() {
        return beamVelocity;
    }

    public int getCoilTemp() {
        return coilTemp;
    }

    public int getFuelAmount() {
        return fuelAmount;
    }

    public int getFuelType() {
        return fuelType;
    }

    public int getCoolantAmount() {
        return coolantAmount;
    }

    public int getCoolantType() {
        return coolantType;
    }

    public int getCurrentRfPerTick() {
        return currentRfPerTick;
    }

    public int getOperatingMode() {
        return operatingMode;
    }

    public boolean isShowHologram() {
        return showHologram;
    }

    public int getCollisionFlashTicks() {
        return collisionFlashTicks;
    }

    public int getTotalCollisions() {
        return totalCollisions;
    }

    public void syncToClient() {
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(),
                    Block.UPDATE_ALL);
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    public static boolean isFuelContainerItem(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        return stack.is(Items.LAVA_BUCKET)
                || stack.is(ObsidiumMod.BIO_OIL_BUCKET)
                || stack.is(ObsidiumMod.BIODIESEL_BUCKET)
                || stack.is(ObsidiumMod.PYROCLASTIC_DISTILLATE_BUCKET)
                || stack.is(ObsidiumMod.PYRO_DIESEL_BUCKET)
                || stack.is(ObsidiumMod.PYROCLASTIC_FLASK)
                || stack.getItem() instanceof VolcaniteBottleItem;
    }

    public static boolean isCoolantContainerItem(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        return stack.is(Items.WATER_BUCKET)
                || stack.is(ObsidiumMod.BIO_ETHANOL_BUCKET);
    }

    public static boolean isSolidCryoItem(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        return stack.is(Items.BLUE_ICE)
                || stack.is(Items.PACKED_ICE)
                || stack.is(Items.ICE)
                || stack.is(Items.SNOW_BLOCK)
                || stack.is(Items.SNOWBALL)
                || stack.is(ObsidiumMod.DEW_DROPLET);
    }

    public static int getFuelTypeForFluid(FluidVariant variant) {
        if (variant == null || variant.isBlank())
            return FUEL_NONE;
        if (variant.isOf(ObsidiumMod.PYRO_DIESEL_FLUID))
            return FUEL_PYRO_DIESEL;
        if (variant.isOf(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID))
            return FUEL_DISTILLATE;
        if (variant.isOf(ObsidiumMod.BIODIESEL_FLUID))
            return FUEL_BIODIESEL;
        if (variant.isOf(ObsidiumMod.BIO_OIL_FLUID))
            return FUEL_BIO_OIL;
        if (variant.isOf(Fluids.LAVA))
            return FUEL_LAVA;
        return FUEL_NONE;
    }

    public static int getCoolantTypeForFluid(FluidVariant variant) {
        if (variant == null || variant.isBlank())
            return COOLANT_NONE;
        if (variant.isOf(ObsidiumMod.BIO_ETHANOL_FLUID))
            return COOLANT_BIO_ETHANOL;
        if (variant.isOf(Fluids.WATER))
            return COOLANT_WATER;
        return COOLANT_NONE;
    }

    public FluidVariant getFuelFluidVariant() {
        return switch (fuelType) {
            case FUEL_PYRO_DIESEL -> FluidVariant.of(ObsidiumMod.PYRO_DIESEL_FLUID);
            case FUEL_DISTILLATE -> FluidVariant.of(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID);
            case FUEL_BIODIESEL -> FluidVariant.of(ObsidiumMod.BIODIESEL_FLUID);
            case FUEL_BIO_OIL -> FluidVariant.of(ObsidiumMod.BIO_OIL_FLUID);
            case FUEL_LAVA -> FluidVariant.of(Fluids.LAVA);
            default -> FluidVariant.blank();
        };
    }

    public FluidVariant getCoolantFluidVariant() {
        return switch (coolantType) {
            case COOLANT_BIO_ETHANOL -> FluidVariant.of(ObsidiumMod.BIO_ETHANOL_FLUID);
            case COOLANT_WATER -> FluidVariant.of(Fluids.WATER);
            default -> FluidVariant.blank();
        };
    }

    public int insertFluid(FluidVariant variant, int maxMb, boolean simulate) {
        if (variant == null || variant.isBlank() || maxMb <= 0)
            return 0;

        int fType = getFuelTypeForFluid(variant);
        if (fType != FUEL_NONE) {
            if (fuelAmount > 0 && fuelType != fType)
                return 0;
            int toInsert = Math.min(maxMb, MAX_FUEL - fuelAmount);
            if (toInsert > 0 && !simulate) {
                fuelType = fType;
                fuelAmount += toInsert;
                setChanged();
                syncToClient();
            }
            return toInsert;
        }

        int cType = getCoolantTypeForFluid(variant);
        if (cType != COOLANT_NONE) {
            if (coolantAmount > 0 && coolantType != cType)
                return 0;
            int toInsert = Math.min(maxMb, MAX_COOLANT - coolantAmount);
            if (toInsert > 0 && !simulate) {
                coolantType = cType;
                coolantAmount += toInsert;
                setChanged();
                syncToClient();
            }
            return toInsert;
        }

        return 0;
    }

    /**
     * Returns true if pos lies on the 5x5 horizontal perimeter ring of this
     * controller.
     */
    public boolean isPosOnRing(BlockPos pos) {
        if (pos.getY() != this.worldPosition.getY())
            return false;
        int cx = this.worldPosition.getX() + this.ringOffsetX;
        int cz = this.worldPosition.getZ() + this.ringOffsetZ;
        int dx = Math.abs(pos.getX() - cx);
        int dz = Math.abs(pos.getZ() - cz);
        return Math.max(dx, dz) == 2;
    }

    public static boolean isValidRingMember(Block b) {
        return b == ObsidiumMod.SYNCHROTRON_RING_COIL_BLOCK
                || b == ObsidiumMod.HADRON_ENERGY_PORT_BLOCK
                || b == ObsidiumMod.HADRON_ITEM_PORT_BLOCK
                || b == ObsidiumMod.HADRON_FLUID_PORT_BLOCK
                || b == ObsidiumMod.ADVANCED_VOLCANITE_CASING_BLOCK
                || b == ObsidiumMod.VOLCANITE_ENERGY_UNIT_BLOCK;
    }

    private int checkRingCandidate(Level level, BlockPos center) {
        int coils = 0;
        int energyPorts = 0;
        int itemPorts = 0;
        int fluidPorts = 0;
        int otherAllowed = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos checkPos = center.offset(dx, 0, dz);
                int chebyshev = Math.max(Math.abs(dx), Math.abs(dz));
                if (chebyshev == 2) {
                    if (checkPos.equals(this.worldPosition))
                        continue;
                    Block b = level.getBlockState(checkPos).getBlock();
                    if (b == ObsidiumMod.SYNCHROTRON_RING_COIL_BLOCK) {
                        coils++;
                    } else if (b == ObsidiumMod.HADRON_ENERGY_PORT_BLOCK) {
                        energyPorts++;
                    } else if (b == ObsidiumMod.HADRON_ITEM_PORT_BLOCK) {
                        itemPorts++;
                    } else if (b == ObsidiumMod.HADRON_FLUID_PORT_BLOCK) {
                        fluidPorts++;
                    } else if (isValidRingMember(b)) {
                        otherAllowed++;
                    } else {
                        return -1;
                    }
                } else {
                    BlockState inner = level.getBlockState(checkPos);
                    if (!inner.isAir() && inner.canOcclude()) {
                        return -1;
                    }
                }
            }
        }
        int totalNonCoils = energyPorts + itemPorts + fluidPorts + otherAllowed;
        if (energyPorts >= 1 && itemPorts >= 1 && fluidPorts >= 1
                && coils >= 9 && (coils + totalNonCoils) == 15) {
            return coils;
        }
        return -1;
    }

    private int computeDiagnosticMask(Level level, BlockPos center) {
        int coils = 0;
        int energyPorts = 0;
        int itemPorts = 0;
        int fluidPorts = 0;
        boolean badPerimeter = false;
        boolean innerBlocked = false;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos checkPos = center.offset(dx, 0, dz);
                int chebyshev = Math.max(Math.abs(dx), Math.abs(dz));
                if (chebyshev == 2) {
                    if (checkPos.equals(this.worldPosition))
                        continue;
                    Block b = level.getBlockState(checkPos).getBlock();
                    if (b == ObsidiumMod.SYNCHROTRON_RING_COIL_BLOCK)
                        coils++;
                    else if (b == ObsidiumMod.HADRON_ENERGY_PORT_BLOCK)
                        energyPorts++;
                    else if (b == ObsidiumMod.HADRON_ITEM_PORT_BLOCK)
                        itemPorts++;
                    else if (b == ObsidiumMod.HADRON_FLUID_PORT_BLOCK)
                        fluidPorts++;
                    else if (!isValidRingMember(b))
                        badPerimeter = true;
                } else {
                    BlockState inner = level.getBlockState(checkPos);
                    if (!inner.isAir() && inner.canOcclude()) {
                        innerBlocked = true;
                    }
                }
            }
        }

        int mask = (Math.min(15, coils) & 0xF) << 8;
        if (energyPorts == 0)
            mask |= 1;
        if (itemPorts == 0)
            mask |= 2;
        if (fluidPorts == 0)
            mask |= 4;
        if (coils < 9)
            mask |= 8;
        if (badPerimeter)
            mask |= 16;
        if (innerBlocked)
            mask |= 32;
        return mask;
    }

    public void clearRingFormedState(Level level) {
        if (level == null || level.isClientSide() || !this.isFormed)
            return;
        BlockPos center = this.worldPosition.offset(this.ringOffsetX, 0, this.ringOffsetZ);
        updateRingBlockStates(level, center, false);
    }

    private void updateRingBlockStates(Level level, BlockPos center, boolean formedState) {
        if (level == null || level.isClientSide())
            return;
        BlockState ctrlState = level.getBlockState(this.worldPosition);
        if (ctrlState.hasProperty(OlympianHadronColliderBlock.FORMED)
                && ctrlState.getValue(OlympianHadronColliderBlock.FORMED) != formedState) {
            level.setBlock(this.worldPosition, ctrlState.setValue(OlympianHadronColliderBlock.FORMED, formedState),
                    Block.UPDATE_ALL);
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != 2)
                    continue;
                BlockPos ringPos = center.offset(dx, 0, dz);
                if (ringPos.equals(this.worldPosition))
                    continue;
                BlockState st = level.getBlockState(ringPos);
                if (st.hasProperty(SynchrotronRingCoilBlock.FORMED)
                        && st.getValue(SynchrotronRingCoilBlock.FORMED) != formedState) {
                    level.setBlock(ringPos, st.setValue(SynchrotronRingCoilBlock.FORMED, formedState),
                            Block.UPDATE_ALL);
                } else if (st.hasProperty(HadronColliderPortBlock.FORMED)
                        && st.getValue(HadronColliderPortBlock.FORMED) != formedState) {
                    level.setBlock(ringPos, st.setValue(HadronColliderPortBlock.FORMED, formedState), Block.UPDATE_ALL);
                }
            }
        }
    }

    public void evaluateMultiblock(Level level) {
        Direction facing = getBlockState().hasProperty(OlympianHadronColliderBlock.FACING)
                ? getBlockState().getValue(OlympianHadronColliderBlock.FACING)
                : Direction.NORTH;
        Direction preferred = facing.getOpposite();
        Direction[] dirs = {
                preferred,
                preferred.getClockWise(),
                preferred.getCounterClockWise(),
                facing
        };

        boolean found = false;
        int foundOffX = preferred.getStepX() * 2;
        int foundOffZ = preferred.getStepZ() * 2;
        int foundCoils = 0;

        for (Direction d : dirs) {
            BlockPos center = this.worldPosition.relative(d, 2);
            int coils = checkRingCandidate(level, center);
            if (coils >= 0) {
                found = true;
                foundOffX = d.getStepX() * 2;
                foundOffZ = d.getStepZ() * 2;
                foundCoils = coils;
                break;
            }
        }

        if (found) {
            this.formationStatusMask = 0;
        } else {
            this.formationStatusMask = computeDiagnosticMask(level, this.worldPosition.relative(preferred, 2));
        }

        if (found != this.isFormed || foundOffX != this.ringOffsetX || foundOffZ != this.ringOffsetZ
                || foundCoils != this.coilCount) {
            boolean wasFormed = this.isFormed;
            if (wasFormed && (!found || foundOffX != this.ringOffsetX || foundOffZ != this.ringOffsetZ)) {
                updateRingBlockStates(level, this.worldPosition.offset(this.ringOffsetX, 0, this.ringOffsetZ), false);
            }
            this.isFormed = found;
            this.ringOffsetX = foundOffX;
            this.ringOffsetZ = foundOffZ;
            this.coilCount = foundCoils;

            if (found) {
                updateRingBlockStates(level, this.worldPosition.offset(this.ringOffsetX, 0, this.ringOffsetZ), true);
            }

            if (found && !wasFormed) {
                level.playSound(null, this.worldPosition, ModSounds.MULTIBLOCK_FORM, SoundSource.BLOCKS, 1.0F, 1.15F);
                if (level instanceof ServerLevel sl) {
                    BlockPos c = this.worldPosition.offset(this.ringOffsetX, 0, this.ringOffsetZ);
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.getX() + 0.5, c.getY() + 0.8, c.getZ() + 0.5, 40,
                            2.0, 0.3, 2.0, 0.08);
                }
            } else if (!found && wasFormed) {
                this.beamVelocity = 0;
                this.currentRfPerTick = 0;
                level.playSound(null, this.worldPosition, ModSounds.MULTIBLOCK_BREAK, SoundSource.BLOCKS, 0.9F, 0.9F);
            }
            setChanged();
            syncToClient();
        } else if (this.isFormed && this.syncTicker % 20 == 0) {
            updateRingBlockStates(level, this.worldPosition.offset(this.ringOffsetX, 0, this.ringOffsetZ), true);
        }
    }

    /**
     * Places missing Energy/Item/Fluid Ports and Synchrotron Ring Coils around the
     * preferred 5x5 ring behind the controller.
     */
    public void tryAutoBuildRing(Player player) {
        if (this.level == null || this.level.isClientSide() || this.isFormed)
            return;

        Direction facing = getBlockState().hasProperty(OlympianHadronColliderBlock.FACING)
                ? getBlockState().getValue(OlympianHadronColliderBlock.FACING)
                : Direction.NORTH;
        Direction toCenter = facing.getOpposite();
        BlockPos center = this.worldPosition.relative(toCenter, 2);

        boolean hasEnergyPort = false;
        boolean hasItemPort = false;
        boolean hasFluidPort = false;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != 2)
                    continue;
                BlockPos pos = center.offset(dx, 0, dz);
                Block b = this.level.getBlockState(pos).getBlock();
                if (b == ObsidiumMod.HADRON_ENERGY_PORT_BLOCK)
                    hasEnergyPort = true;
                else if (b == ObsidiumMod.HADRON_ITEM_PORT_BLOCK)
                    hasItemPort = true;
                else if (b == ObsidiumMod.HADRON_FLUID_PORT_BLOCK)
                    hasFluidPort = true;
            }
        }

        BlockPos energyPortTarget = center.relative(toCenter, 2);
        BlockPos itemPortTarget = center.relative(toCenter.getCounterClockWise(), 2);
        BlockPos fluidPortTarget = center.relative(toCenter.getClockWise(), 2);

        int placed = 0;

        if (!hasEnergyPort) {
            if (!tryPlaceRequiredPort(player, energyPortTarget, ObsidiumMod.HADRON_ENERGY_PORT_BLOCK,
                    ObsidiumMod.HADRON_ENERGY_PORT_ITEM, "Hadron Energy Port")) {
                evaluateMultiblock(this.level);
                return;
            }
            placed++;
        }
        if (!hasItemPort) {
            if (!tryPlaceRequiredPort(player, itemPortTarget, ObsidiumMod.HADRON_ITEM_PORT_BLOCK,
                    ObsidiumMod.HADRON_ITEM_PORT_ITEM, "Hadron Item Port")) {
                evaluateMultiblock(this.level);
                return;
            }
            placed++;
        }
        if (!hasFluidPort) {
            if (!tryPlaceRequiredPort(player, fluidPortTarget, ObsidiumMod.HADRON_FLUID_PORT_BLOCK,
                    ObsidiumMod.HADRON_FLUID_PORT_ITEM, "Hadron Fluid Port")) {
                evaluateMultiblock(this.level);
                return;
            }
            placed++;
        }

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != 2)
                    continue;
                BlockPos target = center.offset(dx, 0, dz);
                if (target.equals(this.worldPosition))
                    continue;
                BlockState existing = this.level.getBlockState(target);
                if (isValidRingMember(existing.getBlock()))
                    continue;
                if (!existing.isAir() && !existing.canBeReplaced())
                    continue;

                boolean hasCoil = player.isCreative()
                        || consumeItemFromPlayer(player, ObsidiumMod.SYNCHROTRON_RING_COIL_ITEM);
                if (!hasCoil) {
                    player.displayClientMessage(Component.literal(
                            "§c⚛ Need more Synchrotron Ring Coils in inventory! (Placed " + placed + " blocks)"), true);
                    evaluateMultiblock(this.level);
                    return;
                }
                this.level.setBlock(target, ObsidiumMod.SYNCHROTRON_RING_COIL_BLOCK.defaultBlockState(),
                        Block.UPDATE_ALL);
                placed++;
            }
        }
        evaluateMultiblock(this.level);
        if (placed > 0) {
            player.displayClientMessage(
                    Component.literal("§b⚛ Assembled 5×5 Collider Ring (12 Coils + Energy, Item & Fluid Ports)!"),
                    true);
        }
    }

    private boolean tryPlaceRequiredPort(Player player, BlockPos preferredTarget, Block portBlock, Item portItem,
            String portName) {
        if (this.level == null)
            return false;
        BlockState existing = this.level.getBlockState(preferredTarget);
        boolean replacingCoil = existing.is(ObsidiumMod.SYNCHROTRON_RING_COIL_BLOCK);
        if (!existing.isAir() && !existing.canBeReplaced() && !replacingCoil) {
            player.displayClientMessage(
                    Component.literal(
                            "§c⚛ Clear obstruction at " + preferredTarget.toShortString() + " for " + portName + "!"),
                    true);
            return false;
        }
        boolean hasPort = player.isCreative() || consumeItemFromPlayer(player, portItem);
        if (!hasPort) {
            player.displayClientMessage(
                    Component.literal("§c⚛ Need 1× " + portName + " in inventory to form the 5×5 Collider!"), true);
            return false;
        }
        if (replacingCoil && !player.isCreative()) {
            ItemStack refund = new ItemStack(ObsidiumMod.SYNCHROTRON_RING_COIL_ITEM);
            if (!player.getInventory().add(refund)) {
                player.drop(refund, false);
            }
        }
        this.level.setBlock(preferredTarget, portBlock.defaultBlockState(), Block.UPDATE_ALL);
        return true;
    }

    private static boolean consumeItemFromPlayer(Player player, Item item) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (!st.isEmpty() && st.is(item)) {
                st.shrink(1);
                return true;
            }
        }
        return false;
    }

    public void toggleOperatingMode() {
        this.operatingMode = (this.operatingMode == MODE_AUTO_COLLIDE) ? MODE_STORAGE_RING : MODE_AUTO_COLLIDE;
        setChanged();
        syncToClient();
    }

    public void toggleHologram() {
        this.showHologram = !this.showHologram;
        setChanged();
        syncToClient();
    }

    private record CollisionOutcome(int costA, int costB, ItemStack primary, ItemStack secondary, long burstRf) {
    }

    private static boolean matchesAny(Item item, Item... candidates) {
        for (Item c : candidates) {
            if (item == c)
                return true;
        }
        return false;
    }

    private static int @Nullable [] matchOrderedCosts(Item a, Item b,
            java.util.function.Predicate<Item> first, int costFirst,
            java.util.function.Predicate<Item> second, int costSecond) {
        if (first.test(a) && second.test(b)) {
            return new int[] { costFirst, costSecond };
        }
        if (first.test(b) && second.test(a)) {
            return new int[] { costSecond, costFirst };
        }
        return null;
    }

    private CollisionOutcome computeCollisionOutcome(ItemStack stackA, ItemStack stackB) {
        long baseBurst = switch (fuelType) {
            case FUEL_PYRO_DIESEL -> 15_000_000L;
            case FUEL_DISTILLATE -> 10_000_000L;
            case FUEL_BIODIESEL -> 6_000_000L;
            default -> 3_000_000L;
        };
        double velScale = Math.max(0.3, beamVelocity / (double) MAX_VELOCITY);
        double cryoMult = coilTemp < 120 ? 1.3 : 1.0;
        long scaledBurst = Math.round(baseBurst * velScale * cryoMult * (1.0 + (coilCount + 3) * 0.05));

        Item a = stackA.getItem();
        Item b = stackB.getItem();
        int[] costs;

        if ((costs = matchOrderedCosts(a, b,
                x -> x == ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 4,
                x -> x == ObsidiumMod.ASTRAL_VOLCANIC_CATALYST, 4)) != null) {
            return new CollisionOutcome(costs[0], costs[1],
                    new ItemStack(ObsidiumMod.SINGULARITY_HADRON_ALLOY, 1),
                    new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 2),
                    Math.round(scaledBurst * 2.5));
        }

        if ((costs = matchOrderedCosts(a, b,
                x -> matchesAny(x, Items.COAL_BLOCK, ObsidiumMod.ASH_BRIQUETTE), 4,
                x -> x == Items.IRON_BLOCK, 2)) != null) {
            return new CollisionOutcome(costs[0], costs[1], new ItemStack(Items.DIAMOND, 2),
                    new ItemStack(ObsidiumMod.VOLCANIC_ASH, 2), scaledBurst);
        }
        if ((costs = matchOrderedCosts(a, b,
                x -> x == Items.DIAMOND, 4,
                x -> matchesAny(x, Items.GOLD_BLOCK, Items.NETHERITE_SCRAP), 2)) != null) {
            return new CollisionOutcome(costs[0], costs[1], new ItemStack(Items.NETHERITE_INGOT, 1),
                    new ItemStack(ObsidiumMod.VOLCANIC_ASH, 2), scaledBurst);
        }
        if ((costs = matchOrderedCosts(a, b,
                x -> matchesAny(x, Items.COPPER_BLOCK, ObsidiumMod.PYRIC_BRONZE_INGOT), 4,
                x -> matchesAny(x, Items.GOLD_INGOT, ObsidiumMod.SILVER_INGOT), 4)) != null) {
            return new CollisionOutcome(costs[0], costs[1], new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 2),
                    new ItemStack(ObsidiumMod.VOLCANITE_INGOT, 2), scaledBurst);
        }
        if ((costs = matchOrderedCosts(a, b,
                x -> x == ObsidiumMod.VOLCANIC_ALLOY, 4,
                x -> matchesAny(x, ObsidiumMod.VOLCANITE_THERMO_CORE, ObsidiumMod.CINDER_CORE), 2)) != null) {
            return new CollisionOutcome(costs[0], costs[1], new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 1),
                    new ItemStack(ObsidiumMod.VOLCANITE_DUST, 2), (long) (scaledBurst * 1.25));
        }
        if ((costs = matchOrderedCosts(a, b,
                x -> matchesAny(x, Items.ENDER_EYE, Items.ENDER_PEARL), 4,
                x -> matchesAny(x, ObsidiumMod.SUPREME_VOLCANIC_ALLOY, ObsidiumMod.CINDER_CORE), 2)) != null) {
            return new CollisionOutcome(costs[0], costs[1], new ItemStack(ObsidiumMod.ASTRAL_VOLCANIC_CATALYST, 1),
                    new ItemStack(Items.DIAMOND, 2), (long) (scaledBurst * 1.5));
        }
        if ((costs = matchOrderedCosts(a, b,
                x -> matchesAny(x, Items.IRON_INGOT, ObsidiumMod.IRON_DUST), 4,
                x -> matchesAny(x, Items.REDSTONE_BLOCK, Items.GLOWSTONE), 4)) != null) {
            return new CollisionOutcome(costs[0], costs[1], new ItemStack(ObsidiumMod.VOLCANITE_INGOT, 2),
                    new ItemStack(ObsidiumMod.SILVER_INGOT, 2), scaledBurst);
        }
        if ((costs = matchOrderedCosts(a, b,
                x -> matchesAny(x, ObsidiumMod.TIN_INGOT, ObsidiumMod.TIN_DUST), 4,
                x -> matchesAny(x, Items.COPPER_INGOT, ObsidiumMod.COPPER_DUST), 4)) != null) {
            return new CollisionOutcome(costs[0], costs[1], new ItemStack(ObsidiumMod.PYRIC_BRONZE_INGOT, 4),
                    new ItemStack(ObsidiumMod.LEAD_INGOT, 1), scaledBurst);
        }
        if ((costs = matchOrderedCosts(a, b,
                x -> matchesAny(x, ObsidiumMod.LEAD_INGOT, ObsidiumMod.LEAD_DUST), 4,
                x -> matchesAny(x, Items.IRON_INGOT, ObsidiumMod.IRON_DUST), 4)) != null) {
            return new CollisionOutcome(costs[0], costs[1], new ItemStack(ObsidiumMod.FEROPLUMB_INGOT, 4),
                    new ItemStack(ObsidiumMod.SILVER_INGOT, 1), scaledBurst);
        }
        if (ItemStack.isSameItem(stackA, stackB)) {
            // Matter-Antimatter Annihilation: 2x Burst RF!
            return new CollisionOutcome(4, 4, new ItemStack(ObsidiumMod.VOLCANIC_ASH, 2),
                    new ItemStack(ObsidiumMod.VOLCANITE_NUGGET, 2),
                    Math.max(3_000_000L, Math.round(scaledBurst * 2.0)));
        }
        return null;
    }

    private boolean canFitOutput(int slot, ItemStack toInsert) {
        if (toInsert.isEmpty())
            return true;
        ItemStack existing = inventory.getItem(slot);
        if (existing.isEmpty())
            return true;
        return ItemStack.isSameItemSameComponents(existing, toInsert)
                && existing.getCount() + toInsert.getCount() <= existing.getMaxStackSize();
    }

    private void insertIntoOutputSlot(int slot, ItemStack toInsert) {
        if (toInsert.isEmpty())
            return;
        ItemStack existing = inventory.getItem(slot);
        if (existing.isEmpty()) {
            inventory.setItem(slot, toInsert.copy());
        } else {
            existing.grow(toInsert.getCount());
        }
    }

    private void insertOrDropRareOutput(ServerLevel sl, ItemStack stack, double dropX, double dropY, double dropZ) {
        if (stack.isEmpty())
            return;
        if (canFitOutput(SLOT_OUTPUT_MAIN, stack)) {
            insertIntoOutputSlot(SLOT_OUTPUT_MAIN, stack);
            return;
        }
        if (canFitOutput(SLOT_OUTPUT_SECOND, stack)) {
            insertIntoOutputSlot(SLOT_OUTPUT_SECOND, stack);
            return;
        }
        ItemEntity drop = new ItemEntity(sl, dropX, dropY, dropZ, stack.copy());
        drop.setDefaultPickUpDelay();
        drop.setInvulnerable(true);
        drop.setGlowingTag(true);
        sl.addFreshEntity(drop);
    }

    private void notifyCollisionPlayers(ServerLevel sl, @Nullable Player caller, BlockPos center, Component message) {
        if (caller != null) {
            caller.displayClientMessage(message, true);
            return;
        }
        AABB box = new AABB(center).inflate(18.0);
        for (ServerPlayer sp : sl.getEntitiesOfClass(ServerPlayer.class, box)) {
            sp.displayClientMessage(message, true);
        }
    }

    public boolean triggerCollision(@Nullable Player caller) {
        if (!(this.level instanceof ServerLevel sl) || !this.isFormed || this.beamVelocity < 300) {
            if (caller != null && !this.level.isClientSide()) {
                caller.displayClientMessage(
                        Component.literal("§c⚛ Need >= 30.0% c velocity and a formed 5×5 ring to collide!"), true);
            }
            return false;
        }
        ItemStack stackA = inventory.getItem(SLOT_BEAM_A);
        ItemStack stackB = inventory.getItem(SLOT_BEAM_B);
        if (stackA.isEmpty() || stackB.isEmpty()) {
            if (caller != null) {
                caller.displayClientMessage(Component.literal("§c⚛ Load particles into both Beam A and Beam B slots!"),
                        true);
            }
            return false;
        }

        CollisionOutcome outcome = computeCollisionOutcome(stackA, stackB);
        if (outcome == null) {
            if (caller != null) {
                caller.displayClientMessage(Component
                        .literal("§c⚛ Incompatible particles! Check [?] or Manual for valid collision recipes."), true);
            }
            return false;
        }
        if (stackA.getCount() < outcome.costA() || stackB.getCount() < outcome.costB()) {
            if (caller != null) {
                caller.displayClientMessage(Component.literal("§c⚛ Need at least " + outcome.costA()
                        + "× in Beam A and " + outcome.costB() + "× in Beam B to collide!"), true);
            }
            return false;
        }

        boolean isSingularityAttempt = outcome.primary().is(ObsidiumMod.SINGULARITY_HADRON_ALLOY);
        ItemStack decayPrimary = new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 2);
        ItemStack decaySecondary = new ItemStack(ObsidiumMod.VOLCANITE_DUST, 4);

        if (isSingularityAttempt) {
            if (!canFitOutput(SLOT_OUTPUT_MAIN, decayPrimary) || !canFitOutput(SLOT_OUTPUT_SECOND, decaySecondary)) {
                if (caller != null) {
                    caller.displayClientMessage(
                            Component.literal("§e⚛ Output slots full! Empty collision outputs first."), true);
                }
                return false;
            }
        } else if (!canFitOutput(SLOT_OUTPUT_MAIN, outcome.primary())
                || !canFitOutput(SLOT_OUTPUT_SECOND, outcome.secondary())) {
            if (caller != null) {
                caller.displayClientMessage(Component.literal("§e⚛ Output slots full! Empty collision outputs first."),
                        true);
            }
            return false;
        }

        BlockPos center = this.worldPosition.offset(this.ringOffsetX, 0, this.ringOffsetZ);
        double cx = center.getX() + 0.5;
        double cy = center.getY() + 0.7;
        double cz = center.getZ() + 0.5;

        boolean cryoSuperconductingBonus = !isSingularityAttempt && this.beamVelocity >= 900 && this.coilTemp < 120;
        boolean wonSingularity = false;

        stackA.shrink(outcome.costA());
        stackB.shrink(outcome.costB());

        if (isSingularityAttempt) {
            wonSingularity = sl.random.nextFloat() < 0.10f;
            if (wonSingularity) {
                insertOrDropRareOutput(sl, outcome.primary(), this.worldPosition.getX() + 0.5,
                        this.worldPosition.getY() + 1.25, this.worldPosition.getZ() + 0.5);
                insertOrDropRareOutput(sl, outcome.secondary(), this.worldPosition.getX() + 0.5,
                        this.worldPosition.getY() + 1.25, this.worldPosition.getZ() + 0.5);
            } else {
                insertIntoOutputSlot(SLOT_OUTPUT_MAIN, decayPrimary);
                insertIntoOutputSlot(SLOT_OUTPUT_SECOND, decaySecondary);
            }
        } else {
            insertIntoOutputSlot(SLOT_OUTPUT_MAIN, outcome.primary());
            insertIntoOutputSlot(SLOT_OUTPUT_SECOND, outcome.secondary());
            if (cryoSuperconductingBonus && sl.random.nextFloat() < 0.10f) {
                wonSingularity = true;
                insertOrDropRareOutput(sl, new ItemStack(ObsidiumMod.SINGULARITY_HADRON_ALLOY, 1),
                        this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.25,
                        this.worldPosition.getZ() + 0.5);
            }
        }

        long space = Math.max(0L, MAX_ENERGY - energyStorage.amount);
        energyStorage.amount += Math.min(space, outcome.burstRf());

        this.beamVelocity = 250; // Keep 25% orbital momentum after smash
        this.collisionFlashTicks = 30;
        this.totalCollisions++;

        sl.playSound(null, center, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.85F, 1.45F);
        sl.playSound(null, center, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.9F, 1.85F);
        sl.sendParticles(ParticleTypes.FLASH, cx, cy, cz, 2, 0.0, 0.0, 0.0, 0.0);
        sl.sendParticles(ParticleTypes.END_ROD, cx, cy, cz, 45, 0.3, 0.3, 0.3, 0.18);
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx, cy, cz, 60, 0.6, 0.3, 0.6, 0.25);

        if (wonSingularity) {
            sl.playSound(null, center, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0F, 1.05F);
            sl.playSound(null, center, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.7F, 1.5F);
            sl.sendParticles(ParticleTypes.DRAGON_BREATH, cx, cy, cz, 60, 0.8, 0.4, 0.8, 0.05);
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, cx, cy + 0.3, cz, 50, 0.7, 0.6, 0.7, 0.2);
            notifyCollisionPlayers(sl, caller, center, Component.literal(
                    String.format("§d✦ QUANTUM MIRACLE (10%%)! Synthesized Singularity Hadron Alloy! +%,d RF",
                            outcome.burstRf())));
        } else if (isSingularityAttempt) {
            notifyCollisionPlayers(sl, caller, center, Component.literal(
                    String.format("§5⚛ Singularity decayed (10%% chance for Singularity Hadron Alloy). +%,d RF",
                            outcome.burstRf())));
        } else if (caller != null) {
            caller.displayClientMessage(
                    Component.literal(String.format("§b💥 HADRON COLLISION! +%,d RF Burst!", outcome.burstRf())), true);
        }
        pushItemsOut(sl);
        setChanged();
        syncToClient();
        return true;
    }

    private double getFuelMultiplier() {
        return switch (fuelType) {
            case FUEL_PYRO_DIESEL -> 2.5;
            case FUEL_DISTILLATE -> 1.8;
            case FUEL_BIODIESEL -> 1.4;
            case FUEL_LAVA, FUEL_BIO_OIL -> 1.0;
            default -> 0.0;
        };
    }

    private void processSlotContainers() {
        // Fuel slot (Slot 4 -> Empty return Slot 6)
        ItemStack fuelStack = inventory.getItem(SLOT_FUEL_IN);
        ItemStack outContainer = inventory.getItem(SLOT_CONTAINER_OUT);
        if (!fuelStack.isEmpty()) {
            if (fuelStack.getItem() instanceof VolcaniteBottleItem) {
                int bottleLava = VolcaniteBottleItem.getLava(fuelStack);
                if (bottleLava > 0 && (fuelAmount == 0 || fuelType == FUEL_LAVA) && fuelAmount < MAX_FUEL) {
                    int move = Math.min(bottleLava, Math.min(1000, MAX_FUEL - fuelAmount));
                    fuelType = FUEL_LAVA;
                    fuelAmount += move;
                    VolcaniteBottleItem.setLava(fuelStack, bottleLava - move);
                    setChanged();
                }
            } else if (fuelStack.is(ObsidiumMod.PYROCLASTIC_FLASK)) {
                if ((fuelAmount == 0 || fuelType == FUEL_DISTILLATE) && fuelAmount + 250 <= MAX_FUEL
                        && (outContainer.isEmpty() || (outContainer.is(Items.GLASS_BOTTLE)
                                && outContainer.getCount() < outContainer.getMaxStackSize()))) {
                    fuelType = FUEL_DISTILLATE;
                    fuelAmount += 250;
                    fuelStack.shrink(1);
                    if (outContainer.isEmpty())
                        inventory.setItem(SLOT_CONTAINER_OUT, new ItemStack(Items.GLASS_BOTTLE));
                    else
                        outContainer.grow(1);
                    setChanged();
                }
            } else {
                int targetType = FUEL_NONE;
                if (fuelStack.is(Items.LAVA_BUCKET))
                    targetType = FUEL_LAVA;
                else if (fuelStack.is(ObsidiumMod.BIO_OIL_BUCKET))
                    targetType = FUEL_BIO_OIL;
                else if (fuelStack.is(ObsidiumMod.BIODIESEL_BUCKET))
                    targetType = FUEL_BIODIESEL;
                else if (fuelStack.is(ObsidiumMod.PYROCLASTIC_DISTILLATE_BUCKET))
                    targetType = FUEL_DISTILLATE;
                else if (fuelStack.is(ObsidiumMod.PYRO_DIESEL_BUCKET))
                    targetType = FUEL_PYRO_DIESEL;

                if (targetType != FUEL_NONE && (fuelAmount == 0 || fuelType == targetType)
                        && fuelAmount + 1000 <= MAX_FUEL
                        && (outContainer.isEmpty() || (outContainer.is(Items.BUCKET)
                                && outContainer.getCount() < outContainer.getMaxStackSize()))) {
                    fuelType = targetType;
                    fuelAmount += 1000;
                    fuelStack.shrink(1);
                    if (outContainer.isEmpty())
                        inventory.setItem(SLOT_CONTAINER_OUT, new ItemStack(Items.BUCKET));
                    else
                        outContainer.grow(1);
                    setChanged();
                }
            }
        }

        // Coolant slot (Slot 5 -> Buckets or Solid Cryo)
        ItemStack coolStack = inventory.getItem(SLOT_COOLANT_IN);
        outContainer = inventory.getItem(SLOT_CONTAINER_OUT);
        if (!coolStack.isEmpty()) {
            int bucketCoolType = COOLANT_NONE;
            if (coolStack.is(Items.WATER_BUCKET))
                bucketCoolType = COOLANT_WATER;
            else if (coolStack.is(ObsidiumMod.BIO_ETHANOL_BUCKET))
                bucketCoolType = COOLANT_BIO_ETHANOL;

            if (bucketCoolType != COOLANT_NONE && (coolantAmount == 0 || coolantType == bucketCoolType)
                    && coolantAmount + 1000 <= MAX_COOLANT
                    && (outContainer.isEmpty() || (outContainer.is(Items.BUCKET)
                            && outContainer.getCount() < outContainer.getMaxStackSize()))) {
                coolantType = bucketCoolType;
                coolantAmount += 1000;
                coolStack.shrink(1);
                if (outContainer.isEmpty())
                    inventory.setItem(SLOT_CONTAINER_OUT, new ItemStack(Items.BUCKET));
                else
                    outContainer.grow(1);
                setChanged();
            } else if (isSolidCryoItem(coolStack) && (coolantAmount == 0 || coolantType == COOLANT_WATER)) {
                int yieldMb = 0;
                int chillK = 0;
                if (coolStack.is(Items.BLUE_ICE)) {
                    yieldMb = 1000;
                    chillK = 25;
                } else if (coolStack.is(Items.PACKED_ICE)) {
                    yieldMb = 500;
                    chillK = 12;
                } else if (coolStack.is(Items.ICE)) {
                    yieldMb = 300;
                    chillK = 8;
                } else if (coolStack.is(Items.SNOW_BLOCK)) {
                    yieldMb = 250;
                    chillK = 5;
                } else if (coolStack.is(Items.SNOWBALL)) {
                    yieldMb = 100;
                    chillK = 2;
                } else if (coolStack.is(ObsidiumMod.DEW_DROPLET)) {
                    yieldMb = 400;
                    chillK = 10;
                }

                if (yieldMb > 0 && coolantAmount + yieldMb <= MAX_COOLANT) {
                    coolantType = COOLANT_WATER;
                    coolantAmount += yieldMb;
                    coilTemp = Math.max(20, coilTemp - chillK);
                    coolStack.shrink(1);
                    setChanged();
                }
            }
        }
    }

    private void chargeSlotItem() {
        ItemStack chargeTarget = inventory.getItem(SLOT_CONTAINER_OUT);
        if (chargeTarget.isEmpty() || energyStorage.amount <= 0)
            return;
        EnergyStorage itemEnergy = EnergyStorage.ITEM.find(chargeTarget,
                ContainerItemContext.withConstant(chargeTarget));
        if (itemEnergy == null)
            return;
        EnergyStorageUtil.move(energyStorage, itemEnergy, 250_000L, null);
    }

    private void pushEnergyOut(Level level) {
        if (energyStorage.amount <= 0 || !isFormed)
            return;
        BlockPos center = this.worldPosition.offset(this.ringOffsetX, 0, this.ringOffsetZ);

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != 2)
                    continue;
                BlockPos ringPos = center.offset(dx, 0, dz);
                for (Direction dir : Direction.values()) {
                    BlockPos neighborPos = ringPos.relative(dir);
                    if (isPosOnRing(neighborPos))
                        continue;
                    EnergyStorage target = EnergyStorage.SIDED.find(level, neighborPos, dir.getOpposite());
                    if (target != null && target != energyStorage) {
                        EnergyStorageUtil.move(energyStorage, target, MAX_EXTRACT, null);
                        if (energyStorage.amount <= 0)
                            return;
                    }
                }
            }
        }
    }

    private void pushItemsOut(Level level) {
        if (level == null || level.isClientSide())
            return;

        List<BlockPos> sourcePositions = new ArrayList<>();
        sourcePositions.add(this.worldPosition);
        if (this.isFormed) {
            BlockPos center = this.worldPosition.offset(this.ringOffsetX, 0, this.ringOffsetZ);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != 2)
                        continue;
                    BlockPos ringPos = center.offset(dx, 0, dz);
                    if (!ringPos.equals(this.worldPosition)) {
                        sourcePositions.add(ringPos);
                    }
                }
            }
        }

        int[] outSlots = { SLOT_OUTPUT_MAIN, SLOT_OUTPUT_SECOND, SLOT_CONTAINER_OUT };
        boolean changed = false;

        for (int slot : outSlots) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !isOutputSlot(slot, stack, null))
                continue;

            for (BlockPos sourcePos : sourcePositions) {
                for (Direction dir : Direction.values()) {
                    BlockPos neighborPos = sourcePos.relative(dir);
                    if (neighborPos.equals(this.worldPosition) || (this.isFormed && isPosOnRing(neighborPos)))
                        continue;

                    BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                    if (neighborBe instanceof VolcaniteModularChestBlockEntity modularChest) {
                        int orig = stack.getCount();
                        ItemStack rem = modularChest.insertStorageItem(stack);
                        if (rem.getCount() != orig) {
                            inventory.setItem(slot, rem.isEmpty() ? ItemStack.EMPTY : rem);
                            stack = inventory.getItem(slot);
                            changed = true;
                            if (stack.isEmpty())
                                break;
                        }
                    } else if (neighborBe instanceof VolcaniteStoragePipeBlockEntity) {
                        int orig = stack.getCount();
                        ItemStack rem = VolcaniteStoragePipeBlockEntity.routeItemIntoNetwork(
                                level, neighborPos, null, stack, Set.of(this.worldPosition, sourcePos), false);
                        if (rem.getCount() != orig) {
                            inventory.setItem(slot, rem.isEmpty() ? ItemStack.EMPTY : rem);
                            stack = inventory.getItem(slot);
                            changed = true;
                            if (stack.isEmpty())
                                break;
                        }
                    }
                }
                if (stack.isEmpty())
                    break;
            }
        }

        for (BlockPos sourcePos : sourcePositions) {
            if (VolcaniteStoragePipeBlockEntity.autoEjectMachineOutputs(level, sourcePos, this)) {
                changed = true;
            }
        }

        if (changed) {
            setChanged();
        }
    }

    private void damageEntitiesInCore(ServerLevel sl) {
        if (!this.isFormed)
            return;
        BlockPos center = this.worldPosition.offset(this.ringOffsetX, 0, this.ringOffsetZ);
        AABB coreBox = new AABB(
                center.getX() - 1.0, center.getY() - 0.25, center.getZ() - 1.0,
                center.getX() + 2.0, center.getY() + 2.25, center.getZ() + 2.0);
        List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, coreBox,
                ent -> ent.isAlive() && !ent.isSpectator() && !(ent instanceof Player p && p.isCreative()));
        if (victims.isEmpty())
            return;

        float dmg = this.beamVelocity > 0 ? (4.0F + (this.beamVelocity / 100.0F)) : 3.0F;
        for (LivingEntity e : victims) {
            e.hurt(sl.damageSources().lightningBolt(), dmg);
            if (this.beamVelocity > 0) {
                e.igniteForSeconds(3);
            }
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + 0.8, e.getZ(), 14, 0.3, 0.4, 0.3, 0.12);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, e.getX(), e.getY() + 0.5, e.getZ(), 8, 0.25, 0.3, 0.25,
                    0.04);
            sl.playSound(null, e.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.BLOCKS, 0.6F, 1.6F);
            if (e instanceof ServerPlayer sp) {
                sp.displayClientMessage(
                        Component.literal("§c☢ COLLIDER CORE HAZARD! High-energy radiation inside the 3×3 ring!"),
                        true);
            }
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, OlympianHadronColliderBlockEntity entity) {
        if (level.isClientSide())
            return;

        long previousEnergy = entity.energyStorage.amount;
        int previousFuel = entity.fuelAmount;
        int previousCoolant = entity.coolantAmount;
        int previousVelocity = entity.beamVelocity;
        int previousTemperature = entity.coilTemp;
        int previousFlashTicks = entity.collisionFlashTicks;

        entity.evaluateMultiblock(level);
        if (entity.collisionFlashTicks > 0) {
            entity.collisionFlashTicks--;
        }

        entity.processSlotContainers();
        entity.chargeSlotItem();
        if (level.getGameTime() % 2 == 0) {
            entity.pushItemsOut(level);
        }

        boolean accelerating = false;
        int producedRf = 0;

        if (entity.isFormed) {
            boolean hasBeams = !entity.inventory.getItem(SLOT_BEAM_A).isEmpty()
                    && !entity.inventory.getItem(SLOT_BEAM_B).isEmpty();
            int fuelCost = entity.beamVelocity > 700 ? 4 : 2;
            boolean hasFuel = entity.fuelAmount >= fuelCost && entity.fuelType != FUEL_NONE;

            if (hasBeams && hasFuel && entity.coilTemp < 650) {
                accelerating = true;
                entity.fuelAmount -= fuelCost;
                if (entity.fuelAmount <= 0) {
                    entity.fuelAmount = 0;
                    entity.fuelType = FUEL_NONE;
                }

                // Thermal balance: beams heat magnets, cryo coolant cools magnets
                int heatGain = 1 + (entity.beamVelocity / 350);
                entity.coilTemp = Math.min(800, entity.coilTemp + heatGain);

                if (entity.coolantAmount > 0 && entity.coilTemp > 35) {
                    int coolNeed = entity.coolantType == COOLANT_BIO_ETHANOL ? 2 : 3;
                    int dropK = entity.coolantType == COOLANT_BIO_ETHANOL ? 6 : 4;
                    if (entity.coolantAmount >= coolNeed) {
                        entity.coolantAmount -= coolNeed;
                        entity.coilTemp = Math.max(20, entity.coilTemp - dropK);
                        if (entity.coolantAmount <= 0) {
                            entity.coolantAmount = 0;
                            entity.coolantType = COOLANT_NONE;
                        }
                    }
                }

                boolean cryoLock = entity.coilTemp < 120;
                double fuelMult = entity.getFuelMultiplier();
                int velGain = Math.max(1, (int) Math.round(fuelMult * (cryoLock ? 2.5 : 1.0)));
                if (entity.beamVelocity < MAX_VELOCITY) {
                    entity.beamVelocity = Math.min(MAX_VELOCITY, entity.beamVelocity + velGain);
                }
            } else {
                // Coast down gradually if fuel/beams run out or magnets overheat
                if (entity.beamVelocity > 0) {
                    entity.beamVelocity = Math.max(0, entity.beamVelocity - 4);
                }
                if (entity.coolantAmount > 0 && entity.coilTemp > 45) {
                    int coolRate = entity.coolantType == COOLANT_BIO_ETHANOL ? 3 : 2;
                    entity.coolantAmount = Math.max(0, entity.coolantAmount - 1);
                    entity.coilTemp = Math.max(40, entity.coilTemp - coolRate);
                    if (entity.coolantAmount == 0)
                        entity.coolantType = COOLANT_NONE;
                } else if (entity.coilTemp > 110 && entity.syncTicker % 10 == 0) {
                    entity.coilTemp = Math.max(110, entity.coilTemp - 1);
                }
            }

            // Continuous Synchrotron Radiation Energy Production
            if (entity.beamVelocity > 0 && entity.fuelType != FUEL_NONE) {
                double velFrac = entity.beamVelocity / (double) MAX_VELOCITY;
                double fuelMult = entity.getFuelMultiplier();
                double cryoMult = entity.coilTemp < 120 ? 1.5 : (entity.coilTemp >= 550 ? 0.5 : 1.0);
                double coilMult = 1.0 + ((entity.coilCount + 3) * 0.10);
                producedRf = (int) Math.round(35_000.0 * velFrac * fuelMult * cryoMult * coilMult);
                long space = Math.max(0L, MAX_ENERGY - entity.energyStorage.amount);
                entity.energyStorage.amount += Math.min(space, producedRf);
            } else if (entity.beamVelocity > 0) {
                double velFrac = entity.beamVelocity / (double) MAX_VELOCITY;
                producedRf = (int) Math.round(15_000.0 * velFrac);
                long space = Math.max(0L, MAX_ENERGY - entity.energyStorage.amount);
                entity.energyStorage.amount += Math.min(space, producedRf);
            }

            // Auto-Collide at 99.9% c
            if (accelerating && entity.operatingMode == MODE_AUTO_COLLIDE && entity.beamVelocity >= MAX_VELOCITY) {
                entity.triggerCollision(null);
            }

            if (level instanceof ServerLevel sl && level.getGameTime() % 10 == 0) {
                entity.damageEntitiesInCore(sl);
            }

            entity.pushEnergyOut(level);
        }

        entity.currentRfPerTick = producedRf;
        boolean shouldBeLit = entity.isFormed && entity.beamVelocity > 0;
        BlockState currentState = level.getBlockState(pos);
        boolean activeDiffers = currentState.hasProperty(OlympianHadronColliderBlock.ACTIVE)
                && currentState.getValue(OlympianHadronColliderBlock.ACTIVE) != shouldBeLit;
        boolean formedDiffers = currentState.hasProperty(OlympianHadronColliderBlock.FORMED)
                && currentState.getValue(OlympianHadronColliderBlock.FORMED) != entity.isFormed;
        if (activeDiffers || formedDiffers) {
            BlockState updated = currentState;
            if (updated.hasProperty(OlympianHadronColliderBlock.ACTIVE)) {
                updated = updated.setValue(OlympianHadronColliderBlock.ACTIVE, shouldBeLit);
            }
            if (updated.hasProperty(OlympianHadronColliderBlock.FORMED)) {
                updated = updated.setValue(OlympianHadronColliderBlock.FORMED, entity.isFormed);
            }
            level.setBlock(pos, updated, Block.UPDATE_ALL);
        }

        // Continuous operation also changes saved data when no inventory/energy
        // transaction occurs.
        // Client synchronization alone does not mark the chunk for saving.
        if (previousEnergy != entity.energyStorage.amount || previousFuel != entity.fuelAmount
                || previousCoolant != entity.coolantAmount || previousVelocity != entity.beamVelocity
                || previousTemperature != entity.coilTemp || previousFlashTicks != entity.collisionFlashTicks) {
            entity.setChanged();
        }

        entity.syncTicker++;
        boolean stateChangedForRender = (entity.beamVelocity == 0 && entity.lastSyncedVel != 0)
                || (entity.collisionFlashTicks == 0 && entity.lastSyncedFlash != 0);
        if (stateChangedForRender
                || (entity.syncTicker % 5 == 0 && (entity.beamVelocity > 0 || entity.collisionFlashTicks > 0))) {
            entity.lastSyncedVel = entity.beamVelocity;
            entity.lastSyncedFlash = entity.collisionFlashTicks;
            entity.syncToClient();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        MachineInventory.saveInventory(tag, inventory, provider);
        tag.putLong("Energy", energyStorage.amount);
        tag.putBoolean("Formed", isFormed);
        tag.putInt("RingOffX", ringOffsetX);
        tag.putInt("RingOffZ", ringOffsetZ);
        tag.putInt("CoilCount", coilCount);
        tag.putInt("BeamVel", beamVelocity);
        tag.putInt("CoilTemp", coilTemp);
        tag.putInt("FuelAmt", fuelAmount);
        tag.putInt("FuelType", fuelType);
        tag.putInt("CoolAmt", coolantAmount);
        tag.putInt("CoolType", coolantType);
        tag.putInt("OpMode", operatingMode);
        tag.putBoolean("ShowHolo", showHologram);
        tag.putInt("FlashTicks", collisionFlashTicks);
        tag.putInt("TotalCollisions", totalCollisions);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        MachineInventory.loadInventory(tag, inventory, provider);
        energyStorage.amount = Math.min(MAX_ENERGY, tag.getLong("Energy"));
        isFormed = tag.getBoolean("Formed");
        if (tag.contains("RingOffX"))
            ringOffsetX = tag.getInt("RingOffX");
        if (tag.contains("RingOffZ"))
            ringOffsetZ = tag.getInt("RingOffZ");
        coilCount = tag.getInt("CoilCount");
        beamVelocity = tag.getInt("BeamVel");
        coilTemp = tag.contains("CoilTemp") ? tag.getInt("CoilTemp") : 40;
        fuelAmount = tag.getInt("FuelAmt");
        fuelType = tag.getInt("FuelType");
        coolantAmount = tag.getInt("CoolAmt");
        coolantType = tag.getInt("CoolType");
        operatingMode = tag.getInt("OpMode");
        showHologram = !tag.contains("ShowHolo") || tag.getBoolean("ShowHolo");
        collisionFlashTicks = tag.getInt("FlashTicks");
        totalCollisions = tag.getInt("TotalCollisions");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.olympian_hadron_collider");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new OlympianHadronColliderMenu(syncId, playerInventory, this.data, this.inventory, this);
    }

    @Override
    public Container getInventory() {
        return this.inventory;
    }

    @Override
    public boolean isInputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == SLOT_BEAM_A || slot == SLOT_BEAM_B || slot == SLOT_FUEL_IN || slot == SLOT_COOLANT_IN;
    }

    @Override
    public boolean isOutputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot == SLOT_OUTPUT_MAIN || slot == SLOT_OUTPUT_SECOND)
            return true;
        return slot == SLOT_CONTAINER_OUT && (stack.is(Items.BUCKET) || stack.is(Items.GLASS_BOTTLE));
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction dir) {
        if (index == SLOT_BEAM_A) {
            if (!inventory.canPlaceItem(SLOT_BEAM_A, stack))
                return false;
            ItemStack a = inventory.getItem(SLOT_BEAM_A);
            ItemStack b = inventory.getItem(SLOT_BEAM_B);
            // Help pipes split different materials cleanly across Beam A and Beam B
            if (!a.isEmpty() && !ItemStack.isSameItem(a, stack) && b.isEmpty())
                return false;
            return true;
        }
        return inventory.canPlaceItem(index, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction dir) {
        return isOutputSlot(index, stack, dir);
    }

    @Override
    public int getContainerSize() {
        return inventory.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return inventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventory.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return inventory.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return inventory.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory.setItem(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
    }
}
