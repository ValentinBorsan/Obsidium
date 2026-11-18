package net.valentin.mythicrails;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/** The cart owns the dock session; station bridge and cargo/fluid/energy moves share one session. */
final class FreightDocking {
    private static final int INTERVAL = 4, QUIET_TICKS = 40, BATCH = 8;
    private BlockPos stationPos;
    private boolean released;
    private int quietTicks, cooldown;
    private int status;

    int status() { return status; }

    boolean holds(HermesFreightCart cart) {
        var pos = BlockPos.containing(cart.getX(), cart.getY() + 0.1, cart.getZ()).below();
        var block = cart.level().getBlockState(pos).getBlock();
        return (block instanceof FreightStationBlock || block instanceof RailUtilityStationBlock)
                && !(pos.equals(stationPos) && released);
    }

    boolean release(HermesFreightCart cart, BlockPos pos) {
        var state = cart.level().getBlockState(pos);
        Direction direction;
        if (state.getBlock() instanceof FreightStationBlock) {
            direction = state.getValue(FreightStationBlock.FACING);
        } else if (state.getBlock() instanceof RailUtilityStationBlock) {
            direction = state.getValue(RailUtilityStationBlock.FACING);
        } else {
            return false;
        }
        if (!pos.equals(stationPos) || released) return false;
        var railState = cart.level().getBlockState(pos.above());
        if (!(railState.getBlock() instanceof BaseRailBlock rail)) return false;
        var shape = railState.getValue(rail.getShapeProperty());
        if (!(shape == RailShape.EAST_WEST && direction.getAxis() == Direction.Axis.X
                || shape == RailShape.NORTH_SOUTH && direction.getAxis() == Direction.Axis.Z)) {
            return false;
        }
        released = true;
        status = 7;
        cart.setDeltaMovement(direction.getStepX() * 0.3, 0, direction.getStepZ() * 0.3);
        return true;
    }

