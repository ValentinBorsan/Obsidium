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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.CreativeEnergyCoreBlockEntity;
import net.valentin.obsidium.block.VolcaniteEnergyCoreBlockEntity;
import org.joml.Matrix4f;

public class VolcaniteEnergyCoreRenderer implements BlockEntityRenderer<VolcaniteEnergyCoreBlockEntity> {
    private static final ResourceLocation TOP_TEX = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "block/energy_core_formed_top");
    private static final ResourceLocation SIDE_TEX = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "block/energy_core_formed_side");
    private static final ResourceLocation CORE_TEX = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "block/volcanite_energy_core");

    public VolcaniteEnergyCoreRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(VolcaniteEnergyCoreBlockEntity blockEntity) {
        return true;
    }

    @Override
    public void render(VolcaniteEnergyCoreBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!entity.isFormed() || entity instanceof CreativeEnergyCoreBlockEntity) {
            return;
        }

        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite topSprite = atlas.apply(TOP_TEX);
        TextureAtlasSprite sideSprite = atlas.apply(SIDE_TEX);
        TextureAtlasSprite coreSprite = atlas.apply(CORE_TEX);

        if (topSprite == null || sideSprite == null || coreSprite == null) {
            return;
        }

        VertexConsumer consumer = bufferSource.getBuffer(Sheets.solidBlockSheet());
        Matrix4f matrix = poseStack.last().pose();

        // Slight 0.0025f expansion so the unified 3x3 shell cleanly envelopes the 9 blocks without z-fighting
        float minX = -1.0025f;
        float maxX = 2.0025f;
        float minY = -0.0025f;
        float maxY = 1.0025f;
        float minZ = -1.0025f;
        float maxZ = 2.0025f;

        int fullBright = 0xF000F0;
        int sideLight = Math.max(packedLight, 0xB000B0);

        float tu0 = topSprite.getU0();
        float tu1 = topSprite.getU1();
        float tv0 = topSprite.getV0();
        float tv1 = topSprite.getV1();

        float su0 = sideSprite.getU0();
        float su1 = sideSprite.getU1();
        float sv0 = sideSprite.getV0();
        // Top 1/3rd of the 48x48 side texture is the 48x16 (3-block wide x 1-block tall) side strip
        float sv1 = sv0 + (sideSprite.getV1() - sv0) * (16.0f / 48.0f);

        // 1. Unified 3x3 Top Face (y = maxY)
        addQuadTop(consumer, matrix, minX, maxY, minZ, maxX, maxZ, tu0, tv0, tu1, tv1, sideLight, 255, 255, 255);

        // 2. Unified 3x3 Bottom Face (y = minY)
        addQuadBottom(consumer, matrix, minX, minY, minZ, maxX, maxZ, tu0, tv0, tu1, tv1, packedLight, 200, 200, 200);

        // 3. Unified 3-block-wide North Face (z = minZ)
        addQuadNorth(consumer, matrix, minX, minY, maxX, maxY, minZ, su0, sv0, su1, sv1, sideLight, 245, 245, 255);

        // 4. Unified 3-block-wide South Face (z = maxZ)
        addQuadSouth(consumer, matrix, minX, minY, maxX, maxY, maxZ, su0, sv0, su1, sv1, sideLight, 245, 245, 255);

        // 5. Unified 3-block-wide West Face (x = minX)
        addQuadWest(consumer, matrix, minX, minY, maxY, minZ, maxZ, su0, sv0, su1, sv1, sideLight, 235, 235, 245);

        // 6. Unified 3-block-wide East Face (x = maxX)
        addQuadEast(consumer, matrix, maxX, minY, maxY, minZ, maxZ, su0, sv0, su1, sv1, sideLight, 235, 235, 245);

        // 7. Raised Central Energy Matrix Turret (matching Magma Forge's raised center [1..15, 16..20, 1..15])
        float cMinX = 0.0625f;
        float cMaxX = 0.9375f;
        float cMinY = 1.0025f;
        float cMaxY = 1.2525f;
        float cMinZ = 0.0625f;
        float cMaxZ = 0.9375f;

        float cu0 = coreSprite.getU0();
        float cu1 = coreSprite.getU1();
        float cv0 = coreSprite.getV0();
        float cv1 = coreSprite.getV1();
        float cvSide1 = cv0 + (cv1 - cv0) * 0.25f;

        long amt = entity.energyStorage.amount;
        long cap = Math.max(1L, entity.getDynamicMaxCapacity());
        float fillPct = Math.min(1.0f, Math.max(0.0f, (float) ((double) amt / (double) cap)));

        // Glow pulse based on stored energy
        int pulse = (int) (215 + 40 * Math.sin((System.currentTimeMillis() % 2000L) / 2000.0 * Math.PI * 2.0));
        int cr = 180 + (int) (75 * fillPct);
        int cg = 220 + (int) (35 * fillPct);
        int cb = 255;

        addQuadTop(consumer, matrix, cMinX, cMaxY, cMinZ, cMaxX, cMaxZ, cu0, cv0, cu1, cv1, fullBright, cr, cg, cb);
        addQuadNorth(consumer, matrix, cMinX, cMinY, cMaxX, cMaxY, cMinZ, cu0, cv0, cu1, cvSide1, fullBright, pulse, 240, 255);
        addQuadSouth(consumer, matrix, cMinX, cMinY, cMaxX, cMaxY, cMaxZ, cu0, cv0, cu1, cvSide1, fullBright, pulse, 240, 255);
        addQuadWest(consumer, matrix, cMinX, cMinY, cMaxY, cMinZ, cMaxZ, cu0, cv0, cu1, cvSide1, fullBright, pulse, 240, 255);
        addQuadEast(consumer, matrix, cMaxX, cMinY, cMaxY, cMinZ, cMaxZ, cu0, cv0, cu1, cvSide1, fullBright, pulse, 240, 255);
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
