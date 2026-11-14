package net.valentin.obsidium.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.valentin.obsidium.block.DemetrianBonsaiPlanterBlock;
import net.valentin.obsidium.block.DemetrianBonsaiPlanterBlockEntity;
import net.valentin.obsidium.block.GraftedSaplingBlock;
import net.valentin.obsidium.block.GraftedSaplingBlockEntity;
import net.valentin.obsidium.block.VolcanicBeehiveBlockEntity;

public class VolcanicBee extends Bee {

    public VolcanicBee(EntityType<? extends Bee> entityType, Level level) {
        super(entityType, level);
        this.setPathfindingMalus(PathType.LAVA, 0.0F);
        this.setPathfindingMalus(PathType.DANGER_FIRE, 0.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, 0.0F);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Bee.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.FLYING_SPEED, 0.65)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // Spawn ember/flame particles around the volcanic bee
        if (this.level().isClientSide()) {
            if (this.random.nextFloat() < 0.25F) {
                double px = this.getX() + (this.random.nextDouble() - 0.5) * 0.4;
                double py = this.getY() + this.random.nextDouble() * 0.4;
                double pz = this.getZ() + (this.random.nextDouble() - 0.5) * 0.4;
                this.level().addParticle(ParticleTypes.FLAME, px, py, pz, 0.0, 0.01, 0.0);
            }
        } else {
            // When near a Volcanic Beehive, deposit nectar and enter hive
            if (this.hasNectar() && this.level().getBlockEntity(this.blockPosition()) instanceof VolcanicBeehiveBlockEntity hive) {
                if (!hive.isFull()) {
                    hive.addBee(this, true);
                    return;
                }
            } else if (this.hasNectar()) {
                // Check adjacent blocks for hive
                for (BlockPos neighbor : BlockPos.betweenClosed(this.blockPosition().offset(-1, -1, -1), this.blockPosition().offset(1, 1, 1))) {
                    if (this.level().getBlockEntity(neighbor) instanceof VolcanicBeehiveBlockEntity hive && !hive.isFull()) {
                        hive.addBee(this, true);
                        return;
                    }
                }
            }

            // When carrying volcanic nectar/pollen, pollinate nearby mineral saplings
            if (this.hasNectar() && this.tickCount % 40 == 0 && this.level() instanceof ServerLevel sl) {
                BlockPos pos = this.blockPosition();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -2; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            BlockPos target = pos.offset(dx, dy, dz);
                            BlockState state = sl.getBlockState(target);

                            // Accelerate GraftedSapling metamorphosis
                            if (state.getBlock() instanceof GraftedSaplingBlock) {
                                if (sl.getBlockEntity(target) instanceof GraftedSaplingBlockEntity be) {
                                    if (be.canMetamorphose()) {
                                        be.forceMetamorphosis();
                                    }
                                }
                            }

                            // Accelerate Demetrian Bonsai Planter growth
                            if (state.getBlock() instanceof DemetrianBonsaiPlanterBlock) {
                                if (sl.getBlockEntity(target) instanceof DemetrianBonsaiPlanterBlockEntity bonsai) {
                                    if (bonsai.hasSapling()) {
                                        bonsai.advanceGrowth(30);
                                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX() + 0.5, target.getY() + 0.4, target.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.02);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity living) {
            // Volcanic sting ignites target and applies fiery sting
            living.igniteForSeconds(4);
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.LAVA, target.getX(), target.getY() + 0.5, target.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
            }
        }
        return hurt;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BEE_LOOP;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BEE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BEE_DEATH;
    }
}
