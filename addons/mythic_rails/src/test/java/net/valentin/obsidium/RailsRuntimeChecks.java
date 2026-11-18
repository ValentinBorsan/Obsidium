package net.valentin.obsidium;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.*;
import net.valentin.mythicrails.*;

public final class RailsRuntimeChecks implements ModInitializer {
    private int ticks;
    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
        System.out.println("RAILS_OK_" + name);
    }
    private static int dropped(ServerLevel world, Item item) {
        return world.getEntitiesOfClass(ItemEntity.class, new AABB(0,80,0,100,120,100)).stream()
                .filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum();
    }
    @Override public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> server.overworld().setChunkForced(2,2,true));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks != 20) return;
            server.tickRateManager().setFrozen(true);
            try {
                var world=server.overworld();
                for(int x=32;x<=63;x++){
                    world.setBlockAndUpdate(new BlockPos(x,99,32),Blocks.STONE.defaultBlockState());
                    world.setBlockAndUpdate(new BlockPos(x,100,32),Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE,RailShape.EAST_WEST));
                }
                var cart=new HermesFreightCart(MythicRails.FREIGHT_CART,world);
                check(cart.getContainerSize()==54 && cart.getItemStacks().size()==54,"ALL_54_SLOTS_ALLOCATED");
                cart.setItem(53,new ItemStack(Items.DIAMOND,17));
                check(cart.getItem(53).getCount()==17,"LAST_SLOT_ACCESSIBLE");
                var named=new ItemStack(Items.IRON_INGOT,23); named.set(DataComponents.CUSTOM_NAME,Component.literal("Cargo A"));
                cart.setItem(0,named); cart.setCustomName(Component.literal("Hermes Test"));
                cart.setPos(40.5,100.0625,32.5);
                world.addFreshEntity(cart);
                var save=new CompoundTag();check(cart.save(save),"ENTITY_SAVED_WITH_CUSTOM_TYPE");
                var restored=EntityType.loadEntityRecursive(save,world,e->e);
                check(restored instanceof HermesFreightCart,"CUSTOM_TYPE_RELOADED");
                var loaded=(HermesFreightCart)restored;
                check(loaded.getContainerSize()==HermesFreightCart.TOTAL_SLOTS && loaded.getItem(53).getCount()==17,"LAST_SLOT_PERSISTS");
                check(ItemStack.isSameItemSameComponents(named,loaded.getItem(0)) && loaded.getItem(0).getCount()==23,"CARGO_COMPONENTS_PERSIST");
                check(loaded.getName().getString().equals("Hermes Test"),"CART_NAME_PERSISTS");
                var player=new ServerPlayer(server,world,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"RailsChecks"),ClientInformation.createDefault());
                player.setPos(40.5,100.5,33);
                var menu=cart.createMenu(1,player.getInventory());
                check(menu.slots.size()==94,"SIX_ROW_MENU");
                check(menu.getType()==MythicRails.FREIGHT_MENU,"CUSTOM_MENU_TYPE");
                var clientMenu=MythicRails.FREIGHT_MENU.create(2,player.getInventory());
                check(clientMenu.slots.size()==94,"CLIENT_MENU_SLOT_PARITY");
                check(cart.getDefaultDisplayBlockState().is(MythicRails.CARGO_DISPLAY),"CUSTOM_CARGO_MODEL");
                check(menu.stillValid(player),"MENU_VALID_NEARBY");
                player.getInventory().setItem(9,new ItemStack(Items.GOLD_INGOT,5));
                menu.quickMoveStack(player,58);
                check(player.getInventory().getItem(9).isEmpty() && cart.getItem(1).is(Items.GOLD_INGOT)
                        && cart.getItem(1).getCount()==5,"CUSTOM_MENU_SHIFT_CLICK_TO_CARGO");
                menu.quickMoveStack(player,53);
                check(cart.getItem(53).isEmpty() && player.getInventory().contains(new ItemStack(Items.DIAMOND)),
                        "CUSTOM_MENU_SHIFT_CLICK_FROM_LAST_CARGO_SLOT");
                player.setPos(90,100,32);check(!menu.stillValid(player),"MENU_CLOSES_AT_DISTANCE");
                check(cart.getPickResult().is(MythicRails.FREIGHT_CART_ITEM),"PICK_RETURNS_ADDON_ITEM");
                var hopperPos=new BlockPos(40,99,32);
                world.setBlockAndUpdate(hopperPos,Blocks.HOPPER.defaultBlockState());
                var hopper=(HopperBlockEntity)world.getBlockEntity(hopperPos);
                check(HopperBlockEntity.suckInItems(world,hopper),"VANILLA_HOPPER_EXTRACTS_FROM_CART");
                check(hopper.getItem(0).getCount()==1 && cart.getItem(0).getCount()==22,"HOPPER_EXACT_ITEM_ACCOUNTING");
                cart.clearContent();
                for(int i=0;i<53;i++)cart.setItem(i,new ItemStack(Items.COBBLESTONE,64));
                var remainder=HopperBlockEntity.addItem(new SimpleContainer(1),cart,new ItemStack(Items.GOLD_INGOT,64),Direction.DOWN);
                check(remainder.isEmpty() && cart.getItem(53).getCount()==64,"HOPPER_INSERT_REACHES_LAST_SLOT");
                remainder=HopperBlockEntity.addItem(new SimpleContainer(1),cart,new ItemStack(Items.DIAMOND),Direction.DOWN);
                check(remainder.is(Items.DIAMOND) && remainder.getCount()==1,"FULL_CART_REJECTS_WITHOUT_LOSS");
                cart.clearContent();cart.setItem(53,new ItemStack(Items.DIAMOND,17));
                cart.setPos(45.5,100.0625,32.5);cart.setDeltaMovement(0.2,0,0);
                for(int i=0;i<8;i++)cart.tick();
                check(cart.getX()>46 && cart.isOnRails(),"VANILLA_RAIL_MOVEMENT");
                check(cart.getItem(53).getCount()==17,"MOVEMENT_PRESERVES_CARGO");
                int beforeCargo=dropped(world,Items.DIAMOND),beforeCart=dropped(world,MythicRails.FREIGHT_CART_ITEM);
                cart.destroy(world.damageSources().generic());
                check(cart.isRemoved(),"DESTROY_REMOVES_ENTITY");
                check(dropped(world,Items.DIAMOND)-beforeCargo==17,"DESTROY_DROPS_CARGO_ONCE");
                check(dropped(world,MythicRails.FREIGHT_CART_ITEM)-beforeCart==1,"DESTROY_DROPS_ADDON_CART_ONCE");
                var item=new ItemStack(MythicRails.FREIGHT_CART_ITEM);
                item.set(DataComponents.CUSTOM_NAME,Component.literal("Placed Cart"));player.setItemInHand(InteractionHand.MAIN_HAND,item);
                var pos=new BlockPos(54,100,32);
                var context=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
                check(item.getItem().useOn(context).consumesAction() && item.isEmpty(),"PLACEMENT_CONSUMES_ONE_SURVIVAL_ITEM");
                var placed=world.getEntitiesOfClass(HermesFreightCart.class,new AABB(pos).inflate(1));
                check(placed.size()==1 && placed.get(0).getName().getString().equals("Placed Cart"),"PLACEMENT_SPAWNS_NAMED_CUSTOM_CART");
                var invalid=new ItemStack(MythicRails.FREIGHT_CART_ITEM);player.setItemInHand(InteractionHand.MAIN_HAND,invalid);
                pos=new BlockPos(54,99,32);
                check(invalid.getItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)))==InteractionResult.FAIL
                        && invalid.getCount()==1,"NON_RAIL_PLACEMENT_SAFE");
                check(server.getRecipeManager().byKey(MythicRails.id("hermes_freight_cart")).isPresent(),"RECIPE_LOADED");
                System.out.println("RAILS_ALL_PASSED");
            } catch(Throwable t){t.printStackTrace();System.out.println("RAILS_FAILED");}
            finally {server.halt(false);}
        });
    }
}
