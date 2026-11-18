package net.valentin.obsidium.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.valentin.obsidium.block.AegisSanctuaryWardBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {
    @Inject(method = "isValidSpawnPostitionForType", at = @At("HEAD"), cancellable = true)
    private static void obsidium$blockHostileSpawnNearWard(
            ServerLevel level,
            MobCategory category,
            StructureManager structureManager,
            ChunkGenerator generator,
            MobSpawnSettings.SpawnerData data,
            BlockPos.MutableBlockPos pos,
            double distSqr,
            CallbackInfoReturnable<Boolean> cir) {
        if (category == MobCategory.MONSTER && AegisSanctuaryWardBlockEntity.isProtected(level, pos)) {
            cir.setReturnValue(false);
        }
    }
}
