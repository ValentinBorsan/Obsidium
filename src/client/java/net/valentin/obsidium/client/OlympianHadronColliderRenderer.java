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
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.OlympianHadronColliderBlockEntity;
import org.joml.Matrix4f;

import java.util.WeakHashMap;

public class OlympianHadronColliderRenderer implements BlockEntityRenderer<OlympianHadronColliderBlockEntity> {
    private static final ResourceLocation GLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ObsidiumMod.MOD_ID, "textures/block/volcanite_energy_glow.png");

    private final WeakHashMap<OlympianHadronColliderBlockEntity, float[]> phaseMap = new WeakHashMap<>();

    public OlympianHadronColliderRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(OlympianHadronColliderBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    private static boolean isRingBlockPresent(Level level, BlockPos pos) {
        Block b = level.getBlockState(pos).getBlock();
        return b == ObsidiumMod.SYNCHROTRON_RING_COIL_BLOCK
                || b == ObsidiumMod.HADRON_ENERGY_PORT_BLOCK
                || b == ObsidiumMod.HADRON_ITEM_PORT_BLOCK
                || b == ObsidiumMod.HADRON_FLUID_PORT_BLOCK
                || b == ObsidiumMod.ADVANCED_VOLCANITE_CASING_BLOCK
                || b == ObsidiumMod.VOLCANITE_ENERGY_UNIT_BLOCK;
    }

    @Override
    public void render(OlympianHadronColliderBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = entity.getLevel();
        if (level == null) return;

        int fullBright = 0x00F000F0;
        long nowMs = System.currentTimeMillis();
        float cx = entity.getRingOffsetX() + 0.5f;
        float cz = entity.getRingOffsetZ() + 0.5f;

        VertexConsumer glowConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(GLOW_TEXTURE));

        // 1. Unformed 5x5 Hologram Placement Guide
        if (!entity.isFormed()) {
            if (!entity.isShowHologram()) return;
            float pulse = 0.65f + 0.35f * (float) Math.sin(nowMs / 320.0);
            int alpha = (int) (125 * pulse);

            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != 2) continue;
                    int relX = entity.getRingOffsetX() + dx;
                    int relZ = entity.getRingOffsetZ() + dz;
                    if (relX == 0 && relZ == 0) continue;

                    BlockPos targetWorld = entity.getBlockPos().offset(relX, 0, relZ);
                    boolean valid = isRingBlockPresent(level, targetWorld);
                    boolean isMidpoint = (dx == 0 || dz == 0);

                    poseStack.pushPose();
                    poseStack.translate(relX, 0.0f, relZ);
                    Matrix4f mat = poseStack.last().pose();
                    if (!valid) {
                        int hr = isMidpoint ? 255 : 0;
                        int hg = isMidpoint ? 195 : 210;
                        int hb = isMidpoint ? 40 : 255;
                        addSolidGlowBox(glowConsumer, mat, 0.14f, 0.86f, 0.14f, 0.86f, 0.14f, 0.86f,
                                hr, hg, hb, alpha, fullBright, packedOverlay);
                    } else {
                        addSolidGlowBox(glowConsumer, mat, 0.28f, 0.72f, 1.01f, 1.06f, 0.28f, 0.72f,
                                60, 255, 140, 120, fullBright, packedOverlay);
                    }
                    poseStack.popPose();
                }
            }
            return;
        }

        // 2. Formed 5x5 Accelerator Track & Particle Beams
        int vel = entity.getBeamVelocity();
        int temp = entity.getCoilTemp();

        float[] state = phaseMap.computeIfAbsent(entity, k -> new float[]{(float) (nowMs & 0xFFFFF), 0.0f});
        float dtSec = Math.min(0.1f, Math.max(0.0f, ((nowMs & 0xFFFFF) - state[0]) / 1000.0f));
        if (dtSec < 0.0f) dtSec = 0.016f;
        state[0] = (float) (nowMs & 0xFFFFF);
        if (vel > 0) {
            float angularSpeed = 1.6f + (vel / 1000.0f) * 18.0f;
            state[1] = (state[1] + dtSec * angularSpeed) % ((float) (Math.PI * 2.0));
        }
        float angleA = state[1];

        // Track color based on cryo temperature
        int tr = temp < 120 ? 25 : (temp >= 550 ? 255 : 70);
        int tg = temp < 120 ? 225 : (temp >= 550 ? 75 : 155);
        int tb = temp < 120 ? 255 : (temp >= 550 ? 35 : 230);
        int trackAlpha = vel > 0 ? 190 : 95;
        float trackRadius = 1.62f;
        float trackY = 0.58f;

        // Toroidal Confinement Track (24 rotated segments)
        int segs = 24;
        for (int s = 0; s < segs; s++) {
            double ang = s * (Math.PI * 2.0 / segs);
            float sx = cx + (float) Math.cos(ang) * trackRadius;
            float sz = cz + (float) Math.sin(ang) * trackRadius;
            poseStack.pushPose();
            poseStack.translate(sx, trackY, sz);
            poseStack.mulPose(Axis.YP.rotation((float) -ang));
            Matrix4f m = poseStack.last().pose();
            addSolidGlowBox(glowConsumer, m, -0.055f, 0.055f, -0.055f, 0.055f, -0.23f, 0.23f,
                    tr, tg, tb, trackAlpha, fullBright, packedOverlay);
            poseStack.popPose();
        }

        // 4 Magnetic Bridge Couplers connecting the ring frame into the confinement track
        float[][] bridges = { {cx + 1.82f, cz, 0f}, {cx - 1.82f, cz, 0f}, {cx, cz + 1.82f, 90f}, {cx, cz - 1.82f, 90f} };
        for (float[] br : bridges) {
            poseStack.pushPose();
            poseStack.translate(br[0], trackY, br[1]);
            if (br[2] != 0f) poseStack.mulPose(Axis.YP.rotationDegrees(br[2]));
            Matrix4f m = poseStack.last().pose();
            addSolidGlowBox(glowConsumer, m, -0.24f, 0.24f, -0.04f, 0.04f, -0.04f, 0.04f,
                    tr, tg, tb, 175, fullBright, packedOverlay);
            poseStack.popPose();
        }

        // Dual Counter-Rotating 3D Particle Beams & Orbiting Items
        if (vel > 0) {
            // Beam A (Clockwise) - Cyan-White Plasma + Comet Tail
            for (int t = 8; t >= 0; t--) {
                float a = angleA - t * 0.13f;
                float px = cx + (float) Math.cos(a) * trackRadius;
                float pz = cz + (float) Math.sin(a) * trackRadius;
                float hSize = t == 0 ? 0.145f : (0.11f - t * 0.01f);
                int alpha = t == 0 ? 250 : Math.max(40, 210 - t * 22);
                int rCol = t == 0 ? 225 : 25;
                int gCol = t == 0 ? 255 : 220;

                poseStack.pushPose();
                poseStack.translate(px, trackY + 0.02f, pz);
                poseStack.mulPose(Axis.YP.rotation(-a));
                Matrix4f m = poseStack.last().pose();
                addSolidGlowBox(glowConsumer, m, -hSize, hSize, -hSize, hSize, -hSize, hSize,
                        rCol, gCol, 255, alpha, fullBright, packedOverlay);
                poseStack.popPose();
            }

            // Beam B (Counter-Clockwise) - Solar Gold / Magenta Plasma + Comet Tail
            for (int t = 8; t >= 0; t--) {
                float b = -angleA + t * 0.13f;
                float px = cx + (float) Math.cos(b) * trackRadius;
                float pz = cz + (float) Math.sin(b) * trackRadius;
                float hSize = t == 0 ? 0.145f : (0.11f - t * 0.01f);
                int alpha = t == 0 ? 250 : Math.max(40, 210 - t * 22);
                int gCol = t == 0 ? 240 : Math.max(60, 175 - t * 14);
                int bCol = t == 0 ? 150 : 210;

                poseStack.pushPose();
                poseStack.translate(px, trackY + 0.02f, pz);
                poseStack.mulPose(Axis.YP.rotation(-b));
                Matrix4f m = poseStack.last().pose();
                addSolidGlowBox(glowConsumer, m, -hSize, hSize, -hSize, hSize, -hSize, hSize,
                        255, gCol, bCol, alpha, fullBright, packedOverlay);
                poseStack.popPose();
            }

            // Render actual 3D items orbiting inside Beam A & Beam B
            ItemStack itemA = entity.getItem(OlympianHadronColliderBlockEntity.SLOT_BEAM_A);
            if (!itemA.isEmpty()) {
                float ax = cx + (float) Math.cos(angleA) * trackRadius;
                float az = cz + (float) Math.sin(angleA) * trackRadius;
                renderOrbitingItem(itemA, ax, trackY + 0.02f, az, (nowMs / 4.0f) % 360.0f, poseStack, bufferSource, level, fullBright);
            }
            ItemStack itemB = entity.getItem(OlympianHadronColliderBlockEntity.SLOT_BEAM_B);
            if (!itemB.isEmpty()) {
                float bx = cx + (float) Math.cos(-angleA) * trackRadius;
                float bz = cz + (float) Math.sin(-angleA) * trackRadius;
                renderOrbitingItem(itemB, bx, trackY + 0.02f, bz, -(nowMs / 4.0f) % 360.0f, poseStack, bufferSource, level, fullBright);
            }
        }

        // 3. Collision Shockwave Ring & Core Flash
        int flash = entity.getCollisionFlashTicks();
        if (flash > 0) {
            float prog = 1.0f - (flash / 30.0f);
            float ringRad = 0.25f + prog * 2.15f;
            int ringAlpha = Math.max(20, (int) ((1.0f - prog) * 245));

            // Core flash orb
            if (flash > 10) {
                float coreR = 0.45f * (flash - 10) / 20.0f;
                poseStack.pushPose();
                poseStack.translate(cx, trackY + 0.15f, cz);
                poseStack.mulPose(Axis.YP.rotationDegrees((nowMs / 3.0f) % 360.0f));
                poseStack.mulPose(Axis.XP.rotationDegrees(35.0f));
                Matrix4f cm = poseStack.last().pose();
                addSolidGlowBox(glowConsumer, cm, -coreR, coreR, -coreR, coreR, -coreR, coreR,
                        255, 255, 255, 235, fullBright, packedOverlay);
                poseStack.popPose();
            }

            // Expanding shockwave ring segments
            int waveSegs = 20;
            for (int w = 0; w < waveSegs; w++) {
                double ang = w * (Math.PI * 2.0 / waveSegs);
                float wx = cx + (float) Math.cos(ang) * ringRad;
                float wz = cz + (float) Math.sin(ang) * ringRad;
                poseStack.pushPose();
                poseStack.translate(wx, trackY + 0.08f, wz);
                poseStack.mulPose(Axis.YP.rotation((float) -ang));
                Matrix4f wm = poseStack.last().pose();
                addSolidGlowBox(glowConsumer, wm, -0.06f, 0.06f, -0.05f, 0.05f, -0.22f, 0.22f,
                        180, 245, 255, ringAlpha, fullBright, packedOverlay);
                poseStack.popPose();
            }
        }
    }

    private void renderOrbitingItem(ItemStack stack, float x, float y, float z, float spinDeg,
                                    PoseStack poseStack, MultiBufferSource bufferSource, Level level, int light) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(spinDeg));
        poseStack.scale(0.45f, 0.45f, 0.45f);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.GROUND,
                light,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                level,
                0
        );
        poseStack.popPose();
    }

    private static void addSolidGlowBox(VertexConsumer c, Matrix4f m,
                                        float x0, float x1, float y0, float y1, float z0, float z1,
                                        int r, int g, int b, int a, int light, int overlay) {
        // Top (+Y)
        c.addVertex(m, x0, y1, z0).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        c.addVertex(m, x0, y1, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        c.addVertex(m, x1, y1, z1).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        c.addVertex(m, x1, y1, z0).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);

        // Bottom (-Y)
        c.addVertex(m, x0, y0, z0).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        c.addVertex(m, x1, y0, z0).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        c.addVertex(m, x1, y0, z1).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        c.addVertex(m, x0, y0, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);

        // North (-Z)
        c.addVertex(m, x0, y1, z0).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        c.addVertex(m, x1, y1, z0).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        c.addVertex(m, x1, y0, z0).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        c.addVertex(m, x0, y0, z0).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);

        // South (+Z)
        c.addVertex(m, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        c.addVertex(m, x0, y1, z1).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        c.addVertex(m, x0, y0, z1).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        c.addVertex(m, x1, y0, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);

        // West (-X)
        c.addVertex(m, x0, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        c.addVertex(m, x0, y1, z0).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        c.addVertex(m, x0, y0, z0).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        c.addVertex(m, x0, y0, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);

        // East (+X)
        c.addVertex(m, x1, y1, z0).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        c.addVertex(m, x1, y1, z1).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        c.addVertex(m, x1, y0, z1).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        c.addVertex(m, x1, y0, z0).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
    }
}

