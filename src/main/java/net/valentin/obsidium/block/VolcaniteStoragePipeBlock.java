package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
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
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.*;
import org.jetbrains.annotations.Nullable;

public class VolcaniteStoragePipeBlock extends Block implements SimpleWaterloggedBlock, EntityBlock {
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

    // Standard 6x6 pipe shapes (for Hermes Item Pipe & Universal Conduit)
    private static final VoxelShape CENTER = Block.box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape UP_SHAPE = Block.box(5, 11, 5, 11, 16, 11);
    private static final VoxelShape DOWN_SHAPE = Block.box(5, 0, 5, 11, 5, 11);
    private static final VoxelShape NORTH_SHAPE = Block.box(5, 5, 0, 11, 11, 5);
    private static final VoxelShape SOUTH_SHAPE = Block.box(5, 5, 11, 11, 11, 16);
    private static final VoxelShape WEST_SHAPE = Block.box(0, 5, 5, 5, 11, 11);
    private static final VoxelShape EAST_SHAPE = Block.box(11, 5, 5, 16, 11, 11);

    // Thin 4x4 Fiber-Optic Cable shapes (for Volcanite Optic Fiber)
    private static final VoxelShape FIBER_CENTER = Block.box(6, 6, 6, 10, 10, 10);
    private static final VoxelShape FIBER_UP = Block.box(6, 10, 6, 10, 16, 10);
    private static final VoxelShape FIBER_DOWN = Block.box(6, 0, 6, 10, 6, 10);
    private static final VoxelShape FIBER_NORTH = Block.box(6, 6, 0, 10, 10, 6);
    private static final VoxelShape FIBER_SOUTH = Block.box(6, 6, 10, 10, 10, 16);
    private static final VoxelShape FIBER_WEST = Block.box(0, 6, 6, 6, 10, 10);
    private static final VoxelShape FIBER_EAST = Block.box(10, 6, 6, 16, 10, 10);

    public boolean isOpticFiber() {
        return this.getClass() == VolcaniteStoragePipeBlock.class;
    }

    public static String getChannelTypeDisplay(int type) {
        return switch (type) {
            case VolcaniteStoragePipeBlockEntity.TYPE_ITEM -> "§a📦 ITEMS";
            case VolcaniteStoragePipeBlockEntity.TYPE_FLUID -> "§b💧 FLUIDS";
            case VolcaniteStoragePipeBlockEntity.TYPE_ENERGY -> "§6⚡ ENERGY";
            default -> "§d🌐 OMNI (All)";
        };
    }

