package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;

/**
 * High-capacity 3-chamber nursery barrel dedicated to saplings, cuttings, and mineral tree hybrids.
 * Storing up to 4,096 saplings per chamber (12,288 total).
 */
public class DemetrianSaplingBarrelBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {
    public static final int CHAMBER_COUNT = 3;
    public static final long MAX_CHAMBER_CAPACITY = 4096;

    public static class Chamber {
        private ItemVariant variant = ItemVariant.blank();
        private long amount = 0;
        private boolean locked = false;

        public ItemVariant getVariant() {
            return variant;
        }

        public void setVariant(ItemVariant variant) {
            this.variant = variant != null ? variant : ItemVariant.blank();
        }

        public long getAmount() {
            return amount;
        }

        public void setAmount(long amount) {
            this.amount = Math.max(0, Math.min(MAX_CHAMBER_CAPACITY, amount));
        }

        public boolean isLocked() {
            return locked;
        }

        public void setLocked(boolean locked) {
            this.locked = locked;
        }

        public boolean isEmpty() {
            return amount <= 0 && variant.isBlank();
        }

        public ItemStack toSampleStack() {
            return variant.isBlank() ? ItemStack.EMPTY : variant.toStack();
        }
    }

    private final Chamber[] chambers = new Chamber[CHAMBER_COUNT];

    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            int ch = index / 3;
            int part = index % 3;
            if (ch < 0 || ch >= CHAMBER_COUNT) return 0;
            Chamber c = chambers[ch];
            return switch (part) {
                case 0 -> (int) (c.amount & 0xFFFF);
                case 1 -> (int) ((c.amount >> 16) & 0xFFFF);
                case 2 -> c.locked ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            int ch = index / 3;
            int part = index % 3;
            if (ch < 0 || ch >= CHAMBER_COUNT) return;
            Chamber c = chambers[ch];
            switch (part) {
                case 0 -> {
                    long high = c.amount & 0xFFFF0000L;
                    c.amount = high | (value & 0xFFFFL);
                }
                case 1 -> {
                    long low = c.amount & 0xFFFFL;
                    c.amount = ((long) (value & 0xFFFF) << 16) | low;
                }
                case 2 -> c.locked = (value == 1);
            }
        }

