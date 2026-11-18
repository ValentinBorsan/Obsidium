package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.fluid.FluidBucketMapping;
import net.valentin.obsidium.item.VolcaniteBottleItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VolcaniteStorageTerminalMenu extends AbstractContainerMenu {
    private final VolcaniteStorageControllerBlockEntity controller;
    private final ContainerData data;
    private ItemStack remote;
    private Player remotePlayer;

    public VolcaniteStorageTerminalMenu withRemote(ItemStack remote, Player player) {
        this.remote = remote;
        this.remotePlayer = player;
        return this;
    }

    private final SimpleContainer displayInventory = new SimpleContainer(54);
    private int scrollRow = 0;
    private String searchFilter = "";
    private int viewMode = 0; // 0 = Items, 1 = Fluids
    private long lastKnownVersion = -1;
    private int lastKnownViewMode = -1;
    private int lastKnownSortMode = -1;
    private int lastKnownScrollRow = -1;
    private String lastKnownSearch = "";
    /** Button id offset used by the client to request a scroll row (avoids clashing with 100..103). */
    public static final int SCROLL_BUTTON_OFFSET = 1000;
    // Synced to the client so the screen can draw a working scrollbar
    private final DataSlot filteredCountSlot = DataSlot.standalone();
    private final DataSlot scrollRowSlot = DataSlot.standalone();

    public VolcaniteStorageTerminalMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, null, new SimpleContainerData(6));
    }

    public VolcaniteStorageTerminalMenu(int syncId, Inventory playerInventory, VolcaniteStorageControllerBlockEntity controller, ContainerData data) {
        super(ObsidiumMod.VOLCANITE_STORAGE_TERMINAL_MENU, syncId);
        this.controller = controller;
        this.data = data;
        checkContainerDataCount(data, 6);

        // 54 Grid slots (6 rows x 9 columns) at x=8, y=20
        for (int row = 0; row < 6; ++row) {
            for (int col = 0; col < 9; ++col) {
                int index = col + row * 9;
                int x = 8 + col * 18;
                int y = 20 + row * 18;
                this.addSlot(new Slot(displayInventory, index, x, y) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return false; // Handled directly in clicked()
                    }
                });
            }
        }

        // Player Inventory at x=8, y=140
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 140 + i * 18));
            }
        }

        // Player Hotbar at x=8, y=198
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 198));
        }

        this.addDataSlots(data);
        this.addDataSlot(filteredCountSlot);
        this.addDataSlot(scrollRowSlot);
        updateDisplaySlots();
    }

    public int getEnergy() {
        return (this.data.get(0) << 16) | (this.data.get(1) & 0xFFFF);
    }

    public int getTotalStored() {
        return (this.data.get(2) << 16) | (this.data.get(3) & 0xFFFF);
    }

    public int getViewMode() {
        return this.data.get(4);
    }

    public int getSortMode() {
        return (this.data.getCount() >= 6) ? this.data.get(5) : 0;
    }

    /** Total number of entries matching the current search (synced). */
    public int getFilteredCount() {
        return filteredCountSlot.get();
    }

    /** Current first visible row (synced). */
    public int getScrollRow() {
        return scrollRowSlot.get();
    }

    /** Max scroll row so the last row of entries is still visible. */
    public int getMaxScrollRow() {
        int rows = (getFilteredCount() + 8) / 9;
        return Math.max(0, rows - 6);
    }

    public void setSearchFilter(String filter) {
        this.searchFilter = filter == null ? "" : filter.toLowerCase().trim();
        this.scrollRow = 0;
        updateDisplaySlots();
    }

    public void updateDisplaySlots() {
        if (controller == null) return;
        this.viewMode = this.data.get(4);
        int sort = getSortMode(); // 0 = Qty Desc, 1 = Qty Asc, 2 = Name A-Z, 3 = Name Z-A

        if (viewMode == 0) {
            // --- ITEM VIEW MODE ---
            Map<Item, Integer> inventory = controller.getUnifiedInventory();
            List<Map.Entry<Item, Integer>> filtered = new ArrayList<>();

            for (Map.Entry<Item, Integer> entry : inventory.entrySet()) {
                if (searchFilter.isEmpty() || entry.getKey().getDescription().getString().toLowerCase().contains(searchFilter)) {
                    filtered.add(entry);
                }
            }

            // Sort: Mode 0 = Quantity Descending (highest volume first by default!)
            if (sort == 0) {
                filtered.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
            } else if (sort == 1) {
                filtered.sort((a, b) -> Integer.compare(a.getValue(), b.getValue()));
            } else if (sort == 2) {
                filtered.sort((a, b) -> a.getKey().getDescription().getString().compareToIgnoreCase(b.getKey().getDescription().getString()));
            } else if (sort == 3) {
                filtered.sort((a, b) -> b.getKey().getDescription().getString().compareToIgnoreCase(a.getKey().getDescription().getString()));
            }

            int startIndex = clampScroll(filtered.size()) * 9;
            for (int i = 0; i < 54; i++) {
                int entryIndex = startIndex + i;
                if (entryIndex < filtered.size()) {
                    Map.Entry<Item, Integer> entry = filtered.get(entryIndex);
                    ItemStack stack = new ItemStack(entry.getKey(), 1);
                    int realCount = entry.getValue();
                    CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                        tag.putInt("RealCount", realCount);
                    });
                    displayInventory.setItem(i, stack);
                } else {
                    displayInventory.setItem(i, ItemStack.EMPTY);
                }
            }
        } else if (viewMode == 1) {
            // --- FLUID VIEW MODE ---
            Map<FluidVariant, Integer> fluids = controller.getUnifiedFluids();
            List<Map.Entry<FluidVariant, Integer>> filtered = new ArrayList<>();

            for (Map.Entry<FluidVariant, Integer> entry : fluids.entrySet()) {
                String fluidName = entry.getKey().getFluid().defaultFluidState().createLegacyBlock().getBlock().getName().getString().toLowerCase();
                if (searchFilter.isEmpty() || fluidName.contains(searchFilter)) {
                    filtered.add(entry);
                }
            }

            // Sort: Mode 0 = Quantity Descending (highest volume first by default!)
            if (sort == 0) {
                filtered.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
            } else if (sort == 1) {
                filtered.sort((a, b) -> Integer.compare(a.getValue(), b.getValue()));
            } else if (sort == 2) {
                filtered.sort((a, b) -> {
                    String na = a.getKey().getFluid().defaultFluidState().createLegacyBlock().getBlock().getName().getString();
                    String nb = b.getKey().getFluid().defaultFluidState().createLegacyBlock().getBlock().getName().getString();
                    return na.compareToIgnoreCase(nb);
                });
            } else if (sort == 3) {
                filtered.sort((a, b) -> {
                    String na = a.getKey().getFluid().defaultFluidState().createLegacyBlock().getBlock().getName().getString();
                    String nb = b.getKey().getFluid().defaultFluidState().createLegacyBlock().getBlock().getName().getString();
                    return nb.compareToIgnoreCase(na);
                });
            }

            int startIndex = clampScroll(filtered.size()) * 9;
            for (int i = 0; i < 54; i++) {
                int entryIndex = startIndex + i;
                if (entryIndex < filtered.size()) {
                    Map.Entry<FluidVariant, Integer> entry = filtered.get(entryIndex);
                    FluidVariant fv = entry.getKey();
                    Item iconItem = fv.isOf(Fluids.LAVA) ? Items.LAVA_BUCKET : (fv.isOf(Fluids.WATER) ? Items.WATER_BUCKET : Items.BUCKET);
                    ItemStack stack = new ItemStack(iconItem, 1);
                    int fluidAmount = entry.getValue();
                    String fluidId = BuiltInRegistries.FLUID.getKey(fv.getFluid()).toString();
                    CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                        tag.putBoolean("IsFluid", true);
                        tag.putString("FluidId", fluidId);
                        tag.putInt("RealCount", fluidAmount);
                    });
                    displayInventory.setItem(i, stack);
                } else {
                    displayInventory.setItem(i, ItemStack.EMPTY);
                }
            }
        } else if (viewMode == 2) {
            // --- TOOLS & EQUIPMENT VIEW MODE (From connected Tool Chests & Armories) ---
            List<ItemStack> tools = controller.getUnifiedEquipment();
            List<ItemStack> filtered = new ArrayList<>();

            for (ItemStack s : tools) {
                if (searchFilter.isEmpty() || s.getHoverName().getString().toLowerCase().contains(searchFilter)) {
                    filtered.add(s);
                }
            }

            // Sort:
            if (sort == 0) {
                // Durability Descending (fullest first)
                filtered.sort((a, b) -> {
                    double pctA = a.isDamageableItem() ? (double)(a.getMaxDamage() - a.getDamageValue()) / a.getMaxDamage() : 1.0;
                    double pctB = b.isDamageableItem() ? (double)(b.getMaxDamage() - b.getDamageValue()) / b.getMaxDamage() : 1.0;
                    return Double.compare(pctB, pctA);
                });
            } else if (sort == 1) {
                // Durability Ascending (most damaged first)
                filtered.sort((a, b) -> {
                    double pctA = a.isDamageableItem() ? (double)(a.getMaxDamage() - a.getDamageValue()) / a.getMaxDamage() : 1.0;
                    double pctB = b.isDamageableItem() ? (double)(b.getMaxDamage() - b.getDamageValue()) / b.getMaxDamage() : 1.0;
                    return Double.compare(pctA, pctB);
                });
            } else if (sort == 2) {
                filtered.sort((a, b) -> a.getHoverName().getString().compareToIgnoreCase(b.getHoverName().getString()));
            } else if (sort == 3) {
                filtered.sort((a, b) -> b.getHoverName().getString().compareToIgnoreCase(a.getHoverName().getString()));
            }

            int startIndex = clampScroll(filtered.size()) * 9;
            for (int i = 0; i < 54; i++) {
                int entryIndex = startIndex + i;
                if (entryIndex < filtered.size()) {
                    displayInventory.setItem(i, filtered.get(entryIndex).copy());
                } else {
                    displayInventory.setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }

    /** Clamps scrollRow to the valid range for the given entry count and syncs scroll state. */
    private int clampScroll(int entryCount) {
        int rows = (entryCount + 8) / 9;
        int maxRow = Math.max(0, rows - 6);
        if (scrollRow > maxRow) scrollRow = maxRow;
        if (scrollRow < 0) scrollRow = 0;
        filteredCountSlot.set(entryCount);
        scrollRowSlot.set(scrollRow);
        return scrollRow;
    }

    @Override
    public void broadcastChanges() {
        if (controller != null) {
            long currentVersion = controller.getInventoryVersion();
            int currentView = getViewMode();
            int currentSort = getSortMode();
            if (currentVersion != lastKnownVersion || currentView != lastKnownViewMode || currentSort != lastKnownSortMode || scrollRow != lastKnownScrollRow || !searchFilter.equals(lastKnownSearch)) {
                lastKnownVersion = currentVersion;
                lastKnownViewMode = currentView;
                lastKnownSortMode = currentSort;
                lastKnownScrollRow = scrollRow;
                lastKnownSearch = searchFilter;
                updateDisplaySlots();
            }
        }
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!player.level().isClientSide() && !stillValid(player)) return false;
        if (id == 100) {
            this.viewMode = 0;
            this.scrollRow = 0;
            this.data.set(4, 0);
            updateDisplaySlots();
            return true;
        } else if (id == 101) {
            this.viewMode = 1;
            this.scrollRow = 0;
            this.data.set(4, 1);
            updateDisplaySlots();
            return true;
        } else if (id == 104) {
            this.viewMode = 2;
            this.scrollRow = 0;
            this.data.set(4, 2);
            updateDisplaySlots();
            return true;
        } else if (id == 102) {
            int nextSort = (getSortMode() + 1) % 4;
            this.data.set(5, nextSort);
            this.scrollRow = 0;
            updateDisplaySlots();
            return true;
        } else if (id == 103) {
            if (controller != null) {
                VolcaniteCrafterInterfaceBlockEntity crafter = controller.findConnectedCrafterInterface();
                if (crafter != null) {
                    player.openMenu(crafter);
                } else {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("§6[Network] §cNo Volcanite Crafter Interface connected to this Storage Network!"), true);
                }
            }
            return true;
        } else if (id >= SCROLL_BUTTON_OFFSET) {
            this.scrollRow = id - SCROLL_BUTTON_OFFSET;
            updateDisplaySlots();
            return true;
        }
        return false;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (!player.level().isClientSide() && !stillValid(player)) return;
        if (controller != null && slotId >= 0 && slotId < 54) {
            ItemStack carried = getCarried();

            if (viewMode == 1) {
                // FLUID VIEW INTERACTION
                Slot slot = this.slots.get(slotId);
                ItemStack slotStack = (slot != null && slot.hasItem()) ? slot.getItem() : ItemStack.EMPTY;
                CustomData cd = slotStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                CompoundTag tag = cd.copyTag();

                // F1: Deposit a complete bucket, including supported mod fluids.
                FluidVariant bucketFluid = FluidBucketMapping.fluidInBucket(carried);
                if (!bucketFluid.isBlank()) {
                    if (controller.insertFluidExactly(bucketFluid, 1000)) {
                        carried.shrink(1);
                        if (carried.isEmpty()) {
                            setCarried(new ItemStack(Items.BUCKET));
                        } else if (!player.getInventory().add(new ItemStack(Items.BUCKET))) {
                            player.drop(new ItemStack(Items.BUCKET), false);
                        }
                        updateDisplaySlots();
                        return;
                    }
                }

                // F2: Player is holding empty Bucket -> Extract 1000 mB from clicked fluid
                if (!carried.isEmpty() && carried.is(Items.BUCKET) && tag.getBoolean("IsFluid")) {
                    String fluidId = tag.getString("FluidId");
                    Fluid fluid = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(fluidId));
                    Item filledBucket = fluid == null ? Items.AIR : FluidBucketMapping.filledBucket(FluidVariant.of(fluid));
                    if (filledBucket != Items.AIR) {
                        if (controller.extractFluidExactly(FluidVariant.of(fluid), 1000)) {
                            carried.shrink(1);
                            if (carried.isEmpty()) {
                                setCarried(new ItemStack(filledBucket));
                            } else if (!player.getInventory().add(new ItemStack(filledBucket))) {
                                player.drop(new ItemStack(filledBucket), false);
                            }
                            updateDisplaySlots();
                            return;
                        }
                    }
                }

                // F3: Player is holding VolcaniteBottle
                if (!carried.isEmpty() && carried.getItem() instanceof VolcaniteBottleItem bottle) {
                    FluidVariant bottleFluid = VolcaniteBottleItem.getFluid(carried);
                    int bottleAmount = VolcaniteBottleItem.getFluidAmount(carried);

                    if (bottleAmount > 0 && !bottleFluid.isBlank()) {
                        int inserted = controller.insertFluid(bottleFluid, Math.min(1000, bottleAmount), false);
                        if (inserted > 0) {
                            VolcaniteBottleItem.setFluid(carried, bottleFluid, bottleAmount - inserted);
                            updateDisplaySlots();
                            return;
                        }
                    } else if (tag.getBoolean("IsFluid")) {
                        String fluidId = tag.getString("FluidId");
                        Fluid fluid = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(fluidId));
                        if (fluid != null && fluid != Fluids.EMPTY) {
                            int space = bottle.getCapacity() - bottleAmount;
                            int toTake = Math.min(1000, space);
                            int extracted = controller.extractFluid(FluidVariant.of(fluid), toTake, false);
                            if (extracted > 0) {
                                VolcaniteBottleItem.setFluid(carried, FluidVariant.of(fluid), bottleAmount + extracted);
                                updateDisplaySlots();
                                return;
                            }
                        }
                    }
                }

                return;
            }

            if (viewMode == 2) {
                // TOOLS & EQUIPMENT INTERACTION
                // Case 1: Player is holding an item -> Insert into network (routes to Armory/Tool Chest)
                if (!carried.isEmpty()) {
                    ItemStack remaining = controller.insertItem(carried, false);
                    setCarried(remaining);
                    updateDisplaySlots();
                    return;
                }

                // Case 2: Player cursor is empty -> Extract exact clicked tool from Armory/network
                Slot slot = this.slots.get(slotId);
                if (slot != null && slot.hasItem()) {
                    ItemStack slotStack = slot.getItem();
                    ItemStack extracted = controller.extractSpecificEquipment(slotStack, false);

                    if (!extracted.isEmpty()) {
                        if (clickType == ClickType.QUICK_MOVE) {
                            moveExtractedToPlayer(extracted, player);
                        } else {
                            setCarried(extracted);
                        }
                        updateDisplaySlots();
                    }
                    return;
                }
                return;
            }

            // ITEM VIEW INTERACTION (viewMode == 0)
            // Case 1: Player is holding an item -> Insert into network
            if (!carried.isEmpty()) {
                ItemStack remaining = controller.insertItem(carried, false);
                setCarried(remaining);
                updateDisplaySlots();
                return;
            }

            // Case 2: Player cursor is empty -> Extract from clicked slot
            Slot slot = this.slots.get(slotId);
            if (slot != null && slot.hasItem()) {
                Item item = slot.getItem().getItem();
                int extractCount = (button == 1) ? 1 : item.getDefaultInstance().getMaxStackSize();
                ItemStack extracted = controller.extractItem(item, extractCount, false);

                if (!extracted.isEmpty()) {
                    if (clickType == ClickType.QUICK_MOVE) {
                        // Shift-click: move directly to player inventory
                        moveExtractedToPlayer(extracted, player);
                    } else {
                        setCarried(extracted);
                    }
                    updateDisplaySlots();
                }
                return;
            }
        }

        super.clicked(slotId, button, clickType, player);
    }

    private void moveExtractedToPlayer(ItemStack extracted, Player player) {
        this.moveItemStackTo(extracted, 54, 90, false);
        // moveItemStackTo returns true even when only part of the stack fits.
        if (!extracted.isEmpty()) {
            ItemStack remaining = controller.insertItem(extracted, false);
            if (!remaining.isEmpty()) {
                player.drop(remaining, false);
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!player.level().isClientSide() && !stillValid(player)) return ItemStack.EMPTY;
        if (controller == null) return ItemStack.EMPTY;
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemStack = slotStack.copy();

            if (index >= 54) {
                if (viewMode == 1) {
                    FluidVariant bucketFluid = FluidBucketMapping.fluidInBucket(slotStack);
                    if (!bucketFluid.isBlank()) {
                        if (controller.insertFluidExactly(bucketFluid, 1000)) {
                            slotStack.shrink(1);
                            if (slotStack.isEmpty()) slot.set(new ItemStack(Items.BUCKET));
                            else if (!player.getInventory().add(new ItemStack(Items.BUCKET))) {
                                player.drop(new ItemStack(Items.BUCKET), false);
                            }
                            slot.setChanged();
                            updateDisplaySlots();
                            return itemStack;
                        }
                    }
                } else {
                    ItemStack remaining = controller.insertItem(slotStack, false);
                    slot.set(remaining);
                    slot.setChanged();
                    updateDisplaySlots();
                    return itemStack;
                }
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (controller == null) return true; // Client menu has no block entity.
        var level = controller.getLevel();
        if (level == null || controller.isRemoved() || !level.getChunkSource().hasChunk(controller.getBlockPos().getX() >> 4, controller.getBlockPos().getZ() >> 4)
                || level.getBlockEntity(controller.getBlockPos()) != controller) return false;
        if (remote != null) {
            boolean carried = false;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (player.getInventory().getItem(i) == remote) carried = true;
            }
            return player == remotePlayer && carried
                    && net.valentin.obsidium.item.VolcaniteStorageRemoteItem.resolveController(player, remote) == controller;
        }
        return player.level() == level
                && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(controller.getBlockPos())) <= 64;
    }
}
