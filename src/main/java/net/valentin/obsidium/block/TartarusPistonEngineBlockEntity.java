package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.VolcaniteBottleItem;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyItem;
import team.reborn.energy.api.base.SimpleEnergyStorage;

public class TartarusPistonEngineBlockEntity extends BlockEntity implements MenuProvider, InventoryHolder, MachineInventory, WorldlyContainer, SingleSlotStorage<FluidVariant> {

    public static final int INVENTORY_SIZE = 4;
    // Slot 0: Fuel Input (Solid fuels, Buckets, Bottles)
    // Slot 1: Empty Bucket / Container Return
    // Slot 2: Battery / Tool Charge Slot
    // Slot 3: Cryo-Coolant Input (Ice, Packed Ice, Blue Ice, Snow, Dew Droplet, Water/Bio-Ethanol Bucket)

    public static final int DATA_COUNT = 13;
    public static final int MODE_ECO = 0;
    public static final int MODE_STANDARD = 1;
    public static final int MODE_OVERDRIVE = 2;

    public static final int MAX_ENERGY = 2_000_000;
    public static final int MAX_EXTRACT_RATE = 32_000;
    public static final int MAX_FLUID = 16_000; // 16,000 mB fuel
    public static final int MAX_CRYO = 8_000;   // 8,000 cryo units

    private final NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private int burnTime = 0;
    private int maxBurnTime = 0;
    private int currentRfPerTick = 0;
    private int fluidAmount = 0;
    private FluidVariant storedFluid = FluidVariant.blank();

