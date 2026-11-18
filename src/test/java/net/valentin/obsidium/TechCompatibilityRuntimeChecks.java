package net.valentin.obsidium;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.block.*;
import team.reborn.energy.api.*;

/** Optional integration with the real, separately installed Tech Reborn mod. */
public final class TechCompatibilityRuntimeChecks implements ModInitializer {
    @Override public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            for(int x=1;x<=7;x++) for(int z=1;z<=7;z++) server.overworld().setChunkForced(x,z,true);
        });
        int[] ticks={0};
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if(++ticks[0]!=25) return;
            server.tickRateManager().setFrozen(true);
            try {
                check(FabricLoader.getInstance().isModLoaded("techreborn") && FabricLoader.getInstance().isModLoaded("reborncore"),"REAL_MODS_LOADED");
                ServerLevel level=server.overworld();
                for(int x=28;x<=104;x++) for(int z=28;z<=104;z++) level.setBlock(new BlockPos(x,99,z),Blocks.SMOOTH_STONE.defaultBlockState(),2);
                level.setDefaultSpawnPos(new BlockPos(40,101,40),0);
                FluidVariant water=FluidVariant.of(Fluids.WATER);

                BlockPos barrelPos=new BlockPos(32,100,32);
                var barrel=barrel(level,barrelPos);place(level,barrelPos.east(),"basic_tank_unit");
                Storage<FluidVariant> tank=FluidStorage.SIDED.find(level,barrelPos.east(),Direction.WEST);
                check(tank!=null && tank.supportsInsertion(),"TECH_TANK_EXPOSES_FABRIC_FLUID_STORAGE");
                barrel.insertFluid(water,1000,false);
                LiquidTankBlockEntity.tick(level,barrelPos,barrel.getBlockState(),barrel);
                check(barrel.getFluidAmount()==950 && amount(tank,water)==50*81L,"BARREL_TO_TECH_TANK_CONSERVES_FLUID");

                BlockPos pipePos=new BlockPos(48,100,32);
                var pipe=pipe(level,pipePos);place(level,pipePos.east(),"basic_tank_unit");
                var routed=pipe.getFluidStorage(Direction.WEST);
                var target=FluidStorage.SIDED.find(level,pipePos.east(),Direction.WEST);
                var source=barrel(level,pipePos.west());source.insertFluid(water,1000,false);
                try(Transaction tx=Transaction.openOuter()){StorageUtil.move(source,routed,f->f.equals(water),250*81L,tx);}
                check(source.getFluidAmount()==1000 && amount(target,water)==0,"TECH_FLUID_TRANSFER_ABORT_ROLLS_BACK");
                long moved;try(Transaction tx=Transaction.openOuter()){moved=StorageUtil.move(source,routed,f->f.equals(water),250*81L,tx);tx.commit();}
                check(moved==250*81L && source.getFluidAmount()==750 && amount(target,water)==moved,"NAIAD_TO_TECH_TANK_COMMITS_EXACTLY");
                try(Transaction tx=Transaction.openOuter()){check(routed.insert(water,81,tx)==0,"NAIAD_TICK_BUDGET_ALREADY_CONSUMED");}

                BlockPos pullPos=new BlockPos(64,100,32);
                var pullPipe=pipe(level,pullPos);place(level,pullPos.east(),"basic_tank_unit");
                var techSource=FluidStorage.SIDED.find(level,pullPos.east(),Direction.WEST);
                try(Transaction tx=Transaction.openOuter()){check(techSource.insert(water,1000*81L,tx)==1000*81L,"TECH_TANK_SEEDED");tx.commit();}
                var obsTarget=barrel(level,pullPos.west());pullPipe.setValveMode(Direction.EAST,NaiadFluidPipeBlockEntity.ValveMode.EXTRACT);
                NaiadFluidPipeBlockEntity.tick(level,pullPos,pullPipe.getBlockState(),pullPipe);
                check(obsTarget.getFluidAmount()==250 && amount(techSource,water)==750*81L,"TECH_TANK_TO_NAIAD_TO_BARREL");

                BlockPos wirePos=new BlockPos(32,100,64);
                var wire=wire(level,wirePos);place(level,wirePos.east(),"low_voltage_su");
                EnergyStorage techEnergy=EnergyStorage.SIDED.find(level,wirePos.east(),Direction.WEST);
                check(techEnergy!=null && techEnergy.supportsInsertion(),"TECH_ENERGY_STORAGE_DISCOVERED");
                long initial=techEnergy.getAmount();
                try(Transaction tx=Transaction.openOuter()){wire.getEnergyStorage(Direction.WEST).insert(1000,tx);}
                check(techEnergy.getAmount()==initial,"TECH_ENERGY_ABORT_ROLLS_BACK");
                long inserted;try(Transaction tx=Transaction.openOuter()){inserted=wire.getEnergyStorage(Direction.WEST).insert(1000,tx);tx.commit();}
                check(inserted>0 && techEnergy.getAmount()-initial==inserted,"ASTRAPE_TO_TECH_ENERGY_CONSERVES_RF");

                BlockPos suPos=new BlockPos(64,100,64);place(level,suPos,"low_voltage_su");
                EnergyStorage seed=EnergyStorage.SIDED.find(level,suPos,Direction.WEST);
                try(Transaction tx=Transaction.openOuter()){check(seed.insert(1000,tx)>0,"TECH_ENERGY_SOURCE_SEEDED");tx.commit();}
                Direction output=null;
                for(Direction d:Direction.values()) {
                    var storage=EnergyStorage.SIDED.find(level,suPos,d);
                    if(storage!=null && storage.supportsExtraction()){output=d;break;}
                }
                check(output!=null,"TECH_ENERGY_OUTPUT_DISCOVERED");
                BlockPos outWirePos=suPos.relative(output);var outWire=wire(level,outWirePos);
                BlockPos sinkPos=outWirePos.relative(output);level.setBlock(sinkPos,ObsidiumMod.VOLCANITE_FURNACE_BLOCK.defaultBlockState(),3);
                var sink=EnergyStorage.SIDED.find(level,sinkPos,output.getOpposite());
                var outputStorage=EnergyStorage.SIDED.find(level,suPos,output);long before=outputStorage.getAmount();
                outWire.setWireMode(output.getOpposite(),VolcaniteEnergyWireBlockEntity.WireMode.EXTRACT);
                VolcaniteEnergyWireBlockEntity.tick(level,outWirePos,outWire.getBlockState(),outWire);
                check(sink.getAmount()>0 && before-outputStorage.getAmount()==sink.getAmount(),"TECH_ENERGY_TO_ASTRAPE_TO_FURNACE");

                BlockPos itemPos=new BlockPos(48,100,80);
                level.setBlock(itemPos,ObsidiumMod.HERMES_ITEM_PIPE_BLOCK.defaultBlockState(),3);
                var hermes=(HermesItemPipeBlockEntity)level.getBlockEntity(itemPos);
                level.setBlock(itemPos.west(),Blocks.CHEST.defaultBlockState(),3);place(level,itemPos.east(),"electric_furnace");
                Container chest=VolcaniteStoragePipeBlockEntity.getContainerAt(level,itemPos.west());
                Container furnace=VolcaniteStoragePipeBlockEntity.getContainerAt(level,itemPos.east());
                check(furnace!=null,"TECH_MACHINE_EXPOSES_CONTAINER");chest.setItem(0,new ItemStack(Items.IRON_ORE,16));
                hermes.setModule(Direction.WEST,VolcaniteStoragePipeBlockEntity.ModuleType.EXTRACT);
                hermes.setModule(Direction.EAST,VolcaniteStoragePipeBlockEntity.ModuleType.INSERT);
                for(int i=0;i<5;i++) HermesItemPipeBlockEntity.tick(level,itemPos,hermes.getBlockState(),hermes);
                int received=0;for(int i=0;i<furnace.getContainerSize();i++)if(furnace.getItem(i).is(Items.IRON_ORE))received+=furnace.getItem(i).getCount();
                check(received==0 && chest.getItem(0).getCount()==16,"HERMES_RESPECTS_DISABLED_TECH_INPUT_SIDE");
                configureTechItemInput(level.getBlockEntity(itemPos.east()));
                for(int i=0;i<5;i++) HermesItemPipeBlockEntity.tick(level,itemPos,hermes.getBlockState(),hermes);
                received=0;for(int i=0;i<furnace.getContainerSize();i++)if(furnace.getItem(i).is(Items.IRON_ORE))received+=furnace.getItem(i).getCount();
                check(received>0 && received+chest.getItem(0).getCount()==16,"HERMES_TO_TECH_MACHINE_CONSERVES_ITEMS");
                System.out.println("TECHCOMPAT_ALL_PASSED");
            } catch(Throwable failure){failure.printStackTrace();System.out.println("TECHCOMPAT_FAILED");}
            finally{server.halt(false);}
        });
    }
    private static void place(ServerLevel level,BlockPos pos,String id){
        var key=ResourceLocation.fromNamespaceAndPath("techreborn",id);check(BuiltInRegistries.BLOCK.containsKey(key),"BLOCK_"+id.toUpperCase());
        level.setBlock(pos,BuiltInRegistries.BLOCK.get(key).defaultBlockState(),3);
        tickExternalBlock(level, pos);
    }
    private static void configureTechItemInput(net.minecraft.world.level.block.entity.BlockEntity entity) throws ReflectiveOperationException {
        // Equivalent to selecting INPUT for slot 0 / WEST in Tech Reborn's own side configuration.
        Object configuration=entity.getClass().getMethod("getSlotConfiguration").invoke(entity);
        Object holder=configuration.getClass().getMethod("getSlotDetails",int.class).invoke(configuration,0);
        Class<?> mode=Class.forName("reborncore.common.blockentity.SlotConfiguration$ExtractConfig");
        Class<?> io=Class.forName("reborncore.common.blockentity.SlotConfiguration$SlotIO");
        Class<?> slot=Class.forName("reborncore.common.blockentity.SlotConfiguration$SlotConfig");
        Object input=io.getConstructor(mode).newInstance(mode.getField("INPUT").get(null));
        Object side=slot.getConstructor(Direction.class,io,int.class).newInstance(Direction.WEST,input,0);
        holder.getClass().getMethod("updateSlotConfig",slot).invoke(holder,side);
        entity.setChanged();
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void tickExternalBlock(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        var entity = level.getBlockEntity(pos);
        if (entity != null && state.getBlock() instanceof EntityBlock block) {
            net.minecraft.world.level.block.entity.BlockEntityTicker ticker = block.getTicker(level, state,
                    (net.minecraft.world.level.block.entity.BlockEntityType) entity.getType());
            if (ticker != null) ticker.tick(level, pos, state, entity);
        }
    }
    private static long amount(Storage<FluidVariant> storage,FluidVariant fluid){long amount=0;for(var view:storage)if(view.getResource().equals(fluid))amount+=view.getAmount();return amount;}
    private static LiquidTankBlockEntity barrel(ServerLevel l,BlockPos p){l.setBlock(p,ObsidiumMod.VOLCANIC_WOOD_LIQUID_BARREL_BLOCK.defaultBlockState(),3);return (LiquidTankBlockEntity)l.getBlockEntity(p);}
    private static NaiadFluidPipeBlockEntity pipe(ServerLevel l,BlockPos p){l.setBlock(p,ObsidiumMod.NAIAD_FLUID_PIPE_BLOCK.defaultBlockState(),3);return (NaiadFluidPipeBlockEntity)l.getBlockEntity(p);}
    private static VolcaniteEnergyWireBlockEntity wire(ServerLevel l,BlockPos p){l.setBlock(p,ObsidiumMod.VOLCANITE_ENERGY_WIRE_BLOCK.defaultBlockState(),3);return (VolcaniteEnergyWireBlockEntity)l.getBlockEntity(p);}
    private static void check(boolean passed,String name){if(!passed)throw new AssertionError(name);System.out.println("TECHCOMPAT_OK_"+name);}
}
