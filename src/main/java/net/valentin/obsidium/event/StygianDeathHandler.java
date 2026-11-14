package net.valentin.obsidium.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.StygianCenotaphBlockEntity;
import net.valentin.obsidium.item.EternalCoinItem;
import net.valentin.obsidium.item.StygianKeyItem;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StygianDeathHandler {

    public record RespawnReward(BlockPos gravePos, String dimensionId, ItemStack preservedCoin, String ownerName) {}

    private static final Map<UUID, RespawnReward> PENDING_RESPAWN = new ConcurrentHashMap<>();

    public static void register() {
        // Intercept fatal player death before items drop
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (!(entity instanceof ServerPlayer player)) {
                return true;
            }

            // If player has a Totem of Undying, let the totem activate normally
            if (player.getMainHandItem().is(Items.TOTEM_OF_UNDYING) || player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
                return true;
            }

            handlePlayerDeathGrave(player);
            return true;
        });

        // Grant the Stygian Key (and preserved Eternal Coin) on respawn
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            RespawnReward reward = PENDING_RESPAWN.remove(newPlayer.getUUID());
            if (reward != null) {
                ItemStack key = StygianKeyItem.createKey(reward.gravePos(), reward.dimensionId(), newPlayer.getUUID().toString(), reward.ownerName());
                if (!newPlayer.getInventory().add(key)) {
                    newPlayer.drop(key, true);
                }

                if (!reward.preservedCoin().isEmpty()) {
                    if (!newPlayer.getInventory().add(reward.preservedCoin())) {
                        newPlayer.drop(reward.preservedCoin(), true);
                    }
                }

                newPlayer.sendSystemMessage(Component.literal("§5==========================================").withStyle(ChatFormatting.DARK_PURPLE));
                newPlayer.sendSystemMessage(Component.literal("§6[Stygian Cenotaph] §fYou received the §dStygian Key§f!").withStyle(ChatFormatting.GOLD));
                newPlayer.sendSystemMessage(Component.literal("§7Grave Location: §e" + reward.gravePos().toShortString() + " §7(" + reward.dimensionId() + ")").withStyle(ChatFormatting.GRAY));
                newPlayer.sendSystemMessage(Component.literal("§bRight-click in air with the key to trace distance.").withStyle(ChatFormatting.AQUA));
                newPlayer.sendSystemMessage(Component.literal("§5==========================================").withStyle(ChatFormatting.DARK_PURPLE));
            }
        });
    }

    private static void handlePlayerDeathGrave(ServerPlayer player) {
        // 1. Check for Eternal Coin (Tier 2, powered)
        ItemStack eternalCoin = null;
        int eternalSlot = -1;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ObsidiumMod.ETERNAL_COIN) && stack.getItem() instanceof EternalCoinItem coinItem) {
                if (coinItem.getStoredEnergy(stack) >= EternalCoinItem.COST_PER_DEATH) {
                    eternalCoin = stack;
                    eternalSlot = i;
                    break;
                }
            }
        }

        // 2. Check for Charon's Coin (Tier 1, single use)
        ItemStack charonCoin = null;
        int charonSlot = -1;
        if (eternalCoin == null) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(ObsidiumMod.CHARON_COIN)) {
                    charonCoin = stack;
                    charonSlot = i;
                    break;
                }
            }
        }

        // If player doesn't have either coin, proceed with vanilla death
        if (eternalCoin == null && charonCoin == null) {
            return;
        }

        // Consume / charge the coin
        ItemStack coinToPreserve = ItemStack.EMPTY;
        if (eternalCoin != null) {
            ((EternalCoinItem) eternalCoin.getItem()).tryUseEnergy(eternalCoin, EternalCoinItem.COST_PER_DEATH);
            coinToPreserve = eternalCoin.copy();
            player.getInventory().setItem(eternalSlot, ItemStack.EMPTY);
        } else {
            charonCoin.shrink(1);
            if (charonCoin.isEmpty()) {
                player.getInventory().setItem(charonSlot, ItemStack.EMPTY);
            }
        }

        ServerLevel level = player.serverLevel();
        BlockPos gravePos = calculateSafeGravePos(level, player.blockPosition());

        // Capture all equipment & inventory
        NonNullList<ItemStack> captured = NonNullList.withSize(StygianCenotaphBlockEntity.INVENTORY_SIZE, ItemStack.EMPTY);

        // Main inventory slots 0..35
        for (int i = 0; i < 36; i++) {
            captured.set(i, player.getInventory().getItem(i).copy());
        }

        // Armor slots (36: Helmet, 37: Chestplate, 38: Leggings, 39: Boots)
        captured.set(36, player.getItemBySlot(EquipmentSlot.HEAD).copy());
        captured.set(37, player.getItemBySlot(EquipmentSlot.CHEST).copy());
        captured.set(38, player.getItemBySlot(EquipmentSlot.LEGS).copy());
        captured.set(39, player.getItemBySlot(EquipmentSlot.FEET).copy());

        // Offhand slot (40)
        captured.set(40, player.getItemBySlot(EquipmentSlot.OFFHAND).copy());

        // Capture XP
        int xp = player.totalExperience;
        player.totalExperience = 0;
        player.experienceLevel = 0;
        player.experienceProgress = 0;

        // Clear player inventory so no items drop into lava / ground
        player.getInventory().clearContent();

        // Place Stygian Cenotaph
        level.setBlock(gravePos, ObsidiumMod.STYGIAN_CENOTAPH_BLOCK.defaultBlockState(), 3);
        BlockEntity be = level.getBlockEntity(gravePos);
        if (be instanceof StygianCenotaphBlockEntity cenotaph) {
            cenotaph.initializeGrave(player, captured, xp);
        }

        // Schedule respawn key delivery
        String dimId = level.dimension().location().toString();
        PENDING_RESPAWN.put(player.getUUID(), new RespawnReward(gravePos, dimId, coinToPreserve, player.getName().getString()));

        // Notify in server chat log
        player.sendSystemMessage(Component.literal("§5[Stygian Cenotaph] §7Your inventory was preserved at §6" + gravePos.toShortString()).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private static BlockPos calculateSafeGravePos(ServerLevel level, BlockPos origin) {
        int y = origin.getY();
        int minY = level.getMinBuildHeight() + 2;
        int maxY = level.getMaxBuildHeight() - 2;

        if (y < minY) {
            y = minY;
        } else if (y > maxY) {
            y = maxY;
        }

        BlockPos target = new BlockPos(origin.getX(), y, origin.getZ());

        // If target is floating in void or completely unsupported, place an obsidian foundation underneath if needed
        BlockPos below = target.below();
        if (level.getBlockState(below).isAir() && target.getY() <= minY + 2) {
            level.setBlock(below, Blocks.OBSIDIAN.defaultBlockState(), 3);
        }

        return target;
    }
}

