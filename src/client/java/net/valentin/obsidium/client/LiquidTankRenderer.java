package net.valentin.obsidium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.block.LiquidTankBlockEntity;
import org.joml.Matrix4f;

public class LiquidTankRenderer implements BlockEntityRenderer<LiquidTankBlockEntity> {

    public LiquidTankRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(LiquidTankBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        FluidVariant fluid = entity.getStoredFluid();
        int amount = entity.getFluidAmount();
        int capacity = entity.getMaxCapacity();

        if (fluid == null || fluid.isBlank() || amount <= 0 || capacity <= 0) return;

        TextureAtlasSprite sprite = FluidVariantRendering.getSprite(fluid);
        if (sprite == null) {
            sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(
                fluid.isOf(Fluids.LAVA) ? 
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "block/lava_still") : 
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_still")
            );
        }
        if (sprite == null) return;

        int color = FluidVariantRendering.getColor(fluid);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;
        if (a <= 0) a = 230;

        if (fluid.isOf(net.valentin.obsidium.ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID)) {
            r = 255; g = 45; b = 135; a = 240;
        } else if (fluid.isOf(net.valentin.obsidium.ObsidiumMod.STEAM_FLUID)) {
            r = 215; g = 245; b = 255; a = 190;
        }

        // Lava and Pyroclastic Distillate glow with full brightness
        int light = (fluid.isOf(Fluids.LAVA) || fluid.isOf(net.valentin.obsidium.ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID)) ? 0xF000F0 : packedLight;

        float fraction = Math.min(1.0f, Math.max(0.02f, (float) amount / (float) capacity));
        float minX = 0.126f;
        float maxX = 0.874f;
        float minZ = 0.126f;
        float maxZ = 0.874f;
        float minY = 0.063f;
        float maxY = minY + fraction * (0.937f - minY);

        VertexConsumer consumer = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());
        Matrix4f matrix = poseStack.last().pose();

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        // 1. Top face (y = maxY, normal 0, 1, 0)
        consumer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, a).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        consumer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, a).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        consumer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        consumer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, a).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);

        // 2. Bottom face (y = minY, normal 0, -1, 0)
        consumer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, a).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, -1, 0);
        consumer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, a).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, -1, 0);
        consumer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, -1, 0);
        consumer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, a).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, -1, 0);

        // Interpolated side V according to fraction
        float sideV0 = v1 - fraction * (v1 - v0);

        // 3. North face (z = minZ, normal 0, 0, -1)
        consumer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, a).setUv(u0, sideV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, -1);
        consumer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, a).setUv(u1, sideV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, -1);
        consumer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, -1);
        consumer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, a).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, -1);

        // 4. South face (z = maxZ, normal 0, 0, 1)
        consumer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, a).setUv(u0, sideV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, a).setUv(u1, sideV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, a).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 1);

        // 5. West face (x = minX, normal -1, 0, 0)
        consumer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, a).setUv(u0, sideV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(-1, 0, 0);
        consumer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, a).setUv(u1, sideV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(-1, 0, 0);
        consumer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(-1, 0, 0);
        consumer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, a).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(-1, 0, 0);

        // 6. East face (x = maxX, normal 1, 0, 0)
        consumer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, a).setUv(u0, sideV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(1, 0, 0);
        consumer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, a).setUv(u1, sideV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(1, 0, 0);
        consumer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(1, 0, 0);
        consumer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, a).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(1, 0, 0);
    }
}
