package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyStorage;

public class VolcaniteEnergyCoreBlockEntity extends BlockEntity implements MenuProvider, InventoryHolder {
    public static final long BASE_FORMED_CAPACITY = 100_000_000L;
    public static final long BASIC_CELL_BONUS = 25_000_000L;
    public static final long ADVANCED_CELL_BONUS = 100_000_000L;
    public static final long ADVANCED_CASING_BONUS = 15_000_000L;
    public static final long BASIC_CASING_BONUS = 5_000_000L;
    public static final long UNFORMED_CAPACITY = 5_000_000L;

    private boolean isFormed = false;
    private int basicCellsCount = 0;
    private int advancedCellsCount = 0;
    private long dynamicMaxCapacity = UNFORMED_CAPACITY;

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(900_000_000L, 5_000_000L, 5_000_000L) {
        @Override
        public long getCapacity() {
            return dynamicMaxCapacity;
        }

        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            if (!isFormed && !(VolcaniteEnergyCoreBlockEntity.this instanceof CreativeEnergyCoreBlockEntity)) {
                return 0;
            }
            long space = Math.max(0, getCapacity() - this.amount);
            long capped = Math.min(maxAmount, space);
            return super.insert(capped, transaction);
        }

        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    private int transferMode = 0; // 0 = Charge, 1 = Discharge

