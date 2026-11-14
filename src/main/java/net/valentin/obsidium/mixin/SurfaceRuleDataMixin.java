package net.valentin.obsidium.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.valentin.obsidium.ObsidiumMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SurfaceRuleData.class)
public class SurfaceRuleDataMixin {

    @Inject(method = "overworld", at = @At("RETURN"), cancellable = true)
    private static void obsidium$addVolcanicCalderaSurfaceRule(CallbackInfoReturnable<SurfaceRules.RuleSource> cir) {
        ResourceKey<Biome> calderaKey = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "volcanic_caldera")
        );

        // Elegant, orderly volcanic surface in the Overworld:
        // On floor (surface block): Smooth Basalt
        // Under floor (sub-surface down a few blocks): Basalt
        // Deep under floor: Blackstone
        SurfaceRules.RuleSource calderaSurface = SurfaceRules.ifTrue(
            SurfaceRules.isBiome(calderaKey),
            SurfaceRules.ifTrue(
                SurfaceRules.abovePreliminarySurface(),
                SurfaceRules.sequence(
                    SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.state(Blocks.SMOOTH_BASALT.defaultBlockState())),
                    SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SurfaceRules.state(Blocks.BASALT.defaultBlockState())),
                    SurfaceRules.ifTrue(SurfaceRules.DEEP_UNDER_FLOOR, SurfaceRules.state(Blocks.BLACKSTONE.defaultBlockState()))
                )
            )
        );

        cir.setReturnValue(SurfaceRules.sequence(calderaSurface, cir.getReturnValue()));
    }
}

