package net.valentin.obsidium.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.valentin.obsidium.ObsidiumMod;

public class PyricBronzeToolMaterial implements Tier {
    public static final PyricBronzeToolMaterial INSTANCE = new PyricBronzeToolMaterial();

    @Override
    public int getUses() { return 650; }

    @Override
    public float getSpeed() { return 7.0f; }

    @Override
    public float getAttackDamageBonus() { return 2.5f; }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_IRON_TOOL; }

    @Override
    public int getEnchantmentValue() { return 16; }

    @Override
    public Ingredient getRepairIngredient() { return Ingredient.of(ObsidiumMod.PYRIC_BRONZE_INGOT); }
}
