package net.valentin.obsidium.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class StygianCenotaphBlock extends Block implements EntityBlock {

    // A nice altar/tomb shape (slightly smaller than full block, like a sacred sarcophagus)
    protected static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);

    public StygianCenotaphBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StygianCenotaphBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof StygianCenotaphBlockEntity cenotaph) {
                cenotaph.tryOpen(player);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof StygianCenotaphBlockEntity cenotaph) {
                boolean isCreative = player.isCreative();
                boolean isAdmin = player.hasPermissions(2);

                if (!isCreative && !isAdmin) {
                    // Normal players cannot break it with a pickaxe; they must right-click with the key to open it
                    player.sendSystemMessage(Component.literal("§c[Stygian Cenotaph] This cenotaph cannot be broken manually! Use your Stygian Key (right-click) to reclaim your soul.").withStyle(ChatFormatting.RED));
                    return state;
                } else {
                    cenotaph.tryOpen(player);
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}

