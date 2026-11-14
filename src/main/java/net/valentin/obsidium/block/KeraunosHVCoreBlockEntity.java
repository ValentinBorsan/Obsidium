package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.FaradayArmorItem;
import net.valentin.obsidium.item.HVDielectricHotStickItem;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.ArrayList;
import java.util.List;

public class KeraunosHVCoreBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final long UNFORMED_CAPACITY = 10_000_000L;
    public static final long BASE_FORMED_CAPACITY = 1_000_000_000L;
    public static final long BASIC_CELL_BONUS = 150_000_000L;
    public static final long ADV_UNIT_BONUS = 600_000_000L;
    public static final long STANDARD_PORT_RATE = 25_000_000L;
    public static final long OVERVOLTAGE_PORT_RATE = 50_000_000L;

    public static final int INVENTORY_SIZE = 1;
    public static final int DATA_COUNT = 13;

    private boolean isFormed = false;
    private boolean serviceMode = false;
    private boolean overvoltageMode = false;
    private int transferMode = 0; // 0 = Charge item, 1 = Discharge item

    private int portsCount = 0;
    private int basicCellsCount = 0;
    private int advancedCellsCount = 0;
    private int invalidBlocksCount = 0;
    private long dynamicMaxCapacity = UNFORMED_CAPACITY;
    private int lastThroughputRft = 0;
    private int arcTickCounter = 0;
    private long lastBlastGameTime = -100L;

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(16_000_000_000L, OVERVOLTAGE_PORT_RATE, OVERVOLTAGE_PORT_RATE) {
        @Override
        public long getCapacity() {
            return dynamicMaxCapacity;
        }

        @Override
        public long insert(long maxAmount, net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction) {
            if (!isFormed || serviceMode) return 0;
            long currentCap = getCapacity();
            long space = Math.max(0L, currentCap - amount);
            if (space <= 0L) return 0L;
            return super.insert(Math.min(maxAmount, space), transaction);
        }

        @Override
        public long extract(long maxAmount, net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction) {
            if (!isFormed || serviceMode) return 0;
            return super.extract(maxAmount, transaction);
        }

        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    public final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            KeraunosHVCoreBlockEntity.this.setChanged();
        }
    };

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            long amt = energyStorage.amount;
            long cap = dynamicMaxCapacity;
            return switch (index) {
                case 0 -> (int) ((amt >> 32) & 0xFFFF);
                case 1 -> (int) ((amt >> 16) & 0xFFFF);
                case 2 -> (int) (amt & 0xFFFF);
                case 3 -> (int) ((cap >> 32) & 0xFFFF);
                case 4 -> (int) ((cap >> 16) & 0xFFFF);
                case 5 -> (int) (cap & 0xFFFF);
                case 6 -> getVoltageKv();
                case 7 -> portsCount;
                case 8 -> (basicCellsCount & 0xFF) | ((advancedCellsCount & 0xFF) << 8);
                case 9 -> (isFormed ? 1 : 0)
                        | (serviceMode ? 2 : 0)
                        | (overvoltageMode ? 4 : 0)
                        | (transferMode == 1 ? 8 : 0);
                case 10 -> (lastThroughputRft >> 16) & 0xFFFF;
                case 11 -> lastThroughputRft & 0xFFFF;
                case 12 -> invalidBlocksCount;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 9) {
                // Handled via clickMenuButton on server
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public KeraunosHVCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.KERAUNOS_HV_CORE_BLOCK_ENTITY, pos, state);
    }

    public boolean isFormed() {
        return isFormed;
    }

    public boolean isServiceMode() {
        return serviceMode;
    }

    public boolean isOvervoltageMode() {
        return overvoltageMode;
    }

    public boolean isEnergized() {
        return isFormed && !serviceMode && energyStorage.amount > 0L;
    }

    public int getVoltageKv() {
        if (!isEnergized() || dynamicMaxCapacity <= 0L) return 0;
        double ratio = Math.min(1.0, Math.max(0.0, (double) energyStorage.amount / (double) dynamicMaxCapacity));
        int baseKv = 100 + (int) Math.round(900.0 * ratio);
        return overvoltageMode ? baseKv * 2 : baseKv;
    }

    public long getPortTransferRate() {
        return overvoltageMode ? OVERVOLTAGE_PORT_RATE : STANDARD_PORT_RATE;
    }

    public void setServiceMode(boolean grounded) {
        this.serviceMode = grounded;
        if (this.level != null && !this.level.isClientSide()) {
            BlockState state = getBlockState();
            if (state.getBlock() instanceof KeraunosHVCoreBlock) {
                this.level.setBlock(this.worldPosition,
                        state.setValue(KeraunosHVCoreBlock.GROUNDED, this.serviceMode)
                             .setValue(KeraunosHVCoreBlock.ACTIVE, isEnergized()),
                        Block.UPDATE_ALL);
            }
        }
        setChanged();
    }

    public void toggleServiceMode(@Nullable Player player) {
        setServiceMode(!this.serviceMode);
        if (level != null && !level.isClientSide()) {
            level.playSound(null, worldPosition,
                    serviceMode ? SoundEvents.IRON_DOOR_CLOSE : SoundEvents.BEACON_ACTIVATE,
                    SoundSource.BLOCKS, 0.9F, serviceMode ? 0.7F : 1.4F);
            if (player != null) {
                player.displayClientMessage(serviceMode
                        ? Component.literal("⏚ Keraunos Core GROUNDED (0 kV — Safe for Maintenance)").withStyle(ChatFormatting.GREEN)
                        : Component.literal("⚡ Keraunos Core ENERGIZED (" + getVoltageKv() + " kV)").withStyle(ChatFormatting.AQUA),
                        true);
            }
        }
    }

    public void toggleOvervoltageMode() {
        this.overvoltageMode = !this.overvoltageMode;
        if (level != null && !level.isClientSide()) {
            level.playSound(null, worldPosition, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 0.7F, overvoltageMode ? 1.6F : 1.0F);
        }
        setChanged();
    }

    public void toggleTransferMode() {
        this.transferMode = (this.transferMode == 0) ? 1 : 0;
        setChanged();
    }

    public static boolean isValidShellBlock(Block b) {
        return b == ObsidiumMod.KERAUNOS_HV_PORT_BLOCK
                || b == ObsidiumMod.ADVANCED_VOLCANITE_CASING_BLOCK
                || b == ObsidiumMod.VOLCANITE_CASING_BLOCK
                || b == ObsidiumMod.VOLCANITE_GLASS_BLOCK
                || b == ObsidiumMod.SUPREME_VOLCANITE_GLASS_BLOCK
                || b == ObsidiumMod.REINFORCED_TINTED_GLASS_BLOCK
                || b == ObsidiumMod.VOLCANITE_ENERGY_CELL_BLOCK
                || b == ObsidiumMod.VOLCANITE_ENERGY_UNIT_BLOCK;
    }

    @Nullable
    public static KeraunosHVCoreBlockEntity findFormedCoreForShellBlock(Level level, BlockPos pos) {
        if (level == null) return null;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos checkPos = pos.offset(dx, dy, dz);
                    if (level.getBlockEntity(checkPos) instanceof KeraunosHVCoreBlockEntity core && core.isFormed()) {
                        return core;
                    }
                }
            }
        }
        return null;
    }

    public void evaluateMultiblock(Level level, BlockPos pos) {
        int ports = 0;
        int basic = 0;
        int adv = 0;
        int invalid = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos p = pos.offset(dx, dy, dz);
                    Block b = level.getBlockState(p).getBlock();
                    if (!isValidShellBlock(b)) {
                        invalid++;
                    } else if (b == ObsidiumMod.KERAUNOS_HV_PORT_BLOCK) {
                        ports++;
                    } else if (b == ObsidiumMod.VOLCANITE_ENERGY_CELL_BLOCK) {
                        basic++;
                    } else if (b == ObsidiumMod.VOLCANITE_ENERGY_UNIT_BLOCK) {
                        adv++;
                    }
                }
            }
        }

        boolean newFormed = (invalid == 0 && ports >= 1);
        long newCap = newFormed
                ? BASE_FORMED_CAPACITY + (long) basic * BASIC_CELL_BONUS + (long) adv * ADV_UNIT_BONUS
                : UNFORMED_CAPACITY;

        boolean changed = (newFormed != isFormed) || (ports != portsCount)
                || (basic != basicCellsCount) || (adv != advancedCellsCount)
                || (invalid != invalidBlocksCount) || (newCap != dynamicMaxCapacity);

        this.isFormed = newFormed;
        this.portsCount = ports;
        this.basicCellsCount = basic;
        this.advancedCellsCount = adv;
        this.invalidBlocksCount = invalid;
        this.dynamicMaxCapacity = newCap;

        if (this.energyStorage.amount > this.dynamicMaxCapacity) {
            this.energyStorage.amount = this.dynamicMaxCapacity;
            changed = true;
        }

        if (isFormed) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos p = pos.offset(dx, dy, dz);
                        BlockEntity ringBe = level.getBlockEntity(p);
                        if (ringBe instanceof VolcaniteEnergyCellBlockEntity cell && cell.energyStorage.amount > 0) {
                            long space = this.dynamicMaxCapacity - this.energyStorage.amount;
                            if (space > 0) {
                                long transfer = Math.min(space, cell.energyStorage.amount);
                                cell.energyStorage.amount -= transfer;
                                this.energyStorage.amount += transfer;
                                cell.setChanged();
                                changed = true;
                            }
                        } else if (ringBe instanceof VolcaniteEnergyUnitBlockEntity unit && unit.energyStorage.amount > 0) {
                            long space = this.dynamicMaxCapacity - this.energyStorage.amount;
                            if (space > 0) {
                                long transfer = Math.min(space, unit.energyStorage.amount);
                                unit.energyStorage.amount -= transfer;
                                this.energyStorage.amount += transfer;
                                unit.setChanged();
                                changed = true;
                            }
                        }
                    }
                }
            }
        }

        BlockState state = getBlockState();
        if (state.getBlock() instanceof KeraunosHVCoreBlock) {
            boolean activeVal = isEnergized();
            if (state.getValue(KeraunosHVCoreBlock.ACTIVE) != activeVal
                    || state.getValue(KeraunosHVCoreBlock.GROUNDED) != serviceMode) {
                level.setBlock(pos, state
                        .setValue(KeraunosHVCoreBlock.ACTIVE, activeVal)
                        .setValue(KeraunosHVCoreBlock.GROUNDED, serviceMode), Block.UPDATE_ALL);
            }
        }

        if (changed) setChanged();
    }

    public void handlePlayerInteraction(Level level, BlockPos interactionPos, Player player) {
        boolean holdingHotStick = player.getMainHandItem().getItem() instanceof HVDielectricHotStickItem
                || player.getOffhandItem().getItem() instanceof HVDielectricHotStickItem;

        if (isEnergized()) {
            if (!holdingHotStick) {
                if (!player.getAbilities().instabuild) {
                    float dmg = FaradayArmorItem.hasFullSuit(player) ? 2.0F : 6.0F;
                    player.hurt(level.damageSources().lightningBolt(), dmg);
                    Vec3 push = player.position().subtract(Vec3.atCenterOf(interactionPos)).normalize().scale(0.6);
                    player.push(push.x, 0.28, push.z);
                    player.hurtMarked = true;
                }
                if (level instanceof ServerLevel sl) {
                    Vec3 c = Vec3.atCenterOf(interactionPos);
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y + 0.5, c.z, 24, 0.4, 0.4, 0.4, 0.25);
                    sl.playSound(null, interactionPos, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 0.7F, 1.7F);
                }
                player.displayClientMessage(
                        Component.literal("⚡ DANGER: HIGH VOLTAGE (" + getVoltageKv() + " kV)! Hold an HV Dielectric Hot-Stick to operate!")
                                .withStyle(ChatFormatting.RED), true);
                return;
            }

            if (player.isShiftKeyDown()) {
                toggleServiceMode(player);
                return;
            }
        } else if (holdingHotStick && player.isShiftKeyDown()) {
            toggleServiceMode(player);
            return;
        }

        player.openMenu(this);
    }

    public void triggerArcBlast(Level level, BlockPos originPos, @Nullable Player breaker) {
        if (!(level instanceof ServerLevel sl)) return;
        long now = sl.getGameTime();
        if (now == lastBlastGameTime) return;
        lastBlastGameTime = now;

        Vec3 center = Vec3.atCenterOf(originPos);
        sl.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y, center.z, 2, 0.0, 0.0, 0.0, 0.0);
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 70, 2.2, 2.2, 2.2, 0.45);
        sl.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 3, 0.2, 0.2, 0.2, 0.0);
        sl.playSound(null, originPos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 1.4F, 1.2F);
        sl.playSound(null, originPos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.8F, 1.5F);

        List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, new AABB(originPos).inflate(6.0));
        for (LivingEntity entity : victims) {
            if (entity instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
            boolean fullSuit = FaradayArmorItem.hasFullSuit(entity);
            float dmg = fullSuit ? 5.0F : 16.0F;
            entity.hurt(fullSuit ? sl.damageSources().explosion(null, null) : sl.damageSources().lightningBolt(), dmg);
            Vec3 push = entity.position().subtract(center);
            if (push.lengthSqr() > 0.001) {
                Vec3 n = push.normalize();
                entity.push(n.x * 1.1, 0.45, n.z * 1.1);
                entity.hurtMarked = true;
            }
            if (entity instanceof Player p) {
                p.displayClientMessage(Component.literal("💥 HIGH-VOLTAGE ARC BLAST! Ground the Keraunos Core with a Hot-Stick before dismantling!").withStyle(ChatFormatting.RED), true);
            }
        }

        long lost = Math.max(50_000L, this.energyStorage.amount / 4L);
        this.energyStorage.amount = Math.max(0L, this.energyStorage.amount - lost);
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, KeraunosHVCoreBlockEntity entity) {
        if (level.isClientSide()) return;

        entity.evaluateMultiblock(level, pos);

        boolean didSomething = false;
        long totalMovedThisTick = 0L;

        // 1. Push energy out through all KeraunosHVPortBlock blocks in the 3x3x3 shell
        if (entity.isFormed && !entity.serviceMode && entity.energyStorage.amount > 0) {
            List<BlockPos> portPositions = new ArrayList<>();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos shellPos = pos.offset(dx, dy, dz);
                        if (level.getBlockState(shellPos).getBlock() == ObsidiumMod.KERAUNOS_HV_PORT_BLOCK) {
                            portPositions.add(shellPos);
                        }
                    }
                }
            }

            long perPortRate = entity.getPortTransferRate();
            for (BlockPos portPos : portPositions) {
                if (entity.energyStorage.amount <= 0) break;
                for (Direction dir : Direction.values()) {
                    BlockPos outsidePos = portPos.relative(dir);
                    // Only push outside the 3x3x3 footprint
                    if (Math.abs(outsidePos.getX() - pos.getX()) <= 1
                            && Math.abs(outsidePos.getY() - pos.getY()) <= 1
                            && Math.abs(outsidePos.getZ() - pos.getZ()) <= 1) {
                        continue;
                    }
                    BlockEntity targetBe = level.getBlockEntity(outsidePos);
                    if (targetBe instanceof AstrapeEnergyPylonBlockEntity) continue;
                    EnergyStorage target = EnergyStorage.SIDED.find(level, outsidePos, dir.getOpposite());
                    if (target != null && target.supportsInsertion()) {
                        long moved = EnergyStorageUtil.move(entity.energyStorage, target, perPortRate, null);
                        if (moved > 0) {
                            didSomething = true;
                            totalMovedThisTick += moved;
                        }
                    }
                }
            }
        }

        // 2. Supercharge / Discharge item in slot 0 (allowed whenever not destroyed, rate = 10M RF/t)
        if (!entity.inventory.isEmpty()) {
            ItemStack stack = entity.inventory.getItem(0);
            SingleSlotStorage<ItemVariant> slot = InventoryStorage.of(entity.inventory, null).getSlot(0);
            ContainerItemContext ctx = ContainerItemContext.ofSingleSlot(slot);
            EnergyStorage itemStorage = EnergyStorage.ITEM.find(stack, ctx);
            if (itemStorage != null) {
                if (entity.transferMode == 0) {
                    if (itemStorage.supportsInsertion() && entity.energyStorage.amount > 0) {
                        long prev = entity.serviceMode ? 0L : Math.min(10_000_000L, entity.energyStorage.amount);
                        if (prev > 0) {
                            long moved = EnergyStorageUtil.move(entity.energyStorage, itemStorage, 10_000_000L, null);
                            if (moved > 0) {
                                didSomething = true;
                                totalMovedThisTick += moved;
                            }
                        }
                    }
                } else {
                    if (itemStorage.supportsExtraction() && entity.energyStorage.amount < entity.dynamicMaxCapacity && !entity.serviceMode) {
                        long space = entity.dynamicMaxCapacity - entity.energyStorage.amount;
                        long moved = EnergyStorageUtil.move(itemStorage, entity.energyStorage, Math.min(10_000_000L, space), null);
                        if (moved > 0) {
                            didSomething = true;
                            totalMovedThisTick += moved;
                        }
                    }
                }
            }
        }

        entity.lastThroughputRft = (int) Math.min(Integer.MAX_VALUE, totalMovedThisTick);

        // 3. Proximity High-Voltage Arc Hazard
        entity.arcTickCounter++;
        if (entity.arcTickCounter >= 15) {
            entity.arcTickCounter = 0;
            if (entity.isFormed && !entity.serviceMode && entity.energyStorage.amount > 100_000L && level instanceof ServerLevel sl) {
                double radius = entity.overvoltageMode ? 6.5 : 4.5;
                int kv = entity.getVoltageKv();
                List<LivingEntity> nearby = sl.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(radius));
                Vec3 coreCenter = Vec3.atCenterOf(pos);

                for (LivingEntity target : nearby) {
                    if (target instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
                    if (FaradayArmorItem.hasFullSuit(target)) continue;

                    int pieces = FaradayArmorItem.getEquippedPieceCount(target);
                    float baseDmg = 3.5F + (kv / 250.0F);
                    float mitigatedDmg = Math.max(2.0F, baseDmg * (1.0F - pieces * 0.15F));

                    target.hurt(sl.damageSources().lightningBolt(), mitigatedDmg);
                    Vec3 away = target.position().subtract(coreCenter);
                    if (away.lengthSqr() > 0.001) {
                        Vec3 norm = away.normalize();
                        target.push(norm.x * 0.35, 0.15, norm.z * 0.35);
                        target.hurtMarked = true;
                    }

                    // Draw electric arc beam from core to entity
                    Vec3 dest = target.getEyePosition();
                    for (int s = 1; s <= 10; s++) {
                        double t = s / 10.0;
                        double px = coreCenter.x + (dest.x - coreCenter.x) * t + (sl.random.nextDouble() - 0.5) * 0.2;
                        double py = coreCenter.y + (dest.y - coreCenter.y) * t + (sl.random.nextDouble() - 0.5) * 0.2;
                        double pz = coreCenter.z + (dest.z - coreCenter.z) * t + (sl.random.nextDouble() - 0.5) * 0.2;
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 1, 0.02, 0.02, 0.02, 0.02);
                    }
                    sl.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 0.35F, 1.85F);

                    if (target instanceof Player p) {
                        p.displayClientMessage(Component.literal("⚡ HV Arc Field (" + kv + " kV)! Wear full Aether-Faraday Suit (" + pieces + "/4)!").withStyle(ChatFormatting.YELLOW), true);
                    }

                    long arcCost = Math.min(entity.energyStorage.amount, (long) kv * 25L);
                    if (arcCost > 0) {
                        entity.energyStorage.amount -= arcCost;
                        didSomething = true;
                    }
                }
            }
        }

        if (didSomething) entity.setChanged();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putLong("Energy", energyStorage.amount);
        tag.putBoolean("Formed", isFormed);
        tag.putBoolean("ServiceMode", serviceMode);
        tag.putBoolean("OvervoltageMode", overvoltageMode);
        tag.putInt("TransferMode", transferMode);
        tag.putInt("Ports", portsCount);
        tag.putInt("BasicCells", basicCellsCount);
        tag.putInt("AdvCells", advancedCellsCount);
        tag.putLong("DynamicCap", dynamicMaxCapacity);
        MachineInventory.saveInventory(tag, inventory, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.isFormed = tag.getBoolean("Formed");
        this.serviceMode = tag.getBoolean("ServiceMode");
        this.overvoltageMode = tag.getBoolean("OvervoltageMode");
        this.transferMode = tag.getInt("TransferMode");
        this.portsCount = tag.getInt("Ports");
        this.basicCellsCount = tag.getInt("BasicCells");
        this.advancedCellsCount = tag.getInt("AdvCells");
        this.dynamicMaxCapacity = tag.contains("DynamicCap") ? tag.getLong("DynamicCap") : (isFormed ? BASE_FORMED_CAPACITY : UNFORMED_CAPACITY);
        this.energyStorage.amount = tag.getLong("Energy");
        MachineInventory.loadInventory(tag, this.inventory, provider);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new KeraunosHVCoreMenu(syncId, playerInventory, this, this.data, this.worldPosition);
    }

    // --- Container delegation for drops & Menu ---
    @Override
    public int getContainerSize() {
        return inventory.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return inventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventory.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return inventory.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return inventory.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory.setItem(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
    }
}
