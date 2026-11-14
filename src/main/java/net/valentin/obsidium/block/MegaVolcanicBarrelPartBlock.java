package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.valentin.obsidium.ObsidiumMod;

public class MegaVolcanicBarrelPartBlock extends Block {
    public static final BooleanProperty UPPER = BooleanProperty.create("upper");
    public static final IntegerProperty POS_X = IntegerProperty.create("pos_x", 0, 1);
    public static final IntegerProperty POS_Z = IntegerProperty.create("pos_z", 0, 1);

    public MegaVolcanicBarrelPartBlock(Properties properties) {
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
                    if (level.getBlockState(candidate).is(ObsidiumMod.MEGA_VOLCANIC_BARREL)
                            && level.getBlockEntity(candidate) instanceof MegaVolcanicBarrelBlockEntity) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    public static MegaVolcanicBarrelBlockEntity findController(Level level, BlockPos partPos) {
        BlockPos controllerPos = findControllerPos(level, partPos);
        if (controllerPos == null) return null;
        return level.getBlockEntity(controllerPos) instanceof MegaVolcanicBarrelBlockEntity controller ? controller : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            MegaVolcanicBarrelBlockEntity controller = findController(level, pos);
            if (controller != null) player.openMenu(controller);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        BlockPos controllerPos = findControllerPos(level, pos);
        if (controllerPos == null || !(level.getBlockState(controllerPos).getBlock() instanceof MegaVolcanicBarrelBlock controller)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        BlockHitResult redirectedHit = new BlockHitResult(hitResult.getLocation(), hitResult.getDirection(), controllerPos,
                hitResult.isInside());
        return controller.interactFromPart(stack, level.getBlockState(controllerPos), level, controllerPos, player, hand, redirectedHit);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        MegaVolcanicBarrelBlockEntity controller = findController(level, pos);
        return controller == null ? 0 : (int) Math.floor((double) controller.getFluidAmount() / controller.getMaxCapacity() * 15.0);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            BlockPos controllerPos = findControllerPos(level, pos);
            if (controllerPos != null && level.getBlockState(controllerPos).getBlock() instanceof MegaVolcanicBarrelBlock controller) {
                controller.dismantleFromPart(level, controllerPos, pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
