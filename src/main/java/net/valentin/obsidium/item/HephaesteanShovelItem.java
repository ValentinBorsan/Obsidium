package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.util.ToolMiningHelper;
import team.reborn.energy.api.base.SimpleEnergyItem;

import java.util.List;

public class HephaesteanShovelItem extends ShovelItem implements SimpleEnergyItem {

    public static final long CAPACITY = 1_000_000L;
    public static final long MAX_INPUT = 50_000L;
    public static final long ENERGY_PER_BLOCK = 100L;

    public HephaesteanShovelItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) { return CAPACITY; }

    @Override
    public long getEnergyMaxInput(ItemStack stack) { return MAX_INPUT; }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) { return 0; }

    public static int getExcavationMode(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getInt("ShovelMode");
    }

    public static void cycleExcavationMode(ItemStack stack) {
        int next = (getExcavationMode(stack) + 1) % 3;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("ShovelMode", next));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            cycleExcavationMode(stack);
            if (!level.isClientSide()) {
                int mode = getExcavationMode(stack);
                String modeName = switch (mode) {
                    case 0 -> "§e1x1 Precision";
                    case 1 -> "§a3x3 Flat Excavation";
                    case 2 -> "§d5x5 Continental Shaper";
                    default -> "1x1";
                };
                player.displayClientMessage(Component.literal("§6[Hephaestean Shovel] §bMode: " + modeName), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6F, 1.3F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return super.use(level, player, hand);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (getStoredEnergy(stack) < ENERGY_PER_BLOCK) return 1.0F;
        return super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        stack.setDamageValue(0);

        if (getStoredEnergy(stack) >= ENERGY_PER_BLOCK) {
            tryUseEnergy(stack, ENERGY_PER_BLOCK);

            if (!level.isClientSide() && miner instanceof ServerPlayer player && level instanceof ServerLevel serverLevel) {
                int mode = getExcavationMode(stack);
                if (mode == 1) { // 3x3
                    List<BlockPos> surrounding = ToolMiningHelper.getSurroundingBlocks(pos, player, 1, 1, 1);
                    int broken = ToolMiningHelper.breakBlocksInArea(serverLevel, player, stack, surrounding, false);
                    tryUseEnergy(stack, broken * ENERGY_PER_BLOCK);
                } else if (mode == 2) { // 5x5
                    List<BlockPos> vast = ToolMiningHelper.getSurroundingBlocks(pos, player, 2, 2, 1);
                    int broken = ToolMiningHelper.breakBlocksInArea(serverLevel, player, stack, vast, false);
                    tryUseEnergy(stack, broken * ENERGY_PER_BLOCK);
                }
            }
        }
        return super.mineBlock(stack, level, state, pos, miner);
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
        int mode = getExcavationMode(stack);
        String modeName = switch (mode) {
            case 0 -> "§e1x1 Precision";
            case 1 -> "§a3x3 Flat Excavation";
            case 2 -> "§d5x5 Continental Shaper";
            default -> "1x1";
        };
        tooltip.add(Component.literal("§6Excavation Mode: " + modeName).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§8[Shift + Right-Click: Cycle Mode]").withStyle(ChatFormatting.DARK_GRAY));
    }
}

