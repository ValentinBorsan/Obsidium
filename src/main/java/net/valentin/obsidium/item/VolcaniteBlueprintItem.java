package net.valentin.obsidium.item;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.valentin.obsidium.block.VolcaniteWorktableBlockEntity;

import java.util.List;

public class VolcaniteBlueprintItem extends Item {
    public static final int GRID_SIZE = 9;

    public VolcaniteBlueprintItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean hasRecipe(ItemStack blueprint) {
        if (blueprint.isEmpty() || !blueprint.has(DataComponents.CUSTOM_DATA)) return false;
        CustomData data = blueprint.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        return tag.contains("Output") || tag.contains("Items");
    }

    public static NonNullList<ItemStack> getRecipeGrid(ItemStack blueprint, HolderLookup.Provider registries) {
        NonNullList<ItemStack> grid = NonNullList.withSize(GRID_SIZE, ItemStack.EMPTY);
        if (blueprint.isEmpty() || registries == null) return grid;
        CustomData data = blueprint.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        if (tag.contains("Items")) {
            ContainerHelper.loadAllItems(tag, grid, registries);
        }
        return grid;
    }

    public static ItemStack getRecipeOutput(ItemStack blueprint, HolderLookup.Provider registries) {
        if (blueprint.isEmpty() || registries == null) return ItemStack.EMPTY;
        CustomData data = blueprint.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        if (tag.contains("Output")) {
            return ItemStack.parse(registries, tag.getCompound("Output")).orElse(ItemStack.EMPTY);
        }
        return ItemStack.EMPTY;
    }

    public static void setRecipe(ItemStack blueprint, NonNullList<ItemStack> grid, ItemStack output, HolderLookup.Provider registries) {
        if (blueprint.isEmpty() || registries == null) return;
        CustomData.update(DataComponents.CUSTOM_DATA, blueprint, tag -> {
            ContainerHelper.saveAllItems(tag, grid, registries);
            if (!output.isEmpty()) {
                tag.put("Output", output.save(registries));
            } else {
                tag.remove("Output");
            }
        });
    }

    public static void clearRecipe(ItemStack blueprint) {
        if (blueprint.isEmpty()) return;
        blueprint.remove(DataComponents.CUSTOM_DATA);
    }

    public static boolean isUniform3x3(NonNullList<ItemStack> grid) {
        if (grid.size() < 9) return false;
        ItemStack first = grid.get(0);
        if (first.isEmpty()) return false;
        for (int i = 0; i < 9; i++) {
            ItemStack s = grid.get(i);
            if (s.isEmpty() || !ItemStack.isSameItemSameComponents(first, s)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (be instanceof VolcaniteWorktableBlockEntity worktable) {
            if (!level.isClientSide()) {
                ItemStack output = worktable.getItem(10);
                if (!output.isEmpty()) {
                    NonNullList<ItemStack> grid = NonNullList.withSize(GRID_SIZE, ItemStack.EMPTY);
                    for (int i = 0; i < 9; i++) {
                        ItemStack inGrid = worktable.getItem(i);
                        grid.set(i, inGrid.isEmpty() ? ItemStack.EMPTY : inGrid.copyWithCount(1));
                    }
                    setRecipe(stack, grid, output.copy(), level.registryAccess());
                    level.playSound(null, context.getClickedPos(), SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0f, 1.0f);
                    if (player != null) {
                        player.displayClientMessage(Component.literal("§6[Blueprint] §aSaved recipe: §f" + output.getHoverName().getString()), true);
                    }
                    return InteractionResult.SUCCESS;
                } else {
                    if (player != null) {
                        player.displayClientMessage(Component.literal("§6[Blueprint] §cWorktable has no valid recipe to copy."), true);
                    }
                    return InteractionResult.CONSUME;
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && hasRecipe(stack)) {
            if (!level.isClientSide()) {
                clearRecipe(stack);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.7f, 1.2f);
                player.displayClientMessage(Component.literal("§6[Blueprint] §eRecipe cleared."), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        HolderLookup.Provider provider = context.registries();
        if (!hasRecipe(stack) || provider == null) {
            tooltip.add(Component.literal("§7Empty Blueprint"));
            tooltip.add(Component.literal("§8Right-click on a Volcanite Worktable to copy recipe."));
            return;
        }

        ItemStack output = getRecipeOutput(stack, provider);
        NonNullList<ItemStack> grid = getRecipeGrid(stack, provider);

        if (!output.isEmpty()) {
            tooltip.add(Component.literal("§6Recipe: §e" + output.getHoverName().getString() + (output.getCount() > 1 ? " x" + output.getCount() : "")));
        }

        if (isUniform3x3(grid)) {
            tooltip.add(Component.literal("§bType: §f3x3 Uniform (Auto-Pulls 9x items)"));
            tooltip.add(Component.literal("§7Ingredient: §f" + grid.get(0).getHoverName().getString() + " x9"));
        } else {
            tooltip.add(Component.literal("§bPattern:"));
            for (int r = 0; r < 3; r++) {
                StringBuilder row = new StringBuilder("  ");
                for (int c = 0; c < 3; c++) {
                    ItemStack slot = grid.get(c + r * 3);
                    if (slot.isEmpty()) {
                        row.append("§8[ - ] ");
                    } else {
                        String name = slot.getHoverName().getString();
                        if (name.length() > 6) name = name.substring(0, 5) + ".";
                        row.append("§f[").append(name).append("] ");
                    }
                }
                tooltip.add(Component.literal(row.toString()));
            }
        }
        tooltip.add(Component.literal("§8Shift + Right-Click in air to clear."));
    }
}

