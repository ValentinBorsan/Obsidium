package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.PollinatingBrushItem;
import org.jetbrains.annotations.Nullable;

public class VolcanicBeehiveBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty HONEY_LEVEL = BlockStateProperties.LEVEL_HONEY; // 0 to 5

    public VolcanicBeehiveBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HONEY_LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HONEY_LEVEL);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VolcanicBeehiveBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide() ? null : (lvl, pos, st, be) -> {
            if (be instanceof VolcanicBeehiveBlockEntity hive) {
                hive.serverTick(lvl, pos, st);
            }
        };
    }

    public static boolean isSmoked(Level level, BlockPos pos) {
        for (int i = 1; i <= 5; ++i) {
            BlockPos check = pos.below(i);
            BlockState state = level.getBlockState(check);
            if (CampfireBlock.isLitCampfire(state)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        int honey = state.getValue(HONEY_LEVEL);

        // 1. Shearing full hive -> 3x Volcanic Honeycomb
        if (honey >= 5 && stack.getItem() instanceof ShearsItem) {
            level.playSound(player, pos, SoundEvents.BEEHIVE_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!level.isClientSide()) {
                Block.popResource(level, pos, new ItemStack(ObsidiumMod.VOLCANIC_HONEYCOMB, 3));
                stack.hurtAndBreak(1, player, Player.getSlotForHand(hand));
                level.setBlock(pos, state.setValue(HONEY_LEVEL, 0), 3);

                if (!isSmoked(level, pos)) {
                    angerBees(level, pos, player);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        // 2. Glass Bottle on full hive -> Volcanic Honey Bottle
        if (honey >= 5 && stack.is(Items.GLASS_BOTTLE)) {
            level.playSound(player, pos, SoundEvents.BEEHIVE_DRIP, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!level.isClientSide()) {
                stack.shrink(1);
                ItemStack honeyBottle = new ItemStack(ObsidiumMod.VOLCANIC_HONEY_BOTTLE);
                if (!player.getInventory().add(honeyBottle)) {
                    player.drop(honeyBottle, false);
                }
                level.setBlock(pos, state.setValue(HONEY_LEVEL, 0), 3);

                if (!isSmoked(level, pos)) {
                    angerBees(level, pos, player);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        // 3. Pollinating Brush -> peaceful harvest of royal pollen without angering bees
        if (stack.getItem() instanceof PollinatingBrushItem) {
            if (honey > 0) {
                level.playSound(player, pos, SoundEvents.BRUSH_GENERIC, SoundSource.BLOCKS, 1.0F, 1.2F);
                if (!level.isClientSide()) {
                    PollinatingBrushItem.setCharge(stack, "hephaestus_volcanite", PollinatingBrushItem.MAX_CHARGES);
                    level.setBlock(pos, state.setValue(HONEY_LEVEL, Math.max(0, honey - 1)), 3);
                    if (level instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.05);
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.isCreative() && level.getBlockEntity(pos) instanceof VolcanicBeehiveBlockEntity hive) {
            boolean hasSilkTouch = net.minecraft.world.item.enchantment.EnchantmentHelper.hasTag(player.getMainHandItem(), net.minecraft.tags.EnchantmentTags.PREVENTS_BEE_SPAWNS_WHEN_MINING);
            if (!hasSilkTouch && level instanceof ServerLevel) {
                hive.angerBees(player);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private void angerBees(Level level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof VolcanicBeehiveBlockEntity be) {
            be.angerBees(player);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(HONEY_LEVEL) >= 5) {
            // Drip glowing volcanic honey particles
            for (int i = 0; i < random.nextInt(2) + 1; ++i) {
                double px = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
                double py = pos.getY() - 0.05;
                double pz = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
                level.addParticle(ParticleTypes.DRIPPING_DRIPSTONE_LAVA, px, py, pz, 0.0, 0.0, 0.0);
            }
        }
    }
}

