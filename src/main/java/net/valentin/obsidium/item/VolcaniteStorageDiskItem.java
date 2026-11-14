package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

public class VolcaniteStorageDiskItem extends Item {
    private final int capacity;

    public VolcaniteStorageDiskItem(Properties properties, int capacity) {
        super(properties);
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    public static class StoredEntry {
        public final Item item;
        public int count;

        public StoredEntry(Item item, int count) {
            this.item = item;
            this.count = count;
        }
    }

    public static int getTotalCount(ItemStack disk) {
        CustomData data = disk.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getInt("TotalCount");
    }

    public static List<StoredEntry> getStoredEntries(ItemStack disk) {
        List<StoredEntry> list = new ArrayList<>();
        CustomData data = disk.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        if (tag.contains("Items", 9)) {
            ListTag listTag = tag.getList("Items", 10);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag itemTag = listTag.getCompound(i);
                String id = itemTag.getString("id");
                int count = itemTag.getInt("count");
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(id));
                if (item != null && count > 0) {
                    list.add(new StoredEntry(item, count));
                }
            }
        }
        return list;
    }

    public static ItemStack insert(ItemStack disk, ItemStack toInsert, boolean simulate) {
        if (disk.isEmpty() || toInsert.isEmpty()) return toInsert;
        if (!(disk.getItem() instanceof VolcaniteStorageDiskItem diskItem)) return toInsert;

        // This disk format stores only item IDs and counts. Reject any variant
        // that cannot be reconstructed exactly instead of silently discarding its data.
        if (!canStoreLosslessly(toInsert)) {
            return toInsert;
        }

        int currentTotal = getTotalCount(disk);
        int space = diskItem.getCapacity() - currentTotal;
        if (space <= 0) return toInsert;

        int insertAmount = Math.min(space, toInsert.getCount());
        if (insertAmount <= 0) return toInsert;

        if (!simulate) {
            CustomData.update(DataComponents.CUSTOM_DATA, disk, tag -> {
                ListTag listTag = tag.getList("Items", 10);
                String insertId = BuiltInRegistries.ITEM.getKey(toInsert.getItem()).toString();
                boolean found = false;

                for (int i = 0; i < listTag.size(); i++) {
                    CompoundTag entry = listTag.getCompound(i);
                    if (entry.getString("id").equals(insertId)) {
                        entry.putInt("count", entry.getInt("count") + insertAmount);
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    CompoundTag newEntry = new CompoundTag();
                    newEntry.putString("id", insertId);
                    newEntry.putInt("count", insertAmount);
                    listTag.add(newEntry);
                }

                tag.put("Items", listTag);
                tag.putInt("TotalCount", currentTotal + insertAmount);
            });
        }

        ItemStack remaining = toInsert.copy();
        remaining.shrink(insertAmount);
        return remaining;
    }

    public static boolean canStoreLosslessly(ItemStack stack) {
        return !stack.isEmpty()
                && ItemStack.isSameItemSameComponents(stack, new ItemStack(stack.getItem()));
    }

    public static ItemStack extract(ItemStack disk, Item item, int amount, boolean simulate) {
        if (disk.isEmpty() || item == null || amount <= 0) return ItemStack.EMPTY;
        if (!(disk.getItem() instanceof VolcaniteStorageDiskItem)) return ItemStack.EMPTY;

        CustomData data = disk.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        if (!tag.contains("Items", 9)) return ItemStack.EMPTY;

        ListTag listTag = tag.getList("Items", 10);
        String targetId = BuiltInRegistries.ITEM.getKey(item).toString();
        int available = 0;
        int foundIndex = -1;

        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag entry = listTag.getCompound(i);
            if (entry.getString("id").equals(targetId)) {
                available = entry.getInt("count");
                foundIndex = i;
                break;
            }
        }

        if (available <= 0 || foundIndex == -1) return ItemStack.EMPTY;

        int extractAmount = Math.min(available, amount);
        int finalAvailable = available;
        int finalFoundIndex = foundIndex;

        if (!simulate) {
            CustomData.update(DataComponents.CUSTOM_DATA, disk, currentTag -> {
                ListTag list = currentTag.getList("Items", 10);
                int currentTotal = currentTag.getInt("TotalCount");
                if (extractAmount >= finalAvailable) {
                    list.remove(finalFoundIndex);
                } else {
                    CompoundTag entry = list.getCompound(finalFoundIndex);
                    entry.putInt("count", finalAvailable - extractAmount);
                }
                currentTag.put("Items", list);
                currentTag.putInt("TotalCount", Math.max(0, currentTotal - extractAmount));
            });
        }

        return new ItemStack(item, extractAmount);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag isAdvanced) {
        int count = getTotalCount(stack);
        int percent = (int) (((float) count / capacity) * 100);
        ChatFormatting color = percent >= 95 ? ChatFormatting.RED : (percent >= 70 ? ChatFormatting.GOLD : ChatFormatting.GREEN);

        tooltip.add(Component.literal("Stored: " + String.format("%,d", count) + " / " + String.format("%,d", capacity) + " items (" + percent + "%)").withStyle(color));
        tooltip.add(Component.literal("Volcanite Network Storage").withStyle(ChatFormatting.DARK_GRAY));
    }
}
