package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyStorage;
import net.minecraft.world.MenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.valentin.obsidium.item.VolcaniteBottleItem;

public class VolcaniteGeneratorBlockEntity extends BlockEntity implements MenuProvider, InventoryHolder, MachineInventory {
    // Solid fuel / Burn time system
    private int burnTime = 0;
    private int maxBurnTime = 0;

    // Optional lava buffer (can still receive lava via pipes or bottles/buckets)
    private int lavaAmount = 0;
    private final int MAX_LAVA = 16000;
    
    // RF generation values - rebalanced/nerfed generation rate (5 RF/t)
    public static final int RF_PER_TICK = 5;
    private static final int MAX_ENERGY = 100000;

    private final SimpleContainer inventory = new SimpleContainer(2) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot == 0 && (getFuelBurnTime(stack) > 0 || stack.is(Items.LAVA_BUCKET) || stack.getItem() instanceof VolcaniteBottleItem);
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

    @Override
    public net.minecraft.world.Container getInventory() {
        return this.inventory;
    }

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index == 0) return burnTime;
            if (index == 1) return maxBurnTime;
            if (index == 2) return lavaAmount;
            if (index == 3) return (int) (energyStorage.amount >> 16);
            if (index == 4) return (int) (energyStorage.amount & 0xFFFF);
            return 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) burnTime = value;
            if (index == 1) maxBurnTime = value;
            if (index == 2) lavaAmount = value;
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(MAX_ENERGY, 0, 500) {
        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    public VolcaniteGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.VOLCANITE_GENERATOR_BE, pos, state);
    }

    public static int getFuelBurnTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.is(ObsidiumMod.VOLCANIC_ASH)) return 800;
        return AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0);
    }

    public static boolean isFuel(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(ObsidiumMod.VOLCANIC_ASH)) return true;
        if (stack.getItem() instanceof VolcaniteBottleItem && VolcaniteBottleItem.getLava(stack) > 0) return true;
        if (stack.is(Items.LAVA_BUCKET)) return true;
        return AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0) > 0;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VolcaniteGeneratorBlockEntity entity) {
        if (level.isClientSide()) return;

        boolean didSomething = false;

        // 1. Drain lava bottle if present in slot 0 to fill internal lava tank
        ItemStack input = entity.inventory.getItem(0);
        if (!input.isEmpty() && input.getItem() instanceof VolcaniteBottleItem) {
            int bottleLava = VolcaniteBottleItem.getLava(input);
            if (bottleLava > 0 && entity.lavaAmount < entity.MAX_LAVA) {
                int toTransfer = Math.min(bottleLava, Math.min(1000, entity.MAX_LAVA - entity.lavaAmount));
                if (toTransfer > 0) {
                    entity.lavaAmount += toTransfer;
                    VolcaniteBottleItem.setLava(input, bottleLava - toTransfer);
                    didSomething = true;
                }
            }
        }

        // 2. Fuel consumption when burnTime is finished and energy has space
        boolean canProduce = entity.energyStorage.amount < entity.energyStorage.capacity;
        if (entity.burnTime <= 0 && canProduce) {
            // First priority: internal lava tank
            if (entity.lavaAmount >= 10) {
                entity.lavaAmount -= 10;
                entity.burnTime = 100; // 10 mB = 5 seconds of 10 RF/t
                entity.maxBurnTime = 100;
                didSomething = true;
            } else if (!input.isEmpty()) {
                // Second priority: item in slot 0
                if (input.is(Items.LAVA_BUCKET)) {
                    ItemStack output = entity.inventory.getItem(1);
                    if (output.isEmpty() || (output.is(Items.BUCKET) && output.getCount() < 16)) {
                        input.shrink(1);
                        if (output.isEmpty()) {
                            entity.inventory.setItem(1, new ItemStack(Items.BUCKET));
                        } else {
                            output.grow(1);
                        }
                        entity.burnTime = 10000;
                        entity.maxBurnTime = 10000;
                        didSomething = true;
                    }
                } else if (input.getItem() instanceof VolcaniteBottleItem) {
                    int bottleLava = VolcaniteBottleItem.getLava(input);
                    if (bottleLava >= 100) {
                        VolcaniteBottleItem.setLava(input, bottleLava - 100);
                        entity.burnTime = 1000;
                        entity.maxBurnTime = 1000;
                        didSomething = true;
                    }
                } else {
                    int fuelVal = getFuelBurnTime(input);
                    if (fuelVal > 0) {
                        entity.burnTime = fuelVal;
                        entity.maxBurnTime = fuelVal;
                        input.shrink(1);
                        didSomething = true;
                    }
                }
            }
        }

        // 3. Generate energy while burning
        boolean isGenerating = false;
        if (entity.burnTime > 0) {
            entity.burnTime--;
            if (entity.energyStorage.amount < entity.energyStorage.capacity) {
                entity.energyStorage.amount = Math.min(entity.energyStorage.capacity, entity.energyStorage.amount + RF_PER_TICK);
            }
            isGenerating = true;
            didSomething = true;
        }

        // 4. Update block LIT state
        boolean currentLitState = state.getValue(VolcaniteGeneratorBlock.LIT);
        if (currentLitState != isGenerating) {
            level.setBlock(pos, state.setValue(VolcaniteGeneratorBlock.LIT, isGenerating), 3);
            didSomething = true;
        }

        // 5. Push energy to adjacent blocks
        if (entity.energyStorage.amount > 0) {
            for (Direction dir : Direction.values()) {
                EnergyStorage target = EnergyStorage.SIDED.find(level, pos.relative(dir), dir.getOpposite());
                if (target != null) {
                    long moved = EnergyStorageUtil.move(entity.energyStorage, target, 500, null);
                    if (moved > 0) {
                        didSomething = true;
                    }
                }
            }
        }

        if (didSomething) {
            entity.setChanged();
        }

        VolcaniteStoragePipeBlockEntity.autoEjectMachineOutputs(level, pos, entity);
    }

    public int getLavaAmount() { return lavaAmount; }
    public int getMaxLava() { return MAX_LAVA; }
    public int getBurnTime() { return burnTime; }
    public int getMaxBurnTime() { return maxBurnTime; }

    public void addLava(int amount) {
        this.lavaAmount = Math.min(MAX_LAVA, this.lavaAmount + amount);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("MaxBurnTime", maxBurnTime);
        tag.putInt("LavaAmount", lavaAmount);
        tag.putLong("Energy", energyStorage.amount);
        MachineInventory.saveInventory(tag, inventory, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.burnTime = tag.getInt("BurnTime");
        this.maxBurnTime = tag.getInt("MaxBurnTime");
        this.lavaAmount = tag.getInt("LavaAmount");
        this.energyStorage.amount = tag.getLong("Energy");
        MachineInventory.loadInventory(tag, this.inventory, provider);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.volcanite_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new VolcaniteGeneratorMenu(syncId, playerInventory, this.inventory, this.data);
    }
}