    public static void cycleConduitFaceMode(VolcaniteUniversalConduitBlockEntity conduit, Level level, BlockPos pos, Direction targetFace, @Nullable Player player) {
        if (conduit.isSideDisconnected(targetFace)) {
            conduit.setSideDisconnected(targetFace, false);
            conduit.setModule(targetFace, VolcaniteStoragePipeBlockEntity.ModuleType.EXTRACT);
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 1.5F);
            if (player != null) {
                player.displayClientMessage(Component.literal("§6[Wrench] §eSide §f" + targetFace.getName().toUpperCase() + " §7➔ §6📥 IMPORT (Pull)"), true);
            }
            return;
        }
        VolcaniteStoragePipeBlockEntity.ModuleType current = conduit.getModule(targetFace);
        if (current == VolcaniteStoragePipeBlockEntity.ModuleType.EXTRACT) {
            conduit.setModule(targetFace, VolcaniteStoragePipeBlockEntity.ModuleType.INSERT);
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 1.7F);
            if (player != null) {
                player.displayClientMessage(Component.literal("§6[Wrench] §eSide §f" + targetFace.getName().toUpperCase() + " §7➔ §b📤 EXPORT (Push)"), true);
            }
        } else if (current == VolcaniteStoragePipeBlockEntity.ModuleType.INSERT) {
            conduit.setModule(targetFace, VolcaniteStoragePipeBlockEntity.ModuleType.NONE);
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 1.3F);
            if (player != null) {
                player.displayClientMessage(Component.literal("§6[Wrench] §eSide §f" + targetFace.getName().toUpperCase() + " §7➔ §d⇄ BIDIRECTIONAL (Auto)"), true);
            }
        } else {
            conduit.setSideDisconnected(targetFace, true);
            level.playSound(null, pos, SoundEvents.SPYGLASS_STOP_USING, SoundSource.BLOCKS, 1.0F, 1.2F);
            if (player != null) {
                player.displayClientMessage(Component.literal("§6[Wrench] §cSide §f" + targetFace.getName().toUpperCase() + " §c➔ ⛔ DISCONNECTED"), true);
            }
        }
    }

    public static void cycleConduitChannelType(VolcaniteUniversalConduitBlockEntity conduit, Level level, BlockPos pos, Direction targetFace, @Nullable Player player) {
        int current = conduit.getTransferType(targetFace);
        int next = (current + 1) % 4;
        BlockPos neighborPos = pos.relative(targetFace);
        BlockEntity neighborBe = level.getBlockEntity(neighborPos);
        boolean isMachineFace = VolcaniteStoragePipeBlockEntity.isExternalContainer(level, neighborPos, neighborBe);
        if (isMachineFace) {
            conduit.setTransferType(targetFace, next);
            if (player != null) {
                player.displayClientMessage(Component.literal("§6[Conduit] §eSide §f" + targetFace.getName().toUpperCase() + " §etype: " + getChannelTypeDisplay(next)), true);
            }
        } else {
            conduit.setAllFacesTransferType(next);
            if (player != null) {
                player.displayClientMessage(Component.literal("§6[Conduit] §eAll sides set to: " + getChannelTypeDisplay(next)), true);
            }
        }
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 1.3F);
    }

    public static ItemStack getConnectorDropForModule(VolcaniteStoragePipeBlockEntity pipeBe, VolcaniteStoragePipeBlockEntity.ModuleType module) {
        if (pipeBe != null && pipeBe.getClass() == VolcaniteStoragePipeBlockEntity.class) {
            if (module == VolcaniteStoragePipeBlockEntity.ModuleType.INSERT) {
                return new ItemStack(ObsidiumMod.VOLCANITE_EXPORT_CONNECTOR);
            } else if (module == VolcaniteStoragePipeBlockEntity.ModuleType.EXTRACT) {
                return new ItemStack(ObsidiumMod.VOLCANITE_IMPORT_CONNECTOR);
            }
        }
        return new ItemStack(ObsidiumMod.VOLCANITE_LOGISTICS_MODULE);
    }

    public VolcaniteStoragePipeBlock(Properties properties) {
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
        if (isOpticFiber()) {
            VoxelShape shape = FIBER_CENTER;
            if (state.getValue(UP)) shape = Shapes.or(shape, FIBER_UP);
            if (state.getValue(DOWN)) shape = Shapes.or(shape, FIBER_DOWN);
            if (state.getValue(NORTH)) shape = Shapes.or(shape, FIBER_NORTH);
            if (state.getValue(SOUTH)) shape = Shapes.or(shape, FIBER_SOUTH);
            if (state.getValue(WEST)) shape = Shapes.or(shape, FIBER_WEST);
            if (state.getValue(EAST)) shape = Shapes.or(shape, FIBER_EAST);
            return shape;
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
        VolcaniteStoragePipeBlockEntity pipeBe = (be instanceof VolcaniteStoragePipeBlockEntity p) ? p : null;

        return this.defaultBlockState()
            .setValue(UP, connectsToFace(level, pos, Direction.UP, pipeBe))
            .setValue(DOWN, connectsToFace(level, pos, Direction.DOWN, pipeBe))
            .setValue(NORTH, connectsToFace(level, pos, Direction.NORTH, pipeBe))
            .setValue(SOUTH, connectsToFace(level, pos, Direction.SOUTH, pipeBe))
            .setValue(EAST, connectsToFace(level, pos, Direction.EAST, pipeBe))
            .setValue(WEST, connectsToFace(level, pos, Direction.WEST, pipeBe));
    }

    public boolean connectsToFace(BlockGetter level, BlockPos pos, Direction dir, @Nullable VolcaniteStoragePipeBlockEntity pipeBe) {
        if (pipeBe != null && pipeBe.isSideDisconnected(dir)) {
            return false;
        }
        BlockPos neighborPos = pos.relative(dir);
        if (level != null) {
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (neighborBe instanceof VolcaniteStoragePipeBlockEntity neighborPipe) {
                if (neighborPipe.isSideDisconnected(dir.getOpposite())) {
                    return false;
                }
            }
        }
        if (this instanceof VolcaniteUniversalConduitBlock) {
            Block neighborBlock = level != null ? level.getBlockState(neighborPos).getBlock() : null;
            if (neighborBlock instanceof VolcaniteUniversalConduitBlock) {
                return true;
            }
            if (neighborBlock instanceof HermesItemPipeBlock || PipeConnectionHelper.isOpticFiber(neighborBlock)) {
                return false;
            }
            return PipeConnectionHelper.canConnect(level, neighborPos, dir);
        }
        if (isOpticFiber()) {
            return PipeConnectionHelper.canOpticFiberConnect(level, neighborPos, dir, pipeBe);
        }
        return PipeConnectionHelper.canConnect(level, neighborPos, dir);
    }

    public static boolean connectsTo(BlockGetter level, BlockPos pos, Direction dir, @Nullable VolcaniteStoragePipeBlockEntity pipeBe) {
        if (pipeBe != null && pipeBe.isSideDisconnected(dir)) {
            return false;
        }
        BlockPos neighborPos = pos.relative(dir);
        if (level != null) {
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (neighborBe instanceof VolcaniteStoragePipeBlockEntity neighborPipe) {
                if (neighborPipe.isSideDisconnected(dir.getOpposite())) {
                    return false;
                }
            }
        }
        return PipeConnectionHelper.canConnect(level, neighborPos, dir);
    }

    public static boolean connectsTo(BlockGetter level, BlockPos pos) {
        return PipeConnectionHelper.canConnect(level, pos, null);
    }

    public static Direction getTargetFaceFromHit(BlockHitResult hitResult, BlockPos pos) {
        net.minecraft.world.phys.Vec3 hitVec = hitResult.getLocation().subtract(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        double absX = Math.abs(hitVec.x);
        double absY = Math.abs(hitVec.y);
        double absZ = Math.abs(hitVec.z);
        double max = Math.max(absX, Math.max(absY, absZ));

        if (max > 0.18) {
            if (absX == max) return hitVec.x > 0 ? Direction.EAST : Direction.WEST;
            if (absY == max) return hitVec.y > 0 ? Direction.UP : Direction.DOWN;
            return hitVec.z > 0 ? Direction.SOUTH : Direction.NORTH;
        }

        return hitResult.getDirection();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VolcaniteStoragePipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == ObsidiumMod.VOLCANITE_STORAGE_PIPE_BE ?
            (BlockEntityTicker<T>) (BlockEntityTicker<VolcaniteStoragePipeBlockEntity>) VolcaniteStoragePipeBlockEntity::tick : null;
    }

    public static Direction getClickedPipeFace(BlockHitResult hitResult, BlockPos pos, Level level) {
        return getClickedPipeFace(hitResult, pos, level, level.getBlockState(pos));
    }

    public static Direction getClickedPipeFace(BlockHitResult hitResult, BlockPos pos, Level level, BlockState state) {
        // 1. Check all directions for connected external containers/machines
        java.util.List<Direction> containerFaces = new java.util.ArrayList<>();
        for (Direction d : Direction.values()) {
            if (state.getValue(getProperty(d))) {
                BlockPos neighborPos = pos.relative(d);
                BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                if (VolcaniteStoragePipeBlockEntity.isExternalContainer(level, neighborPos, neighborBe)) {
                    containerFaces.add(d);
                }
            }
        }

        // 2. If exactly one container is connected, always target that container face!
        if (containerFaces.size() == 1) {
            return containerFaces.get(0);
        }

        net.minecraft.world.phys.Vec3 hitVec = hitResult.getLocation().subtract(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Direction armHit = getTargetFaceFromHit(hitResult, pos);

        // 3. If multiple containers are connected, pick the one closest to the clicked arm
        if (containerFaces.size() > 1) {
            if (containerFaces.contains(armHit)) {
                return armHit;
            }
            Direction best = containerFaces.get(0);
            double bestScore = -999.0;
            for (Direction d : containerFaces) {
                double score = hitVec.x * d.getStepX() + hitVec.y * d.getStepY() + hitVec.z * d.getStepZ();
                if (score > bestScore) {
                    bestScore = score;
                    best = d;
                }
            }
            return best;
        }

        // 4. If no containers, check any connected pipe face
        java.util.List<Direction> connectedFaces = new java.util.ArrayList<>();
        for (Direction d : Direction.values()) {
            if (state.getValue(getProperty(d))) {
                connectedFaces.add(d);
            }
        }
        if (connectedFaces.size() == 1) {
            return connectedFaces.get(0);
        }
        if (connectedFaces.size() > 1) {
            if (connectedFaces.contains(armHit)) {
                return armHit;
            }
            Direction best = connectedFaces.get(0);
            double bestScore = -999.0;
            for (Direction d : connectedFaces) {
                double score = hitVec.x * d.getStepX() + hitVec.y * d.getStepY() + hitVec.z * d.getStepZ();
                if (score > bestScore) {
                    bestScore = score;
                    best = d;
                }
            }
            return best;
        }

        return hitResult.getDirection();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof VolcaniteStoragePipeBlockEntity pipeBe)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        Item item = stack.getItem();
        Direction targetFace = getClickedPipeFace(hitResult, pos, level, state);

        // 0. Facade application
        if (item instanceof PipeFacadeItem) {
            if (pipeBe.getFacadeBlock() != null) {
                return ItemInteractionResult.FAIL;
            }
            Block mimic = PipeFacadeItem.getMimicBlock(stack, player);
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

        // Sneak + click: Dismantle facade if holding bare hand or wrench
        if (player.isShiftKeyDown() && pipeBe.getFacadeBlock() != null) {
            if (stack.isEmpty() || VolcaniteWrenchItem.isWrench(stack)) {
                if (!level.isClientSide()) {
                    Block fBlock = pipeBe.getFacadeBlock();
                    ItemStack drop = PipeFacadeItem.getFacadeFor(fBlock);
                    if (!drop.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
                    }
                    pipeBe.setFacadeBlock(null);
                    level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
                    if (player != null) {
                        player.displayClientMessage(Component.literal("§6[Pipe] §cDismantled pipe facade!"), true);
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        // Sneak + click: Dismantle existing module or energy upgrade from this face
        if (player.isShiftKeyDown() && !VolcaniteWrenchItem.isWrench(player.getItemInHand(hand))) {
            boolean isUniversal = pipeBe instanceof VolcaniteUniversalConduitBlockEntity;
            if (pipeBe.getModule(targetFace) == VolcaniteStoragePipeBlockEntity.ModuleType.NONE && pipeBe.getEnergyUpgradeTier(targetFace) == 0) {
                for (Direction d : Direction.values()) {
                    if ((!isUniversal && pipeBe.getModule(d) != VolcaniteStoragePipeBlockEntity.ModuleType.NONE) || pipeBe.getEnergyUpgradeTier(d) > 0) {
                        targetFace = d;
                        break;
                    }
                }
            }

            if ((!isUniversal && pipeBe.getModule(targetFace) != VolcaniteStoragePipeBlockEntity.ModuleType.NONE) || pipeBe.getEnergyUpgradeTier(targetFace) > 0) {
                if (!level.isClientSide()) {
                    boolean dismantledSomething = false;
                    if (pipeBe.getEnergyUpgradeTier(targetFace) > 0) {
                        ItemStack upgradeStack = pipeBe.getEnergyUpgradeItem(targetFace);
                        if (!upgradeStack.isEmpty()) {
                            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, upgradeStack);
                        }
                        pipeBe.setEnergyUpgrade(targetFace, 0, ItemStack.EMPTY);
                        player.displayClientMessage(Component.literal("§6[Pipe] §cDismantled Energy Upgrade from §f" + targetFace.getName().toUpperCase() + " §cside!"), true);
                        dismantledSomething = true;
                    }

                    if (!isUniversal && pipeBe.getModule(targetFace) != VolcaniteStoragePipeBlockEntity.ModuleType.NONE) {
                        ItemStack toDrop = getConnectorDropForModule(pipeBe, pipeBe.getModule(targetFace));
                        pipeBe.setValveModule(targetFace, false);
                        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, toDrop);

                        for (int s = 0; s < 3; s++) {
                            ItemStack filter = pipeBe.getFilterItem(targetFace, s);
                            if (!filter.isEmpty()) {
                                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, filter.copy());
                            }
                        }
                        pipeBe.clearFilters(targetFace);
                        pipeBe.setModule(targetFace, VolcaniteStoragePipeBlockEntity.ModuleType.NONE);
                        BlockState updated = makeConnections(level, pos).setValue(WATERLOGGED, state.getValue(WATERLOGGED));
                        level.setBlock(pos, updated, 3);
                        player.displayClientMessage(Component.literal("§6[Optic Fiber] §cDismantled connector from §f" + targetFace.getName().toUpperCase() + " §cside!"), true);
                        dismantledSomething = true;
                    }

                    if (dismantledSomething) {
                        level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 1.0F, 0.8F);
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        // 0a. CABLE CONNECTOR (Volcanite Import Connector / Volcanite Export Connector)
        if (item instanceof VolcaniteCableConnectorItem connectorItem) {
            Direction connectorFace = VolcaniteCableConnectorItem.findBestContainerFace(level, pos, hitResult.getDirection(), pipeBe);
            if (connectorFace == null) {
                if (!level.isClientSide() && player != null) {
                    player.displayClientMessage(Component.literal("§c[Fiber Connector] No adjacent chest or machine found to connect!"), true);
                }
                return ItemInteractionResult.FAIL;
            }
            connectorItem.attachConnector(level, pos, pipeBe, connectorFace, player, stack);
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        // 0b. ENERGY / OVERCLOCK / STACK UPGRADE
        if (item instanceof VolcanitePipeEnergyUpgradeItem || item instanceof VolcaniteOverclockUpgradeItem || item instanceof VolcaniteStackUpgradeItem) {
            if (isOpticFiber() && item instanceof VolcanitePipeEnergyUpgradeItem) {
                if (!level.isClientSide() && player != null) {
                    player.displayClientMessage(Component.literal("§c[Optic Fiber] Fiber cables are data-only and do not transmit RF energy! Use Stack/Overclock upgrades on connectors."), true);
                }
                return ItemInteractionResult.FAIL;
            }
            if (!state.getValue(getProperty(targetFace))) {
                for (Direction d : Direction.values()) {
                    if (state.getValue(getProperty(d))) {
                        targetFace = d;
                        break;
                    }
                }
            }
            if (!state.getValue(getProperty(targetFace)) && !(pipeBe instanceof VolcaniteUniversalConduitBlockEntity)) {
                if (!level.isClientSide() && player != null) {
                    player.displayClientMessage(Component.literal("§c[Pipe] Cannot mount upgrade on a disconnected face!"), true);
                }
                return ItemInteractionResult.FAIL;
            }

            if (!level.isClientSide()) {
                int newTier;
                String tierName;
                if (item instanceof VolcanitePipeEnergyUpgradeItem upg) {
                    newTier = upg.getTier();
                    tierName = upg.getTierName() + " Energy Upgrade";
                } else if (item instanceof VolcaniteOverclockUpgradeItem oc) {
                    newTier = oc.getTier();
                    tierName = stack.getHoverName().getString();
                } else {
                    newTier = 3;
                    tierName = "Stack Upgrade";
                }

                int currentTier = pipeBe.getEnergyUpgradeTier(targetFace);
                ItemStack currentStack = pipeBe.getEnergyUpgradeItem(targetFace);

                if (currentTier >= newTier && !currentStack.isEmpty()) {
                    if (player != null) {
                        player.displayClientMessage(Component.literal("§e[Pipe] §7" + targetFace.getName().toUpperCase() + " side already has " + currentStack.getHoverName().getString()), true);
                    }
                    return ItemInteractionResult.sidedSuccess(false);
                }

                if (!currentStack.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, currentStack);
                }

                pipeBe.setEnergyUpgrade(targetFace, newTier, stack.copyWithCount(1));
                if (player != null && !player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.BLOCKS, 1.0F, 1.2F);
                if (player != null) {
                    long maxRf = pipeBe.getMaxEnergyTransfer(targetFace);
                    long maxFluid = pipeBe.getMaxFluidTransfer(targetFace);
                    int maxItems = pipeBe.getMaxItemTransfer(targetFace);
                    player.displayClientMessage(Component.literal("§6[Conduit] §aInstalled §e" + tierName + " §aon §f" + targetFace.getName().toUpperCase() + " §a(⚡ " + String.format("%,d", maxRf) + " RF/t | 💧 " + String.format("%,d", maxFluid) + " mB/t | 📦 " + maxItems + " items)"), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        // 3. WRENCH
        if (VolcaniteWrenchItem.isWrench(player.getItemInHand(hand))) {
            targetFace = getTargetFaceFromHit(hitResult, pos);
            if (pipeBe instanceof VolcaniteUniversalConduitBlockEntity conduit) {
                if (!level.isClientSide()) {
                    if (player.isShiftKeyDown()) {
                        cycleConduitChannelType(conduit, level, pos, targetFace, player);
                    } else {
                        cycleConduitFaceMode(conduit, level, pos, targetFace, player);
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }

            if (player.isShiftKeyDown()) {
                if (!level.isClientSide()) {
                    level.destroyBlock(pos, true, player);
                    player.displayClientMessage(Component.literal("§6[Wrench] §cDismantled " + state.getBlock().getName().getString()), true);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }

            if (!level.isClientSide()) {
                if (pipeBe.getModule(targetFace) != VolcaniteStoragePipeBlockEntity.ModuleType.NONE) {
                    VolcaniteStoragePipeBlockEntity.ModuleType current = pipeBe.getModule(targetFace);
                    VolcaniteStoragePipeBlockEntity.ModuleType next = (current == VolcaniteStoragePipeBlockEntity.ModuleType.EXTRACT) ?
                        VolcaniteStoragePipeBlockEntity.ModuleType.INSERT : VolcaniteStoragePipeBlockEntity.ModuleType.EXTRACT;

                    pipeBe.setModule(targetFace, next);
                    level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 1.6F);
                    if (player != null) {
                        String modeText = (next == VolcaniteStoragePipeBlockEntity.ModuleType.EXTRACT) ? "§6EXTRACT (into network)" : "§bINSERT (into container)";
                        player.displayClientMessage(Component.literal("§6[Wrench] §eSide §f" + targetFace.getName().toUpperCase() + " §emode set to " + modeText), true);
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

        // 4. Open Menu if clicking mounted face or any mounted module / energy upgrade
        Direction faceToOpen = null;
        if (pipeBe.getModule(targetFace) != VolcaniteStoragePipeBlockEntity.ModuleType.NONE || pipeBe.getEnergyUpgradeTier(targetFace) > 0) {
            faceToOpen = targetFace;
        } else {
            for (Direction d : Direction.values()) {
                if (pipeBe.getModule(d) != VolcaniteStoragePipeBlockEntity.ModuleType.NONE || pipeBe.getEnergyUpgradeTier(d) > 0) {
                    faceToOpen = d;
                    break;
                }
            }
        }

        if (faceToOpen != null) {
            if (!level.isClientSide()) {
                pipeBe.setSelectedFaceForMenu(faceToOpen);
                player.openMenu(pipeBe);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof VolcaniteStoragePipeBlockEntity pipeBe) {
            // Shift + click with bare hand: Dismantle facade first if present
            if (player.isShiftKeyDown() && pipeBe.getFacadeBlock() != null) {
                if (!level.isClientSide()) {
                    Block fBlock = pipeBe.getFacadeBlock();
                    ItemStack drop = PipeFacadeItem.getFacadeFor(fBlock);
                    if (!drop.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
                    }
                    pipeBe.setFacadeBlock(null);
                    level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
                    player.displayClientMessage(Component.literal("§6[Pipe] §cDismantled pipe facade!"), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }

            Direction targetFace = getClickedPipeFace(hitResult, pos, level, state);

            // Sneak + click: Dismantle existing module or upgrade from this face
            if (player.isShiftKeyDown()) {
                boolean isUniversal = pipeBe instanceof VolcaniteUniversalConduitBlockEntity;
                if (pipeBe.getModule(targetFace) == VolcaniteStoragePipeBlockEntity.ModuleType.NONE && pipeBe.getEnergyUpgradeTier(targetFace) == 0) {
                    for (Direction d : Direction.values()) {
                        if ((!isUniversal && pipeBe.getModule(d) != VolcaniteStoragePipeBlockEntity.ModuleType.NONE) || pipeBe.getEnergyUpgradeTier(d) > 0) {
                            targetFace = d;
                            break;
                        }
                    }
                }

                if ((!isUniversal && pipeBe.getModule(targetFace) != VolcaniteStoragePipeBlockEntity.ModuleType.NONE) || pipeBe.getEnergyUpgradeTier(targetFace) > 0) {
                    if (!level.isClientSide()) {
                        boolean dismantledSomething = false;
                        if (pipeBe.getEnergyUpgradeTier(targetFace) > 0) {
                            ItemStack upgradeStack = pipeBe.getEnergyUpgradeItem(targetFace);
                            if (!upgradeStack.isEmpty()) {
                                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, upgradeStack);
                            }
                            pipeBe.setEnergyUpgrade(targetFace, 0, ItemStack.EMPTY);
                            player.displayClientMessage(Component.literal("§6[Pipe] §cDismantled Energy Upgrade from §f" + targetFace.getName().toUpperCase() + " §cside!"), true);
                            dismantledSomething = true;
                        }

                        if (!isUniversal && pipeBe.getModule(targetFace) != VolcaniteStoragePipeBlockEntity.ModuleType.NONE) {
                            ItemStack toDrop = getConnectorDropForModule(pipeBe, pipeBe.getModule(targetFace));
                            pipeBe.setValveModule(targetFace, false);
                            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, toDrop);

                            for (int s = 0; s < 3; s++) {
                                ItemStack filter = pipeBe.getFilterItem(targetFace, s);
                                if (!filter.isEmpty()) {
                                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, filter.copy());
                                }
                            }
                            pipeBe.clearFilters(targetFace);
                            pipeBe.setModule(targetFace, VolcaniteStoragePipeBlockEntity.ModuleType.NONE);
                            BlockState updated = makeConnections(level, pos).setValue(WATERLOGGED, state.getValue(WATERLOGGED));
                            level.setBlock(pos, updated, 3);
                            player.displayClientMessage(Component.literal("§6[Optic Fiber] §cDismantled connector from §f" + targetFace.getName().toUpperCase() + " §cside!"), true);
                            dismantledSomething = true;
                        }

                        if (dismantledSomething) {
                            level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 1.0F, 0.8F);
                        }
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide());
                }
            }

            Direction faceToOpen = null;
            if (pipeBe.getModule(targetFace) != VolcaniteStoragePipeBlockEntity.ModuleType.NONE || pipeBe.getEnergyUpgradeTier(targetFace) > 0) {
                faceToOpen = targetFace;
            } else {
                for (Direction d : Direction.values()) {
                    if (pipeBe.getModule(d) != VolcaniteStoragePipeBlockEntity.ModuleType.NONE || pipeBe.getEnergyUpgradeTier(d) > 0) {
                        faceToOpen = d;
                        break;
                    }
                }
            }
            // Universal Conduit can always open its GUI on any connected face (or clicked face) so players can insert Energy Upgrades directly!
            if (faceToOpen == null && pipeBe instanceof VolcaniteUniversalConduitBlockEntity) {
                faceToOpen = targetFace;
            }

            if (faceToOpen != null) {
                if (!level.isClientSide()) {
                    pipeBe.setSelectedFaceForMenu(faceToOpen);
                    player.openMenu(pipeBe);
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
            if (be instanceof VolcaniteStoragePipeBlockEntity pipeBe) {
                if (pipeBe.getFacadeBlock() != null) {
                    ItemStack drop = PipeFacadeItem.getFacadeFor(pipeBe.getFacadeBlock());
                    if (!drop.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), drop);
                    }
                }
                for (Direction d : Direction.values()) {
                    VolcaniteStoragePipeBlockEntity.ModuleType m = pipeBe.getModule(d);
                    if (m != VolcaniteStoragePipeBlockEntity.ModuleType.NONE && !(pipeBe instanceof VolcaniteUniversalConduitBlockEntity)) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), getConnectorDropForModule(pipeBe, m));
                    }
                    ItemStack upgrade = pipeBe.getEnergyUpgradeItem(d);
                    if (!upgrade.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), upgrade);
                    }
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
