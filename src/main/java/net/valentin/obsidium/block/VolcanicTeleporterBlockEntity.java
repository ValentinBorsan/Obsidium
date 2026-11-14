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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.teleport.VolcaniteTeleportNetworkState;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.base.SimpleEnergyItem;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.List;
import java.util.UUID;

/**
 * BlockEntity backing Basic and Core Volcanic Teleporter pads.
 * Maintains energy storage, network registration, wireless inventory charging,
 * and Sneak-to-Quick-Warp pad warmup mechanics.
 */
public class VolcanicTeleporterBlockEntity extends BlockEntity implements MenuProvider {
    public static final long BASIC_CAPACITY = 2_000_000L;
    public static final long BASIC_MAX_INPUT = 25_000L;
    public static final long CORE_CAPACITY = 20_000_000L;
    public static final long CORE_MAX_INPUT = 250_000L;
    public static final int DATA_COUNT = 6;
    private static final int WARMUP_TICKS = 25;

    private final boolean supreme;
    public final SimpleEnergyStorage energyStorage;

    private String padName = "";
    private int iconIndex = 0;
    private boolean isPublic = true;
    private UUID ownerUuid = new UUID(0L, 0L);
    private String ownerName = "";
    private String pinnedTargetId = "";
    private int stepWarmupTicks = 0;
    private @Nullable UUID warmingPlayerId = null;
    private int regTicker = 0;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            long stored = energyStorage.amount;
            long max = energyStorage.capacity;
            return switch (index) {
                case 0 -> (int) (stored & 0xFFFF);
                case 1 -> (int) ((stored >> 16) & 0xFFFF);
                case 2 -> (int) (max & 0xFFFF);
                case 3 -> (int) ((max >> 16) & 0xFFFF);
                case 4 -> (supreme ? 1 : 0) | (isPublic ? 2 : 0) | (!pinnedTargetId.isEmpty() ? 4 : 0);
                case 5 -> iconIndex;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public VolcanicTeleporterBlockEntity(BlockPos pos, BlockState state, boolean supreme) {
        super(supreme ? ObsidiumMod.VOLCANIC_CORE_TELEPORTER_BE : ObsidiumMod.BASIC_VOLCANIC_TELEPORTER_BE, pos, state);
        this.supreme = supreme;
        long cap = supreme ? CORE_CAPACITY : BASIC_CAPACITY;
        long maxIn = supreme ? CORE_MAX_INPUT : BASIC_MAX_INPUT;
        this.energyStorage = new SimpleEnergyStorage(cap, maxIn, 0) {
            @Override
            protected void onFinalCommit() {
                setChanged();
            }
        };
        this.padName = (supreme ? "Core Matrix " : "Teleporter ") + "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]";
    }

    public boolean isSupreme() { return supreme; }
    public String getPadName() { return padName; }
    public int getIconIndex() { return iconIndex; }
    public boolean isPublic() { return isPublic; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public String getOwnerName() { return ownerName; }
    public String getPinnedTargetId() { return pinnedTargetId; }

    public void initializePlacement(Player player) {
        this.ownerUuid = player.getUUID();
        this.ownerName = player.getName().getString();
        if (this.padName.isBlank()) {
            this.padName = (supreme ? "Core Matrix " : "Teleporter ") + "[" + worldPosition.getX() + ", " + worldPosition.getY() + ", " + worldPosition.getZ() + "]";
        }
        setChanged();
        registerInNetwork();
    }

    public void updateSettings(String name, int iconIndex, boolean isPublic) {
        if (name != null && !name.isBlank()) {
            this.padName = name.trim().substring(0, Math.min(32, name.trim().length()));
        }
        this.iconIndex = Math.max(0, Math.min(7, iconIndex));
        this.isPublic = isPublic;
        setChanged();
        registerInNetwork();
        syncToClient();
    }

    public void setPinnedTargetId(String targetId) {
        this.pinnedTargetId = targetId != null ? targetId : "";
        setChanged();
        syncToClient();
    }

    public void registerInNetwork() {
        if (this.level == null || this.level.isClientSide() || this.level.getServer() == null) return;
        String dim = this.level.dimension().location().toString();
        String id = VolcaniteTeleportNetworkState.makePadId(dim, this.worldPosition);
        VolcaniteTeleportNetworkState.get(this.level.getServer()).registerOrUpdatePad(
                new VolcaniteTeleportNetworkState.TeleporterNode(
                        id, this.padName, dim, this.worldPosition,
                        this.iconIndex, this.supreme, this.isPublic, this.ownerUuid, this.ownerName
                )
        );
    }

    public void syncToClient() {
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VolcanicTeleporterBlockEntity entity) {
        if (level.isClientSide()) return;

        entity.regTicker++;
        if (entity.regTicker == 1 || entity.regTicker % 100 == 0) {
            entity.registerInNetwork();
        }

        boolean energized = entity.energyStorage.amount >= 2500L;
        if (state.hasProperty(VolcanicTeleporterBlock.ACTIVE) && state.getValue(VolcanicTeleporterBlock.ACTIVE) != energized) {
            level.setBlock(pos, state.setValue(VolcanicTeleporterBlock.ACTIVE, energized), Block.UPDATE_ALL);
        }

        AABB padBox = new AABB(
                pos.getX() + 0.05, pos.getY() + 0.3, pos.getZ() + 0.05,
                pos.getX() + 0.95, pos.getY() + 1.8, pos.getZ() + 0.95
        );
        List<ServerPlayer> standingPlayers = ((ServerLevel) level).getEntitiesOfClass(ServerPlayer.class, padBox);

        // Passively top up held/inventory RF items when standing on pad
        if (entity.energyStorage.amount > 0 && !standingPlayers.isEmpty()) {
            long transferBudget = entity.supreme ? 50_000L : 10_000L;
            for (ServerPlayer sp : standingPlayers) {
                entity.chargePlayerItems(sp, transferBudget);
            }
        }

        // Sneak-on-Pad Quick-Warp warmup if a target is pinned
        if (entity.pinnedTargetId.isEmpty() || standingPlayers.isEmpty()) {
            entity.stepWarmupTicks = 0;
            entity.warmingPlayerId = null;
            return;
        }

        ServerPlayer sneakingPlayer = null;
        for (ServerPlayer sp : standingPlayers) {
            if (sp.isShiftKeyDown()) {
                sneakingPlayer = sp;
                break;
            }
        }

        if (sneakingPlayer == null) {
            entity.stepWarmupTicks = 0;
            entity.warmingPlayerId = null;
            return;
        }

        if (!sneakingPlayer.getUUID().equals(entity.warmingPlayerId)) {
            entity.warmingPlayerId = sneakingPlayer.getUUID();
            entity.stepWarmupTicks = 0;
        }

        entity.stepWarmupTicks++;
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(entity.supreme ? ParticleTypes.END_ROD : ParticleTypes.FLAME,
                    pos.getX() + 0.5, pos.getY() + 0.65 + (entity.stepWarmupTicks / 25.0), pos.getZ() + 0.5,
                    4, 0.25, 0.1, 0.25, 0.03);
        }

        if (entity.stepWarmupTicks % 5 == 0) {
            int pct = (entity.stepWarmupTicks * 100) / WARMUP_TICKS;
            int bars = Math.max(1, (entity.stepWarmupTicks * 6) / WARMUP_TICKS);
            String barStr = "█".repeat(bars) + "░".repeat(Math.max(0, 6 - bars));
            sneakingPlayer.displayClientMessage(Component.literal("§b⚡ Quantum Lock: §e[" + barStr + "] " + pct + "%"), true);
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS,
                    0.45F, 0.8F + (entity.stepWarmupTicks / (float) WARMUP_TICKS));
        }

        if (entity.stepWarmupTicks >= WARMUP_TICKS) {
            entity.stepWarmupTicks = 0;
            entity.warmingPlayerId = null;
            if (level.getServer() != null) {
                VolcaniteTeleportNetworkState.get(level.getServer())
                        .executeTeleport(sneakingPlayer, entity.pinnedTargetId, pos, null);
            }
        }
    }

