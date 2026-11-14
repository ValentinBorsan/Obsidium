package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.MythicScrollItem;
import net.valentin.obsidium.mythology.DivinePact;
import net.valentin.obsidium.mythology.DivinePactHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class OlympianAltarBlockEntity extends BlockEntity implements Clearable {
    private ItemStack placedItem = ItemStack.EMPTY;
    private int ritualProgress = 0;
    private boolean isRitualRunning = false;
    private DivinePact targetPact = DivinePact.NONE;
    private final List<BlockPos> cachedObelisks = new ArrayList<>();

    public OlympianAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.OLYMPIAN_ALTAR_BE, pos, state);
    }

    public ItemStack getPlacedItem() {
        return placedItem;
    }

    public void setPlacedItem(ItemStack stack) {
        this.placedItem = stack;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public ItemStack removePlacedItem() {
        ItemStack item = this.placedItem;
        this.placedItem = ItemStack.EMPTY;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return item;
    }

    public boolean isRitualRunning() {
        return isRitualRunning;
    }

    public int getRitualProgress() {
        return ritualProgress;
    }

    public DivinePact getTargetPact() {
        return targetPact;
    }

    public void tryStartRitual(Player player) {
        if (level == null || level.isClientSide() || isRitualRunning) return;

        if (placedItem.isEmpty()) {
            player.displayClientMessage(Component.literal("§cPlace a weapon, tool, or armor piece on the Altar first (Right-Click Altar with item)!"), true);
            return;
        }

        if (DivinePactHelper.hasPact(placedItem)) {
            player.displayClientMessage(Component.literal("§cThis item is already bound to a Divine Pact! (Only 1 Pact allowed per item)"), true);
            return;
        }

        // Find 4 Astrape Ritual Obelisks in surrounding area (radius 4, height -1..+1)
        // Supports Cardinal Cross (+), Diagonal (X), and natural uneven terrain!
        cachedObelisks.clear();
        MythicScrollItem scrollItem = null;

        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (dx == 0 && dz == 0) continue;
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos p = worldPosition.offset(dx, dy, dz);
                    if (level.getBlockEntity(p) instanceof AstrapeRitualObeliskBlockEntity obelisk) {
                        if (!cachedObelisks.contains(p)) {
                            cachedObelisks.add(p);
                            ItemStack stack = obelisk.getHeldItem();
                            if (stack.getItem() instanceof MythicScrollItem msi) {
                                scrollItem = msi;
                            }
                        }
                    }
                }
            }
        }

        if (cachedObelisks.size() < 4) {
            player.displayClientMessage(Component.literal("§cIncomplete Multiblock! Found §e" + cachedObelisks.size() + "§c/4 Astrape Ritual Obelisks (Place 4 Obelisks within 3–4 blocks of the Altar)."), true);
            return;
        }

        if (scrollItem == null) {
            player.displayClientMessage(Component.literal("§cPlace a Mythic Scroll (e.g. Scroll of Zeus/Apollo) on one of the 4 Obelisks! (Right-Click Obelisk with Scroll)"), true);
            return;
        }

        DivinePact pact = scrollItem.getPact();

        // Validate Weather / Celestial Conditions for the God's Pact
        if (!validatePactConditions(pact, player)) {
            return;
        }

        // Begin Ritual
        this.targetPact = pact;
        this.ritualProgress = 0;
        this.isRitualRunning = true;
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.2F);
        player.displayClientMessage(Component.literal("§6⚡ The Olympian Ritual has commenced! The celestial forces answer..."), true);
    }

    private boolean validatePactConditions(DivinePact pact, Player player) {
        if (level == null) return false;

        switch (pact) {
            case ZEUS -> {
                if (!level.isThundering()) {
                    player.displayClientMessage(Component.literal("§cThe Wrath of Zeus requires an active Thunderstorm with Lightning!"), true);
                    return false;
                }
            }
            case APOLLO -> {
                if (!level.isDay() || level.isRaining() || !level.canSeeSky(worldPosition.above())) {
                    player.displayClientMessage(Component.literal("§cThe Radiance of Apollo requires a Clear Midday Sky under direct sunlight!"), true);
                    return false;
                }
            }
            case ARTEMIS -> {
                if (!level.isNight() || level.isRaining() || !level.canSeeSky(worldPosition.above())) {
                    player.displayClientMessage(Component.literal("§cThe Grace of Artemis requires a Clear Night Sky under moonlight!"), true);
                    return false;
                }
            }
            case POSEIDON -> {
                if (!level.isRaining()) {
                    player.displayClientMessage(Component.literal("§cThe Fury of Poseidon requires active Rain or Storm above the altar!"), true);
                    return false;
                }
            }
            case HEPHAESTUS -> {
                BlockState below = level.getBlockState(worldPosition.below());
                boolean hasHeat = below.is(Blocks.LAVA) || below.is(Blocks.MAGMA_BLOCK) ||
                        below.getBlock() == ObsidiumMod.VOLCANITE_ENERGY_CORE_BLOCK ||
                        level.dimension() == Level.NETHER;
                if (!hasHeat) {
                    player.displayClientMessage(Component.literal("§cThe Fire of Hephaestus requires active Lava or Magma directly beneath the Altar!"), true);
                    return false;
                }
            }
            case THANATOS -> {
                boolean isUnderworld = worldPosition.getY() < 0 || level.dimension() == Level.NETHER;
                if (!isUnderworld) {
                    player.displayClientMessage(Component.literal("§cThe Scythe of Thanatos requires the Depths of the Abyss (below Y=0 or Nether)!"), true);
                    return false;
                }
            }
            case ATHENA -> {
                if (worldPosition.getY() < 90) {
                    player.displayClientMessage(Component.literal("§cThe Wisdom of Athena requires High Olympian Altitude (Y > 90 Mountain Peak)!"), true);
                    return false;
                }
            }
            default -> {}
        }
        return true;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, OlympianAltarBlockEntity altar) {
        if (!altar.isRitualRunning) return;

        altar.ritualProgress++;

        if (level instanceof ServerLevel serverLevel) {
            // Beams converging from Obelisks to Altar
            for (BlockPos obeliskPos : altar.cachedObelisks) {
                if (altar.ritualProgress % 3 == 0) {
                    Vec3 start = Vec3.atCenterOf(obeliskPos).add(0, 0.8, 0);
                    Vec3 end = Vec3.atCenterOf(pos).add(0, 0.9, 0);
                    Vec3 dir = end.subtract(start).normalize().scale(0.3);

                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            start.x, start.y, start.z, 2, dir.x, dir.y, dir.z, 0.1);
                    serverLevel.sendParticles(ParticleTypes.ENCHANT,
                            end.x + (serverLevel.random.nextDouble() - 0.5) * 0.4,
                            end.y + serverLevel.random.nextDouble() * 0.5,
                            end.z + (serverLevel.random.nextDouble() - 0.5) * 0.4,
                            3, 0, 0.05, 0, 0.02);
                }
            }

            if (altar.ritualProgress % 15 == 0) {
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7F, 1.0F + (altar.ritualProgress / 70.0F));
            }

            // Divine Phenomenon Climax at progress 50
            if (altar.ritualProgress == 50) {
                triggerDivinePhenomenon(serverLevel, pos, altar.targetPact);
            }

            // Completion based on configured duration
            int duration = net.valentin.obsidium.config.ModConfig.get().altarRitualDurationTicks;
            if (altar.ritualProgress >= duration) {
                altar.completeRitual(serverLevel, pos);
            }
        }
    }

    private static void triggerDivinePhenomenon(ServerLevel level, BlockPos pos, DivinePact pact) {
        switch (pact) {
            case ZEUS -> {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(Vec3.atBottomCenterOf(pos));
                    bolt.setVisualOnly(!net.valentin.obsidium.config.ModConfig.get().zeusSummonsTrueLightning);
                    level.addFreshEntity(bolt);
                }
            }
            case APOLLO -> {
                level.sendParticles(ParticleTypes.FLASH, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 30, 0.4, 0.6, 0.4, 0.08);
            }
            case ARTEMIS -> {
                level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 25, 0.3, 0.5, 0.3, 0.05);
            }
            case POSEIDON -> {
                level.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 40, 0.4, 0.5, 0.4, 0.1);
            }
            case HEPHAESTUS -> {
                level.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 20, 0.3, 0.4, 0.3, 0.05);
            }
            case THANATOS -> {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 25, 0.3, 0.4, 0.3, 0.05);
            }
            case ATHENA -> {
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 20, 0.3, 0.4, 0.3, 0.05);
            }
            default -> {}
        }
    }

    private void completeRitual(ServerLevel level, BlockPos pos) {
        this.isRitualRunning = false;
        this.ritualProgress = 0;

        if (!placedItem.isEmpty() && targetPact != DivinePact.NONE) {
            DivinePactHelper.setPact(placedItem, targetPact);

            // Grant Olympian Odyssey quest advancements to nearby worshippers
            for (net.minecraft.server.level.ServerPlayer worshipper : level.getEntitiesOfClass(
                    net.minecraft.server.level.ServerPlayer.class, new net.minecraft.world.phys.AABB(pos).inflate(24.0))) {
                net.valentin.obsidium.mythology.OdysseyAdvancements.awardPact(worshipper, targetPact);
            }

            // Consume Scroll and items on the 4 obelisks
            for (BlockPos obeliskPos : cachedObelisks) {
                if (level.getBlockEntity(obeliskPos) instanceof AstrapeRitualObeliskBlockEntity obelisk) {
                    obelisk.removeItem();
                }
            }

            level.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0F, 1.2F);
            level.sendParticles(ParticleTypes.FIREWORK, pos.getX() + 0.5, pos.getY() + 1.4, pos.getZ() + 0.5, 30, 0.3, 0.5, 0.3, 0.08);

            setChanged();
            level.sendBlockUpdated(pos, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void clearContent() {
        this.placedItem = ItemStack.EMPTY;
        this.isRitualRunning = false;
        this.ritualProgress = 0;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (!placedItem.isEmpty()) {
            tag.put("PlacedItem", placedItem.save(provider));
        }
        tag.putBoolean("RitualRunning", isRitualRunning);
        tag.putInt("Progress", ritualProgress);
        tag.putString("Pact", targetPact.getId());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("PlacedItem")) {
            this.placedItem = ItemStack.parse(provider, tag.getCompound("PlacedItem")).orElse(ItemStack.EMPTY);
        } else {
            this.placedItem = ItemStack.EMPTY;
        }
        this.isRitualRunning = tag.getBoolean("RitualRunning");
        this.ritualProgress = tag.getInt("Progress");
        this.targetPact = DivinePact.fromId(tag.getString("Pact"));
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        saveAdditional(tag, provider);
        return tag;
    }
}
