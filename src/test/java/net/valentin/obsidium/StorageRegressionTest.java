package net.valentin.obsidium;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.valentin.obsidium.block.StorageItemTransfers;
import net.valentin.obsidium.item.VolcaniteStorageDiskItem;

import java.util.ArrayList;
import java.util.List;

/** Standalone regression checks, run by Gradle check/storageRegressionTest. */
public final class StorageRegressionTest {
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        ItemStack plain = new ItemStack(Items.PAPER, 12);
        ItemStack named = plain.copy();
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Keep this name"));
        ItemStack dataItem = plain.copy();
        CustomData.update(DataComponents.CUSTOM_DATA, dataItem, tag -> tag.putLong("Energy", 12345));
        ItemStack damaged = new ItemStack(Items.DIAMOND_PICKAXE);
        damaged.setDamageValue(10);
        check(VolcaniteStorageDiskItem.canStoreLosslessly(plain), "Plain item should be accepted");
        check(!VolcaniteStorageDiskItem.canStoreLosslessly(named), "Named item would lose its name");
        check(!VolcaniteStorageDiskItem.canStoreLosslessly(dataItem), "Custom data would be lost");
        check(!VolcaniteStorageDiskItem.canStoreLosslessly(damaged), "Durability would be reset");
        check(!VolcaniteStorageDiskItem.canStoreLosslessly(ItemStack.EMPTY), "Empty stack is not storable");

        List<ItemStack> inventory = new ArrayList<>(List.of(named.copy(), plain.copy(), named.copy(), dataItem.copy()));
        List<ItemStack> snapshot = inventory.stream().map(ItemStack::copy).toList();
        ItemStack preview = StorageItemTransfers.extract(inventory, 4, Items.PAPER, 64, true);
        check(preview.getCount() == 24 && ItemStack.isSameItemSameComponents(preview, named),
                "Extraction must combine only identical named variants");
        for (int i = 0; i < inventory.size(); i++) {
            check(ItemStack.matches(snapshot.get(i), inventory.get(i)), "Simulation changed slot " + i);
        }
        ItemStack extracted = StorageItemTransfers.extract(inventory, 4, Items.PAPER, 64, false);
        check(ItemStack.matches(preview, extracted), "Actual extraction differs from simulation");
        check(inventory.get(0).isEmpty() && inventory.get(2).isEmpty(), "Matched items were not removed");
        check(ItemStack.matches(inventory.get(1), plain) && ItemStack.matches(inventory.get(3), dataItem),
                "Other variants must remain untouched");
        ItemStack partial = StorageItemTransfers.extract(inventory, 4, Items.PAPER, 5, false);
        check(partial.getCount() == 5 && inventory.get(1).getCount() == 7,
                "Partial extraction must conserve quantities");
        check(StorageItemTransfers.extract(inventory, 1, Items.PAPER, 64, false).isEmpty(),
                "Locked slots must not be accessible");
        check(StorageItemTransfers.extract(inventory, 4, Items.PAPER, 0, false).isEmpty(),
                "Zero extraction must be a no-op");
        check(StorageItemTransfers.extract(inventory, 4, Items.PAPER, -1, false).isEmpty(),
                "Negative extraction must be a no-op");
        ItemStack custom = StorageItemTransfers.extract(inventory, 4, Items.PAPER, 64, false);
        check(custom.getCount() == 7 && ItemStack.isSameItemSameComponents(custom, plain),
                "Plain variant must not absorb custom data items");
        ItemStack last = StorageItemTransfers.extract(inventory, 4, Items.PAPER, 64, false);
        check(last.getCount() == 12 && ItemStack.isSameItemSameComponents(last, dataItem),
                "Last variant must retain custom data");
        check(inventory.stream().allMatch(ItemStack::isEmpty), "Items remain after complete extraction");
        System.out.println("Storage regression checks passed");
        FluidRegressionTest.run();
        CraftingRegressionTest.run();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