    private void chargePlayerItems(ServerPlayer player, long budgetPerTick) {
        if (energyStorage.amount <= 0) return;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (st.isEmpty() || !(st.getItem() instanceof SimpleEnergyItem energyItem)) continue;
            long cap = energyItem.getEnergyCapacity(st);
            long cur = energyItem.getStoredEnergy(st);
            long maxIn = energyItem.getEnergyMaxInput(st);
            long need = cap - cur;
            if (need > 0 && maxIn > 0) {
                long move = Math.min(Math.min(need, budgetPerTick), Math.min(maxIn, energyStorage.amount));
                if (move > 0) {
                    energyItem.setStoredEnergy(st, cur + move);
                    energyStorage.amount -= move;
                    setChanged();
                    return;
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putLong("Energy", energyStorage.amount);
        tag.putString("PadName", padName);
        tag.putInt("Icon", iconIndex);
        tag.putBoolean("Public", isPublic);
        tag.putUUID("Owner", ownerUuid);
        tag.putString("OwnerName", ownerName);
        tag.putString("Pinned", pinnedTargetId);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        energyStorage.amount = Math.min(energyStorage.capacity, tag.getLong("Energy"));
        if (tag.contains("PadName")) padName = tag.getString("PadName");
        iconIndex = tag.getInt("Icon");
        isPublic = !tag.contains("Public") || tag.getBoolean("Public");
        if (tag.hasUUID("Owner")) ownerUuid = tag.getUUID("Owner");
        ownerName = tag.getString("OwnerName");
        pinnedTargetId = tag.getString("Pinned");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.obsidium.teleporter_network");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new VolcaniteTeleporterMenu(syncId, playerInventory, this.data, this);
    }
}

