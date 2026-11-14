package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.ItemTags;
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

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class DionysianDistilleryBlockEntity extends BlockEntity implements MenuProvider, InventoryHolder, MachineInventory, WorldlyContainer {

    public static final int INVENTORY_SIZE = 5;
    // Slot 0: Fruit / Biomass Input
    // Slot 1: Fuel Input
    // Slot 2: Empty Glass Bottle Input
    // Slot 3: Volcanic Țuică Output
    // Slot 4: Volcanic Ash Byproduct Output

    public static final int MAX_PROGRESS = 120; // 6 seconds per batch
    public static final int MAX_FLUID = 16_000;

    private final NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private int burnTime = 0;
    private int totalBurnTime = 0;
    private int progress = 0;
    private int fluidAmount = 0;
    private FluidVariant storedFluid = FluidVariant.blank();

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> MAX_PROGRESS;
                case 2 -> burnTime;
                case 3 -> totalBurnTime;
                case 4 -> fluidAmount;
                case 5 -> getFluidTypeId(storedFluid);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 2 -> burnTime = value;
                case 3 -> totalBurnTime = value;
                case 4 -> fluidAmount = value;
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public DionysianDistilleryBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.DIONYSIAN_DISTILLERY_BE, pos, state);
    }

    public static boolean isFruit(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(ObsidiumMod.DIONYSIAN_POMACE)
                || stack.is(ObsidiumMod.HERMES_COPPER_FRUIT)
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

    public static boolean isBioOilInput(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(ObsidiumMod.DIONYSIAN_POMACE)
                || stack.is(ObsidiumMod.FOLIAGE_BALE_ITEM)
                || stack.is(ItemTags.LEAVES)
                || stack.is(Items.WHEAT_SEEDS)
                || stack.is(Items.MELON_SEEDS)
                || stack.is(Items.PUMPKIN_SEEDS)
                || stack.is(Items.BEETROOT_SEEDS)
                || stack.is(Items.TORCHFLOWER_SEEDS)
                || stack.is(Items.PITCHER_POD);
    }

    public static boolean isBioEthanolInput(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(ObsidiumMod.DIONYSIAN_POMACE)) return false;
        return isFruit(stack)
                || stack.is(Items.SUGAR)
                || stack.is(Items.CARROT)
                || stack.is(Items.POTATO)
                || stack.is(Items.BEETROOT);
    }

    public static boolean isAnyDistillableInput(ItemStack stack) {
        return isFruit(stack) || isBioOilInput(stack) || isBioEthanolInput(stack);
    }

    @Nullable
    public static FluidVariant getBiofuelOutputVariant(ItemStack stack) {
        if (isBioOilInput(stack)) return FluidVariant.of(ObsidiumMod.BIO_OIL_FLUID);
        if (isBioEthanolInput(stack)) return FluidVariant.of(ObsidiumMod.BIO_ETHANOL_FLUID);
        return null;
    }

    public static int getBiofuelYieldMb(ItemStack stack) {
        if (stack.is(ObsidiumMod.FOLIAGE_BALE_ITEM)) return 1000;
        if (stack.is(ObsidiumMod.DIONYSIAN_POMACE)) return 400;
        if (stack.is(ObsidiumMod.HEPHAESTUS_VOLCANITE_FRUIT) || stack.is(ObsidiumMod.TARTAREAN_NETHERITE_FRUIT)) return 500;
        return 250;
    }

    public static int getFluidTypeId(FluidVariant variant) {
        if (variant == null || variant.isBlank()) return 0;
        if (variant.isOf(ObsidiumMod.BIO_OIL_FLUID)) return 1;
        if (variant.isOf(ObsidiumMod.BIO_ETHANOL_FLUID)) return 2;
        return 0;
    }

    public FluidVariant getStoredFluid() {
        return storedFluid;
    }

    public int getFluidAmount() {
        return fluidAmount;
    }

    public int extractFluid(FluidVariant variant, int maxExtract, boolean simulate) {
        if (fluidAmount <= 0 || !storedFluid.equals(variant) || maxExtract <= 0) return 0;
        int toExtract = Math.min(fluidAmount, maxExtract);
        if (toExtract > 0 && !simulate) {
            fluidAmount -= toExtract;
            if (fluidAmount <= 0) {
                fluidAmount = 0;
                storedFluid = FluidVariant.blank();
            }
            setChanged();
            syncToClient();
        }
        return toExtract;
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static int getAshYield(ItemStack fruit) {
        if (fruit.is(ObsidiumMod.HEPHAESTUS_VOLCANITE_FRUIT)) return 3;
        if (fruit.is(ObsidiumMod.ELECTRUM_FRUIT) || fruit.is(ObsidiumMod.MIDAS_GOLD_FRUIT) || fruit.is(ObsidiumMod.ARTEMIS_SILVER_FRUIT)) return 2;
        return 1;
    }

    public static ItemStack getMineralByproduct(ItemStack fruit) {
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
            return new ItemStack(Items.GOLD_NUGGET, 2);
        } else if (fruit.is(ObsidiumMod.TARTAREAN_NETHERITE_FRUIT)) {
            return new ItemStack(ObsidiumMod.NETHERITE_NUGGET, 2);
        } else if (fruit.is(Items.GOLDEN_APPLE) || fruit.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            return new ItemStack(Items.GOLD_NUGGET, 4);
        } else {
            return new ItemStack(ObsidiumMod.VOLCANIC_ASH, getAshYield(fruit));
        }
    }

    public static boolean isFuel(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(ObsidiumMod.VOLCANIC_ASH)) return true;
        return AbstractFurnaceBlockEntity.isFuel(stack);
    }

    public static int getBurnDuration(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.is(ObsidiumMod.VOLCANIC_ASH)) return 400;
        if (stack.is(Items.LAVA_BUCKET)) return 20000;
        return AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0);
    }

    public void tickServer(Level level, BlockPos pos, BlockState state) {
        boolean changed = false;
        boolean needClientSync = false;

        if (burnTime > 0) {
            burnTime--;
            changed = true;
        }

        ItemStack fruit = items.get(0);
        ItemStack fuel = items.get(1);
        ItemStack bottles = items.get(2);
        ItemStack outputTuica = items.get(3);
        ItemStack outputAsh = items.get(4);

        if (bottles.is(Items.BUCKET) && fluidAmount >= 1000 && outputTuica.isEmpty()) {
            ItemStack filledBucket = ItemStack.EMPTY;
            if (storedFluid.isOf(ObsidiumMod.BIO_OIL_FLUID)) {
                filledBucket = new ItemStack(ObsidiumMod.BIO_OIL_BUCKET);
            } else if (storedFluid.isOf(ObsidiumMod.BIO_ETHANOL_FLUID)) {
                filledBucket = new ItemStack(ObsidiumMod.BIO_ETHANOL_BUCKET);
            }
            if (!filledBucket.isEmpty()) {
                fluidAmount -= 1000;
                if (fluidAmount <= 0) {
                    fluidAmount = 0;
                    storedFluid = FluidVariant.blank();
                }
                bottles.shrink(1);
                items.set(3, filledBucket);
                changed = true;
                needClientSync = true;
            }
        }

        boolean bottleMode = isFruit(fruit) && !bottles.isEmpty() && (bottles.is(Items.GLASS_BOTTLE) || bottles.is(ObsidiumMod.EMPTY_TUICA_BOTTLE_1L));

        ItemStack expectedTuica;
        if (fruit.is(Items.APPLE) || fruit.is(Items.GOLDEN_APPLE) || fruit.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            expectedTuica = new ItemStack(ObsidiumMod.VOLCANIC_CIDER);
        } else if (bottles.is(ObsidiumMod.EMPTY_TUICA_BOTTLE_1L)) {
            expectedTuica = new ItemStack(ObsidiumMod.VOLCANIC_TUICA_1L);
        } else {
            expectedTuica = new ItemStack(ObsidiumMod.VOLCANIC_TUICA);
        }
        ItemStack expectedByproduct = isFruit(fruit) ? getMineralByproduct(fruit) : new ItemStack(ObsidiumMod.VOLCANIC_ASH, 1);

        boolean canDistillBottle = bottleMode
                && (outputTuica.isEmpty() || (ItemStack.isSameItemSameComponents(outputTuica, expectedTuica) && outputTuica.getCount() + expectedTuica.getCount() <= outputTuica.getMaxStackSize()))
                && (outputAsh.isEmpty() || (ItemStack.isSameItemSameComponents(outputAsh, expectedByproduct) && outputAsh.getCount() + expectedByproduct.getCount() <= outputAsh.getMaxStackSize()));

        FluidVariant biofuelOut = getBiofuelOutputVariant(fruit);
        int biofuelYield = getBiofuelYieldMb(fruit);
        boolean fluidMode = !bottleMode && biofuelOut != null
                && (fluidAmount == 0 || storedFluid.equals(biofuelOut))
                && fluidAmount + biofuelYield <= MAX_FLUID
                && (outputAsh.isEmpty() || (ItemStack.isSameItemSameComponents(outputAsh, expectedByproduct) && outputAsh.getCount() + expectedByproduct.getCount() <= outputAsh.getMaxStackSize()));

        boolean canDistill = canDistillBottle || fluidMode;

        if (burnTime <= 0 && canDistill) {
            if (isFuel(fuel)) {
                int duration = getBurnDuration(fuel);
                if (duration > 0) {
                    burnTime = totalBurnTime = duration;
                    if (fuel.is(Items.LAVA_BUCKET)) {
                        items.set(1, new ItemStack(Items.BUCKET));
                    } else {
                        fuel.shrink(1);
                    }
                    changed = true;
                }
            }
        }

        boolean isBurning = burnTime > 0;

        if (isBurning && canDistill) {
            progress++;
            changed = true;

            if (progress >= MAX_PROGRESS) {
                progress = 0;

                if (canDistillBottle) {
                    fruit.shrink(1);
                    bottles.shrink(1);

                    if (outputTuica.isEmpty()) {
                        items.set(3, expectedTuica.copy());
                    } else {
                        outputTuica.grow(expectedTuica.getCount());
                    }
                } else if (fluidMode) {
                    fruit.shrink(1);
                    if (fluidAmount == 0) {
                        storedFluid = biofuelOut;
                    }
                    fluidAmount = Math.min(MAX_FLUID, fluidAmount + biofuelYield);
                    needClientSync = true;
                }

                if (outputAsh.isEmpty()) {
                    items.set(4, expectedByproduct.copy());
                } else {
                    outputAsh.grow(expectedByproduct.getCount());
                }

                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BREWING_STAND_BREW, net.minecraft.sounds.SoundSource.BLOCKS, 0.7F, 1.0F);
            }
        } else {
            if (progress > 0) {
                progress = Math.max(0, progress - 2);
                changed = true;
            }
        }

        boolean isLitNow = burnTime > 0;
        if (state.hasProperty(DionysianDistilleryBlock.LIT) && state.getValue(DionysianDistilleryBlock.LIT) != isLitNow) {
            level.setBlock(pos, state.setValue(DionysianDistilleryBlock.LIT, isLitNow), 3);
            needClientSync = true;
        }

        // Auto-pump produced Bio-Ethanol / Bio-Oil into adjacent or pipe-connected Volcanic Distillation Chambers, Generators, or Tanks
        if (fluidAmount > 0 && !storedFluid.isBlank() && level.getGameTime() % 5 == 0) {
            int pumped = pumpBiofuelToTargets(level, pos, storedFluid, Math.min(fluidAmount, 500));
            if (pumped > 0) {
                fluidAmount -= pumped;
                if (fluidAmount <= 0) {
                    fluidAmount = 0;
                    storedFluid = FluidVariant.blank();
                }
                changed = true;
                needClientSync = true;
            }
        }

        if (changed) {
            setChanged();
        }
        if (needClientSync) {
            syncToClient();
        }

        VolcaniteStoragePipeBlockEntity.autoEjectMachineOutputs(level, pos, this);
    }

    private static int pumpBiofuelToTargets(Level level, BlockPos startPos, FluidVariant fluid, int maxAmount) {
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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putInt("BurnTime", this.burnTime);
        tag.putInt("TotalBurnTime", this.totalBurnTime);
        tag.putInt("Progress", this.progress);
        tag.putInt("FluidAmount", this.fluidAmount);
        tag.putInt("FluidType", getFluidTypeId(this.storedFluid));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items.clear();
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.burnTime = tag.getInt("BurnTime");
        this.totalBurnTime = tag.getInt("TotalBurnTime");
        this.progress = tag.getInt("Progress");
        this.fluidAmount = tag.getInt("FluidAmount");
        int fType = tag.getInt("FluidType");
        this.storedFluid = switch (fType) {
            case 1 -> FluidVariant.of(ObsidiumMod.BIO_OIL_FLUID);
            case 2 -> FluidVariant.of(ObsidiumMod.BIO_ETHANOL_FLUID);
            default -> FluidVariant.blank();
        };
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.dionysian_distillery");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new DionysianDistilleryMenu(syncId, playerInventory, this, this.dataAccess);
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
        if (slot == 0) return isAnyDistillableInput(stack);
        if (slot == 1) return isFuel(stack);
        if (slot == 2) return stack.is(Items.GLASS_BOTTLE) || stack.is(ObsidiumMod.EMPTY_TUICA_BOTTLE_1L) || stack.is(Items.BUCKET);
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return new int[]{ 3, 4, 1 }; // Output tuica/bucket, ash, or empty bucket
        } else if (side == Direction.UP) {
            return new int[]{ 0, 2 }; // Fruit/biomass and bottles/buckets from top
        } else {
            return new int[]{ 1, 0, 2 }; // Fuel from sides
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot == 0) return isAnyDistillableInput(stack);
        if (slot == 1) return isFuel(stack);
        if (slot == 2) return stack.is(Items.GLASS_BOTTLE) || stack.is(ObsidiumMod.EMPTY_TUICA_BOTTLE_1L) || stack.is(Items.BUCKET);
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot == 3 || slot == 4) return true;
        if (slot == 1 && stack.is(Items.BUCKET)) return true;
        return false;
    }

    @Override
    public boolean isInputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 0 || slot == 1 || slot == 2;
    }

    @Override
    public boolean isOutputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 3 || slot == 4;
    }
}
