package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import team.reborn.energy.api.base.SimpleEnergyStorage;
import java.util.LinkedList;
import java.util.Queue;
import java.util.HashSet;
import java.util.Set;

public class VolcaniteLavaPumpBlockEntity extends BlockEntity implements MenuProvider {
    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(100000, 10000, 0) {
        @Override
        protected void onFinalCommit() { setChanged(); }
    };
    
    private int lavaAmount = 0;
    private final int MAX_LAVA = 16000;
    private int tickCounter = 0;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index == 0) return lavaAmount;
            if (index == 1) return (int) (energyStorage.amount & 0xFFFF);
            if (index == 2) return (int) ((energyStorage.amount >> 16) & 0xFFFF);
            return 0;
        }
        @Override
        public void set(int index, int value) {
            if (index == 0) lavaAmount = value;
            if (index == 1) energyStorage.amount = (energyStorage.amount & 0xFFFF0000L) | (value & 0xFFFF);
            if (index == 2) energyStorage.amount = (energyStorage.amount & 0xFFFFL) | ((long) value << 16);
        }
        @Override
        public int getCount() { return 3; }
    };

    public VolcaniteLavaPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.VOLCANITE_LAVA_PUMP_BE, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VolcaniteLavaPumpBlockEntity entity) {
        if (level.isClientSide()) return;
        boolean dirty = false;
        
        entity.tickCounter++;

        // Pump logic (once per second)
        if (entity.tickCounter % 20 == 0 && entity.energyStorage.amount >= 5000 && entity.lavaAmount <= entity.MAX_LAVA - 1000) {
            BlockPos target = entity.findLavaBelow(level, pos);
            if (target != null) {
                level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
                entity.energyStorage.amount -= 5000;
                entity.lavaAmount += 1000;
                dirty = true;
            }
        }

        // Push Lava logic
        if (entity.lavaAmount > 0) {
            Queue<BlockPos> queue = new LinkedList<>();
            Set<BlockPos> visited = new HashSet<>();
            for (Direction dir : Direction.values()) queue.add(pos.relative(dir));
            
            while (!queue.isEmpty() && entity.lavaAmount > 0) {
                BlockPos curr = queue.poll();
                if (!visited.add(curr)) continue;
                if (visited.size() > 64) break; // Limit search
                
                BlockEntity be = level.getBlockEntity(curr);
                if (be instanceof LiquidTankBlockEntity tank) {
                    int canAccept = tank.getMaxCapacity() - tank.getFluidAmount();
                    if (canAccept > 0 && tank.canAccept(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.LAVA))) {
                        int toTransfer = Math.min(entity.lavaAmount, Math.min(canAccept, 1000));
                        int inserted = tank.insertFluid(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.LAVA), toTransfer, false);
                        if (inserted > 0) {
                            entity.lavaAmount -= inserted;
                            dirty = true;
                        }
                    }
                } else if (level.getBlockState(curr).getBlock() instanceof VolcaniteStoragePipeBlock) {
                    for (Direction dir : Direction.values()) queue.add(curr.relative(dir));
                } else {
                    for (Direction dir : Direction.values()) {
                        var target = net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.find(level, curr, dir);
                        if (target != null) {
                            try (net.fabricmc.fabric.api.transfer.v1.transaction.Transaction t = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                                long toMoveDroplets = Math.min(entity.lavaAmount * 81L, 1000 * 81L);
                                long inserted = target.insert(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.LAVA), toMoveDroplets, t);
                                if (inserted > 0) {
                                    int insertedMb = (int)(inserted / 81L);
                                    entity.lavaAmount -= insertedMb;
                                    t.commit();
                                    dirty = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (dirty) entity.setChanged();
    }

    public int getLavaAmount() { return lavaAmount; }
    public int getMaxCapacity() { return MAX_LAVA; }
    public boolean insertLava(int amount) {
        if (this.lavaAmount + amount <= MAX_LAVA) {
            this.lavaAmount += amount;
            setChanged();
            return true;
        }
        return false;
    }
    public boolean extractLava(int amount) {
        if (this.lavaAmount >= amount) {
            this.lavaAmount -= amount;
            setChanged();
            return true;
        }
        return false;
    }
    
    private BlockPos findLavaBelow(Level level, BlockPos startPos) {
        // Search downwards up to 64 blocks
        for (int i = 1; i <= 64; i++) {
            BlockPos check = startPos.below(i);
            BlockState st = level.getBlockState(check);
            if (st.is(Blocks.LAVA) && st.getFluidState().isSource()) {
                return check;
            } else if (!st.isAir() && !st.is(Blocks.LAVA)) {
                // If we hit solid ground and it's not lava, stop going straight down, 
                // but a real pump might search in a radius. For simplicity, we search in a small radius at current depth.
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        BlockPos r = check.offset(x, 0, z);
                        if (level.getBlockState(r).is(Blocks.LAVA) && level.getBlockState(r).getFluidState().isSource()) {
                            return r;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("LavaAmount", lavaAmount);
        tag.putLong("Energy", energyStorage.amount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.lavaAmount = tag.getInt("LavaAmount");
        this.energyStorage.amount = tag.getLong("Energy");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new VolcaniteLavaPumpMenu(syncId, playerInventory, this.data);
    }
}
