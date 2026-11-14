package net.valentin.obsidium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class MetalFruitItem extends Item {
    private final Item mineral;
    private final int mineralCount;

    public MetalFruitItem(Properties properties, Item mineral, int mineralCount) {
        super(properties);
        this.mineral = mineral;
        this.mineralCount = mineralCount;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide() && entity instanceof Player player) {
            ItemStack mineralStack = new ItemStack(mineral, mineralCount);
            if (!player.getInventory().add(mineralStack)) {
                player.drop(mineralStack, false);
            }
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.obsidium.metal_fruit", mineralCount, Component.translatable(mineral.getDescriptionId())));
    }
}
