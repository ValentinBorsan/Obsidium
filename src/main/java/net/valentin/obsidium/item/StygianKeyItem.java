package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.valentin.obsidium.ObsidiumMod;

import java.util.List;

public class StygianKeyItem extends Item {

    public StygianKeyItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createKey(BlockPos pos, String dimensionId, String ownerUUID, String ownerName) {
        ItemStack stack = new ItemStack(ObsidiumMod.STYGIAN_KEY);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt("GraveX", pos.getX());
            tag.putInt("GraveY", pos.getY());
            tag.putInt("GraveZ", pos.getZ());
            tag.putString("GraveDim", dimensionId);
            tag.putString("OwnerUUID", ownerUUID);
            tag.putString("OwnerName", ownerName);
        });
        return stack;
    }

    public static BlockPos getGravePos(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        if (tag.contains("GraveX")) {
            return new BlockPos(tag.getInt("GraveX"), tag.getInt("GraveY"), tag.getInt("GraveZ"));
        }
        return null;
    }

    public static String getGraveDim(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getString("GraveDim");
    }

    public static String getOwnerUUID(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getString("OwnerUUID");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockPos pos = getGravePos(stack);
        String dim = getGraveDim(stack);

        if (pos != null && level.isClientSide()) {
            double dist = Math.sqrt(player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
            player.displayClientMessage(Component.literal("§5[Stygian Cenotaph] §7Location: §e" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " §7(" + dim + ") - §b" + (int) dist + "m away").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BlockPos pos = getGravePos(stack);
        String dim = getGraveDim(stack);
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        String owner = data.copyTag().getString("OwnerName");

        if (pos != null) {
            tooltip.add(Component.literal("§7Owner: §e" + (owner.isEmpty() ? "Unknown" : owner)).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("§7Location: §6X: " + pos.getX() + "  Y: " + pos.getY() + "  Z: " + pos.getZ()).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal("§7Dimension: §d" + dim).withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.add(Component.literal("§eRight-click in air §7to calculate distance.").withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal("§aRight-click the Cenotaph §7to reclaim your items.").withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.literal("§cUnbound key (no associated grave).").withStyle(ChatFormatting.RED));
        }
    }
}

