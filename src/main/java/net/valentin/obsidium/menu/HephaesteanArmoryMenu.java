package net.valentin.obsidium.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.HephaesteanPaxelItem;

public class HephaesteanArmoryMenu extends AbstractContainerMenu {

    private final Container container;

    public HephaesteanArmoryMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(27));
    }

    public HephaesteanArmoryMenu(int syncId, Inventory playerInventory, Container container) {
        super(ObsidiumMod.HEPHAESTEAN_ARMORY_MENU, syncId);
        this.container = container;
        checkContainerSize(container, 27);
        container.startOpen(playerInventory.player);

        // --- Left Column: 4 Dedicated Armor Slots ---
        // Slot 0: Helmet (x: 12, y: 18)
        this.addSlot(new Slot(container, 0, 12, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ArmorItem a && a.getType() == ArmorItem.Type.HELMET;
            }
        });
        // Slot 1: Chestplate (x: 12, y: 36)
        this.addSlot(new Slot(container, 1, 12, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ArmorItem a && a.getType() == ArmorItem.Type.CHESTPLATE;
            }
        });
        // Slot 2: Leggings (x: 12, y: 54)
        this.addSlot(new Slot(container, 2, 12, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ArmorItem a && a.getType() == ArmorItem.Type.LEGGINGS;
            }
        });
        // Slot 3: Boots (x: 12, y: 72)
        this.addSlot(new Slot(container, 3, 12, 72) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ArmorItem a && a.getType() == ArmorItem.Type.BOOTS;
            }
        });

        // --- Center Column: 5 Dedicated Weapon & Tool Slots ---
        // Slot 4: Sword / Weapon (x: 40, y: 18)
        this.addSlot(new Slot(container, 4, 40, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof SwordItem || stack.getItem() instanceof HephaesteanPaxelItem;
            }
        });
        // Slot 5: Pickaxe (x: 40, y: 36)
        this.addSlot(new Slot(container, 5, 40, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof PickaxeItem || stack.getItem() instanceof HephaesteanPaxelItem;
            }
        });
        // Slot 6: Axe (x: 40, y: 54)
        this.addSlot(new Slot(container, 6, 40, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof AxeItem || stack.getItem() instanceof HephaesteanPaxelItem;
            }
        });
        // Slot 7: Shovel (x: 58, y: 36)
        this.addSlot(new Slot(container, 7, 58, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ShovelItem || stack.getItem() instanceof HephaesteanPaxelItem;
            }
        });
        // Slot 8: Hoe / Paxel (x: 58, y: 54)
        this.addSlot(new Slot(container, 8, 58, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof HoeItem || stack.getItem() instanceof HephaesteanPaxelItem;
            }
        });

        // --- Right 18 Slots (6x3 grid for tools, accessories, and spare gear) ---
        int slotIndex = 9;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 6; col++) {
                this.addSlot(new Slot(container, slotIndex++, 84 + col * 18, 18 + row * 18));
            }
        }

        // --- Player Inventory (3 rows of 9) ---
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 18 + col * 18, 98 + row * 18));
            }
        }

        // --- Player Hotbar (9 slots) ---
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 18 + col * 18, 156));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 27) {
                if (!this.moveItemStackTo(itemstack1, 27, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, 27, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }
}
