package net.valentin.obsidium;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.valentin.obsidium.block.*;
import net.valentin.obsidium.config.ModConfig;

/** Native recipe validity plus deterministic tests for early/mid/late progression boundaries. */
public final class ProgressionRuntimeChecks implements ModInitializer {
    @Override public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            server.tickRateManager().setFrozen(true);
            try {
                int recipes=0;
                for(var holder:server.getRecipeManager().getRecipes()) {
                    if(!holder.id().getNamespace().equals("obsidium")) continue;
                    recipes++;
                    for(var ingredient:holder.value().getIngredients()) {
                        if(!ingredient.isEmpty() && ingredient.getItems().length==0)
                            throw new AssertionError("Unresolved ingredient: "+holder.id());
                    }
                }
                check(recipes>=434,"ALL_OBSIDIUM_RECIPES_LOAD_WITH_VALID_INGREDIENTS");
                long items=BuiltInRegistries.ITEM.keySet().stream().filter(id->id.getNamespace().equals("obsidium")).count();
                check(items==405,"OBTAINABILITY_REPORT_COVERS_NATIVE_ITEM_REGISTRY");
                check(requires(server,"volcanite_furnace",Items.BLAST_FURNACE),"EARLY_FURNACE_REQUIRES_VANILLA_WORKSHOP");
                check(requires(server,"volcanite_grinder",ObsidiumMod.VOLCANITE_FURNACE_ITEM),"GRINDER_FOLLOWS_FURNACE");
                for(String name:new String[]{"midas_gold_sapling","ares_iron_sapling","hermes_copper_sapling","hephaestus_volcanite_sapling","cronus_lead_sapling","zeus_tin_sapling","artemis_silver_sapling","electrum_sapling",
                        "volcanite_seed","iron_seed","gold_seed","copper_seed","coal_seed","lapis_seed","redstone_seed","tin_seed","lead_seed","silver_seed",
                        "advanced_hermes_item_pipe","advanced_naiad_fluid_pipe","advanced_volcanite_energy_wire"}) {
                    check(requires(server,name,ObsidiumMod.VOLCANITE_FLUX_MECHANISM),"MID_WORKSHOP_GATE_"+name.toUpperCase());
                }
                check(requires(server,"quantum_link_module",ObsidiumMod.ADVANCED_VOLCANITE_CASING_ITEM)
                        && requires(server,"quantum_link_module",ObsidiumMod.VOLCANITE_ENERGY_CELL_ITEM),"QUANTUM_REQUIRES_MID_GAME_INFRASTRUCTURE");
                check(requires(server,"diamond_seed",ObsidiumMod.ASTRAL_VOLCANIC_CATALYST),"DIAMOND_RENEWAL_REQUIRES_BOSS_STAGE");
                check(requires(server,"netherite_seed",ObsidiumMod.SUPREME_VOLCANIC_ALLOY)
                        && requires(server,"netherite_seed",Items.NETHERITE_INGOT),"NETHERITE_CROP_REMAINS_LATE_GAME");
                check(requires(server,"tartarean_netherite_sapling",ObsidiumMod.SUPREME_VOLCANIC_ALLOY)
                        && resultCount(server,"tartarean_netherite_sapling")==1,"NETHERITE_TREE_NO_CHEAP_DOUBLE_SEED");
                check(requires(server,"supreme_volcanic_alloy",Items.NETHER_STAR),"SUPREME_RETAINS_HIGH_END_GATE");
                check(requires(server,"singularity_volcanite_energy_wire",ObsidiumMod.SINGULARITY_HADRON_ALLOY),"SINGULARITY_REQUIRES_COLLIDER_OUTPUT");
                check(GraftingRegistry.resolveHybrid("hephaestus_volcanite","cronus_lead")!=ObsidiumMod.TARTAREAN_NETHERITE_SAPLING
                        && GraftingRegistry.resolveHybrid("hephaestus_volcanite","ares_iron")!=ObsidiumMod.TARTAREAN_NETHERITE_SAPLING,"COMMON_GRAFTS_CANNOT_SKIP_NETHERITE_GATE");
                check(!GraftingRegistry.canProduceRareMutation("hephaestus_volcanite","electrum")
                        && GraftingRegistry.canProduceRareMutation("tartarean_netherite","hephaestus_volcanite"),"BEE_AND_BRUSH_RARE_MUTATION_NEEDS_LATE_PARENT");
                check(GraftingRegistry.resolveHybrid("oak","hephaestus_volcanite")==ObsidiumMod.HADES_COAL_SAPLING,
                        "COAL_HYBRID_HAS_NON_CIRCULAR_STARTER_ROUTE");
                check(GraftingRegistry.resolveHybrid("tartarean_netherite","plouton_emerald")==ObsidiumMod.ADAMANTINE_DIAMOND_SAPLING,"DIAMOND_TREE_STILL_OBTAINABLE_BY_LATE_GRAFTING");
                int oldMultiplier=ModConfig.get().rareResourceGrowthMultiplier;
                try {
                    ModConfig.get().rareResourceGrowthMultiplier=4;
                    check(DemetrianBonsaiPlanterBlockEntity.requiredGrowthTicks(new ItemStack(ObsidiumMod.TARTAREAN_NETHERITE_SAPLING_ITEM))==4800
                            && DemetrianBonsaiPlanterBlockEntity.requiredGrowthTicks(new ItemStack(Items.OAK_SAPLING))==1200,"RARE_BONSAI_SLOWER_COMMON_UNCHANGED");
                    ModConfig.get().rareResourceGrowthMultiplier=-100;
                    check(ModConfig.get().getRareResourceGrowthMultiplier()==1,"MODPACK_MULTIPLIER_LOWER_BOUND");
                    ModConfig.get().rareResourceGrowthMultiplier=Integer.MAX_VALUE;
                    check(ModConfig.get().getRareResourceGrowthMultiplier()==64,"MODPACK_MULTIPLIER_UPPER_BOUND");
                } finally {ModConfig.get().rareResourceGrowthMultiplier=oldMultiplier;}
                var level=server.overworld();BlockPos pos=new BlockPos(32,100,32);
                level.setBlock(pos.below(),Blocks.NETHERRACK.defaultBlockState(),3);
                level.setBlock(pos,ObsidiumMod.NETHERITE_CROP.defaultBlockState(),3);
                ((VolcanicOreCropBlock)ObsidiumMod.NETHERITE_CROP).performBonemeal(level,level.random,pos,level.getBlockState(pos));
                check(((VolcanicOreCropBlock)ObsidiumMod.NETHERITE_CROP).getAge(level.getBlockState(pos))==1,"RARE_CROP_FERTILIZER_NOT_INSTANT");
                var arboristPos=new BlockPos(48,100,48);
                level.setBlock(arboristPos,ObsidiumMod.GEOTHERMAL_ARBORIST_BLOCK.defaultBlockState(),3);
                var arborist=(GeothermalArboristBlockEntity)level.getBlockEntity(arboristPos);
                set(arborist,"mutationSelection",1);set(arborist,"mutationProgress",100);set(arborist,"lavaAmount",1000);arborist.energyStorage.amount=10000;
                level.setBlock(arboristPos.east(),ObsidiumMod.HEPHAESTUS_VOLCANITE_LEAVES.defaultBlockState(),3);
                level.setBlock(arboristPos.west(),ObsidiumMod.ARES_IRON_LEAVES.defaultBlockState(),3);
                var mutation=GeothermalArboristBlockEntity.class.getDeclaredMethod("advanceTreeMutationPassive",net.minecraft.server.level.ServerLevel.class,BlockPos.class);mutation.setAccessible(true);
                mutation.invoke(arborist,level,arboristPos);
                check(arborist.getInventory().getItem(9).isEmpty() && arborist.energyStorage.amount==10000,"AUTOMATIC_MUTATION_CANNOT_BOOTSTRAP_RARE_TREE");
                level.setBlock(arboristPos.west(),ObsidiumMod.TARTAREAN_NETHERITE_LEAVES.defaultBlockState(),3);
                mutation.invoke(arborist,level,arboristPos);
                check(!arborist.getInventory().getItem(9).isEmpty() && arborist.energyStorage.amount<10000,"AUTOMATIC_LATE_PROPAGATION_STILL_WORKS");
                System.out.println("PROGRESSION_ALL_PASSED");
            }catch(Throwable failure){failure.printStackTrace();System.out.println("PROGRESSION_FAILED");}
            finally{server.halt(false);}
        });
    }
    private static void set(Object target,String field,int value)throws ReflectiveOperationException {
        var f=target.getClass().getDeclaredField(field);f.setAccessible(true);f.setInt(target,value);
    }
    private static boolean requires(net.minecraft.server.MinecraftServer server,String recipe,Item item) {
        var holder=server.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("obsidium",recipe)).orElseThrow();
        return holder.value().getIngredients().stream().anyMatch(i->i.test(new ItemStack(item)));
    }
    private static int resultCount(net.minecraft.server.MinecraftServer server,String recipe){
        return server.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("obsidium",recipe)).orElseThrow().value().getResultItem(server.registryAccess()).getCount();
    }
    private static void check(boolean passed,String name){if(!passed)throw new AssertionError(name);System.out.println("PROGRESSION_OK_"+name);}
}
