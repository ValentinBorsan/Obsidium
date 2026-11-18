package net.valentin.mythicrails;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Compact transceiver mounted next to a Semaphore, Bumper, or Switch Rail for remote control. */
public final class SignalConnectorBlock extends BaseEntityBlock {
    public static final MapCodec<SignalConnectorBlock> CODEC = simpleCodec(SignalConnectorBlock::new);
    public static final BooleanProperty LINKED = BooleanProperty.create("linked");
    private static final VoxelShape SHAPE = box(3, 0, 3, 13, 8, 13);

    public SignalConnectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LINKED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LINKED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SignalConnectorBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SignalConnectorBlockEntity be) {
            int kind = be.targetKind();
            String targetName = switch (kind) {
                case 1 -> "Semaphore";
                case 2 -> "Rail Bumper";
                case 3 -> "Switch Rail";
                default -> "None (place next to Semaphore, Bumper, or Switch Rail)";
            };
            String linked = be.controlPos() != null
                    ? "Linked to (" + be.controlPos().toShortString() + ")"
                    : "Not linked (use Signal Linker)";
            player.displayClientMessage(Component.literal("Signal Connector [" + targetName + "] - " + linked), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static SignalConnectorBlockEntity findAdjacentConnector(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(pos.relative(dir)) instanceof SignalConnectorBlockEntity be) {
                return be;
            }
        }
        return null;
    }
}
