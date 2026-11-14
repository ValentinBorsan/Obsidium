package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import team.reborn.energy.api.base.SimpleEnergyItem;

import java.util.List;

public class VolcaniteEnergyCellBlockItem extends BlockItem implements SimpleEnergyItem {
    private final long capacity;
    private final long maxTransfer;
    private final boolean advanced;

    public VolcaniteEnergyCellBlockItem(Block block, Properties properties, long capacity, long maxTransfer, boolean advanced) {
        super(block, properties);
        this.capacity = capacity;
        this.maxTransfer = maxTransfer;
        this.advanced = advanced;
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return capacity;
    }

    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return stack.getCount() == 1 ? maxTransfer : 0L;
    }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return stack.getCount() == 1 ? maxTransfer : 0L;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getStoredEnergy(stack) > 0L;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        long cap = Math.max(1L, capacity);
        long stored = Math.min(cap, getStoredEnergy(stack));
        return Math.min(13, Math.max(0, Math.round((float) stored * 13.0F / (float) cap)));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return advanced ? 0x00E5FF : 0xFFAA00;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, context, tooltipComponents, isAdvanced);
        long stored = getStoredEnergy(stack);
        double pct = (double) stored * 100.0 / (double) Math.max(1L, capacity);
        ChatFormatting energyColor = advanced ? ChatFormatting.AQUA : ChatFormatting.GOLD;

        tooltipComponents.add(Component.literal(String.format("⚡ Energy: %,d / %,d RF (%.0f%%)", stored, capacity, pct)).withStyle(energyColor));
        tooltipComponents.add(Component.literal(String.format("§7Max I/O: §e%,d RF/t §8(Up to 4× w/ Upgrades)", maxTransfer)));
        tooltipComponents.add(Component.literal("§73×3 Core Matrix Bonus: §a+" + (advanced ? "100M" : "25M") + " RF"));

        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom != null) {
            CompoundTag tag = custom.copyTag();
            boolean hasCfg = tag.contains("FaceModes") || tag.getInt("RedstoneMode") != 0 || tag.getBoolean("FieldCharge") || tag.getInt("TransferMode") != 0;
            if (hasCfg) {
                tooltipComponents.add(Component.literal("§d⚙ Saved I/O Config"
                        + (tag.getBoolean("FieldCharge") ? " §8| §b🛜 Field ON" : "")));
            }
        }
        tooltipComponents.add(Component.literal("§8Right-Click face w/ Wrench: Cycle I/O · Shift+Click: Pick up w/ RF"));
    }
}

