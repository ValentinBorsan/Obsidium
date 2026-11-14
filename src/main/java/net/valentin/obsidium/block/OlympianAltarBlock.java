package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.HephaesteanResonatorItem;
import org.jetbrains.annotations.Nullable;

public class OlympianAltarBlock extends Block implements EntityBlock {
    protected static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);

    public OlympianAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OlympianAltarBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ObsidiumMod.OLYMPIAN_ALTAR_BE ?
                (lvl, p, st, be) -> {
                    if (be instanceof OlympianAltarBlockEntity altar) {
                        OlympianAltarBlockEntity.tick(lvl, p, st, altar);
                    }
                } : null;
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof OlympianAltarBlockEntity altar) {
            ItemStack placed = altar.getPlacedItem();
            if (!placed.isEmpty() && !altar.isRitualRunning()) {
                if (!level.isClientSide()) {
                    ItemStack taken = altar.removePlacedItem();
                    if (!player.getInventory().add(taken)) {
                        player.drop(taken, false);
                    }
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.5F);
                }
                return;
            }
        }
        super.attack(state, level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof OlympianAltarBlockEntity altar) {
            ItemStack placed = altar.getPlacedItem();

            // 1. Activating ritual via Hephaestean Resonator
            if (stack.getItem() instanceof HephaesteanResonatorItem) {
                if (!level.isClientSide()) {
                    altar.tryStartRitual(player);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }

            // 2. Insert weapon / tool / armor into altar
            if (placed.isEmpty() && !stack.isEmpty()) {
                if (!level.isClientSide()) {
                    altar.setPlacedItem(stack.split(1));
                    level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.BLOCKS, 0.9F, 1.1F);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }

            // 3. Shift + Click with empty hand: retrieve item from altar
            if (player.isShiftKeyDown() && stack.isEmpty() && !placed.isEmpty()) {
                if (!altar.isRitualRunning()) {
                    if (!level.isClientSide()) {
                        ItemStack taken = altar.removePlacedItem();
                        if (!player.getInventory().add(taken)) {
                            player.drop(taken, false);
                        }
                        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.5F);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }

            // 4. Normal click with empty hand: start ritual
            if (stack.isEmpty()) {
                if (!level.isClientSide()) {
                    altar.tryStartRitual(player);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof OlympianAltarBlockEntity altar) {
            ItemStack placed = altar.getPlacedItem();
            if (player.isShiftKeyDown() && !placed.isEmpty()) {
                if (!altar.isRitualRunning()) {
                    if (!level.isClientSide()) {
                        ItemStack taken = altar.removePlacedItem();
                        if (!player.getInventory().add(taken)) {
                            player.drop(taken, false);
                        }
                        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.5F);
                    }
                    return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());
                }
            }
            if (!level.isClientSide()) {
                altar.tryStartRitual(player);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof OlympianAltarBlockEntity altar && !altar.getPlacedItem().isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, altar.getPlacedItem());
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}

