package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.fluid.TransactionalFluidAccess;
import net.valentin.obsidium.item.VolcaniteBottleItem;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class LiquidTankBlockEntity extends BlockEntity implements MenuProvider, SingleSlotStorage<FluidVariant>, InventoryHolder, MachineInventory {
    protected FluidVariant storedFluid = FluidVariant.blank();
    protected int fluidAmount = 0; // in mB
    protected int maxCapacity = 16000; // 16 buckets base

    private record FluidSnapshot(FluidVariant fluid, int amount) {}

    private final TransactionalFluidAccess<FluidSnapshot> transactionalFluid =
            new TransactionalFluidAccess<>(
                    () -> new FluidSnapshot(storedFluid, fluidAmount),
                    snapshot -> { storedFluid = snapshot.fluid(); fluidAmount = snapshot.amount(); },
                    (fluid, amount) -> applyTransactionalFluid(fluid, amount, true),
                    (fluid, amount) -> applyTransactionalFluid(fluid, amount, false),
                    this::syncAndMarkDirty);

    private boolean fluidTransactionActive;

    private int applyTransactionalFluid(FluidVariant fluid, int amount, boolean inserting) {
        boolean previous = fluidTransactionActive;
        fluidTransactionActive = true;
        try {
            return inserting ? insertFluid(fluid, amount, false) : extractFluid(fluid, amount, false);
        } finally {
            fluidTransactionActive = previous;
        }
    }

    private final SimpleContainer inventory = new SimpleContainer(2) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot == 0;
        }
    };

    @Override
    public boolean isInputSlot(int slot, ItemStack stack, @org.jetbrains.annotations.Nullable Direction side) {
        return slot == 0;
    }

    @Override
    public boolean isOutputSlot(int slot, ItemStack stack, @org.jetbrains.annotations.Nullable Direction side) {
        return slot == 1;
    }

    @Override
    public Container getInventory() {
        return this.inventory;
    }

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index == 0) return (fluidAmount >> 16);
            if (index == 1) return (fluidAmount & 0xFFFF);
            if (index == 2) return (maxCapacity >> 16);
            if (index == 3) return (maxCapacity & 0xFFFF);
            if (index == 4) {
                if (storedFluid.isBlank()) return 0;
                return BuiltInRegistries.FLUID.getId(storedFluid.getFluid());
            }
            return 0;
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() { return 5; }
    };

    public LiquidTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public FluidVariant getStoredFluid() {
        return this.storedFluid;
    }

    public int getFluidAmount() {
        return this.fluidAmount;
    }

    public int getMaxCapacity() {
        return this.maxCapacity;
    }

    public boolean canAccept(FluidVariant variant) {
        if (variant.isBlank()) return false;
        return this.storedFluid.isBlank() || this.storedFluid.equals(variant);
    }

    public int insertFluid(FluidVariant variant, int amount_mB, boolean simulate) {
        if (!canAccept(variant) || amount_mB <= 0) return 0;
        int space = this.maxCapacity - this.fluidAmount;
        int toInsert = Math.min(space, amount_mB);
        if (toInsert > 0 && !simulate) {
            this.storedFluid = variant;
            this.fluidAmount += toInsert;
            syncAndMarkDirty();
        }
        return toInsert;
    }

    public int extractFluid(FluidVariant variant, int amount_mB, boolean simulate) {
        if (this.storedFluid.isBlank() || this.fluidAmount <= 0 || amount_mB <= 0) return 0;
        if (!this.storedFluid.equals(variant)) return 0;
        int toExtract = Math.min(this.fluidAmount, amount_mB);
        if (toExtract > 0 && !simulate) {
            this.fluidAmount -= toExtract;
            if (this.fluidAmount <= 0) {
                this.fluidAmount = 0;
                this.storedFluid = FluidVariant.blank();
            }
            syncAndMarkDirty();
        }
        return toExtract;
    }

    public void syncAndMarkDirty() {
        if (fluidTransactionActive) return;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // Fabric Transfer API implementation (SingleSlotStorage<FluidVariant>)
    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        return transferFluid(resource, maxAmount, true, transaction);
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        return transferFluid(resource, maxAmount, false, transaction);
    }

    private long transferFluid(FluidVariant resource, long maxAmount, boolean inserting, TransactionContext transaction) {
        int amount = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, maxAmount) / 81L);
        return transactionalFluid.transfer(resource, amount, inserting, transaction) * 81L;
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
        return (long) this.maxCapacity * 81L;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LiquidTankBlockEntity entity) {
        if (level.isClientSide()) return;

        // 1. Process inventory (slot 0 input, slot 1 output)
        ItemStack input = entity.inventory.getItem(0);
        ItemStack output = entity.inventory.getItem(1);

        if (!input.isEmpty()) {
            // A) Filled bucket / flask -> empty container
            FluidVariant bucketFluidVariant = FluidVariant.blank();
            int containerFluidAmount = 1000;
            Item emptyReturnItem = Items.BUCKET;
            if (input.is(Items.WATER_BUCKET)) {
                bucketFluidVariant = FluidVariant.of(Fluids.WATER);
            } else if (input.is(Items.LAVA_BUCKET)) {
                bucketFluidVariant = FluidVariant.of(Fluids.LAVA);
            } else if (input.is(ObsidiumMod.PYROCLASTIC_DISTILLATE_BUCKET)) {
                bucketFluidVariant = FluidVariant.of(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID);
            } else if (input.is(ObsidiumMod.STEAM_BUCKET)) {
                bucketFluidVariant = FluidVariant.of(ObsidiumMod.STEAM_FLUID);
            } else if (input.is(ObsidiumMod.PYROCLASTIC_FLASK)) {
                bucketFluidVariant = FluidVariant.of(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID);
                containerFluidAmount = 250;
                emptyReturnItem = Items.GLASS_BOTTLE;
            } else if (input.is(ObsidiumMod.DEW_DROPLET)) {
                bucketFluidVariant = FluidVariant.of(Fluids.WATER);
                containerFluidAmount = 250;
                emptyReturnItem = Items.AIR;
            } else if (input.is(ObsidiumMod.MAGMA_DROPLET)) {
                bucketFluidVariant = FluidVariant.of(Fluids.LAVA);
                containerFluidAmount = 250;
                emptyReturnItem = Items.AIR;
            }

            if (!bucketFluidVariant.isBlank() && entity.canAccept(bucketFluidVariant) && entity.fluidAmount + containerFluidAmount <= entity.maxCapacity) {
                if (emptyReturnItem == Items.AIR || output.isEmpty() || (output.is(emptyReturnItem) && output.getCount() < output.getMaxStackSize())) {
                    entity.storedFluid = bucketFluidVariant;
                    entity.fluidAmount += containerFluidAmount;
                    input.shrink(1);
                    if (emptyReturnItem != Items.AIR) {
                        if (output.isEmpty()) {
                            entity.inventory.setItem(1, new ItemStack(emptyReturnItem));
                        } else {
                            output.grow(1);
                        }
                    }
                    entity.syncAndMarkDirty();
                }
            }
            // B) Empty bucket -> filled bucket (Lava, Water, Pyroclastic Distillate, Steam)
            else if (input.is(Items.BUCKET) && entity.fluidAmount >= 1000 && !entity.storedFluid.isBlank()) {
                Fluid fluid = entity.storedFluid.getFluid();
                Item bucketResult = fluid.getBucket();
                if (bucketResult != null && bucketResult != Items.AIR) {
                    ItemStack filledBucket = new ItemStack(bucketResult);
                    if (output.isEmpty() || (ItemStack.isSameItem(output, filledBucket) && output.getCount() < output.getMaxStackSize())) {
                        entity.fluidAmount -= 1000;
                        if (entity.fluidAmount <= 0) {
                            entity.fluidAmount = 0;
                            entity.storedFluid = FluidVariant.blank();
                        }
                        input.shrink(1);
                        if (output.isEmpty()) {
                            entity.inventory.setItem(1, filledBucket);
                        } else {
                            output.grow(1);
                        }
                        entity.syncAndMarkDirty();
                    }
                }
            }
            // B2) Empty Glass Bottle -> Pyroclastic Flask (250 mB)
            else if (input.is(Items.GLASS_BOTTLE) && entity.fluidAmount >= 250 && entity.storedFluid.isOf(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID)) {
                ItemStack filledFlask = new ItemStack(ObsidiumMod.PYROCLASTIC_FLASK);
                if (output.isEmpty() || (ItemStack.isSameItem(output, filledFlask) && output.getCount() < output.getMaxStackSize())) {
                    entity.fluidAmount -= 250;
                    if (entity.fluidAmount <= 0) {
                        entity.fluidAmount = 0;
                        entity.storedFluid = FluidVariant.blank();
                    }
                    input.shrink(1);
                    if (output.isEmpty()) {
                        entity.inventory.setItem(1, filledFlask);
                    } else {
                        output.grow(1);
                    }
                    entity.syncAndMarkDirty();
                }
            }
            // C) Volcanite Bottle
            else if (input.getItem() instanceof VolcaniteBottleItem bottle) {
                FluidVariant bottleFluid = VolcaniteBottleItem.getFluid(input);
                int bottleAmount = VolcaniteBottleItem.getFluidAmount(input);
                int maxCap = bottle.getCapacity();

                // If bottle has fluid and tank has space -> drain bottle
                if (!bottleFluid.isBlank() && bottleAmount > 0 && entity.canAccept(bottleFluid) && entity.fluidAmount < entity.maxCapacity) {
                    int toTransfer = Math.min(bottleAmount, entity.maxCapacity - entity.fluidAmount);
                    entity.storedFluid = bottleFluid;
                    entity.fluidAmount += toTransfer;
                    int newBottleAmount = bottleAmount - toTransfer;
                    if (newBottleAmount <= 0) {
                        VolcaniteBottleItem.setFluid(input, FluidVariant.blank(), 0);
                    } else {
                        VolcaniteBottleItem.setFluid(input, bottleFluid, newBottleAmount);
                    }
                    entity.syncAndMarkDirty();
                }
                // If tank has fluid and bottle has space -> fill bottle
                else if (!entity.storedFluid.isBlank() && entity.fluidAmount > 0 &&
                        (bottleFluid.isBlank() || bottleFluid.equals(entity.storedFluid)) && bottleAmount < maxCap) {
                    int toTransfer = Math.min(entity.fluidAmount, maxCap - bottleAmount);
                    VolcaniteBottleItem.setFluid(input, entity.storedFluid, bottleAmount + toTransfer);
                    entity.fluidAmount -= toTransfer;
                    if (entity.fluidAmount <= 0) {
                        entity.fluidAmount = 0;
                        entity.storedFluid = FluidVariant.blank();
                    }
                    entity.syncAndMarkDirty();
                }
            }
        }

        // 2. Auto-pumping to adjacent containers / pipes (up to 50 mB per tick)
        if (entity.fluidAmount > 0 && !entity.storedFluid.isBlank()) {
            int maxTransfer_mB = 50;

            // Direct push to adjacent blocks
            for (Direction dir : Direction.values()) {
                if (entity.fluidAmount <= 0) break;
                BlockPos targetPos = pos.relative(dir);
                Storage<FluidVariant> target = FluidStorage.SIDED.find(level, targetPos, dir.getOpposite());
                if (target != entity && target != null && target.supportsInsertion()) {
                    try (Transaction t = Transaction.openOuter()) {
                        long insertedDroplets = target.insert(entity.storedFluid, (long) maxTransfer_mB * 81L, t);
                        if (insertedDroplets > 0) {
                            int inserted_mB = (int) (insertedDroplets / 81L);
                            entity.fluidAmount -= inserted_mB;
                            maxTransfer_mB -= inserted_mB;
                            if (entity.fluidAmount <= 0) {
                                entity.fluidAmount = 0;
                                entity.storedFluid = FluidVariant.blank();
                            }
                            entity.syncAndMarkDirty();
                            t.commit();
                        }
                    }
                }
            }

            // Pipe network traversal
            if (maxTransfer_mB > 0 && entity.fluidAmount > 0) {
                for (Direction initialDir : Direction.values()) {
                    if (maxTransfer_mB <= 0 || entity.fluidAmount <= 0) break;
                    BlockPos neighborPos = pos.relative(initialDir);
                    if (level.getBlockState(neighborPos).getBlock() instanceof VolcaniteStoragePipeBlock) {
                        Queue<BlockPos> queue = new LinkedList<>();
                        Set<BlockPos> visited = new HashSet<>();
                        queue.add(neighborPos);
                        visited.add(neighborPos);

                        while (!queue.isEmpty() && maxTransfer_mB > 0 && entity.fluidAmount > 0) {
                            BlockPos curr = queue.poll();
                            for (Direction dir : Direction.values()) {
                                BlockPos targetPos = curr.relative(dir);
                                if (visited.contains(targetPos) || targetPos.equals(pos)) continue;

                                if (level.getBlockState(targetPos).getBlock() instanceof VolcaniteStoragePipeBlock) {
                                    visited.add(targetPos);
                                    queue.add(targetPos);
                                } else {
                                    Storage<FluidVariant> target = FluidStorage.SIDED.find(level, targetPos, dir.getOpposite());
                                    if (target != entity && target != null && target.supportsInsertion()) {
                                        try (Transaction t = Transaction.openOuter()) {
                                            long insertedDroplets = target.insert(entity.storedFluid, (long) maxTransfer_mB * 81L, t);
                                            if (insertedDroplets > 0) {
                                                int inserted_mB = (int) (insertedDroplets / 81L);
                                                entity.fluidAmount -= inserted_mB;
                                                maxTransfer_mB -= inserted_mB;
                                                if (entity.fluidAmount <= 0) {
                                                    entity.fluidAmount = 0;
                                                    entity.storedFluid = FluidVariant.blank();
                                                }
                                                entity.syncAndMarkDirty();
                                                t.commit();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        VolcaniteStoragePipeBlockEntity.autoEjectMachineOutputs(level, pos, entity);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putString("Fluid", BuiltInRegistries.FLUID.getKey(this.storedFluid.getFluid()).toString());
        tag.putInt("FluidAmount", this.fluidAmount);
        tag.putInt("MaxCapacity", this.maxCapacity);
        ContainerHelper.saveAllItems(tag, this.inventory.getItems(), provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("Fluid")) {
            Fluid f = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(tag.getString("Fluid")));
            this.storedFluid = (f != null && f != Fluids.EMPTY) ? FluidVariant.of(f) : FluidVariant.blank();
        }
        if (tag.contains("FluidAmount")) this.fluidAmount = tag.getInt("FluidAmount");
        if (tag.contains("MaxCapacity")) this.maxCapacity = tag.getInt("MaxCapacity");
        this.inventory.clearContent();
        ContainerHelper.loadAllItems(tag, this.inventory.getItems(), provider);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, provider);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.supreme_liquid_tank");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new LiquidTankMenu(syncId, playerInventory, this.inventory, this.data);
    }
}
