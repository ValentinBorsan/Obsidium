package net.valentin.obsidium.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.valentin.obsidium.ObsidiumMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(OverworldBiomeBuilder.class)
public class OverworldBiomeBuilderMixin {

    @Inject(method = "addBiomes", at = @At("TAIL"))
    private void obsidium$addVolcanicCaldera(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> consumer, CallbackInfo ci) {
        ResourceKey<Biome> caldera = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "volcanic_caldera")
        );

        // 1. Hot, Arid Plateaus and Highlands in the Overworld
        consumer.accept(Pair.of(
            Climate.parameters(
                Climate.Parameter.span(0.55f, 1.0f),   // Hot
                Climate.Parameter.span(-1.0f, -0.35f),  // Very Arid
                Climate.Parameter.span(0.30f, 0.90f),  // Inland / Far Inland
                Climate.Parameter.span(-0.85f, 0.40f), // Mountain Peaks & High Valleys
                Climate.Parameter.point(0.0f),         // Surface
                Climate.Parameter.span(0.10f, 1.0f),   // Ridges & Peaks
                0.0f
            ),
            caldera
        ));

        // 2. Volcanic Valleys & Basalt Calderas
        consumer.accept(Pair.of(
            Climate.parameters(
                Climate.Parameter.span(0.65f, 1.0f),   // Hot
                Climate.Parameter.span(-0.80f, -0.20f), // Arid
                Climate.Parameter.span(0.20f, 0.70f),  // Mid-Inland
                Climate.Parameter.span(-0.40f, 0.25f), // Rolling Slopes & Low Valleys
                Climate.Parameter.point(0.0f),         // Surface
                Climate.Parameter.span(-0.60f, 0.0f),  // Low Weirdness
                0.0f
            ),
            caldera
        ));
    }
}

