package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.PipeFacadeItem;
import net.valentin.obsidium.item.VolcaniteWrenchItem;
import org.jetbrains.annotations.Nullable;

public class VolcaniteEnergyWireBlock extends Block implements SimpleWaterloggedBlock, EntityBlock {
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

    private static final VoxelShape CENTER = Block.box(6, 6, 6, 10, 10, 10);
    private static final VoxelShape UP_SHAPE = Block.box(6, 10, 6, 10, 16, 10);
    private static final VoxelShape DOWN_SHAPE = Block.box(6, 0, 6, 10, 6, 10);
    private static final VoxelShape NORTH_SHAPE = Block.box(6, 6, 0, 10, 10, 6);
    private static final VoxelShape SOUTH_SHAPE = Block.box(6, 6, 10, 10, 10, 16);
    private static final VoxelShape WEST_SHAPE = Block.box(0, 6, 6, 6, 10, 10);
    private static final VoxelShape EAST_SHAPE = Block.box(10, 6, 6, 16, 10, 10);

    private final int tier;
    private final long maxTransfer;
    private final long capacity;

    public VolcaniteEnergyWireBlock(Properties properties, int tier, long maxTransfer, long capacity) {
        super(properties);
        this.tier = tier;
        this.maxTransfer = maxTransfer;
        this.capacity = capacity;
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(NORTH, false).setValue(EAST, false)
            .setValue(SOUTH, false).setValue(WEST, false)
            .setValue(UP, false).setValue(DOWN, false)
            .setValue(WATERLOGGED, false));
    }

    public VolcaniteEnergyWireBlock(Properties properties) {
        this(properties, 1, 5_000L, 20_000L);
    }

    public int getTier() { return tier; }
    public long getMaxTransfer() { return maxTransfer; }
    public long getCapacity() { return capacity; }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        String tierName = switch (tier) {
            case 2 -> "§b★ Advanced Tier";
            case 3 -> "§d★ Supreme Tier";
            default -> "§e★ Basic Tier";
        };
        tooltip.add(Component.literal(tierName));
        tooltip.add(Component.literal("§7Max Transfer: §a" + String.format("%,d", maxTransfer) + " RF/t"));
        tooltip.add(Component.literal("§7Internal Buffer: §e" + String.format("%,d", capacity) + " RF"));
        tooltip.add(Component.literal("§8Right-click face with Wrench to configure:"));
        tooltip.add(Component.literal("§8• Normal (Bidirectional) / Extract / Insert"));
        tooltip.add(Component.literal("§8Shift + Right-Click to dismantle"));
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

    public BlockState makeConnections(BlockGetter level, BlockPos pos) {
        BlockEntity be = (level != null && pos != null) ? level.getBlockEntity(pos) : null;
        VolcaniteEnergyWireBlockEntity wireBe = (be instanceof VolcaniteEnergyWireBlockEntity w) ? w : null;

        return this.defaultBlockState()
            .setValue(UP, connectsTo(level, pos, Direction.UP, wireBe))
            .setValue(DOWN, connectsTo(level, pos, Direction.DOWN, wireBe))
            .setValue(NORTH, connectsTo(level, pos, Direction.NORTH, wireBe))
            .setValue(SOUTH, connectsTo(level, pos, Direction.SOUTH, wireBe))
            .setValue(EAST, connectsTo(level, pos, Direction.EAST, wireBe))
            .setValue(WEST, connectsTo(level, pos, Direction.WEST, wireBe));
    }

    private boolean connectsTo(BlockGetter level, BlockPos pos, Direction dir, @Nullable VolcaniteEnergyWireBlockEntity wireBe) {
        if (wireBe != null && wireBe.isSideDisconnected(dir)) {
            return false;
        }
        BlockPos neighborPos = pos.relative(dir);
        if (level == null) return false;

        BlockEntity neighborBe = level.getBlockEntity(neighborPos);
        if (neighborBe instanceof VolcaniteEnergyWireBlockEntity otherWire && otherWire.isSideDisconnected(dir.getOpposite())) {
            return false;
        }

        BlockState state = level.getBlockState(neighborPos);
        Block block = state.getBlock();
        if (block instanceof VolcaniteEnergyWireBlock || 
            block instanceof VolcaniteGeneratorBlock || 
            block instanceof VolcaniteGrinderBlock || 
            block instanceof AdvancedVolcaniteFurnaceBlock ||
            block instanceof VolcaniteLavaPumpBlock ||
            block instanceof VolcaniteEnergyCellBlock ||
            block instanceof VolcaniteEnergyUnitBlock ||
            block instanceof VolcaniteEnergyCoreBlock ||
            block == ObsidiumMod.VOLCANITE_CASING_BLOCK ||
            block == ObsidiumMod.ADVANCED_VOLCANITE_CASING_BLOCK ||
            block instanceof VolcaniteUniversalConduitBlock ||
            block instanceof VolcaniteSteamGeneratorBlock ||
            block instanceof VolcaniteGeothermalGeneratorBlock ||
            block instanceof SupremeVolcanicGeneratorBlock ||
            block instanceof DemetrianBiomassGeneratorBlock ||
            block instanceof TartarusPistonEngineBlock ||
            block instanceof DionysianBioCalderaBlock ||
            block instanceof VolcaniteStorageControllerBlock) {
            return true;
        }

        if (level instanceof Level world) {
            return team.reborn.energy.api.EnergyStorage.SIDED.find(world, neighborPos, dir.getOpposite()) != null;
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
        if (be instanceof VolcaniteEnergyWireBlockEntity wireBe) {
            // 0. Facade application
            if (stack.getItem() instanceof PipeFacadeItem) {
                if (wireBe.getFacadeBlock() != null) {
                    return ItemInteractionResult.FAIL;
                }
                Block mimic = PipeFacadeItem.getMimicBlock(stack, player);
                if (!level.isClientSide()) {
                    wireBe.setFacadeBlock(mimic);
                    if (player != null && !player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    level.playSound(null, pos, mimic.defaultBlockState().getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                    if (player != null) {
                        player.displayClientMessage(Component.literal("§6[Wire] §aApplied facade: §f" + mimic.getName().getString()), true);
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }

            // Shift + click: Dismantle facade if holding bare hand or wrench
            if (player != null && player.isShiftKeyDown() && wireBe.getFacadeBlock() != null) {
                if (stack.isEmpty() || VolcaniteWrenchItem.isWrench(stack)) {
                    if (!level.isClientSide()) {
                        Block fBlock = wireBe.getFacadeBlock();
                        ItemStack drop = PipeFacadeItem.getFacadeFor(fBlock);
                        if (!drop.isEmpty()) {
                            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
                        }
                        wireBe.setFacadeBlock(null);
                        level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
                        player.displayClientMessage(Component.literal("§6[Wire] §cDismantled wire facade!"), true);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
        }

        if (VolcaniteWrenchItem.isWrench(stack)) {
            if (player != null && player.isShiftKeyDown()) {
                if (!level.isClientSide()) {
                    level.destroyBlock(pos, true, player);
                    player.displayClientMessage(Component.literal("§6[Wrench] §cDismantled " + state.getBlock().getName().getString()), true);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }

            if (be instanceof VolcaniteEnergyWireBlockEntity wireBe) {
                Direction targetFace = getTargetFaceFromHit(hitResult, pos);
                if (!level.isClientSide()) {
                    BlockPos neighborPos = pos.relative(targetFace);
                    BlockEntity neighborBe = level.getBlockEntity(neighborPos);

                    // If clicked face is connected to an external machine/device, cycle Export/Import/Normal mode!
                    if (state.getValue(getProperty(targetFace)) && !(neighborBe instanceof VolcaniteEnergyWireBlockEntity)) {
                        VolcaniteEnergyWireBlockEntity.WireMode next = wireBe.cycleWireMode(targetFace);
                        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, 1.2F);
                        if (player != null) {
                            String modeDesc = switch (next) {
                                case EXTRACT -> "§6EXTRACT / EXPORT (Pull Energy)";
                                case INSERT -> "§bINSERT / IMPORT (Push Energy)";
                                case NORMAL -> "§eNORMAL (Bidirectional)";
                            };
                            player.displayClientMessage(Component.literal("§6[Astrape Wire] §eFace §f" + targetFace.getName().toUpperCase() + " §emode: " + modeDesc), true);
                        }
                    } else {
                        boolean nowDisc = wireBe.toggleSideDisconnected(targetFace);
                        if (nowDisc) {
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
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VolcaniteEnergyWireBlockEntity wireBe && wireBe.getFacadeBlock() != null) {
                if (!level.isClientSide()) {
                    Block fBlock = wireBe.getFacadeBlock();
                    ItemStack drop = PipeFacadeItem.getFacadeFor(fBlock);
                    if (!drop.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
                    }
                    wireBe.setFacadeBlock(null);
                    level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
                    player.displayClientMessage(Component.literal("§6[Wire] §cDismantled wire facade!"), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VolcaniteEnergyWireBlockEntity wireBe && wireBe.getFacadeBlock() != null) {
                ItemStack drop = PipeFacadeItem.getFacadeFor(wireBe.getFacadeBlock());
                if (!drop.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), drop);
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VolcaniteEnergyWireBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ObsidiumMod.VOLCANITE_ENERGY_WIRE_BE ? 
            (BlockEntityTicker<T>) (BlockEntityTicker<VolcaniteEnergyWireBlockEntity>) VolcaniteEnergyWireBlockEntity::tick : null;
    }
}
