package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.fluid.TransactionalFluidAccess;
import net.valentin.obsidium.item.VolcaniteLiquidStorageDiskItem;
import net.valentin.obsidium.item.VolcaniteStorageDiskItem;

import java.util.Map;

public class VolcaniteStorageDriveBlockEntity extends BlockEntity implements Container, MenuProvider {
    private NonNullList<ItemStack> items = NonNullList.withSize(8, ItemStack.EMPTY);

    private final TransactionalFluidAccess<java.util.List<ItemStack>> transactionalFluid =
            new TransactionalFluidAccess<>(
                    () -> items.stream().map(ItemStack::copy).toList(),
                    snapshot -> { for (int i = 0; i < items.size(); i++) items.set(i, snapshot.get(i)); },
                    (fluid, amount) -> insertFluid(fluid, amount, false),
                    (fluid, amount) -> extractFluid(fluid, amount, false), this::setChanged);

    public int transferFluid(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant fluid, int amount,
                             boolean inserting, TransactionContext transaction) {
        return transactionalFluid.transfer(fluid, amount, inserting, transaction);
    }

    public VolcaniteStorageDriveBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.VOLCANITE_STORAGE_DRIVE_BE, pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.volcanite_storage_disk_drive");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return new VolcaniteStorageDriveMenu(syncId, inv, this);
    }

    @Override
    public int getContainerSize() {
        return 8;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
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
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return !stack.isEmpty()
                && slot >= 0 && slot < items.size()
                && (stack.getItem() instanceof VolcaniteStorageDiskItem
                        || stack.getItem() instanceof VolcaniteLiquidStorageDiskItem);
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    // --- Network Storage Methods ---

    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return stack;
        ItemStack remaining = stack.copy();

        for (int i = 0; i < items.size(); i++) {
            ItemStack disk = items.get(i);
            if (!disk.isEmpty() && disk.getItem() instanceof VolcaniteStorageDiskItem) {
                remaining = VolcaniteStorageDiskItem.insert(disk, remaining, simulate);
                if (!simulate) setChanged();
                if (remaining.isEmpty()) break;
            }
        }
        return remaining;
    }

    public ItemStack extractMatchingItem(ItemStack template, int amount, boolean simulate) {
        if (!VolcaniteStorageDiskItem.canStoreLosslessly(template)) return ItemStack.EMPTY;
        return extractItem(template.getItem(), amount, simulate);
    }

    public ItemStack extractItem(Item item, int amount, boolean simulate) {
        if (item == null || amount <= 0) return ItemStack.EMPTY;
        int needed = amount;
        ItemStack result = ItemStack.EMPTY;

        for (int i = 0; i < items.size(); i++) {
            ItemStack disk = items.get(i);
            if (!disk.isEmpty() && disk.getItem() instanceof VolcaniteStorageDiskItem) {
                ItemStack extracted = VolcaniteStorageDiskItem.extract(disk, item, needed, simulate);
                if (!extracted.isEmpty()) {
                    if (result.isEmpty()) {
                        result = extracted.copy();
                    } else {
                        result.grow(extracted.getCount());
                    }
                    needed -= extracted.getCount();
                    if (!simulate) setChanged();
                    if (needed <= 0) break;
                }
            }
        }
        return result;
    }

    public void collectStoredItems(Map<Item, Integer> map) {
        for (int i = 0; i < items.size(); i++) {
            ItemStack disk = items.get(i);
            if (!disk.isEmpty() && disk.getItem() instanceof VolcaniteStorageDiskItem) {
                for (VolcaniteStorageDiskItem.StoredEntry entry : VolcaniteStorageDiskItem.getStoredEntries(disk)) {
                    map.put(entry.item, map.getOrDefault(entry.item, 0) + entry.count);
                }
            }
        }
    }

    public int getTotalStored() {
        int total = 0;
        for (ItemStack disk : items) {
            if (!disk.isEmpty() && disk.getItem() instanceof VolcaniteStorageDiskItem) {
                total += VolcaniteStorageDiskItem.getTotalCount(disk);
            }
        }
        return total;
    }

    public int getMaxCapacity() {
        int total = 0;
        for (ItemStack disk : items) {
            if (!disk.isEmpty() && disk.getItem() instanceof VolcaniteStorageDiskItem diskItem) {
                total += diskItem.getCapacity();
            }
        }
        return total;
    }

    public int insertFluid(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant fluid, int amount_mB, boolean simulate) {
        if (fluid.isBlank() || amount_mB <= 0) return 0;
        int remaining = amount_mB;
        int totalInserted = 0;

        for (int i = 0; i < items.size(); i++) {
            ItemStack disk = items.get(i);
            if (!disk.isEmpty() && disk.getItem() instanceof VolcaniteLiquidStorageDiskItem) {
                int inserted = VolcaniteLiquidStorageDiskItem.insert(disk, fluid, remaining, simulate);
                remaining -= inserted;
                totalInserted += inserted;
                if (!simulate && inserted > 0) setChanged();
                if (remaining <= 0) break;
            }
        }
        return totalInserted;
    }

    public int extractFluid(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant fluid, int amount_mB, boolean simulate) {
        if (amount_mB <= 0) return 0;
        int remaining = amount_mB;
        int totalExtracted = 0;

        for (int i = 0; i < items.size(); i++) {
            ItemStack disk = items.get(i);
            if (!disk.isEmpty() && disk.getItem() instanceof VolcaniteLiquidStorageDiskItem) {
                int extracted = VolcaniteLiquidStorageDiskItem.extract(disk, fluid, remaining, simulate);
                remaining -= extracted;
                totalExtracted += extracted;
                if (!simulate && extracted > 0) setChanged();
                if (remaining <= 0) break;
            }
        }
        return totalExtracted;
    }

    public void collectStoredFluids(Map<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant, Integer> map) {
        for (int i = 0; i < items.size(); i++) {
            ItemStack disk = items.get(i);
            if (!disk.isEmpty() && disk.getItem() instanceof VolcaniteLiquidStorageDiskItem) {
                net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant fluid = VolcaniteLiquidStorageDiskItem.getStoredFluid(disk);
                int amount = VolcaniteLiquidStorageDiskItem.getStoredAmount(disk);
                if (!fluid.isBlank() && amount > 0) {
                    map.put(fluid, map.getOrDefault(fluid, 0) + amount);
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, this.items, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, provider);
    }
}
