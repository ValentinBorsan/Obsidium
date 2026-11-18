package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;

public class HermesItemPipeBlock extends VolcaniteStoragePipeBlock {
    private final int tier;
    private final int maxItemsPerTransfer;
    private final int tickInterval;

    public HermesItemPipeBlock(Properties properties, int tier, int maxItemsPerTransfer, int tickInterval) {
        super(properties);
        this.tier = tier;
        this.maxItemsPerTransfer = maxItemsPerTransfer;
        this.tickInterval = tickInterval;
    }

    public HermesItemPipeBlock(Properties properties) {
        this(properties, 1, 8, 5);
    }

    public int getTier() { return tier; }
    public int getMaxItemsPerTransfer() { return maxItemsPerTransfer; }
    public int getTickInterval() { return tickInterval; }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        String tierName = switch (tier) {
            case 2 -> "§b★ Advanced Tier";
            case 3 -> "§d★ Supreme Tier";
            default -> "§e★ Basic Tier";
        };
        tooltip.add(net.minecraft.network.chat.Component.literal(tierName));
        tooltip.add(net.minecraft.network.chat.Component.literal("§7Transfer: §a" + maxItemsPerTransfer + " items / " + tickInterval + "t"));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HermesItemPipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ObsidiumMod.HERMES_ITEM_PIPE_BE ?
                (BlockEntityTicker<T>) (BlockEntityTicker<HermesItemPipeBlockEntity>) HermesItemPipeBlockEntity::tick : null;
    }
}
