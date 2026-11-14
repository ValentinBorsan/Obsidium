package net.valentin.obsidium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.VolcanicDieselGeneratorBlock;
import net.valentin.obsidium.block.VolcanicDieselGeneratorBlockEntity;
import org.joml.Matrix4f;

public class VolcanicDieselGeneratorRenderer implements BlockEntityRenderer<VolcanicDieselGeneratorBlockEntity> {

    private static final ResourceLocation PISTON_TEX = ResourceLocation.fromNamespaceAndPath("minecraft", "block/copper_block");
    private static final ResourceLocation FLYWHEEL_TEX = ResourceLocation.fromNamespaceAndPath("minecraft", "block/netherite_block");
    private static final ResourceLocation FLUID_TEX = ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_still");

    public VolcanicDieselGeneratorRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(VolcanicDieselGeneratorBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite pistonSprite = atlas.apply(PISTON_TEX);
        TextureAtlasSprite flywheelSprite = atlas.apply(FLYWHEEL_TEX);
        TextureAtlasSprite fluidSprite = atlas.apply(FLUID_TEX);
        if (pistonSprite == null || flywheelSprite == null || fluidSprite == null) return;

        int rpm = entity.getRpm();
        boolean isLit = entity.getBlockState().hasProperty(VolcanicDieselGeneratorBlock.LIT)
                && entity.getBlockState().getValue(VolcanicDieselGeneratorBlock.LIT);
        double timeSec = (System.currentTimeMillis() % 100000L) / 1000.0;
        double speed = 2.0 + (rpm / (double) VolcanicDieselGeneratorBlockEntity.MAX_RPM) * 18.0;
        double phaseBase = (rpm > 0 || isLit) ? timeSec * speed : 0.0;

        VertexConsumer solid = bufferSource.getBuffer(Sheets.solidBlockSheet());

        float[][] pistonOffsets = {
                {0.22f, 0.22f, 0.0f},
                {0.62f, 0.22f, (float) Math.PI},
                {0.22f, 0.62f, (float) (Math.PI * 0.5)},
                {0.62f, 0.62f, (float) (Math.PI * 1.5)}
        };

        int pistonLight = isLit ? Math.max(packedLight, 0xD000D0) : packedLight;
        for (float[] p : pistonOffsets) {
            float stroke = (rpm > 0 || isLit) ? (float) ((Math.sin(phaseBase + p[2]) * 0.5 + 0.5) * 0.14) : 0.02f;
            float minX = p[0];
            float maxX = p[0] + 0.16f;
            float minZ = p[1];
            float maxZ = p[1] + 0.16f;
            float minY = 0.78f;
            float maxY = 0.88f + stroke;
            int r = isLit ? 255 : 210;
            int g = isLit ? (int) (185 + 60 * (stroke / 0.14f)) : 195;
            int b = isLit ? 140 : 185;
            drawBox(solid, poseStack.last().pose(), minX, minY, minZ, maxX, maxY, maxZ, pistonSprite, pistonLight, r, g, b, 255);
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.88, 0.5);
        float angle = (rpm > 0 || isLit) ? (float) ((timeSec * (90.0 + rpm * 0.65)) % 360.0) : 0.0f;
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        drawBox(solid, poseStack.last().pose(), -0.14f, -0.05f, -0.04f, 0.14f, 0.05f, 0.04f, flywheelSprite, packedLight, 230, 230, 240, 255);
        drawBox(solid, poseStack.last().pose(), -0.04f, -0.05f, -0.14f, 0.04f, 0.05f, 0.14f, flywheelSprite, packedLight, 230, 230, 240, 255);
        poseStack.popPose();

        int fluidAmt = entity.getFluidAmount();
        FluidVariant fv = entity.getStoredFluid();
        if (fluidAmt > 0 && !fv.isBlank()) {
            float frac = Math.min(1.0f, Math.max(0.05f, (float) fluidAmt / VolcanicDieselGeneratorBlockEntity.MAX_FLUID));
            int[] rgb = getFluidRgb(fv);
            VertexConsumer trans = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());
            drawBox(trans, poseStack.last().pose(),
                    0.18f, 0.815f, 0.06f,
                    0.18f + 0.64f * frac, 0.855f, 0.13f,
                    fluidSprite, 0xF000F0, rgb[0], rgb[1], rgb[2], 235);
        }
    }

    private static int[] getFluidRgb(FluidVariant fv) {
        if (fv.isOf(ObsidiumMod.BIO_OIL_FLUID)) return new int[]{245, 158, 11};
        if (fv.isOf(ObsidiumMod.BIO_ETHANOL_FLUID)) return new int[]{56, 189, 248};
        if (fv.isOf(ObsidiumMod.BIODIESEL_FLUID)) return new int[]{74, 222, 128};
        if (fv.isOf(ObsidiumMod.PYRO_DIESEL_FLUID)) return new int[]{251, 146, 60};
        return new int[]{200, 200, 200};
    }

    private static void drawBox(VertexConsumer c, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1,
                                TextureAtlasSprite s, int light, int r, int g, int b, int a) {
        float u0 = s.getU0(), u1 = s.getU1(), v0 = s.getV0(), v1 = s.getV1();
        c.addVertex(m, x0, y1, z0).setColor(r, g, b, a).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        c.addVertex(m, x0, y1, z1).setColor(r, g, b, a).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        c.addVertex(m, x1, y1, z1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        c.addVertex(m, x1, y1, z0).setColor(r, g, b, a).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);

        c.addVertex(m, x0, y0, z0).setColor(r, g, b, a).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, -1, 0);
        c.addVertex(m, x1, y0, z0).setColor(r, g, b, a).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, -1, 0);
        c.addVertex(m, x1, y0, z1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, -1, 0);
        c.addVertex(m, x0, y0, z1).setColor(r, g, b, a).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, -1, 0);

        c.addVertex(m, x0, y1, z0).setColor(r, g, b, a).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, -1);
        c.addVertex(m, x1, y1, z0).setColor(r, g, b, a).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, -1);
        c.addVertex(m, x1, y0, z0).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, -1);
        c.addVertex(m, x0, y0, z0).setColor(r, g, b, a).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, -1);

        c.addVertex(m, x1, y1, z1).setColor(r, g, b, a).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
        c.addVertex(m, x0, y1, z1).setColor(r, g, b, a).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
        c.addVertex(m, x0, y0, z1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
        c.addVertex(m, x1, y0, z1).setColor(r, g, b, a).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 1);

        c.addVertex(m, x0, y1, z1).setColor(r, g, b, a).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(-1, 0, 0);
        c.addVertex(m, x0, y1, z0).setColor(r, g, b, a).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(-1, 0, 0);
        c.addVertex(m, x0, y0, z0).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(-1, 0, 0);
        c.addVertex(m, x0, y0, z1).setColor(r, g, b, a).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(-1, 0, 0);

        c.addVertex(m, x1, y1, z0).setColor(r, g, b, a).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(1, 0, 0);
        c.addVertex(m, x1, y1, z1).setColor(r, g, b, a).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(1, 0, 0);
        c.addVertex(m, x1, y0, z1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(1, 0, 0);
        c.addVertex(m, x1, y0, z0).setColor(r, g, b, a).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(1, 0, 0);
    }
}

