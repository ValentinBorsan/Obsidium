package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.QuantumLinkModuleItem;
import net.valentin.obsidium.item.VolcaniteWrenchItem;
import org.jetbrains.annotations.Nullable;

public class VolcaniteModularChestBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LINKED = BooleanProperty.create("linked");

    public VolcaniteModularChestBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LINKED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LINKED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(LINKED, false);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VolcaniteModularChestBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : (lvl, p, st, be) -> {
            if (be instanceof VolcaniteModularChestBlockEntity chest) {
                VolcaniteModularChestBlockEntity.tick(lvl, p, st, chest);
            }
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        // Shift + Right-click with Volcanite Wrench: Dismantle chest while keeping all items & modules inside!
        if (player != null && player.isShiftKeyDown() && VolcaniteWrenchItem.isWrench(stack)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VolcaniteModularChestBlockEntity chest) {
                if (!level.isClientSide()) {
                    ItemStack drop = new ItemStack(ObsidiumMod.VOLCANITE_MODULAR_CHEST_ITEM);
                    chest.saveToItem(drop, level.registryAccess());
                    chest.clearLocalContents();
                    level.removeBlock(pos, false);
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
                    level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.BLOCKS, 0.9F, 1.1F);
                    player.displayClientMessage(Component.literal("§6[Modular Chest] §aDismantled with contents & modules preserved!"), true);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        // Right-click with Dye when Quantum Linked: change channel color
        if (player != null && stack.getItem() instanceof DyeItem dyeItem) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VolcaniteModularChestBlockEntity chest && chest.hasQuantumLinkModule()) {
                if (!level.isClientSide()) {
                    chest.ensureQuantumOwner(player.getUUID());
                    chest.setQuantumColor(dyeItem.getDyeColor());
                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }
                    level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                    player.displayClientMessage(Component.literal("§d[Quantum Chest] §fColor set to §e" + QuantumLinkModuleItem.getColorDisplayName(dyeItem.getDyeColor()) + " §7(Ch #" + chest.getQuantumChannel() + ")"), true);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof VolcaniteModularChestBlockEntity chest) {
                chest.ensureQuantumOwner(player.getUUID());
            }
            if (blockEntity instanceof net.minecraft.world.MenuProvider menuProvider) {
                player.openMenu(menuProvider);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof VolcaniteModularChestBlockEntity chest) {
                chest.dropLocalContents(level, pos);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
