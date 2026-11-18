package net.valentin.obsidium;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.valentin.obsidium.block.*;
import net.valentin.obsidium.item.VolcaniteStorageRemoteItem;
import net.valentin.obsidium.config.ModConfig;

public final class RemoteRuntimeChecks implements ModInitializer {
    private static final class TestPlayer extends ServerPlayer {
        net.minecraft.world.inventory.AbstractContainerMenu opened;
        TestPlayer(net.minecraft.server.MinecraftServer server, ServerLevel level) {
            super(server, level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"RemoteChecks"), ClientInformation.createDefault());
        }
        @Override public java.util.OptionalInt openMenu(net.minecraft.world.MenuProvider provider) {
            opened = provider.createMenu(4, getInventory(), this);
            return java.util.OptionalInt.of(4);
        }
        @Override public void displayClientMessage(net.minecraft.network.chat.Component message, boolean overlay) {}
    }
    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
        System.out.println("REMOTE_OK_" + name);
    }
    private static ItemStack linked(ServerLevel level, BlockPos pos) {
        var stack = new ItemStack(ObsidiumMod.VOLCANITE_STORAGE_REMOTE);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putBoolean("Linked", true); tag.putString("Dim", level.dimension().location().toString());
            tag.putInt("X", pos.getX()); tag.putInt("Y", pos.getY()); tag.putInt("Z", pos.getZ());
        });
        return stack;
    }
    @Override public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            server.tickRateManager().setFrozen(true);
            var config = ModConfig.get(); int oldRange = config.remoteTerminalRange;
            boolean oldCross = config.remoteTerminalCrossDimension;
            try {
                var world = server.overworld(); var nether = server.getLevel(Level.NETHER);
                var pos = new BlockPos(32,100,32);
                world.setBlockAndUpdate(pos, ObsidiumMod.VOLCANITE_STORAGE_CONTROLLER_BLOCK.defaultBlockState());
                nether.setBlockAndUpdate(pos, ObsidiumMod.VOLCANITE_STORAGE_CONTROLLER_BLOCK.defaultBlockState());
                var controller = (VolcaniteStorageControllerBlockEntity) world.getBlockEntity(pos);
                var other = (VolcaniteStorageControllerBlockEntity) nether.getBlockEntity(pos);
                var player = new TestPlayer(server, world);
                var remote = linked(world,pos); player.getInventory().setItem(0,remote);
                player.setPos(232.5,100.5,32.5);
                config.remoteTerminalRange=256; config.remoteTerminalCrossDimension=false;
                check(VolcaniteStorageRemoteItem.resolveController(player,remote)==controller,"CONFIGURED_256_RANGE");
                var menu = ((VolcaniteStorageTerminalMenu)controller.createMenu(1,player.getInventory(),player)).withRemote(remote,player);
                check(menu.stillValid(player),"MENU_VALID_BEYOND_128");
                remote.getItem().use(world,player,net.minecraft.world.InteractionHand.MAIN_HAND);
                check(player.opened instanceof VolcaniteStorageTerminalMenu && player.opened.stillValid(player),"ITEM_USE_OPENS_REMOTE_SESSION");
                player.setPos(288.5,100.5,32.5);
                check(menu.stillValid(player),"EXACT_RANGE_BOUNDARY");
                player.setPos(288.6,100.5,32.5);
                check(!menu.stillValid(player),"OUTSIDE_RANGE_CLOSES");
                player.setPos(232.5,100.5,32.5); config.remoteTerminalRange=100;
                check(!menu.stillValid(player),"CONFIG_CHANGE_REVALIDATED");
                config.remoteTerminalRange=256;
                var local = (VolcaniteStorageTerminalMenu)controller.createMenu(2,player.getInventory(),player);
                check(!local.stillValid(player),"POSSESSION_DOES_NOT_EXTEND_LOCAL_MENU");
                player.getInventory().setItem(0,remote.copy());
                check(!menu.stillValid(player),"REPLACEMENT_REMOTE_INVALIDATES_SESSION");
                player.getInventory().setItem(0,remote);
                check(menu.stillValid(player),"ORIGINAL_REMOTE_RESTORED");
                CustomData.update(DataComponents.CUSTOM_DATA,remote,t->t.putString("Dim","INVALID DIMENSION!"));
                check(VolcaniteStorageRemoteItem.resolveController(player,remote)==null,"MALFORMED_DIMENSION_SAFE");
                remote=linked(nether,pos); player.getInventory().setItem(0,remote);
                check(VolcaniteStorageRemoteItem.resolveController(player,remote)==null,"CROSS_DIMENSION_DISABLED");
                config.remoteTerminalCrossDimension=true;
                check(VolcaniteStorageRemoteItem.resolveController(player,remote)==other,"CORRECT_DESTINATION_WORLD");
                var cross=((VolcaniteStorageTerminalMenu)other.createMenu(3,player.getInventory(),player)).withRemote(remote,player);
                check(cross.stillValid(player),"CROSS_DIMENSION_MENU_VALID");
                remote.getItem().use(world,player,net.minecraft.world.InteractionHand.MAIN_HAND);
                check(player.opened != null && player.opened.stillValid(player),"ITEM_USE_CROSS_DIMENSION");
                CustomData.update(DataComponents.CUSTOM_DATA,remote,t->t.putString("Dim",world.dimension().location().toString()));
                check(!cross.stillValid(player),"RELINK_INVALIDATES_SESSION");
                CustomData.update(DataComponents.CUSTOM_DATA,remote,t->t.putString("Dim",nether.dimension().location().toString()));
                config.remoteTerminalCrossDimension=false;
                check(!cross.stillValid(player),"CROSS_DIMENSION_CONFIG_REVALIDATED");
                config.remoteTerminalRange=Integer.MAX_VALUE;
                remote=linked(world,new BlockPos(2000000,100,2000000));
                check(VolcaniteStorageRemoteItem.resolveController(player,remote)==null,"UNLOADED_DESTINATION_REJECTED");
                check(!world.getChunkSource().hasChunk(2000000 >> 4, 2000000 >> 4),"NO_DESTINATION_CHUNK_LOAD");
                remote=linked(world,pos); player.getInventory().setItem(0,remote);
                world.setBlockAndUpdate(pos,Blocks.CHEST.defaultBlockState());
                check(VolcaniteStorageRemoteItem.resolveController(player,remote)==null,"OTHER_MENU_PROVIDER_REJECTED");
                check(!menu.stillValid(player),"REMOVED_CONTROLLER_INVALIDATES_MENU");
                check(VolcaniteStorageRemoteItem.resolveController(player,new ItemStack(ObsidiumMod.VOLCANITE_STORAGE_REMOTE))==null,"UNLINKED_REMOTE_REJECTED");
                remote=linked(world,pos); CustomData.update(DataComponents.CUSTOM_DATA,remote,t->t.remove("X"));
                check(VolcaniteStorageRemoteItem.resolveController(player,remote)==null,"INCOMPLETE_COORDINATES_REJECTED");
                System.out.println("REMOTE_ALL_PASSED");
            } catch(Throwable failure) {failure.printStackTrace();System.out.println("REMOTE_FAILED");}
            finally {config.remoteTerminalRange=oldRange;config.remoteTerminalCrossDimension=oldCross;server.halt(false);}
        });
    }
}
