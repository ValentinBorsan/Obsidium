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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.PoseidonAbyssalPumpBlock;
import net.valentin.obsidium.block.PoseidonAbyssalPumpBlockEntity;
import org.joml.Matrix4f;

public class PoseidonAbyssalPumpRenderer implements BlockEntityRenderer<PoseidonAbyssalPumpBlockEntity> {
    private static final ResourceLocation WHITE_TEX = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    public PoseidonAbyssalPumpRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(PoseidonAbyssalPumpBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public void render(PoseidonAbyssalPumpBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (entity.getLevel() == null) return;
        BlockState state = entity.getBlockState();
        if (!(state.getBlock() instanceof PoseidonAbyssalPumpBlock)) return;

        // 1. Unformed Hologram Guide for the 7 missing Casing positions
        if (!entity.isFormed()) {
            if (!entity.isShowHologram()) return;
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(WHITE_TEX));
            BlockPos origin = entity.getBlockPos();
            for (BlockPos cp : entity.getCasingPositions()) {
                if (entity.getLevel().getBlockState(cp).is(ObsidiumMod.POSEIDON_ABYSSAL_CASING_BLOCK)) continue;
                poseStack.pushPose();
                poseStack.translate(cp.getX() - origin.getX() + 0.5, cp.getY() - origin.getY() + 0.5, cp.getZ() - origin.getZ() + 0.5);
                Matrix4f mat = poseStack.last().pose();
                renderBox(consumer, mat, -0.45f, 0.45f, -0.45f, 0.45f, -0.45f, 0.45f, 0, 210, 255, 95, 0x00F000F0, packedOverlay);
                poseStack.popPose();
            }
            return;
        }

        // 2. Formed 2x2x2: Render 3D Rotating Abyssal Turbine & Glowing Vortex Core at exact center
        Direction facing = state.hasProperty(PoseidonAbyssalPumpBlock.FACING) ? state.getValue(PoseidonAbyssalPumpBlock.FACING) : Direction.NORTH;
        Direction right = facing.getCounterClockWise();
        Direction back = facing.getOpposite();

        double cx = 0.5 + 0.5 * (right.getStepX() + back.getStepX());
        double cy = 1.0;
        double cz = 0.5 + 0.5 * (right.getStepZ() + back.getStepZ());

        float time = (entity.getLevel().getGameTime() + partialTick);
        float speedMult = entity.isActivelyPumping() ? (1.0f + entity.getSpeedTier() * 1.4f) : 0.25f;
        float angle = (time * 6.0f * speedMult) % 360.0f;

        int argb = entity.getLastFluidColor();
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(WHITE_TEX));

        // Central Fluid Vortex Column
        poseStack.pushPose();
        poseStack.translate(cx, cy, cz);
        poseStack.mulPose(Axis.YP.rotationDegrees(-angle * 0.7f));
        Matrix4f coreMat = poseStack.last().pose();
        renderBox(consumer, coreMat, -0.22f, 0.22f, -0.78f, 0.78f, -0.22f, 0.22f, r, g, b, 190, 0x00F000F0, packedOverlay);
        poseStack.popPose();

        // Spinning Bronze/Prismarine Turbine Blades
        poseStack.pushPose();
        poseStack.translate(cx, cy, cz);
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        Matrix4f bladeMat = poseStack.last().pose();
        renderBox(consumer, bladeMat, -0.68f, 0.68f, -0.10f, 0.10f, -0.07f, 0.07f, 200, 155, 65, 240, 0x00F000F0, packedOverlay);
        renderBox(consumer, bladeMat, -0.07f, 0.07f, -0.10f, 0.10f, -0.68f, 0.68f, 200, 155, 65, 240, 0x00F000F0, packedOverlay);
        poseStack.popPose();
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
                               float x, float y, float z, float u, float v,
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
