package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.List;
import java.util.Optional;

public class AeolusStormTurbineBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, MachineInventory {
    public static final int INVENTORY_SIZE = 1; // Slot 0: Storm Catalyst or RF Item to charge
    public static final int MAX_ENERGY = 1_000_000;
    public static final int MAX_OUTPUT = 10_000;
    public static final int BASE_CLEAR_RATE = 80; // 80 RF/t at 100% altitude & 100% biome
    public static final int SURGE_BONUS_RATE = 2_000; // +2,000 RF/t during Astrape Lightning Surge
    public static final int SURGE_BURST_ENERGY = 75_000; // +75,000 RF instant on lightning strike
    public static final int SURGE_DURATION_TICKS = 200; // 10 seconds

    private static final int[] SLOTS = new int[]{0};

    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            AeolusStormTurbineBlockEntity.this.setChanged();
        }
    };

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(MAX_ENERGY, 0, MAX_OUTPUT) {
        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    private int generationRate = 0;
    private int altitudePct = 100; // 50..250%
    private int biomePct = 100;    // 40..175%
    private int weatherState = 0;  // 0=Blocked, 1=Clear, 2=Rain, 3=Thunder, 4=Lightning Surge
    private int surgeTicks = 0;    // 0..200 ticks
    private int hasLightningRod = 0; // 0 or 1
    private int lastSeenLightningId = -1;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) ((energyStorage.amount >> 16) & 0xFFFF);
                case 1 -> (int) (energyStorage.amount & 0xFFFF);
                case 2 -> generationRate;
                case 3 -> altitudePct;
                case 4 -> biomePct;
                case 5 -> weatherState;
                case 6 -> surgeTicks;
                case 7 -> hasLightningRod;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energyStorage.amount = (energyStorage.amount & 0xFFFFL) | ((long) (value & 0xFFFF) << 16);
                case 1 -> energyStorage.amount = (energyStorage.amount & 0xFFFF0000L) | (value & 0xFFFFL);
                case 2 -> generationRate = value;
                case 3 -> altitudePct = value;
                case 4 -> biomePct = value;
                case 5 -> weatherState = value;
                case 6 -> surgeTicks = value;
                case 7 -> hasLightningRod = value;
            }
        }

        @Override
        public int getCount() {
            return 8;
        }
    };

    public AeolusStormTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.AEOLUS_STORM_TURBINE_BE, pos, state);
    }

    public static boolean isStormCatalyst(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(Items.COPPER_INGOT)
                || stack.is(Items.AMETHYST_SHARD)
                || stack.is(ObsidiumMod.ELECTRUM_FRUIT)
                || stack.is(ObsidiumMod.ZEUS_TIN_FRUIT)
                || stack.is(ObsidiumMod.ASTRAL_VOLCANIC_CATALYST);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AeolusStormTurbineBlockEntity entity) {
        if (level.isClientSide()) return;
        ServerLevel serverLevel = (ServerLevel) level;

        if (entity.surgeTicks > 0) {
            entity.surgeTicks--;
        }

        boolean rodOnTop = level.getBlockState(pos.above()).is(Blocks.LIGHTNING_ROD);
        entity.hasLightningRod = rodOnTop ? 1 : 0;
        BlockPos skyCheckPos = rodOnTop ? pos.above(2) : pos.above();
        boolean canSeeSky = level.canSeeSky(skyCheckPos);

        // 1. Calculate Altitude Wind Factor (50% .. 250%)
        int y = pos.getY();
        int baseAltPct;
        if (y < 60) {
            baseAltPct = 50;
        } else {
            baseAltPct = 75 + Math.min(155, (y - 60) * 155 / 120);
        }
        // Check 4 horizontal directions at rotor height for open airflow (+5% each, up to +20%)
        int openSides = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(dir)).isAir()) {
                openSides++;
            }
        }
        entity.altitudePct = Math.max(50, Math.min(250, baseAltPct + openSides * 5));

        // 2. Calculate Biome Resonance Factor (40% .. 175%)
        Holder<Biome> biomeHolder = level.getBiome(pos);
        Optional<ResourceKey<Biome>> biomeKeyOpt = biomeHolder.unwrapKey();
        String biomePath = biomeKeyOpt.map(k -> k.location().getPath()).orElse("");

        int calcBiomePct = 100;
        if (biomeHolder.is(BiomeTags.IS_MOUNTAIN) || biomePath.contains("peaks") || biomePath.contains("windswept")) {
            calcBiomePct = 175;
        } else if (biomeHolder.is(BiomeTags.IS_OCEAN) || biomeHolder.is(BiomeTags.IS_DEEP_OCEAN)
                || biomeHolder.is(BiomeTags.IS_BEACH) || biomeHolder.is(BiomeTags.IS_RIVER)) {
            calcBiomePct = 140;
        } else if (biomePath.contains("plains") || biomePath.contains("meadow") || biomePath.contains("savanna")) {
            calcBiomePct = 120;
        } else if (biomeHolder.is(BiomeTags.IS_NETHER) || biomeHolder.is(BiomeTags.IS_BADLANDS) || biomePath.contains("desert")) {
            calcBiomePct = 40;
        }
        entity.biomePct = calcBiomePct;

        // 3. Detect natural or attracted Lightning Strikes nearby
        if (canSeeSky && serverLevel.getGameTime() % 5 == 0) {
            List<LightningBolt> bolts = serverLevel.getEntitiesOfClass(LightningBolt.class, new AABB(pos).inflate(4.5));
            for (LightningBolt bolt : bolts) {
                if (bolt.getId() != entity.lastSeenLightningId) {
                    entity.lastSeenLightningId = bolt.getId();
                    entity.triggerAstrapeSurge(serverLevel, pos);
                    break;
                }
            }
        }

        // 4. Attract Lightning Strikes during Thunderstorms
        boolean isThundering = level.isThundering();
        boolean isRaining = level.isRaining();
        ItemStack slotStack = entity.inventory.getItem(0);
        boolean hasCatalyst = isStormCatalyst(slotStack);

        if (canSeeSky && isThundering && entity.surgeTicks <= 20) {
            int strikeChance = hasCatalyst ? 90 : (rodOnTop ? 160 : 340);
            if (level.random.nextInt(strikeChance) == 0) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel);
                if (bolt != null) {
                    BlockPos strikePos = rodOnTop ? pos.above(2) : pos.above();
                    bolt.moveTo(strikePos.getX() + 0.5, strikePos.getY(), strikePos.getZ() + 0.5);
                    bolt.setVisualOnly(true);
                    serverLevel.addFreshEntity(bolt);
                    entity.lastSeenLightningId = bolt.getId();
                }
                if (hasCatalyst && level.random.nextFloat() < 0.35f) {
                    slotStack.shrink(1);
                    entity.setChanged();
                }
                entity.triggerAstrapeSurge(serverLevel, pos);
            }
        }

        // 5. Compute Total Generation Rate (RF/t)
        int rate = 0;
        if (!canSeeSky && entity.surgeTicks <= 0) {
            entity.weatherState = 0;
        } else {
            double weatherMult = 1.0;
            if (isThundering) {
                weatherMult = 4.5;
                entity.weatherState = 3;
            } else if (isRaining) {
                weatherMult = 2.2;
                entity.weatherState = 2;
            } else {
                entity.weatherState = 1;
            }

            if (canSeeSky) {
                double rodBonus = rodOnTop ? 1.20 : 1.0;
                double raw = BASE_CLEAR_RATE
                        * (entity.altitudePct / 100.0)
                        * (entity.biomePct / 100.0)
                        * weatherMult
                        * rodBonus;
                rate = Math.max(5, (int) Math.round(raw));
            }

            if (entity.surgeTicks > 0) {
                entity.weatherState = 4;
                rate += SURGE_BONUS_RATE;
            }
        }

        entity.generationRate = rate;

        // 6. Add Generated Energy to Internal Capacitor
        if (rate > 0 && entity.energyStorage.amount < entity.energyStorage.capacity) {
            entity.energyStorage.amount = Math.min(entity.energyStorage.capacity, entity.energyStorage.amount + rate);
            entity.setChanged();
        }

        // 7. Charge RF-compatible Item in Slot 0 (if not a storm catalyst)
        if (!slotStack.isEmpty() && !hasCatalyst && entity.energyStorage.amount > 0) {
            EnergyStorage itemEnergy = ContainerItemContext.withConstant(slotStack).find(EnergyStorage.ITEM);
            if (itemEnergy != null && itemEnergy.supportsInsertion()) {
                try (Transaction tx = Transaction.openOuter()) {
                    long toCharge = Math.min(entity.energyStorage.amount, 5_000L);
                    long accepted = itemEnergy.insert(toCharge, tx);
                    if (accepted > 0) {
                        entity.energyStorage.amount -= accepted;
                        tx.commit();
                        entity.setChanged();
                    }
                }
            }
        }

        // 8. Push Energy to all 6 adjacent neighbors
        if (entity.energyStorage.amount > 0) {
            for (Direction dir : Direction.values()) {
                EnergyStorage neighbor = EnergyStorage.SIDED.find(level, pos.relative(dir), dir.getOpposite());
                if (neighbor != null && neighbor.supportsInsertion()) {
                    EnergyStorageUtil.move(entity.energyStorage, neighbor, MAX_OUTPUT, null);
                }
            }
        }

        // 9. Update BlockState (ACTIVE & STORM) and ambient effects
        boolean isActive = rate > 0;
        boolean isStormVisual = entity.weatherState >= 3;
        if (state.getValue(AeolusStormTurbineBlock.ACTIVE) != isActive
                || state.getValue(AeolusStormTurbineBlock.STORM) != isStormVisual) {
            level.setBlock(pos, state
                    .setValue(AeolusStormTurbineBlock.ACTIVE, isActive)
                    .setValue(AeolusStormTurbineBlock.STORM, isStormVisual), 3);
        }

        if (isActive && serverLevel.getGameTime() % 40 == 0) {
            float pitch = isStormVisual ? 1.35F : 0.9F + (entity.altitudePct / 300.0F);
            serverLevel.playSound(null, pos, SoundEvents.BREEZE_IDLE_GROUND, SoundSource.BLOCKS, 0.35F, pitch);
        }
    }

    private void triggerAstrapeSurge(ServerLevel level, BlockPos pos) {
        this.surgeTicks = SURGE_DURATION_TICKS;
        this.energyStorage.amount = Math.min(this.energyStorage.capacity, this.energyStorage.amount + SURGE_BURST_ENERGY);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 28, 0.4, 0.5, 0.4, 0.15);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 10, 0.3, 0.4, 0.3, 0.05);
        level.playSound(null, pos, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.BLOCKS, 0.9F, 1.15F);
        setChanged();
    }

    public SimpleContainer getInventory() {
        return this.inventory;
    }

    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
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

    @Override
    public boolean isInputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 0 && isStormCatalyst(stack);
    }

    @Override
    public boolean isOutputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return index == 0 && isStormCatalyst(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.aeolus_storm_turbine");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new AeolusStormTurbineMenu(syncId, playerInventory, this, this.data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.inventory.getItems(), registries);
        tag.putLong("Energy", this.energyStorage.amount);
        tag.putInt("SurgeTicks", this.surgeTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.inventory.clearContent();
        ContainerHelper.loadAllItems(tag, this.inventory.getItems(), registries);
        if (tag.contains("Energy")) {
            this.energyStorage.amount = tag.getLong("Energy");
        }
        if (tag.contains("SurgeTicks")) {
            this.surgeTicks = tag.getInt("SurgeTicks");
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
