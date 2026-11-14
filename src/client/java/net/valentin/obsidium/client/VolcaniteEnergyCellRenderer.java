package net.valentin.obsidium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.VolcaniteEnergyCellBlockEntity;
import org.joml.Matrix4f;

public class VolcaniteEnergyCellRenderer implements BlockEntityRenderer<VolcaniteEnergyCellBlockEntity> {
    private static final ResourceLocation GLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ObsidiumMod.MOD_ID, "textures/block/volcanite_energy_glow.png");

    public VolcaniteEnergyCellRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(VolcaniteEnergyCellBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (entity.isPartOfFormedMultiblock()) {
            return;
        }

        long cap = Math.max(1L, entity.getEffectiveCapacity());
        float fillPct = Math.min(1.0F, Math.max(0.0F, (float) ((double) entity.energyStorage.amount / (double) cap)));
        boolean advanced = entity.isAdvancedUnit();
        boolean redstoneActive = entity.isActiveByRedstone();
        long nowMs = System.currentTimeMillis();
        float pulse = 0.85F + 0.15F * (float) Math.sin(nowMs / 350.0);

        int fullBright = 0x00F000F0;
        int ledLight = redstoneActive ? fullBright : packedLight;

        // Primary plasma theme color (Gold-Orange for Basic Cell, Electric Cyan-Aqua for Advanced Unit)
        int pr, pg, pb;
        if (advanced) {
            pr = Math.min(255, (int) ((20 + 70 * fillPct) * pulse));
            pg = Math.min(255, (int) ((205 + 45 * fillPct) * pulse));
            pb = 255;
        } else {
            pr = 255;
            pg = Math.min(255, (int) ((115 + 110 * fillPct) * pulse));
            pb = Math.min(255, (int) ((10 + 75 * fillPct) * pulse));
        }
        if (!redstoneActive) {
            pr /= 3;
            pg /= 3;
            pb /= 3;
        }

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(GLOW_TEXTURE));

        for (Direction dir : Direction.values()) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.5F, 0.5F);
            switch (dir) {
                case NORTH -> {}
                case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
                case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
                case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
                case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
                case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            }

            Matrix4f mat = poseStack.last().pose();
            int faceMode = entity.getFaceMode(dir);
            int[] collarRgb = getCollarRgb(faceMode, redstoneActive);
            int collarLight = (faceMode == VolcaniteEnergyCellBlockEntity.FACE_NONE || !redstoneActive) ? packedLight : fullBright;

            // 1. Center I/O Port Collar Frame (matches pixels 5..11 around center diode)
            float outer = 0.148F;
            float inner = 0.112F;
            float zCollar = -0.5015F;
            addFaceQuad(consumer, mat, -outer, inner, outer, outer, zCollar, collarRgb[0], collarRgb[1], collarRgb[2], 240, collarLight, packedOverlay);
            addFaceQuad(consumer, mat, -outer, -outer, outer, -inner, zCollar, collarRgb[0], collarRgb[1], collarRgb[2], 240, collarLight, packedOverlay);
            addFaceQuad(consumer, mat, -outer, -inner, -inner, inner, zCollar, collarRgb[0], collarRgb[1], collarRgb[2], 240, collarLight, packedOverlay);
            addFaceQuad(consumer, mat, inner, -inner, outer, inner, zCollar, collarRgb[0], collarRgb[1], collarRgb[2], 240, collarLight, packedOverlay);

            // 2. Center Plasma Core Diode (scales slightly with stored energy)
            if (fillPct > 0.005F) {
                float coreHalf = 0.032F + 0.032F * fillPct;
                addFaceQuad(consumer, mat, -coreHalf, -coreHalf, coreHalf, coreHalf, -0.5022F,
                        pr, pg, pb, 255, ledLight, packedOverlay);
            }

            // 3. Dual Vertical LED Charge Bars on horizontal faces (exact pixel channels x=4 & x=11, y=3..13)
            if (dir.getAxis().isHorizontal() && fillPct > 0.002F) {
                float minY = -0.3125F;
                float topY = minY + 0.625F * fillPct;
                float zLed = -0.5018F;
                addFaceQuad(consumer, mat, -0.250F, minY, -0.1875F, topY, zLed, pr, pg, pb, 255, ledLight, packedOverlay);
                addFaceQuad(consumer, mat, 0.1875F, minY, 0.250F, topY, zLed, pr, pg, pb, 255, ledLight, packedOverlay);
            }

            poseStack.popPose();
        }

        // 4. Wireless Field-Charge Spinning Halo Ring on top when enabled
        if (entity.isFieldChargeEnabled() && redstoneActive) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 1.032F, 0.5F);
            float angle = ((nowMs % 3600L) / 3600.0F) * 360.0F;
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
            Matrix4f ringMat = poseStack.last().pose();

            int hr = advanced ? 40 : 255;
            int hg = advanced ? 230 : 200;
            int hb = advanced ? 255 : 65;

            float rOut = 0.30F;
            float rIn = 0.25F;
            float h = 0.015F;
            renderThinBox(consumer, ringMat, -rOut, rOut, -h, h, -rOut, -rIn, hr, hg, hb, 235, fullBright, packedOverlay);
            renderThinBox(consumer, ringMat, -rOut, rOut, -h, h, rIn, rOut, hr, hg, hb, 235, fullBright, packedOverlay);
            renderThinBox(consumer, ringMat, -rOut, -rIn, -h, h, -rIn, rIn, hr, hg, hb, 235, fullBright, packedOverlay);
            renderThinBox(consumer, ringMat, rIn, rOut, -h, h, -rIn, rIn, hr, hg, hb, 235, fullBright, packedOverlay);
            poseStack.popPose();
        }
    }

    private static int[] getCollarRgb(int faceMode, boolean redstoneActive) {
        int[] base = switch (faceMode) {
            case VolcaniteEnergyCellBlockEntity.FACE_INPUT -> new int[]{0, 175, 255};   // Electric Blue
            case VolcaniteEnergyCellBlockEntity.FACE_OUTPUT -> new int[]{255, 130, 10}; // Magma Orange
            case VolcaniteEnergyCellBlockEntity.FACE_NONE -> new int[]{42, 36, 52};    // Disabled Slate
            default -> new int[]{185, 95, 255};                                        // Both: Violet-Amethyst
        };
        if (!redstoneActive) {
            return new int[]{base[0] / 3, base[1] / 3, base[2] / 3};
        }
        return base;
    }

    private static void addFaceQuad(VertexConsumer consumer, Matrix4f matrix,
                                    float x0, float y0, float x1, float y1, float zPlane,
                                    int r, int g, int b, int a, int light, int overlay) {
        consumer.addVertex(matrix, x0, y1, zPlane).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        consumer.addVertex(matrix, x1, y1, zPlane).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        consumer.addVertex(matrix, x1, y0, zPlane).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        consumer.addVertex(matrix, x0, y0, zPlane).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
    }

    private static void renderThinBox(VertexConsumer consumer, Matrix4f matrix,
                                      float minX, float maxX, float minY, float maxY, float minZ, float maxZ,
                                      int r, int g, int b, int a, int light, int overlay) {
        // Top (+Y) & Bottom (-Y)
        consumer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        consumer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        consumer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        consumer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);

        consumer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        consumer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        consumer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        consumer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);

        // Sides (-Z, +Z, -X, +X)
        addFaceQuad(consumer, matrix, minX, minY, maxX, maxY, minZ, r, g, b, a, light, overlay);

        consumer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);

        consumer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        consumer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        consumer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        consumer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);

        consumer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        consumer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        consumer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        consumer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
    }
}

