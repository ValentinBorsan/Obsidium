package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VolcaniteWorktableBlockEntity extends BaseContainerBlockEntity implements MachineInventory, WorldlyContainer {
    // 0-8: Crafting Grid, 9: Lava Bucket, 10: Output, 11: Blueprint
    private NonNullList<ItemStack> items = NonNullList.withSize(12, ItemStack.EMPTY);

    private static final int[] SLOTS_TOP = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] SLOTS_SIDES = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    private static final int[] SLOTS_BOTTOM = new int[]{10};

    private boolean isCraftingExtraction = false;
    public boolean isPlayerMenuExtraction = false;
    private boolean autoOutput = true;
    private boolean antiMix = true;

    public VolcaniteWorktableBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.VOLCANITE_WORKTABLE_BE, pos, state);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return SLOTS_BOTTOM;
        }
        if (side == Direction.UP) {
            return SLOTS_TOP;
        }
        return SLOTS_SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot == 10 || slot == 11) return false;
        if (slot == 9) return stack.is(Items.LAVA_BUCKET);
        if (slot >= 0 && slot <= 8) {
            ItemStack bp = items.get(11);
            if (!bp.is(ObsidiumMod.VOLCANITE_BLUEPRINT) && !antiMix) return true;
            return isAllowedInGrid(slot, stack);
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 10;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == 11) return stack.is(ObsidiumMod.VOLCANITE_BLUEPRINT);
        if (slot == 9) return stack.is(Items.LAVA_BUCKET);
        if (slot >= 0 && slot <= 8) return isAllowedInGrid(slot, stack);
        return false;
    }

    @Override
    public boolean isInputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot == 9) return stack.is(Items.LAVA_BUCKET);
        if (slot >= 0 && slot <= 8) {
            if (side != null && !isAllowedInGrid(slot, stack)) {
                return false;
            }
            return true;
        }
        return false;
    }

    public boolean isAllowedInGrid(int slot, ItemStack stack) {
        if (stack.isEmpty()) return true;

        // 1. If Blueprint is installed with a recipe, strictly follow blueprint requirement for this slot
        ItemStack bp = items.get(11);
        if (bp.is(ObsidiumMod.VOLCANITE_BLUEPRINT) && net.valentin.obsidium.item.VolcaniteBlueprintItem.hasRecipe(bp)) {
            NonNullList<ItemStack> bpGrid = net.valentin.obsidium.item.VolcaniteBlueprintItem.getRecipeGrid(bp, level != null ? level.registryAccess() : null);
            if (slot >= 0 && slot < bpGrid.size()) {
                ItemStack expected = bpGrid.get(slot);
                if (expected.isEmpty()) return false;
                return ItemStack.isSameItemSameComponents(expected, stack);
            }
            return false;
        }

        // 2. Otherwise use Anti-Mix batching rules
        if (!antiMix) return true;

        // If no blueprint is set, items WITHOUT a valid uniform 3x3 recipe (e.g. blocks, tools, random items)
        // are strictly prohibited from entering the crafting grid!
        if (!isValidUniform3x3Recipe(stack, level)) {
            return false;
        }

        boolean hasItemsInGrid = false;
        boolean matchesExisting = false;

        for (int i = 0; i < 9; i++) {
            ItemStack inSlot = items.get(i);
            if (!inSlot.isEmpty()) {
                hasItemsInGrid = true;
                if (ItemStack.isSameItemSameComponents(inSlot, stack)) {
                    matchesExisting = true;
                    break;
                }
            }
        }

        // If grid is currently empty, any item with a valid 3x3 recipe can start the batch
        if (!hasItemsInGrid) {
            return true;
        }

        // If grid already contains items, only items matching the active batch are accepted
        return matchesExisting;
    }

    public static boolean isValidUniform3x3Recipe(ItemStack sample, Level level) {
        if (sample == null || sample.isEmpty() || level == null || level.getServer() == null) return false;
        List<ItemStack> testGrid = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            testGrid.add(sample.copyWithCount(1));
        }
        CraftingInput input = CraftingInput.of(3, 3, testGrid);
        return level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).isPresent();
    }

    @Override
    public boolean isOutputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 10;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VolcaniteWorktableBlockEntity entity) {
        if (level.isClientSide()) return;

        // Auto-pull ingredients from adjacent chest (blueprint order or 3x3 with min 9 threshold)
        if (level.getGameTime() % 4 == 0) {
            entity.pullIngredientsFromChest(level);
        }

        // Auto-balance homogeneous grid items (e.g. nuggets, ingots) if anti-mix is active
        if (entity.antiMix && level.getGameTime() % 2 == 0) {
            entity.balanceCraftingGrid();
        }

        entity.updateRecipeOutput(level);

        // Auto-export crafted output to adjacent chest or machine
        if (entity.autoOutput && level.getGameTime() % 4 == 0) {
            entity.autoExportOutput(level);
        }
    }

    public void balanceCraftingGrid() {
        if (level == null || level.isClientSide()) return;

        ItemStack sample = ItemStack.EMPTY;
        boolean homogeneous = true;
        int totalCount = 0;

        for (int i = 0; i < 9; i++) {
            ItemStack s = items.get(i);
            if (!s.isEmpty()) {
                if (sample.isEmpty()) {
                    sample = s;
                } else if (!ItemStack.isSameItemSameComponents(sample, s)) {
                    homogeneous = false;
                    break;
                }
                totalCount += s.getCount();
            }
        }

        // Only auto-balance if all non-empty slots contain the exact same item type, total >= 9, and forms a valid 3x3 recipe
        if (!homogeneous || sample.isEmpty() || totalCount < 9 || !isValidUniform3x3Recipe(sample, level)) {
            return;
        }

        int perSlot = totalCount / 9;
        int extra = totalCount % 9;
        boolean needsBalance = false;
        for (int i = 0; i < 9; i++) {
            int expected = perSlot + (i < extra ? 1 : 0);
            if (items.get(i).getCount() != expected || !ItemStack.isSameItemSameComponents(items.get(i), sample)) {
                needsBalance = true;
                break;
            }
        }

        if (needsBalance) {
            for (int i = 0; i < 9; i++) {
                int count = perSlot + (i < extra ? 1 : 0);
                items.set(i, sample.copyWithCount(count));
            }
            setChanged();
        }
    }

    public void updateRecipeOutput(Level level) {
        if (level == null || level.isClientSide()) return;

        List<ItemStack> grid = new ArrayList<>(9);
        boolean hasAny = false;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = items.get(i);
            grid.add(stack);
            if (!stack.isEmpty()) hasAny = true;
        }

        if (!hasAny) {
            if (!items.get(10).isEmpty()) {
                items.set(10, ItemStack.EMPTY);
                setChanged();
            }
            return;
        }

        CraftingInput input = CraftingInput.of(3, 3, grid);
        Optional<RecipeHolder<CraftingRecipe>> match = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);

        if (match.isPresent()) {
            ItemStack result = match.get().value().assemble(input, level.registryAccess());
            if (!ItemStack.matches(items.get(10), result)) {
                items.set(10, result);
                setChanged();
            }
        } else {
            if (!items.get(10).isEmpty()) {
                items.set(10, ItemStack.EMPTY);
                setChanged();
            }
        }
    }

    public void onTakeCraftedItem(Player player, ItemStack craftedStack) {
        if (isCraftingExtraction) return;
        isCraftingExtraction = true;
        try {
            consumeCraftingIngredients(player);
        } finally {
            isCraftingExtraction = false;
        }
    }

    public void consumeCraftingIngredients(@Nullable Player player) {
        if (level == null) return;

        List<ItemStack> grid = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            grid.add(items.get(i));
        }
        CraftingInput input = CraftingInput.of(3, 3, grid);
        Optional<RecipeHolder<CraftingRecipe>> match = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);

        NonNullList<ItemStack> remaining = NonNullList.withSize(9, ItemStack.EMPTY);
        if (match.isPresent()) {
            remaining = match.get().value().getRemainingItems(input);
        }

        for (int i = 0; i < 9; i++) {
            ItemStack slotItem = items.get(i);
            ItemStack remainder = (i < remaining.size()) ? remaining.get(i) : ItemStack.EMPTY;
            if (!slotItem.isEmpty()) {
                slotItem.shrink(1);
            }
            if (!remainder.isEmpty()) {
                if (slotItem.isEmpty()) {
                    items.set(i, remainder);
                } else if (player != null) {
                    if (!player.getInventory().add(remainder)) {
                        player.drop(remainder, false);
                    }
                } else {
                    VolcaniteChestBlockEntity chest = getAdjacentChest();
                    boolean inserted = false;
                    if (chest != null) {
                        int put = VolcaniteStoragePipeBlockEntity.insertIntoContainer(chest, Direction.UP, remainder, false);
                        if (put > 0) {
                            remainder.shrink(put);
                            if (remainder.isEmpty()) inserted = true;
                        }
                    }
                    if (!inserted && !remainder.isEmpty()) {
                        for (Direction dir : Direction.values()) {
                            BlockPos targetPos = worldPosition.relative(dir);
                            if (!level.isLoaded(targetPos)) continue;
                            Container targetContainer = VolcaniteStoragePipeBlockEntity.getContainerAt(level, targetPos);
                            if (targetContainer != null && targetContainer != this && targetContainer != chest) {
                                int put = VolcaniteStoragePipeBlockEntity.insertIntoContainer(targetContainer, dir.getOpposite(), remainder, false);
                                if (put > 0) {
                                    remainder.shrink(put);
                                    if (remainder.isEmpty()) {
                                        inserted = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if (!inserted && !remainder.isEmpty()) {
                        Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, remainder);
                    }
                }
            }
        }

        setChanged();
        updateRecipeOutput(level);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot == 10) {
            ItemStack outputStack = items.get(10);
            if (outputStack.isEmpty()) return ItemStack.EMPTY;

            ItemStack taken = ContainerHelper.removeItem(items, 10, amount);
            if (!taken.isEmpty() && !isCraftingExtraction && !isPlayerMenuExtraction) {
                isCraftingExtraction = true;
                try {
                    consumeCraftingIngredients(null);
                } finally {
                    isCraftingExtraction = false;
                }
            }
            return taken;
        }
        ItemStack stack = ContainerHelper.removeItem(items, slot, amount);
        if (!stack.isEmpty() && slot >= 0 && slot <= 8) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                updateRecipeOutput(level);
            }
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot == 10) {
            ItemStack outputStack = items.get(10);
            if (outputStack.isEmpty()) return ItemStack.EMPTY;

            ItemStack taken = ContainerHelper.takeItem(items, 10);
            if (!taken.isEmpty() && !isCraftingExtraction && !isPlayerMenuExtraction) {
                isCraftingExtraction = true;
                try {
                    consumeCraftingIngredients(null);
                } finally {
                    isCraftingExtraction = false;
                }
            }
            return taken;
        }
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
        if (slot >= 0 && slot <= 8 && level != null && !level.isClientSide()) {
            updateRecipeOutput(level);
        }
    }

    @Override
    protected Component getDefaultName() { return Component.translatable("block.obsidium.volcanite_worktable"); }

    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory playerInventory) {
        VolcaniteChestBlockEntity chest = getAdjacentChest();
        VolcaniteWorktableMenu menu = new VolcaniteWorktableMenu(syncId, playerInventory, this, chest != null ? chest : new net.minecraft.world.SimpleContainer(9));
        menu.hasChest.set(chest != null ? 1 : 0);
        menu.autoOutput.set(this.autoOutput ? 1 : 0);
        menu.antiMix.set(this.antiMix ? 1 : 0);
        return menu;
    }

    public boolean isAutoOutput() { return autoOutput; }
    public void setAutoOutput(boolean autoOutput) {
        this.autoOutput = autoOutput;
        setChanged();
    }
    public void toggleAutoOutput() {
        setAutoOutput(!this.autoOutput);
    }

    public boolean isAntiMix() { return antiMix; }
    public void setAntiMix(boolean antiMix) {
        this.antiMix = antiMix;
        setChanged();
    }
    public void toggleAntiMix() {
        setAntiMix(!this.antiMix);
    }

    public void autoExportOutput(Level level) {
        if (level == null || level.isClientSide()) return;
        if (!autoOutput) return;

        ItemStack output = items.get(10);
        if (output.isEmpty()) return;

        // If no blueprint is present, only auto-export 3x3 recipes (all 9 slots filled).
        // Do not auto-output 1-slot recipes like uncrafting blocks into ingots.
        ItemStack bp = items.get(11);
        boolean hasBp = bp.is(ObsidiumMod.VOLCANITE_BLUEPRINT) && net.valentin.obsidium.item.VolcaniteBlueprintItem.hasRecipe(bp);
        if (!hasBp) {
            for (int i = 0; i < 9; i++) {
                if (items.get(i).isEmpty()) {
                    return;
                }
            }
        }

        int count = output.getCount();
        ItemStack toInsert = output.copy();

        // 1. Check adjacent linked VolcaniteChest first
        VolcaniteChestBlockEntity linkedChest = getAdjacentChest();
        if (linkedChest != null && !linkedChest.isRemoved()) {
            int canAccept = VolcaniteStoragePipeBlockEntity.insertIntoContainer(linkedChest, Direction.UP, toInsert, true);
            if (canAccept == count) {
                VolcaniteStoragePipeBlockEntity.insertIntoContainer(linkedChest, Direction.UP, toInsert.copy(), false);
                consumeCraftingIngredients(null);
                setChanged();
                return;
            }
        }

        // 2. Check all 6 directions (Chests, Double Chests, Barrels, Machines, Furnaces, etc.)
        for (Direction dir : Direction.values()) {
            BlockPos targetPos = worldPosition.relative(dir);
            if (!level.isLoaded(targetPos)) continue;

            Direction sideToInsert = dir.getOpposite();
            Container targetContainer = VolcaniteStoragePipeBlockEntity.getContainerAt(level, targetPos);
            if (targetContainer != null && targetContainer != this && targetContainer != linkedChest) {
                int canAccept = VolcaniteStoragePipeBlockEntity.insertIntoContainer(targetContainer, sideToInsert, toInsert, true);
                if (canAccept == count) {
                    VolcaniteStoragePipeBlockEntity.insertIntoContainer(targetContainer, sideToInsert, toInsert.copy(), false);
                    consumeCraftingIngredients(null);
                    setChanged();
                    return;
                }
            }

            // 3. Fabric ItemStorage fallback for third-party or mod machines
            Storage<ItemVariant> targetStorage = ItemStorage.SIDED.find(level, targetPos, sideToInsert);
            if (targetStorage != null && targetStorage.supportsInsertion()) {
                ItemVariant variant = ItemVariant.of(toInsert);
                try (Transaction tx = Transaction.openOuter()) {
                    long inserted = targetStorage.insert(variant, count, tx);
                    if (inserted == count) {
                        tx.commit();
                        consumeCraftingIngredients(null);
                        setChanged();
                        return;
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("AutoOutput", this.autoOutput);
        tag.putBoolean("AntiMix", this.antiMix);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("AutoOutput")) {
            this.autoOutput = tag.getBoolean("AutoOutput");
        }
        if (tag.contains("AntiMix")) {
            this.antiMix = tag.getBoolean("AntiMix");
        }
    }
    
    private VolcaniteChestBlockEntity getAdjacentChest() {
        if (level == null) return null;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(worldPosition.relative(dir));
            if (be instanceof VolcaniteChestBlockEntity chest) return chest;
        }
        return null;
    }

    public Container getSourceContainer() {
        VolcaniteChestBlockEntity chest = getAdjacentChest();
        if (chest != null && !chest.isRemoved()) return chest;
        if (level == null) return null;
        for (Direction dir : Direction.values()) {
            BlockPos targetPos = worldPosition.relative(dir);
            if (!level.isLoaded(targetPos)) continue;
            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(targetPos);
            if (be instanceof Container container && be != this) {
                return container;
            }
        }
        return null;
    }

    public static int countItemInContainer(Container container, ItemStack sample) {
        int total = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack s = container.getItem(i);
            if (ItemStack.isSameItemSameComponents(s, sample)) {
                total += s.getCount();
            }
        }
        return total;
    }

    public static ItemStack extractItemFromContainer(Container container, ItemStack sample, int amount) {
        int remaining = amount;
        ItemStack extracted = ItemStack.EMPTY;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack s = container.getItem(i);
            if (ItemStack.isSameItemSameComponents(s, sample)) {
                int take = Math.min(remaining, s.getCount());
                ItemStack taken = container.removeItem(i, take);
                if (!taken.isEmpty()) {
                    if (extracted.isEmpty()) {
                        extracted = taken.copy();
                    } else {
                        extracted.grow(taken.getCount());
                    }
                    remaining -= taken.getCount();
                    if (remaining <= 0) break;
                }
            }
        }
        return extracted;
    }

    public void pullIngredientsFromChest(Level level) {
        if (level == null || level.isClientSide()) return;
        Container source = getSourceContainer();
        if (source == null) return;

        ItemStack bp = items.get(11);
        boolean hasBp = bp.is(ObsidiumMod.VOLCANITE_BLUEPRINT) && net.valentin.obsidium.item.VolcaniteBlueprintItem.hasRecipe(bp);

        if (hasBp) {
            NonNullList<ItemStack> bpGrid = net.valentin.obsidium.item.VolcaniteBlueprintItem.getRecipeGrid(bp, level.registryAccess());
            if (net.valentin.obsidium.item.VolcaniteBlueprintItem.isUniform3x3(bpGrid)) {
                ItemStack target = bpGrid.get(0);
                if (target.isEmpty()) return;

                int countInChest = countItemInContainer(source, target);
                // RULE: If < 9 items in chest, DO NOT MOVE!
                if (countInChest < 9) return;

                // Check if all 9 grid slots can accept 1 item
                boolean canAccept9 = true;
                for (int i = 0; i < 9; i++) {
                    ItemStack inGrid = items.get(i);
                    if (!inGrid.isEmpty() && (!ItemStack.isSameItemSameComponents(inGrid, target) || inGrid.getCount() >= inGrid.getMaxStackSize())) {
                        canAccept9 = false;
                        break;
                    }
                }

                if (canAccept9) {
                    ItemStack extracted = extractItemFromContainer(source, target, 9);
                    if (extracted.getCount() == 9) {
                        for (int i = 0; i < 9; i++) {
                            ItemStack inGrid = items.get(i);
                            if (inGrid.isEmpty()) {
                                items.set(i, target.copyWithCount(1));
                            } else {
                                inGrid.grow(1);
                            }
                        }
                        setChanged();
                        updateRecipeOutput(level);
                    }
                }
            } else {
                // Complex blueprint recipe: check required items per slot
                boolean canAcceptBatch = true;
                for (int i = 0; i < 9; i++) {
                    ItemStack req = bpGrid.get(i);
                    if (req.isEmpty()) continue;
                    ItemStack inGrid = items.get(i);
                    if (!inGrid.isEmpty() && (!ItemStack.isSameItemSameComponents(inGrid, req) || inGrid.getCount() >= inGrid.getMaxStackSize())) {
                        canAcceptBatch = false;
                        break;
                    }
                }
                if (!canAcceptBatch) return;

                // Check if container has all required ingredients for 1 craft
                boolean hasAllInChest = true;
                for (int i = 0; i < 9; i++) {
                    ItemStack req = bpGrid.get(i);
                    if (req.isEmpty()) continue;
                    if (countItemInContainer(source, req) < 1) {
                        hasAllInChest = false;
                        break;
                    }
                }
                if (!hasAllInChest) return;

                // Pull ingredients in blueprint order
                for (int i = 0; i < 9; i++) {
                    ItemStack req = bpGrid.get(i);
                    if (req.isEmpty()) continue;
                    ItemStack extracted = extractItemFromContainer(source, req, 1);
                    if (!extracted.isEmpty()) {
                        ItemStack inGrid = items.get(i);
                        if (inGrid.isEmpty()) {
                            items.set(i, extracted);
                        } else {
                            inGrid.grow(extracted.getCount());
                        }
                    }
                }
                setChanged();
                updateRecipeOutput(level);
            }
        } else {
            // No blueprint present: support standard 3x3 homogeneous recipes with 9-item threshold and anti-mix
            if (antiMix) {
                boolean gridEmpty = true;
                ItemStack activeSample = ItemStack.EMPTY;
                for (int i = 0; i < 9; i++) {
                    if (!items.get(i).isEmpty()) {
                        gridEmpty = false;
                        if (activeSample.isEmpty()) activeSample = items.get(i);
                    }
                }

                if (gridEmpty) {
                    // Find an item in chest that has at least 9 count AND forms a valid 3x3 recipe!
                    for (int c = 0; c < source.getContainerSize(); c++) {
                        ItemStack inChest = source.getItem(c);
                        if (!inChest.isEmpty()) {
                            // Do not auto-input items that do not have a 3x3 recipe (e.g. blocks)
                            if (!isValidUniform3x3Recipe(inChest, level)) {
                                continue;
                            }
                            int totalInChest = countItemInContainer(source, inChest);
                            if (totalInChest >= 9) {
                                ItemStack extracted = extractItemFromContainer(source, inChest, 9);
                                if (extracted.getCount() == 9) {
                                    for (int i = 0; i < 9; i++) {
                                        items.set(i, inChest.copyWithCount(1));
                                    }
                                    setChanged();
                                    updateRecipeOutput(level);
                                    break;
                                }
                            }
                        }
                    }
                } else if (!activeSample.isEmpty()) {
                    // If active sample has no 3x3 recipe (e.g. blocks placed manually), do not pull more
                    if (!isValidUniform3x3Recipe(activeSample, level)) {
                        return;
                    }
                    // Only pull matching items if total in chest >= 9
                    int countInChest = countItemInContainer(source, activeSample);
                    if (countInChest >= 9) {
                        boolean canAccept9 = true;
                        for (int i = 0; i < 9; i++) {
                            ItemStack inGrid = items.get(i);
                            if (inGrid.isEmpty() || !ItemStack.isSameItemSameComponents(inGrid, activeSample) || inGrid.getCount() >= inGrid.getMaxStackSize()) {
                                canAccept9 = false;
                                break;
                            }
                        }
                        if (canAccept9) {
                            ItemStack extracted = extractItemFromContainer(source, activeSample, 9);
                            if (extracted.getCount() == 9) {
                                for (int i = 0; i < 9; i++) {
                                    items.get(i).grow(1);
                                }
                                setChanged();
                                updateRecipeOutput(level);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public int getContainerSize() { return items.size(); }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() { items.clear(); }

    @Override
    protected NonNullList<ItemStack> getItems() { return this.items; }

    @Override
    protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
}
