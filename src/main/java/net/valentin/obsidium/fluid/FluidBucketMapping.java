package net.valentin.obsidium.fluid;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;

public final class FluidBucketMapping {
    private FluidBucketMapping() {}

    public static Item filledBucket(FluidVariant variant) {
        if (variant.isBlank() || !variant.getComponents().isEmpty()) return Items.AIR;
        Item bucket = variant.getFluid().getBucket();
        return bucket == null || bucket == Items.BUCKET ? Items.AIR : bucket;
    }

    public static FluidVariant fluidInBucket(ItemStack stack) {
        if (stack.isEmpty() || stack.is(Items.BUCKET)) return FluidVariant.blank();
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            if (fluid.defaultFluidState().isSource() && fluid.getBucket() == stack.getItem()
                    && filledBucket(FluidVariant.of(fluid)) != Items.AIR) {
                return FluidVariant.of(fluid);
            }
        }
        return FluidVariant.blank();
    }
}
