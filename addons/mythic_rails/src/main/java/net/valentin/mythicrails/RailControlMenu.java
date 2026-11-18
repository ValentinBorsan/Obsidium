package net.valentin.mythicrails;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Syncs up to 8 linked Signal Connectors to RailControlScreen and dispatches remote toggle commands. */
public final class RailControlMenu extends AbstractContainerMenu {
    // Data layout: [0] = linkCount, then for each slot i (0..7): [1+i*2] = kind, [2+i*2] = mode
    private static final int DATA_COUNT = 1 + RailControlBlockEntity.MAX_LINKS * 2;
    private final Level level;
    private final BlockPos pos;
    private final ContainerData data;

    public RailControlMenu(int id, Inventory inv) {
        this(id, inv, null, BlockPos.ZERO);
    }

    public RailControlMenu(int id, Inventory inv, Level level, BlockPos pos) {
        super(MythicRails.RAIL_CONTROL_MENU, id);
        this.level = level;
        this.pos = pos.immutable();
        this.data = level == null ? new SimpleContainerData(DATA_COUNT) : new ContainerData() {
            @Override
            public int get(int index) {
                if (!(level.getBlockEntity(pos) instanceof RailControlBlockEntity ctrl)) return 0;
                ctrl.pruneMissing();
                if (index == 0) return ctrl.links().size();
                int slot = (index - 1) / 2;
                int field = (index - 1) % 2;
                var conn = ctrl.connectorAt(slot);
                if (conn == null) return 0;
                return field == 0 ? conn.targetKind() : conn.mode();
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
        addDataSlots(data);
    }

    public int linkCount() { return data.get(0); }
    public int slotKind(int slot) { return data.get(1 + slot * 2); }
    public int slotMode(int slot) { return data.get(2 + slot * 2); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (level == null || level.isClientSide) return false;
        if (!(level.getBlockEntity(pos) instanceof RailControlBlockEntity ctrl)) return false;
        if (id >= 0 && id < RailControlBlockEntity.MAX_LINKS) {
            ctrl.cycleConnector(id);
            return true;
        }
        switch (id) {
            case 100 -> { ctrl.setAllSemaphores(2); return true; } // ALL GREEN
            case 101 -> { ctrl.setAllSemaphores(1); return true; } // ALL RED
            case 102 -> { ctrl.setAllSemaphores(0); return true; } // ALL AUTO
            case 103 -> { ctrl.toggleAllBumpers(); return true; }  // BUMPERS
            case 104 -> { ctrl.toggleAllSwitches(); return true; } // SWITCHES
            default -> { return false; }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockState(pos).is(MythicRails.RAIL_CONTROL_SYSTEM)
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
