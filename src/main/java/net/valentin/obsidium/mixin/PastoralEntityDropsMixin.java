package net.valentin.obsidium.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.block.PastoralHarvestDrops;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class PastoralEntityDropsMixin {
    @Inject(method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void obsidium$capturePastoralLoot(ItemStack stack, float offset, CallbackInfoReturnable<ItemEntity> cir) {
        ItemEntity captured = PastoralHarvestDrops.capture((Entity)(Object)this, stack, offset);
        if (captured != null) cir.setReturnValue(captured);
    }
}
