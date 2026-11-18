package net.valentin.mythicrails.mixin;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.mythicrails.EmberPoweredRailBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

/**
 * Vanilla hard-codes Blocks.POWERED_RAIL in its acceleration/braking check.
 * WrapOperation preserves the original operation and supports Prism's MixinExtras 0.4.1;
 * current-toolchain Redirect annotations crash its legacy factory wrapper at bootstrap.
 */
@Mixin(AbstractMinecart.class)
public abstract class MinecartPoweredRailMixin {
    @WrapOperation(method = "moveAlongTrack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    private boolean mythicRails$isPoweredRail(BlockState state, Block expected, Operation<Boolean> original) {
        return original.call(state, expected) || expected == Blocks.POWERED_RAIL && state.getBlock() instanceof EmberPoweredRailBlock;
    }
}