    private int cryoAmount = 0;
    private int cylinderTemp = 25;   // 25°C .. 1200°C
    private int rpm = 0;             // 0 .. 9000 RPM
    private int driveMode = MODE_STANDARD;

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
                case 7 -> fluidAmount;
                case 8 -> MAX_FLUID;
                case 9 -> cryoAmount;
                case 10 -> cylinderTemp;
                case 11 -> rpm;
                case 12 -> driveMode;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> burnTime = value;
                case 1 -> maxBurnTime = value;
                case 2 -> currentRfPerTick = value;
                case 7 -> fluidAmount = value;
                case 9 -> cryoAmount = value;
                case 10 -> cylinderTemp = value;
                case 11 -> rpm = value;
                case 12 -> driveMode = value;
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public TartarusPistonEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.TARTARUS_PISTON_ENGINE_BE, pos, state);
    }

    public boolean isBurning() {
        return this.burnTime > 0;
    }

    public void cycleDriveMode() {
        this.driveMode = (this.driveMode + 1) % 3;
        setChanged();
    }

    public static int getCryoValue(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.is(Items.BLUE_ICE)) return 2000;
        if (stack.is(Items.PACKED_ICE)) return 750;
        if (stack.is(Items.ICE)) return 250;
        if (stack.is(ObsidiumMod.DEW_DROPLET)) return 200;
        if (stack.is(Items.SNOW_BLOCK)) return 150;
        if (stack.is(Items.SNOWBALL)) return 40;
        if (stack.is(Items.WATER_BUCKET)) return 400;
        if (stack.is(ObsidiumMod.BIO_ETHANOL_BUCKET)) return 800;
        return 0;
    }

    public static boolean isCryoItem(ItemStack stack) {
        return getCryoValue(stack) > 0;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TartarusPistonEngineBlockEntity entity) {
        if (level.isClientSide()) return;
        boolean dirty = false;

        // 1. Process fuel container in Slot 0
        dirty |= entity.processFuelSlot();

        // 2. Process Cryo-Ice / Coolant in Slot 3
        dirty |= entity.processCryoSlot();

        // 3. Combustion, Heat, Cryo-Cooling & RPM Physics
        boolean activeBurn = entity.burnTime > 0;
        if (activeBurn) {
            int burnStep = entity.driveMode == MODE_OVERDRIVE ? 2 : 1;
            entity.burnTime = Math.max(0, entity.burnTime - burnStep);

            // Heat generation per tick
            int heatGain = switch (entity.driveMode) {
                case MODE_ECO -> 2;
                case MODE_OVERDRIVE -> 7;
                default -> 4;
            };
            entity.cylinderTemp = Math.min(1200, entity.cylinderTemp + heatGain);

            // Cryo-Cooling active if cryo buffer > 0
            boolean cryoActive = entity.cryoAmount > 0;
            if (cryoActive && entity.cylinderTemp > 90) {
                int cryoUse = entity.driveMode == MODE_OVERDRIVE ? 4 : 2;
                int actualUse = Math.min(cryoUse, entity.cryoAmount);
                entity.cryoAmount -= actualUse;
                entity.cylinderTemp = Math.max(25, entity.cylinderTemp - actualUse * 3);
            }

            // Target RPM
            int targetRpm = switch (entity.driveMode) {
                case MODE_ECO -> 3200;
                case MODE_OVERDRIVE -> cryoActive ? 9000 : 7200;
                default -> cryoActive ? 6000 : 5000;
            };
            if (entity.cylinderTemp >= 950) {
                targetRpm = (int) (targetRpm * 0.65); // Thermal throttling
            }
            if (entity.rpm < targetRpm) {
                entity.rpm = Math.min(targetRpm, entity.rpm + 120);
            } else if (entity.rpm > targetRpm) {
                entity.rpm = Math.max(targetRpm, entity.rpm - 80);
            }

            // Calculate effective RF/t with Cryo-Supercharge, Drive Mode, and Thermal Throttle
            double mult = switch (entity.driveMode) {
                case MODE_ECO -> 0.75;
                case MODE_OVERDRIVE -> 1.60;
                default -> 1.00;
            };
            if (cryoActive && entity.cylinderTemp <= 420) {
                mult *= 1.35; // Cryo-Supercharged Compression bonus (+35%)
            } else if (entity.cylinderTemp >= 950) {
                mult *= 0.65; // Overheated throttle
            }
            double rpmScale = Math.max(0.35, Math.min(1.0, entity.rpm / 4500.0));
            int effectiveRf = (int) Math.round(entity.currentRfPerTick * mult * rpmScale);

            if (entity.energyStorage.amount < entity.energyStorage.capacity) {
                long added = Math.min(effectiveRf, entity.energyStorage.capacity - entity.energyStorage.amount);
                entity.energyStorage.amount += added;
            }
            dirty = true;

            if (level.getGameTime() % 20 == 0 && level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 1.0, pos.getY() + 2.1, pos.getZ() + 1.0,
                        3, 0.3, 0.1, 0.3, 0.02);
                if (cryoActive && entity.cylinderTemp <= 420) {
                    serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, pos.getX() + 1.0, pos.getY() + 1.4, pos.getZ() + 1.0,
                            3, 0.5, 0.3, 0.5, 0.02);
                } else {
                    serverLevel.sendParticles(ParticleTypes.FLAME, pos.getX() + 1.0, pos.getY() + 1.1, pos.getZ() + 1.0,
                            2, 0.2, 0.1, 0.2, 0.01);
                }
            }
        } else {
            // Idle cooldown & flywheel coast-down
            if (entity.rpm > 0) {
                entity.rpm = Math.max(0, entity.rpm - 90);
                dirty = true;
            }
            if (entity.cylinderTemp > 25) {
                int coolRate = entity.cryoAmount > 0 ? 5 : 2;
                entity.cylinderTemp = Math.max(25, entity.cylinderTemp - coolRate);
                dirty = true;
            }
        }

        // 4. Start new burn cycle if needed and has energy space
        if (entity.burnTime <= 0 && entity.energyStorage.amount < entity.energyStorage.capacity) {
            boolean cryoEfficiency = entity.cryoAmount > 0 && entity.cylinderTemp <= 420;
            if (entity.fluidAmount >= 50 && !entity.storedFluid.isBlank()) {
                int baseRf = getRfPerTickForFluid(entity.storedFluid);
                int baseDuration = getDurationForFluid(entity.storedFluid);
                if (entity.driveMode == MODE_ECO) baseDuration = (int) (baseDuration * 1.45);
                if (cryoEfficiency) baseDuration = (int) (baseDuration * 1.25);

                entity.fluidAmount -= 50;
                if (entity.fluidAmount <= 0) entity.storedFluid = FluidVariant.blank();
                entity.burnTime = baseDuration;
                entity.maxBurnTime = baseDuration;
                entity.currentRfPerTick = baseRf;
                dirty = true;
            } else if (!entity.items.get(0).isEmpty()) {
                ItemStack solidFuel = entity.items.get(0);
                int vanillaBurn = AbstractFurnaceBlockEntity.getFuel().getOrDefault(solidFuel.getItem(), 0);
                if (vanillaBurn > 0 && !isBucketFuel(solidFuel)) {
                    boolean isAshBriquette = solidFuel.is(ObsidiumMod.ASH_BRIQUETTE);
                    int rate = isAshBriquette ? 2_200 : 1_500;
                    int burnDuration = Math.max(15, isAshBriquette ? vanillaBurn / 3 : vanillaBurn / 6);
                    if (entity.driveMode == MODE_ECO) burnDuration = (int) (burnDuration * 1.45);
                    if (cryoEfficiency) burnDuration = (int) (burnDuration * 1.25);

                    entity.burnTime = burnDuration;
                    entity.maxBurnTime = burnDuration;
                    entity.currentRfPerTick = rate;
                    solidFuel.shrink(1);
                    dirty = true;
                }
            }
        }

        // 5. Charge battery/tool in Slot 2
        ItemStack batteryStack = entity.items.get(2);
        if (!batteryStack.isEmpty() && entity.energyStorage.amount > 0) {
            EnergyStorage itemStorage = EnergyStorage.ITEM.find(batteryStack, null);
            if (itemStorage != null && itemStorage.supportsInsertion()) {
                EnergyStorageUtil.move(entity.energyStorage, itemStorage, Math.min(10_000, entity.energyStorage.amount), null);
                dirty = true;
            } else if (batteryStack.getItem() instanceof SimpleEnergyItem energyItem) {
                long needed = energyItem.getEnergyCapacity(batteryStack) - energyItem.getStoredEnergy(batteryStack);
                if (needed > 0) {
                    long toTransfer = Math.min(Math.min(needed, 10_000), entity.energyStorage.amount);
                    energyItem.setStoredEnergy(batteryStack, energyItem.getStoredEnergy(batteryStack) + toTransfer);
                    entity.energyStorage.amount -= toTransfer;
                    dirty = true;
                }
            }
        }

        // 6. Push RF to all surrounding external blocks from the 2x2x2 footprint
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

        if (dirty) entity.setChanged();
        VolcaniteStoragePipeBlockEntity.autoEjectMachineOutputs(level, pos, entity);
    }

    private boolean processFuelSlot() {
        ItemStack fuelStack = this.items.get(0);
        if (fuelStack.isEmpty()) return false;

        FluidVariant bucketFluid = getFluidFromBucket(fuelStack);
        if (!bucketFluid.isBlank()) {
            if (canAcceptFluid(bucketFluid, 1000) && canOutputBucket(Items.BUCKET)) {
                insertFluidInternal(bucketFluid, 1000);
                outputBucket(new ItemStack(Items.BUCKET));
                fuelStack.shrink(1);
                return true;
            }
        } else if (fuelStack.is(ObsidiumMod.PYROCLASTIC_FLASK)) {
            FluidVariant dist = FluidVariant.of(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID);
            if (canAcceptFluid(dist, 250) && canOutputBucket(Items.GLASS_BOTTLE)) {
                insertFluidInternal(dist, 250);
                outputBucket(new ItemStack(Items.GLASS_BOTTLE));
                fuelStack.shrink(1);
                return true;
            }
        } else if (fuelStack.getItem() instanceof VolcaniteBottleItem) {
            int bottleLava = VolcaniteBottleItem.getLava(fuelStack);
            if (bottleLava > 0 && canAcceptFluid(FluidVariant.of(Fluids.LAVA), Math.min(250, bottleLava))) {
                int move = Math.min(250, bottleLava);
                insertFluidInternal(FluidVariant.of(Fluids.LAVA), move);
                VolcaniteBottleItem.setLava(fuelStack, bottleLava - move);
                return true;
            }
        }
        return false;
    }

    private boolean processCryoSlot() {
        ItemStack cryoStack = this.items.get(3);
        if (cryoStack.isEmpty()) return false;
        int val = getCryoValue(cryoStack);
        if (val <= 0 || this.cryoAmount + val > MAX_CRYO) return false;

        boolean isBucket = cryoStack.is(Items.WATER_BUCKET) || cryoStack.is(ObsidiumMod.BIO_ETHANOL_BUCKET);
        if (isBucket && !canOutputBucket(Items.BUCKET)) return false;

        this.cryoAmount = Math.min(MAX_CRYO, this.cryoAmount + val);
        if (isBucket) {
            outputBucket(new ItemStack(Items.BUCKET));
        }
        cryoStack.shrink(1);
        return true;
    }

    private static boolean isBucketFuel(ItemStack stack) {
        return !getFluidFromBucket(stack).isBlank() || stack.is(ObsidiumMod.PYROCLASTIC_FLASK);
    }

    private static FluidVariant getFluidFromBucket(ItemStack stack) {
        if (stack.is(Items.LAVA_BUCKET)) return FluidVariant.of(Fluids.LAVA);
        if (stack.is(ObsidiumMod.BIO_OIL_BUCKET)) return FluidVariant.of(ObsidiumMod.BIO_OIL_FLUID);
        if (stack.is(ObsidiumMod.BIODIESEL_BUCKET)) return FluidVariant.of(ObsidiumMod.BIODIESEL_FLUID);
        if (stack.is(ObsidiumMod.PYROCLASTIC_DISTILLATE_BUCKET)) return FluidVariant.of(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID);
        if (stack.is(ObsidiumMod.PYRO_DIESEL_BUCKET)) return FluidVariant.of(ObsidiumMod.PYRO_DIESEL_FLUID);
        return FluidVariant.blank();
    }

    private static int getRfPerTickForFluid(FluidVariant variant) {
        if (variant.isOf(ObsidiumMod.PYRO_DIESEL_FLUID)) return 16_000;
        if (variant.isOf(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID)) return 10_000;
        if (variant.isOf(ObsidiumMod.BIODIESEL_FLUID)) return 6_000;
        if (variant.isOf(ObsidiumMod.BIO_OIL_FLUID)) return 3_500;
        return 2_500; // Lava
    }

    private static int getDurationForFluid(FluidVariant variant) {
        if (variant.isOf(ObsidiumMod.PYRO_DIESEL_FLUID)) return 40;
        if (variant.isOf(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID)) return 35;
        if (variant.isOf(ObsidiumMod.BIODIESEL_FLUID)) return 30;
        if (variant.isOf(ObsidiumMod.BIO_OIL_FLUID)) return 24;
        return 20; // Lava
    }

    private boolean canAcceptFluid(FluidVariant variant, int amount) {
        if (variant.isBlank()) return false;
        boolean validFuel = variant.isOf(Fluids.LAVA)
                || variant.isOf(ObsidiumMod.BIO_OIL_FLUID)
                || variant.isOf(ObsidiumMod.BIODIESEL_FLUID)
                || variant.isOf(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID)
                || variant.isOf(ObsidiumMod.PYRO_DIESEL_FLUID);
        if (!validFuel) return false;
        if (this.storedFluid.isBlank() || this.storedFluid.equals(variant)) {
            return this.fluidAmount + amount <= MAX_FLUID;
        }
        return false;
    }

    private void insertFluidInternal(FluidVariant variant, int amount) {
        this.storedFluid = variant;
        this.fluidAmount = Math.min(MAX_FLUID, this.fluidAmount + amount);
    }

    private boolean canOutputBucket(net.minecraft.world.item.Item returnItem) {
        ItemStack out = this.items.get(1);
        if (out.isEmpty()) return true;
        return out.is(returnItem) && out.getCount() < out.getMaxStackSize();
    }

    private void outputBucket(ItemStack returnStack) {
        ItemStack out = this.items.get(1);
        if (out.isEmpty()) {
            this.items.set(1, returnStack);
        } else if (out.is(returnStack.getItem())) {
            out.grow(returnStack.getCount());
        }
    }

    public boolean handleFluidInteraction(Player player, InteractionHand hand, ItemStack stack) {
        FluidVariant bucketFluid = getFluidFromBucket(stack);
        if (!bucketFluid.isBlank() && canAcceptFluid(bucketFluid, 1000)) {
            if (!level.isClientSide()) {
                insertFluidInternal(bucketFluid, 1000);
                if (!player.getAbilities().instabuild) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                level.playSound(null, worldPosition, bucketFluid.isOf(Fluids.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                setChanged();
            }
            return true;
        }
        int cryoVal = getCryoValue(stack);
        if (cryoVal > 0 && this.cryoAmount + cryoVal <= MAX_CRYO) {
            if (!level.isClientSide()) {
                this.cryoAmount = Math.min(MAX_CRYO, this.cryoAmount + cryoVal);
                boolean isBucket = stack.is(Items.WATER_BUCKET) || stack.is(ObsidiumMod.BIO_ETHANOL_BUCKET);
                if (!player.getAbilities().instabuild) {
                    if (isBucket) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                    else stack.shrink(1);
                }
                level.playSound(null, worldPosition, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.BLOCKS, 0.8F, 1.2F);
                setChanged();
            }
            return true;
        }
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatableWithFallback("block.obsidium.tartarus_piston_engine", "Cryo-Thermic Piston Engine");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new TartarusPistonEngineMenu(syncId, playerInventory, this, this.dataAccess, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putInt("BurnTime", this.burnTime);
        tag.putInt("MaxBurnTime", this.maxBurnTime);
        tag.putInt("CurrentRf", this.currentRfPerTick);
        tag.putLong("Energy", this.energyStorage.amount);
        tag.putInt("FluidAmount", this.fluidAmount);
        tag.putInt("CryoAmount", this.cryoAmount);
        tag.putInt("CylinderTemp", this.cylinderTemp);
        tag.putInt("Rpm", this.rpm);
        tag.putInt("DriveMode", this.driveMode);
        if (!this.storedFluid.isBlank()) {
            tag.putString("StoredFluid", BuiltInRegistries.FLUID.getKey(this.storedFluid.getFluid()).toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.burnTime = tag.getInt("BurnTime");
        this.maxBurnTime = tag.getInt("MaxBurnTime");
        this.currentRfPerTick = tag.getInt("CurrentRf");
        this.energyStorage.amount = tag.getLong("Energy");
        this.fluidAmount = tag.getInt("FluidAmount");
        this.cryoAmount = tag.getInt("CryoAmount");
        this.cylinderTemp = tag.contains("CylinderTemp") ? tag.getInt("CylinderTemp") : 25;
        this.rpm = tag.getInt("Rpm");
        this.driveMode = tag.getInt("DriveMode");
        if (tag.contains("StoredFluid")) {
            Fluid f = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(tag.getString("StoredFluid")));
            this.storedFluid = (f != null && f != Fluids.EMPTY) ? FluidVariant.of(f) : FluidVariant.blank();
        }
    }

    // SingleSlotStorage for Fluids (accepts direct pipe pumping!)
    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        if (!canAcceptFluid(resource, 1)) return 0;
        int maxAccept_mB = MAX_FLUID - this.fluidAmount;
        long maxAccept_droplets = maxAccept_mB * 81L;
        long toInsert_droplets = Math.min(maxAmount, maxAccept_droplets);
        int toInsert_mB = (int) (toInsert_droplets / 81L);
        if (toInsert_mB <= 0) return 0;

        transaction.addCloseCallback((t, res) -> {
            if (res.wasCommitted()) {
                insertFluidInternal(resource, toInsert_mB);
                setChanged();
            }
        });
        return (long) toInsert_mB * 81L;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        return 0;
    }

    @Override
    public boolean isResourceBlank() {
        return this.storedFluid.isBlank();
    }

    @Override
    public FluidVariant getResource() {
        return this.storedFluid;
    }

    @Override
    public long getAmount() {
        return (long) this.fluidAmount * 81L;
    }

    @Override
    public long getCapacity() {
        return (long) MAX_FLUID * 81L;
    }

    // WorldlyContainer / InventoryHolder
    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{0, 1, 2, 3};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot == 3) return isCryoItem(stack);
        if (slot == 0) return AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0) > 0 || isBucketFuel(stack);
        return false;
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
        return slot == 0 || slot == 3;
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
