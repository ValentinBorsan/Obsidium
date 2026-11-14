package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.DemetrianBonsaiPlanterBlock;
import net.valentin.obsidium.block.DemetrianBonsaiPlanterBlockEntity;
import net.valentin.obsidium.block.GraftedSaplingBlock;
import net.valentin.obsidium.block.GraftedSaplingBlockEntity;

import java.util.List;

public class PollinatingBrushItem extends Item {
    public static final int MAX_CHARGES = 8;

    public PollinatingBrushItem(Properties properties) {
        super(properties.durability(128).rarity(Rarity.UNCOMMON));
    }

    public static String getChargedSpecies(ItemStack stack) {
        if (!stack.has(DataComponents.CUSTOM_DATA)) return "";
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getString("Species");
    }

    public static int getCharges(ItemStack stack) {
        if (!stack.has(DataComponents.CUSTOM_DATA)) return 0;
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getInt("Charges");
    }

    public static void setCharge(ItemStack stack, String species, int charges) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString("Species", species != null ? species : "");
            tag.putInt("Charges", Math.max(0, Math.min(MAX_CHARGES, charges)));
        });
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        Block block = state.getBlock();

        // 1. Gathering Pollen from Tree Leaves
        if (state.is(BlockTags.LEAVES) || block.getDescriptionId().contains("leaves")) {
            String species = identifyLeafSpecies(block);
            if (!level.isClientSide()) {
                setCharge(stack, species, MAX_CHARGES);
                stack.hurtAndBreak(1, player, Player.getSlotForHand(context.getHand()));

                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.05);
                }
            }
            level.playSound(player, pos, SoundEvents.BRUSH_GENERIC, SoundSource.BLOCKS, 0.8F, 1.2F);
            if (player != null && !level.isClientSide()) {
                player.displayClientMessage(Component.literal("§a✔ Harvested Sacred Pollen: §6" + DemetrianScionItem.formatSpeciesName(species)), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // 2. Pollinating a Grafted Sapling
        if (block instanceof GraftedSaplingBlock) {
            int charges = getCharges(stack);
            if (charges > 0) {
                if (!level.isClientSide()) {
                    if (level.getBlockEntity(pos) instanceof GraftedSaplingBlockEntity be) {
                        be.forceMetamorphosis();
                    }
                    setCharge(stack, getChargedSpecies(stack), charges - 1);
                    stack.hurtAndBreak(1, player, Player.getSlotForHand(context.getHand()));

                    if (level instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 15, 0.25, 0.2, 0.25, 0.05);
                        sl.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 8, 0.1, 0.1, 0.1, 0.02);
                    }
                }
                level.playSound(player, pos, SoundEvents.BRUSH_SAND_COMPLETED, SoundSource.BLOCKS, 0.9F, 1.4F);
                if (player != null && !level.isClientSide()) {
                    player.displayClientMessage(Component.literal("§6✦ Pollination Successful! Accelerated metamorphosis triggered."), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            } else {
                if (player != null && level.isClientSide()) {
                    player.displayClientMessage(Component.literal("§cBrush has no pollen charges! Brush leaves first."), true);
                }
                return InteractionResult.FAIL;
            }
        }

        // 3. Pollinating a Demetrian Bonsai Planter
        if (block instanceof DemetrianBonsaiPlanterBlock) {
            int charges = getCharges(stack);
            if (charges > 0) {
                if (!level.isClientSide()) {
                    if (level.getBlockEntity(pos) instanceof DemetrianBonsaiPlanterBlockEntity bonsai && bonsai.hasSapling()) {
                        bonsai.advanceGrowth(80);
                        setCharge(stack, getChargedSpecies(stack), charges - 1);
                        stack.hurtAndBreak(1, player, Player.getSlotForHand(context.getHand()));

                        if (level instanceof ServerLevel sl) {
                            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.25, 0.2, 0.25, 0.05);
                            sl.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 6, 0.1, 0.1, 0.1, 0.02);

                            // 5% Rare Hybrid Mutation into Pyretic Titan Volcanite Bonsai!
                            boolean isVolcanic = bonsai.getSapling().is(ObsidiumMod.HEPHAESTUS_VOLCANITE_SAPLING.asItem());
                            if (isVolcanic && sl.random.nextFloat() < 0.05F) {
                                bonsai.setItem(DemetrianBonsaiPlanterBlockEntity.SLOT_SAPLING, new ItemStack(ObsidiumMod.PYRETIC_TITAN_SAPLING_ITEM));
                                bonsai.syncClient();
                                sl.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0F, 1.0F);
                                sl.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 25, 0.3, 0.3, 0.3, 0.05);
                                if (player != null) {
                                    player.displayClientMessage(Component.literal("§6★ RARE HYBRID MUTATION! Pyretic Titan Volcanite Bonsai manifested! (5% chance)"), true);
                                }
                            }
                        }
                    }
                }
                level.playSound(player, pos, SoundEvents.BRUSH_SAND_COMPLETED, SoundSource.BLOCKS, 0.9F, 1.4F);
                if (player != null && !level.isClientSide()) {
                    player.displayClientMessage(Component.literal("§6✦ Sacred Pollen Applied! Bonsai growth accelerated."), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            } else {
                if (player != null && level.isClientSide()) {
                    player.displayClientMessage(Component.literal("§cBrush has no pollen charges! Brush leaves first."), true);
                }
                return InteractionResult.FAIL;
            }
        }

        return super.useOn(context);
    }

    private String identifyLeafSpecies(Block leaves) {
        if (leaves == ObsidiumMod.MIDAS_GOLD_LEAVES) return "midas_gold";
        if (leaves == ObsidiumMod.ARES_IRON_LEAVES) return "ares_iron";
        if (leaves == ObsidiumMod.HERMES_COPPER_LEAVES) return "hermes_copper";
        if (leaves == ObsidiumMod.HEPHAESTUS_VOLCANITE_LEAVES) return "hephaestus_volcanite";
        if (leaves == ObsidiumMod.CRONUS_LEAD_LEAVES) return "cronus_lead";
        if (leaves == ObsidiumMod.ZEUS_TIN_LEAVES) return "zeus_tin";
        if (leaves == ObsidiumMod.ARTEMIS_SILVER_LEAVES) return "artemis_silver";
        if (leaves == ObsidiumMod.ELECTRUM_LEAVES) return "electrum";
        if (leaves == ObsidiumMod.TARTAREAN_NETHERITE_LEAVES) return "tartarean_netherite";
        if (leaves == Blocks.BIRCH_LEAVES) return "birch";
        if (leaves == Blocks.SPRUCE_LEAVES) return "spruce";
        if (leaves == Blocks.JUNGLE_LEAVES) return "jungle";
        if (leaves == Blocks.ACACIA_LEAVES) return "acacia";
        if (leaves == Blocks.DARK_OAK_LEAVES) return "dark_oak";
        if (leaves == Blocks.CHERRY_LEAVES) return "cherry";
        return "oak";
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int charges = getCharges(stack);
        String species = getChargedSpecies(stack);

        tooltip.add(Component.literal("§6✦ Demetrian Pollinating Brush").withStyle(ChatFormatting.GOLD));
        if (charges > 0 && !species.isEmpty()) {
            tooltip.add(Component.literal("§e• Charged Pollen: §a" + DemetrianScionItem.formatSpeciesName(species) + " §7(" + charges + "/" + MAX_CHARGES + ")"));
        } else {
            tooltip.add(Component.literal("§8• Empty (Right-click tree leaves to collect pollen)"));
        }
        tooltip.add(Component.literal("§7• Right-click leaves to harvest species pollen."));
        tooltip.add(Component.literal("§7• Right-click saplings to fecundate & accelerate growth."));
    }
}
