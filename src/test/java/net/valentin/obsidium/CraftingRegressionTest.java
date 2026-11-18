package net.valentin.obsidium;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.MapCloningRecipe;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.valentin.obsidium.block.CraftingIngredientBatch;
import net.valentin.obsidium.block.RecipeGridTransfer;
import net.valentin.obsidium.block.StorageItemTransfers;

import java.util.ArrayList;
import java.util.List;

final class CraftingRegressionTest {
    static void run() {
        SimpleContainer grid = new SimpleContainer(9);
        SimpleContainer source = new SimpleContainer(9);
        List<ItemStack> dropped = new ArrayList<>();
        List<List<Item>> choices = List.of(List.of(Items.OAK_PLANKS, Items.BIRCH_PLANKS));
        grid.setItem(0, new ItemStack(Items.OAK_PLANKS));
        source.setItem(0, new ItemStack(Items.BIRCH_PLANKS, 10));
        check(RecipeGridTransfer.transfer(grid, source, choices, 64, null, dropped::add) == 0,
                "Alternative ingredients must not become the existing plank type");
        check(grid.getItem(0).getCount() == 1 && source.getItem(0).getCount() == 10,
                "Rejected alternative changed inventory");

        grid.clearContent();
        source.setItem(1, new ItemStack(Items.OAK_PLANKS));
        check(RecipeGridTransfer.transfer(grid, source, choices, 64, null, dropped::add) == 1,
                "Later rounds must keep the variant chosen by the first round");
        check(grid.getItem(0).is(Items.OAK_PLANKS) && grid.getItem(0).getCount() == 1,
                "Planks were converted or duplicated");
        check(source.getItem(0).getCount() == 10, "Different plank variant was consumed");

        ItemStack named = new ItemStack(Items.PAPER);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Keep name"));
        grid.clearContent();
        source.clearContent();
        grid.setItem(0, named.copy());
        source.setItem(0, new ItemStack(Items.PAPER, 3));
        check(RecipeGridTransfer.transfer(grid, source, List.of(List.of(Items.PAPER)), 1, null, dropped::add) == 0,
                "Same item with different components must not merge");
        check(source.getItem(0).getCount() == 3, "Plain paper became named paper");

        grid.clearContent();
        source.clearContent();
        source.setItem(0, new ItemStack(Items.OAK_PLANKS, 2));
        source.setItem(1, new ItemStack(Items.STICK));
        RecipeGridTransfer.Network full = new RecipeGridTransfer.Network() {
            public ItemStack extract(Item item, ItemStack existing) { return ItemStack.EMPTY; }
            public ItemStack insert(ItemStack stack) { return stack; }
        };
        check(RecipeGridTransfer.transfer(grid, source,
                List.of(List.of(Items.OAK_PLANKS), List.of(Items.STICK)), 64, full, dropped::add) == 1,
                "An incomplete second round must retain the first round");
        check(source.getItem(0).getCount() == 1 && grid.getItem(0).getCount() == 1
                && grid.getItem(1).getCount() == 1 && dropped.isEmpty(),
                "Failed round did not refund the original player slot");

        grid.clearContent();
        source.clearContent();
        List<ItemStack> networkItems = new ArrayList<>(List.of(named.copy()));
        RecipeGridTransfer.Network rejecting = new RecipeGridTransfer.Network() {
            public ItemStack extract(Item item, ItemStack existing) {
                return existing.isEmpty() ? StorageItemTransfers.extract(networkItems, 1, item, 1, false)
                        : StorageItemTransfers.extractMatching(networkItems, 1, existing, 1, false);
            }
            public ItemStack insert(ItemStack stack) { return stack; }
        };
        check(RecipeGridTransfer.transfer(grid, source,
                List.of(List.of(Items.PAPER), List.of(Items.DIAMOND)), 1, rejecting, dropped::add) == 0,
                "Missing second ingredient must fail");
        check(grid.isEmpty() && dropped.size() == 1 && ItemStack.matches(dropped.get(0), named),
                "A full network must not discard refunded ingredients or their components");
        dropped.clear();

        grid.setItem(0, new ItemStack(Items.ENDER_PEARL, 15));
        source.setItem(0, new ItemStack(Items.ENDER_PEARL, 10));
        check(RecipeGridTransfer.transfer(grid, source, List.of(List.of(Items.ENDER_PEARL)), 64, null, dropped::add) == 1,
                "Batch refill must stop at the item's stack limit");
        check(grid.getItem(0).getCount() == 16 && source.getItem(0).getCount() == 9,
                "Stack limit overflow lost or duplicated ingredients");

        ItemStack requiredMap = new ItemStack(Items.FILLED_MAP);
        requiredMap.set(DataComponents.MAP_ID, new MapId(1));
        ItemStack differentMap = requiredMap.copy();
        differentMap.set(DataComponents.MAP_ID, new MapId(2));
        List<ItemStack> storage = new ArrayList<>(List.of(differentMap.copy(), new ItemStack(Items.MAP, 3)));
        List<ItemStack> pattern = new ArrayList<>();
        pattern.add(requiredMap);
        pattern.add(new ItemStack(Items.MAP));
        while (pattern.size() < 9) pattern.add(ItemStack.EMPTY);
        List<ItemStack> refunded = new ArrayList<>();
        try (CraftingIngredientBatch batch = new CraftingIngredientBatch(pattern,
                required -> StorageItemTransfers.extractMatching(storage, storage.size(), required, 1, false), refunded::add)) {
            check(!batch.isComplete(), "Blueprint must not substitute a different filled map");
        }
        check(ItemStack.matches(storage.get(0), differentMap) && storage.get(1).getCount() == 3,
                "A failed exact reservation consumed the wrong map");

        storage.add(requiredMap.copy());
        try (CraftingIngredientBatch batch = new CraftingIngredientBatch(pattern,
                required -> StorageItemTransfers.extractMatching(storage, storage.size(), required, 1, false), refunded::add)) {
            check(batch.isComplete(), "Exact map variant was not found after a different variant");
            CraftingInput actual = CraftingInput.of(3, 3, batch.input());
            MapCloningRecipe recipe = new MapCloningRecipe(CraftingBookCategory.MISC);
            check(recipe.matches(actual, null), "Actual extracted input does not match the vanilla recipe");
            ItemStack result = recipe.assemble(actual, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
            check(result.getCount() == 2 && new MapId(1).equals(result.get(DataComponents.MAP_ID)),
                    "Vanilla assembly must copy the map that was actually consumed");
            batch.consume();
        }
        check(refunded.isEmpty() && ItemStack.matches(storage.get(0), differentMap)
                && storage.get(1).getCount() == 2 && storage.get(2).isEmpty(),
                "Successful crafting changed unrelated variants or refunded consumed ingredients");

        try (CraftingIngredientBatch batch = new CraftingIngredientBatch(List.of(named, new ItemStack(Items.DIAMOND)),
                required -> required.is(Items.PAPER) ? named.copy() : ItemStack.EMPTY, refunded::add)) {
            check(!batch.isComplete(), "Incomplete batch must not commit");
        }
        check(refunded.size() == 1 && ItemStack.matches(refunded.get(0), named), "Partial batch refund lost data");
        refunded.clear();
        try (CraftingIngredientBatch batch = new CraftingIngredientBatch(List.of(named), required -> named.copy(), refunded::add)) {
            check(batch.isComplete(), "Valid batch failed");
            // Deliberately leave unconsumed: e.g. output full, stock limit or recipe rejected.
        }
        check(refunded.size() == 1 && ItemStack.matches(refunded.get(0), named), "Abandoned valid batch was not refunded");
        refunded.clear();
        try (CraftingIngredientBatch batch = new CraftingIngredientBatch(List.of(named, new ItemStack(Items.DIAMOND)), required -> {
                if (required.is(Items.DIAMOND)) throw new IllegalStateException("Test extraction failure");
                return named.copy();
            }, refunded::add)) {
            check(batch.isComplete(), "Unexpected incomplete batch");
            throw new AssertionError("Expected extraction failure");
        } catch (IllegalStateException expected) {
            check(refunded.size() == 1 && ItemStack.matches(refunded.get(0), named), "Extraction exception lost reserved items");
        }
        System.out.println("Crafting regression checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
