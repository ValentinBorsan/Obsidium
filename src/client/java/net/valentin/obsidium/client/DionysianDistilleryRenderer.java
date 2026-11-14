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
import net.valentin.obsidium.block.DionysianDistilleryBlock;
import net.valentin.obsidium.block.DionysianDistilleryBlockEntity;
import org.joml.Matrix4f;

public class DionysianDistilleryRenderer implements BlockEntityRenderer<DionysianDistilleryBlockEntity> {

    private static final ResourceLocation COIL_TEX = ResourceLocation.fromNamespaceAndPath("minecraft", "block/copper_block");
    private static final ResourceLocation AMETHYST_TEX = ResourceLocation.fromNamespaceAndPath("minecraft", "block/amethyst_block");
    private static final ResourceLocation FLUID_TEX = ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_still");

    public DionysianDistilleryRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(DionysianDistilleryBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite coilSprite = atlas.apply(COIL_TEX);
        TextureAtlasSprite amethystSprite = atlas.apply(AMETHYST_TEX);
        TextureAtlasSprite fluidSprite = atlas.apply(FLUID_TEX);
        if (coilSprite == null || amethystSprite == null || fluidSprite == null) return;

        boolean isLit = entity.getBlockState().hasProperty(DionysianDistilleryBlock.LIT)
                && entity.getBlockState().getValue(DionysianDistilleryBlock.LIT);
        double timeSec = (System.currentTimeMillis() % 100000L) / 1000.0;

        VertexConsumer solid = bufferSource.getBuffer(Sheets.solidBlockSheet());

        // 1. Rotating Copper & Amethyst Condensing Coil on Top (y = 1.01 .. 1.24)
        poseStack.pushPose();
        poseStack.translate(0.5, 1.12, 0.5);
        float spin = isLit ? (float) ((timeSec * 140.0) % 360.0) : 0.0f;
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));

        // Central amethyst condenser core
        int coreLight = isLit ? 0xF000F0 : packedLight;
        drawBox(solid, poseStack.last().pose(), -0.07f, -0.10f, -0.07f, 0.07f, 0.10f, 0.07f, amethystSprite, coreLight, 225, 175, 255, 255);
        // Crossed copper condensing arms
        drawBox(solid, poseStack.last().pose(), -0.22f, -0.03f, -0.035f, 0.22f, 0.03f, 0.035f, coilSprite, packedLight, 235, 180, 145, 255);
        drawBox(solid, poseStack.last().pose(), -0.035f, -0.03f, -0.22f, 0.035f, 0.03f, 0.22f, coilSprite, packedLight, 235, 180, 145, 255);
        poseStack.popPose();

        // 2. Rising Helical Distillate Droplets when LIT
        VertexConsumer trans = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());
        FluidVariant fv = entity.getStoredFluid();
        int[] dropRgb = getDropRgb(fv);
        if (isLit) {
            for (int k = 0; k < 3; k++) {
                double phase = (timeSec * 1.4 + k / 3.0) % 1.0;
                double angle = timeSec * 4.5 + k * (Math.PI * 2.0 / 3.0);
                float dx = 0.5f + (float) (Math.cos(angle) * 0.24);
                float dz = 0.5f + (float) (Math.sin(angle) * 0.24);
                float dy = 1.02f + (float) (phase * 0.36);
                int alpha = (int) ((1.0 - Math.abs(phase * 2.0 - 1.0)) * 220);
                if (alpha > 20) {
                    drawBox(trans, poseStack.last().pose(),
                            dx - 0.035f, dy - 0.035f, dz - 0.035f,
                            dx + 0.035f, dy + 0.035f, dz + 0.035f,
                            fluidSprite, 0xF000F0, dropRgb[0], dropRgb[1], dropRgb[2], alpha);
                }
            }
        }

        // 3. Visible Biofuel Sight-Column on Top-Corner
        int amt = entity.getFluidAmount();
        if (amt > 0 && !fv.isBlank()) {
            float frac = Math.min(1.0f, Math.max(0.06f, (float) amt / DionysianDistilleryBlockEntity.MAX_FLUID));
            int[] rgb = getDropRgb(fv);
            drawBox(trans, poseStack.last().pose(),
                    0.78f, 1.005f, 0.12f,
                    0.88f, 1.005f + 0.24f * frac, 0.22f,
                    fluidSprite, 0xF000F0, rgb[0], rgb[1], rgb[2], 235);
        }
    }

    private static int[] getDropRgb(FluidVariant fv) {
        if (fv != null && !fv.isBlank()) {
            if (fv.isOf(ObsidiumMod.BIO_OIL_FLUID)) return new int[]{245, 158, 11};
            if (fv.isOf(ObsidiumMod.BIO_ETHANOL_FLUID)) return new int[]{56, 189, 248};
        }
        return new int[]{192, 132, 252}; // Classic Țuică purple-gold
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

