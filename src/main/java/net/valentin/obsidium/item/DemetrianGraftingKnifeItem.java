package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.GraftedSaplingBlockEntity;
import net.valentin.obsidium.block.MineralTreeLeavesBlock;
import net.valentin.obsidium.block.OreTreeSaplingBlock;

import java.util.List;

public class DemetrianGraftingKnifeItem extends Item {
    public static final float HYBRIDIZATION_CHANCE = 0.10f; // 10% chance

    public DemetrianGraftingKnifeItem(Properties properties) {
        super(properties.durability(250).fireResistant().rarity(Rarity.RARE));
    }

    private static String getSaplingSpecies(Block block) {
        if (block instanceof OreTreeSaplingBlock ore) return ore.species();
        if (block == Blocks.OAK_SAPLING) return "oak";
        if (block == Blocks.SPRUCE_SAPLING) return "spruce";
        if (block == Blocks.BIRCH_SAPLING) return "birch";
        if (block == Blocks.JUNGLE_SAPLING) return "jungle";
        if (block == Blocks.ACACIA_SAPLING) return "acacia";
        if (block == Blocks.DARK_OAK_SAPLING) return "dark_oak";
        if (block == Blocks.CHERRY_SAPLING) return "cherry";
        if (block == Blocks.MANGROVE_PROPAGULE) return "mangrove";
        return "oak";
    }

    private static String getLeavesSpecies(Block block) {
        if (block instanceof MineralTreeLeavesBlock leaves) return leaves.species();
        if (block == Blocks.OAK_LEAVES) return "oak";
        if (block == Blocks.SPRUCE_LEAVES) return "spruce";
        if (block == Blocks.BIRCH_LEAVES) return "birch";
        if (block == Blocks.JUNGLE_LEAVES) return "jungle";
        if (block == Blocks.ACACIA_LEAVES) return "acacia";
        if (block == Blocks.DARK_OAK_LEAVES) return "dark_oak";
        if (block == Blocks.CHERRY_LEAVES) return "cherry";
        if (block == Blocks.MANGROVE_LEAVES) return "mangrove";
        return null;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        // 1. Incise a Planted Sapling (Rootstock creation for 4-step grafting)
        if (block instanceof OreTreeSaplingBlock || block instanceof SaplingBlock) {
            if (!level.isClientSide()) {
                String species = getSaplingSpecies(block);
                ItemStack originalSapling = new ItemStack(block.asItem());

                level.setBlock(pos, ObsidiumMod.GRAFTED_SAPLING_BLOCK.defaultBlockState(), 3);
                if (level.getBlockEntity(pos) instanceof GraftedSaplingBlockEntity grafted) {
                    grafted.initRootstock(originalSapling, species);
                }

                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
                level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.8F, 1.4F);
                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.25, 0.25, 0.25, 0.05);
                }

