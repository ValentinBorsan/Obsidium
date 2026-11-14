package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;

public class DemetrianComposterBlockEntity extends BlockEntity implements MenuProvider, InventoryHolder, WorldlyContainer, MachineInventory {
    public static final int INVENTORY_SIZE = 3;
    private static final int NATURAL_INPUT = 0;
    private static final int ASH_INPUT = 1;
    private static final int FERTILIZER_OUTPUT = 2;
    private static final int CRAFT_TICKS = 40;
    private int progress;
    private int tickCounter;
    private boolean outputBlocked;

    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            DemetrianComposterBlockEntity.this.setChanged();
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) { return index == 0 ? progress : index == 1 && outputBlocked ? 1 : 0; }
        @Override public void set(int index, int value) {
            if (index == 0) progress = Math.max(0, Math.min(CRAFT_TICKS, value));
            else if (index == 1) outputBlocked = value != 0;
        }
        @Override public int getCount() { return 2; }
    };

    public DemetrianComposterBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.DEMETRIAN_COMPOSTER_BE, pos, state);
    }

    public static boolean isCompostable(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(ItemTags.LEAVES)
                || stack.getItem() instanceof net.valentin.obsidium.item.VolcanicPetalItem
                || stack.getItem() instanceof net.valentin.obsidium.item.VolcanicSeedItem
                || stack.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("minecraft", "crops")))
                || stack.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "crops")))
                || stack.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("minecraft", "seeds")))
                || stack.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "seeds")))
                || stack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS)
                || stack.is(Items.WHEAT_SEEDS) || stack.is(Items.PUMPKIN_SEEDS)
                || stack.is(Items.MELON_SEEDS) || stack.is(Items.BEETROOT_SEEDS)
                || stack.is(Items.TORCHFLOWER_SEEDS) || stack.is(Items.PITCHER_POD)
                || stack.is(ItemTags.SAPLINGS)
                || stack.is(ItemTags.FLOWERS)
                || stack.is(ObsidiumMod.MIDAS_GOLD_LEAVES_ITEM)
                || stack.is(ObsidiumMod.ARES_IRON_LEAVES_ITEM)
                || stack.is(ObsidiumMod.HERMES_COPPER_LEAVES_ITEM)
                || stack.is(ObsidiumMod.CRONUS_LEAD_LEAVES_ITEM)
                || stack.is(ObsidiumMod.ZEUS_TIN_LEAVES_ITEM)
                || stack.is(ObsidiumMod.ARTEMIS_SILVER_LEAVES_ITEM)
                || stack.is(ObsidiumMod.HEPHAESTUS_VOLCANITE_LEAVES_ITEM)
                || stack.is(ObsidiumMod.ELECTRUM_LEAVES_ITEM)
                || stack.is(ObsidiumMod.TARTAREAN_NETHERITE_LEAVES_ITEM)
                || GeothermalArboristBlockEntity.getCropStateForSeed(stack) != null
                || stack.is(Items.APPLE)
                || stack.is(Items.SWEET_BERRIES)
                || stack.is(Items.GLOW_BERRIES)
                || stack.is(Items.MELON_SLICE)
                || stack.is(ObsidiumMod.MIDAS_GOLD_FRUIT)
                || stack.is(ObsidiumMod.ARES_IRON_FRUIT)
                || stack.is(ObsidiumMod.HERMES_COPPER_FRUIT)
                || stack.is(ObsidiumMod.CRONUS_LEAD_FRUIT)
                || stack.is(ObsidiumMod.ZEUS_TIN_FRUIT)
                || stack.is(ObsidiumMod.ARTEMIS_SILVER_FRUIT)
                || stack.is(ObsidiumMod.HEPHAESTUS_VOLCANITE_FRUIT)
                || stack.is(ObsidiumMod.ELECTRUM_FRUIT)
                || stack.is(ObsidiumMod.TARTAREAN_NETHERITE_FRUIT)
                || stack.is(ObsidiumMod.DIONYSIAN_POMACE));
    }

    @Override public Container getInventory() { return inventory; }

    public int insertNatural(ItemStack offered) {
        if (!isCompostable(offered)) return 0;
        ItemStack input = inventory.getItem(NATURAL_INPUT);
        if (input.isEmpty()) {
            int inserted = Math.min(offered.getCount(), inventory.getMaxStackSize());
            inventory.setItem(NATURAL_INPUT, offered.copyWithCount(inserted));
            return inserted;
        }
        if (!ItemStack.isSameItemSameComponents(input, offered)) return 0;
        int inserted = Math.min(offered.getCount(), input.getMaxStackSize() - input.getCount());
        if (inserted > 0) {
            input.grow(inserted);
            inventory.setChanged();
        }
        return inserted;
    }

    public void tickServer(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        tickCounter++;
        if (tickCounter % 10 == 0) exportOutput(level, pos);
        ItemStack natural = inventory.getItem(NATURAL_INPUT);
        ItemStack ash = inventory.getItem(ASH_INPUT);
        ItemStack output = inventory.getItem(FERTILIZER_OUTPUT);
        if (!isCompostable(natural) || !ash.is(ObsidiumMod.VOLCANIC_ASH)) {
            outputBlocked = false;
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            return;
        }
        if (progress < CRAFT_TICKS) {
            progress++;
            if (progress % 5 == 0) setChanged();
        }
        if (progress < CRAFT_TICKS) return;
        int yield = natural.is(ObsidiumMod.DIONYSIAN_POMACE) ? 4 : 2;
        if (!output.isEmpty() && (!output.is(Items.BONE_MEAL) || output.getCount() > output.getMaxStackSize() - yield)) {
            outputBlocked = true;
            setChanged();
            return;
        }

        outputBlocked = false;
        natural.shrink(1);
        ash.shrink(1);
        if (natural.isEmpty()) inventory.setItem(NATURAL_INPUT, ItemStack.EMPTY);
        if (ash.isEmpty()) inventory.setItem(ASH_INPUT, ItemStack.EMPTY);
        if (output.isEmpty()) inventory.setItem(FERTILIZER_OUTPUT, new ItemStack(Items.BONE_MEAL, yield));
        else output.grow(yield);
        progress = 0;
        inventory.setChanged();
        setChanged();
        level.levelEvent(1500, pos, 1);
    }

    private void exportOutput(Level level, BlockPos pos) {
        ItemStack output = inventory.getItem(FERTILIZER_OUTPUT);
        if (output.isEmpty()) return;
        for (Direction direction : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(direction));
            if (neighbor instanceof GeothermalArboristBlockEntity arborist) {
                int inserted = arborist.insertFertilizer(output);
                if (inserted > 0) {
                    output.shrink(inserted);
                    if (output.isEmpty()) inventory.setItem(FERTILIZER_OUTPUT, ItemStack.EMPTY);
                    inventory.setChanged();
                    return;
                }
            }
        }
        int moved = VolcaniteStoragePipeBlockEntity.tryAutoOutputStack(level, pos, this, output);
        if (moved > 0) {
            if (output.isEmpty()) {
                inventory.setItem(FERTILIZER_OUTPUT, ItemStack.EMPTY);
            }
            inventory.setChanged();
            setChanged();
        }
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
        for (int slot = 0; slot < INVENTORY_SIZE; slot++) items.set(slot, inventory.getItem(slot));
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Progress", progress);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        for (int slot = 0; slot < INVENTORY_SIZE; slot++) inventory.setItem(slot, items.get(slot));
        progress = Math.max(0, Math.min(CRAFT_TICKS, tag.getInt("Progress")));
    }

    @Override public Component getDisplayName() { return Component.translatable("block.obsidium.demetrian_composter"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new DemetrianComposterMenu(id, playerInventory, inventory, data);
    }
    @Override public int getContainerSize() { return INVENTORY_SIZE; }
    @Override public boolean isEmpty() { return inventory.isEmpty(); }
    @Override public ItemStack getItem(int slot) { return inventory.getItem(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { return inventory.removeItem(slot, amount); }
    @Override public ItemStack removeItemNoUpdate(int slot) { return inventory.removeItemNoUpdate(slot); }
    @Override public void setItem(int slot, ItemStack stack) { inventory.setItem(slot, stack); }
    @Override public boolean stillValid(Player player) { return inventory.stillValid(player); }
    @Override public void clearContent() { inventory.clearContent(); }
    @Override public int[] getSlotsForFace(Direction side) { return side == Direction.DOWN ? new int[]{FERTILIZER_OUTPUT} : new int[]{NATURAL_INPUT, ASH_INPUT}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == NATURAL_INPUT ? isCompostable(stack) : slot == ASH_INPUT && stack.is(ObsidiumMod.VOLCANIC_ASH);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == FERTILIZER_OUTPUT; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == NATURAL_INPUT ? isCompostable(stack) : slot == ASH_INPUT && stack.is(ObsidiumMod.VOLCANIC_ASH);
    }
    @Override public boolean isInputSlot(int slot, ItemStack stack, @Nullable Direction side) { return slot == NATURAL_INPUT || slot == ASH_INPUT; }
    @Override public boolean isOutputSlot(int slot, ItemStack stack, @Nullable Direction side) { return slot == FERTILIZER_OUTPUT; }
}
