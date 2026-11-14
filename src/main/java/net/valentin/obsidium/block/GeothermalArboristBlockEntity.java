package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.valentin.obsidium.item.PyrogenicBloomInfuserItem;
import net.valentin.obsidium.item.VolcanicPetalItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import net.minecraft.world.WorldlyContainer;

public class GeothermalArboristBlockEntity extends BlockEntity implements MenuProvider, InventoryHolder, WorldlyContainer, MachineInventory {

    public static final int INVENTORY_SIZE = 13;
    // Slots:
    // 0, 1: Seeds / Saplings Input
    // 2, 3: Lava/Fluid Bucket In / Out
    // 4..8: Harvest outputs; 9: one reserved hybrid sapling output
    // 10: Auto-Hoe Upgrade (Thermic Soil Tiller / Hoe)
    // 11: Bloom Infuser Upgrade (Pyrogenic Bloom Infuser)
    // 12: Fertilizer Input (Bone Meal, Volcanic Ash)

    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            GeothermalArboristBlockEntity.this.setChanged();
        }
    };

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(200000, 10000, 0) {
        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    private int lavaAmount = 0;
    public static final int MAX_LAVA = 10000;
    private int radius = 1; // 1 = 3x3, 2 = 5x5, 3 = 7x7, 4 = 9x9 (Default is 3x3 for clear and controlled
                            // testing!)
    private int mode = 0; // 0 = All (Crops & Trees), 1 = Crops Only, 2 = Trees Only
    private static final int MUTATION_PROGRESS_REQUIRED = 100;
    private static final int MUTATION_PROGRESS_PER_FRUIT = 10;
    private static final int MUTATION_ENERGY_COST = 4000;
    private static final int MUTATION_LAVA_COST = 250;
    private int mutationSelection = 0; // 0 = Electrum, 1 = Tartarean Netherite
    private int mutationProgress = 0;

    private int currentScanIndex = 0;
    private int tickTimer = 0;
    private int growthCooldown = 0;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> lavaAmount;
                case 1 -> (int) (energyStorage.amount & 0xFFFF);
                case 2 -> (int) ((energyStorage.amount >> 16) & 0xFFFF);
                case 3 -> radius;
                case 4 -> mode;
                case 5 -> hasTillerUpgrade() ? 1 : 0;
                case 6 -> hasBloomInfuser() ? 1 : 0;
                case 7 -> mutationSelection;
                case 8 -> mutationProgress;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> lavaAmount = value;
                case 1 -> energyStorage.amount = (energyStorage.amount & 0xFFFF0000L) | (value & 0xFFFF);
                case 2 -> energyStorage.amount = (energyStorage.amount & 0xFFFFL) | ((long) value << 16);
                case 3 -> radius = Math.max(1, Math.min(4, value));
                case 4 -> mode = Math.floorMod(value, 4);
                case 7 -> {
                    mutationSelection = Math.floorMod(value, 2);
                    mutationProgress = 0;
                    setChanged();
                }
            }
        }

        @Override
        public int getCount() {
            return 9;
        }
    };

    public boolean hasTillerUpgrade() {
        ItemStack stack = inventory.getItem(10);
        return !stack.isEmpty() && stack.getItem() instanceof HoeItem;
    }

    public boolean hasBloomInfuser() {
        ItemStack stack = inventory.getItem(11);
        return !stack.isEmpty() && stack.getItem() instanceof PyrogenicBloomInfuserItem;
    }

    public static boolean isFertilizer(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        return stack.is(Items.BONE_MEAL) || stack.is(ObsidiumMod.VOLCANIC_ASH) || stack.is(ObsidiumMod.VOLCANITE_DUST);
    }

    public ItemStack getAvailableFertilizer() {
        // Priority 1: Dedicated fertilizer slot (Slot 12)
        ItemStack fert12 = inventory.getItem(12);
        if (!fert12.isEmpty() && isFertilizer(fert12)) {
            return fert12;
        }
        // Priority 2: Check input slots 0 and 1
        for (int i = 0; i <= 1; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && isFertilizer(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public void consumeFertilizer(ServerLevel level, ItemStack fert) {
        // Bloom Infuser catalyst provides a 40% chance to NOT consume fertilizer!
        boolean shouldConsume = !hasBloomInfuser() || level.random.nextFloat() < 0.60f;
        if (shouldConsume) {
            fert.shrink(1);
            for (int i = 0; i < INVENTORY_SIZE; i++) {
                if (inventory.getItem(i).isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
            }
            inventory.setChanged();
        }
    }

    public GeothermalArboristBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.GEOTHERMAL_ARBORIST_BE, pos, state);
    }

    @Override
    public Container getInventory() {
        return this.inventory;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GeothermalArboristBlockEntity entity) {
        if (level.isClientSide())
            return;

        entity.tickTimer++;
        if (entity.growthCooldown > 0) {
            entity.growthCooldown--;
        }

        // 1. Process Lava Bucket Input
        entity.processLavaInput();

        // 2. Auto-export stored harvest outputs (slots 4..8) to adjacent storage
        // Unit or Container
        if (entity.tickTimer % 10 == 0) {
            entity.exportOutputsToAdjacentStorage();
        }

        // 3. Perform agricultural cycle every 7 ticks (balanced pacing, -25% harvester rate)
        if (entity.tickTimer % 7 == 0) {
            boolean didWork = entity.performAgriculturalCycle((ServerLevel) level, pos, state);

            boolean wasWorking = state.getValue(GeothermalArboristBlock.WORKING);
            if (wasWorking != didWork) {
                level.setBlock(pos, state.setValue(GeothermalArboristBlock.WORKING, didWork), 3);
            }
        }
    }

    public int getLavaAmount() {
        return lavaAmount;
    }

    public int getMaxLava() {
        return MAX_LAVA;
    }

    public int getRemainingFluidCapacity() {
        return Math.max(0, MAX_LAVA - lavaAmount);
    }

    public int insertFluid(int amount) {
        int toInsert = Math.min(amount, getRemainingFluidCapacity());
        if (toInsert > 0) {
            lavaAmount += toInsert;
            setChanged();
        }
        return toInsert;
    }

    public int insertFertilizer(ItemStack offered) {
        if (offered.isEmpty() || !isFertilizer(offered)) return 0;
        ItemStack slot = inventory.getItem(12);
        if (slot.isEmpty()) {
            int inserted = Math.min(offered.getCount(), offered.getMaxStackSize());
            inventory.setItem(12, offered.copyWithCount(inserted));
            return inserted;
        }
        if (!ItemStack.isSameItemSameComponents(slot, offered)) return 0;
        int inserted = Math.min(offered.getCount(), slot.getMaxStackSize() - slot.getCount());
        if (inserted > 0) {
            slot.grow(inserted);
            inventory.setChanged();
        }
        return inserted;
    }

    private void processLavaInput() {
        ItemStack inputStack = inventory.getItem(2);
        if (inputStack.isEmpty())
            return;

        if (inputStack.is(Items.LAVA_BUCKET) && lavaAmount <= MAX_LAVA - 1000) {
            ItemStack outputStack = inventory.getItem(3);
            if (outputStack.isEmpty()) {
                inputStack.shrink(1);
                inventory.setItem(3, new ItemStack(Items.BUCKET));
                lavaAmount += 1000;
                setChanged();
            } else if (outputStack.is(Items.BUCKET) && outputStack.getCount() < outputStack.getMaxStackSize()) {
                inputStack.shrink(1);
                outputStack.grow(1);
                lavaAmount += 1000;
                setChanged();
            }
        } else if ((inputStack.is(Items.WATER_BUCKET) || inputStack.is(ObsidiumMod.STEAM_BUCKET)) && lavaAmount <= MAX_LAVA - 1000) {
            ItemStack outputStack = inventory.getItem(3);
            if (outputStack.isEmpty()) {
                inputStack.shrink(1);
                inventory.setItem(3, new ItemStack(Items.BUCKET));
                lavaAmount += 1000;
                setChanged();
            } else if (outputStack.is(Items.BUCKET) && outputStack.getCount() < outputStack.getMaxStackSize()) {
                inputStack.shrink(1);
                outputStack.grow(1);
                lavaAmount += 1000;
                setChanged();
            }
        } else if (inputStack.getItem() instanceof net.valentin.obsidium.item.VolcaniteBottleItem) {
            int bottleLava = net.valentin.obsidium.item.VolcaniteBottleItem.getLava(inputStack);
            if (bottleLava > 0 && lavaAmount < MAX_LAVA) {
                int toTransfer = Math.min(bottleLava, Math.min(1000, MAX_LAVA - lavaAmount));
                if (toTransfer > 0) {
                    lavaAmount += toTransfer;
                    net.valentin.obsidium.item.VolcaniteBottleItem.setLava(inputStack, bottleLava - toTransfer);
                    setChanged();
                }
            }
        } else if (inputStack.is(Items.POTION) && lavaAmount <= MAX_LAVA - 250) {
            PotionContents contents = inputStack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
            if (contents != null && contents.is(Potions.WATER)) {
                ItemStack outputStack = inventory.getItem(3);
                if (outputStack.isEmpty()) {
                    inputStack.shrink(1);
                    inventory.setItem(3, new ItemStack(Items.GLASS_BOTTLE));
                    lavaAmount += 250;
                    setChanged();
                } else if (outputStack.is(Items.GLASS_BOTTLE)
                        && outputStack.getCount() < outputStack.getMaxStackSize()) {
                    inputStack.shrink(1);
                    outputStack.grow(1);
                    lavaAmount += 250;
                    setChanged();
                }
            }
        }
    }

    public static boolean isVolcanicSoil(BlockState state) {
        return state.is(Blocks.NETHERRACK) || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL)
                || state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN);
    }

    public static boolean isValidArboristSoil(BlockState state) {
        return state.is(Blocks.FARMLAND) || state.is(BlockTags.DIRT) || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)
                || isVolcanicSoil(state);
    }

    public static BlockState getCropStateForSeed(ItemStack stack) {
        if (stack.isEmpty())
            return null;
        Item item = stack.getItem();
        if (item instanceof VolcanicPetalItem petalItem) {
            return petalItem.getCropBlock().defaultBlockState();
        }
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof CropBlock crop) {
                return crop.defaultBlockState();
            }
        }
        if (item == ObsidiumMod.VOLCANITE_PETAL) return ObsidiumMod.VOLCANITE_CROP.defaultBlockState();
        if (item == ObsidiumMod.IRON_PETAL) return ObsidiumMod.IRON_CROP.defaultBlockState();
        if (item == ObsidiumMod.GOLD_PETAL) return ObsidiumMod.GOLD_CROP.defaultBlockState();
        if (item == ObsidiumMod.COPPER_PETAL) return ObsidiumMod.COPPER_CROP.defaultBlockState();
        if (item == ObsidiumMod.COAL_PETAL) return ObsidiumMod.COAL_CROP.defaultBlockState();
        if (item == ObsidiumMod.LAPIS_PETAL) return ObsidiumMod.LAPIS_CROP.defaultBlockState();
        if (item == ObsidiumMod.REDSTONE_PETAL) return ObsidiumMod.REDSTONE_CROP.defaultBlockState();
        if (item == ObsidiumMod.DIAMOND_PETAL) return ObsidiumMod.DIAMOND_CROP.defaultBlockState();
        if (item == ObsidiumMod.NETHERITE_PETAL) return ObsidiumMod.NETHERITE_CROP.defaultBlockState();
        if (item == ObsidiumMod.TIN_PETAL) return ObsidiumMod.TIN_CROP.defaultBlockState();
        if (item == ObsidiumMod.LEAD_PETAL) return ObsidiumMod.LEAD_CROP.defaultBlockState();
        if (item == ObsidiumMod.SILVER_PETAL) return ObsidiumMod.SILVER_CROP.defaultBlockState();

        if (item == Items.WHEAT_SEEDS)
            return Blocks.WHEAT.defaultBlockState();
        if (item == Items.BEETROOT_SEEDS)
            return Blocks.BEETROOTS.defaultBlockState();
        if (item == Items.CARROT)
            return Blocks.CARROTS.defaultBlockState();
        if (item == Items.POTATO)
            return Blocks.POTATOES.defaultBlockState();
        if (item == Items.MELON_SEEDS)
            return Blocks.MELON_STEM.defaultBlockState();
        if (item == Items.PUMPKIN_SEEDS)
            return Blocks.PUMPKIN_STEM.defaultBlockState();
        if (item == Items.TORCHFLOWER_SEEDS)
            return Blocks.TORCHFLOWER_CROP.defaultBlockState();
        if (item == Items.PITCHER_POD)
            return Blocks.PITCHER_CROP.defaultBlockState();
        return null;
    }

    private boolean performAgriculturalCycle(ServerLevel level, BlockPos machinePos, BlockState state) {
        if (energyStorage.amount < 30 || !hasHarvestOutputSpace())
            return false;

        Direction facing = state.getValue(GeothermalArboristBlock.FACING);
        Direction right = facing.getClockWise();
        List<BlockPos> spots = new java.util.ArrayList<>();

        // Working field directly in front of the machine:
        // Width: radius * 2 + 1 (left to right)
        // Depth: radius * 2 + 1 (forward away from machine front)
        // For default radius = 1, this is EXACTLY 3x3 (9 blocks) in front of the
        // machine!
        int depth = radius * 2 + 1;
        for (int forward = 1; forward <= depth; forward++) {
            for (int side = -radius; side <= radius; side++) {
                BlockPos p = machinePos.relative(facing, forward).relative(right, side);
                spots.add(p);
            }
        }

        if (spots.isEmpty())
            return false;

        boolean didAction = false;
        int actionsDone = 0;
        int maxActionsPerTick = 1; // 1 action per work pulse keeps the farm calm, orderly, and easy to observe

        for (int i = 0; i < spots.size() && actionsDone < maxActionsPerTick; i++) {
            int spotIdx = (currentScanIndex + i) % spots.size();
            BlockPos columnPos = spots.get(spotIdx);

            if (processFieldColumn(level, columnPos, machinePos)) {
                didAction = true;
                actionsDone++;
            }
        }

        currentScanIndex = (currentScanIndex + actionsDone + 1) % spots.size();
        return didAction;
    }

    private boolean processFieldColumn(ServerLevel level, BlockPos columnPos, BlockPos machinePos) {
        if (mode == 3) {
            return harvestNearbyLeaves(level, columnPos, machinePos);
        }

        // Check if any block in this column is a Demetrian Bonsai Planter (Modes 0 and 2)
        if (mode == 0 || mode == 2) {
            for (int y = machinePos.getY() + 2; y >= machinePos.getY() - 2; y--) {
                BlockPos p = new BlockPos(columnPos.getX(), y, columnPos.getZ());
                if (level.getBlockEntity(p) instanceof DemetrianBonsaiPlanterBlockEntity bonsai) {
                    if (processBonsaiPlanter(level, p, bonsai, machinePos)) {
                        return true;
                    }
                }
            }
        }

        // Find ground/soil level in this column: scan Y from machinePos.getY() + 1 down
        // to machinePos.getY() - 2
        BlockPos soilPos = null;
        for (int y = machinePos.getY() + 1; y >= machinePos.getY() - 2; y--) {
            BlockPos p = new BlockPos(columnPos.getX(), y, columnPos.getZ());
            BlockState s = level.getBlockState(p);
            if (isValidArboristSoil(s)) {
                soilPos = p;
                break;
            }
        }

        if (soilPos == null)
            return false;

        BlockPos plantPos = soilPos.above();
        BlockState plantState = level.getBlockState(plantPos);
        BlockState soilState = level.getBlockState(soilPos);

        // A. CROP / FLOWER HANDLING (Modes 0 or 1)
        if (mode == 0 || mode == 1) {
            // 1. Mature Crop Harvesting
            if (plantState.getBlock() instanceof CropBlock crop && crop.isMaxAge(plantState)) {
                if (energyStorage.amount >= 250) {
                    List<ItemStack> cropDrops = new java.util.ArrayList<>();
                    if (crop instanceof VolcanicOreCropBlock oreCrop) {
                        int rawCount = oreCrop.getMinDrop()
                                + level.random.nextInt(oreCrop.getMaxDrop() - oreCrop.getMinDrop() + 1);
                        // 25% yield nerf for automated mechanical harvester
                        int count = Math.max(1, (int) Math.round(rawCount * 0.75));
                        cropDrops.add(new ItemStack(oreCrop.getDropItem(), count));
                    } else {
                        cropDrops.addAll(Block.getDrops(plantState, level, plantPos, null));
                    }
                    if (!canFitInHarvestOutputs(cropDrops)) return false;

                    energyStorage.amount -= 250;
                    if (lavaAmount >= 5)
                        lavaAmount -= 5;

                    for (ItemStack drop : cropDrops) depositOutput(drop);

                    level.playSound(null, plantPos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.sendParticles(ParticleTypes.CLOUD, plantPos.getX() + 0.5, plantPos.getY() + 0.2,
                            plantPos.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.02);

                    // Reset crop to age 0 directly!
                    // The flower stays permanently planted in the soil, never spills seeds, and
                    // never consumes input seeds to replant!
                    level.setBlock(plantPos, crop.getStateForAge(0), Block.UPDATE_ALL);

                    setChanged();
                    return true;
                }
            }

            // 2. Auto-Hoe / Tilling on untilled dirt/grass (NEVER till Netherrack or volcanic soils!)
            if (plantState.isAir() && !soilState.is(Blocks.FARMLAND) && !isVolcanicSoil(soilState)) {
                boolean hasSeeds = hasSeedInInputSlots();
                // Till if we have tiller upgrade OR if we have seeds in input slots ready to be
                // planted
                if (hasTillerUpgrade() || hasSeeds) {
                    int energyCost = hasTillerUpgrade() ? 80 : 150;
                    int fluidCost = hasTillerUpgrade() ? 0 : 2;
                    if (energyStorage.amount >= energyCost && lavaAmount >= fluidCost) {
                        energyStorage.amount -= energyCost;
                        lavaAmount -= fluidCost;
                        level.setBlock(soilPos, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7),
                                Block.UPDATE_ALL);
                        level.playSound(null, soilPos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                        level.sendParticles(ParticleTypes.LAVA, soilPos.getX() + 0.5, soilPos.getY() + 1.0,
                                soilPos.getZ() + 0.5, 3, 0.2, 0.1, 0.2, 0.02);

                        if (hasTillerUpgrade()) {
                            ItemStack hoe = inventory.getItem(10);
                            if (hoe.isDamageableItem()) {
                                hoe.hurtAndBreak(1, level, null, item -> inventory.setItem(10, ItemStack.EMPTY));
                            }
                        }
                        setChanged();
                        return true;
                    }
                }
            }

            // 3. Planting Seeds / Flowers on Farmland OR Volcanic Soils (Netherrack, Magma, etc.)
            if (plantState.isAir() && (soilState.is(Blocks.FARMLAND) || isVolcanicSoil(soilState))) {
                if (energyStorage.amount >= 120 && tryPlantSeed(level, plantPos)) {
                    energyStorage.amount -= 120;
                    setChanged();
                    return true;
                }
            }

            // 4. Growth: Fertilizer & Geothermal Heat (Steady, paced, visible growth)
            if (plantState.getBlock() instanceof CropBlock crop && !crop.isMaxAge(plantState)
                    && growthCooldown <= 0) {
                ItemStack fert = getAvailableFertilizer();
                boolean usedFertilizer = !fert.isEmpty();
                boolean passiveHeat = !usedFertilizer && lavaAmount >= 1;
                int growthCost = usedFertilizer ? 250 : 60;
                if ((usedFertilizer || passiveHeat) && energyStorage.amount >= growthCost) {
                    if (usedFertilizer) {
                        consumeFertilizer(level, fert);
                    } else {
                        lavaAmount--;
                    }
                    energyStorage.amount -= growthCost;

                    int currentAge = crop.getAge(plantState);
                    int maxAge = crop.getMaxAge();
                    // Fertilizer (Bone Meal etc.): when Bloom Infuser upgrade is installed, advance 2-3 stages and grow significantly faster!
                    int stages;
                    if (usedFertilizer) {
                        if (hasBloomInfuser()) {
                            stages = 2 + (level.random.nextFloat() < 0.45f ? 1 : 0); // 2-3 stages per trigger
                        } else {
                            stages = (level.random.nextFloat() < 0.40f) ? 2 : 1;
                        }
                    } else {
                        stages = 1;
                    }
                    level.setBlock(plantPos, crop.getStateForAge(Math.min(maxAge, currentAge + stages)), Block.UPDATE_ALL);
                    level.sendParticles(usedFertilizer ? ParticleTypes.COMPOSTER : ParticleTypes.HAPPY_VILLAGER,
                            plantPos.getX() + 0.5, plantPos.getY() + 0.4, plantPos.getZ() + 0.5,
                            usedFertilizer ? 6 : 2, 0.2, 0.2, 0.2, 0.03);
                    // Fertilized growth cooldown: with Bloom Infuser upgrade it is 14 ticks; standard fertilized is 30 ticks; passive geothermal heat is 1500
                    growthCooldown = usedFertilizer ? (hasBloomInfuser() ? 14 : 30) : 1500;
                    setChanged();
                    return true;
                }
            }
        }

        // B. TREE / ARBORIST HANDLING (Modes 0 or 2)
        if (mode == 0 || mode == 2) {
            // Passive tree hybridization progress if compatible species coexist
            if (hasNearbyDifferentSpecies(level, machinePos)) {
                advanceTreeMutationPassive(level, machinePos);
            }

            if (energyStorage.amount >= 150 && harvestNearbyFruit(level, columnPos, machinePos)) {
                energyStorage.amount -= 150;
                setChanged();
                return true;
            }

            // 1. Fell Grown Tree (Preserve trees if they are active hybridization parents!)
            if (plantState.is(BlockTags.LOGS) || plantState.getBlock() == ObsidiumMod.TARTAREAN_NETHERITE_LOG) {
                boolean isApexTree = plantState.is(ObsidiumMod.TARTAREAN_NETHERITE_LOG) || plantState.is(ObsidiumMod.ELECTRUM_LOG);
                boolean inHybridization = !isApexTree && hasNearbyDifferentSpecies(level, machinePos);
                if (!inHybridization && energyStorage.amount >= 1000 && fellTree(level, plantPos)) {
                    energyStorage.amount -= 1000;
                    if (lavaAmount >= 20)
                        lavaAmount -= 20;
                    setChanged();
                    return true;
                }
            }

            // 2. Plant supported saplings on dirt, grass, or volcanic soil (input slots 0 and 1)
            if (plantState.isAir() && isValidArboristSoil(soilState)) {
                if (energyStorage.amount >= 150 && tryPlantSapling(level, plantPos)) {
                    energyStorage.amount -= 150;
                    setChanged();
                    return true;
                }
            }

            // 3. Sapling Growth Acceleration: Bone Meal / Fertilizer & Geothermal Warmth
            boolean isSapling = plantState.is(BlockTags.SAPLINGS) || plantState.getBlock() instanceof SaplingBlock;
            if (isSapling && growthCooldown <= 0) {
                ItemStack fert = getAvailableFertilizer();
                boolean usedFertilizer = !fert.isEmpty();
                boolean passiveHeat = !usedFertilizer && lavaAmount >= 1;
                int growthCost = usedFertilizer ? 250 : 60;
                if ((usedFertilizer || passiveHeat) && energyStorage.amount >= growthCost) {
                    if (usedFertilizer) consumeFertilizer(level, fert);
                    else lavaAmount--;
                    energyStorage.amount -= growthCost;

                    if (plantState.getBlock() instanceof BonemealableBlock bonemealable) {
                        if (bonemealable.isValidBonemealTarget(level, plantPos, plantState)) {
                            bonemealable.performBonemeal(level, level.random, plantPos, plantState);
                        }
                    } else {
                        plantState.randomTick(level, plantPos, level.random);
                    }
                    level.sendParticles(usedFertilizer ? ParticleTypes.COMPOSTER : ParticleTypes.HAPPY_VILLAGER,
                            plantPos.getX() + 0.5, plantPos.getY() + 0.4, plantPos.getZ() + 0.5,
                            usedFertilizer ? 6 : 2, 0.2, 0.2, 0.2, 0.03);
                    growthCooldown = usedFertilizer ? (hasBloomInfuser() ? 25 : 45) : 1500;
                    setChanged();
                    return true;
                }
            }
        }

        return false;
    }

    private boolean harvestNearbyFruit(ServerLevel level, BlockPos columnPos, BlockPos machinePos) {
        BlockPos ripePos = null;
        MineralTreeLeavesBlock ripeLeaves = null;
        for (int dy = -1; dy <= 10 && ripePos == null; dy++) {
            for (int dx = -2; dx <= 2 && ripePos == null; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos check = columnPos.offset(dx, dy, dz);
                    BlockState candidate = level.getBlockState(check);
                    if (candidate.getBlock() instanceof MineralTreeLeavesBlock leaves
                            && candidate.getValue(MineralTreeLeavesBlock.FRUITING)) {
                        ripePos = check;
                        ripeLeaves = leaves;
                        break;
                    }
                }
            }
        }
        if (ripePos == null || ripeLeaves == null || ripeLeaves.fruitItem() == null)
            return false;

        // 25% chance of fruit remaining unplucked / still maturing this cycle
        if (level.random.nextFloat() < 0.25f) {
            return false;
        }

        ItemStack fruit = new ItemStack(ripeLeaves.fruitItem());
        if (!canFitInHarvestOutputs(List.of(fruit))) return false;
        depositOutput(fruit);
        if (!fruit.isEmpty())
            return false;

        BlockState ripeState = level.getBlockState(ripePos);
        if (ripeState.getBlock() == ripeLeaves && ripeState.getValue(MineralTreeLeavesBlock.FRUITING)) {
            level.setBlock(ripePos, ripeState.setValue(MineralTreeLeavesBlock.FRUITING, false), Block.UPDATE_ALL);
        }

        advanceTreeMutation(level, machinePos);
        return true;
    }

    private boolean harvestNearbyLeaves(ServerLevel level, BlockPos columnPos, BlockPos machinePos) {
        for (int y = machinePos.getY() + 1; y <= machinePos.getY() + 12; y++) {
            BlockPos leafPos = new BlockPos(columnPos.getX(), y, columnPos.getZ());
            BlockState leaves = level.getBlockState(leafPos);
            if (!leaves.is(BlockTags.LEAVES) || energyStorage.amount < 80) continue;

            List<ItemStack> drops = Block.getDrops(leaves, level, leafPos, level.getBlockEntity(leafPos), null,
                    new ItemStack(Items.SHEARS));
            if (!canFitInHarvestOutputs(drops)) return false;
            level.destroyBlock(leafPos, false);
            for (ItemStack drop : drops) depositOutput(drop);
            energyStorage.amount -= 80;
            level.playSound(null, leafPos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.8F, 1.2F);
            level.sendParticles(ParticleTypes.COMPOSTER, leafPos.getX() + 0.5, leafPos.getY() + 0.5,
                    leafPos.getZ() + 0.5, 4, 0.25, 0.25, 0.25, 0.02);
            setChanged();
            return true;
        }
        return false;
    }

    private void advanceTreeMutation(ServerLevel level, BlockPos machinePos) {
        if (!inventory.getItem(9).isEmpty()) return;

        Set<String> species = getNearbyLeavesSpecies(level, machinePos);
        boolean eligible = mutationSelection == 0
                ? (species.contains("midas_gold") && (species.contains("hermes_copper") || species.contains("artemis_silver")))
                    || (species.contains("hermes_copper") && species.contains("zeus_tin"))
                : species.contains("hephaestus_volcanite")
                    && (species.contains("ares_iron") || species.contains("cronus_lead") || species.contains("midas_gold"));
        if (!eligible) return;

        mutationProgress = Math.min(MUTATION_PROGRESS_REQUIRED,
                mutationProgress + MUTATION_PROGRESS_PER_FRUIT);
        if (mutationProgress < MUTATION_PROGRESS_REQUIRED
                || energyStorage.amount < MUTATION_ENERGY_COST
                || lavaAmount < MUTATION_LAVA_COST) {
            setChanged();
            return;
        }

        energyStorage.amount -= MUTATION_ENERGY_COST;
        lavaAmount -= MUTATION_LAVA_COST;
        if (mutationSelection == 0) {
            inventory.setItem(9, new ItemStack(ObsidiumMod.ELECTRUM_SAPLING_ITEM));
            level.playSound(null, machinePos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 1.35F);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, machinePos.getX() + 0.5, machinePos.getY() + 1.2,
                    machinePos.getZ() + 0.5, 8, 0.35, 0.25, 0.35, 0.03);
        } else {
            boolean isRareTitan = level.random.nextFloat() < 0.05F;
            if (isRareTitan) {
                inventory.setItem(9, new ItemStack(ObsidiumMod.PYRETIC_TITAN_SAPLING_ITEM));
                level.playSound(null, machinePos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.sendParticles(ParticleTypes.FLAME, machinePos.getX() + 0.5, machinePos.getY() + 1.2,
                        machinePos.getZ() + 0.5, 25, 0.35, 0.35, 0.35, 0.08);
            } else {
                inventory.setItem(9, new ItemStack(ObsidiumMod.TARTAREAN_NETHERITE_SAPLING_ITEM));
                level.playSound(null, machinePos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.9F, 1.1F);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, machinePos.getX() + 0.5, machinePos.getY() + 1.2,
                        machinePos.getZ() + 0.5, 12, 0.35, 0.25, 0.35, 0.05);
            }
        }
        mutationProgress = 0;
        inventory.setChanged();
        setChanged();
    }

    private void advanceTreeMutationPassive(ServerLevel level, BlockPos machinePos) {
        if (!inventory.getItem(9).isEmpty()) return;

        Set<String> species = getNearbyLeavesSpecies(level, machinePos);
        boolean eligible = mutationSelection == 0
                ? (species.contains("midas_gold") && (species.contains("hermes_copper") || species.contains("artemis_silver")))
                    || (species.contains("hermes_copper") && species.contains("zeus_tin"))
                : species.contains("hephaestus_volcanite")
                    && (species.contains("ares_iron") || species.contains("cronus_lead") || species.contains("midas_gold"));
        if (!eligible) return;

        // Steady passive advance: +2 per cycle when trees are coexisting
        mutationProgress = Math.min(MUTATION_PROGRESS_REQUIRED, mutationProgress + 2);
        if (mutationProgress < MUTATION_PROGRESS_REQUIRED
                || energyStorage.amount < MUTATION_ENERGY_COST
                || lavaAmount < MUTATION_LAVA_COST) {
            setChanged();
            return;
        }

        energyStorage.amount -= MUTATION_ENERGY_COST;
        lavaAmount -= MUTATION_LAVA_COST;
        if (mutationSelection == 0) {
            inventory.setItem(9, new ItemStack(ObsidiumMod.ELECTRUM_SAPLING_ITEM));
            level.playSound(null, machinePos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 1.35F);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, machinePos.getX() + 0.5, machinePos.getY() + 1.2,
                    machinePos.getZ() + 0.5, 8, 0.35, 0.25, 0.35, 0.03);
        } else {
            boolean isRareTitan = level.random.nextFloat() < 0.05F;
            if (isRareTitan) {
                inventory.setItem(9, new ItemStack(ObsidiumMod.PYRETIC_TITAN_SAPLING_ITEM));
                level.playSound(null, machinePos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.sendParticles(ParticleTypes.FLAME, machinePos.getX() + 0.5, machinePos.getY() + 1.2,
                        machinePos.getZ() + 0.5, 25, 0.35, 0.35, 0.35, 0.08);
            } else {
                inventory.setItem(9, new ItemStack(ObsidiumMod.TARTAREAN_NETHERITE_SAPLING_ITEM));
                level.playSound(null, machinePos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.9F, 1.1F);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, machinePos.getX() + 0.5, machinePos.getY() + 1.2,
                        machinePos.getZ() + 0.5, 12, 0.35, 0.25, 0.35, 0.05);
            }
        }
        mutationProgress = 0;
        inventory.setChanged();
        setChanged();
    }

    private Set<String> getNearbyLeavesSpecies(ServerLevel level, BlockPos machinePos) {
        Set<String> species = new HashSet<>();
        int foliageRadius = radius + 2;
        for (BlockPos pos : BlockPos.betweenClosed(machinePos.offset(-foliageRadius, -4, -foliageRadius),
                machinePos.offset(foliageRadius, 12, foliageRadius))) {
            if (level.getBlockState(pos).getBlock() instanceof MineralTreeLeavesBlock leaves) {
                species.add(leaves.species());
            }
        }
        return species;
    }

    private boolean hasNearbyDifferentSpecies(ServerLevel level, BlockPos machinePos) {
        Set<String> species = getNearbyLeavesSpecies(level, machinePos);
        return (species.contains("midas_gold") && (species.contains("hermes_copper") || species.contains("artemis_silver")))
                || (species.contains("hermes_copper") && species.contains("zeus_tin"))
                || (species.contains("hephaestus_volcanite") && (species.contains("ares_iron") || species.contains("cronus_lead") || species.contains("midas_gold")));
    }

    private boolean hasSeedInInputSlots() {
        for (int i = 0; i <= 1; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && getCropStateForSeed(stack) != null) {
                return true;
            }
        }
        return false;
    }

    private boolean tryPlantSeed(Level level, BlockPos plantPos) {
        // EXCLUSIVELY check input slots 0 and 1! Never take from output slots 4..9!
        for (int i = 0; i <= 1; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty())
                continue;

            BlockState cropState = getCropStateForSeed(stack);
            if (cropState != null) {
                level.setBlock(plantPos, cropState, Block.UPDATE_ALL);
                stack.shrink(1);
                if (stack.isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
                inventory.setChanged();
                setChanged();
                level.playSound(null, plantPos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                return true;
            }
        }
        return false;
    }

    private boolean tryPlantSapling(Level level, BlockPos plantPos) {
        // EXCLUSIVELY check input slots 0 and 1!
        for (int i = 0; i <= 1; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && (stack.is(ItemTags.SAPLINGS) || DemetrianBonsaiPlanterBlockEntity.isSaplingItem(stack))) {
                if (stack.getItem() instanceof BlockItem blockItem) {
                    BlockState saplingState = blockItem.getBlock().defaultBlockState();
                    if (!saplingState.canSurvive(level, plantPos)) {
                        continue;
                    }
                    level.setBlock(plantPos, saplingState, Block.UPDATE_ALL);
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        inventory.setItem(i, ItemStack.EMPTY);
                    }
                    inventory.setChanged();
                    setChanged();
                    level.playSound(null, plantPos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean processBonsaiPlanter(ServerLevel level, BlockPos planterPos, DemetrianBonsaiPlanterBlockEntity bonsai, BlockPos machinePos) {
        // 1. Mature Bonsai Harvesting by Arborist (Auto-prunes mature miniature trees without destroying them)
        if (bonsai.isMature()) {
            List<ItemStack> drops = bonsai.generateBonsaiDrops(bonsai.getSapling().getItem());
            if (canFitInHarvestOutputs(drops)) {
                if (energyStorage.amount >= 250) energyStorage.amount -= 250;
                for (ItemStack drop : drops) {
                    depositOutput(drop);
                }
                bonsai.resetGrowth(); // Tree returns to sprout stage 0 and grows anew!
                level.playSound(null, planterPos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.8F, 1.2F);
                level.sendParticles(ParticleTypes.COMPOSTER, planterPos.getX() + 0.5, planterPos.getY() + 0.6, planterPos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.02);
                bonsai.setChanged();
                setChanged();
                return true;
            }
        }

        // 2. Collect Harvested Drops from Bonsai Output Slots (if any remained)
        if (bonsai.hasOutputs()) {
            boolean anyCollected = false;
            for (int i = DemetrianBonsaiPlanterBlockEntity.OUTPUT_START; i < DemetrianBonsaiPlanterBlockEntity.OUTPUT_START + DemetrianBonsaiPlanterBlockEntity.OUTPUT_COUNT; i++) {
                ItemStack drop = bonsai.getItem(i);
                if (!drop.isEmpty()) {
                    List<ItemStack> toCheck = List.of(drop);
                    if (canFitInHarvestOutputs(toCheck)) {
                        ItemStack dropCopy = drop.copy();
                        // Clear slot before depositing so adjacent exports can never mirror/wipe the same slot
                        bonsai.setItem(i, ItemStack.EMPTY);
                        depositOutput(dropCopy);
                        anyCollected = true;
                    }
                }
            }
            if (anyCollected) {
                if (energyStorage.amount >= 100) energyStorage.amount -= 100;
                level.playSound(null, planterPos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.2F);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, planterPos.getX() + 0.5, planterPos.getY() + 0.5, planterPos.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.02);
                bonsai.setChanged();
                bonsai.syncClient();
                setChanged();
                return true;
            }
        }

        // 2b. Insert explicit Soil if missing (only if a sapling isn't already handled below, or preemptively)
        if (!bonsai.hasSoil()) {
            for (int slot = 0; slot <= 1; slot++) {
                ItemStack candidate = inventory.getItem(slot);
                if (!candidate.isEmpty() && DemetrianBonsaiPlanterBlockEntity.isSoilItem(candidate)) {
                    ItemStack soil = candidate.split(1);
                    bonsai.setItem(DemetrianBonsaiPlanterBlockEntity.SLOT_SOIL, soil);
                    if (candidate.isEmpty()) inventory.setItem(slot, ItemStack.EMPTY);
                    level.playSound(null, planterPos, SoundEvents.GRAVEL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                    bonsai.setChanged();
                    bonsai.syncClient();
                    inventory.setChanged();
                    setChanged();
                    return true;
                }
            }
        }

        // 3. Plant Sapling if missing (auto-default soil to dirt if pot has no soil yet)
        if (!bonsai.hasSapling()) {
            for (int slot = 0; slot <= 1; slot++) {
                ItemStack candidate = inventory.getItem(slot);
                if (!candidate.isEmpty() && DemetrianBonsaiPlanterBlockEntity.isSaplingItem(candidate)) {
                    if (!bonsai.hasSoil()) {
                        ItemStack soil = ItemStack.EMPTY;
                        for (int soilSlot = 0; soilSlot <= 1; soilSlot++) {
                            ItemStack soilCandidate = inventory.getItem(soilSlot);
                            if (!soilCandidate.isEmpty() && DemetrianBonsaiPlanterBlockEntity.isSoilItem(soilCandidate)) {
                                soil = soilCandidate.split(1);
                                if (soilCandidate.isEmpty()) inventory.setItem(soilSlot, ItemStack.EMPTY);
                                break;
                            }
                        }
                        if (soil.isEmpty()) {
                            soil = new ItemStack(Items.DIRT);
                        }
                        bonsai.setItem(DemetrianBonsaiPlanterBlockEntity.SLOT_SOIL, soil);
                    }
                    ItemStack sapling = candidate.split(1);
                    bonsai.setItem(DemetrianBonsaiPlanterBlockEntity.SLOT_SAPLING, sapling);
                    bonsai.resetGrowth();
                    if (candidate.isEmpty()) inventory.setItem(slot, ItemStack.EMPTY);
                    level.playSound(null, planterPos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, planterPos.getX() + 0.5, planterPos.getY() + 0.5, planterPos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.05);
                    bonsai.setChanged();
                    bonsai.syncClient();
                    inventory.setChanged();
                    setChanged();
                    return true;
                }
            }
        }

        // 4. Accelerate Growth: Fertilizer or Geothermal Warmth
        if (bonsai.hasSapling() && growthCooldown <= 0) {
            ItemStack fert = getAvailableFertilizer();
            boolean usedFertilizer = !fert.isEmpty();
            boolean passiveHeat = !usedFertilizer && lavaAmount >= 1;
            int growthCost = usedFertilizer ? 250 : 80;

            if ((usedFertilizer || passiveHeat) && energyStorage.amount >= growthCost) {
                int advanceTicks;
                if (usedFertilizer) {
                    consumeFertilizer(level, fert);
                    advanceTicks = hasBloomInfuser() ? 160 : 100;
                } else {
                    lavaAmount--;
                    advanceTicks = 30;
                }
                energyStorage.amount -= growthCost;
                bonsai.advanceGrowth(advanceTicks);

                level.sendParticles(usedFertilizer ? ParticleTypes.COMPOSTER : ParticleTypes.FLAME,
                        planterPos.getX() + 0.5, planterPos.getY() + 0.4, planterPos.getZ() + 0.5,
                        usedFertilizer ? 5 : 2, 0.2, 0.2, 0.2, 0.02);

                growthCooldown = usedFertilizer ? (hasBloomInfuser() ? 40 : 60) : 120;
                setChanged();
                return true;
            }
        }

        return false;
    }

    private boolean fellTree(ServerLevel level, BlockPos startLog) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(startLog);
        visited.add(startLog);

        int maxTreeBlocks = 512;
        BlockPos lowest = startLog;

        while (!queue.isEmpty() && visited.size() <= maxTreeBlocks) {
            BlockPos current = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0)
                            continue;
                        BlockPos n = current.offset(dx, dy, dz);
                        if (!visited.contains(n)) {
                            BlockState bs = level.getBlockState(n);
                            boolean isLog = bs.is(BlockTags.LOGS) || bs.getBlock() == ObsidiumMod.TARTAREAN_NETHERITE_LOG;
                            boolean isLeaf = bs.is(BlockTags.LEAVES) || bs.getBlock() == ObsidiumMod.TARTAREAN_NETHERITE_LEAVES;
                            if (isLog || isLeaf) {
                                visited.add(n);
                                queue.add(n);
                                if (isLog && n.getY() < lowest.getY()) {
                                    lowest = n;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!hasHarvestOutputSpace() && !hasAdjacentStorageSpace()) return false;

        List<ItemStack> treeDrops = new java.util.ArrayList<>();
        for (BlockPos bPos : visited) {
            BlockState bs = level.getBlockState(bPos);
            treeDrops.addAll(Block.getDrops(bs, level, bPos, level.getBlockEntity(bPos)));
            if (bs.getBlock() instanceof MineralTreeLeavesBlock leaves && bs.getValue(MineralTreeLeavesBlock.FRUITING)) {
                net.minecraft.world.item.Item fItem = leaves.fruitItem();
                if (fItem != null) {
                    treeDrops.add(new ItemStack(fItem));
                }
            }
        }

        // Break tree blocks & deposit drops
        for (BlockPos bPos : visited) {
            level.destroyBlock(bPos, false);
        }
        for (ItemStack drop : treeDrops) {
            depositOutput(drop);
            if (!drop.isEmpty()) {
                Block.popResource(level, lowest, drop);
            }
        }

        level.playSound(null, startLog, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.2F, 0.9F);
        level.sendParticles(ParticleTypes.LAVA, startLog.getX() + 0.5, startLog.getY() + 0.5, startLog.getZ() + 0.5, 6,
                0.3, 0.3, 0.3, 0.05);

        // Auto-replant sapling at lowest base log position
        tryPlantSapling(level, lowest);
        return true;
    }

    private boolean hasAdjacentStorageSpace() {
        if (level == null) return false;
        for (Direction dir : Direction.values()) {
            BlockPos targetPos = worldPosition.relative(dir);
            BlockEntity be = level.getBlockEntity(targetPos);
            Container container = VolcaniteStoragePipeBlockEntity.getContainerAt(level, targetPos);
            if (be == this || container == this.inventory) continue;
            if (VolcaniteStoragePipeBlockEntity.isBlockedAutoOutputTarget(level, targetPos, be, container)) continue;
            if (be instanceof VolcaniteItemBarrelBlockEntity
                    || be instanceof VolcaniteStoragePipeBlockEntity
                    || be instanceof HermesItemPipeBlockEntity
                    || be instanceof VolcaniteUniversalConduitBlockEntity) {
                return true;
            }
            if (VolcaniteStoragePipeBlockEntity.isPureStorage(container)) {
                for (int slot = 0; slot < container.getContainerSize(); slot++) {
                    ItemStack slotStack = container.getItem(slot);
                    if (slotStack.isEmpty() || slotStack.getCount() < slotStack.getMaxStackSize()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasHarvestOutputSpace() {
        for (int slot = 4; slot <= 8; slot++) {
            ItemStack output = inventory.getItem(slot);
            if (output.isEmpty() || output.getCount() < output.getMaxStackSize()) return true;
        }
        return false;
    }

    private boolean canFitInHarvestOutputs(List<ItemStack> drops) {
        List<ItemStack> simulated = new java.util.ArrayList<>(5);
        for (int slot = 4; slot <= 8; slot++) simulated.add(inventory.getItem(slot).copy());
        for (ItemStack drop : drops) {
            int remaining = drop.getCount();
            for (ItemStack output : simulated) {
                if (remaining <= 0) break;
                if (ItemStack.isSameItemSameComponents(output, drop) && output.getCount() < output.getMaxStackSize()) {
                    int moved = Math.min(remaining, output.getMaxStackSize() - output.getCount());
                    output.grow(moved);
                    remaining -= moved;
                }
            }
            while (remaining > 0) {
                int empty = -1;
                for (int i = 0; i < simulated.size(); i++) {
                    if (simulated.get(i).isEmpty()) { empty = i; break; }
                }
                if (empty < 0) return false;
                int moved = Math.min(remaining, drop.getMaxStackSize());
                simulated.set(empty, drop.copyWithCount(moved));
                remaining -= moved;
            }
        }
        return true;
    }

    public boolean tryExportToAdjacentStorage(ItemStack stack) {
        if (level == null || stack.isEmpty())
            return false;
        VolcaniteStoragePipeBlockEntity.tryAutoOutputStack(level, worldPosition, this, stack);
        return stack.isEmpty();
    }

    public void exportOutputsToAdjacentStorage() {
        if (level == null || level.isClientSide())
            return;
        VolcaniteStoragePipeBlockEntity.autoEjectMachineOutputs(level, worldPosition, this);
    }

    public void depositOutput(ItemStack stack) {
        if (stack.isEmpty())
            return;

        // Keep one stack of planting material available; send extra saplings/seeds to compost.
        if (stack.is(ItemTags.SAPLINGS) || DemetrianBonsaiPlanterBlockEntity.isSaplingItem(stack) || getCropStateForSeed(stack) != null) {
            int saplingSlot = -1;
            for (int i = 0; i <= 1; i++) {
                if (ItemStack.isSameItemSameComponents(inventory.getItem(i), stack)) {
                    saplingSlot = i;
                    break;
                }
            }
            if (saplingSlot < 0) {
                for (int i = 0; i <= 1; i++) {
                    if (inventory.getItem(i).isEmpty()) {
                        saplingSlot = i;
                        break;
                    }
                }
            }
            if (saplingSlot >= 0) {
                ItemStack input = inventory.getItem(saplingSlot);
                int moved = Math.min(stack.getCount(), input.isEmpty() ? stack.getMaxStackSize()
                        : input.getMaxStackSize() - input.getCount());
                if (input.isEmpty() && moved > 0) inventory.setItem(saplingSlot, stack.copyWithCount(moved));
                else if (moved > 0) input.grow(moved);
                stack.shrink(moved);
            }
            if (!stack.isEmpty() && level != null) {
                for (Direction direction : Direction.values()) {
                    BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
                    if (neighbor instanceof DemetrianComposterBlockEntity composter) {
                        int moved = composter.insertNatural(stack);
                        if (moved > 0) stack.shrink(moved);
                        if (stack.isEmpty()) break;
                    }
                }
            }
            if (stack.isEmpty()) {
                inventory.setChanged();
                setChanged();
                return;
            }
        }

        // 1. If it is a seed/sapling matching input slots 0 or 1, replenish inputs
        // first up to max stack
        for (int i = 0; i <= 1; i++) {
            ItemStack inSlot = inventory.getItem(i);
            if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(inSlot, stack)) {
                int canFit = inSlot.getMaxStackSize() - inSlot.getCount();
                int toAdd = Math.min(canFit, stack.getCount());
                if (toAdd > 0) {
                    inSlot.grow(toAdd);
                    stack.shrink(toAdd);
                    setChanged();
                    if (stack.isEmpty())
                        return;
                }
            }
        }

        // 2. Direct transfer extra items to adjacent Storage Unit or Chest
        if (tryExportToAdjacentStorage(stack)) {
            return;
        }

        // 3. Output slots 4..8; slot 9 is reserved for the rare hybrid sapling.
        for (int i = 4; i <= 8; i++) {
            ItemStack current = inventory.getItem(i);
            if (current.isEmpty()) {
                inventory.setItem(i, stack.copy());
                stack.setCount(0);
                setChanged();
                return;
            } else if (ItemStack.isSameItemSameComponents(current, stack)) {
                int canFit = current.getMaxStackSize() - current.getCount();
                int toAdd = Math.min(canFit, stack.getCount());
                if (toAdd > 0) {
                    current.grow(toAdd);
                    stack.shrink(toAdd);
                    setChanged();
                    if (stack.isEmpty())
                        return;
                }
            }
        }

        // 4. Extra items stay safely stored in buffer - NEVER drop items on the ground!
    }

    public void cycleRadius() {
        this.radius++;
        if (this.radius > 4)
            this.radius = 1; // Cycles 1 (3x3), 2 (5x5), 3 (7x7), 4 (9x9)
        this.currentScanIndex = 0;
        setChanged();
    }

    public void cycleMode() {
        this.mode = (this.mode + 1) % 4; // 0 = All, 1 = Crops, 2 = Trees, 3 = Leaves
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        NonNullList<ItemStack> list = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            list.set(i, inventory.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, list, registries);
        tag.putLong("Energy", energyStorage.amount);
        tag.putInt("Lava", lavaAmount);
        tag.putInt("Radius", radius);
        tag.putInt("Mode", mode);
        tag.putInt("MutationSelection", mutationSelection);
        tag.putInt("MutationProgress", mutationProgress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        NonNullList<ItemStack> list = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, list, registries);
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setItem(i, list.get(i));
        }
        energyStorage.amount = tag.getLong("Energy");
        lavaAmount = tag.getInt("Lava");
        radius = tag.getInt("Radius");
        if (radius <= 0)
            radius = 1;
        mode = tag.getInt("Mode");
        mutationSelection = Math.floorMod(tag.getInt("MutationSelection"), 2);
        mutationProgress = Math.max(0, Math.min(MUTATION_PROGRESS_REQUIRED, tag.getInt("MutationProgress")));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.geothermal_arborist");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new GeothermalArboristMenu(containerId, playerInventory, this.inventory, this.data);
    }

    // WorldlyContainer & Container implementation
    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return this.inventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.inventory.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack res = this.inventory.removeItem(slot, amount);
        if (!res.isEmpty()) setChanged();
        return res;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack res = this.inventory.removeItemNoUpdate(slot);
        if (!res.isEmpty()) setChanged();
        return res;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.inventory.setItem(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }

    @Override
    public void clearContent() {
        this.inventory.clearContent();
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        // Slot 12: Dedicated Fertilizer (Bone Meal, Volcanic Ash, Volcanite Dust)
        if (slot == 12) {
            return isFertilizer(stack);
        }
        // Slots 0 & 1: Seeds, Saplings & Bonsai Soil (only if NOT fertilizer to ensure fertilizer prioritizes slot 12)
        if (slot == 0 || slot == 1) {
            return !isFertilizer(stack) && (getCropStateForSeed(stack) != null
                    || stack.is(ItemTags.SAPLINGS)
                    || DemetrianBonsaiPlanterBlockEntity.isSaplingItem(stack)
                    || DemetrianBonsaiPlanterBlockEntity.isSoilItem(stack));
        }
        // Slot 2: Fluid / Water / Lava bucket
        if (slot == 2) {
            return stack.is(Items.WATER_BUCKET) || stack.is(Items.LAVA_BUCKET) || stack.is(ObsidiumMod.STEAM_BUCKET);
        }
        // Slot 10: Auto-Hoe Upgrade (Any Hoe)
        if (slot == 10) {
            return stack.getItem() instanceof HoeItem;
        }
        // Slot 11: Bloom Infuser Upgrade
        if (slot == 11) {
            return stack.getItem() instanceof PyrogenicBloomInfuserItem;
        }
        // Slots 4..8 are harvest outputs, slot 9 is reserved hybrid output, slot 3 is empty bucket output
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return new int[] { 12, 4, 5, 6, 7, 8, 3 };
        } else if (side == Direction.UP) {
            return new int[] { 12, 0, 1, 2 };
        } else {
            return new int[] { 12, 0, 1, 2, 4, 5, 6, 7, 8 };
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return (slot >= 4 && slot <= 8) || slot == 3;
    }

    @Override
    public boolean isInputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 0 || slot == 1 || slot == 2 || slot == 10 || slot == 11 || slot == 12;
    }

    @Override
    public boolean isOutputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return (slot >= 4 && slot <= 8) || slot == 3;
    }
}
