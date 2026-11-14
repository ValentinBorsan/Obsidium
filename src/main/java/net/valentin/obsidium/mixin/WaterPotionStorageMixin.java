package net.valentin.obsidium.mixin;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.impl.transfer.fluid.WaterPotionStorage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = WaterPotionStorage.class, remap = false)
public abstract class WaterPotionStorageMixin {
    @Invoker("<init>")
    public static WaterPotionStorage obsidium$create(ContainerItemContext context) {
        throw new UnsupportedOperationException();
    }

    @Inject(method = "find", at = @At("HEAD"), cancellable = true)
    private static void fixWaterPotionFind(ContainerItemContext context, CallbackInfoReturnable<WaterPotionStorage> cir) {
        try {
            if (context == null) {
                cir.setReturnValue(null);
                return;
            }
            ItemVariant variant = context.getItemVariant();
            if (variant == null || variant.isBlank() || !variant.isOf(Items.POTION)) {
                cir.setReturnValue(null);
                return;
            }

            Optional<? extends PotionContents> potionOpt = variant.getComponents().get(DataComponents.POTION_CONTENTS);
            PotionContents contents = null;
            if (potionOpt != null && potionOpt.isPresent()) {
                contents = potionOpt.get();
            } else {
                ItemStack stack = variant.toStack();
                contents = stack.get(DataComponents.POTION_CONTENTS);
            }

            if (contents == null) {
                cir.setReturnValue(null);
                return;
            }

            if (!contents.is(Potions.WATER)) {
                cir.setReturnValue(null);
                return;
            }

            cir.setReturnValue(obsidium$create(context));
        } catch (Throwable t) {
            cir.setReturnValue(null);
        }
    }
}
