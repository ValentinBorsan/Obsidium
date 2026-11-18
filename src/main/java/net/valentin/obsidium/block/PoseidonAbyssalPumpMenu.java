package net.valentin.obsidium.block;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;

public class PoseidonAbyssalPumpMenu extends AbstractContainerMenu {
    private final ContainerData data;
    private final @Nullable PoseidonAbyssalPumpBlockEntity blockEntity;

    public PoseidonAbyssalPumpMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainerData(PoseidonAbyssalPumpBlockEntity.DATA_COUNT), null);
    }

    public PoseidonAbyssalPumpMenu(int syncId, Inventory playerInventory, ContainerData data, @Nullable PoseidonAbyssalPumpBlockEntity blockEntity) {
        super(ObsidiumMod.POSEIDON_ABYSSAL_PUMP_MENU, syncId);
        checkContainerDataCount(data, PoseidonAbyssalPumpBlockEntity.DATA_COUNT);
        this.data = data;
        this.blockEntity = blockEntity;
        this.addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (blockEntity != null && player instanceof ServerPlayer sp) {
            blockEntity.handleButton(id, sp);
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
        if (blockEntity == null) return true;
        if (blockEntity.isRemoved()) return false;
        return player.distanceToSqr(
                blockEntity.getBlockPos().getX() + 0.5,
                blockEntity.getBlockPos().getY() + 0.5,
                blockEntity.getBlockPos().getZ() + 0.5) <= 64.0;
    }

    public boolean isFormed() { return data.get(0) != 0; }
    public boolean isShowHologram() { return data.get(1) != 0; }
    public long getEnergyKrf() {
        long low = Integer.toUnsignedLong(data.get(2)) & 0xFFFFL;
        long high = Integer.toUnsignedLong(data.get(16)) & 0xFFFFL;
        return (high << 16) | low;
    }
    public long getFluidMb() {
        long low = Integer.toUnsignedLong(data.get(3)) & 0xFFFFL;
        long high = Integer.toUnsignedLong(data.get(17)) & 0xFFFFL;
        return (high << 16) | low;
    }
    public int getSpeedTier() { return data.get(4); }
    public int getRadiusIndex() { return data.get(5); }
    public int getReplaceMode() { return data.get(6); }
    public int getFilterMode() { return data.get(7); }
    public int getRedstoneMode() { return data.get(8); }
    public boolean isQuantumEnabled() { return data.get(9) != 0; }
    public DyeColor getQuantumColor() { return DyeColor.byId(Math.floorMod(data.get(10), 16)); }
    public int getQuantumChannel() { return data.get(11); }
    public boolean isQuantumPrivate() { return data.get(12) != 0; }
    public int getPumpStatus() { return data.get(13); }
    public int getScanY() { return data.get(14); }
    public Fluid getCurrentFluid() {
        int id = data.get(15);
        return id > 0 ? BuiltInRegistries.FLUID.byId(id) : Fluids.EMPTY;
    }
}
