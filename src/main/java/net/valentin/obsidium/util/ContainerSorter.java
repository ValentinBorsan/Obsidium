package net.valentin.obsidium.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.block.VolcaniteModularChestMenu;
import net.valentin.obsidium.block.VolcaniteStorageTerminalMenu;
import net.valentin.obsidium.menu.AdvancedBackpackMenu;
import net.valentin.obsidium.menu.BasicBackpackMenu;

import java.util.ArrayList;
import java.util.List;

public final class ContainerSorter {
    private ContainerSorter() {}

    public static void sortOpenContainer(ServerPlayer player, boolean descending) {
        if (player == null) return;
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null) return;

        // 1. Special handling for VolcaniteModularChestMenu (uses multi-page sort)
        if (menu instanceof VolcaniteModularChestMenu modularMenu) {
            modularMenu.clickMenuButton(player, descending
                    ? VolcaniteModularChestMenu.BTN_SORT_QUANTITY_DESC
                    : VolcaniteModularChestMenu.BTN_SORT_QUANTITY_ASC);
            return;
        }

        // 2. Special handling for VolcaniteStorageTerminalMenu (cycles network sort)
        if (menu instanceof VolcaniteStorageTerminalMenu terminalMenu) {
            terminalMenu.clickMenuButton(player, 102);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5F, 1.3F);
            return;
        }

        // Only allow generic slot sorting on dedicated storage menus or player inventory (never machines or furnaces)
        boolean isStorageMenu = menu instanceof ChestMenu
                || menu instanceof InventoryMenu
                || menu instanceof BasicBackpackMenu
                || menu instanceof AdvancedBackpackMenu;
        if (!isStorageMenu) return;

        // 3. Collect all sortable slots from the open container (or player main inventory 9..35 if in InventoryMenu)
        List<Slot> targetSlots = new ArrayList<>();
        if (menu instanceof InventoryMenu) {
            for (Slot slot : menu.slots) {
                if (slot.container instanceof Inventory && slot.getContainerSlot() >= 9 && slot.getContainerSlot() < 36) {
                    targetSlots.add(slot);
                }
            }
        } else {
            for (Slot slot : menu.slots) {
                if (!(slot.container instanceof Inventory) && slot.isActive()) {
                    // Skip output-only or special upgrade/module slots (maxStackSize == 1 when not a normal chest slot)
                    if (slot.getMaxStackSize() > 1) {
                        targetSlots.add(slot);
                    }
                }
            }
        }

        if (targetSlots.size() <= 1) return;

        // 4. Extract all items from target slots & merge identical stacks up to maxStackSize
        List<ItemStack> merged = new ArrayList<>();
        for (Slot slot : targetSlots) {
            ItemStack s = slot.getItem();
            if (s.isEmpty()) continue;
            if (!slot.mayPickup(player)) continue;

            ItemStack remaining = s.copy();
            slot.set(ItemStack.EMPTY);

            for (ItemStack existing : merged) {
                if (ItemStack.isSameItemSameComponents(existing, remaining) && existing.getCount() < existing.getMaxStackSize()) {
                    int space = existing.getMaxStackSize() - existing.getCount();
                    int transfer = Math.min(space, remaining.getCount());
                    existing.grow(transfer);
                    remaining.shrink(transfer);
                    if (remaining.isEmpty()) break;
                }
            }
            while (!remaining.isEmpty()) {
                int splitCount = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                merged.add(remaining.split(splitCount));
            }
        }

        if (merged.isEmpty()) return;

        // 5. Sort by total quantity of that item type across the container (then stack count, then name A-Z)
        java.util.Map<net.minecraft.world.item.Item, Integer> totalCounts = new java.util.HashMap<>();
        for (ItemStack st : merged) {
            totalCounts.merge(st.getItem(), st.getCount(), Integer::sum);
        }

        merged.sort((a, b) -> {
            int totA = totalCounts.getOrDefault(a.getItem(), a.getCount());
            int totB = totalCounts.getOrDefault(b.getItem(), b.getCount());
            int cmpTot = descending ? Integer.compare(totB, totA) : Integer.compare(totA, totB);
            if (cmpTot != 0) return cmpTot;

            int cmpStack = descending ? Integer.compare(b.getCount(), a.getCount()) : Integer.compare(a.getCount(), b.getCount());
            if (cmpStack != 0) return cmpStack;

            return a.getHoverName().getString().compareToIgnoreCase(b.getHoverName().getString());
        });

        // 6. Write sorted items back into target slots
        int writeIdx = 0;
        List<ItemStack> unplaced = new ArrayList<>();
        for (ItemStack st : merged) {
            boolean placed = false;
            while (writeIdx < targetSlots.size()) {
                Slot target = targetSlots.get(writeIdx++);
                if (target.getItem().isEmpty() && target.mayPlace(st)) {
                    target.set(st);
                    target.setChanged();
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                unplaced.add(st);
            }
        }

        // Safeguard: Never lose items if unplaced
        for (ItemStack leftover : unplaced) {
            if (!leftover.isEmpty()) {
                player.getInventory().placeItemBackInInventory(leftover);
                if (!leftover.isEmpty()) {
                    player.drop(leftover, false);
                }
            }
        }

        Container firstContainer = targetSlots.get(0).container;
        firstContainer.setChanged();
        menu.broadcastChanges();

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.7F, descending ? 1.25F : 1.05F);
        String dirLabel = descending ? "§aQuantity (High ➔ Low)" : "§eQuantity (Low ➔ High)";
        player.displayClientMessage(Component.literal("§6[Sort] §7Arranged container by " + dirLabel), true);
    }
}

