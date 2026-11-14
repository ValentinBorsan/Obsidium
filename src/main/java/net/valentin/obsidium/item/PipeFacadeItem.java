package net.valentin.obsidium.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.PipeFacadeHolder;

import java.util.List;

public class PipeFacadeItem extends Item {
    private static final String MIMIC_KEY = "MimicBlock";

    public PipeFacadeItem(Properties properties) {
        super(properties.stacksTo(64));
    }

    public static Block getMimicBlock(ItemStack stack, Player player) {
        // 1. Check if stack has an imprinted block in CustomData
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(MIMIC_KEY)) {
                ResourceLocation id = ResourceLocation.tryParse(tag.getString(MIMIC_KEY));
                if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) {
                    Block block = BuiltInRegistries.BLOCK.get(id);
                    if (block != Blocks.AIR) {
                        return block;
                    }
                }
            }
        }

        // 2. Check if player is holding a BlockItem in their offhand
        if (player != null) {
            ItemStack offhand = player.getOffhandItem();
            if (offhand != stack && offhand.getItem() instanceof BlockItem blockItem) {
                Block candidate = blockItem.getBlock();
                if (isValidFacadeBlock(candidate)) {
                    return candidate;
                }
            }
        }

        // 3. Default to Volcanite Casing
        return ObsidiumMod.VOLCANITE_CASING_BLOCK;
    }

    public Block getMimicBlock() {
        return ObsidiumMod.VOLCANITE_CASING_BLOCK;
    }

    public static boolean isValidFacadeBlock(Block block) {
        if (block == null || block == Blocks.AIR) return false;
        BlockState state = block.defaultBlockState();
        return !state.isAir() && !(block instanceof net.valentin.obsidium.block.VolcaniteStoragePipeBlock)
                && !(block instanceof net.valentin.obsidium.block.NaiadFluidPipeBlock)
                && !(block instanceof net.valentin.obsidium.block.VolcaniteEnergyWireBlock);
    }

    public static void setMimicBlock(ItemStack stack, Block block) {
        if (block == null || block == ObsidiumMod.VOLCANITE_CASING_BLOCK) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag tag = customData.copyTag();
                tag.remove(MIMIC_KEY);
                if (tag.isEmpty()) {
                    stack.remove(DataComponents.CUSTOM_DATA);
                } else {
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                }
            }
            return;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putString(MIMIC_KEY, id.toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static ItemStack getFacadeFor(Block block) {
        if (block == null) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(ObsidiumMod.PIPE_FACADE);
        setMimicBlock(stack, block);
        return stack;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof PipeFacadeHolder holder) {
            if (holder.getFacadeBlock() != null) {
                return InteractionResult.FAIL;
            }
            Block mimic = getMimicBlock(stack, player);
            if (!level.isClientSide()) {
                holder.setFacadeBlock(mimic);
                if (player != null && !player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, mimic.defaultBlockState().getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                if (player != null) {
                    player.displayClientMessage(Component.literal("§6[Pipe] §aApplied facade: §f" + mimic.getName().getString()), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // Right-click or Shift + right-click any solid block in the world to copy its appearance onto the Universal Pipe Facade
        if (player != null) {
            Block clickedBlock = level.getBlockState(pos).getBlock();
            if (isValidFacadeBlock(clickedBlock)) {
                if (!level.isClientSide()) {
                    setMimicBlock(stack, clickedBlock);
                    level.playSound(null, pos, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 0.8F, 1.2F);
                    player.displayClientMessage(Component.literal("§6[Facade] §eCopied block appearance: §f" + clickedBlock.getName().getString()), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (player.isShiftKeyDown()) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null && customData.copyTag().contains(MIMIC_KEY)) {
                if (!level.isClientSide()) {
                    setMimicBlock(stack, null);
                    level.playSound(null, player.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 0.6F, 1.4F);
                    player.displayClientMessage(Component.literal("§6[Facade] §aReset to default (Volcanite Casing) — ready to stack!"), true);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
        }
        return super.use(level, player, usedHand);
    }

    @Override
    public Component getName(ItemStack stack) {
        Block mimic = getMimicBlock(stack, null);
        if (mimic != null && mimic != ObsidiumMod.VOLCANITE_CASING_BLOCK) {
            return Component.literal("Pipe Facade (" + mimic.getName().getString() + ")");
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Block mimic = getMimicBlock(stack, null);
        tooltip.add(Component.literal("§6✦ Universal Pipe Concealment Facade"));
        tooltip.add(Component.literal("§7Disguises pipe as: §f" + mimic.getName().getString()));
        tooltip.add(Component.literal("§e• Right-click any block §7to copy its texture"));
        tooltip.add(Component.literal("§e• Shift + Right-click in air §7to reset for stacking"));
        tooltip.add(Component.literal("§e• Or hold a block in Offhand §7while clicking a pipe"));
        tooltip.add(Component.literal("§8• Right-click any pipe/wire to seamlessly encase it"));
        tooltip.add(Component.literal("§8• Shift + right-click pipe with bare hand/wrench to remove"));
    }
}
