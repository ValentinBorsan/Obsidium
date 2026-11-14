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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.VolcaniteStoragePipeBlock;
import net.valentin.obsidium.block.VolcaniteStoragePipeBlockEntity;
import net.valentin.obsidium.block.VolcaniteStoragePipeBlockEntity.ModuleType;
import net.valentin.obsidium.block.VolcaniteUniversalConduitBlockEntity;
import org.joml.Matrix4f;

public class VolcaniteStoragePipeRenderer implements BlockEntityRenderer<VolcaniteStoragePipeBlockEntity> {
    private static final ResourceLocation EXTRACT_TEXTURE = ResourceLocation.fromNamespaceAndPath("obsidium", "textures/block/volcanite_pipe_module_extract.png");
    private static final ResourceLocation INSERT_TEXTURE = ResourceLocation.fromNamespaceAndPath("obsidium", "textures/block/volcanite_pipe_module_insert.png");

    public VolcaniteStoragePipeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(VolcaniteStoragePipeBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = entity.getBlockState();
        if (!(state.getBlock() instanceof VolcaniteStoragePipeBlock)) return;

        if (entity.getFacadeBlock() != null) {
            BlockState facadeState = entity.getFacadeBlock().defaultBlockState();
            poseStack.pushPose();
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                facadeState, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY
            );
            poseStack.popPose();
            if (!facadeState.is(net.minecraft.world.level.block.Blocks.GLASS)) {
                return;
            }
        }

        boolean isOpticFiber = entity.isOpticFiber();
        boolean isUniversal = entity instanceof VolcaniteUniversalConduitBlockEntity;
        VolcaniteUniversalConduitBlockEntity conduit = isUniversal ? (VolcaniteUniversalConduitBlockEntity) entity : null;
        long nowMs = System.currentTimeMillis();

        for (Direction dir : Direction.values()) {
            // NEVER render on a disconnected side
            if (!state.getValue(VolcaniteStoragePipeBlock.getProperty(dir))) {
                continue;
            }

            ModuleType module = entity.getModule(dir);
            int upgradeTier = entity.getEnergyUpgradeTier(dir);
            boolean isExternalTarget = isUniversal && entity.getLevel() != null
                    && VolcaniteStoragePipeBlockEntity.isExternalContainer(
                            entity.getLevel(),
                            entity.getBlockPos().relative(dir),
                            entity.getLevel().getBlockEntity(entity.getBlockPos().relative(dir)));

            if (module == ModuleType.NONE && upgradeTier == 0 && !isExternalTarget) continue;

            poseStack.pushPose();
            // Center of block
            poseStack.translate(0.5, 0.5, 0.5);

            // Rotate so local -Z points in direction 'dir' (towards the connected block)
            switch (dir) {
                case NORTH -> {} // Already pointing along -Z
                case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
                case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
                case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
                case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
                case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            }

            int type = entity.getTransferType(dir);
            int[] chRgb = getChannelRgb(type, conduit, nowMs);
            int mr, mg, mb;
            if (isOpticFiber) {
                // Optic Fiber Connectors: IMPORT (ModuleType.EXTRACT) = Cyber Cyan, EXPORT (ModuleType.INSERT) = Amber Gold
                if (module == ModuleType.EXTRACT) {
                    mr = 0; mg = 225; mb = 255; // Import Connector (Cyan-Blue)
                } else {
                    mr = 255; mg = 145; mb = 0; // Export Connector (Amber-Orange)
                }
            } else if (isUniversal) {
                // Universal Conduit Integrated Smart Collars:
                // EXTRACT (IMPORT) = Amber Orange, INSERT (EXPORT) = Cyber Cyan, NONE (AUTO) = Amethyst Obsidian
                if (module == ModuleType.EXTRACT) {
                    mr = 255; mg = 140; mb = 0;
                } else if (module == ModuleType.INSERT) {
                    mr = 0; mg = 225; mb = 255;
                } else {
                    mr = 130; mg = 65; mb = 210;
                }
            } else {
                // Standard Pipe Modules: EXTRACT = Orange/Gold, INSERT = Cyan/Blue, Energy-Only Upgrade = Tier color
                if (module == ModuleType.EXTRACT) {
                    mr = 255; mg = 140; mb = 0;
                } else if (module == ModuleType.INSERT) {
                    mr = 0; mg = 220; mb = 255;
                } else {
                    switch (upgradeTier) {
                        case 2 -> { mr = 0; mg = 220; mb = 255; }
                        case 3 -> { mr = 230; mg = 60; mb = 255; }
                        default -> { mr = 255; mg = 185; mb = 0; }
                    }
                }
            }

            int jr, jg, jb;
            if (isUniversal) {
                jr = chRgb[0]; jg = chRgb[1]; jb = chRgb[2];
            } else {
                // Medium type colors for jewel: ITEMS = Green, LIQUIDS = Deep Blue (or Tier color if energy upgrade present)
                jr = (upgradeTier == 3) ? 255 : ((upgradeTier == 2) ? 0 : ((upgradeTier == 1) ? 255 : 0));
                jg = (upgradeTier == 3) ? 80 : ((upgradeTier == 2) ? 240 : ((upgradeTier == 1) ? 200 : ((type == 0) ? 255 : 150)));
                jb = (upgradeTier == 3) ? 255 : ((upgradeTier == 2) ? 255 : ((upgradeTier == 1) ? 40 : ((type == 0) ? 120 : 255)));
            }

            ResourceLocation texture = (module == ModuleType.INSERT) ? INSERT_TEXTURE : EXTRACT_TEXTURE;
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(texture));
            Matrix4f matrix = poseStack.last().pose();
            int glowLight = 0x00F000F0;
            boolean redstoneActive = !isUniversal || conduit.isFaceActiveByRedstone(dir);
            int ringLight = redstoneActive ? glowLight : packedLight;

