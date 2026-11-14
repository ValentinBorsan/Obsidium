package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.VolcaniteBarrelUpgradeItem;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class VolcaniteItemBarrelBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {
    public enum BarrelTier {
        BASIC(4096, "Basic Volcanite Barrel", 0xFFFF9933),
        ADVANCED(16384, "Advanced Volcanite Barrel", 0xFF00E5FF),
        SUPREME(65536, "Supreme Volcanite Barrel", 0xFFFF3399);

        private final long baseCapacity;
        private final String displayName;
        private final int color;

        BarrelTier(long baseCapacity, String displayName, int color) {
            this.baseCapacity = baseCapacity;
            this.displayName = displayName;
            this.color = color;
        }

        public long getBaseCapacity() {
            return baseCapacity;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getColor() {
            return color;
        }
    }

    private final BarrelTier tier;
    private ItemVariant storedVariant = ItemVariant.blank();
    private long storedAmount = 0;
    private boolean locked = false;

    public final SimpleContainer upgrades = new SimpleContainer(4) {
        @Override
        public void setChanged() {
            super.setChanged();
            VolcaniteItemBarrelBlockEntity.this.sync();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.getItem() instanceof VolcaniteBarrelUpgradeItem;
        }
    };

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) (storedAmount & 0xFFFF);
                case 1 -> (int) ((storedAmount >> 16) & 0xFFFF);
                case 2 -> (int) (getMaxCapacity() & 0xFFFF);
                case 3 -> (int) ((getMaxCapacity() >> 16) & 0xFFFF);
                case 4 -> locked ? 1 : 0;
                case 5 -> hasVoidUpgrade() ? 1 : 0;
                case 6 -> tier.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 4) {
                locked = (value == 1);
                sync();
            }
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public VolcaniteItemBarrelBlockEntity(BlockPos pos, BlockState state, BarrelTier tier) {
        super(switch (tier) {
            case ADVANCED -> ObsidiumMod.ADVANCED_VOLCANITE_ITEM_BARREL_BE;
            case SUPREME -> ObsidiumMod.SUPREME_VOLCANITE_ITEM_BARREL_BE;
            default -> ObsidiumMod.BASIC_VOLCANITE_ITEM_BARREL_BE;
        }, pos, state);
        this.tier = tier;
    }

    public BarrelTier getTier() {
        return tier;
    }

    public ItemVariant getStoredVariant() {
        return storedVariant;
    }

    public ItemStack getStoredItemSample() {
        return storedVariant.isBlank() ? ItemStack.EMPTY : storedVariant.toStack();
    }

    public long getStoredAmount() {
        return storedAmount;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        sync();
    }

    public boolean hasVoidUpgrade() {
        for (int i = 0; i < upgrades.getContainerSize(); i++) {
            ItemStack stack = upgrades.getItem(i);
            if (stack.getItem() instanceof VolcaniteBarrelUpgradeItem up && up.getUpgradeType() == VolcaniteBarrelUpgradeItem.UpgradeType.VOID) {
                return true;
            }
        }
        return false;
    }

    public long getMaxCapacity() {
        long multiplier = 1;
        for (int i = 0; i < upgrades.getContainerSize(); i++) {
            ItemStack stack = upgrades.getItem(i);
            if (stack.getItem() instanceof VolcaniteBarrelUpgradeItem up) {
                multiplier += up.getUpgradeType().getMultiplier();
            }
        }
        return tier.getBaseCapacity() * multiplier;
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // --- Physical In-World UX Interactions ---

    public boolean depositStack(ItemStack stack, boolean allMatching, Player player) {
        if (stack == null || stack.isEmpty()) return false;

        ItemVariant resource = ItemVariant.of(stack);
        if (storedVariant.isBlank()) {
            if (!locked || storedVariant.equals(resource)) {
                storedVariant = resource;
            } else {
                return false;
            }
        } else if (!storedVariant.equals(resource)) {
            return false;
        }

        long capacity = getMaxCapacity();
        long space = capacity - storedAmount;
        boolean voiding = hasVoidUpgrade();

        if (allMatching && player != null) {
            long totalDeposited = 0;
            Inventory inv = player.getInventory();
            // Only pull from the 36 regular inventory slots, not armor/offhand.
            for (int i = 0; i < Math.min(36, inv.getContainerSize()); i++) {
                ItemStack invStack = inv.getItem(i);
                if (!invStack.isEmpty() && ItemVariant.of(invStack).equals(storedVariant)) {
                    long toAdd = voiding ? invStack.getCount() : Math.min(invStack.getCount(), capacity - storedAmount);
                    if (toAdd > 0) {
                        invStack.shrink((int) toAdd);
                        if (!voiding || storedAmount < capacity) {
                            storedAmount = Math.min(capacity, storedAmount + toAdd);
                        }
                        totalDeposited += toAdd;
                    }
                }
            }
            if (totalDeposited > 0) {
                if (level != null) level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.4F);
                sync();
                return true;
            }
            return false;
        } else {
            long toAdd = voiding ? stack.getCount() : Math.min(stack.getCount(), space);
            if (toAdd > 0) {
                stack.shrink((int) toAdd);
                if (!voiding || storedAmount < capacity) {
                    storedAmount = Math.min(capacity, storedAmount + toAdd);
                }
                if (level != null) level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.4F);
                sync();
                return true;
            }
        }
        return false;
    }

    public boolean extractItem(Player player, boolean fullStack) {
        if (storedVariant.isBlank() || storedAmount <= 0) return false;

        int maxToTake = fullStack ? (int) Math.min(64, storedVariant.toStack().getMaxStackSize()) : 1;
        int count = (int) Math.min(storedAmount, maxToTake);
        if (count <= 0) return false;

        ItemStack extractStack = storedVariant.toStack(count);
        storedAmount -= count;
        if (storedAmount == 0 && !locked) {
            storedVariant = ItemVariant.blank();
        }

        if (player != null) {
            if (!player.getInventory().add(extractStack)) {
                Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), extractStack);
            }
            if (level != null) level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 1.8F);
        } else {
            if (level != null) Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, extractStack);
        }

        sync();
        return true;
    }

    public boolean toggleLock(Player player) {
        locked = !locked;
        if (storedAmount == 0 && !locked) {
            storedVariant = ItemVariant.blank();
        }
        if (level != null) level.playSound(null, worldPosition, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, locked ? 1.4F : 0.9F);
        if (player != null) {
            String status = locked ? "§aLOCKED 🔒 (Filter preserved even when empty)" : "§cUNLOCKED 🔓 (Clears filter when empty)";
            player.displayClientMessage(Component.literal("§6[Barrel] §f" + status), true);
        }
        sync();
        return true;
    }

    public boolean installUpgrade(ItemStack stack, Player player) {
        if (!(stack.getItem() instanceof VolcaniteBarrelUpgradeItem upItem)) return false;

        for (int i = 0; i < upgrades.getContainerSize(); i++) {
            if (upgrades.getItem(i).isEmpty()) {
                upgrades.setItem(i, stack.split(1));
                if (level != null) level.playSound(null, worldPosition, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 1.0F, 1.2F);
                if (player != null) {
                    player.displayClientMessage(Component.literal("§6[Barrel] §aInstalled Upgrade: §e" + upItem.getUpgradeType().getDisplayName()), true);
                }
                sync();
                return true;
            }
        }
        if (player != null) {
            player.displayClientMessage(Component.literal("§c[Barrel] All 4 upgrade slots are full!"), true);
        }
        return false;
    }

    // --- Fabric Transfer API Storage Implementation ---

    public final Storage<ItemVariant> itemStorage = new Storage<ItemVariant>() {
        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource == null || resource.isBlank() || maxAmount <= 0) return 0;

            if (storedVariant.isBlank()) {
                if (locked) return 0;
            } else if (!storedVariant.equals(resource)) {
                return 0;
            }

            long capacity = getMaxCapacity();
            long space = capacity - storedAmount;
            boolean voiding = hasVoidUpgrade();

            long toInsert = voiding ? maxAmount : Math.min(maxAmount, space);
            if (toInsert > 0) {
                transaction.addCloseCallback((t, res) -> {
                    if (res.wasCommitted()) {
                        if (storedVariant.isBlank()) {
                            storedVariant = resource;
                        }
                        if (voiding) {
                            storedAmount = Math.min(capacity, storedAmount + maxAmount);
                        } else {
                            storedAmount += toInsert;
                        }
                        sync();
                    }
                });
                return toInsert;
            }
            return 0;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource == null || resource.isBlank() || storedVariant.isBlank() || storedAmount <= 0 || maxAmount <= 0) return 0;
            if (!storedVariant.equals(resource)) return 0;

            long toExtract = Math.min(maxAmount, storedAmount);
            if (toExtract > 0) {
                transaction.addCloseCallback((t, res) -> {
                    if (res.wasCommitted()) {
                        storedAmount -= toExtract;
                        if (storedAmount == 0 && !locked) {
                            storedVariant = ItemVariant.blank();
                        }
                        sync();
                    }
                });
                return toExtract;
            }
            return 0;
        }

        @Override
        public Iterator<StorageView<ItemVariant>> iterator() {
            if (storedVariant.isBlank() || storedAmount <= 0) {
                return Collections.emptyIterator();
            }
            return List.<StorageView<ItemVariant>>of(new StorageView<ItemVariant>() {
                @Override
                public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
                    return itemStorage.extract(resource, maxAmount, transaction);
                }

                @Override
                public boolean isResourceBlank() {
                    return storedVariant.isBlank();
                }

                @Override
                public ItemVariant getResource() {
                    return storedVariant;
                }

                @Override
                public long getAmount() {
                    return storedAmount;
                }

                @Override
                public long getCapacity() {
                    return getMaxCapacity();
                }
            }).iterator();
        }
    };

    // --- Vanilla WorldlyContainer Adapter (Hoppers, Droppers, Mod Machines) ---

    @Override
    public int getContainerSize() {
        return 2; // Slot 0: virtual Input, Slot 1: virtual Output
    }

    @Override
    public boolean isEmpty() {
        return storedAmount == 0 && storedVariant.isBlank();
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot == 1 && !storedVariant.isBlank() && storedAmount > 0) {
            int count = (int) Math.min(storedAmount, storedVariant.toStack().getMaxStackSize());
            return storedVariant.toStack(count);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot == 1 && !storedVariant.isBlank() && storedAmount > 0) {
            int toTake = (int) Math.min(amount, Math.min(storedAmount, storedVariant.toStack().getMaxStackSize()));
            ItemStack out = storedVariant.toStack(toTake);
            storedAmount -= toTake;
            if (storedAmount == 0 && !locked) {
                storedVariant = ItemVariant.blank();
            }
            sync();
            return out;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return removeItem(slot, 64);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0 && !stack.isEmpty()) {
            depositStack(stack, false, null);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        storedAmount = 0;
        if (!locked) storedVariant = ItemVariant.blank();
        sync();
    }

    // --- Direct Pipe / Hopper Interaction Helpers ---

    public int insertItemFromPipe(ItemStack stack, @Nullable Direction side, boolean simulate) {
        if (stack == null || stack.isEmpty()) return 0;
        ItemVariant variant = ItemVariant.of(stack);
        if (storedVariant.isBlank()) {
            if (locked) return 0;
        } else if (!storedVariant.equals(variant)) {
            return 0;
        }

        long capacity = getMaxCapacity();
        long space = capacity - storedAmount;
        boolean voiding = hasVoidUpgrade();
        if (space <= 0 && !voiding) return 0;

        int toAdd = (int) Math.min(stack.getCount(), voiding ? stack.getCount() : space);
        if (toAdd > 0 && !simulate) {
            if (storedVariant.isBlank()) {
                storedVariant = variant;
            }
            if (!voiding || storedAmount < capacity) {
                storedAmount = Math.min(capacity, storedAmount + toAdd);
            }
            sync();
        }
        return toAdd;
    }

    public ItemStack extractItemForPipe(int maxAmount, @Nullable ItemStack filter, @Nullable Direction side, boolean simulate) {
        if (storedVariant.isBlank() || storedAmount <= 0 || maxAmount <= 0) return ItemStack.EMPTY;
        ItemStack sample = storedVariant.toStack();
        if (filter != null && !filter.isEmpty() && !ItemStack.isSameItem(sample, filter)) return ItemStack.EMPTY;

        int toTake = (int) Math.min(maxAmount, Math.min(storedAmount, sample.getMaxStackSize()));
        if (toTake <= 0) return ItemStack.EMPTY;

        ItemStack result = storedVariant.toStack(toTake);
        if (!simulate) {
            storedAmount -= toTake;
            if (storedAmount == 0 && !locked) {
                storedVariant = ItemVariant.blank();
            }
            sync();
        }
        return result;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot != 0 || stack == null || stack.isEmpty()) return false;
        ItemVariant variant = ItemVariant.of(stack);
        if (storedVariant.isBlank()) return !locked;
        if (!storedVariant.equals(variant)) return false;
        return hasVoidUpgrade() || storedAmount < getMaxCapacity();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{0, 1}; // Slot 0: Input, Slot 1: Output
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 1 && !storedVariant.isBlank() && storedAmount > 0;
    }

    // --- NBT Serialization ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!storedVariant.isBlank()) {
            tag.put("StoredItem", storedVariant.toStack().saveOptional(registries));
        }
        tag.putLong("StoredAmount", storedAmount);
        tag.putBoolean("Locked", locked);

        CompoundTag upTag = new CompoundTag();
        for (int i = 0; i < upgrades.getContainerSize(); i++) {
            ItemStack u = upgrades.getItem(i);
            if (!u.isEmpty()) {
                upTag.put("Slot" + i, u.saveOptional(registries));
            }
        }
        tag.put("Upgrades", upTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("StoredItem")) {
            ItemStack stack = ItemStack.parseOptional(registries, tag.getCompound("StoredItem"));
            storedVariant = ItemVariant.of(stack);
        } else {
            storedVariant = ItemVariant.blank();
        }
        storedAmount = tag.getLong("StoredAmount");
        locked = tag.getBoolean("Locked");

        if (tag.contains("Upgrades")) {
            CompoundTag upTag = tag.getCompound("Upgrades");
            for (int i = 0; i < upgrades.getContainerSize(); i++) {
                if (upTag.contains("Slot" + i)) {
                    upgrades.setItem(i, ItemStack.parseOptional(registries, upTag.getCompound("Slot" + i)));
                } else {
                    upgrades.setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(tier.getDisplayName());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new VolcaniteItemBarrelMenu(syncId, playerInventory, this, this.upgrades, this.data);
    }
}
