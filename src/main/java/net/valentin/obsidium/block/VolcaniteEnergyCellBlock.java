package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.VolcaniteEnergyCellBlockItem;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VolcaniteEnergyCellBlock extends Block implements EntityBlock {
    public VolcaniteEnergyCellBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VolcaniteEnergyCellBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ObsidiumMod.VOLCANITE_ENERGY_CELL_BE ?
                (BlockEntityTicker<T>) (BlockEntityTicker<VolcaniteEnergyCellBlockEntity>) VolcaniteEnergyCellBlockEntity::tick : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof VolcaniteEnergyCellBlockEntity cell) {
            cell.loadFromItemStack(stack);
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof VolcaniteEnergyCellBlockEntity cell) {
            for (ItemStack drop : drops) {
                if (drop.getItem() instanceof VolcaniteEnergyCellBlockItem) {
                    cell.saveToItemStack(drop);
                }
            }
        }
        return drops;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VolcaniteEnergyCellBlockEntity cell) {
                Containers.dropContents(level, pos, cell.inventory);
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            VolcaniteEnergyCoreBlockEntity formedCore = VolcaniteEnergyCoreBlockEntity.findFormedCoreForRingBlock(level, pos);
            if (formedCore != null) {
                player.openMenu(formedCore);
                return InteractionResult.SUCCESS;
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MenuProvider provider) {
                player.openMenu(provider);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        VolcaniteEnergyCoreBlockEntity formedCore = VolcaniteEnergyCoreBlockEntity.findFormedCoreForRingBlock(level, pos);
        if (formedCore != null) {
            long amt = formedCore.energyStorage.amount;
            long cap = formedCore.energyStorage.getCapacity();
            if (cap <= 0 || amt <= 0) return 0;
            return (int) Math.floor((double) amt / cap * 14.0) + 1;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof VolcaniteEnergyCellBlockEntity cell) {
            long amt = cell.energyStorage.amount;
            long cap = cell.getEffectiveCapacity();
            if (cap <= 0 || amt <= 0) return 0;
            return (int) Math.floor((double) amt / cap * 14.0) + 1;
        }
        return 0;
    }
}