            if (isOpticFiber) {
                // Sleek Stepped Fiber-Optic Cable Connector Plug:
                renderBox(consumer, matrix, -0.17F, 0.17F, -0.17F, 0.17F, -0.34F, -0.22F, 45, 42, 55, 255, packedLight, packedOverlay);
                renderBox(consumer, matrix, -0.235F, 0.235F, -0.235F, 0.235F, -0.44F, -0.34F, mr, mg, mb, 255, packedLight, packedOverlay);
                renderBox(consumer, matrix, -0.3125F, 0.3125F, -0.3125F, 0.3125F, -0.5005F, -0.44F, 35, 32, 42, 255, packedLight, packedOverlay);
                renderBox(consumer, matrix, -0.242F, 0.242F, -0.242F, 0.242F, -0.40F, -0.37F, mr, mg, mb, 255, glowLight, packedOverlay);
                renderBox(consumer, matrix, -0.315F, 0.315F, -0.315F, 0.315F, -0.495F, -0.475F, mr, mg, mb, 255, glowLight, packedOverlay);
                renderBox(consumer, matrix, -0.09F, 0.09F, -0.09F, 0.09F, -0.502F, -0.45F, jr, jg, jb, 255, glowLight, packedOverlay);
            } else if (isUniversal) {
                // Sleek Integrated Smart Conduit Collar:
                // 1. Dark obsidian collar cuff (-0.41 to -0.27 in Z)
                renderBox(consumer, matrix, -0.24F, 0.24F, -0.24F, 0.24F, -0.41F, -0.27F, 28, 22, 40, 255, packedLight, packedOverlay);
                // 2. Import/Export/Auto status band around cuff
                int bandR = redstoneActive ? mr : mr / 3;
                int bandG = redstoneActive ? mg : mg / 3;
                int bandB = redstoneActive ? mb : mb / 3;
                renderBox(consumer, matrix, -0.252F, 0.252F, -0.252F, 0.252F, -0.36F, -0.32F, bandR, bandG, bandB, 255, ringLight, packedOverlay);
                // 3. Flush machine docking flange (-0.5005 to -0.41 in Z)
                renderBox(consumer, matrix, -0.3125F, 0.3125F, -0.3125F, 0.3125F, -0.5005F, -0.41F, 38, 30, 52, 255, packedLight, packedOverlay);
                // 4. Channel-colored rim accent near machine face
                renderBox(consumer, matrix, -0.316F, 0.316F, -0.316F, 0.316F, -0.485F, -0.455F, jr, jg, jb, 255, ringLight, packedOverlay);
                // 5. Glowing channel jewel badge
                renderBox(consumer, matrix, -0.095F, 0.095F, -0.095F, 0.095F, -0.502F, -0.45F, jr, jg, jb, 255, ringLight, packedOverlay);
            } else {
                // Standard 6x6 Pipe Collar & Flange
                renderBox(consumer, matrix, -0.25F, 0.25F, -0.25F, 0.25F, -0.40F, -0.25F, mr, mg, mb, 255, packedLight, packedOverlay);
                renderBox(consumer, matrix, -0.3125F, 0.3125F, -0.3125F, 0.3125F, -0.5005F, -0.40F, mr, mg, mb, 255, packedLight, packedOverlay);
                renderBox(consumer, matrix, -0.255F, 0.255F, -0.255F, 0.255F, -0.34F, -0.31F, mr, mg, mb, 255, glowLight, packedOverlay);
                renderBox(consumer, matrix, -0.10F, 0.10F, -0.10F, 0.10F, -0.502F, -0.45F, jr, jg, jb, 255, glowLight, packedOverlay);
            }

