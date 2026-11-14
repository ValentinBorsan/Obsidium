package net.valentin.obsidium;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelResource;
import net.valentin.obsidium.block.OlympianHadronColliderBlockEntity;
import net.valentin.obsidium.block.OlympianHadronColliderMenu;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Fabric-only integration entrypoint, staged as a separate test mod in an isolated server directory. */
public final class ColliderRuntimeChecks implements ModInitializer {
    private static final BlockPos ACTIVE = new BlockPos(36, 100, 36);

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                server.tickRateManager().setFrozen(true);
                Path expected = server.getWorldPath(LevelResource.ROOT).resolve("collider-expected.nbt");
                if ("reload".equals(System.getProperty("obsidium.audit.phase"))) {
                    OlympianHadronColliderBlockEntity collider = collider(server.overworld(), ACTIVE);
                    CompoundTag saved = NbtIo.readCompressed(expected, NbtAccounter.unlimitedHeap());
                    check(saved.equals(collider.saveWithoutMetadata(server.registryAccess())), "RESTART_STATE_MATCHES");
                } else {
                    prepareAndCheck(server, expected);
                }
                System.out.println("COLLIDER_AUDIT_ALL_PASSED");
            } catch (Throwable failure) {
                failure.printStackTrace();
                System.out.println("COLLIDER_AUDIT_FAILED");
            } finally {
                server.halt(false);
            }
        });
    }

    private static void prepareAndCheck(MinecraftServer server, Path expected) throws Exception {
        ServerLevel level = server.overworld();
        level.setChunkForced(ACTIVE.getX() >> 4, ACTIVE.getZ() >> 4, true);
        BlockPos center = ACTIVE.offset(0, 0, 2);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos pos = center.offset(dx, 0, dz);
                level.setBlock(pos, Math.max(Math.abs(dx), Math.abs(dz)) == 2
                        ? ObsidiumMod.SYNCHROTRON_RING_COIL_BLOCK.defaultBlockState()
                        : Blocks.AIR.defaultBlockState(), 3);
            }
        }
        level.setBlock(center.offset(0, 0, 2), ObsidiumMod.HADRON_ENERGY_PORT_BLOCK.defaultBlockState(), 3);
        level.setBlock(center.offset(-2, 0, 0), ObsidiumMod.HADRON_ITEM_PORT_BLOCK.defaultBlockState(), 3);
        level.setBlock(center.offset(2, 0, 0), ObsidiumMod.HADRON_FLUID_PORT_BLOCK.defaultBlockState(), 3);
        level.setBlock(ACTIVE, ObsidiumMod.OLYMPIAN_HADRON_COLLIDER_BLOCK.defaultBlockState(), 3);
        OlympianHadronColliderBlockEntity active = collider(level, ACTIVE);
        active.setItem(0, new ItemStack(Items.IRON_INGOT));
        active.setItem(1, new ItemStack(Items.COPPER_INGOT));
        CompoundTag seed = active.saveWithoutMetadata(server.registryAccess());
        seed.putInt("BeamVel", 600);
        seed.putInt("CoilTemp", 200);
        seed.putInt("FuelAmt", 10000);
        seed.putInt("FuelType", OlympianHadronColliderBlockEntity.FUEL_LAVA);
        seed.putInt("CoolAmt", 10000);
        seed.putInt("CoolType", OlympianHadronColliderBlockEntity.COOLANT_WATER);
        seed.putInt("OpMode", OlympianHadronColliderBlockEntity.MODE_STORAGE_RING);
        active.loadWithComponents(seed, server.registryAccess());
        tick(level, active);
        tick(level, active); // Stabilize formed/lit block states before clearing the dirty flag.
        check(active.isFormed(), "RING_FORMED");
        CompoundTag before = active.saveWithoutMetadata(server.registryAccess());
        LevelChunk chunk = level.getChunkAt(ACTIVE);
        chunk.setUnsaved(false);
        tick(level, active);
        CompoundTag after = active.saveWithoutMetadata(server.registryAccess());
        check(after.getInt("FuelAmt") < before.getInt("FuelAmt"), "FUEL_CONSUMED");
        check(after.getInt("CoolAmt") < before.getInt("CoolAmt"), "COOLANT_CONSUMED");
        check(after.getLong("Energy") > before.getLong("Energy"), "ENERGY_GENERATED");
        check(after.getInt("BeamVel") > before.getInt("BeamVel"), "VELOCITY_CHANGED");
        check(after.getInt("CoilTemp") != before.getInt("CoilTemp"), "TEMPERATURE_CHANGED");
        check(chunk.isUnsaved(), "RUNNING_TICK_MARKS_CHUNK");

        BlockPos idlePos = new BlockPos(68, 100, 68);
        level.setBlock(idlePos, ObsidiumMod.OLYMPIAN_HADRON_COLLIDER_BLOCK.defaultBlockState(), 3);
        OlympianHadronColliderBlockEntity idle = collider(level, idlePos);
        tick(level, idle);
        LevelChunk idleChunk = level.getChunkAt(idlePos);
        idleChunk.setUnsaved(false);
        tick(level, idle);
        check(!idleChunk.isUnsaved(), "IDLE_TICK_STAYS_CLEAN");
        CompoundTag flash = idle.saveWithoutMetadata(server.registryAccess());
        flash.putInt("FlashTicks", 3);
        idle.loadWithComponents(flash, server.registryAccess());
        idleChunk.setUnsaved(false);
        tick(level, idle);
        check(idle.getCollisionFlashTicks() == 2 && idleChunk.isUnsaved(), "FLASH_COUNTDOWN_SAVED");

        checkMenus(server, level);
        Files.createDirectories(expected.getParent());
        NbtIo.writeCompressed(active.saveWithoutMetadata(server.registryAccess()), expected);
        chunk.setUnsaved(true); // The test cleared flags explicitly; ensure the test world is saved normally.
        server.saveEverything(false, true, true);
        check(Files.size(expected) > 0, "EXPECTED_STATE_WRITTEN");
    }

    private static void checkMenus(MinecraftServer server, ServerLevel level) {
        BlockPos pos = new BlockPos(100, 100, 100);
        level.setBlock(pos, ObsidiumMod.OLYMPIAN_HADRON_COLLIDER_BLOCK.defaultBlockState(), 3);
        OlympianHadronColliderBlockEntity collider = collider(level, pos);
        collider.setItem(0, new ItemStack(Items.IRON_INGOT));
        ServerPlayer player = player(server, level, pos);
        OlympianHadronColliderMenu menu = (OlympianHadronColliderMenu) collider.createMenu(1, player.getInventory(), player);
        check(menu.stillValid(player), "NEARBY_MENU_VALID");
        check(menu.clickMenuButton(player, 0), "NEARBY_BUTTON_ALLOWED");
        int mode = menu.getOperatingMode();
        player.setPos(pos.getX() + 20, pos.getY(), pos.getZ());
        check(!menu.stillValid(player), "DISTANT_MENU_INVALID");
        check(!menu.clickMenuButton(player, 0) && menu.getOperatingMode() == mode, "DISTANT_BUTTON_REJECTED");
        check(menu.quickMoveStack(player, 0).isEmpty() && collider.getItem(0).getCount() == 1, "DISTANT_SHIFT_CLICK_REJECTED");
        ServerPlayer otherDimension = player(server, server.getLevel(Level.NETHER), pos);
        check(!menu.stillValid(otherDimension), "OTHER_DIMENSION_INVALID");
        player.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        check(!new OlympianHadronColliderMenu(2, player.getInventory()).stillValid(player), "SERVER_PLACEHOLDER_INVALID");
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        check(!menu.stillValid(player) && !menu.clickMenuButton(player, 0), "REMOVED_BLOCK_INVALID");
        level.setBlock(pos, ObsidiumMod.OLYMPIAN_HADRON_COLLIDER_BLOCK.defaultBlockState(), 3);
        check(!menu.stillValid(player), "REPLACED_BLOCK_INVALID");
    }

    private static ServerPlayer player(MinecraftServer server, ServerLevel level, BlockPos pos) {
        ServerPlayer player = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), "ColliderAudit"),
                ClientInformation.createDefault());
        player.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return player;
    }

    private static OlympianHadronColliderBlockEntity collider(ServerLevel level, BlockPos pos) {
        return (OlympianHadronColliderBlockEntity) level.getBlockEntity(pos);
    }

    private static void tick(ServerLevel level, OlympianHadronColliderBlockEntity collider) {
        OlympianHadronColliderBlockEntity.tick(level, collider.getBlockPos(), collider.getBlockState(), collider);
    }

    private static void check(boolean success, String name) {
        if (!success) throw new AssertionError(name);
        System.out.println("COLLIDER_AUDIT_OK_" + name);
    }
}
