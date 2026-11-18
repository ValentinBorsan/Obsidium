package net.valentin.obsidium.storage;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.valentin.obsidium.fluid.FluidVariantPersistence;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-persisted cross-dimensional storage network for Quantum Link Modules.
 * Supports 16 DyeColor channels x 999 numeric channels, in both Public and Private (owner UUID) scopes.
 */
public class QuantumLinkNetworkState extends SavedData {
    private static final String SAVE_NAME = "obsidium_quantum_link_network";
    public static final int ITEM_VAULT_SLOTS = 54;
    public static final long FLUID_VAULT_CAPACITY = 256_000L; // 256 Buckets in mB
    public static final long ENERGY_VAULT_CAPACITY = 10_000_000L; // 10M RF per channel
    public static final int MIN_CHANNEL = 1;
    public static final int MAX_CHANNEL = 999;

    public static final SavedData.Factory<QuantumLinkNetworkState> FACTORY = new SavedData.Factory<>(
            QuantumLinkNetworkState::new,
            QuantumLinkNetworkState::load,
            null
    );

    public static final class FluidVault {
        public FluidVariant fluid = FluidVariant.blank();
        public long amount = 0L;
    }

    private final Map<String, NonNullList<ItemStack>> itemVaults = new HashMap<>();
    private final Map<String, FluidVault> fluidVaults = new HashMap<>();
    private final Map<String, Long> energyVaults = new HashMap<>();

    private final QuantumLinkTransferStorages transferStorages = new QuantumLinkTransferStorages(this);

    public net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage<FluidVariant> getFluidStorage(String key) {
        return transferStorages.fluid(key);
    }

    public team.reborn.energy.api.EnergyStorage getEnergyStorage(String key) {
        return transferStorages.energy(key);
    }

    public void setEnergyState(String key, long amount) {
        energyVaults.put(key, Math.max(0L, Math.min(ENERGY_VAULT_CAPACITY, amount)));
        setDirty();
    }

    public QuantumLinkNetworkState() {
    }

