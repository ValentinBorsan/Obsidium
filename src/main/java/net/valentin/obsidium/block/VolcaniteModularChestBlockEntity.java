package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.HermesFilterUpgradeItem;
import net.valentin.obsidium.item.VolcaniteAdvancedFilterModuleItem;
import net.valentin.obsidium.item.VolcaniteBarrelUpgradeItem;
import net.valentin.obsidium.item.VolcaniteCompactorUpgradeItem;
import net.valentin.obsidium.item.VolcaniteLogisticsModuleItem;
import net.valentin.obsidium.item.VolcaniteOverclockUpgradeItem;
import net.valentin.obsidium.item.VolcaniteStackUpgradeItem;
import net.valentin.obsidium.item.VolcaniteStorageUpgradeItem;
import net.valentin.obsidium.item.VolcaniteVacuumUpgradeItem;
import net.valentin.obsidium.item.VolcaniteFilteredVoidUpgradeItem;
import net.valentin.obsidium.item.VolcaniteVoidUpgradeItem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class VolcaniteModularChestBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SLOTS_PER_PAGE = 54;
    public static final int STORAGE_SLOTS = 54; // Page 1 size
    public static final int MAX_PAGES = 4;
    public static final int MAX_STORAGE_SLOTS = SLOTS_PER_PAGE * MAX_PAGES; // 216
    public static final int MODULE_SLOTS = 4;
    public static final int TOTAL_SLOTS = MAX_STORAGE_SLOTS + MODULE_SLOTS; // 220

    // 4 Universal Flex Module Bays (indices 216..219)
    public static final int SLOT_MOD_0 = 216;
    public static final int SLOT_MOD_1 = 217;
    public static final int SLOT_MOD_2 = 218;
    public static final int SLOT_MOD_3 = 219;

    // Legacy aliases kept for compatibility
    public static final int SLOT_STACK_MOD = SLOT_MOD_0;
    public static final int SLOT_VACUUM_MOD = SLOT_MOD_1;
    public static final int SLOT_COMPACTOR_MOD = SLOT_MOD_2;
    public static final int SLOT_VOID_MOD = SLOT_MOD_3;

    private static final int[][] PAGE_ACCESSIBLE_SLOTS = new int[MAX_PAGES + 1][];
    static {
        for (int pages = 1; pages <= MAX_PAGES; pages++) {
            int count = pages * SLOTS_PER_PAGE;
            PAGE_ACCESSIBLE_SLOTS[pages] = new int[count];
            for (int i = 0; i < count; i++) {
                PAGE_ACCESSIBLE_SLOTS[pages][i] = i;
            }
        }
    }

    private NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
    private int currentPage = 0;
    private boolean lastQuantitySortDescending = true;

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> getCurrentPage();
                case 1 -> getUnlockedPages();
                case 2 -> lastQuantitySortDescending ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                setCurrentPage(value);
            } else if (index == 2) {
                lastQuantitySortDescending = (value != 0);
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public VolcaniteModularChestBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.VOLCANITE_MODULAR_CHEST_BE, pos, state);
    }

    public static boolean isValidModule(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item instanceof VolcaniteStackUpgradeItem
                || item instanceof VolcaniteStorageUpgradeItem
                || item instanceof VolcaniteBarrelUpgradeItem
                || item instanceof VolcaniteVacuumUpgradeItem
                || item instanceof VolcaniteCompactorUpgradeItem
                || item instanceof net.valentin.obsidium.item.VolcaniteFilteredCompactorUpgradeItem
                || item instanceof VolcaniteVoidUpgradeItem
                || item instanceof VolcaniteFilteredVoidUpgradeItem
                || item instanceof VolcaniteLogisticsModuleItem
                || item instanceof VolcaniteOverclockUpgradeItem
                || item instanceof HermesFilterUpgradeItem
                || item instanceof VolcaniteAdvancedFilterModuleItem;
    }

    public static boolean isFilterModule(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item instanceof HermesFilterUpgradeItem || item instanceof VolcaniteAdvancedFilterModuleItem;
    }

    /**
     * Checks whether the candidate ItemStack is allowed by all 9-slot Filter Modules
     * (HermesFilterUpgradeItem / VolcaniteAdvancedFilterModuleItem) installed in the 4 Module Bays.
     */
    public boolean matchesChestFilters(ItemStack candidate) {
        if (candidate.isEmpty()) return true;
        HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : null;

        boolean hasActiveWhitelist = false;
        boolean matchedAnyWhitelist = false;

        for (int i = SLOT_MOD_0; i < TOTAL_SLOTS; i++) {
            ItemStack modStack = items.get(i);
            if (!isFilterModule(modStack)) continue;

            NonNullList<ItemStack> filterList = HermesFilterUpgradeItem.getFilterItems(modStack, registries);
            boolean hasAnyEntry = false;
            boolean matchedThisModule = false;
            for (ItemStack f : filterList) {
                if (!f.isEmpty()) {
                    hasAnyEntry = true;
                    if (ItemStack.isSameItem(f, candidate)) {
                        matchedThisModule = true;
                        break;
                    }
                }
            }

            if (!hasAnyEntry) continue; // Unconfigured filter module allows everything

            if (HermesFilterUpgradeItem.isBlacklist(modStack)) {
                // Blacklist: if item is listed in any blacklist module, block it immediately
                if (matchedThisModule) {
                    return false;
                }
            } else {
                // Whitelist: item must match at least one configured whitelist module
                hasActiveWhitelist = true;
                if (matchedThisModule) {
                    matchedAnyWhitelist = true;
                }
            }
        }

        return !hasActiveWhitelist || matchedAnyWhitelist;
    }

    public boolean hasActiveWhitelistFilter() {
        HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : null;
        for (int i = SLOT_MOD_0; i < TOTAL_SLOTS; i++) {
            ItemStack modStack = items.get(i);
            if (!isFilterModule(modStack) || HermesFilterUpgradeItem.isBlacklist(modStack)) continue;
            NonNullList<ItemStack> filterList = HermesFilterUpgradeItem.getFilterItems(modStack, registries);
            for (ItemStack f : filterList) {
                if (!f.isEmpty()) return true;
            }
        }
        return false;
    }

    public int getUnlockedPages() {
        int pages = 1;
        for (int i = SLOT_MOD_0; i < TOTAL_SLOTS; i++) {
            Item item = items.get(i).getItem();
            if (item instanceof VolcaniteStackUpgradeItem || item instanceof VolcaniteStorageUpgradeItem) {
                pages += 1;
            } else if (item instanceof VolcaniteBarrelUpgradeItem barrelUpg) {
                switch (barrelUpg.getUpgradeType()) {
                    case BASIC -> pages += 1;
                    case ADVANCED -> pages += 2;
                    case SUPREME -> pages += 3;
                    default -> {}
                }
            }
        }
        return Math.min(MAX_PAGES, pages);
    }

    public int getUnlockedSlots() {
        return getUnlockedPages() * SLOTS_PER_PAGE;
    }

    public int getCurrentPage() {
        int maxPage = getUnlockedPages() - 1;
        if (currentPage > maxPage) {
            currentPage = maxPage;
        }
        if (currentPage < 0) {
            currentPage = 0;
        }
        return currentPage;
    }

    public void setCurrentPage(int page) {
        int maxPage = getUnlockedPages() - 1;
        this.currentPage = Math.max(0, Math.min(page, maxPage));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.obsidium.volcanite_modular_chest");
    }

    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory inv) {
        return new VolcaniteModularChestMenu(syncId, inv, this, this.data);
    }

    @Override
    public int getContainerSize() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= TOTAL_SLOTS) return ItemStack.EMPTY;
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = ContainerHelper.removeItem(items, slot, amount);
        if (!stack.isEmpty()) setChanged();
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= TOTAL_SLOTS) return;
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putBoolean("FlexModulesV2", true);
        tag.putInt("CurrentPage", this.currentPage);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.currentPage = tag.getInt("CurrentPage");

        // Automatically migrate legacy 58-slot saves (where modules sat at 54..57) into 216..219
        if (!tag.getBoolean("FlexModulesV2")) {
            for (int offset = 0; offset < 4; offset++) {
                ItemStack oldMod = this.items.get(54 + offset);
                if (isValidModule(oldMod) && this.items.get(SLOT_MOD_0 + offset).isEmpty()) {
                    this.items.set(SLOT_MOD_0 + offset, oldMod);
                    this.items.set(54 + offset, ItemStack.EMPTY);
                }
            }
        }
    }

    // WorldlyContainer: Automation accesses all unlocked storage slots (0 .. unlockedSlots - 1)
    @Override
    public int[] getSlotsForFace(Direction side) {
        return PAGE_ACCESSIBLE_SLOTS[getUnlockedPages()];
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot >= SLOT_MOD_0 && slot < TOTAL_SLOTS) {
            return isValidModule(stack);
        }
        return slot >= 0 && slot < getUnlockedSlots() && matchesChestFilters(stack);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot >= 0 && slot < getUnlockedSlots() && matchesChestFilters(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 0 && slot < getUnlockedSlots();
    }

    // ==================== FLEX MODULE QUERIES ====================

    public boolean hasStackUpgrade() {
        return getUnlockedPages() > 1;
    }

    public int getVacuumCount() {
        int count = 0;
        for (int i = SLOT_MOD_0; i < TOTAL_SLOTS; i++) {
            if (items.get(i).getItem() instanceof VolcaniteVacuumUpgradeItem) count++;
        }
        return count;
    }

    public boolean hasVacuumUpgrade() {
        return getVacuumCount() > 0;
    }

    public int getCompactorCount() {
        int count = 0;
        for (int i = SLOT_MOD_0; i < TOTAL_SLOTS; i++) {
            Item item = items.get(i).getItem();
            if (item instanceof VolcaniteCompactorUpgradeItem
                    || item instanceof net.valentin.obsidium.item.VolcaniteFilteredCompactorUpgradeItem) {
                count++;
            }
        }
        return count;
    }

    private boolean canCompactorProcess(Level level, ItemStack candidate) {
        for (int i = SLOT_MOD_0; i < TOTAL_SLOTS; i++) {
            if (items.get(i).getItem() instanceof VolcaniteCompactorUpgradeItem) {
                return true;
            }
        }
        for (int i = SLOT_MOD_0; i < TOTAL_SLOTS; i++) {
            ItemStack modStack = items.get(i);
            if (modStack.getItem() instanceof net.valentin.obsidium.item.VolcaniteFilteredCompactorUpgradeItem
                    && net.valentin.obsidium.item.HermesFilterUpgradeItem.matches(modStack, candidate, level.registryAccess())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasCompactorUpgrade() {
        return getCompactorCount() > 0;
    }

    public boolean hasVoidUpgrade() {
        for (int i = SLOT_MOD_0; i < TOTAL_SLOTS; i++) {
            Item item = items.get(i).getItem();
            if (item instanceof VolcaniteVoidUpgradeItem) return true;
            if (item instanceof VolcaniteBarrelUpgradeItem b && b.getUpgradeType() == VolcaniteBarrelUpgradeItem.UpgradeType.VOID) {
                return true;
            }
        }
        return false;
    }

    public boolean hasFilteredVoidUpgrade() {
        for (int i = SLOT_MOD_0; i < TOTAL_SLOTS; i++) {
            if (items.get(i).getItem() instanceof VolcaniteFilteredVoidUpgradeItem) {
                return true;
            }
        }
        return false;
    }

    public boolean shouldFilteredVoid(ItemStack candidate) {
        if (candidate.isEmpty() || this.level == null) return false;
        HolderLookup.Provider registries = this.level.registryAccess();
        for (int i = SLOT_MOD_0; i < TOTAL_SLOTS; i++) {
            ItemStack modStack = items.get(i);
            if (modStack.getItem() instanceof VolcaniteFilteredVoidUpgradeItem
                    && VolcaniteFilteredVoidUpgradeItem.shouldVoid(modStack, candidate, registries)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasLogisticsModule() {
        for (int i = SLOT_MOD_0; i < TOTAL_SLOTS; i++) {
            if (items.get(i).getItem() instanceof VolcaniteLogisticsModuleItem) return true;
        }
        return false;
    }

    public int getOverclockTier() {
        int best = 0;
        for (int i = SLOT_MOD_0; i < TOTAL_SLOTS; i++) {
            if (items.get(i).getItem() instanceof VolcaniteOverclockUpgradeItem oc) {
                best = Math.max(best, oc.getTier());
            }
        }
        return best;
    }

    public double getVacuumRadius() {
        int count = getVacuumCount();
        if (count <= 0) return 0.0;
        double base = 6.0 + (count - 1) * 3.0 + getOverclockTier() * 2.0;
        return Math.min(16.0, base);
    }

    // ==================== SORTING & ORGANIZATION ====================

    private record ItemGroupKey(Item item, int componentsHash, ItemStack representative) {
        static ItemGroupKey of(ItemStack stack) {
            return new ItemGroupKey(stack.getItem(), stack.getComponents().hashCode(), stack.copyWithCount(1));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ItemGroupKey other)) return false;
            return this.item == other.item && ItemStack.isSameItemSameComponents(this.representative, other.representative);
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(item) + componentsHash;
        }
    }

    /**
     * Merges all partial stacks in the unlocked slots and sorts items by TOTAL QUANTITY
     * (either Descending: highest quantity first, or Ascending: lowest quantity first).
     */
    public void sortByQuantity(boolean descending) {
        this.lastQuantitySortDescending = descending;
        int limit = getUnlockedSlots();

        // 1. Consolidate all stacks by item + components
        Map<ItemGroupKey, Integer> totalCounts = new HashMap<>();
        List<ItemGroupKey> uniqueOrder = new ArrayList<>();

        for (int i = 0; i < limit; i++) {
            ItemStack st = items.get(i);
            if (!st.isEmpty()) {
                ItemGroupKey key = ItemGroupKey.of(st);
                if (!totalCounts.containsKey(key)) {
                    uniqueOrder.add(key);
                    totalCounts.put(key, st.getCount());
                } else {
                    totalCounts.put(key, totalCounts.get(key) + st.getCount());
                }
                items.set(i, ItemStack.EMPTY);
            }
        }

        // 2. Sort item types by total quantity (and alphabetically as tie-breaker)
        Comparator<ItemGroupKey> comp = Comparator
                .<ItemGroupKey>comparingInt(totalCounts::get)
                .thenComparing(k -> k.representative().getHoverName().getString(), String.CASE_INSENSITIVE_ORDER);
        if (descending) {
            comp = Comparator
                    .<ItemGroupKey>comparingInt(totalCounts::get).reversed()
                    .thenComparing(k -> k.representative().getHoverName().getString(), String.CASE_INSENSITIVE_ORDER);
        }
        uniqueOrder.sort(comp);

        // 3. Rebuild full and partial stacks in sorted order
        int slotIdx = 0;
        for (ItemGroupKey key : uniqueOrder) {
            int remaining = totalCounts.get(key);
            int maxStack = key.representative().getMaxStackSize();
            while (remaining > 0 && slotIdx < limit) {
                int take = Math.min(remaining, maxStack);
                items.set(slotIdx++, key.representative().copyWithCount(take));
                remaining -= take;
            }
        }
        setChanged();
    }

    /**
     * Merges all partial stacks in the unlocked slots and sorts items Alphabetically (A-Z),
     * with larger stacks placed before partial stacks of the same item.
     */
    public void sortAlphabetically() {
        int limit = getUnlockedSlots();
        Map<ItemGroupKey, Integer> totalCounts = new HashMap<>();
        List<ItemGroupKey> uniqueOrder = new ArrayList<>();

        for (int i = 0; i < limit; i++) {
            ItemStack st = items.get(i);
            if (!st.isEmpty()) {
                ItemGroupKey key = ItemGroupKey.of(st);
                if (!totalCounts.containsKey(key)) {
                    uniqueOrder.add(key);
                    totalCounts.put(key, st.getCount());
                } else {
                    totalCounts.put(key, totalCounts.get(key) + st.getCount());
                }
                items.set(i, ItemStack.EMPTY);
            }
        }

        uniqueOrder.sort(Comparator
                .<ItemGroupKey, String>comparing(k -> k.representative().getHoverName().getString(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Comparator.<ItemGroupKey>comparingInt(totalCounts::get).reversed()));

        int slotIdx = 0;
        for (ItemGroupKey key : uniqueOrder) {
            int remaining = totalCounts.get(key);
            int maxStack = key.representative().getMaxStackSize();
            while (remaining > 0 && slotIdx < limit) {
                int take = Math.min(remaining, maxStack);
                items.set(slotIdx++, key.representative().copyWithCount(take));
                remaining -= take;
            }
        }
        setChanged();
    }

    /**
     * Quick-stacks matching items from the player's inventory into the chest.
     */
    public void quickStackFromPlayer(Player player) {
        if (player == null) return;
        Inventory inv = player.getInventory();
        boolean changed = false;

        for (int i = 9; i < 36; i++) {
            ItemStack playerStack = inv.getItem(i);
            if (!playerStack.isEmpty() && containsItemType(playerStack)) {
                ItemStack rem = insertStorageItem(playerStack);
                if (rem.getCount() != playerStack.getCount()) {
                    inv.setItem(i, rem);
                    changed = true;
                }
            }
        }
        if (changed) {
            inv.setChanged();
            setChanged();
        }
    }

    public boolean containsItemType(ItemStack stack) {
        int limit = getUnlockedSlots();
        for (int i = 0; i < limit; i++) {
            ItemStack inSlot = items.get(i);
            if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(inSlot, stack)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasFullStackOf(ItemStack stack) {
        int limit = getUnlockedSlots();
        for (int i = 0; i < limit; i++) {
            ItemStack inSlot = items.get(i);
            if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(inSlot, stack) && inSlot.getCount() >= inSlot.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    // ==================== TICK & ACTIVE MODULES ====================

    public static void tick(Level level, BlockPos pos, BlockState state, VolcaniteModularChestBlockEntity chest) {
        if (level.isClientSide()) return;

        int ocTier = chest.getOverclockTier();
        int vacuumInterval = Math.max(2, 10 >> ocTier);
        int compactorInterval = Math.max(2, 20 >> ocTier);
        int logisticsInterval = Math.max(2, 8 >> ocTier);
        long gameTime = level.getGameTime();

        // 1. Vacuum Module Logic
        if (chest.hasVacuumUpgrade() && gameTime % vacuumInterval == 0) {
            AABB area = new AABB(pos).inflate(chest.getVacuumRadius());
            List<ItemEntity> nearbyItems = level.getEntitiesOfClass(ItemEntity.class, area);
            boolean suckedAny = false;

            for (ItemEntity entity : nearbyItems) {
                if (!entity.isAlive() || entity.getItem().isEmpty()) continue;
                ItemStack stack = entity.getItem();
                int original = stack.getCount();

                ItemStack remainder = chest.insertStorageItem(stack);
                if (remainder.getCount() != original) {
                    suckedAny = true;
                    if (remainder.isEmpty()) {
                        entity.discard();
                    } else {
                        entity.setItem(remainder);
                    }
                    if (level instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY(), entity.getZ(),
                                5, 0.2, 0.2, 0.2, 0.05);
                    }
                }
            }

            if (suckedAny && gameTime % 40 == 0) {
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4F, 1.2F);
            }
        }

        // 2. Auto-Compactor Module Logic (3x3 & 2x2 across all unlocked slots)
        if (chest.hasCompactorUpgrade() && gameTime % compactorInterval == 0) {
            chest.runAutoCompaction(level);
        }

        // 3. Active Logistics Module (Auto-Pull from container above, Auto-Push to container below)
        if (chest.hasLogisticsModule() && gameTime % logisticsInterval == 0) {
            chest.runAutoLogistics(level, pos);
        }

        // 4. Smart Void Module cleanup if full via external hoppers
        if (chest.hasVoidUpgrade() && gameTime % 20 == 0) {
            chest.cleanOverflowDuplicates();
        }

        // 5. Selective Void Module purge for player-configured filter items
        if (chest.hasFilteredVoidUpgrade() && gameTime % 20 == 0) {
            chest.cleanFilteredVoidItems(level);
        }
    }

    public ItemStack insertStorageItem(ItemStack stack) {
        return insertItem(stack, false);
    }

    // --- Network Storage Methods ---

    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (!matchesChestFilters(stack)) return stack;
        ItemStack current = stack.copy();
        if (shouldFilteredVoid(current)) return ItemStack.EMPTY;
        int limit = getUnlockedSlots();

        // 1. Fill existing matching stacks in unlocked pages
        for (int i = 0; i < limit; i++) {
            ItemStack inSlot = items.get(i);
            if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(inSlot, current)) {
                int space = Math.min(inSlot.getMaxStackSize(), this.getMaxStackSize()) - inSlot.getCount();
                if (space > 0) {
                    int toAdd = Math.min(space, current.getCount());
                    if (!simulate) {
                        inSlot.grow(toAdd);
                        setChanged();
                    }
                    current.shrink(toAdd);
                    if (current.isEmpty()) return ItemStack.EMPTY;
                }
            }
        }

        // 2. Place in empty unlocked slots
        for (int i = 0; i < limit; i++) {
            ItemStack inSlot = items.get(i);
            if (inSlot.isEmpty()) {
                int toAdd = Math.min(current.getMaxStackSize(), current.getCount());
                if (!simulate) {
                    items.set(i, current.copyWithCount(toAdd));
                    setChanged();
                }
                current.shrink(toAdd);
                if (current.isEmpty()) return ItemStack.EMPTY;
            }
        }

        // 3. Smart Overflow Void: if chest is full (or item already has a full stack inside) and Void Module is active, void excess!
        if (hasVoidUpgrade() && hasFullStackOf(current)) {
            return ItemStack.EMPTY;
        }

        return current;
    }

    public ItemStack extractItem(Item item, int amount, boolean simulate) {
        ItemStack result = StorageItemTransfers.extract(items, getUnlockedSlots(), item, amount, simulate);
        if (!simulate && !result.isEmpty()) setChanged();
        return result;
    }

    public ItemStack extractMatchingItem(ItemStack template, int amount, boolean simulate) {
        ItemStack result = StorageItemTransfers.extractMatching(items, getUnlockedSlots(), template, amount, simulate);
        if (!simulate && !result.isEmpty()) setChanged();
        return result;
    }

    public void collectStoredItems(Map<Item, Integer> map) {
        int limit = getUnlockedSlots();
        for (int i = 0; i < limit; i++) {
            ItemStack current = items.get(i);
            if (!current.isEmpty()) {
                map.put(current.getItem(), map.getOrDefault(current.getItem(), 0) + current.getCount());
            }
        }
    }

    public int getTotalStored() {
        int total = 0;
        int limit = getUnlockedSlots();
        for (int i = 0; i < limit; i++) {
            ItemStack current = items.get(i);
            if (!current.isEmpty()) {
                total += current.getCount();
            }
        }
        return total;
    }

    public int getMaxCapacity() {
        return getUnlockedSlots() * 64;
    }

    private void cleanOverflowDuplicates() {
        // Keeps the last slot free for incoming hopper items if every single unlocked slot is occupied by duplicates
        int limit = getUnlockedSlots();
        ItemStack last = items.get(limit - 1);
        if (last.isEmpty()) return;

        for (int i = 0; i < limit - 1; i++) {
            ItemStack s = items.get(i);
            if (s.isEmpty()) return; // Still has empty slots
        }

        // All unlocked slots are occupied: check if the last slot's item already has at least 2 full stacks earlier in the chest
        int fullStacks = 0;
        for (int i = 0; i < limit - 1; i++) {
            ItemStack s = items.get(i);
            if (ItemStack.isSameItemSameComponents(s, last) && s.getCount() >= s.getMaxStackSize()) {
                fullStacks++;
            }
        }
        if (fullStacks >= 2) {
            items.set(limit - 1, ItemStack.EMPTY);
            setChanged();
        }
    }

    private void cleanFilteredVoidItems(Level level) {
        int limit = getUnlockedSlots();
        boolean removedAny = false;
        for (int i = 0; i < limit; i++) {
            ItemStack st = items.get(i);
            if (!st.isEmpty() && shouldFilteredVoid(st)) {
                items.set(i, ItemStack.EMPTY);
                removedAny = true;
            }
        }
        if (removedAny) {
            setChanged();
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.05, worldPosition.getZ() + 0.5,
                        5, 0.2, 0.1, 0.2, 0.02);
            }
        }
    }

    private void runAutoLogistics(Level level, BlockPos pos) {
        int transferLimit = 8 * (1 << getOverclockTier());
        // Pull from container directly ABOVE (e.g. Furnace / Grinder / Press output)
        Container above = VolcaniteStoragePipeBlockEntity.getContainerAt(level, pos.above());
        if (above != null && !(above instanceof VolcaniteModularChestBlockEntity)) {
            int size = above.getContainerSize();
            for (int i = 0; i < size; i++) {
                ItemStack inSlot = above.getItem(i);
                if (inSlot.isEmpty()) continue;
                if (!VolcaniteStoragePipeBlockEntity.isOutputSlot(above, i, inSlot, Direction.DOWN)) continue;

                int count = Math.min(inSlot.getCount(), transferLimit);
                ItemStack toPull = inSlot.copyWithCount(count);
                ItemStack rem = insertStorageItem(toPull);
                int pulled = count - rem.getCount();
                if (pulled > 0) {
                    above.removeItem(i, pulled);
                    above.setChanged();
                    break;
                }
            }
        }
    }

    private record CompactionRule(int gridWidth, int requiredCount, ItemStack result) {}
    private static final Map<Item, Optional<CompactionRule>> COMPACTION_CACHE = new HashMap<>();

    private Optional<CompactionRule> findCompactionRule(Level level, ItemStack sample) {
        Item item = sample.getItem();
        if (COMPACTION_CACHE.containsKey(item)) {
            return COMPACTION_CACHE.get(item);
        }
        if (level.getServer() == null) return Optional.empty();

        // Try 3x3 (9 -> 1) first
        List<ItemStack> grid9 = new ArrayList<>(9);
        for (int g = 0; g < 9; g++) grid9.add(new ItemStack(item, 1));
        CraftingInput input9 = CraftingInput.of(3, 3, grid9);
        Optional<RecipeHolder<CraftingRecipe>> match9 = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input9, level);
        if (match9.isPresent()) {
            ItemStack res = match9.get().value().assemble(input9, level.registryAccess());
            if (!res.isEmpty() && res.getCount() == 1) {
                Optional<CompactionRule> rule = Optional.of(new CompactionRule(3, 9, res.copy()));
                COMPACTION_CACHE.put(item, rule);
                return rule;
            }
        }

        // Try 2x2 (4 -> 1) next (e.g. Quartz, Glowstone, Clay, Bricks)
        List<ItemStack> grid4 = new ArrayList<>(4);
        for (int g = 0; g < 4; g++) grid4.add(new ItemStack(item, 1));
        CraftingInput input4 = CraftingInput.of(2, 2, grid4);
        Optional<RecipeHolder<CraftingRecipe>> match4 = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input4, level);
        if (match4.isPresent()) {
            ItemStack res = match4.get().value().assemble(input4, level.registryAccess());
            if (!res.isEmpty() && res.getCount() == 1) {
                Optional<CompactionRule> rule = Optional.of(new CompactionRule(2, 4, res.copy()));
                COMPACTION_CACHE.put(item, rule);
                return rule;
            }
        }

        COMPACTION_CACHE.put(item, Optional.empty());
        return Optional.empty();
    }

    private void runAutoCompaction(Level level) {
        if (level == null || level.getServer() == null) return;
        int limit = getUnlockedSlots();
        int maxCraftsPerCycle = Math.max(1, getCompactorCount() * (1 << getOverclockTier()));

        // Count totals per item across all unlocked slots
        Map<Item, Integer> counts = new HashMap<>();
        for (int i = 0; i < limit; i++) {
            ItemStack st = items.get(i);
            if (!st.isEmpty() && !st.isDamaged() && !st.isEnchanted()) {
                counts.merge(st.getItem(), st.getCount(), Integer::sum);
            }
        }

        for (Map.Entry<Item, Integer> entry : counts.entrySet()) {
            int totalAvailable = entry.getValue();
            if (totalAvailable < 4) continue;

            ItemStack candidate = new ItemStack(entry.getKey());
            if (!canCompactorProcess(level, candidate)) continue;

            Optional<CompactionRule> optRule = findCompactionRule(level, candidate);
            if (optRule.isEmpty()) continue;

            CompactionRule rule = optRule.get();
            if (totalAvailable < rule.requiredCount()) continue;

            int crafts = Math.min(maxCraftsPerCycle, totalAvailable / rule.requiredCount());
            if (crafts <= 0) continue;

            ItemStack craftedResult = rule.result().copyWithCount(crafts * rule.result().getCount());
            int toConsume = crafts * rule.requiredCount();

            // Consume ingredients across slots first, then insert result (or rollback if chest is completely full)
            List<int[]> consumedFrom = new ArrayList<>();
            int remainingToConsume = toConsume;
            for (int i = 0; i < limit && remainingToConsume > 0; i++) {
                ItemStack st = items.get(i);
                if (!st.isEmpty() && st.is(entry.getKey()) && !st.isDamaged() && !st.isEnchanted()) {
                    int take = Math.min(remainingToConsume, st.getCount());
                    st.shrink(take);
                    if (st.isEmpty()) items.set(i, ItemStack.EMPTY);
                    consumedFrom.add(new int[]{i, take});
                    remainingToConsume -= take;
                }
            }

            ItemStack leftover = insertStorageItem(craftedResult);
            if (!leftover.isEmpty()) {
                // Rollback if result couldn't fit
                for (int[] rec : consumedFrom) {
                    ItemStack cur = items.get(rec[0]);
                    if (cur.isEmpty()) {
                        items.set(rec[0], new ItemStack(entry.getKey(), rec[1]));
                    } else {
                        cur.grow(rec[1]);
                    }
                }
                continue;
            }

            setChanged();
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT, worldPosition.getX() + 0.5, worldPosition.getY() + 1.05, worldPosition.getZ() + 0.5,
                        6, 0.2, 0.1, 0.2, 0.05);
            }
            break;
        }
    }
}
