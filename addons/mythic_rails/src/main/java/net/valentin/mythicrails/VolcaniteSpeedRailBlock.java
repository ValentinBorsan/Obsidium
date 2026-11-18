package net.valentin.mythicrails;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.RailBlock;

/** High-speed Volcanite-reinforced rail for long-distance rail lines. */
public final class VolcaniteSpeedRailBlock extends RailBlock {
    public static final MapCodec<RailBlock> CODEC = simpleCodec(VolcaniteSpeedRailBlock::new);

    public VolcaniteSpeedRailBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<RailBlock> codec() {
        return CODEC;
    }
}
