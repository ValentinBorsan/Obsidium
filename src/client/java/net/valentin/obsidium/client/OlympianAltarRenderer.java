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
import net.valentin.obsidium.block.OlympianAltarBlockEntity;

public class OlympianAltarRenderer implements BlockEntityRenderer<OlympianAltarBlockEntity> {

    public OlympianAltarRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(OlympianAltarBlockEntity altar, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack placed = altar.getPlacedItem();
        if (placed.isEmpty()) return;

        poseStack.pushPose();

        long time = System.currentTimeMillis();
        float hover = (float) Math.sin(time / 450.0) * 0.05f;
        float rot = (time / 30.0f) % 360.0f;

        // Position directly hovering in the altar focal niche
        poseStack.translate(0.5, 1.05 + hover, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(rot));
        poseStack.scale(0.75f, 0.75f, 0.75f);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                placed,
                ItemDisplayContext.GROUND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                altar.getLevel(),
                0
        );

        poseStack.popPose();
    }
}

