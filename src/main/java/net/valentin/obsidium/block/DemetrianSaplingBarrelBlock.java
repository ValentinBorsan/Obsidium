package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class DemetrianSaplingBarrelBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public DemetrianSaplingBarrelBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DemetrianSaplingBarrelBlockEntity(pos, state);
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DemetrianSaplingBarrelBlockEntity barrelBe) {
                boolean fullStack = player.isShiftKeyDown();
                int countToTake = fullStack ? 64 : 1;
                // Extract from the first non-empty chamber
                for (int i = 0; i < DemetrianSaplingBarrelBlockEntity.CHAMBER_COUNT; i++) {
                    if (barrelBe.extractSapling(i, countToTake, player)) {
                        break;
                    }
                }
            }
        }
        super.attack(state, level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof DemetrianSaplingBarrelBlockEntity barrelBe)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // 1. Direct sapling deposit with right-click (when not sneaking)
        if (!player.isShiftKeyDown() && DemetrianSaplingBarrelBlockEntity.isSapling(stack)) {
            if (!level.isClientSide()) {
                barrelBe.insertSapling(stack, player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        // 2. Open GUI (when clicking with empty hand, or shift-clicking with sapling, or any non-sapling item)
        if (!level.isClientSide()) {
            player.openMenu(barrelBe);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DemetrianSaplingBarrelBlockEntity barrelBe) {
                barrelBe.dropAllContents();
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }
}

