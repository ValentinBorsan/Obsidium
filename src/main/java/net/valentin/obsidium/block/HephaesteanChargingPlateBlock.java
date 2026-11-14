package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HephaesteanChargingPlateBlock extends Block implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    protected static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 2.0, 15.0);

    public HephaesteanChargingPlateBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HephaesteanChargingPlateBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ObsidiumMod.HEPHAESTEAN_CHARGING_PLATE_BE ?
            (lvl, pos, st, be) -> {
                if (be instanceof HephaesteanChargingPlateBlockEntity plateBe) {
                    HephaesteanChargingPlateBlockEntity.tick(lvl, pos, st, plateBe);
                }
            } : null;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hitResult) {
        if (!stack.isEmpty()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HephaesteanChargingPlateBlockEntity plate) {
                if (stack.getItem() instanceof team.reborn.energy.api.base.SimpleEnergyItem energyItem) {
                    if (!level.isClientSide()) {
                        long cap = energyItem.getEnergyCapacity(stack);
                        long cur = energyItem.getStoredEnergy(stack);
                        long needed = cap - cur;
                        if (needed <= 0) {
                            player.displayClientMessage(Component.literal("§a✔ Item is already fully charged! (" + String.format("%,d", cur) + " RF)"), true);
                        } else if (plate.energyStorage.amount <= 0) {
                            player.displayClientMessage(Component.literal("§cCharging Plate has no stored energy! (0 / " + String.format("%,d", plate.energyStorage.capacity) + " RF)"), true);
                        } else {
                            long toCharge = plate.chargeStack(stack, 100_000L);
                            if (toCharge > 0) {
                                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.6F);
                                if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                                    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 10, 0.2, 0.2, 0.2, 0.05);
                                }
                                player.displayClientMessage(Component.literal("§6⚡ Charged §b+" + String.format("%,d", toCharge) + " RF §7(" + String.format("%,d", cur + toCharge) + " / " + String.format("%,d", cap) + " RF)"), true);
                            }
                        }
                    }
                    return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.phys.BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HephaesteanChargingPlateBlockEntity plate) {
                player.displayClientMessage(Component.literal("§6[Hephaestean Induction Plate] §eEnergy: §f" +
                        String.format("%,d", plate.energyStorage.amount) + " §7/ §e" + String.format("%,d", plate.energyStorage.capacity) + " RF"), true);
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 1.8F);
            }
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof HephaesteanChargingPlateBlockEntity plate) {
            long amt = plate.energyStorage.amount;
            long cap = plate.energyStorage.capacity;
            if (cap <= 0 || amt <= 0) return 0;
            return (int) Math.floor((double) amt / cap * 14.0) + 1;
        }
        return 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6★ Hephaestean Induction Plate"));
        tooltip.add(Component.literal("§7Step on to wirelessly charge armor, tools & batteries."));
        tooltip.add(Component.literal("§aMax Transfer: §f50,000 RF/t"));
        tooltip.add(Component.literal("§eInternal Buffer: §f1,000,000 RF"));
    }
}

