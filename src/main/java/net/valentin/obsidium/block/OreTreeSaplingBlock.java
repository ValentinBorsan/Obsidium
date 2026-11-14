package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.Optional;

public class OreTreeSaplingBlock extends SaplingBlock {
    private final String species;

    public OreTreeSaplingBlock(String treeName, String configuredFeature, Properties properties) {
        this(treeName, null, configuredFeature, properties);
    }

    public OreTreeSaplingBlock(String treeName, String megaConfiguredFeature, String regularConfiguredFeature, Properties properties) {
        super(createGrower(treeName, megaConfiguredFeature, regularConfiguredFeature), properties);
        this.species = treeName;
    }

    public String species() {
        return species;
    }

    private static TreeGrower createGrower(String treeName, String megaConfiguredFeature, String regularConfiguredFeature) {
        Optional<ResourceKey<ConfiguredFeature<?, ?>>> mega = megaConfiguredFeature != null
                ? Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath("obsidium", megaConfiguredFeature)))
                : Optional.empty();
        Optional<ResourceKey<ConfiguredFeature<?, ?>>> regular = regularConfiguredFeature != null
                ? Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath("obsidium", regularConfiguredFeature)))
                : Optional.empty();
        return new TreeGrower(treeName, mega, regular, Optional.empty());
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.BASE_STONE_NETHER) || super.mayPlaceOn(state, level, pos);
    }
}
