package net.valentin.obsidium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Consumes 800 RF/t to suppress natural hostile mob spawns within a 48-block horizontal radius.
 */
public final class AegisSanctuaryWardBlock extends BaseEntityBlock {
    public static final MapCodec<AegisSanctuaryWardBlock> CODEC = simpleCodec(AegisSanctuaryWardBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public AegisSanctuaryWardBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AegisSanctuaryWardBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ObsidiumMod.AEGIS_SANCTUARY_WARD_BE, AegisSanctuaryWardBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AegisSanctuaryWardBlockEntity ward) {
            ContainerData data = new ContainerData() {
                @Override
                public int get(int index) {
                    return switch (index) {
                        case 0 -> (int) (ward.energyStorage.amount / 100L);
                        case 1 -> ward.isActive() ? 1 : 0;
                        case 2 -> ward.isManualEnabled() ? 1 : 0;
                        default -> 0;
                    };
                }

                @Override
                public void set(int index, int value) {}

                @Override
                public int getCount() { return AegisSanctuaryWardBlockEntity.DATA_COUNT; }
            };
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new AegisSanctuaryWardMenu(id, inv, data, ward),
                    Component.translatable("block.obsidium.aegis_sanctuary_ward")));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.obsidium.aegis_sanctuary_ward.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.obsidium.aegis_sanctuary_ward.2").withStyle(ChatFormatting.GOLD));
    }
}
