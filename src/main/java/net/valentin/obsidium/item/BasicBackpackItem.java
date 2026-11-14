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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.valentin.obsidium.menu.BasicBackpackMenu;

import java.util.List;

public class BasicBackpackItem extends Item {
    public static final int CONTAINER_SIZE = 27;

    public BasicBackpackItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            level.playSound(null, player.blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.8F, 1.0F);

            player.openMenu(new SimpleMenuProvider(
                    (syncId, inv, p) -> new BasicBackpackMenu(syncId, inv, hand),
                    stack.getHoverName()
            ));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static NonNullList<ItemStack> loadInventory(ItemStack backpack, HolderLookup.Provider registries) {
        NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
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

        int occupied = 0;
        if (context.registries() != null) {
            occupied = getOccupiedSlots(stack, context.registries());
        }

        tooltip.add(Component.literal("Capacity: 27 Slots").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Stored Items: " + occupied + " / 27 Slots").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("Compact leather storage reinforced with volcanic alloy.").withStyle(ChatFormatting.GRAY));
    }
}