    boolean tick(HermesFreightCart cart) {
        var level = cart.level();
        var railPos = BlockPos.containing(cart.getX(), cart.getY() + 0.1, cart.getZ());
        var pos = railPos.below();
        var railState = level.getBlockState(railPos);
        var state = level.getBlockState(pos);
        boolean isItemStation = state.getBlock() instanceof FreightStationBlock;
        boolean isUtilStation = state.getBlock() instanceof RailUtilityStationBlock;
        if ((!isItemStation && !isUtilStation) || !(railState.getBlock() instanceof BaseRailBlock rail)) {
            clear();
            return false;
        }

        Direction direction = isItemStation
                ? state.getValue(FreightStationBlock.FACING)
                : state.getValue(RailUtilityStationBlock.FACING);
        boolean enabled = isItemStation
                ? state.getValue(FreightStationBlock.ENABLED)
                : state.getValue(RailUtilityStationBlock.ENABLED);

        var shape = railState.getValue(rail.getShapeProperty());
        boolean aligned = shape == RailShape.EAST_WEST && direction.getAxis() == Direction.Axis.X
                || shape == RailShape.NORTH_SOUTH && direction.getAxis() == Direction.Axis.Z;
        if (!pos.equals(stationPos)) {
            clear();
            stationPos = pos.immutable();
        }
        if (released) {
            status = 7;
            return false;
        }

        var anchor = aligned ? new Vec3(railPos.getX() + 0.5, railPos.getY() + 0.0625, railPos.getZ() + 0.5) : cart.position();
        cart.setPos(anchor.x, anchor.y, anchor.z);
        cart.setDeltaMovement(Vec3.ZERO);
        cart.tickVanillaStopped();
        if (cart.isRemoved()) return true;
        cart.setPos(anchor.x, anchor.y, anchor.z);
        cart.setDeltaMovement(Vec3.ZERO);
        if (!aligned) { status = 8; quietTicks = 0; return true; }
        if (!enabled) { status = 3; quietTicks = 0; return true; }
        if (cooldown-- > 0) return true;
        cooldown = INTERVAL - 1;

        long moved = 0;
        boolean complete = false;

        if (isItemStation) {
            var station = (FreightStationBlock) state.getBlock();
            if (!(level.getBlockEntity(pos) instanceof FreightStationBlockEntity buffer)) {
                quietTicks = 0; status = 4; return true;
            }
            var cargo = InventoryStorage.of(cart, Direction.DOWN);
            try (var transaction = Transaction.openOuter()) {
                if (station.isLoader()) buffer.bridgeAll(transaction);
                moved = StorageUtil.move(station.isLoader() ? buffer.storage() : cargo,
                        station.isLoader() ? cargo : buffer.storage(), variant -> true, BATCH, transaction);
                if (!station.isLoader()) buffer.bridgeAll(transaction);
                transaction.commit();
            }
            complete = station.isLoader()
                    ? isFull(cart) || (buffer.isEmpty() && buffer.allExternalEmpty())
                    : cartEmptyCargo(cart);
        } else {
            var uStation = (RailUtilityStationBlock) state.getBlock();
            if (!(level.getBlockEntity(pos) instanceof RailUtilityStationBlockEntity be)) {
                quietTicks = 0; status = 4; return true;
            }
            var kind = uStation.kind();
            if (kind.isFluid) {
                if (cart.payloadType() != CartModuleItem.PayloadType.LIQUID_BARREL) {
                    complete = true;
                } else if (kind.isLoader) {
                    int fType = be.fluidTypeCode();
                    int space = HermesFreightCart.MAX_FLUID - cart.fluidAmount();
                    int canMove = Math.min(1000, Math.min(space, be.fluidMb()));
                    if (fType > 0 && canMove > 0 && (cart.fluidAmount() == 0 || cart.fluidType() == fType)) {
                        try (var tx = Transaction.openOuter()) {
                            long extracted = be.fluidStorage().extract(be.fluidStorage().variant, canMove * 81L, tx);
                            int mb = (int) (extracted / 81L);
                            if (mb > 0 && cart.addFluid(fType, mb) == mb) {
                                tx.commit();
                                moved = mb;
                            }
                        }
                    }
                    complete = cart.fluidAmount() >= HermesFreightCart.MAX_FLUID || be.fluidMb() == 0;
                } else {
                    int cType = cart.fluidType();
                    int canMove = Math.min(1000, Math.min(cart.fluidAmount(), RailUtilityStationBlockEntity.MAX_FLUID_MB - be.fluidMb()));
                    if (cType > 0 && canMove > 0) {
                        var variant = FluidVariant.of(cType == 2 ? Fluids.LAVA : Fluids.WATER);
                        try (var tx = Transaction.openOuter()) {
                            long inserted = be.fluidStorage().insert(variant, canMove * 81L, tx);
                            int mb = (int) (inserted / 81L);
                            if (mb > 0 && cart.extractFluid(mb) == mb) {
                                tx.commit();
                                moved = mb;
                            }
                        }
                    }
                    complete = cart.fluidAmount() == 0 || be.fluidMb() >= RailUtilityStationBlockEntity.MAX_FLUID_MB;
                }
            } else {
                // Energy Loader / Unloader
                if (kind.isLoader) {
                    int space = cart.maxEnergy() - cart.energy();
                    int canMove = Math.min(50_000, Math.min(space, be.energyAmount()));
                    if (canMove > 0) {
                        try (var tx = Transaction.openOuter()) {
                            long extracted = be.energyStorage().extract(canMove, tx);
                            if (extracted > 0) {
                                int added = cart.addEnergy((int) extracted);
                                if (added > 0) {
                                    tx.commit();
                                    moved = added;
                                }
                            }
                        }
                    }
                    complete = cart.energy() >= cart.maxEnergy() || be.energyAmount() == 0;
                } else {
                    int space = (int) Math.min(50_000L, RailUtilityStationBlockEntity.MAX_ENERGY - be.energyAmount());
                    int canMove = Math.min(space, cart.energy());
                    if (canMove > 0) {
                        try (var tx = Transaction.openOuter()) {
                            long inserted = be.energyStorage().insert(canMove, tx);
                            if (inserted > 0) {
                                int taken = cart.extractEnergy((int) inserted);
                                if (taken > 0) {
                                    tx.commit();
                                    moved = taken;
                                }
                            }
                        }
                    }
                    complete = cart.energy() == 0 || be.energyAmount() >= RailUtilityStationBlockEntity.MAX_ENERGY;
                }
            }
        }

        status = moved > 0 ? 1 : complete ? (FreightStationBlock.hasDepartureSignal(level, pos) ? 2 : 6) : 5;
        quietTicks = moved == 0 && complete ? Math.min(QUIET_TICKS, quietTicks + INTERVAL) : 0;
        if (quietTicks >= QUIET_TICKS && !FreightStationBlock.hasDepartureSignal(level, pos)) {
            released = true;
            status = 7;
            cart.setDeltaMovement(direction.getStepX() * 0.3, 0, direction.getStepZ() * 0.3);
        }
        return true;
    }

    private static boolean isEmpty(Storage<ItemVariant> storage) {
        for (var view : storage.nonEmptyViews()) if (view.getAmount() > 0) return false;
        return true;
    }

    private static boolean cartEmptyCargo(HermesFreightCart cart) {
        for (int i = 0; i < HermesFreightCart.CARGO_SIZE; i++) {
            if (!cart.getItem(i).isEmpty()) return false;
        }
        return true;
    }

    private static boolean isFull(HermesFreightCart cart) {
        for (int i = 0; i < HermesFreightCart.CARGO_SIZE; i++) {
            ItemStack stack = cart.getItem(i);
            if (stack.isEmpty() || stack.getCount() < Math.min(stack.getMaxStackSize(), cart.getMaxStackSize())) return false;
        }
        return true;
    }

    private void clear() { status = 0; stationPos = null; released = false; quietTicks = 0; cooldown = 0; }

    void save(CompoundTag entity) {
        if (stationPos == null) return;
        var tag = new CompoundTag();
        tag.putLong("Pos", stationPos.asLong());
        tag.putBoolean("Released", released);
        tag.putInt("QuietTicks", quietTicks);
        tag.putInt("Cooldown", cooldown);
        entity.put("MythicRailsDock", tag);
    }

    void load(CompoundTag entity) {
        clear();
        if (!entity.contains("MythicRailsDock", 10)) return;
        var tag = entity.getCompound("MythicRailsDock");
        if (!tag.contains("Pos", 4)) return;
        stationPos = BlockPos.of(tag.getLong("Pos"));
        released = tag.getBoolean("Released");
        quietTicks = Math.clamp(tag.getInt("QuietTicks"), 0, QUIET_TICKS);
        cooldown = Math.clamp(tag.getInt("Cooldown"), 0, INTERVAL);
    }
}
