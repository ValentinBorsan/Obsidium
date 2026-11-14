package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.VolcaniteBottleItem;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class CreativeWaterTankBlockEntity extends LiquidTankBlockEntity {
    public static final int INFINITE_CAPACITY = 1_000_000_000;

    protected final ContainerData creativeData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index == 0) return (INFINITE_CAPACITY >> 16);
            if (index == 1) return (INFINITE_CAPACITY & 0xFFFF);
            if (index == 2) return (INFINITE_CAPACITY >> 16);
            if (index == 3) return (INFINITE_CAPACITY & 0xFFFF);
            return 0;
        }
        @Override
        public void set(int index, int value) {}
        @Override
        public int getCount() { return 4; }
    };

    public CreativeWaterTankBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.CREATIVE_WATER_TANK_BE, pos, state);
        this.maxCapacity = INFINITE_CAPACITY;
        this.fluidAmount = INFINITE_CAPACITY;
        this.storedFluid = FluidVariant.of(Fluids.WATER);
    }

    @Override
    public BlockEntityType<?> getType() {
        return ObsidiumMod.CREATIVE_WATER_TANK_BE;
    }

    @Override
    public int getFluidAmount() {
        return INFINITE_CAPACITY;
    }

    public int getWaterAmount() {
        return INFINITE_CAPACITY;
    }

    @Override
    public int getMaxCapacity() {
        return INFINITE_CAPACITY;
    }

    @Override
    public int extractFluid(FluidVariant variant, int amount_mB, boolean simulate) {
        if (variant.isOf(Fluids.WATER)) return amount_mB;
        return 0;
    }

    @Override
    public int insertFluid(FluidVariant variant, int amount_mB, boolean simulate) {
        return amount_mB;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.fluidAmount = INFINITE_CAPACITY;
        this.maxCapacity = INFINITE_CAPACITY;
        this.storedFluid = FluidVariant.of(Fluids.WATER);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.creative_water_tank");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new CreativeWaterTankMenu(syncId, playerInventory, (SimpleContainer) this.getInventory(), this.creativeData);
    }

    public static void creativeTick(Level level, BlockPos pos, BlockState state, CreativeWaterTankBlockEntity entity) {
        if (level.isClientSide()) return;

        entity.fluidAmount = INFINITE_CAPACITY;
        entity.maxCapacity = INFINITE_CAPACITY;
        entity.storedFluid = FluidVariant.of(Fluids.WATER);

        // 1. Process Instant-Fill Container in Slot 0
        Container inv = entity.getInventory();
        ItemStack input = inv.getItem(0);
        if (!input.isEmpty()) {
            if (input.is(Items.BUCKET)) {
                ItemStack output = inv.getItem(1);
                if (output.isEmpty()) {
                    input.shrink(1);
                    inv.setItem(1, new ItemStack(Items.WATER_BUCKET));
                    entity.setChanged();
                } else if (output.is(Items.WATER_BUCKET) && output.getCount() < output.getMaxStackSize()) {
                    input.shrink(1);
                    output.grow(1);
                    entity.setChanged();
                }
            } else if (input.getItem() instanceof VolcaniteBottleItem bottle) {
                int current = VolcaniteBottleItem.getFluidAmount(input);
                if (current < bottle.getCapacity()) {
                    VolcaniteBottleItem.setFluid(input, FluidVariant.of(Fluids.WATER), bottle.getCapacity());
                    entity.setChanged();
                }
            }
        }

        // 2. Direct Pushing to all 6 adjacent sides
        long pushAmountDroplets = FluidConstants.BUCKET * 50;
        for (Direction dir : Direction.values()) {
            BlockPos targetPos = pos.relative(dir);
            Storage<FluidVariant> target = FluidStorage.SIDED.find(level, targetPos, dir.getOpposite());
            if (target != null && target.supportsInsertion()) {
                try (Transaction t = Transaction.openOuter()) {
                    long inserted = target.insert(FluidVariant.of(Fluids.WATER), pushAmountDroplets, t);
                    if (inserted > 0) {
                        t.commit();
                    }
                }
            }
        }

        // 3. Network Pushing through adjacent VolcanitePipeBlocks
        for (Direction initialDir : Direction.values()) {
            BlockPos neighborPos = pos.relative(initialDir);
            if (level.getBlockState(neighborPos).getBlock() instanceof VolcaniteStoragePipeBlock) {
                Queue<BlockPos> queue = new LinkedList<>();
                Set<BlockPos> visited = new HashSet<>();
                queue.add(neighborPos);
                visited.add(neighborPos);

                int branchesLeft = 16;
                while (!queue.isEmpty() && branchesLeft > 0) {
                    BlockPos curr = queue.poll();
                    branchesLeft--;

                    for (Direction dir : Direction.values()) {
                        BlockPos targetPos = curr.relative(dir);
                        if (visited.contains(targetPos) || targetPos.equals(pos)) continue;

                        if (level.getBlockState(targetPos).getBlock() instanceof VolcaniteStoragePipeBlock) {
                            visited.add(targetPos);
                            queue.add(targetPos);
                        } else {
                            Storage<FluidVariant> target = FluidStorage.SIDED.find(level, targetPos, dir.getOpposite());
                            if (target != null && target.supportsInsertion()) {
                                try (Transaction t = Transaction.openOuter()) {
                                    long inserted = target.insert(FluidVariant.of(Fluids.WATER), pushAmountDroplets, t);
                                    if (inserted > 0) {
                                        t.commit();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
