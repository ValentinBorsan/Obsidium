package net.valentin.obsidium.block;

import net.valentin.obsidium.ObsidiumMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class MagmaForgeRecipe {
    public final String category;
    public final List<ItemStack> ingredients; // 1 to 3 ingredients with counts
    public final ItemStack result;
    public final ItemStack bonusResult; // Extra reward when forged with >= 2 Perfect Strikes
    public final long energyCost;
    public final long lavaCost; // in droplets (81000 droplets = 1000 mB = 1 Bucket)

    public MagmaForgeRecipe(String category, List<ItemStack> ingredients, ItemStack result, ItemStack bonusResult, long energyCost, long lavaCostMb) {
        this.category = category;
        this.ingredients = ingredients;
        this.result = result;
        this.bonusResult = bonusResult;
        this.energyCost = energyCost;
        this.lavaCost = lavaCostMb * 81L;
    }

    public static MagmaForgeRecipe of(String category, ItemStack in1, ItemStack in2, ItemStack in3, ItemStack result, ItemStack bonusResult, long energyCost, long lavaCostMb) {
        List<ItemStack> list = new ArrayList<>();
        if (!in1.isEmpty()) list.add(in1);
        if (!in2.isEmpty()) list.add(in2);
        if (!in3.isEmpty()) list.add(in3);
        return new MagmaForgeRecipe(category, list, result, bonusResult, energyCost, lavaCostMb);
    }

    public long getLavaCostMb() {
        return lavaCost / 81L;
    }

    public String getCategory() {
        return category;
    }

    public List<ItemStack> getIngredients() {
        return ingredients;
    }

    public ItemStack getResult() {
        return result;
    }

    public ItemStack getBonusResult() {
        return bonusResult;
    }

    public long getEnergyCost() {
        return energyCost;
    }

    public long getLavaCost() {
        return getLavaCostMb();
    }

    /**
     * Checks if the 3 input slots contain all required ingredients (order-independent) with sufficient stack counts.
     */
    public boolean matches(ItemStack slot0, ItemStack slot1, ItemStack slot2) {
        ItemStack[] slots = new ItemStack[]{slot0, slot1, slot2};
        boolean[] usedSlots = new boolean[3];

        // Count non-empty slots
        int nonEmptySlots = 0;
        for (ItemStack s : slots) {
            if (!s.isEmpty()) nonEmptySlots++;
        }
        if (nonEmptySlots != ingredients.size()) {
            return false;
        }

        for (ItemStack req : ingredients) {
            boolean found = false;
            for (int i = 0; i < 3; i++) {
                if (!usedSlots[i] && !slots[i].isEmpty() && areItemsCompatible(slots[i], req) && slots[i].getCount() >= req.getCount()) {
                    usedSlots[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    public static boolean areItemsCompatible(ItemStack slot, ItemStack req) {
        if (slot.is(req.getItem())) return true;

        // Conventional tag compatibility for common metals & materials from other mods
        if (req.is(net.minecraft.world.item.Items.COPPER_INGOT) && slot.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ingots/copper")))) return true;
        if (req.is(net.minecraft.world.item.Items.IRON_INGOT) && slot.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ingots/iron")))) return true;
        if (req.is(net.minecraft.world.item.Items.GOLD_INGOT) && slot.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ingots/gold")))) return true;
        if (req.is(net.minecraft.world.item.Items.NETHERITE_INGOT) && slot.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ingots/netherite")))) return true;
        if (req.is(ObsidiumMod.TIN_INGOT) && slot.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ingots/tin")))) return true;
        if (req.is(ObsidiumMod.LEAD_INGOT) && slot.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ingots/lead")))) return true;
        if (req.is(ObsidiumMod.VOLCANITE_INGOT) && slot.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ingots/volcanite")))) return true;
        if (req.is(ObsidiumMod.PYRIC_BRONZE_INGOT) && slot.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ingots/bronze")))) return true;

        return false;
    }

    /**
     * Consumes the required ingredient amounts from the 3 input slots.
     */
    public void consumeInputs(net.minecraft.world.Container container) {
        boolean[] usedSlots = new boolean[3];
        for (ItemStack req : ingredients) {
            for (int i = 0; i < 3; i++) {
                ItemStack s = container.getItem(i);
                if (!usedSlots[i] && !s.isEmpty() && areItemsCompatible(s, req) && s.getCount() >= req.getCount()) {
                    usedSlots[i] = true;
                    s.shrink(req.getCount());
                    if (s.isEmpty()) {
                        container.setItem(i, ItemStack.EMPTY);
                    }
                    break;
                }
            }
        }
    }

    public Item getIngredientItem(int idx) {
        if (idx < 0 || idx >= ingredients.size()) return Items.AIR;
        return ingredients.get(idx).getItem();
    }

    public ItemStack getIngredientStack(int idx) {
        if (idx < 0 || idx >= ingredients.size()) return ItemStack.EMPTY;
        return ingredients.get(idx);
    }
}
