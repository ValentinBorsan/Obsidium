package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyStorage;

public class VolcaniteGeothermalGeneratorBlockEntity extends BlockEntity implements MenuProvider, InventoryHolder, MachineInventory {
    public static final int INVENTORY_SIZE = 2;
    // Slot 0: Lava Bucket input
    // Slot 1: Bucket output
    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            VolcaniteGeothermalGeneratorBlockEntity.this.setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot == 0 && (stack.is(net.minecraft.world.item.Items.LAVA_BUCKET) || stack.getItem() instanceof net.valentin.obsidium.item.VolcaniteBottleItem);
        }
    };

    @Override
    public boolean isInputSlot(int slot, ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        return slot == 0;
    }

    @Override
    public boolean isOutputSlot(int slot, ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        return slot == 1;
    }

    public static final int MAX_LAVA = 16000;
    public static final int MAX_ENERGY = 500000;
    public static final int MAX_OUTPUT = 2000;
    public static final int ACTIVE_BOOST_RATE = 25; // Rebalanced/nerfed to 25 RF/t
    public static final int LAVA_PER_BOOST_TICK = 2; // mB/t

    private int lavaAmount = 0;
    private int passiveRate = 0;
    private int activeBoost = 1; // 1 = enabled, 0 = disabled (default on)
    private int scanTimer = 0;

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(MAX_ENERGY, 0, MAX_OUTPUT) {
        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> lavaAmount;
                case 1 -> passiveRate;
                case 2 -> activeBoost;
                case 3 -> (int) (energyStorage.amount & 0xFFFF);
                case 4 -> (int) ((energyStorage.amount >> 16) & 0xFFFF);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> lavaAmount = value;
                case 1 -> passiveRate = value;
                case 2 -> activeBoost = value;
                case 3 -> energyStorage.amount = (energyStorage.amount & 0xFFFF0000L) | (value & 0xFFFF);
                case 4 -> energyStorage.amount = (energyStorage.amount & 0xFFFFL) | ((long) value << 16);
            }
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public VolcaniteGeothermalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.VOLCANITE_GEOTHERMAL_GENERATOR_BE, pos, state);
    }

    @Override
    public Container getInventory() {
        return this.inventory;
    }

    public int getLavaAmount() {
        return lavaAmount;
    }

    public int getMaxLava() {
        return MAX_LAVA;
    }

    public int insertLava(int amount) {
        int toInsert = Math.min(amount, MAX_LAVA - lavaAmount);
        if (toInsert > 0) {
            lavaAmount += toInsert;
            setChanged();
        }
        return toInsert;
    }

    public int getPassiveRate() {
        return passiveRate;
    }

    public boolean isBoostEnabled() {
        return activeBoost == 1;
    }

    public void toggleBoost() {
        this.activeBoost = (this.activeBoost == 1) ? 0 : 1;
        setChanged();
    }

    private int calculatePassiveHeat(Level level, BlockPos pos) {
        int rate = 0;
        for (Direction dir : Direction.values()) {
            BlockPos checkPos = pos.relative(dir);
            BlockState checkState = level.getBlockState(checkPos);

            if (checkState.is(Blocks.LAVA)) {
                if (checkState.getFluidState().isSource()) {
                    rate += 4; // Rebalanced passive lava source heat
                } else {
                    rate += 1; // Rebalanced passive flowing lava heat
                }
            } else if (checkState.is(Blocks.MAGMA_BLOCK)) {
                rate += 2; // Rebalanced passive magma heat
            } else if (checkState.is(ObsidiumMod.VOLCANITE_BLOCK)) {
                rate += 6; // Rebalanced passive volcanite block heat
            } else if (checkState.is(ObsidiumMod.VOLCANITE_ORE)) {
                rate += 3; // Rebalanced passive volcanite ore heat
            }
        }
        return rate;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VolcaniteGeothermalGeneratorBlockEntity entity) {
        if (level.isClientSide()) return;

        boolean dirty = false;

        // 1. Lava Bucket / Bottle input
        ItemStack input = entity.inventory.getItem(0);
        ItemStack output = entity.inventory.getItem(1);
        if (input.is(Items.LAVA_BUCKET) && entity.lavaAmount + 1000 <= MAX_LAVA) {
            if (output.isEmpty() || (output.is(Items.BUCKET) && output.getCount() < output.getMaxStackSize())) {
                entity.lavaAmount += 1000;
                input.shrink(1);
                if (output.isEmpty()) {
                    entity.inventory.setItem(1, new ItemStack(Items.BUCKET));
                } else {
                    output.grow(1);
                }
                dirty = true;
            }
        } else if (input.getItem() instanceof net.valentin.obsidium.item.VolcaniteBottleItem) {
            int bottleLava = net.valentin.obsidium.item.VolcaniteBottleItem.getLava(input);
            if (bottleLava > 0 && entity.lavaAmount < MAX_LAVA) {
                int toTransfer = Math.min(bottleLava, Math.min(1000, MAX_LAVA - entity.lavaAmount));
                if (toTransfer > 0) {
                    entity.lavaAmount += toTransfer;
                    net.valentin.obsidium.item.VolcaniteBottleItem.setLava(input, bottleLava - toTransfer);
                    dirty = true;
                }
            }
        }

        // 2. Periodic heat scan (every 20 ticks)
        if (++entity.scanTimer >= 20) {
            entity.scanTimer = 0;
            int newRate = entity.calculatePassiveHeat(level, pos);
            if (newRate != entity.passiveRate) {
                entity.passiveRate = newRate;
                dirty = true;
            }
        }

        // 3. Energy generation (Passive + Active Boost)
        int currentGeneration = 0;
        boolean canStoreEnergy = entity.energyStorage.amount < entity.energyStorage.capacity;

        if (canStoreEnergy) {
            // Passive heat generation
            currentGeneration += entity.passiveRate;

            // Active lava boost
            if (entity.activeBoost == 1 && entity.lavaAmount >= LAVA_PER_BOOST_TICK) {
                entity.lavaAmount -= LAVA_PER_BOOST_TICK;
                currentGeneration += ACTIVE_BOOST_RATE;
            }

            if (currentGeneration > 0) {
                entity.energyStorage.amount = Math.min(entity.energyStorage.capacity, entity.energyStorage.amount + currentGeneration);
                dirty = true;
            }
        }

        // Sync WORKING state
        boolean isWorking = currentGeneration > 0;
        if (state.getValue(VolcaniteGeothermalGeneratorBlock.WORKING) != isWorking) {
            level.setBlock(pos, state.setValue(VolcaniteGeothermalGeneratorBlock.WORKING, isWorking), 3);
            dirty = true;
        }

        // 4. Output energy to neighbors
        if (entity.energyStorage.amount > 0) {
            for (Direction dir : Direction.values()) {
                EnergyStorage target = EnergyStorage.SIDED.find(level, pos.relative(dir), dir.getOpposite());
                if (target != null) {
                    long moved = EnergyStorageUtil.move(entity.energyStorage, target, MAX_OUTPUT, null);
                    if (moved > 0) {
                        dirty = true;
                    }
                }
            }
        }

        if (dirty) {
            entity.setChanged();
        }

        VolcaniteStoragePipeBlockEntity.autoEjectMachineOutputs(level, pos, entity);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("Lava", lavaAmount);
        tag.putInt("PassiveRate", passiveRate);
        tag.putInt("ActiveBoost", activeBoost);
        tag.putLong("Energy", energyStorage.amount);
        MachineInventory.saveInventory(tag, inventory, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        lavaAmount = tag.getInt("Lava");
        passiveRate = tag.getInt("PassiveRate");
        activeBoost = tag.getInt("ActiveBoost");
        energyStorage.amount = tag.getLong("Energy");
        MachineInventory.loadInventory(tag, inventory, provider);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.volcanite_geothermal_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new VolcaniteGeothermalGeneratorMenu(syncId, playerInventory, inventory, data);
    }
}
