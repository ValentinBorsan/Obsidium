package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.List;

/** Pastoral nursery & feeder: breeds adults, accelerates babies, shears sheep, and gathers eggs. */
public final class AmbrosiaFeederBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final long MAX_ENERGY = 200_000L;
    public static final int DATA_COUNT = 2;
    private static final int[] INPUT_SLOTS = {0, 1, 2, 3, 4, 5};
    private static final int[] OUTPUT_SLOTS = {6, 7, 8};
    private static final int[] ALL_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};

    private int nearbyAnimals;
    private static final TagKey<Item> EXTRA_FEED = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("obsidium", "pastoral_feed"));
    private static final List<TagKey<Item>> FOOD_TAGS = List.of(
            ItemTags.SNIFFER_FOOD, ItemTags.FOX_FOOD, ItemTags.COW_FOOD, ItemTags.GOAT_FOOD,
            ItemTags.SHEEP_FOOD, ItemTags.WOLF_FOOD, ItemTags.CAT_FOOD, ItemTags.HORSE_FOOD,
            ItemTags.CAMEL_FOOD, ItemTags.ARMADILLO_FOOD, ItemTags.BEE_FOOD, ItemTags.CHICKEN_FOOD,
            ItemTags.FROG_FOOD, ItemTags.HOGLIN_FOOD, ItemTags.LLAMA_FOOD, ItemTags.OCELOT_FOOD,
            ItemTags.PANDA_FOOD, ItemTags.PIG_FOOD, ItemTags.RABBIT_FOOD, ItemTags.STRIDER_FOOD,
            ItemTags.TURTLE_FOOD, ItemTags.PARROT_FOOD, ItemTags.AXOLOTL_FOOD, EXTRA_FEED);

    private final NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(MAX_ENERGY, 5_000L, 0L) {
        @Override
        protected void onFinalCommit() { setChanged(); }
    };

    public AmbrosiaFeederBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.AMBROSIA_FEEDER_BE, pos, state);
    }

    public ContainerData dataAccess() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> (int) (energyStorage.amount / 100L);
                    case 1 -> nearbyAnimals;
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return DATA_COUNT; }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AmbrosiaFeederBlockEntity be) {
        if (level.getGameTime() % 20 != 0) return;
        AABB box = PastoralInventory.workArea(pos);
        List<Animal> animals = level.getEntitiesOfClass(Animal.class, box, Animal::isAlive);
        be.nearbyAnimals = animals.size();
        if (level.hasNeighborSignal(pos)) return;
        boolean changed = false;

        // 1. Nursery Heat: speed up baby growth
        for (Animal animal : animals) {
            if (animal.isBaby() && be.energyStorage.amount >= 200L) {
                be.energyStorage.amount -= 200L;
                animal.ageUp(40);
                changed = true;
            }
        }

        // 2. Gentle Shear: shear ready sheep into output slots 6..8
        for (Animal animal : animals) {
            if (animal instanceof Sheep sheep && sheep.readyForShearing() && be.energyStorage.amount >= 200L) {
                ItemStack wool = new ItemStack(woolForColor(sheep.getColor()), 1 + level.random.nextInt(3));
                if (be.insertOutput(wool)) {
                    be.energyStorage.amount -= 200L;
                    sheep.setSheared(true);
                    changed = true;
                }
            }
        }

        // 3. Collect Eggs on the ground into output slots 6..8
        for (ItemEntity itemEnt : level.getEntitiesOfClass(ItemEntity.class, box)) {
            ItemStack stack = itemEnt.getItem();
            if (stack.is(Items.EGG) && be.insertOutput(stack.copy())) {
                itemEnt.discard();
                changed = true;
            }
        }

        // 4. Breed ready adults (cap at 16 animals in pen to avoid lag)
        if (animals.size() < 16 && be.energyStorage.amount >= 400L) {
            for (Animal animal : animals) {
                if (!animal.isBaby() && animal.getAge() == 0 && !animal.isInLove()) {
                    if (be.consumeMatchingFood(animal)) {
                        be.energyStorage.amount -= 400L;
                        animal.setInLove(null);
                        changed = true;
                        break;
                    }
                }
            }
        }

        if (be.autoExportOutputs(level, pos)) {
            changed = true;
        }

        if (changed) be.setChanged();
    }

    private boolean autoExportOutputs(Level level, BlockPos pos) {
        boolean hasOutput = false;
        for (int s : OUTPUT_SLOTS) {
            if (!items.get(s).isEmpty()) { hasOutput = true; break; }
        }
        if (!hasOutput) return false;
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

    private boolean consumeMatchingFood(Animal animal) {
        for (int slot : INPUT_SLOTS) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty() && animal.isFood(stack)) {
                ItemStack remainder = stack.is(Items.TROPICAL_FISH_BUCKET) ? new ItemStack(Items.WATER_BUCKET)
                        : stack.getItem().hasCraftingRemainingItem() ? new ItemStack(stack.getItem().getCraftingRemainingItem()) : ItemStack.EMPTY;
                if (!remainder.isEmpty() && !insertOutput(remainder)) continue;
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private boolean insertOutput(ItemStack stack) {
        return PastoralInventory.insertAll(items, OUTPUT_SLOTS, stack);
    }

    private static net.minecraft.world.item.Item woolForColor(DyeColor color) {
        return switch (color) {
            case WHITE -> Items.WHITE_WOOL;
            case ORANGE -> Items.ORANGE_WOOL;
            case MAGENTA -> Items.MAGENTA_WOOL;
            case LIGHT_BLUE -> Items.LIGHT_BLUE_WOOL;
            case YELLOW -> Items.YELLOW_WOOL;
            case LIME -> Items.LIME_WOOL;
            case PINK -> Items.PINK_WOOL;
            case GRAY -> Items.GRAY_WOOL;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_WOOL;
            case CYAN -> Items.CYAN_WOOL;
            case PURPLE -> Items.PURPLE_WOOL;
            case BLUE -> Items.BLUE_WOOL;
            case BROWN -> Items.BROWN_WOOL;
            case GREEN -> Items.GREEN_WOOL;
            case RED -> Items.RED_WOOL;
            case BLACK -> Items.BLACK_WOOL;
        };
    }

    public static boolean isBreedingFood(ItemStack stack) {
        return !stack.isEmpty() && FOOD_TAGS.stream().anyMatch(stack::is);
    }

    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot >= 0 && slot < 6 && isBreedingFood(stack); }
    @Override public int[] getSlotsForFace(Direction side) { return ALL_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction dir) { return index < 6 && isBreedingFood(stack); }
    @Override public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction dir) { return index >= 6; }

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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        energyStorage.amount = Math.clamp(tag.getLong("Energy"), 0L, MAX_ENERGY);
    }
}
