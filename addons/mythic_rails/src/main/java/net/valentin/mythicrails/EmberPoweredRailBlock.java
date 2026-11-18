package net.valentin.mythicrails;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.PoweredRailBlock;
public final class EmberPoweredRailBlock extends PoweredRailBlock {
    public static final MapCodec<PoweredRailBlock> CODEC = simpleCodec(EmberPoweredRailBlock::new);
    public EmberPoweredRailBlock(Properties properties) { super(properties); }
    @Override public MapCodec<PoweredRailBlock> codec() { return CODEC; }
}
