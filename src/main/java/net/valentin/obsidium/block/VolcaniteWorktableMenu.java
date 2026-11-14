package net.valentin.obsidium.block;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.ObsidiumMod;

public class VolcaniteWorktableMenu extends AbstractContainerMenu {
    private final Container container;
    private final Container chestContainer;
    public final DataSlot hasChest = DataSlot.standalone();
    public final DataSlot autoOutput = DataSlot.standalone();
    public final DataSlot antiMix = DataSlot.standalone();

    public VolcaniteWorktableMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(12), new SimpleContainer(9));
    }

    public VolcaniteWorktableMenu(int syncId, Inventory playerInventory, Container container, Container chestContainer) {
        super(ObsidiumMod.VOLCANITE_WORKTABLE_MENU, syncId);
        this.container = container;
        this.chestContainer = chestContainer;
        checkContainerSize(container, 12);
        checkContainerSize(chestContainer, 9);
        container.startOpen(playerInventory.player);
        chestContainer.startOpen(playerInventory.player);
        
        this.addDataSlot(hasChest);
        this.addDataSlot(autoOutput);
        this.addDataSlot(antiMix);
        if (container instanceof VolcaniteWorktableBlockEntity table) {
            this.autoOutput.set(table.isAutoOutput() ? 1 : 0);
            this.antiMix.set(table.isAntiMix() ? 1 : 0);
        } else {
            this.autoOutput.set(1);
            this.antiMix.set(1);
        }

        // 3x3 Crafting Grid (Slots 0-8)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 3; ++col) {
                this.addSlot(new Slot(container, col + row * 3, 30 + col * 18, 17 + row * 18));
            }
        }
        
        // Lava Bucket Slot (9)
        this.addSlot(new Slot(container, 9, 9, 35));
        
        // Output Slot (10)
        this.addSlot(new Slot(container, 10, 124, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public ItemStack remove(int amount) {
                if (container instanceof VolcaniteWorktableBlockEntity table) {
                    table.isPlayerMenuExtraction = true;
                    try {
                        return super.remove(amount);
                    } finally {
                        table.isPlayerMenuExtraction = false;
                    }
                }
                return super.remove(amount);
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                if (container instanceof VolcaniteWorktableBlockEntity table) {
                    table.onTakeCraftedItem(player, stack);
                }
                super.onTake(player, stack);
            }
        });

        // Blueprint Slot (11)
        this.addSlot(new Slot(container, 11, 9, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ObsidiumMod.VOLCANITE_BLUEPRINT);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        
        // Chest Slots (9 slots, visually below or to the side, starting at x=180)
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(chestContainer, i, 180 + (i % 3) * 18, 17 + (i / 3) * 18) {
                @Override public boolean isActive() { return hasChest.get() == 1; }
            });
        }

        // Player Inventory (Slots 20-46)
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        // Hotbar (Slots 47-55)
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }

        if (container instanceof VolcaniteWorktableBlockEntity table && table.getLevel() != null) {
            table.updateRecipeOutput(table.getLevel());
        }
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (this.container instanceof VolcaniteWorktableBlockEntity table && table.getLevel() != null) {
            table.updateRecipeOutput(table.getLevel());
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            if (this.container instanceof VolcaniteWorktableBlockEntity table) {
                table.toggleAutoOutput();
                this.autoOutput.set(table.isAutoOutput() ? 1 : 0);
                return true;
            }
        } else if (id == 1) {
            if (this.container instanceof VolcaniteWorktableBlockEntity table) {
                table.toggleAntiMix();
                this.antiMix.set(table.isAntiMix() ? 1 : 0);
                return true;
            }
        } else if (id == 2) {
            if (this.container instanceof VolcaniteWorktableBlockEntity table && table.getLevel() != null) {
                ItemStack bp = table.getItem(11);
                ItemStack out = table.getItem(10);
                if (bp.is(ObsidiumMod.VOLCANITE_BLUEPRINT) && !out.isEmpty()) {
                    net.minecraft.core.NonNullList<ItemStack> grid = net.minecraft.core.NonNullList.withSize(9, ItemStack.EMPTY);
                    for (int i = 0; i < 9; i++) {
                        ItemStack inGrid = table.getItem(i);
                        grid.set(i, inGrid.isEmpty() ? ItemStack.EMPTY : inGrid.copyWithCount(1));
                    }
                    net.valentin.obsidium.item.VolcaniteBlueprintItem.setRecipe(bp, grid, out.copy(), table.getLevel().registryAccess());
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("§6[Blueprint] §aSaved recipe: §f" + out.getHoverName().getString()), true);
                    return true;
                }
            }
        }
        return super.clickMenuButton(player, id);
    }

    public Container getChestContainer() {
        return this.chestContainer;
    }

    @Override
    public boolean stillValid(Player player) { return this.container.stillValid(player); }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
        this.chestContainer.stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index == 10) { // Output slot
                while (slot.hasItem()) {
                    ItemStack outStack = slot.getItem();
                    ItemStack crafted = outStack.copy();

                    if (!this.moveItemStackTo(outStack, 21, this.slots.size(), true)) {
                        break;
                    }

                    slot.onQuickCraft(outStack, crafted);
                    slot.onTake(player, crafted);

                    if (this.container instanceof VolcaniteWorktableBlockEntity table && table.getLevel() != null) {
                        table.updateRecipeOutput(table.getLevel());
                    }

                    if (itemstack.isEmpty()) {
                        itemstack = crafted;
                    }

                    ItemStack next = slot.getItem();
                    if (next.isEmpty() || !ItemStack.isSameItemSameComponents(next, crafted)) {
                        break;
                    }
                }
                return itemstack;
            } else if (index < 12) { // Crafting grid (0-8), lava (9), or blueprint (11)
                if (!this.moveItemStackTo(itemstack1, 21, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 12 && index < 21) { // Chest slots (12-20)
                if (!this.moveItemStackTo(itemstack1, 21, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else { // Player inventory (21-56)
                if (itemstack1.is(ObsidiumMod.VOLCANITE_BLUEPRINT)) {
                    if (!this.moveItemStackTo(itemstack1, 11, 12, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (itemstack1.is(net.minecraft.world.item.Items.LAVA_BUCKET)) {
                    if (!this.moveItemStackTo(itemstack1, 9, 10, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemstack1, 0, 9, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }
}