    public static QuantumLinkNetworkState get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            overworld = server.overworld();
        }
        return overworld.getDataStorage().computeIfAbsent(FACTORY, SAVE_NAME);
    }

    @Nullable
    public static QuantumLinkNetworkState get(@Nullable Level level) {
        if (level == null || level.isClientSide() || level.getServer() == null) {
            return null;
        }
        return get(level.getServer());
    }

    public static int clampChannel(int channel) {
        return Math.max(MIN_CHANNEL, Math.min(MAX_CHANNEL, channel));
    }

    public static String buildChannelKey(boolean isPrivate, @Nullable UUID ownerUuid, DyeColor color, int channel) {
        int ch = clampChannel(channel);
        int cId = color != null ? color.getId() : DyeColor.PURPLE.getId();
        if (isPrivate && ownerUuid != null) {
            return "priv:" + ownerUuid + ":" + cId + ":" + ch;
        }
        return "pub:" + cId + ":" + ch;
    }

    public NonNullList<ItemStack> getItemVault(String key) {
        return itemVaults.computeIfAbsent(key, k -> NonNullList.withSize(ITEM_VAULT_SLOTS, ItemStack.EMPTY));
    }

    public FluidVault getFluidVault(String key) {
        return fluidVaults.computeIfAbsent(key, k -> new FluidVault());
    }

    public FluidVariant getFluid(String key) {
        FluidVault vault = fluidVaults.get(key);
        return vault != null ? vault.fluid : FluidVariant.blank();
    }

    public long getFluidAmount(String key) {
        FluidVault vault = fluidVaults.get(key);
        return vault != null ? vault.amount : 0L;
    }

    public long insertFluid(String key, FluidVariant resource, long maxAmount, boolean simulate) {
        if (resource == null || resource.isBlank() || maxAmount <= 0L) {
            return 0L;
        }
        FluidVault vault = getFluidVault(key);
        if (!vault.fluid.isBlank() && !vault.fluid.equals(resource)) {
            return 0L;
        }
        long space = Math.max(0L, FLUID_VAULT_CAPACITY - vault.amount);
        long inserted = Math.min(maxAmount, space);
        if (inserted > 0L && !simulate) {
            if (vault.fluid.isBlank()) {
                vault.fluid = resource;
            }
            vault.amount += inserted;
            setDirty();
        }
        return inserted;
    }

    public long extractFluid(String key, FluidVariant resource, long maxAmount, boolean simulate) {
        if (maxAmount <= 0L) {
            return 0L;
        }
        FluidVault vault = fluidVaults.get(key);
        if (vault == null || vault.fluid.isBlank() || vault.amount <= 0L) {
            return 0L;
        }
        if (resource != null && !resource.isBlank() && !vault.fluid.equals(resource)) {
            return 0L;
        }
        long extracted = Math.min(maxAmount, vault.amount);
        if (extracted > 0L && !simulate) {
            vault.amount -= extracted;
            if (vault.amount <= 0L) {
                vault.amount = 0L;
                vault.fluid = FluidVariant.blank();
            }
            setDirty();
        }
        return extracted;
    }

    public void setFluidState(String key, FluidVariant fluid, long amount) {
        FluidVault vault = getFluidVault(key);
        vault.fluid = (fluid != null && amount > 0L) ? fluid : FluidVariant.blank();
        vault.amount = Math.max(0L, Math.min(FLUID_VAULT_CAPACITY, amount));
        setDirty();
    }

    public long getEnergy(String key) {
        return energyVaults.getOrDefault(key, 0L);
    }

    public long insertEnergy(String key, long maxAmount, boolean simulate) {
        if (maxAmount <= 0L) return 0L;
        long cur = energyVaults.getOrDefault(key, 0L);
        long inserted = Math.min(maxAmount, Math.max(0L, ENERGY_VAULT_CAPACITY - cur));
        if (inserted > 0L && !simulate) {
            energyVaults.put(key, cur + inserted);
            setDirty();
        }
        return inserted;
    }

    public long extractEnergy(String key, long maxAmount, boolean simulate) {
        if (maxAmount <= 0L) return 0L;
        long cur = energyVaults.getOrDefault(key, 0L);
        long extracted = Math.min(maxAmount, cur);
        if (extracted > 0L && !simulate) {
            long rem = cur - extracted;
            if (rem <= 0L) energyVaults.remove(key);
            else energyVaults.put(key, rem);
            setDirty();
        }
        return extracted;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag itemList = new ListTag();
        for (Map.Entry<String, NonNullList<ItemStack>> entry : itemVaults.entrySet()) {
            boolean hasAny = false;
            for (ItemStack s : entry.getValue()) {
                if (!s.isEmpty()) {
                    hasAny = true;
                    break;
                }
            }
            if (!hasAny) continue;
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("Key", entry.getKey());
            ContainerHelper.saveAllItems(entryTag, entry.getValue(), provider);
            itemList.add(entryTag);
        }
        tag.put("ItemVaults", itemList);

        ListTag fluidList = new ListTag();
        for (Map.Entry<String, FluidVault> entry : fluidVaults.entrySet()) {
            FluidVault vault = entry.getValue();
            if (vault.fluid.isBlank() || vault.amount <= 0L) continue;
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("Key", entry.getKey());
            FluidVariantPersistence.write(entryTag, vault.fluid, provider);
            entryTag.putLong("Amount", vault.amount);
            fluidList.add(entryTag);
        }
        tag.put("FluidVaults", fluidList);

        ListTag energyList = new ListTag();
        for (Map.Entry<String, Long> entry : energyVaults.entrySet()) {
            if (entry.getValue() <= 0L) continue;
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("Key", entry.getKey());
            entryTag.putLong("Amount", entry.getValue());
            energyList.add(entryTag);
        }
        tag.put("EnergyVaults", energyList);
        return tag;
    }

    public static QuantumLinkNetworkState load(CompoundTag tag, HolderLookup.Provider provider) {
        QuantumLinkNetworkState state = new QuantumLinkNetworkState();

        if (tag.contains("ItemVaults", Tag.TAG_LIST)) {
            ListTag itemList = tag.getList("ItemVaults", Tag.TAG_COMPOUND);
            for (int i = 0; i < itemList.size(); i++) {
                CompoundTag entryTag = itemList.getCompound(i);
                String key = entryTag.getString("Key");
                if (key.isEmpty()) continue;
                NonNullList<ItemStack> items = NonNullList.withSize(ITEM_VAULT_SLOTS, ItemStack.EMPTY);
                ContainerHelper.loadAllItems(entryTag, items, provider);
                state.itemVaults.put(key, items);
            }
        }

        if (tag.contains("FluidVaults", Tag.TAG_LIST)) {
            ListTag fluidList = tag.getList("FluidVaults", Tag.TAG_COMPOUND);
            for (int i = 0; i < fluidList.size(); i++) {
                CompoundTag entryTag = fluidList.getCompound(i);
                String key = entryTag.getString("Key");
                if (key.isEmpty()) continue;
                FluidVariant variant = FluidVariantPersistence.read(entryTag, provider);
                long amount = entryTag.getLong("Amount");
                if (!variant.isBlank() && amount > 0L) {
                    FluidVault vault = new FluidVault();
                    vault.fluid = variant;
                    vault.amount = Math.min(FLUID_VAULT_CAPACITY, amount);
                    state.fluidVaults.put(key, vault);
                }
            }
        }

        if (tag.contains("EnergyVaults", Tag.TAG_LIST)) {
            ListTag energyList = tag.getList("EnergyVaults", Tag.TAG_COMPOUND);
            for (int i = 0; i < energyList.size(); i++) {
                CompoundTag entryTag = energyList.getCompound(i);
                String key = entryTag.getString("Key");
                long amount = entryTag.getLong("Amount");
                if (!key.isEmpty() && amount > 0L) {
                    state.energyVaults.put(key, Math.min(ENERGY_VAULT_CAPACITY, amount));
                }
            }
        }

        return state;
    }
}
