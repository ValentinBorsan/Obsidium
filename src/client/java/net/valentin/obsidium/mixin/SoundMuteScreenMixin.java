package net.valentin.obsidium.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.valentin.obsidium.client.VolcaniteGuiTheme;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Universal mute button interaction & tooltip support for all Obsidium screens.
 * Any screen that belongs to net.valentin.obsidium will automatically support
 * clicking the top-right speaker button and rendering its tooltip.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class SoundMuteScreenMixin {
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;
    @Shadow protected int imageHeight;

    private boolean obsidium$isObsidiumScreen() {
        return this.getClass().getName().startsWith("net.valentin.obsidium.");
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void obsidium$onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button == 0 && obsidium$isObsidiumScreen()) {
            if (VolcaniteGuiTheme.handleMuteClick(this.leftPos, this.topPos, this.imageWidth, mouseX, mouseY)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void obsidium$onRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (obsidium$isObsidiumScreen()) {
            VolcaniteGuiTheme.renderMuteTooltip(net.minecraft.client.Minecraft.getInstance().font, guiGraphics, this.leftPos, this.topPos, this.imageWidth, mouseX, mouseY);
        }
    }
}
