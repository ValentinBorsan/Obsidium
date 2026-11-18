package net.valentin.obsidium;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.LevelResource;
import net.valentin.mythicrails.*;
import java.nio.file.*;

public final class TrainRuntimeChecks implements ModInitializer {
    private int ticks;

    private static final class TestPlayer extends ServerPlayer {
        TestPlayer(net.minecraft.server.MinecraftServer server, ServerLevel level) {
            super(server, level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "TrainChecks"), ClientInformation.createDefault());
        }
        @Override public void displayClientMessage(Component text, boolean overlay) {}
    }

    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
        System.out.println("RAIL_TRAINS_OK_" + name);
    }

    private static int amount(Container inventory, Item item) {
        int n = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(item)) n += inventory.getItem(i).getCount();
        }
        return n;
    }

    private static void clearVehicles(ServerLevel level) {
        for (var e : level.getAllEntities()) if (e instanceof AbstractMinecart) e.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
    }

    private static void line(ServerLevel level, int z) {
        for (int x = 32; x < 64; x++) {
            level.setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(x, 100, z), MythicRails.OBSIDIAN_RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
        }
    }

    private static FreightStationBlockEntity station(ServerLevel level, int x, int z, boolean load) {
        var pos = new BlockPos(x, 99, z);
        level.setBlockAndUpdate(pos, (load ? MythicRails.LOADING_STATION : MythicRails.UNLOADING_STATION).defaultBlockState().setValue(FreightStationBlock.FACING, Direction.EAST));
        level.setBlockAndUpdate(pos.above(), MythicRails.OBSIDIAN_RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
        var result = (FreightStationBlockEntity) level.getBlockEntity(pos);
        result.clearContent();
        return result;
    }

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            server.overworld().setChunkForced(2, 2, true);
            server.overworld().setChunkForced(3, 2, true);
            server.overworld().setChunkForced(2, 3, true);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks != 20) return;
            server.tickRateManager().setFrozen(true);
            try {
                var level = server.overworld();
                var marker = server.getWorldPath(LevelResource.ROOT).resolve("train-restart.dat");
                if (Files.exists(marker)) {
                    var data = NbtIo.readCompressed(marker, NbtAccounter.unlimitedHeap());
                    var cart = (HermesFreightCart) level.getEntity(data.getUUID("Cart"));
                    check(cart != null && cart.fuelTicks() == data.getInt("Fuel"), "MODULAR_CART_PERSISTS");
                    System.out.println("RAIL_TRAINS_ALL_PASSED");
                    return;
                }

                line(level, 32);
                line(level, 40);
                var player = new TestPlayer(server, level);
                player.setPos(40, 100, 42);

                var buffer = station(level, 40, 40, true);
                check(buffer.getContainerSize() == 9, "STATIONS_HAVE_NINE_REAL_SLOTS");
                var menu = new FreightStationMenu(1, player.getInventory(), level, buffer.getBlockPos());
                check(menu.slots.size() == 45 && MythicRails.STATION_MENU.create(2, player.getInventory()).slots.size() == 45, "STATION_CLIENT_SERVER_SLOT_PARITY");

                var apiStorage = ItemStorage.SIDED.find(level, buffer.getBlockPos(), Direction.SOUTH);
                check(apiStorage != null, "STATION_AUTOMATION_STORAGE_REGISTERED");
                try (var tx = Transaction.openOuter()) {
                    check(apiStorage.insert(ItemVariant.of(Items.GOLD_INGOT), 13, tx) == 13, "AUTOMATION_INSERTS_INTO_REAL_BUFFER");
                    tx.commit();
                }
                buffer.clearContent();

                // Test 1: Coal Motor self-propelled cart
                var cart = new HermesFreightCart(MythicRails.FREIGHT_CART, level);
                cart.setPos(36.5, 100.0625, 32.5);
                cart.setHeading(Direction.EAST);
                cart.setItem(HermesFreightCart.SLOT_MOTOR, new ItemStack(MythicRails.COAL_MOTOR_MODULE));
                cart.setItem(HermesFreightCart.SLOT_MODULE, new ItemStack(MythicRails.CARGO_MODULE));
                cart.setItem(HermesFreightCart.SLOT_FUEL, new ItemStack(Items.COAL, 4));
                level.addFreshEntity(cart);
                cart.setRunning(true);
                for (int i = 0; i < 30; i++) cart.tick();
                check(cart.getX() > 40 && cart.fuelTicks() > 0, "COAL_MOTOR_CART_PROPELS_SELF");
                clearVehicles(level);

                // Test 2: Electric Motor + Energy Cell + Liquid Barrel
                var eCart = new HermesFreightCart(MythicRails.FREIGHT_CART, level);
                eCart.setPos(36.5, 100.0625, 32.5);
                eCart.setHeading(Direction.EAST);
                eCart.setItem(HermesFreightCart.SLOT_MOTOR, new ItemStack(MythicRails.ELECTRIC_MOTOR_MODULE));
                eCart.setItem(HermesFreightCart.SLOT_MODULE, new ItemStack(MythicRails.LIQUID_BARREL_MODULE));
                eCart.setItem(HermesFreightCart.SLOT_AUX, new ItemStack(Items.WATER_BUCKET));
                level.addFreshEntity(eCart);
                eCart.tick();
                check(eCart.fluidAmount() == 1000 && eCart.fluidType() == 1 && eCart.getItem(HermesFreightCart.SLOT_AUX).is(Items.BUCKET),
                        "LIQUID_BARREL_DRAINS_BUCKET");
                clearVehicles(level);

                // Test 3: Smelter & Grinder
                var sCart = new HermesFreightCart(MythicRails.FREIGHT_CART, level);
                sCart.setPos(36.5, 100.0625, 32.5);
                sCart.setItem(HermesFreightCart.SLOT_MOTOR, new ItemStack(MythicRails.COAL_MOTOR_MODULE));
                sCart.setItem(HermesFreightCart.SLOT_MODULE, new ItemStack(MythicRails.GRINDER_MODULE));
                sCart.setItem(HermesFreightCart.SLOT_FUEL, new ItemStack(Items.COAL, 4));
                sCart.setItem(0, new ItemStack(Items.IRON_ORE, 1));
                level.addFreshEntity(sCart);
                for (int i = 0; i < 25; i++) sCart.tick();
                check(amount(sCart, Items.RAW_IRON) == 2, "GRINDER_MODULE_CRUSHES_ORE");
                clearVehicles(level);

                System.out.println("RAIL_TRAINS_ALL_PASSED");
            } catch (Throwable failure) {
                failure.printStackTrace();
                System.out.println("RAIL_TRAINS_FAILED");
            } finally {
                server.halt(false);
            }
        });
    }
}
