package net.valentin.obsidium.registry;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import static net.valentin.obsidium.ObsidiumMod.*;

public final class ModWorldgenAndFuels {
	private ModWorldgenAndFuels() {}

	public static void register() {
		ResourceKey<PlacedFeature> volcanicCone = ResourceKey.create(
			Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanic_cone")
		);
		BiomeModifications.addFeature(
			BiomeSelectors.includeByKey(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanic_caldera"))),
			GenerationStep.Decoration.RAW_GENERATION,
			volcanicCone
		);

		// Generare Copaci Metalici in Volcanic Caldera (inclusiv Cronus Lead, Zeus Tin si Artemis Silver)
		String[] calderaTrees = {
			"midas_gold_tree_placed",
			"ares_iron_tree_placed",
			"hermes_copper_tree_placed",
			"hephaestus_volcanite_tree_placed",
			"cronus_lead_tree_placed",
			"zeus_tin_tree_placed",
			"artemis_silver_tree_placed"
		};
		for (String treeFeature : calderaTrees) {
			BiomeModifications.addFeature(
				BiomeSelectors.includeByKey(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanic_caldera"))),
				GenerationStep.Decoration.VEGETAL_DECORATION,
				ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(MOD_ID, treeFeature))
			);
		}

		// Generare Minereu Volcanite in Lume (Thermal clusters langa Obsidian sau Lava, Y -64 -> -10)
		BiomeModifications.addFeature(
			BiomeSelectors.foundInOverworld(),
			GenerationStep.Decoration.UNDERGROUND_ORES,
			ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_thermal_ore_placed"))
		);

		// Generare Minereu Volcanite in Nether (Langa oceane de lava si obsidian, Y 10 -> 115)
		BiomeModifications.addFeature(
			BiomeSelectors.foundInTheNether(),
			GenerationStep.Decoration.UNDERGROUND_ORES,
			ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_thermal_ore_nether_placed"))
		);

		// Generare Minereu Volcanite in Lume (Deepslate, extrem de jos, Y -64 -> -10)
		if (net.valentin.obsidium.config.ModConfig.get().enableVolcaniteOreGen) {
			BiomeModifications.addFeature(
				BiomeSelectors.foundInOverworld(),
				GenerationStep.Decoration.UNDERGROUND_ORES,
				ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_ore_placed"))
			);
		}

		// Generare Tin Ore in Lume (Stone & Deepslate, Y 0 -> 96)
		if (net.valentin.obsidium.config.ModConfig.get().enableTinOreGen) {
			BiomeModifications.addFeature(
				BiomeSelectors.foundInOverworld(),
				GenerationStep.Decoration.UNDERGROUND_ORES,
				ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(MOD_ID, "tin_ore_placed"))
			);
		}

		// Generare Lead Ore in Lume (Stone & Deepslate, Y -64 -> 32)
		if (net.valentin.obsidium.config.ModConfig.get().enableLeadOreGen) {
			BiomeModifications.addFeature(
				BiomeSelectors.foundInOverworld(),
				GenerationStep.Decoration.UNDERGROUND_ORES,
				ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(MOD_ID, "lead_ore_placed"))
			);
		}

