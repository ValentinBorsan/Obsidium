package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import net.valentin.obsidium.ObsidiumMod;
import net.minecraft.util.RandomSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;

public class VolcaniteGeneratorBlock extends Block implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public VolcaniteGeneratorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VolcaniteGeneratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return type == ObsidiumMod.VOLCANITE_GENERATOR_BE
                ? (BlockEntityTicker<T>) (BlockEntityTicker<VolcaniteGeneratorBlockEntity>) VolcaniteGeneratorBlockEntity::tick
                : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VolcaniteGeneratorBlockEntity gen) {
                player.openMenu(gen);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof InventoryHolder holder) {
                net.minecraft.world.Containers.dropContents(level, pos, holder.getInventory());
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            double d0 = (double) pos.getX() + 0.5D;
            double d1 = (double) pos.getY();
            double d2 = (double) pos.getZ() + 0.5D;
            if (random.nextDouble() < 0.1D) {
                level.playLocalSound(d0, d1, d2, net.valentin.obsidium.sound.ModSounds.GENERATOR_HUM,
                        SoundSource.BLOCKS, 1.0F, 1.0F, false);
            }
            level.addParticle(ParticleTypes.FLAME, d0 + (random.nextDouble() * 0.4 - 0.2), d1 + 0.5,
                    d2 + (random.nextDouble() * 0.4 - 0.2), 0.0D, 0.0D, 0.0D);
            level.addParticle(ParticleTypes.SMOKE, d0 + (random.nextDouble() * 0.4 - 0.2), d1 + 0.9,
                    d2 + (random.nextDouble() * 0.4 - 0.2), 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof VolcaniteGeneratorBlockEntity gen) {
            long amt = gen.energyStorage.amount;
            long cap = gen.energyStorage.capacity;
            if (cap <= 0 || amt <= 0) return 0;
            return (int) Math.floor((double) amt / cap * 14.0) + 1;
        }
        return 0;
    }
}
