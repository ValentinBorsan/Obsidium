package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.VolcaniteLogisticsModuleItem;
import net.valentin.obsidium.item.VolcaniteWrenchItem;
import org.jetbrains.annotations.Nullable;

public class NaiadFluidPipeBlock extends Block implements SimpleWaterloggedBlock, EntityBlock {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static BooleanProperty getProperty(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    private static final VoxelShape CENTER = Block.box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape UP_SHAPE = Block.box(5, 11, 5, 11, 16, 11);
    private static final VoxelShape DOWN_SHAPE = Block.box(5, 0, 5, 11, 5, 11);
    private static final VoxelShape NORTH_SHAPE = Block.box(5, 5, 0, 11, 11, 5);
    private static final VoxelShape SOUTH_SHAPE = Block.box(5, 5, 11, 11, 11, 16);
    private static final VoxelShape WEST_SHAPE = Block.box(0, 5, 5, 5, 11, 11);
    private static final VoxelShape EAST_SHAPE = Block.box(11, 5, 5, 16, 11, 11);

    public NaiadFluidPipeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(EAST, false)
                .setValue(SOUTH, false).setValue(WEST, false)
                .setValue(UP, false).setValue(DOWN, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, WATERLOGGED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof PipeFacadeHolder holder && holder.getFacadeBlock() != null) {
            return Shapes.block();
        }
        VoxelShape shape = CENTER;
        if (state.getValue(UP)) shape = Shapes.or(shape, UP_SHAPE);
        if (state.getValue(DOWN)) shape = Shapes.or(shape, DOWN_SHAPE);
        if (state.getValue(NORTH)) shape = Shapes.or(shape, NORTH_SHAPE);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, SOUTH_SHAPE);
        if (state.getValue(WEST)) shape = Shapes.or(shape, WEST_SHAPE);
        if (state.getValue(EAST)) shape = Shapes.or(shape, EAST_SHAPE);
        return shape;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        return makeConnections(context.getLevel(), context.getClickedPos())
                .setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return makeConnections(level, pos).setValue(WATERLOGGED, state.getValue(WATERLOGGED));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        BlockState newState = makeConnections(level, pos).setValue(WATERLOGGED, state.getValue(WATERLOGGED));
        if (!newState.equals(state)) {
            level.setBlock(pos, newState, 3);
        }
    }

    public BlockState makeConnections(BlockGetter level, BlockPos pos) {
        BlockEntity be = (level != null && pos != null) ? level.getBlockEntity(pos) : null;
        NaiadFluidPipeBlockEntity pipeBe = (be instanceof NaiadFluidPipeBlockEntity p) ? p : null;

        return this.defaultBlockState()
                .setValue(UP, connectsTo(level, pos, Direction.UP, pipeBe))
                .setValue(DOWN, connectsTo(level, pos, Direction.DOWN, pipeBe))
                .setValue(NORTH, connectsTo(level, pos, Direction.NORTH, pipeBe))
                .setValue(SOUTH, connectsTo(level, pos, Direction.SOUTH, pipeBe))
                .setValue(EAST, connectsTo(level, pos, Direction.EAST, pipeBe))
                .setValue(WEST, connectsTo(level, pos, Direction.WEST, pipeBe));
    }

    public static boolean connectsTo(BlockGetter level, BlockPos pos, Direction dir, @Nullable NaiadFluidPipeBlockEntity pipeBe) {
        if (pipeBe != null && pipeBe.isSideDisconnected(dir)) {
            return false;
        }
        BlockPos neighborPos = pos.relative(dir);
        if (level == null) return false;

        BlockEntity neighborBe = level.getBlockEntity(neighborPos);
        if (neighborBe instanceof NaiadFluidPipeBlockEntity neighborPipe) {
            if (neighborPipe.isSideDisconnected(dir.getOpposite())) {
                return false;
            }
            return true;
        }

        // Connect to anything holding or moving fluids
        BlockState neighborState = level.getBlockState(neighborPos);
        Block block = neighborState.getBlock();
        if (block instanceof LiquidTankBlock ||
            block instanceof VolcaniteLavaPumpBlock ||
            block instanceof HydrothermalCondenserBlock ||
            block instanceof MagmaticCrucibleBlock ||
            block instanceof HydrothermalVitrifierBlock) {
            return true;
        }

        if (level instanceof Level world) {
            return FluidStorage.SIDED.find(world, neighborPos, dir.getOpposite()) != null;
        }
        return false;
    }

    public static Direction getTargetFaceFromHit(BlockHitResult hitResult, BlockPos pos) {
        net.minecraft.world.phys.Vec3 hitVec = hitResult.getLocation().subtract(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        double absX = Math.abs(hitVec.x);
        double absY = Math.abs(hitVec.y);
        double absZ = Math.abs(hitVec.z);
        double max = Math.max(absX, Math.max(absY, absZ));

        if (max > 0.18) {
            if (absY == max) return hitVec.y > 0 ? Direction.UP : Direction.DOWN;
            if (absZ == max) return hitVec.z > 0 ? Direction.SOUTH : Direction.NORTH;
            if (absX == max) return hitVec.x > 0 ? Direction.EAST : Direction.WEST;
        }
        return hitResult.getDirection();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof NaiadFluidPipeBlockEntity pipeBe)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        Direction targetFace = getTargetFaceFromHit(hitResult, pos);

        // 0. PIPE FACADE
        if (stack.getItem() instanceof net.valentin.obsidium.item.PipeFacadeItem) {
            if (pipeBe.getFacadeBlock() != null) {
                return ItemInteractionResult.FAIL;
            }
            net.minecraft.world.level.block.Block mimic = net.valentin.obsidium.item.PipeFacadeItem.getMimicBlock(stack, player);
            if (!level.isClientSide()) {
                pipeBe.setFacadeBlock(mimic);
                if (player != null && !player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, mimic.defaultBlockState().getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                if (player != null) {
                    player.displayClientMessage(Component.literal("§6[Pipe] §aApplied facade: §f" + mimic.getName().getString()), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        if (player.isShiftKeyDown() && pipeBe.getFacadeBlock() != null && (stack.isEmpty() || VolcaniteWrenchItem.isWrench(stack))) {
            if (!level.isClientSide()) {
                net.minecraft.world.level.block.Block fBlock = pipeBe.getFacadeBlock();
                ItemStack drop = net.valentin.obsidium.item.PipeFacadeItem.getFacadeFor(fBlock);
                if (!drop.isEmpty()) {
                    net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
                }
                pipeBe.setFacadeBlock(null);
                level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
                if (player != null) {
                    player.displayClientMessage(Component.literal("§6[Pipe] §cDismantled pipe facade!"), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        // 1. LOGISTICS VALVE MODULE
        if (stack.getItem() instanceof VolcaniteLogisticsModuleItem) {
            if (!state.getValue(getProperty(targetFace))) {
                if (!level.isClientSide() && player != null) {
                    player.displayClientMessage(Component.literal("§c[Naiad Pipe] Cannot mount valve on a disconnected face!"), true);
                }
                return ItemInteractionResult.FAIL;
            }

            if (!level.isClientSide()) {
                NaiadFluidPipeBlockEntity.ValveMode current = pipeBe.getValveMode(targetFace);
                if (current == NaiadFluidPipeBlockEntity.ValveMode.NONE) {
                    pipeBe.setValveMode(targetFace, NaiadFluidPipeBlockEntity.ValveMode.EXTRACT);
                    if (player != null && !player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 1.0F, 1.2F);
                    if (player != null) {
                        player.displayClientMessage(Component.literal("§b[Naiad Pipe] §aMounted Valve on §f" + targetFace.getName().toUpperCase() + " §aside! Mode: §6EXTRACT (Pulling)"), true);
                    }
                } else {
                    NaiadFluidPipeBlockEntity.ValveMode next = pipeBe.cycleValveMode(targetFace);
                    level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, 1.2F);
                    if (player != null) {
                        String modeDesc = next == NaiadFluidPipeBlockEntity.ValveMode.EXTRACT ? "§6EXTRACT (Pulling)" : "§bINSERT (Pushing)";
                        player.displayClientMessage(Component.literal("§b[Naiad Pipe] §7Face §f" + targetFace.getName().toUpperCase() + " §7valve mode: " + modeDesc), true);
                    }
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        // 2. WRENCH
        if (VolcaniteWrenchItem.isWrench(player.getItemInHand(hand))) {
            if (player != null && player.isShiftKeyDown()) {
                if (!level.isClientSide()) {
                    level.destroyBlock(pos, true, player);
                    player.displayClientMessage(Component.literal("§6[Wrench] §cDismantled " + state.getBlock().getName().getString()), true);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }

            if (!level.isClientSide()) {
                if (pipeBe.getValveMode(targetFace) != NaiadFluidPipeBlockEntity.ValveMode.NONE && !player.isShiftKeyDown()) {
                    NaiadFluidPipeBlockEntity.ValveMode next = pipeBe.cycleValveMode(targetFace);
                    level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 1.6F);
                    if (player != null) {
                        String modeDesc = next == NaiadFluidPipeBlockEntity.ValveMode.EXTRACT ? "§6EXTRACT (Pulling)" : "§bINSERT (Pushing)";
                        player.displayClientMessage(Component.literal("§6[Wrench] §b[Naiad Pipe] §7Face §f" + targetFace.getName().toUpperCase() + " §7valve mode: " + modeDesc), true);
                    }
                } else {
                    boolean nowDisconnected = pipeBe.toggleSideDisconnected(targetFace);
                    if (nowDisconnected) {
                        level.playSound(null, pos, SoundEvents.SPYGLASS_STOP_USING, SoundSource.BLOCKS, 1.0F, 1.2F);
                        if (player != null) {
                            player.displayClientMessage(Component.literal("§6[Wrench] §cDisconnected §f" + targetFace.getName().toUpperCase() + " §cside!"), true);
                        }
                    } else {
                        level.playSound(null, pos, SoundEvents.SPYGLASS_USE, SoundSource.BLOCKS, 1.0F, 1.4F);
                        if (player != null) {
                            player.displayClientMessage(Component.literal("§6[Wrench] §aConnected §f" + targetFace.getName().toUpperCase() + " §aside!"), true);
                        }
                    }
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof NaiadFluidPipeBlockEntity pipeBe) {
            Direction targetFace = getTargetFaceFromHit(hitResult, pos);

            if (player.isShiftKeyDown() && pipeBe.getFacadeBlock() != null) {
                if (!level.isClientSide()) {
                    net.minecraft.world.level.block.Block fBlock = pipeBe.getFacadeBlock();
                    ItemStack drop = net.valentin.obsidium.item.PipeFacadeItem.getFacadeFor(fBlock);
                    if (!drop.isEmpty()) {
                        net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
                    }
                    pipeBe.setFacadeBlock(null);
                    level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
                    player.displayClientMessage(Component.literal("§6[Pipe] §cDismantled pipe facade!"), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }

            if (player.isShiftKeyDown()) {
                if (pipeBe.getValveMode(targetFace) == NaiadFluidPipeBlockEntity.ValveMode.NONE) {
                    for (Direction d : Direction.values()) {
                        if (pipeBe.getValveMode(d) != NaiadFluidPipeBlockEntity.ValveMode.NONE) {
                            targetFace = d;
                            break;
                        }
                    }
                }

                if (pipeBe.getValveMode(targetFace) != NaiadFluidPipeBlockEntity.ValveMode.NONE) {
                    if (!level.isClientSide()) {
                        pipeBe.dismantleValve(targetFace);
                        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.2F);
                        player.displayClientMessage(Component.literal("§b[Naiad Pipe] §cDismantled Valve from §f" + targetFace.getName().toUpperCase() + " §cside!"), true);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide());
                }
            } else if (pipeBe.getValveMode(targetFace) != NaiadFluidPipeBlockEntity.ValveMode.NONE) {
                if (!level.isClientSide()) {
                    NaiadFluidPipeBlockEntity.ValveMode next = pipeBe.cycleValveMode(targetFace);
                    level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, 1.2F);
                    String modeDesc = next == NaiadFluidPipeBlockEntity.ValveMode.EXTRACT ? "§6EXTRACT (Pulling)" : "§bINSERT (Pushing)";
                    player.displayClientMessage(Component.literal("§b[Naiad Pipe] §7Face §f" + targetFace.getName().toUpperCase() + " §7valve mode: " + modeDesc), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof NaiadFluidPipeBlockEntity pipeBe) {
                pipeBe.dropAllValves();
                if (pipeBe.getFacadeBlock() != null) {
                    ItemStack drop = net.valentin.obsidium.item.PipeFacadeItem.getFacadeFor(pipeBe.getFacadeBlock());
                    if (!drop.isEmpty()) {
                        net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), drop);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NaiadFluidPipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ObsidiumMod.NAIAD_FLUID_PIPE_BE ?
                (BlockEntityTicker<T>) (BlockEntityTicker<NaiadFluidPipeBlockEntity>) NaiadFluidPipeBlockEntity::tick : null;
    }
}
