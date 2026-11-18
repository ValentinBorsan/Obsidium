package net.valentin.mythicrails;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DetectorRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Detector rail that outputs comparator signal proportional to Hermes Freight Cart fill (items/fluid/RF). */
public final class ObsidianDetectorRailBlock extends DetectorRailBlock {
    public static final MapCodec<DetectorRailBlock> CODEC = simpleCodec(ObsidianDetectorRailBlock::new);

    public ObsidianDetectorRailBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<DetectorRailBlock> codec() {
        return CODEC;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (state.getValue(POWERED)) {
            AABB box = new AABB(
                    pos.getX() + 0.125, pos.getY(), pos.getZ() + 0.125,
                    pos.getX() + 0.875, pos.getY() + 0.875, pos.getZ() + 0.875);
            var carts = level.getEntitiesOfClass(HermesFreightCart.class, box, EntitySelector.ENTITY_STILL_ALIVE);
            if (!carts.isEmpty()) {
                return computeCartFillSignal(carts.getFirst());
            }
        }
        return super.getAnalogOutputSignal(state, level, pos);
    }

    private static int computeCartFillSignal(HermesFreightCart cart) {
        var payload = cart.payloadType();
        double ratio;
        if (payload == CartModuleItem.PayloadType.LIQUID_BARREL) {
            ratio = cart.fluidAmount() / (double) HermesFreightCart.MAX_FLUID;
        } else if (payload == CartModuleItem.PayloadType.ENERGY_CELL
                || (cart.motorType() == CartModuleItem.MotorType.ELECTRIC && payload == CartModuleItem.PayloadType.NONE)) {
            ratio = cart.maxEnergy() > 0 ? cart.energy() / (double) cart.maxEnergy() : 0.0;
        } else {
            int filled = 0;
            int total = 54;
            for (int i = 0; i < total; i++) {
                var stack = cart.getItem(i);
                if (!stack.isEmpty()) {
                    filled += Math.round(stack.getCount() * 100.0F / Math.max(1, stack.getMaxStackSize()));
                }
            }
            ratio = filled / (total * 100.0);
        }
        if (ratio <= 0.0) return 0;
        return Math.clamp((int) Math.ceil(ratio * 15.0), 1, 15);
    }
}
