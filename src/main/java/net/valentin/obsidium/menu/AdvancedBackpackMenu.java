package net.valentin.obsidium.menu;

import net.minecraft.core.NonNullList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.*;
import team.reborn.energy.api.base.SimpleEnergyItem;

public class AdvancedBackpackMenu extends AbstractContainerMenu {
    private final SimpleContainer backpackContainer;
    private final InteractionHand hand;
    private final Player player;
    private final int heldSlotIndex;
    private final ItemStack openedBackpack;
    private final ContainerData data;

    public AdvancedBackpackMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, InteractionHand.MAIN_HAND);
    }

    public AdvancedBackpackMenu(int syncId, Inventory playerInventory, InteractionHand hand) {
        super(ObsidiumMod.ADVANCED_BACKPACK_MENU, syncId);
        this.hand = hand;
        this.player = playerInventory.player;
        this.heldSlotIndex = (hand == InteractionHand.MAIN_HAND) ? playerInventory.selected : 40;
        this.openedBackpack = playerInventory.getItem(this.heldSlotIndex);

        this.backpackContainer = new SimpleContainer(AdvancedBackpackItem.TOTAL_CONTAINER_SIZE);

        if (!player.level().isClientSide()) {
            ItemStack backpackStack = getBackpackStack();
            if (backpackStack.getItem() instanceof AdvancedBackpackItem) {
                NonNullList<ItemStack> loaded = AdvancedBackpackItem.loadInventory(backpackStack, player.level().registryAccess());
                for (int i = 0; i < loaded.size(); i++) {
                    this.backpackContainer.setItem(i, loaded.get(i));
                }
            }
        }

        this.backpackContainer.addListener(this::onBackpackInventoryChanged);

        // ContainerData for syncing 64-bit RF stored & max capacity (4 shorts)
        this.data = new ContainerData() {
            private int s0, s1, s2, s3;

            @Override
            public int get(int index) {
                if (!player.level().isClientSide()) {
                    long stored = getStoredEnergy();
                    long max = AdvancedBackpackItem.MAX_ENERGY;
                    return switch (index) {
                        case 0 -> (int) ((stored >> 16) & 0xFFFF);
                        case 1 -> (int) (stored & 0xFFFF);
                        case 2 -> (int) ((max >> 16) & 0xFFFF);
                        case 3 -> (int) (max & 0xFFFF);
                        default -> 0;
                    };
                }
                return switch (index) {
                    case 0 -> s0;
                    case 1 -> s1;
                    case 2 -> s2;
                    case 3 -> s3;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> s0 = value;
                    case 1 -> s1 = value;
                    case 2 -> s2 = value;
                    case 3 -> s3 = value;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
        this.addDataSlots(this.data);

        // 1. Main Storage Slots: 6 rows x 9 columns (0..53)
        for (int row = 0; row < 6; ++row) {
            for (int col = 0; col < 9; ++col) {
                int index = col + row * 9;
                this.addSlot(new Slot(this.backpackContainer, index, 65 + col * 18, 21 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return !(stack.getItem() instanceof BasicBackpackItem || stack.getItem() instanceof AdvancedBackpackItem);
                    }
                });
            }
        }

        // 2. Armor Slots (54..57)
        // Helmet (54)
        this.addSlot(new Slot(this.backpackContainer, AdvancedBackpackItem.HELMET_SLOT, 13, 21) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isArmorForSlot(stack, EquipmentSlot.HEAD);
            }
            @Override
            public int getMaxStackSize() { return 1; }
        });
        // Chestplate (55)
        this.addSlot(new Slot(this.backpackContainer, AdvancedBackpackItem.CHEST_SLOT, 13, 39) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isArmorForSlot(stack, EquipmentSlot.CHEST);
            }
            @Override
            public int getMaxStackSize() { return 1; }
        });
        // Leggings (56)
        this.addSlot(new Slot(this.backpackContainer, AdvancedBackpackItem.LEGS_SLOT, 13, 57) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isArmorForSlot(stack, EquipmentSlot.LEGS);
            }
            @Override
            public int getMaxStackSize() { return 1; }
        });
        // Boots (57)
        this.addSlot(new Slot(this.backpackContainer, AdvancedBackpackItem.BOOTS_SLOT, 13, 75) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isArmorForSlot(stack, EquipmentSlot.FEET);
            }
            @Override
            public int getMaxStackSize() { return 1; }
        });

        // 3. Tool Slots (58..62)
        int[] toolY = {21, 39, 57, 75, 93};
        for (int i = 0; i < 5; i++) {
            int slotIdx = AdvancedBackpackItem.TOOL_PICKAXE + i;
            this.addSlot(new Slot(this.backpackContainer, slotIdx, 35, toolY[i]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return isToolOrWeapon(stack);
                }
                @Override
                public int getMaxStackSize() { return 1; }
            });
        }

        // 4. Battery / Recharge Slot (63)
        this.addSlot(new Slot(this.backpackContainer, AdvancedBackpackItem.BATTERY_SLOT, 13, 93) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isBatteryItem(stack);
            }
            @Override
            public int getMaxStackSize() { return 1; }
        });

        // 5. Player Inventory: 3 rows x 9 columns
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int slotIndex = col + row * 9 + 9;
                this.addSlot(new Slot(playerInventory, slotIndex, 65 + col * 18, 139 + row * 18) {
                    @Override
                    public boolean mayPickup(Player player) {
                        return slotIndex != heldSlotIndex && super.mayPickup(player);
                    }
                });
            }
        }

        // 6. Player Hotbar: 9 slots
        for (int col = 0; col < 9; ++col) {
            int slotIndex = col;
            this.addSlot(new Slot(playerInventory, slotIndex, 65 + col * 18, 197) {
                @Override
                public boolean mayPickup(Player player) {
                    return slotIndex != heldSlotIndex && super.mayPickup(player);
                }
            });
        }
    }

    public static boolean isArmorForSlot(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ArmorItem armor) {
            return armor.getEquipmentSlot() == slot;
        }
        if (slot == EquipmentSlot.CHEST && stack.is(Items.ELYTRA)) {
            return true;
        }
        return false;
    }

    public static boolean isToolOrWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item instanceof TieredItem
                || item instanceof SwordItem
                || item instanceof DiggerItem
                || item instanceof ProjectileWeaponItem
                || item instanceof ShearsItem
                || item instanceof FishingRodItem
                || item instanceof TridentItem
                || item instanceof VolcaniteWrenchItem
                || item instanceof PyrogenicBloomInfuserItem;
    }

    public static boolean isBatteryItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof SimpleEnergyItem
                || stack.getItem() instanceof VolcanitePortableBatteryItem;
    }

    public long getEnergyAmount() {
        return (((long) this.data.get(0) & 0xFFFF) << 16) | ((long) this.data.get(1) & 0xFFFF);
    }

    public long getMaxEnergy() {
        return (((long) this.data.get(2) & 0xFFFF) << 16) | ((long) this.data.get(3) & 0xFFFF);
    }

    public ItemStack getBackpackStack() {
        return openedBackpack;
    }

    public long getStoredEnergy() {
        ItemStack stack = getBackpackStack();
        if (stack.getItem() instanceof AdvancedBackpackItem item) {
            return item.getStoredEnergy(stack);
        }
        return 0;
    }

    public void setStoredEnergy(long amount) {
        ItemStack stack = getBackpackStack();
        if (stack.getItem() instanceof AdvancedBackpackItem item) {
            item.setStoredEnergy(stack, Math.max(0, Math.min(AdvancedBackpackItem.MAX_ENERGY, amount)));
        }
    }

    private void onBackpackInventoryChanged(Container container) {
        if (!player.level().isClientSide()) {
            ItemStack backpackStack = getBackpackStack();
            if (backpackStack.getItem() instanceof AdvancedBackpackItem) {
                NonNullList<ItemStack> items = NonNullList.withSize(AdvancedBackpackItem.TOTAL_CONTAINER_SIZE, ItemStack.EMPTY);
                for (int i = 0; i < AdvancedBackpackItem.TOTAL_CONTAINER_SIZE; i++) {
                    items.set(i, container.getItem(i));
                }
                AdvancedBackpackItem.saveInventory(backpackStack, items, player.level().registryAccess());
            }
        }
    }

    @Override
    public void broadcastChanges() {
        if (!player.level().isClientSide() && !stillValid(player)) return;
        super.broadcastChanges();

        if (!player.level().isClientSide()) {
            long stored = getStoredEnergy();

            // 1. Passive UI energy drain: 2 RF every 20 ticks
            if (player.level().getGameTime() % 20 == 0 && stored > 0) {
                stored = Math.max(0, stored - 2);
                setStoredEnergy(stored);
            }

            // 2. Battery Slot charging: draw RF from portable battery placed in battery slot
            ItemStack batteryStack = backpackContainer.getItem(AdvancedBackpackItem.BATTERY_SLOT);
            if (!batteryStack.isEmpty() && batteryStack.getItem() instanceof VolcanitePortableBatteryItem batteryItem) {
                long needed = AdvancedBackpackItem.MAX_ENERGY - stored;
                long batteryStored = batteryItem.getStoredEnergy(batteryStack);
                if (needed > 0 && batteryStored > 0) {
                    long transfer = Math.min(needed, Math.min(batteryStored, AdvancedBackpackItem.MAX_INPUT));
                    stored += transfer;
                    setStoredEnergy(stored);
                    batteryItem.setStoredEnergy(batteryStack, batteryStored - transfer);
                    backpackContainer.setChanged();
                }
            }

            // 3. Recharge tools & armor inside the backpack if energy > 1,000 RF
            if (stored > 1_000) {
                for (int i = AdvancedBackpackItem.HELMET_SLOT; i <= AdvancedBackpackItem.TOOL_UTILITY; i++) {
                    ItemStack target = backpackContainer.getItem(i);
                    if (!target.isEmpty() && target.getItem() instanceof SimpleEnergyItem energyItem) {
                        long cap = energyItem.getEnergyCapacity(target);
                        long cur = energyItem.getStoredEnergy(target);
                        if (cur < cap) {
                            long toMove = Math.min(500, Math.min(stored - 1_000, cap - cur));
                            if (toMove > 0) {
                                energyItem.setStoredEnergy(target, cur + toMove);
                                stored -= toMove;
                                setStoredEnergy(stored);
                                backpackContainer.setChanged();
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return player == this.player && !player.isRemoved()
                && player.getInventory().getItem(heldSlotIndex) == openedBackpack
                && player.getItemInHand(hand) == openedBackpack
                && openedBackpack.getItem() instanceof AdvancedBackpackItem;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        onBackpackInventoryChanged(this.backpackContainer);
        if (!player.level().isClientSide()) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6F, 1.8F);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!player.level().isClientSide() && !stillValid(player)) return ItemStack.EMPTY;
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();

            // From inside backpack (0..63)
            if (index < AdvancedBackpackItem.TOTAL_CONTAINER_SIZE) {
                if (!this.moveItemStackTo(itemStack2, AdvancedBackpackItem.TOTAL_CONTAINER_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else { // From player inventory (64..99)
                if (itemStack2.getItem() instanceof BasicBackpackItem || itemStack2.getItem() instanceof AdvancedBackpackItem) {
                    return ItemStack.EMPTY;
                }

                boolean moved = false;

                // 1. Try Armor slots
                if (isArmorForSlot(itemStack2, EquipmentSlot.HEAD)) {
                    moved = this.moveItemStackTo(itemStack2, AdvancedBackpackItem.HELMET_SLOT, AdvancedBackpackItem.HELMET_SLOT + 1, false);
                } else if (isArmorForSlot(itemStack2, EquipmentSlot.CHEST)) {
                    moved = this.moveItemStackTo(itemStack2, AdvancedBackpackItem.CHEST_SLOT, AdvancedBackpackItem.CHEST_SLOT + 1, false);
                } else if (isArmorForSlot(itemStack2, EquipmentSlot.LEGS)) {
                    moved = this.moveItemStackTo(itemStack2, AdvancedBackpackItem.LEGS_SLOT, AdvancedBackpackItem.LEGS_SLOT + 1, false);
                } else if (isArmorForSlot(itemStack2, EquipmentSlot.FEET)) {
                    moved = this.moveItemStackTo(itemStack2, AdvancedBackpackItem.BOOTS_SLOT, AdvancedBackpackItem.BOOTS_SLOT + 1, false);
                }

                // 2. Try Battery slot
                if (!moved && isBatteryItem(itemStack2)) {
                    moved = this.moveItemStackTo(itemStack2, AdvancedBackpackItem.BATTERY_SLOT, AdvancedBackpackItem.BATTERY_SLOT + 1, false);
                }

                // 3. Try Tool slots
                if (!moved && isToolOrWeapon(itemStack2)) {
                    moved = this.moveItemStackTo(itemStack2, AdvancedBackpackItem.TOOL_PICKAXE, AdvancedBackpackItem.TOOL_UTILITY + 1, false);
                }

                // 4. Default: Try Main Storage (0..53)
                if (!moved) {
                    if (!this.moveItemStackTo(itemStack2, 0, AdvancedBackpackItem.MAIN_STORAGE_SIZE, false)) {
                        return ItemStack.EMPTY;
                    }
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

