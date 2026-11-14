package net.valentin.obsidium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.VolcanicTeleporterBlock;
import net.valentin.obsidium.block.VolcanicTeleporterBlockEntity;
import org.joml.Matrix4f;

/**
 * Renders a subtle rotating quantum energy halo and orbiting field nodes above energized teleporter pads.
 */
public class VolcanicTeleporterRenderer implements BlockEntityRenderer<VolcanicTeleporterBlockEntity> {
    private static final ResourceLocation GLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ObsidiumMod.MOD_ID, "textures/block/volcanite_energy_glow.png");

    public VolcanicTeleporterRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public int getViewDistance() {
        return 48;
    }

    @Override
    public void render(VolcanicTeleporterBlockEntity entity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = entity.getLevel();
        if (level == null) return;

        boolean active = entity.getBlockState().hasProperty(VolcanicTeleporterBlock.ACTIVE)
                && entity.getBlockState().getValue(VolcanicTeleporterBlock.ACTIVE);
        if (!active) return;

        boolean supreme = entity.isSupreme();
        double time = (level.getGameTime() + partialTick);
        float bob = (float) Math.sin(time * 0.08) * 0.04f;
        float rot = (float) ((time * (supreme ? 3.2 : 1.8)) % 360.0);

        int r = supreme ? 0 : 255;
        int g = supreme ? 230 : 155;
        int b = supreme ? 255 : 40;
        int alpha = supreme ? 150 : 120;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.54 + bob, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(rot));

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(GLOW_TEXTURE));
        Matrix4f mat = poseStack.last().pose();
        int fullBright = 0x00F000F0;

        // Rotating quantum projection square & cross filaments above the 8px pad
        addGlowQuad(vc, mat, -0.34f, 0.34f, 0.0f, 0.025f, -0.34f, 0.34f, r, g, b, alpha, fullBright, packedOverlay);

        // Orbiting quantum corner pylons
        float offset = 0.28f;
        for (int idx = 0; idx < 4; idx++) {
            float sx = (idx == 0 || idx == 3) ? -offset : offset;
            float sz = (idx < 2) ? -offset : offset;
            int nr = (supreme && idx % 2 == 1) ? 210 : r;
            int ng = (supreme && idx % 2 == 1) ? 80 : g;
            int nb = (supreme && idx % 2 == 1) ? 255 : b;
            addGlowQuad(vc, mat, sx - 0.035f, sx + 0.035f, 0.04f, 0.14f, sz - 0.035f, sz + 0.035f,
                    nr, ng, nb, 195, fullBright, packedOverlay);
        }

        poseStack.popPose();
    }

    private static void addGlowQuad(VertexConsumer c, Matrix4f m,
                                    float x0, float x1, float y0, float y1, float z0, float z1,
                                    int r, int g, int b, int a, int light, int overlay) {
        c.addVertex(m, x0, y1, z0).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        c.addVertex(m, x0, y1, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        c.addVertex(m, x1, y1, z1).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        c.addVertex(m, x1, y1, z0).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);

        c.addVertex(m, x0, y0, z0).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        c.addVertex(m, x1, y0, z0).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        c.addVertex(m, x1, y0, z1).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        c.addVertex(m, x0, y0, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);

        c.addVertex(m, x0, y1, z0).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        c.addVertex(m, x1, y1, z0).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        c.addVertex(m, x1, y0, z0).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        c.addVertex(m, x0, y0, z0).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);

        c.addVertex(m, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        c.addVertex(m, x0, y1, z1).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        c.addVertex(m, x0, y0, z1).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        c.addVertex(m, x1, y0, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);

        c.addVertex(m, x0, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        c.addVertex(m, x0, y1, z0).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        c.addVertex(m, x0, y0, z0).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        c.addVertex(m, x0, y0, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);

        c.addVertex(m, x1, y1, z0).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        c.addVertex(m, x1, y1, z1).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        c.addVertex(m, x1, y0, z1).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        c.addVertex(m, x1, y0, z0).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
    }
}

