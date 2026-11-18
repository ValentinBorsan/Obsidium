package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.valentin.obsidium.ObsidiumMod;

public class MineralTreeLeavesBlock extends LeavesBlock {
    public static final BooleanProperty FRUITING = BooleanProperty.create("fruiting");
    private final String species;

    public MineralTreeLeavesBlock(String species, Properties properties) {
        super(properties);
        this.species = species;
        registerDefaultState(defaultBlockState().setValue(FRUITING, false));
    }

    public String species() {
        return species;
    }

    public Item fruitItem() {
        return switch (species) {
            case "midas_gold" -> ObsidiumMod.MIDAS_GOLD_FRUIT;
            case "ares_iron" -> ObsidiumMod.ARES_IRON_FRUIT;
            case "hermes_copper" -> ObsidiumMod.HERMES_COPPER_FRUIT;
            case "hephaestus_volcanite", "pyretic_titan" -> ObsidiumMod.HEPHAESTUS_VOLCANITE_FRUIT;
            case "electrum" -> ObsidiumMod.ELECTRUM_FRUIT;
            case "tartarean_netherite" -> ObsidiumMod.TARTAREAN_NETHERITE_FRUIT;
            case "cronus_lead" -> ObsidiumMod.CRONUS_LEAD_FRUIT;
            case "zeus_tin" -> ObsidiumMod.ZEUS_TIN_FRUIT;
            case "artemis_silver" -> ObsidiumMod.ARTEMIS_SILVER_FRUIT;
            default -> null;
        };
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        BlockState current = level.getBlockState(pos);
        if (current.is(this) && !current.getValue(FRUITING)
                && current.getValue(DISTANCE) <= 6 && random.nextInt(800 *
                        (("tartarean_netherite".equals(species()) || "adamantine_diamond".equals(species()) || "pyretic_titan".equals(species()))
                                ? net.valentin.obsidium.config.ModConfig.get().getRareResourceGrowthMultiplier() : 1)) == 0) {
            level.setBlock(pos, current.setValue(FRUITING, true), 2);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FRUITING);
    }
}
