package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

public class VolcanicOreCropBlock extends CropBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
    private static final VoxelShape[] SHAPES = new VoxelShape[]{
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D)
    };

    private final Supplier<Item> seedSupplier;
    private final Supplier<Item> dropSupplier;
    private final int minDrop;
    private final int maxDrop;

    public VolcanicOreCropBlock(Properties properties, Supplier<Item> seedSupplier, Supplier<Item> dropSupplier, int minDrop, int maxDrop) {
        super(properties);
        this.seedSupplier = seedSupplier;
        this.dropSupplier = dropSupplier;
        this.minDrop = minDrop;
        this.maxDrop = maxDrop;
    }

    public VolcanicOreCropBlock(Properties properties, Supplier<Item> seedSupplier, Supplier<Item> dropSupplier) {
        this(properties, seedSupplier, dropSupplier, 3, 6);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return this.seedSupplier.get();
    }

    public static boolean isValidSoil(BlockState state) {
        return state.is(Blocks.FARMLAND) || state.is(net.minecraft.tags.BlockTags.DIRT) || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.NETHERRACK) || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL)
                || state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return isValidSoil(state);
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        // Volcanic flora is thermally sustained - thrives in pitch darkness, under roofs, in caves, and never pops off!
        BlockPos below = pos.below();
        return isValidSoil(level.getBlockState(below));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(this.getAgeProperty())];
    }

    public boolean harvest(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        int age = this.getAge(state);
        if (age >= this.getMaxAge()) {
            if (!level.isClientSide()) {
                // Harvest drops ONLY primary resource (3 to 6 petals), no seeds
                int count = minDrop + level.random.nextInt(maxDrop - minDrop + 1);
                ItemStack drops = new ItemStack(this.dropSupplier.get(), count);

                if (player != null && player.getInventory().add(drops)) {
                    // Added directly to player inventory, nothing on the ground!
                } else if (!drops.isEmpty()) {
                    popResource(level, pos, drops);
                }

                // Reset crop to age 0 (right-click harvest)
                level.setBlock(pos, this.getStateForAge(0), 2);
                level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return true;
        }
        return false;
    }

    public boolean harvest(Level level, BlockPos pos, BlockState state) {
        return harvest(level, pos, state, null);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return this.getAge(state) < this.getMaxAge();
    }

    public boolean isRareResourceCrop() {
        return seedSupplier.get() == net.valentin.obsidium.ObsidiumMod.DIAMOND_SEED
                || seedSupplier.get() == net.valentin.obsidium.ObsidiumMod.NETHERITE_SEED;
    }

    @Override
    protected int getBonemealAgeIncrease(Level level) {
        return isRareResourceCrop() ? 1 : super.getBonemealAgeIncrease(level);
    }

    @Override
    public void randomTick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        int age = this.getAge(state);
        if (age < this.getMaxAge()) {
            boolean thermalBoost = isThermalEnvironment(level, pos);
            // Normal growth is chance 1 in 12, with thermal boost it is 1 in 5 (more than 2x faster!)
            int chance = (thermalBoost ? 5 : 12) * (isRareResourceCrop()
                    ? net.valentin.obsidium.config.ModConfig.get().getRareResourceGrowthMultiplier() : 1);
            if (random.nextInt(chance) == 0) {
                level.setBlock(pos, this.getStateForAge(age + 1), 2);
                if (thermalBoost && random.nextFloat() < 0.4f) {
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 3, 0.15, 0.15, 0.15, 0.02);
                }
            }
        }
    }

    public static boolean isThermalEnvironment(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (below.is(Blocks.MAGMA_BLOCK) || below.is(Blocks.NETHERRACK) || below.is(Blocks.OBSIDIAN) || below.is(Blocks.CRYING_OBSIDIAN)) {
            return true;
        }
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockState adj = level.getBlockState(pos.relative(dir));
            if (adj.is(Blocks.LAVA) || adj.is(Blocks.MAGMA_BLOCK) || adj.getFluidState().is(net.minecraft.world.level.material.Fluids.LAVA)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (harvest(level, pos, state, player)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        // Magmatic Agriculture: Volcanic Ash as sacred super-fertilizer!
        if (stack.is(net.valentin.obsidium.ObsidiumMod.VOLCANIC_ASH) && this.getAge(state) < this.getMaxAge()) {
            if (!level.isClientSide()) {
                int boost = isRareResourceCrop() ? 1 : 2 + level.random.nextInt(2); // Advances by 2-3 growth stages!
                int newAge = Math.min(this.getMaxAge(), this.getAge(state) + boost);
                level.setBlock(pos, this.getStateForAge(newAge), 2);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.85F, 1.4F);
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 12, 0.2, 0.2, 0.2, 0.04);
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 8, 0.15, 0.15, 0.15, 0.02);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    public Item getDropItem() {
        return this.dropSupplier.get();
    }

    public Item getSeedItem() {
        return this.seedSupplier.get();
    }

    public int getMinDrop() {
        return minDrop;
    }

    public int getMaxDrop() {
        return maxDrop;
    }
}
