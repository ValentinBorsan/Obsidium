package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import team.reborn.energy.api.base.SimpleEnergyItem;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;

public class VolcanitePortableBatteryItem extends Item implements SimpleEnergyItem {
    private final long capacity;
    private final long maxIn;
    private final long maxOut;

    public VolcanitePortableBatteryItem(Properties properties, long capacity, long maxIn, long maxOut) {
        super(properties);
        this.capacity = capacity;
        this.maxIn = maxIn;
        this.maxOut = maxOut;
    }

    public static boolean isAutoChargeEnabled(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        return !tag.contains("AutoCharge") || tag.getBoolean("AutoCharge");
    }

    public static void setAutoChargeEnabled(ItemStack stack, boolean enabled) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean("AutoCharge", enabled));
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return capacity;
    }

    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return maxIn;
    }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return maxOut;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getStoredEnergy(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round((float) getStoredEnergy(stack) * 13.0F / (float) capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return capacity >= 1_000_000 ? 0xFFAA00 : 0x00E5FF;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            boolean next = !isAutoChargeEnabled(stack);
            setAutoChargeEnabled(stack, next);
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.literal("§6[Portable Battery] Auto-Charge: " +
                        (next ? "§aACTIVATED §7(Charging Inventory)" : "§cDEACTIVATED §7(Standby)")), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        next ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.STONE_BUTTON_CLICK_OFF,
                        SoundSource.PLAYERS, 0.6F, next ? 1.5F : 0.8F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return super.use(level, player, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide() && entity instanceof Player player) {
            if (!isAutoChargeEnabled(stack)) return;

            long stored = getStoredEnergy(stack);
            if (stored <= 0) return;

            // Charge items in inventory (every 10 ticks to reduce lag)
            if (level.getGameTime() % 10 == 0) {
                InventoryStorage invStorage = InventoryStorage.of(player.getInventory(), null);
                SingleSlotStorage<ItemVariant> batterySlot = invStorage.getSlot(slotId);
                ContainerItemContext batteryCtx = ContainerItemContext.ofSingleSlot(batterySlot);
                EnergyStorage batteryStorage = EnergyStorage.ITEM.find(stack, batteryCtx);
                if (batteryStorage == null) return;

                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack targetStack = player.getInventory().getItem(i);
                    if (targetStack.isEmpty() || targetStack == stack || targetStack.getItem() instanceof VolcanitePortableBatteryItem) continue;

                    SingleSlotStorage<ItemVariant> targetSlot = invStorage.getSlot(i);
                    ContainerItemContext targetCtx = ContainerItemContext.ofSingleSlot(targetSlot);
                    EnergyStorage targetStorage = EnergyStorage.ITEM.find(targetStack, targetCtx);
                    if (targetStorage != null && targetStorage.supportsInsertion()) {
                        long moved = EnergyStorageUtil.move(batteryStorage, targetStorage, this.maxOut, null);
                        if (moved > 0) {
                            stored -= moved;
                            if (stored <= 0) break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        long stored = getStoredEnergy(stack);
        ChatFormatting color = capacity >= 1_000_000 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GREEN;
        tooltipComponents.add(Component.literal("Energy: " + String.format("%,d", stored) + " / " + String.format("%,d", capacity) + " RF").withStyle(color));
        boolean autoCharge = isAutoChargeEnabled(stack);
        tooltipComponents.add(Component.literal("Auto-Charge: " + (autoCharge ? "§aENABLED" : "§cDISABLED")).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("§8(Sneak + Right-Click to toggle)").withStyle(ChatFormatting.DARK_GRAY));
        if (capacity >= 1_000_000) {
            tooltipComponents.add(Component.literal("§6★ Ultra-Capacity Supreme Energy Cell").withStyle(ChatFormatting.GOLD));
            tooltipComponents.add(Component.literal("§7Transfer rate: §b100,000 RF/t").withStyle(ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(Component.literal("§7Transfer rate: §b10,000 RF/t").withStyle(ChatFormatting.GRAY));
        }
    }
}
