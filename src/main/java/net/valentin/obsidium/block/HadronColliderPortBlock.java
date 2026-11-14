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

import java.util.List;

/**
 * Dedicated multiblock I/O port block (Energy, Item, or Fluid) for the 5x5 Olympian Hadron Collider ring.
 * Visually locks into an energized state when the multiblock forms.
 */
public class HadronColliderPortBlock extends Block {
    public enum PortType { ENERGY, ITEM, FLUID }

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    private final PortType portType;

    public HadronColliderPortBlock(Properties properties, PortType portType) {
        super(properties);
        this.portType = portType;
        this.registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
    }

    public PortType getPortType() {
        return this.portType;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        switch (this.portType) {
            case ENERGY -> {
                tooltip.add(Component.literal("⚡ Hadron Energy Port (HV RF Output)").withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.literal("Extracts up to 5M RF/t into Wires, Conduits, or Energy Units.").withStyle(ChatFormatting.YELLOW));
            }
            case ITEM -> {
                tooltip.add(Component.literal("📦 Hadron Item Port (Beam Input & Auto-Output)").withStyle(ChatFormatting.GREEN));
                tooltip.add(Component.literal("Accepts Beam A/B, Fuel, Cryo items & auto-outputs into Modular Chests / Pipes.").withStyle(ChatFormatting.YELLOW));
            }
            case FLUID -> {
                tooltip.add(Component.literal("💧 Hadron Fluid Port (Fuel & Cryo Coolant I/O)").withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.literal("Pumps Pyro-Diesel / Biodiesel / Distillate & Cryo Coolants directly into the ring.").withStyle(ChatFormatting.YELLOW));
            }
        }
        tooltip.add(Component.literal("Required: 1× Energy Port, 1× Item Port & 1× Fluid Port on the 5×5 ring.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Lights up & locks when the 5×5 Collider multiblock is formed.").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private InteractionResult openControllerOrNotify(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide()) {
            OlympianHadronColliderBlockEntity controller = SynchrotronRingCoilBlock.findController(level, pos);
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
        if (player.isShiftKeyDown() && !stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide()) {
            OlympianHadronColliderBlockEntity controller = SynchrotronRingCoilBlock.findController(level, pos);
            if (controller != null && controller.isFormed()) {
                player.openMenu(controller);
                return ItemInteractionResult.SUCCESS;
            }
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
            OlympianHadronColliderBlockEntity controller = SynchrotronRingCoilBlock.findController(level, pos);
            if (controller != null) {
                controller.evaluateMultiblock(level);
            }
        }
    }
}
