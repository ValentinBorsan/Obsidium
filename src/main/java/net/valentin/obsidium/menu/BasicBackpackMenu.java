package net.valentin.obsidium.menu;

import net.minecraft.core.NonNullList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.AdvancedBackpackItem;
import net.valentin.obsidium.item.BasicBackpackItem;
import net.minecraft.world.InteractionHand;

public class BasicBackpackMenu extends AbstractContainerMenu {
    private final SimpleContainer backpackContainer;
    private final InteractionHand hand;
    private final Player player;
    private final int heldSlotIndex;

    public BasicBackpackMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, InteractionHand.MAIN_HAND);
    }

    public BasicBackpackMenu(int syncId, Inventory playerInventory, InteractionHand hand) {
        super(ObsidiumMod.BASIC_BACKPACK_MENU, syncId);
        this.hand = hand;
        this.player = playerInventory.player;
        this.heldSlotIndex = (hand == InteractionHand.MAIN_HAND) ? playerInventory.selected : 40;

        this.backpackContainer = new SimpleContainer(BasicBackpackItem.CONTAINER_SIZE);

        if (!player.level().isClientSide()) {
            ItemStack backpackStack = getBackpackStack();
            if (backpackStack.getItem() instanceof BasicBackpackItem) {
                NonNullList<ItemStack> loaded = BasicBackpackItem.loadInventory(backpackStack, player.level().registryAccess());
                for (int i = 0; i < loaded.size(); i++) {
                    this.backpackContainer.setItem(i, loaded.get(i));
                }
            }
        }

        this.backpackContainer.addListener(this::onBackpackInventoryChanged);

        // Backpack slots: 3 rows x 9 columns
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int index = col + row * 9;
                this.addSlot(new Slot(this.backpackContainer, index, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return !(stack.getItem() instanceof BasicBackpackItem || stack.getItem() instanceof AdvancedBackpackItem);
                    }
                });
            }
        }

        // Player Inventory: 3 rows x 9 columns
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int slotIndex = col + row * 9 + 9;
                this.addSlot(new Slot(playerInventory, slotIndex, 8 + col * 18, 84 + row * 18) {
                    @Override
                    public boolean mayPickup(Player player) {
                        return slotIndex != heldSlotIndex && super.mayPickup(player);
                    }
                });
            }
        }

        // Player Hotbar: 9 slots
        for (int col = 0; col < 9; ++col) {
            int slotIndex = col;
            this.addSlot(new Slot(playerInventory, slotIndex, 8 + col * 18, 142) {
                @Override
                public boolean mayPickup(Player player) {
                    return slotIndex != heldSlotIndex && super.mayPickup(player);
                }
            });
        }
    }

    private ItemStack getBackpackStack() {
        return player.getItemInHand(hand);
    }

    private void onBackpackInventoryChanged(Container container) {
        if (!player.level().isClientSide()) {
            ItemStack backpackStack = getBackpackStack();
            if (backpackStack.getItem() instanceof BasicBackpackItem) {
                NonNullList<ItemStack> items = NonNullList.withSize(BasicBackpackItem.CONTAINER_SIZE, ItemStack.EMPTY);
                for (int i = 0; i < BasicBackpackItem.CONTAINER_SIZE; i++) {
                    items.set(i, container.getItem(i));
                }
                BasicBackpackItem.saveInventory(backpackStack, items, player.level().registryAccess());
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        ItemStack stack = player.getItemInHand(hand);
        return !player.isRemoved() && stack.getItem() instanceof BasicBackpackItem;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        onBackpackInventoryChanged(this.backpackContainer);
        if (!player.level().isClientSide()) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 0.8F, 0.9F);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();

            // From backpack storage (0..26)
            if (index < BasicBackpackItem.CONTAINER_SIZE) {
                if (!this.moveItemStackTo(itemStack2, BasicBackpackItem.CONTAINER_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else { // From player inventory
                if (itemStack2.getItem() instanceof BasicBackpackItem || itemStack2.getItem() instanceof AdvancedBackpackItem) {
                    return ItemStack.EMPTY;
                }
                if (!this.moveItemStackTo(itemStack2, 0, BasicBackpackItem.CONTAINER_SIZE, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemStack2.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemStack;
    }
}

