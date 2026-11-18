package net.valentin.obsidium;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.LevelResource;
import net.valentin.obsidium.block.*;
import net.valentin.obsidium.fluid.*;
import net.valentin.obsidium.item.VolcaniteBottleItem;
import net.valentin.obsidium.storage.QuantumLinkNetworkState;

/** Full variant persistence and safe boundaries for plain-fluid containers. */
public final class FluidPersistenceRuntimeChecks implements ModInitializer {
    @Override public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            server.tickRateManager().setFrozen(true);
            try {
                var level=server.overworld();var registry=server.registryAccess();
                var plain=FluidVariant.of(Fluids.WATER);
                var named=FluidVariant.of(Fluids.WATER,DataComponentPatch.builder().set(DataComponents.CUSTOM_NAME,Component.literal("Preserved Water")).build());
                var other=FluidVariant.of(Fluids.WATER,DataComponentPatch.builder().set(DataComponents.CUSTOM_NAME,Component.literal("Other Water")).build());
                var network=QuantumLinkNetworkState.get(server);String key="fluid-persistence-checks";
                check(network.insertFluid(key,named,1000,false)==1000,"QUANTUM_ACCEPTS_FULL_VARIANT");
                check(network.insertFluid(key,plain,10,true)==0 && network.insertFluid(key,other,10,false)==0,"DIFFERENT_COMPONENTS_DO_NOT_MERGE");
                try(Transaction tx=Transaction.openOuter()){network.getFluidStorage(key).extract(named,200*81L,tx);}
                check(network.getFluid(key).equals(named) && network.getFluidAmount(key)==1000,"QUANTUM_ROLLBACK_PRESERVES_COMPONENTS");
                level.getDataStorage().save();
                var saved=NbtIo.readCompressed(server.getWorldPath(LevelResource.ROOT).resolve("data/obsidium_quantum_link_network.dat"),NbtAccounter.unlimitedHeap());
                var reloaded=QuantumLinkNetworkState.load(saved.getCompound("data"),registry);
                check(reloaded.getFluid(key).equals(named) && reloaded.getFluidAmount(key)==1000,"QUANTUM_DISK_RELOAD_PRESERVES_COMPONENTS");
                var legacy=new CompoundTag();var entries=new ListTag();var entry=new CompoundTag();entry.putString("Key","legacy");entry.putString("Fluid","minecraft:water");entry.putLong("Amount",42);entries.add(entry);legacy.put("FluidVaults",entries);
                var old=QuantumLinkNetworkState.load(legacy,registry);
                check(old.getFluid("legacy").equals(plain) && old.getFluidAmount("legacy")==42,"LEGACY_QUANTUM_SAVE_MIGRATES");
                entry.putString("FluidVariant","malformed");
                var corrupt=QuantumLinkNetworkState.load(legacy,registry);
                check(corrupt.getFluid("legacy").isBlank() && corrupt.getFluidAmount("legacy")==0,"MALFORMED_VARIANT_NOT_DOWNGRADED_TO_PLAIN");
                entry.remove("FluidVariant");entry.putString("Fluid","not a valid id!");
                check(QuantumLinkNetworkState.load(legacy,registry).getFluid("legacy").isBlank(),"INVALID_LEGACY_ID_SAFE");
                var barrel=barrel(level,new BlockPos(32,100,32));barrel.insertFluid(named,1000,false);
                var tag=barrel.saveWithoutMetadata(registry);
                var copy=new VolcanicWoodLiquidBarrelBlockEntity(barrel.getBlockPos(),barrel.getBlockState());copy.loadWithComponents(tag,registry);
                check(copy.getStoredFluid().equals(named) && copy.getFluidAmount()==1000,"LOCAL_BARREL_RELOAD_PRESERVES_COMPONENTS");
                var packet=barrel.getUpdateTag(registry);copy.loadWithComponents(packet,registry);
                check(copy.getStoredFluid().equals(named),"LOCAL_UPDATE_PRESERVES_COMPONENTS");
                var packed=new ItemStack(ObsidiumMod.VOLCANIC_WOOD_LIQUID_BARREL_ITEM);barrel.saveToItem(packed,registry);
                copy.loadWithComponents(packed.get(DataComponents.BLOCK_ENTITY_DATA).copyTag(),registry);
                check(copy.getStoredFluid().equals(named) && copy.getFluidAmount()==1000,"PACKED_BARREL_PRESERVES_COMPONENTS");
                var legacyBarrel=tag.copy();legacyBarrel.remove("FluidVariant");copy.loadWithComponents(legacyBarrel,registry);
                check(copy.getStoredFluid().equals(plain) && copy.getFluidAmount()==1000,"LEGACY_BARREL_SAVE_MIGRATES");
                var target=barrel(level,new BlockPos(36,100,32));
                check(FluidNetworkTransfers.pump(barrel,target,named,50)==50 && target.getStoredFluid().equals(named)
                        && target.getFluidAmount()==50 && barrel.getFluidAmount()==950,"PUMP_PRESERVES_VARIANT_AND_QUANTITY");
                check(target.insertFluid(plain,100,false)==0 && target.getFluidAmount()==50,"LOCAL_COMPONENT_MISMATCH_REJECTED");
                var containers=barrel(level,new BlockPos(48,100,32));containers.insertFluid(named,1000,false);
                containers.getInventory().setItem(0,new ItemStack(Items.BUCKET));tick(level,containers);
                check(containers.getFluidAmount()==1000 && containers.getInventory().getItem(0).is(Items.BUCKET)
                        && containers.getInventory().getItem(1).isEmpty(),"BUCKET_CANNOT_STRIP_COMPONENTS");
                containers.getInventory().setItem(0,new ItemStack(ObsidiumMod.VOLCANITE_BOTTLE));tick(level,containers);
                check(containers.getFluidAmount()==1000 && VolcaniteBottleItem.getFluidAmount(containers.getInventory().getItem(0))==0,"VOLCANITE_BOTTLE_CANNOT_STRIP_COMPONENTS");
                var bottleContext=ContainerItemContext.withConstant(new ItemStack(ObsidiumMod.VOLCANITE_BOTTLE));
                var itemStorage=FluidStorage.ITEM.find(new ItemStack(ObsidiumMod.VOLCANITE_BOTTLE),bottleContext);
                check(itemStorage!=null,"ITEM_ADAPTER_EXISTS");
                try(Transaction tx=Transaction.openOuter()){check(itemStorage.insert(named,1000*81L,tx)==0,"ITEM_ADAPTER_REJECTS_UNREPRESENTABLE_COMPONENTS");}
                var player=new net.minecraft.server.level.ServerPlayer(server,level,
                        new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"FluidHandChecks"),net.minecraft.server.level.ClientInformation.createDefault());
                var use=VolcanicWoodLiquidBarrelBlock.class.getDeclaredMethod("useItemOn",ItemStack.class,
                        net.minecraft.world.level.block.state.BlockState.class,net.minecraft.world.level.Level.class,BlockPos.class,
                        net.minecraft.world.entity.player.Player.class,net.minecraft.world.InteractionHand.class,net.minecraft.world.phys.BlockHitResult.class);
                use.setAccessible(true);
                var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(containers.getBlockPos()),
                        net.minecraft.core.Direction.UP,containers.getBlockPos(),false);
                for(Item item:new Item[]{Items.BUCKET,Items.GLASS_BOTTLE}) {
                    var held=new ItemStack(item);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,held);
                    use.invoke(containers.getBlockState().getBlock(),held,containers.getBlockState(),level,containers.getBlockPos(),player,
                            net.minecraft.world.InteractionHand.MAIN_HAND,hit);
                    check(containers.getFluidAmount()==1000 && containers.getStoredFluid().equals(named)
                            && player.getMainHandItem().is(item),"HAND_CONTAINER_PRESERVES_COMPONENTS_"+item);
                }
                var ordinary=barrel(level,new BlockPos(64,100,32));ordinary.insertFluid(plain,1000,false);ordinary.getInventory().setItem(0,new ItemStack(Items.BUCKET));tick(level,ordinary);
                check(ordinary.getFluidAmount()==0 && ordinary.getInventory().getItem(1).is(Items.WATER_BUCKET),"PLAIN_BUCKET_FILL_UNCHANGED");
                check(FluidBucketMapping.filledBucket(named)==Items.AIR && FluidBucketMapping.filledBucket(plain)==Items.WATER_BUCKET,"BUCKET_MAPPING_CONTRACT");
                System.out.println("FLUIDPERSIST_ALL_PASSED");
            }catch(Throwable failure){failure.printStackTrace();System.out.println("FLUIDPERSIST_FAILED");}
            finally{server.halt(false);}
        });
    }
    private static VolcanicWoodLiquidBarrelBlockEntity barrel(ServerLevel level,BlockPos pos){level.setBlock(pos,ObsidiumMod.VOLCANIC_WOOD_LIQUID_BARREL_BLOCK.defaultBlockState(),3);return (VolcanicWoodLiquidBarrelBlockEntity)level.getBlockEntity(pos);}
    private static void tick(ServerLevel level,LiquidTankBlockEntity tank){LiquidTankBlockEntity.tick(level,tank.getBlockPos(),tank.getBlockState(),tank);}
    private static void check(boolean passed,String name){if(!passed)throw new AssertionError(name);System.out.println("FLUIDPERSIST_OK_"+name);}
}
