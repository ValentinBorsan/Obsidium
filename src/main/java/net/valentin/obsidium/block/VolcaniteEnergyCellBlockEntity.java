package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.VolcaniteEnergyCellBlockItem;
import net.valentin.obsidium.item.VolcaniteOverclockUpgradeItem;
import net.valentin.obsidium.item.VolcanitePipeEnergyUpgradeItem;
import net.valentin.obsidium.item.VolcaniteStackUpgradeItem;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyItem;
import team.reborn.energy.api.base.SimpleEnergyStorage;

public class VolcaniteEnergyCellBlockEntity extends BlockEntity implements MenuProvider, InventoryHolder {
    public static final int FACE_INPUT = 0;
    public static final int FACE_OUTPUT = 1;
    public static final int FACE_BOTH = 2;
    public static final int FACE_NONE = 3;

    protected final long baseCapacity;
    protected final long baseMaxTransfer;
    protected final long baseItemChargeRate;
    protected final boolean advancedUnit;

    private final int[] faceModes = new int[] {
        FACE_BOTH, FACE_BOTH, FACE_BOTH, FACE_BOTH, FACE_BOTH, FACE_BOTH
    };
    private int transferMode = 0; // 0 = Charge, 1 = Discharge
    private int redstoneMode = 0; // 0 = Always On, 1 = High, 2 = Low
    private boolean fieldChargeEnabled = false;

    private long tickInAccumulator = 0L;
    private long tickOutAccumulator = 0L;
    private long avgInPerTick = 0L;
    private long avgOutPerTick = 0L;
    private long lastSyncedEnergy = -1L;

    public final SimpleEnergyStorage energyStorage;

