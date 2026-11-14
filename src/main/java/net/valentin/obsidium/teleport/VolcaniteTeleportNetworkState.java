package net.valentin.obsidium.teleport;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.valentin.obsidium.block.VolcanicTeleporterBlock;
import net.valentin.obsidium.block.VolcanicTeleporterBlockEntity;
import net.valentin.obsidium.item.VolcaniteWarpGauntletItem;
import net.valentin.obsidium.network.TeleportNetworkSyncPayload;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Server-persisted Quantum Teleportation Network registry storing public/private teleporter pads
 * and per-player personal field waypoints across all dimensions.
 */
public class VolcaniteTeleportNetworkState extends SavedData {
    private static final String SAVE_NAME = "obsidium_teleport_network";
    public static final int MAX_PERSONAL_WAYPOINTS = 16;
    public static final int BASIC_MAX_RANGE = 2500;
    public static final long BLOCK_CROSS_DIM_COST = 50_000L;
    public static final long GAUNTLET_CROSS_DIM_COST = 75_000L;

    public static final SavedData.Factory<VolcaniteTeleportNetworkState> FACTORY = new SavedData.Factory<>(
            VolcaniteTeleportNetworkState::new,
            VolcaniteTeleportNetworkState::load,
            null
    );

    public record TeleporterNode(
            String id,
            String name,
            String dimension,
            BlockPos pos,
            int iconIndex,
            boolean supreme,
            boolean isPublic,
            UUID ownerUuid,
            String ownerName
    ) {}

    public record PersonalWaypoint(
            String id,
            String name,
            String dimension,
            BlockPos pos,
            float yaw,
            int iconIndex
    ) {}

    public record ResolvedDestination(
            String id,
            String name,
            String dimension,
            BlockPos pos,
            float yaw,
            int iconIndex,
            int category,
            boolean isPadTarget
    ) {}

    private final Map<String, TeleporterNode> pads = new LinkedHashMap<>();
    private final Map<UUID, List<PersonalWaypoint>> waypoints = new HashMap<>();

    public VolcaniteTeleportNetworkState() {}

