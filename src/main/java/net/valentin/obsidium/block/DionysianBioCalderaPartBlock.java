package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.valentin.obsidium.ObsidiumMod;

public class DionysianBioCalderaPartBlock extends Block {
    public static final BooleanProperty UPPER = BooleanProperty.create("upper");
    public static final IntegerProperty POS_X = IntegerProperty.create("pos_x", 0, 1);
    public static final IntegerProperty POS_Z = IntegerProperty.create("pos_z", 0, 1);

    public DionysianBioCalderaPartBlock(Properties properties) {
        super(properties.noLootTable());
        registerDefaultState(defaultBlockState()
                .setValue(UPPER, false)
                .setValue(POS_X, 0)
                .setValue(POS_Z, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UPPER, POS_X, POS_Z);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public static BlockPos findControllerPos(Level level, BlockPos partPos) {
        for (int dx = -1; dx <= 0; dx++) {
            for (int dy = -1; dy <= 0; dy++) {
                for (int dz = -1; dz <= 0; dz++) {
                    BlockPos candidate = partPos.offset(dx, dy, dz);
                    if (level.getBlockState(candidate).is(ObsidiumMod.DIONYSIAN_BIO_CALDERA)
                            && level.getBlockEntity(candidate) instanceof DionysianBioCalderaBlockEntity) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    public static DionysianBioCalderaBlockEntity findController(Level level, BlockPos partPos) {
        BlockPos controllerPos = findControllerPos(level, partPos);
        if (controllerPos == null) return null;
        return level.getBlockEntity(controllerPos) instanceof DionysianBioCalderaBlockEntity controller ? controller : null;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (state.getValue(UPPER) && random.nextDouble() < 0.35) {
            DionysianBioCalderaBlockEntity controller = findController(level, pos);
            if (controller != null && controller.isFermenting()) {
                double px = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
                double py = pos.getY() + 1.05;
                double pz = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
                level.addParticle(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, px, py, pz, 0.0, 0.02, 0.0);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            DionysianBioCalderaBlockEntity controller = findController(level, pos);
            if (controller != null) player.openMenu(controller);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            BlockPos controllerPos = findControllerPos(level, pos);
            if (controllerPos != null && level.getBlockState(controllerPos).getBlock() instanceof DionysianBioCalderaBlock controller) {
                controller.dismantleFromPart(level, controllerPos, pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        DionysianBioCalderaBlockEntity controller = findController(level, pos);
        if (controller != null) {
            long amt = controller.energyStorage.amount;
            long cap = controller.energyStorage.capacity;
            if (cap <= 0 || amt <= 0) return 0;
            return (int) Math.floor((double) amt / cap * 14.0) + 1;
        }
        return 0;
    }
}

