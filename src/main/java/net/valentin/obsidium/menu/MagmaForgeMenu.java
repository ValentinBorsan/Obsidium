package net.valentin.obsidium.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.MagmaForgeCoreBlockEntity;
import net.valentin.obsidium.item.VolcaniteBottleItem;
import org.jetbrains.annotations.Nullable;

public class MagmaForgeMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    @Nullable
    private final MagmaForgeCoreBlockEntity blockEntity;

    public MagmaForgeMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(6), new SimpleContainerData(8), null);
    }

    public MagmaForgeMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        this(containerId, playerInventory, container, data, null);
    }

    public MagmaForgeMenu(int containerId, Inventory playerInventory, Container container, ContainerData data, @Nullable MagmaForgeCoreBlockEntity blockEntity) {
        super(ObsidiumMod.MAGMA_FORGE_MENU, containerId);
        this.container = container;
        this.data = data;
        this.blockEntity = blockEntity;
        checkContainerSize(container, 6);
        checkContainerDataCount(data, 8);
        container.startOpen(playerInventory.player);
        this.addDataSlots(data);

        // Slot 0: Input 1 (Left)
        this.addSlot(new Slot(container, 0, 74, 32));
        // Slot 1: Input 2 (Center)
        this.addSlot(new Slot(container, 1, 104, 28));
        // Slot 2: Input 3 (Right)
        this.addSlot(new Slot(container, 2, 134, 32));

        // Slot 3: Lava Bucket / Volcanite Bottle Fuel Slot (Below Lava Tank)
        this.addSlot(new Slot(container, 3, 14, 110) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.LAVA_BUCKET) || stack.getItem() instanceof VolcaniteBottleItem;
            }
        });

        // Slot 4: Main Output
        this.addSlot(new Slot(container, 4, 169, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Slot 5: Masterwork Bonus Output
        this.addSlot(new Slot(container, 5, 169, 88) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Player Inventory (3x9)
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 60 + j * 18, 152 + i * 18));
            }
        }
        // Hotbar (9)
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 60 + i * 18, 210));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.blockEntity != null) {
            if (id == 0) {
                this.blockEntity.handleHammerStrike(player, false);
                return true;
            } else if (id == 1) {
                this.blockEntity.handleHammerStrike(player, true);
                return true;
            } else if (id >= 100 && id < 100 + MagmaForgeCoreBlockEntity.RECIPES.size()) {
                this.blockEntity.autoFillRecipe(player, id - 100);
                return true;
            }
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack inSlot = slot.getItem();
            itemStack = inSlot.copy();

            if (index < 6) {
                // From Forge to Player Inventory
                if (!this.moveItemStackTo(inSlot, 6, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From Player Inventory to Forge
                if (inSlot.is(Items.LAVA_BUCKET) || inSlot.getItem() instanceof VolcaniteBottleItem) {
                    if (!this.moveItemStackTo(inSlot, 3, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(inSlot, 0, 3, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (inSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    public long getEnergy() {
        return (this.data.get(0) & 0xFFFFL) | ((this.data.get(1) & 0xFFFFL) << 16);
    }

    public long getFluidMb() {
        return (this.data.get(2) & 0xFFFFL) | ((this.data.get(3) & 0xFFFFL) << 16);
    }

    public int getCraftProgress() {
        return this.data.get(4);
    }

    public int getMaxProgress() {
        int max = this.data.get(5);
        return max > 0 ? max : MagmaForgeCoreBlockEntity.MAX_PROGRESS;
    }

    public int getPerfectStrikes() {
        return this.data.get(6);
    }

    public int getHeatLevel() {
        return this.data.get(7);
    }

    public Container getContainer() {
        return this.container;
    }
}
