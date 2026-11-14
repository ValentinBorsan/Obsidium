package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.base.SimpleEnergyStorage;

public class AdvancedDionysianPressBlockEntity extends BlockEntity implements MenuProvider, InventoryHolder, WorldlyContainer, MachineInventory {

    public static final int LANES = 3;
    public static final int INVENTORY_SIZE = 7;
    // Slots 0, 1, 2: Fruit Inputs (3 parallel lanes)
    // Slots 3, 4, 5: Mineral Outputs (Nuggets per lane)
    // Slot 6: Shared Organic Pomace Output

    public static final int MAX_PROGRESS = 25; // 1.25s per fruit (3.2x faster than basic press)
    public static final int RF_PER_TICK_PER_LANE = 600; // 600 RF/t per active lane (up to 1,800 RF/t)
    public static final int DATA_COUNT = 9;

    private final NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final int[] progress = new int[LANES];
    private int activeLanes = 0;

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(250_000L, 25_000L, 0L) {
        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress[0];
                case 1 -> progress[1];
                case 2 -> progress[2];
                case 3 -> MAX_PROGRESS;
                case 4 -> activeLanes;
                case 5 -> (int) (energyStorage.amount >> 16);
                case 6 -> (int) (energyStorage.amount & 0xFFFF);
                case 7 -> (int) (energyStorage.capacity >> 16);
                case 8 -> (int) (energyStorage.capacity & 0xFFFF);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index >= 0 && index < LANES) progress[index] = value;
            else if (index == 4) activeLanes = value;
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public AdvancedDionysianPressBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.ADVANCED_DIONYSIAN_PRESS_BE, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AdvancedDionysianPressBlockEntity be) {
        be.tickServer(level, pos, state);
    }

    private boolean canPressLane(int lane) {
        ItemStack fruit = items.get(lane);
        if (!DionysianPressBlockEntity.isFruit(fruit)) return false;

        ItemStack mineralOut = items.get(3 + lane);
        ItemStack expectedMineral = DionysianPressBlockEntity.getMineralOutput(fruit);
        boolean mineralFits = mineralOut.isEmpty()
                || (ItemStack.isSameItemSameComponents(mineralOut, expectedMineral)
                    && mineralOut.getCount() + expectedMineral.getCount() <= mineralOut.getMaxStackSize());
        if (!mineralFits) return false;

        ItemStack pomaceOut = items.get(6);
        int pomaceYield = DionysianPressBlockEntity.getPomaceYield(fruit);
        return pomaceOut.isEmpty()
                || (pomaceOut.is(ObsidiumMod.DIONYSIAN_POMACE)
                    && pomaceOut.getCount() + pomaceYield <= pomaceOut.getMaxStackSize());
    }

    public void tickServer(Level level, BlockPos pos, BlockState state) {
        boolean changed = false;
        int runningCount = 0;
        boolean playedSoundThisTick = false;

        for (int lane = 0; lane < LANES; lane++) {
            if (canPressLane(lane) && energyStorage.amount >= RF_PER_TICK_PER_LANE) {
                energyStorage.amount -= RF_PER_TICK_PER_LANE;
                progress[lane]++;
                runningCount++;
                changed = true;

                if (!playedSoundThisTick && progress[lane] % 12 == 0) {
                    level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.3F, 1.4F);
                    playedSoundThisTick = true;
                }

                if (progress[lane] >= MAX_PROGRESS) {
                    progress[lane] = 0;
                    ItemStack fruit = items.get(lane);
                    ItemStack expectedMineral = DionysianPressBlockEntity.getMineralOutput(fruit);
                    int nuggetCount = DionysianPressBlockEntity.getRandomNuggetCount(fruit, level);
                    int pomaceYield = DionysianPressBlockEntity.getPomaceYield(fruit);
                    fruit.shrink(1);

                    ItemStack mineralSlot = items.get(3 + lane);
                    if (mineralSlot.isEmpty()) {
                        items.set(3 + lane, expectedMineral.copyWithCount(nuggetCount));
                    } else {
                        mineralSlot.grow(nuggetCount);
                    }

                    ItemStack pomaceSlot = items.get(6);
                    if (pomaceSlot.isEmpty()) {
                        items.set(6, new ItemStack(ObsidiumMod.DIONYSIAN_POMACE, pomaceYield));
                    } else {
                        pomaceSlot.grow(pomaceYield);
                    }

                    level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.4F, 1.55F);
                }
            } else if (progress[lane] > 0) {
                progress[lane] = Math.max(0, progress[lane] - 2);
                changed = true;
            }
        }

        if (activeLanes != runningCount) {
            activeLanes = runningCount;
            changed = true;
        }

        boolean isLit = runningCount > 0;
        if (state.getValue(AdvancedDionysianPressBlock.LIT) != isLit) {
            level.setBlock(pos, state.setValue(AdvancedDionysianPressBlock.LIT, isLit), 3);
        }

        if (changed) {
            setChanged();
        }

        VolcaniteStoragePipeBlockEntity.autoEjectMachineOutputs(level, pos, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putIntArray("ProgressLanes", progress);
        tag.putLong("Energy", energyStorage.amount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        int[] loaded = tag.getIntArray("ProgressLanes");
        for (int i = 0; i < Math.min(LANES, loaded.length); i++) {
            progress[i] = loaded[i];
        }
        energyStorage.amount = tag.getLong("Energy");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.advanced_dionysian_press");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new AdvancedDionysianPressMenu(syncId, playerInventory, this, this.dataAccess);
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
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
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
        return slot >= 0 && slot < LANES && DionysianPressBlockEntity.isFruit(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return new int[]{ 3, 4, 5, 6 };
        }
        return new int[]{ 0, 1, 2 };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 3 && slot < INVENTORY_SIZE;
    }

    @Override
    public boolean isInputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot >= 0 && slot < LANES;
    }

    @Override
    public boolean isOutputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot >= 3 && slot < INVENTORY_SIZE;
    }
}
