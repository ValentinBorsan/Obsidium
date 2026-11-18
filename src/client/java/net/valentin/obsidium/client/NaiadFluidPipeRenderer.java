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
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.block.NaiadFluidPipeBlock;
import net.valentin.obsidium.block.NaiadFluidPipeBlockEntity;
import net.valentin.obsidium.block.NaiadFluidPipeBlockEntity.ValveMode;
import org.joml.Matrix4f;

public class NaiadFluidPipeRenderer implements BlockEntityRenderer<NaiadFluidPipeBlockEntity> {
    private static final ResourceLocation EXTRACT_TEXTURE = ResourceLocation.fromNamespaceAndPath("obsidium", "textures/block/volcanite_pipe_module_extract.png");
    private static final ResourceLocation INSERT_TEXTURE = ResourceLocation.fromNamespaceAndPath("obsidium", "textures/block/volcanite_pipe_module_insert.png");

    public NaiadFluidPipeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(NaiadFluidPipeBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = entity.getBlockState();
        if (!(state.getBlock() instanceof NaiadFluidPipeBlock)) return;

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

            ValveMode mode = entity.getValveMode(dir);
            if (mode == ValveMode.NONE) continue;

            if (entity.getLevel() != null) {
                net.minecraft.world.level.block.entity.BlockEntity neighborBe = entity.getLevel().getBlockEntity(entity.getBlockPos().relative(dir));
                if (neighborBe instanceof NaiadFluidPipeBlockEntity) continue;
            }

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

            // Mode colors: EXTRACT (Export) = Orange/Gold, INSERT (Import) = Cyan/Blue
            int mr, mg, mb;
            if (mode == ValveMode.EXTRACT) {
                mr = 255; mg = 140; mb = 0; // Orange-Gold
            } else {
                mr = 0; mg = 220; mb = 255; // Cyan-Blue
            }

            ResourceLocation texture = (mode == ValveMode.EXTRACT) ? EXTRACT_TEXTURE : INSERT_TEXTURE;
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(texture));
            Matrix4f matrix = poseStack.last().pose();

            // 1. Sleek 7x7 Collar around pipe arm (-0.40 to -0.25 in Z)
            renderBox(consumer, matrix, -0.22F, 0.22F, -0.22F, 0.22F, -0.40F, -0.25F, mr, mg, mb, 255, packedLight, packedOverlay);

            // 2. 8.5x8.5 Docking Flange touching the connected block flush (-0.5005 to -0.40 in Z)
            renderBox(consumer, matrix, -0.265F, 0.265F, -0.265F, 0.265F, -0.5005F, -0.40F, mr, mg, mb, 255, packedLight, packedOverlay);

            // 3. Glowing neon indicator ring (emissive 0x00F000F0) - Orange for Export, Cyan for Import!
            int glowLight = 0x00F000F0;
            renderBox(consumer, matrix, -0.225F, 0.225F, -0.225F, 0.225F, -0.34F, -0.31F, mr, mg, mb, 255, glowLight, packedOverlay);

            // 4. Liquid indicator jewel (Deep Blue 0, 160, 255)
            renderBox(consumer, matrix, -0.10F, 0.10F, -0.10F, 0.10F, -0.502F, -0.45F, 0, 160, 255, 255, glowLight, packedOverlay);

            poseStack.popPose();
        }

        // Render colored dye band around pipe center if color-isolated
        if (entity.getPipeColor() >= 0) {
            int rgb = net.valentin.obsidium.item.QuantumLinkModuleItem.getChannelRgb(net.minecraft.world.item.DyeColor.byId(entity.getPipeColor()));
            int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            Matrix4f matrix = poseStack.last().pose();
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(INSERT_TEXTURE));
            renderBox(consumer, matrix, -0.205F, 0.205F, -0.205F, 0.205F, -0.205F, 0.205F, r, g, b, 255, 0x00F000F0, packedOverlay);
            poseStack.popPose();
        }

        // Render glowing quantum core inside pipe center if quantum linked
        if (entity.isQuantumLinked()) {
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            Matrix4f matrix = poseStack.last().pose();
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(INSERT_TEXTURE));
            int glowLight = 0x00F000F0;
            renderBox(consumer, matrix, -0.15F, 0.15F, -0.15F, 0.15F, -0.15F, 0.15F, 180, 75, 255, 230, glowLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static void renderBox(VertexConsumer consumer, Matrix4f matrix,
                                  float minX, float maxX, float minY, float maxY, float minZ, float maxZ,
                                  int r, int g, int b, int a, int light, int overlay) {
        // North face (-Z)
        vertex(consumer, matrix, minX, minY, minZ, 0, 0, 0, 0, -1, r, g, b, a, light, overlay);
        vertex(consumer, matrix, minX, maxY, minZ, 0, 1, 0, 0, -1, r, g, b, a, light, overlay);
        vertex(consumer, matrix, maxX, maxY, minZ, 1, 1, 0, 0, -1, r, g, b, a, light, overlay);
        vertex(consumer, matrix, maxX, minY, minZ, 1, 0, 0, 0, -1, r, g, b, a, light, overlay);

        // South face (+Z)
        vertex(consumer, matrix, maxX, minY, maxZ, 0, 0, 0, 0, 1, r, g, b, a, light, overlay);
        vertex(consumer, matrix, maxX, maxY, maxZ, 0, 1, 0, 0, 1, r, g, b, a, light, overlay);
        vertex(consumer, matrix, minX, maxY, maxZ, 1, 1, 0, 0, 1, r, g, b, a, light, overlay);
        vertex(consumer, matrix, minX, minY, maxZ, 1, 0, 0, 0, 1, r, g, b, a, light, overlay);

        // East face (+X)
        vertex(consumer, matrix, maxX, minY, minZ, 0, 0, 1, 0, 0, r, g, b, a, light, overlay);
        vertex(consumer, matrix, maxX, maxY, minZ, 0, 1, 1, 0, 0, r, g, b, a, light, overlay);
        vertex(consumer, matrix, maxX, maxY, maxZ, 1, 1, 1, 0, 0, r, g, b, a, light, overlay);
        vertex(consumer, matrix, maxX, minY, maxZ, 1, 0, 1, 0, 0, r, g, b, a, light, overlay);

        // West face (-X)
        vertex(consumer, matrix, minX, minY, maxZ, 0, 0, -1, 0, 0, r, g, b, a, light, overlay);
        vertex(consumer, matrix, minX, maxY, maxZ, 0, 1, -1, 0, 0, r, g, b, a, light, overlay);
        vertex(consumer, matrix, minX, maxY, minZ, 1, 1, -1, 0, 0, r, g, b, a, light, overlay);
        vertex(consumer, matrix, minX, minY, minZ, 1, 0, -1, 0, 0, r, g, b, a, light, overlay);

        // Up face (+Y)
        vertex(consumer, matrix, minX, maxY, minZ, 0, 0, 0, 1, 0, r, g, b, a, light, overlay);
        vertex(consumer, matrix, minX, maxY, maxZ, 0, 1, 0, 1, 0, r, g, b, a, light, overlay);
        vertex(consumer, matrix, maxX, maxY, maxZ, 1, 1, 0, 1, 0, r, g, b, a, light, overlay);
        vertex(consumer, matrix, maxX, maxY, minZ, 1, 0, 0, 1, 0, r, g, b, a, light, overlay);

        // Down face (-Y)
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