                if (player != null) {
                    stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                    player.displayClientMessage(Component.literal("§6[Grafting] §a✦ Incised §e" + DemetrianScionItem.formatSpeciesName(species) + " §aRootstock! §7Right-click with a Scion cutting to graft."), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // 2. Harvest Scion Cuttings from Leaves (Shift + Right-Click)
        String leavesSpecies = getLeavesSpecies(block);
        if (leavesSpecies != null && player != null && player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                ItemStack scion = DemetrianScionItem.create(leavesSpecies, 2);
                if (!player.getInventory().add(scion)) {
                    level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, scion));
                }

                level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.1F);
                level.playSound(null, pos, SoundEvents.CHERRY_LEAVES_PLACE, SoundSource.BLOCKS, 0.8F, 1.3F);
                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.25, 0.25, 0.25, 0.05);
                }

                stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                player.displayClientMessage(Component.literal("§6[Grafting] §a✦ Harvested §e" + DemetrianScionItem.formatSpeciesName(leavesSpecies) + " Scion§a! §7Plant a rootstock sapling & right-click it with this knife to incise."), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // 3. Direct Leaf-to-Leaf Hybridization (Regular Right-Click on Mineral Leaves)
        if (!(block instanceof MineralTreeLeavesBlock targetLeaves)) {
            if (player != null && !level.isClientSide()) {
                player.displayClientMessage(Component.literal("§6[Grafting] §cThis tool only grafts mineral tree leaves or saplings! §7[Tip: Shift+Right-Click leaves for Scion]"), true);
            }
            return InteractionResult.PASS;
        }

        String targetSpecies = targetLeaves.species();

        // Check compatibility
        String requiredPartner = null;
        String altPartner = null;
        Item resultSapling = null;
        String resultName = null;

        if ("midas_gold".equals(targetSpecies)) {
            requiredPartner = "artemis_silver";
            altPartner = "hermes_copper";
            resultSapling = ObsidiumMod.ELECTRUM_SAPLING_ITEM;
            resultName = "Electrum Sapling";
        } else if ("artemis_silver".equals(targetSpecies)) {
            requiredPartner = "midas_gold";
            altPartner = "zeus_tin";
            resultSapling = ObsidiumMod.ELECTRUM_SAPLING_ITEM;
            resultName = "Electrum Sapling";
        } else if ("hermes_copper".equals(targetSpecies)) {
            requiredPartner = "midas_gold";
            altPartner = "zeus_tin";
            resultSapling = ObsidiumMod.ELECTRUM_SAPLING_ITEM;
            resultName = "Electrum Sapling";
        } else if ("zeus_tin".equals(targetSpecies)) {
            requiredPartner = "hermes_copper";
            altPartner = "midas_gold";
            resultSapling = ObsidiumMod.ELECTRUM_SAPLING_ITEM;
            resultName = "Electrum Sapling";
        } else if ("hephaestus_volcanite".equals(targetSpecies)) {
            requiredPartner = "ares_iron";
            altPartner = "cronus_lead";
            resultSapling = ObsidiumMod.TARTAREAN_NETHERITE_SAPLING_ITEM;
            resultName = "Tartarean Netherite Sapling";
        } else if ("ares_iron".equals(targetSpecies)) {
            requiredPartner = "hephaestus_volcanite";
            altPartner = "cronus_lead";
            resultSapling = ObsidiumMod.TARTAREAN_NETHERITE_SAPLING_ITEM;
            resultName = "Tartarean Netherite Sapling";
        } else if ("cronus_lead".equals(targetSpecies)) {
            requiredPartner = "hephaestus_volcanite";
            altPartner = "ares_iron";
            resultSapling = ObsidiumMod.TARTAREAN_NETHERITE_SAPLING_ITEM;
            resultName = "Tartarean Netherite Sapling";
        } else {
            if (player != null && !level.isClientSide()) {
                player.displayClientMessage(Component.literal("§6[Grafting] §eThis is already an apex hybrid tree (" + targetSpecies + ")!"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // Check if partner species is present in adjacent leaves (radius 4) OR held in offhand
        boolean partnerPresent = hasPartnerLeavesNearby(level, pos, requiredPartner, altPartner);
        if (!partnerPresent && player != null) {
            ItemStack offhand = player.getItemInHand(InteractionHand.OFF_HAND);
            partnerPresent = isPartnerItem(offhand, requiredPartner, altPartner);
        }

        if (!partnerPresent) {
            if (player != null && !level.isClientSide()) {
                String partnerDesc = formatSpeciesName(requiredPartner);
                if (altPartner != null) partnerDesc += " or " + formatSpeciesName(altPartner);
                player.displayClientMessage(Component.literal("§6[Grafting] §cNo compatible partner found! Grow near §e" + partnerDesc + " §cleaves or hold partner fruit/petal in offhand (F). §7[Tip: Shift+Right-Click leaves to harvest a Scion!]"), true);
                level.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // Attempt Hybridization (10% chance)
        if (!level.isClientSide()) {
            boolean success = level.random.nextFloat() < HYBRIDIZATION_CHANCE;

            if (success) {
                // Drop Sapling
                ItemStack saplingDrop = new ItemStack(resultSapling);
                ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, saplingDrop);
                entity.setDefaultPickUpDelay();
                level.addFreshEntity(entity);

                if (level instanceof ServerLevel sl) {
                    if (resultSapling == ObsidiumMod.ELECTRUM_SAPLING_ITEM) {
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 20, 0.35, 0.35, 0.35, 0.05);
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 15, 0.25, 0.25, 0.25, 0.05);
                    } else {
                        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 25, 0.35, 0.35, 0.35, 0.06);
                        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 12, 0.2, 0.2, 0.2, 0.1);
                    }
                }

                level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.6F);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);

                if (player != null) {
                    player.displayClientMessage(Component.literal("§6[Grafting] §a✦ Hybridization Successful! Produced §e" + resultName + "§a!"), true);
                }
            } else {
                // Failed attempt
                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.02);
                    sl.sendParticles(ParticleTypes.COMPOSTER, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.02);
                }
                level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 0.7F, 0.9F + level.random.nextFloat() * 0.3F);

                if (player != null) {
                    player.displayClientMessage(Component.literal("§6[Grafting] §7Grafting attempt failed (10% chance). Try again!"), true);
                }
            }

            if (player != null) {
                stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static boolean hasPartnerLeavesNearby(Level level, BlockPos pos, String partner1, String partner2) {
        int radius = 4;
        for (BlockPos check : BlockPos.betweenClosed(pos.offset(-radius, -3, -radius), pos.offset(radius, 3, radius))) {
            BlockState st = level.getBlockState(check);
            if (st.getBlock() instanceof MineralTreeLeavesBlock leaves) {
                String sp = leaves.species();
                if (sp.equals(partner1) || (partner2 != null && sp.equals(partner2))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isPartnerItem(ItemStack stack, String partner1, String partner2) {
        if (stack.isEmpty()) return false;
        if (matchesSpecies(stack, partner1)) return true;
        return partner2 != null && matchesSpecies(stack, partner2);
    }

    private static boolean matchesSpecies(ItemStack stack, String species) {
        return switch (species) {
            case "hermes_copper" -> stack.is(ObsidiumMod.HERMES_COPPER_FRUIT) || stack.is(ObsidiumMod.COPPER_PETAL) || stack.is(ObsidiumMod.HERMES_COPPER_SAPLING.asItem());
            case "midas_gold" -> stack.is(ObsidiumMod.MIDAS_GOLD_FRUIT) || stack.is(ObsidiumMod.GOLD_PETAL) || stack.is(ObsidiumMod.MIDAS_GOLD_SAPLING.asItem());
            case "ares_iron" -> stack.is(ObsidiumMod.ARES_IRON_FRUIT) || stack.is(ObsidiumMod.IRON_PETAL) || stack.is(ObsidiumMod.ARES_IRON_SAPLING.asItem());
            case "hephaestus_volcanite" -> stack.is(ObsidiumMod.HEPHAESTUS_VOLCANITE_FRUIT) || stack.is(ObsidiumMod.VOLCANITE_PETAL) || stack.is(ObsidiumMod.HEPHAESTUS_VOLCANITE_SAPLING.asItem());
            case "cronus_lead" -> stack.is(ObsidiumMod.CRONUS_LEAD_FRUIT) || stack.is(ObsidiumMod.LEAD_PETAL) || stack.is(ObsidiumMod.CRONUS_LEAD_SAPLING.asItem());
            case "zeus_tin" -> stack.is(ObsidiumMod.ZEUS_TIN_FRUIT) || stack.is(ObsidiumMod.TIN_PETAL) || stack.is(ObsidiumMod.ZEUS_TIN_SAPLING.asItem());
            case "artemis_silver" -> stack.is(ObsidiumMod.ARTEMIS_SILVER_FRUIT) || stack.is(ObsidiumMod.SILVER_PETAL) || stack.is(ObsidiumMod.ARTEMIS_SILVER_SAPLING.asItem());
            default -> false;
        };
    }

    private static String formatSpeciesName(String species) {
        return switch (species) {
            case "hermes_copper" -> "Hermes Copper";
            case "midas_gold" -> "Midas Gold";
            case "ares_iron" -> "Ares Iron";
            case "hephaestus_volcanite" -> "Hephaestus Volcanite";
            case "cronus_lead" -> "Cronus Lead";
            case "zeus_tin" -> "Zeus Tin";
            case "artemis_silver" -> "Artemis Silver";
            default -> species;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6★ Demetrian Grafting Knife").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§e• Shift + Right-Click Leaves: §7Harvest a Noble Scion cutting"));
        tooltip.add(Component.literal("§e• Right-Click Planted Sapling: §7Incise into a Grafted Rootstock"));
        tooltip.add(Component.literal("§e• Right-Click Leaves: §7Direct cross-breed (10% chance)"));
        tooltip.add(Component.literal("§8  (Requires partner fruit/petal in offhand OR nearby partner tree)"));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§aFull Grafting Recipe Guide:").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("§7  Incised Sapling + Scion + Grafting Tape ➔ 100% Hybrid"));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
