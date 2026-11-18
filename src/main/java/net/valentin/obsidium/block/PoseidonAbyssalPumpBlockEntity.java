package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.storage.QuantumLinkNetworkState;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PoseidonAbyssalPumpBlockEntity extends BlockEntity implements MenuProvider {
    public static final long MAX_ENERGY = 20_000_000L;
    public static final long MAX_FLUID_MB = 256_000L; // 256 Buckets
    public static final int DATA_COUNT = 18;

    public static final int SPEED_NORMAL = 0;    // 1,000 mB/t, 250 RF/t
    public static final int SPEED_TURBO = 1;     // 4,000 mB/t, 1,000 RF/t
    public static final int SPEED_OVERDRIVE = 2; // 16,000 mB/t, 4,000 RF/t

    public static final int FILTER_ALL = 0;
    public static final int FILTER_WATER = 1;
    public static final int FILTER_LAVA = 2;

    public static final int REPLACE_AIR = 0;
    public static final int REPLACE_COBBLE = 1;
    public static final int REPLACE_NETHERRACK = 2;
    public static final int REPLACE_GLASS = 3;

    public static final int PUMP_IDLE = 0;
    public static final int PUMP_INFINITE_WATER = 1;
    public static final int PUMP_AREA_DRAIN = 2;
    public static final int PUMP_DRAINED = 3;

    private static final int[] RADII = {8, 16, 32, 48};

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(MAX_ENERGY, 1_000_000L, 0L) {
        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    public final SingleFluidStorage fluidStorage = new SingleFluidStorage() {
        @Override protected long getCapacity(FluidVariant variant) {
            // Preserve legacy overfilled saves, but accept no additional fluid until drained.
            return Math.max(81L * MAX_FLUID_MB, amount);
        }
        @Override protected void onFinalCommit() { setChanged(); }
    };

    private boolean formed = false;
    private boolean showHologram = true;
    private int speedTier = SPEED_NORMAL;
    private int radiusIndex = 1; // default 16
    private int replaceMode = REPLACE_AIR;
    private int filterMode = FILTER_ALL;
    private int redstoneMode = PipeConfigurable.REDSTONE_ALWAYS;

    private boolean quantumEnabled = false;
    private DyeColor quantumColor = DyeColor.CYAN;
    private int quantumChannel = 1;
    private boolean quantumPrivate = false;
    private @Nullable UUID quantumOwner = null;

    private int pumpStatus = PUMP_IDLE;
    private int scanYOffset = 1;
    private int scanCursor = 0;
    private boolean areaComplete = false;
    private int validationTicker = 0;
    private int syncTicker = 0;
    private int lastRfCost = 0;
    private int lastFluidColor = 0xFF00BFFF;

    public final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            long krf = energyStorage.amount / 1000L;
            long mb = fluidStorage.amount / 81L;
            return switch (index) {
                case 0 -> formed ? 1 : 0;
                case 1 -> showHologram ? 1 : 0;
                case 2 -> (int) (krf & 0xFFFFL);
                case 3 -> (int) (mb & 0xFFFFL);
                case 4 -> speedTier;
                case 5 -> radiusIndex;
                case 6 -> replaceMode;
                case 7 -> filterMode;
                case 8 -> redstoneMode;
                case 9 -> quantumEnabled ? 1 : 0;
                case 10 -> quantumColor.getId();
                case 11 -> quantumChannel;
                case 12 -> quantumPrivate ? 1 : 0;
                case 13 -> pumpStatus;
                case 14 -> worldPosition.getY() - scanYOffset;
                case 15 -> BuiltInRegistries.FLUID.getId(fluidStorage.variant.isBlank() ? Fluids.EMPTY : fluidStorage.variant.getFluid());
                case 16 -> (int) ((krf >>> 16) & 0xFFFFL);
                case 17 -> (int) ((mb >>> 16) & 0xFFFFL);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Synced server -> client
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public PoseidonAbyssalPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.POSEIDON_ABYSSAL_PUMP_BE, pos, state);
    }

    public boolean isFormed() { return formed; }
    public boolean isShowHologram() { return showHologram; }
    public int getSpeedTier() { return speedTier; }
    public int getPumpStatus() { return pumpStatus; }
    public int getLastFluidColor() { return lastFluidColor; }
    public boolean isActivelyPumping() { return formed && (pumpStatus == PUMP_INFINITE_WATER || pumpStatus == PUMP_AREA_DRAIN) && lastRfCost > 0; }

    public int getScanRadius() {
        return RADII[Math.floorMod(radiusIndex, RADII.length)];
    }

    public long getTargetMbPerTick() {
        return switch (speedTier) {
            case SPEED_TURBO -> 4_000L;
            case SPEED_OVERDRIVE -> 16_000L;
            default -> 1_000L;
        };
    }

    public long getRfPerTickCost() {
        return switch (speedTier) {
            case SPEED_TURBO -> 1_000L;
            case SPEED_OVERDRIVE -> 4_000L;
            default -> 250L;
        };
    }

    public List<BlockPos> getCasingPositions() {
        Direction facing = getBlockState().hasProperty(PoseidonAbyssalPumpBlock.FACING)
                ? getBlockState().getValue(PoseidonAbyssalPumpBlock.FACING)
                : Direction.NORTH;
        Direction right = facing.getCounterClockWise();
        Direction back = facing.getOpposite();

        List<BlockPos> positions = new ArrayList<>(7);
        for (int dy = 0; dy <= 1; dy++) {
            for (int dz = 0; dz <= 1; dz++) {
                for (int dx = 0; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    positions.add(worldPosition.relative(right, dx).relative(back, dz).above(dy));
                }
            }
        }
        return positions;
    }

    public List<BlockPos> getAllMemberPositions() {
        List<BlockPos> all = new ArrayList<>(8);
        all.add(worldPosition);
        all.addAll(getCasingPositions());
        return all;
    }

    public boolean validateMultiblock(Level level) {
        List<BlockPos> casings = getCasingPositions();
        boolean valid = true;
        for (BlockPos cp : casings) {
            if (!level.isLoaded(cp) || !level.getBlockState(cp).is(ObsidiumMod.POSEIDON_ABYSSAL_CASING_BLOCK)) {
                valid = false;
                break;
            }
        }

        if (valid != this.formed) {
            this.formed = valid;
            BlockState selfState = getBlockState();
            if (selfState.is(ObsidiumMod.POSEIDON_ABYSSAL_PUMP_BLOCK)) {
                level.setBlock(worldPosition, selfState.setValue(PoseidonAbyssalPumpBlock.FORMED, valid), 3);
            }
            for (BlockPos cp : casings) {
                if (level.isLoaded(cp) && level.getBlockState(cp).is(ObsidiumMod.POSEIDON_ABYSSAL_CASING_BLOCK)) {
                    BlockState cs = level.getBlockState(cp);
                    if (cs.getValue(PoseidonAbyssalCasingBlock.FORMED) != valid) {
                        level.setBlock(cp, cs.setValue(PoseidonAbyssalCasingBlock.FORMED, valid), 3);
                    }
                    if (level.getBlockEntity(cp) instanceof PoseidonAbyssalCasingBlockEntity cbe) {
                        cbe.setControllerPos(valid ? worldPosition : null);
                    }
                }
            }
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        } else if (valid) {
            for (BlockPos cp : casings) {
                if (level.isLoaded(cp) && level.getBlockEntity(cp) instanceof PoseidonAbyssalCasingBlockEntity cbe) {
                    if (!worldPosition.equals(cbe.getControllerPos())) {
                        cbe.setControllerPos(worldPosition);
                    }
                }
            }
        }
        return valid;
    }

    public void unformCasings() {
        if (level == null || level.isClientSide()) return;
        this.formed = false;
        for (BlockPos cp : getCasingPositions()) {
            if (level.isLoaded(cp) && level.getBlockState(cp).is(ObsidiumMod.POSEIDON_ABYSSAL_CASING_BLOCK)) {
                BlockState cs = level.getBlockState(cp);
                if (cs.getValue(PoseidonAbyssalCasingBlock.FORMED)) {
                    level.setBlock(cp, cs.setValue(PoseidonAbyssalCasingBlock.FORMED, false), 3);
                }
                if (level.getBlockEntity(cp) instanceof PoseidonAbyssalCasingBlockEntity cbe) {
                    cbe.setControllerPos(null);
                }
            }
        }
    }

    public void onMemberRemoved() {
        if (level != null && !level.isClientSide()) {
            validateMultiblock(level);
        }
    }

    public void autoBuild(ServerPlayer player) {
        if (level == null || level.isClientSide()) return;
        int placed = 0;
        for (BlockPos cp : getCasingPositions()) {
            BlockState existing = level.getBlockState(cp);
            if (existing.is(ObsidiumMod.POSEIDON_ABYSSAL_CASING_BLOCK)) continue;
            if (!existing.canBeReplaced()) continue;

            if (!player.isCreative()) {
                if (!consumeCasingFromPlayer(player)) {
                    player.displayClientMessage(Component.literal("§c[Poseidon Pump] Need more Poseidon Abyssal Casings in inventory!"), true);
                    break;
                }
            }
            level.setBlock(cp, ObsidiumMod.POSEIDON_ABYSSAL_CASING_BLOCK.defaultBlockState(), 3);
            placed++;
        }
        boolean nowFormed = validateMultiblock(level);
        if (nowFormed) {
            player.displayClientMessage(Component.literal("§b[Poseidon Pump] §a2×2×2 Abyssal Pump formed!"), true);
        } else if (placed > 0) {
            player.displayClientMessage(Component.literal("§b[Poseidon Pump] §ePlaced " + placed + " casing(s)."), true);
        }
    }

    private static boolean consumeCasingFromPlayer(ServerPlayer player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (!st.isEmpty() && st.is(ObsidiumMod.POSEIDON_ABYSSAL_CASING_ITEM)) {
                st.shrink(1);
                return true;
            }
        }
        return false;
    }

    public void ensureQuantumOwner(@Nullable UUID owner) {
        if (this.quantumOwner == null && owner != null) {
            this.quantumOwner = owner;
            setChanged();
        }
    }

    public void handleButton(int id, ServerPlayer player) {
        ensureQuantumOwner(player.getUUID());
        switch (id) {
            case 0 -> { showHologram = !showHologram; syncToClients(); }
            case 1 -> autoBuild(player);
            case 2 -> speedTier = (speedTier + 1) % 3;
            case 3 -> { radiusIndex = (radiusIndex + 1) % RADII.length; resetScan(); }
            case 4 -> replaceMode = (replaceMode + 1) % 4;
            case 5 -> { filterMode = (filterMode + 1) % 3; resetScan(); }
            case 6 -> redstoneMode = Math.floorMod(redstoneMode + 1, 4);
            case 7 -> quantumEnabled = !quantumEnabled;
            case 8 -> quantumColor = DyeColor.byId((quantumColor.getId() + 1) % 16);
            case 9 -> quantumChannel = QuantumLinkNetworkState.clampChannel(quantumChannel + 1);
            case 10 -> quantumChannel = QuantumLinkNetworkState.clampChannel(quantumChannel - 1);
            case 11 -> quantumPrivate = !quantumPrivate;
            case 12 -> resetScan();
        }
        setChanged();
    }

    private void resetScan() {
        this.scanYOffset = 1;
        this.scanCursor = 0;
        this.areaComplete = false;
    }

    private boolean isRedstoneAllowed(Level level, BlockPos pos) {
        boolean powered = false;
        for (BlockPos mp : getAllMemberPositions()) {
            if (level.isLoaded(mp) && level.hasNeighborSignal(mp)) {
                powered = true;
                break;
            }
        }
        return switch (redstoneMode) {
            case PipeConfigurable.REDSTONE_HIGH -> powered;
            case PipeConfigurable.REDSTONE_LOW -> !powered;
            case PipeConfigurable.REDSTONE_OFF -> false;
            default -> true;
        };
    }

    private boolean acceptsFluid(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) return false;
        return switch (filterMode) {
            case FILTER_WATER -> fluid == Fluids.WATER;
            case FILTER_LAVA -> fluid == Fluids.LAVA;
            default -> true;
        };
    }

    private boolean detectInfinite2x2Water(Level level) {
        if (!acceptsFluid(Fluids.WATER)) return false;
        Direction facing = getBlockState().hasProperty(PoseidonAbyssalPumpBlock.FACING)
                ? getBlockState().getValue(PoseidonAbyssalPumpBlock.FACING)
                : Direction.NORTH;
        Direction right = facing.getCounterClockWise();
        Direction back = facing.getOpposite();

        for (int depth = 1; depth <= 8; depth++) {
            boolean allFourWater = true;
            for (int dz = 0; dz <= 1 && allFourWater; dz++) {
                for (int dx = 0; dx <= 1; dx++) {
                    BlockPos checkPos = worldPosition.relative(right, dx).relative(back, dz).below(depth);
                    if (!level.isLoaded(checkPos)) {
                        allFourWater = false;
                        break;
                    }
                    FluidState fs = level.getFluidState(checkPos);
                    if (fs.getType() != Fluids.WATER || !fs.isSource()) {
                        allFourWater = false;
                        break;
                    }
                }
            }
            if (allFourWater) return true;
        }
        return false;
    }

    private BlockState getReplacementBlockState() {
        return switch (replaceMode) {
            case REPLACE_COBBLE -> Blocks.COBBLESTONE.defaultBlockState();
            case REPLACE_NETHERRACK -> Blocks.NETHERRACK.defaultBlockState();
            case REPLACE_GLASS -> Blocks.GLASS.defaultBlockState();
            default -> Blocks.AIR.defaultBlockState();
        };
    }

    private static int computeFluidColor(Fluid fluid) {
        if (fluid == Fluids.WATER) return 0xFF00D4FF;
        if (fluid == Fluids.LAVA) return 0xFFFF6611;
        if (fluid == ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID) return 0xFFD946EF;
        if (fluid == ObsidiumMod.STEAM_FLUID) return 0xFFE2E8F0;
        return 0xFF38BDF8;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PoseidonAbyssalPumpBlockEntity entity) {
        if (level.isClientSide()) return;

        entity.validationTicker++;
        if (entity.validationTicker >= 20) {
            entity.validationTicker = 0;
            entity.validateMultiblock(level);
        }

        if (!entity.formed) {
            entity.pumpStatus = PUMP_IDLE;
            entity.lastRfCost = 0;
            return;
        }

        boolean didSomething = false;
        long targetMb = entity.getTargetMbPerTick();

        if (entity.isRedstoneAllowed(level, pos)) {
            long rfCost = entity.getRfPerTickCost();
            long spaceMb = (entity.fluidStorage.getCapacity() - entity.fluidStorage.amount) / 81L;

            if (entity.energyStorage.amount >= rfCost && spaceMb > 0L) {
                if (entity.detectInfinite2x2Water(level)) {
                    // Mode 1: Infinite 2x2 Water (Zero Lag!)
                    FluidVariant water = FluidVariant.of(Fluids.WATER);
                    if (entity.fluidStorage.variant.isBlank() || entity.fluidStorage.variant.equals(water)) {
                        long toAddMb = Math.min(targetMb, spaceMb);
                        if (toAddMb > 0L) {
                            try (Transaction tx = Transaction.openOuter()) {
                                long inserted = entity.fluidStorage.insert(water, toAddMb * 81L, tx);
                                if (inserted > 0L) {
                                    tx.commit();
                                    entity.energyStorage.amount -= rfCost;
                                    entity.lastRfCost = (int) rfCost;
                                    entity.pumpStatus = PUMP_INFINITE_WATER;
                                    entity.lastFluidColor = computeFluidColor(Fluids.WATER);
                                    didSomething = true;
                                }
                            }
                        }
                    }
                } else if (!entity.areaComplete) {
                    // Mode 2: High-Speed Area Drain
                    int pumpedBlocks = entity.runAreaDrainStep(level, targetMb, spaceMb);
                    if (pumpedBlocks > 0) {
                        entity.energyStorage.amount -= rfCost;
                        entity.lastRfCost = (int) rfCost;
                        entity.pumpStatus = PUMP_AREA_DRAIN;
                        didSomething = true;
                    } else if (entity.areaComplete) {
                        entity.pumpStatus = PUMP_DRAINED;
                        entity.lastRfCost = 0;
                    } else {
                        entity.pumpStatus = PUMP_AREA_DRAIN;
                        entity.lastRfCost = 0;
                    }
                } else {
                    entity.pumpStatus = PUMP_DRAINED;
                    entity.lastRfCost = 0;
                }
            } else {
                entity.pumpStatus = PUMP_IDLE;
                entity.lastRfCost = 0;
            }
        } else {
            entity.pumpStatus = PUMP_IDLE;
            entity.lastRfCost = 0;
        }

        // Output 1: Integrated Quantum Link push
        if (entity.quantumEnabled && entity.fluidStorage.amount >= 81L && !entity.fluidStorage.variant.isBlank()) {
            QuantumLinkNetworkState netState = QuantumLinkNetworkState.get(level);
            if (netState != null) {
                String qKey = QuantumLinkNetworkState.buildChannelKey(entity.quantumPrivate, entity.quantumOwner, entity.quantumColor, entity.quantumChannel);
                FluidVariant cur = entity.fluidStorage.variant;
                long availMb = Math.min(targetMb, entity.fluidStorage.amount / 81L);
                long pushedMb = netState.insertFluid(qKey, cur, availMb, false);
                if (pushedMb > 0L) {
                    try (Transaction tx = Transaction.openOuter()) {
                        entity.fluidStorage.extract(cur, pushedMb * 81L, tx);
                        tx.commit();
                    }
                    didSomething = true;
                }
            }
        }

        // Output 2: Auto-Eject to adjacent fluid storages / Naiad Fluid Pipes from all 8 blocks
        if (entity.fluidStorage.amount > 0L) {
            for (BlockPos memberPos : entity.getAllMemberPositions()) {
                for (Direction dir : Direction.values()) {
                    BlockPos np = memberPos.relative(dir);
                    if (!level.isLoaded(np)) continue;
                    BlockEntity nbe = level.getBlockEntity(np);
                    if (nbe instanceof PoseidonAbyssalPumpBlockEntity || nbe instanceof PoseidonAbyssalCasingBlockEntity) {
                        continue;
                    }
                    Storage<FluidVariant> target = FluidStorage.SIDED.find(level, np, dir.getOpposite());
                    if (target != null && target.supportsInsertion()) {
                        long moved = StorageUtil.move(entity.fluidStorage, target, f -> !f.isBlank(), targetMb * 81L, null);
                        if (moved > 0L) {
                            didSomething = true;
                        }
                    }
                }
                if (entity.fluidStorage.amount <= 0L) break;
            }
        }

        entity.syncTicker++;
        if (didSomething) {
            entity.setChanged();
        }
        if (entity.syncTicker >= 10) {
            entity.syncTicker = 0;
            entity.syncToClients();
        }
    }

    private int runAreaDrainStep(Level level, long targetMb, long spaceMb) {
        int radius = getScanRadius();
        int width = radius * 2 + 1;
        int layerSize = width * width;
        int minY = Math.max(level.getMinBuildHeight(), worldPosition.getY() - 64);
        int maxSources = (int) Math.min(targetMb / 1000L, spaceMb / 1000L);
        if (maxSources <= 0) return 0;

        int drained = 0;
        int checked = 0;
        final int maxChecksPerTick = 512;

        while (drained < maxSources && checked < maxChecksPerTick) {
            int curY = worldPosition.getY() - scanYOffset;
            if (curY < minY) {
                areaComplete = true;
                break;
            }

            int dx = (scanCursor % width) - radius;
            int dz = (scanCursor / width) - radius;
            BlockPos targetPos = new BlockPos(worldPosition.getX() + dx, curY, worldPosition.getZ() + dz);

            scanCursor++;
            if (scanCursor >= layerSize) {
                scanCursor = 0;
                scanYOffset++;
            }
            checked++;

            if (!level.isLoaded(targetPos)) continue;
            FluidState fs = level.getFluidState(targetPos);
            if (fs.isEmpty() || !fs.isSource()) continue;
            // Water contained in a solid block is not a world-fluid block to replace.
            if (!(level.getBlockState(targetPos).getBlock() instanceof net.minecraft.world.level.block.LiquidBlock)) continue;

            Fluid fluid = fs.getType();
            if (!acceptsFluid(fluid)) continue;

            FluidVariant variant = FluidVariant.of(fluid);
            if (!fluidStorage.variant.isBlank() && !fluidStorage.variant.equals(variant)) continue;

            try (Transaction tx = Transaction.openOuter()) {
                long inserted = fluidStorage.insert(variant, FluidConstants.BUCKET, tx);
                if (inserted == FluidConstants.BUCKET) {
                    tx.commit();
                    level.setBlock(targetPos, getReplacementBlockState(), 2);
                    lastFluidColor = computeFluidColor(fluid);
                    drained++;
                }
            }
        }
        return drained;
    }

    private void syncToClients() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new PoseidonAbyssalPumpMenu(syncId, playerInventory, this.dataAccess, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("Energy", energyStorage.amount);
        CompoundTag fluidTag = new CompoundTag();
        fluidStorage.writeNbt(fluidTag, registries);
        tag.put("Fluid", fluidTag);

        tag.putBoolean("Formed", formed);
        tag.putBoolean("ShowHologram", showHologram);
        tag.putInt("SpeedTier", speedTier);
        tag.putInt("RadiusIndex", radiusIndex);
        tag.putInt("ReplaceMode", replaceMode);
        tag.putInt("FilterMode", filterMode);
        tag.putInt("RedstoneMode", redstoneMode);
        tag.putBoolean("QuantumEnabled", quantumEnabled);
        tag.putInt("QuantumColor", quantumColor.getId());
        tag.putInt("QuantumChannel", quantumChannel);
        tag.putBoolean("QuantumPrivate", quantumPrivate);
        if (quantumOwner != null) tag.putUUID("QuantumOwner", quantumOwner);
        tag.putInt("ScanYOffset", scanYOffset);
        tag.putInt("ScanCursor", scanCursor);
        tag.putBoolean("AreaComplete", areaComplete);
        tag.putInt("PumpStatus", pumpStatus);
        tag.putInt("LastRfCost", lastRfCost);
        tag.putInt("LastFluidColor", lastFluidColor);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyStorage.amount = tag.getLong("Energy");
        if (tag.contains("Fluid")) {
            fluidStorage.readNbt(tag.getCompound("Fluid"), registries);
        }
        formed = tag.getBoolean("Formed");
        showHologram = !tag.contains("ShowHologram") || tag.getBoolean("ShowHologram");
        speedTier = tag.getInt("SpeedTier");
        radiusIndex = tag.contains("RadiusIndex") ? tag.getInt("RadiusIndex") : 1;
        replaceMode = tag.getInt("ReplaceMode");
        filterMode = tag.getInt("FilterMode");
        redstoneMode = tag.getInt("RedstoneMode");
        quantumEnabled = tag.getBoolean("QuantumEnabled");
        if (tag.contains("QuantumColor")) quantumColor = DyeColor.byId(Math.floorMod(tag.getInt("QuantumColor"), 16));
        if (tag.contains("QuantumChannel")) quantumChannel = QuantumLinkNetworkState.clampChannel(tag.getInt("QuantumChannel"));
        quantumPrivate = tag.getBoolean("QuantumPrivate");
        quantumOwner = tag.hasUUID("QuantumOwner") ? tag.getUUID("QuantumOwner") : null;
        if (tag.contains("ScanYOffset")) scanYOffset = Math.max(1, tag.getInt("ScanYOffset"));
        scanCursor = tag.getInt("ScanCursor");
        areaComplete = tag.getBoolean("AreaComplete");
        pumpStatus = tag.getInt("PumpStatus");
        lastRfCost = tag.getInt("LastRfCost");
        if (tag.contains("LastFluidColor")) lastFluidColor = tag.getInt("LastFluidColor");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
