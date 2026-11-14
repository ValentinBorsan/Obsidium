package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.TintedGlassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Heavy reinforced obsidian glass for mob grinding chambers and blast containment.
 * - Blast & Fire Proof (1200.0F resistance against Creepers, Withering & TNT).
 * - Light-Blocking (Acts as Tinted Glass with 15 Light Opacity; chamber interior remains pitch dark).
 * - Mob Sight Blocker: getVisualShape returns a full cube so mob Line-of-Sight raycasts
 *   (ClipContext.Block.VISUAL) hit the glass, completely preventing Creepers from igniting,
 *   Endermen from aggroing, and Skeletons/Zombies from tracking the player!
 */
public class ReinforcedTintedGlassBlock extends TintedGlassBlock {

    public ReinforcedTintedGlassBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Full cube visual occlusion shape ensures LivingEntity#hasLineOfSight returns false!
        return Shapes.block();
    }
}

