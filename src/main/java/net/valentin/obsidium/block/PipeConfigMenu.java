package net.valentin.obsidium.block;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.storage.QuantumLinkNetworkState;
import org.jetbrains.annotations.Nullable;

public class PipeConfigMenu extends AbstractContainerMenu {
    public static final int BTN_FACE_BASE = 0; // 0..5 -> D,U,N,S,W,E
    public static final int BTN_CYCLE_FACE_MODE = 6;
    public static final int BTN_CYCLE_REDSTONE = 7;
    public static final int BTN_TOGGLE_ROUND_ROBIN = 8;
    public static final int BTN_CYCLE_THROUGHPUT = 9;
    public static final int BTN_PREV_PIPE_COLOR = 10;
    public static final int BTN_NEXT_PIPE_COLOR = 11;
    public static final int BTN_Q_PREV_COLOR = 12;
    public static final int BTN_Q_NEXT_COLOR = 13;
    public static final int BTN_Q_DEC_1 = 14;
    public static final int BTN_Q_INC_1 = 15;
    public static final int BTN_Q_DEC_10 = 16;
    public static final int BTN_Q_INC_10 = 17;
    public static final int BTN_Q_TOGGLE_PRIVATE = 18;

    private final ContainerData data;
    private final @Nullable PipeConfigurable pipe;
    private int selectedFace = 2; // NORTH default

