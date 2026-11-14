package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.valentin.obsidium.ObsidiumMod;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyItem;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.List;

public class HephaesteanChargingPlateBlockEntity extends BlockEntity {
    public static long getMaxCapacity() {
        return net.valentin.obsidium.config.ModConfig.get().chargingPlateCapacity;
    }

    public static long getMaxReceive() {
        return net.valentin.obsidium.config.ModConfig.get().chargingPlateMaxReceive;
    }

    public static long getMaxPlayerTransfer() {
        return net.valentin.obsidium.config.ModConfig.get().chargingPlateTransferRate;
    }

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(
            net.valentin.obsidium.config.ModConfig.get().chargingPlateCapacity,
            net.valentin.obsidium.config.ModConfig.get().chargingPlateMaxReceive,
            net.valentin.obsidium.config.ModConfig.get().chargingPlateTransferRate
    ) {
        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    /**
     * Sided storage for cables/wires/nodes: ONLY allows insertion into the charging plate.
     * Extraction is disabled so adjacent wires/pipes cannot siphon energy out of the plate!
     */
    public final EnergyStorage sidedEnergyStorage = new EnergyStorage() {
        @Override
        public boolean supportsInsertion() {
            return true;
        }

        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            return energyStorage.insert(maxAmount, transaction);
        }

        @Override
        public boolean supportsExtraction() {
            return false;
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public long getAmount() {
            return energyStorage.getAmount();
        }

        @Override
        public long getCapacity() {
            return energyStorage.getCapacity();
        }
    };

    private int activeTicks = 0;

    public HephaesteanChargingPlateBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.HEPHAESTEAN_CHARGING_PLATE_BE, pos, state);
    }

    /**
     * Charges a single ItemStack directly from the plate's energy buffer.
     * Returns the amount of RF transferred.
     */
    public long chargeStack(ItemStack targetStack, long maxTransferBudget) {
        if (targetStack.isEmpty() || this.energyStorage.amount <= 0 || maxTransferBudget <= 0) return 0;

        if (targetStack.getItem() instanceof SimpleEnergyItem energyItem) {
            long cap = energyItem.getEnergyCapacity(targetStack);
            long cur = energyItem.getStoredEnergy(targetStack);
            long maxIn = energyItem.getEnergyMaxInput(targetStack);
            long needed = cap - cur;
            if (needed > 0 && maxIn > 0) {
                long toMove = Math.min(Math.min(needed, maxTransferBudget), Math.min(maxIn, this.energyStorage.amount));
                if (toMove > 0) {
                    energyItem.setStoredEnergy(targetStack, cur + toMove);
                    this.energyStorage.amount -= toMove;
                    setChanged();
                    return toMove;
                }
            }
        }
        return 0;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HephaesteanChargingPlateBlockEntity entity) {
        if (level.isClientSide()) return;

        boolean isCharging = false;

        // 1. Auto-pull energy from adjacent blocks (especially block below) if needed
        if (entity.energyStorage.amount < entity.energyStorage.capacity) {
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                EnergyStorage neighbor = EnergyStorage.SIDED.find(level, pos.relative(dir), dir.getOpposite());
                if (neighbor != null && neighbor.supportsExtraction()) {
                    long pulled = EnergyStorageUtil.move(neighbor, entity.energyStorage, getMaxReceive(), null);
                    if (pulled > 0) {
                        entity.setChanged();
                        break;
                    }
                }
            }
        }

        if (entity.energyStorage.amount > 0) {
            AABB boundingBox = new AABB(pos).inflate(0.4, 1.2, 0.4);

            // 2. Charge dropped items (ItemEntity) on the plate
            List<ItemEntity> droppedItems = level.getEntitiesOfClass(ItemEntity.class, boundingBox);
            for (ItemEntity itemEntity : droppedItems) {
                if (entity.energyStorage.amount <= 0) break;
                ItemStack stack = itemEntity.getItem();
                long transferred = entity.chargeStack(stack, getMaxPlayerTransfer());
                if (transferred > 0) {
                    itemEntity.setItem(stack);
                    isCharging = true;
                    if (level instanceof ServerLevel serverLevel && level.random.nextFloat() < 0.3F) {
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                itemEntity.getX(), itemEntity.getY() + 0.2, itemEntity.getZ(),
                                2, 0.05, 0.05, 0.05, 0.02);
                    }
                }
            }

            // 3. Charge players standing on, jumping, or adjacent to the plate
            List<Player> players = level.getEntitiesOfClass(Player.class, boundingBox);
            for (Player player : players) {
                if (entity.energyStorage.amount <= 0) break;

                long remainingBudget = getMaxPlayerTransfer();
                InventoryStorage invStorage = InventoryStorage.of(player.getInventory(), null);

                for (int slotIndex = 0; slotIndex < player.getInventory().getContainerSize() && remainingBudget > 0; slotIndex++) {
                    ItemStack targetStack = player.getInventory().getItem(slotIndex);
                    if (targetStack.isEmpty()) continue;

                    // A. SimpleEnergyItem (Volcanite Portable Battery, weapons, armor, etc.)
                    if (targetStack.getItem() instanceof SimpleEnergyItem energyItem) {
                        long cap = energyItem.getEnergyCapacity(targetStack);
                        long cur = energyItem.getStoredEnergy(targetStack);
                        long maxIn = energyItem.getEnergyMaxInput(targetStack);
                        long needed = cap - cur;
                        if (needed > 0 && maxIn > 0) {
                            long toMove = Math.min(Math.min(needed, remainingBudget), Math.min(maxIn, entity.energyStorage.amount));
                            if (toMove > 0) {
                                energyItem.setStoredEnergy(targetStack, cur + toMove);
                                entity.energyStorage.amount -= toMove;
                                remainingBudget -= toMove;
                                entity.setChanged();
                                isCharging = true;
                                continue;
                            }
                        }
                    }

                    // B. Generic EnergyStorage.ITEM API charge (TechReborn / external mod items)
                    SingleSlotStorage<ItemVariant> targetSlot = invStorage.getSlot(slotIndex);
                    ContainerItemContext targetCtx = ContainerItemContext.ofSingleSlot(targetSlot);
                    EnergyStorage itemStorage = EnergyStorage.ITEM.find(targetStack, targetCtx);

                    if (itemStorage != null && itemStorage.supportsInsertion()) {
                        long moved = EnergyStorageUtil.move(entity.energyStorage, itemStorage, remainingBudget, null);
                        if (moved > 0) {
                            remainingBudget -= moved;
                            entity.setChanged();
                            isCharging = true;
                        }
                    }
                }

                if (isCharging) {
                    entity.activeTicks++;
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                player.getX() + (serverLevel.random.nextDouble() - 0.5) * 0.4,
                                player.getY() + serverLevel.random.nextDouble() * 1.2,
                                player.getZ() + (serverLevel.random.nextDouble() - 0.5) * 0.4,
                                2, 0.05, 0.1, 0.05, 0.02);

                        if (entity.activeTicks % 15 == 0) {
                            serverLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.4F, 1.8F);
                        }
                    }
                }
            }
        }

        if (!isCharging) {
            entity.activeTicks = 0;
        }

        boolean currentLit = state.getValue(HephaesteanChargingPlateBlock.LIT);
        if (currentLit != isCharging) {
            level.setBlock(pos, state.setValue(HephaesteanChargingPlateBlock.LIT, isCharging), 3);
            entity.setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putLong("Energy", energyStorage.amount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.energyStorage.amount = tag.getLong("Energy");
    }
}
