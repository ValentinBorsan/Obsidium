package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import team.reborn.energy.api.base.SimpleEnergyItem;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class HephaesteanAxeItem extends AxeItem implements SimpleEnergyItem {

    public static final long CAPACITY = 1_000_000L;
    public static final long MAX_INPUT = 50_000L;
    public static final long ENERGY_PER_BLOCK = 100L;
    public static final int MAX_BLOCKS = 512;

    public HephaesteanAxeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) { return CAPACITY; }

    @Override
    public long getEnergyMaxInput(ItemStack stack) { return MAX_INPUT; }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) { return 0; }

    public static boolean isLumberjackMode(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getBoolean("Lumberjack");
    }

    public static void toggleLumberjackMode(ItemStack stack) {
        boolean curr = isLumberjackMode(stack);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean("Lumberjack", !curr));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            toggleLumberjackMode(stack);
            if (!level.isClientSide()) {
                boolean active = isLumberjackMode(stack);
                player.displayClientMessage(Component.literal("§6[Hephaestean Axe] §bFelling Mode: " + (active ? "§aGrand Timber Felling" : "§c1x1 Precision")), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6F, 1.2F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockState state = level.getBlockState(pos);

        // Right-Click on log to instantly transmute into refined charcoal
        if (state.is(BlockTags.LOGS) && getStoredEnergy(stack) >= 200L) {
            tryUseEnergy(stack, 200L);
            if (!level.isClientSide()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                Block.popResource(level, pos, new ItemStack(Items.CHARCOAL, 2));
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.05);
                }
            }
            level.playSound(player, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 1.2F);
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return super.useOn(context);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (getStoredEnergy(stack) < ENERGY_PER_BLOCK) return 1.0F;
        return super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        stack.setDamageValue(0);

        if (getStoredEnergy(stack) >= ENERGY_PER_BLOCK) {
            tryUseEnergy(stack, ENERGY_PER_BLOCK);

            if (!level.isClientSide() && state.is(BlockTags.LOGS) && miner instanceof Player player) {
                if (isLumberjackMode(stack) && !player.isShiftKeyDown()) {
                    fellTree(level, pos, player, stack);
                }
            }
        }
        return super.mineBlock(stack, level, state, pos, miner);
    }

    private void fellTree(Level level, BlockPos startPos, Player player, ItemStack tool) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty() && visited.size() <= MAX_BLOCKS && getStoredEnergy(tool) >= ENERGY_PER_BLOCK) {
            BlockPos current = queue.poll();

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos neighbor = current.offset(dx, dy, dz);

                        if (!visited.contains(neighbor) && level.getBlockState(neighbor).is(BlockTags.LOGS)) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }

            if (!current.equals(startPos)) {
                BlockState s = level.getBlockState(current);
                if (s.is(BlockTags.LOGS)) {
                    level.destroyBlock(current, true, player);
                    tryUseEnergy(tool, ENERGY_PER_BLOCK);
                }
            }
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) { return true; }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round((double) getStoredEnergy(stack) * 13.0 / (double) CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) { return 0x00E5FF; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        long stored = getStoredEnergy(stack);
        tooltip.add(Component.literal("§b⚡ Energy: §f" + String.format("%,d", stored) + " §b/ §f" + String.format("%,d", CAPACITY) + " RF").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("§6Felling Mode: " + (isLumberjackMode(stack) ? "§aGrand Timber Felling" : "§71x1 Precision")).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§8[Shift + Right-Click: Toggle Mode]").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("§eHephaestean Pyrolysis: §7Right-click log to transmute into charcoal.").withStyle(ChatFormatting.YELLOW));
    }
}

