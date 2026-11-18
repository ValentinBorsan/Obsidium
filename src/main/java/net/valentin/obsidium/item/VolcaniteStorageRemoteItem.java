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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
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
            var controller = resolveController(player, stack);
            if (controller == null) {
                player.displayClientMessage(Component.literal("§cCannot reach linked controller: check dimension, range and loaded chunk."), true);
                return InteractionResultHolder.fail(stack);
            }
            player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, opener) -> {
                var menu = (net.valentin.obsidium.block.VolcaniteStorageTerminalMenu) controller.createMenu(id, inventory, opener);
                return menu.withRemote(stack, opener);
            }, controller.getDisplayName()));
            level.playSound(null, player.blockPosition(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 0.5F, 1.8F);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Resolves only loaded controllers; never loads or generates a destination chunk. */
    public static net.valentin.obsidium.block.VolcaniteStorageControllerBlockEntity resolveController(Player player, ItemStack stack) {
        if (!(stack.getItem() instanceof VolcaniteStorageRemoteItem) || player.getServer() == null) return null;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.getBoolean("Linked") || !tag.contains("X", 3) || !tag.contains("Y", 3)
                || !tag.contains("Z", 3)) return null;
        var id = net.minecraft.resources.ResourceLocation.tryParse(tag.getString("Dim"));
        if (id == null) return null;
        var destination = player.getServer().getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, id));
        if (destination == null) return null;
        boolean sameDimension = destination == player.level();
        var config = net.valentin.obsidium.config.ModConfig.get();
        if (!sameDimension && !config.remoteTerminalCrossDimension) return null;
        BlockPos pos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
        double range = Math.max(0, config.remoteTerminalRange);
        if (sameDimension && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) > range * range) return null;
        if (!destination.isInWorldBounds(pos) || !destination.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return null;
        return destination.getBlockEntity(pos) instanceof net.valentin.obsidium.block.VolcaniteStorageControllerBlockEntity controller
                && !controller.isRemoved() ? controller : null;
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