    public final SimpleContainer inventory = new SimpleContainer(2) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            if (stack.isEmpty()) return false;
            if (slot == 0) {
                return EnergyStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack)) != null;
            }
            if (slot == 1) {
                return isUpgradeItem(stack);
            }
            return false;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            long maxCap = getEffectiveCapacity();
            if (energyStorage.amount > maxCap) {
                energyStorage.amount = maxCap;
            }
            syncToClient();
        }
    };

    public VolcaniteEnergyCellBlockEntity(BlockPos pos, BlockState state) {
        this(ObsidiumMod.VOLCANITE_ENERGY_CELL_BE, pos, state, 2_000_000L, 25_000L, 100_000L, false);
    }

    protected VolcaniteEnergyCellBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            long baseCapacity,
            long baseMaxTransfer,
            long baseItemChargeRate,
            boolean advancedUnit
    ) {
        super(type, pos, state);
        this.baseCapacity = baseCapacity;
        this.baseMaxTransfer = baseMaxTransfer;
        this.baseItemChargeRate = baseItemChargeRate;
        this.advancedUnit = advancedUnit;
        this.energyStorage = new SimpleEnergyStorage(baseCapacity * 2L, baseMaxTransfer * 4L, baseMaxTransfer * 4L) {
            @Override
            public long getCapacity() {
                return getEffectiveCapacity();
            }

            @Override
            public long insert(long maxAmount, TransactionContext transaction) {
                long cap = getEffectiveCapacity();
                long maxRate = getEffectiveMaxTransfer();
                long space = Math.max(0L, cap - this.amount);
                long allowed = Math.min(maxAmount, Math.min(maxRate, space));
                if (allowed <= 0L) return 0L;
                long inserted = super.insert(allowed, transaction);
                if (inserted > 0L) {
                    transaction.addCloseCallback((t, result) -> {
                        if (result.wasCommitted()) {
                            tickInAccumulator += inserted;
                        }
                    });
                }
                return inserted;
            }

            @Override
            public long extract(long maxAmount, TransactionContext transaction) {
                long maxRate = getEffectiveMaxTransfer();
                long allowed = Math.min(maxAmount, maxRate);
                if (allowed <= 0L) return 0L;
                long extracted = super.extract(allowed, transaction);
                if (extracted > 0L) {
                    transaction.addCloseCallback((t, result) -> {
                        if (result.wasCommitted()) {
                            tickOutAccumulator += extracted;
                        }
                    });
                }
                return extracted;
            }

            @Override
            protected void onFinalCommit() {
                VolcaniteEnergyCellBlockEntity.this.setChanged();
            }
        };
    }

    public static boolean isUpgradeItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.getItem() instanceof VolcanitePipeEnergyUpgradeItem
                || stack.getItem() instanceof VolcaniteOverclockUpgradeItem
                || stack.getItem() instanceof VolcaniteStackUpgradeItem;
    }

    public int getUpgradeTier() {
        ItemStack up = inventory.getItem(1);
        if (up.isEmpty()) return 0;
        if (up.getItem() instanceof VolcaniteStackUpgradeItem) return 3;
        if (up.getItem() instanceof VolcanitePipeEnergyUpgradeItem pe) return Math.max(1, Math.min(3, pe.getTier()));
        if (up.getItem() instanceof VolcaniteOverclockUpgradeItem oc) return Math.max(1, Math.min(3, oc.getTier()));
        return 0;
    }

    public long getEffectiveCapacity() {
        int tier = getUpgradeTier();
        // Tier 0: 1.0x, Tier 1: 1.25x, Tier 2: 1.5x, Tier 3: 2.0x
        if (tier >= 3) return baseCapacity * 2L;
        if (tier == 2) return (baseCapacity * 3L) / 2L;
        if (tier == 1) return (baseCapacity * 5L) / 4L;
        return baseCapacity;
    }

    public long getEffectiveMaxTransfer() {
        return baseMaxTransfer * (1L + getUpgradeTier());
    }

    public boolean isAdvancedUnit() {
        return advancedUnit;
    }

    public int getFaceMode(Direction dir) {
        if (dir == null) return FACE_BOTH;
        return faceModes[dir.get3DDataValue()];
    }

    public void setFaceMode(Direction dir, int mode) {
        if (dir == null) return;
        faceModes[dir.get3DDataValue()] = Math.max(FACE_INPUT, Math.min(FACE_NONE, mode));
        syncToClient();
    }

    public int cycleFaceMode(Direction dir) {
        if (dir == null) return FACE_BOTH;
        int next = (getFaceMode(dir) + 1) % 4;
        setFaceMode(dir, next);
        return next;
    }

    public void resetAllFaceModes(int mode) {
        int clamped = Math.max(FACE_INPUT, Math.min(FACE_NONE, mode));
        for (int i = 0; i < 6; i++) {
            faceModes[i] = clamped;
        }
        syncToClient();
    }

    public int getPackedFaceModes() {
        int packed = 0;
        for (int i = 0; i < 6; i++) {
            packed |= (faceModes[i] & 0x3) << (i * 2);
        }
        return packed;
    }

    public void unpackFaceModes(int packed) {
        for (int i = 0; i < 6; i++) {
            faceModes[i] = (packed >> (i * 2)) & 0x3;
        }
    }

    public int getRedstoneMode() {
        return redstoneMode;
    }

    public void setRedstoneMode(int mode) {
        this.redstoneMode = Math.max(0, Math.min(2, mode));
        syncToClient();
    }

    public boolean isFieldChargeEnabled() {
        return fieldChargeEnabled;
    }

    public void setFieldChargeEnabled(boolean enabled) {
        this.fieldChargeEnabled = enabled;
        syncToClient();
    }

    public int getTransferMode() {
        return transferMode;
    }

    public boolean isActiveByRedstone() {
        if (redstoneMode == 0) return true;
        if (level == null) return true;
        boolean powered = level.hasNeighborSignal(worldPosition);
        return (redstoneMode == 1) == powered;
    }

    public boolean isPartOfFormedMultiblock() {
        if (level == null) return false;
        return VolcaniteEnergyCoreBlockEntity.findFormedCoreForRingBlock(level, worldPosition) != null
                || KeraunosHVCoreBlockEntity.findFormedCoreForShellBlock(level, worldPosition) != null;
    }

    @Nullable
    public EnergyStorage getSidedEnergyStorage(@Nullable Direction side) {
        int mode = side == null ? FACE_BOTH : getFaceMode(side);
        if (mode == FACE_NONE) return null;

        boolean allowIn = (mode == FACE_INPUT || mode == FACE_BOTH);
        boolean allowOut = (mode == FACE_OUTPUT || mode == FACE_BOTH);

        return new EnergyStorage() {
            @Override
            public boolean supportsInsertion() {
                return allowIn && isActiveByRedstone();
            }

            @Override
            public long insert(long maxAmount, TransactionContext transaction) {
                if (!supportsInsertion()) return 0L;
                return energyStorage.insert(maxAmount, transaction);
            }

            @Override
            public boolean supportsExtraction() {
                return allowOut && isActiveByRedstone();
            }

            @Override
            public long extract(long maxAmount, TransactionContext transaction) {
                if (!supportsExtraction()) return 0L;
                return energyStorage.extract(maxAmount, transaction);
            }

            @Override
            public long getAmount() {
                return energyStorage.amount;
            }

            @Override
            public long getCapacity() {
                return getEffectiveCapacity();
            }
        };
    }

    protected void syncToClient() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
            lastSyncedEnergy = energyStorage.amount;
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VolcaniteEnergyCellBlockEntity entity) {
        if (level.isClientSide()) return;

        // Multiblock core handles pooling & output when cell belongs to a formed 3x3 Energy Core or 3x3x3 Keraunos HV Matrix
        if (entity.isPartOfFormedMultiblock()) {
            entity.avgInPerTick = 0L;
            entity.avgOutPerTick = 0L;
            entity.tickInAccumulator = 0L;
            entity.tickOutAccumulator = 0L;
            return;
        }

        boolean didSomething = false;
        long maxCap = entity.getEffectiveCapacity();
        if (entity.energyStorage.amount > maxCap) {
            entity.energyStorage.amount = maxCap;
            didSomething = true;
        }

        if (entity.isActiveByRedstone()) {
            long maxRate = entity.getEffectiveMaxTransfer();

            for (Direction dir : Direction.values()) {
                int mode = entity.getFaceMode(dir);
                if (mode == FACE_NONE) continue;

                BlockPos targetPos = pos.relative(dir);
                if (level.getBlockEntity(targetPos) instanceof AstrapeEnergyPylonBlockEntity) continue;
                BlockEntity targetBe = level.getBlockEntity(targetPos);

                EnergyStorage target = EnergyStorage.SIDED.find(level, targetPos, dir.getOpposite());
                if (target == null) continue;

                // Pull from pure sources on INPUT or BOTH faces
                if ((mode == FACE_INPUT || mode == FACE_BOTH)
                        && entity.energyStorage.amount < maxCap
                        && target.supportsExtraction() && !target.supportsInsertion()) {
                    long pulled = EnergyStorageUtil.move(target, entity.energyStorage, maxRate, null);
                    if (pulled > 0) didSomething = true;
                }

                // Push on OUTPUT or BOTH faces
                if (mode == FACE_OUTPUT) {
                    if (target.supportsInsertion() && entity.energyStorage.amount > 0) {
                        long pushed = EnergyStorageUtil.move(entity.energyStorage, target, maxRate, null);
                        if (pushed > 0) didSomething = true;
                    }
                } else if (mode == FACE_BOTH && target.supportsInsertion() && entity.energyStorage.amount > 0) {
                    if (targetBe instanceof VolcaniteEnergyCellBlockEntity otherCell) {
                        // Zero-ping-pong smart equalization between adjacent Energy Cells
                        long otherCap = Math.max(1L, otherCell.getEffectiveCapacity());
                        double myPct = (double) entity.energyStorage.amount / (double) Math.max(1L, maxCap);
                        double otherPct = (double) otherCell.energyStorage.amount / (double) otherCap;
                        if (myPct > otherPct + 0.002) {
                            long targetDiff = Math.round((myPct - otherPct) * 0.5 * Math.min(maxCap, otherCap));
                            if (targetDiff > 0) {
                                long moved = EnergyStorageUtil.move(entity.energyStorage, target, Math.min(maxRate, targetDiff), null);
                                if (moved > 0) didSomething = true;
                            }
                        }
                    } else {
                        long pushed = EnergyStorageUtil.move(entity.energyStorage, target, maxRate, null);
                        if (pushed > 0) didSomething = true;
                    }
                }
            }

            // Slot 0: Charge or Discharge held item
            if (!entity.inventory.getItem(0).isEmpty()) {
                ItemStack stack = entity.inventory.getItem(0);
                SingleSlotStorage<ItemVariant> slot = InventoryStorage.of(entity.inventory, null).getSlot(0);
                ContainerItemContext ctx = ContainerItemContext.ofSingleSlot(slot);
                EnergyStorage itemStorage = EnergyStorage.ITEM.find(stack, ctx);
                if (itemStorage != null) {
                    long itemRate = entity.baseItemChargeRate * (1L + entity.getUpgradeTier());
                    if (entity.transferMode == 0) {
                        if (itemStorage.supportsInsertion() && entity.energyStorage.amount > 0) {
                            long moved = EnergyStorageUtil.move(entity.energyStorage, itemStorage, itemRate, null);
                            if (moved > 0) didSomething = true;
                        }
                    } else {
                        if (itemStorage.supportsExtraction() && entity.energyStorage.amount < maxCap) {
                            long space = maxCap - entity.energyStorage.amount;
                            long toMove = Math.min(itemRate, space);
                            long moved = EnergyStorageUtil.move(itemStorage, entity.energyStorage, toMove, null);
                            if (moved > 0) didSomething = true;
                        }
                    }
                }
            }

            // Wireless Field Charge for nearby players' inventories (every 10 ticks)
            if (entity.fieldChargeEnabled && level.getGameTime() % 10 == 0 && entity.energyStorage.amount > 0) {
                double radius = entity.advancedUnit ? 8.0 : 5.0;
                AABB area = new AABB(pos).inflate(radius);
                long perPulseRate = entity.baseItemChargeRate * (1L + entity.getUpgradeTier());
                for (Player player : level.getEntitiesOfClass(Player.class, area)) {
                    InventoryStorage invStorage = InventoryStorage.of(player.getInventory(), null);
                    boolean chargedPlayer = false;
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack pStack = player.getInventory().getItem(i);
                        if (pStack.isEmpty() || pStack.getItem() instanceof VolcaniteEnergyCellBlockItem) continue;
                        SingleSlotStorage<ItemVariant> slot = invStorage.getSlot(i);
                        ContainerItemContext ctx = ContainerItemContext.ofSingleSlot(slot);
                        EnergyStorage itemStorage = EnergyStorage.ITEM.find(pStack, ctx);
                        if (itemStorage != null && itemStorage.supportsInsertion()) {
                            long moved = EnergyStorageUtil.move(entity.energyStorage, itemStorage, perPulseRate, null);
                            if (moved > 0) {
                                didSomething = true;
                                chargedPlayer = true;
                                if (entity.energyStorage.amount <= 0) break;
                            }
                        }
                    }
                    if (chargedPlayer && level instanceof ServerLevel sl && level.getGameTime() % 20 == 0) {
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                player.getX(), player.getY() + 1.0, player.getZ(),
                                4, 0.25, 0.3, 0.25, 0.02);
                    }
                    if (entity.energyStorage.amount <= 0) break;
                }
            }
        }

        // Smooth rolling telemetry (EMA with 0-snap)
        entity.avgInPerTick = (entity.tickInAccumulator == 0 && entity.avgInPerTick < 5)
                ? 0L : (entity.avgInPerTick * 4L + entity.tickInAccumulator) / 5L;
        entity.avgOutPerTick = (entity.tickOutAccumulator == 0 && entity.avgOutPerTick < 5)
                ? 0L : (entity.avgOutPerTick * 4L + entity.tickOutAccumulator) / 5L;
        entity.tickInAccumulator = 0L;
        entity.tickOutAccumulator = 0L;

        // Client visual sync when fill state changes visibly
        long currentEnergy = entity.energyStorage.amount;
        boolean boundaryCrossed = (currentEnergy == 0 && entity.lastSyncedEnergy != 0)
                || (currentEnergy == maxCap && entity.lastSyncedEnergy != maxCap);
        boolean largeDelta = entity.lastSyncedEnergy < 0
                || Math.abs(currentEnergy - entity.lastSyncedEnergy) >= Math.max(100L, maxCap / 60L);
        boolean periodicDelta = level.getGameTime() % 20 == 0 && currentEnergy != entity.lastSyncedEnergy;
        if (boundaryCrossed || largeDelta || periodicDelta) {
            entity.syncToClient();
        }

        if (didSomething) entity.setChanged();
    }

    public boolean hasNonDefaultConfig() {
        if (energyStorage.amount > 0 || transferMode != 0 || redstoneMode != 0 || fieldChargeEnabled) {
            return true;
        }
        for (int mode : faceModes) {
            if (mode != FACE_BOTH) return true;
        }
        return false;
    }

    public void saveToItemStack(ItemStack stack) {
        if (stack.isEmpty() || !hasNonDefaultConfig()) return;
        if (stack.getItem() instanceof SimpleEnergyItem sei) {
            long clamped = Math.min(energyStorage.amount, sei.getEnergyCapacity(stack));
            if (clamped > 0) {
                sei.setStoredEnergy(stack, clamped);
            }
        }
        boolean hasExtra = transferMode != 0 || redstoneMode != 0 || fieldChargeEnabled;
        if (!hasExtra) {
            for (int mode : faceModes) {
                if (mode != FACE_BOTH) {
                    hasExtra = true;
                    break;
                }
            }
        }
        if (hasExtra) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                tag.putIntArray("FaceModes", faceModes.clone());
                tag.putInt("TransferMode", transferMode);
                tag.putInt("RedstoneMode", redstoneMode);
                tag.putBoolean("FieldCharge", fieldChargeEnabled);
            });
        }
    }

    public void loadFromItemStack(ItemStack stack) {
        if (stack.isEmpty()) return;
        if (stack.getItem() instanceof SimpleEnergyItem sei) {
            long stored = sei.getStoredEnergy(stack);
            this.energyStorage.amount = Math.max(0L, Math.min(getEffectiveCapacity(), stored));
        }
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom != null) {
            CompoundTag tag = custom.copyTag();
            if (tag.contains("FaceModes")) {
                int[] saved = tag.getIntArray("FaceModes");
                for (int i = 0; i < Math.min(6, saved.length); i++) {
                    faceModes[i] = Math.max(FACE_INPUT, Math.min(FACE_NONE, saved[i]));
                }
            }
            if (tag.contains("TransferMode")) {
                this.transferMode = Math.max(0, Math.min(1, tag.getInt("TransferMode")));
            }
            if (tag.contains("RedstoneMode")) {
                this.redstoneMode = Math.max(0, Math.min(2, tag.getInt("RedstoneMode")));
            }
            if (tag.contains("FieldCharge")) {
                this.fieldChargeEnabled = tag.getBoolean("FieldCharge");
            }
        }
        syncToClient();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putLong("Energy", energyStorage.amount);
        tag.putInt("TransferMode", transferMode);
        tag.putInt("RedstoneMode", redstoneMode);
        tag.putBoolean("FieldCharge", fieldChargeEnabled);
        tag.putIntArray("FaceModes", faceModes);
        tag.putLong("AvgIn", avgInPerTick);
        tag.putLong("AvgOut", avgOutPerTick);
        MachineInventory.saveInventory(tag, inventory, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        MachineInventory.loadInventory(tag, this.inventory, provider);
        this.energyStorage.amount = Math.min(getEffectiveCapacity(), Math.max(0L, tag.getLong("Energy")));
        if (tag.contains("TransferMode")) this.transferMode = tag.getInt("TransferMode");
        if (tag.contains("RedstoneMode")) this.redstoneMode = Math.max(0, Math.min(2, tag.getInt("RedstoneMode")));
        if (tag.contains("FieldCharge")) this.fieldChargeEnabled = tag.getBoolean("FieldCharge");
        if (tag.contains("FaceModes")) {
            int[] saved = tag.getIntArray("FaceModes");
            for (int i = 0; i < Math.min(6, saved.length); i++) {
                this.faceModes[i] = Math.max(FACE_INPUT, Math.min(FACE_NONE, saved[i]));
            }
        }
        this.avgInPerTick = tag.getLong("AvgIn");
        this.avgOutPerTick = tag.getLong("AvgOut");
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Override
    public net.minecraft.world.Container getInventory() {
        return this.inventory;
    }

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            long amt = energyStorage.amount;
            long cap = getEffectiveCapacity();
            long inRate = avgInPerTick;
            long outRate = avgOutPerTick;
            return switch (index) {
                case 0 -> (int) ((amt >> 32) & 0xFFFF);
                case 1 -> (int) ((amt >> 16) & 0xFFFF);
                case 2 -> (int) (amt & 0xFFFF);
                case 3 -> (int) ((cap >> 32) & 0xFFFF);
                case 4 -> (int) ((cap >> 16) & 0xFFFF);
                case 5 -> (int) (cap & 0xFFFF);
                case 6 -> 0; // Standalone Cell
                case 7 -> (advancedUnit ? 1 : 0) | ((getUpgradeTier() & 0x7) << 1);
                case 8 -> transferMode;
                case 9 -> getPackedFaceModes();
                case 10 -> redstoneMode;
                case 11 -> fieldChargeEnabled ? 1 : 0;
                case 12 -> (int) ((inRate >> 16) & 0xFFFF);
                case 13 -> (int) (inRate & 0xFFFF);
                case 14 -> (int) ((outRate >> 16) & 0xFFFF);
                case 15 -> (int) (outRate & 0xFFFF);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 8) {
                transferMode = value == 0 ? 0 : 1;
                syncToClient();
            } else if (index == 9) {
                unpackFaceModes(value);
                syncToClient();
            } else if (index == 10) {
                redstoneMode = Math.max(0, Math.min(2, value));
                syncToClient();
            } else if (index == 11) {
                fieldChargeEnabled = value != 0;
                syncToClient();
            }
        }

        @Override
        public int getCount() {
            return 16;
        }
    };

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new VolcaniteBatteryMenu(syncId, playerInventory, this.data, this.inventory);
    }
}
