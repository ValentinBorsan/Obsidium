package net.valentin.obsidium;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.valentin.obsidium.block.*;
import net.valentin.obsidium.storage.QuantumLinkNetworkState;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;
import java.util.*;

public final class NewContentRuntimeChecks implements ModInitializer {
    private static int failures;
    private static final Map<BlockPos, SimpleEnergyStorage> energy = new HashMap<>();
    @Override public void onInitialize() {
        EnergyStorage.SIDED.registerForBlocks((w,p,s,b,d) -> energy.get(p), Blocks.GOLD_BLOCK);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            for (int x=1;x<=8;x++) for(int z=1;z<=8;z++) server.overworld().setChunkForced(x,z,true);
        });
        int[] ticks={0};
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if(++ticks[0]!=30) return;
            server.tickRateManager().setFrozen(true);
            ServerLevel level=server.overworld();
            QuantumLinkNetworkState net=QuantumLinkNetworkState.get(server);
            var player=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"AuditPlayer"),ClientInformation.createDefault()) {
                @Override public void displayClientMessage(net.minecraft.network.chat.Component c,boolean overlay) {}
            };
            probe("POSEIDON_RECIPE_REGISTERED",()-> {
                observed(server.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("obsidium","poseidon_abyssal_pump")).isPresent(),
                        "poseidon_recipe_loaded=true");
            });
            probe("POSEIDON_CAPACITY",()-> {
                var pump=pump(level,new BlockPos(32,100,32));
                long mb=pump.fluidStorage.getCapacity()/81;
                observed(mb==256_000L,"capacity_mB="+mb+",advertised_mB=256000");
            });
            probe("POSEIDON_WATERLOGGED_DESTRUCTION",()-> {
                var pump=pump(level,new BlockPos(64,100,64));
                for(BlockPos p:pump.getCasingPositions()) level.setBlock(p,ObsidiumMod.POSEIDON_ABYSSAL_CASING_BLOCK.defaultBlockState(),3);
                observed(pump.validateMultiblock(level),"formed=true");
                BlockPos target=pump.getBlockPos().offset(-16,-1,-16);
                level.setBlock(target,Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,true),3);
                pump.energyStorage.amount=10_000;
                PoseidonAbyssalPumpBlockEntity.tick(level,pump.getBlockPos(),pump.getBlockState(),pump);
                observed(level.getBlockState(target).is(Blocks.OAK_STAIRS) && pump.fluidStorage.amount==0,
                    "waterlogged_stairs_removed="+level.getBlockState(target).isAir()+",pumped_mB="+pump.fluidStorage.amount/81);
            });
            probe("QUANTUM_FLUID_DOUBLE_EXTRACTION",()-> {
                String key="audit-fluid-double"; FluidVariant water=FluidVariant.of(Fluids.WATER);
                net.insertFluid(key,water,1,false);
                var method=NaiadFluidPipeBlockEntity.class.getDeclaredMethod("createQuantumFluidExtractStorage",QuantumLinkNetworkState.class,String.class);
                method.setAccessible(true);
                @SuppressWarnings("unchecked") Storage<FluidVariant> source=(Storage<FluidVariant>)method.invoke(null,net,key);
                var dest=new FluidTank();
                long a,b;
                try(Transaction tx=Transaction.openOuter()) {
                    a=source.extract(water,81,tx);dest.insert(water,a,tx);
                    b=source.extract(water,81,tx);dest.insert(water,b,tx);tx.commit();
                }
                observed(a+b==81 && dest.amount==81 && net.getFluidAmount(key)==0,
                    "initial_mB=1,destination_mB="+dest.amount/81+",source_mB="+net.getFluidAmount(key));
            });
            probe("QUANTUM_ENERGY_DOUBLE_EXTRACTION",()-> {
                String key="audit-energy-double";net.insertEnergy(key,1000,false);
                var method=VolcaniteEnergyWireBlockEntity.class.getDeclaredMethod("createQuantumExtractStorage",QuantumLinkNetworkState.class,String.class);
                method.setAccessible(true);EnergyStorage source=(EnergyStorage)method.invoke(null,net,key);
                var dest=new SimpleEnergyStorage(10_000,10_000,10_000);
                try(Transaction tx=Transaction.openOuter()) {
                    dest.insert(source.extract(1000,tx),tx);dest.insert(source.extract(1000,tx),tx);tx.commit();
                }
                observed(dest.amount==1000 && net.getEnergy(key)==0,"initial_RF=1000,destination_RF="+dest.amount+",source_RF="+net.getEnergy(key));
            });
            probe("QUANTUM_FLUID_NESTED_ABORT",()-> {
                var pipe=pipe(level,new BlockPos(48,100,32));pipe.setQuantumLinked(true,null);pipe.setQuantumChannel(901);
                String key=QuantumLinkNetworkState.buildChannelKey(false,null,pipe.getQuantumColor(),901);
                var storage=pipe.getFluidStorage(Direction.WEST);
                try(Transaction parent=Transaction.openOuter()) {
                    try(Transaction child=parent.openNested()) {storage.insert(FluidVariant.of(Fluids.WATER),81,child);child.commit();}
                }
                observed(net.getFluidAmount(key)==0,"outer_aborted=true,remaining_mB="+net.getFluidAmount(key));
            });
            probe("QUANTUM_ENERGY_NESTED_ABORT",()-> {
                var wire=wire(level,new BlockPos(64,100,32));wire.setQuantumLinked(true,null);wire.setQuantumChannel(902);
                String key=QuantumLinkNetworkState.buildChannelKey(false,null,wire.getQuantumColor(),902);
                try(Transaction parent=Transaction.openOuter()) {
                    try(Transaction child=parent.openNested()) {wire.getEnergyStorage(Direction.WEST).insert(1000,child);child.commit();}
                }
                observed(net.getEnergy(key)==0,"outer_aborted=true,remaining_RF="+net.getEnergy(key));
            });
            probe("CONTROL_FILTER_RECOVERY",()-> {
                BlockPos pos=new BlockPos(80,100,32);var pipe=pipe(level,pos);
                pipe.getFilterContainer().setItem(0,new ItemStack(Items.WATER_BUCKET));
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
                int count=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3)).stream()
                    .filter(e->e.getItem().is(Items.WATER_BUCKET)).mapToInt(e->e.getItem().getCount()).sum();
                observed(count==1,"filter_water_buckets_before=1,dropped="+count);
            });
            probe("PIPE_MENU_REMOTE_AND_REMOVED",()-> {
                BlockPos pos=new BlockPos(96,100,32);var pipe=pipe(level,pos);var menu=new PipeConfigMenu(1,player.getInventory(),pipe);
                player.setPos(2000,100,2000);boolean far=menu.stillValid(player);
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);boolean removed=menu.stillValid(player);
                observed(!far && !removed,"valid_at_2000_blocks="+far+",valid_after_removal="+removed);
            });
            probe("ENERGY_RATE_PER_CALL",()-> {
                BlockPos pos=new BlockPos(32,100,48);var wire=wire(level,pos);
                energy.put(pos.east(),new SimpleEnergyStorage(100_000,100_000,100_000));level.setBlock(pos.east(),Blocks.GOLD_BLOCK.defaultBlockState(),3);
                long moved=0;
                try(Transaction tx=Transaction.openOuter()) {
                    for(int i=0;i<3;i++) moved+=wire.getEnergyStorage(Direction.WEST).insert(wire.getEffectiveMaxTransfer(),tx);
                    tx.commit();
                }
                observed(moved==wire.getEffectiveMaxTransfer(),"advertised_RF_per_tick="+wire.getEffectiveMaxTransfer()+",same_tick_inserted_RF="+moved);
            });
            probe("ENERGY_RATE_PER_SOURCE",()-> {
                BlockPos pos=new BlockPos(48,100,48);var wire=wire(level,pos);
                var source1=new SimpleEnergyStorage(100_000,0,100_000);source1.amount=100_000;
                var source2=new SimpleEnergyStorage(100_000,0,100_000);source2.amount=100_000;
                var dest=new SimpleEnergyStorage(100_000,100_000,0);
                energy.put(pos.west(),source1);energy.put(pos.north(),source2);energy.put(pos.east(),dest);
                for(BlockPos p:List.of(pos.west(),pos.north(),pos.east())) level.setBlock(p,Blocks.GOLD_BLOCK.defaultBlockState(),3);
                VolcaniteEnergyWireBlockEntity.tick(level,pos,wire.getBlockState(),wire);
                observed(dest.amount==wire.getEffectiveMaxTransfer(),"advertised_RF_per_tick="+wire.getEffectiveMaxTransfer()+",one_tick_received_RF="+dest.amount);
            });
            probe("LEGACY_WIRE_ENERGY_LOST",()-> {
                var wire=wire(level,new BlockPos(80,100,48));var tag=new CompoundTag();tag.putLong("Energy",12000);
                wire.loadWithComponents(tag,server.registryAccess());
                var saved=wire.saveWithoutMetadata(server.registryAccess());
                observed(saved.getLong("Energy")==12000 && wire.getEnergyStorage(null).getAmount()==12000,
                    "legacy_saved_RF=12000,resaved_energy_key="+saved.contains("Energy")+",exposed_RF="+wire.getEnergyStorage(null).getAmount());
            });
            probe("OPTIC_HERMES_ISOLATION_BYPASS",()-> {
                BlockPos pos=new BlockPos(32,100,96);
                level.setBlock(pos,ObsidiumMod.HERMES_ITEM_PIPE_BLOCK.defaultBlockState(),3);
                level.setBlock(pos.east(),ObsidiumMod.VOLCANITE_STORAGE_PIPE_BLOCK.defaultBlockState(),3);
                level.setBlock(pos.east(2),ObsidiumMod.HERMES_ITEM_PIPE_BLOCK.defaultBlockState(),3);
                level.setBlock(pos.east(3),Blocks.CHEST.defaultBlockState(),3);
                var end=(VolcaniteStoragePipeBlockEntity)level.getBlockEntity(pos.east(2));
                end.setModule(Direction.EAST,VolcaniteStoragePipeBlockEntity.ModuleType.INSERT);
                var fiber=(VolcaniteStoragePipeBlockEntity)level.getBlockEntity(pos.east());
                boolean visual=PipeConnectionHelper.canOpticFiberConnect(level,pos,Direction.WEST,fiber);
                int targets=HermesItemPipeBlockEntity.findItemPipeInsertTargets(level,pos).size();
                observed(!visual && targets==0,"visual_connection="+visual+",insert_targets_reachable_via_fiber="+targets);
            });
            probe("PIPE_PRIVATE_OWNER_BYPASS",()-> {
                var pipe=pipe(level,new BlockPos(96,100,80));pipe.setQuantumLinked(true,UUID.randomUUID());pipe.setQuantumPrivate(true);
                var menu=new PipeConfigMenu(2,player.getInventory(),pipe);
                player.setPos(96.5,100.5,80.5);
                boolean accepted=menu.clickMenuButton(player,PipeConfigMenu.BTN_Q_INC_1);
                observed(!accepted && pipe.getQuantumChannel()==1,"non_owner_changed_private_channel="+accepted+",channel="+pipe.getQuantumChannel());
            });
            probe("PAID_VALVE_LOST_ON_GUI_CYCLE",()-> {
                BlockPos pos=new BlockPos(80,100,80);var pipe=pipe(level,pos);
                pipe.setValveMode(Direction.NORTH,NaiadFluidPipeBlockEntity.ValveMode.EXTRACT);pipe.setHasValveModule(Direction.NORTH,true);
                var menu=new PipeConfigMenu(3,player.getInventory(),pipe);
                player.setPos(pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5);
                observed(menu.clickMenuButton(player,PipeConfigMenu.BTN_CYCLE_FACE_MODE), "cycle_insert_accepted=true");
                observed(menu.clickMenuButton(player,PipeConfigMenu.BTN_CYCLE_FACE_MODE), "cycle_none_accepted=true");
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
                int count=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3)).stream()
                    .filter(e->e.getItem().is(ObsidiumMod.VOLCANITE_LOGISTICS_MODULE)).mapToInt(e->e.getItem().getCount()).sum();
                observed(count==1,"paid_valves_before=1,returned_after_cycle_and_break="+count);
            });
            probe("LEGACY_PIPE_FLUID_LOST",()-> {
                var pipe=pipe(level,new BlockPos(112,100,32));
                var legacy=SingleFluidStorage.withFixedCapacity(162000,()->{});
                try(Transaction tx=Transaction.openOuter()){legacy.insert(FluidVariant.of(Fluids.WATER),81000,tx);tx.commit();}
                var fluidTag=new CompoundTag();legacy.writeNbt(fluidTag,server.registryAccess());
                var tag=new CompoundTag();tag.put("Fluid",fluidTag);
                pipe.loadWithComponents(tag,server.registryAccess());
                var saved=pipe.saveWithoutMetadata(server.registryAccess());
                var restored=SingleFluidStorage.withFixedCapacity(162000,()->{});
                restored.readNbt(saved.getCompound("Fluid"),server.registryAccess());
                observed(restored.amount==81000,"legacy_pipe_mB=1000,resaved_fluid_key="+saved.contains("Fluid"));
            });
            probe("PAID_VALVE_LOST_ON_RELOAD",()-> {
                BlockPos pos=new BlockPos(112,100,80);var pipe=pipe(level,pos);
                pipe.setValveMode(Direction.NORTH,NaiadFluidPipeBlockEntity.ValveMode.EXTRACT);pipe.setHasValveModule(Direction.NORTH,true);
                var saved=pipe.saveWithoutMetadata(server.registryAccess());
                var reloaded=new NaiadFluidPipeBlockEntity(pos,pipe.getBlockState());
                reloaded.loadWithComponents(saved,server.registryAccess());level.setBlockEntity(reloaded);
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
                int count=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3)).stream()
                    .filter(e->e.getItem().is(ObsidiumMod.VOLCANITE_LOGISTICS_MODULE)).mapToInt(e->e.getItem().getCount()).sum();
                observed(count==1,"paid_valves_before_reload=1,returned_after_reload_and_break="+count);
            });
            probe("ENERGY_BUDGET_ABORT_RESTORED",()-> {
                BlockPos pos=new BlockPos(112,100,96);var wire=wire(level,pos);
                var dest=new SimpleEnergyStorage(100000,100000,0);energy.put(pos.east(),dest);
                level.setBlock(pos.east(),Blocks.GOLD_BLOCK.defaultBlockState(),3);
                try(Transaction tx=Transaction.openOuter()){wire.getEnergyStorage(Direction.WEST).insert(25000,tx);}
                observed(dest.amount==0,"aborted_target_RF=0");
                long moved;try(Transaction tx=Transaction.openOuter()){moved=wire.getEnergyStorage(Direction.WEST).insert(25000,tx);tx.commit();}
                observed(moved==25000 && dest.amount==25000,"budget_available_after_abort=true");
            });
            probe("POSEIDON_LEGACY_OVERFILL_PRESERVED",()-> {
                BlockPos pos=new BlockPos(112,100,112);var pump=pump(level,pos);
                pump.fluidStorage.variant=FluidVariant.of(Fluids.WATER);pump.fluidStorage.amount=300000L*81L;
                var saved=pump.saveWithoutMetadata(server.registryAccess());
                var copy=new PoseidonAbyssalPumpBlockEntity(pos,pump.getBlockState());copy.loadWithComponents(saved,server.registryAccess());
                long inserted;try(Transaction tx=Transaction.openOuter()){inserted=copy.fluidStorage.insert(FluidVariant.of(Fluids.WATER),81,tx);tx.commit();}
                observed(copy.fluidStorage.amount==300000L*81L && inserted==0,"old_overfill_preserved_and_new_input_refused=true");
            });
            System.out.println(failures==0 ? "NEWCONTENT_ALL_PASSED" : "NEWCONTENT_FAILED");
            server.halt(false);
        });
    }
    private static PoseidonAbyssalPumpBlockEntity pump(ServerLevel l,BlockPos p) {
        l.setBlock(p,ObsidiumMod.POSEIDON_ABYSSAL_PUMP_BLOCK.defaultBlockState(),3);return (PoseidonAbyssalPumpBlockEntity)l.getBlockEntity(p);
    }
    private static NaiadFluidPipeBlockEntity pipe(ServerLevel l,BlockPos p) {
        l.setBlock(p,ObsidiumMod.NAIAD_FLUID_PIPE_BLOCK.defaultBlockState(),3);return (NaiadFluidPipeBlockEntity)l.getBlockEntity(p);
    }
    private static VolcaniteEnergyWireBlockEntity wire(ServerLevel l,BlockPos p) {
        l.setBlock(p,ObsidiumMod.VOLCANITE_ENERGY_WIRE_BLOCK.defaultBlockState(),3);return (VolcaniteEnergyWireBlockEntity)l.getBlockEntity(p);
    }
    interface Checked {void run() throws Exception;}
    private static void probe(String name,Checked body) {
        System.out.println("AUDIT_CASE "+name);
        try {body.run();System.out.println("NEWCONTENT_OK_"+name);}catch(Throwable e){failures++;System.out.println("NEWCONTENT_FAILED_"+name);e.printStackTrace();}
    }
    private static void observed(boolean b,String details) {System.out.println("AUDIT_OBSERVED "+details);if(!b)throw new AssertionError(details);}
    private static final class FluidTank extends SingleVariantStorage<FluidVariant> {
        protected FluidVariant getBlankVariant(){return FluidVariant.blank();}
        protected long getCapacity(FluidVariant f){return 1000000;}
    }
}
