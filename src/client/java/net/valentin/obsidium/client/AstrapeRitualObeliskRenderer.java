package net.valentin.obsidium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.block.AstrapeRitualObeliskBlockEntity;

public class AstrapeRitualObeliskRenderer implements BlockEntityRenderer<AstrapeRitualObeliskBlockEntity> {

    public AstrapeRitualObeliskRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(AstrapeRitualObeliskBlockEntity obelisk, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack held = obelisk.getHeldItem();
        if (held.isEmpty()) return;

        poseStack.pushPose();

        long time = System.currentTimeMillis();
        float hover = (float) Math.sin((time + obelisk.getBlockPos().hashCode()) / 450.0) * 0.04f;
        float rot = (time / 35.0f) % 360.0f;

        // Position hovering over the obelisk upper focus
        poseStack.translate(0.5, 1.15 + hover, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(rot));
        poseStack.scale(0.65f, 0.65f, 0.65f);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                held,
                ItemDisplayContext.GROUND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                obelisk.getLevel(),
                0
        );

        poseStack.popPose();
    }
}

