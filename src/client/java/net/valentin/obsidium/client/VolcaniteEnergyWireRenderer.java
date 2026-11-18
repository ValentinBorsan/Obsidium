package net.valentin.obsidium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.block.VolcaniteEnergyWireBlock;
import net.valentin.obsidium.block.VolcaniteEnergyWireBlockEntity;
import net.valentin.obsidium.block.VolcaniteEnergyWireBlockEntity.WireMode;
import org.joml.Matrix4f;

public class VolcaniteEnergyWireRenderer implements BlockEntityRenderer<VolcaniteEnergyWireBlockEntity> {
    private static final ResourceLocation EXTRACT_TEXTURE = ResourceLocation.fromNamespaceAndPath("obsidium", "textures/block/volcanite_pipe_module_extract.png");
    private static final ResourceLocation INSERT_TEXTURE = ResourceLocation.fromNamespaceAndPath("obsidium", "textures/block/volcanite_pipe_module_insert.png");

    public VolcaniteEnergyWireRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(VolcaniteEnergyWireBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = entity.getBlockState();
        if (!(state.getBlock() instanceof VolcaniteEnergyWireBlock)) return;

        if (entity.getFacadeBlock() != null) {
            BlockState facadeState = entity.getFacadeBlock().defaultBlockState();
            poseStack.pushPose();
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                facadeState, poseStack, bufferSource, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY
            );
            poseStack.popPose();
            if (!facadeState.is(net.minecraft.world.level.block.Blocks.GLASS)) {
                return;
            }
        }

        for (Direction dir : Direction.values()) {
            if (entity.isSideDisconnected(dir)) continue;
            if (!state.getValue(VolcaniteEnergyWireBlock.getProperty(dir))) continue;

            BlockEntity neighborBe = entity.getLevel() != null ? entity.getLevel().getBlockEntity(entity.getBlockPos().relative(dir)) : null;
            if (neighborBe instanceof VolcaniteEnergyWireBlockEntity) continue; // Only render dock collar on machine connections

            WireMode mode = entity.getWireMode(dir);

            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);

            switch (dir) {
                case NORTH -> {}
                case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
                case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
                case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
                case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
                case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            }

