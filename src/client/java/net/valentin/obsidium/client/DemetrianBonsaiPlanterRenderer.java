package net.valentin.obsidium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.DemetrianBonsaiPlanterBlockEntity;

public class DemetrianBonsaiPlanterRenderer implements BlockEntityRenderer<DemetrianBonsaiPlanterBlockEntity> {

    public DemetrianBonsaiPlanterRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DemetrianBonsaiPlanterBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack sapling = entity.getSapling();
        if (sapling.isEmpty()) return;

        float progress = entity.getGrowthRatio();
        // Trees are firmly rooted and STATIC - they do NOT spin while growing!
        float staticAngle = (Math.abs(entity.getBlockPos().getX() * 31 + entity.getBlockPos().getZ() * 17) % 4) * 90.0F;

        // Stage 0: Sprout (Progress < 0.20) -> Render 2D Sapling Item emerging from soil
        if (progress < 0.20F) {
            renderSprout(sapling, progress / 0.20F, staticAngle, entity, poseStack, bufferSource, packedLight);
            return;
        }

        // Stages 1, 2, 3: Full 3D Miniature Tree with realistic logs & leaves!
        TreeSpecies species = getTreeSpecies(sapling.getItem());
        renderMiniTree(species, sapling.getItem(), progress, staticAngle, poseStack, bufferSource, packedLight);
    }

    private void renderSprout(ItemStack sapling, float subProgress, float staticAngle, DemetrianBonsaiPlanterBlockEntity entity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // Compact, clean sprout scale
        float scale = 0.45F + 0.20F * subProgress;
        float centerY = 0.30F + (scale * 0.45F);
        poseStack.translate(0.5, centerY, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(staticAngle));
        poseStack.scale(scale, scale, scale);

        // Crossed faces for full foliage representation
        Minecraft.getInstance().getItemRenderer().renderStatic(
                sapling,
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                entity.getLevel(),
                0
        );

        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        Minecraft.getInstance().getItemRenderer().renderStatic(
                sapling,
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                entity.getLevel(),
                0
        );

        poseStack.popPose();
    }

    private void renderMiniTree(TreeSpecies species, Item saplingItem, float progress, float staticAngle, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // Base at center of the pot, resting right on top of the soil (Y = 0.30)
        poseStack.translate(0.5, 0.30, 0.5);
        // Fixed static orientation - does NOT spin!
        poseStack.mulPose(Axis.YP.rotationDegrees(staticAngle));

        // Refined compact bonsai scaling: neatly fits within the ceramic tray
        float miniScale;
        int stage; // 1 = young trunk, 2 = developing canopy, 3 = mature full bonsai

        if (progress < 0.50F) {
            // Stage 1: Young Bonsai (0.20 to 0.50)
            float t = (progress - 0.20F) / 0.30F;
            miniScale = 0.12F + 0.03F * t; // 0.12 to 0.15
            stage = 1;
        } else if (progress < 0.80F) {
            // Stage 2: Developing Canopy (0.50 to 0.80)
            float t = (progress - 0.50F) / 0.30F;
            miniScale = 0.15F + 0.04F * t; // 0.15 to 0.19
            stage = 2;
        } else {
            // Stage 3: Full Mature Bonsai (0.80 to 1.0)
            float t = Math.min(1.0F, (progress - 0.80F) / 0.20F);
            miniScale = 0.19F + 0.04F * t; // 0.19 to 0.23
            stage = 3;
        }

        // Slight bonus scale for Pyretic Titan Mega Bonsai
        if (saplingItem == ObsidiumMod.PYRETIC_TITAN_SAPLING_ITEM) {
            miniScale *= 1.10F;
        }

        poseStack.scale(miniScale, miniScale, miniScale);

        BlockState log = species.log();
        BlockState leaves = species.leaves();

        // 1. MINIATURIZED WOOD TRUNK
        renderMiniBlock(log, 0, 0, 0, poseStack, bufferSource, packedLight);
        renderMiniBlock(log, 0, 1, 0, poseStack, bufferSource, packedLight);

        if (stage >= 2) {
            renderMiniBlock(log, 0, 2, 0, poseStack, bufferSource, packedLight);
            // Lateral bonsai branch
            renderMiniBlock(log, 1, 1, 0, poseStack, bufferSource, packedLight);
        }
        if (stage >= 3) {
            // Upper branch on opposite side for natural bonsai aesthetic
            renderMiniBlock(log, -1, 2, 0, poseStack, bufferSource, packedLight);
        }

        // 2. MINIATURIZED FOLIAGE / CANOPY
        if (stage == 1) {
            // Young Stage: Compact 5-leaf cluster
            renderMiniBlock(leaves, 0, 2, 0, poseStack, bufferSource, packedLight);
            renderMiniBlock(leaves, 1, 2, 0, poseStack, bufferSource, packedLight);
            renderMiniBlock(leaves, -1, 2, 0, poseStack, bufferSource, packedLight);
            renderMiniBlock(leaves, 0, 2, 1, poseStack, bufferSource, packedLight);
            renderMiniBlock(leaves, 0, 2, -1, poseStack, bufferSource, packedLight);
            renderMiniBlock(leaves, 0, 3, 0, poseStack, bufferSource, packedLight);
        } else if (stage == 2) {
            // Developing Stage: Layered canopy
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    renderMiniBlock(leaves, dx, 2, dz, poseStack, bufferSource, packedLight);
                }
            }
            renderMiniBlock(leaves, 0, 3, 0, poseStack, bufferSource, packedLight);
            renderMiniBlock(leaves, 1, 3, 0, poseStack, bufferSource, packedLight);
            renderMiniBlock(leaves, -1, 3, 0, poseStack, bufferSource, packedLight);
            renderMiniBlock(leaves, 0, 3, 1, poseStack, bufferSource, packedLight);
            renderMiniBlock(leaves, 0, 3, -1, poseStack, bufferSource, packedLight);
            renderMiniBlock(leaves, 0, 4, 0, poseStack, bufferSource, packedLight);
        } else {
            // Mature Stage: Complete 3D Bonsai Crown!
            // Lower Layer (Y=2): 3x3 leaves
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    renderMiniBlock(leaves, dx, 2, dz, poseStack, bufferSource, packedLight);
                }
            }

            // Mid Layer (Y=3): Rounded 3x3 leaves
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) <= 1) {
                        renderMiniBlock(leaves, dx, 3, dz, poseStack, bufferSource, packedLight);
                    }
                }
            }
            renderMiniBlock(leaves, 1, 3, 1, poseStack, bufferSource, packedLight);
            renderMiniBlock(leaves, -1, 3, -1, poseStack, bufferSource, packedLight);

            // Crown Top (Y=4): Top cap leaf
            renderMiniBlock(leaves, 0, 4, 0, poseStack, bufferSource, packedLight);
            renderMiniBlock(leaves, 1, 4, 0, poseStack, bufferSource, packedLight);
            renderMiniBlock(leaves, -1, 4, 0, poseStack, bufferSource, packedLight);
            renderMiniBlock(leaves, 0, 4, 1, poseStack, bufferSource, packedLight);
            renderMiniBlock(leaves, 0, 4, -1, poseStack, bufferSource, packedLight);
        }

        poseStack.popPose();
    }

    private void renderMiniBlock(BlockState state, int bx, int by, int bz, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        // A standard 1x1x1 block goes from (0,0,0) to (1,1,1).
        // By translating (bx - 0.5, by, bz - 0.5), block (0, by, 0) is perfectly centered on X=0, Z=0!
        poseStack.translate(bx - 0.5F, (float) by, bz - 0.5F);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                state,
                poseStack,
                bufferSource,
                packedLight,
                OverlayTexture.NO_OVERLAY
        );
        poseStack.popPose();
    }

    private TreeSpecies getTreeSpecies(Item item) {
        // Obsidium Ore Trees
        if (item == ObsidiumMod.HEPHAESTUS_VOLCANITE_SAPLING.asItem()) {
            return new TreeSpecies(ObsidiumMod.HEPHAESTUS_VOLCANITE_LOG.defaultBlockState(), ObsidiumMod.HEPHAESTUS_VOLCANITE_LEAVES.defaultBlockState(), true);
        }
        if (item == ObsidiumMod.PYRETIC_TITAN_SAPLING_ITEM) {
            return new TreeSpecies(ObsidiumMod.PYRETIC_TITAN_LOG.defaultBlockState(), ObsidiumMod.PYRETIC_TITAN_LEAVES.defaultBlockState(), true);
        }
        if (item == ObsidiumMod.MIDAS_GOLD_SAPLING.asItem()) {
            return new TreeSpecies(ObsidiumMod.MIDAS_GOLD_LOG.defaultBlockState(), ObsidiumMod.MIDAS_GOLD_LEAVES.defaultBlockState(), true);
        }
        if (item == ObsidiumMod.ARES_IRON_SAPLING.asItem()) {
            return new TreeSpecies(ObsidiumMod.ARES_IRON_LOG.defaultBlockState(), ObsidiumMod.ARES_IRON_LEAVES.defaultBlockState(), true);
        }
        if (item == ObsidiumMod.HERMES_COPPER_SAPLING.asItem()) {
            return new TreeSpecies(ObsidiumMod.HERMES_COPPER_LOG.defaultBlockState(), ObsidiumMod.HERMES_COPPER_LEAVES.defaultBlockState(), true);
        }
        if (item == ObsidiumMod.CRONUS_LEAD_SAPLING.asItem()) {
            return new TreeSpecies(ObsidiumMod.CRONUS_LEAD_LOG.defaultBlockState(), ObsidiumMod.CRONUS_LEAD_LEAVES.defaultBlockState(), true);
        }
        if (item == ObsidiumMod.ZEUS_TIN_SAPLING.asItem()) {
            return new TreeSpecies(ObsidiumMod.ZEUS_TIN_LOG.defaultBlockState(), ObsidiumMod.ZEUS_TIN_LEAVES.defaultBlockState(), true);
        }
        if (item == ObsidiumMod.ARTEMIS_SILVER_SAPLING.asItem()) {
            return new TreeSpecies(ObsidiumMod.ARTEMIS_SILVER_LOG.defaultBlockState(), ObsidiumMod.ARTEMIS_SILVER_LEAVES.defaultBlockState(), true);
        }
        if (item == ObsidiumMod.ELECTRUM_SAPLING.asItem()) {
            return new TreeSpecies(ObsidiumMod.ELECTRUM_LOG.defaultBlockState(), ObsidiumMod.ELECTRUM_LEAVES.defaultBlockState(), true);
        }
        if (item == ObsidiumMod.TARTAREAN_NETHERITE_SAPLING.asItem()) {
            return new TreeSpecies(ObsidiumMod.TARTAREAN_NETHERITE_LOG.defaultBlockState(), ObsidiumMod.TARTAREAN_NETHERITE_LEAVES.defaultBlockState(), true);
        }
        if (item == ObsidiumMod.HADES_COAL_SAPLING.asItem()) {
            return new TreeSpecies(Blocks.OAK_LOG.defaultBlockState(), Blocks.OAK_LEAVES.defaultBlockState(), true);
        }
        if (item == ObsidiumMod.POSEIDON_LAPIS_SAPLING.asItem()) {
            return new TreeSpecies(Blocks.SPRUCE_LOG.defaultBlockState(), Blocks.SPRUCE_LEAVES.defaultBlockState(), true);
        }
        if (item == ObsidiumMod.ASTRAPE_REDSTONE_SAPLING.asItem()) {
            return new TreeSpecies(Blocks.BIRCH_LOG.defaultBlockState(), Blocks.BIRCH_LEAVES.defaultBlockState(), true);
        }
        if (item == ObsidiumMod.APOLLO_QUARTZ_SAPLING.asItem()) {
            return new TreeSpecies(Blocks.BIRCH_LOG.defaultBlockState(), Blocks.BIRCH_LEAVES.defaultBlockState(), true);
        }
        if (item == ObsidiumMod.DIONYSIAN_AMETHYST_SAPLING.asItem()) {
            return new TreeSpecies(Blocks.DARK_OAK_LOG.defaultBlockState(), Blocks.DARK_OAK_LEAVES.defaultBlockState(), true);
        }
        if (item == ObsidiumMod.HYPERION_GLOWSTONE_SAPLING.asItem()) {
            return new TreeSpecies(Blocks.OAK_LOG.defaultBlockState(), Blocks.OAK_LEAVES.defaultBlockState(), true);
        }
        if (item == ObsidiumMod.PLOUTON_EMERALD_SAPLING.asItem()) {
            return new TreeSpecies(Blocks.JUNGLE_LOG.defaultBlockState(), Blocks.JUNGLE_LEAVES.defaultBlockState(), true);
        }
        if (item == ObsidiumMod.ADAMANTINE_DIAMOND_SAPLING.asItem()) {
            return new TreeSpecies(ObsidiumMod.HEPHAESTUS_VOLCANITE_LOG.defaultBlockState(), ObsidiumMod.TARTAREAN_NETHERITE_LEAVES.defaultBlockState(), true);
        }

        // Vanilla Saplings
        if (item == Items.BIRCH_SAPLING) {
            return new TreeSpecies(Blocks.BIRCH_LOG.defaultBlockState(), Blocks.BIRCH_LEAVES.defaultBlockState(), false);
        }
        if (item == Items.SPRUCE_SAPLING) {
            return new TreeSpecies(Blocks.SPRUCE_LOG.defaultBlockState(), Blocks.SPRUCE_LEAVES.defaultBlockState(), false);
        }
        if (item == Items.JUNGLE_SAPLING) {
            return new TreeSpecies(Blocks.JUNGLE_LOG.defaultBlockState(), Blocks.JUNGLE_LEAVES.defaultBlockState(), false);
        }
        if (item == Items.ACACIA_SAPLING) {
            return new TreeSpecies(Blocks.ACACIA_LOG.defaultBlockState(), Blocks.ACACIA_LEAVES.defaultBlockState(), false);
        }
        if (item == Items.DARK_OAK_SAPLING) {
            return new TreeSpecies(Blocks.DARK_OAK_LOG.defaultBlockState(), Blocks.DARK_OAK_LEAVES.defaultBlockState(), false);
        }
        if (item == Items.CHERRY_SAPLING) {
            return new TreeSpecies(Blocks.CHERRY_LOG.defaultBlockState(), Blocks.CHERRY_LEAVES.defaultBlockState(), false);
        }
        if (item == Items.MANGROVE_PROPAGULE) {
            return new TreeSpecies(Blocks.MANGROVE_LOG.defaultBlockState(), Blocks.MANGROVE_LEAVES.defaultBlockState(), false);
        }

        return new TreeSpecies(Blocks.OAK_LOG.defaultBlockState(), Blocks.OAK_LEAVES.defaultBlockState(), false);
    }

    private record TreeSpecies(BlockState log, BlockState leaves, boolean isMineral) {}
}
