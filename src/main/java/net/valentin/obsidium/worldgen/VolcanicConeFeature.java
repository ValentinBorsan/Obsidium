package net.valentin.obsidium.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.valentin.obsidium.ObsidiumMod;

public class VolcanicConeFeature extends Feature<NoneFeatureConfiguration> {
    public VolcanicConeFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    private static boolean isValidGround(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(Blocks.SMOOTH_BASALT)
                || state.is(Blocks.BASALT)
                || state.is(Blocks.BLACKSTONE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.GRASS_BLOCK);
    }

    private static boolean canReplace(BlockState state) {
        return state.isAir()
                || isValidGround(state)
                || state.is(Blocks.LAVA)
                || state.is(Blocks.WATER);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        BlockPos base = origin.below();
        BlockState ground = level.getBlockState(base);
        if (!isValidGround(ground)) {
            return false;
        }

        int radius = 7 + context.random().nextInt(3);
        int height = 12 + context.random().nextInt(7);
        boolean placedAny = false;

        for (int dy = 0; dy <= height; dy++) {
            int layerRadius = Math.max(2, (int) Math.ceil(radius * (1.0 - (double) dy / (height + 2))));
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int dx = -layerRadius; dx <= layerRadius; dx++) {
                for (int dz = -layerRadius; dz <= layerRadius; dz++) {
                    int distanceSquared = dx * dx + dz * dz;
                    if (distanceSquared > layerRadius * layerRadius) continue;

                    boolean crater = dy >= height - 2 && distanceSquared <= 2;
                    cursor.setWithOffset(base, dx, dy, dz);
                    BlockState existing = level.getBlockState(cursor);
                    if (!canReplace(existing)) continue;

                    BlockState volcanicRock;
                    float roll = context.random().nextFloat();
                    if (roll < 0.08F) {
                        volcanicRock = ObsidiumMod.VOLCANITE_ORE.defaultBlockState();
                    } else if (roll < 0.35F) {
                        volcanicRock = Blocks.SMOOTH_BASALT.defaultBlockState();
                    } else if (roll < 0.60F) {
                        volcanicRock = Blocks.BLACKSTONE.defaultBlockState();
                    } else {
                        volcanicRock = Blocks.BASALT.defaultBlockState();
                    }

                    if (crater) {
                        volcanicRock = dy == height - 2
                                ? Blocks.LAVA.defaultBlockState()
                                : Blocks.AIR.defaultBlockState();
                    } else if (dy == height - 3 && distanceSquared <= 2) {
                        volcanicRock = Blocks.MAGMA_BLOCK.defaultBlockState();
                    }
                    placedAny |= level.setBlock(cursor, volcanicRock, 2);
                }
            }
        }
        return placedAny;
    }
}