        @Override
        public int getCount() {
            return CHAMBER_COUNT * 3; // 9 data slots
        }
    };

    public DemetrianSaplingBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.DEMETRIAN_SAPLING_BARREL_BE, pos, state);
        for (int i = 0; i < CHAMBER_COUNT; i++) {
            chambers[i] = new Chamber();
        }
    }

    public Chamber getChamber(int index) {
        if (index >= 0 && index < CHAMBER_COUNT) {
            return chambers[index];
        }
        return chambers[0];
    }

    public ItemStack getStoredItemSample(int index) {
        if (index >= 0 && index < CHAMBER_COUNT) {
            return chambers[index].toSampleStack();
        }
        return ItemStack.EMPTY;
    }

    public static boolean isSapling(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.is(ItemTags.SAPLINGS)) return true;
        if (stack.getItem() instanceof BlockItem bi) {
            Block block = bi.getBlock();
            return block instanceof SaplingBlock || block instanceof OreTreeSaplingBlock;
        }
        return false;
    }

    public boolean insertSapling(ItemStack stack, @Nullable Player player) {
        if (!isSapling(stack) || stack.isEmpty()) return false;

        ItemVariant variant = ItemVariant.of(stack);

        // 1. Try matching chamber
        for (int i = 0; i < CHAMBER_COUNT; i++) {
            Chamber c = chambers[i];
            if (!c.variant.isBlank() && c.variant.equals(variant) && c.amount < MAX_CHAMBER_CAPACITY) {
                long space = MAX_CHAMBER_CAPACITY - c.amount;
                int toAdd = (int) Math.min(stack.getCount(), space);
                if (toAdd > 0) {
                    stack.shrink(toAdd);
                    c.amount += toAdd;
                    onSuccessfulDeposit(player);
                    return true;
                }
            }
        }

        // 2. Try empty chamber
        for (int i = 0; i < CHAMBER_COUNT; i++) {
            Chamber c = chambers[i];
            if (c.variant.isBlank() && !c.locked) {
                int toAdd = (int) Math.min(stack.getCount(), MAX_CHAMBER_CAPACITY);
                if (toAdd > 0) {
                    c.setVariant(variant);
                    c.setAmount(toAdd);
                    stack.shrink(toAdd);
                    onSuccessfulDeposit(player);
                    return true;
                }
            }
        }

        return false;
    }

    private void onSuccessfulDeposit(@Nullable Player player) {
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F, 1.4F);
            level.playSound(null, worldPosition, SoundEvents.COMPOSTER_READY, SoundSource.BLOCKS, 0.6F, 1.2F);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, 6, 0.25, 0.1, 0.25, 0.05);
            }
        }
        sync();
    }

    public boolean extractSapling(int chamberIndex, int maxCount, @Nullable Player player) {
        if (chamberIndex < 0 || chamberIndex >= CHAMBER_COUNT) return false;
        Chamber c = chambers[chamberIndex];
        if (c.variant.isBlank() || c.amount <= 0) return false;

        int count = (int) Math.min(maxCount, Math.min(c.amount, c.variant.toStack().getMaxStackSize()));
        if (count <= 0) return false;

        ItemStack drop = c.variant.toStack(count);
        c.amount -= count;
        if (c.amount <= 0 && !c.locked) {
            c.variant = ItemVariant.blank();
        }

        if (player != null) {
            if (!player.getInventory().add(drop)) {
                Containers.dropItemStack(level, player.getX(), player.getY() + 0.5, player.getZ(), drop);
            }
            if (level != null) {
                level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 1.8F);
            }
        } else {
            if (level != null) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, drop);
            }
        }

        sync();
        return true;
    }

    public void dropAllContents() {
        if (level == null || level.isClientSide()) return;
        for (int i = 0; i < CHAMBER_COUNT; i++) {
            Chamber c = chambers[i];
            if (!c.variant.isBlank() && c.amount > 0) {
                long remaining = c.amount;
                int maxStack = c.variant.toStack().getMaxStackSize();
                while (remaining > 0) {
                    int toDrop = (int) Math.min(remaining, maxStack);
                    Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, c.variant.toStack(toDrop));
                    remaining -= toDrop;
                }
            }
        }
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("§6Demetrian Nursery Barrel");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new DemetrianSaplingBarrelMenu(syncId, playerInventory, this, this.containerData);
    }

    // --- WorldlyContainer Implementation for Hoppers and Logistics Pipes ---

    @Override
    public int getContainerSize() {
        return 6; // 0,1,2 = Virtual Inputs | 3,4,5 = Virtual Outputs
    }

    @Override
    public boolean isEmpty() {
        for (Chamber c : chambers) {
            if (!c.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot >= 3 && slot <= 5) {
            int ch = slot - 3;
            Chamber c = chambers[ch];
            if (!c.variant.isBlank() && c.amount > 0) {
                int count = (int) Math.min(c.amount, c.variant.toStack().getMaxStackSize());
                return c.variant.toStack(count);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot >= 3 && slot <= 5) {
            int ch = slot - 3;
            Chamber c = chambers[ch];
            if (!c.variant.isBlank() && c.amount > 0) {
                int toTake = (int) Math.min(amount, Math.min(c.amount, c.variant.toStack().getMaxStackSize()));
                ItemStack result = c.variant.toStack(toTake);
                c.amount -= toTake;
                if (c.amount <= 0 && !c.locked) {
                    c.variant = ItemVariant.blank();
                }
                sync();
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return removeItem(slot, 64);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot <= 2 && !stack.isEmpty() && isSapling(stack)) {
            Chamber c = chambers[slot];
            ItemVariant v = ItemVariant.of(stack);
            if (c.variant.isBlank() || c.variant.equals(v)) {
                c.setVariant(v);
                long space = MAX_CHAMBER_CAPACITY - c.amount;
                int added = (int) Math.min(stack.getCount(), space);
                c.amount += added;
                stack.shrink(added);
                sync();
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (Chamber c : chambers) {
            c.variant = ItemVariant.blank();
            c.amount = 0;
        }
        sync();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return new int[]{3, 4, 5}; // Extract from outputs
        } else {
            return new int[]{0, 1, 2}; // Insert into inputs
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (slot >= 0 && slot <= 2 && isSapling(stack)) {
            Chamber c = chambers[slot];
            if (c.amount >= MAX_CHAMBER_CAPACITY) return false;
            ItemVariant v = ItemVariant.of(stack);
            return c.variant.isBlank() || c.variant.equals(v);
        }
        return false;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 3 && slot <= 5 && !stack.isEmpty();
    }

    // --- NBT Serialization ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag chTag = new CompoundTag();
        for (int i = 0; i < CHAMBER_COUNT; i++) {
            Chamber c = chambers[i];
            CompoundTag single = new CompoundTag();
            if (!c.variant.isBlank()) {
                single.put("Item", c.variant.toStack().saveOptional(registries));
            }
            single.putLong("Amount", c.amount);
            single.putBoolean("Locked", c.locked);
            chTag.put("Chamber" + i, single);
        }
        tag.put("Chambers", chTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Chambers")) {
            CompoundTag chTag = tag.getCompound("Chambers");
            for (int i = 0; i < CHAMBER_COUNT; i++) {
                if (chTag.contains("Chamber" + i)) {
                    CompoundTag single = chTag.getCompound("Chamber" + i);
                    Chamber c = chambers[i];
                    if (single.contains("Item")) {
                        ItemStack stack = ItemStack.parseOptional(registries, single.getCompound("Item"));
                        c.variant = ItemVariant.of(stack);
                    } else {
                        c.variant = ItemVariant.blank();
                    }
                    c.amount = single.getLong("Amount");
                    c.locked = single.getBoolean("Locked");
                }
            }
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }
}
