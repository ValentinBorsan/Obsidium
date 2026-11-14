package net.valentin.obsidium.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.teleport.VolcaniteTeleportNetworkState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Quantum Teleporter Pad block supporting two tiers:
 * - Basic Volcanic Teleporter (2M RF, 2,500-block intra-dimensional range)
 * - Volcanic Core Teleporter (20M RF, infinite cross-dimensional range)
 */
public class VolcanicTeleporterBlock extends BaseEntityBlock {
    public static final MapCodec<VolcanicTeleporterBlock> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    propertiesCodec(),
                    Codec.BOOL.fieldOf("supreme").forGetter(b -> b.supreme)
            ).apply(instance, VolcanicTeleporterBlock::new)
    );

    public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;
    private static final VoxelShape PAD_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);

    private final boolean supreme;

    public VolcanicTeleporterBlock(Properties properties, boolean supreme) {
        super(properties);
        this.supreme = supreme;
        registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    public boolean isSupreme() {
        return supreme;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return PAD_SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof VolcanicTeleporterBlockEntity tp) {
            if (placer instanceof Player player) {
                tp.initializePlacement(player);
            } else {
                tp.registerInNetwork();
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof VolcanicTeleporterBlockEntity tp) {
                tp.registerInNetwork();
                serverPlayer.openMenu(tp);
                if (serverPlayer.server != null) {
                    VolcaniteTeleportNetworkState.get(serverPlayer.server).sendSyncToPlayer(serverPlayer, tp, null);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide() && level.getServer() != null) {
                VolcaniteTeleportNetworkState.get(level.getServer()).removePad(level.dimension().location().toString(), pos);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VolcanicTeleporterBlockEntity(pos, state, this.supreme);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        BlockEntityType<?> expected = this.supreme ? ObsidiumMod.VOLCANIC_CORE_TELEPORTER_BE : ObsidiumMod.BASIC_VOLCANIC_TELEPORTER_BE;
        return level.isClientSide() ? null : createTickerHelper(type, expected,
                (lvl, p, st, be) -> VolcanicTeleporterBlockEntity.tick(lvl, p, st, (VolcanicTeleporterBlockEntity) be));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (this.supreme) {
            tooltip.add(Component.literal("▸ Infinite Cross-Dimensional Quantum Teleporter Pad").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal("▸ Buffer: 20,000,000 RF (250k RF/t In)").withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.literal("▸ Intra-Dimensional Teleporter Pad (2,500m Range)").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal("▸ Buffer: 2,000,000 RF (25k RF/t In)").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.literal("▸ Sneak on Pad for 1.25s to Quick-Warp to Pinned Target").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("▸ Passively charges held RF gear & Warp Gauntlets while standing").withStyle(ChatFormatting.DARK_GRAY));
    }
}

