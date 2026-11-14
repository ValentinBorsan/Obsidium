package net.valentin.obsidium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.KeraunosHVCoreBlockEntity;
import org.joml.Matrix4f;

public class KeraunosHVCoreRenderer implements BlockEntityRenderer<KeraunosHVCoreBlockEntity> {
    private static final ResourceLocation TOP_TEX = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "block/keraunos_formed_top");
    private static final ResourceLocation BOT_TEX = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "block/keraunos_formed_bottom");
    private static final ResourceLocation SIDE_TEX = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "block/keraunos_formed_side");
    private static final ResourceLocation PORT_TEX = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "block/keraunos_hv_port");

    public KeraunosHVCoreRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(KeraunosHVCoreBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public void render(KeraunosHVCoreBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!entity.isFormed()) {
            return;
        }

        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite topSprite = atlas.apply(TOP_TEX);
        TextureAtlasSprite botSprite = atlas.apply(BOT_TEX);
        TextureAtlasSprite sideSprite = atlas.apply(SIDE_TEX);
        TextureAtlasSprite portSprite = atlas.apply(PORT_TEX);

        if (topSprite == null || botSprite == null || sideSprite == null || portSprite == null) {
            return;
        }

        VertexConsumer consumer = bufferSource.getBuffer(Sheets.solidBlockSheet());
        Matrix4f matrix = poseStack.last().pose();

        // Envelope entire 3x3x3 cube centered at (0,0,0) -> [-1.0025 .. +2.0025]
        float min = -1.0025f;
        float max = 2.0025f;

        int fullBright = 0xF000F0;
        int shellLight = entity.isEnergized() ? fullBright : Math.max(packedLight, 0xB000B0);

        // Dynamic tint by operating state
        int r = 255, g = 255, b = 255;
        if (entity.isServiceMode()) {
            r = 165; g = 255; b = 200; // Calm Emerald Grounded tint
        } else if (entity.isOvervoltageMode() && entity.isEnergized()) {
            float pulse = (float) (0.85 + 0.15 * Math.sin((System.currentTimeMillis() % 1000L) / 1000.0 * Math.PI * 2.0));
            r = 255;
            g = (int) (235 * pulse);
            b = (int) (185 * pulse);
        } else if (entity.isEnergized()) {
            float pulse = (float) (0.9 + 0.1 * Math.sin((System.currentTimeMillis() % 2000L) / 2000.0 * Math.PI * 2.0));
            r = (int) (230 * pulse);
            g = (int) (250 * pulse);
            b = 255;
        }

        // 1. Unified 3x3 Top Face (Y = max)
        addQuadTop(consumer, matrix, min, max, min, max, max,
                topSprite.getU0(), topSprite.getV0(), topSprite.getU1(), topSprite.getV1(), shellLight, r, g, b);

        // 2. Unified 3x3 Bottom Face (Y = min)
        addQuadBottom(consumer, matrix, min, min, min, max, max,
                botSprite.getU0(), botSprite.getV0(), botSprite.getU1(), botSprite.getV1(), packedLight, (r * 4) / 5, (g * 4) / 5, (b * 4) / 5);

        // 3. Unified 3x3 Side Faces (N, S, W, E)
        float su0 = sideSprite.getU0(), su1 = sideSprite.getU1();
        float sv0 = sideSprite.getV0(), sv1 = sideSprite.getV1();
        addQuadNorth(consumer, matrix, min, min, max, max, min, su0, sv0, su1, sv1, shellLight, r, g, b);
        addQuadSouth(consumer, matrix, min, min, max, max, max, su0, sv0, su1, sv1, shellLight, r, g, b);
        addQuadWest(consumer, matrix, min, min, max, min, max, su0, sv0, su1, sv1, shellLight, (r * 24) / 25, (g * 24) / 25, (b * 24) / 25);
        addQuadEast(consumer, matrix, max, min, max, min, max, su0, sv0, su1, sv1, shellLight, (r * 24) / 25, (g * 24) / 25, (b * 24) / 25);

        // 4. Overlay HV Transformer Port Bushings on exposed exterior faces so I/O ports stay clearly visible
        Level level = entity.getLevel();
        if (level != null) {
            BlockPos corePos = entity.getBlockPos();
            float pu0 = portSprite.getU0(), pu1 = portSprite.getU1();
            float pv0 = portSprite.getV0(), pv1 = portSprite.getV1();
            float pad = 0.0045f;

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        if (level.getBlockState(corePos.offset(dx, dy, dz)).getBlock() == ObsidiumMod.KERAUNOS_HV_PORT_BLOCK) {
                            float x0 = dx, x1 = dx + 1.0f;
                            float y0 = dy, y1 = dy + 1.0f;
                            float z0 = dz, z1 = dz + 1.0f;

                            if (dy == 1)  addQuadTop(consumer, matrix, x0, 2.0f + pad, z0, x1, z1, pu0, pv0, pu1, pv1, shellLight, 255, 255, 255);
                            if (dy == -1) addQuadBottom(consumer, matrix, x0, -1.0f - pad, z0, x1, z1, pu0, pv0, pu1, pv1, shellLight, 230, 230, 230);
                            if (dz == -1) addQuadNorth(consumer, matrix, x0, y0, x1, y1, -1.0f - pad, pu0, pv0, pu1, pv1, shellLight, 255, 255, 255);
                            if (dz == 1)  addQuadSouth(consumer, matrix, x0, y0, x1, y1, 2.0f + pad, pu0, pv0, pu1, pv1, shellLight, 255, 255, 255);
                            if (dx == -1) addQuadWest(consumer, matrix, -1.0f - pad, y0, y1, z0, z1, pu0, pv0, pu1, pv1, shellLight, 250, 250, 250);
                            if (dx == 1)  addQuadEast(consumer, matrix, 2.0f + pad, y0, y1, z0, z1, pu0, pv0, pu1, pv1, shellLight, 250, 250, 250);
                        }
                    }
                }
            }
        }
    }

    private void addQuadTop(VertexConsumer c, Matrix4f m, float x0, float y, float z0, float x1, float z1, float u0, float v0, float u1, float v1, int light, int r, int g, int b) {
        c.addVertex(m, x0, y, z0).setColor(r, g, b, 255).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        c.addVertex(m, x0, y, z1).setColor(r, g, b, 255).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        c.addVertex(m, x1, y, z1).setColor(r, g, b, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        c.addVertex(m, x1, y, z0).setColor(r, g, b, 255).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
    }

    private void addQuadBottom(VertexConsumer c, Matrix4f m, float x0, float y, float z0, float x1, float z1, float u0, float v0, float u1, float v1, int light, int r, int g, int b) {
        c.addVertex(m, x0, y, z0).setColor(r, g, b, 255).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, -1, 0);
        c.addVertex(m, x1, y, z0).setColor(r, g, b, 255).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, -1, 0);
        c.addVertex(m, x1, y, z1).setColor(r, g, b, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, -1, 0);
        c.addVertex(m, x0, y, z1).setColor(r, g, b, 255).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, -1, 0);
    }

    private void addQuadNorth(VertexConsumer c, Matrix4f m, float x0, float y0, float x1, float y1, float z, float u0, float v0, float u1, float v1, int light, int r, int g, int b) {
        c.addVertex(m, x0, y1, z).setColor(r, g, b, 255).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, -1);
        c.addVertex(m, x1, y1, z).setColor(r, g, b, 255).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, -1);
        c.addVertex(m, x1, y0, z).setColor(r, g, b, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, -1);
        c.addVertex(m, x0, y0, z).setColor(r, g, b, 255).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, -1);
    }

    private void addQuadSouth(VertexConsumer c, Matrix4f m, float x0, float y0, float x1, float y1, float z, float u0, float v0, float u1, float v1, int light, int r, int g, int b) {
        c.addVertex(m, x1, y1, z).setColor(r, g, b, 255).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
        c.addVertex(m, x0, y1, z).setColor(r, g, b, 255).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
        c.addVertex(m, x0, y0, z).setColor(r, g, b, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
        c.addVertex(m, x1, y0, z).setColor(r, g, b, 255).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
    }

    private void addQuadWest(VertexConsumer c, Matrix4f m, float x, float y0, float y1, float z0, float z1, float u0, float v0, float u1, float v1, int light, int r, int g, int b) {
        c.addVertex(m, x, y1, z1).setColor(r, g, b, 255).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(-1, 0, 0);
        c.addVertex(m, x, y1, z0).setColor(r, g, b, 255).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(-1, 0, 0);
        c.addVertex(m, x, y0, z0).setColor(r, g, b, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(-1, 0, 0);
        c.addVertex(m, x, y0, z1).setColor(r, g, b, 255).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(-1, 0, 0);
    }

    private void addQuadEast(VertexConsumer c, Matrix4f m, float x, float y0, float y1, float z0, float z1, float u0, float v0, float u1, float v1, int light, int r, int g, int b) {
        c.addVertex(m, x, y1, z0).setColor(r, g, b, 255).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(1, 0, 0);
        c.addVertex(m, x, y1, z1).setColor(r, g, b, 255).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(1, 0, 0);
        c.addVertex(m, x, y0, z1).setColor(r, g, b, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(1, 0, 0);
        c.addVertex(m, x, y0, z0).setColor(r, g, b, 255).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(1, 0, 0);
    }
}
