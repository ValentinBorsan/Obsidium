package net.valentin.obsidium.item;

import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.valentin.obsidium.ObsidiumMod;

import java.util.*;

public class TelluricAstrolabeItem extends Item {
    public static final ResourceKey<Biome> CALDERA_KEY = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "volcanic_caldera")
    );

    public TelluricAstrolabeItem(Properties properties) {
        super(properties.durability(128));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            BlockPos playerPos = player.blockPosition();

            // Shift + Right-Click: Lock on nearest Caldera (Compass Navigation)
            if (player.isShiftKeyDown()) {
                Pair<BlockPos, Holder<Biome>> caldera = serverLevel.findClosestBiome3d(
                        holder -> holder.is(CALDERA_KEY),
                        playerPos,
                        6400,
                        32,
                        64
                );

                if (caldera != null) {
                    BlockPos targetPos = caldera.getFirst();
                    setCalderaLock(stack, targetPos);
                    int dist = (int) Math.sqrt(playerPos.distSqr(targetPos));
                    String dir = PloutonRodItem.getDirectionString(playerPos, targetPos);

                    player.displayClientMessage(
                            Component.literal("§6★ [Telluric Astrolabe] §cVolcanic Caldera locked! §e" + dist + "m §7towards §a[" + dir + "] §8(X: " + targetPos.getX() + ", Z: " + targetPos.getZ() + ")"),
                            true
                    );
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0F, 1.2F);
                    spawnDirectionalParticles(serverLevel, playerPos, targetPos);
                    stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                } else {
                    player.displayClientMessage(
                            Component.literal("§6[Telluric Astrolabe] §cNo Volcanic Caldera detected within 6,400 blocks."),
                            true
                    );
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.0F);
                }

                player.getCooldowns().addCooldown(this, 20);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }

            // Normal Right-Click: Full Biome Scan (Nearby Biomes + Caldera Long-Range)
            performFullBiomeScan(serverLevel, player, stack, hand);
            player.getCooldowns().addCooldown(this, 25);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private void performFullBiomeScan(ServerLevel level, Player player, ItemStack stack, InteractionHand hand) {
        BlockPos playerPos = player.blockPosition();

        // 1. Scan nearby distinct biomes in a ~400 block radius
        Map<ResourceLocation, BiomeScanEntry> nearbyBiomes = new HashMap<>();
        int sampleRadius = 384;
        int step = 48;

        for (int dx = -sampleRadius; dx <= sampleRadius; dx += step) {
            for (int dz = -sampleRadius; dz <= sampleRadius; dz += step) {
                if (dx * dx + dz * dz > sampleRadius * sampleRadius) continue;

                BlockPos samplePos = playerPos.offset(dx, 0, dz);
                Holder<Biome> biomeHolder = level.getBiome(samplePos);
                Optional<ResourceKey<Biome>> keyOpt = biomeHolder.unwrapKey();
                if (keyOpt.isEmpty()) continue;

                ResourceLocation biomeId = keyOpt.get().location();
                int dist = (int) Math.sqrt(dx * dx + dz * dz);
                String dir = (dx == 0 && dz == 0) ? "Aici" : PloutonRodItem.getDirectionString(playerPos, samplePos);

                BiomeScanEntry existing = nearbyBiomes.get(biomeId);
                if (existing == null || dist < existing.distance) {
                    nearbyBiomes.put(biomeId, new BiomeScanEntry(biomeId, dist, dir));
                }
            }
        }

        // Sort by closest distance
        List<BiomeScanEntry> sortedBiomes = new ArrayList<>(nearbyBiomes.values());
        sortedBiomes.sort(Comparator.comparingInt(b -> b.distance));

        // 2. Long-Range Scan for Volcanic Caldera (up to 6,400 blocks)
        Pair<BlockPos, Holder<Biome>> calderaResult = level.findClosestBiome3d(
                holder -> holder.is(CALDERA_KEY),
                playerPos,
                6400,
                32,
                64
        );

        // 3. Play celestial resonance sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.3F);

        // 4. Print clean, structured output to Chat
        player.sendSystemMessage(Component.literal("§8§m--------------------------------------------------"));
        player.sendSystemMessage(Component.literal(" §6✦ §e§lTelluric Astrolabe §8— §7Biome Cartography"));
        player.sendSystemMessage(Component.literal(" §7Current position: §8X: " + playerPos.getX() + ", Z: " + playerPos.getZ()));
        player.sendSystemMessage(Component.literal(""));

        player.sendSystemMessage(Component.literal(" §6Nearby Biomes Detected §8(~400 blocks):"));
        int count = 0;
        for (BiomeScanEntry entry : sortedBiomes) {
            if (count++ >= 8) break; // Display top 8 closest distinct biomes

            Component biomeName = Component.translatable(Util.makeDescriptionId("biome", entry.id));
            String distText = entry.distance == 0 ? "§a[Current Biome]" : "§e" + entry.distance + "m §7towards §b[" + entry.direction + "]";
            player.sendSystemMessage(
                    Component.literal("  §8• ").append(biomeName.copy().withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal(" §8— " + distText))
            );
        }

        player.sendSystemMessage(Component.literal(""));

        // Caldera report
        if (calderaResult != null) {
            BlockPos cPos = calderaResult.getFirst();
            int cDist = (int) Math.sqrt(playerPos.distSqr(cPos));
            String cDir = PloutonRodItem.getDirectionString(playerPos, cPos);

            player.sendSystemMessage(Component.literal(" §6★ §c§lVolcanic Caldera §e(Volcanite Deposit):"));
            player.sendSystemMessage(Component.literal("    §7Distance: §6§l" + cDist + "m §8| §7Direction: §a§l[" + cDir + "] §8| §7Coordinates: §8X: " + cPos.getX() + ", Z: " + cPos.getZ()));
            player.sendSystemMessage(Component.literal("    §8(Use §eShift + Right-Click §8to lock compass onto the Caldera)"));

            // Particle trail towards Caldera
            spawnDirectionalParticles(level, playerPos, cPos);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.7F, 1.1F);
        } else {
            player.sendSystemMessage(Component.literal(" §7• §cVolcanic Caldera: §7None detected within 6,400 blocks."));
        }

        player.sendSystemMessage(Component.literal("§8§m--------------------------------------------------"));

        // Consume durability
        stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
    }

    private static void spawnDirectionalParticles(ServerLevel level, BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0) return;

        double normX = dx / len;
        double normZ = dz / len;

        for (int step = 1; step <= 8; step++) {
            double px = from.getX() + 0.5 + normX * step * 0.75;
            double py = from.getY() + 1.2;
            double pz = from.getZ() + 0.5 + normZ * step * 0.75;

            level.sendParticles(ParticleTypes.FLAME, px, py, pz, 1, 0.05, 0.05, 0.05, 0.01);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py + 0.1, pz, 1, 0.02, 0.02, 0.02, 0.005);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide() && isSelected && entity instanceof Player player && level.getGameTime() % 40 == 0) {
            BlockPos target = getCalderaLock(stack);
            if (target != null) {
                int dist = (int) Math.sqrt(player.blockPosition().distSqr(target));
                String dir = PloutonRodItem.getDirectionString(player.blockPosition(), target);
                player.displayClientMessage(
                        Component.literal("§6★ [Calderă] §e" + dist + "m §7spre §a[" + dir + "] §8(" + target.getX() + ", " + target.getZ() + ")"),
                        true
                );
            }
        }
    }

    public static void setCalderaLock(ItemStack stack, BlockPos pos) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt("LockX", pos.getX());
            tag.putInt("LockZ", pos.getZ());
            tag.putBoolean("HasLock", true);
        });
    }

    public static BlockPos getCalderaLock(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        if (tag.getBoolean("HasLock")) {
            return new BlockPos(tag.getInt("LockX"), 64, tag.getInt("LockZ"));
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6Ancient Telluric Astrolabe forged for exploration:").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§7• Right-Click: Scans all nearby biomes (~400m)").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7  and locates Volcanic Calderas up to §66,400 blocks§7.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7• Shift + Right-Click: Locks compass onto Caldera.").withStyle(ChatFormatting.GRAY));

        BlockPos lock = getCalderaLock(stack);
        if (lock != null) {
            tooltip.add(Component.literal("§6★ Locked Caldera: §eX: " + lock.getX() + ", Z: " + lock.getZ()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    private static class BiomeScanEntry {
        final ResourceLocation id;
        final int distance;
        final String direction;

        BiomeScanEntry(ResourceLocation id, int distance, String direction) {
            this.id = id;
            this.distance = distance;
            this.direction = direction;
        }
    }
}

