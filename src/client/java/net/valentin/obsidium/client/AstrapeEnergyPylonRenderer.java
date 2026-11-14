package net.valentin.obsidium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.block.AstrapeEnergyPylonBlock;
import net.valentin.obsidium.block.AstrapeEnergyPylonBlockEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class AstrapeEnergyPylonRenderer implements BlockEntityRenderer<AstrapeEnergyPylonBlockEntity> {

    public AstrapeEnergyPylonRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(AstrapeEnergyPylonBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96; // Render long-distance energy beams across base
    }

    @Override
    public void render(AstrapeEnergyPylonBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockPos currentPos = entity.getBlockPos();
        BlockState state = entity.getBlockState();
        if (!(state.getBlock() instanceof AstrapeEnergyPylonBlock)) return;

        Direction facing = state.getValue(AstrapeEnergyPylonBlock.FACING);
        Vector3f localTip = getTipOffset(facing);

        long gameTime = entity.getLevel() != null ? entity.getLevel().getGameTime() : 0;
        float animTime = gameTime + partialTick;

        // 1. Render Floating Resonator Crystal above the pylon tip
        renderLevitatingCrystal(poseStack, bufferSource, localTip, animTime, entity.isTransferring());

        // 2. Render Laser Beams to linked pylons
        if (entity.getLinkedPylons().isEmpty()) return;

        VertexConsumer beamConsumer = bufferSource.getBuffer(RenderType.lightning());

        for (BlockPos targetPos : entity.getLinkedPylons()) {
            // Render each connection once (ordered pos)
            if (currentPos.compareTo(targetPos) >= 0) continue;

            Direction targetFacing = Direction.UP;
            if (entity.getLevel() != null && entity.getLevel().isLoaded(targetPos)) {
                BlockState targetState = entity.getLevel().getBlockState(targetPos);
                if (!(entity.getLevel().getBlockEntity(targetPos) instanceof AstrapeEnergyPylonBlockEntity)
                        || !(targetState.getBlock() instanceof AstrapeEnergyPylonBlock)) {
                    continue;
                }
                targetFacing = targetState.getValue(AstrapeEnergyPylonBlock.FACING);
            }

            Vector3f targetTip = getTipOffset(targetFacing);
            float dx = (targetPos.getX() - currentPos.getX()) + (targetTip.x - localTip.x);
            float dy = (targetPos.getY() - currentPos.getY()) + (targetTip.y - localTip.y);
            float dz = (targetPos.getZ() - currentPos.getZ()) + (targetTip.z - localTip.z);

            float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance < 0.01f) continue;

            poseStack.pushPose();
            poseStack.translate(localTip.x, localTip.y, localTip.z);

            // Rotate local +Z axis cleanly onto exact normalized vector towards target tip
            poseStack.mulPose(new org.joml.Quaternionf().rotationTo(0.0f, 0.0f, 1.0f, dx / distance, dy / distance, dz / distance));

            // Spin beam along axis
            poseStack.mulPose(Axis.ZP.rotationDegrees(animTime * 12.0f));

            Matrix4f matrix = poseStack.last().pose();

            // Inner hot core (White / Gold)
            drawBeamCylinder(beamConsumer, matrix, distance, 0.035f, 255, 255, 255, 240);

            // Outer plasma sheath (Electric Cyan / Gold pulse)
            float pulse = 0.08f + (float) Math.sin(animTime * 0.2f) * 0.015f;
            drawBeamCylinder(beamConsumer, matrix, distance, pulse, 255, 195, 45, 140);
            drawBeamCylinder(beamConsumer, matrix, distance, pulse * 1.5f, 80, 225, 255, 80);

            poseStack.popPose();
        }
    }

    private static Vector3f getTipOffset(Direction facing) {
        return switch (facing) {
            case DOWN -> new Vector3f(0.5f, 0.15f, 0.5f);
            case NORTH -> new Vector3f(0.5f, 0.5f, 0.15f);
            case SOUTH -> new Vector3f(0.5f, 0.5f, 0.85f);
            case WEST -> new Vector3f(0.15f, 0.5f, 0.5f);
            case EAST -> new Vector3f(0.85f, 0.5f, 0.5f);
            default -> new Vector3f(0.5f, 0.85f, 0.5f); // UP
        };
    }

    private static void renderLevitatingCrystal(PoseStack poseStack, MultiBufferSource bufferSource, Vector3f tip, float animTime, boolean active) {
        poseStack.pushPose();
        float hoverY = (float) Math.sin(animTime * 0.1f) * 0.04f;
        poseStack.translate(tip.x, tip.y + hoverY, tip.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(animTime * 4.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees(25.0f));

        VertexConsumer crystal = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f mat = poseStack.last().pose();

        float size = active ? 0.09f : 0.07f;
        int r = active ? 255 : 220;
        int g = active ? 210 : 160;
        int b = active ? 60 : 35;
        int a = 230;

        // Octahedron crystal
        drawTriangle(crystal, mat, 0, size, 0, -size, 0, -size, size, 0, -size, r, g, b, a);
        drawTriangle(crystal, mat, 0, size, 0, size, 0, -size, size, 0, size, r, g, b, a);
        drawTriangle(crystal, mat, 0, size, 0, size, 0, size, -size, 0, size, r, g, b, a);
        drawTriangle(crystal, mat, 0, size, 0, -size, 0, size, -size, 0, -size, r, g, b, a);

        drawTriangle(crystal, mat, 0, -size, 0, size, 0, -size, -size, 0, -size, r, g, b, a);
        drawTriangle(crystal, mat, 0, -size, 0, size, 0, size, size, 0, -size, r, g, b, a);
        drawTriangle(crystal, mat, 0, -size, 0, -size, 0, size, size, 0, size, r, g, b, a);
        drawTriangle(crystal, mat, 0, -size, 0, -size, 0, -size, -size, 0, size, r, g, b, a);

        poseStack.popPose();
    }

    private static void drawBeamCylinder(VertexConsumer consumer, Matrix4f mat, float length, float radius, int r, int g, int b, int a) {
        // Cross quads along Z-axis (oriented towards target)
        // Quad 1: X-plane
        consumer.addVertex(mat, -radius, 0.0f, 0.0f).setColor(r, g, b, a);
        consumer.addVertex(mat, radius, 0.0f, 0.0f).setColor(r, g, b, a);
        consumer.addVertex(mat, radius, 0.0f, length).setColor(r, g, b, a);
        consumer.addVertex(mat, -radius, 0.0f, length).setColor(r, g, b, a);

        // Quad 2: Y-plane
        consumer.addVertex(mat, 0.0f, -radius, 0.0f).setColor(r, g, b, a);
        consumer.addVertex(mat, 0.0f, radius, 0.0f).setColor(r, g, b, a);
        consumer.addVertex(mat, 0.0f, radius, length).setColor(r, g, b, a);
        consumer.addVertex(mat, 0.0f, -radius, length).setColor(r, g, b, a);

        // Quad 3 & 4 (diagonal crosses for rounder volumetric look)
        float d = radius * 0.7071f;
        consumer.addVertex(mat, -d, -d, 0.0f).setColor(r, g, b, a);
        consumer.addVertex(mat, d, d, 0.0f).setColor(r, g, b, a);
        consumer.addVertex(mat, d, d, length).setColor(r, g, b, a);
        consumer.addVertex(mat, -d, -d, length).setColor(r, g, b, a);

        consumer.addVertex(mat, -d, d, 0.0f).setColor(r, g, b, a);
        consumer.addVertex(mat, d, -d, 0.0f).setColor(r, g, b, a);
        consumer.addVertex(mat, d, -d, length).setColor(r, g, b, a);
        consumer.addVertex(mat, -d, d, length).setColor(r, g, b, a);
    }

    private static void drawTriangle(VertexConsumer consumer, Matrix4f mat, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, int r, int g, int b, int a) {
        consumer.addVertex(mat, x1, y1, z1).setColor(r, g, b, a);
        consumer.addVertex(mat, x2, y2, z2).setColor(r, g, b, a);
        consumer.addVertex(mat, x3, y3, z3).setColor(r, g, b, a);
        consumer.addVertex(mat, x1, y1, z1).setColor(r, g, b, a);
    }
}