            int mr, mg, mb;
            ResourceLocation texture;
            if (mode == WireMode.EXTRACT) {
                mr = 255; mg = 140; mb = 0; // Orange-Gold (Export)
                texture = EXTRACT_TEXTURE;
            } else if (mode == WireMode.INSERT) {
                mr = 0; mg = 220; mb = 255; // Cyan-Blue (Import)
                texture = INSERT_TEXTURE;
            } else {
                mr = 255; mg = 220; mb = 50; // Yellow (Normal)
                texture = INSERT_TEXTURE;
            }

            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(texture));
            Matrix4f matrix = poseStack.last().pose();

            // Wire arm collar around machine connection (-0.5005 to -0.38 in Z)
            renderBox(consumer, matrix, -0.20F, 0.20F, -0.20F, 0.20F, -0.5005F, -0.38F, mr, mg, mb, 255, packedLight, packedOverlay);

            // Glowing energy ring (emissive 0x00F000F0) - Orange for Export, Cyan for Import, Yellow for Normal!
            int glowLight = 0x00F000F0;
            renderBox(consumer, matrix, -0.21F, 0.21F, -0.21F, 0.21F, -0.44F, -0.40F, mr, mg, mb, 255, glowLight, packedOverlay);

            // Glowing yellow spark center (Energy indicator)
            renderBox(consumer, matrix, -0.08F, 0.08F, -0.08F, 0.08F, -0.502F, -0.46F, 255, 240, 60, 255, glowLight, packedOverlay);

            poseStack.popPose();
        }

        // Render colored dye band around wire center if color-isolated
        if (entity.getPipeColor() >= 0) {
            int rgb = net.valentin.obsidium.item.QuantumLinkModuleItem.getChannelRgb(net.minecraft.world.item.DyeColor.byId(entity.getPipeColor()));
            int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            Matrix4f matrix = poseStack.last().pose();
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(INSERT_TEXTURE));
            renderBox(consumer, matrix, -0.145F, 0.145F, -0.145F, 0.145F, -0.145F, 0.145F, r, g, b, 255, 0x00F000F0, packedOverlay);
            poseStack.popPose();
        }

        // Render glowing quantum core in the center of the wire if quantum linked
        if (entity.isQuantumLinked()) {
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            Matrix4f matrix = poseStack.last().pose();
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(INSERT_TEXTURE));
            int glowLight = 0x00F000F0;
            renderBox(consumer, matrix, -0.11F, 0.11F, -0.11F, 0.11F, -0.11F, 0.11F, 180, 75, 255, 230, glowLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static void renderBox(VertexConsumer consumer, Matrix4f matrix,
                                  float minX, float maxX, float minY, float maxY, float minZ, float maxZ,
                                  int r, int g, int b, int a, int light, int overlay) {
        vertex(consumer, matrix, minX, minY, minZ, 0, 0, 0, 0, -1, r, g, b, a, light, overlay);
        vertex(consumer, matrix, minX, maxY, minZ, 0, 1, 0, 0, -1, r, g, b, a, light, overlay);
        vertex(consumer, matrix, maxX, maxY, minZ, 1, 1, 0, 0, -1, r, g, b, a, light, overlay);
        vertex(consumer, matrix, maxX, minY, minZ, 1, 0, 0, 0, -1, r, g, b, a, light, overlay);

        vertex(consumer, matrix, maxX, minY, maxZ, 0, 0, 0, 0, 1, r, g, b, a, light, overlay);
        vertex(consumer, matrix, maxX, maxY, maxZ, 0, 1, 0, 0, 1, r, g, b, a, light, overlay);
        vertex(consumer, matrix, minX, maxY, maxZ, 1, 1, 0, 0, 1, r, g, b, a, light, overlay);
        vertex(consumer, matrix, minX, minY, maxZ, 1, 0, 0, 0, 1, r, g, b, a, light, overlay);

        vertex(consumer, matrix, maxX, minY, minZ, 0, 0, 1, 0, 0, r, g, b, a, light, overlay);
        vertex(consumer, matrix, maxX, maxY, minZ, 0, 1, 1, 0, 0, r, g, b, a, light, overlay);
        vertex(consumer, matrix, maxX, maxY, maxZ, 1, 1, 1, 0, 0, r, g, b, a, light, overlay);
        vertex(consumer, matrix, maxX, minY, maxZ, 1, 0, 1, 0, 0, r, g, b, a, light, overlay);

        vertex(consumer, matrix, minX, minY, maxZ, 0, 0, -1, 0, 0, r, g, b, a, light, overlay);
        vertex(consumer, matrix, minX, maxY, maxZ, 0, 1, -1, 0, 0, r, g, b, a, light, overlay);
        vertex(consumer, matrix, minX, maxY, minZ, 1, 1, -1, 0, 0, r, g, b, a, light, overlay);
        vertex(consumer, matrix, minX, minY, minZ, 1, 0, -1, 0, 0, r, g, b, a, light, overlay);

        vertex(consumer, matrix, minX, maxY, minZ, 0, 0, 0, 1, 0, r, g, b, a, light, overlay);
        vertex(consumer, matrix, minX, maxY, maxZ, 0, 1, 0, 1, 0, r, g, b, a, light, overlay);
        vertex(consumer, matrix, maxX, maxY, maxZ, 1, 1, 0, 1, 0, r, g, b, a, light, overlay);
        vertex(consumer, matrix, maxX, maxY, minZ, 1, 0, 0, 1, 0, r, g, b, a, light, overlay);

        vertex(consumer, matrix, minX, minY, maxZ, 0, 0, 0, -1, 0, r, g, b, a, light, overlay);
        vertex(consumer, matrix, minX, minY, minZ, 0, 1, 0, -1, 0, r, g, b, a, light, overlay);
        vertex(consumer, matrix, maxX, minY, minZ, 1, 1, 0, -1, 0, r, g, b, a, light, overlay);
        vertex(consumer, matrix, maxX, minY, maxZ, 1, 0, 0, -1, 0, r, g, b, a, light, overlay);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
                               float x, float y, float z,
                               float u, float v,
                               float nx, float ny, float nz,
                               int r, int g, int b, int a, int light, int overlay) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }
}
