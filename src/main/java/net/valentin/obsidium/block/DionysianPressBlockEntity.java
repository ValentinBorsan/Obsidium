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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.base.SimpleEnergyStorage;

public class DionysianPressBlockEntity extends BlockEntity implements MenuProvider, InventoryHolder, WorldlyContainer, MachineInventory {

    public static final int INVENTORY_SIZE = 4;
    // Slot 0: Fruit Input
    // Slot 1: Mineral Output (Nuggets)
    // Slot 2: Pomace Output (Dionysian Pomace)
    // Slot 3: Optional Fuel Input (Used if no RF energy is connected)

    public static final int MAX_PROGRESS = 80; // 4 seconds per fruit
    public static final int RF_PER_TICK = 200; // 200 RF/t

    private final NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private int progress = 0;
    private int burnTime = 0;
    private int totalBurnTime = 0;

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(50_000, 5_000, 0) {
        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> MAX_PROGRESS;
                case 2 -> burnTime;
                case 3 -> totalBurnTime;
                case 4 -> (int) (energyStorage.amount >> 16);
                case 5 -> (int) (energyStorage.amount & 0xFFFF);
                case 6 -> (int) (energyStorage.capacity >> 16);
                case 7 -> (int) (energyStorage.capacity & 0xFFFF);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 2 -> burnTime = value;
                case 3 -> totalBurnTime = value;
            }
        }

        @Override
        public int getCount() {
            return 8;
        }
    };

    public DionysianPressBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.DIONYSIAN_PRESS_BE, pos, state);
    }

    public static boolean isFruit(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(ObsidiumMod.HERMES_COPPER_FRUIT)
                || stack.is(ObsidiumMod.ARES_IRON_FRUIT)
                || stack.is(ObsidiumMod.MIDAS_GOLD_FRUIT)
                || stack.is(ObsidiumMod.CRONUS_LEAD_FRUIT)
                || stack.is(ObsidiumMod.ZEUS_TIN_FRUIT)
                || stack.is(ObsidiumMod.ARTEMIS_SILVER_FRUIT)
                || stack.is(ObsidiumMod.ELECTRUM_FRUIT)
                || stack.is(ObsidiumMod.HEPHAESTUS_VOLCANITE_FRUIT)
                || stack.is(ObsidiumMod.TARTAREAN_NETHERITE_FRUIT)
                || stack.is(Items.APPLE)
                || stack.is(Items.GOLDEN_APPLE)
                || stack.is(Items.ENCHANTED_GOLDEN_APPLE)
                || stack.is(Items.SWEET_BERRIES)
                || stack.is(Items.GLOW_BERRIES)
                || stack.is(Items.MELON_SLICE)
                || stack.is(Items.CHORUS_FRUIT);
    }

    public static ItemStack getMineralOutput(ItemStack fruit) {
        if (fruit.isEmpty()) return ItemStack.EMPTY;
        if (fruit.is(ObsidiumMod.MIDAS_GOLD_FRUIT)) {
            return new ItemStack(Items.GOLD_NUGGET, 3);
        } else if (fruit.is(ObsidiumMod.ARES_IRON_FRUIT)) {
            return new ItemStack(Items.IRON_NUGGET, 3);
        } else if (fruit.is(ObsidiumMod.HERMES_COPPER_FRUIT)) {
            return new ItemStack(ObsidiumMod.COPPER_NUGGET, 3);
        } else if (fruit.is(ObsidiumMod.CRONUS_LEAD_FRUIT)) {
            return new ItemStack(ObsidiumMod.LEAD_NUGGET, 3);
        } else if (fruit.is(ObsidiumMod.ZEUS_TIN_FRUIT)) {
            return new ItemStack(ObsidiumMod.TIN_NUGGET, 3);
        } else if (fruit.is(ObsidiumMod.ARTEMIS_SILVER_FRUIT)) {
            return new ItemStack(ObsidiumMod.SILVER_NUGGET, 3);
        } else if (fruit.is(ObsidiumMod.HEPHAESTUS_VOLCANITE_FRUIT)) {
            return new ItemStack(ObsidiumMod.VOLCANITE_NUGGET, 3);
        } else if (fruit.is(ObsidiumMod.ELECTRUM_FRUIT)) {
            return new ItemStack(Items.GOLD_NUGGET, 3);
        } else if (fruit.is(ObsidiumMod.TARTAREAN_NETHERITE_FRUIT)) {
            return new ItemStack(ObsidiumMod.NETHERITE_NUGGET, 2);
        } else {
            return new ItemStack(ObsidiumMod.VOLCANIC_ASH, 2);
        }
    }

    public static int getRandomNuggetCount(ItemStack fruit, Level level) {
        if (fruit.is(ObsidiumMod.TARTAREAN_NETHERITE_FRUIT)) {
            return 1 + level.random.nextInt(2); // 1-2 Netherite nuggets
        }
        if (fruit.is(ObsidiumMod.MIDAS_GOLD_FRUIT)
                || fruit.is(ObsidiumMod.ARES_IRON_FRUIT)
                || fruit.is(ObsidiumMod.HERMES_COPPER_FRUIT)
                || fruit.is(ObsidiumMod.CRONUS_LEAD_FRUIT)
                || fruit.is(ObsidiumMod.ZEUS_TIN_FRUIT)
                || fruit.is(ObsidiumMod.ARTEMIS_SILVER_FRUIT)
                || fruit.is(ObsidiumMod.HEPHAESTUS_VOLCANITE_FRUIT)
                || fruit.is(ObsidiumMod.ELECTRUM_FRUIT)) {
            return 1 + level.random.nextInt(3); // 1-3 Mineral nuggets
        }
        return 1 + level.random.nextInt(2); // 1-2 Ash for mundane fruits
    }

    public static int getPomaceYield(ItemStack fruit) {
        if (fruit.is(ObsidiumMod.HEPHAESTUS_VOLCANITE_FRUIT) || fruit.is(ObsidiumMod.TARTAREAN_NETHERITE_FRUIT)) {
            return 2;
        }
        return 1;
    }

    public static boolean isFuel(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(ObsidiumMod.DIONYSIAN_POMACE)) return true;
        if (stack.is(ObsidiumMod.VOLCANIC_ASH)) return true;
        return AbstractFurnaceBlockEntity.isFuel(stack);
    }

    public static int getBurnDuration(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.is(ObsidiumMod.DIONYSIAN_POMACE)) return 1600;
        if (stack.is(ObsidiumMod.VOLCANIC_ASH)) return 400;
        if (stack.is(Items.LAVA_BUCKET)) return 20000;
        return AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DionysianPressBlockEntity be) {
        be.tickServer(level, pos, state);
    }

    public void tickServer(Level level, BlockPos pos, BlockState state) {
        boolean changed = false;

        if (burnTime > 0) {
            burnTime--;
            changed = true;
        }

        ItemStack fruit = items.get(0);
        ItemStack outputNugget = items.get(1);
        ItemStack outputPomace = items.get(2);
        ItemStack fuel = items.get(3);

        ItemStack expectedMineral = getMineralOutput(fruit);
        int pomaceYield = getPomaceYield(fruit);
        ItemStack expectedPomace = new ItemStack(ObsidiumMod.DIONYSIAN_POMACE, pomaceYield);

        boolean canPress = isFruit(fruit)
                && (outputNugget.isEmpty() || (ItemStack.isSameItemSameComponents(outputNugget, expectedMineral) && outputNugget.getCount() + expectedMineral.getCount() <= outputNugget.getMaxStackSize()))
                && (outputPomace.isEmpty() || (ItemStack.isSameItemSameComponents(outputPomace, expectedPomace) && outputPomace.getCount() + expectedPomace.getCount() <= outputPomace.getMaxStackSize()));

        boolean hasEnergy = energyStorage.amount >= RF_PER_TICK;

        // Thermal fallback refuel if no RF energy
        if (!hasEnergy && burnTime <= 0 && canPress) {
            if (isFuel(fuel)) {
                int duration = getBurnDuration(fuel);
                if (duration > 0) {
                    burnTime = totalBurnTime = duration;
                    if (fuel.is(Items.LAVA_BUCKET)) {
                        items.set(3, new ItemStack(Items.BUCKET));
                    } else {
                        fuel.shrink(1);
                    }
                    changed = true;
                }
            }
        }

        boolean isPowered = hasEnergy || burnTime > 0;

        if (isPowered && canPress) {
            if (hasEnergy) {
                energyStorage.amount -= RF_PER_TICK;
            }

            progress++;
            changed = true;

            // Audio cues while operating
            if (progress % 20 == 0) {
                level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.4F, 1.2F);
            }

            if (progress >= MAX_PROGRESS) {
                progress = 0;
                fruit.shrink(1);

                // Add mineral nuggets (1-3 randomized)
                int nuggetCount = getRandomNuggetCount(fruit, level);
                if (outputNugget.isEmpty()) {
                    items.set(1, expectedMineral.copyWithCount(nuggetCount));
                } else {
                    outputNugget.grow(nuggetCount);
                }

                // Add pomace
                if (outputPomace.isEmpty()) {
                    items.set(2, expectedPomace.copy());
                } else {
                    outputPomace.grow(expectedPomace.getCount());
                }

                level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5F, 1.4F);
                level.playSound(null, pos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 0.7F, 0.8F);
            }
        } else {
            if (progress > 0) {
                progress = Math.max(0, progress - 2);
                changed = true;
            }
        }

        boolean isLit = (burnTime > 0) || (hasEnergy && canPress);
        if (state.getValue(DionysianPressBlock.LIT) != isLit) {
            level.setBlock(pos, state.setValue(DionysianPressBlock.LIT, isLit), 3);
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
        tag.putInt("Progress", progress);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("TotalBurnTime", totalBurnTime);
        tag.putLong("Energy", energyStorage.amount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        progress = tag.getInt("Progress");
        burnTime = tag.getInt("BurnTime");
        totalBurnTime = tag.getInt("TotalBurnTime");
        energyStorage.amount = tag.getLong("Energy");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.dionysian_press");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new DionysianPressMenu(syncId, playerInventory, this, this.dataAccess);
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
        if (slot == 0) return isFruit(stack);
        if (slot == 3) return isFuel(stack);
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return new int[]{ 1, 2, 3 }; // Output nuggets, pomace, or empty bucket
        } else if (side == Direction.UP) {
            return new int[]{ 0 }; // Fruit from top
        } else {
            return new int[]{ 3, 0 }; // Fuel or fruit from sides
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot == 0) return isFruit(stack);
        if (slot == 3) return isFuel(stack);
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot == 1 || slot == 2) return true;
        if (slot == 3 && stack.is(Items.BUCKET)) return true;
        return false;
    }

    @Override
    public boolean isInputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 0 || slot == 3;
    }

    @Override
    public boolean isOutputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 1 || slot == 2;
    }
}

