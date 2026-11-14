package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.valentin.obsidium.ObsidiumMod;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class PloutonRodItem extends Item {

    public PloutonRodItem(Properties properties) {
        super(properties.durability(96));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        if (player == null) return InteractionResult.PASS;

        // Shift + Click on block: Set Quick Waypoint
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                setWaypoint(stack, clickedPos, level.dimension().location().toString());
                player.displayClientMessage(Component.literal("§6[Plouton's Rod] §aWaypoint saved at: §e" + clickedPos.toShortString()).withStyle(ChatFormatting.GOLD), true);
                level.playSound(null, clickedPos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0F, 1.2F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // Normal Click: Scan downwards for ores
        if (!level.isClientSide()) {
            scanForOres(level, player, clickedPos, stack);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Click in air: Show Waypoint navigation if set, or scan around player
        if (!level.isClientSide()) {
            BlockPos wp = getWaypointPos(stack);

            if (player.isShiftKeyDown()) {
                // Clear waypoint
                clearWaypoint(stack);
                player.displayClientMessage(Component.literal("§6[Plouton's Rod] §cWaypoint cleared.").withStyle(ChatFormatting.RED), true);
            } else if (wp != null) {
                // Navigate to waypoint
                double dist = Math.sqrt(player.distanceToSqr(wp.getX() + 0.5, wp.getY() + 0.5, wp.getZ() + 0.5));
                String dir = getDirectionString(player.blockPosition(), wp);
                player.displayClientMessage(Component.literal("§d[Active Waypoint] §e" + (int) dist + "m §7towards §a" + dir + " §7(Y: " + wp.getY() + ")").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.8F, 1.5F);
            } else {
                // Scan below player
                scanForOres(level, player, player.blockPosition(), stack);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static final TagKey<Block> C_ORES = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores"));
    public static final TagKey<Block> C_ORES_VOLCANITE = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores/volcanite"));

    public static boolean isVolcaniteOre(BlockState state) {
        if (state.isAir()) return false;
        return state.is(ObsidiumMod.VOLCANITE_ORE) || state.is(C_ORES_VOLCANITE);
    }

    private void scanForOres(Level level, Player player, BlockPos origin, ItemStack stack) {
        BlockPos closestOre = null;
        BlockState closestState = null;
        double minDistance = Double.MAX_VALUE;

        BlockPos closestVolcanite = null;
        BlockState volcaniteState = null;
        double minVolcaniteDistance = Double.MAX_VALUE;
        int volcaniteCount = 0;

        // Expanded 17x17 search radius around origin (-8 to +8 horizontal, down 32 blocks, up 8 blocks)
        for (int y = origin.getY() + 8; y >= origin.getY() - 32; y--) {
            for (int dx = -8; dx <= 8; dx++) {
                for (int dz = -8; dz <= 8; dz++) {
                    BlockPos p = new BlockPos(origin.getX() + dx, y, origin.getZ() + dz);
                    BlockState s = level.getBlockState(p);

                    if (isVolcaniteOre(s)) {
                        volcaniteCount++;
                        double d = origin.distSqr(p);
                        if (d < minVolcaniteDistance) {
                            minVolcaniteDistance = d;
                            closestVolcanite = p;
                            volcaniteState = s;
                        }
                    } else if (isValuableOre(s)) {
                        double d = origin.distSqr(p);
                        if (d < minDistance) {
                            minDistance = d;
                            closestOre = p;
                            closestState = s;
                        }
                    }
                }
            }
        }

        // Prioritize Volcanite Ore if detected nearby
        if (closestVolcanite != null && volcaniteState != null) {
            int dist = (int) Math.sqrt(minVolcaniteDistance);
            String dir = getDirectionString(origin, closestVolcanite);

            player.displayClientMessage(Component.literal("§6[Plouton's Rod] §c★ Volcanite Ore Found! §7(" + volcaniteCount + " nearby) §e" + dist + "m §7at Y: " + closestVolcanite.getY() + " §a[" + dir + "]").withStyle(ChatFormatting.GOLD), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9F, 1.8F);

            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.FLAME, origin.getX() + 0.5, origin.getY() + 1.2, origin.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.04);
                sl.sendParticles(ParticleTypes.GLOW, closestVolcanite.getX() + 0.5, closestVolcanite.getY() + 0.5, closestVolcanite.getZ() + 0.5, 10, 0.2, 0.2, 0.2, 0.05);
            }
        } else if (closestOre != null && closestState != null) {
            int depth = origin.getY() - closestOre.getY();
            int dist = (int) Math.sqrt(minDistance);
            String name = closestState.getBlock().getName().getString();
            String dir = getDirectionString(origin, closestOre);
            float pitch = (float) Math.max(0.5, Math.min(2.0, 2.0 - (depth / 20.0)));

            player.displayClientMessage(Component.literal("§6[Plouton's Rod] §fDetected: §b" + name + " §7(" + dist + "m, Y: " + closestOre.getY() + ") §a[" + dir + "]").withStyle(ChatFormatting.GOLD), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.0F, pitch);

            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT, origin.getX() + 0.5, origin.getY() + 1.0, origin.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.05);
            }
        } else {
            player.displayClientMessage(Component.literal("§7[Plouton's Rod] No ores detected nearby (17×17 area, 32 blocks deep).").withStyle(ChatFormatting.GRAY), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.6F, 0.7F);
        }

        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
    }

    public static boolean isValuableOre(BlockState state) {
        if (state.isAir() || state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE)) return false;
        return state.is(C_ORES)
                || isVolcaniteOre(state)
                || state.is(ObsidiumMod.LEAD_ORE) || state.is(ObsidiumMod.DEEPSLATE_LEAD_ORE)
                || state.is(ObsidiumMod.TIN_ORE) || state.is(ObsidiumMod.DEEPSLATE_TIN_ORE)
                || state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE)
                || state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE)
                || state.is(Blocks.COPPER_ORE) || state.is(Blocks.DEEPSLATE_COPPER_ORE)
                || state.is(Blocks.GOLD_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE) || state.is(Blocks.NETHER_GOLD_ORE)
                || state.is(Blocks.REDSTONE_ORE) || state.is(Blocks.DEEPSLATE_REDSTONE_ORE)
                || state.is(Blocks.LAPIS_ORE) || state.is(Blocks.DEEPSLATE_LAPIS_ORE)
                || state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)
                || state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE)
                || state.is(Blocks.NETHER_QUARTZ_ORE) || state.is(Blocks.ANCIENT_DEBRIS);
    }

    public static void setWaypoint(ItemStack stack, BlockPos pos, String dim) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt("WpX", pos.getX());
            tag.putInt("WpY", pos.getY());
            tag.putInt("WpZ", pos.getZ());
            tag.putString("WpDim", dim);
        });
    }

    public static void clearWaypoint(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove("WpX");
            tag.remove("WpY");
            tag.remove("WpZ");
            tag.remove("WpDim");
        });
    }

    public static BlockPos getWaypointPos(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        if (tag.contains("WpX")) {
            return new BlockPos(tag.getInt("WpX"), tag.getInt("WpY"), tag.getInt("WpZ"));
        }
        return null;
    }

    public static String getWaypointDim(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getString("WpDim");
    }

    public static String getDirectionString(BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double angle = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
        while (angle < 0) angle += 360.0;

        if (angle >= 337.5 || angle < 22.5) return "South";
        if (angle >= 22.5 && angle < 67.5) return "South-West";
        if (angle >= 67.5 && angle < 112.5) return "West";
        if (angle >= 112.5 && angle < 157.5) return "North-West";
        if (angle >= 157.5 && angle < 202.5) return "North";
        if (angle >= 202.5 && angle < 247.5) return "North-East";
        if (angle >= 247.5 && angle < 292.5) return "East";
        return "South-East";
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6Plouton's Divining Rod:").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§7• Right-click: Scans 5x5 column down 32 blocks.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7• Shift + Right-click block: Sets Waypoint.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7• Right-click in air: Shows direction and distance to Waypoint.").withStyle(ChatFormatting.GRAY));

        BlockPos wp = getWaypointPos(stack);
        if (wp != null) {
            tooltip.add(Component.literal("§d📍 Waypoint: §e" + wp.toShortString() + " §7(Shift + Right-click in air to clear)").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
