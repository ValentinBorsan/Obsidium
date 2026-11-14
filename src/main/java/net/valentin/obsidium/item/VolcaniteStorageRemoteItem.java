package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.valentin.obsidium.block.VolcaniteStorageControllerBlock;

import java.util.List;

public class VolcaniteStorageRemoteItem extends Item {
    public VolcaniteStorageRemoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (player != null && player.isShiftKeyDown()) {
            if (level.getBlockState(pos).getBlock() instanceof VolcaniteStorageControllerBlock) {
                if (!level.isClientSide()) {
                    ItemStack stack = context.getItemInHand();
                    String dim = level.dimension().location().toString();
                    CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                        tag.putInt("X", pos.getX());
                        tag.putInt("Y", pos.getY());
                        tag.putInt("Z", pos.getZ());
                        tag.putString("Dim", dim);
                        tag.putBoolean("Linked", true);
                    });
                    level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.5F);
                    player.displayClientMessage(Component.literal("§6[Volcanite Storage Remote] §aLinked to Controller at §e(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")"), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();

        if (!tag.getBoolean("Linked")) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.literal("§cRemote not linked! Sneak + Right-Click a Volcanite Storage Controller to link."), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide()) {
            String dim = tag.getString("Dim");
            boolean sameDim = level.dimension().location().toString().equals(dim);
            if (!sameDim && !net.valentin.obsidium.config.ModConfig.get().remoteTerminalCrossDimension) {
                player.displayClientMessage(Component.literal("§cCannot connect across dimensions!"), true);
                return InteractionResultHolder.fail(stack);
            }

            BlockPos pos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
            double distSq = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            int maxRange = net.valentin.obsidium.config.ModConfig.get().remoteTerminalRange;
            if (sameDim && distSq > (double) maxRange * maxRange) {
                player.displayClientMessage(Component.literal("§cOut of range! Maximum remote distance is " + maxRange + " blocks (" + (int) Math.sqrt(distSq) + "m away)."), true);
                return InteractionResultHolder.fail(stack);
            }

            if (!level.isLoaded(pos)) {
                player.displayClientMessage(Component.literal("§cController chunk is not loaded!"), true);
                return InteractionResultHolder.fail(stack);
            }

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MenuProvider menuProvider) {
                player.openMenu(menuProvider);
                level.playSound(null, player.blockPosition(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 0.5F, 1.8F);
                return InteractionResultHolder.sidedSuccess(stack, false);
            } else {
                player.displayClientMessage(Component.literal("§cVolcanite Storage Controller not found at linked location!"), true);
                return InteractionResultHolder.fail(stack);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag isAdvanced) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        if (tag.getBoolean("Linked")) {
            int x = tag.getInt("X");
            int y = tag.getInt("Y");
            int z = tag.getInt("Z");
            String dim = tag.getString("Dim");
            tooltip.add(Component.literal("Linked: (" + x + ", " + y + ", " + z + ")").withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.literal("Dimension: " + dim).withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.literal("§eRemote Range: §f" + net.valentin.obsidium.config.ModConfig.get().remoteTerminalRange + " Blocks").withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.literal("Not Linked").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("Shift + Right-Click Controller to link").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("§eRemote Range: §f" + net.valentin.obsidium.config.ModConfig.get().remoteTerminalRange + " Blocks").withStyle(ChatFormatting.GOLD));
        }
    }
}
