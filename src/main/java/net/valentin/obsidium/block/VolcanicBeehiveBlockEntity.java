package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.entity.VolcanicBee;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class VolcanicBeehiveBlockEntity extends BlockEntity {
    public static final int MAX_BEES = 3;
    public static final int TICKS_BEFORE_HONEY = 600; // 30s to process nectar

    private final List<BeeData> storedBees = new ArrayList<>();

    public VolcanicBeehiveBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.VOLCANIC_BEEHIVE_BE, pos, state);
    }

    public boolean isFull() {
        return storedBees.size() >= MAX_BEES;
    }

    public int getBeeCount() {
        return storedBees.size();
    }

    public void addBee(Entity entity, boolean hasNectar) {
        if (storedBees.size() < MAX_BEES) {
            CompoundTag tag = new CompoundTag();
            entity.save(tag);
            storedBees.add(new BeeData(tag, 0, hasNectar ? TICKS_BEFORE_HONEY : 200));
            if (this.level != null) {
                BlockPos pos = this.getBlockPos();
                this.level.playSound(null, pos, SoundEvents.BEEHIVE_ENTER, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            entity.discard();
            setChanged();
        }
    }

    public void angerBees(Player player) {
        if (this.level instanceof ServerLevel sl) {
            releaseAllBees(sl, player);
        }
    }

    public void releaseAllBees(ServerLevel sl, Player angerTarget) {
        BlockState state = getBlockState();
        Direction facing = state.hasProperty(VolcanicBeehiveBlock.FACING) ? state.getValue(VolcanicBeehiveBlock.FACING) : Direction.NORTH;
        BlockPos exitPos = getBlockPos().relative(facing);

        Iterator<BeeData> it = storedBees.iterator();
        while (it.hasNext()) {
            BeeData data = it.next();
            spawnBee(sl, data.entityData, exitPos, angerTarget);
            it.remove();
        }
        setChanged();
    }

    private void spawnBee(ServerLevel sl, CompoundTag data, BlockPos pos, Player angerTarget) {
        Entity entity = EntityType.loadEntityRecursive(data, sl, e -> e);
        if (entity != null) {
            entity.moveTo(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, sl.random.nextFloat() * 360.0F, 0.0F);
            if (entity instanceof VolcanicBee bee) {
                bee.dropOffNectar();
                bee.setStayOutOfHiveCountdown(400);
                bee.setHivePos(this.getBlockPos());
                if (angerTarget != null) {
                    bee.setRemainingPersistentAngerTime(600);
                    bee.setPersistentAngerTarget(angerTarget.getUUID());
                    bee.setTarget(angerTarget);
                }
            }
            sl.addFreshEntity(entity);
            sl.playSound(null, pos, SoundEvents.BEEHIVE_EXIT, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel sl)) return;

        boolean changed = false;
        Iterator<BeeData> it = storedBees.iterator();

        while (it.hasNext()) {
            BeeData bee = it.next();
            bee.ticksInHive++;

            // Finished making honey
            if (bee.ticksInHive >= bee.minOccupationTicks) {
                int honey = state.getValue(VolcanicBeehiveBlock.HONEY_LEVEL);
                if (honey < 5) {
                    level.setBlock(pos, state.setValue(VolcanicBeehiveBlock.HONEY_LEVEL, honey + 1), 3);
                    state = level.getBlockState(pos);
                }

                // Release bee during day or if hive is disturbed
                Direction facing = state.hasProperty(VolcanicBeehiveBlock.FACING) ? state.getValue(VolcanicBeehiveBlock.FACING) : Direction.NORTH;
                BlockPos exitPos = pos.relative(facing);
                spawnBee(sl, bee.entityData, exitPos, null);
                it.remove();
                changed = true;
            }
        }

        if (changed) {
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag beesTag = new ListTag();
        for (BeeData bee : storedBees) {
            CompoundTag beeTag = new CompoundTag();
            beeTag.put("EntityData", bee.entityData);
            beeTag.putInt("TicksInHive", bee.ticksInHive);
            beeTag.putInt("MinOccupationTicks", bee.minOccupationTicks);
            beesTag.add(beeTag);
        }
        tag.put("Bees", beesTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        storedBees.clear();
        if (tag.contains("Bees", Tag.TAG_LIST)) {
            ListTag beesTag = tag.getList("Bees", Tag.TAG_COMPOUND);
            for (int i = 0; i < beesTag.size(); i++) {
                CompoundTag beeTag = beesTag.getCompound(i);
                storedBees.add(new BeeData(
                        beeTag.getCompound("EntityData"),
                        beeTag.getInt("TicksInHive"),
                        beeTag.getInt("MinOccupationTicks")
                ));
            }
        }
    }

    private static class BeeData {
        final CompoundTag entityData;
        int ticksInHive;
        final int minOccupationTicks;

        BeeData(CompoundTag entityData, int ticksInHive, int minOccupationTicks) {
            this.entityData = entityData;
            this.ticksInHive = ticksInHive;
            this.minOccupationTicks = minOccupationTicks;
        }
    }
}