		// Generare Silver Ore in Lume (Stone & Deepslate, Y -64 -> 32)
		if (net.valentin.obsidium.config.ModConfig.get().enableSilverOreGen) {
			BiomeModifications.addFeature(
				BiomeSelectors.foundInOverworld(),
				GenerationStep.Decoration.UNDERGROUND_ORES,
				ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(MOD_ID, "silver_ore_placed"))
			);
		}

		// Fuels
		FuelRegistry.INSTANCE.add(ASH_BRIQUETTE, 3200);
		FuelRegistry.INSTANCE.add(STICK_BUNDLE, 1600);
		FuelRegistry.INSTANCE.add(DIONYSIAN_POMACE, 1600);
		FuelRegistry.INSTANCE.add(MIDAS_GOLD_LOG_ITEM, 300);
		FuelRegistry.INSTANCE.add(ARES_IRON_LOG_ITEM, 300);
		FuelRegistry.INSTANCE.add(HERMES_COPPER_LOG_ITEM, 300);
		FuelRegistry.INSTANCE.add(HEPHAESTUS_VOLCANITE_LOG_ITEM, 300);
		FuelRegistry.INSTANCE.add(ELECTRUM_LOG_ITEM, 300);
		FuelRegistry.INSTANCE.add(TARTAREAN_NETHERITE_LOG_ITEM, 300);
		FuelRegistry.INSTANCE.add(CRONUS_LEAD_LOG_ITEM, 300);
		FuelRegistry.INSTANCE.add(ZEUS_TIN_LOG_ITEM, 300);
		FuelRegistry.INSTANCE.add(ARTEMIS_SILVER_LOG_ITEM, 300);

		// Compostables (Vanilla Composter support for Seeds, Petals, Saplings, Fruits, Leaves)
		CompostingChanceRegistry.INSTANCE.add(VOLCANITE_SEED, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(IRON_SEED, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(GOLD_SEED, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(COPPER_SEED, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(COAL_SEED, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(LAPIS_SEED, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(REDSTONE_SEED, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(DIAMOND_SEED, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(NETHERITE_SEED, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(TIN_SEED, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(LEAD_SEED, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(SILVER_SEED, 0.3F);

		CompostingChanceRegistry.INSTANCE.add(VOLCANITE_PETAL, 0.65F);
		CompostingChanceRegistry.INSTANCE.add(IRON_PETAL, 0.65F);
		CompostingChanceRegistry.INSTANCE.add(GOLD_PETAL, 0.65F);
		CompostingChanceRegistry.INSTANCE.add(COPPER_PETAL, 0.65F);
		CompostingChanceRegistry.INSTANCE.add(COAL_PETAL, 0.65F);
		CompostingChanceRegistry.INSTANCE.add(LAPIS_PETAL, 0.65F);
		CompostingChanceRegistry.INSTANCE.add(REDSTONE_PETAL, 0.65F);
		CompostingChanceRegistry.INSTANCE.add(DIAMOND_PETAL, 0.65F);
		CompostingChanceRegistry.INSTANCE.add(NETHERITE_PETAL, 0.65F);
		CompostingChanceRegistry.INSTANCE.add(TIN_PETAL, 0.65F);
		CompostingChanceRegistry.INSTANCE.add(LEAD_PETAL, 0.65F);
		CompostingChanceRegistry.INSTANCE.add(SILVER_PETAL, 0.65F);

		CompostingChanceRegistry.INSTANCE.add(MIDAS_GOLD_SAPLING_ITEM, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(ARES_IRON_SAPLING_ITEM, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(HERMES_COPPER_SAPLING_ITEM, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(HEPHAESTUS_VOLCANITE_SAPLING_ITEM, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(ELECTRUM_SAPLING_ITEM, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(TARTAREAN_NETHERITE_SAPLING_ITEM, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(CRONUS_LEAD_SAPLING_ITEM, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(ZEUS_TIN_SAPLING_ITEM, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(ARTEMIS_SILVER_SAPLING_ITEM, 0.3F);

		CompostingChanceRegistry.INSTANCE.add(MIDAS_GOLD_LEAVES_ITEM, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(ARES_IRON_LEAVES_ITEM, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(HERMES_COPPER_LEAVES_ITEM, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(HEPHAESTUS_VOLCANITE_LEAVES_ITEM, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(ELECTRUM_LEAVES_ITEM, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(TARTAREAN_NETHERITE_LEAVES_ITEM, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(CRONUS_LEAD_LEAVES_ITEM, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(ZEUS_TIN_LEAVES_ITEM, 0.3F);
		CompostingChanceRegistry.INSTANCE.add(ARTEMIS_SILVER_LEAVES_ITEM, 0.3F);

		CompostingChanceRegistry.INSTANCE.add(MIDAS_GOLD_FRUIT, 0.65F);
		CompostingChanceRegistry.INSTANCE.add(ARES_IRON_FRUIT, 0.65F);
		CompostingChanceRegistry.INSTANCE.add(HERMES_COPPER_FRUIT, 0.65F);
		CompostingChanceRegistry.INSTANCE.add(HEPHAESTUS_VOLCANITE_FRUIT, 0.65F);
		CompostingChanceRegistry.INSTANCE.add(ELECTRUM_FRUIT, 0.65F);
		CompostingChanceRegistry.INSTANCE.add(TARTAREAN_NETHERITE_FRUIT, 0.65F);
		CompostingChanceRegistry.INSTANCE.add(CRONUS_LEAD_FRUIT, 0.65F);
		CompostingChanceRegistry.INSTANCE.add(ZEUS_TIN_FRUIT, 0.65F);
		CompostingChanceRegistry.INSTANCE.add(ARTEMIS_SILVER_FRUIT, 0.65F);

		CompostingChanceRegistry.INSTANCE.add(DIONYSIAN_POMACE, 0.85F);
	}
}