    public PipeConfigMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(1), new SimpleContainerData(11), null);
    }

    public PipeConfigMenu(int syncId, Inventory playerInventory, PipeConfigurable pipe) {
        this(syncId, playerInventory,
                pipe.getFilterContainer() != null ? pipe.getFilterContainer() : new SimpleContainer(1),
                new SimpleContainerData(11),
                pipe);
    }

    private PipeConfigMenu(int syncId, Inventory playerInventory, Container filterContainer, ContainerData clientData,
            @Nullable PipeConfigurable pipe) {
        super(ObsidiumMod.PIPE_CONFIG_MENU, syncId);
        this.pipe = pipe;

        if (pipe != null) {
            this.data = new ContainerData() {
                @Override
                public int get(int index) {
                    return switch (index) {
                        case 0 -> pipe.getPipeKind();
                        case 1 -> selectedFace;
                        case 2 -> pipe.getPipeColor();
                        case 3 -> pipe.getRedstoneMode();
                        case 4 -> pipe.isRoundRobin() ? 1 : 0;
                        case 5 -> pipe.getThroughputPercent();
                        case 6 -> pipe.getFaceModeOrdinal(Direction.from3DDataValue(selectedFace));
                        case 7 -> pipe.isQuantumLinked() ? 1 : 0;
                        case 8 -> pipe.getQuantumColor().getId();
                        case 9 -> pipe.getQuantumChannel();
                        case 10 -> pipe.isQuantumPrivate() ? 1 : 0;
                        default -> 0;
                    };
                }

                @Override
                public void set(int index, int value) {
                    if (index == 1)
                        selectedFace = Math.floorMod(value, 6);
                }

                @Override
                public int getCount() {
                    return 11;
                }
            };
        } else {
            this.data = clientData;
        }

        checkContainerSize(filterContainer, 1);
        checkContainerDataCount(this.data, 11);
        this.addDataSlots(this.data);

        // Fluid Filter Slot (at 148, 42) - active for Fluid Pipe
        this.addSlot(new Slot(filterContainer, 0, 148, 42) {
            @Override
            public boolean isActive() {
                return getPipeKind() == PipeConfigurable.KIND_FLUID;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // Player Inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 108 + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 166));
        }
    }

    public int getPipeKind() {
        return data.get(0);
    }

    public int getSelectedFace() {
        return Math.floorMod(data.get(1), 6);
    }

    public int getPipeColor() {
        return data.get(2);
    }

    public int getRedstoneMode() {
        return data.get(3);
    }

    public boolean isRoundRobin() {
        return data.get(4) != 0;
    }

    public int getThroughputPercent() {
        int v = data.get(5);
        return (v == 25 || v == 50 || v == 75 || v == 100) ? v : 100;
    }

    public int getFaceModeOrdinal() {
        return data.get(6);
    }

    public boolean isQuantumLinked() {
        return data.get(7) != 0;
    }

    public DyeColor getQuantumColor() {
        return DyeColor.byId(Math.floorMod(data.get(8), 16));
    }

    public int getQuantumChannel() {
        return QuantumLinkNetworkState.clampChannel(data.get(9));
    }

    public boolean isQuantumPrivate() {
        return data.get(10) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (pipe == null || !stillValid(player) || !pipe.canConfigure(player.getUUID()))
            return false;
        pipe.ensureQuantumOwner(player.getUUID());
        if (id >= BTN_FACE_BASE && id < BTN_FACE_BASE + 6) {
            this.selectedFace = id;
            broadcastChanges();
            return true;
        }
        switch (id) {
            case BTN_CYCLE_FACE_MODE -> pipe.cycleFaceMode(Direction.from3DDataValue(selectedFace));
            case BTN_CYCLE_REDSTONE -> pipe.setRedstoneMode((pipe.getRedstoneMode() + 1) % 4);
            case BTN_TOGGLE_ROUND_ROBIN -> pipe.setRoundRobin(!pipe.isRoundRobin());
            case BTN_CYCLE_THROUGHPUT ->
                pipe.setThroughputPercent(PipeConfigurable.nextThroughput(pipe.getThroughputPercent()));
            case BTN_PREV_PIPE_COLOR -> {
                int c = pipe.getPipeColor();
                pipe.setPipeColor(c <= -1 ? 15 : (c == 0 ? -1 : c - 1));
            }
            case BTN_NEXT_PIPE_COLOR -> {
                int c = pipe.getPipeColor();
                pipe.setPipeColor(c >= 15 ? -1 : c + 1);
            }
            case BTN_Q_PREV_COLOR ->
                pipe.setQuantumColor(DyeColor.byId(Math.floorMod(pipe.getQuantumColor().getId() - 1, 16)));
            case BTN_Q_NEXT_COLOR ->
                pipe.setQuantumColor(DyeColor.byId(Math.floorMod(pipe.getQuantumColor().getId() + 1, 16)));
            case BTN_Q_DEC_1 -> pipe.setQuantumChannel(pipe.getQuantumChannel() - 1);
            case BTN_Q_INC_1 -> pipe.setQuantumChannel(pipe.getQuantumChannel() + 1);
            case BTN_Q_DEC_10 -> pipe.setQuantumChannel(pipe.getQuantumChannel() - 10);
            case BTN_Q_INC_10 -> pipe.setQuantumChannel(pipe.getQuantumChannel() + 10);
            case BTN_Q_TOGGLE_PRIVATE -> pipe.setQuantumPrivate(!pipe.isQuantumPrivate());
            default -> {
                return false;
            }
        }
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack orig = slot.getItem();
            newStack = orig.copy();
            if (index == 0) {
                if (!this.moveItemStackTo(orig, 1, this.slots.size(), true))
                    return ItemStack.EMPTY;
            } else if (getPipeKind() == PipeConfigurable.KIND_FLUID) {
                if (!this.moveItemStackTo(orig, 0, 1, false))
                    return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
            if (orig.isEmpty())
                slot.setByPlayer(ItemStack.EMPTY);
            else
                slot.setChanged();
        }
        return newStack;
    }

    @Override
    public boolean stillValid(Player player) {
        if (pipe == null)
            return true; // Client menu has no server block entity.
        if (!(pipe instanceof net.minecraft.world.level.block.entity.BlockEntity block))
            return false;
        return !block.isRemoved() && block.getLevel() == player.level()
                && player.level().getBlockEntity(block.getBlockPos()) == block
                && player.distanceToSqr(block.getBlockPos().getX() + 0.5,
                        block.getBlockPos().getY() + 0.5, block.getBlockPos().getZ() + 0.5) <= 64.0
                && pipe.canConfigure(player.getUUID());
    }
}
