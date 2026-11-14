package net.valentin.obsidium.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.valentin.obsidium.ObsidiumMod;

public class VolcanicToolMaterial implements Tier {
    public static final VolcanicToolMaterial INSTANCE = new VolcanicToolMaterial();

    @Override
    public int getUses() { return 3000; }

    @Override
    public float getSpeed() { return 10.0f; }

    @Override
    public float getAttackDamageBonus() { return 6.0f; }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_NETHERITE_TOOL; }

    @Override
    public int getEnchantmentValue() { return 22; }

    @Override
    public Ingredient getRepairIngredient() { return Ingredient.of(ObsidiumMod.VOLCANIC_ALLOY); }
}
