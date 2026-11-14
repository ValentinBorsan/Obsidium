package net.valentin.obsidium.item;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.List;

public class VolcaniteLiquidStorageDiskItem extends Item {
    private final int capacity; // in mB (e.g. 32000, 64000, etc.)

    public VolcaniteLiquidStorageDiskItem(Properties properties, int capacity) {
        super(properties);
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    public static FluidVariant getStoredFluid(ItemStack disk) {
        CustomData data = disk.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        if (tag.contains("Fluid")) {
            String fluidId = tag.getString("Fluid");
            Fluid fluid = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(fluidId));
            if (fluid != null && fluid != Fluids.EMPTY) {
                return FluidVariant.of(fluid);
            }
        }
        return FluidVariant.blank();
    }

    public static int getStoredAmount(ItemStack disk) {
        CustomData data = disk.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getInt("Amount");
    }

    public static int insert(ItemStack disk, FluidVariant fluid, int amount_mB, boolean simulate) {
        if (disk.isEmpty() || fluid.isBlank() || amount_mB <= 0
                || !fluid.getComponents().isEmpty()) return 0;
        if (!(disk.getItem() instanceof VolcaniteLiquidStorageDiskItem diskItem)) return 0;

        FluidVariant currentFluid = getStoredFluid(disk);
        int currentAmount = getStoredAmount(disk);

        if (!currentFluid.isBlank() && !currentFluid.equals(fluid)) {
            return 0; // Disk already stores a different fluid
        }

        int space = diskItem.getCapacity() - currentAmount;
        if (space <= 0) return 0;

        int toInsert = Math.min(space, amount_mB);
        if (!simulate) {
            CustomData.update(DataComponents.CUSTOM_DATA, disk, tag -> {
                tag.putString("Fluid", BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString());
                tag.putInt("Amount", currentAmount + toInsert);
            });
        }

        return toInsert;
    }

    public static int extract(ItemStack disk, FluidVariant fluid, int amount_mB, boolean simulate) {
        if (disk.isEmpty() || amount_mB <= 0) return 0;
        if (!(disk.getItem() instanceof VolcaniteLiquidStorageDiskItem)) return 0;

        FluidVariant currentFluid = getStoredFluid(disk);
        int currentAmount = getStoredAmount(disk);

        if (currentFluid.isBlank() || currentAmount <= 0) return 0;
        if (!fluid.isBlank() && !currentFluid.equals(fluid)) return 0;

        int toExtract = Math.min(currentAmount, amount_mB);
        if (!simulate) {
            CustomData.update(DataComponents.CUSTOM_DATA, disk, tag -> {
                int remaining = currentAmount - toExtract;
                if (remaining <= 0) {
                    tag.remove("Fluid");
                    tag.remove("Amount");
                } else {
                    tag.putInt("Amount", remaining);
                }
            });
        }

        return toExtract;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag isAdvanced) {
        FluidVariant fluid = getStoredFluid(stack);
        int amount = getStoredAmount(stack);

        if (fluid.isBlank() || amount <= 0) {
            tooltip.add(Component.literal("Fluid: Empty (0 / " + String.format("%,d", capacity) + " mB)").withStyle(ChatFormatting.GRAY));
        } else {
            String fluidName;
            ChatFormatting color;
            if (fluid.isOf(Fluids.LAVA)) {
                fluidName = "Lava";
                color = ChatFormatting.GOLD;
            } else if (fluid.isOf(Fluids.WATER)) {
                fluidName = "Water";
                color = ChatFormatting.AQUA;
            } else if (fluid.isOf(net.valentin.obsidium.ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID)) {
                fluidName = "Pyroclastic Distillate";
                color = ChatFormatting.LIGHT_PURPLE;
            } else if (fluid.isOf(net.valentin.obsidium.ObsidiumMod.STEAM_FLUID)) {
                fluidName = "High-Pressure Steam";
                color = ChatFormatting.AQUA;
            } else {
                fluidName = fluid.getFluid().defaultFluidState().createLegacyBlock().getBlock().getName().getString();
                color = ChatFormatting.AQUA;
            }
            int percent = (int) (((float) amount / capacity) * 100);
            tooltip.add(Component.literal("Fluid: " + fluidName + " (" + String.format("%,d", amount) + " / " + String.format("%,d", capacity) + " mB - " + percent + "%)").withStyle(color));
        }
        tooltip.add(Component.literal("Volcanite Liquid Network Storage").withStyle(ChatFormatting.DARK_GRAY));
    }
}
