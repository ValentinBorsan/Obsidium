package net.valentin.obsidium.fluid;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;

/** Full Fabric variant persistence, with a read-only fallback for legacy fluid-ID saves. */
public final class FluidVariantPersistence {
    private FluidVariantPersistence() {}

    public static void write(CompoundTag tag, FluidVariant fluid, HolderLookup.Provider registries) {
        tag.put("FluidVariant", FluidVariant.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), fluid).getOrThrow());
        tag.putString("Fluid", BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString());
    }

    public static FluidVariant read(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("FluidVariant")) {
            // Never strip components by falling back to the plain ID if a new-format payload is malformed.
            return FluidVariant.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("FluidVariant"))
                    .result().orElse(FluidVariant.blank());
        }
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("Fluid"));
        return id != null && BuiltInRegistries.FLUID.containsKey(id)
                ? FluidVariant.of(BuiltInRegistries.FLUID.get(id)) : FluidVariant.blank();
    }
}
