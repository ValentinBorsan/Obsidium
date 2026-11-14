package net.valentin.obsidium.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.valentin.obsidium.ObsidiumMod;

public final class GraftingRegistry {
    private GraftingRegistry() {}

    public static Block resolveHybrid(String rootstock, String scion) {
        if (rootstock == null || scion == null) return Blocks.OAK_SAPLING;
        String r = rootstock.toLowerCase();
        String s = scion.toLowerCase();

        // 1. Apex Diamond Tree (Netherite + Emerald)
        if (isPair(r, s, "tartarean_netherite", "plouton_emerald") || isPair(r, s, "tartarean_netherite", "emerald")) {
            return ObsidiumMod.ADAMANTINE_DIAMOND_SAPLING;
        }

        // 2. Tartarean Netherite Tree (Volcanite + Lead)
        if (isPair(r, s, "hephaestus_volcanite", "cronus_lead") || isPair(r, s, "hephaestus_volcanite", "ares_iron")) {
            return ObsidiumMod.TARTAREAN_NETHERITE_SAPLING;
        }

        // 3. Plouton Emerald Tree (Lapis + Electrum / Gold)
        if (isPair(r, s, "poseidon_lapis", "electrum") || isPair(r, s, "poseidon_lapis", "midas_gold")) {
            return ObsidiumMod.PLOUTON_EMERALD_SAPLING;
        }

        // 4. Dionysian Amethyst Tree (Redstone + Quartz)
        if (isPair(r, s, "astrape_redstone", "apollo_quartz") || isPair(r, s, "astrape_redstone", "quartz")) {
            return ObsidiumMod.DIONYSIAN_AMETHYST_SAPLING;
        }

        // 5. Hyperion Glowstone Tree (Redstone + Gold)
        if (isPair(r, s, "astrape_redstone", "midas_gold")) {
            return ObsidiumMod.HYPERION_GLOWSTONE_SAPLING;
        }

        // 6. Apollo Quartz Tree (Iron + Silver)
        if (isPair(r, s, "ares_iron", "artemis_silver")) {
            return ObsidiumMod.APOLLO_QUARTZ_SAPLING;
        }

        // 7. Electrum Tree (Gold + Silver)
        if (isPair(r, s, "midas_gold", "artemis_silver")) {
            return ObsidiumMod.ELECTRUM_SAPLING;
        }

        // 8. Poseidon Lapis Tree (Gold + Copper)
        if (isPair(r, s, "midas_gold", "hermes_copper")) {
            return ObsidiumMod.POSEIDON_LAPIS_SAPLING;
        }

        // 9. Astrape Redstone Tree (Iron + Coal)
        if (isPair(r, s, "ares_iron", "hades_coal") || isPair(r, s, "ares_iron", "coal")) {
            return ObsidiumMod.ASTRAPE_REDSTONE_SAPLING;
        }

        // 10. Hades Coal Tree (Oak + Volcanite or Dark Oak + Coal)
        if (isPair(r, s, "oak", "hades_coal") || isPair(r, s, "dark_oak", "coal")) {
            return ObsidiumMod.HADES_COAL_SAPLING;
        }

        // 11. Base Ore Trees grafted onto vanilla saplings
        if (r.contains("oak") || r.contains("spruce") || r.contains("birch")) {
            return switch (s) {
                case "midas_gold" -> ObsidiumMod.MIDAS_GOLD_SAPLING;
                case "ares_iron" -> ObsidiumMod.ARES_IRON_SAPLING;
                case "hermes_copper" -> ObsidiumMod.HERMES_COPPER_SAPLING;
                case "hephaestus_volcanite" -> ObsidiumMod.HEPHAESTUS_VOLCANITE_SAPLING;
                case "cronus_lead" -> ObsidiumMod.CRONUS_LEAD_SAPLING;
                case "zeus_tin" -> ObsidiumMod.ZEUS_TIN_SAPLING;
                case "artemis_silver" -> ObsidiumMod.ARTEMIS_SILVER_SAPLING;
                case "hades_coal" -> ObsidiumMod.HADES_COAL_SAPLING;
                default -> ObsidiumMod.MIDAS_GOLD_SAPLING;
            };
        }

        return ObsidiumMod.MIDAS_GOLD_SAPLING;
    }

    private static boolean isPair(String r, String s, String a, String b) {
        return (r.equals(a) && s.equals(b)) || (r.equals(b) && s.equals(a));
    }
}
