package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import team.reborn.energy.api.base.SimpleEnergyItem;

import java.util.List;

public class HephaesteanHoeItem extends HoeItem implements SimpleEnergyItem {

    public static final long CAPACITY = 1_000_000L;
    public static final long MAX_INPUT = 50_000L;
    public static final long ENERGY_PER_TILL = 200L;

    public HephaesteanHoeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) { return CAPACITY; }

    @Override
    public long getEnergyMaxInput(ItemStack stack) { return MAX_INPUT; }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) { return 0; }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (getStoredEnergy(stack) >= ENERGY_PER_TILL) {
            int tilled = 0;
            // 5x5 tilling and instant hydration
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos target = pos.offset(x, 0, z);
                    BlockState state = level.getBlockState(target);
                    if ((state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)) && level.getBlockState(target.above()).isAir()) {
                        if (!level.isClientSide()) {
                            level.setBlock(target, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 3);
                        }
                        tilled++;
                    }
                }
            }

            if (tilled > 0) {
                tryUseEnergy(stack, tilled * 50L);
                level.playSound(player, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        return super.useOn(context);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) { return true; }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round((double) getStoredEnergy(stack) * 13.0 / (double) CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) { return 0x00E5FF; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        long stored = getStoredEnergy(stack);
        tooltip.add(Component.literal("§b⚡ Energy: §f" + String.format("%,d", stored) + " §b/ §f" + String.format("%,d", CAPACITY) + " RF").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("§6Divine Agriculture: §7Tills and hydrates a 5x5 area.").withStyle(ChatFormatting.GOLD));
    }
}

