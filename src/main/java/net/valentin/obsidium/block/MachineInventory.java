package net.valentin.obsidium.block;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Interface implemented by machines to classify slots as inputs or outputs for automation (pipes, hoppers, sorting stations)
 * and provide standardized indexed inventory serialization to prevent slot displacement on world reload.
 */
public interface MachineInventory {
    /**
     * Checks if a slot is an input slot (can receive items from pipes or automation).
     */
    boolean isInputSlot(int slot, ItemStack stack, @Nullable Direction side);

    /**
     * Checks if a slot is an output slot (can be extracted by pipes or automation).
     */
    boolean isOutputSlot(int slot, ItemStack stack, @Nullable Direction side);

    default boolean isInputSlot(int slot) {
        return isInputSlot(slot, ItemStack.EMPTY, null);
    }

    default boolean isOutputSlot(int slot) {
        return isOutputSlot(slot, ItemStack.EMPTY, null);
    }

    /**
     * Saves SimpleContainer items with explicit "Slot" indices so slot positions are strictly preserved.
     */
    static void saveInventory(CompoundTag tag, SimpleContainer inventory, HolderLookup.Provider provider) {
        ContainerHelper.saveAllItems(tag, inventory.getItems(), provider);
    }

    /**
     * Loads SimpleContainer items using explicit "Slot" indices with fallback for older legacy formats.
     */
    static void loadInventory(CompoundTag tag, SimpleContainer inventory, HolderLookup.Provider provider) {
        inventory.clearContent();
        if (tag.contains("Items", Tag.TAG_LIST)) {
            ContainerHelper.loadAllItems(tag, inventory.getItems(), provider);
        } else if (tag.contains("Inventory", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Inventory", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag itemTag = list.getCompound(i);
                if (itemTag.contains("Slot")) {
                    int slot = itemTag.getByte("Slot") & 255;
                    if (slot < inventory.getContainerSize()) {
                        ItemStack.parse(provider, itemTag).ifPresent(s -> inventory.setItem(slot, s));
                    }
                } else {
                    ItemStack.parse(provider, itemTag).ifPresent(inventory::addItem);
                }
            }
        }
    }
}

