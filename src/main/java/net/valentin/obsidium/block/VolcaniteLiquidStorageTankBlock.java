package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;

public class VolcaniteLiquidStorageTankBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public VolcaniteLiquidStorageTankBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VolcaniteLiquidStorageTankBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == ObsidiumMod.VOLCANITE_LIQUID_STORAGE_TANK_BE ?
            (BlockEntityTicker<T>) (BlockEntityTicker<VolcaniteLiquidStorageTankBlockEntity>) VolcaniteLiquidStorageTankBlockEntity::tick : null;
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof VolcaniteLiquidStorageTankBlockEntity tank) {
            // 1. Lava Bucket direct insert
            if (stack.is(net.minecraft.world.item.Items.LAVA_BUCKET)) {
                if (tank.insertFluid(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.LAVA), 1000, true) == 1000) {
                    if (!level.isClientSide()) {
                        tank.insertFluid(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.LAVA), 1000, false);
                        if (!player.getAbilities().instabuild) {
                            player.setItemInHand(hand, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET));
                        }
                        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BUCKET_EMPTY_LAVA, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
            // 2. Water Bucket direct insert
            else if (stack.is(net.minecraft.world.item.Items.WATER_BUCKET)) {
                if (tank.insertFluid(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER), 1000, true) == 1000) {
                    if (!level.isClientSide()) {
                        tank.insertFluid(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER), 1000, false);
                        if (!player.getAbilities().instabuild) {
                            player.setItemInHand(hand, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET));
                        }
                        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BUCKET_EMPTY, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
            // 3. Empty Bucket direct extract
            else if (stack.is(net.minecraft.world.item.Items.BUCKET) && tank.getTotalFluidAmount() >= 1000) {
                net.minecraft.world.level.material.Fluid active = tank.getActiveFluid();
                if (active != net.minecraft.world.level.material.Fluids.EMPTY) {
                    if (!level.isClientSide()) {
                        tank.extractFluid(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(active), 1000, false);
                        net.minecraft.world.item.Item filled = (active == net.minecraft.world.level.material.Fluids.LAVA) ? net.minecraft.world.item.Items.LAVA_BUCKET : net.minecraft.world.item.Items.WATER_BUCKET;
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                            if (stack.isEmpty()) {
                                player.setItemInHand(hand, new net.minecraft.world.item.ItemStack(filled));
                            } else if (!player.getInventory().add(new net.minecraft.world.item.ItemStack(filled))) {
                                player.drop(new net.minecraft.world.item.ItemStack(filled), false);
                            }
                        }
                        level.playSound(null, pos, (active == net.minecraft.world.level.material.Fluids.LAVA) ? net.minecraft.sounds.SoundEvents.BUCKET_FILL_LAVA : net.minecraft.sounds.SoundEvents.BUCKET_FILL, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
            // 4. Volcanite Bottle direct interaction
            else if (stack.getItem() instanceof net.valentin.obsidium.item.VolcaniteBottleItem bottle) {
                net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant bottleFluid = net.valentin.obsidium.item.VolcaniteBottleItem.getFluid(stack);
                int bottleAmount = net.valentin.obsidium.item.VolcaniteBottleItem.getFluidAmount(stack);
                net.minecraft.world.level.material.Fluid active = tank.getActiveFluid();

                // If sneak-clicking with a bottle that has fluid: drain bottle into tank!
                if (player.isShiftKeyDown() && bottleAmount > 0 && (active == net.minecraft.world.level.material.Fluids.EMPTY || active == bottleFluid.getFluid())) {
                    if (!level.isClientSide()) {
                        int inserted = tank.insertFluid(bottleFluid, Math.min(1000, bottleAmount), false);
                        if (inserted > 0) {
                            net.valentin.obsidium.item.VolcaniteBottleItem.setFluid(stack, bottleFluid, bottleAmount - inserted);
                            level.playSound(null, pos, (bottleFluid.getFluid() == net.minecraft.world.level.material.Fluids.LAVA) ? net.minecraft.sounds.SoundEvents.BUCKET_EMPTY_LAVA : net.minecraft.sounds.SoundEvents.BUCKET_EMPTY, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                        }
                    }
                    return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
                // If normal right-click: fill bottle from tank!
                else if (bottleAmount < bottle.getCapacity() && active != net.minecraft.world.level.material.Fluids.EMPTY && tank.getTotalFluidAmount() > 0 &&
                         (bottleFluid.isBlank() || bottleFluid.getFluid() == active)) {
                    if (!level.isClientSide()) {
                        int space = bottle.getCapacity() - bottleAmount;
                        int toTake = Math.min(space, Math.min(1000, tank.getTotalFluidAmount()));
                        int extracted = tank.extractFluid(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(active), toTake, false);
                        if (extracted > 0) {
                            net.valentin.obsidium.item.VolcaniteBottleItem.setFluid(stack, net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(active), bottleAmount + extracted);
                            level.playSound(null, pos, (active == net.minecraft.world.level.material.Fluids.LAVA) ? net.minecraft.sounds.SoundEvents.BUCKET_FILL_LAVA : net.minecraft.sounds.SoundEvents.BUCKET_FILL, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                        }
                    }
                    return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
        }

        if (!player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                if (be instanceof MenuProvider menuProvider) {
                    player.openMenu(menuProvider);
                }
            }
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MenuProvider menuProvider) {
                player.openMenu(menuProvider);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VolcaniteLiquidStorageTankBlockEntity tank) {
                Containers.dropContents(level, pos, tank);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
