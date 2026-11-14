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

public class SupremeVolcanicGeneratorMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    @Nullable
    private final SupremeVolcanicGeneratorBlockEntity blockEntity;

    public SupremeVolcanicGeneratorMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(3), new SimpleContainerData(9), null);
    }

    public SupremeVolcanicGeneratorMenu(int syncId, Inventory playerInventory, Container container, ContainerData data, @Nullable SupremeVolcanicGeneratorBlockEntity blockEntity) {
        super(ObsidiumMod.SUPREME_VOLCANIC_GENERATOR_MENU, syncId);
        checkContainerSize(container, 3);
        checkContainerDataCount(data, 9);
        this.container = container;
        this.data = data;
        this.blockEntity = blockEntity;

        // Slot 0: Pyroclastic Distillate Bucket / Flask input (16, 22)
        this.addSlot(new Slot(container, 0, 16, 22) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return SupremeVolcanicGeneratorBlockEntity.isDistillateContainer(stack);
            }
        });

        // Slot 1: Empty Bucket / Bottle output (16, 52)
        this.addSlot(new Slot(container, 1, 16, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Slot 2: Astral Volcanic Catalyst (64, 22)
        this.addSlot(new Slot(container, 2, 64, 22) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return SupremeVolcanicGeneratorBlockEntity.getCatalystBurstRate(stack) > 0;
            }
        });

        // Player Inventory
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }

        this.addDataSlots(data);
    }

    public int getDistillateAmount() {
        return this.data.get(0);
    }

    public int getPressure() {
        return this.data.get(1);
    }

    public int getPhase() {
        return this.data.get(2);
    }

    public int getEruptionTicks() {
        return this.data.get(3);
    }

    public int getMaxEruptionTicks() {
        return Math.max(1, this.data.get(4));
    }

    public int getCooldownTicks() {
        return this.data.get(5);
    }

    public int getCurrentBurstRate() {
        return this.data.get(6);
    }

    public int getEnergyAmount() {
        return (this.data.get(8) << 16) | (this.data.get(7) & 0xFFFF);
    }

    public ItemStack getCatalystItem() {
        return this.container.getItem(2);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && this.blockEntity != null) {
            return this.blockEntity.triggerManualEruption();
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemStack = slotStack.copy();

            if (index < 3) {
                if (!this.moveItemStackTo(slotStack, 3, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (SupremeVolcanicGeneratorBlockEntity.isDistillateContainer(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (SupremeVolcanicGeneratorBlockEntity.getCatalystBurstRate(slotStack) > 0) {
                    if (!this.moveItemStackTo(slotStack, 2, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}
