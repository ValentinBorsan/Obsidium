package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.VolcaniteBottleItem;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class VolcanicDistillationChamberBlockEntity extends BlockEntity implements MenuProvider, InventoryHolder, MachineInventory {
    public static final int INVENTORY_SIZE = 5;
    // Slot 0: Lava Bucket / Volcanite Bottle input (14, 22)
    // Slot 1: Empty Lava Bucket output (14, 52)
    // Slot 2: Chemical Reagent input (64, 22)
    // Slot 3: Empty Bucket / Glass Bottle input for bottling Distillate (122, 22)
    // Slot 4: Filled Pyroclastic Distillate Bucket / Flask output (122, 52)

    public static final int MAX_LAVA = 16000;
    public static final int MAX_ETHANOL = 16000;
    public static final int MAX_DISTILLATE = 16000;
    public static final int MAX_ENERGY = 250000;
    public static final int RF_PER_TICK = 250;
    public static final int MAX_PROGRESS = 100; // 5 seconds per chemical distillation batch

    private int lavaAmount = 0;
    private int ethanolAmount = 0;
    private int distillateAmount = 0;
    private int progress = 0;
    private FluidVariant inputFluid = FluidVariant.blank();
    private FluidVariant secondaryFluid = FluidVariant.blank();
    private FluidVariant outputFluid = FluidVariant.blank();

    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            VolcanicDistillationChamberBlockEntity.this.setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            if (slot == 0) return isValidInputBucket(stack);
            if (slot == 2) return isValidReagent(stack);
            if (slot == 3) return stack.is(Items.BUCKET) || stack.is(Items.GLASS_BOTTLE);
            return false;
        }
    };

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(MAX_ENERGY, 5000, 0) {
        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    public final SingleVariantStorage<FluidVariant> fluidStorage = new SingleVariantStorage<>() {
        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }

        @Override
        protected long getCapacity(FluidVariant variant) {
            return MAX_LAVA * 81L;
        }

        @Override
        protected boolean canInsert(FluidVariant variant) {
            return variant.isOf(Fluids.LAVA)
                    || variant.isOf(ObsidiumMod.BIO_OIL_FLUID)
                    || variant.isOf(ObsidiumMod.BIODIESEL_FLUID);
        }

        @Override
        protected boolean canExtract(FluidVariant variant) {
            return false;
        }

        @Override
        protected void onFinalCommit() {
            lavaAmount = (int) (amount / 81L);
            inputFluid = lavaAmount > 0 ? variant : FluidVariant.blank();
            setChanged();
        }
    };

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> lavaAmount;
                case 1 -> distillateAmount;
                case 2 -> progress;
                case 3 -> MAX_PROGRESS;
                case 4 -> (int) (energyStorage.amount & 0xFFFF);
                case 5 -> (int) ((energyStorage.amount >> 16) & 0xFFFF);
                case 6 -> getInputFluidTypeId(inputFluid);
                case 7 -> getOutputFluidTypeId(outputFluid);
                case 8 -> ethanolAmount;
                case 9 -> getSecondaryFluidTypeId(secondaryFluid);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> lavaAmount = value;
                case 1 -> distillateAmount = value;
                case 2 -> progress = value;
                case 4 -> energyStorage.amount = (energyStorage.amount & 0xFFFF0000L) | (value & 0xFFFFL);
                case 5 -> energyStorage.amount = (energyStorage.amount & 0xFFFFL) | ((long) (value & 0xFFFF) << 16);
                case 8 -> ethanolAmount = value;
            }
        }

        @Override
        public int getCount() {
            return 10;
        }
    };

    public VolcanicDistillationChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.VOLCANIC_DISTILLATION_CHAMBER_BE, pos, state);
    }

    public static boolean isValidInputBucket(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(Items.LAVA_BUCKET)
                || stack.is(ObsidiumMod.BIO_OIL_BUCKET)
                || stack.is(ObsidiumMod.BIODIESEL_BUCKET)
                || stack.is(ObsidiumMod.BIO_ETHANOL_BUCKET)
                || stack.is(ObsidiumMod.PYROCLASTIC_DISTILLATE_BUCKET)
                || stack.getItem() instanceof VolcaniteBottleItem;
    }

    public static boolean isValidReagent(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return getReagentDistillateYield(stack) > 0
                || stack.is(ObsidiumMod.BIO_ETHANOL_BUCKET)
                || stack.is(ObsidiumMod.PYROCLASTIC_DISTILLATE_BUCKET)
                || stack.is(ObsidiumMod.PYROCLASTIC_FLASK);
    }

    public static int getInputFluidTypeId(FluidVariant variant) {
        if (variant == null || variant.isBlank()) return 0;
        if (variant.isOf(Fluids.LAVA)) return 1;
        if (variant.isOf(ObsidiumMod.BIO_OIL_FLUID)) return 2;
        if (variant.isOf(ObsidiumMod.BIODIESEL_FLUID)) return 3;
        return 0;
    }

    public static int getSecondaryFluidTypeId(FluidVariant variant) {
        if (variant == null || variant.isBlank()) return 0;
        if (variant.isOf(ObsidiumMod.BIO_ETHANOL_FLUID)) return 1;
        if (variant.isOf(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID)) return 2;
        return 0;
    }

    public static int getOutputFluidTypeId(FluidVariant variant) {
        if (variant == null || variant.isBlank()) return 0;
        if (variant.isOf(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID)) return 1;
        if (variant.isOf(ObsidiumMod.BIODIESEL_FLUID)) return 2;
        if (variant.isOf(ObsidiumMod.PYRO_DIESEL_FLUID)) return 3;
        return 0;
    }

    public int getLavaAmount() {
        return lavaAmount;
    }

    public int getEthanolAmount() {
        return ethanolAmount;
    }

    public FluidVariant getInputFluid() {
        return inputFluid;
    }

    public FluidVariant getSecondaryFluid() {
        return secondaryFluid;
    }

    public FluidVariant getOutputFluid() {
        return outputFluid;
    }

    public int insertFluid(FluidVariant variant, int maxInsert, boolean simulate) {
        if (variant == null || variant.isBlank() || maxInsert <= 0) return 0;

        if (variant.isOf(Fluids.LAVA) || variant.isOf(ObsidiumMod.BIO_OIL_FLUID) || variant.isOf(ObsidiumMod.BIODIESEL_FLUID)) {
            if (lavaAmount > 0 && !inputFluid.equals(variant)) return 0;
            int toInsert = Math.min(maxInsert, MAX_LAVA - lavaAmount);
            if (toInsert > 0 && !simulate) {
                inputFluid = variant;
                lavaAmount += toInsert;
                syncFluidStorage();
                setChanged();
            }
            return toInsert;
        }

        if (variant.isOf(ObsidiumMod.BIO_ETHANOL_FLUID) || variant.isOf(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID)) {
            if (ethanolAmount > 0 && !secondaryFluid.equals(variant)) return 0;
            int toInsert = Math.min(maxInsert, MAX_ETHANOL - ethanolAmount);
            if (toInsert > 0 && !simulate) {
                secondaryFluid = variant;
                ethanolAmount += toInsert;
                setChanged();
            }
            return toInsert;
        }

        return 0;
    }

    public static int getReagentDistillateYield(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.is(ObsidiumMod.ASTRAL_VOLCANIC_CATALYST)) return 2000; // Supercharged 2,000 mB yield!
        if (stack.is(ObsidiumMod.VOLCANIC_ALLOY) || stack.is(ObsidiumMod.VOLCANITE_THERMO_CORE)) return 1500;
        if (stack.is(ObsidiumMod.VOLCANITE) || stack.is(ObsidiumMod.VOLCANITE_INGOT) || stack.is(Items.MAGMA_CREAM)) return 1000;
        if (stack.is(ObsidiumMod.VOLCANIC_ASH)) return 600;
        if (stack.is(Items.BLAZE_POWDER) || stack.is(Items.GUNPOWDER) || stack.is(Items.GLOWSTONE_DUST)) return 500;
        return 0;
    }

    public static int getReagentLavaCost(ItemStack stack) {
        if (stack.isEmpty()) return 500;
        if (stack.is(ObsidiumMod.ASTRAL_VOLCANIC_CATALYST) || stack.is(ObsidiumMod.VOLCANIC_ALLOY) || stack.is(ObsidiumMod.VOLCANITE_THERMO_CORE)) return 1000;
        if (stack.is(ObsidiumMod.VOLCANITE) || stack.is(ObsidiumMod.VOLCANITE_INGOT) || stack.is(Items.MAGMA_CREAM)) return 800;
        return 500;
    }

    public void syncFluidStorage() {
        if (lavaAmount <= 0) {
            inputFluid = FluidVariant.blank();
        } else if (inputFluid.isBlank()) {
            inputFluid = FluidVariant.of(Fluids.LAVA);
        }
        this.fluidStorage.variant = inputFluid;
        this.fluidStorage.amount = lavaAmount * 81L;
    }

    public int getDistillateAmount() {
        return distillateAmount;
    }

    public int extractDistillate(int maxExtract) {
        int extracted = Math.min(distillateAmount, maxExtract);
        if (extracted > 0) {
            distillateAmount -= extracted;
            if (distillateAmount <= 0) {
                distillateAmount = 0;
                outputFluid = FluidVariant.blank();
            }
            setChanged();
        }
        return extracted;
    }

    public int extractDistillate(FluidVariant variant, int maxExtract, boolean simulate) {
        if (distillateAmount <= 0 || outputFluid.isBlank() || !outputFluid.equals(variant) || maxExtract <= 0) {
            return 0;
        }
        int extracted = Math.min(distillateAmount, maxExtract);
        if (extracted > 0 && !simulate) {
            distillateAmount -= extracted;
            if (distillateAmount <= 0) {
                distillateAmount = 0;
                outputFluid = FluidVariant.blank();
            }
            setChanged();
        }
        return extracted;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VolcanicDistillationChamberBlockEntity entity) {
        if (level.isClientSide()) return;

        boolean dirty = false;

        // 1a. Drain Input Bucket / Volcanite Bottle from Slot 0 into Primary or Secondary Fluid Tank
        ItemStack lavaInput = entity.inventory.getItem(0);
        ItemStack lavaOutput = entity.inventory.getItem(1);
        FluidVariant primaryBucketFluid = FluidVariant.blank();
        FluidVariant secondaryBucketFluid = FluidVariant.blank();
        if (lavaInput.is(Items.LAVA_BUCKET)) primaryBucketFluid = FluidVariant.of(Fluids.LAVA);
        else if (lavaInput.is(ObsidiumMod.BIO_OIL_BUCKET)) primaryBucketFluid = FluidVariant.of(ObsidiumMod.BIO_OIL_FLUID);
        else if (lavaInput.is(ObsidiumMod.BIODIESEL_BUCKET)) primaryBucketFluid = FluidVariant.of(ObsidiumMod.BIODIESEL_FLUID);
        else if (lavaInput.is(ObsidiumMod.BIO_ETHANOL_BUCKET)) secondaryBucketFluid = FluidVariant.of(ObsidiumMod.BIO_ETHANOL_FLUID);
        else if (lavaInput.is(ObsidiumMod.PYROCLASTIC_DISTILLATE_BUCKET)) secondaryBucketFluid = FluidVariant.of(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID);

        if (!primaryBucketFluid.isBlank()
                && (entity.lavaAmount == 0 || entity.inputFluid.equals(primaryBucketFluid))
                && entity.lavaAmount + 1000 <= MAX_LAVA) {
            if (lavaOutput.isEmpty() || (lavaOutput.is(Items.BUCKET) && lavaOutput.getCount() < lavaOutput.getMaxStackSize())) {
                if (entity.lavaAmount == 0) {
                    entity.inputFluid = primaryBucketFluid;
                }
                entity.lavaAmount += 1000;
                entity.syncFluidStorage();
                lavaInput.shrink(1);
                if (lavaOutput.isEmpty()) {
                    entity.inventory.setItem(1, new ItemStack(Items.BUCKET));
                } else {
                    lavaOutput.grow(1);
                }
                dirty = true;
            }
        } else if (!secondaryBucketFluid.isBlank()
                && (entity.ethanolAmount == 0 || entity.secondaryFluid.equals(secondaryBucketFluid))
                && entity.ethanolAmount + 1000 <= MAX_ETHANOL) {
            if (lavaOutput.isEmpty() || (lavaOutput.is(Items.BUCKET) && lavaOutput.getCount() < lavaOutput.getMaxStackSize())) {
                if (entity.ethanolAmount == 0) {
                    entity.secondaryFluid = secondaryBucketFluid;
                }
                entity.ethanolAmount += 1000;
                lavaInput.shrink(1);
                if (lavaOutput.isEmpty()) {
                    entity.inventory.setItem(1, new ItemStack(Items.BUCKET));
                } else {
                    lavaOutput.grow(1);
                }
                dirty = true;
            }
        } else if (lavaInput.getItem() instanceof VolcaniteBottleItem
                && (entity.lavaAmount == 0 || entity.inputFluid.isOf(Fluids.LAVA))) {
            int bottleLava = VolcaniteBottleItem.getLava(lavaInput);
            if (bottleLava > 0 && entity.lavaAmount < MAX_LAVA) {
                int toTransfer = Math.min(bottleLava, Math.min(1000, MAX_LAVA - entity.lavaAmount));
                if (toTransfer > 0) {
                    if (entity.lavaAmount == 0) entity.inputFluid = FluidVariant.of(Fluids.LAVA);
                    entity.lavaAmount += toTransfer;
                    entity.syncFluidStorage();
                    VolcaniteBottleItem.setLava(lavaInput, bottleLava - toTransfer);
                    dirty = true;
                }
            }
        }

        // 1b. Drain Liquid Buckets / Flasks from Slot 2 into Secondary Fluid Tank
        ItemStack reagent = entity.inventory.getItem(2);
        lavaOutput = entity.inventory.getItem(1);
        if (!reagent.isEmpty()) {
            FluidVariant secFluidFromReagent = FluidVariant.blank();
            int secFluidAmt = 0;
            ItemStack emptyReturn = ItemStack.EMPTY;
            if (reagent.is(ObsidiumMod.BIO_ETHANOL_BUCKET)) {
                secFluidFromReagent = FluidVariant.of(ObsidiumMod.BIO_ETHANOL_FLUID);
                secFluidAmt = 1000;
                emptyReturn = new ItemStack(Items.BUCKET);
            } else if (reagent.is(ObsidiumMod.PYROCLASTIC_DISTILLATE_BUCKET)) {
                secFluidFromReagent = FluidVariant.of(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID);
                secFluidAmt = 1000;
                emptyReturn = new ItemStack(Items.BUCKET);
            } else if (reagent.is(ObsidiumMod.PYROCLASTIC_FLASK)) {
                secFluidFromReagent = FluidVariant.of(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID);
                secFluidAmt = 250;
                emptyReturn = new ItemStack(Items.GLASS_BOTTLE);
            }
            if (!secFluidFromReagent.isBlank()
                    && (entity.ethanolAmount == 0 || entity.secondaryFluid.equals(secFluidFromReagent))
                    && entity.ethanolAmount + secFluidAmt <= MAX_ETHANOL
                    && (lavaOutput.isEmpty() || (ItemStack.isSameItemSameComponents(lavaOutput, emptyReturn) && lavaOutput.getCount() < lavaOutput.getMaxStackSize()))) {
                if (entity.ethanolAmount == 0) {
                    entity.secondaryFluid = secFluidFromReagent;
                }
                entity.ethanolAmount += secFluidAmt;
                reagent.shrink(1);
                if (lavaOutput.isEmpty()) {
                    entity.inventory.setItem(1, emptyReturn);
                } else {
                    lavaOutput.grow(1);
                }
                dirty = true;
            }
        }

        // 2. Chemical Distillation / Refining Process
        reagent = entity.inventory.getItem(2);
        lavaOutput = entity.inventory.getItem(1);
        int yield = 0;
        int inputCost = 0;
        int secondaryCost = 0;
        boolean consumeReagentItem = false;
        FluidVariant targetOutputFluid = FluidVariant.blank();
        ItemStack reagentReturn = ItemStack.EMPTY;

        if (entity.lavaAmount > 0) {
            if (entity.inputFluid.isOf(Fluids.LAVA) && !reagent.isEmpty()) {
                int rYield = getReagentDistillateYield(reagent);
                if (rYield > 0) {
                    yield = rYield;
                    inputCost = getReagentLavaCost(reagent);
                    consumeReagentItem = true;
                    targetOutputFluid = FluidVariant.of(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID);
                }
            } else if (entity.inputFluid.isOf(ObsidiumMod.BIO_OIL_FLUID)) {
                if (entity.ethanolAmount >= 500 && entity.secondaryFluid.isOf(ObsidiumMod.BIO_ETHANOL_FLUID)) {
                    yield = 1500;
                    inputCost = 1000;
                    secondaryCost = 500;
                    targetOutputFluid = FluidVariant.of(ObsidiumMod.BIODIESEL_FLUID);
                } else if (reagent.is(ObsidiumMod.BIO_ETHANOL_BUCKET)) {
                    yield = 1500;
                    inputCost = 1000;
                    consumeReagentItem = true;
                    targetOutputFluid = FluidVariant.of(ObsidiumMod.BIODIESEL_FLUID);
                    reagentReturn = new ItemStack(Items.BUCKET);
                }
            } else if (entity.inputFluid.isOf(ObsidiumMod.BIODIESEL_FLUID)) {
                if (entity.ethanolAmount >= 500 && entity.secondaryFluid.isOf(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID)) {
                    yield = 1500;
                    inputCost = 1000;
                    secondaryCost = 500;
                    targetOutputFluid = FluidVariant.of(ObsidiumMod.PYRO_DIESEL_FLUID);
                } else if (reagent.is(ObsidiumMod.PYROCLASTIC_DISTILLATE_BUCKET)) {
                    yield = 1500;
                    inputCost = 1000;
                    consumeReagentItem = true;
                    targetOutputFluid = FluidVariant.of(ObsidiumMod.PYRO_DIESEL_FLUID);
                    reagentReturn = new ItemStack(Items.BUCKET);
                } else if (reagent.is(ObsidiumMod.PYROCLASTIC_FLASK)) {
                    yield = 1000;
                    inputCost = 1000;
                    consumeReagentItem = true;
                    targetOutputFluid = FluidVariant.of(ObsidiumMod.PYRO_DIESEL_FLUID);
                    reagentReturn = new ItemStack(Items.GLASS_BOTTLE);
                } else if (reagent.is(ObsidiumMod.ASTRAL_VOLCANIC_CATALYST)) {
                    yield = 2000;
                    inputCost = 1000;
                    consumeReagentItem = true;
                    targetOutputFluid = FluidVariant.of(ObsidiumMod.PYRO_DIESEL_FLUID);
                } else if (reagent.is(Items.BLAZE_POWDER) || reagent.is(ObsidiumMod.VOLCANITE) || reagent.is(ObsidiumMod.VOLCANITE_INGOT)) {
                    yield = 1000;
                    inputCost = 1000;
                    consumeReagentItem = true;
                    targetOutputFluid = FluidVariant.of(ObsidiumMod.PYRO_DIESEL_FLUID);
                }
            }
        }

        boolean canReturnReagent = reagentReturn.isEmpty()
                || lavaOutput.isEmpty()
                || (ItemStack.isSameItemSameComponents(lavaOutput, reagentReturn) && lavaOutput.getCount() < lavaOutput.getMaxStackSize());

        boolean canDistill = yield > 0
                && !targetOutputFluid.isBlank()
                && canReturnReagent
                && entity.lavaAmount >= inputCost
                && (secondaryCost == 0 || entity.ethanolAmount >= secondaryCost)
                && (entity.distillateAmount == 0 || entity.outputFluid.equals(targetOutputFluid))
                && entity.distillateAmount + yield <= MAX_DISTILLATE
                && entity.energyStorage.amount >= RF_PER_TICK;

        boolean isWorking = false;
        if (canDistill) {
            entity.energyStorage.amount -= RF_PER_TICK;
            entity.progress++;
            isWorking = true;
            dirty = true;

            if (entity.progress >= MAX_PROGRESS) {
                entity.progress = 0;
                entity.lavaAmount -= inputCost;
                entity.syncFluidStorage();
                if (secondaryCost > 0) {
                    entity.ethanolAmount -= secondaryCost;
                    if (entity.ethanolAmount <= 0) {
                        entity.ethanolAmount = 0;
                        entity.secondaryFluid = FluidVariant.blank();
                    }
                }
                if (consumeReagentItem && !reagent.isEmpty()) {
                    reagent.shrink(1);
                }
                if (!reagentReturn.isEmpty()) {
                    if (lavaOutput.isEmpty()) {
                        entity.inventory.setItem(1, reagentReturn.copy());
                    } else {
                        lavaOutput.grow(1);
                    }
                }
                if (entity.distillateAmount == 0) {
                    entity.outputFluid = targetOutputFluid;
                }
                entity.distillateAmount = Math.min(MAX_DISTILLATE, entity.distillateAmount + yield);
                level.playSound(null, pos, net.valentin.obsidium.sound.ModSounds.DISTILLATION_COMPLETE, SoundSource.BLOCKS, 0.7F, 1.0F);
            }
        } else if (entity.progress > 0) {
            entity.progress = Math.max(0, entity.progress - 2);
            dirty = true;
        }

        // 3. Bottling Output Fluid (Slot 3 -> Slot 4)
        ItemStack emptyContainer = entity.inventory.getItem(3);
        ItemStack filledOutput = entity.inventory.getItem(4);
        if (!emptyContainer.isEmpty() && entity.distillateAmount > 0 && !entity.outputFluid.isBlank()) {
            if (emptyContainer.is(Items.BUCKET) && entity.distillateAmount >= 1000) {
                ItemStack bucketResult = ItemStack.EMPTY;
                if (entity.outputFluid.isOf(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID)) {
                    bucketResult = new ItemStack(ObsidiumMod.PYROCLASTIC_DISTILLATE_BUCKET, 1);
                } else if (entity.outputFluid.isOf(ObsidiumMod.BIODIESEL_FLUID)) {
                    bucketResult = new ItemStack(ObsidiumMod.BIODIESEL_BUCKET, 1);
                } else if (entity.outputFluid.isOf(ObsidiumMod.PYRO_DIESEL_FLUID)) {
                    bucketResult = new ItemStack(ObsidiumMod.PYRO_DIESEL_BUCKET, 1);
                }
                if (!bucketResult.isEmpty() && filledOutput.isEmpty()) {
                    entity.distillateAmount -= 1000;
                    if (entity.distillateAmount <= 0) entity.outputFluid = FluidVariant.blank();
                    emptyContainer.shrink(1);
                    entity.inventory.setItem(4, bucketResult);
                    dirty = true;
                }
            } else if (emptyContainer.is(Items.GLASS_BOTTLE) && entity.distillateAmount >= 250 && entity.outputFluid.isOf(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID)) {
                if (filledOutput.isEmpty() || (filledOutput.is(ObsidiumMod.PYROCLASTIC_FLASK) && filledOutput.getCount() < filledOutput.getMaxStackSize())) {
                    entity.distillateAmount -= 250;
                    if (entity.distillateAmount <= 0) entity.outputFluid = FluidVariant.blank();
                    emptyContainer.shrink(1);
                    if (filledOutput.isEmpty()) {
                        entity.inventory.setItem(4, new ItemStack(ObsidiumMod.PYROCLASTIC_FLASK, 1));
                    } else {
                        filledOutput.grow(1);
                    }
                    dirty = true;
                }
            }
        }

        // 4. Auto-Pump Output Fluid into adjacent or pipe-connected Generators / Chambers / Tanks!
        if (entity.distillateAmount > 0 && !entity.outputFluid.isBlank() && level.getGameTime() % 5 == 0) {
            int pumped = pumpDistillateToGenerators(level, pos, entity.outputFluid, Math.min(entity.distillateAmount, 500));
            if (pumped > 0) {
                entity.distillateAmount -= pumped;
                if (entity.distillateAmount <= 0) {
                    entity.distillateAmount = 0;
                    entity.outputFluid = FluidVariant.blank();
                }
                dirty = true;
            }
        }

        // 5. Sync WORKING blockstate
        if (state.getValue(VolcanicDistillationChamberBlock.WORKING) != isWorking) {
            level.setBlock(pos, state.setValue(VolcanicDistillationChamberBlock.WORKING, isWorking), 3);
            dirty = true;
        }

        if (dirty) {
            entity.setChanged();
        }

        VolcaniteStoragePipeBlockEntity.autoEjectMachineOutputs(level, pos, entity);
    }

    private static int pumpDistillateToGenerators(Level level, BlockPos startPos, FluidVariant fluid, int maxAmount) {
        int remaining = maxAmount;
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty() && visited.size() < 64 && remaining > 0) {
            BlockPos curr = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = curr.relative(dir);
                if (!visited.add(next)) continue;
                BlockEntity be = level.getBlockEntity(next);
                if (be instanceof SupremeVolcanicGeneratorBlockEntity gen && fluid.isOf(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID)) {
                    int accepted = gen.insertDistillate(remaining);
                    remaining -= accepted;
                    if (remaining <= 0) break;
                } else if (be instanceof VolcanicDieselGeneratorBlockEntity dieselGen && VolcanicDieselGeneratorBlockEntity.getFuelProfile(fluid) != null) {
                    int accepted = dieselGen.insertFluidInternal(fluid, remaining, false);
                    remaining -= accepted;
                    if (remaining <= 0) break;
                } else if (be instanceof VolcanicDistillationChamberBlockEntity otherChamber && !next.equals(startPos)
                        && (fluid.isOf(ObsidiumMod.BIODIESEL_FLUID) || fluid.isOf(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID))) {
                    int accepted = otherChamber.insertFluid(fluid, remaining, false);
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
                } else if (be instanceof VolcaniteStoragePipeBlockEntity
                        || be instanceof VolcaniteUniversalConduitBlockEntity
                        || be instanceof NaiadFluidPipeBlockEntity) {
                    queue.add(next);
                }
            }
        }
        return maxAmount - remaining;
    }

    @Override
    public Container getInventory() {
        return this.inventory;
    }

    @Override
    public boolean isInputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot == 0) return isValidInputBucket(stack);
        if (slot == 2) return isValidReagent(stack);
        if (slot == 3) return stack.is(Items.BUCKET) || stack.is(Items.GLASS_BOTTLE);
        return false;
    }

    @Override
    public boolean isOutputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 1 || slot == 4;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("Lava", lavaAmount);
        tag.putInt("EthanolAmount", ethanolAmount);
        tag.putInt("Distillate", distillateAmount);
        tag.putInt("Progress", progress);
        tag.putLong("Energy", energyStorage.amount);
        tag.putInt("InputFluidType", getInputFluidTypeId(inputFluid));
        tag.putInt("SecondaryFluidType", getSecondaryFluidTypeId(secondaryFluid));
        tag.putInt("OutputFluidType", getOutputFluidTypeId(outputFluid));
        MachineInventory.saveInventory(tag, inventory, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.lavaAmount = tag.getInt("Lava");
        this.ethanolAmount = tag.getInt("EthanolAmount");
        this.distillateAmount = tag.getInt("Distillate");
        this.progress = tag.getInt("Progress");
        this.energyStorage.amount = tag.getLong("Energy");
        int inType = tag.getInt("InputFluidType");
        this.inputFluid = switch (inType) {
            case 2 -> FluidVariant.of(ObsidiumMod.BIO_OIL_FLUID);
            case 3 -> FluidVariant.of(ObsidiumMod.BIODIESEL_FLUID);
            case 1 -> FluidVariant.of(Fluids.LAVA);
            default -> this.lavaAmount > 0 ? FluidVariant.of(Fluids.LAVA) : FluidVariant.blank();
        };
        int secType = tag.getInt("SecondaryFluidType");
        this.secondaryFluid = switch (secType) {
            case 1 -> FluidVariant.of(ObsidiumMod.BIO_ETHANOL_FLUID);
            case 2 -> FluidVariant.of(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID);
            default -> FluidVariant.blank();
        };
        int outType = tag.getInt("OutputFluidType");
        this.outputFluid = switch (outType) {
            case 2 -> FluidVariant.of(ObsidiumMod.BIODIESEL_FLUID);
            case 3 -> FluidVariant.of(ObsidiumMod.PYRO_DIESEL_FLUID);
            case 1 -> FluidVariant.of(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID);
            default -> this.distillateAmount > 0 ? FluidVariant.of(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID) : FluidVariant.blank();
        };
        MachineInventory.loadInventory(tag, this.inventory, provider);
        syncFluidStorage();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.volcanic_distillation_chamber");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new VolcanicDistillationChamberMenu(syncId, playerInventory, this.inventory, this.data);
    }
}

