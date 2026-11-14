package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
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
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.VolcanitePipeEnergyUpgradeItem;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.Iterator;
import java.util.List;

public class HydrothermalLithofierBlockEntity extends BlockEntity implements MenuProvider, InventoryHolder, WorldlyContainer, MachineInventory {
    public static final int INVENTORY_SIZE = 6;
    // Slot 0: Water Droplet / Bucket Input
    // Slot 1: Water Bucket Output
    // Slot 2: Lava Droplet / Bucket Input
    // Slot 3: Lava Bucket Output
    // Slot 4: Rock Output
    // Slot 5: Energy / Speed Upgrade

    public static final int MAX_WATER = 4000;
    public static final int MAX_LAVA = 4000;

    public static final int MODE_COBBLESTONE = 0;
    public static final int MODE_STONE = 1;
    public static final int MODE_SMOOTH_BASALT = 2;
    public static final int MODE_DEEPSLATE = 3;

    private int waterAmount = 0;
    private int lavaAmount = 0;
    private int progress = 0;
    private int mode = MODE_COBBLESTONE;

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(50000, 5000, 0) {
        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            HydrothermalLithofierBlockEntity.this.setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            if (slot == 0) return stack.is(Items.WATER_BUCKET) || stack.is(ObsidiumMod.DEW_DROPLET);
            if (slot == 2) return stack.is(Items.LAVA_BUCKET) || stack.is(ObsidiumMod.MAGMA_DROPLET);
            if (slot == 5) return stack.getItem() instanceof VolcanitePipeEnergyUpgradeItem;
            return false;
        }
    };

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> waterAmount;
                case 1 -> lavaAmount;
                case 2 -> progress;
                case 3 -> getMaxProgressForMode();
                case 4 -> (int) energyStorage.amount;
                case 5 -> (int) energyStorage.capacity;
                case 6 -> mode;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> waterAmount = value;
                case 1 -> lavaAmount = value;
                case 2 -> progress = value;
                case 4 -> energyStorage.amount = value;
                case 6 -> mode = Math.max(0, Math.min(3, value));
            }
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public HydrothermalLithofierBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.HYDROTHERMAL_LITHOFIER_BE, pos, state);
    }

    public int getWaterAmount() { return waterAmount; }
    public int getLavaAmount() { return lavaAmount; }
    public int getMode() { return mode; }
    public void setMode(int newMode) {
        if (newMode >= 0 && newMode <= 3 && this.mode != newMode) {
            this.mode = newMode;
            this.progress = 0;
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    public void addWater(int amount) {
        this.waterAmount = Math.min(MAX_WATER, this.waterAmount + amount);
        setChanged();
    }

    public void addLava(int amount) {
        this.lavaAmount = Math.min(MAX_LAVA, this.lavaAmount + amount);
        setChanged();
    }

    public int getMaxProgressForMode() {
        return switch (mode) {
            case MODE_STONE -> 30;
            case MODE_SMOOTH_BASALT -> 40;
            case MODE_DEEPSLATE -> 50;
            default -> 20; // Cobblestone
        };
    }

    public int getWaterCostForMode() {
        return switch (mode) {
            case MODE_STONE -> 35;
            case MODE_SMOOTH_BASALT -> 50;
            case MODE_DEEPSLATE -> 75;
            default -> 25; // Cobblestone
        };
    }

    public int getLavaCostForMode() {
        return switch (mode) {
            case MODE_STONE -> 35;
            case MODE_SMOOTH_BASALT -> 50;
            case MODE_DEEPSLATE -> 75;
            default -> 25; // Cobblestone
        };
    }

    public ItemStack getResultForMode() {
        return switch (mode) {
            case MODE_STONE -> new ItemStack(Blocks.STONE);
            case MODE_SMOOTH_BASALT -> new ItemStack(Blocks.SMOOTH_BASALT);
            case MODE_DEEPSLATE -> new ItemStack(Blocks.DEEPSLATE);
            default -> new ItemStack(Blocks.COBBLESTONE);
        };
    }

    public final Storage<FluidVariant> fluidStorage = new Storage<FluidVariant>() {
        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isOf(Fluids.WATER)) {
                long maxInsert_mB = maxAmount / 81L;
                long space_mB = MAX_WATER - waterAmount;
                long toInsert = Math.min(maxInsert_mB, space_mB);
                if (toInsert > 0) {
                    transaction.addCloseCallback((t, res) -> {
                        if (res.wasCommitted()) {
                            waterAmount += (int) toInsert;
                            setChanged();
                        }
                    });
                    return toInsert * 81L;
                }
            } else if (resource.isOf(Fluids.LAVA)) {
                long maxInsert_mB = maxAmount / 81L;
                long space_mB = MAX_LAVA - lavaAmount;
                long toInsert = Math.min(maxInsert_mB, space_mB);
                if (toInsert > 0) {
                    transaction.addCloseCallback((t, res) -> {
                        if (res.wasCommitted()) {
                            lavaAmount += (int) toInsert;
                            setChanged();
                        }
                    });
                    return toInsert * 81L;
                }
            }
            return 0;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public Iterator<StorageView<FluidVariant>> iterator() {
            return List.<StorageView<FluidVariant>>of().iterator();
        }
    };

    public static void tick(Level level, BlockPos pos, BlockState state, HydrothermalLithofierBlockEntity entity) {
        if (level.isClientSide()) return;

        boolean changed = false;

        // Process bucket / droplet inputs
        // Water (slot 0 -> slot 1)
        ItemStack waterSlot = entity.inventory.getItem(0);
        if (!waterSlot.isEmpty()) {
            if (waterSlot.is(Items.WATER_BUCKET) && entity.waterAmount <= MAX_WATER - 1000) {
                ItemStack outSlot = entity.inventory.getItem(1);
                if (outSlot.isEmpty() || (outSlot.is(Items.BUCKET) && outSlot.getCount() < outSlot.getMaxStackSize())) {
                    entity.waterAmount += 1000;
                    waterSlot.shrink(1);
                    if (outSlot.isEmpty()) entity.inventory.setItem(1, new ItemStack(Items.BUCKET));
                    else outSlot.grow(1);
                    changed = true;
                }
            } else if (waterSlot.is(ObsidiumMod.DEW_DROPLET) && entity.waterAmount <= MAX_WATER - 250) {
                entity.waterAmount += 250;
                waterSlot.shrink(1);
                changed = true;
            }
        }

        // Lava (slot 2 -> slot 3)
        ItemStack lavaSlot = entity.inventory.getItem(2);
        if (!lavaSlot.isEmpty()) {
            if (lavaSlot.is(Items.LAVA_BUCKET) && entity.lavaAmount <= MAX_LAVA - 1000) {
                ItemStack outSlot = entity.inventory.getItem(3);
                if (outSlot.isEmpty() || (outSlot.is(Items.BUCKET) && outSlot.getCount() < outSlot.getMaxStackSize())) {
                    entity.lavaAmount += 1000;
                    lavaSlot.shrink(1);
                    if (outSlot.isEmpty()) entity.inventory.setItem(3, new ItemStack(Items.BUCKET));
                    else outSlot.grow(1);
                    changed = true;
                }
            } else if (lavaSlot.is(ObsidiumMod.MAGMA_DROPLET) && entity.lavaAmount <= MAX_LAVA - 250) {
                entity.lavaAmount += 250;
                lavaSlot.shrink(1);
                changed = true;
            }
        }

        int waterCost = entity.getWaterCostForMode();
        int lavaCost = entity.getLavaCostForMode();
        ItemStack targetResult = entity.getResultForMode();
        ItemStack currentOut = entity.inventory.getItem(4);

        boolean canOutput = currentOut.isEmpty() || (ItemStack.isSameItemSameComponents(currentOut, targetResult) && currentOut.getCount() < currentOut.getMaxStackSize());
        boolean hasFluids = entity.waterAmount >= waterCost && entity.lavaAmount >= lavaCost;

        if (hasFluids && canOutput) {
            // Speed calculation: passive = 1 tick, energy = +1, upgrade = +tier
            int speed = 1;
            int energyCost = 0;
            if (entity.energyStorage.amount >= 120) {
                speed += 1;
                energyCost += 120;

                ItemStack upgrade = entity.inventory.getItem(5);
                if (!upgrade.isEmpty() && upgrade.getItem() instanceof VolcanitePipeEnergyUpgradeItem upItem) {
                    int tier = upItem.getTier();
                    int extraSpeed = tier * 2;
                    int extraEnergy = tier * 120;
                    if (entity.energyStorage.amount >= (energyCost + extraEnergy)) {
                        speed += extraSpeed;
                        energyCost += extraEnergy;
                    }
                }
            }

            if (energyCost > 0) {
                entity.energyStorage.amount -= energyCost;
            }

            entity.progress += speed;
            int maxProg = entity.getMaxProgressForMode();

            if (entity.progress >= maxProg) {
                entity.progress = 0;
                entity.waterAmount -= waterCost;
                entity.lavaAmount -= lavaCost;

                if (currentOut.isEmpty()) {
                    entity.inventory.setItem(4, targetResult.copy());
                } else {
                    currentOut.grow(1);
                }

                level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.8F);
                changed = true;
            }

            if (!state.getValue(HydrothermalLithofierBlock.LIT)) {
                level.setBlock(pos, state.setValue(HydrothermalLithofierBlock.LIT, true), 3);
            }
            changed = true;
        } else {
            if (entity.progress > 0) {
                entity.progress = 0;
                changed = true;
            }
            if (state.getValue(HydrothermalLithofierBlock.LIT)) {
                level.setBlock(pos, state.setValue(HydrothermalLithofierBlock.LIT, false), 3);
            }
        }

        if (changed) {
            entity.setChanged();
        }

        VolcaniteStoragePipeBlockEntity.autoEjectMachineOutputs(level, pos, entity);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Water", waterAmount);
        tag.putInt("Lava", lavaAmount);
        tag.putInt("Progress", progress);
        tag.putInt("Mode", mode);
        tag.putLong("Energy", energyStorage.amount);

        CompoundTag invTag = new CompoundTag();
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                invTag.put("Slot" + i, stack.save(registries));
            }
        }
        tag.put("Inventory", invTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        waterAmount = tag.getInt("Water");
        lavaAmount = tag.getInt("Lava");
        progress = tag.getInt("Progress");
        mode = tag.getInt("Mode");
        energyStorage.amount = tag.getLong("Energy");

        CompoundTag invTag = tag.getCompound("Inventory");
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (invTag.contains("Slot" + i)) {
                inventory.setItem(i, ItemStack.parse(registries, invTag.getCompound("Slot" + i)).orElse(ItemStack.EMPTY));
            }
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.hydrothermal_lithofier");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new HydrothermalLithofierMenu(syncId, playerInventory, this.inventory, this.data);
    }

    @Override
    public Container getInventory() {
        return this.inventory;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return new int[]{4, 1, 3}; // Output rock, empty buckets
        } else if (side == Direction.UP) {
            return new int[]{0, 2};    // Water & Lava input
        } else {
            return new int[]{0, 2, 4}; // Sided access
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 4 || slot == 1 || slot == 3;
    }

    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
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
        return inventory.stillValid(player);
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (slot == 0) {
            return stack.is(net.minecraft.world.item.Items.WATER_BUCKET) || stack.is(ObsidiumMod.DEW_DROPLET);
        }
        if (slot == 2) {
            return stack.is(net.minecraft.world.item.Items.LAVA_BUCKET) || stack.is(ObsidiumMod.MAGMA_DROPLET);
        }
        if (slot == 5) {
            return stack.is(ObsidiumMod.ASTRAL_VOLCANIC_CATALYST);
        }
        return false;
    }

    @Override
    public boolean isInputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean isOutputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 4 || slot == 1 || slot == 3;
    }
}

