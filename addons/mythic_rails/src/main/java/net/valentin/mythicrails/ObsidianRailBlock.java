package net.valentin.mythicrails;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.RailBlock;
public final class ObsidianRailBlock extends RailBlock {
    public static final MapCodec<RailBlock> CODEC = simpleCodec(ObsidianRailBlock::new);
    public ObsidianRailBlock(Properties properties) { super(properties); }
    @Override public MapCodec<RailBlock> codec() { return CODEC; }
}
