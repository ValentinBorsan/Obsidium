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
import net.minecraft.world.entity.EquipmentSlot;
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

import java.util.List;

public class TyphonicShovelItem extends ShovelItem {

    public TyphonicShovelItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    public static boolean is3x3Mode(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getBoolean("Mode3x3");
    }

    public static void set3x3Mode(ItemStack stack, boolean enabled) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean("Mode3x3", enabled));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            boolean current = is3x3Mode(stack);
            set3x3Mode(stack, !current);
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.literal("§6[Typhonic Shovel] §eExcavation Mode: " + (!current ? "§a3x3 Flat" : "§c1x1 Normal")), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, !current ? 1.4F : 0.9F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return super.use(level, player, hand);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!level.isClientSide() && miner instanceof ServerPlayer player && is3x3Mode(stack) && level instanceof ServerLevel serverLevel) {
            List<BlockPos> surrounding = ToolMiningHelper.getSurroundingBlocks(pos, player, 1, 1, 1);
            int broken = ToolMiningHelper.breakBlocksInArea(serverLevel, player, stack, surrounding, false);
            if (broken > 0) {
                stack.hurtAndBreak(broken, player, EquipmentSlot.MAINHAND);
            }
        }
        return super.mineBlock(stack, level, state, pos, miner);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        boolean mode3x3 = is3x3Mode(stack);
        tooltip.add(Component.literal("§6Excavation Mode: " + (mode3x3 ? "§a3x3 Flat Shovel" : "§71x1 Standard")).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§8[Shift + Right-Click in air to toggle mode]").withStyle(ChatFormatting.DARK_GRAY));
    }
}

