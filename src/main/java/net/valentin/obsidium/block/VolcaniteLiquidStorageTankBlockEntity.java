package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.fluid.TransactionalFluidAccess;
import net.valentin.obsidium.item.VolcaniteBottleItem;
import net.valentin.obsidium.item.VolcaniteLiquidStorageDiskItem;
import team.reborn.energy.api.base.SimpleEnergyStorage;

public class VolcaniteLiquidStorageTankBlockEntity extends BlockEntity implements Container, MenuProvider, SingleSlotStorage<FluidVariant> {
    private NonNullList<ItemStack> items = NonNullList.withSize(6, ItemStack.EMPTY);
    public static final int BASE_TANK_CAPACITY = 16000; // 16 buckets base buffer
    private int internalFluidAmount = 0;
    private Fluid internalFluid = Fluids.EMPTY;

    private record FluidSnapshot(Fluid fluid, int amount, java.util.List<ItemStack> items) {}

    private final TransactionalFluidAccess<FluidSnapshot> transactionalFluid =
            new TransactionalFluidAccess<>(
                    () -> new FluidSnapshot(internalFluid, internalFluidAmount, items.stream().map(ItemStack::copy).toList()),
                    snapshot -> {
                        internalFluid = snapshot.fluid();
                        internalFluidAmount = snapshot.amount();
                        for (int i = 0; i < items.size(); i++) items.set(i, snapshot.items().get(i));
                    },
                    (fluid, amount) -> insertFluid(fluid, amount, false),
                    (fluid, amount) -> extractFluid(fluid, amount, false), this::setChanged);

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(50000, 500, 0) {
        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            int totalFluid = getTotalFluidAmount();
            int maxCap = getMaxFluidCapacity();
            if (index == 0) return totalFluid >> 16;
            if (index == 1) return totalFluid & 0xFFFF;
            if (index == 2) return maxCap >> 16;
            if (index == 3) return maxCap & 0xFFFF;
            if (index == 4) return getFluidTypeId();
            if (index == 5) return (int) (energyStorage.amount >> 16);
            if (index == 6) return (int) (energyStorage.amount & 0xFFFF);
            return 0;
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 7;
        }
    };

    public VolcaniteLiquidStorageTankBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.VOLCANITE_LIQUID_STORAGE_TANK_BE, pos, state);
    }

    private int getFluidTypeId() {
        Fluid f = getActiveFluid();
        if (f == Fluids.LAVA) return 1;
        if (f == Fluids.WATER) return 2;
        if (f == ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID) return 3;
        if (f == ObsidiumMod.STEAM_FLUID) return 4;
        return 0;
    }

    public Fluid getActiveFluid() {
        if (internalFluid != Fluids.EMPTY) return internalFluid;
        for (int i = 2; i < 6; i++) {
            ItemStack disk = items.get(i);
            if (!disk.isEmpty() && disk.getItem() instanceof VolcaniteLiquidStorageDiskItem) {
                FluidVariant fv = VolcaniteLiquidStorageDiskItem.getStoredFluid(disk);
                if (!fv.isBlank()) return fv.getFluid();
            }
        }
        return Fluids.EMPTY;
    }

    public int getTotalFluidAmount() {
        int total = internalFluidAmount;
        for (int i = 2; i < 6; i++) {
            ItemStack disk = items.get(i);
            if (!disk.isEmpty() && disk.getItem() instanceof VolcaniteLiquidStorageDiskItem) {
                total += VolcaniteLiquidStorageDiskItem.getStoredAmount(disk);
            }
        }
        return total;
    }

    public int getMaxFluidCapacity() {
        int total = BASE_TANK_CAPACITY;
        for (int i = 2; i < 6; i++) {
            ItemStack disk = items.get(i);
            if (!disk.isEmpty() && disk.getItem() instanceof VolcaniteLiquidStorageDiskItem diskItem) {
                total += diskItem.getCapacity();
            }
        }
        return total;
    }

    public int insertFluid(FluidVariant fluid, int amount_mB, boolean simulate) {
        if (fluid.isBlank() || amount_mB <= 0 || !fluid.getComponents().isEmpty()) return 0;
        Fluid active = getActiveFluid();
        if (active != Fluids.EMPTY && active != fluid.getFluid()) return 0;

        int remaining = amount_mB;
        int totalInserted = 0;

        // 1. Insert into inserted liquid disks first
        for (int i = 2; i < 6; i++) {
            ItemStack disk = items.get(i);
            if (!disk.isEmpty() && disk.getItem() instanceof VolcaniteLiquidStorageDiskItem) {
                int inserted = VolcaniteLiquidStorageDiskItem.insert(disk, fluid, remaining, simulate);
                remaining -= inserted;
                totalInserted += inserted;
                if (!simulate && inserted > 0) setChanged();
                if (remaining <= 0) break;
            }
        }

        // 2. Insert into internal buffer
        if (remaining > 0) {
            int space = BASE_TANK_CAPACITY - internalFluidAmount;
            if (space > 0) {
                int toBuffer = Math.min(space, remaining);
                totalInserted += toBuffer;
                if (!simulate) {
                    internalFluid = fluid.getFluid();
                    internalFluidAmount += toBuffer;
                    setChanged();
                }
            }
        }

        return totalInserted;
    }

    public int extractFluid(FluidVariant fluid, int amount_mB, boolean simulate) {
        if (amount_mB <= 0 || (!fluid.isBlank() && !fluid.getComponents().isEmpty())) return 0;
        Fluid active = getActiveFluid();
        if (active == Fluids.EMPTY) return 0;
        if (!fluid.isBlank() && fluid.getFluid() != active) return 0;

        int remaining = amount_mB;
        int totalExtracted = 0;

        // 1. Extract from internal buffer first
        if (internalFluidAmount > 0) {
            int toExtract = Math.min(internalFluidAmount, remaining);
            remaining -= toExtract;
            totalExtracted += toExtract;
            if (!simulate) {
                internalFluidAmount -= toExtract;
                if (internalFluidAmount <= 0) internalFluid = Fluids.EMPTY;
                setChanged();
            }
        }

        // 2. Extract from disks
        if (remaining > 0) {
            for (int i = 2; i < 6; i++) {
                ItemStack disk = items.get(i);
                if (!disk.isEmpty() && disk.getItem() instanceof VolcaniteLiquidStorageDiskItem) {
                    int extracted = VolcaniteLiquidStorageDiskItem.extract(disk, fluid.isBlank() ? FluidVariant.of(active) : fluid, remaining, simulate);
                    remaining -= extracted;
                    totalExtracted += extracted;
                    if (!simulate && extracted > 0) setChanged();
                    if (remaining <= 0) break;
                }
            }
        }

        return totalExtracted;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VolcaniteLiquidStorageTankBlockEntity entity) {
        if (level.isClientSide()) return;
        boolean dirty = false;

        // Handle Slot 0 Input (Bucket/Bottle drainage or fill)
        ItemStack input = entity.items.get(0);
        ItemStack output = entity.items.get(1);

        if (!input.isEmpty()) {
            // Lava Bucket input
            if (input.is(Items.LAVA_BUCKET)) {
                if (entity.insertFluid(FluidVariant.of(Fluids.LAVA), 1000, true) == 1000) {
                    if (output.isEmpty() || (output.is(Items.BUCKET) && output.getCount() < output.getMaxStackSize())) {
                        entity.insertFluid(FluidVariant.of(Fluids.LAVA), 1000, false);
                        input.shrink(1);
                        if (output.isEmpty()) entity.items.set(1, new ItemStack(Items.BUCKET));
                        else output.grow(1);
                        dirty = true;
                    }
                }
            }
            // Water Bucket input
            else if (input.is(Items.WATER_BUCKET)) {
                if (entity.insertFluid(FluidVariant.of(Fluids.WATER), 1000, true) == 1000) {
                    if (output.isEmpty() || (output.is(Items.BUCKET) && output.getCount() < output.getMaxStackSize())) {
                        entity.insertFluid(FluidVariant.of(Fluids.WATER), 1000, false);
                        input.shrink(1);
                        if (output.isEmpty()) entity.items.set(1, new ItemStack(Items.BUCKET));
                        else output.grow(1);
                        dirty = true;
                    }
                }
            }
            // Pyroclastic Distillate Bucket input
            else if (input.is(ObsidiumMod.PYROCLASTIC_DISTILLATE_BUCKET)) {
                if (entity.insertFluid(FluidVariant.of(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID), 1000, true) == 1000) {
                    if (output.isEmpty() || (output.is(Items.BUCKET) && output.getCount() < output.getMaxStackSize())) {
                        entity.insertFluid(FluidVariant.of(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID), 1000, false);
                        input.shrink(1);
                        if (output.isEmpty()) entity.items.set(1, new ItemStack(Items.BUCKET));
                        else output.grow(1);
                        dirty = true;
                    }
                }
            }
            // Pyroclastic Flask input (250 mB)
            else if (input.is(ObsidiumMod.PYROCLASTIC_FLASK)) {
                if (entity.insertFluid(FluidVariant.of(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID), 250, true) == 250) {
                    if (output.isEmpty() || (output.is(Items.GLASS_BOTTLE) && output.getCount() < output.getMaxStackSize())) {
                        entity.insertFluid(FluidVariant.of(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID), 250, false);
                        input.shrink(1);
                        if (output.isEmpty()) entity.items.set(1, new ItemStack(Items.GLASS_BOTTLE));
                        else output.grow(1);
                        dirty = true;
                    }
                }
            }
            // Steam Bucket input
            else if (input.is(ObsidiumMod.STEAM_BUCKET)) {
                if (entity.insertFluid(FluidVariant.of(ObsidiumMod.STEAM_FLUID), 1000, true) == 1000) {
                    if (output.isEmpty() || (output.is(Items.BUCKET) && output.getCount() < output.getMaxStackSize())) {
                        entity.insertFluid(FluidVariant.of(ObsidiumMod.STEAM_FLUID), 1000, false);
                        input.shrink(1);
                        if (output.isEmpty()) entity.items.set(1, new ItemStack(Items.BUCKET));
                        else output.grow(1);
                        dirty = true;
                    }
                }
            }
            // Volcanite Bottle input (drain or fill)
            else if (input.getItem() instanceof VolcaniteBottleItem bottle) {
                net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant bottleFluid = VolcaniteBottleItem.getFluid(input);
                int bottleAmount = VolcaniteBottleItem.getFluidAmount(input);
                Fluid active = entity.getActiveFluid();

                // If bottle has space, and tank has fluid of the same type (or bottle is empty): FILL bottle from tank!
                if (bottleAmount < bottle.getCapacity() && active != Fluids.EMPTY && entity.getTotalFluidAmount() > 0 &&
                    (bottleFluid.isBlank() || bottleFluid.getFluid() == active)) {
                    int space = bottle.getCapacity() - bottleAmount;
                    int toExtract = Math.min(space, Math.min(1000, entity.getTotalFluidAmount()));
                    int extracted = entity.extractFluid(FluidVariant.of(active), toExtract, false);
                    if (extracted > 0) {
                        VolcaniteBottleItem.setFluid(input, FluidVariant.of(active), bottleAmount + extracted);
                        dirty = true;
                    }
                }
                // Otherwise if bottle has fluid and tank has space: DRAIN bottle into tank!
                else if (bottleAmount > 0 && (active == Fluids.EMPTY || active == bottleFluid.getFluid())) {
                    int canInsert = entity.insertFluid(bottleFluid, Math.min(1000, bottleAmount), false);
                    if (canInsert > 0) {
                        VolcaniteBottleItem.setFluid(input, bottleFluid, bottleAmount - canInsert);
                        dirty = true;
                    }
                }
            }
            // Empty bucket fill from tank
            else if (input.is(Items.BUCKET) && entity.getTotalFluidAmount() >= 1000) {
                Fluid active = entity.getActiveFluid();
                Item filledBucket = active.getBucket();
                if (filledBucket != null && filledBucket != Items.AIR) {
                    if (output.isEmpty() || (output.is(filledBucket) && output.getCount() < output.getMaxStackSize())) {
                        entity.extractFluid(FluidVariant.of(active), 1000, false);
                        input.shrink(1);
                        if (output.isEmpty()) entity.items.set(1, new ItemStack(filledBucket));
                        else output.grow(1);
                        dirty = true;
                    }
                }
            }
            // Empty glass bottle fill from tank (Pyroclastic Flask)
            else if (input.is(Items.GLASS_BOTTLE) && entity.getTotalFluidAmount() >= 250 && entity.getActiveFluid() == ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID) {
                if (output.isEmpty() || (output.is(ObsidiumMod.PYROCLASTIC_FLASK) && output.getCount() < output.getMaxStackSize())) {
                    entity.extractFluid(FluidVariant.of(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID), 250, false);
                    input.shrink(1);
                    if (output.isEmpty()) entity.items.set(1, new ItemStack(ObsidiumMod.PYROCLASTIC_FLASK));
                    else output.grow(1);
                    dirty = true;
                }
            }
        }

        if (dirty) entity.setChanged();
    }

    // --- SingleSlotStorage Implementation (for Fluid Pipes / Conduits) ---
    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        int amount = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, maxAmount) / 81L);
        return transactionalFluid.transfer(resource, amount, true, transaction) * 81L;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        int amount = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, maxAmount) / 81L);
        return transactionalFluid.transfer(resource, amount, false, transaction) * 81L;
    }

    @Override
    public boolean isResourceBlank() {
        return getActiveFluid() == Fluids.EMPTY || getTotalFluidAmount() <= 0;
    }

    @Override
    public FluidVariant getResource() {
        Fluid f = getActiveFluid();
        return f == Fluids.EMPTY ? FluidVariant.blank() : FluidVariant.of(f);
    }

    @Override
    public long getAmount() {
        return getTotalFluidAmount() * 81L;
    }

    @Override
    public long getCapacity() {
        return getMaxFluidCapacity() * 81L;
    }

    // --- Container & MenuProvider ---
    @Override
    public int getContainerSize() { return 6; }

    @Override
    public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }

    @Override
    public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = ContainerHelper.removeItem(items, slot, amount);
        if (!stack.isEmpty()) setChanged();
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (slot == 0) {
            return stack.is(Items.LAVA_BUCKET)
                    || stack.is(Items.WATER_BUCKET)
                    || stack.is(Items.BUCKET)
                    || stack.is(ObsidiumMod.PYROCLASTIC_DISTILLATE_BUCKET)
                    || stack.is(ObsidiumMod.PYROCLASTIC_FLASK)
                    || stack.is(ObsidiumMod.STEAM_BUCKET)
                    || stack.getItem() instanceof VolcaniteBottleItem;
        }
        if (slot >= 2 && slot < 6) {
            return stack.getItem() instanceof VolcaniteLiquidStorageDiskItem;
        }
        return false;
    }

    @Override
    public void clearContent() { items.clear(); setChanged(); }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.volcanite_liquid_storage_tank");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return new VolcaniteLiquidStorageTankMenu(syncId, inv, this, this.data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, this.items, provider);
        tag.putLong("Energy", energyStorage.amount);
        tag.putInt("InternalAmount", internalFluidAmount);
        tag.putString("InternalFluid", BuiltInRegistries.FLUID.getKey(internalFluid).toString());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.items = NonNullList.withSize(6, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, provider);
        this.energyStorage.amount = tag.getLong("Energy");
        this.internalFluidAmount = tag.getInt("InternalAmount");
        this.internalFluid = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(tag.getString("InternalFluid")));
    }
}