    public final SimpleContainer inventory = new SimpleContainer(1) {
        @Override
        public boolean canPlaceItem(int slot, net.minecraft.world.item.ItemStack stack) {
            return !stack.isEmpty() && EnergyStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack)) != null;
        }

        @Override
        public void setChanged() {
            VolcaniteEnergyCoreBlockEntity.this.setChanged();
        }
    };

    @Override
    public net.minecraft.world.Container getInventory() {
        return this.inventory;
    }

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            long amt = energyStorage.amount;
            long cap = energyStorage.getCapacity();
            return switch (index) {
                case 0 -> (int) ((amt >> 32) & 0xFFFF);
                case 1 -> (int) ((amt >> 16) & 0xFFFF);
                case 2 -> (int) (amt & 0xFFFF);
                case 3 -> (int) ((cap >> 32) & 0xFFFF);
                case 4 -> (int) ((cap >> 16) & 0xFFFF);
                case 5 -> (int) (cap & 0xFFFF);
                case 6 -> isFormed ? 2 : 1; // 1 = Unformed Core, 2 = Formed 3x3 Core
                case 7 -> (basicCellsCount & 0xFF) | ((advancedCellsCount & 0xFF) << 8);
                case 8 -> transferMode;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 8) {
                transferMode = value;
                setChanged();
            }
        }

        @Override
        public int getCount() {
            return 16;
        }
    };

    public VolcaniteEnergyCoreBlockEntity(BlockPos pos, BlockState state) {
        this(ObsidiumMod.VOLCANITE_ENERGY_CORE_BE, pos, state);
    }

    protected VolcaniteEnergyCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean isFormed() {
        return isFormed;
    }

    public int getBasicCellsCount() {
        return basicCellsCount;
    }

    public int getAdvancedCellsCount() {
        return advancedCellsCount;
    }

    public long getDynamicMaxCapacity() {
        return dynamicMaxCapacity;
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

    public static boolean isValidMultiblockRingBlock(Block b) {
        return b == ObsidiumMod.VOLCANITE_ENERGY_CELL_BLOCK ||
               b == ObsidiumMod.VOLCANITE_ENERGY_UNIT_BLOCK ||
               b == ObsidiumMod.VOLCANITE_CASING_BLOCK ||
               b == ObsidiumMod.ADVANCED_VOLCANITE_CASING_BLOCK;
    }

    @Nullable
    public static VolcaniteEnergyCoreBlockEntity findFormedCoreForRingBlock(Level level, BlockPos pos) {
        if (level == null) return null;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos checkPos = pos.offset(dx, 0, dz);
                BlockEntity be = level.getBlockEntity(checkPos);
                if (be instanceof VolcaniteEnergyCoreBlockEntity core && !(core instanceof CreativeEnergyCoreBlockEntity) && core.isFormed()) {
                    return core;
                }
            }
        }
        return null;
    }

    public void evaluateMultiblock(Level level, BlockPos pos) {
        int basicCells = 0;
        int advCells = 0;
        int basicCasings = 0;
        int advCasings = 0;
        boolean allValid = true;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                Block b = level.getBlockState(pos.offset(dx, 0, dz)).getBlock();
                if (b == ObsidiumMod.VOLCANITE_ENERGY_CELL_BLOCK) {
                    basicCells++;
                } else if (b == ObsidiumMod.VOLCANITE_ENERGY_UNIT_BLOCK) {
                    advCells++;
                } else if (b == ObsidiumMod.VOLCANITE_CASING_BLOCK) {
                    basicCasings++;
                } else if (b == ObsidiumMod.ADVANCED_VOLCANITE_CASING_BLOCK) {
                    advCasings++;
                } else {
                    allValid = false;
                }
            }
        }

        boolean newFormed = allValid && (basicCells + advCells) >= 4;
        if (newFormed != this.isFormed || basicCells != this.basicCellsCount || advCells != this.advancedCellsCount) {
            boolean wasFormed = this.isFormed;
            this.isFormed = newFormed;
            this.basicCellsCount = basicCells;
            this.advancedCellsCount = advCells;

            if (newFormed) {
                this.dynamicMaxCapacity = BASE_FORMED_CAPACITY
                        + (basicCells * BASIC_CELL_BONUS)
                        + (advCells * ADVANCED_CELL_BONUS)
                        + (advCasings * ADVANCED_CASING_BONUS)
                        + (basicCasings * BASIC_CASING_BONUS);
                if (!wasFormed) {
                    level.playSound(null, pos, net.valentin.obsidium.sound.ModSounds.MULTIBLOCK_FORM, SoundSource.BLOCKS, 0.9F, 1.1F);
                    if (level instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 30, 1.1, 0.25, 1.1, 0.08);
                    }
                }
            } else {
                this.dynamicMaxCapacity = UNFORMED_CAPACITY;
                if (this.energyStorage.amount > this.dynamicMaxCapacity) {
                    this.energyStorage.amount = this.dynamicMaxCapacity;
                }
                if (wasFormed) {
                    level.playSound(null, pos, net.valentin.obsidium.sound.ModSounds.MULTIBLOCK_BREAK, SoundSource.BLOCKS, 0.8F, 1.0F);
                }
            }
            setChanged();
        }

        // Pool any energy sitting inside the surrounding Basic/Advanced Energy Cells into the Core
        if (this.isFormed) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    BlockPos cellPos = pos.offset(dx, 0, dz);
                    BlockEntity be = level.getBlockEntity(cellPos);
                    if (be instanceof VolcaniteEnergyCellBlockEntity basicCell && basicCell.energyStorage.amount > 0) {
                        long space = this.dynamicMaxCapacity - this.energyStorage.amount;
                        long move = Math.min(space, basicCell.energyStorage.amount);
                        if (move > 0) {
                            basicCell.energyStorage.amount -= move;
                            this.energyStorage.amount += move;
                            basicCell.setChanged();
                            this.setChanged();
                        }
                    } else if (be instanceof VolcaniteEnergyUnitBlockEntity advCell && advCell.energyStorage.amount > 0) {
                        long space = this.dynamicMaxCapacity - this.energyStorage.amount;
                        long move = Math.min(space, advCell.energyStorage.amount);
                        if (move > 0) {
                            advCell.energyStorage.amount -= move;
                            this.energyStorage.amount += move;
                            advCell.setChanged();
                            this.setChanged();
                        }
                    }
                }
            }
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VolcaniteEnergyCoreBlockEntity entity) {
        if (level.isClientSide()) return;

        entity.evaluateMultiblock(level, pos);

        if (!entity.isFormed || entity.energyStorage.amount == 0) return;

        boolean didSomething = false;

        // Push energy out from the entire 3x3 Multiblock footprint to external adjacent targets
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos memberPos = pos.offset(dx, 0, dz);
                for (Direction dir : Direction.values()) {
                    BlockPos targetPos = memberPos.relative(dir);
                    // Skip blocks that are part of this 3x3 multiblock itself
                    if (targetPos.getY() == pos.getY()
                            && Math.abs(targetPos.getX() - pos.getX()) <= 1
                            && Math.abs(targetPos.getZ() - pos.getZ()) <= 1) {
                        continue;
                    }
                    if (level.getBlockEntity(targetPos) instanceof AstrapeEnergyPylonBlockEntity) continue;
                    EnergyStorage target = EnergyStorage.SIDED.find(level, targetPos, dir.getOpposite());
                    if (target != null && target != entity.energyStorage) {
                        long moved = EnergyStorageUtil.move(entity.energyStorage, target, 1_000_000L, null);
                        if (moved > 0) didSomething = true;
                    }
                }
            }
        }

        // Charge / Discharge item in slot
        if (!entity.inventory.isEmpty()) {
            net.minecraft.world.item.ItemStack stack = entity.inventory.getItem(0);
            SingleSlotStorage<ItemVariant> slot = InventoryStorage.of(entity.inventory, null).getSlot(0);
            ContainerItemContext ctx = ContainerItemContext.ofSingleSlot(slot);
            EnergyStorage itemStorage = EnergyStorage.ITEM.find(stack, ctx);
            if (itemStorage != null) {
                if (entity.transferMode == 0) {
                    if (itemStorage.supportsInsertion()) {
                        long moved = EnergyStorageUtil.move(entity.energyStorage, itemStorage, 2_000_000L, null);
                        if (moved > 0) didSomething = true;
                    }
                } else {
                    if (itemStorage.supportsExtraction() && entity.energyStorage.amount < entity.energyStorage.getCapacity()) {
                        long space = entity.energyStorage.getCapacity() - entity.energyStorage.amount;
                        long toMove = Math.min(2_000_000L, space);
                        long moved = EnergyStorageUtil.move(itemStorage, entity.energyStorage, toMove, null);
                        if (moved > 0) didSomething = true;
                    }
                }
            }
        }

        if (didSomething) entity.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putLong("Energy", energyStorage.amount);
        tag.putInt("TransferMode", transferMode);
        tag.putBoolean("Formed", isFormed);
        tag.putInt("BasicCells", basicCellsCount);
        tag.putInt("AdvCells", advancedCellsCount);
        tag.putLong("DynamicCap", dynamicMaxCapacity);
        MachineInventory.saveInventory(tag, inventory, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.isFormed = tag.getBoolean("Formed");
        this.basicCellsCount = tag.getInt("BasicCells");
        this.advancedCellsCount = tag.getInt("AdvCells");
        this.dynamicMaxCapacity = tag.contains("DynamicCap") ? tag.getLong("DynamicCap") : (isFormed ? BASE_FORMED_CAPACITY : UNFORMED_CAPACITY);
        this.energyStorage.amount = tag.getLong("Energy");
        if (tag.contains("TransferMode")) this.transferMode = tag.getInt("TransferMode");
        MachineInventory.loadInventory(tag, this.inventory, provider);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new VolcaniteBatteryMenu(syncId, playerInventory, this.data, this.inventory);
    }
}
