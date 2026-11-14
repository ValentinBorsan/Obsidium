package net.valentin.obsidium.block;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;

public class TartarusPistonEngineMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    @Nullable
    private final TartarusPistonEngineBlockEntity blockEntity;

    public TartarusPistonEngineMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(TartarusPistonEngineBlockEntity.INVENTORY_SIZE),
                new SimpleContainerData(TartarusPistonEngineBlockEntity.DATA_COUNT), null);
    }

    public TartarusPistonEngineMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        this(syncId, playerInventory, container, data, null);
    }

    public TartarusPistonEngineMenu(int syncId, Inventory playerInventory, Container container, ContainerData data,
                                    @Nullable TartarusPistonEngineBlockEntity blockEntity) {
        super(ObsidiumMod.TARTARUS_PISTON_ENGINE_MENU, syncId);
        checkContainerSize(container, TartarusPistonEngineBlockEntity.INVENTORY_SIZE);
        checkContainerDataCount(data, TartarusPistonEngineBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockEntity = blockEntity;

        container.startOpen(playerInventory.player);

        // Slot 0: Fuel input (54, 21)
        this.addSlot(new Slot(container, 0, 54, 21));
        // Slot 1: Empty bucket return (54, 49)
        this.addSlot(new Slot(container, 1, 54, 49) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });
        // Slot 2: Battery / Tool charge slot (150, 49)
        this.addSlot(new Slot(container, 2, 150, 49));
        // Slot 3: Cryo-Ice Coolant Input (150, 21)
        this.addSlot(new Slot(container, 3, 150, 21) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return TartarusPistonEngineBlockEntity.isCryoItem(stack);
            }
        });

        // Player Inventory (3 rows x 9 columns)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar (9 columns)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.blockEntity == null || !stillValid(player)) return false;
        if (id == 0) {
            this.blockEntity.cycleDriveMode();
            return true;
        }
        return false;
    }

    public boolean isBurning() { return this.data.get(0) > 0; }
    public int getCurrentRfPerTick() { return this.data.get(2); }
    public long getEnergy() { return ((long) this.data.get(3) << 16) | (this.data.get(4) & 0xFFFFL); }
    public long getMaxEnergy() { return ((long) this.data.get(5) << 16) | (this.data.get(6) & 0xFFFFL); }
    public int getFluidAmount() { return this.data.get(7); }
    public int getMaxFluid() { return this.data.get(8); }
    public int getCryoAmount() { return this.data.get(9); }
    public int getCylinderTemp() { return this.data.get(10); }
    public int getRpm() { return this.data.get(11); }
    public int getDriveMode() { return this.data.get(12); }

    public int getEnergyScaled(int pixels) {
        long cap = getMaxEnergy();
        if (cap <= 0) cap = TartarusPistonEngineBlockEntity.MAX_ENERGY;
        return (int) (getEnergy() * pixels / cap);
    }

    public int getFluidScaled(int pixels) {
        int max = getMaxFluid();
        if (max <= 0) max = TartarusPistonEngineBlockEntity.MAX_FLUID;
        return getFluidAmount() * pixels / max;
    }

    public int getCryoScaled(int pixels) {
        return getCryoAmount() * pixels / TartarusPistonEngineBlockEntity.MAX_CRYO;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (invSlot < TartarusPistonEngineBlockEntity.INVENTORY_SIZE) {
                if (!this.moveItemStackTo(originalStack, TartarusPistonEngineBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (TartarusPistonEngineBlockEntity.isCryoItem(originalStack)) {
                    if (!this.moveItemStackTo(originalStack, 3, 4, false)) return ItemStack.EMPTY;
                } else if (!this.moveItemStackTo(originalStack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return newStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}
