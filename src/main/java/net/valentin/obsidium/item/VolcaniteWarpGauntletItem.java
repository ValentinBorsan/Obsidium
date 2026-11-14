package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.valentin.obsidium.block.VolcanicTeleporterBlockEntity;
import net.valentin.obsidium.block.VolcaniteTeleporterMenu;
import net.valentin.obsidium.teleport.VolcaniteTeleportNetworkState;
import team.reborn.energy.api.base.SimpleEnergyItem;

import java.util.List;

/**
 * Quantum Warp Gauntlet allowing portable access to the entire Volcanite Teleporter Network,
 * personal coordinate bookmarks, cross-dimensional jumps, and instant Shift-Right-Click Quick-Warp.
 */
public class VolcaniteWarpGauntletItem extends Item implements SimpleEnergyItem {
    public static final long ENERGY_CAPACITY = 5_000_000L;
    public static final long MAX_INSERT = 50_000L;
    public static final long MAX_EXTRACT = 0L;

    public VolcaniteWarpGauntletItem(Properties properties) {
        super(properties.rarity(Rarity.RARE));
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return ENERGY_CAPACITY;
    }

    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return MAX_INSERT;
    }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return MAX_EXTRACT;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round((double) getStoredEnergy(stack) * 13.0 / (double) ENERGY_CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00E5FF;
    }

    public static String getPinnedTarget(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getString("PinnedTarget");
    }

    public static void setPinnedTarget(ItemStack stack, String targetId, String targetName,
                                       String dim, int x, int y, int z) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString("PinnedTarget", targetId);
            tag.putString("PinnedName", targetName);
            tag.putString("WpDim", dim);
            tag.putInt("WpX", x);
            tag.putInt("WpY", y);
            tag.putInt("WpZ", z);
        });
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        if (player == null) return InteractionResult.PASS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof VolcanicTeleporterBlockEntity pad) {
            if (player.isShiftKeyDown()) {
                if (!level.isClientSide() && level.getServer() != null) {
                    String dim = level.dimension().location().toString();
                    String padId = VolcaniteTeleportNetworkState.makePadId(dim, pos);
                    pad.registerInNetwork();
                    setPinnedTarget(stack, padId, pad.getPadName(), dim, pos.getX(), pos.getY(), pos.getZ());

                    level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9F, 1.7F);
                    if (level instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                                18, 0.3, 0.2, 0.3, 0.08);
                    }
                    player.displayClientMessage(Component.literal("§b⛩ Warp Gauntlet pinned to §e" + pad.getPadName()), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
            return InteractionResult.PASS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        if (player instanceof ServerPlayer serverPlayer && serverPlayer.server != null) {
            VolcaniteTeleportNetworkState state = VolcaniteTeleportNetworkState.get(serverPlayer.server);

            // Shift + Right-click in air: Instant Quick-Warp to pinned target (or bed/spawn)
            if (player.isShiftKeyDown()) {
                String targetId = getPinnedTarget(stack);
                if (targetId.isBlank() || state.findDestination(serverPlayer, targetId) == null) {
                    targetId = state.findDestination(serverPlayer, "bed") != null ? "bed" : "spawn";
                }
                boolean ok = state.executeTeleport(serverPlayer, targetId, null, usedHand);
                return ok ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
            }

            // Normal Right-click: Open portable Quantum Teleportation Network GUI
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (syncId, inv, p) -> new VolcaniteTeleporterMenu(syncId, inv, usedHand, stack),
                    Component.translatable("container.obsidium.teleporter_network")
            ));
            state.sendSyncToPlayer(serverPlayer, null, stack);
            return InteractionResultHolder.consume(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal(String.format("§7Energy: §b%,d / %,d RF", getStoredEnergy(stack), ENERGY_CAPACITY)));
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        String pinned = tag.getString("PinnedName");
        if (!pinned.isBlank()) {
            tooltipComponents.add(Component.literal("§e📌 Quick-Warp: §f" + pinned).withStyle(ChatFormatting.YELLOW));
        } else {
            tooltipComponents.add(Component.literal("§e📌 Quick-Warp: §7Bed / World Spawn").withStyle(ChatFormatting.GRAY));
        }
        tooltipComponents.add(Component.literal("§b▸ Right-Click: §fOpen Quantum Teleport Matrix"));
        tooltipComponents.add(Component.literal("§d▸ Shift + Right-Click: §fInstant Quick-Warp"));
        tooltipComponents.add(Component.literal("§7▸ Shift + Click Pad: Pin pad as Quick-Warp target"));
    }
}
