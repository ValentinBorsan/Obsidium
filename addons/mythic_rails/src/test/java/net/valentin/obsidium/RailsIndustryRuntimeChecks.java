package net.valentin.obsidium;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.*;
import net.valentin.mythicrails.*;

public final class RailsIndustryRuntimeChecks implements ModInitializer {
    private int ticks;
    private static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);System.out.println("RAILS_INDUSTRY_OK_"+name);}
    private static void advance(AbstractMinecart cart,int n){for(int i=0;i<n;i++)cart.tick();}
    private static void clear(ServerLevel world){for(var e:world.getAllEntities())if(e instanceof AbstractMinecart)e.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);}
    private static Minecart regular(ServerLevel world,int x){var c=new Minecart(EntityType.MINECART,world);c.setPos(x+0.5,100.0625,32.5);world.addFreshEntity(c);return c;}
    private static HermesFreightCart engine(ServerLevel world){var e=new HermesFreightCart(MythicRails.FREIGHT_CART,world);e.setPos(36.5,100.0625,32.5);e.setHeading(Direction.EAST);e.setItem(HermesFreightCart.SLOT_MOTOR,new ItemStack(MythicRails.COAL_MOTOR_MODULE));world.addFreshEntity(e);return e;}
    private static int drops(ServerLevel world,Item item){return world.getEntitiesOfClass(ItemEntity.class,new AABB(30,90,28,64,115,48)).stream().filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum();}
    private static final class TestPlayer extends ServerPlayer {
        AbstractContainerMenu opened;
        TestPlayer(net.minecraft.server.MinecraftServer server,ServerLevel level){super(server,level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"RailsIndustry"),ClientInformation.createDefault());}
        @Override public java.util.OptionalInt openMenu(MenuProvider provider){opened=provider.createMenu(8,getInventory(),this);return java.util.OptionalInt.of(8);}
    }
    @Override public void onInitialize(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->server.overworld().setChunkForced(2,2,true));
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(++ticks!=20)return;server.tickRateManager().setFrozen(true);
            try{
                var world=server.overworld();
                for(int x=32;x<64;x++){
                    world.setBlockAndUpdate(new BlockPos(x,99,32),Blocks.STONE.defaultBlockState());
                    world.setBlockAndUpdate(new BlockPos(x,100,32),MythicRails.OBSIDIAN_RAIL.defaultBlockState().setValue(RailBlock.SHAPE,RailShape.EAST_WEST));
                }
                check(MythicRails.OBSIDIAN_RAIL.defaultBlockState().is(BlockTags.RAILS)&&MythicRails.EMBER_RAIL.defaultBlockState().is(BlockTags.RAILS),"CUSTOM_RAILS_REGISTERED_IN_VANILLA_TAG");
                var vanilla=regular(world,34);vanilla.setDeltaMovement(0.2,0,0);advance(vanilla,6);
                check(vanilla.getX()>35 && vanilla.isOnRails(),"VANILLA_CART_ON_CUSTOM_NORMAL_RAIL");
                clear(world);var freight=new HermesFreightCart(MythicRails.FREIGHT_CART,world);freight.setPos(34.5,100.0625,32.5);freight.setItem(53,new ItemStack(Items.DIAMOND,5));freight.setDeltaMovement(0.2,0,0);world.addFreshEntity(freight);advance(freight,6);
                check(freight.getX()>35 && freight.getItem(53).getCount()==5,"FREIGHT_CART_ON_CUSTOM_NORMAL_RAIL");clear(world);
                world.setBlockAndUpdate(new BlockPos(40,99,32),Blocks.REDSTONE_BLOCK.defaultBlockState());
                world.setBlockAndUpdate(new BlockPos(40,100,32),MythicRails.EMBER_RAIL.defaultBlockState().setValue(PoweredRailBlock.SHAPE,RailShape.EAST_WEST));
                check(world.getBlockState(new BlockPos(40,100,32)).getValue(PoweredRailBlock.POWERED),"CUSTOM_POWERED_RAIL_RESPONDS_TO_REDSTONE");
                vanilla=regular(world,40);vanilla.setDeltaMovement(0.1,0,0);vanilla.tick();double customSpeed=vanilla.getDeltaMovement().horizontalDistance();
                check(customSpeed>0.12,"CUSTOM_RAIL_ACCELERATES_VANILLA_CART");clear(world);
                world.setBlockAndUpdate(new BlockPos(52,99,32),Blocks.REDSTONE_BLOCK.defaultBlockState());world.setBlockAndUpdate(new BlockPos(52,100,32),Blocks.POWERED_RAIL.defaultBlockState().setValue(PoweredRailBlock.SHAPE,RailShape.EAST_WEST));
                vanilla=regular(world,52);vanilla.setDeltaMovement(0.1,0,0);vanilla.tick();
                check(Math.abs(customSpeed-vanilla.getDeltaMovement().horizontalDistance())<0.00001,"POWERED_ACCELERATION_MATCHES_VANILLA");clear(world);
                world.setBlockAndUpdate(new BlockPos(44,100,32),MythicRails.EMBER_RAIL.defaultBlockState().setValue(PoweredRailBlock.SHAPE,RailShape.EAST_WEST));
                vanilla=regular(world,44);vanilla.setDeltaMovement(0.2,0,0);vanilla.tick();check(vanilla.getDeltaMovement().horizontalDistance()<0.12,"UNPOWERED_CUSTOM_RAIL_BRAKES");clear(world);
                // Curves use the full vanilla rail shape set, not only the straight transport path.
                for(int z=50;z<=54;z++){
                    world.setBlockAndUpdate(new BlockPos(40,99,z),Blocks.STONE.defaultBlockState());
                    world.setBlockAndUpdate(new BlockPos(40,100,z),MythicRails.OBSIDIAN_RAIL.defaultBlockState());
                }
                for(int x=41;x<=45;x++){
                    world.setBlockAndUpdate(new BlockPos(x,99,50),Blocks.STONE.defaultBlockState());
                    world.setBlockAndUpdate(new BlockPos(x,100,50),MythicRails.OBSIDIAN_RAIL.defaultBlockState().setValue(RailBlock.SHAPE,RailShape.EAST_WEST));
                }
                world.setBlock(new BlockPos(40,100,50),MythicRails.OBSIDIAN_RAIL.defaultBlockState().setValue(RailBlock.SHAPE,RailShape.SOUTH_EAST),2);
                vanilla=regular(world,40);vanilla.setPos(40.5,100.0625,51.2);vanilla.setDeltaMovement(0,0,-0.2);advance(vanilla,15);
                check(vanilla.getX()>41 && Math.abs(vanilla.getZ()-50.5)<0.2,"CUSTOM_CURVED_RAIL_TURNS_VANILLA_CART");clear(world);
                check(HermesFreightCart.acceptsFuel(new ItemStack(Items.COAL))&&!HermesFreightCart.acceptsFuel(new ItemStack(Items.DIAMOND)),"SOLID_FUEL_RULES");
                check(!HermesFreightCart.acceptsFuel(new ItemStack(Items.LAVA_BUCKET)),"FUEL_CONTAINER_REJECTED_WITHOUT_BUCKET_LOSS");
                var engine=engine(world);engine.setItem(HermesFreightCart.SLOT_FUEL,new ItemStack(Items.COAL,3));engine.tick();
                check(engine.fuelTicks()==0 && engine.getItem(HermesFreightCart.SLOT_FUEL).getCount()==3,"MOTOR_OFF_DOES_NOT_CONSUME_FUEL");
                engine.setRunning(true);engine.tick();check(engine.fuelTicks()==1599 && engine.getItem(HermesFreightCart.SLOT_FUEL).getCount()==2,"ONE_COAL_STARTS_ONE_BURN");
                double start=engine.getX();advance(engine,10);check(engine.getX()>start+1 && engine.isOnRails(),"FUELED_LOCOMOTIVE_SELF_PROPULSION");
                engine.setRunning(false);int remaining=engine.fuelTicks();advance(engine,4);check(engine.fuelTicks()==remaining,"PAUSED_MOTOR_PRESERVES_BURN_RESERVE");
                engine.getItem(HermesFreightCart.SLOT_FUEL).set(DataComponents.CUSTOM_NAME,Component.literal("Engine Coal"));
                var tag=new CompoundTag();engine.save(tag);var restored=EntityType.loadEntityRecursive(tag,world,e->e);
                check(restored instanceof HermesFreightCart,"LOCOMOTIVE_CUSTOM_TYPE_RELOAD");
                var copy=(HermesFreightCart)restored;
                check(copy.fuelTicks()==remaining && !copy.running() && copy.heading()==Direction.EAST,"ENGINE_STATE_PERSISTS");
                check(copy.getItem(HermesFreightCart.SLOT_FUEL).getCount()==2 && copy.getItem(HermesFreightCart.SLOT_FUEL).getHoverName().getString().equals("Engine Coal"),"FUEL_COUNT_AND_COMPONENTS_PERSIST");
                var player=new TestPlayer(server,world);player.setPos(engine.getX(),100.5,34);
                var menu=(FreightCartMenu)engine.createMenu(1,player.getInventory(),player);
                check(menu.slots.size()==94 && menu.getType()==MythicRails.FREIGHT_MENU,"LOCOMOTIVE_FUEL_GUI");
                check(MythicRails.FREIGHT_MENU.create(2,player.getInventory()).slots.size()==94,"LOCOMOTIVE_CLIENT_SLOT_PARITY");
                check(menu.clickMenuButton(player,2) && engine.heading()==Direction.SOUTH,"ENGINE_DIRECTION_BUTTON");
                check(menu.clickMenuButton(player,0) && engine.running(),"ENGINE_START_BUTTON");
                check(!menu.clickMenuButton(player,1),"RUNNING_ENGINE_DIRECTION_LOCK");
                player.setPos(500,100,500);check(!menu.clickMenuButton(player,0),"REMOTE_ENGINE_CONTROL_REJECTED");
                clear(world);engine=engine(world);engine.setPos(36.5,110,32.5);engine.setItem(HermesFreightCart.SLOT_FUEL,new ItemStack(Items.COAL,2));engine.setRunning(true);engine.tick();
                check(engine.fuelTicks()==0&&engine.getItem(HermesFreightCart.SLOT_FUEL).getCount()==2,"OFF_RAIL_ENGINE_DOES_NOT_BURN");
                clear(world);engine=engine(world);engine.setItem(HermesFreightCart.SLOT_FUEL,new ItemStack(Items.DIAMOND,3));engine.setRunning(true);advance(engine,5);
                check(engine.fuelTicks()==0&&engine.getItem(HermesFreightCart.SLOT_FUEL).getCount()==3,"INVALID_FUEL_NOT_DESTROYED");
                clear(world);engine=engine(world);engine.setItem(HermesFreightCart.SLOT_FUEL,new ItemStack(Items.COAL,5));int coal=drops(world,Items.COAL),cartDrops=drops(world,MythicRails.FREIGHT_CART_ITEM);
                engine.hurt(world.damageSources().generic(),100);
                check(engine.isRemoved()&&drops(world,Items.COAL)-coal==5,"ENGINE_DESTRUCTION_DROPS_FUEL_ONCE");
                check(drops(world,MythicRails.FREIGHT_CART_ITEM)-cartDrops==1,"ENGINE_DESTRUCTION_DROPS_CUSTOM_ITEM");
                clear(world);
                var pos=new BlockPos(40,99,40);world.setBlockAndUpdate(pos,MythicRails.LOADING_STATION.defaultBlockState().setValue(FreightStationBlock.FACING,Direction.EAST));world.setBlockAndUpdate(pos.above(),MythicRails.OBSIDIAN_RAIL.defaultBlockState().setValue(RailBlock.SHAPE,RailShape.EAST_WEST));
                world.setBlockAndUpdate(pos.north(),Blocks.CHEST.defaultBlockState());var source=(ChestBlockEntity)world.getBlockEntity(pos.north());source.setItem(0,new ItemStack(Items.IRON_INGOT,20));
                player.setPos(40.5,100,42);var stationMenu=new FreightStationMenu(3,player.getInventory(),world,pos);
                check(stationMenu.slots.size()==45&&stationMenu.stillValid(player),"STATION_MENU_EXPOSES_NINE_BUFFER_SLOTS");
                check(stationMenu.clickMenuButton(player,1)&&stationMenu.clickMenuButton(player,2),"EMPTY_STATION_ROTATION");
                check(world.getBlockState(pos).getValue(FreightStationBlock.FACING)==Direction.EAST,"STATION_DIRECTION_ROUND_TRIP");
                check(stationMenu.clickMenuButton(player,0)&&!world.getBlockState(pos).getValue(FreightStationBlock.ENABLED),"STATION_PAUSE_CONTROL");
                freight=new HermesFreightCart(MythicRails.FREIGHT_CART,world);freight.setPos(40.5,100.0625,40.5);world.addFreshEntity(freight);advance(freight,12);
                check(freight.isEmpty()&&source.getItem(0).getCount()==20&&freight.getDockStatus()==3,"PAUSED_STATION_HOLDS_WITHOUT_TRANSFER");
                check(stationMenu.clickMenuButton(player,1)&&stationMenu.clickMenuButton(player,2),"PAUSED_OCCUPIED_STATION_CAN_BE_ALIGNED");
                stationMenu.clickMenuButton(player,0);advance(freight,4);
                check(!freight.isEmpty()&&source.getItem(0).getCount()<20,"STATION_RESUME_TRANSFERS");
                check(stationMenu.value(4)==1&&stationMenu.value(6)==1,"STATION_LIVE_CARGO_DATA");
                check(stationMenu.clickMenuButton(player,3)&&freight.getDockStatus()==7,"MANUAL_RELEASE_CONTROL");
                check(!stationMenu.clickMenuButton(player,3),"MANUAL_RELEASE_IDEMPOTENT");
                player.setPos(500,100,500);check(!stationMenu.clickMenuButton(player,0),"REMOTE_STATION_CONTROL_REJECTED");
                player.setPos(40.5,100,42);world.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());check(!stationMenu.stillValid(player),"REMOVED_STATION_INVALIDATES_GUI");
                for(String recipe:new String[]{"obsidian_rail","ember_powered_rail","ember_locomotive"})check(server.getRecipeManager().byKey(MythicRails.id(recipe)).isPresent(),"RECIPE_LOADED_"+recipe);
                System.out.println("RAILS_INDUSTRY_ALL_PASSED");
            }catch(Throwable t){t.printStackTrace();System.out.println("RAILS_INDUSTRY_FAILED");}
            finally{server.halt(false);}
        });
    }
}
