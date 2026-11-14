package net.valentin.obsidium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.block.DemetrianSaplingBarrelBlock;
import net.valentin.obsidium.block.DemetrianSaplingBarrelBlockEntity;

public class DemetrianSaplingBarrelRenderer implements BlockEntityRenderer<DemetrianSaplingBarrelBlockEntity> {

    public DemetrianSaplingBarrelRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DemetrianSaplingBarrelBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = entity.getBlockState();
        if (!(state.getBlock() instanceof DemetrianSaplingBarrelBlock)) return;

        Direction facing = state.getValue(DemetrianSaplingBarrelBlock.FACING);
        int light = entity.getLevel() != null ? LevelRenderer.getLightColor(entity.getLevel(), entity.getBlockPos().relative(facing)) : packedLight;
        Font font = Minecraft.getInstance().font;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        // Align with block rotation
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> {}
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
            default -> {}
        }

        // --- 1. Render on the FRONT FACE ---
        poseStack.pushPose();
        poseStack.translate(0.0, 0.0, 0.505);

        float[] slotOffsetsX = {-0.28f, 0.0f, 0.28f};

        for (int i = 0; i < DemetrianSaplingBarrelBlockEntity.CHAMBER_COUNT; i++) {
            ItemStack sample = entity.getStoredItemSample(i);
            long amount = entity.getChamber(i).getAmount();

            if (!sample.isEmpty() && amount > 0) {
                float ox = slotOffsetsX[i];

                // Render item
                poseStack.pushPose();
                poseStack.translate(ox, 0.08, 0.0);
                poseStack.scale(0.32f, 0.32f, 0.02f);
                Minecraft.getInstance().getItemRenderer().renderStatic(
                        sample,
                        ItemDisplayContext.FIXED,
                        light,
                        OverlayTexture.NO_OVERLAY,
                        poseStack,
                        bufferSource,
                        entity.getLevel(),
                        0
                );
                poseStack.popPose();

                // Render count text below item
                String countText = amount >= 1000 ? String.format("%.1fK", amount / 1000.0) : String.valueOf(amount);
                poseStack.pushPose();
                poseStack.translate(ox, -0.16, 0.005);
                poseStack.scale(0.010f, -0.010f, 0.010f);
                int tw = font.width(countText);
                font.drawInBatch(
                        countText,
                        -tw / 2.0f,
                        0,
                        0xFF86EFAC,
                        false,
                        poseStack.last().pose(),
                        bufferSource,
                        Font.DisplayMode.POLYGON_OFFSET,
                        0,
                        light
                );
                poseStack.popPose();
            }
        }
        poseStack.popPose();

        // --- 2. Render on the TOP FACE (Miniatures growing in the 3 soil openings) ---
        poseStack.pushPose();
        poseStack.translate(0.0, 0.505, 0.0);
        poseStack.mulPose(Axis.XP.rotationDegrees(90));

        // 3 Opening coordinates matching top texture:
        // Chamber 0: center (0.0, -0.25)
        // Chamber 1: south-west (-0.25, 0.1875)
        // Chamber 2: south-east (0.1875, 0.1875)
        float[][] topPositions = {
                {0.0f, -0.22f},
                {-0.24f, 0.18f},
                {0.24f, 0.18f}
        };

        for (int i = 0; i < DemetrianSaplingBarrelBlockEntity.CHAMBER_COUNT; i++) {
            ItemStack sample = entity.getStoredItemSample(i);
            long amount = entity.getChamber(i).getAmount();

            if (!sample.isEmpty() && amount > 0) {
                float tx = topPositions[i][0];
                float ty = topPositions[i][1];

                poseStack.pushPose();
                poseStack.translate(tx, ty, -0.01);
                poseStack.scale(0.28f, 0.28f, 0.02f);
                Minecraft.getInstance().getItemRenderer().renderStatic(
                        sample,
                        ItemDisplayContext.FIXED,
                        light,
                        OverlayTexture.NO_OVERLAY,
                        poseStack,
                        bufferSource,
                        entity.getLevel(),
                        0
                );
                poseStack.popPose();
            }
        }
        poseStack.popPose();

        poseStack.popPose();
    }
}

