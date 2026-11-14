package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyItem;
import team.reborn.energy.api.base.SimpleEnergyStorage;

public class DemetrianBiomassGeneratorBlockEntity extends BlockEntity implements MenuProvider, InventoryHolder, MachineInventory, WorldlyContainer {

    private static final TagKey<net.minecraft.world.item.Item> BIOGAS_LEAVES = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("obsidium", "biogas_leaves")
    );

    public static final int INVENTORY_SIZE = 3;
    // Slot 0: Fruit / Biomass Input
    // Slot 1: Byproduct Output (Volcanic Ash)
    // Slot 2: Battery / Tool Charging Slot

    public static final int MAX_ENERGY = 500_000;
    public static final int MAX_EXTRACT_RATE = 2_500;
    public static final int MAX_BIOGAS = 16_000; // 16,000 mB internal reservoir

    public record FruitBioData(int rfPerTick, int durationTicks, int ashYield, int biogasYield) {}

    private final NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private int burnTime = 0;
    private int maxBurnTime = 0;
    private int currentRfPerTick = 0;
    private int pendingAsh = 0;
    private int biogasAmount = 0;

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
                case 0 -> burnTime;
                case 1 -> maxBurnTime;
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
                case 0 -> burnTime = value;
                case 1 -> maxBurnTime = value;
                case 2 -> currentRfPerTick = value;
                case 7 -> biogasAmount = value;
            }
        }

        @Override
        public int getCount() {
            return 9;
        }
    };

    public DemetrianBiomassGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.DEMETRIAN_BIOMASS_GENERATOR_BE, pos, state);
    }

    public static FruitBioData getFruitBioData(ItemStack stack) {
        if (stack.isEmpty()) return null;

        // Compressed Foliage Biomass Bale: high-density organic fuel
        if (stack.is(ObsidiumMod.FOLIAGE_BALE_ITEM)) {
            return new FruitBioData(160, 720, 1, 3800); // 115,200 FE, 1 Ash, 3,800 mB Biogas!
        }

        // All tree leaves (Vanilla + Modded): easy common biogas source
        if (stack.is(BIOGAS_LEAVES) || stack.is(ItemTags.LEAVES)) {
            return new FruitBioData(30, 100, 0, 350); // 3,000 FE, 350 mB Biogas
        }

        // Volcanic ore petals and floral crops: decent organic biomass (higher-tier ores give more energy)
        if (stack.getItem() instanceof net.valentin.obsidium.item.VolcanicPetalItem
                || stack.is(TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.fromNamespaceAndPath("minecraft", "crops")))
                || stack.is(TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "crops")))) {
            if (stack.is(ObsidiumMod.NETHERITE_PETAL) || stack.is(ObsidiumMod.DIAMOND_PETAL)) {
                return new FruitBioData(80, 200, 0, 800); // 16,000 FE, 800 mB
            }
            if (stack.is(ObsidiumMod.VOLCANITE_PETAL)) {
                return new FruitBioData(60, 180, 0, 600); // 10,800 FE, 600 mB
            }
            return new FruitBioData(40, 150, 0, 450); // 6,000 FE, 450 mB
        }

        // Seeds (volcanic ore seeds + vanilla/modded seeds): good biomass yield
        if (stack.getItem() instanceof net.valentin.obsidium.item.VolcanicSeedItem
                || stack.is(TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.fromNamespaceAndPath("minecraft", "seeds")))
                || stack.is(TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "seeds")))
                || stack.is(net.minecraft.tags.ItemTags.VILLAGER_PLANTABLE_SEEDS)
                || stack.is(net.minecraft.world.item.Items.WHEAT_SEEDS)
                || stack.is(net.minecraft.world.item.Items.PUMPKIN_SEEDS)
                || stack.is(net.minecraft.world.item.Items.MELON_SEEDS)
                || stack.is(net.minecraft.world.item.Items.BEETROOT_SEEDS)
                || stack.is(net.minecraft.world.item.Items.TORCHFLOWER_SEEDS)
                || stack.is(net.minecraft.world.item.Items.PITCHER_POD)
                || GeothermalArboristBlockEntity.getCropStateForSeed(stack) != null) {
            if (stack.getItem() instanceof net.valentin.obsidium.item.VolcanicSeedItem) {
                return new FruitBioData(50, 140, 0, 450); // 7,000 FE, 450 mB
            }
            return new FruitBioData(25, 120, 0, 250); // 3,000 FE, 250 mB
        }

        if (stack.is(ObsidiumMod.TARTAREAN_NETHERITE_FRUIT)) {
            // Apex Tartarean Netherite bio-caloric singularity
            return new FruitBioData(1200, 800, 4, 12000); // 960,000 FE, 4 Ash, 12,000 mB
        } else if (stack.is(ObsidiumMod.HEPHAESTUS_VOLCANITE_FRUIT)) {
            // Supreme volcanic bio-power
            return new FruitBioData(800, 500, 3, 8000); // 400,000 FE, 3 Ash, 8000 mB
        } else if (stack.is(ObsidiumMod.ELECTRUM_FRUIT)) {
            // Super conductive bio-alloy
            return new FruitBioData(420, 400, 2, 4000); // 168,000 FE, 2 Ash, 4000 mB
        } else if (stack.is(ObsidiumMod.ARTEMIS_SILVER_FRUIT)) {
            // Radiant lunar silver essence
            return new FruitBioData(280, 320, 2, 2800); // 89,600 FE, 2 Ash, 2800 mB
        } else if (stack.is(ObsidiumMod.MIDAS_GOLD_FRUIT)) {
            // Rich golden biomass
            return new FruitBioData(260, 350, 2, 2500); // 91,000 FE, 2 Ash, 2500 mB
        } else if (stack.is(ObsidiumMod.CRONUS_LEAD_FRUIT)) {
            // Dense titan lead fuel
            return new FruitBioData(220, 220, 2, 2000); // 48,400 FE, 2 Ash, 2000 mB
        } else if (stack.is(ObsidiumMod.ARES_IRON_FRUIT)) {
            // Durable dense fuel
            return new FruitBioData(140, 250, 1, 1500); // 35,000 FE, 1 Ash, 1500 mB
        } else if (stack.is(ObsidiumMod.ZEUS_TIN_FRUIT)) {
            // Galvanic storm tin fruit
            return new FruitBioData(100, 210, 1, 1200); // 21,000 FE, 1 Ash, 1200 mB
        } else if (stack.is(ObsidiumMod.HERMES_COPPER_FRUIT)) {
            // Swift copper reaction
            return new FruitBioData(80, 200, 1, 1000); // 16,000 FE, 1 Ash, 1000 mB
        } else if (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            return new FruitBioData(300, 300, 2, 2000); // 90,000 FE, 2 Ash, 2000 mB
        } else if (stack.is(Items.APPLE)
                || stack.is(Items.SWEET_BERRIES)
                || stack.is(Items.GLOW_BERRIES)
                || stack.is(Items.MELON_SLICE)
                || stack.is(Items.CHORUS_FRUIT)) {
            // Standard fruits
            return new FruitBioData(40, 150, 1, 500); // 6,000 FE, 1 Ash, 500 mB
        }

        return null;
    }

    public static boolean isBiomassFruit(ItemStack stack) {
        return getFruitBioData(stack) != null;
    }

    public void tickServer(Level level, BlockPos pos, BlockState state) {
        boolean changed = false;

        ItemStack fruitInput = items.get(0);
        ItemStack ashOutput = items.get(1);
        ItemStack chargeSlot = items.get(2);

        // 1. Check if we can digest a new fruit into the biogas reservoir
        boolean hasEnergyRoom = energyStorage.amount < energyStorage.capacity;
        if (burnTime <= 0 && hasEnergyRoom) {
            if (!fruitInput.isEmpty()) {
                FruitBioData data = getFruitBioData(fruitInput);
                if (data != null) {
                    boolean canStoreAsh = data.ashYield() == 0
                            || ashOutput.isEmpty()
                            || (ashOutput.is(ObsidiumMod.VOLCANIC_ASH)
                                && ashOutput.getCount() + data.ashYield() <= ashOutput.getMaxStackSize());
                    if (canStoreAsh) {
                        fruitInput.shrink(1);
                        burnTime = maxBurnTime = data.durationTicks();
                        currentRfPerTick = data.rfPerTick();
                        pendingAsh = data.ashYield();
                        biogasAmount = Math.min(MAX_BIOGAS, biogasAmount + data.biogasYield());
                        changed = true;
                    }
                }
            } else if (biogasAmount >= 100) {
                // Burn stored Biogas from internal reservoir if available!
                biogasAmount -= 100;
                burnTime = maxBurnTime = 100; // 5 seconds of 120 RF/t
                currentRfPerTick = 120;
                changed = true;
            }
        }

        // 2. Generate energy while digesting biomass
        boolean isGenerating = false;
        if (burnTime > 0) {
            burnTime--;
            if (energyStorage.amount < energyStorage.capacity) {
                energyStorage.amount = Math.min(energyStorage.capacity, energyStorage.amount + currentRfPerTick);
            }
            isGenerating = true;
            changed = true;

            // Deposit ash byproduct when digestion finishes
            if (burnTime == 0 && pendingAsh > 0) {
                if (ashOutput.isEmpty()) {
                    items.set(1, new ItemStack(ObsidiumMod.VOLCANIC_ASH, pendingAsh));
                } else if (ashOutput.is(ObsidiumMod.VOLCANIC_ASH)) {
                    ashOutput.grow(pendingAsh);
                }
                pendingAsh = 0;
            }
        } else {
            currentRfPerTick = 0;
        }

        // 3. Update block LIT state
        boolean currentLit = state.getValue(DemetrianBiomassGeneratorBlock.LIT);
        if (currentLit != isGenerating) {
            level.setBlock(pos, state.setValue(DemetrianBiomassGeneratorBlock.LIT, isGenerating), 3);
            changed = true;
        }

        // 4. Charge item in slot 2 if present
        if (!chargeSlot.isEmpty() && energyStorage.amount > 0) {
            if (chargeSlot.getItem() instanceof SimpleEnergyItem energyItem) {
                long stored = energyItem.getStoredEnergy(chargeSlot);
                long capacity = energyItem.getEnergyCapacity(chargeSlot);
                long needed = capacity - stored;
                if (needed > 0) {
                    long toCharge = Math.min(needed, Math.min(energyStorage.amount, 500L));
                    energyItem.setStoredEnergy(chargeSlot, stored + toCharge);
                    energyStorage.amount -= toCharge;
                    changed = true;
                }
            }
        }

        // 5. Automatically push energy to adjacent receivers
        if (energyStorage.amount > 0) {
            for (Direction dir : Direction.values()) {
                EnergyStorage target = EnergyStorage.SIDED.find(level, pos.relative(dir), dir.getOpposite());
                if (target != null && target.supportsInsertion()) {
                    long moved = EnergyStorageUtil.move(energyStorage, target, MAX_EXTRACT_RATE, null);
                    if (moved > 0) {
                        changed = true;
                    }
                }
            }
        }

        if (changed) {
            setChanged();
        }

        VolcaniteStoragePipeBlockEntity.autoEjectMachineOutputs(level, pos, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putInt("BurnTime", this.burnTime);
        tag.putInt("MaxBurnTime", this.maxBurnTime);
        tag.putInt("CurrentRfPerTick", this.currentRfPerTick);
        tag.putInt("PendingAsh", this.pendingAsh);
        tag.putInt("BiogasAmount", this.biogasAmount);
        tag.putLong("EnergyAmount", this.energyStorage.amount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items.clear();
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.burnTime = tag.getInt("BurnTime");
        this.maxBurnTime = tag.getInt("MaxBurnTime");
        this.currentRfPerTick = tag.getInt("CurrentRfPerTick");
        this.pendingAsh = tag.getInt("PendingAsh");
        this.biogasAmount = tag.getInt("BiogasAmount");
        this.energyStorage.amount = tag.getLong("EnergyAmount");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.demetrian_biomass_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new DemetrianBiomassGeneratorMenu(syncId, playerInventory, this, this.dataAccess);
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
        if (slot == 0) return isBiomassFruit(stack);
        if (slot == 2) return stack.getItem() instanceof SimpleEnergyItem;
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return new int[]{ 1 }; // Extract Ash byproduct from bottom
        } else if (side == Direction.UP) {
            return new int[]{ 0 }; // Insert fruits from top
        } else {
            return new int[]{ 0, 1 }; // Sides
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot == 0) return isBiomassFruit(stack);
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 1; // Only extract ash byproduct
    }

    @Override
    public boolean isInputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 0;
    }

    @Override
    public boolean isOutputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 1;
    }
}
