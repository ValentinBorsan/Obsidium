package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.valentin.obsidium.item.DemetrianBotanistLensItem;
import net.valentin.obsidium.item.DemetrianGraftingTapeItem;
import net.valentin.obsidium.item.DemetrianScionItem;
import org.jetbrains.annotations.Nullable;

public class GraftedSaplingBlock extends Block implements EntityBlock {
    public enum Stage implements StringRepresentable {
        INCISED("incised"),
        GRAFTED("grafted");

        private final String name;

        Stage(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final EnumProperty<Stage> STAGE = EnumProperty.create("stage", Stage.class);
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);

    public GraftedSaplingBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, Stage.INCISED));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GraftedSaplingBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof GraftedSaplingBlockEntity grafted)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Pass to Botanist Lens
        if (stack.getItem() instanceof DemetrianBotanistLensItem) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        Stage currentStage = state.getValue(STAGE);

        // 1. Applying Scion (when stage is INCISED)
        if (stack.getItem() instanceof DemetrianScionItem) {
            if (currentStage == Stage.INCISED) {
                if (!level.isClientSide()) {
                    String scionSpecies = DemetrianScionItem.getSpecies(stack);
                    grafted.applyScion(stack, scionSpecies);
                    stack.shrink(1);

                    level.playSound(null, pos, SoundEvents.CHERRY_LEAVES_PLACE, SoundSource.BLOCKS, 0.8F, 1.2F);
                    if (level instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 10, 0.2, 0.2, 0.2, 0.02);
                    }

                    if (player != null) {
                        String name = DemetrianScionItem.formatSpeciesName(scionSpecies);
                        player.displayClientMessage(Component.literal("§6[Grafting] §e" + name + " Scion applied! §7Bind with Grafting Tape."), true);
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            } else {
                if (player != null && !level.isClientSide()) {
                    player.displayClientMessage(Component.literal("§6[Grafting] §cThis sapling already has an applied Scion! Bind with Grafting Tape."), true);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        // 2. Sealing with Grafting Tape (when stage is GRAFTED)
        if (stack.getItem() instanceof DemetrianGraftingTapeItem) {
            if (currentStage == Stage.GRAFTED) {
                if (!level.isClientSide()) {
                    stack.shrink(1);

                    String rootSpecies = grafted.getRootstockSpecies();
                    String scionSpecies = grafted.getScionSpecies();
                    Block hybridBlock = GraftingRegistry.resolveHybrid(rootSpecies, scionSpecies);

                    // Morph into the new hybrid sapling!
                    level.setBlock(pos, hybridBlock.defaultBlockState(), 3);

                    level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0F, 1.5F);
                    level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.2F);

                    if (level instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 25, 0.35, 0.35, 0.35, 0.05);
                        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 15, 0.25, 0.25, 0.25, 0.08);
                    }

                    if (player != null) {
                        player.displayClientMessage(Component.literal("§6[Grafting Successful!] §a✦ Metamorphosis complete! Created: §e" + hybridBlock.getName().getString() + "§a!"), true);
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            } else {
                if (player != null && !level.isClientSide()) {
                    player.displayClientMessage(Component.literal("§6[Grafting] §cYou must insert a Scion cutting before binding with tape!"), true);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof GraftedSaplingBlockEntity grafted) {
                grafted.dropGraftContents();
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }
}
