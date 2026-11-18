package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.fluid.TransactionalFluidAccess;
import net.valentin.obsidium.fluid.FluidNetworkTransfers;
import net.valentin.obsidium.fluid.FluidVariantPersistence;
import net.valentin.obsidium.fluid.FluidBucketMapping;
import net.valentin.obsidium.item.VolcaniteBottleItem;
import net.valentin.obsidium.storage.QuantumLinkNetworkState;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public class LiquidTankBlockEntity extends BlockEntity implements MenuProvider, SingleSlotStorage<FluidVariant>, InventoryHolder, MachineInventory {
    protected FluidVariant storedFluid = FluidVariant.blank();
    protected int fluidAmount = 0; // in mB
    protected int maxCapacity = 16000; // 16 buckets base

    // Quantum Link state (for Liquid Barrels upgraded with Quantum Link Module)
    protected boolean quantumLinked = false;
    protected DyeColor quantumColor = DyeColor.PURPLE;
    protected int quantumChannel = 1;
    protected boolean quantumPrivate = false;
    protected @Nullable UUID quantumOwner = null;

    private record FluidSnapshot(FluidVariant fluid, int amount) {}

    private final TransactionalFluidAccess<FluidSnapshot> transactionalFluid =
            new TransactionalFluidAccess<>(
                    () -> new FluidSnapshot(getStoredFluid(), getFluidAmount()),
                    snapshot -> restoreFluidSnapshot(snapshot.fluid(), snapshot.amount()),
                    (fluid, amount) -> applyTransactionalFluid(fluid, amount, true),
                    (fluid, amount) -> applyTransactionalFluid(fluid, amount, false),
                    this::syncAndMarkDirty);

    private void restoreFluidSnapshot(FluidVariant fluid, int amount) {
        String key = getActiveQuantumKey();
        if (key != null) {
            QuantumLinkNetworkState net = QuantumLinkNetworkState.get(this.level);
            if (net != null) {
                net.setFluidState(key, fluid, amount);
                return;
            }
        }
        this.storedFluid = fluid;
        this.fluidAmount = amount;
    }

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
            int amt = getFluidAmount();
            int cap = getMaxCapacity();
            FluidVariant fluid = getStoredFluid();
            return switch (index) {
                case 0 -> (amt >> 16);
                case 1 -> (amt & 0xFFFF);
                case 2 -> (cap >> 16);
                case 3 -> (cap & 0xFFFF);
                case 4 -> fluid.isBlank() ? 0 : BuiltInRegistries.FLUID.getId(fluid.getFluid());
                case 5 -> quantumLinked ? 1 : 0;
                case 6 -> quantumColor.getId();
                case 7 -> quantumChannel;
                case 8 -> quantumPrivate ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 6 -> setQuantumColor(DyeColor.byId(Math.floorMod(value, 16)));
                case 7 -> setQuantumChannel(value);
                case 8 -> setQuantumPrivate(value != 0);
            }
        }

        @Override
        public int getCount() { return 9; }
    };

    public LiquidTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    public String getActiveQuantumKey() {
        if (!quantumLinked || this.level == null || this.level.isClientSide()) {
            return null;
        }
        return QuantumLinkNetworkState.buildChannelKey(quantumPrivate, quantumOwner, quantumColor, quantumChannel);
    }

    public boolean isQuantumLinked() {
        return quantumLinked;
    }

    public DyeColor getQuantumColor() {
        return quantumColor;
    }

    public int getQuantumChannel() {
        return quantumChannel;
    }

    public boolean isQuantumPrivate() {
        return quantumPrivate;
    }

    public void setQuantumLinked(boolean linked, @Nullable UUID owner) {
        this.quantumLinked = linked;
        if (owner != null) {
            this.quantumOwner = owner;
        }
        if (this.level != null && !this.level.isClientSide()) {
            BlockState st = getBlockState();
            if (st.hasProperty(VolcanicWoodLiquidBarrelBlock.LINKED) && st.getValue(VolcanicWoodLiquidBarrelBlock.LINKED) != linked) {
                this.level.setBlock(worldPosition, st.setValue(VolcanicWoodLiquidBarrelBlock.LINKED, linked), Block.UPDATE_ALL);
            }
        }
        syncAndMarkDirty();
    }

    public void ensureQuantumOwner(@Nullable UUID owner) {
        if (this.quantumOwner == null && owner != null) {
            this.quantumOwner = owner;
            setChanged();
        }
    }

    public void setQuantumColor(DyeColor color) {
        if (color != null && this.quantumColor != color) {
            this.quantumColor = color;
            syncAndMarkDirty();
        }
    }

    public void setQuantumChannel(int channel) {
        int clamped = QuantumLinkNetworkState.clampChannel(channel);
        if (this.quantumChannel != clamped) {
            this.quantumChannel = clamped;
            syncAndMarkDirty();
        }
    }

    public void setQuantumPrivate(boolean priv) {
        if (this.quantumPrivate != priv) {
            this.quantumPrivate = priv;
            syncAndMarkDirty();
        }
    }

    public FluidVariant getStoredFluid() {
        String key = getActiveQuantumKey();
        if (key != null) {
            QuantumLinkNetworkState net = QuantumLinkNetworkState.get(this.level);
            if (net != null) return net.getFluid(key);
        }
        return this.storedFluid;
    }

    public int getFluidAmount() {
        String key = getActiveQuantumKey();
        if (key != null) {
            QuantumLinkNetworkState net = QuantumLinkNetworkState.get(this.level);
            if (net != null) return (int) Math.min(Integer.MAX_VALUE, net.getFluidAmount(key));
        }
        return this.fluidAmount;
    }

    public int getMaxCapacity() {
        if (quantumLinked) {
            return (int) QuantumLinkNetworkState.FLUID_VAULT_CAPACITY;
        }
        return this.maxCapacity;
    }

    public boolean canAccept(FluidVariant variant) {
        if (variant.isBlank()) return false;
        FluidVariant cur = getStoredFluid();
        return cur.isBlank() || cur.equals(variant);
    }

    public int insertFluid(FluidVariant variant, int amount_mB, boolean simulate) {
        if (!canAccept(variant) || amount_mB <= 0) return 0;
        String key = getActiveQuantumKey();
        if (key != null) {
            QuantumLinkNetworkState net = QuantumLinkNetworkState.get(this.level);
            if (net != null) {
                int inserted = (int) net.insertFluid(key, variant, amount_mB, simulate);
                if (inserted > 0 && !simulate) {
                    syncAndMarkDirty();
                }
                return inserted;
            }
        }
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
        if (amount_mB <= 0) return 0;
        String key = getActiveQuantumKey();
        if (key != null) {
            QuantumLinkNetworkState net = QuantumLinkNetworkState.get(this.level);
            if (net != null) {
                int extracted = (int) net.extractFluid(key, variant, amount_mB, simulate);
                if (extracted > 0 && !simulate) {
                    syncAndMarkDirty();
                }
                return extracted;
            }
        }
        if (this.storedFluid.isBlank() || this.fluidAmount <= 0) return 0;
        if (variant != null && !variant.isBlank() && !this.storedFluid.equals(variant)) return 0;
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
        return getStoredFluid().isBlank();
    }

    @Override
    public FluidVariant getResource() {
        return getStoredFluid();
    }

    @Override
    public long getAmount() {
        return (long) getFluidAmount() * 81L;
    }

    @Override
    public long getCapacity() {
        return (long) getMaxCapacity() * 81L;
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

            if (!bucketFluidVariant.isBlank() && entity.canAccept(bucketFluidVariant) && entity.getFluidAmount() + containerFluidAmount <= entity.getMaxCapacity()) {
                if (emptyReturnItem == Items.AIR || output.isEmpty() || (output.is(emptyReturnItem) && output.getCount() < output.getMaxStackSize())) {
                    int inserted = entity.insertFluid(bucketFluidVariant, containerFluidAmount, false);
                    if (inserted == containerFluidAmount) {
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
            }
            // B) Empty bucket -> filled bucket (Lava, Water, Pyroclastic Distillate, Steam)
            else if (input.is(Items.BUCKET) && entity.getFluidAmount() >= 1000 && !entity.getStoredFluid().isBlank()) {
                FluidVariant curFluid = entity.getStoredFluid();
                Item bucketResult = FluidBucketMapping.filledBucket(curFluid);
                if (bucketResult != null && bucketResult != Items.AIR) {
                    ItemStack filledBucket = new ItemStack(bucketResult);
                    if (output.isEmpty() || (ItemStack.isSameItem(output, filledBucket) && output.getCount() < output.getMaxStackSize())) {
                        if (entity.extractFluid(curFluid, 1000, false) == 1000) {
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
            }
            // B2) Empty Glass Bottle -> Pyroclastic Flask (250 mB)
            else if (input.is(Items.GLASS_BOTTLE) && entity.getFluidAmount() >= 250 && entity.getStoredFluid().isOf(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID)) {
                ItemStack filledFlask = new ItemStack(ObsidiumMod.PYROCLASTIC_FLASK);
                if (output.isEmpty() || (ItemStack.isSameItem(output, filledFlask) && output.getCount() < output.getMaxStackSize())) {
                    if (entity.extractFluid(FluidVariant.of(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID), 250, false) == 250) {
                        input.shrink(1);
                        if (output.isEmpty()) {
                            entity.inventory.setItem(1, filledFlask);
                        } else {
                            output.grow(1);
                        }
                        entity.syncAndMarkDirty();
                    }
                }
            }
            // C) Volcanite Bottle
            else if (input.getItem() instanceof VolcaniteBottleItem bottle) {
                FluidVariant bottleFluid = VolcaniteBottleItem.getFluid(input);
                int bottleAmount = VolcaniteBottleItem.getFluidAmount(input);
                int maxCap = bottle.getCapacity();

                // If bottle has fluid and tank has space -> drain bottle
                if (!bottleFluid.isBlank() && bottleAmount > 0 && entity.canAccept(bottleFluid) && entity.getFluidAmount() < entity.getMaxCapacity()) {
                    int toTransfer = entity.insertFluid(bottleFluid, bottleAmount, false);
                    if (toTransfer > 0) {
                        int newBottleAmount = bottleAmount - toTransfer;
                        if (newBottleAmount <= 0) {
                            VolcaniteBottleItem.setFluid(input, FluidVariant.blank(), 0);
                        } else {
                            VolcaniteBottleItem.setFluid(input, bottleFluid, newBottleAmount);
                        }
                        entity.syncAndMarkDirty();
                    }
                }
                // If tank has fluid and bottle has space -> fill bottle
                else if (!entity.getStoredFluid().isBlank() && !entity.getStoredFluid().hasComponents() && entity.getFluidAmount() > 0 &&
                        (bottleFluid.isBlank() || bottleFluid.equals(entity.getStoredFluid())) && bottleAmount < maxCap) {
                    FluidVariant curFluid = entity.getStoredFluid();
                    int toTransfer = entity.extractFluid(curFluid, maxCap - bottleAmount, false);
                    if (toTransfer > 0) {
                        VolcaniteBottleItem.setFluid(input, curFluid, bottleAmount + toTransfer);
                        entity.syncAndMarkDirty();
                    }
                }
            }
        }

        // 2. Auto-pumping to adjacent containers / pipes (up to 50 mB per tick)
        if (entity.getFluidAmount() > 0 && !entity.getStoredFluid().isBlank()) {
            int maxTransfer_mB = 50;

            // Direct push to adjacent blocks
            for (Direction dir : Direction.values()) {
                if (entity.getFluidAmount() <= 0 || maxTransfer_mB <= 0) break;
                FluidVariant curFluid = entity.getStoredFluid();
                if (curFluid.isBlank()) break;
                BlockPos targetPos = pos.relative(dir);
                Storage<FluidVariant> target = FluidStorage.SIDED.find(level, targetPos, dir.getOpposite());
                if (target != entity && target != null && target.supportsInsertion()) {
                    int avail = Math.min(maxTransfer_mB, entity.getFluidAmount());
                    maxTransfer_mB -= FluidNetworkTransfers.pump(entity, target, curFluid, avail);
                }
            }

            // Pipe network traversal
            if (maxTransfer_mB > 0 && entity.getFluidAmount() > 0) {
                for (Direction initialDir : Direction.values()) {
                    if (maxTransfer_mB <= 0 || entity.getFluidAmount() <= 0) break;
                    BlockPos neighborPos = pos.relative(initialDir);
                    if (level.getBlockState(neighborPos).getBlock() instanceof VolcaniteStoragePipeBlock) {
                        Queue<BlockPos> queue = new LinkedList<>();
                        Set<BlockPos> visited = new HashSet<>();
                        queue.add(neighborPos);
                        visited.add(neighborPos);

                        while (!queue.isEmpty() && maxTransfer_mB > 0 && entity.getFluidAmount() > 0) {
                            BlockPos curr = queue.poll();
                            for (Direction dir : Direction.values()) {
                                BlockPos targetPos = curr.relative(dir);
                                if (visited.contains(targetPos) || targetPos.equals(pos)) continue;

                                if (level.getBlockState(targetPos).getBlock() instanceof VolcaniteStoragePipeBlock) {
                                    visited.add(targetPos);
                                    queue.add(targetPos);
                                } else {
                                    FluidVariant curFluid = entity.getStoredFluid();
                                    if (curFluid.isBlank()) break;
                                    Storage<FluidVariant> target = FluidStorage.SIDED.find(level, targetPos, dir.getOpposite());
                                    if (target != entity && target != null && target.supportsInsertion()) {
                                        int avail = Math.min(maxTransfer_mB, entity.getFluidAmount());
                                        maxTransfer_mB -= FluidNetworkTransfers.pump(entity, target, curFluid, avail);
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
        FluidVariantPersistence.write(tag, this.storedFluid, provider);
        tag.putInt("FluidAmount", this.fluidAmount);
        tag.putInt("MaxCapacity", this.maxCapacity);
        tag.putBoolean("QuantumLinked", this.quantumLinked);
        tag.putInt("QuantumColor", this.quantumColor.getId());
        tag.putInt("QuantumChannel", this.quantumChannel);
        tag.putBoolean("QuantumPrivate", this.quantumPrivate);
        if (this.quantumOwner != null) {
            tag.putUUID("QuantumOwner", this.quantumOwner);
        }
        ContainerHelper.saveAllItems(tag, this.inventory.getItems(), provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.storedFluid = FluidVariantPersistence.read(tag, provider);
        this.fluidAmount = this.storedFluid.isBlank() ? 0 : Math.max(0, tag.getInt("FluidAmount"));
        if (tag.contains("MaxCapacity")) this.maxCapacity = tag.getInt("MaxCapacity");
        this.quantumLinked = tag.getBoolean("QuantumLinked");
        if (tag.contains("QuantumColor")) {
            this.quantumColor = DyeColor.byId(Math.floorMod(tag.getInt("QuantumColor"), 16));
        }
        if (tag.contains("QuantumChannel")) {
            this.quantumChannel = QuantumLinkNetworkState.clampChannel(tag.getInt("QuantumChannel"));
        }
        this.quantumPrivate = tag.getBoolean("QuantumPrivate");
        this.quantumOwner = tag.hasUUID("QuantumOwner") ? tag.getUUID("QuantumOwner") : null;
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
