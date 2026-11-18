package net.valentin.obsidium;

import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.valentin.obsidium.block.*;

/** Positive regression checks in a temporary server world, including a restart. */
public final class PastoralRuntimeChecks implements ModInitializer {
    private static final BlockPos OVERFLOW = new BlockPos(144,80,80);
    private static final BlockPos RECORD = OVERFLOW.above(3);
    private static int checks;

    @Override public void onInitialize() {
        int[] ticks = {0};
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks[0] != 60) return;
            server.tickRateManager().setFrozen(true);
            try {
                ServerLevel level = server.overworld();
                if (level.getGameTime()%60 != 0) throw new AssertionError("Unexpected clock: " + level.getGameTime());
                if (level.getBlockEntity(RECORD) instanceof ChestBlockEntity) {
                    afterRestart(level);
                    System.out.println("PASTORAL_ALL_PASSED checks="+checks);
                } else {
                    beforeRestart(level);
                    System.out.println("PASTORAL_RESTART_READY checks="+checks);
                }
            } catch (Throwable error) {
                System.out.println("PASTORAL_FAILED"); error.printStackTrace();
            }
            server.halt(false);
        });
    }

    private static PanHarvesterBlockEntity pan(ServerLevel level, BlockPos pos) {
        level.setBlock(pos,ObsidiumMod.PAN_HARVESTER.defaultBlockState(),3);
        return (PanHarvesterBlockEntity) level.getBlockEntity(pos);
    }
    private static void tick(ServerLevel level, BlockPos pos, PanHarvesterBlockEntity pan) {
        PanHarvesterBlockEntity.serverTick(level,pos,level.getBlockState(pos),pan);
    }
    @SuppressWarnings("unchecked")
    private static List<ItemStack> pending(PanHarvesterBlockEntity pan) throws Exception {
        var field = PanHarvesterBlockEntity.class.getDeclaredField("pendingDrops"); field.setAccessible(true);
        return (List<ItemStack>) field.get(pan);
    }
    private static int total(Container container) {
        int n=0; for(int i=0;i<container.getContainerSize();i++) n+=container.getItem(i).getCount(); return n;
    }
    private static Cow cow(ServerLevel level,BlockPos pos) {
        Cow cow=EntityType.COW.create(level); cow.moveTo(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,0,0);level.addFreshEntity(cow);return cow;
    }
    private static long liveCows(ServerLevel level,BlockPos pos) {
        return level.getEntitiesOfClass(Cow.class,new AABB(pos).inflate(4)).stream().filter(Cow::isAlive).count();
    }
    private static void beforeRestart(ServerLevel level) throws Exception {
        BlockPos p=new BlockPos(80,80,80);var export=pan(level,p);
        level.setBlock(p.east(),Blocks.CHEST.defaultBlockState(),3);var chest=(ChestBlockEntity)level.getBlockEntity(p.east());
        export.setItem(0,new ItemStack(Items.LEATHER,8));tick(level,p,export);
        check(export.isEmpty()&&total(chest)==8&&export.energyStorage.amount==0,"ZERO_RF_EXPORT");
        export.setItem(0,new ItemStack(Items.LEATHER,4));level.setBlock(p.above(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);tick(level,p,export);
        check(export.getItem(0).getCount()==4&&total(chest)==8,"REDSTONE_PAUSES_EXPORT");
        level.removeBlock(p.above(),false);

        BlockPos h=new BlockPos(112,80,80);var protectedPan=pan(level,h);protectedPan.energyStorage.amount=1200;
        for(int i=0;i<3;i++) {
            Horse horse=EntityType.HORSE.create(level);horse.setTamed(true);horse.moveTo(h.getX()+.5,h.getY()+1,h.getZ()+.5,0,0);level.addFreshEntity(horse);
            check(!PanHarvesterBlockEntity.isEligible(horse),"TAMED_HORSE_PROTECTED_"+i);
        }
        Cow named=cow(level,h);named.setCustomName(Component.literal("Keep me"));check(!PanHarvesterBlockEntity.isEligible(named),"NAMED_COW_PROTECTED");
        Cow baby=cow(level,h);baby.setAge(-24000);check(!PanHarvesterBlockEntity.isEligible(baby),"BABY_PROTECTED");
        var wolf=EntityType.WOLF.create(level);wolf.setTame(true,false);check(!PanHarvesterBlockEntity.isEligible(wolf),"TAMED_WOLF_PROTECTED");
        tick(level,h,protectedPan);check(named.isAlive()&&baby.isAlive()&&protectedPan.energyStorage.amount==1200,"PROTECTED_ANIMALS_NOT_HARVESTED");

        var full=pan(level,OVERFLOW);for(int i=0;i<9;i++)full.setItem(i,new ItemStack(Items.COBBLESTONE,64));full.energyStorage.amount=1200;
        for(int i=0;i<3;i++)cow(level,OVERFLOW);
        tick(level,OVERFLOW,full);check(full.energyStorage.amount==1200&&liveCows(level,OVERFLOW)==3,"FULL_BUFFER_STOPS_HARVEST");
        full.setItem(0,new ItemStack(Items.COBBLESTONE,63));tick(level,OVERFLOW,full);
        check(full.energyStorage.amount==0&&liveCows(level,OVERFLOW)==2,"SURPLUS_ONLY_HARVESTED");
        int queued=pending(full).stream().mapToInt(ItemStack::getCount).sum();check(queued>0,"VARIABLE_LOOT_QUEUED");
        check(level.getEntitiesOfClass(ItemEntity.class,new AABB(OVERFLOW).inflate(6)).isEmpty(),"HARVEST_LOOT_NOT_LEFT_IN_WORLD");
        full.energyStorage.amount=1200;tick(level,OVERFLOW,full);check(full.energyStorage.amount==1200&&liveCows(level,OVERFLOW)==2,"QUEUE_BLOCKS_NEW_HARVEST");
        level.setBlock(RECORD,Blocks.CHEST.defaultBlockState(),3);((ChestBlockEntity)level.getBlockEntity(RECORD)).setItem(0,new ItemStack(Items.PAPER,queued));
        var copied=new PanHarvesterBlockEntity(OVERFLOW,level.getBlockState(OVERFLOW));copied.loadWithComponents(full.saveWithFullMetadata(level.registryAccess()),level.registryAccess());
        check(pending(copied).stream().mapToInt(ItemStack::getCount).sum()==queued,"QUEUE_NBT_ROUNDTRIP");

        BlockPos a=new BlockPos(176,80,80);level.setBlock(a,ObsidiumMod.AMBROSIA_FEEDER.defaultBlockState(),3);var feeder=(AmbrosiaFeederBlockEntity)level.getBlockEntity(a);
        var insert=AmbrosiaFeederBlockEntity.class.getDeclaredMethod("insertOutput",ItemStack.class);insert.setAccessible(true);
        for(int i=6;i<9;i++)feeder.setItem(i,new ItemStack(Items.WHITE_WOOL,63));
        check((boolean)insert.invoke(feeder,new ItemStack(Items.WHITE_WOOL,3))&&feeder.getItem(6).getCount()==64&&feeder.getItem(8).getCount()==64,"FRAGMENTED_SPACE_MERGED");
        feeder.setItem(6,new ItemStack(Items.WHITE_WOOL,63));
        check(!(boolean)insert.invoke(feeder,new ItemStack(Items.WHITE_WOOL,2))&&feeder.getItem(6).getCount()==63,"FAILED_INSERT_ATOMIC");
        var turtle=EntityType.TURTLE.create(level);check(turtle.isFood(new ItemStack(Items.SEAGRASS))&&AmbrosiaFeederBlockEntity.isBreedingFood(new ItemStack(Items.SEAGRASS)),"TURTLE_FOOD_ACCEPTED");
        check(AmbrosiaFeederBlockEntity.isBreedingFood(new ItemStack(Items.GOLDEN_CARROT))&&AmbrosiaFeederBlockEntity.isBreedingFood(new ItemStack(Items.BEEF)),"VANILLA_FOOD_TAGS");
        check(!AmbrosiaFeederBlockEntity.isBreedingFood(new ItemStack(Items.COBBLESTONE)),"NONFOOD_REJECTED");
        try(var tx=Transaction.openOuter()) {
            var storage=ItemStorage.SIDED.find(level,a,Direction.UP);
            check(storage.insert(ItemVariant.of(Items.SEAGRASS),4,tx)==4,"FABRIC_FOOD_INSERT");
        }
        check(feeder.getItem(0).isEmpty(),"FABRIC_ABORT_RESTORES_INPUT");
        feeder.energyStorage.amount=123400;
        var restored=new AmbrosiaFeederBlockEntity(a,level.getBlockState(a));restored.loadWithComponents(feeder.saveWithFullMetadata(level.registryAccess()),level.registryAccess());
        check(restored.getItem(6).getCount()==63&&restored.energyStorage.amount==123400,"FEEDER_NBT_CONTROL");
        var area=Class.forName("net.valentin.obsidium.block.PastoralInventory").getDeclaredMethod("workArea",BlockPos.class);area.setAccessible(true);AABB box=(AABB)area.invoke(null,a);
        check(box.getXsize()==9&&box.getYsize()==4&&box.getZsize()==9,"AREA_MATCHES_9_4_9");
        try(var capture=PastoralHarvestDrops.begin(turtle)) {turtle.spawnAtLocation(new ItemStack(Items.DIAMOND,2));check(capture.items().size()==1,"DROP_SCOPE_CAPTURES_TARGET");}
        ItemEntity normal=turtle.spawnAtLocation(new ItemStack(Items.DIAMOND,1));check(normal!=null&&level.getEntity(normal.getUUID())==normal,"NORMAL_DROPS_UNAFFECTED");normal.discard();

        var axolotl=EntityType.AXOLOTL.create(level);
        var consume=AmbrosiaFeederBlockEntity.class.getDeclaredMethod("consumeMatchingFood",net.minecraft.world.entity.animal.Animal.class);consume.setAccessible(true);
        feeder.clearContent();feeder.setItem(0,new ItemStack(Items.TROPICAL_FISH_BUCKET));
        for(int i=6;i<9;i++)feeder.setItem(i,new ItemStack(Items.COBBLESTONE,64));
        check(!(boolean)consume.invoke(feeder,axolotl)&&feeder.getItem(0).is(Items.TROPICAL_FISH_BUCKET),"FOOD_CONTAINER_FULL_OUTPUT_NO_LOSS");
        feeder.setItem(6,ItemStack.EMPTY);
        check((boolean)consume.invoke(feeder,axolotl)&&feeder.getItem(0).isEmpty()&&feeder.getItem(6).is(Items.WATER_BUCKET),"FOOD_CONTAINER_RETURNED");
        BlockPos w=new BlockPos(208,80,80);level.setBlock(w,ObsidiumMod.AEGIS_SANCTUARY_WARD.defaultBlockState(),3);var ward=(AegisSanctuaryWardBlockEntity)level.getBlockEntity(w);ward.energyStorage.amount=1600;
        AegisSanctuaryWardBlockEntity.serverTick(level,w,level.getBlockState(w),ward);check(ward.isActive()&&ward.energyStorage.amount==800,"WARD_ACTIVE_COST");
        check(AegisSanctuaryWardBlockEntity.isProtected(level,w.offset(48,24,48))&&!AegisSanctuaryWardBlockEntity.isProtected(level,w.offset(49,0,0)),"WARD_BOUNDARY");
        ward.toggleManualEnabled();AegisSanctuaryWardBlockEntity.serverTick(level,w,level.getBlockState(w),ward);check(!AegisSanctuaryWardBlockEntity.isProtected(level,w),"WARD_MANUAL_OFF");
        ward.toggleManualEnabled();level.setBlock(w.east(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);AegisSanctuaryWardBlockEntity.serverTick(level,w,level.getBlockState(w),ward);check(!ward.isActive()&&ward.energyStorage.amount==800,"WARD_REDSTONE");
    }
    private static void afterRestart(ServerLevel level) throws Exception {
        var restored=(PanHarvesterBlockEntity)level.getBlockEntity(OVERFLOW);check(restored!=null,"RESTART_CONTROLLER_PRESENT");
        int expected=((ChestBlockEntity)level.getBlockEntity(RECORD)).getItem(0).getCount();
        check(pending(restored).stream().mapToInt(ItemStack::getCount).sum()==expected,"RESTART_LOOT_PRESERVED");
        restored.clearContent();restored.energyStorage.amount=0;tick(level,OVERFLOW,restored);
        check(pending(restored).isEmpty()&&total(restored)==expected,"RESTART_QUEUE_DRAINS_WITHOUT_RF");
        var drops=pending(restored);drops.add(new ItemStack(Items.DIAMOND,3));level.removeBlock(OVERFLOW,false);
        check(level.getEntitiesOfClass(ItemEntity.class,new AABB(OVERFLOW).inflate(3)).stream().filter(e->e.getItem().is(Items.DIAMOND)).mapToInt(e->e.getItem().getCount()).sum()==3,"BREAK_DROPS_PENDING_ONCE");
    }
    private static void check(boolean value,String name) {if(!value)throw new AssertionError(name);checks++;System.out.println("PASTORAL_OK_"+name);}
}
