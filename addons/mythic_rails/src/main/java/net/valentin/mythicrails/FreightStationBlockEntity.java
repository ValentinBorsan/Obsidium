package net.valentin.mythicrails;

import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/** Nine persistent buffer slots with auto-import/auto-export to adjacent chests and machines. */
public final class FreightStationBlockEntity extends BlockEntity implements Container {
    private static final Direction[] PORT_SIDES = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN
    };
    private final NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);

    public FreightStationBlockEntity(BlockPos pos, BlockState state) {
        super(MythicRails.STATION_BE, pos, state);
    }

    public Storage<ItemVariant> storage() {
        return InventoryStorage.of(this, null);
    }

    public List<Storage<ItemVariant>> externalStorages() {
        List<Storage<ItemVariant>> list = new ArrayList<>();
        if (level == null) return list;
        for (Direction side : PORT_SIDES) {
            BlockPos other = worldPosition.relative(side);
            if (!level.getChunkSource().hasChunk(other.getX() >> 4, other.getZ() >> 4)) continue;
            var found = ItemStorage.SIDED.find(level, other, side.getOpposite());
            if (found != null) list.add(found);
        }
        return list;
    }

    public void bridgeAll(TransactionContext tx) {
        boolean loading = ((FreightStationBlock) getBlockState().getBlock()).isLoader();
        for (var ext : externalStorages()) {
            long moved = StorageUtil.move(
                    loading ? ext : storage(),
                    loading ? storage() : ext,
                    v -> true, 16, tx);
            if (moved > 0) break;
        }
    }

    public boolean allExternalEmpty() {
        for (var ext : externalStorages()) {
            for (var view : ext) {
                if (!view.isResourceBlank() && view.getAmount() > 0) return false;
            }
        }
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FreightStationBlockEntity station) {
        if (level.getGameTime() % 4 != 0 || !state.getValue(FreightStationBlock.ENABLED)) return;
        // A docked freight cart performs the same bridge inside its cargo transaction.
        if (!level.getEntitiesOfClass(HermesFreightCart.class, new AABB(pos.above()).inflate(0.1, 0.2, 0.1)).isEmpty()) return;
        try (var tx = Transaction.openOuter()) {
            station.bridgeAll(tx);
            tx.commit();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    @Override
    public void setChanged() {
        // This block has no comparator output. Avoid vanilla's neighbor probes at unloaded chunk edges.
        if (level != null) level.blockEntityChanged(worldPosition);
    }

    @Override public int getContainerSize() { return 9; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        var result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        var result = ContainerHelper.takeItem(items, slot);
        setChanged();
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(Math.min(stack.getMaxStackSize(), getMaxStackSize()));
        setChanged();
    }

    @Override public void clearContent() { items.clear(); setChanged(); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
}
