package net.valentin.obsidium.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.block.VolcanicOreCropBlock;

import java.util.function.Supplier;

public class VolcanicPetalItem extends Item {
    private final Supplier<Block> cropBlockSupplier;

    public VolcanicPetalItem(Supplier<Block> cropBlockSupplier, Properties properties) {
        super(properties);
        this.cropBlockSupplier = cropBlockSupplier;
    }

    public Block getCropBlock() {
        return cropBlockSupplier.get();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();

        // Must click on the top of the soil block
        if (clickedFace != Direction.UP) {
            return InteractionResult.PASS;
        }

        BlockPos plantPos = clickedPos.above();
        BlockState soilState = level.getBlockState(clickedPos);
        BlockState targetState = level.getBlockState(plantPos);

        if (VolcanicOreCropBlock.isValidSoil(soilState) && targetState.canBeReplaced()) {
            Block cropBlock = cropBlockSupplier.get();
            if (cropBlock instanceof VolcanicOreCropBlock oreCrop) {
                if (!level.isClientSide()) {
                    level.setBlock(plantPos, oreCrop.getStateForAge(0), Block.UPDATE_ALL);
                    level.playSound(null, plantPos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                    Player player = context.getPlayer();
                    if (player != null && !player.getAbilities().instabuild) {
                        context.getItemInHand().shrink(1);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        return super.useOn(context);
    }
}
