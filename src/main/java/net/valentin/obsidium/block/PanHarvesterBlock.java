package net.valentin.obsidium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Harvests only surplus adult livestock above a configurable minimum per species (9x4x9). */
public final class PanHarvesterBlock extends BaseEntityBlock {
    public static final MapCodec<PanHarvesterBlock> CODEC = simpleCodec(PanHarvesterBlock::new);

    public PanHarvesterBlock(Properties properties) {
        super(properties);
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PanHarvesterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ObsidiumMod.PAN_HARVESTER_BE, PanHarvesterBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PanHarvesterBlockEntity be) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new PanHarvesterMenu(id, inv, be, be.dataAccess(), be),
                    Component.translatable("block.obsidium.pan_pastoral_harvester")));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof PanHarvesterBlockEntity be) {
                Containers.dropContents(level, pos, be);
                be.dropPendingContents();
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.obsidium.pan_pastoral_harvester.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.obsidium.pan_pastoral_harvester.2").withStyle(ChatFormatting.GOLD));
    }
}
