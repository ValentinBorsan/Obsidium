package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.VolcaniteBlueprintItem;
import net.valentin.obsidium.item.VolcaniteOverclockUpgradeItem;
import net.valentin.obsidium.item.VolcanitePipeEnergyUpgradeItem;
import net.valentin.obsidium.item.VolcaniteStackUpgradeItem;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class VolcaniteCrafterInterfaceBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    // Slots:
    // 0..8: 3x3 Interactive Crafting Grid
    // 9: Crafting Output Slot
    // 10: Blueprint Encoder / Loader Slot
    // 11..14: 4 Automated Network Blueprint Slots
    // 15: Speed / Batch Upgrade Slot
    public static final int CONTAINER_SIZE = 16;
    private static final int[] NO_SLOTS = new int[0];

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private int scanTimer = 0;
    private int craftTimer = 0;
    private BlockPos cachedControllerPos = null;

    private boolean autoCraftEnabled = true;
    private int stockMode = 1; // 0 = 64, 1 = 256, 2 = 1024, 3 = Infinite
    private boolean autoRefillGrid = true;
    private boolean isUpdatingOutput = false;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> {
                    VolcaniteStorageControllerBlockEntity ctrl = findController();
                    yield (ctrl != null && ctrl.isPowered()) ? 1 : 0;
                }
                case 1 -> {
                    VolcaniteStorageControllerBlockEntity ctrl = findController();
                    yield (ctrl != null) ? (int) ((ctrl.getEnergy() * 100L) / VolcaniteStorageControllerBlockEntity.MAX_ENERGY) : 0;
                }
                case 2 -> autoCraftEnabled ? 1 : 0;
                case 3 -> stockMode;
                case 4 -> autoRefillGrid ? 1 : 0;
                case 5 -> getUpgradeTier();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 2 -> autoCraftEnabled = (value != 0);
                case 3 -> stockMode = value % 4;
                case 4 -> autoRefillGrid = (value != 0);
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public VolcaniteCrafterInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.VOLCANITE_CRAFTER_INTERFACE_BE, pos, state);
    }

    public int getUpgradeTier() {
        ItemStack upg = items.get(15);
        if (upg.isEmpty()) return 0;
        if (upg.getItem() instanceof VolcanitePipeEnergyUpgradeItem eu) return eu.getTier();
        if (upg.getItem() instanceof VolcaniteStackUpgradeItem) return 3;
        if (upg.getItem() instanceof VolcaniteOverclockUpgradeItem) return 2;
        return 0;
    }

    public int getStockLimit() {
        return switch (stockMode) {
            case 0 -> 64;
            case 1 -> 256;
            case 2 -> 1024;
            default -> Integer.MAX_VALUE;
        };
    }

    public boolean isAutoCraftEnabled() {
        return autoCraftEnabled;
    }

    public void toggleAutoCraft() {
        this.autoCraftEnabled = !this.autoCraftEnabled;
        setChanged();
    }

    public void cycleStockMode() {
        this.stockMode = (this.stockMode + 1) % 4;
        setChanged();
    }

    public boolean isAutoRefillGrid() {
        return autoRefillGrid;
    }

    public void toggleAutoRefillGrid() {
        this.autoRefillGrid = !this.autoRefillGrid;
        setChanged();
    }

    public VolcaniteStorageControllerBlockEntity findController() {
        if (level == null) return null;
        if (cachedControllerPos != null && level.getBlockEntity(cachedControllerPos) instanceof VolcaniteStorageControllerBlockEntity controller && controller.isPowered()) {
            return controller;
        }

        for (Direction dir : Direction.values()) {
            BlockPos p = worldPosition.relative(dir);
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof VolcaniteStorageControllerBlockEntity ctrl && ctrl.isPowered()) {
                cachedControllerPos = p;
                return ctrl;
            }
            if (be instanceof VolcaniteNetworkNodeReceiverBlockEntity receiver && receiver.isLinked()) {
                VolcaniteStorageControllerBlockEntity ctrl = receiver.getConnectedController();
                if (ctrl != null && ctrl.isPowered()) {
                    cachedControllerPos = ctrl.getBlockPos();
                    return ctrl;
                }
            }
            if (be instanceof VolcaniteNetworkNodeEmitterBlockEntity emitter && emitter.isLinked()) {
                VolcaniteStorageControllerBlockEntity ctrl = emitter.getConnectedController();
                if (ctrl != null && ctrl.isPowered()) {
                    cachedControllerPos = ctrl.getBlockPos();
                    return ctrl;
                }
            }
        }

        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(worldPosition);
        visited.add(worldPosition);

        int steps = 0;
        int maxSteps = 4096;
        while (!queue.isEmpty() && steps++ < maxSteps) {
            BlockPos current = queue.poll();
            if (!level.isLoaded(current)) continue;

            for (Direction dir : Direction.values()) {
                if (!PipeConnectionHelper.canTraverseNetwork(level, current, dir)) continue;
                BlockPos next = current.relative(dir);
                if (!level.isLoaded(next)) continue;
                BlockState nextState = level.getBlockState(next);
                if (nextState.isAir()) continue;
                if (visited.add(next)) {
                    BlockEntity be = level.getBlockEntity(next);
                    if (be instanceof VolcaniteStorageControllerBlockEntity controller && controller.isPowered()) {
                        cachedControllerPos = next;
                        return controller;
                    } else if (be instanceof VolcaniteNetworkNodeReceiverBlockEntity receiver && receiver.isLinked()) {
                        VolcaniteStorageControllerBlockEntity ctrl = receiver.getConnectedController();
                        if (ctrl != null && ctrl.isPowered()) {
                            cachedControllerPos = ctrl.getBlockPos();
                            return ctrl;
                        }
                        BlockPos ePos = receiver.getLinkedEmitterPos();
                        if (ePos != null && visited.add(ePos)) {
                            queue.add(ePos);
                        }
                        queue.add(next);
                    } else if (be instanceof VolcaniteNetworkNodeEmitterBlockEntity emitter && emitter.isLinked()) {
                        VolcaniteStorageControllerBlockEntity ctrl = emitter.getConnectedController();
                        if (ctrl != null && ctrl.isPowered()) {
                            cachedControllerPos = ctrl.getBlockPos();
                            return ctrl;
                        }
                        for (BlockPos rPos : emitter.getLinkedReceivers()) {
                            if (visited.add(rPos)) {
                                queue.add(rPos);
                            }
                        }
                        queue.add(next);
                    } else {
                        if (PipeConnectionHelper.isLogisticsPipe(nextState.getBlock()) ||
                            PipeConnectionHelper.isStorageNetworkBlock(nextState.getBlock())) {
                            queue.add(next);
                        }
                    }
                }
            }
        }

        cachedControllerPos = null;
        return null;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VolcaniteCrafterInterfaceBlockEntity entity) {
        if (level.isClientSide()) return;

        if (++entity.scanTimer >= 20) {
            entity.scanTimer = 0;
            VolcaniteStorageControllerBlockEntity ctrl = entity.findController();
            boolean isOnline = ctrl != null && ctrl.isPowered();

            if (state.getValue(VolcaniteCrafterInterfaceBlock.WORKING) != isOnline) {
                level.setBlock(pos, state.setValue(VolcaniteCrafterInterfaceBlock.WORKING, isOnline), 3);
            }
        }

        if (entity.autoCraftEnabled) {
            int tier = entity.getUpgradeTier();
            int interval = switch (tier) {
                case 1 -> 12;
                case 2 -> 6;
                case 3 -> 2;
                default -> 20;
            };
            if (++entity.craftTimer >= interval) {
                entity.craftTimer = 0;
                entity.tickAutoCrafter(level, tier);
            }
        }
    }

    private void tickAutoCrafter(Level level, int upgradeTier) {
        VolcaniteStorageControllerBlockEntity controller = findController();
        if (controller == null || !controller.isPowered() || controller.getEnergy() < 25) return;

        int maxCraftsPerSlot = switch (upgradeTier) {
            case 1 -> 2;
            case 2 -> 4;
            case 3 -> 8;
            default -> 1;
        };
        int stockLimit = getStockLimit();

        for (int slot = 11; slot <= 14; slot++) {
            ItemStack bp = items.get(slot);
            if (bp.isEmpty() || !(bp.getItem() instanceof VolcaniteBlueprintItem) || !VolcaniteBlueprintItem.hasRecipe(bp)) {
                continue;
            }

            NonNullList<ItemStack> grid = VolcaniteBlueprintItem.getRecipeGrid(bp, level.registryAccess());
            if (grid.stream().allMatch(ItemStack::isEmpty) || level.getServer() == null) continue;
            CraftingInput previewInput = CraftingInput.of(3, 3, grid.stream().map(ItemStack::copy).toList());
            if (level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, previewInput, level).isEmpty()) continue;

            for (int c = 0; c < maxCraftsPerSlot; c++) {
                if (controller.getEnergy() < 250) break;
                try (CraftingIngredientBatch batch = new CraftingIngredientBatch(grid,
                        required -> controller.extractMatchingItem(required, 1, false),
                        stack -> insertOrDrop(controller, stack))) {
                    if (!batch.isComplete()) break;

                    // Resolve and assemble from the objects actually taken, never the blueprint's cached output.
                    CraftingInput actualInput = CraftingInput.of(3, 3, batch.input());
                    Optional<RecipeHolder<CraftingRecipe>> match = level.getServer().getRecipeManager()
                            .getRecipeFor(RecipeType.CRAFTING, actualInput, level);
                    if (match.isEmpty()) break;
                    ItemStack result = match.get().value().assemble(actualInput, level.registryAccess());
                    if (result.isEmpty()) break;
                    int currentStock = controller.getUnifiedInventory().getOrDefault(result.getItem(), 0);
                    if ((long) currentStock + result.getCount() > stockLimit) break;
                    if (!controller.insertItem(result.copy(), true).isEmpty()) break;
                    NonNullList<ItemStack> remainders = match.get().value().getRemainingItems(actualInput);
                    if (controller.getEnergy() < 250) break;

                    controller.consumeEnergy(250L);
                    batch.consume();
                    // A real insertion may still reject a remainder; preserve it as a world drop.
                    insertOrDrop(controller, result);
                    for (ItemStack remainder : remainders) {
                        if (!remainder.isEmpty()) insertOrDrop(controller, remainder.copy());
                    }
                }
            }
        }
    }

    private void insertOrDrop(VolcaniteStorageControllerBlockEntity controller, ItemStack stack) {
        ItemStack remaining = controller.insertItem(stack, false);
        if (!remaining.isEmpty() && level != null) {
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, remaining);
        }
    }

    public void updateRecipeOutput() {
        if (level == null || level.isClientSide() || isUpdatingOutput) return;
        isUpdatingOutput = true;
        try {
            List<ItemStack> grid = new ArrayList<>(9);
            boolean hasAny = false;
            for (int i = 0; i < 9; i++) {
                ItemStack s = items.get(i);
                grid.add(s);
                if (!s.isEmpty()) hasAny = true;
            }

            if (!hasAny || level.getServer() == null) {
                items.set(9, ItemStack.EMPTY);
                return;
            }

            CraftingInput input = CraftingInput.of(3, 3, grid);
            Optional<RecipeHolder<CraftingRecipe>> match = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
            if (match.isPresent()) {
                ItemStack res = match.get().value().assemble(input, level.registryAccess());
                items.set(9, res);
            } else {
                items.set(9, ItemStack.EMPTY);
            }
        } finally {
            isUpdatingOutput = false;
        }
    }

    public void onCraftFromGrid(Player player) {
        if (level == null || level.isClientSide() || level.getServer() == null) return;

        List<ItemStack> gridSnapshot = new ArrayList<>(9);
        ItemStack[] previousItems = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            ItemStack s = items.get(i);
            gridSnapshot.add(s.copy());
            previousItems[i] = s.isEmpty() ? ItemStack.EMPTY : s.copyWithCount(1);
        }

        CraftingInput input = CraftingInput.of(3, 3, gridSnapshot);
        Optional<RecipeHolder<CraftingRecipe>> match = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        NonNullList<ItemStack> remainders = match.map(r -> r.value().getRemainingItems(input))
                .orElseGet(() -> NonNullList.withSize(9, ItemStack.EMPTY));

        VolcaniteStorageControllerBlockEntity controller = findController();
        boolean canRefillFromNet = this.autoRefillGrid && controller != null && controller.isPowered();

        for (int i = 0; i < 9; i++) {
            ItemStack slotItem = items.get(i);
            ItemStack prevItem = previousItems[i];
            if (!slotItem.isEmpty()) {
                slotItem.shrink(1);
                if (slotItem.isEmpty()) {
                    items.set(i, ItemStack.EMPTY);
                }
            }

            // Auto-refill from Storage Network if slot became empty!
            if (items.get(i).isEmpty() && !prevItem.isEmpty() && canRefillFromNet) {
                ItemStack refilled = controller.extractMatchingItem(prevItem, 1, false);
                if (!refilled.isEmpty()) {
                    items.set(i, refilled);
                }
            }

            ItemStack rem = (i < remainders.size()) ? remainders.get(i) : ItemStack.EMPTY;
            if (!rem.isEmpty()) {
                if (items.get(i).isEmpty()) {
                    items.set(i, rem);
                } else if (controller != null && controller.isPowered()) {
                    ItemStack left = controller.insertItem(rem, false);
                    if (!left.isEmpty() && player != null && !player.getInventory().add(left)) {
                        player.drop(left, false);
                    }
                } else if (player != null) {
                    if (!player.getInventory().add(rem)) {
                        player.drop(rem, false);
                    }
                }
            }
        }

        setChanged();
        updateRecipeOutput();
    }

    public void encodeOrLoadBlueprint(Player player) {
        if (level == null || level.isClientSide()) return;
        ItemStack bp = items.get(10);
        if (bp.isEmpty() || !(bp.getItem() instanceof VolcaniteBlueprintItem)) {
            if (player != null) {
                player.displayClientMessage(Component.literal("§6[Crafter] §cPlace a Volcanite Blueprint in the Blueprint slot first!"), true);
            }
            return;
        }

        updateRecipeOutput();
        ItemStack output = items.get(9);
        if (!output.isEmpty()) {
            // Save 3x3 recipe onto the Blueprint
            NonNullList<ItemStack> grid = NonNullList.withSize(9, ItemStack.EMPTY);
            for (int i = 0; i < 9; i++) {
                ItemStack inSlot = items.get(i);
                grid.set(i, inSlot.isEmpty() ? ItemStack.EMPTY : inSlot.copyWithCount(1));
            }
            VolcaniteBlueprintItem.setRecipe(bp, grid, output.copy(), level.registryAccess());
            level.playSound(null, worldPosition, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.1F);
            if (player != null) {
                player.displayClientMessage(Component.literal("§6[Blueprint] §aEncoded recipe: §e" + output.getHoverName().getString()), true);
            }
            setChanged();
        } else if (VolcaniteBlueprintItem.hasRecipe(bp)) {
            // Grid has no valid recipe, so load the Blueprint's recipe from the Storage Network into the 3x3 grid!
            clearGridToNetwork(player);
            for (int i = 0; i < 9; i++) {
                if (!items.get(i).isEmpty()) {
                    if (player != null) player.displayClientMessage(Component.literal("§6[Crafter] §cMake room to return the current ingredients first!"), true);
                    return;
                }
            }
            VolcaniteStorageControllerBlockEntity controller = findController();
            if (controller == null || !controller.isPowered()) {
                if (player != null) {
                    player.displayClientMessage(Component.literal("§6[Crafter] §cStorage Network offline! Cannot pull Blueprint ingredients."), true);
                }
                return;
            }
            NonNullList<ItemStack> bpGrid = VolcaniteBlueprintItem.getRecipeGrid(bp, level.registryAccess());
            boolean pulledAny = false;
            for (int i = 0; i < 9 && i < bpGrid.size(); i++) {
                ItemStack req = bpGrid.get(i);
                if (!req.isEmpty()) {
                    ItemStack extracted = controller.extractMatchingItem(req, 1, false);
                    if (!extracted.isEmpty()) {
                        items.set(i, extracted);
                        pulledAny = true;
                    }
                }
            }
            updateRecipeOutput();
            setChanged();
            if (pulledAny) {
                level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.7F, 1.2F);
                if (player != null) {
                    player.displayClientMessage(Component.literal("§6[Crafter] §aLoaded Blueprint ingredients from Storage Network!"), true);
                }
            } else if (player != null) {
                player.displayClientMessage(Component.literal("§6[Crafter] §eMissing ingredients in Storage Network for this Blueprint."), true);
            }
        }
    }

    public void clearGridToNetwork(@Nullable Player player) {
        if (level == null || level.isClientSide()) return;
        VolcaniteStorageControllerBlockEntity controller = findController();
        boolean changed = false;

        for (int i = 0; i < 9; i++) {
            ItemStack s = items.get(i);
            if (!s.isEmpty()) {
                ItemStack rem = s.copy();
                if (controller != null && controller.isPowered()) {
                    rem = controller.insertItem(rem, false);
                }
                if (!rem.isEmpty() && player != null) {
                    if (player.getInventory().add(rem)) {
                        rem = ItemStack.EMPTY;
                    }
                }
                items.set(i, rem);
                changed = true;
            }
        }

        if (changed) {
            updateRecipeOutput();
            setChanged();
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack itemStack, @Nullable Direction direction) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return false;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < items.size(); i++) {
            if (i == 9) continue;
            if (!items.get(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = ContainerHelper.removeItem(items, slot, amount);
        if (slot >= 0 && slot <= 8) {
            updateRecipeOutput();
        }
        setChanged();
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = ContainerHelper.takeItem(items, slot);
        if (slot >= 0 && slot <= 8) {
            updateRecipeOutput();
        }
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        if (slot >= 0 && slot <= 8) {
            updateRecipeOutput();
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return net.minecraft.world.Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.obsidium.volcanite_crafter_interface");
    }

    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory playerInventory) {
        updateRecipeOutput();
        return new VolcaniteCrafterInterfaceMenu(syncId, playerInventory, this, this.data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // Clear virtual output slot 9 before saving so it is never duplicated
        ItemStack savedOut = items.get(9);
        items.set(9, ItemStack.EMPTY);
        ContainerHelper.saveAllItems(tag, items, registries);
        items.set(9, savedOut);

        tag.putBoolean("AutoCraftEnabled", autoCraftEnabled);
        tag.putInt("StockMode", stockMode);
        tag.putBoolean("AutoRefillGrid", autoRefillGrid);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        if (tag.contains("AutoCraftEnabled")) this.autoCraftEnabled = tag.getBoolean("AutoCraftEnabled");
        if (tag.contains("StockMode")) this.stockMode = tag.getInt("StockMode");
        if (tag.contains("AutoRefillGrid")) this.autoRefillGrid = tag.getBoolean("AutoRefillGrid");
    }
}
