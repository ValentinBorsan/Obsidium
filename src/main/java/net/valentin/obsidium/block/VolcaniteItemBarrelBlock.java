package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.valentin.obsidium.item.VolcaniteBarrelUpgradeItem;
import net.valentin.obsidium.item.VolcaniteWrenchItem;
import org.jetbrains.annotations.Nullable;

public class VolcaniteItemBarrelBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final VolcaniteItemBarrelBlockEntity.BarrelTier tier;

    public VolcaniteItemBarrelBlock(Properties properties, VolcaniteItemBarrelBlockEntity.BarrelTier tier) {
        super(properties);
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public VolcaniteItemBarrelBlockEntity.BarrelTier getTier() {
        return tier;
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
        return new VolcaniteItemBarrelBlockEntity(pos, state, tier);
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VolcaniteItemBarrelBlockEntity barrelBe) {
                boolean fullStack = player.isShiftKeyDown();
                barrelBe.extractItem(player, fullStack);
            }
        }
        super.attack(state, level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof VolcaniteItemBarrelBlockEntity barrelBe)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // 1. Wrench: Toggle Lock Mode
        if (VolcaniteWrenchItem.isWrench(stack)) {
            if (!level.isClientSide()) {
                barrelBe.toggleLock(player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        // 2. Barrel Upgrade installation
        if (stack.getItem() instanceof VolcaniteBarrelUpgradeItem) {
            if (!level.isClientSide()) {
                barrelBe.installUpgrade(stack, player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        // 3. Shift + Right-click: Open Menu / GUI
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                player.openMenu(barrelBe);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        // 4. Deposit this stack and every matching stack in the player's inventory.
        if (!stack.isEmpty()) {
            if (!level.isClientSide()) {
                barrelBe.depositStack(stack, true, player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof VolcaniteItemBarrelBlockEntity barrelBe) {
            // Open GUI on sneak-click or bare hand click
            if (player.isShiftKeyDown()) {
                if (!level.isClientSide()) {
                    player.openMenu(barrelBe);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }

            if (!level.isClientSide()) {
                if (barrelBe.getStoredVariant().isBlank()) {
                    player.displayClientMessage(Component.literal("§6[Barrel] §7Empty barrel " + (barrelBe.isLocked() ? "§c[LOCKED]" : "§8[UNLOCKED]")), true);
                } else {
                    String itemName = barrelBe.getStoredItemSample().getHoverName().getString();
                    String formatted = String.format("%,d", barrelBe.getStoredAmount());
                    String maxFormatted = String.format("%,d", barrelBe.getMaxCapacity());
                    String lockStr = barrelBe.isLocked() ? " §a🔒" : "";
                    String voidStr = barrelBe.hasVoidUpgrade() ? " §c[VOID]" : "";
                    player.displayClientMessage(Component.literal("§6[Barrel] §f" + itemName + ": §e" + formatted + " §7/ §6" + maxFormatted + lockStr + voidStr), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VolcaniteItemBarrelBlockEntity barrelBe) {
                // Drop installed upgrades
                for (int i = 0; i < barrelBe.upgrades.getContainerSize(); i++) {
                    ItemStack u = barrelBe.upgrades.getItem(i);
                    if (!u.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), u);
                    }
                }

                // Drop stored items in sensible chunks if destroyed
                long amount = barrelBe.getStoredAmount();
                if (!barrelBe.getStoredVariant().isBlank() && amount > 0) {
                    while (amount > 0) {
                        int count = (int) Math.min(amount, 64);
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), barrelBe.getStoredVariant().toStack(count));
                        amount -= count;
                    }
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}

