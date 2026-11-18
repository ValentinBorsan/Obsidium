package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.QuantumLinkModuleItem;
import net.valentin.obsidium.item.VolcaniteBottleItem;
import net.valentin.obsidium.item.VolcaniteWrenchItem;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.TooltipFlag;
import java.util.List;
import net.minecraft.network.chat.Component;

public class VolcanicWoodLiquidBarrelBlock extends LiquidTankBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LINKED = BooleanProperty.create("linked");
    private final boolean reinforced;

    public VolcanicWoodLiquidBarrelBlock(Properties properties, boolean reinforced) {
        super(properties);
        this.reinforced = reinforced;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LINKED, false));
    }

    public VolcanicWoodLiquidBarrelBlock(Properties properties) {
        this(properties, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LINKED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(LINKED, false);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return reinforced ? new ReinforcedVolcanicLiquidBarrelBlockEntity(pos, state) : new VolcanicWoodLiquidBarrelBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        BlockEntityType<?> expected = reinforced ? ObsidiumMod.REINFORCED_VOLCANIC_LIQUID_BARREL_BE : ObsidiumMod.VOLCANIC_WOOD_LIQUID_BARREL_BE;
        return type == expected ?
                (BlockEntityTicker<T>) (BlockEntityTicker<LiquidTankBlockEntity>) LiquidTankBlockEntity::tick : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof LiquidTankBlockEntity tank) {
            // 0. Quantum Link Module install / Dye recolor / Wrench unlink
            if (stack.is(ObsidiumMod.QUANTUM_LINK_MODULE) && !tank.isQuantumLinked()) {
                if (!level.isClientSide()) {
                    tank.setQuantumLinked(true, player.getUUID());
                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }
                    level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.2F);
                    player.displayClientMessage(Component.literal("§d[Quantum Barrel] §aLinked to Channel #" + tank.getQuantumChannel() + " (" + QuantumLinkModuleItem.getColorDisplayName(tank.getQuantumColor()) + ", " + (tank.isQuantumPrivate() ? "Private" : "Public") + ")"), true);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
            if (tank.isQuantumLinked() && stack.getItem() instanceof DyeItem dyeItem) {
                if (!level.isClientSide()) {
                    tank.ensureQuantumOwner(player.getUUID());
                    tank.setQuantumColor(dyeItem.getDyeColor());
                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }
                    level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                    player.displayClientMessage(Component.literal("§d[Quantum Barrel] §fColor set to §e" + QuantumLinkModuleItem.getColorDisplayName(dyeItem.getDyeColor()) + " §7(Ch #" + tank.getQuantumChannel() + ")"), true);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
            if (tank.isQuantumLinked() && player.isShiftKeyDown() && VolcaniteWrenchItem.isWrench(stack)) {
                if (!level.isClientSide()) {
                    tank.setQuantumLinked(false, null);
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(ObsidiumMod.QUANTUM_LINK_MODULE));
                    level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.BLOCKS, 0.9F, 1.1F);
                    player.displayClientMessage(Component.literal("§d[Quantum Barrel] §eQuantum Link Module removed!"), true);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }

            // 1. Water Bucket Insert
            if (stack.is(Items.WATER_BUCKET)) {
                if (tank.canAccept(FluidVariant.of(Fluids.WATER)) && tank.getFluidAmount() + 1000 <= tank.getMaxCapacity()) {
                    if (!level.isClientSide()) {
                        tank.insertFluid(FluidVariant.of(Fluids.WATER), 1000, false);
                        if (!player.getAbilities().instabuild) {
                            player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                        }
                        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
            // 2. Lava Bucket Insert
            else if (stack.is(Items.LAVA_BUCKET)) {
                if (tank.canAccept(FluidVariant.of(Fluids.LAVA)) && tank.getFluidAmount() + 1000 <= tank.getMaxCapacity()) {
                    if (!level.isClientSide()) {
                        tank.insertFluid(FluidVariant.of(Fluids.LAVA), 1000, false);
                        if (!player.getAbilities().instabuild) {
                            player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                        }
                        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
            // 3. Pyroclastic Distillate Bucket Insert
            else if (stack.is(ObsidiumMod.PYROCLASTIC_DISTILLATE_BUCKET)) {
                if (tank.canAccept(FluidVariant.of(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID)) && tank.getFluidAmount() + 1000 <= tank.getMaxCapacity()) {
                    if (!level.isClientSide()) {
                        tank.insertFluid(FluidVariant.of(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID), 1000, false);
                        if (!player.getAbilities().instabuild) {
                            player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                        }
                        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
            // 4. Steam Bucket Insert
            else if (stack.is(ObsidiumMod.STEAM_BUCKET)) {
                if (tank.canAccept(FluidVariant.of(ObsidiumMod.STEAM_FLUID)) && tank.getFluidAmount() + 1000 <= tank.getMaxCapacity()) {
                    if (!level.isClientSide()) {
                        tank.insertFluid(FluidVariant.of(ObsidiumMod.STEAM_FLUID), 1000, false);
                        if (!player.getAbilities().instabuild) {
                            player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                        }
                        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
            // 4a. Dew Droplet Insert -> Water (250 mB per droplet)
            else if (stack.is(ObsidiumMod.DEW_DROPLET)) {
                if (tank.canAccept(FluidVariant.of(Fluids.WATER)) && tank.getFluidAmount() + 250 <= tank.getMaxCapacity()) {
                    if (!level.isClientSide()) {
                        int space = tank.getMaxCapacity() - tank.getFluidAmount();
                        int maxDroplets = space / 250;
                        int toUse = player != null && player.isShiftKeyDown() ? Math.min(stack.getCount(), maxDroplets) : 1;
                        int addedAmount = toUse * 250;

                        tank.insertFluid(FluidVariant.of(Fluids.WATER), addedAmount, false);
                        if (player != null && !player.getAbilities().instabuild) {
                            stack.shrink(toUse);
                        }
                        level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.3F);
                        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH,
                                    pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 8, 0.2, 0.1, 0.2, 0.05);
                        }
                        if (player != null) {
                            player.displayClientMessage(Component.literal("§b[Barrel] §a+" + addedAmount + " mB Water §7(" + tank.getFluidAmount() + "/" + tank.getMaxCapacity() + " mB)"), true);
                        }
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
            // 4b. Magma Droplet Insert -> Lava (250 mB per droplet)
            else if (stack.is(ObsidiumMod.MAGMA_DROPLET)) {
                if (tank.canAccept(FluidVariant.of(Fluids.LAVA)) && tank.getFluidAmount() + 250 <= tank.getMaxCapacity()) {
                    if (!level.isClientSide()) {
                        int space = tank.getMaxCapacity() - tank.getFluidAmount();
                        int maxDroplets = space / 250;
                        int toUse = player != null && player.isShiftKeyDown() ? Math.min(stack.getCount(), maxDroplets) : 1;
                        int addedAmount = toUse * 250;

                        tank.insertFluid(FluidVariant.of(Fluids.LAVA), addedAmount, false);
                        if (player != null && !player.getAbilities().instabuild) {
                            stack.shrink(toUse);
                        }
                        level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.7F, 1.3F);
                        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                                    pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 6, 0.2, 0.1, 0.2, 0.02);
                        }
                        if (player != null) {
                            player.displayClientMessage(Component.literal("§6[Barrel] §a+" + addedAmount + " mB Lava §7(" + tank.getFluidAmount() + "/" + tank.getMaxCapacity() + " mB)"), true);
                        }
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
            // 5. Pyroclastic Flask Insert (250 mB)
            else if (stack.is(ObsidiumMod.PYROCLASTIC_FLASK)) {
                if (tank.canAccept(FluidVariant.of(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID)) && tank.getFluidAmount() + 250 <= tank.getMaxCapacity()) {
                    if (!level.isClientSide()) {
                        tank.insertFluid(FluidVariant.of(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID), 250, false);
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                            if (stack.isEmpty()) {
                                player.setItemInHand(hand, bottle);
                            } else if (!player.getInventory().add(bottle)) {
                                player.drop(bottle, false);
                            }
                        }
                        level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
            // 6. Volcanite Bottle Insert
            else if (stack.getItem() instanceof VolcaniteBottleItem) {
                FluidVariant bFluid = VolcaniteBottleItem.getFluid(stack);
                int bAmount = VolcaniteBottleItem.getFluidAmount(stack);
                if (!bFluid.isBlank() && bAmount > 0 && tank.canAccept(bFluid) && tank.getFluidAmount() < tank.getMaxCapacity()) {
                    if (!level.isClientSide()) {
                        int toTransfer = Math.min(bAmount, tank.getMaxCapacity() - tank.getFluidAmount());
                        tank.insertFluid(bFluid, toTransfer, false);
                        int rem = bAmount - toTransfer;
                        VolcaniteBottleItem.setFluid(stack, rem > 0 ? bFluid : FluidVariant.blank(), Math.max(0, rem));
                        level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
            // 7. Empty Bucket Extract (1000 mB)
            else if (stack.is(Items.BUCKET) && tank.getFluidAmount() >= 1000 && !tank.getStoredFluid().isBlank()) {
                Fluid fluid = tank.getStoredFluid().getFluid();
                Item bucketItem = net.valentin.obsidium.fluid.FluidBucketMapping.filledBucket(tank.getStoredFluid());
                if (bucketItem != null && bucketItem != Items.AIR) {
                    if (!level.isClientSide()) {
                        tank.extractFluid(tank.getStoredFluid(), 1000, false);
                        ItemStack filledBucket = new ItemStack(bucketItem);
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                            if (stack.isEmpty()) {
                                player.setItemInHand(hand, filledBucket);
                            } else if (!player.getInventory().add(filledBucket)) {
                                player.drop(filledBucket, false);
                            }
                        }
                        level.playSound(null, pos, (fluid == Fluids.LAVA) ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
            // 8. Empty Glass Bottle Extract (250 mB)
            else if (stack.is(Items.GLASS_BOTTLE) && tank.getFluidAmount() >= 250 && !tank.getStoredFluid().isBlank() && !tank.getStoredFluid().hasComponents()) {
                Fluid fluid = tank.getStoredFluid().getFluid();
                ItemStack result = ItemStack.EMPTY;
                if (fluid == Fluids.WATER) {
                    result = PotionContents.createItemStack(Items.POTION, Potions.WATER);
                } else if (fluid == ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID) {
                    result = new ItemStack(ObsidiumMod.PYROCLASTIC_FLASK);
                }
                if (!result.isEmpty()) {
                    if (!level.isClientSide()) {
                        tank.extractFluid(tank.getStoredFluid(), 250, false);
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                            if (stack.isEmpty()) {
                                player.setItemInHand(hand, result);
                            } else if (!player.getInventory().add(result)) {
                                player.drop(result, false);
                            }
                        }
                        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof LiquidTankBlockEntity tank) {
                tank.ensureQuantumOwner(player.getUUID());
                player.openMenu(tank);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (reinforced) {
            tooltip.add(Component.literal("§6⚡ Capacity: §f256,000 mB §8(256 Buckets)"));
            tooltip.add(Component.literal("§eReinforced with Pyric Bronze bands"));
        } else {
            tooltip.add(Component.literal("§6⚡ Capacity: §f64,000 mB §8(64 Buckets)"));
            tooltip.add(Component.literal("§7Crafted from charred volcanic wood"));
        }
        tooltip.add(Component.literal("§8Right-click with bucket to fill/empty"));
        tooltip.add(Component.literal("§8Right-click empty-handed to open GUI"));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof LiquidTankBlockEntity tank && tank.getMaxCapacity() > 0) {
            return (int) Math.floor(((double) tank.getFluidAmount() / tank.getMaxCapacity()) * 15.0);
        }
        return 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof LiquidTankBlockEntity tank && tank.isQuantumLinked()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(ObsidiumMod.QUANTUM_LINK_MODULE));
            }
            if (blockEntity instanceof InventoryHolder holder) {
                Containers.dropContents(level, pos, holder.getInventory());
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
