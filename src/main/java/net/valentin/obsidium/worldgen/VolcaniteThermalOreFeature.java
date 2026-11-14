package net.valentin.obsidium.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.FluidState;
import net.valentin.obsidium.ObsidiumMod;

public class VolcaniteThermalOreFeature extends Feature<NoneFeatureConfiguration> {

    public VolcaniteThermalOreFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        // Search for a thermal anchor (Obsidian, Crying Obsidian, Magma, or Lava) within radius 3
        BlockPos.MutableBlockPos anchorPos = null;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        searchLoop:
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    FluidState fluid = level.getFluidState(cursor);

                    if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN)
                            || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.LAVA)
                            || fluid.is(FluidTags.LAVA)) {
                        anchorPos = new BlockPos.MutableBlockPos(cursor.getX(), cursor.getY(), cursor.getZ());
                        break searchLoop;
                    }
                }
            }
        }

        // If no obsidian or lava is nearby, do not generate
        if (anchorPos == null) {
            return false;
        }

        // Generate a vein of 2 to 5 Volcanite Ore blocks directly adjacent to / embedded in the thermal anchor
        int veinSize = 2 + random.nextInt(4); // 2..5 ores
        boolean placedAny = false;
        BlockPos.MutableBlockPos placePos = new BlockPos.MutableBlockPos();

        for (int attempt = 0; attempt < veinSize * 4; attempt++) {
            int ox = anchorPos.getX() + random.nextInt(3) - random.nextInt(3);
            int oy = anchorPos.getY() + random.nextInt(2) - random.nextInt(2);
            int oz = anchorPos.getZ() + random.nextInt(3) - random.nextInt(3);
            placePos.set(ox, oy, oz);

            BlockState target = level.getBlockState(placePos);
            if (isReplaceable(target)) {
                level.setBlock(placePos, ObsidiumMod.VOLCANITE_ORE.defaultBlockState(), 2);
                placedAny = true;
                veinSize--;
                if (veinSize <= 0) break;
            }
        }

        return placedAny;
    }

    private static boolean isReplaceable(BlockState state) {
        return state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                || state.is(BlockTags.STONE_ORE_REPLACEABLES)
                || state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.CRYING_OBSIDIAN)
                || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.BASALT)
                || state.is(Blocks.SMOOTH_BASALT)
                || state.is(Blocks.BLACKSTONE)
                || state.is(Blocks.NETHERRACK);
    }
}

