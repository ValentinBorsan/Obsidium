package net.valentin.obsidium.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.PloutonRodItem;
import net.valentin.obsidium.item.TelluricAstrolabeItem;

public class ObsidiumCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register(ObsidiumCommands::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        // 1. Full command: /obsidium tp <caldera|waypoint>
        dispatcher.register(
            Commands.literal("obsidium")
                .then(Commands.literal("tp")
                    .then(Commands.literal("caldera")
                        .executes(ObsidiumCommands::teleportToCaldera)
                    )
                    .then(Commands.literal("waypoint")
                        .executes(ObsidiumCommands::teleportToWaypoint)
                    )
                )
        );

        // 2. Short alias: /obtp <caldera|waypoint>
        dispatcher.register(
            Commands.literal("obtp")
                .then(Commands.literal("caldera")
                    .executes(ObsidiumCommands::teleportToCaldera)
                )
                .then(Commands.literal("waypoint")
                    .executes(ObsidiumCommands::teleportToWaypoint)
                )
                .executes(ObsidiumCommands::teleportToWaypoint) // /obtp default to waypoint
        );
    }

    private static int teleportToCaldera(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("§cAceastă comandă poate fi executată doar de un jucător."));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        BlockPos playerPos = player.blockPosition();

        Pair<BlockPos, Holder<Biome>> caldera = level.findClosestBiome3d(
                holder -> holder.is(TelluricAstrolabeItem.CALDERA_KEY),
                playerPos,
                6400,
                32,
                64
        );

        if (caldera == null) {
            source.sendFailure(Component.literal("§c[Obsidium] Nicio Calderă Vulcanică nu a fost găsită pe o rază de 6.400 de blocuri!"));
            return 0;
        }

        BlockPos cPos = caldera.getFirst();
        int safeY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, cPos.getX(), cPos.getZ()) + 1;
        double targetX = cPos.getX() + 0.5;
        double targetY = safeY;
        double targetZ = cPos.getZ() + 0.5;

        player.teleportTo(level, targetX, targetY, targetZ, player.getYRot(), player.getXRot());
        level.playSound(null, targetX, targetY, targetZ, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);

        source.sendSuccess(() -> Component.literal("§6★ [Obsidium] §aTeleportat la Caldera Vulcanică! §8(X: " + cPos.getX() + ", Y: " + safeY + ", Z: " + cPos.getZ() + ")"), false);
        return 1;
    }

    private static int teleportToWaypoint(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("§cAceastă comandă poate fi executată doar de un jucător."));
            return 0;
        }

        BlockPos targetPos = null;
        String targetDim = null;
        String sourceName = null;

        // 1. Check main hand and off hand
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();

        if (main.is(ObsidiumMod.PLOUTON_ROD) && PloutonRodItem.getWaypointPos(main) != null) {
            targetPos = PloutonRodItem.getWaypointPos(main);
            targetDim = PloutonRodItem.getWaypointDim(main);
            sourceName = "Plouton's Rod (Main Hand)";
        } else if (main.is(ObsidiumMod.TELLURIC_ASTROLABE) && TelluricAstrolabeItem.getCalderaLock(main) != null) {
            targetPos = TelluricAstrolabeItem.getCalderaLock(main);
            sourceName = "Astrolab Teluric (Main Hand)";
        } else if (off.is(ObsidiumMod.PLOUTON_ROD) && PloutonRodItem.getWaypointPos(off) != null) {
            targetPos = PloutonRodItem.getWaypointPos(off);
            targetDim = PloutonRodItem.getWaypointDim(off);
            sourceName = "Plouton's Rod (Off Hand)";
        } else if (off.is(ObsidiumMod.TELLURIC_ASTROLABE) && TelluricAstrolabeItem.getCalderaLock(off) != null) {
            targetPos = TelluricAstrolabeItem.getCalderaLock(off);
            sourceName = "Astrolab Teluric (Off Hand)";
        }

        // 2. If not held in hands, scan entire player inventory
        if (targetPos == null) {
            Inventory inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (stack.is(ObsidiumMod.PLOUTON_ROD) && PloutonRodItem.getWaypointPos(stack) != null) {
                    targetPos = PloutonRodItem.getWaypointPos(stack);
                    targetDim = PloutonRodItem.getWaypointDim(stack);
                    sourceName = "Plouton's Rod";
                    break;
                } else if (stack.is(ObsidiumMod.TELLURIC_ASTROLABE) && TelluricAstrolabeItem.getCalderaLock(stack) != null) {
                    targetPos = TelluricAstrolabeItem.getCalderaLock(stack);
                    sourceName = "Astrolab Teluric";
                    break;
                }
            }
        }

        if (targetPos == null) {
            source.sendFailure(Component.literal("§c[Obsidium] Nu s-a găsit niciun waypoint salvat!"));
            source.sendFailure(Component.literal("§7• Salvează un punct cu §ePlouton's Rod §7(Shift + Click dreapta pe bloc)."));
            source.sendFailure(Component.literal("§7• Sau blochează o Calderă cu §6Astrolabul Teluric §7(Shift + Click dreapta)."));
            return 0;
        }

        // Determine destination level
        ServerLevel targetLevel = player.serverLevel();
        if (targetDim != null && !targetDim.isEmpty()) {
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(targetDim));
            ServerLevel foundLevel = player.getServer().getLevel(dimKey);
            if (foundLevel != null) {
                targetLevel = foundLevel;
            }
        }

        // Ensure safe Y if at void/default
        int safeY = targetPos.getY();
        if (safeY <= targetLevel.getMinBuildHeight() || safeY == 64) {
            safeY = targetLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, targetPos.getX(), targetPos.getZ()) + 1;
        }

        double targetX = targetPos.getX() + 0.5;
        double targetY = safeY;
        double targetZ = targetPos.getZ() + 0.5;

        player.teleportTo(targetLevel, targetX, targetY, targetZ, player.getYRot(), player.getXRot());
        targetLevel.playSound(null, targetX, targetY, targetZ, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);

        final String finalSourceName = sourceName;
        final BlockPos finalPos = new BlockPos(targetPos.getX(), safeY, targetPos.getZ());
        source.sendSuccess(() -> Component.literal("§6★ [Obsidium] §aTeleportat la waypoint (§e" + finalSourceName + "§a): §8" + finalPos.toShortString()), false);
        return 1;
    }
}

