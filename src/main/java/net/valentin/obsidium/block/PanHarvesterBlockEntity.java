package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Keeps a configurable minimum of adults per species and harvests surplus into its 9-slot buffer. */
public final class PanHarvesterBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final long MAX_ENERGY = 200_000L;
    public static final long HARVEST_COST = 1_200L;
    public static final int DATA_COUNT = 3;
    private static final int[] OUTPUT_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};

    private final NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(MAX_ENERGY, 5_000L, 0L) {
        @Override
        protected void onFinalCommit() { setChanged(); }
    };

    private int keepAdults = 2;
    private final List<ItemStack> pendingDrops = new ArrayList<>();
    public static final TagKey<EntityType<?>> HARVESTABLE = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("obsidium", "pastoral_harvestable"));

    public static boolean isEligible(Animal animal) {
        return animal.isAlive() && !animal.isBaby() && !animal.hasCustomName()
                && !(animal instanceof TamableAnimal pet && pet.isTame())
                && !(animal instanceof AbstractHorse horse && horse.isTamed())
                && animal.getType().is(HARVESTABLE);
    }

    private boolean flushPendingDrops() {
        boolean changed = false;
        var iterator = pendingDrops.listIterator();
        while (iterator.hasNext()) {
            ItemStack stack = iterator.next();
            int remaining = PastoralInventory.insert(items, OUTPUT_SLOTS, stack);
            if (remaining != stack.getCount()) changed = true;
            if (remaining == 0) iterator.remove();
            else iterator.set(stack.copyWithCount(remaining));
        }
        return changed;
    }

    private boolean hasOutputRoom() {
        for (ItemStack stack : items) if (stack.isEmpty() || stack.getCount() < Math.min(64, stack.getMaxStackSize())) return true;
        return false;
    }

    public PanHarvesterBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.PAN_HARVESTER_BE, pos, state);
    }

    public void cycleKeepAdults() {
        keepAdults = switch (keepAdults) {
            case 2 -> 4;
            case 4 -> 6;
            case 6 -> 8;
            default -> 2;
        };
        setChanged();
    }

    public ContainerData dataAccess() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> (int) (energyStorage.amount / 100L);
                    case 1 -> keepAdults;
                    case 2 -> Math.min(32767, pendingDrops.stream().mapToInt(ItemStack::getCount).sum());
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return DATA_COUNT; }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PanHarvesterBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel) || level.getGameTime() % 30 != 0 || level.hasNeighborSignal(pos)) return;
        AABB box = PastoralInventory.workArea(pos);

        // Sweep any loose drops inside the pen into our output buffer
        boolean changed = be.autoExportOutputs(level, pos);
        changed |= be.flushPendingDrops();
        for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, box)) {
            ItemStack stack = drop.getItem();
            if (!stack.isEmpty() && be.insertOutput(stack.copy())) {
                drop.discard();
                changed = true;
            }
        }

        if (be.energyStorage.amount < HARVEST_COST || !be.pendingDrops.isEmpty() || !be.hasOutputRoom()) {
            if (changed) be.setChanged();
            return;
        }

        // Group eligible adult animals by species
        Map<EntityType<?>, List<Animal>> bySpecies = new HashMap<>();
        for (Animal animal : level.getEntitiesOfClass(Animal.class, box)) {
            if (!isEligible(animal)) continue;
            bySpecies.computeIfAbsent(animal.getType(), k -> new ArrayList<>()).add(animal);
        }

        for (List<Animal> list : bySpecies.values()) {
            if (list.size() > be.keepAdults) {
                Animal target = list.getFirst();
                try (var capture = PastoralHarvestDrops.begin(target)) {
                    try {
                        if (target.hurt(serverLevel.damageSources().genericKill(), Float.MAX_VALUE)) {
                            be.energyStorage.amount -= HARVEST_COST;
                        }
                    } finally {
                        be.pendingDrops.addAll(capture.items());
                        be.flushPendingDrops();
                        be.setChanged();
                    }
                }
                changed = true;
                break;
            }
        }

        if (be.autoExportOutputs(level, pos)) {
            changed = true;
        }

        if (changed) be.setChanged();
    }

    private boolean autoExportOutputs(Level level, BlockPos pos) {
        if (isEmpty()) return false;
        var selfOut = InventoryStorage.of(this, Direction.DOWN);
        boolean movedAny = false;
        for (Direction dir : Direction.values()) {
            BlockPos other = pos.relative(dir);
            if (!level.getChunkSource().hasChunk(other.getX() >> 4, other.getZ() >> 4)) continue;
            var targetBe = level.getBlockEntity(other);
            if (targetBe instanceof AmbrosiaFeederBlockEntity || targetBe instanceof PanHarvesterBlockEntity) continue;
            var ext = ItemStorage.SIDED.find(level, other, dir.getOpposite());
            if (ext == null) continue;
            try (var tx = Transaction.openOuter()) {
                long moved = StorageUtil.move(selfOut, ext, v -> true, 16, tx);
                if (moved > 0) {
                    tx.commit();
                    movedAny = true;
                }
            }
        }
        return movedAny;
    }

    private boolean insertOutput(ItemStack stack) {
        return PastoralInventory.insertAll(items, OUTPUT_SLOTS, stack);
    }

    public void dropPendingContents() {
        if (level != null && !level.isClientSide) {
            for (ItemStack stack : pendingDrops) net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX()+.5, worldPosition.getY()+.5, worldPosition.getZ()+.5, stack);
            pendingDrops.clear();
        }
    }

    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return false; }
    @Override public int[] getSlotsForFace(Direction side) { return OUTPUT_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction dir) { return false; }
    @Override public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction dir) { return true; }

    @Override public int getContainerSize() { return 9; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { var r = ContainerHelper.removeItem(items, slot, amount); if (!r.isEmpty()) setChanged(); return r; }
    @Override public ItemStack removeItemNoUpdate(int slot) { var r = ContainerHelper.takeItem(items, slot); setChanged(); return r; }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); setChanged(); }
    @Override public void clearContent() { items.clear(); setChanged(); }
    @Override public boolean stillValid(Player player) { return net.minecraft.world.Container.stillValidBlockEntity(this, player); }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putLong("Energy", energyStorage.amount);
        tag.putInt("KeepAdults", keepAdults);
        net.minecraft.nbt.ListTag pending = new net.minecraft.nbt.ListTag();
        for (ItemStack stack : pendingDrops) pending.add(stack.save(registries));
        tag.put("PendingDrops", pending);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        energyStorage.amount = Math.clamp(tag.getLong("Energy"), 0L, MAX_ENERGY);
        pendingDrops.clear();
        net.minecraft.nbt.ListTag pending = tag.getList("PendingDrops", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < pending.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(registries, pending.getCompound(i));
            if (!stack.isEmpty()) pendingDrops.add(stack);
        }
        keepAdults = tag.contains("KeepAdults") ? Math.clamp(tag.getInt("KeepAdults"), 2, 8) : 2;
    }
}
