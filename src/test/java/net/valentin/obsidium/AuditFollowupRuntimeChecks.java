package net.valentin.obsidium;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.advancements.critereon.RecipeCraftedTrigger;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.valentin.obsidium.item.*;
import net.valentin.obsidium.menu.*;

import java.util.List;
import java.util.UUID;

/** Regression checks for backpack identity and native advancement definitions. */
public final class AuditFollowupRuntimeChecks implements ModInitializer {
    @Override public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            server.tickRateManager().setFrozen(true);
            try {
                var player=new ServerPlayer(server,server.overworld(),new GameProfile(UUID.randomUUID(),"BackpackChecks"),ClientInformation.createDefault());
                for(boolean advanced:new boolean[]{false,true}) {
                    for(InteractionHand hand:InteractionHand.values()) {
                        player.getInventory().clearContent();player.getInventory().selected=0;
                        ItemStack original=new ItemStack(advanced?ObsidiumMod.ADVANCED_BACKPACK:ObsidiumMod.BASIC_BACKPACK);
                        ItemStack replacement=original.copy();
                        original.set(DataComponents.CUSTOM_NAME,Component.literal("Original"));
                        replacement.set(DataComponents.CUSTOM_NAME,Component.literal("Replacement"));
                        var contents=NonNullList.withSize(advanced?AdvancedBackpackItem.TOTAL_CONTAINER_SIZE:BasicBackpackItem.CONTAINER_SIZE,ItemStack.EMPTY);
                        contents.set(0,new ItemStack(Items.DIAMOND,3));save(original,contents,advanced,server.registryAccess());
                        contents.set(0,new ItemStack(Items.EMERALD,5));save(replacement,contents,advanced,server.registryAccess());
                        int selected=hand==InteractionHand.MAIN_HAND?0:40;
                        player.getInventory().setItem(selected,original);player.getInventory().setItem(1,replacement);
                        AbstractContainerMenu menu=advanced?new AdvancedBackpackMenu(1,player.getInventory(),hand):new BasicBackpackMenu(1,player.getInventory(),hand);
                        String label=(advanced?"ADVANCED_":"BASIC_")+hand;
                        check(menu.stillValid(player) && menu.slots.get(0).getItem().is(Items.DIAMOND),label+"_LOADS_EXACT_OPENED_STACK");
                        if(hand==InteractionHand.MAIN_HAND) player.getInventory().selected=1;
                        else player.getInventory().setItem(40,replacement);
                        check(!menu.stillValid(player),label+"_REPLACEMENT_INVALIDATES_MENU");
                        check(menu.quickMoveStack(player,0).isEmpty(),label+"_INVALID_MENU_REJECTS_TRANSFER");
                        // A delayed callback/close must save the original object, never the new hand item.
                        menu.slots.get(0).set(new ItemStack(Items.DIAMOND,4));menu.removed(player);
                        var a=load(original,advanced,server.registryAccess());var b=load(replacement,advanced,server.registryAccess());
                        check(a.get(0).is(Items.DIAMOND) && a.get(0).getCount()==4
                                && b.get(0).is(Items.EMERALD) && b.get(0).getCount()==5,label+"_CLOSE_CANNOT_OVERWRITE_OTHER_BACKPACK");
                        check(original.get(DataComponents.CUSTOM_NAME).getString().equals("Original")
                                && replacement.get(DataComponents.CUSTOM_NAME).getString().equals("Replacement"),label+"_PRESERVES_COMPONENTS");
                        player.getInventory().selected=0;player.getInventory().setItem(selected,original);
                        AbstractContainerMenu copyMenu=advanced?new AdvancedBackpackMenu(2,player.getInventory(),hand):new BasicBackpackMenu(2,player.getInventory(),hand);
                        player.getInventory().setItem(selected,original.copy());
                        check(!copyMenu.stillValid(player),label+"_IDENTICAL_COPY_IS_NOT_ORIGINAL");
                    }
                }
                player.getInventory().selected=0;
                var energyA=new ItemStack(ObsidiumMod.ADVANCED_BACKPACK);var energyB=new ItemStack(ObsidiumMod.ADVANCED_BACKPACK);
                ((AdvancedBackpackItem)energyA.getItem()).setStoredEnergy(energyA,1000);
                ((AdvancedBackpackItem)energyB.getItem()).setStoredEnergy(energyB,500);
                player.getInventory().setItem(0,energyA);player.getInventory().setItem(1,energyB);
                var energyMenu=new AdvancedBackpackMenu(3,player.getInventory(),InteractionHand.MAIN_HAND);player.getInventory().selected=1;
                energyMenu.broadcastChanges();
                check(energyMenu.getBackpackStack()==energyA && energyMenu.getStoredEnergy()==1000
                        && ((AdvancedBackpackItem)energyB.getItem()).getStoredEnergy(energyB)==500,"INVALID_ADVANCED_MENU_DOES_NOT_CHARGE_OTHER_BACKPACK");

                String[] names={"thermal_foundation","mobile_workshop","first_circuit","fluid_engineer","advanced_routing","quantum_engineer","rare_gardener","hephaestean_collection","collider_architect","singularity","taste_of_the_caldera"};
                int craftCriteria=0;
                for(String name:names) {
                    var id=ResourceLocation.fromNamespaceAndPath("obsidium","odyssey/achievements/"+name);
                    var holder=server.getAdvancements().get(id);
                    check(holder!=null,"ADVANCEMENT_LOADS_"+name.toUpperCase());
                    var advancement=holder.value();
                    check(advancement.parent().isPresent() && server.getAdvancements().get(advancement.parent().get())!=null,
                            "ADVANCEMENT_PARENT_"+name.toUpperCase());
                    for(var criterion:advancement.criteria().values()) {
                        if(criterion.triggerInstance() instanceof RecipeCraftedTrigger.TriggerInstance crafted) {
                            check(server.getRecipeManager().byKey(crafted.recipeId()).isPresent(),"CRAFT_TARGET_EXISTS_"+crafted.recipeId().getPath().toUpperCase());
                            var matches=RecipeCraftedTrigger.TriggerInstance.class.getDeclaredMethod("matches",ResourceLocation.class,List.class);matches.setAccessible(true);
                            check((boolean)matches.invoke(crafted,crafted.recipeId(),List.of())
                                    && !(boolean)matches.invoke(crafted,ResourceLocation.withDefaultNamespace("stick"),List.of()),
                                    "CRAFT_CRITERION_MATCHES_ONLY_TARGET_"+crafted.recipeId().getPath().toUpperCase());
                            craftCriteria++;
                        }
                    }
                }
                check(craftCriteria>=10,"CRAFT_ACHIEVEMENTS_USE_REAL_CRAFT_TRIGGER");
                var or=server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath("obsidium","odyssey/achievements/advanced_routing")).value();
                check(or.requirements().test(key->key.equals("hermes_item_pipe")),"ANY_ADVANCED_TRANSPORT_COMPLETES_ROUTING");
                var and=server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath("obsidium","odyssey/achievements/hephaestean_collection")).value();
                check(!and.requirements().test(key->key.equals("helmet")) && and.requirements().test(key->true),"ARMOR_CHALLENGE_REQUIRES_ALL_FOUR_PIECES");
                System.out.println("FOLLOWUP_ALL_PASSED");
            }catch(Throwable failure){failure.printStackTrace();System.out.println("FOLLOWUP_FAILED");}
            finally{server.halt(false);}
        });
    }
    private static void save(ItemStack stack,NonNullList<ItemStack> contents,boolean advanced,net.minecraft.core.HolderLookup.Provider provider){
        if(advanced)AdvancedBackpackItem.saveInventory(stack,contents,provider);else BasicBackpackItem.saveInventory(stack,contents,provider);
    }
    private static NonNullList<ItemStack> load(ItemStack stack,boolean advanced,net.minecraft.core.HolderLookup.Provider provider){
        return advanced?AdvancedBackpackItem.loadInventory(stack,provider):BasicBackpackItem.loadInventory(stack,provider);
    }
    private static void check(boolean passed,String name){if(!passed)throw new AssertionError(name);System.out.println("FOLLOWUP_OK_"+name);}
}
