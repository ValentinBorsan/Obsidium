package net.valentin.obsidium.mixin;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.ResourceLocation;
import net.valentin.obsidium.sound.MachineSoundConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts sound playback in Minecraft client.
 * If Obsidium machine sounds are muted by the player, completely cancels playback
 * of any sound under the "obsidium:machine/*" namespace/path.
 */
@Mixin(SoundEngine.class)
public class SoundEngineMixin {

    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    private void obsidium$onPlaySound(SoundInstance sound, CallbackInfo ci) {
        if (sound == null) return;
        ResourceLocation loc = sound.getLocation();
        if (MachineSoundConfig.isMachineSound(loc) && MachineSoundConfig.isMuted()) {
            ci.cancel();
        }
    }
}
