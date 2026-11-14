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
import net.valentin.obsidium.block.DemetrianLumberDepotBlock;
import net.valentin.obsidium.block.DemetrianLumberDepotBlockEntity;

public class DemetrianLumberDepotRenderer implements BlockEntityRenderer<DemetrianLumberDepotBlockEntity> {

    public DemetrianLumberDepotRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DemetrianLumberDepotBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = entity.getBlockState();
        if (!(state.getBlock() instanceof DemetrianLumberDepotBlock)) return;

        Direction facing = state.getValue(DemetrianLumberDepotBlock.FACING);
        int light = entity.getLevel() != null ? LevelRenderer.getLightColor(entity.getLevel(), entity.getBlockPos().relative(facing)) : packedLight;
        Font font = Minecraft.getInstance().font;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> {}
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
            default -> {}
        }

        // Render on the front face
        poseStack.pushPose();
        poseStack.translate(0.0, 0.0, 0.505);

        // 4 bays in a 2x2 grid on the front face
        // Bay 0: top-left (-0.22, 0.20)
        // Bay 1: top-right (0.22, 0.20)
        // Bay 2: bottom-left (-0.22, -0.18)
        // Bay 3: bottom-right (0.22, -0.18)
        float[][] bayPositions = {
                {-0.22f, 0.22f},
                {0.22f, 0.22f},
                {-0.22f, -0.16f},
                {0.22f, -0.16f}
        };

        for (int i = 0; i < DemetrianLumberDepotBlockEntity.CHAMBER_COUNT; i++) {
            ItemStack sample = entity.getStoredItemSample(i);
            long amount = entity.getChamber(i).getAmount();

            if (!sample.isEmpty() && amount > 0) {
                float ox = bayPositions[i][0];
                float oy = bayPositions[i][1];

                // Render miniature log item
                poseStack.pushPose();
                poseStack.translate(ox, oy, 0.0);
                poseStack.scale(0.24f, 0.24f, 0.24f);
                try {
                    Minecraft.getInstance().getItemRenderer().renderStatic(
                            sample,
                            ItemDisplayContext.GUI,
                            light,
                            OverlayTexture.NO_OVERLAY,
                            poseStack,
                            bufferSource,
                            entity.getLevel(),
                            0
                    );
                } catch (Exception ignored) {
                }
                poseStack.popPose();

                // Render count text below item
                String countText = amount >= 1000 ? String.format("%.1fK", amount / 1000.0) : String.valueOf(amount);
                poseStack.pushPose();
                poseStack.translate(ox, oy - 0.16, 0.005);
                poseStack.scale(0.009f, -0.009f, 0.009f);
                int tw = font.width(countText);
                font.drawInBatch(
                        countText,
                        -tw / 2.0f,
                        0,
                        0xFFFDE68A,
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
        poseStack.popPose();
    }
}
