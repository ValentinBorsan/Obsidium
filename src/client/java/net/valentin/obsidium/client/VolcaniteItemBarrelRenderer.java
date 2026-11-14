package net.valentin.obsidium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.block.VolcaniteItemBarrelBlock;
import net.valentin.obsidium.block.VolcaniteItemBarrelBlockEntity;

public class VolcaniteItemBarrelRenderer implements BlockEntityRenderer<VolcaniteItemBarrelBlockEntity> {

    public VolcaniteItemBarrelRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(VolcaniteItemBarrelBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = entity.getBlockState();
        if (!(state.getBlock() instanceof VolcaniteItemBarrelBlock)) return;

        Direction facing = state.getValue(VolcaniteItemBarrelBlock.FACING);
        ItemStack sample = entity.getStoredItemSample();
        long amount = entity.getStoredAmount();
        boolean locked = entity.isLocked();
        boolean voiding = entity.hasVoidUpgrade();

        int light = entity.getLevel() != null ? LevelRenderer.getLightColor(entity.getLevel(), entity.getBlockPos().relative(facing)) : packedLight;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        // Rotate to match the facing direction of the barrel
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> {}
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
            default -> {}
        }

        // Move to the front face plane
        poseStack.translate(0.0, 0.0, 0.505);

        Font font = Minecraft.getInstance().font;

        // 1. Render 3D Floating Item
        if (!sample.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.0, 0.10, 0.0);
            poseStack.scale(0.52f, 0.52f, 0.02f);
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    sample,
                    ItemDisplayContext.FIXED,
                    light,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    bufferSource,
                    entity.getLevel(),
                    0
            );
            poseStack.popPose();

            // 2. Render Formatted Amount String
            String countText;
            if (amount >= 1_000_000) {
                countText = String.format("%.1fM", amount / 1_000_000.0);
            } else if (amount >= 10_000) {
                countText = String.format("%.1fK", amount / 1_000.0);
            } else {
                countText = String.format("%,d", amount);
            }

            poseStack.pushPose();
            poseStack.translate(0.0, -0.22, 0.005);
            poseStack.scale(0.013f, -0.013f, 0.013f);
            int tw = font.width(countText);
            font.drawInBatch(
                    countText,
                    -tw / 2.0f,
                    0,
                    0xFFFFDD44,
                    false,
                    poseStack.last().pose(),
                    bufferSource,
                    Font.DisplayMode.POLYGON_OFFSET,
                    0,
                    light
            );
            poseStack.popPose();
        }

        // 3. Render Lock Icon if locked
        if (locked) {
            poseStack.pushPose();
            poseStack.translate(0.32, 0.32, 0.005);
            poseStack.scale(0.010f, -0.010f, 0.010f);
            font.drawInBatch(
                    "🔒",
                    -font.width("🔒") / 2.0f,
                    0,
                    0xFFFFCC00,
                    false,
                    poseStack.last().pose(),
                    bufferSource,
                    Font.DisplayMode.POLYGON_OFFSET,
                    0,
                    light
            );
            poseStack.popPose();
        }

        // 4. Render Void Badge if void upgrade installed
        if (voiding) {
            poseStack.pushPose();
            poseStack.translate(-0.30, 0.32, 0.005);
            poseStack.scale(0.008f, -0.008f, 0.008f);
            font.drawInBatch(
                    "VOID",
                    -font.width("VOID") / 2.0f,
                    0,
                    0xFFFF3366,
                    false,
                    poseStack.last().pose(),
                    bufferSource,
                    Font.DisplayMode.POLYGON_OFFSET,
                    0,
                    light
            );
            poseStack.popPose();
        }

        poseStack.popPose();
    }
}

