package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;

public class VolcaniteEnergyUnitBlockEntity extends VolcaniteEnergyCellBlockEntity {
    public VolcaniteEnergyUnitBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.VOLCANITE_ENERGY_UNIT_BE, pos, state, 15_000_000L, 150_000L, 1_000_000L, true);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VolcaniteEnergyUnitBlockEntity entity) {
        VolcaniteEnergyCellBlockEntity.tick(level, pos, state, entity);
    }
}
