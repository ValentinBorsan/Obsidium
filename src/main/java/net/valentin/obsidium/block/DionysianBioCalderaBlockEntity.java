package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyItem;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class DionysianBioCalderaBlockEntity extends BlockEntity implements MenuProvider, InventoryHolder, MachineInventory, WorldlyContainer {

    public static final int INVENTORY_SIZE = 3;
    // Slot 0: Fruit / Biomass Input
    // Slot 1: Byproduct Output (Bone Meal / Volcanic Ash)
    // Slot 2: Battery / Tool Charge Slot

    public static final int MAX_ENERGY = 1_000_000;
    public static final int MAX_EXTRACT_RATE = 3_000;
    public static final int MAX_BIOGAS = 32_000; // 32,000 mB reservoir

    private final NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private int fermentTime = 0;
    private int maxFermentTime = 0;
    private int currentRfPerTick = 0;
    private int biogasAmount = 0;
    private int compostCounter = 0;

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
                case 0 -> fermentTime;
                case 1 -> maxFermentTime;
                case 2 -> currentRfPerTick;
                case 3 -> (int) (energyStorage.amount >> 16);
                case 4 -> (int) (energyStorage.amount & 0xFFFF);
                case 5 -> (int) (energyStorage.capacity >> 16);
                case 6 -> (int) (energyStorage.capacity & 0xFFFF);
                case 7 -> biogasAmount;
                case 8 -> MAX_BIOGAS;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> fermentTime = value;
                case 1 -> maxFermentTime = value;
                case 2 -> currentRfPerTick = value;
                case 7 -> biogasAmount = value;
            }
        }

        @Override
        public int getCount() {
            return 9;
        }
    };

    public DionysianBioCalderaBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.DIONYSIAN_BIO_CALDERA_BE, pos, state);
    }

    public boolean isFermenting() {
        return this.fermentTime > 0;
    }

    public int getBioOilAmount() {
        return this.biogasAmount;
    }

    public int extractBioOil(int maxExtract, boolean simulate) {
        int toExtract = Math.min(this.biogasAmount, maxExtract);
        if (toExtract > 0 && !simulate) {
            this.biogasAmount -= toExtract;
            setChanged();
        }
        return toExtract;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DionysianBioCalderaBlockEntity entity) {
        if (level.isClientSide()) return;
        boolean dirty = false;

        // 1. Fermentation of Biomass in slot 0 to produce Biogas
        if (entity.biogasAmount < MAX_BIOGAS && !entity.items.get(0).isEmpty()) {
            ItemStack input = entity.items.get(0);
            int bioYield = getBiomassYield(input);
            if (bioYield > 0) {
                int toAdd = Math.min(bioYield, MAX_BIOGAS - entity.biogasAmount);
                entity.biogasAmount += toAdd;
                input.shrink(1);
                dirty = true;

                // Produce bone meal fertilizer byproduct periodically
                entity.compostCounter += toAdd;
                if (entity.compostCounter >= 2_000) {
                    entity.compostCounter = 0;
                    entity.outputByproduct(new ItemStack(Items.BONE_MEAL));
                }
            }
        }

        // 2. Burning Bio-Oil slowly to generate auxiliary Bio-Energy (400 RF/t)
        if (entity.biogasAmount >= 5 && entity.energyStorage.amount < entity.energyStorage.capacity) {
            entity.biogasAmount -= 5;
            long added = Math.min(400, entity.energyStorage.capacity - entity.energyStorage.amount);
            entity.energyStorage.amount += added;
            entity.currentRfPerTick = 400;
            entity.fermentTime = 20;
            entity.maxFermentTime = 20;
            dirty = true;

            // Green bubbling particles
            if (level.getGameTime() % 25 == 0 && level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 1.0, pos.getY() + 2.0, pos.getZ() + 1.0,
                        3, 0.4, 0.1, 0.4, 0.05);
            }
        } else if (entity.energyStorage.amount >= entity.energyStorage.capacity || entity.biogasAmount < 10) {
            if (entity.currentRfPerTick > 0) {
                entity.currentRfPerTick = 0;
                dirty = true;
            }
        }

        // 3. Charge battery/tool in slot 2
        ItemStack batteryStack = entity.items.get(2);
        if (!batteryStack.isEmpty() && entity.energyStorage.amount > 0) {
            EnergyStorage itemStorage = EnergyStorage.ITEM.find(batteryStack, null);
            if (itemStorage != null && itemStorage.supportsInsertion()) {
                EnergyStorageUtil.move(entity.energyStorage, itemStorage, Math.min(2_000, entity.energyStorage.amount), null);
                dirty = true;
            } else if (batteryStack.getItem() instanceof SimpleEnergyItem energyItem) {
                long needed = energyItem.getEnergyCapacity(batteryStack) - energyItem.getStoredEnergy(batteryStack);
                if (needed > 0) {
                    long toTransfer = Math.min(Math.min(needed, 2_000), entity.energyStorage.amount);
                    energyItem.setStoredEnergy(batteryStack, energyItem.getStoredEnergy(batteryStack) + toTransfer);
                    entity.energyStorage.amount -= toTransfer;
                    dirty = true;
                }
            }
        }

        // 4. Push RF to all surrounding external blocks from the 2x2x2 footprint
        if (entity.energyStorage.amount > 0) {
            for (int dx = -1; dx <= 2; dx++) {
                for (int dy = -1; dy <= 2; dy++) {
                    for (int dz = -1; dz <= 2; dz++) {
                        if (dx >= 0 && dx <= 1 && dy >= 0 && dy <= 1 && dz >= 0 && dz <= 1) continue;
                        BlockPos targetPos = pos.offset(dx, dy, dz);
                        EnergyStorage targetStorage = EnergyStorage.SIDED.find(level, targetPos, null);
                        if (targetStorage != null && targetStorage.supportsInsertion()) {
                            long moved = EnergyStorageUtil.move(entity.energyStorage, targetStorage, MAX_EXTRACT_RATE, null);
                            if (moved > 0) {
                                dirty = true;
                                if (entity.energyStorage.amount <= 0) break;
                            }
                        }
                    }
                    if (entity.energyStorage.amount <= 0) break;
                }
                if (entity.energyStorage.amount <= 0) break;
            }
        }

        // 5. Auto-pump Bio-Oil from any block of the 2x2x2 footprint into adjacent or pipe-connected Distillation Chambers, Diesel Generators, or Tanks
        if (entity.biogasAmount > 0 && level.getGameTime() % 5 == 0) {
            int pumped = pumpBioOilToTargets(level, pos, FluidVariant.of(ObsidiumMod.BIO_OIL_FLUID), Math.min(entity.biogasAmount, 500));
            if (pumped > 0) {
                entity.biogasAmount -= pumped;
                if (entity.biogasAmount < 0) entity.biogasAmount = 0;
                dirty = true;
            }
        }

        if (dirty) entity.setChanged();

        VolcaniteStoragePipeBlockEntity.autoEjectMachineOutputs(level, pos, entity);
    }

    private static int pumpBioOilToTargets(Level level, BlockPos originPos, FluidVariant fluid, int maxAmount) {
        int remaining = maxAmount;
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = 0; dz <= 1; dz++) {
                    BlockPos p = originPos.offset(dx, dy, dz);
                    queue.add(p);
                    visited.add(p);
                }
            }
        }

        while (!queue.isEmpty() && visited.size() < 72 && remaining > 0) {
            BlockPos curr = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = curr.relative(dir);
                if (!visited.add(next)) continue;
                BlockEntity be = level.getBlockEntity(next);
                if (be instanceof VolcanicDistillationChamberBlockEntity chamber) {
                    int accepted = chamber.insertFluid(fluid, remaining, false);
                    remaining -= accepted;
                    if (remaining <= 0) break;
                } else if (be instanceof VolcanicDieselGeneratorBlockEntity dieselGen && VolcanicDieselGeneratorBlockEntity.getFuelProfile(fluid) != null) {
                    int accepted = dieselGen.insertFluidInternal(fluid, remaining, false);
                    remaining -= accepted;
                    if (remaining <= 0) break;
                } else if (be instanceof LiquidTankBlockEntity tank) {
                    int accepted = tank.insertFluid(fluid, remaining, false);
                    remaining -= accepted;
                    if (remaining <= 0) break;
                } else if (be instanceof VolcaniteLiquidStorageTankBlockEntity storageTank) {
                    int accepted = storageTank.insertFluid(fluid, remaining, false);
                    remaining -= accepted;
                    if (remaining <= 0) break;
                } else if (be instanceof NaiadFluidPipeBlockEntity
                        || be instanceof VolcaniteStoragePipeBlockEntity
                        || be instanceof VolcaniteUniversalConduitBlockEntity) {
                    queue.add(next);
                }
            }
        }
        return maxAmount - remaining;
    }

    private static int getBiomassYield(ItemStack stack) {
        if (stack.is(ObsidiumMod.FOLIAGE_BALE_ITEM)) return 2_000;
        if (stack.is(Items.HAY_BLOCK)) return 2_700;
        if (stack.is(ObsidiumMod.TARTAREAN_NETHERITE_FRUIT)) return 4_000;
        if (stack.is(ObsidiumMod.CORNUCOPIA)) return 3_000;
        if (stack.is(ObsidiumMod.ARTEMIS_SILVER_FRUIT)) return 2_200;
        if (stack.is(ObsidiumMod.MIDAS_GOLD_FRUIT) || stack.is(ObsidiumMod.ARES_IRON_FRUIT) || stack.is(ObsidiumMod.HEPHAESTUS_VOLCANITE_FRUIT) || stack.is(ObsidiumMod.CRONUS_LEAD_FRUIT)) return 2_000;
        if (stack.is(ObsidiumMod.HERMES_COPPER_FRUIT) || stack.is(ObsidiumMod.ELECTRUM_FRUIT) || stack.is(ObsidiumMod.ZEUS_TIN_FRUIT)) return 1_500;
        if (stack.getItem() instanceof net.valentin.obsidium.item.VolcanicPetalItem
                || stack.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("minecraft", "crops")))
                || stack.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "crops")))) {
            if (stack.is(ObsidiumMod.NETHERITE_PETAL) || stack.is(ObsidiumMod.DIAMOND_PETAL)) return 800;
            if (stack.is(ObsidiumMod.VOLCANITE_PETAL)) return 600;
            return 450;
        }
        if (stack.getItem() instanceof net.valentin.obsidium.item.VolcanicSeedItem
                || stack.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("minecraft", "seeds")))
                || stack.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "seeds")))
                || stack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS)
                || stack.is(Items.WHEAT_SEEDS) || stack.is(Items.PUMPKIN_SEEDS)
                || stack.is(Items.MELON_SEEDS) || stack.is(Items.BEETROOT_SEEDS)
                || stack.is(Items.TORCHFLOWER_SEEDS) || stack.is(Items.PITCHER_POD)) {
            if (stack.getItem() instanceof net.valentin.obsidium.item.VolcanicSeedItem) return 400;
            return 250;
        }
        if (stack.is(Items.APPLE) || stack.is(Items.GOLDEN_APPLE)) return 500;
        if (stack.is(Items.CARROT) || stack.is(Items.POTATO) || stack.is(Items.BEETROOT) || stack.is(Items.SWEET_BERRIES)) return 250;
        if (stack.is(Items.WHEAT) || stack.is(Items.MELON_SLICE) || stack.is(Items.PUMPKIN)) return 300;
        if (stack.is(Items.KELP) || stack.is(Items.DRIED_KELP) || stack.is(Items.SUGAR_CANE)) return 200;
        if (stack.is(ItemTags.LEAVES)
                || stack.is(ItemTags.SAPLINGS)
                || stack.is(ItemTags.FLOWERS)
                || DemetrianBonsaiPlanterBlockEntity.isSaplingItem(stack)
                || (stack.getItem() instanceof BlockItem bi
                        && (bi.getBlock() instanceof LeavesBlock || bi.getBlock() instanceof MineralTreeLeavesBlock))) {
            return 150;
        }
        return 0;
    }

    private void outputByproduct(ItemStack byproduct) {
        ItemStack current = this.items.get(1);
        if (current.isEmpty()) {
            this.items.set(1, byproduct);
        } else if (current.is(byproduct.getItem())) {
            current.grow(Math.min(byproduct.getCount(), current.getMaxStackSize() - current.getCount()));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatableWithFallback("block.obsidium.dionysian_bio_caldera", "Dionysian Bio-Caldera");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new DionysianBioCalderaMenu(syncId, playerInventory, this, this.dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putInt("FermentTime", this.fermentTime);
        tag.putInt("MaxFermentTime", this.maxFermentTime);
        tag.putInt("CurrentRf", this.currentRfPerTick);
        tag.putLong("Energy", this.energyStorage.amount);
        tag.putInt("BiogasAmount", this.biogasAmount);
        tag.putInt("CompostCounter", this.compostCounter);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.fermentTime = tag.getInt("FermentTime");
        this.maxFermentTime = tag.getInt("MaxFermentTime");
        this.currentRfPerTick = tag.getInt("CurrentRf");
        this.energyStorage.amount = tag.getLong("Energy");
        this.biogasAmount = tag.getInt("BiogasAmount");
        this.compostCounter = tag.getInt("CompostCounter");
    }

    // WorldlyContainer / InventoryHolder
    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{0, 1, 2};
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (slot == 0) return getBiomassYield(stack) > 0;
        if (slot == 2) {
            return stack.getItem() instanceof SimpleEnergyItem
                    || EnergyStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack)) != null;
        }
        return false;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 0 && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 1;
    }

    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
        setChanged();
    }

    @Override
    public boolean isInputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 0;
    }

    @Override
    public boolean isOutputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 1;
    }

    @Override
    public Container getInventory() {
        return this;
    }
}
