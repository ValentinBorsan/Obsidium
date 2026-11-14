package net.valentin.obsidium.item;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class VolcaniteBottleItem extends Item {
    private final int capacity;

    public VolcaniteBottleItem(Properties properties, int capacity) {
        super(properties);
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    public static FluidVariant getFluid(ItemStack stack) {
        if (stack.isEmpty()) return FluidVariant.blank();
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();

        if (tag.contains("FluidId")) {
            String id = tag.getString("FluidId");
            Fluid fluid = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(id));
            if (fluid != null && fluid != Fluids.EMPTY) {
                return FluidVariant.of(fluid);
            }
        } else if (tag.contains("LavaAmount") && tag.getInt("LavaAmount") > 0) {
            return FluidVariant.of(Fluids.LAVA);
        }
        return FluidVariant.blank();
    }

    public static int getFluidAmount(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();

        if (tag.contains("FluidAmount")) {
            return tag.getInt("FluidAmount");
        } else if (tag.contains("LavaAmount")) {
            return tag.getInt("LavaAmount");
        }
        return 0;
    }

    public static void setFluid(ItemStack stack, FluidVariant variant, int amount) {
        if (stack.isEmpty()) return;
        int max = (stack.getItem() instanceof VolcaniteBottleItem bottle) ? bottle.getCapacity() : 5000;
        final int boundedAmount = Math.max(0, Math.min(amount, max));

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (boundedAmount <= 0 || variant.isBlank()) {
                tag.remove("FluidId");
                tag.remove("FluidAmount");
                tag.remove("LavaAmount");
            } else {
                tag.putString("FluidId", BuiltInRegistries.FLUID.getKey(variant.getFluid()).toString());
                tag.putInt("FluidAmount", boundedAmount);
                if (variant.getFluid() == Fluids.LAVA) {
                    tag.putInt("LavaAmount", boundedAmount);
                } else {
                    tag.remove("LavaAmount");
                }
            }
        });
    }

    // Backwards compatibility for existing lava consumers
    public static int getLava(ItemStack stack) {
        FluidVariant fv = getFluid(stack);
        if (fv.getFluid() == Fluids.LAVA) {
            return getFluidAmount(stack);
        }
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getInt("LavaAmount");
    }

    public static void setLava(ItemStack stack, int amount) {
        setFluid(stack, FluidVariant.of(Fluids.LAVA), amount);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        FluidVariant currentFluid = getFluid(stack);
        int currentAmount = getFluidAmount(stack);

        // Can pickup fluid if bottle has at least 1000 mB space
        if (currentAmount + 1000 <= capacity) {
            BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = hitResult.getBlockPos();
                if (level.mayInteract(player, pos) && player.mayUseItemAt(pos, hitResult.getDirection(), stack)) {
                    BlockState state = level.getBlockState(pos);
                    Fluid hitFluid = state.getFluidState().getType();

                    if (hitFluid != Fluids.EMPTY && (currentFluid.isBlank() || currentFluid.getFluid() == hitFluid)) {
                        if (state.getBlock() instanceof BucketPickup pickup) {
                            ItemStack picked = pickup.pickupBlock(player, level, pos, state);
                            if (!picked.isEmpty()) {
                                setFluid(stack, FluidVariant.of(hitFluid), currentAmount + 1000);
                                if (hitFluid == Fluids.LAVA) {
                                    level.playSound(null, pos, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 1.0F, 1.0F);
                                } else {
                                    level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                                }
                                level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
                                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                            }
                        }
                    }
                }
            }
        }

        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        FluidVariant fv = getFluid(stack);
        int amount = getFluidAmount(stack);

        if (!fv.isBlank() && amount > 0) {
            Fluid fluid = fv.getFluid();
            String name;
            ChatFormatting color;
            if (fluid == Fluids.LAVA) {
                name = "Lava";
                color = ChatFormatting.GOLD;
            } else if (fluid == Fluids.WATER) {
                name = "Water";
                color = ChatFormatting.AQUA;
            } else if (fluid == net.valentin.obsidium.ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID) {
                name = "Pyroclastic Distillate";
                color = ChatFormatting.LIGHT_PURPLE;
            } else if (fluid == net.valentin.obsidium.ObsidiumMod.STEAM_FLUID) {
                name = "High-Pressure Steam";
                color = ChatFormatting.AQUA;
            } else {
                name = BuiltInRegistries.FLUID.getKey(fluid).getPath();
                color = ChatFormatting.LIGHT_PURPLE;
            }
            tooltipComponents.add(Component.literal(name + ": " + amount + " / " + capacity + " mB").withStyle(color));
        } else {
            tooltipComponents.add(Component.literal("Empty: 0 / " + capacity + " mB").withStyle(ChatFormatting.GRAY));
        }
        tooltipComponents.add(Component.literal("§7Holds any liquid. Right-click fluids in world or insert into tank.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
