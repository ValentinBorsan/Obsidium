package net.valentin.obsidium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.VolcaniteSortingStationBlockEntity;
import org.joml.Matrix4f;

public class VolcaniteSortingStationRenderer implements BlockEntityRenderer<VolcaniteSortingStationBlockEntity> {
    private static final ResourceLocation ITEM_TEXTURE = ResourceLocation.fromNamespaceAndPath("obsidium", "textures/block/volcanite_sorting_station_item_face.png");
    private static final ResourceLocation LIQUID_TEXTURE = ResourceLocation.fromNamespaceAndPath("obsidium", "textures/block/volcanite_sorting_station_liquid_face.png");

    public VolcaniteSortingStationRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(VolcaniteSortingStationBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        for (Direction dir : Direction.values()) {
            int mode = entity.getFaceMode(dir);
            if (mode == VolcaniteSortingStationBlockEntity.MODE_DISABLED) continue;

            poseStack.pushPose();
            // Center of block
            poseStack.translate(0.5, 0.5, 0.5);

            // Rotate so local -Z points in direction 'dir'
            switch (dir) {
                case NORTH -> {}
                case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
                case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
                case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
                case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
                case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            }

            ResourceLocation texture = (mode == VolcaniteSortingStationBlockEntity.MODE_ITEM) ? ITEM_TEXTURE : LIQUID_TEXTURE;
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(texture));
            Matrix4f matrix = poseStack.last().pose();

            // Render glowing colored face plate flush against face (Z = -0.501f)
            // Emissive full brightness: 0xF000F0
            int glowLight = 0xF000F0;
            float min = -0.5f;
            float max = 0.5f;
            float z = -0.5005f;

            // Quad facing -Z
            consumer.addVertex(matrix, min, max, z).setColor(255, 255, 255, 255).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(glowLight).setNormal(0, 0, -1);
            consumer.addVertex(matrix, max, max, z).setColor(255, 255, 255, 255).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(glowLight).setNormal(0, 0, -1);
            consumer.addVertex(matrix, max, min, z).setColor(255, 255, 255, 255).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(glowLight).setNormal(0, 0, -1);
            consumer.addVertex(matrix, min, min, z).setColor(255, 255, 255, 255).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(glowLight).setNormal(0, 0, -1);

            // Floating 3D item miniature
            ItemStack filter = entity.getFilterItem(dir);
            if (!filter.isEmpty() && !filter.is(ObsidiumMod.VOLCANITE_STORAGE_UPGRADE) && !filter.is(ObsidiumMod.VOLCANITE_LIQUID_UPGRADE)) {
                poseStack.pushPose();
                poseStack.translate(0, 0, -0.525F);
                poseStack.scale(0.32F, 0.32F, 0.32F);
                Minecraft.getInstance().getItemRenderer().renderStatic(
                    filter, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, entity.getLevel(), 0
                );
                poseStack.popPose();
            }

            poseStack.popPose();
        }
    }
}
