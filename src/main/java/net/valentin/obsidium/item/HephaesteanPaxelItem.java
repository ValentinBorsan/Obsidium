package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.valentin.obsidium.util.ToolMiningHelper;
import team.reborn.energy.api.base.SimpleEnergyItem;

import java.util.List;

public class HephaesteanPaxelItem extends DiggerItem implements SimpleEnergyItem {

    public static final long CAPACITY = 3_000_000L;
    public static final long MAX_INPUT = 100_000L;
    public static final long ENERGY_PER_BLOCK = 120L;
    public static final long ENERGY_AUTO_SMELT = 150L;
    public static final long ENERGY_ERUPTION = 5_000L;

    public HephaesteanPaxelItem(Tier tier, Properties properties) {
        super(tier, BlockTags.MINEABLE_WITH_PICKAXE, properties);
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) { return CAPACITY; }

    @Override
    public long getEnergyMaxInput(ItemStack stack) { return MAX_INPUT; }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) { return 0; }

    public static int getPaxelMode(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getInt("PaxelMode");
    }

    public static void cyclePaxelMode(ItemStack stack) {
        int next = (getPaxelMode(stack) + 1) % 3;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("PaxelMode", next));
    }

    public static boolean isAutoSmelt(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getBoolean("AutoSmelt");
    }

    public static void toggleAutoSmelt(ItemStack stack) {
        boolean curr = isAutoSmelt(stack);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean("AutoSmelt", !curr));
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) || 
               state.is(BlockTags.MINEABLE_WITH_AXE) || 
               state.is(BlockTags.MINEABLE_WITH_SHOVEL) || 
               state.is(BlockTags.MINEABLE_WITH_HOE) ||
               state.is(BlockTags.SWORD_EFFICIENT);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (getStoredEnergy(stack) < ENERGY_PER_BLOCK) return 1.0F;
        if (isCorrectToolForDrops(stack, state)) {
            return 18.0F;
        }
        return super.getDestroySpeed(stack, state);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            cyclePaxelMode(stack);
            if (!level.isClientSide()) {
                int mode = getPaxelMode(stack);
                String modeName = switch (mode) {
                    case 0 -> "§e1x1 Precision";
                    case 1 -> "§a3x3 Omni-Tunnel";
                    case 2 -> "§d3x3x3 Cataclysm Quarry";
                    default -> "1x1";
                };
                player.displayClientMessage(Component.literal("§6[Hephaestean Paxel] §bMode: " + modeName), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6F, 1.4F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        // Right-Click in air: trigger Magma Eruption shockwave!
        if (!player.getCooldowns().isOnCooldown(this) && getStoredEnergy(stack) >= ENERGY_ERUPTION) {
            tryUseEnergy(stack, ENERGY_ERUPTION);
            player.getCooldowns().addCooldown(this, 30);

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
            if (level instanceof ServerLevel serverLevel) {
                Vec3 look = player.getLookAngle();
                Vec3 origin = player.getEyePosition();

                for (int i = 1; i <= 12; i++) {
                    Vec3 pos = origin.add(look.scale(i));
                    serverLevel.sendParticles(ParticleTypes.LAVA, pos.x, pos.y, pos.z, 15, 0.4, 0.4, 0.4, 0.1);
                    serverLevel.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 20, 0.3, 0.3, 0.3, 0.08);

                    AABB box = new AABB(pos.x - 1.8, pos.y - 1.8, pos.z - 1.8, pos.x + 1.8, pos.y + 1.8, pos.z + 1.8);
                    List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive());
                    for (LivingEntity entity : targets) {
                        entity.hurt(serverLevel.damageSources().playerAttack(player), 20.0F);
                        entity.igniteForSeconds(20);
                        entity.setDeltaMovement(entity.getDeltaMovement().add(look.x * 0.9, 0.7, look.z * 0.9));
                    }
                }
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

        // Till or Path
        if ((state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)) && level.getBlockState(pos.above()).isAir()) {
            if (player != null && player.isShiftKeyDown()) {
                level.setBlock(pos, Blocks.DIRT_PATH.defaultBlockState(), 3);
                level.playSound(player, pos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0F, 1.0F);
            } else {
                level.setBlock(pos, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 3);
                level.playSound(player, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            tryUseEnergy(stack, 50L);
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return super.useOn(context);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        stack.setDamageValue(0);
        long cost = ENERGY_PER_BLOCK + (isAutoSmelt(stack) ? ENERGY_AUTO_SMELT : 0);

        if (getStoredEnergy(stack) >= cost) {
            tryUseEnergy(stack, cost);

            if (!level.isClientSide() && miner instanceof ServerPlayer player && level instanceof ServerLevel serverLevel) {
                int mode = getPaxelMode(stack);
                boolean autoSmelt = isAutoSmelt(stack);

                if (mode == 1) { // 3x3
                    List<BlockPos> surrounding = ToolMiningHelper.getSurroundingBlocks(pos, player, 1, 1, 1);
                    int broken = ToolMiningHelper.breakBlocksInArea(serverLevel, player, stack, surrounding, autoSmelt);
                    tryUseEnergy(stack, broken * cost);
                } else if (mode == 2) { // 3x3x3
                    List<BlockPos> quarry = ToolMiningHelper.getSurroundingBlocks(pos, player, 1, 1, 3);
                    int broken = ToolMiningHelper.breakBlocksInArea(serverLevel, player, stack, quarry, autoSmelt);
                    tryUseEnergy(stack, broken * cost);
                }
            }
        }
        return super.mineBlock(stack, level, state, pos, miner);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) { return true; }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round((double) getStoredEnergy(stack) * 13.0 / (double) CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) { return 0xFFD700; } // Divine gold

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        long stored = getStoredEnergy(stack);
        tooltip.add(Component.literal("§6⚡ Energy: §f" + String.format("%,d", stored) + " §6/ §f" + String.format("%,d", CAPACITY) + " RF").withStyle(ChatFormatting.GOLD));
        int mode = getPaxelMode(stack);
        String modeName = switch (mode) {
            case 0 -> "§e1x1 Precision";
            case 1 -> "§a3x3 Omni-Tunnel";
            case 2 -> "§d3x3x3 Cataclysm Quarry";
            default -> "1x1";
        };
        tooltip.add(Component.literal("§bOmni-Mode: " + modeName).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("§8[Shift + Right-Click: Cycle Mode]").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("§8[Right-Click: Magma Eruption Shockwave]").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("§6★ The Ultimate Divine Multi-Tool of Hephaestus").withStyle(ChatFormatting.GOLD));
    }
}

