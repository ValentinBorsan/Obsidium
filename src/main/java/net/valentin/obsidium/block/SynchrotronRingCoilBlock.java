package net.valentin.obsidium.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Superconducting magnetic coil block forming the 5x5 horizontal ring
 * of the Olympian Hadron Collider multiblock reactor.
 */
public class SynchrotronRingCoilBlock extends Block {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public SynchrotronRingCoilBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal("⚛ Superconducting Synchrotron Magnet Coil").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Arrange 12 Coils + 1 Energy Port + 1 Item Port + 1 Fluid Port").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("in a 5×5 hollow ring with 1 Olympian Hadron Collider at a side midpoint.").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Locks into an energized superconducting state when formed.").withStyle(ChatFormatting.GREEN));
    }

    /** Finds an Olympian Hadron Collider controller whose 5x5 ring perimeter includes this coil position. */
    @Nullable
    public static OlympianHadronColliderBlockEntity findController(Level level, BlockPos coilPos) {
        if (level == null) return null;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos candidate = coilPos.offset(dx, 0, dz);
                if (level.getBlockEntity(candidate) instanceof OlympianHadronColliderBlockEntity controller) {
                    if (controller.isPosOnRing(coilPos)) {
                        return controller;
                    }
                }
            }
        }
        return null;
    }

    private InteractionResult openControllerOrNotify(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide()) {
            OlympianHadronColliderBlockEntity controller = findController(level, pos);
            if (controller != null && controller.isFormed()) {
                player.openMenu(controller);
            } else {
                player.displayClientMessage(
                        Component.literal("⚛ 5×5 Collider Ring requires: 1 Controller + 12 Synchrotron Coils + 1 Energy Port + 1 Item Port + 1 Fluid Port.")
                                .withStyle(ChatFormatting.AQUA),
                        true
                );
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            OlympianHadronColliderBlockEntity controller = findController(level, pos);
            if (controller != null && controller.isFormed()) {
                player.openMenu(controller);
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return openControllerOrNotify(level, pos, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            OlympianHadronColliderBlockEntity controller = findController(level, pos);
            if (controller != null) {
                controller.evaluateMultiblock(level);
            }
        }
    }
}

