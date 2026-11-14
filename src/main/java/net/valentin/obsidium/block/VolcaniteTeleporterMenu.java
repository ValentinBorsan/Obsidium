package net.valentin.obsidium.block;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.VolcaniteWarpGauntletItem;
import org.jetbrains.annotations.Nullable;

/**
 * Container menu for both in-world Volcanic Teleporter pads and the handheld Volcanite Warp Gauntlet.
 */
public class VolcaniteTeleporterMenu extends AbstractContainerMenu {
    private final ContainerData data;
    @Nullable
    private final VolcanicTeleporterBlockEntity blockEntity;
    @Nullable
    private final InteractionHand gauntletHand;

    public VolcaniteTeleporterMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainerData(VolcanicTeleporterBlockEntity.DATA_COUNT), null, null);
    }

    public VolcaniteTeleporterMenu(int syncId, Inventory playerInventory, ContainerData data,
                                   @Nullable VolcanicTeleporterBlockEntity blockEntity) {
        this(syncId, playerInventory, data, blockEntity, null);
    }

    public VolcaniteTeleporterMenu(int syncId, Inventory playerInventory, InteractionHand gauntletHand, ItemStack gauntletStack) {
        this(syncId, playerInventory, createGauntletData(gauntletStack), null, gauntletHand);
    }

    private VolcaniteTeleporterMenu(int syncId, Inventory playerInventory, ContainerData data,
                                    @Nullable VolcanicTeleporterBlockEntity blockEntity,
                                    @Nullable InteractionHand gauntletHand) {
        super(ObsidiumMod.VOLCANIC_TELEPORTER_MENU, syncId);
        checkContainerDataCount(data, VolcanicTeleporterBlockEntity.DATA_COUNT);
        this.data = data;
        this.blockEntity = blockEntity;
        this.gauntletHand = gauntletHand;
        this.addDataSlots(data);
    }

    private static ContainerData createGauntletData(ItemStack gauntletStack) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                long stored = 0L;
                if (gauntletStack.getItem() instanceof VolcaniteWarpGauntletItem g) {
                    stored = g.getStoredEnergy(gauntletStack);
                }
                long max = VolcaniteWarpGauntletItem.ENERGY_CAPACITY;
                return switch (index) {
                    case 0 -> (int) (stored & 0xFFFF);
                    case 1 -> (int) ((stored >> 16) & 0xFFFF);
                    case 2 -> (int) (max & 0xFFFF);
                    case 3 -> (int) ((max >> 16) & 0xFFFF);
                    case 4 -> 1; // Supreme cross-dim capability
                    case 5 -> 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return VolcanicTeleporterBlockEntity.DATA_COUNT;
            }
        };
    }

    @Nullable
    public VolcanicTeleporterBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Nullable
    public InteractionHand getGauntletHand() {
        return gauntletHand;
    }

    public long getStoredRf() {
        long low = data.get(0) & 0xFFFFL;
        long high = data.get(1) & 0xFFFFL;
        return (high << 16) | low;
    }

    public long getMaxRf() {
        long low = data.get(2) & 0xFFFFL;
        long high = data.get(3) & 0xFFFFL;
        return Math.max(1L, (high << 16) | low);
    }

    public boolean isSupreme() {
        return (data.get(4) & 1) != 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.blockEntity != null) {
            if (this.blockEntity.isRemoved()) return false;
            return player.distanceToSqr(
                    this.blockEntity.getBlockPos().getX() + 0.5,
                    this.blockEntity.getBlockPos().getY() + 0.5,
                    this.blockEntity.getBlockPos().getZ() + 0.5) <= 64.0;
        }
        if (this.gauntletHand != null) {
            return player.getItemInHand(this.gauntletHand).getItem() instanceof VolcaniteWarpGauntletItem;
        }
        return true;
    }
}

