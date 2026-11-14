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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import team.reborn.energy.api.base.SimpleEnergyItem;

import java.util.List;

public class HephaesteanResonatorItem extends Item implements SimpleEnergyItem {

    public static final long CAPACITY = 250_000L;
    public static final long MAX_INPUT = 10_000L;
    public static final long COST_PER_SCAN = 500L;

    public static final int MODE_MODDED = 0;
    public static final int MODE_PRECIOUS = 1;
    public static final int MODE_COMMON = 2;
    public static final int MODE_ALL = 3;
    public static final int MODE_WAYPOINT = 4;
    public static final int MODE_COUNT = 5;

    public HephaesteanResonatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) { return CAPACITY; }

    @Override
    public long getEnergyMaxInput(ItemStack stack) { return MAX_INPUT; }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) { return 0; }

    @Override
    public boolean isBarVisible(ItemStack stack) { return true; }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round((double) getStoredEnergy(stack) * 13.0 / (double) CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) { return 0x00E5FF; }

    public static int getMode(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getInt("ResonatorMode");
    }

    public static void cycleMode(ItemStack stack, Player player) {
        int next = (getMode(stack) + 1) % MODE_COUNT;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("ResonatorMode", next));
        player.displayClientMessage(Component.literal("§6[Hephaestean Resonator] §fSelected Mode: " + getModeName(next)).withStyle(ChatFormatting.GOLD), true);
    }

    public static String getModeName(int mode) {
        return switch (mode) {
            case MODE_MODDED -> "§bModded Ores (Lead, Tin, Volcanite)";
            case MODE_PRECIOUS -> "§dPrecious Ores (Diamond, Netherite, Gold, Emerald)";
            case MODE_COMMON -> "§eCommon Ores (Iron, Copper, Coal, Lapis, Redstone)";
            case MODE_ALL -> "§aAll Ores";
            case MODE_WAYPOINT -> "§5Ariadne's Thread (Waypoint Guidance)";
            default -> "Unknown";
        };
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        if (player == null) return InteractionResult.PASS;

        // Shift + Click on block: Set Waypoint
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                setWaypoint(stack, pos, level.dimension().location().toString());
                player.displayClientMessage(Component.literal("§6[Hephaestean Resonator] §aWaypoint saved at: §e" + pos.toShortString()).withStyle(ChatFormatting.GOLD), true);
                level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.5F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return use(level, player, context.getHand()).getResult();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                cycleMode(stack, player);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5F, 1.4F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        if (getStoredEnergy(stack) < COST_PER_SCAN) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.literal("§c[Hephaestean Resonator] Insufficient RF energy (500 RF required)!").withStyle(ChatFormatting.RED), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.8F, 0.5F);
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide()) {
            int mode = getMode(stack);
            tryUseEnergy(stack, COST_PER_SCAN);

            if (mode == MODE_WAYPOINT) {
                navigateWaypoint(level, player, stack);
            } else {
                scanSphereOres(level, player, mode);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private void navigateWaypoint(Level level, Player player, ItemStack stack) {
        BlockPos wp = getWaypointPos(stack);
        if (wp == null) {
            player.displayClientMessage(Component.literal("§c[Hephaestean Resonator] No waypoint set! Shift + Right-click a block first.").withStyle(ChatFormatting.RED), true);
            return;
        }

        double dist = Math.sqrt(player.distanceToSqr(wp.getX() + 0.5, wp.getY() + 0.5, wp.getZ() + 0.5));
        String dir = PloutonRodItem.getDirectionString(player.blockPosition(), wp);
        int deltaY = wp.getY() - player.getBlockY();
        String yStr = deltaY > 0 ? ("+" + deltaY + " up") : (deltaY < 0 ? (deltaY + " down") : "same level");

        player.displayClientMessage(Component.literal("§5[Ariadne's Thread] §e" + (int) dist + "m §7towards §a" + dir + " §7(Y: " + wp.getY() + ", " + yStr + ")").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.6F, 1.8F);

        // Spawn visual beam towards waypoint
        if (level instanceof ServerLevel sl) {
            spawnParticleLine(sl, player.getEyePosition(), wp.getCenter(), ParticleTypes.SOUL_FIRE_FLAME, 12);
        }
    }

    private void scanSphereOres(Level level, Player player, int mode) {
        BlockPos playerPos = player.blockPosition();
        int radius = 24;
        BlockPos closest = null;
        BlockState closestState = null;
        double minDistance = Double.MAX_VALUE;
        int totalFound = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radius * radius) continue;

                    BlockPos p = playerPos.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(p);

                    if (matchesFilter(s, mode)) {
                        totalFound++;
                        double dist = playerPos.distSqr(p);
                        if (dist < minDistance) {
                            minDistance = dist;
                            closest = p;
                            closestState = s;
                        }
                    }
                }
            }
        }

        if (closest != null && closestState != null) {
            int d = (int) Math.sqrt(minDistance);
            String name = closestState.getBlock().getName().getString();
            String dir = PloutonRodItem.getDirectionString(playerPos, closest);

            boolean isVolcanite = PloutonRodItem.isVolcaniteOre(closestState);
            if (isVolcanite) {
                player.displayClientMessage(Component.literal("§6[Resonator] §c★ Volcanite Ore Detected! §7(" + totalFound + " nearby) - §e" + d + "m §7at Y: " + closest.getY() + " §a[" + dir + "]").withStyle(ChatFormatting.GOLD), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.8F);
            } else {
                player.displayClientMessage(Component.literal("§6[Resonator] §fFound: §b" + name + " §7(" + totalFound + " nearby) - §e" + d + "m §7at Y: " + closest.getY() + " §a[" + dir + "]").withStyle(ChatFormatting.GOLD), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.9F, 1.6F);
            }

            // Spawn particle trail from player to the ore block
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(isVolcanite ? ParticleTypes.FLAME : ParticleTypes.ELECTRIC_SPARK, player.getEyePosition().x, player.getEyePosition().y, player.getEyePosition().z, 16, 0.2, 0.2, 0.2, 0.05);
                spawnParticleLine(sl, player.getEyePosition(), closest.getCenter(), isVolcanite ? ParticleTypes.FLAME : ParticleTypes.ELECTRIC_SPARK, 16);
                sl.sendParticles(ParticleTypes.GLOW, closest.getX() + 0.5, closest.getY() + 0.5, closest.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.05);
            }
        } else {
            player.displayClientMessage(Component.literal("§7[Resonator] No matching ores within 24-block radius.").withStyle(ChatFormatting.GRAY), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.6F, 0.6F);
        }
    }

    private void spawnParticleLine(ServerLevel level, net.minecraft.world.phys.Vec3 from, net.minecraft.world.phys.Vec3 to, net.minecraft.core.particles.ParticleOptions particle, int count) {
        net.minecraft.world.phys.Vec3 diff = to.subtract(from);
        double len = diff.length();
        net.minecraft.world.phys.Vec3 step = diff.normalize();

        int points = Math.min(count, (int) Math.max(5, len));
        for (int i = 1; i <= points; i++) {
            double progress = (double) i / (double) points * Math.min(len, 16.0);
            net.minecraft.world.phys.Vec3 p = from.add(step.scale(progress));
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0.0);
        }
    }

    private boolean matchesFilter(BlockState state, int mode) {
        if (state.isAir() || state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE)) return false;

        boolean isLeadOrTinOrVolcanite = state.is(ObsidiumMod.LEAD_ORE) || state.is(ObsidiumMod.DEEPSLATE_LEAD_ORE)
                || state.is(ObsidiumMod.TIN_ORE) || state.is(ObsidiumMod.DEEPSLATE_TIN_ORE)
                || state.is(ObsidiumMod.VOLCANITE_ORE);

        if (mode == MODE_MODDED) {
            return isLeadOrTinOrVolcanite;
        }

        boolean isPrecious = state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)
                || state.is(Blocks.ANCIENT_DEBRIS)
                || state.is(Blocks.GOLD_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE) || state.is(Blocks.NETHER_GOLD_ORE)
                || state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE);

        if (mode == MODE_PRECIOUS) {
            return isPrecious;
        }

        boolean isCommon = state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE)
                || state.is(Blocks.COPPER_ORE) || state.is(Blocks.DEEPSLATE_COPPER_ORE)
                || state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE)
                || state.is(Blocks.LAPIS_ORE) || state.is(Blocks.DEEPSLATE_LAPIS_ORE)
                || state.is(Blocks.REDSTONE_ORE) || state.is(Blocks.DEEPSLATE_REDSTONE_ORE);

        if (mode == MODE_COMMON) {
            return isCommon;
        }

        if (mode == MODE_ALL) {
            return isLeadOrTinOrVolcanite || isPrecious || isCommon || PloutonRodItem.isValuableOre(state);
        }

        return false;
    }

    public static void setWaypoint(ItemStack stack, BlockPos pos, String dim) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt("WpX", pos.getX());
            tag.putInt("WpY", pos.getY());
            tag.putInt("WpZ", pos.getZ());
            tag.putString("WpDim", dim);
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        long energy = getStoredEnergy(stack);
        tooltip.add(Component.literal("§b⚡ Energy: §f" + String.format("%,d", energy) + " §b/ §f" + String.format("%,d", CAPACITY) + " RF").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("§6Current Mode: " + getModeName(getMode(stack))).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§7• Shift + Right-click in air: Switch detection mode.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7• Shift + Right-click block: Set target Waypoint.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7• Right-click: Scan 3D sphere (24 blocks) or guide to Waypoint.").withStyle(ChatFormatting.GRAY));

        BlockPos wp = getWaypointPos(stack);
        if (wp != null) {
            tooltip.add(Component.literal("§5📍 Waypoint: §e" + wp.toShortString()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
