package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.valentin.obsidium.menu.AdvancedBackpackMenu;
import team.reborn.energy.api.base.SimpleEnergyItem;

import java.util.List;

public class AdvancedBackpackItem extends Item implements SimpleEnergyItem {
    public static final int TOTAL_CONTAINER_SIZE = 64;
    public static final int MAIN_STORAGE_SIZE = 54;
    public static final int HELMET_SLOT = 54;
    public static final int CHEST_SLOT = 55;
    public static final int LEGS_SLOT = 56;
    public static final int BOOTS_SLOT = 57;
    public static final int TOOL_PICKAXE = 58;
    public static final int TOOL_AXE = 59;
    public static final int TOOL_SHOVEL = 60;
    public static final int TOOL_SWORD = 61;
    public static final int TOOL_UTILITY = 62;
    public static final int BATTERY_SLOT = 63;

    public static final long MAX_ENERGY = 1_000_000L;
    public static final long MAX_INPUT = 25_000L;
    public static final long MAX_OUTPUT = 25_000L;
    public static final long PASSIVE_RF_DRAIN_INTERVAL = 20L; // every 20 ticks (1s)
    public static final long PASSIVE_RF_DRAIN = 2L;

    public AdvancedBackpackItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant().rarity(net.minecraft.world.item.Rarity.RARE));
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return MAX_ENERGY;
    }

    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return MAX_INPUT;
    }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return MAX_OUTPUT;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        long stored = getStoredEnergy(stack);

        // Quick charge with offhand battery if holding one
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof VolcanitePortableBatteryItem batteryItem && hand == InteractionHand.MAIN_HAND) {
            long batteryStored = batteryItem.getStoredEnergy(offhand);
            long space = MAX_ENERGY - stored;
            if (batteryStored > 0 && space > 0) {
                long transfer = Math.min(space, Math.min(batteryStored, MAX_INPUT * 4));
                setStoredEnergy(stack, stored + transfer);
                batteryItem.setStoredEnergy(offhand, batteryStored - transfer);
                stored += transfer;
                level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.8F);
                if (!level.isClientSide()) {
                    player.displayClientMessage(Component.literal("§a[Advanced Volcanite Backpack] Recharged +" + String.format("%,d", transfer) + " RF from Portable Battery!"), true);
                }
            }
        }

        if (stored <= 0) {
            if (!level.isClientSide()) {
                player.displayClientMessage(
                        Component.literal("§c[Advanced Volcanite Backpack] ⚡ Energy Depleted! (0 / 1,000,000 RF) - Charge with a Portable Battery to open.")
                                .withStyle(ChatFormatting.RED),
                        true
                );
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide()) {
            // Consume 25 RF on initialization
            setStoredEnergy(stack, Math.max(0, stored - 25));

            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 1.8F);

            player.openMenu(new SimpleMenuProvider(
                    (syncId, inv, p) -> new AdvancedBackpackMenu(syncId, inv, hand),
                    stack.getHoverName()
            ));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide() && entity instanceof Player) {
            long stored = getStoredEnergy(stack);

            // 1. Passive RF drain (Quantum containment field)
            if (level.getGameTime() % PASSIVE_RF_DRAIN_INTERVAL == 0 && stored > 0) {
                setStoredEnergy(stack, Math.max(0, stored - PASSIVE_RF_DRAIN));
                stored = getStoredEnergy(stack);
            }

            // 2. Charge from internal battery slot (every 10 ticks)
            if (level.getGameTime() % 10 == 0 && stored < MAX_ENERGY) {
                NonNullList<ItemStack> items = loadInventory(stack, level.registryAccess());
                ItemStack batteryStack = items.get(BATTERY_SLOT);
                if (!batteryStack.isEmpty() && batteryStack.getItem() instanceof VolcanitePortableBatteryItem batteryItem) {
                    long batteryStored = batteryItem.getStoredEnergy(batteryStack);
                    long needed = MAX_ENERGY - stored;
                    if (batteryStored > 0 && needed > 0) {
                        long transfer = Math.min(needed, Math.min(batteryStored, MAX_INPUT));
                        setStoredEnergy(stack, stored + transfer);
                        batteryItem.setStoredEnergy(batteryStack, batteryStored - transfer);
                        items.set(BATTERY_SLOT, batteryStack);
                        saveInventory(stack, items, level.registryAccess());
                    }
                }
            }
        }
    }

    public static NonNullList<ItemStack> loadInventory(ItemStack backpack, HolderLookup.Provider registries) {
        NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_CONTAINER_SIZE, ItemStack.EMPTY);
        CustomData data = backpack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        ContainerHelper.loadAllItems(tag, items, registries);
        return items;
    }

    public static void saveInventory(ItemStack backpack, NonNullList<ItemStack> items, HolderLookup.Provider registries) {
        CustomData.update(DataComponents.CUSTOM_DATA, backpack, tag -> {
            ContainerHelper.saveAllItems(tag, items, registries);
        });
    }

    public static int getOccupiedSlots(ItemStack backpack, HolderLookup.Provider registries) {
        NonNullList<ItemStack> items = loadInventory(backpack, registries);
        int count = 0;
        for (ItemStack item : items) {
            if (!item.isEmpty()) count++;
        }
        return count;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, context, tooltip, isAdvanced);

        long stored = getStoredEnergy(stack);
        ChatFormatting energyColor = stored > 100_000 ? ChatFormatting.AQUA : (stored > 0 ? ChatFormatting.YELLOW : ChatFormatting.RED);
        tooltip.add(Component.literal("⚡ Energy: " + String.format("%,d", stored) + " / " + String.format("%,d", MAX_ENERGY) + " RF").withStyle(energyColor));

        int occupied = 0;
        if (context.registries() != null) {
            occupied = getOccupiedSlots(stack, context.registries());
        }

        tooltip.add(Component.literal("Capacity: 64 Total Slots (54 Storage, 4 Armor, 5 Tools, 1 Battery)").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Occupied Slots: " + occupied + " / 64").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§6★ Supreme Quantum Storage Matrix").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("⚡ Consumes RF to stabilize quantum field. Auto-recharges from Portable Batteries.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("⚡ Dedicated Loadout Bays for Armor, Tools & Battery.").withStyle(ChatFormatting.DARK_AQUA));
    }
}

