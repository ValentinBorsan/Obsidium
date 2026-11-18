package net.valentin.obsidium.block;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;

public final class AegisSanctuaryWardMenu extends AbstractContainerMenu {
    private final ContainerData data;
    private final @Nullable AegisSanctuaryWardBlockEntity blockEntity;

    public AegisSanctuaryWardMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainerData(AegisSanctuaryWardBlockEntity.DATA_COUNT), null);
    }

    public AegisSanctuaryWardMenu(int syncId, Inventory playerInventory, ContainerData data, @Nullable AegisSanctuaryWardBlockEntity blockEntity) {
        super(ObsidiumMod.AEGIS_SANCTUARY_WARD_MENU, syncId);
        checkContainerDataCount(data, AegisSanctuaryWardBlockEntity.DATA_COUNT);
        this.data = data;
        this.blockEntity = blockEntity;
        this.addDataSlots(data);
    }

    public long energy() { return data.get(0) * 100L; }
    public boolean isActive() { return data.get(1) != 0; }
    public boolean isManualEnabled() { return data.get(2) != 0; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && blockEntity != null && stillValid(player)) {
            blockEntity.toggleManualEnabled();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity == null || (player.level() == blockEntity.getLevel() && !blockEntity.isRemoved()
                && player.distanceToSqr(blockEntity.getBlockPos().getX() + 0.5,
                blockEntity.getBlockPos().getY() + 0.5,
                blockEntity.getBlockPos().getZ() + 0.5) <= 64.0);
    }
}
