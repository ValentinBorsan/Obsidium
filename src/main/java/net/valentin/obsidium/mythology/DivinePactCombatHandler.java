package net.valentin.obsidium.mythology;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Optional;

public final class DivinePactCombatHandler {
    private static final ThreadLocal<Boolean> IS_PROCESSING_PACT = ThreadLocal.withInitial(() -> false);

    private DivinePactCombatHandler() {}

    public static void register() {
        // 1. Damage Hook: Attack & Impact (Zeus, Apollo, Poseidon) and Defense (Athena Aegis Bastion)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity.level().isClientSide() || !(entity.level() instanceof ServerLevel level)) return true;
            if (IS_PROCESSING_PACT.get()) return true;

            IS_PROCESSING_PACT.set(true);
            try {
                // Attacker hooks (Zeus, Apollo, Poseidon)
                if (source.getEntity() instanceof Player player) {
                    ItemStack weapon = player.getMainHandItem();
                    DivinePact pact = DivinePactHelper.getPact(weapon);
                    if (pact != DivinePact.NONE) {
                        switch (pact) {
                            case ZEUS -> triggerZeusStrike(level, player, entity);
                            case APOLLO -> triggerApolloRadiance(level, player, entity);
                            case POSEIDON -> triggerPoseidonTide(level, player, entity);
                            default -> {}
                        }
                    }
                }

                // Defender hooks (Athena Aegis Bastion)
                if (entity instanceof Player player) {
                    ItemStack offhand = player.getOffhandItem();
                    ItemStack mainhand = player.getMainHandItem();
                    boolean hasAthena = DivinePactHelper.getPact(offhand) == DivinePact.ATHENA ||
                            DivinePactHelper.getPact(mainhand) == DivinePact.ATHENA;

                    if (hasAthena && player.getRandom().nextFloat() < net.valentin.obsidium.config.ModConfig.get().athenaShieldBlockChance) {
                        // Aegis Bastion Shield Flash: Repels and blunts damage
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.4F);
                        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                                player.getX(), player.getY() + 1.0, player.getZ(),
                                12, 0.4, 0.6, 0.4, 0.05);
                        if (source.getEntity() instanceof LivingEntity attacker) {
                            Vec3 push = attacker.position().subtract(player.position()).normalize().scale(0.8);
                            attacker.setDeltaMovement(push.x, 0.25, push.z);
                            attacker.hurtMarked = true;
                        }
                        return false; // Nullify damage
                    }
                }
            } finally {
                IS_PROCESSING_PACT.set(false);
            }

            return true;
        });

        // 2. Kill Hook (Thanatos Soul Siphon)
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity.level().isClientSide() || !(entity.level() instanceof ServerLevel level)) return;
            if (source.getEntity() instanceof Player player) {
                ItemStack weapon = player.getMainHandItem();
                if (DivinePactHelper.getPact(weapon) == DivinePact.THANATOS) {
                    // Soul Siphon: Heal player based on config on kill
                    player.heal(net.valentin.obsidium.config.ModConfig.get().thanatosSoulSiphonHeal);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.8F, 1.2F);
                    level.sendParticles(ParticleTypes.SOUL,
                            entity.getX(), entity.getY() + 1.0, entity.getZ(),
                            15, 0.3, 0.5, 0.3, 0.05);
                }
            }
        });

        // 4. Mining Hook (Hephaestus Auto-Smelt)
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return true;
            ItemStack tool = player.getMainHandItem();
            if (DivinePactHelper.getPact(tool) == DivinePact.HEPHAESTUS) {
                // Check if drops can be smelted
                List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, blockEntity, player, tool);
                boolean smeltedAny = false;

                for (ItemStack drop : drops) {
                    Optional<net.minecraft.world.item.crafting.RecipeHolder<SmeltingRecipe>> recipe =
                            serverLevel.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(drop), serverLevel);

                    if (recipe.isPresent()) {
                        ItemStack result = recipe.get().value().assemble(new SingleRecipeInput(drop), serverLevel.registryAccess());
                        if (!result.isEmpty()) {
                            result.setCount(result.getCount() * drop.getCount());
                            Block.popResource(serverLevel, pos, result);
                            smeltedAny = true;
                        }
                    } else {
                        Block.popResource(serverLevel, pos, drop);
                    }
                }

                if (smeltedAny) {
                    serverLevel.sendParticles(ParticleTypes.FLAME,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            8, 0.2, 0.2, 0.2, 0.02);
                    serverLevel.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.4F, 1.3F);
                    level.destroyBlock(pos, false, player);
                    return false; // Handled custom drops
                }
            }
            return true;
        });
    }

    private static void triggerZeusStrike(ServerLevel level, Player player, LivingEntity target) {
        float chance = level.isThundering() ? 1.0F : net.valentin.obsidium.config.ModConfig.get().zeusChainLightningChance;
        if (level.random.nextFloat() <= chance) {
            // Target lightning spark
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    15, 0.3, 0.5, 0.3, 0.08);
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.5F, 1.8F);

            // Additional 4.0 electric damage
            target.hurt(level.damageSources().magic(), 4.0F);

            // Chain lightning to up to 3 nearby hostiles
            AABB searchArea = target.getBoundingBox().inflate(5.0);
            List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, searchArea, e ->
                    e != player && e != target && e.isAlive() && (e instanceof Enemy));

            int chained = 0;
            for (LivingEntity chainTarget : nearby) {
                if (chained >= 3) break;
                chainTarget.hurt(level.damageSources().magic(), 3.0F);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        chainTarget.getX(), chainTarget.getY() + 1.0, chainTarget.getZ(),
                        8, 0.2, 0.4, 0.2, 0.05);
                chained++;
            }
        }
    }

    private static void triggerApolloRadiance(ServerLevel level, Player player, LivingEntity target) {
        // True sun fire
        target.igniteForSeconds(6);
        target.hurt(level.damageSources().onFire(), 3.5F);
        level.sendParticles(ParticleTypes.FLAME,
                target.getX(), target.getY() + 1.0, target.getZ(),
                20, 0.3, 0.6, 0.3, 0.05);
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    private static void triggerPoseidonTide(ServerLevel level, Player player, LivingEntity target) {
        if (player.isInWaterOrRain() || target.isInWaterOrRain()) {
            // Massive tidal surge (+6.0 bonus damage in rain/water)
            target.hurt(level.damageSources().playerAttack(player), 6.0F);
            Vec3 knock = target.position().subtract(player.position()).normalize().scale(1.2).add(0, 0.3, 0);
            target.setDeltaMovement(knock);
            target.hurtMarked = true;
            level.sendParticles(ParticleTypes.SPLASH,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    30, 0.4, 0.5, 0.4, 0.1);
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 0.8F, 1.2F);
        }
    }
}