            // Optional: Render small 3D icon of filtered item on the connector
            ItemStack filter = entity.getFilterItem(dir);
            if (!filter.isEmpty()) {
                poseStack.pushPose();
                poseStack.translate(0, isOpticFiber ? 0.245F : 0.26F, isOpticFiber ? -0.39F : -0.335F);
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
                poseStack.scale(0.20F, 0.20F, 0.20F);
                Minecraft.getInstance().getItemRenderer().renderStatic(
                    filter, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, entity.getLevel(), 0
                );
                poseStack.popPose();
            }

            poseStack.popPose();
        }

        // Render dynamic color-changing core, arm rails, and animated 3D flow pulses!
        if (isUniversal || isOpticFiber || entity.hasAnyModule() || entity.hasAnyEnergyUpgrade()) {
            int maxUpg = entity.getHighestEnergyUpgradeTier();
            int pType = entity.getPrimaryTransferType();
            int cr, cg, cb;
            if (isUniversal) {
                int[] coreRgb = getChannelRgb(pType, conduit, nowMs);
                cr = coreRgb[0]; cg = coreRgb[1]; cb = coreRgb[2];
            } else if (isOpticFiber) {
                if (entity.hasAnyModule()) {
                    cr = 0;
                    cg = (pType == 0) ? 245 : 190;
                    cb = (pType == 0) ? 200 : 255;
                } else {
                    cr = 0; cg = 225; cb = 255;
                }
            } else {
                cr = (maxUpg == 3) ? 235 : ((maxUpg == 2) ? 0 : ((maxUpg == 1) ? 255 : 0));
                cg = (maxUpg == 3) ? 70 : ((maxUpg == 2) ? 225 : ((maxUpg == 1) ? 185 : ((pType == 0) ? 255 : 210)));
                cb = (maxUpg == 3) ? 255 : ((maxUpg == 2) ? 255 : ((maxUpg == 1) ? 0 : ((pType == 0) ? 102 : 255)));
            }
            int glowLight = 0x00F000F0;

            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            Matrix4f matrix = poseStack.last().pose();
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(EXTRACT_TEXTURE));

            float coreRadius = isOpticFiber ? 0.085F : (isUniversal ? 0.145F : 0.15F);
            float armRadius = isOpticFiber ? 0.05F : (isUniversal ? 0.105F : 0.08F);
            renderBox(consumer, matrix, -coreRadius, coreRadius, -coreRadius, coreRadius, -coreRadius, coreRadius, cr, cg, cb, 255, glowLight, packedOverlay);

            // Outer core corner highlight dots for Universal Conduit
            if (isUniversal) {
                float cOut = 0.192F;
                float cIn = 0.145F;
                renderBox(consumer, matrix, -cOut, -cIn, -cOut, -cIn, -cOut, cOut, cr, cg, cb, 255, glowLight, packedOverlay);
                renderBox(consumer, matrix, cIn, cOut, -cOut, -cIn, -cOut, cOut, cr, cg, cb, 255, glowLight, packedOverlay);
                renderBox(consumer, matrix, -cOut, -cIn, cIn, cOut, -cOut, cOut, cr, cg, cb, 255, glowLight, packedOverlay);
                renderBox(consumer, matrix, cIn, cOut, cIn, cOut, -cOut, cOut, cr, cg, cb, 255, glowLight, packedOverlay);
            }

            // Glowing conduits & 3D animated flow pulses extending through connected arms
            for (Direction d : Direction.values()) {
                if (state.getValue(VolcaniteStoragePipeBlock.getProperty(d))) {
                    poseStack.pushPose();
                    switch (d) {
                        case NORTH -> {}
                        case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
                        case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
                        case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
                        case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
                        case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                    }
                    Matrix4f armMat = poseStack.last().pose();
                    int ar = cr, ag = cg, ab = cb;
                    boolean redstoneOn = true;
                    if (isUniversal) {
                        int[] armRgb = getChannelRgb(conduit.getTransferType(d), conduit, nowMs);
                        redstoneOn = conduit.isFaceActiveByRedstone(d);
                        ar = redstoneOn ? armRgb[0] : (armRgb[0] / 3);
                        ag = redstoneOn ? armRgb[1] : (armRgb[1] / 3);
                        ab = redstoneOn ? armRgb[2] : (armRgb[2] / 3);
                    }
                    int armLight = redstoneOn ? glowLight : packedLight;
                    renderBox(consumer, armMat, -armRadius, armRadius, -armRadius, armRadius, -0.49F, -coreRadius, ar, ag, ab, 255, armLight, packedOverlay);

                    if (isUniversal) {
                        // 4 thin neon accent rails hugging the 6x6 obsidian pipe corners so channel color pops from outside too
                        float rOut = 0.193F;
                        float rIn = 0.168F;
                        renderBox(consumer, armMat, -rOut, -rIn, -rOut, -rIn, -0.495F, -coreRadius, ar, ag, ab, 255, armLight, packedOverlay);
                        renderBox(consumer, armMat, rIn, rOut, -rOut, -rIn, -0.495F, -coreRadius, ar, ag, ab, 255, armLight, packedOverlay);
                        renderBox(consumer, armMat, -rOut, -rIn, rIn, rOut, -0.495F, -coreRadius, ar, ag, ab, 255, armLight, packedOverlay);
                        renderBox(consumer, armMat, rIn, rOut, rIn, rOut, -0.495F, -coreRadius, ar, ag, ab, 255, armLight, packedOverlay);

                        // 3D Animated Flow Pulse Rings along the arm!
                        if (redstoneOn) {
                            ModuleType armMode = conduit.getModule(d);
                            boolean hasActivity = armMode != ModuleType.NONE
                                    || conduit.getStoredEnergy() > 0
                                    || conduit.getStoredFluidMb() > 0;
                            if (hasActivity) {
                                float phase = (float) ((nowMs % 1200L) / 1200.0);
                                for (int p = 0; p < 2; p++) {
                                    float t = (phase + p * 0.5F) % 1.0F;
                                    // EXTRACT pulls IN (-0.46 -> -0.17); INSERT pushes OUT (-0.17 -> -0.46)
                                    float zPos = (armMode == ModuleType.EXTRACT)
                                            ? (-0.46F + t * 0.29F)
                                            : (-0.17F - t * 0.29F);
                                    float pr = Math.min(255, ar + 50);
                                    float pg = Math.min(255, ag + 50);
                                    float pb = Math.min(255, ab + 50);
                                    renderBox(consumer, armMat, -0.198F, 0.198F, -0.198F, 0.198F, zPos - 0.018F, zPos + 0.018F,
                                            (int) pr, (int) pg, (int) pb, 255, glowLight, packedOverlay);
                                }
                            }
                        }
                    }

                    poseStack.popPose();
                }
            }

            poseStack.popPose();
        }
    }

    private static int[] getChannelRgb(int type, VolcaniteUniversalConduitBlockEntity conduit, long nowMs) {
        return switch (type) {
            case VolcaniteStoragePipeBlockEntity.TYPE_ITEM -> new int[]{16, 245, 110};    // Emerald Green
            case VolcaniteStoragePipeBlockEntity.TYPE_FLUID -> {
                if (conduit != null && conduit.getStoredFluidMb() > 0) {
                    var fluid = conduit.getStoredFluidVariant();
                    if (fluid.isOf(Fluids.LAVA)) yield new int[]{255, 105, 15};
                    if (fluid.isOf(Fluids.WATER)) yield new int[]{40, 145, 255};
                    if (fluid.isOf(ObsidiumMod.BIO_OIL_FLUID)) yield new int[]{245, 165, 20};
                    if (fluid.isOf(ObsidiumMod.BIO_ETHANOL_FLUID)) yield new int[]{34, 215, 240};
                    if (fluid.isOf(ObsidiumMod.BIODIESEL_FLUID)) yield new int[]{74, 222, 128};
                    if (fluid.isOf(ObsidiumMod.PYRO_DIESEL_FLUID)) yield new int[]{245, 65, 35};
                    if (fluid.isOf(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID)) yield new int[]{235, 35, 145};
                    if (fluid.isOf(ObsidiumMod.STEAM_FLUID)) yield new int[]{210, 225, 235};
                }
                yield new int[]{0, 215, 255}; // Cyber Aqua-Cyan
            }
            case VolcaniteStoragePipeBlockEntity.TYPE_ENERGY -> new int[]{255, 115, 15}; // Electric Magma Gold-Red
            default -> {
                // TYPE_OMNI: Smooth Royal Amethyst <-> Volcanite Gold pulse
                float pulse = 0.5F + 0.5F * (float) Math.sin(nowMs / 450.0);
                int r = (int) (217 + (250 - 217) * pulse);
                int g = (int) (70 + (204 - 70) * pulse);
                int b = (int) (239 + (21 - 239) * pulse);
                yield new int[]{r, g, b};
            }
        };
    }

    private static void renderBox(VertexConsumer consumer, Matrix4f matrix,
                                  float minX, float maxX, float minY, float maxY, float minZ, float maxZ,
                                  int r, int g, int b, int a,
                                  int packedLight, int packedOverlay) {
        // North face (Z = minZ, normal 0, 0, -1) - facing towards machine
        addQuad(consumer, matrix,
            minX, maxY, minZ, 0, 0,
            maxX, maxY, minZ, 1, 0,
            maxX, minY, minZ, 1, 1,
            minX, minY, minZ, 0, 1,
            r, g, b, a,
            0, 0, -1, packedLight, packedOverlay);

        // South face (Z = maxZ, normal 0, 0, 1) - facing towards pipe core
        addQuad(consumer, matrix,
            maxX, maxY, maxZ, 0, 0,
            minX, maxY, maxZ, 1, 0,
            minX, minY, maxZ, 1, 1,
            maxX, minY, maxZ, 0, 1,
            r, g, b, a,
            0, 0, 1, packedLight, packedOverlay);

        // West face (X = minX, normal -1, 0, 0)
        addQuad(consumer, matrix,
            minX, maxY, maxZ, 0, 0,
            minX, maxY, minZ, 1, 0,
            minX, minY, minZ, 1, 1,
            minX, minY, maxZ, 0, 1,
            r, g, b, a,
            -1, 0, 0, packedLight, packedOverlay);

        // East face (X = maxX, normal 1, 0, 0)
        addQuad(consumer, matrix,
            maxX, maxY, minZ, 0, 0,
            maxX, maxY, maxZ, 1, 0,
            maxX, minY, maxZ, 1, 1,
            maxX, minY, minZ, 0, 1,
            r, g, b, a,
            1, 0, 0, packedLight, packedOverlay);

        // Up face (Y = maxY, normal 0, 1, 0)
        addQuad(consumer, matrix,
            minX, maxY, maxZ, 0, 1,
            maxX, maxY, maxZ, 1, 1,
            maxX, maxY, minZ, 1, 0,
            minX, maxY, minZ, 0, 0,
            r, g, b, a,
            0, 1, 0, packedLight, packedOverlay);

        // Down face (Y = minY, normal 0, -1, 0)
        addQuad(consumer, matrix,
            minX, minY, minZ, 0, 0,
            maxX, minY, minZ, 1, 0,
            maxX, minY, maxZ, 1, 1,
            minX, minY, maxZ, 0, 1,
            r, g, b, a,
            0, -1, 0, packedLight, packedOverlay);
    }

    private static void addQuad(VertexConsumer consumer, Matrix4f matrix,
                                float x1, float y1, float z1, float u1, float v1,
                                float x2, float y2, float z2, float u2, float v2,
                                float x3, float y3, float z3, float u3, float v3,
                                float x4, float y4, float z4, float u4, float v4,
                                int r, int g, int b, int a,
                                float nx, float ny, float nz, int packedLight, int packedOverlay) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(nx, ny, nz);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(u2, v2).setOverlay(packedOverlay).setLight(packedLight).setNormal(nx, ny, nz);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(u3, v3).setOverlay(packedOverlay).setLight(packedLight).setNormal(nx, ny, nz);
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(u4, v4).setOverlay(packedOverlay).setLight(packedLight).setNormal(nx, ny, nz);
    }
}