    public static VolcaniteTeleportNetworkState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, SAVE_NAME);
    }

    public static String makePadId(String dimension, BlockPos pos) {
        return "pad:" + dimension + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    public void registerOrUpdatePad(TeleporterNode node) {
        this.pads.put(node.id(), node);
        setDirty();
    }

    public void removePad(String dimension, BlockPos pos) {
        if (this.pads.remove(makePadId(dimension, pos)) != null) {
            setDirty();
        }
    }

    @Nullable
    public TeleporterNode getPad(String padId) {
        return this.pads.get(padId);
    }

    public List<TeleporterNode> getAccessiblePads(UUID playerUuid) {
        List<TeleporterNode> result = new ArrayList<>();
        for (TeleporterNode node : this.pads.values()) {
            if (node.isPublic() || node.ownerUuid().equals(playerUuid)) {
                result.add(node);
            }
        }
        return result;
    }

    public List<PersonalWaypoint> getPersonalWaypoints(UUID playerUuid) {
        return List.copyOf(this.waypoints.getOrDefault(playerUuid, List.of()));
    }

    public boolean addOrUpdatePersonalWaypoint(UUID playerUuid, PersonalWaypoint wp) {
        List<PersonalWaypoint> list = this.waypoints.computeIfAbsent(playerUuid, k -> new ArrayList<>());
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(wp.id())) {
                list.set(i, wp);
                setDirty();
                return true;
            }
        }
        if (list.size() >= MAX_PERSONAL_WAYPOINTS) {
            return false;
        }
        list.add(wp);
        setDirty();
        return true;
    }

    public boolean removePersonalWaypoint(UUID playerUuid, String waypointId) {
        List<PersonalWaypoint> list = this.waypoints.get(playerUuid);
        if (list == null) return false;
        boolean removed = list.removeIf(w -> w.id().equals(waypointId));
        if (removed) setDirty();
        return removed;
    }

    @Nullable
    public ResolvedDestination findDestination(ServerPlayer player, String targetId) {
        if (targetId == null || targetId.isBlank()) return null;

        if ("bed".equals(targetId)) {
            BlockPos respawn = player.getRespawnPosition();
            if (respawn == null) return null;
            String dim = player.getRespawnDimension().location().toString();
            return new ResolvedDestination("bed", "Respawn Bed", dim, respawn, player.getRespawnAngle(), 1,
                    TeleportNetworkSyncPayload.CAT_SYSTEM, false);
        }

        if ("spawn".equals(targetId)) {
            ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
            if (overworld == null) return null;
            BlockPos sp = overworld.getSharedSpawnPos();
            int topY = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sp.getX(), sp.getZ());
            BlockPos safeSpawn = new BlockPos(sp.getX(), Math.max(sp.getY(), topY), sp.getZ());
            return new ResolvedDestination("spawn", "World Spawn", Level.OVERWORLD.location().toString(),
                    safeSpawn, 0.0f, 6, TeleportNetworkSyncPayload.CAT_SYSTEM, false);
        }

        if (targetId.startsWith("pad:")) {
            TeleporterNode node = this.pads.get(targetId);
            if (node == null) return null;
            if (!node.isPublic() && !node.ownerUuid().equals(player.getUUID())) return null;
            return new ResolvedDestination(node.id(), node.name(), node.dimension(), node.pos(),
                    player.getYRot(), node.iconIndex(), TeleportNetworkSyncPayload.CAT_PAD, true);
        }

        if (targetId.startsWith("wp:")) {
            List<PersonalWaypoint> list = this.waypoints.getOrDefault(player.getUUID(), List.of());
            for (PersonalWaypoint wp : list) {
                if (wp.id().equals(targetId)) {
                    return new ResolvedDestination(wp.id(), wp.name(), wp.dimension(), wp.pos(),
                            wp.yaw(), wp.iconIndex(), TeleportNetworkSyncPayload.CAT_WAYPOINT, false);
                }
            }
        }

        return null;
    }

    public static long calculateRfCost(String sourceDimension, BlockPos sourcePos, String destDimension,
                                       BlockPos destPos, boolean fromBlock, boolean supremeSource) {
        boolean crossDim = !sourceDimension.equals(destDimension);
        if (crossDim) {
            return fromBlock ? BLOCK_CROSS_DIM_COST : GAUNTLET_CROSS_DIM_COST;
        }
        double dist = Math.sqrt(sourcePos.distSqr(destPos));
        return Math.min(50_000L, 2_500L + Math.round(dist * (supremeSource ? 10.0 : 35.0)));
    }

    public void sendSyncToPlayer(ServerPlayer player, @Nullable VolcanicTeleporterBlockEntity pad,
                                 @Nullable ItemStack gauntletStack) {
        boolean fromBlock = pad != null;
        BlockPos sourcePos = fromBlock ? pad.getBlockPos() : player.blockPosition();
        String sourceDim = fromBlock && pad.getLevel() != null
                ? pad.getLevel().dimension().location().toString()
                : player.level().dimension().location().toString();
        boolean supremeSource = !fromBlock || pad.isSupreme();

        long availableRf = 0L;
        if (fromBlock) {
            availableRf = pad.energyStorage.amount;
        } else if (gauntletStack != null && gauntletStack.getItem() instanceof VolcaniteWarpGauntletItem gauntlet) {
            availableRf = gauntlet.getStoredEnergy(gauntletStack);
        }

        String sourceName = fromBlock ? pad.getPadName() : "Volcanite Warp Gauntlet";
        int sourceIcon = fromBlock ? pad.getIconIndex() : 0;
        boolean sourcePublic = !fromBlock || pad.isPublic();
        String pinnedId = fromBlock ? pad.getPinnedTargetId()
                : (gauntletStack != null ? VolcaniteWarpGauntletItem.getPinnedTarget(gauntletStack) : "");

        List<TeleportNetworkSyncPayload.DestinationEntry> entries = new ArrayList<>();

        // 1. Registered Teleporter Pads
        String selfPadId = fromBlock ? makePadId(sourceDim, sourcePos) : "";
        for (TeleporterNode node : getAccessiblePads(player.getUUID())) {
            if (fromBlock && node.id().equals(selfPadId)) continue;
            entries.add(buildEntry(player, sourceDim, sourcePos, fromBlock, supremeSource, availableRf,
                    node.id(), node.name(), node.dimension(), node.pos(), node.iconIndex(),
                    TeleportNetworkSyncPayload.CAT_PAD));
        }

        // 2. Personal Bookmarked Waypoints
        for (PersonalWaypoint wp : getPersonalWaypoints(player.getUUID())) {
            entries.add(buildEntry(player, sourceDim, sourcePos, fromBlock, supremeSource, availableRf,
                    wp.id(), wp.name(), wp.dimension(), wp.pos(), wp.iconIndex(),
                    TeleportNetworkSyncPayload.CAT_WAYPOINT));
        }

        // 3. Home Respawn Bed & Overworld Spawn
        ResolvedDestination bed = findDestination(player, "bed");
        if (bed != null) {
            entries.add(buildEntry(player, sourceDim, sourcePos, fromBlock, supremeSource, availableRf,
                    bed.id(), bed.name(), bed.dimension(), bed.pos(), bed.iconIndex(),
                    TeleportNetworkSyncPayload.CAT_SYSTEM));
        }
        ResolvedDestination spawn = findDestination(player, "spawn");
        if (spawn != null) {
            entries.add(buildEntry(player, sourceDim, sourcePos, fromBlock, supremeSource, availableRf,
                    spawn.id(), spawn.name(), spawn.dimension(), spawn.pos(), spawn.iconIndex(),
                    TeleportNetworkSyncPayload.CAT_SYSTEM));
        }

        TeleportNetworkSyncPayload payload = new TeleportNetworkSyncPayload(
                fromBlock, sourcePos, supremeSource, sourceName, sourceIcon, sourcePublic, pinnedId, entries
        );
        ServerPlayNetworking.send(player, payload);
    }

    private TeleportNetworkSyncPayload.DestinationEntry buildEntry(
            ServerPlayer player, String sourceDim, BlockPos sourcePos, boolean fromBlock,
            boolean supremeSource, long availableRf, String id, String name, String destDim,
            BlockPos destPos, int iconIndex, int category) {
        boolean crossDim = !sourceDim.equals(destDim);
        int dist = crossDim ? -1 : (int) Math.round(Math.sqrt(sourcePos.distSqr(destPos)));
        long cost = calculateRfCost(sourceDim, sourcePos, destDim, destPos, fromBlock, supremeSource);
        boolean inCapabilities = supremeSource || (!crossDim && dist <= BASIC_MAX_RANGE);
        boolean canAfford = inCapabilities && (player.isCreative() || availableRf >= cost);
        return new TeleportNetworkSyncPayload.DestinationEntry(
                id, name, destDim, destPos.getX(), destPos.getY(), destPos.getZ(),
                iconIndex, category, dist, cost, crossDim, canAfford
        );
    }

    public boolean executeTeleport(ServerPlayer player, String targetId,
                                   @Nullable BlockPos sourceBlockPos,
                                   @Nullable InteractionHand gauntletHand) {
        MinecraftServer server = player.server;
        ResolvedDestination dest = findDestination(player, targetId);
        if (dest == null) {
            player.displayClientMessage(Component.literal("§c⛩ Destination no longer exists or is private!"), true);
            return false;
        }

        ResourceLocation dimLoc = ResourceLocation.tryParse(dest.dimension());
        if (dimLoc == null) return false;
        ResourceKey<Level> targetKey = ResourceKey.create(Registries.DIMENSION, dimLoc);
        ServerLevel targetLevel = server.getLevel(targetKey);
        if (targetLevel == null) {
            player.displayClientMessage(Component.literal("§c⛩ Target dimension is unavailable!"), true);
            return false;
        }

        boolean fromBlock = sourceBlockPos != null;
        VolcanicTeleporterBlockEntity sourcePad = null;
        ItemStack gauntletStack = ItemStack.EMPTY;
        boolean supremeSource;
        String sourceDim;
        BlockPos originPos;

        if (fromBlock) {
            BlockEntity be = player.level().getBlockEntity(sourceBlockPos);
            if (!(be instanceof VolcanicTeleporterBlockEntity tp)) {
                return false;
            }
            if (player.blockPosition().distSqr(sourceBlockPos) > 100.0) {
                return false;
            }
            sourcePad = tp;
            supremeSource = tp.isSupreme();
            sourceDim = player.level().dimension().location().toString();
            originPos = sourceBlockPos;
        } else {
            InteractionHand hand = gauntletHand != null ? gauntletHand : InteractionHand.MAIN_HAND;
            ItemStack candidate = player.getItemInHand(hand);
            if (!(candidate.getItem() instanceof VolcaniteWarpGauntletItem)) {
                InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                candidate = player.getItemInHand(other);
            }
            if (!(candidate.getItem() instanceof VolcaniteWarpGauntletItem)) {
                player.displayClientMessage(Component.literal("§c⛩ Volcanite Warp Gauntlet must be held to teleport!"), true);
                return false;
            }
            gauntletStack = candidate;
            supremeSource = true;
            sourceDim = player.level().dimension().location().toString();
            originPos = player.blockPosition();
        }

        boolean crossDim = !sourceDim.equals(dest.dimension());
        double dist = crossDim ? 0.0 : Math.sqrt(originPos.distSqr(dest.pos()));

        if (!supremeSource && (crossDim || dist > BASIC_MAX_RANGE)) {
            player.displayClientMessage(Component.literal("§c⛩ Destination out of range! Upgrade to Volcanic Core Teleporter for Cross-Dimension / Infinite range."), true);
            return false;
        }

        long cost = calculateRfCost(sourceDim, originPos, dest.dimension(), dest.pos(), fromBlock, supremeSource);
        if (!player.isCreative()) {
            if (fromBlock) {
                if (sourcePad.energyStorage.amount < cost) {
                    player.displayClientMessage(Component.literal(String.format("§c⚡ Not enough RF! Need %,d RF.", cost)), true);
                    return false;
                }
                sourcePad.energyStorage.amount -= cost;
                sourcePad.setChanged();
                sourcePad.syncToClient();
            } else {
                VolcaniteWarpGauntletItem gauntlet = (VolcaniteWarpGauntletItem) gauntletStack.getItem();
                if (gauntlet.getStoredEnergy(gauntletStack) < cost) {
                    player.displayClientMessage(Component.literal(String.format("§c⚡ Gauntlet low RF! Need %,d RF.", cost)), true);
                    return false;
                }
                gauntlet.tryUseEnergy(gauntletStack, cost);
            }
        }

        // Load destination chunk before computing landing height
        targetLevel.getChunk(dest.pos());
        double landY = computeSafeLandingY(targetLevel, dest);

        // Departure effects
        ServerLevel originLevel = player.serverLevel();
        originLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.9F);
        originLevel.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.4, 0.7, 0.4, 0.25);
        originLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 0.8, player.getZ(), 24, 0.4, 0.5, 0.4, 0.12);

        // Collect nearby following tamed pets to bring along
        List<TamableAnimal> petsToWarp = originLevel.getEntitiesOfClass(
                TamableAnimal.class,
                new AABB(originPos).inflate(4.5),
                pet -> pet.isAlive() && pet.isTame() && pet.isOwnedBy(player) && !pet.isOrderedToSit()
        );

        if (player.isPassenger()) {
            player.stopRiding();
        }
        player.resetFallDistance();
        player.setDeltaMovement(Vec3.ZERO);

        double tx = dest.pos().getX() + 0.5;
        double tz = dest.pos().getZ() + 0.5;
        player.teleportTo(targetLevel, tx, landY, tz, Set.of(), dest.yaw(), 0.0f);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 1, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 80, 0, false, false, true));

        for (TamableAnimal pet : petsToWarp) {
            if (pet.level() == targetLevel) {
                pet.teleportTo(tx + 0.7, landY, tz + 0.7);
            } else {
                pet.teleportTo(targetLevel, tx + 0.7, landY, tz + 0.7, Set.of(), pet.getYRot(), pet.getXRot());
            }
        }

        // Arrival effects
        targetLevel.playSound(null, tx, landY, tz, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.15F);
        targetLevel.playSound(null, tx, landY, tz, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.65F, 1.65F);
        targetLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, tx, landY + 0.8, tz, 45, 0.45, 0.6, 0.45, 0.2);
        targetLevel.sendParticles(ParticleTypes.END_ROD, tx, landY + 0.9, tz, 20, 0.35, 0.5, 0.35, 0.08);

        player.displayClientMessage(Component.literal("§b⛩ Quantum Warp ➔ §e" + dest.name() + " §7[" + dest.pos().toShortString() + "]"), true);
        return true;
    }

    private static double computeSafeLandingY(ServerLevel targetLevel, ResolvedDestination dest) {
        BlockPos pos = dest.pos();
        double baseOffset = (dest.isPadTarget() && targetLevel.getBlockState(pos).getBlock() instanceof VolcanicTeleporterBlock)
                ? 0.56
                : 0.1;

        BlockPos headBase = dest.isPadTarget() ? pos.above() : pos;
        for (int dy = 0; dy <= 4; dy++) {
            BlockPos footPos = headBase.above(dy);
            BlockPos headPos = footPos.above();
            boolean footClear = targetLevel.getBlockState(footPos).getCollisionShape(targetLevel, footPos).isEmpty();
            boolean headClear = targetLevel.getBlockState(headPos).getCollisionShape(targetLevel, headPos).isEmpty();
            if (footClear && headClear) {
                return dy == 0 ? pos.getY() + baseOffset : footPos.getY() + 0.1;
            }
        }
        return pos.getY() + baseOffset;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag padList = new ListTag();
        for (TeleporterNode n : this.pads.values()) {
            CompoundTag pt = new CompoundTag();
            pt.putString("Id", n.id());
            pt.putString("Name", n.name());
            pt.putString("Dim", n.dimension());
            pt.putInt("X", n.pos().getX());
            pt.putInt("Y", n.pos().getY());
            pt.putInt("Z", n.pos().getZ());
            pt.putInt("Icon", n.iconIndex());
            pt.putBoolean("Supreme", n.supreme());
            pt.putBoolean("Public", n.isPublic());
            pt.putUUID("Owner", n.ownerUuid());
            pt.putString("OwnerName", n.ownerName());
            padList.add(pt);
        }
        tag.put("Pads", padList);

        ListTag wpPlayers = new ListTag();
        for (Map.Entry<UUID, List<PersonalWaypoint>> entry : this.waypoints.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("Player", entry.getKey());
            ListTag wList = new ListTag();
            for (PersonalWaypoint wp : entry.getValue()) {
                CompoundTag wt = new CompoundTag();
                wt.putString("Id", wp.id());
                wt.putString("Name", wp.name());
                wt.putString("Dim", wp.dimension());
                wt.putInt("X", wp.pos().getX());
                wt.putInt("Y", wp.pos().getY());
                wt.putInt("Z", wp.pos().getZ());
                wt.putFloat("Yaw", wp.yaw());
                wt.putInt("Icon", wp.iconIndex());
                wList.add(wt);
            }
            playerTag.put("Waypoints", wList);
            wpPlayers.add(playerTag);
        }
        tag.put("PlayerWaypoints", wpPlayers);
        return tag;
    }

    public static VolcaniteTeleportNetworkState load(CompoundTag tag, HolderLookup.Provider provider) {
        VolcaniteTeleportNetworkState state = new VolcaniteTeleportNetworkState();
        ListTag padList = tag.getList("Pads", Tag.TAG_COMPOUND);
        for (int i = 0; i < padList.size(); i++) {
            CompoundTag pt = padList.getCompound(i);
            String id = pt.getString("Id");
            String name = pt.getString("Name");
            String dim = pt.getString("Dim");
            BlockPos pos = new BlockPos(pt.getInt("X"), pt.getInt("Y"), pt.getInt("Z"));
            int icon = pt.getInt("Icon");
            boolean supreme = pt.getBoolean("Supreme");
            boolean isPublic = !pt.contains("Public") || pt.getBoolean("Public");
            UUID owner = pt.hasUUID("Owner") ? pt.getUUID("Owner") : new UUID(0L, 0L);
            String ownerName = pt.getString("OwnerName");
            state.pads.put(id, new TeleporterNode(id, name, dim, pos, icon, supreme, isPublic, owner, ownerName));
        }

        ListTag wpPlayers = tag.getList("PlayerWaypoints", Tag.TAG_COMPOUND);
        for (int i = 0; i < wpPlayers.size(); i++) {
            CompoundTag playerTag = wpPlayers.getCompound(i);
            if (!playerTag.hasUUID("Player")) continue;
            UUID playerId = playerTag.getUUID("Player");
            ListTag wList = playerTag.getList("Waypoints", Tag.TAG_COMPOUND);
            List<PersonalWaypoint> parsed = new ArrayList<>();
            for (int j = 0; j < wList.size(); j++) {
                CompoundTag wt = wList.getCompound(j);
                parsed.add(new PersonalWaypoint(
                        wt.getString("Id"),
                        wt.getString("Name"),
                        wt.getString("Dim"),
                        new BlockPos(wt.getInt("X"), wt.getInt("Y"), wt.getInt("Z")),
                        wt.getFloat("Yaw"),
                        wt.getInt("Icon")
                ));
            }
            state.waypoints.put(playerId, parsed);
        }
        return state;
    }
}

