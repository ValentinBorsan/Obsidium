package net.valentin.obsidium;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.valentin.mythicrails.*;
import java.nio.file.*;
import java.util.UUID;

public final class RailStationsRuntimeChecks implements ModInitializer {
    private int ticks;
    private static final BlockPos DOCK = new BlockPos(40,99,40);
    private static void check(boolean ok,String name) {
        if(!ok)throw new AssertionError(name);
        System.out.println("RAIL_STATIONS_OK_"+name);
    }
    private static void advance(HermesFreightCart cart,int ticks) {for(int i=0;i<ticks;i++)cart.tick();}
    private static int count(Container inventory,Item item) {
        int n=0;for(int i=0;i<inventory.getContainerSize();i++)if(inventory.getItem(i).is(item))n+=inventory.getItem(i).getCount();return n;
    }
    private static CompoundTag saved(HermesFreightCart cart) {var tag=new CompoundTag();cart.save(tag);return tag;}
    private static void clearCarts(ServerLevel level) {
        for(var entity:level.getAllEntities()) if(entity instanceof HermesFreightCart)entity.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
    }
    private static Container setup(ServerLevel world,BlockPos pos,boolean loading,Direction facing) {
        world.setBlockAndUpdate(pos, (loading?MythicRails.LOADING_STATION:MythicRails.UNLOADING_STATION)
                .defaultBlockState().setValue(FreightStationBlock.FACING,facing));
        world.setBlockAndUpdate(pos.above(),Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE,
                facing.getAxis()==Direction.Axis.X?RailShape.EAST_WEST:RailShape.NORTH_SOUTH));
        var port=pos.relative(facing.getCounterClockWise());
        world.setBlockAndUpdate(port,Blocks.CHEST.defaultBlockState());
        var chest=(ChestBlockEntity)world.getBlockEntity(port);chest.clearContent();
        ((FreightStationBlockEntity)world.getBlockEntity(pos)).clearContent();return chest;
    }
    private static HermesFreightCart cart(ServerLevel world,BlockPos pos) {
        var cart=new HermesFreightCart(MythicRails.FREIGHT_CART,world);
        cart.setPos(pos.getX()+0.35,pos.getY()+1.0625,pos.getZ()+0.5);cart.setDeltaMovement(0.3,0,0);
        world.addFreshEntity(cart);return cart;
    }
    private static HermesFreightCart reload(ServerLevel world,HermesFreightCart cart) {
        var tag=saved(cart);cart.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        var result=(HermesFreightCart)EntityType.loadEntityRecursive(tag,world,e->e);
        result.setUUID(UUID.randomUUID());world.addFreshEntity(result);return result;
    }
    @Override public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            server.overworld().setChunkForced(2,2,true);
            server.overworld().setChunkForced(3,2,true);
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(++ticks!=20)return;
            server.tickRateManager().setFrozen(true);
            try {
                var world=server.overworld();
                var marker=server.getWorldPath(LevelResource.ROOT).resolve("mythic-rails-station-restart.dat");
                if(Files.exists(marker)) {
                    var data=NbtIo.readCompressed(marker,NbtAccounter.unlimitedHeap());
                    var blocked=(HermesFreightCart)world.getEntity(data.getUUID("Blocked"));
                    var released=(HermesFreightCart)world.getEntity(data.getUUID("Released"));
                    check(blocked!=null && released!=null,"ENTITIES_RESTORED_AFTER_REAL_SERVER_RESTART");
                    check(count(blocked,Items.DIAMOND)==17 && blocked.getItem(53).getCount()==17,"BLOCKED_CARGO_SURVIVES_RESTART");
                    check(blocked.getItem(53).getHoverName().getString().equals("Restart Cargo"),"COMPONENTS_SURVIVE_RESTART");
                    check(Math.abs(blocked.getX()-40.5)<0.01,"BLOCKED_CART_STILL_DOCKED_AFTER_RESTART");
                    check(count((Container)world.getBlockEntity(DOCK.north()),Items.COBBLESTONE)==27*64,"FULL_DESTINATION_UNCHANGED_AFTER_RESTART");
                    check(released.getX()>53.5,"RELEASED_CART_CONTINUES_AFTER_RESTART");
                    check(!world.getBlockState(new BlockPos(44,99,40)).getValue(FreightStationBlock.ENABLED),"STATION_PAUSE_SURVIVES_SERVER_RESTART");
                    var engine=(HermesFreightCart)world.getEntity(data.getUUID("Engine"));
                    check(engine!=null && engine.fuelTicks()==data.getInt("EngineFuelTicks") && engine.getItem(HermesFreightCart.SLOT_FUEL).getCount()==2,"LOCOMOTIVE_FUEL_SURVIVES_SERVER_RESTART");
                    check(engine.getItem(HermesFreightCart.SLOT_FUEL).getHoverName().getString().equals("Restart Engine Coal"),"LOCOMOTIVE_FUEL_COMPONENTS_SURVIVE_RESTART");
                    check(!engine.running() && engine.heading()==Direction.EAST,"LOCOMOTIVE_CONTROLS_SURVIVE_RESTART");
                    int reserve=engine.fuelTicks();engine.setRunning(true);for(int i=0;i<8;i++)engine.tick();
                    check(engine.fuelTicks()==reserve-8 && engine.getItem(HermesFreightCart.SLOT_FUEL).getCount()==2 && engine.getX()>36.5,"LOCOMOTIVE_RESTART_RESUMES_EXISTING_BURN");
                    var sink=(Container)world.getBlockEntity(DOCK.north());sink.clearContent();
                    var savedBuffer=(FreightStationBlockEntity)world.getBlockEntity(DOCK);
                    check(count(savedBuffer,Items.COBBLESTONE)==9*64,"STATION_BUFFER_SURVIVES_RESTART");savedBuffer.clearContent();
                    advance(blocked,80);
                    check(count(sink,Items.DIAMOND)==17 && blocked.isEmpty(),"RESTARTED_CART_UNLOADS_EXACT_CARGO");
                    check(blocked.getX()>41,"RESTARTED_CART_DEPARTS");
                    System.out.println("RAIL_STATIONS_ALL_PASSED");return;
                }
                for(int x=35;x<=63;x++){
                    world.setBlockAndUpdate(new BlockPos(x,99,40),Blocks.STONE.defaultBlockState());
                    world.setBlockAndUpdate(new BlockPos(x,100,40),Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE,RailShape.EAST_WEST));
                }
                var source=setup(world,DOCK,true,Direction.EAST);
                var named=new ItemStack(Items.IRON_INGOT,29);named.set(DataComponents.CUSTOM_NAME,Component.literal("Station Cargo"));
                source.setItem(0,named);var cart=cart(world,DOCK);cart.tick();
                check(Math.abs(cart.getX()-40.5)<0.001 && cart.getDeltaMovement().equals(Vec3.ZERO),"ARRIVAL_CENTERS_AND_STOPS");
                check(count(cart,Items.IRON_INGOT)==8 && count(source,Items.IRON_INGOT)==21,"ATOMIC_FIRST_BATCH_OF_EIGHT");
                check(cart.getItem(0).getHoverName().getString().equals("Station Cargo"),"TRANSFER_PRESERVES_COMPONENTS");
                advance(cart,3);check(count(cart,Items.IRON_INGOT)==8,"TRANSFER_INTERVAL_ENFORCED");
                cart=reload(world,cart);advance(cart,80);
                check(count(cart,Items.IRON_INGOT)==29 && source.isEmpty(),"PARTIAL_TRANSFER_RESUMES_WITHOUT_DUPLICATION");
                check(cart.getX()>41,"LOADER_DEPARTS_WHEN_SOURCE_EMPTY");
                clearCarts(world);source=setup(world,DOCK,true,Direction.EAST);source.setItem(0,new ItemStack(Items.GOLD_INGOT,10));
                cart=cart(world,DOCK);for(int i=0;i<54;i++)cart.setItem(i,new ItemStack(Items.COBBLESTONE,64));
                advance(cart,40);check(cart.getX()>41 && count(source,Items.GOLD_INGOT)+count((Container)world.getBlockEntity(DOCK),Items.GOLD_INGOT)==10,"FULL_LOADER_DEPARTS_WITHOUT_CONSUMING_SOURCE");

                clearCarts(world);var sink=setup(world,DOCK,false,Direction.EAST);
                for(int i=0;i<27;i++)sink.setItem(i,new ItemStack(Items.COBBLESTONE,64));
                var buffer=(FreightStationBlockEntity)world.getBlockEntity(DOCK);for(int i=0;i<9;i++)buffer.setItem(i,new ItemStack(Items.COBBLESTONE,64));
                cart=cart(world,DOCK);cart.setItem(53,new ItemStack(Items.DIAMOND,17));advance(cart,60);
                check(Math.abs(cart.getX()-40.5)<0.01 && count(cart,Items.DIAMOND)==17,"FULL_SINK_HOLDS_CART_WITHOUT_LOSS");
                cart=reload(world,cart);advance(cart,16);
                check(Math.abs(cart.getX()-40.5)<0.01 && count(cart,Items.DIAMOND)==17,"BLOCKED_SESSION_PERSISTS_NBT");
                sink.clearContent();buffer.clearContent();advance(cart,80);
                check(count(sink,Items.DIAMOND)==17 && cart.isEmpty(),"UNBLOCKED_SINK_RECEIVES_EXACT_CARGO");
                check(cart.getX()>41,"EMPTY_UNLOADER_DEPARTS");

                clearCarts(world);sink=setup(world,DOCK,false,Direction.EAST);
                world.setBlockAndUpdate(DOCK.south(),Blocks.REDSTONE_BLOCK.defaultBlockState());
                cart=cart(world,DOCK);cart.setItem(0,new ItemStack(Items.REDSTONE,12));advance(cart,80);
                check(cart.isEmpty() && count(sink,Items.REDSTONE)==12,"REDSTONE_HOLD_ALLOWS_TRANSFERS");
                check(Math.abs(cart.getX()-40.5)<0.01,"REDSTONE_PREVENTS_DEPARTURE");
                world.setBlockAndUpdate(DOCK.south(),Blocks.AIR.defaultBlockState());advance(cart,30);
                check(cart.getX()>41,"REMOVING_SIGNAL_RELEASES_CART");

                clearCarts(world);setup(world,DOCK,false,Direction.EAST);
                world.setBlockAndUpdate(DOCK.north(),Blocks.AIR.defaultBlockState());
                cart=cart(world,DOCK);cart.setItem(0,new ItemStack(Items.DIAMOND,7));advance(cart,60);
                check(cart.isEmpty() && count((Container)world.getBlockEntity(DOCK),Items.DIAMOND)==7,"INTERNAL_BUFFER_WORKS_WITHOUT_EXTERNAL_CONTAINER");
                world.setBlockAndUpdate(DOCK.north(),Blocks.FURNACE.defaultBlockState());
                var standaloneBuffer=(FreightStationBlockEntity)world.getBlockEntity(DOCK);
                FreightStationBlockEntity.serverTick(world,DOCK,world.getBlockState(DOCK),standaloneBuffer);
                check(count(standaloneBuffer,Items.DIAMOND)==7,"SIDED_FURNACE_REJECTS_INVALID_FUEL");
                check(((Container)world.getBlockEntity(DOCK.north())).isEmpty(),"SIDED_REJECTION_PRESERVES_DESTINATION");

                clearCarts(world);source=setup(world,DOCK,true,Direction.WEST);source.setItem(0,new ItemStack(Items.COPPER_INGOT,9));
                cart=cart(world,DOCK);advance(cart,60);
                check(count(cart,Items.COPPER_INGOT)==9 && cart.getX()<39,"WEST_PORT_AND_DEPARTURE_DIRECTION");
                clearCarts(world);source=setup(world,DOCK,true,Direction.EAST);
                world.setBlockAndUpdate(DOCK.above(),Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE,RailShape.NORTH_SOUTH));
                cart=cart(world,DOCK);cart.setDeltaMovement(0,0,0.2);cart.tick();
                check(cart.getDockStatus()==8 && saved(cart).contains("MythicRailsDock"),"MISALIGNED_RAIL_STOPS_WITH_CONFIGURATION_STATUS");
                clearCarts(world);source=setup(world,DOCK,true,Direction.EAST);source.setItem(0,new ItemStack(Items.IRON_INGOT,6));
                sink=setup(world,new BlockPos(56,99,40),false,Direction.EAST);
                for(int x:new int[]{36,37,44,45,46,47,48,49,50,51,52,58,59,60}){
                    world.setBlockAndUpdate(new BlockPos(x,99,40),Blocks.REDSTONE_BLOCK.defaultBlockState());
                    world.setBlockAndUpdate(new BlockPos(x,100,40),Blocks.POWERED_RAIL.defaultBlockState()
                            .setValue(PoweredRailBlock.SHAPE,RailShape.EAST_WEST).setValue(PoweredRailBlock.POWERED,true));
                }
                cart=cart(world,DOCK);advance(cart,250);
                check(source.isEmpty() && count(sink,Items.IRON_INGOT)==6 && cart.isEmpty(),"END_TO_END_LOADING_TRAVEL_UNLOADING");
                check(cart.getX()>57,"END_TO_END_AUTOMATIC_DEPARTURE");
                check(server.getRecipeManager().byKey(MythicRails.id("hermes_loading_station")).isPresent()
                        && server.getRecipeManager().byKey(MythicRails.id("hermes_unloading_station")).isPresent(),"STATION_RECIPES_LOADED");

                clearCarts(world);
                var edge = new BlockPos(1000015,99,1000008);
                // Place directly in the loaded section: vanilla rail placement itself probes neighbor chunks.
                // This isolates whether the station transfer attempts to load its adjacent inventory.
                var section=world.getChunkAt(edge).getSection(world.getChunkAt(edge).getSectionIndex(edge.getY()));
                section.setBlockState(edge.getX() & 15,edge.getY() & 15,edge.getZ() & 15,
                        MythicRails.LOADING_STATION.defaultBlockState().setValue(FreightStationBlock.FACING,Direction.SOUTH));
                section.setBlockState(edge.getX() & 15,(edge.getY()+1) & 15,edge.getZ() & 15,
                        Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE,RailShape.NORTH_SOUTH));
                world.setBlockEntity(new FreightStationBlockEntity(edge,world.getBlockState(edge)));
                check(!world.getChunkSource().hasChunk((edge.getX() + 1) >> 4, edge.getZ() >> 4),"DESTINATION_CHUNK_STARTS_UNLOADED");
                cart=cart(world,edge);cart.setItem(0,new ItemStack(Items.GOLD_INGOT,7));advance(cart,30);
                check(!world.getChunkSource().hasChunk((edge.getX() + 1) >> 4, edge.getZ() >> 4),"STATION_DOES_NOT_FORCE_LOAD_ADJACENT_CHUNK");
                check(count(cart,Items.GOLD_INGOT)==7 && saved(cart).getCompound("MythicRailsDock").getBoolean("Released")==false,
                        "UNLOADED_INVENTORY_HOLDS_CARGO");
                // Prepare a real disk save and restart: one blocked cart and one just released on its station.
                clearCarts(world);
                for(int x=35;x<=63;x++){
                    world.setBlockAndUpdate(new BlockPos(x,99,40),Blocks.STONE.defaultBlockState());
                    world.setBlockAndUpdate(new BlockPos(x,100,40),Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE,RailShape.EAST_WEST));
                }
                sink=setup(world,DOCK,false,Direction.EAST);for(int i=0;i<27;i++)sink.setItem(i,new ItemStack(Items.COBBLESTONE,64));
                buffer=(FreightStationBlockEntity)world.getBlockEntity(DOCK);for(int i=0;i<9;i++)buffer.setItem(i,new ItemStack(Items.COBBLESTONE,64));
                cart=cart(world,DOCK);var cargo=new ItemStack(Items.DIAMOND,17);
                cargo.set(DataComponents.CUSTOM_NAME,Component.literal("Restart Cargo"));cart.setItem(53,cargo);advance(cart,10);
                var releasedPos=new BlockPos(52,99,40);
                setup(world,releasedPos,true,Direction.EAST);var releasedCart=cart(world,releasedPos);
                for(int i=0;i<40 && !saved(releasedCart).getCompound("MythicRailsDock").getBoolean("Released");i++)releasedCart.tick();
                check(saved(releasedCart).getCompound("MythicRailsDock").getBoolean("Released"),"RELEASE_STATE_READY_FOR_DISK_SAVE");
                var data=new CompoundTag();data.putUUID("Blocked",cart.getUUID());data.putUUID("Released",releasedCart.getUUID());
                var pausedPos=new BlockPos(44,99,40);setup(world,pausedPos,true,Direction.EAST);
                world.setBlockAndUpdate(pausedPos,world.getBlockState(pausedPos).setValue(FreightStationBlock.ENABLED,false));
                var engine=new HermesFreightCart(MythicRails.FREIGHT_CART,world);
                engine.setPos(36.5,100.0625,40.5);engine.setHeading(Direction.EAST);
                engine.setItem(HermesFreightCart.SLOT_MOTOR,new ItemStack(MythicRails.COAL_MOTOR_MODULE));
                var engineCoal=new ItemStack(Items.COAL,3);engineCoal.set(DataComponents.CUSTOM_NAME,Component.literal("Restart Engine Coal"));engine.setItem(HermesFreightCart.SLOT_FUEL,engineCoal);
                world.addFreshEntity(engine);engine.setRunning(true);engine.tick();engine.setRunning(false);
                data.putUUID("Engine",engine.getUUID());data.putInt("EngineFuelTicks",engine.fuelTicks());
                NbtIo.writeCompressed(data,marker);
                System.out.println("RAIL_STATIONS_RESTART_READY");
            }catch(Throwable t){t.printStackTrace();System.out.println("RAIL_STATIONS_FAILED");}
            finally{server.halt(false);}
        });
    }
}
