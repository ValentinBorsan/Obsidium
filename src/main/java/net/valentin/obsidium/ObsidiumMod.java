package net.valentin.obsidium;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.valentin.obsidium.block.VolcaniteGeneratorBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.valentin.obsidium.block.VolcanicOreCropBlock;
import net.valentin.obsidium.block.GeothermalArboristBlock;
import net.valentin.obsidium.block.GeothermalArboristBlockEntity;
import net.valentin.obsidium.block.GeothermalArboristMenu;
import net.valentin.obsidium.block.VolcaniteSteamGeneratorBlock;
import net.valentin.obsidium.block.VolcaniteSteamGeneratorBlockEntity;
import net.valentin.obsidium.block.VolcaniteSteamGeneratorMenu;
import net.valentin.obsidium.block.VolcaniteGeothermalGeneratorBlock;
import net.valentin.obsidium.block.VolcaniteGeothermalGeneratorBlockEntity;
import net.valentin.obsidium.block.VolcaniteGeothermalGeneratorMenu;
import net.valentin.obsidium.item.PyrogenicBloomInfuserItem;
import net.valentin.obsidium.item.VolcanicPetalItem;
import net.valentin.obsidium.item.VolcanicSeedItem;
import net.valentin.obsidium.block.CreativeLavaTankBlock;
import net.valentin.obsidium.block.CreativeEnergyCoreBlock;
import net.valentin.obsidium.block.CreativeLavaTankBlockEntity;
import net.valentin.obsidium.block.CreativeEnergyCoreBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import net.valentin.obsidium.block.VolcaniteFurnaceBlock;
import net.valentin.obsidium.block.VolcaniteFurnaceBlockEntity;
import net.valentin.obsidium.block.VolcaniteFurnaceMenu;
import net.valentin.obsidium.block.VolcaniteWorktableBlock;
import net.valentin.obsidium.block.VolcaniteWorktableBlockEntity;
import net.valentin.obsidium.block.VolcaniteWorktableMenu;
import net.valentin.obsidium.block.SupremeVolcanicGeneratorBlock;
import net.valentin.obsidium.block.SupremeVolcanicGeneratorBlockEntity;
import net.valentin.obsidium.block.SupremeVolcanicGeneratorMenu;
import net.valentin.obsidium.block.VolcanicDistillationChamberBlock;
import net.valentin.obsidium.block.VolcanicDistillationChamberBlockEntity;
import net.valentin.obsidium.block.VolcanicDistillationChamberMenu;
import net.valentin.obsidium.block.HydrothermalVitrifierBlock;
import net.valentin.obsidium.block.HydrothermalVitrifierBlockEntity;
import net.valentin.obsidium.block.HydrothermalVitrifierMenu;
import net.valentin.obsidium.block.MagmaticCrucibleBlock;
import net.valentin.obsidium.block.MagmaticCrucibleBlockEntity;
import net.valentin.obsidium.block.MagmaticCrucibleMenu;
import net.valentin.obsidium.block.HydrothermalCondenserBlock;
import net.valentin.obsidium.block.HydrothermalCondenserBlockEntity;
import net.valentin.obsidium.block.HydrothermalCondenserMenu;
import net.valentin.obsidium.block.HydrothermalLithofierBlock;
import net.valentin.obsidium.block.HydrothermalLithofierBlockEntity;
import net.valentin.obsidium.block.HydrothermalLithofierMenu;
import net.valentin.obsidium.block.VolcaniteItemBarrelBlock;
import net.valentin.obsidium.block.VolcaniteItemBarrelBlockEntity;
import net.valentin.obsidium.block.VolcaniteItemBarrelMenu;
import net.valentin.obsidium.item.PipeFacadeItem;
import net.valentin.obsidium.item.VolcaniteBarrelUpgradeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObsidiumMod implements ModInitializer {
	public static final String MOD_ID = "obsidium";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Block getBlock(String name) {
		return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MOD_ID, name));
	}

	public static final Item VOLCANITE = registerItem("volcanite", new Item(new Item.Properties()));
	public static final Item VOLCANITE_INGOT = registerItem("volcanite_ingot", new Item(new Item.Properties().fireResistant()));
	public static final Item VOLCANIC_ALLOY = registerItem("volcanic_alloy", new Item(new Item.Properties()));
	public static final Item SUPREME_VOLCANIC_ALLOY = registerItem("supreme_volcanic_alloy", new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));
	public static final Item ASTRAL_VOLCANIC_CATALYST = registerItem("astral_volcanic_catalyst", new net.valentin.obsidium.item.AstralVolcanicCatalystItem(new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC)));
	public static final Item PYROCLASTIC_DISTILLATE_BUCKET = registerItem("pyroclastic_distillate_bucket", new net.valentin.obsidium.item.PyroclasticDistillateItem(new Item.Properties().fireResistant().stacksTo(1), 1000));
	public static final Item PYROCLASTIC_FLASK = registerItem("pyroclastic_flask", new net.valentin.obsidium.item.PyroclasticDistillateItem(new Item.Properties().fireResistant().stacksTo(16), 250));
	public static final Item STEAM_BUCKET = registerItem("steam_bucket", new net.valentin.obsidium.item.SteamBucketItem(new Item.Properties().stacksTo(1)));

	public static final net.minecraft.world.level.material.Fluid PYROCLASTIC_DISTILLATE_FLUID = Registry.register(
			BuiltInRegistries.FLUID,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "pyroclastic_distillate"),
			new net.valentin.obsidium.fluid.ModVirtualFluid(() -> PYROCLASTIC_DISTILLATE_BUCKET)
	);
	public static final net.minecraft.world.level.material.Fluid STEAM_FLUID = Registry.register(
			BuiltInRegistries.FLUID,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "steam"),
			new net.valentin.obsidium.fluid.ModVirtualFluid(() -> STEAM_BUCKET)
	);

	public static final Item BIO_OIL_BUCKET = registerItem("bio_oil_bucket", new net.valentin.obsidium.item.BiofuelBucketItem(
			new Item.Properties().stacksTo(1),
			"Raw Bio-Oil",
			"Distilled from Seeds, Pomace, Leaves or Foliage Bales in the Dionysian Distillery.",
			"Diesel Generator: 200 RF/t base | Refines with Bio-Ethanol into Biodiesel.",
			net.minecraft.ChatFormatting.GOLD));
	public static final Item BIO_ETHANOL_BUCKET = registerItem("bio_ethanol_bucket", new net.valentin.obsidium.item.BiofuelBucketItem(
			new Item.Properties().stacksTo(1),
			"Refined Bio-Ethanol",
			"Distilled from Fruits, Sugar or Honey in the Dionysian Distillery.",
			"Diesel Generator: 400 RF/t base | Reacts with Bio-Oil into Biodiesel.",
			net.minecraft.ChatFormatting.AQUA));
	public static final Item BIODIESEL_BUCKET = registerItem("biodiesel_bucket", new net.valentin.obsidium.item.BiofuelBucketItem(
			new Item.Properties().stacksTo(1),
			"Demetrian Biodiesel",
			"Synthesized from Bio-Oil + Bio-Ethanol in the Volcanic Distillation Chamber.",
			"Diesel Generator: 750 RF/t base (high efficiency).",
			net.minecraft.ChatFormatting.GREEN));
	public static final Item PYRO_DIESEL_BUCKET = registerItem("pyro_diesel_bucket", new net.valentin.obsidium.item.BiofuelBucketItem(
			new Item.Properties().fireResistant().stacksTo(1),
			"Pyro-Diesel Hyperfuel",
			"Refined from Biodiesel + Pyroclastic/Blaze catalysts in the Volcanic Distillation Chamber.",
			"Diesel Generator: 1,600 RF/t base (maximum output).",
			net.minecraft.ChatFormatting.RED));

	public static final net.minecraft.world.level.material.Fluid BIO_OIL_FLUID = Registry.register(
			BuiltInRegistries.FLUID,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "bio_oil"),
			new net.valentin.obsidium.fluid.ModVirtualFluid(() -> BIO_OIL_BUCKET)
	);
	public static final net.minecraft.world.level.material.Fluid BIO_ETHANOL_FLUID = Registry.register(
			BuiltInRegistries.FLUID,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "bio_ethanol"),
			new net.valentin.obsidium.fluid.ModVirtualFluid(() -> BIO_ETHANOL_BUCKET)
	);
	public static final net.minecraft.world.level.material.Fluid BIODIESEL_FLUID = Registry.register(
			BuiltInRegistries.FLUID,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "biodiesel"),
			new net.valentin.obsidium.fluid.ModVirtualFluid(() -> BIODIESEL_BUCKET)
	);
	public static final net.minecraft.world.level.material.Fluid PYRO_DIESEL_FLUID = Registry.register(
			BuiltInRegistries.FLUID,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "pyro_diesel"),
			new net.valentin.obsidium.fluid.ModVirtualFluid(() -> PYRO_DIESEL_BUCKET)
	);
	public static final Item MAGMA_FORGE_MANUAL_ITEM = registerItem("magma_forge_manual", new net.valentin.obsidium.item.MagmaForgeManualItem(new Item.Properties().stacksTo(1)));
	public static final Item VOLCANIC_ASH = registerItem("volcanic_ash", new net.valentin.obsidium.item.VolcanicAshItem(new Item.Properties()));
	public static final Item VOLCANITE_BINDING = registerItem("volcanite_binding", new Item(new Item.Properties()));
	public static final Item VOLCANIC_PLATE = registerItem("volcanic_plate", new Item(new Item.Properties()));
	public static final Item VOLCANITE_THERMO_CORE = registerItem("volcanite_thermo_core", new net.valentin.obsidium.item.VolcaniteThermoCoreItem(new Item.Properties().fireResistant().stacksTo(16)));
	public static final Item VOLCANITE_FLUX_MECHANISM = registerItem("volcanite_flux_mechanism", new Item(new Item.Properties().fireResistant()));
	public static final Item CINDER_CORE = registerItem("cinder_core", new Item(new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.RARE).stacksTo(16)));
	public static final Item CINDER_STALKER_SKULL = registerItem("cinder_stalker_skull", new net.valentin.obsidium.item.CinderStalkerSkullItem(new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.UNCOMMON).stacksTo(16)));
	public static final Item TARTAREAN_FANG = registerItem("tartarean_fang", new Item(new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.RARE)));
	public static final Item BOW_OF_ARTEMIS = registerItem("bow_of_artemis", new net.valentin.obsidium.item.ArtemisBowItem(new Item.Properties().durability(1150).fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC)));
	public static final Item AEGIS_OF_ATHENA = registerItem("aegis_of_athena", new net.valentin.obsidium.item.AegisShieldItem(new Item.Properties().durability(1600).fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC)));
	public static final Item VOLCANIC_PAPYRUS = registerItem("volcanic_papyrus", new Item(new Item.Properties().fireResistant()));
	public static final Item SCROLL_OF_ZEUS = registerItem("scroll_of_zeus", new net.valentin.obsidium.item.MythicScrollItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).fireResistant(), net.valentin.obsidium.mythology.DivinePact.ZEUS));
	public static final Item SCROLL_OF_APOLLO = registerItem("scroll_of_apollo", new net.valentin.obsidium.item.MythicScrollItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).fireResistant(), net.valentin.obsidium.mythology.DivinePact.APOLLO));
	public static final Item SCROLL_OF_ARTEMIS = registerItem("scroll_of_artemis", new net.valentin.obsidium.item.MythicScrollItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).fireResistant(), net.valentin.obsidium.mythology.DivinePact.ARTEMIS));
	public static final Item SCROLL_OF_ATHENA = registerItem("scroll_of_athena", new net.valentin.obsidium.item.MythicScrollItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).fireResistant(), net.valentin.obsidium.mythology.DivinePact.ATHENA));
	public static final Item SCROLL_OF_HEPHAESTUS = registerItem("scroll_of_hephaestus", new net.valentin.obsidium.item.MythicScrollItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).fireResistant(), net.valentin.obsidium.mythology.DivinePact.HEPHAESTUS));
	public static final Item SCROLL_OF_POSEIDON = registerItem("scroll_of_poseidon", new net.valentin.obsidium.item.MythicScrollItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).fireResistant(), net.valentin.obsidium.mythology.DivinePact.POSEIDON));
	public static final Item SCROLL_OF_THANATOS = registerItem("scroll_of_thanatos", new net.valentin.obsidium.item.MythicScrollItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).fireResistant(), net.valentin.obsidium.mythology.DivinePact.THANATOS));
	public static final Item VOLCANITE_PORTABLE_BATTERY = registerItem("volcanite_portable_battery", new net.valentin.obsidium.item.VolcanitePortableBatteryItem(new Item.Properties().stacksTo(1), 500000, 10000, 10000));
	public static final Item ADVANCED_VOLCANITE_PORTABLE_BATTERY = registerItem("advanced_volcanite_portable_battery", new net.valentin.obsidium.item.VolcanitePortableBatteryItem(new Item.Properties().stacksTo(1).fireResistant().rarity(net.minecraft.world.item.Rarity.RARE), 5000000, 100000, 100000));
	public static final Item BASIC_BACKPACK = registerItem("basic_backpack", new net.valentin.obsidium.item.BasicBackpackItem(new Item.Properties()));
	public static final Item ADVANCED_BACKPACK = registerItem("advanced_backpack", new net.valentin.obsidium.item.AdvancedBackpackItem(new Item.Properties()));
	public static final Item SUPREME_LINK_TOOL = registerItem("supreme_link_tool", new net.valentin.obsidium.item.SupremeLinkToolItem(new Item.Properties()));
	public static final Item PYROGENIC_BLOOM_INFUSER = registerItem("pyrogenic_bloom_infuser", new PyrogenicBloomInfuserItem(new Item.Properties().stacksTo(1).fireResistant()));
	
	public static final Item IRON_DUST = registerItem("iron_dust", new Item(new Item.Properties()));
	public static final Item GOLD_DUST = registerItem("gold_dust", new Item(new Item.Properties()));
	public static final Item COPPER_DUST = registerItem("copper_dust", new Item(new Item.Properties()));
	public static final Item VOLCANITE_DUST = registerItem("volcanite_dust", new Item(new Item.Properties()));
	public static final Item TIN_DUST = registerItem("tin_dust", new Item(new Item.Properties()));
	public static final Item TIN_INGOT = registerItem("tin_ingot", new Item(new Item.Properties()));
	public static final Item RAW_TIN = registerItem("raw_tin", new Item(new Item.Properties()));
	public static final Item LEAD_DUST = registerItem("lead_dust", new Item(new Item.Properties()));
	public static final Item LEAD_INGOT = registerItem("lead_ingot", new Item(new Item.Properties()));
	public static final Item RAW_LEAD = registerItem("raw_lead", new Item(new Item.Properties()));
	public static final Item SILVER_DUST = registerItem("silver_dust", new Item(new Item.Properties()));
	public static final Item SILVER_INGOT = registerItem("silver_ingot", new Item(new Item.Properties()));
	public static final Item RAW_SILVER = registerItem("raw_silver", new Item(new Item.Properties()));
	public static final Item SILVER_NUGGET = registerItem("silver_nugget", new Item(new Item.Properties()));

	// Volcanic Ore Petals
	public static final Item VOLCANITE_PETAL = registerItem("volcanite_petal", new VolcanicPetalItem(() -> getBlock("volcanite_crop"), new Item.Properties().fireResistant()));
	public static final Item IRON_PETAL = registerItem("iron_petal", new VolcanicPetalItem(() -> getBlock("iron_crop"), new Item.Properties()));
	public static final Item GOLD_PETAL = registerItem("gold_petal", new VolcanicPetalItem(() -> getBlock("gold_crop"), new Item.Properties()));
	public static final Item COPPER_PETAL = registerItem("copper_petal", new VolcanicPetalItem(() -> getBlock("copper_crop"), new Item.Properties()));
	public static final Item COAL_PETAL = registerItem("coal_petal", new VolcanicPetalItem(() -> getBlock("coal_crop"), new Item.Properties()));
	public static final Item LAPIS_PETAL = registerItem("lapis_petal", new VolcanicPetalItem(() -> getBlock("lapis_crop"), new Item.Properties()));
	public static final Item REDSTONE_PETAL = registerItem("redstone_petal", new VolcanicPetalItem(() -> getBlock("redstone_crop"), new Item.Properties()));
	public static final Item DIAMOND_PETAL = registerItem("diamond_petal", new VolcanicPetalItem(() -> getBlock("diamond_crop"), new Item.Properties()));
	public static final Item NETHERITE_PETAL = registerItem("netherite_petal", new VolcanicPetalItem(() -> getBlock("netherite_crop"), new Item.Properties().fireResistant()));
	public static final Item TIN_PETAL = registerItem("tin_petal", new VolcanicPetalItem(() -> getBlock("tin_crop"), new Item.Properties()));
	public static final Item LEAD_PETAL = registerItem("lead_petal", new VolcanicPetalItem(() -> getBlock("lead_crop"), new Item.Properties()));
	public static final Item SILVER_PETAL = registerItem("silver_petal", new VolcanicPetalItem(() -> getBlock("silver_crop"), new Item.Properties()));

	// Volcanic Ore Nuggets (Vanilla has Iron & Gold Nuggets)
	public static final Item VOLCANITE_NUGGET = registerItem("volcanite_nugget", new Item(new Item.Properties().fireResistant()));
	public static final Item COPPER_NUGGET = registerItem("copper_nugget", new Item(new Item.Properties()));
	public static final Item COAL_NUGGET = registerItem("coal_nugget", new Item(new Item.Properties()));
	public static final Item LAPIS_NUGGET = registerItem("lapis_nugget", new Item(new Item.Properties()));
	public static final Item REDSTONE_NUGGET = registerItem("redstone_nugget", new Item(new Item.Properties()));
	public static final Item DIAMOND_NUGGET = registerItem("diamond_nugget", new Item(new Item.Properties()));
	public static final Item NETHERITE_NUGGET = registerItem("netherite_nugget", new Item(new Item.Properties().fireResistant()));
	public static final Item TIN_NUGGET = registerItem("tin_nugget", new Item(new Item.Properties()));
	public static final Item LEAD_NUGGET = registerItem("lead_nugget", new Item(new Item.Properties()));

	// New Metallurgic Alloys
	public static final Item PYRIC_BRONZE_INGOT = registerItem("pyric_bronze_ingot", new Item(new Item.Properties()));
	public static final Item PYRIC_BRONZE_DUST = registerItem("pyric_bronze_dust", new Item(new Item.Properties()));
	public static final Item PYRIC_BRONZE_NUGGET = registerItem("pyric_bronze_nugget", new Item(new Item.Properties()));

	public static final Item FEROPLUMB_INGOT = registerItem("feroplumb_ingot", new Item(new Item.Properties()));
	public static final Item FEROPLUMB_DUST = registerItem("feroplumb_dust", new Item(new Item.Properties()));
	public static final Item FEROPLUMB_NUGGET = registerItem("feroplumb_nugget", new Item(new Item.Properties()));

	// --- TIER 1: PROMETHEAN TOOLS (Pyric Bronze) ---
	public static final Item PROMETHEAN_SWORD = registerItem("promethean_sword",
		new net.valentin.obsidium.item.PrometheanSwordItem(net.valentin.obsidium.item.PyricBronzeToolMaterial.INSTANCE,
		new Item.Properties().attributes(net.minecraft.world.item.SwordItem.createAttributes(net.valentin.obsidium.item.PyricBronzeToolMaterial.INSTANCE, 3, -2.4f))));
	public static final Item PROMETHEAN_PICKAXE = registerItem("promethean_pickaxe",
		new net.valentin.obsidium.item.PrometheanPickaxeItem(net.valentin.obsidium.item.PyricBronzeToolMaterial.INSTANCE,
		new Item.Properties().attributes(net.minecraft.world.item.PickaxeItem.createAttributes(net.valentin.obsidium.item.PyricBronzeToolMaterial.INSTANCE, 1, -2.8f))));
	public static final Item PROMETHEAN_AXE = registerItem("promethean_axe",
		new net.valentin.obsidium.item.PrometheanAxeItem(net.valentin.obsidium.item.PyricBronzeToolMaterial.INSTANCE,
		new Item.Properties().attributes(net.minecraft.world.item.AxeItem.createAttributes(net.valentin.obsidium.item.PyricBronzeToolMaterial.INSTANCE, 6.0f, -3.1f))));
	public static final Item PROMETHEAN_SHOVEL = registerItem("promethean_shovel",
		new net.valentin.obsidium.item.PrometheanShovelItem(net.valentin.obsidium.item.PyricBronzeToolMaterial.INSTANCE,
		new Item.Properties().attributes(net.minecraft.world.item.ShovelItem.createAttributes(net.valentin.obsidium.item.PyricBronzeToolMaterial.INSTANCE, 1.5f, -3.0f))));
	public static final Item PROMETHEAN_HOE = registerItem("promethean_hoe",
		new net.valentin.obsidium.item.PrometheanHoeItem(net.valentin.obsidium.item.PyricBronzeToolMaterial.INSTANCE,
		new Item.Properties().attributes(net.minecraft.world.item.HoeItem.createAttributes(net.valentin.obsidium.item.PyricBronzeToolMaterial.INSTANCE, -2, 0.0f))));

	// --- TIER 1: TARTAREAN TOOLS & ARMOR (Feroplumb) ---
	public static final Item TARTAREAN_SWORD = registerItem("tartarean_sword",
		new net.valentin.obsidium.item.TartareanSwordItem(net.valentin.obsidium.item.FeroplumbToolMaterial.INSTANCE,
		new Item.Properties().attributes(net.minecraft.world.item.SwordItem.createAttributes(net.valentin.obsidium.item.FeroplumbToolMaterial.INSTANCE, 4, -2.6f))));
	public static final Item TARTAREAN_PICKAXE = registerItem("tartarean_pickaxe",
		new net.valentin.obsidium.item.TartareanPickaxeItem(net.valentin.obsidium.item.FeroplumbToolMaterial.INSTANCE,
		new Item.Properties().attributes(net.minecraft.world.item.PickaxeItem.createAttributes(net.valentin.obsidium.item.FeroplumbToolMaterial.INSTANCE, 2, -2.8f))));
	public static final Item TARTAREAN_AXE = registerItem("tartarean_axe",
		new net.valentin.obsidium.item.TartareanAxeItem(net.valentin.obsidium.item.FeroplumbToolMaterial.INSTANCE,
		new Item.Properties().attributes(net.minecraft.world.item.AxeItem.createAttributes(net.valentin.obsidium.item.FeroplumbToolMaterial.INSTANCE, 7.0f, -3.2f))));
	public static final Item TARTAREAN_SHOVEL = registerItem("tartarean_shovel",
		new net.valentin.obsidium.item.TartareanShovelItem(net.valentin.obsidium.item.FeroplumbToolMaterial.INSTANCE,
		new Item.Properties().attributes(net.minecraft.world.item.ShovelItem.createAttributes(net.valentin.obsidium.item.FeroplumbToolMaterial.INSTANCE, 2.0f, -3.0f))));
	public static final Item TARTAREAN_HOE = registerItem("tartarean_hoe",
		new net.valentin.obsidium.item.TartareanHoeItem(net.valentin.obsidium.item.FeroplumbToolMaterial.INSTANCE,
		new Item.Properties().attributes(net.minecraft.world.item.HoeItem.createAttributes(net.valentin.obsidium.item.FeroplumbToolMaterial.INSTANCE, -2, 0.0f))));

	public static final Item TARTAREAN_HELMET = registerItem("tartarean_helmet",
		new net.valentin.obsidium.item.TartareanArmorItem(net.valentin.obsidium.item.FeroplumbArmorMaterial.INSTANCE, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));
	public static final Item TARTAREAN_CHESTPLATE = registerItem("tartarean_chestplate",
		new net.valentin.obsidium.item.TartareanArmorItem(net.valentin.obsidium.item.FeroplumbArmorMaterial.INSTANCE, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));
	public static final Item TARTAREAN_LEGGINGS = registerItem("tartarean_leggings",
		new net.valentin.obsidium.item.TartareanArmorItem(net.valentin.obsidium.item.FeroplumbArmorMaterial.INSTANCE, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));
	public static final Item TARTAREAN_BOOTS = registerItem("tartarean_boots",
		new net.valentin.obsidium.item.TartareanArmorItem(net.valentin.obsidium.item.FeroplumbArmorMaterial.INSTANCE, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));

	// --- TIER 2: TYPHONIC TOOLS & ARMOR (Volcanic Alloy) ---
	public static final Item TYPHONIC_SWORD = registerItem("typhonic_sword", 
		new net.valentin.obsidium.item.TyphonicSwordItem(net.valentin.obsidium.item.VolcanicToolMaterial.INSTANCE, 
		new Item.Properties().attributes(net.minecraft.world.item.SwordItem.createAttributes(net.valentin.obsidium.item.VolcanicToolMaterial.INSTANCE, 5, -2.2f)).fireResistant()));
	public static final Item TYPHONIC_PICKAXE = registerItem("typhonic_pickaxe", 
		new net.valentin.obsidium.item.TyphonicPickaxeItem(net.valentin.obsidium.item.VolcanicToolMaterial.INSTANCE, 
		new Item.Properties().attributes(net.minecraft.world.item.PickaxeItem.createAttributes(net.valentin.obsidium.item.VolcanicToolMaterial.INSTANCE, 2, -2.6f)).fireResistant()));
	public static final Item TYPHONIC_AXE = registerItem("typhonic_axe", 
		new net.valentin.obsidium.item.TyphonicAxeItem(net.valentin.obsidium.item.VolcanicToolMaterial.INSTANCE, 
		new Item.Properties().attributes(net.minecraft.world.item.AxeItem.createAttributes(net.valentin.obsidium.item.VolcanicToolMaterial.INSTANCE, 7.0f, -3.0f)).fireResistant()));
	public static final Item TYPHONIC_SHOVEL = registerItem("typhonic_shovel", 
		new net.valentin.obsidium.item.TyphonicShovelItem(net.valentin.obsidium.item.VolcanicToolMaterial.INSTANCE, 
		new Item.Properties().attributes(net.minecraft.world.item.ShovelItem.createAttributes(net.valentin.obsidium.item.VolcanicToolMaterial.INSTANCE, 2.5f, -2.8f)).fireResistant()));
	public static final Item TYPHONIC_HOE = registerItem("typhonic_hoe", 
		new net.valentin.obsidium.item.TyphonicHoeItem(net.valentin.obsidium.item.VolcanicToolMaterial.INSTANCE, 
		new Item.Properties().attributes(net.minecraft.world.item.HoeItem.createAttributes(net.valentin.obsidium.item.VolcanicToolMaterial.INSTANCE, -1, 0.0f)).fireResistant()));

	public static final Item TYPHONIC_HELMET = registerItem("typhonic_helmet", 
		new net.valentin.obsidium.item.VolcanicArmorItem(net.valentin.obsidium.item.VolcanicArmorMaterial.INSTANCE, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties().durability(420).fireResistant()));
	public static final Item TYPHONIC_CHESTPLATE = registerItem("typhonic_chestplate", 
		new net.valentin.obsidium.item.VolcanicArmorItem(net.valentin.obsidium.item.VolcanicArmorMaterial.INSTANCE, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(610).fireResistant()));
	public static final Item TYPHONIC_LEGGINGS = registerItem("typhonic_leggings", 
		new net.valentin.obsidium.item.VolcanicArmorItem(net.valentin.obsidium.item.VolcanicArmorMaterial.INSTANCE, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties().durability(570).fireResistant()));
	public static final Item TYPHONIC_BOOTS = registerItem("typhonic_boots", 
		new net.valentin.obsidium.item.VolcanicArmorItem(net.valentin.obsidium.item.VolcanicArmorMaterial.INSTANCE, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties().durability(495).fireResistant()));

	// Aether-Faraday Dielectric Suit & HV Operating Tool
	public static final Item FARADAY_HELMET = registerItem("faraday_helmet",
		new net.valentin.obsidium.item.FaradayArmorItem(net.valentin.obsidium.item.FaradayArmorMaterial.INSTANCE, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties().durability(520).fireResistant().rarity(net.minecraft.world.item.Rarity.RARE)));
	public static final Item FARADAY_CHESTPLATE = registerItem("faraday_chestplate",
		new net.valentin.obsidium.item.FaradayArmorItem(net.valentin.obsidium.item.FaradayArmorMaterial.INSTANCE, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(750).fireResistant().rarity(net.minecraft.world.item.Rarity.RARE)));
	public static final Item FARADAY_LEGGINGS = registerItem("faraday_leggings",
		new net.valentin.obsidium.item.FaradayArmorItem(net.valentin.obsidium.item.FaradayArmorMaterial.INSTANCE, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties().durability(700).fireResistant().rarity(net.minecraft.world.item.Rarity.RARE)));
	public static final Item FARADAY_BOOTS = registerItem("faraday_boots",
		new net.valentin.obsidium.item.FaradayArmorItem(net.valentin.obsidium.item.FaradayArmorMaterial.INSTANCE, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties().durability(600).fireResistant().rarity(net.minecraft.world.item.Rarity.RARE)));
	public static final Item HV_DIELECTRIC_HOT_STICK = registerItem("hv_dielectric_hot_stick",
		new net.valentin.obsidium.item.HVDielectricHotStickItem(new Item.Properties().stacksTo(1).fireResistant().rarity(net.minecraft.world.item.Rarity.RARE)));

	// Aliases for seamless backwards compatibility
	public static final Item VOLCANIC_SWORD = TYPHONIC_SWORD;
	public static final Item VOLCANIC_PICKAXE = TYPHONIC_PICKAXE;
	public static final Item VOLCANIC_AXE = TYPHONIC_AXE;
	public static final Item VOLCANIC_SHOVEL = TYPHONIC_SHOVEL;
	public static final Item VOLCANIC_HOE = TYPHONIC_HOE;
	public static final Item VOLCANIC_HELMET = TYPHONIC_HELMET;
	public static final Item VOLCANIC_CHESTPLATE = TYPHONIC_CHESTPLATE;
	public static final Item VOLCANIC_LEGGINGS = TYPHONIC_LEGGINGS;
	public static final Item VOLCANIC_BOOTS = TYPHONIC_BOOTS;

	// --- TIER 3: HEPHAESTEAN TOOLS & ARMOR (Supreme RF Powered) ---
	public static final Item HEPHAESTEAN_SWORD = registerItem("hephaestean_sword", 
		new net.valentin.obsidium.item.HephaesteanSwordItem(net.valentin.obsidium.item.SupremeToolMaterial.INSTANCE, 
		new Item.Properties().attributes(net.minecraft.world.item.SwordItem.createAttributes(net.valentin.obsidium.item.SupremeToolMaterial.INSTANCE, 8, -1.8f)).fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC)));
	public static final Item HEPHAESTEAN_PICKAXE = registerItem("hephaestean_pickaxe", 
		new net.valentin.obsidium.item.HephaesteanPickaxeItem(net.valentin.obsidium.item.SupremeToolMaterial.INSTANCE, 
		new Item.Properties().attributes(net.minecraft.world.item.PickaxeItem.createAttributes(net.valentin.obsidium.item.SupremeToolMaterial.INSTANCE, 5, -2.2f)).fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC)));
	public static final Item HEPHAESTEAN_AXE = registerItem("hephaestean_axe", 
		new net.valentin.obsidium.item.HephaesteanAxeItem(net.valentin.obsidium.item.SupremeToolMaterial.INSTANCE, 
		new Item.Properties().attributes(net.minecraft.world.item.AxeItem.createAttributes(net.valentin.obsidium.item.SupremeToolMaterial.INSTANCE, 9.0f, -2.6f)).fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC)));
	public static final Item HEPHAESTEAN_SHOVEL = registerItem("hephaestean_shovel", 
		new net.valentin.obsidium.item.HephaesteanShovelItem(net.valentin.obsidium.item.SupremeToolMaterial.INSTANCE, 
		new Item.Properties().attributes(net.minecraft.world.item.ShovelItem.createAttributes(net.valentin.obsidium.item.SupremeToolMaterial.INSTANCE, 4.5f, -2.6f)).fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC)));
	public static final Item HEPHAESTEAN_HOE = registerItem("hephaestean_hoe", 
		new net.valentin.obsidium.item.HephaesteanHoeItem(net.valentin.obsidium.item.SupremeToolMaterial.INSTANCE, 
		new Item.Properties().attributes(net.minecraft.world.item.HoeItem.createAttributes(net.valentin.obsidium.item.SupremeToolMaterial.INSTANCE, 1, 0.0f)).fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC)));
	public static final Item HEPHAESTEAN_PAXEL = registerItem("hephaestean_paxel",
		new net.valentin.obsidium.item.HephaesteanPaxelItem(net.valentin.obsidium.item.SupremeToolMaterial.INSTANCE,
		new Item.Properties().attributes(net.minecraft.world.item.SwordItem.createAttributes(net.valentin.obsidium.item.SupremeToolMaterial.INSTANCE, 10, -1.8f)).fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC)));

	public static final Item HEPHAESTEAN_HELMET = registerItem("hephaestean_helmet", 
		new net.valentin.obsidium.item.HephaesteanArmorItem(net.valentin.obsidium.item.SupremeArmorMaterial.INSTANCE, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC)));
	public static final Item HEPHAESTEAN_CHESTPLATE = registerItem("hephaestean_chestplate", 
		new net.valentin.obsidium.item.HephaesteanArmorItem(net.valentin.obsidium.item.SupremeArmorMaterial.INSTANCE, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC)));
	public static final Item HEPHAESTEAN_LEGGINGS = registerItem("hephaestean_leggings", 
		new net.valentin.obsidium.item.HephaesteanArmorItem(net.valentin.obsidium.item.SupremeArmorMaterial.INSTANCE, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC)));
	public static final Item HEPHAESTEAN_BOOTS = registerItem("hephaestean_boots", 
		new net.valentin.obsidium.item.HephaesteanArmorItem(net.valentin.obsidium.item.SupremeArmorMaterial.INSTANCE, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC)));

	// Stygian Death & Soul Preservation System
	public static final Item CHARON_COIN = registerItem("charon_coin",
		new net.valentin.obsidium.item.CharonCoinItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON).stacksTo(16)));
	public static final Item ETERNAL_COIN = registerItem("eternal_coin",
		new net.valentin.obsidium.item.EternalCoinItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).stacksTo(1).fireResistant()));
	public static final Item STYGIAN_KEY = registerItem("stygian_key",
		new net.valentin.obsidium.item.StygianKeyItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).stacksTo(1).fireResistant()));

	// Ore Detectors & Waypoint Navigation
	public static final Item PLOUTON_ROD = registerItem("plouton_rod",
		new net.valentin.obsidium.item.PloutonRodItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
	public static final Item HEPHAESTEAN_RESONATOR = registerItem("hephaestean_resonator",
		new net.valentin.obsidium.item.HephaesteanResonatorItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).fireResistant().stacksTo(1)));
	public static final Item TELLURIC_ASTROLABE = registerItem("telluric_astrolabe",
		new net.valentin.obsidium.item.TelluricAstrolabeItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).fireResistant().stacksTo(1)));

	// Aliases for Supreme tier
	public static final Item ADVANCED_VOLCANIC_SWORD = HEPHAESTEAN_SWORD;
	public static final Item ADVANCED_VOLCANIC_PICKAXE = HEPHAESTEAN_PICKAXE;
	public static final Item ADVANCED_VOLCANIC_AXE = HEPHAESTEAN_AXE;
	public static final Item ADVANCED_VOLCANIC_SHOVEL = HEPHAESTEAN_SHOVEL;
	public static final Item ADVANCED_VOLCANIC_HOE = HEPHAESTEAN_HOE;
	public static final Item ADVANCED_VOLCANIC_HELMET = HEPHAESTEAN_HELMET;
	public static final Item ADVANCED_VOLCANIC_CHESTPLATE = HEPHAESTEAN_CHESTPLATE;
	public static final Item ADVANCED_VOLCANIC_LEGGINGS = HEPHAESTEAN_LEGGINGS;
	public static final Item ADVANCED_VOLCANIC_BOOTS = HEPHAESTEAN_BOOTS;

	public static final Block VOLCANITE_ORE = registerBlock("volcanite_ore",
		new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).requiresCorrectToolForDrops().strength(3.0f, 3.0f)));
	public static final Item VOLCANITE_ORE_ITEM = registerItem("volcanite_ore",
		new BlockItem(VOLCANITE_ORE, new Item.Properties()));

	// Tin Ores & Blocks
	public static final Block TIN_ORE = registerBlock("tin_ore",
		new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE).requiresCorrectToolForDrops().strength(3.0f, 3.0f)));
	public static final Item TIN_ORE_ITEM = registerItem("tin_ore",
		new BlockItem(TIN_ORE, new Item.Properties()));

	public static final Block DEEPSLATE_TIN_ORE = registerBlock("deepslate_tin_ore",
		new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE).requiresCorrectToolForDrops().strength(4.5f, 3.0f)));
	public static final Item DEEPSLATE_TIN_ORE_ITEM = registerItem("deepslate_tin_ore",
		new BlockItem(DEEPSLATE_TIN_ORE, new Item.Properties()));

	public static final Block RAW_TIN_BLOCK = registerBlock("raw_tin_block",
		new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.RAW_IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0f, 6.0f)));
	public static final Item RAW_TIN_BLOCK_ITEM = registerItem("raw_tin_block",
		new BlockItem(RAW_TIN_BLOCK, new Item.Properties()));

	public static final Block TIN_BLOCK = registerBlock("tin_block",
		new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0f, 6.0f)));
	public static final Item TIN_BLOCK_ITEM = registerItem("tin_block",
		new BlockItem(TIN_BLOCK, new Item.Properties()));

	// Lead Ores & Blocks
	public static final Block LEAD_ORE = registerBlock("lead_ore",
		new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE).requiresCorrectToolForDrops().strength(3.0f, 3.0f)));
	public static final Item LEAD_ORE_ITEM = registerItem("lead_ore",
		new BlockItem(LEAD_ORE, new Item.Properties()));

	public static final Block DEEPSLATE_LEAD_ORE = registerBlock("deepslate_lead_ore",
		new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE).requiresCorrectToolForDrops().strength(4.5f, 3.0f)));
	public static final Item DEEPSLATE_LEAD_ORE_ITEM = registerItem("deepslate_lead_ore",
		new BlockItem(DEEPSLATE_LEAD_ORE, new Item.Properties()));

	public static final Block RAW_LEAD_BLOCK = registerBlock("raw_lead_block",
		new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.RAW_IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0f, 6.0f)));
	public static final Item RAW_LEAD_BLOCK_ITEM = registerItem("raw_lead_block",
		new BlockItem(RAW_LEAD_BLOCK, new Item.Properties()));

	public static final Block LEAD_BLOCK = registerBlock("lead_block",
		new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0f, 6.0f)));
	public static final Item LEAD_BLOCK_ITEM = registerItem("lead_block",
		new BlockItem(LEAD_BLOCK, new Item.Properties()));

	public static final Block SILVER_ORE = registerBlock("silver_ore",
		new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE).requiresCorrectToolForDrops().strength(3.0f, 3.0f)));
	public static final Item SILVER_ORE_ITEM = registerItem("silver_ore",
		new BlockItem(SILVER_ORE, new Item.Properties()));

	public static final Block DEEPSLATE_SILVER_ORE = registerBlock("deepslate_silver_ore",
		new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE).requiresCorrectToolForDrops().strength(4.5f, 3.0f)));
	public static final Item DEEPSLATE_SILVER_ORE_ITEM = registerItem("deepslate_silver_ore",
		new BlockItem(DEEPSLATE_SILVER_ORE, new Item.Properties()));

	public static final Block RAW_SILVER_BLOCK = registerBlock("raw_silver_block",
		new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.RAW_IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0f, 6.0f)));
	public static final Item RAW_SILVER_BLOCK_ITEM = registerItem("raw_silver_block",
		new BlockItem(RAW_SILVER_BLOCK, new Item.Properties()));

	public static final Block SILVER_BLOCK = registerBlock("silver_block",
		new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0f, 6.0f)));
	public static final Item SILVER_BLOCK_ITEM = registerItem("silver_block",
		new BlockItem(SILVER_BLOCK, new Item.Properties()));

	public static final Block VOLCANITE_BLOCK = registerBlock("volcanite_block",
		new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).requiresCorrectToolForDrops().strength(5.0f, 6.0f)));
	public static final Item VOLCANITE_BLOCK_ITEM = registerItem("volcanite_block",
		new BlockItem(VOLCANITE_BLOCK, new Item.Properties()));

	// Mineral-bearing trees native to the Volcanic Caldera.
	public static final Block MIDAS_GOLD_LOG = registerBlock("midas_gold_log", new net.minecraft.world.level.block.RotatedPillarBlock(
		BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).strength(2.0f, 2.0f).requiresCorrectToolForDrops()));
	public static final Item MIDAS_GOLD_LOG_ITEM = registerItem("midas_gold_log", new BlockItem(MIDAS_GOLD_LOG, new Item.Properties()));
	public static final Block MIDAS_GOLD_PLANKS = registerBlock("midas_gold_planks", new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(2.0f, 3.0f)));
	public static final Item MIDAS_GOLD_PLANKS_ITEM = registerItem("midas_gold_planks", new BlockItem(MIDAS_GOLD_PLANKS, new Item.Properties()));
	public static final Block MIDAS_GOLD_SLAB = registerBlock("midas_gold_slab", new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB).strength(2.0f, 3.0f)));
	public static final Item MIDAS_GOLD_SLAB_ITEM = registerItem("midas_gold_slab", new BlockItem(MIDAS_GOLD_SLAB, new Item.Properties()));
	public static final Block MIDAS_GOLD_STAIRS = registerBlock("midas_gold_stairs", new StairBlock(MIDAS_GOLD_PLANKS.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_STAIRS).strength(2.0f, 3.0f)));
	public static final Item MIDAS_GOLD_STAIRS_ITEM = registerItem("midas_gold_stairs", new BlockItem(MIDAS_GOLD_STAIRS, new Item.Properties()));
	public static final Block MIDAS_GOLD_FENCE = registerBlock("midas_gold_fence", new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE).strength(2.0f, 3.0f)));
	public static final Item MIDAS_GOLD_FENCE_ITEM = registerItem("midas_gold_fence", new BlockItem(MIDAS_GOLD_FENCE, new Item.Properties()));
	public static final Block MIDAS_GOLD_FENCE_GATE = registerBlock("midas_gold_fence_gate", new FenceGateBlock(WoodType.OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE).strength(2.0f, 3.0f)));
	public static final Item MIDAS_GOLD_FENCE_GATE_ITEM = registerItem("midas_gold_fence_gate", new BlockItem(MIDAS_GOLD_FENCE_GATE, new Item.Properties()));
	public static final Block MIDAS_GOLD_LEAVES = registerBlock("midas_gold_leaves", new net.valentin.obsidium.block.MineralTreeLeavesBlock("midas_gold",
		BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).randomTicks().noOcclusion()));
	public static final Item MIDAS_GOLD_LEAVES_ITEM = registerItem("midas_gold_leaves", new BlockItem(MIDAS_GOLD_LEAVES, new Item.Properties()));
	public static final Block MIDAS_GOLD_SAPLING = registerBlock("midas_gold_sapling", new net.valentin.obsidium.block.OreTreeSaplingBlock(
		"midas_gold", "midas_gold_tree", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING).randomTicks()));
	public static final Item MIDAS_GOLD_SAPLING_ITEM = registerItem("midas_gold_sapling", new BlockItem(MIDAS_GOLD_SAPLING, new Item.Properties()));
	public static final Item MIDAS_GOLD_FRUIT = registerItem("midas_gold_fruit", new net.valentin.obsidium.item.MetalFruitItem(
		new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationModifier(0.3f).build()), Items.GOLD_NUGGET, 3));

	public static final Block ARES_IRON_LOG = registerBlock("ares_iron_log", new net.minecraft.world.level.block.RotatedPillarBlock(
		BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_LOG).strength(2.0f, 2.0f).requiresCorrectToolForDrops()));
	public static final Item ARES_IRON_LOG_ITEM = registerItem("ares_iron_log", new BlockItem(ARES_IRON_LOG, new Item.Properties()));
	public static final Block ARES_IRON_PLANKS = registerBlock("ares_iron_planks", new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_PLANKS).strength(2.0f, 3.0f)));
	public static final Item ARES_IRON_PLANKS_ITEM = registerItem("ares_iron_planks", new BlockItem(ARES_IRON_PLANKS, new Item.Properties()));
	public static final Block ARES_IRON_SLAB = registerBlock("ares_iron_slab", new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_SLAB).strength(2.0f, 3.0f)));
	public static final Item ARES_IRON_SLAB_ITEM = registerItem("ares_iron_slab", new BlockItem(ARES_IRON_SLAB, new Item.Properties()));
	public static final Block ARES_IRON_STAIRS = registerBlock("ares_iron_stairs", new StairBlock(ARES_IRON_PLANKS.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_STAIRS).strength(2.0f, 3.0f)));
	public static final Item ARES_IRON_STAIRS_ITEM = registerItem("ares_iron_stairs", new BlockItem(ARES_IRON_STAIRS, new Item.Properties()));
	public static final Block ARES_IRON_FENCE = registerBlock("ares_iron_fence", new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_FENCE).strength(2.0f, 3.0f)));
	public static final Item ARES_IRON_FENCE_ITEM = registerItem("ares_iron_fence", new BlockItem(ARES_IRON_FENCE, new Item.Properties()));
	public static final Block ARES_IRON_FENCE_GATE = registerBlock("ares_iron_fence_gate", new FenceGateBlock(WoodType.SPRUCE, BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_FENCE_GATE).strength(2.0f, 3.0f)));
	public static final Item ARES_IRON_FENCE_GATE_ITEM = registerItem("ares_iron_fence_gate", new BlockItem(ARES_IRON_FENCE_GATE, new Item.Properties()));
	public static final Block ARES_IRON_LEAVES = registerBlock("ares_iron_leaves", new net.valentin.obsidium.block.MineralTreeLeavesBlock("ares_iron",
		BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_LEAVES).randomTicks().noOcclusion()));
	public static final Item ARES_IRON_LEAVES_ITEM = registerItem("ares_iron_leaves", new BlockItem(ARES_IRON_LEAVES, new Item.Properties()));
	public static final Block ARES_IRON_SAPLING = registerBlock("ares_iron_sapling", new net.valentin.obsidium.block.OreTreeSaplingBlock(
		"ares_iron", "ares_iron_tree", BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_SAPLING).randomTicks()));
	public static final Item ARES_IRON_SAPLING_ITEM = registerItem("ares_iron_sapling", new BlockItem(ARES_IRON_SAPLING, new Item.Properties()));
	public static final Item ARES_IRON_FRUIT = registerItem("ares_iron_fruit", new net.valentin.obsidium.item.MetalFruitItem(
		new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationModifier(0.3f).build()), Items.RAW_IRON, 1));

	public static final Block HERMES_COPPER_LOG = registerBlock("hermes_copper_log", new net.minecraft.world.level.block.RotatedPillarBlock(
		BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_LOG).strength(2.0f, 2.0f).requiresCorrectToolForDrops()));
	public static final Item HERMES_COPPER_LOG_ITEM = registerItem("hermes_copper_log", new BlockItem(HERMES_COPPER_LOG, new Item.Properties()));
	public static final Block HERMES_COPPER_PLANKS = registerBlock("hermes_copper_planks", new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_PLANKS).strength(2.0f, 3.0f)));
	public static final Item HERMES_COPPER_PLANKS_ITEM = registerItem("hermes_copper_planks", new BlockItem(HERMES_COPPER_PLANKS, new Item.Properties()));
	public static final Block HERMES_COPPER_SLAB = registerBlock("hermes_copper_slab", new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_SLAB).strength(2.0f, 3.0f)));
	public static final Item HERMES_COPPER_SLAB_ITEM = registerItem("hermes_copper_slab", new BlockItem(HERMES_COPPER_SLAB, new Item.Properties()));
	public static final Block HERMES_COPPER_STAIRS = registerBlock("hermes_copper_stairs", new StairBlock(HERMES_COPPER_PLANKS.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_STAIRS).strength(2.0f, 3.0f)));
	public static final Item HERMES_COPPER_STAIRS_ITEM = registerItem("hermes_copper_stairs", new BlockItem(HERMES_COPPER_STAIRS, new Item.Properties()));
	public static final Block HERMES_COPPER_FENCE = registerBlock("hermes_copper_fence", new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_FENCE).strength(2.0f, 3.0f)));
	public static final Item HERMES_COPPER_FENCE_ITEM = registerItem("hermes_copper_fence", new BlockItem(HERMES_COPPER_FENCE, new Item.Properties()));
	public static final Block HERMES_COPPER_FENCE_GATE = registerBlock("hermes_copper_fence_gate", new FenceGateBlock(WoodType.JUNGLE, BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_FENCE_GATE).strength(2.0f, 3.0f)));
	public static final Item HERMES_COPPER_FENCE_GATE_ITEM = registerItem("hermes_copper_fence_gate", new BlockItem(HERMES_COPPER_FENCE_GATE, new Item.Properties()));
	public static final Block HERMES_COPPER_LEAVES = registerBlock("hermes_copper_leaves", new net.valentin.obsidium.block.MineralTreeLeavesBlock("hermes_copper",
		BlockBehaviour.Properties.ofFullCopy(Blocks.AZALEA_LEAVES).randomTicks().noOcclusion()));
	public static final Item HERMES_COPPER_LEAVES_ITEM = registerItem("hermes_copper_leaves", new BlockItem(HERMES_COPPER_LEAVES, new Item.Properties()));
	public static final Block HERMES_COPPER_SAPLING = registerBlock("hermes_copper_sapling", new net.valentin.obsidium.block.OreTreeSaplingBlock(
		"hermes_copper", "hermes_copper_tree", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING).randomTicks()));
	public static final Item HERMES_COPPER_SAPLING_ITEM = registerItem("hermes_copper_sapling", new BlockItem(HERMES_COPPER_SAPLING, new Item.Properties()));
	public static final Item HERMES_COPPER_FRUIT = registerItem("hermes_copper_fruit", new net.valentin.obsidium.item.MetalFruitItem(
		new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationModifier(0.3f).build()), Items.COPPER_INGOT, 1));

	public static final Block HEPHAESTUS_VOLCANITE_LOG = registerBlock("hephaestus_volcanite_log", new net.minecraft.world.level.block.RotatedPillarBlock(
		BlockBehaviour.Properties.ofFullCopy(Blocks.CRIMSON_STEM).strength(2.5f, 2.5f).requiresCorrectToolForDrops()));
	public static final Item HEPHAESTUS_VOLCANITE_LOG_ITEM = registerItem("hephaestus_volcanite_log", new BlockItem(HEPHAESTUS_VOLCANITE_LOG, new Item.Properties().fireResistant()));
	public static final Block HEPHAESTUS_VOLCANITE_PLANKS = registerBlock("hephaestus_volcanite_planks", new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CRIMSON_PLANKS).strength(2.5f, 3.5f)));
	public static final Item HEPHAESTUS_VOLCANITE_PLANKS_ITEM = registerItem("hephaestus_volcanite_planks", new BlockItem(HEPHAESTUS_VOLCANITE_PLANKS, new Item.Properties().fireResistant()));
	public static final Block HEPHAESTUS_VOLCANITE_SLAB = registerBlock("hephaestus_volcanite_slab", new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CRIMSON_SLAB).strength(2.5f, 3.5f)));
	public static final Item HEPHAESTUS_VOLCANITE_SLAB_ITEM = registerItem("hephaestus_volcanite_slab", new BlockItem(HEPHAESTUS_VOLCANITE_SLAB, new Item.Properties().fireResistant()));
	public static final Block HEPHAESTUS_VOLCANITE_STAIRS = registerBlock("hephaestus_volcanite_stairs", new StairBlock(HEPHAESTUS_VOLCANITE_PLANKS.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.CRIMSON_STAIRS).strength(2.5f, 3.5f)));
	public static final Item HEPHAESTUS_VOLCANITE_STAIRS_ITEM = registerItem("hephaestus_volcanite_stairs", new BlockItem(HEPHAESTUS_VOLCANITE_STAIRS, new Item.Properties().fireResistant()));
	public static final Block HEPHAESTUS_VOLCANITE_FENCE = registerBlock("hephaestus_volcanite_fence", new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CRIMSON_FENCE).strength(2.5f, 3.5f)));
	public static final Item HEPHAESTUS_VOLCANITE_FENCE_ITEM = registerItem("hephaestus_volcanite_fence", new BlockItem(HEPHAESTUS_VOLCANITE_FENCE, new Item.Properties().fireResistant()));
	public static final Block HEPHAESTUS_VOLCANITE_FENCE_GATE = registerBlock("hephaestus_volcanite_fence_gate", new FenceGateBlock(WoodType.CRIMSON, BlockBehaviour.Properties.ofFullCopy(Blocks.CRIMSON_FENCE_GATE).strength(2.5f, 3.5f)));
	public static final Item HEPHAESTUS_VOLCANITE_FENCE_GATE_ITEM = registerItem("hephaestus_volcanite_fence_gate", new BlockItem(HEPHAESTUS_VOLCANITE_FENCE_GATE, new Item.Properties().fireResistant()));
	public static final Block HEPHAESTUS_VOLCANITE_LEAVES = registerBlock("hephaestus_volcanite_leaves", new net.valentin.obsidium.block.MineralTreeLeavesBlock("hephaestus_volcanite",
		BlockBehaviour.Properties.ofFullCopy(Blocks.FLOWERING_AZALEA_LEAVES).randomTicks().noOcclusion()));
	public static final Item HEPHAESTUS_VOLCANITE_LEAVES_ITEM = registerItem("hephaestus_volcanite_leaves", new BlockItem(HEPHAESTUS_VOLCANITE_LEAVES, new Item.Properties().fireResistant()));
	public static final Block HEPHAESTUS_VOLCANITE_SAPLING = registerBlock("hephaestus_volcanite_sapling", new net.valentin.obsidium.block.OreTreeSaplingBlock(
		"hephaestus_volcanite", "hephaestus_volcanite_tree", BlockBehaviour.Properties.ofFullCopy(Blocks.CRIMSON_FUNGUS).randomTicks()));
	public static final Item HEPHAESTUS_VOLCANITE_SAPLING_ITEM = registerItem("hephaestus_volcanite_sapling", new BlockItem(HEPHAESTUS_VOLCANITE_SAPLING, new Item.Properties().fireResistant()));
	public static final Item HEPHAESTUS_VOLCANITE_FRUIT = registerItem("hephaestus_volcanite_fruit", new net.valentin.obsidium.item.MetalFruitItem(
		new Item.Properties().fireResistant().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationModifier(0.3f).build()), VOLCANITE_INGOT, 1));

	// --- RARE MEGA BONSAI APEX HYBRID: PYRETIC TITAN VOLCANITE TREE (5% Rare Mutation) ---
	public static final Block PYRETIC_TITAN_LOG = registerBlock("pyretic_titan_log", new net.minecraft.world.level.block.RotatedPillarBlock(
		BlockBehaviour.Properties.ofFullCopy(Blocks.ANCIENT_DEBRIS).strength(3.5f, 4.0f).lightLevel(state -> 6).requiresCorrectToolForDrops()));
	public static final Item PYRETIC_TITAN_LOG_ITEM = registerItem("pyretic_titan_log", new BlockItem(PYRETIC_TITAN_LOG, new Item.Properties().fireResistant()));

	public static final Block PYRETIC_TITAN_LEAVES = registerBlock("pyretic_titan_leaves", new net.valentin.obsidium.block.MineralTreeLeavesBlock("pyretic_titan",
		BlockBehaviour.Properties.ofFullCopy(Blocks.FLOWERING_AZALEA_LEAVES).lightLevel(state -> 9).randomTicks().noOcclusion()));
	public static final Item PYRETIC_TITAN_LEAVES_ITEM = registerItem("pyretic_titan_leaves", new BlockItem(PYRETIC_TITAN_LEAVES, new Item.Properties().fireResistant()));

	public static final Block PYRETIC_TITAN_SAPLING = registerBlock("pyretic_titan_sapling", new net.valentin.obsidium.block.OreTreeSaplingBlock(
		"pyretic_titan", "hephaestus_volcanite_tree", BlockBehaviour.Properties.ofFullCopy(Blocks.CRIMSON_FUNGUS).lightLevel(state -> 8).randomTicks()));
	public static final Item PYRETIC_TITAN_SAPLING_ITEM = registerItem("pyretic_titan_sapling", new BlockItem(PYRETIC_TITAN_SAPLING, new Item.Properties().fireResistant().rarity(Rarity.EPIC)));

	// The first hybrid tree, born from the Gold (Midas) and Copper (Hermes) lineages.
	public static final Block ELECTRUM_LOG = registerBlock("electrum_log", new net.minecraft.world.level.block.RotatedPillarBlock(
		BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).strength(2.2f, 2.2f).requiresCorrectToolForDrops()));
	public static final Item ELECTRUM_LOG_ITEM = registerItem("electrum_log", new BlockItem(ELECTRUM_LOG, new Item.Properties()));
	public static final Block ELECTRUM_PLANKS = registerBlock("electrum_planks", new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(2.2f, 3.0f)));
	public static final Item ELECTRUM_PLANKS_ITEM = registerItem("electrum_planks", new BlockItem(ELECTRUM_PLANKS, new Item.Properties()));
	public static final Block ELECTRUM_SLAB = registerBlock("electrum_slab", new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB).strength(2.2f, 3.0f)));
	public static final Item ELECTRUM_SLAB_ITEM = registerItem("electrum_slab", new BlockItem(ELECTRUM_SLAB, new Item.Properties()));
	public static final Block ELECTRUM_STAIRS = registerBlock("electrum_stairs", new StairBlock(ELECTRUM_PLANKS.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_STAIRS).strength(2.2f, 3.0f)));
	public static final Item ELECTRUM_STAIRS_ITEM = registerItem("electrum_stairs", new BlockItem(ELECTRUM_STAIRS, new Item.Properties()));
	public static final Block ELECTRUM_FENCE = registerBlock("electrum_fence", new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE).strength(2.2f, 3.0f)));
	public static final Item ELECTRUM_FENCE_ITEM = registerItem("electrum_fence", new BlockItem(ELECTRUM_FENCE, new Item.Properties()));
	public static final Block ELECTRUM_FENCE_GATE = registerBlock("electrum_fence_gate", new FenceGateBlock(WoodType.OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE).strength(2.2f, 3.0f)));
	public static final Item ELECTRUM_FENCE_GATE_ITEM = registerItem("electrum_fence_gate", new BlockItem(ELECTRUM_FENCE_GATE, new Item.Properties()));
	public static final Block ELECTRUM_LEAVES = registerBlock("electrum_leaves", new net.valentin.obsidium.block.MineralTreeLeavesBlock("electrum",
		BlockBehaviour.Properties.ofFullCopy(Blocks.FLOWERING_AZALEA_LEAVES).randomTicks().noOcclusion()));
	public static final Item ELECTRUM_LEAVES_ITEM = registerItem("electrum_leaves", new BlockItem(ELECTRUM_LEAVES, new Item.Properties()));
	public static final Block ELECTRUM_SAPLING = registerBlock("electrum_sapling", new net.valentin.obsidium.block.OreTreeSaplingBlock(
		"electrum", "electrum_tree", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING).randomTicks()));
	public static final Item ELECTRUM_SAPLING_ITEM = registerItem("electrum_sapling", new BlockItem(ELECTRUM_SAPLING, new Item.Properties()));
	public static final Item ELECTRUM_FRUIT = registerItem("electrum_fruit", new net.valentin.obsidium.item.MetalFruitItem(
		new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationModifier(0.4f).build()), Items.GOLD_INGOT, 1));

	// Colossal 2x2 Tartarean Netherite Mega Tree - Legendary subterranean mutation
	public static final Block TARTAREAN_NETHERITE_LOG = registerBlock("tartarean_netherite_log", new net.minecraft.world.level.block.RotatedPillarBlock(
		BlockBehaviour.Properties.ofFullCopy(Blocks.ANCIENT_DEBRIS).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));
	public static final Item TARTAREAN_NETHERITE_LOG_ITEM = registerItem("tartarean_netherite_log", new BlockItem(TARTAREAN_NETHERITE_LOG, new Item.Properties().fireResistant()));
	public static final Block TARTAREAN_NETHERITE_PLANKS = registerBlock("tartarean_netherite_planks", new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CRIMSON_PLANKS).strength(3.0f, 4.0f)));
	public static final Item TARTAREAN_NETHERITE_PLANKS_ITEM = registerItem("tartarean_netherite_planks", new BlockItem(TARTAREAN_NETHERITE_PLANKS, new Item.Properties().fireResistant()));
	public static final Block TARTAREAN_NETHERITE_SLAB = registerBlock("tartarean_netherite_slab", new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CRIMSON_SLAB).strength(3.0f, 4.0f)));
	public static final Item TARTAREAN_NETHERITE_SLAB_ITEM = registerItem("tartarean_netherite_slab", new BlockItem(TARTAREAN_NETHERITE_SLAB, new Item.Properties().fireResistant()));
	public static final Block TARTAREAN_NETHERITE_STAIRS = registerBlock("tartarean_netherite_stairs", new StairBlock(TARTAREAN_NETHERITE_PLANKS.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.CRIMSON_STAIRS).strength(3.0f, 4.0f)));
	public static final Item TARTAREAN_NETHERITE_STAIRS_ITEM = registerItem("tartarean_netherite_stairs", new BlockItem(TARTAREAN_NETHERITE_STAIRS, new Item.Properties().fireResistant()));
	public static final Block TARTAREAN_NETHERITE_FENCE = registerBlock("tartarean_netherite_fence", new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CRIMSON_FENCE).strength(3.0f, 4.0f)));
	public static final Item TARTAREAN_NETHERITE_FENCE_ITEM = registerItem("tartarean_netherite_fence", new BlockItem(TARTAREAN_NETHERITE_FENCE, new Item.Properties().fireResistant()));
	public static final Block TARTAREAN_NETHERITE_FENCE_GATE = registerBlock("tartarean_netherite_fence_gate", new FenceGateBlock(WoodType.CRIMSON, BlockBehaviour.Properties.ofFullCopy(Blocks.CRIMSON_FENCE_GATE).strength(3.0f, 4.0f)));
	public static final Item TARTAREAN_NETHERITE_FENCE_GATE_ITEM = registerItem("tartarean_netherite_fence_gate", new BlockItem(TARTAREAN_NETHERITE_FENCE_GATE, new Item.Properties().fireResistant()));

	public static final Block TARTAREAN_NETHERITE_LEAVES = registerBlock("tartarean_netherite_leaves", new net.valentin.obsidium.block.MineralTreeLeavesBlock("tartarean_netherite",
		BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_LEAVES).randomTicks().noOcclusion()));
	public static final Item TARTAREAN_NETHERITE_LEAVES_ITEM = registerItem("tartarean_netherite_leaves", new BlockItem(TARTAREAN_NETHERITE_LEAVES, new Item.Properties().fireResistant()));

	public static final Block TARTAREAN_NETHERITE_SAPLING = registerBlock("tartarean_netherite_sapling", new net.valentin.obsidium.block.OreTreeSaplingBlock(
		"tartarean_netherite", "tartarean_netherite_mega_tree", "tartarean_netherite_tree", BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_SAPLING).randomTicks()));
	public static final Item TARTAREAN_NETHERITE_SAPLING_ITEM = registerItem("tartarean_netherite_sapling", new BlockItem(TARTAREAN_NETHERITE_SAPLING, new Item.Properties().fireResistant()));

	public static final Item TARTAREAN_NETHERITE_FRUIT = registerItem("tartarean_netherite_fruit", new net.valentin.obsidium.item.MetalFruitItem(
		new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.RARE).food(
			new net.minecraft.world.food.FoodProperties.Builder()
				.nutrition(6)
				.saturationModifier(0.8f)
				.effect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 1200, 0), 1.0f)
				.effect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 600, 0), 1.0f)
				.alwaysEdible()
				.build()
		), Items.NETHERITE_SCRAP, 1));

	// Cronus Lead Tree - Titan of Heavy Minerals & Ancient Time
	public static final Block CRONUS_LEAD_LOG = registerBlock("cronus_lead_log", new net.minecraft.world.level.block.RotatedPillarBlock(
		BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_LOG).strength(2.4f, 2.4f).requiresCorrectToolForDrops()));
	public static final Item CRONUS_LEAD_LOG_ITEM = registerItem("cronus_lead_log", new BlockItem(CRONUS_LEAD_LOG, new Item.Properties()));
	public static final Block CRONUS_LEAD_PLANKS = registerBlock("cronus_lead_planks", new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_PLANKS).strength(2.4f, 3.2f)));
	public static final Item CRONUS_LEAD_PLANKS_ITEM = registerItem("cronus_lead_planks", new BlockItem(CRONUS_LEAD_PLANKS, new Item.Properties()));
	public static final Block CRONUS_LEAD_SLAB = registerBlock("cronus_lead_slab", new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_SLAB).strength(2.4f, 3.2f)));
	public static final Item CRONUS_LEAD_SLAB_ITEM = registerItem("cronus_lead_slab", new BlockItem(CRONUS_LEAD_SLAB, new Item.Properties()));
	public static final Block CRONUS_LEAD_STAIRS = registerBlock("cronus_lead_stairs", new StairBlock(CRONUS_LEAD_PLANKS.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_STAIRS).strength(2.4f, 3.2f)));
	public static final Item CRONUS_LEAD_STAIRS_ITEM = registerItem("cronus_lead_stairs", new BlockItem(CRONUS_LEAD_STAIRS, new Item.Properties()));
	public static final Block CRONUS_LEAD_FENCE = registerBlock("cronus_lead_fence", new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_FENCE).strength(2.4f, 3.2f)));
	public static final Item CRONUS_LEAD_FENCE_ITEM = registerItem("cronus_lead_fence", new BlockItem(CRONUS_LEAD_FENCE, new Item.Properties()));
	public static final Block CRONUS_LEAD_FENCE_GATE = registerBlock("cronus_lead_fence_gate", new FenceGateBlock(WoodType.DARK_OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_FENCE_GATE).strength(2.4f, 3.2f)));
	public static final Item CRONUS_LEAD_FENCE_GATE_ITEM = registerItem("cronus_lead_fence_gate", new BlockItem(CRONUS_LEAD_FENCE_GATE, new Item.Properties()));
	public static final Block CRONUS_LEAD_LEAVES = registerBlock("cronus_lead_leaves", new net.valentin.obsidium.block.MineralTreeLeavesBlock("cronus_lead",
		BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_LEAVES).randomTicks().noOcclusion()));
	public static final Item CRONUS_LEAD_LEAVES_ITEM = registerItem("cronus_lead_leaves", new BlockItem(CRONUS_LEAD_LEAVES, new Item.Properties()));
	public static final Block CRONUS_LEAD_SAPLING = registerBlock("cronus_lead_sapling", new net.valentin.obsidium.block.OreTreeSaplingBlock(
		"cronus_lead", "cronus_lead_tree", BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_SAPLING).randomTicks()));
	public static final Item CRONUS_LEAD_SAPLING_ITEM = registerItem("cronus_lead_sapling", new BlockItem(CRONUS_LEAD_SAPLING, new Item.Properties()));
	public static final Item CRONUS_LEAD_FRUIT = registerItem("cronus_lead_fruit", new net.valentin.obsidium.item.MetalFruitItem(
		new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationModifier(0.4f)
			.effect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 400, 0), 1.0f)
			.effect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 200, 0), 0.5f).build()), RAW_LEAD, 1));

	// Zeus Tin Tree - Olympian King of Thunder, Sky & Shimmering Metal
	public static final Block ZEUS_TIN_LOG = registerBlock("zeus_tin_log", new net.minecraft.world.level.block.RotatedPillarBlock(
		BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_LOG).strength(2.0f, 2.0f).requiresCorrectToolForDrops()));
	public static final Item ZEUS_TIN_LOG_ITEM = registerItem("zeus_tin_log", new BlockItem(ZEUS_TIN_LOG, new Item.Properties()));
	public static final Block ZEUS_TIN_PLANKS = registerBlock("zeus_tin_planks", new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_PLANKS).strength(2.0f, 3.0f)));
	public static final Item ZEUS_TIN_PLANKS_ITEM = registerItem("zeus_tin_planks", new BlockItem(ZEUS_TIN_PLANKS, new Item.Properties()));
	public static final Block ZEUS_TIN_SLAB = registerBlock("zeus_tin_slab", new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_SLAB).strength(2.0f, 3.0f)));
	public static final Item ZEUS_TIN_SLAB_ITEM = registerItem("zeus_tin_slab", new BlockItem(ZEUS_TIN_SLAB, new Item.Properties()));
	public static final Block ZEUS_TIN_STAIRS = registerBlock("zeus_tin_stairs", new StairBlock(ZEUS_TIN_PLANKS.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_STAIRS).strength(2.0f, 3.0f)));
	public static final Item ZEUS_TIN_STAIRS_ITEM = registerItem("zeus_tin_stairs", new BlockItem(ZEUS_TIN_STAIRS, new Item.Properties()));
	public static final Block ZEUS_TIN_FENCE = registerBlock("zeus_tin_fence", new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_FENCE).strength(2.0f, 3.0f)));
	public static final Item ZEUS_TIN_FENCE_ITEM = registerItem("zeus_tin_fence", new BlockItem(ZEUS_TIN_FENCE, new Item.Properties()));
	public static final Block ZEUS_TIN_FENCE_GATE = registerBlock("zeus_tin_fence_gate", new FenceGateBlock(WoodType.BIRCH, BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_FENCE_GATE).strength(2.0f, 3.0f)));
	public static final Item ZEUS_TIN_FENCE_GATE_ITEM = registerItem("zeus_tin_fence_gate", new BlockItem(ZEUS_TIN_FENCE_GATE, new Item.Properties()));
	public static final Block ZEUS_TIN_LEAVES = registerBlock("zeus_tin_leaves", new net.valentin.obsidium.block.MineralTreeLeavesBlock("zeus_tin",
		BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LEAVES).randomTicks().noOcclusion()));
	public static final Item ZEUS_TIN_LEAVES_ITEM = registerItem("zeus_tin_leaves", new BlockItem(ZEUS_TIN_LEAVES, new Item.Properties()));
	public static final Block ZEUS_TIN_SAPLING = registerBlock("zeus_tin_sapling", new net.valentin.obsidium.block.OreTreeSaplingBlock(
		"zeus_tin", "zeus_tin_tree", BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_SAPLING).randomTicks()));
	public static final Item ZEUS_TIN_SAPLING_ITEM = registerItem("zeus_tin_sapling", new BlockItem(ZEUS_TIN_SAPLING, new Item.Properties()));
	public static final Item ZEUS_TIN_FRUIT = registerItem("zeus_tin_fruit", new net.valentin.obsidium.item.MetalFruitItem(
		new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationModifier(0.4f)
			.effect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 400, 0), 1.0f)
			.effect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.JUMP, 400, 0), 1.0f).build()), TIN_INGOT, 1));

	// Artemis Silver Tree - Olympian Goddess of the Moon, Wilderness & Divine Silver Bow
	public static final Block ARTEMIS_SILVER_LOG = registerBlock("artemis_silver_log", new net.minecraft.world.level.block.RotatedPillarBlock(
		BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_LOG).strength(2.0f, 2.0f).requiresCorrectToolForDrops()));
	public static final Item ARTEMIS_SILVER_LOG_ITEM = registerItem("artemis_silver_log", new BlockItem(ARTEMIS_SILVER_LOG, new Item.Properties()));
	public static final Block ARTEMIS_SILVER_PLANKS = registerBlock("artemis_silver_planks", new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_PLANKS).strength(2.0f, 3.0f)));
	public static final Item ARTEMIS_SILVER_PLANKS_ITEM = registerItem("artemis_silver_planks", new BlockItem(ARTEMIS_SILVER_PLANKS, new Item.Properties()));
	public static final Block ARTEMIS_SILVER_SLAB = registerBlock("artemis_silver_slab", new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_SLAB).strength(2.0f, 3.0f)));
	public static final Item ARTEMIS_SILVER_SLAB_ITEM = registerItem("artemis_silver_slab", new BlockItem(ARTEMIS_SILVER_SLAB, new Item.Properties()));
	public static final Block ARTEMIS_SILVER_STAIRS = registerBlock("artemis_silver_stairs", new StairBlock(ARTEMIS_SILVER_PLANKS.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_STAIRS).strength(2.0f, 3.0f)));
	public static final Item ARTEMIS_SILVER_STAIRS_ITEM = registerItem("artemis_silver_stairs", new BlockItem(ARTEMIS_SILVER_STAIRS, new Item.Properties()));
	public static final Block ARTEMIS_SILVER_FENCE = registerBlock("artemis_silver_fence", new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_FENCE).strength(2.0f, 3.0f)));
	public static final Item ARTEMIS_SILVER_FENCE_ITEM = registerItem("artemis_silver_fence", new BlockItem(ARTEMIS_SILVER_FENCE, new Item.Properties()));
	public static final Block ARTEMIS_SILVER_FENCE_GATE = registerBlock("artemis_silver_fence_gate", new FenceGateBlock(WoodType.BIRCH, BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_FENCE_GATE).strength(2.0f, 3.0f)));
	public static final Item ARTEMIS_SILVER_FENCE_GATE_ITEM = registerItem("artemis_silver_fence_gate", new BlockItem(ARTEMIS_SILVER_FENCE_GATE, new Item.Properties()));
	public static final Block ARTEMIS_SILVER_LEAVES = registerBlock("artemis_silver_leaves", new net.valentin.obsidium.block.MineralTreeLeavesBlock("artemis_silver",
		BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LEAVES).randomTicks().noOcclusion()));
	public static final Item ARTEMIS_SILVER_LEAVES_ITEM = registerItem("artemis_silver_leaves", new BlockItem(ARTEMIS_SILVER_LEAVES, new Item.Properties()));
	public static final Block ARTEMIS_SILVER_SAPLING = registerBlock("artemis_silver_sapling", new net.valentin.obsidium.block.OreTreeSaplingBlock(
		"artemis_silver", "artemis_silver_tree", BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_SAPLING).randomTicks()));
	public static final Item ARTEMIS_SILVER_SAPLING_ITEM = registerItem("artemis_silver_sapling", new BlockItem(ARTEMIS_SILVER_SAPLING, new Item.Properties()));
	public static final Item ARTEMIS_SILVER_FRUIT = registerItem("artemis_silver_fruit", new net.valentin.obsidium.item.MetalFruitItem(
		new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationModifier(0.5f)
			.effect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 600, 0), 1.0f)
			.effect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY, 300, 0), 0.5f).build()), SILVER_INGOT, 1));

	// 8 New Grafted Hybrid Ore Trees
	public static final Block HADES_COAL_SAPLING = registerBlock("hades_coal_sapling", new net.valentin.obsidium.block.OreTreeSaplingBlock(
		"hades_coal", "oak_tree", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING).randomTicks()));
	public static final Item HADES_COAL_SAPLING_ITEM = registerItem("hades_coal_sapling", new BlockItem(HADES_COAL_SAPLING, new Item.Properties()));

	public static final Block ASTRAPE_REDSTONE_SAPLING = registerBlock("astrape_redstone_sapling", new net.valentin.obsidium.block.OreTreeSaplingBlock(
		"astrape_redstone", "oak_tree", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING).randomTicks()));
	public static final Item ASTRAPE_REDSTONE_SAPLING_ITEM = registerItem("astrape_redstone_sapling", new BlockItem(ASTRAPE_REDSTONE_SAPLING, new Item.Properties()));

	public static final Block POSEIDON_LAPIS_SAPLING = registerBlock("poseidon_lapis_sapling", new net.valentin.obsidium.block.OreTreeSaplingBlock(
		"poseidon_lapis", "oak_tree", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING).randomTicks()));
	public static final Item POSEIDON_LAPIS_SAPLING_ITEM = registerItem("poseidon_lapis_sapling", new BlockItem(POSEIDON_LAPIS_SAPLING, new Item.Properties()));

	public static final Block APOLLO_QUARTZ_SAPLING = registerBlock("apollo_quartz_sapling", new net.valentin.obsidium.block.OreTreeSaplingBlock(
		"apollo_quartz", "oak_tree", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING).randomTicks()));
	public static final Item APOLLO_QUARTZ_SAPLING_ITEM = registerItem("apollo_quartz_sapling", new BlockItem(APOLLO_QUARTZ_SAPLING, new Item.Properties()));

	public static final Block DIONYSIAN_AMETHYST_SAPLING = registerBlock("dionysian_amethyst_sapling", new net.valentin.obsidium.block.OreTreeSaplingBlock(
		"dionysian_amethyst", "oak_tree", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING).randomTicks()));
	public static final Item DIONYSIAN_AMETHYST_SAPLING_ITEM = registerItem("dionysian_amethyst_sapling", new BlockItem(DIONYSIAN_AMETHYST_SAPLING, new Item.Properties()));

	public static final Block HYPERION_GLOWSTONE_SAPLING = registerBlock("hyperion_glowstone_sapling", new net.valentin.obsidium.block.OreTreeSaplingBlock(
		"hyperion_glowstone", "oak_tree", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING).randomTicks()));
	public static final Item HYPERION_GLOWSTONE_SAPLING_ITEM = registerItem("hyperion_glowstone_sapling", new BlockItem(HYPERION_GLOWSTONE_SAPLING, new Item.Properties()));

	public static final Block PLOUTON_EMERALD_SAPLING = registerBlock("plouton_emerald_sapling", new net.valentin.obsidium.block.OreTreeSaplingBlock(
		"plouton_emerald", "oak_tree", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING).randomTicks()));
	public static final Item PLOUTON_EMERALD_SAPLING_ITEM = registerItem("plouton_emerald_sapling", new BlockItem(PLOUTON_EMERALD_SAPLING, new Item.Properties()));

	public static final Block ADAMANTINE_DIAMOND_SAPLING = registerBlock("adamantine_diamond_sapling", new net.valentin.obsidium.block.OreTreeSaplingBlock(
		"adamantine_diamond", "oak_tree", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING).randomTicks()));
	public static final Item ADAMANTINE_DIAMOND_SAPLING_ITEM = registerItem("adamantine_diamond_sapling", new BlockItem(ADAMANTINE_DIAMOND_SAPLING, new Item.Properties().rarity(Rarity.RARE)));

	public static final Block VOLCANITE_FURNACE_BLOCK = registerBlock("volcanite_furnace", 
		new VolcaniteFurnaceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(5.0f, 1200.0f).requiresCorrectToolForDrops()));
	public static final Item VOLCANITE_FURNACE_ITEM = registerItem("volcanite_furnace", 
		new BlockItem(VOLCANITE_FURNACE_BLOCK, new Item.Properties()));
		
	public static final Block VOLCANITE_WORKTABLE_BLOCK = registerBlock("volcanite_worktable", 
		new VolcaniteWorktableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN)));
	public static final Item VOLCANITE_WORKTABLE_ITEM = registerItem("volcanite_worktable", 
		new BlockItem(VOLCANITE_WORKTABLE_BLOCK, new Item.Properties()));

	public static final Block VOLCANITE_CHEST_BLOCK = registerBlock("volcanite_chest", 
		new net.valentin.obsidium.block.VolcaniteChestBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN)));
	public static final Item VOLCANITE_CHEST_ITEM = registerItem("volcanite_chest", 
		new BlockItem(VOLCANITE_CHEST_BLOCK, new Item.Properties()));

	public static final Block VOLCANITE_MODULAR_CHEST_BLOCK = registerBlock("volcanite_modular_chest", 
		new net.valentin.obsidium.block.VolcaniteModularChestBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(5.0f, 1200.0f).requiresCorrectToolForDrops().noOcclusion()));
	public static final Item VOLCANITE_MODULAR_CHEST_ITEM = registerItem("volcanite_modular_chest", 
		new BlockItem(VOLCANITE_MODULAR_CHEST_BLOCK, new Item.Properties().fireResistant()));

	public static final Block HEPHAESTEAN_ARMORY_BLOCK = registerBlock("hephaestean_armory", 
		new net.valentin.obsidium.block.HephaesteanArmoryBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN)));
	public static final Item HEPHAESTEAN_ARMORY_ITEM = registerItem("hephaestean_armory", 
		new BlockItem(HEPHAESTEAN_ARMORY_BLOCK, new Item.Properties()));
	public static final Block VOLCANITE_TOOL_CHEST_BLOCK = HEPHAESTEAN_ARMORY_BLOCK;
	public static final Item VOLCANITE_TOOL_CHEST_ITEM = HEPHAESTEAN_ARMORY_ITEM;

	public static final Block STYGIAN_CENOTAPH_BLOCK = registerBlock("stygian_cenotaph",
		new net.valentin.obsidium.block.StygianCenotaphBlock(BlockBehaviour.Properties.of().strength(55.0F, 3600000.0F).lightLevel(s -> 7).noOcclusion().noLootTable()));
	public static final Item STYGIAN_CENOTAPH_ITEM = registerItem("stygian_cenotaph",
		new BlockItem(STYGIAN_CENOTAPH_BLOCK, new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));

	public static final Block VOLCANITE_GLASS_BLOCK = registerBlock("volcanite_glass", 
		new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noOcclusion().isViewBlocking((s,l,p) -> false)));
	public static final Item VOLCANITE_GLASS_ITEM = registerItem("volcanite_glass", 
		new BlockItem(VOLCANITE_GLASS_BLOCK, new Item.Properties()));
		
	public static final Block VOLCANITE_CASING_BLOCK = registerBlock("volcanite_casing", 
		new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));
	public static final Item VOLCANITE_CASING_ITEM = registerItem("volcanite_casing", 
		new BlockItem(VOLCANITE_CASING_BLOCK, new Item.Properties()));

	public static final Block ADVANCED_VOLCANITE_CASING_BLOCK = registerBlock("advanced_volcanite_casing", 
		new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(6.0f, 1200.0f).requiresCorrectToolForDrops()));
	public static final Item ADVANCED_VOLCANITE_CASING_ITEM = registerItem("advanced_volcanite_casing", 
		new BlockItem(ADVANCED_VOLCANITE_CASING_BLOCK, new Item.Properties().fireResistant()));

	public static final Block SUPREME_VOLCANITE_GLASS_BLOCK = registerBlock("supreme_volcanite_glass", 
		new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noOcclusion().lightLevel(s -> 7).strength(5.0f, 1200.0f).requiresCorrectToolForDrops().isViewBlocking((s,l,p) -> false)));
	public static final Item SUPREME_VOLCANITE_GLASS_ITEM = registerItem("supreme_volcanite_glass", 
		new BlockItem(SUPREME_VOLCANITE_GLASS_BLOCK, new Item.Properties().fireResistant()));

	public static final Block REINFORCED_TINTED_GLASS_BLOCK = registerBlock("reinforced_tinted_glass",
		new net.valentin.obsidium.block.ReinforcedTintedGlassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.TINTED_GLASS).strength(5.0f, 1200.0f).explosionResistance(1200.0f).noOcclusion().isSuffocating((s, l, p) -> false).requiresCorrectToolForDrops()));
	public static final Item REINFORCED_TINTED_GLASS_ITEM = registerItem("reinforced_tinted_glass",
		new BlockItem(REINFORCED_TINTED_GLASS_BLOCK, new Item.Properties().fireResistant()));

	public static final Block THANATOS_EXECUTIONER_BLOCK = registerBlock("thanatos_executioner",
		new net.valentin.obsidium.block.ThanatosExecutionerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(5.0f, 1200.0f).requiresCorrectToolForDrops()));
	public static final Item THANATOS_EXECUTIONER_ITEM = registerItem("thanatos_executioner",
		new BlockItem(THANATOS_EXECUTIONER_BLOCK, new Item.Properties().fireResistant()));

	public static final Block VOLCANITE_STORAGE_UNIT_BLOCK = registerBlock("volcanite_storage_unit", 
		new net.valentin.obsidium.block.VolcaniteStorageUnitBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(5.0f, 1200.0f).requiresCorrectToolForDrops()));
	public static final Item VOLCANITE_STORAGE_UNIT_ITEM = registerItem("volcanite_storage_unit", 
		new BlockItem(VOLCANITE_STORAGE_UNIT_BLOCK, new Item.Properties().fireResistant()));

	public static final Block VOLCANITE_STORAGE_CONTROLLER_BLOCK = registerBlock("volcanite_storage_controller",
		new net.valentin.obsidium.block.VolcaniteStorageControllerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(5.0f, 1200.0f).requiresCorrectToolForDrops()));
	public static final Item VOLCANITE_STORAGE_CONTROLLER_ITEM = registerItem("volcanite_storage_controller",
		new BlockItem(VOLCANITE_STORAGE_CONTROLLER_BLOCK, new Item.Properties().fireResistant()));

	public static final Block VOLCANITE_STORAGE_INTERFACE_BLOCK = registerBlock("volcanite_storage_interface",
		new net.valentin.obsidium.block.VolcaniteStorageInterfaceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(5.0f, 1200.0f).requiresCorrectToolForDrops()));
	public static final Item VOLCANITE_STORAGE_INTERFACE_ITEM = registerItem("volcanite_storage_interface",
		new BlockItem(VOLCANITE_STORAGE_INTERFACE_BLOCK, new Item.Properties().fireResistant()));

	public static final Block VOLCANITE_STORAGE_PIPE_BLOCK = registerBlock("volcanite_storage_pipe",
		new net.valentin.obsidium.block.VolcaniteStoragePipeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noOcclusion()));
	public static final Item VOLCANITE_STORAGE_PIPE_ITEM = registerItem("volcanite_storage_pipe",
		new BlockItem(VOLCANITE_STORAGE_PIPE_BLOCK, new Item.Properties()));

	public static final Block VOLCANITE_NETWORK_NODE_EMITTER_BLOCK = registerBlock("volcanite_network_node_emitter",
		new net.valentin.obsidium.block.VolcaniteNetworkNodeEmitterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(4.0f, 1200.0f).requiresCorrectToolForDrops().noOcclusion()));
	public static final Item VOLCANITE_NETWORK_NODE_EMITTER_ITEM = registerItem("volcanite_network_node_emitter",
		new BlockItem(VOLCANITE_NETWORK_NODE_EMITTER_BLOCK, new Item.Properties().fireResistant()));

	public static final Block VOLCANITE_NETWORK_NODE_RECEIVER_BLOCK = registerBlock("volcanite_network_node_receiver",
		new net.valentin.obsidium.block.VolcaniteNetworkNodeReceiverBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(4.0f, 1200.0f).requiresCorrectToolForDrops().noOcclusion()));
	public static final Item VOLCANITE_NETWORK_NODE_RECEIVER_ITEM = registerItem("volcanite_network_node_receiver",
		new BlockItem(VOLCANITE_NETWORK_NODE_RECEIVER_BLOCK, new Item.Properties().fireResistant()));

	public static final Block VOLCANITE_STORAGE_DISK_DRIVE_BLOCK = registerBlock("volcanite_storage_disk_drive",
		new net.valentin.obsidium.block.VolcaniteStorageDiskDriveBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(5.0f, 1200.0f).requiresCorrectToolForDrops()));
	public static final Item VOLCANITE_STORAGE_DISK_DRIVE_ITEM = registerItem("volcanite_storage_disk_drive",
		new BlockItem(VOLCANITE_STORAGE_DISK_DRIVE_BLOCK, new Item.Properties().fireResistant()));

	public static final Item VOLCANITE_WRENCH = registerItem("volcanite_wrench",
		new net.valentin.obsidium.item.VolcaniteWrenchItem(new Item.Properties().stacksTo(1).fireResistant()));
	public static final Item DEMETRIAN_GRAFTING_KNIFE = registerItem("demetrian_grafting_knife",
		new net.valentin.obsidium.item.DemetrianGraftingKnifeItem(new Item.Properties()));
	public static final Item DEMETRIAN_SCION = registerItem("demetrian_scion",
		new net.valentin.obsidium.item.DemetrianScionItem(new Item.Properties()));
	public static final Item DEMETRIAN_GRAFTING_TAPE = registerItem("demetrian_grafting_tape",
		new net.valentin.obsidium.item.DemetrianGraftingTapeItem(new Item.Properties()));
	public static final Item DEMETRIAN_BOTANIST_LENS = registerItem("demetrian_botanist_lens",
		new net.valentin.obsidium.item.DemetrianBotanistLensItem(new Item.Properties()));
	public static final Item DEMETRIAN_BOTANY_BOOK = registerItem("demetrian_botany_book",
		new net.valentin.obsidium.item.DemetrianBotanyBookItem(new Item.Properties()));
	public static final Item VOLCANIC_HONEYCOMB = registerItem("volcanic_honeycomb",
		new net.valentin.obsidium.item.VolcanicHoneycombItem(new Item.Properties().fireResistant()));
	public static final Item VOLCANIC_HONEY_BOTTLE = registerItem("volcanic_honey_bottle",
		new net.valentin.obsidium.item.VolcanicHoneyBottleItem(new Item.Properties().fireResistant()));
	public static final Item POLLINATING_BRUSH = registerItem("pollinating_brush",
		new net.valentin.obsidium.item.PollinatingBrushItem(new Item.Properties().fireResistant()));
	public static final Item VOLCANITE_LOGISTICS_MODULE = registerItem("volcanite_logistics_module",
		new net.valentin.obsidium.item.VolcaniteLogisticsModuleItem(new Item.Properties()));
	public static final Item VOLCANITE_IMPORT_CONNECTOR = registerItem("volcanite_import_connector",
		new net.valentin.obsidium.item.VolcaniteCableConnectorItem(new Item.Properties().fireResistant(), false));
	public static final Item VOLCANITE_EXPORT_CONNECTOR = registerItem("volcanite_export_connector",
		new net.valentin.obsidium.item.VolcaniteCableConnectorItem(new Item.Properties().fireResistant(), true));
	public static final Item VOLCANITE_BLUEPRINT = registerItem("volcanite_blueprint",
		new net.valentin.obsidium.item.VolcaniteBlueprintItem(new Item.Properties().stacksTo(1).fireResistant()));

	public static final Block VOLCANITE_CRAFTER_INTERFACE_BLOCK = registerBlock("volcanite_crafter_interface",
		new net.valentin.obsidium.block.VolcaniteCrafterInterfaceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(5.0f, 1200.0f).requiresCorrectToolForDrops()));
	public static final Item VOLCANITE_CRAFTER_INTERFACE_ITEM = registerItem("volcanite_crafter_interface",
		new BlockItem(VOLCANITE_CRAFTER_INTERFACE_BLOCK, new Item.Properties().fireResistant()));

	public static final Block VOLCANITE_SORTING_STATION_BLOCK = registerBlock("volcanite_sorting_station",
		new net.valentin.obsidium.block.VolcaniteSortingStationBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(5.0f, 1200.0f).requiresCorrectToolForDrops().noOcclusion()));
	public static final Item VOLCANITE_SORTING_STATION_ITEM = registerItem("volcanite_sorting_station",
		new BlockItem(VOLCANITE_SORTING_STATION_BLOCK, new Item.Properties().fireResistant()));

	public static final Item VOLCANITE_STORAGE_UPGRADE = registerItem("volcanite_storage_upgrade",
		new net.valentin.obsidium.item.VolcaniteStorageUpgradeItem(new Item.Properties().stacksTo(16).fireResistant()));
	public static final Item VOLCANITE_LIQUID_UPGRADE = registerItem("volcanite_liquid_upgrade",
		new net.valentin.obsidium.item.VolcaniteLiquidUpgradeItem(new Item.Properties().stacksTo(16).fireResistant()));
	public static final Item VOLCANITE_ADVANCED_FILTER_MODULE = registerItem("volcanite_advanced_filter_module",
		new net.valentin.obsidium.item.VolcaniteAdvancedFilterModuleItem(new Item.Properties().stacksTo(16).fireResistant()));
	public static final Item HERMES_FILTER_UPGRADE = registerItem("hermes_filter_upgrade",
		new net.valentin.obsidium.item.HermesFilterUpgradeItem(new Item.Properties().stacksTo(16).fireResistant()));
	public static final Item HERMES_FILTER_BLUEPRINT = registerItem("hermes_filter_blueprint",
		new net.valentin.obsidium.item.HermesFilterBlueprintItem(new Item.Properties().stacksTo(16)));
	public static final Item VOLCANITE_BASIC_PIPE_ENERGY_UPGRADE = registerItem("volcanite_basic_pipe_energy_upgrade",
		new net.valentin.obsidium.item.VolcanitePipeEnergyUpgradeItem(new Item.Properties().stacksTo(16), 1, 500));
	public static final Item VOLCANITE_ADVANCED_PIPE_ENERGY_UPGRADE = registerItem("volcanite_advanced_pipe_energy_upgrade",
		new net.valentin.obsidium.item.VolcanitePipeEnergyUpgradeItem(new Item.Properties().stacksTo(16), 2, 5000));
	public static final Item VOLCANITE_SUPREME_PIPE_ENERGY_UPGRADE = registerItem("volcanite_supreme_pipe_energy_upgrade",
		new net.valentin.obsidium.item.VolcanitePipeEnergyUpgradeItem(new Item.Properties().stacksTo(16), 3, 50000));
	public static final Item VOLCANITE_STACK_UPGRADE = registerItem("volcanite_stack_upgrade",
		new net.valentin.obsidium.item.VolcaniteStackUpgradeItem(new Item.Properties().stacksTo(16).fireResistant()));
	public static final Item VOLCANITE_VACUUM_UPGRADE = registerItem("volcanite_vacuum_upgrade",
		new net.valentin.obsidium.item.VolcaniteVacuumUpgradeItem(new Item.Properties().stacksTo(16).fireResistant()));
	public static final Item VOLCANITE_COMPACTOR_UPGRADE = registerItem("volcanite_compactor_upgrade",
		new net.valentin.obsidium.item.VolcaniteCompactorUpgradeItem(new Item.Properties().stacksTo(16).fireResistant()));
	public static final Item VOLCANITE_FILTERED_COMPACTOR_UPGRADE = registerItem("volcanite_filtered_compactor_upgrade",
		new net.valentin.obsidium.item.VolcaniteFilteredCompactorUpgradeItem(new Item.Properties().stacksTo(16).fireResistant()));
	public static final Item VOLCANITE_VOID_UPGRADE = registerItem("volcanite_void_upgrade",
		new net.valentin.obsidium.item.VolcaniteVoidUpgradeItem(new Item.Properties().stacksTo(16).fireResistant()));
	public static final Item VOLCANITE_FILTERED_VOID_UPGRADE = registerItem("volcanite_filtered_void_upgrade",
		new net.valentin.obsidium.item.VolcaniteFilteredVoidUpgradeItem(new Item.Properties().stacksTo(16).fireResistant()));
	public static final Item QUANTUM_LINK_MODULE = registerItem("quantum_link_module",
		new net.valentin.obsidium.item.QuantumLinkModuleItem(new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.RARE).fireResistant()));

	public static final Item VOLCANITE_OVERCLOCK_UPGRADE = registerItem("volcanite_overclock_upgrade",
		new net.valentin.obsidium.item.VolcaniteOverclockUpgradeItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), 1, 2.0f, 1.75f));
	public static final Item ADVANCED_VOLCANITE_OVERCLOCK_UPGRADE = registerItem("advanced_volcanite_overclock_upgrade",
		new net.valentin.obsidium.item.VolcaniteOverclockUpgradeItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE), 2, 3.5f, 2.5f));
	public static final Item SUPREME_VOLCANITE_OVERCLOCK_UPGRADE = registerItem("supreme_volcanite_overclock_upgrade",
		new net.valentin.obsidium.item.VolcaniteOverclockUpgradeItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC), 3, 6.0f, 4.0f));

	// Volcanite Single-Item Barrel Upgrades
	public static final Item VOLCANITE_BASIC_BARREL_UPGRADE = registerItem("volcanite_basic_barrel_upgrade",
		new VolcaniteBarrelUpgradeItem(new Item.Properties(), VolcaniteBarrelUpgradeItem.UpgradeType.BASIC));
	public static final Item VOLCANITE_ADVANCED_BARREL_UPGRADE = registerItem("volcanite_advanced_barrel_upgrade",
		new VolcaniteBarrelUpgradeItem(new Item.Properties(), VolcaniteBarrelUpgradeItem.UpgradeType.ADVANCED));
	public static final Item VOLCANITE_SUPREME_BARREL_UPGRADE = registerItem("volcanite_supreme_barrel_upgrade",
		new VolcaniteBarrelUpgradeItem(new Item.Properties(), VolcaniteBarrelUpgradeItem.UpgradeType.SUPREME));
	public static final Item VOLCANITE_BARREL_VOID_UPGRADE = registerItem("volcanite_barrel_void_upgrade",
		new VolcaniteBarrelUpgradeItem(new Item.Properties(), VolcaniteBarrelUpgradeItem.UpgradeType.VOID));

	// Universal Pipe Concealment Facade
	public static final Item PIPE_FACADE = registerItem("pipe_facade",
		new PipeFacadeItem(new Item.Properties()));

	public static final Item VOLCANITE_STORAGE_PROCESSOR = registerItem("volcanite_storage_processor",
		new Item(new Item.Properties().fireResistant()));
	public static final Item VOLCANITE_STORAGE_CAPACITOR = registerItem("volcanite_storage_capacitor",
		new Item(new Item.Properties().fireResistant()));
	public static final Item VOLCANITE_STORAGE_DISK_32 = registerItem("volcanite_storage_disk_32",
		new net.valentin.obsidium.item.VolcaniteStorageDiskItem(new Item.Properties().stacksTo(1).fireResistant(), 32000));
	public static final Item VOLCANITE_STORAGE_DISK_64 = registerItem("volcanite_storage_disk_64",
		new net.valentin.obsidium.item.VolcaniteStorageDiskItem(new Item.Properties().stacksTo(1).fireResistant(), 64000));
	public static final Item VOLCANITE_STORAGE_DISK_128 = registerItem("volcanite_storage_disk_128",
		new net.valentin.obsidium.item.VolcaniteStorageDiskItem(new Item.Properties().stacksTo(1).fireResistant(), 128000));
	public static final Item VOLCANITE_STORAGE_DISK_256 = registerItem("volcanite_storage_disk_256",
		new net.valentin.obsidium.item.VolcaniteStorageDiskItem(new Item.Properties().stacksTo(1).fireResistant(), 256000));
	public static final Item VOLCANITE_STORAGE_REMOTE = registerItem("volcanite_storage_remote",
		new net.valentin.obsidium.item.VolcaniteStorageRemoteItem(new Item.Properties().stacksTo(1).fireResistant()));

	public static final Block VOLCANITE_LIQUID_STORAGE_TANK_BLOCK = registerBlock("volcanite_liquid_storage_tank",
		new net.valentin.obsidium.block.VolcaniteLiquidStorageTankBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().strength(4.0f, 6.0f).requiresCorrectToolForDrops()));
	public static final Item VOLCANITE_LIQUID_STORAGE_TANK_ITEM = registerItem("volcanite_liquid_storage_tank",
		new BlockItem(VOLCANITE_LIQUID_STORAGE_TANK_BLOCK, new Item.Properties().fireResistant()));

	public static final Item VOLCANITE_LIQUID_STORAGE_DISK_32 = registerItem("volcanite_liquid_storage_disk_32",
		new net.valentin.obsidium.item.VolcaniteLiquidStorageDiskItem(new Item.Properties().stacksTo(1).fireResistant(), 32000));
	public static final Item VOLCANITE_LIQUID_STORAGE_DISK_64 = registerItem("volcanite_liquid_storage_disk_64",
		new net.valentin.obsidium.item.VolcaniteLiquidStorageDiskItem(new Item.Properties().stacksTo(1).fireResistant(), 64000));
	public static final Item VOLCANITE_LIQUID_STORAGE_DISK_128 = registerItem("volcanite_liquid_storage_disk_128",
		new net.valentin.obsidium.item.VolcaniteLiquidStorageDiskItem(new Item.Properties().stacksTo(1).fireResistant(), 128000));
	public static final Item VOLCANITE_LIQUID_STORAGE_DISK_256 = registerItem("volcanite_liquid_storage_disk_256",
		new net.valentin.obsidium.item.VolcaniteLiquidStorageDiskItem(new Item.Properties().stacksTo(1).fireResistant(), 256000));

	public static final Block SUPREME_LIQUID_TANK_BLOCK = registerBlock("supreme_liquid_tank", 
		new net.valentin.obsidium.block.SupremeLiquidTankBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noOcclusion().isViewBlocking((s,l,p) -> false)));
	public static final Item SUPREME_LIQUID_TANK_ITEM = registerItem("supreme_liquid_tank", 
		new BlockItem(SUPREME_LIQUID_TANK_BLOCK, new Item.Properties()));
		
	public static final Block CREATIVE_LAVA_TANK = registerBlock("creative_lava_tank", new CreativeLavaTankBlock(BlockBehaviour.Properties.of().strength(5.0f, 1200.0f).requiresCorrectToolForDrops().noOcclusion().isViewBlocking((s,l,p) -> false)));
    public static final Block CREATIVE_ENERGY_CORE = registerBlock("creative_energy_core", new CreativeEnergyCoreBlock(BlockBehaviour.Properties.of().strength(5.0f, 1200.0f).requiresCorrectToolForDrops()));
	public static final Item CREATIVE_LAVA_TANK_ITEM = registerItem("creative_lava_tank", new BlockItem(CREATIVE_LAVA_TANK, new Item.Properties().fireResistant()));
    public static final Item CREATIVE_ENERGY_CORE_ITEM = registerItem("creative_energy_core", new BlockItem(CREATIVE_ENERGY_CORE, new Item.Properties().fireResistant()));

	public static final Block CREATIVE_WATER_TANK = registerBlock("creative_water_tank", new net.valentin.obsidium.block.CreativeWaterTankBlock(BlockBehaviour.Properties.of().strength(5.0f, 1200.0f).requiresCorrectToolForDrops().noOcclusion().isViewBlocking((s,l,p) -> false)));
	public static final Item CREATIVE_WATER_TANK_ITEM = registerItem("creative_water_tank", new BlockItem(CREATIVE_WATER_TANK, new Item.Properties().fireResistant()));

	public static final Block VOLCANITE_CHUNK_LOADER = registerBlock("volcanite_chunk_loader", new net.valentin.obsidium.block.VolcaniteChunkLoaderBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(5.0f, 1200.0f).requiresCorrectToolForDrops()));
	public static final Item VOLCANITE_CHUNK_LOADER_ITEM = registerItem("volcanite_chunk_loader", new BlockItem(VOLCANITE_CHUNK_LOADER, new Item.Properties().fireResistant()));

	public static final Block AEGIS_SANCTUARY_WARD = registerBlock("aegis_sanctuary_ward", new net.valentin.obsidium.block.AegisSanctuaryWardBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(4.5f, 1200.0f).requiresCorrectToolForDrops().lightLevel(s -> s.getValue(net.valentin.obsidium.block.AegisSanctuaryWardBlock.LIT) ? 10 : 0)));
	public static final Item AEGIS_SANCTUARY_WARD_ITEM = registerItem("aegis_sanctuary_ward", new BlockItem(AEGIS_SANCTUARY_WARD, new Item.Properties().fireResistant()));

	public static final Block VOLCANITE_GENERATOR_BLOCK = registerBlock("volcanite_generator", 
		new net.valentin.obsidium.block.VolcaniteGeneratorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(5.0f, 1200.0f).requiresCorrectToolForDrops()));
	public static final Item VOLCANITE_GENERATOR_ITEM = registerItem("volcanite_generator", 
		new BlockItem(VOLCANITE_GENERATOR_BLOCK, new Item.Properties()));

	public static final Block VOLCANITE_GRINDER_BLOCK = registerBlock("volcanite_grinder", 
		new net.valentin.obsidium.block.VolcaniteGrinderBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(5.0f, 1200.0f).requiresCorrectToolForDrops()));
	public static final Item VOLCANITE_GRINDER_ITEM = registerItem("volcanite_grinder", 
		new BlockItem(VOLCANITE_GRINDER_BLOCK, new Item.Properties()));

	public static final Block ADVANCED_VOLCANITE_FURNACE_BLOCK = registerBlock("advanced_volcanite_furnace", 
		new net.valentin.obsidium.block.AdvancedVolcaniteFurnaceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(5.0f, 1200.0f).requiresCorrectToolForDrops()));
	public static final Item ADVANCED_VOLCANITE_FURNACE_ITEM = registerItem("advanced_volcanite_furnace", 
		new BlockItem(ADVANCED_VOLCANITE_FURNACE_BLOCK, new Item.Properties()));

	public static final Item VOLCANITE_WARP_GAUNTLET = registerItem("volcanite_warp_gauntlet", 
		new net.valentin.obsidium.item.VolcaniteWarpGauntletItem(new Item.Properties().stacksTo(1).fireResistant()));

	public static final Item VOLCANITE_BOTTLE = registerItem("volcanite_bottle", new net.valentin.obsidium.item.VolcaniteBottleItem(new Item.Properties().stacksTo(1), 5000));
	public static final Item ADVANCED_VOLCANITE_BOTTLE = registerItem("advanced_volcanite_bottle", new net.valentin.obsidium.item.AdvancedVolcaniteBottleItem(new Item.Properties().stacksTo(1), 50000));

	public static final Block VOLCANITE_ENERGY_WIRE_BLOCK = registerBlock("volcanite_energy_wire", 
		new net.valentin.obsidium.block.VolcaniteEnergyWireBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noOcclusion(), 1, 25_000L, 100_000L));
	public static final Item VOLCANITE_ENERGY_WIRE_ITEM = registerItem("volcanite_energy_wire", 
		new BlockItem(VOLCANITE_ENERGY_WIRE_BLOCK, new Item.Properties()));

	public static final Block ADVANCED_VOLCANITE_ENERGY_WIRE_BLOCK = registerBlock("advanced_volcanite_energy_wire", 
		new net.valentin.obsidium.block.VolcaniteEnergyWireBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noOcclusion(), 2, 250_000L, 1_000_000L));
	public static final Item ADVANCED_VOLCANITE_ENERGY_WIRE_ITEM = registerItem("advanced_volcanite_energy_wire", 
		new BlockItem(ADVANCED_VOLCANITE_ENERGY_WIRE_BLOCK, new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

	public static final Block SUPREME_VOLCANITE_ENERGY_WIRE_BLOCK = registerBlock("supreme_volcanite_energy_wire", 
		new net.valentin.obsidium.block.VolcaniteEnergyWireBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noOcclusion(), 3, 2_500_000L, 10_000_000L));
	public static final Item SUPREME_VOLCANITE_ENERGY_WIRE_ITEM = registerItem("supreme_volcanite_energy_wire", 
		new BlockItem(SUPREME_VOLCANITE_ENERGY_WIRE_BLOCK, new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

	public static final Block SINGULARITY_VOLCANITE_ENERGY_WIRE_BLOCK = registerBlock("singularity_volcanite_energy_wire", 
		new net.valentin.obsidium.block.VolcaniteEnergyWireBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noOcclusion(), 4, 25_000_000L, 100_000_000L));
	public static final Item SINGULARITY_VOLCANITE_ENERGY_WIRE_ITEM = registerItem("singularity_volcanite_energy_wire", 
		new BlockItem(SINGULARITY_VOLCANITE_ENERGY_WIRE_BLOCK, new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));

	public static final Block HEPHAESTEAN_CHARGING_PLATE_BLOCK = registerBlock("hephaestean_charging_plate",
		new net.valentin.obsidium.block.HephaesteanChargingPlateBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
	public static final Item HEPHAESTEAN_CHARGING_PLATE_ITEM = registerItem("hephaestean_charging_plate",
		new BlockItem(HEPHAESTEAN_CHARGING_PLATE_BLOCK, new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

	public static final Block ASTRAPE_ENERGY_PYLON_BLOCK = registerBlock("astrape_energy_pylon",
		new net.valentin.obsidium.block.AstrapeEnergyPylonBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
	public static final Item ASTRAPE_ENERGY_PYLON_ITEM = registerItem("astrape_energy_pylon",
		new BlockItem(ASTRAPE_ENERGY_PYLON_BLOCK, new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

	public static final Block OLYMPIAN_ALTAR_BLOCK = registerBlock("olympian_altar",
		new net.valentin.obsidium.block.OlympianAltarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).noOcclusion()));
	public static final Item OLYMPIAN_ALTAR_ITEM = registerItem("olympian_altar",
		new BlockItem(OLYMPIAN_ALTAR_BLOCK, new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).fireResistant()));

	public static final Block ASTRAPE_RITUAL_OBELISK_BLOCK = registerBlock("astrape_ritual_obelisk",
		new net.valentin.obsidium.block.AstrapeRitualObeliskBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).noOcclusion()));
	public static final Item ASTRAPE_RITUAL_OBELISK_ITEM = registerItem("astrape_ritual_obelisk",
		new BlockItem(ASTRAPE_RITUAL_OBELISK_BLOCK, new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON).fireResistant()));
		
	public static final Block MAGMA_FORGE_CORE_BLOCK = registerBlock("magma_forge_core", 
		new net.valentin.obsidium.block.MagmaForgeCoreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN)));
	public static final Item MAGMA_FORGE_CORE_ITEM = registerItem("magma_forge_core", 
		new BlockItem(MAGMA_FORGE_CORE_BLOCK, new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));
	
	public static final Block MAGMA_FORGE_DUMMY_BLOCK = registerBlock("magma_forge_dummy",
		new net.valentin.obsidium.block.MagmaForgeDummyBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noOcclusion().noLootTable()));
		
	public static final Block VOLCANITE_LAVA_PUMP_BLOCK = registerBlock("volcanite_lava_pump", 
		new net.valentin.obsidium.block.VolcaniteLavaPumpBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));
	public static final Item VOLCANITE_LAVA_PUMP_ITEM = registerItem("volcanite_lava_pump", 
		new BlockItem(VOLCANITE_LAVA_PUMP_BLOCK, new Item.Properties()));
		
	public static final Block VOLCANITE_UNIVERSAL_CONDUIT_BLOCK = registerBlock("volcanite_universal_conduit", 
		new net.valentin.obsidium.block.VolcaniteUniversalConduitBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noOcclusion()));
	public static final Item VOLCANITE_UNIVERSAL_CONDUIT_ITEM = registerItem("volcanite_universal_conduit", 
		new BlockItem(VOLCANITE_UNIVERSAL_CONDUIT_BLOCK, new Item.Properties()));

	public static final Block NAIAD_FLUID_PIPE_BLOCK = registerBlock("naiad_fluid_pipe", 
		new net.valentin.obsidium.block.NaiadFluidPipeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noOcclusion(), 1, 250L));
	public static final Item NAIAD_FLUID_PIPE_ITEM = registerItem("naiad_fluid_pipe", 
		new BlockItem(NAIAD_FLUID_PIPE_BLOCK, new Item.Properties()));

	public static final Block ADVANCED_NAIAD_FLUID_PIPE_BLOCK = registerBlock("advanced_naiad_fluid_pipe",
		new net.valentin.obsidium.block.NaiadFluidPipeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noOcclusion(), 2, 1_000L));
	public static final Item ADVANCED_NAIAD_FLUID_PIPE_ITEM = registerItem("advanced_naiad_fluid_pipe",
		new BlockItem(ADVANCED_NAIAD_FLUID_PIPE_BLOCK, new Item.Properties()));

	public static final Block SUPREME_NAIAD_FLUID_PIPE_BLOCK = registerBlock("supreme_naiad_fluid_pipe",
		new net.valentin.obsidium.block.NaiadFluidPipeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noOcclusion(), 3, 4_000L));
	public static final Item SUPREME_NAIAD_FLUID_PIPE_ITEM = registerItem("supreme_naiad_fluid_pipe",
		new BlockItem(SUPREME_NAIAD_FLUID_PIPE_BLOCK, new Item.Properties()));

	public static final Block HERMES_ITEM_PIPE_BLOCK = registerBlock("hermes_item_pipe", 
		new net.valentin.obsidium.block.HermesItemPipeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noOcclusion(), 1, 8, 5));
	public static final Item HERMES_ITEM_PIPE_ITEM = registerItem("hermes_item_pipe", 
		new BlockItem(HERMES_ITEM_PIPE_BLOCK, new Item.Properties()));

	public static final Block ADVANCED_HERMES_ITEM_PIPE_BLOCK = registerBlock("advanced_hermes_item_pipe",
		new net.valentin.obsidium.block.HermesItemPipeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noOcclusion(), 2, 16, 3));
	public static final Item ADVANCED_HERMES_ITEM_PIPE_ITEM = registerItem("advanced_hermes_item_pipe",
		new BlockItem(ADVANCED_HERMES_ITEM_PIPE_BLOCK, new Item.Properties()));

	public static final Block SUPREME_HERMES_ITEM_PIPE_BLOCK = registerBlock("supreme_hermes_item_pipe",
		new net.valentin.obsidium.block.HermesItemPipeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noOcclusion(), 3, 64, 1));
	public static final Item SUPREME_HERMES_ITEM_PIPE_ITEM = registerItem("supreme_hermes_item_pipe",
		new BlockItem(SUPREME_HERMES_ITEM_PIPE_BLOCK, new Item.Properties()));
		
	public static final Block VOLCANITE_ENERGY_CELL_BLOCK = registerBlock("volcanite_energy_cell", 
		new net.valentin.obsidium.block.VolcaniteEnergyCellBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));
	public static final Item VOLCANITE_ENERGY_CELL_ITEM = registerItem("volcanite_energy_cell", 
		new net.valentin.obsidium.item.VolcaniteEnergyCellBlockItem(VOLCANITE_ENERGY_CELL_BLOCK, new Item.Properties(), 2_000_000L, 25_000L, false));
		
	public static final Block VOLCANITE_ENERGY_UNIT_BLOCK = registerBlock("volcanite_energy_unit", 
		new net.valentin.obsidium.block.VolcaniteEnergyUnitBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));
	public static final Item VOLCANITE_ENERGY_UNIT_ITEM = registerItem("volcanite_energy_unit", 
		new net.valentin.obsidium.item.VolcaniteEnergyCellBlockItem(VOLCANITE_ENERGY_UNIT_BLOCK, new Item.Properties(), 15_000_000L, 150_000L, true));
		
	public static final Block VOLCANITE_ENERGY_CORE_BLOCK = registerBlock("volcanite_energy_core", 
		new net.valentin.obsidium.block.VolcaniteEnergyCoreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN)));
	public static final Item VOLCANITE_ENERGY_CORE_ITEM = registerItem("volcanite_energy_core", 
		new BlockItem(VOLCANITE_ENERGY_CORE_BLOCK, new Item.Properties()));

	public static final Block VOLCANITE_LAMP = registerBlock("volcanite_lamp", 
			new net.valentin.obsidium.block.VolcaniteLampBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLOWSTONE).lightLevel(state -> state.getValue(net.valentin.obsidium.block.VolcaniteLampBlock.LIT) ? 15 : 0)));
	public static final Item VOLCANITE_LAMP_ITEM = registerItem("volcanite_lamp", 
			new BlockItem(VOLCANITE_LAMP, new Item.Properties()));

	public static final Block GEOTHERMAL_ARBORIST_BLOCK = registerBlock("geothermal_arborist",
			new GeothermalArboristBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(4.5f, 1200.0f).requiresCorrectToolForDrops().lightLevel(s -> s.getValue(GeothermalArboristBlock.WORKING) ? 12 : 6)));
	public static final Item GEOTHERMAL_ARBORIST_ITEM = registerItem("geothermal_arborist",
			new BlockItem(GEOTHERMAL_ARBORIST_BLOCK, new Item.Properties().fireResistant()));

	public static final Block VOLCANITE_STEAM_GENERATOR_BLOCK = registerBlock("volcanite_steam_generator",
			new VolcaniteSteamGeneratorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(4.5f, 1200.0f).requiresCorrectToolForDrops()));
	public static final Item VOLCANITE_STEAM_GENERATOR_ITEM = registerItem("volcanite_steam_generator",
			new BlockItem(VOLCANITE_STEAM_GENERATOR_BLOCK, new Item.Properties().fireResistant()));

	public static final Block VOLCANITE_GEOTHERMAL_GENERATOR_BLOCK = registerBlock("volcanite_geothermal_generator",
			new VolcaniteGeothermalGeneratorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(4.5f, 1200.0f).requiresCorrectToolForDrops()));
	public static final Item VOLCANITE_GEOTHERMAL_GENERATOR_ITEM = registerItem("volcanite_geothermal_generator",
			new BlockItem(VOLCANITE_GEOTHERMAL_GENERATOR_BLOCK, new Item.Properties().fireResistant()));

	public static final Block SUPREME_VOLCANIC_GENERATOR_BLOCK = registerBlock("supreme_volcanic_generator",
			new SupremeVolcanicGeneratorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(5.5f, 1500.0f).requiresCorrectToolForDrops().lightLevel(s -> s.getValue(SupremeVolcanicGeneratorBlock.WORKING) ? 15 : 7)));
	public static final Item SUPREME_VOLCANIC_GENERATOR_ITEM = registerItem("supreme_volcanic_generator",
			new BlockItem(SUPREME_VOLCANIC_GENERATOR_BLOCK, new Item.Properties().fireResistant()));

	public static final Block VOLCANIC_DISTILLATION_CHAMBER_BLOCK = registerBlock("volcanic_distillation_chamber",
			new VolcanicDistillationChamberBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(5.0f, 1200.0f).requiresCorrectToolForDrops().lightLevel(s -> s.getValue(VolcanicDistillationChamberBlock.WORKING) ? 13 : 5)));
	public static final Item VOLCANIC_DISTILLATION_CHAMBER_ITEM = registerItem("volcanic_distillation_chamber",
			new BlockItem(VOLCANIC_DISTILLATION_CHAMBER_BLOCK, new Item.Properties().fireResistant()));

	public static final Block HYDROTHERMAL_VITRIFIER_BLOCK = registerBlock("hydrothermal_vitrifier",
			new HydrothermalVitrifierBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(5.0f, 1200.0f).requiresCorrectToolForDrops().lightLevel(s -> s.getValue(HydrothermalVitrifierBlock.LIT) ? 12 : 0)));
	public static final Item HYDROTHERMAL_VITRIFIER_ITEM = registerItem("hydrothermal_vitrifier",
			new BlockItem(HYDROTHERMAL_VITRIFIER_BLOCK, new Item.Properties()));

	public static final Item MAGMA_DROPLET = registerItem("magma_droplet",
			new Item(new Item.Properties().fireResistant()));
	public static final Block MAGMATIC_CRUCIBLE_BLOCK = registerBlock("magmatic_crucible",
			new MagmaticCrucibleBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(5.0f, 1200.0f).requiresCorrectToolForDrops().lightLevel(s -> s.getValue(MagmaticCrucibleBlock.LIT) ? 12 : 0)));
	public static final Item MAGMATIC_CRUCIBLE_ITEM = registerItem("magmatic_crucible",
			new BlockItem(MAGMATIC_CRUCIBLE_BLOCK, new Item.Properties()));

	public static final Item DEW_DROPLET = registerItem("dew_droplet",
			new Item(new Item.Properties()));
	public static final Block HYDROTHERMAL_CONDENSER_BLOCK = registerBlock("hydrothermal_condenser",
			new HydrothermalCondenserBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(5.0f, 1200.0f).requiresCorrectToolForDrops().lightLevel(s -> s.getValue(HydrothermalCondenserBlock.LIT) ? 12 : 0)));
	public static final Item HYDROTHERMAL_CONDENSER_ITEM = registerItem("hydrothermal_condenser",
			new BlockItem(HYDROTHERMAL_CONDENSER_BLOCK, new Item.Properties()));

	public static final Block HYDROTHERMAL_LITHOFIER_BLOCK = registerBlock("hydrothermal_lithofier",
			new HydrothermalLithofierBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(5.0f, 1200.0f).requiresCorrectToolForDrops().lightLevel(s -> s.getValue(HydrothermalLithofierBlock.LIT) ? 12 : 0)));
	public static final Item HYDROTHERMAL_LITHOFIER_ITEM = registerItem("hydrothermal_lithofier",
			new BlockItem(HYDROTHERMAL_LITHOFIER_BLOCK, new Item.Properties()));

	public static final Block DIONYSIAN_DISTILLERY_BLOCK = registerBlock("dionysian_distillery",
			new net.valentin.obsidium.block.DionysianDistilleryBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(4.5f, 1200.0f).requiresCorrectToolForDrops().lightLevel(s -> s.getValue(net.valentin.obsidium.block.DionysianDistilleryBlock.LIT) ? 12 : 4)));
	public static final Item DIONYSIAN_DISTILLERY_ITEM = registerItem("dionysian_distillery",
			new BlockItem(DIONYSIAN_DISTILLERY_BLOCK, new Item.Properties().fireResistant()));

	public static final Item VOLCANIC_TUICA = registerItem("volcanic_tuica",
			new net.valentin.obsidium.item.VolcanicTuicaItem(new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.RARE).fireResistant()));

	public static final Item VOLCANIC_CIDER = registerItem("volcanic_cider",
			new net.valentin.obsidium.item.VolcanicCiderItem(new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.RARE).fireResistant()));

	public static final Block DIONYSIAN_PRESS_BLOCK = registerBlock("dionysian_press",
			new net.valentin.obsidium.block.DionysianPressBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(4.5f, 1200.0f).requiresCorrectToolForDrops().lightLevel(s -> s.getValue(net.valentin.obsidium.block.DionysianPressBlock.LIT) ? 12 : 2)));
	public static final Item DIONYSIAN_PRESS_ITEM = registerItem("dionysian_press",
			new BlockItem(DIONYSIAN_PRESS_BLOCK, new Item.Properties().fireResistant()));

	public static final Block ADVANCED_DIONYSIAN_PRESS_BLOCK = registerBlock("advanced_dionysian_press",
			new net.valentin.obsidium.block.AdvancedDionysianPressBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(5.5f, 1600.0f).requiresCorrectToolForDrops().lightLevel(s -> s.getValue(net.valentin.obsidium.block.AdvancedDionysianPressBlock.LIT) ? 14 : 3)));
	public static final Item ADVANCED_DIONYSIAN_PRESS_ITEM = registerItem("advanced_dionysian_press",
			new BlockItem(ADVANCED_DIONYSIAN_PRESS_BLOCK, new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.RARE)));

	public static final Item EMPTY_TUICA_BOTTLE_1L = registerItem("empty_tuica_bottle_1l",
			new Item(new Item.Properties().stacksTo(16)));

	public static final Item VOLCANIC_TUICA_1L = registerItem("volcanic_tuica_1l",
			new net.valentin.obsidium.item.VolcanicTuicaOneLiterItem(new Item.Properties().stacksTo(1).durability(4).rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));

	public static final Item DIONYSIAN_POMACE = registerItem("dionysian_pomace",
			new net.valentin.obsidium.item.DionysianPomaceItem(new Item.Properties().stacksTo(64)));

	public static final Block DEMETRIAN_BIOMASS_GENERATOR_BLOCK = registerBlock("demetrian_biomass_generator",
			new net.valentin.obsidium.block.DemetrianBiomassGeneratorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(4.5f, 1200.0f).requiresCorrectToolForDrops().lightLevel(s -> s.getValue(net.valentin.obsidium.block.DemetrianBiomassGeneratorBlock.LIT) ? 14 : 5)));
	public static final Item DEMETRIAN_BIOMASS_GENERATOR_ITEM = registerItem("demetrian_biomass_generator",
			new BlockItem(DEMETRIAN_BIOMASS_GENERATOR_BLOCK, new Item.Properties().fireResistant()));
	public static final Block DEMETRIAN_COMPOSTER_BLOCK = registerBlock("demetrian_composter",
			new net.valentin.obsidium.block.DemetrianComposterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).strength(2.0f, 3.0f)));
	public static final Item DEMETRIAN_COMPOSTER_ITEM = registerItem("demetrian_composter",
			new BlockItem(DEMETRIAN_COMPOSTER_BLOCK, new Item.Properties()));

	// Volcanic Ash Products & Building Blocks
	public static final Item ASH_BRIQUETTE = registerItem("ash_briquette",
			new Item(new Item.Properties().fireResistant()));
	public static final Item STICK_BUNDLE = registerItem("stick_bundle",
			new net.valentin.obsidium.item.StickBundleItem(new Item.Properties()));

	public static final Block VOLCANIC_CEMENT_BLOCK = registerBlock("volcanic_cement_block",
			new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(4.0f, 1200.0f).requiresCorrectToolForDrops().sound(SoundType.STONE)));
	public static final Item VOLCANIC_CEMENT_BLOCK_ITEM = registerItem("volcanic_cement_block",
			new BlockItem(VOLCANIC_CEMENT_BLOCK, new Item.Properties().fireResistant()));

	public static final Block VOLCANIC_CEMENT_BRICKS = registerBlock("volcanic_cement_bricks",
			new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(4.0f, 1200.0f).requiresCorrectToolForDrops().sound(SoundType.STONE)));
	public static final Item VOLCANIC_CEMENT_BRICKS_ITEM = registerItem("volcanic_cement_bricks",
			new BlockItem(VOLCANIC_CEMENT_BRICKS, new Item.Properties().fireResistant()));

	// Dionysian Aging Barrel & Aged Spirits
	public static final Block DIONYSIAN_AGING_BARREL_BLOCK = registerBlock("dionysian_aging_barrel",
			new net.valentin.obsidium.block.DionysianAgingBarrelBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).strength(2.5f).sound(SoundType.WOOD)));
	public static final Item DIONYSIAN_AGING_BARREL_ITEM = registerItem("dionysian_aging_barrel",
			new BlockItem(DIONYSIAN_AGING_BARREL_BLOCK, new Item.Properties()));

	// Demetrian Nursery Barrel (3-chamber high-capacity sapling cache)
	public static final Block DEMETRIAN_SAPLING_BARREL_BLOCK = registerBlock("demetrian_sapling_barrel",
			new net.valentin.obsidium.block.DemetrianSaplingBarrelBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).strength(2.5f).sound(SoundType.WOOD)));
	public static final Item DEMETRIAN_SAPLING_BARREL_ITEM = registerItem("demetrian_sapling_barrel",
			new BlockItem(DEMETRIAN_SAPLING_BARREL_BLOCK, new Item.Properties()));

	// Foliage Biomass Bale (9 Leaves -> 1 Compressed Bale, supercharged biogas fuel)
	public static final Block FOLIAGE_BALE_BLOCK = registerBlock("foliage_bale",
			new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.HAY_BLOCK).strength(0.5f).sound(SoundType.GRASS)));
	public static final Item FOLIAGE_BALE_ITEM = registerItem("foliage_bale",
			new BlockItem(FOLIAGE_BALE_BLOCK, new Item.Properties()));

	// Demetrian Timber Bundle (9 Logs -> 1 Timber Bundle)
	public static final Block TIMBER_BUNDLE_BLOCK = registerBlock("timber_bundle",
			new net.minecraft.world.level.block.RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).strength(2.0f).sound(SoundType.WOOD)));
	public static final Item TIMBER_BUNDLE_ITEM = registerItem("timber_bundle",
			new BlockItem(TIMBER_BUNDLE_BLOCK, new Item.Properties()));

	// Demetrian Lumber Depot (4-chamber massive log & timber warehouse, 4,096 logs per chamber = 16,384 total)
	public static final Block DEMETRIAN_LUMBER_DEPOT_BLOCK = registerBlock("demetrian_lumber_depot",
			new net.valentin.obsidium.block.DemetrianLumberDepotBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).strength(2.5f).sound(SoundType.WOOD)));
	public static final Item DEMETRIAN_LUMBER_DEPOT_ITEM = registerItem("demetrian_lumber_depot",
			new BlockItem(DEMETRIAN_LUMBER_DEPOT_BLOCK, new Item.Properties()));

	// Volcanic Beehive Block
	public static final Block VOLCANIC_BEEHIVE_BLOCK = registerBlock("volcanic_beehive",
			new net.valentin.obsidium.block.VolcanicBeehiveBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BEEHIVE).strength(1.5f).sound(SoundType.WOOD).ignitedByLava()));
	public static final Item VOLCANIC_BEEHIVE_ITEM = registerItem("volcanic_beehive",
			new BlockItem(VOLCANIC_BEEHIVE_BLOCK, new Item.Properties().fireResistant()));

	// Demetrian Bonsai Planter Block
	public static final Block DEMETRIAN_BONSAI_PLANTER_BLOCK = registerBlock("demetrian_bonsai_planter",
			new net.valentin.obsidium.block.DemetrianBonsaiPlanterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FLOWER_POT).strength(1.2f).sound(SoundType.DECORATED_POT).noOcclusion()));
	public static final Item DEMETRIAN_BONSAI_PLANTER_ITEM = registerItem("demetrian_bonsai_planter",
			new BlockItem(DEMETRIAN_BONSAI_PLANTER_BLOCK, new Item.Properties()));

	// Grafted Rootstock In-World Block
	public static final Block GRAFTED_SAPLING_BLOCK = registerBlock("grafted_sapling",
			new net.valentin.obsidium.block.GraftedSaplingBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING).noCollission().instabreak().sound(SoundType.GRASS)));
	public static final Item GRAFTED_SAPLING_ITEM = registerItem("grafted_sapling",
			new BlockItem(GRAFTED_SAPLING_BLOCK, new Item.Properties()));

	// Volcanic Wood Liquid Barrel & Reinforced Volcanic Liquid Barrel
	public static final Block VOLCANIC_WOOD_LIQUID_BARREL_BLOCK = registerBlock("volcanic_wood_liquid_barrel",
			new net.valentin.obsidium.block.VolcanicWoodLiquidBarrelBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).strength(2.5f).sound(SoundType.WOOD), false));
	public static final Item VOLCANIC_WOOD_LIQUID_BARREL_ITEM = registerItem("volcanic_wood_liquid_barrel",
			new BlockItem(VOLCANIC_WOOD_LIQUID_BARREL_BLOCK, new Item.Properties().fireResistant()));

	public static final Block REINFORCED_VOLCANIC_LIQUID_BARREL_BLOCK = registerBlock("reinforced_volcanic_liquid_barrel",
			new net.valentin.obsidium.block.VolcanicWoodLiquidBarrelBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).strength(3.5f, 12.0f).sound(SoundType.WOOD), true));
	public static final Item REINFORCED_VOLCANIC_LIQUID_BARREL_ITEM = registerItem("reinforced_volcanic_liquid_barrel",
			new BlockItem(REINFORCED_VOLCANIC_LIQUID_BARREL_BLOCK, new Item.Properties().fireResistant()));

	// Volcanite Single-Item Storage Barrels
	public static final Block BASIC_VOLCANITE_ITEM_BARREL_BLOCK = registerBlock("basic_volcanite_item_barrel",
			new VolcaniteItemBarrelBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).strength(3.0f).sound(SoundType.WOOD), VolcaniteItemBarrelBlockEntity.BarrelTier.BASIC));
	public static final Item BASIC_VOLCANITE_ITEM_BARREL_ITEM = registerItem("basic_volcanite_item_barrel",
			new BlockItem(BASIC_VOLCANITE_ITEM_BARREL_BLOCK, new Item.Properties().fireResistant()));

	public static final Block ADVANCED_VOLCANITE_ITEM_BARREL_BLOCK = registerBlock("advanced_volcanite_item_barrel",
			new VolcaniteItemBarrelBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).strength(4.5f, 12.0f).sound(SoundType.NETHER_WOOD), VolcaniteItemBarrelBlockEntity.BarrelTier.ADVANCED));
	public static final Item ADVANCED_VOLCANITE_ITEM_BARREL_ITEM = registerItem("advanced_volcanite_item_barrel",
			new BlockItem(ADVANCED_VOLCANITE_ITEM_BARREL_BLOCK, new Item.Properties().fireResistant()));

	public static final Block SUPREME_VOLCANITE_ITEM_BARREL_BLOCK = registerBlock("supreme_volcanite_item_barrel",
			new VolcaniteItemBarrelBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(6.0f, 18.0f).sound(SoundType.NETHERITE_BLOCK), VolcaniteItemBarrelBlockEntity.BarrelTier.SUPREME));
	public static final Item SUPREME_VOLCANITE_ITEM_BARREL_ITEM = registerItem("supreme_volcanite_item_barrel",
			new BlockItem(SUPREME_VOLCANITE_ITEM_BARREL_BLOCK, new Item.Properties().fireResistant()));

	public static final Item DIONYSIAN_BARREL_CORE = registerItem("dionysian_barrel_core",
			new net.valentin.obsidium.item.DionysianBarrelCoreItem(new Item.Properties().fireResistant()));

	public static final Block MEGA_VOLCANIC_BARREL = registerBlock("mega_volcanic_barrel",
			new net.valentin.obsidium.block.MegaVolcanicBarrelBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).strength(5.0f, 18.0f).sound(SoundType.WOOD)));
	public static final Item MEGA_VOLCANIC_BARREL_ITEM = registerItem("mega_volcanic_barrel",
			new BlockItem(MEGA_VOLCANIC_BARREL, new Item.Properties().fireResistant().stacksTo(1)));
	public static final Block MEGA_VOLCANIC_BARREL_PART = registerBlock("mega_volcanic_barrel_part",
			new net.valentin.obsidium.block.MegaVolcanicBarrelPartBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL)
				.strength(5.0f, 18.0f).sound(SoundType.WOOD).noOcclusion()));

	public static final Block TARTARUS_PISTON_ENGINE = registerBlock("tartarus_piston_engine",
			new net.valentin.obsidium.block.TartarusPistonEngineBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(5.0f, 18.0f).sound(SoundType.NETHERITE_BLOCK)));
	public static final Item TARTARUS_PISTON_ENGINE_ITEM = registerItem("tartarus_piston_engine",
			new BlockItem(TARTARUS_PISTON_ENGINE, new Item.Properties().fireResistant().stacksTo(1)));
	public static final Block TARTARUS_PISTON_ENGINE_PART = registerBlock("tartarus_piston_engine_part",
			new net.valentin.obsidium.block.TartarusPistonEnginePartBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK)
				.strength(5.0f, 18.0f).sound(SoundType.NETHERITE_BLOCK).noOcclusion()));

	public static final Block VOLCANIC_DIESEL_GENERATOR = registerBlock("volcanic_diesel_generator",
			new net.valentin.obsidium.block.VolcanicDieselGeneratorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
				.strength(4.5f, 12.0f).sound(SoundType.METAL).noOcclusion()));
	public static final Item VOLCANIC_DIESEL_GENERATOR_ITEM = registerItem("volcanic_diesel_generator",
			new BlockItem(VOLCANIC_DIESEL_GENERATOR, new Item.Properties().fireResistant().stacksTo(16)));

	public static final Block DIONYSIAN_BIO_CALDERA = registerBlock("dionysian_bio_caldera",
			new net.valentin.obsidium.block.DionysianBioCalderaBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).strength(5.0f, 18.0f).sound(SoundType.WOOD)));
	public static final Item DIONYSIAN_BIO_CALDERA_ITEM = registerItem("dionysian_bio_caldera",
			new BlockItem(DIONYSIAN_BIO_CALDERA, new Item.Properties().fireResistant().stacksTo(1)));
	public static final Block DIONYSIAN_BIO_CALDERA_PART = registerBlock("dionysian_bio_caldera_part",
			new net.valentin.obsidium.block.DionysianBioCalderaPartBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL)
				.strength(5.0f, 18.0f).sound(SoundType.WOOD).noOcclusion()));

	public static final Item VINTAGE_VOLCANIC_TUICA = registerItem("vintage_volcanic_tuica",
			new net.valentin.obsidium.item.AgedTuicaItem(new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.RARE).fireResistant(), 1));

	public static final Item GRAND_RESERVE_TUICA = registerItem("grand_reserve_tuica",
			new net.valentin.obsidium.item.AgedTuicaItem(new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant(), 2));

	// Sacred Mythological Relics
	public static final Item THYRSUS_OF_DIONYSUS = registerItem("thyrsus_of_dionysus",
			new net.valentin.obsidium.item.ThyrsusItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));

	public static final Item CORNUCOPIA = registerItem("cornucopia",
			new net.valentin.obsidium.item.CornucopiaItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));

	public static final Item TALARIA_OF_HERMES = registerItem("talaria_of_hermes",
			new net.valentin.obsidium.item.TalariaOfHermesItem(new Item.Properties().stacksTo(1).fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC)));

	// Volcanic Ore Crops & Seeds
	public static Item getItem(String name) {
		return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MOD_ID, name));
	}

	public static final Block VOLCANITE_CROP = registerBlock("volcanite_crop",
			new VolcanicOreCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), () -> getItem("volcanite_seed"), () -> VOLCANITE_PETAL));
	public static final Item VOLCANITE_SEED = registerItem("volcanite_seed",
			new VolcanicSeedItem(VOLCANITE_CROP, new Item.Properties().fireResistant()));

	public static final Block IRON_CROP = registerBlock("iron_crop",
			new VolcanicOreCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), () -> getItem("iron_seed"), () -> IRON_PETAL));
	public static final Item IRON_SEED = registerItem("iron_seed",
			new VolcanicSeedItem(IRON_CROP, new Item.Properties()));

	public static final Block GOLD_CROP = registerBlock("gold_crop",
			new VolcanicOreCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), () -> getItem("gold_seed"), () -> GOLD_PETAL));
	public static final Item GOLD_SEED = registerItem("gold_seed",
			new VolcanicSeedItem(GOLD_CROP, new Item.Properties()));

	public static final Block COPPER_CROP = registerBlock("copper_crop",
			new VolcanicOreCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), () -> getItem("copper_seed"), () -> COPPER_PETAL));
	public static final Item COPPER_SEED = registerItem("copper_seed",
			new VolcanicSeedItem(COPPER_CROP, new Item.Properties()));

	public static final Block COAL_CROP = registerBlock("coal_crop",
			new VolcanicOreCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), () -> getItem("coal_seed"), () -> COAL_PETAL));
	public static final Item COAL_SEED = registerItem("coal_seed",
			new VolcanicSeedItem(COAL_CROP, new Item.Properties()));

	public static final Block LAPIS_CROP = registerBlock("lapis_crop",
			new VolcanicOreCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), () -> getItem("lapis_seed"), () -> LAPIS_PETAL));
	public static final Item LAPIS_SEED = registerItem("lapis_seed",
			new VolcanicSeedItem(LAPIS_CROP, new Item.Properties()));

	public static final Block REDSTONE_CROP = registerBlock("redstone_crop",
			new VolcanicOreCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), () -> getItem("redstone_seed"), () -> REDSTONE_PETAL));
	public static final Item REDSTONE_SEED = registerItem("redstone_seed",
			new VolcanicSeedItem(REDSTONE_CROP, new Item.Properties()));

	public static final Block DIAMOND_CROP = registerBlock("diamond_crop",
			new VolcanicOreCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), () -> getItem("diamond_seed"), () -> DIAMOND_PETAL));
	public static final Item DIAMOND_SEED = registerItem("diamond_seed",
			new VolcanicSeedItem(DIAMOND_CROP, new Item.Properties()));

	public static final Block NETHERITE_CROP = registerBlock("netherite_crop",
			new VolcanicOreCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), () -> getItem("netherite_seed"), () -> NETHERITE_PETAL));
	public static final Item NETHERITE_SEED = registerItem("netherite_seed",
			new VolcanicSeedItem(NETHERITE_CROP, new Item.Properties().fireResistant()));

	public static final Block TIN_CROP = registerBlock("tin_crop",
			new VolcanicOreCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), () -> getItem("tin_seed"), () -> TIN_PETAL));
	public static final Item TIN_SEED = registerItem("tin_seed",
			new VolcanicSeedItem(TIN_CROP, new Item.Properties()));

	public static final Block LEAD_CROP = registerBlock("lead_crop",
			new VolcanicOreCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), () -> getItem("lead_seed"), () -> LEAD_PETAL));
	public static final Item LEAD_SEED = registerItem("lead_seed",
			new VolcanicSeedItem(LEAD_CROP, new Item.Properties()));

	public static final Block SILVER_CROP = registerBlock("silver_crop",
			new VolcanicOreCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), () -> getItem("silver_seed"), () -> SILVER_PETAL));
	public static final Item SILVER_SEED = registerItem("silver_seed",
			new VolcanicSeedItem(SILVER_CROP, new Item.Properties()));
	
	public static final BlockEntityType<VolcaniteFurnaceBlockEntity> VOLCANITE_FURNACE_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_furnace_be"),
			BlockEntityType.Builder.of(VolcaniteFurnaceBlockEntity::new, VOLCANITE_FURNACE_BLOCK).build(null)
	);

	public static final MenuType<VolcaniteFurnaceMenu> VOLCANITE_FURNACE_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_furnace"),
			new MenuType<>(VolcaniteFurnaceMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final BlockEntityType<VolcaniteWorktableBlockEntity> VOLCANITE_WORKTABLE_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_worktable_be"),
			BlockEntityType.Builder.of(VolcaniteWorktableBlockEntity::new, VOLCANITE_WORKTABLE_BLOCK).build(null)
	);
	
	public static final BlockEntityType<net.valentin.obsidium.block.VolcaniteChestBlockEntity> VOLCANITE_CHEST_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_chest_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.VolcaniteChestBlockEntity::new, VOLCANITE_CHEST_BLOCK).build(null)
	);
	
	public static final BlockEntityType<net.valentin.obsidium.block.VolcaniteModularChestBlockEntity> VOLCANITE_MODULAR_CHEST_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_modular_chest_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.VolcaniteModularChestBlockEntity::new, VOLCANITE_MODULAR_CHEST_BLOCK).build(null)
	);
	
	public static final BlockEntityType<net.valentin.obsidium.block.HephaesteanArmoryBlockEntity> HEPHAESTEAN_ARMORY_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "hephaestean_armory_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.HephaesteanArmoryBlockEntity::new, HEPHAESTEAN_ARMORY_BLOCK).build(null)
	);
	public static final BlockEntityType<?> VOLCANITE_TOOL_CHEST_BE = HEPHAESTEAN_ARMORY_BE;

	public static final BlockEntityType<net.valentin.obsidium.block.StygianCenotaphBlockEntity> STYGIAN_CENOTAPH_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "stygian_cenotaph_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.StygianCenotaphBlockEntity::new, STYGIAN_CENOTAPH_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.SupremeLiquidTankBlockEntity> SUPREME_LIQUID_TANK_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "supreme_liquid_tank_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.SupremeLiquidTankBlockEntity::new, SUPREME_LIQUID_TANK_BLOCK).build(null)
	);
	
	public static final BlockEntityType<CreativeLavaTankBlockEntity> CREATIVE_LAVA_TANK_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "creative_lava_tank"),
            BlockEntityType.Builder.of(CreativeLavaTankBlockEntity::new, CREATIVE_LAVA_TANK).build(null)
    );
    public static final BlockEntityType<CreativeEnergyCoreBlockEntity> CREATIVE_ENERGY_CORE_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "creative_energy_core"),
            BlockEntityType.Builder.of(CreativeEnergyCoreBlockEntity::new, CREATIVE_ENERGY_CORE).build(null)
    );

	public static final BlockEntityType<net.valentin.obsidium.block.CreativeWaterTankBlockEntity> CREATIVE_WATER_TANK_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "creative_water_tank"),
            BlockEntityType.Builder.of(net.valentin.obsidium.block.CreativeWaterTankBlockEntity::new, CREATIVE_WATER_TANK).build(null)
    );

	public static final BlockEntityType<net.valentin.obsidium.block.VolcaniteStorageUnitBlockEntity> VOLCANITE_STORAGE_UNIT_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_storage_unit_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.VolcaniteStorageUnitBlockEntity::new, VOLCANITE_STORAGE_UNIT_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.VolcaniteStorageDriveBlockEntity> VOLCANITE_STORAGE_DRIVE_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_storage_drive_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.VolcaniteStorageDriveBlockEntity::new, VOLCANITE_STORAGE_DISK_DRIVE_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.VolcaniteStoragePipeBlockEntity> VOLCANITE_STORAGE_PIPE_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_storage_pipe_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.VolcaniteStoragePipeBlockEntity::new, VOLCANITE_STORAGE_PIPE_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.VolcaniteStorageControllerBlockEntity> VOLCANITE_STORAGE_CONTROLLER_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_storage_controller_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.VolcaniteStorageControllerBlockEntity::new, VOLCANITE_STORAGE_CONTROLLER_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.VolcaniteStorageInterfaceBlockEntity> VOLCANITE_STORAGE_INTERFACE_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_storage_interface_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.VolcaniteStorageInterfaceBlockEntity::new, VOLCANITE_STORAGE_INTERFACE_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.VolcaniteLiquidStorageTankBlockEntity> VOLCANITE_LIQUID_STORAGE_TANK_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_liquid_storage_tank_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.VolcaniteLiquidStorageTankBlockEntity::new, VOLCANITE_LIQUID_STORAGE_TANK_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.VolcaniteSortingStationBlockEntity> VOLCANITE_SORTING_STATION_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_sorting_station_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.VolcaniteSortingStationBlockEntity::new, VOLCANITE_SORTING_STATION_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.VolcaniteNetworkNodeEmitterBlockEntity> VOLCANITE_NETWORK_NODE_EMITTER_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_network_node_emitter_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.VolcaniteNetworkNodeEmitterBlockEntity::new, VOLCANITE_NETWORK_NODE_EMITTER_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.VolcaniteNetworkNodeReceiverBlockEntity> VOLCANITE_NETWORK_NODE_RECEIVER_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_network_node_receiver_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.VolcaniteNetworkNodeReceiverBlockEntity::new, VOLCANITE_NETWORK_NODE_RECEIVER_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.VolcaniteChunkLoaderBlockEntity> VOLCANITE_CHUNK_LOADER_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_chunk_loader_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.VolcaniteChunkLoaderBlockEntity::new, VOLCANITE_CHUNK_LOADER).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.AegisSanctuaryWardBlockEntity> AEGIS_SANCTUARY_WARD_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "aegis_sanctuary_ward_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.AegisSanctuaryWardBlockEntity::new, AEGIS_SANCTUARY_WARD).build(null)
	);

	public static final BlockEntityType<VolcaniteGeneratorBlockEntity> VOLCANITE_GENERATOR_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_generator_be"),
			BlockEntityType.Builder.of(VolcaniteGeneratorBlockEntity::new, VOLCANITE_GENERATOR_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.VolcaniteGrinderBlockEntity> VOLCANITE_GRINDER_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_grinder_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.VolcaniteGrinderBlockEntity::new, VOLCANITE_GRINDER_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.AdvancedVolcaniteFurnaceBlockEntity> ADVANCED_VOLCANITE_FURNACE_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "advanced_volcanite_furnace_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.AdvancedVolcaniteFurnaceBlockEntity::new, ADVANCED_VOLCANITE_FURNACE_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.VolcaniteEnergyWireBlockEntity> VOLCANITE_ENERGY_WIRE_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_energy_wire_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.VolcaniteEnergyWireBlockEntity::new,
				VOLCANITE_ENERGY_WIRE_BLOCK,
				ADVANCED_VOLCANITE_ENERGY_WIRE_BLOCK,
				SUPREME_VOLCANITE_ENERGY_WIRE_BLOCK,
				SINGULARITY_VOLCANITE_ENERGY_WIRE_BLOCK
			).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.HephaesteanChargingPlateBlockEntity> HEPHAESTEAN_CHARGING_PLATE_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "hephaestean_charging_plate_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.HephaesteanChargingPlateBlockEntity::new, HEPHAESTEAN_CHARGING_PLATE_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.AstrapeEnergyPylonBlockEntity> ASTRAPE_ENERGY_PYLON_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "astrape_energy_pylon_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.AstrapeEnergyPylonBlockEntity::new, ASTRAPE_ENERGY_PYLON_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.OlympianAltarBlockEntity> OLYMPIAN_ALTAR_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "olympian_altar_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.OlympianAltarBlockEntity::new, OLYMPIAN_ALTAR_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.AstrapeRitualObeliskBlockEntity> ASTRAPE_RITUAL_OBELISK_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "astrape_ritual_obelisk_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.AstrapeRitualObeliskBlockEntity::new, ASTRAPE_RITUAL_OBELISK_BLOCK).build(null)
	);
	
	public static final BlockEntityType<net.valentin.obsidium.block.VolcaniteUniversalConduitBlockEntity> VOLCANITE_UNIVERSAL_CONDUIT_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_universal_conduit_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.VolcaniteUniversalConduitBlockEntity::new, VOLCANITE_UNIVERSAL_CONDUIT_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.NaiadFluidPipeBlockEntity> NAIAD_FLUID_PIPE_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "naiad_fluid_pipe_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.NaiadFluidPipeBlockEntity::new,
					NAIAD_FLUID_PIPE_BLOCK, ADVANCED_NAIAD_FLUID_PIPE_BLOCK, SUPREME_NAIAD_FLUID_PIPE_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.HermesItemPipeBlockEntity> HERMES_ITEM_PIPE_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "hermes_item_pipe_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.HermesItemPipeBlockEntity::new,
					HERMES_ITEM_PIPE_BLOCK, ADVANCED_HERMES_ITEM_PIPE_BLOCK, SUPREME_HERMES_ITEM_PIPE_BLOCK).build(null)
	);
	
	public static final BlockEntityType<net.valentin.obsidium.block.VolcaniteLavaPumpBlockEntity> VOLCANITE_LAVA_PUMP_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_lava_pump_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.VolcaniteLavaPumpBlockEntity::new, VOLCANITE_LAVA_PUMP_BLOCK).build(null)
	);
	
	public static final BlockEntityType<net.valentin.obsidium.block.VolcaniteEnergyCellBlockEntity> VOLCANITE_ENERGY_CELL_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_energy_cell_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.VolcaniteEnergyCellBlockEntity::new, VOLCANITE_ENERGY_CELL_BLOCK).build(null)
	);
	
	public static final BlockEntityType<net.valentin.obsidium.block.VolcaniteEnergyUnitBlockEntity> VOLCANITE_ENERGY_UNIT_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_energy_unit_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.VolcaniteEnergyUnitBlockEntity::new, VOLCANITE_ENERGY_UNIT_BLOCK).build(null)
	);
	
	public static final BlockEntityType<net.valentin.obsidium.block.VolcaniteEnergyCoreBlockEntity> VOLCANITE_ENERGY_CORE_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_energy_core_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.VolcaniteEnergyCoreBlockEntity::new, VOLCANITE_ENERGY_CORE_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.MagmaForgeCoreBlockEntity> MAGMA_FORGE_CORE_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "magma_forge_core_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.MagmaForgeCoreBlockEntity::new, MAGMA_FORGE_CORE_BLOCK).build(null)
	);

	public static final BlockEntityType<GeothermalArboristBlockEntity> GEOTHERMAL_ARBORIST_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "geothermal_arborist_be"),
			BlockEntityType.Builder.of(GeothermalArboristBlockEntity::new, GEOTHERMAL_ARBORIST_BLOCK).build(null)
	);

	public static final MenuType<GeothermalArboristMenu> GEOTHERMAL_ARBORIST_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "geothermal_arborist_menu"),
			new MenuType<>(GeothermalArboristMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final BlockEntityType<VolcaniteSteamGeneratorBlockEntity> VOLCANITE_STEAM_GENERATOR_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_steam_generator_be"),
			BlockEntityType.Builder.of(VolcaniteSteamGeneratorBlockEntity::new, VOLCANITE_STEAM_GENERATOR_BLOCK).build(null)
	);

	public static final MenuType<VolcaniteSteamGeneratorMenu> VOLCANITE_STEAM_GENERATOR_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_steam_generator_menu"),
			new MenuType<>(VolcaniteSteamGeneratorMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final BlockEntityType<VolcaniteGeothermalGeneratorBlockEntity> VOLCANITE_GEOTHERMAL_GENERATOR_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_geothermal_generator_be"),
			BlockEntityType.Builder.of(VolcaniteGeothermalGeneratorBlockEntity::new, VOLCANITE_GEOTHERMAL_GENERATOR_BLOCK).build(null)
	);

	public static final MenuType<VolcaniteGeothermalGeneratorMenu> VOLCANITE_GEOTHERMAL_GENERATOR_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_geothermal_generator_menu"),
			new MenuType<>(VolcaniteGeothermalGeneratorMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final BlockEntityType<SupremeVolcanicGeneratorBlockEntity> SUPREME_VOLCANIC_GENERATOR_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "supreme_volcanic_generator_be"),
			BlockEntityType.Builder.of(SupremeVolcanicGeneratorBlockEntity::new, SUPREME_VOLCANIC_GENERATOR_BLOCK).build(null)
	);

	public static final MenuType<SupremeVolcanicGeneratorMenu> SUPREME_VOLCANIC_GENERATOR_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "supreme_volcanic_generator_menu"),
			new MenuType<>(SupremeVolcanicGeneratorMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final BlockEntityType<VolcanicDistillationChamberBlockEntity> VOLCANIC_DISTILLATION_CHAMBER_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanic_distillation_chamber_be"),
			BlockEntityType.Builder.of(VolcanicDistillationChamberBlockEntity::new, VOLCANIC_DISTILLATION_CHAMBER_BLOCK).build(null)
	);

	public static final MenuType<VolcanicDistillationChamberMenu> VOLCANIC_DISTILLATION_CHAMBER_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanic_distillation_chamber_menu"),
			new MenuType<>(VolcanicDistillationChamberMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final BlockEntityType<HydrothermalVitrifierBlockEntity> HYDROTHERMAL_VITRIFIER_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "hydrothermal_vitrifier_be"),
			BlockEntityType.Builder.of(HydrothermalVitrifierBlockEntity::new, HYDROTHERMAL_VITRIFIER_BLOCK).build(null)
	);

	public static final MenuType<HydrothermalVitrifierMenu> HYDROTHERMAL_VITRIFIER_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "hydrothermal_vitrifier_menu"),
			new MenuType<>(HydrothermalVitrifierMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final BlockEntityType<MagmaticCrucibleBlockEntity> MAGMATIC_CRUCIBLE_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "magmatic_crucible_be"),
			BlockEntityType.Builder.of(MagmaticCrucibleBlockEntity::new, MAGMATIC_CRUCIBLE_BLOCK).build(null)
	);

	public static final MenuType<MagmaticCrucibleMenu> MAGMATIC_CRUCIBLE_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "magmatic_crucible_menu"),
			new MenuType<>(MagmaticCrucibleMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final BlockEntityType<HydrothermalCondenserBlockEntity> HYDROTHERMAL_CONDENSER_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "hydrothermal_condenser_be"),
			BlockEntityType.Builder.of(HydrothermalCondenserBlockEntity::new, HYDROTHERMAL_CONDENSER_BLOCK).build(null)
	);

	public static final MenuType<HydrothermalCondenserMenu> HYDROTHERMAL_CONDENSER_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "hydrothermal_condenser_menu"),
			new MenuType<>(HydrothermalCondenserMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final BlockEntityType<HydrothermalLithofierBlockEntity> HYDROTHERMAL_LITHOFIER_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "hydrothermal_lithofier_be"),
			BlockEntityType.Builder.of(HydrothermalLithofierBlockEntity::new, HYDROTHERMAL_LITHOFIER_BLOCK).build(null)
	);

	public static final MenuType<HydrothermalLithofierMenu> HYDROTHERMAL_LITHOFIER_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "hydrothermal_lithofier_menu"),
			new MenuType<>(HydrothermalLithofierMenu::new, FeatureFlags.VANILLA_SET)
	);

	// Volcanite Single-Item Barrels
	public static final BlockEntityType<VolcaniteItemBarrelBlockEntity> BASIC_VOLCANITE_ITEM_BARREL_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "basic_volcanite_item_barrel_be"),
			BlockEntityType.Builder.of((pos, state) -> new VolcaniteItemBarrelBlockEntity(pos, state, VolcaniteItemBarrelBlockEntity.BarrelTier.BASIC), BASIC_VOLCANITE_ITEM_BARREL_BLOCK).build(null)
	);

	public static final BlockEntityType<VolcaniteItemBarrelBlockEntity> ADVANCED_VOLCANITE_ITEM_BARREL_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "advanced_volcanite_item_barrel_be"),
			BlockEntityType.Builder.of((pos, state) -> new VolcaniteItemBarrelBlockEntity(pos, state, VolcaniteItemBarrelBlockEntity.BarrelTier.ADVANCED), ADVANCED_VOLCANITE_ITEM_BARREL_BLOCK).build(null)
	);

	public static final BlockEntityType<VolcaniteItemBarrelBlockEntity> SUPREME_VOLCANITE_ITEM_BARREL_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "supreme_volcanite_item_barrel_be"),
			BlockEntityType.Builder.of((pos, state) -> new VolcaniteItemBarrelBlockEntity(pos, state, VolcaniteItemBarrelBlockEntity.BarrelTier.SUPREME), SUPREME_VOLCANITE_ITEM_BARREL_BLOCK).build(null)
	);

	public static final MenuType<VolcaniteItemBarrelMenu> VOLCANITE_ITEM_BARREL_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_item_barrel_menu"),
			new MenuType<>(VolcaniteItemBarrelMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.DionysianDistilleryBlockEntity> DIONYSIAN_DISTILLERY_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "dionysian_distillery_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.DionysianDistilleryBlockEntity::new, DIONYSIAN_DISTILLERY_BLOCK).build(null)
	);

	public static final MenuType<net.valentin.obsidium.block.DionysianDistilleryMenu> DIONYSIAN_DISTILLERY_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "dionysian_distillery_menu"),
			new MenuType<>(net.valentin.obsidium.block.DionysianDistilleryMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.DionysianPressBlockEntity> DIONYSIAN_PRESS_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "dionysian_press_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.DionysianPressBlockEntity::new, DIONYSIAN_PRESS_BLOCK).build(null)
	);

	public static final MenuType<net.valentin.obsidium.block.DionysianPressMenu> DIONYSIAN_PRESS_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "dionysian_press_menu"),
			new MenuType<>(net.valentin.obsidium.block.DionysianPressMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.AdvancedDionysianPressBlockEntity> ADVANCED_DIONYSIAN_PRESS_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "advanced_dionysian_press_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.AdvancedDionysianPressBlockEntity::new, ADVANCED_DIONYSIAN_PRESS_BLOCK).build(null)
	);

	public static final MenuType<net.valentin.obsidium.block.AdvancedDionysianPressMenu> ADVANCED_DIONYSIAN_PRESS_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "advanced_dionysian_press_menu"),
			new MenuType<>(net.valentin.obsidium.block.AdvancedDionysianPressMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.DemetrianBiomassGeneratorBlockEntity> DEMETRIAN_BIOMASS_GENERATOR_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "demetrian_biomass_generator_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.DemetrianBiomassGeneratorBlockEntity::new, DEMETRIAN_BIOMASS_GENERATOR_BLOCK).build(null)
	);

	public static final MenuType<net.valentin.obsidium.block.DemetrianBiomassGeneratorMenu> DEMETRIAN_BIOMASS_GENERATOR_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "demetrian_biomass_generator_menu"),
			new MenuType<>(net.valentin.obsidium.block.DemetrianBiomassGeneratorMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.DemetrianComposterBlockEntity> DEMETRIAN_COMPOSTER_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "demetrian_composter_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.DemetrianComposterBlockEntity::new, DEMETRIAN_COMPOSTER_BLOCK).build(null)
	);

	public static final MenuType<net.valentin.obsidium.block.DemetrianComposterMenu> DEMETRIAN_COMPOSTER_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "demetrian_composter_menu"),
			new MenuType<>(net.valentin.obsidium.block.DemetrianComposterMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.DionysianAgingBarrelBlockEntity> DIONYSIAN_AGING_BARREL_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "dionysian_aging_barrel_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.DionysianAgingBarrelBlockEntity::new, DIONYSIAN_AGING_BARREL_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.DemetrianSaplingBarrelBlockEntity> DEMETRIAN_SAPLING_BARREL_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "demetrian_sapling_barrel_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.DemetrianSaplingBarrelBlockEntity::new, DEMETRIAN_SAPLING_BARREL_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.DemetrianLumberDepotBlockEntity> DEMETRIAN_LUMBER_DEPOT_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "demetrian_lumber_depot_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.DemetrianLumberDepotBlockEntity::new, DEMETRIAN_LUMBER_DEPOT_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.VolcanicBeehiveBlockEntity> VOLCANIC_BEEHIVE_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanic_beehive_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.VolcanicBeehiveBlockEntity::new, VOLCANIC_BEEHIVE_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.DemetrianBonsaiPlanterBlockEntity> DEMETRIAN_BONSAI_PLANTER_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "demetrian_bonsai_planter_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.DemetrianBonsaiPlanterBlockEntity::new, DEMETRIAN_BONSAI_PLANTER_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.GraftedSaplingBlockEntity> GRAFTED_SAPLING_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "grafted_sapling_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.GraftedSaplingBlockEntity::new, GRAFTED_SAPLING_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.VolcanicWoodLiquidBarrelBlockEntity> VOLCANIC_WOOD_LIQUID_BARREL_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanic_wood_liquid_barrel_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.VolcanicWoodLiquidBarrelBlockEntity::new, VOLCANIC_WOOD_LIQUID_BARREL_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.ReinforcedVolcanicLiquidBarrelBlockEntity> REINFORCED_VOLCANIC_LIQUID_BARREL_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "reinforced_volcanic_liquid_barrel_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.ReinforcedVolcanicLiquidBarrelBlockEntity::new, REINFORCED_VOLCANIC_LIQUID_BARREL_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.MegaVolcanicBarrelBlockEntity> MEGA_VOLCANIC_BARREL_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "mega_volcanic_barrel_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.MegaVolcanicBarrelBlockEntity::new, MEGA_VOLCANIC_BARREL).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.TartarusPistonEngineBlockEntity> TARTARUS_PISTON_ENGINE_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "tartarus_piston_engine_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.TartarusPistonEngineBlockEntity::new, TARTARUS_PISTON_ENGINE).build(null)
	);

	public static final MenuType<net.valentin.obsidium.block.TartarusPistonEngineMenu> TARTARUS_PISTON_ENGINE_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "tartarus_piston_engine_menu"),
			new MenuType<>(net.valentin.obsidium.block.TartarusPistonEngineMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.VolcanicDieselGeneratorBlockEntity> VOLCANIC_DIESEL_GENERATOR_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanic_diesel_generator_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.VolcanicDieselGeneratorBlockEntity::new, VOLCANIC_DIESEL_GENERATOR).build(null)
	);

	public static final MenuType<net.valentin.obsidium.block.VolcanicDieselGeneratorMenu> VOLCANIC_DIESEL_GENERATOR_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanic_diesel_generator_menu"),
			new MenuType<>(net.valentin.obsidium.block.VolcanicDieselGeneratorMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.DionysianBioCalderaBlockEntity> DIONYSIAN_BIO_CALDERA_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "dionysian_bio_caldera_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.DionysianBioCalderaBlockEntity::new, DIONYSIAN_BIO_CALDERA).build(null)
	);

	public static final MenuType<net.valentin.obsidium.block.DionysianBioCalderaMenu> DIONYSIAN_BIO_CALDERA_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "dionysian_bio_caldera_menu"),
			new MenuType<>(net.valentin.obsidium.block.DionysianBioCalderaMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<net.valentin.obsidium.block.DionysianAgingBarrelMenu> DIONYSIAN_AGING_BARREL_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "dionysian_aging_barrel_menu"),
			new MenuType<>(net.valentin.obsidium.block.DionysianAgingBarrelMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<net.valentin.obsidium.block.DemetrianSaplingBarrelMenu> DEMETRIAN_SAPLING_BARREL_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "demetrian_sapling_barrel_menu"),
			new MenuType<>(net.valentin.obsidium.block.DemetrianSaplingBarrelMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<net.valentin.obsidium.block.DemetrianLumberDepotMenu> DEMETRIAN_LUMBER_DEPOT_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "demetrian_lumber_depot_menu"),
			new MenuType<>(net.valentin.obsidium.block.DemetrianLumberDepotMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<VolcaniteWorktableMenu> VOLCANITE_WORKTABLE_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_worktable"),
			new MenuType<>(VolcaniteWorktableMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<net.valentin.obsidium.menu.HephaesteanArmoryMenu> HEPHAESTEAN_ARMORY_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "hephaestean_armory_menu"),
			new MenuType<>(net.valentin.obsidium.menu.HephaesteanArmoryMenu::new, FeatureFlags.VANILLA_SET)
	);
	
	public static final MenuType<net.valentin.obsidium.block.VolcaniteBatteryMenu> VOLCANITE_BATTERY_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_battery_menu"),
			new MenuType<>(net.valentin.obsidium.block.VolcaniteBatteryMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<net.valentin.obsidium.block.VolcaniteGeneratorMenu> VOLCANITE_GENERATOR_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_generator"),
			new MenuType<>(net.valentin.obsidium.block.VolcaniteGeneratorMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<net.valentin.obsidium.block.VolcaniteGrinderMenu> VOLCANITE_GRINDER_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_grinder"),
			new MenuType<>(net.valentin.obsidium.block.VolcaniteGrinderMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<net.valentin.obsidium.block.AdvancedVolcaniteFurnaceMenu> ADVANCED_VOLCANITE_FURNACE_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "advanced_volcanite_furnace"),
			new MenuType<>(net.valentin.obsidium.block.AdvancedVolcaniteFurnaceMenu::new, FeatureFlags.VANILLA_SET)
	);
	
	public static final MenuType<net.valentin.obsidium.block.VolcaniteLavaPumpMenu> VOLCANITE_LAVA_PUMP_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_lava_pump_menu"),
			new MenuType<>(net.valentin.obsidium.block.VolcaniteLavaPumpMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<net.valentin.obsidium.block.CreativeLavaTankMenu> CREATIVE_LAVA_TANK_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "creative_lava_tank_menu"),
			new MenuType<>(net.valentin.obsidium.block.CreativeLavaTankMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<net.valentin.obsidium.block.LiquidTankMenu> LIQUID_TANK_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "liquid_tank"),
			new MenuType<>(net.valentin.obsidium.block.LiquidTankMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<net.valentin.obsidium.block.PipeConfigMenu> PIPE_CONFIG_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "pipe_config"),
			new MenuType<>(net.valentin.obsidium.block.PipeConfigMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<net.valentin.obsidium.block.CreativeWaterTankMenu> CREATIVE_WATER_TANK_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "creative_water_tank_menu"),
			new MenuType<>(net.valentin.obsidium.block.CreativeWaterTankMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<net.valentin.obsidium.block.VolcaniteStorageUnitMenu> VOLCANITE_STORAGE_UNIT_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_storage_unit"),
			new MenuType<>(net.valentin.obsidium.block.VolcaniteStorageUnitMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<net.valentin.obsidium.block.VolcaniteStorageDriveMenu> VOLCANITE_STORAGE_DRIVE_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_storage_drive"),
			new MenuType<>(net.valentin.obsidium.block.VolcaniteStorageDriveMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<net.valentin.obsidium.block.VolcanitePipeModuleMenu> VOLCANITE_PIPE_MODULE_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_pipe_module"),
			new MenuType<>(net.valentin.obsidium.block.VolcanitePipeModuleMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<net.valentin.obsidium.menu.HermesFilterMenu> HERMES_FILTER_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "hermes_filter"),
			new MenuType<>(net.valentin.obsidium.menu.HermesFilterMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<net.valentin.obsidium.block.VolcaniteStorageTerminalMenu> VOLCANITE_STORAGE_TERMINAL_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_storage_terminal"),
			new MenuType<>(net.valentin.obsidium.block.VolcaniteStorageTerminalMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.VolcaniteCrafterInterfaceBlockEntity> VOLCANITE_CRAFTER_INTERFACE_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_crafter_interface_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.VolcaniteCrafterInterfaceBlockEntity::new, VOLCANITE_CRAFTER_INTERFACE_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.ThanatosExecutionerBlockEntity> THANATOS_EXECUTIONER_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "thanatos_executioner_be"),
			BlockEntityType.Builder.of(net.valentin.obsidium.block.ThanatosExecutionerBlockEntity::new, THANATOS_EXECUTIONER_BLOCK).build(null)
	);

	public static final MenuType<net.valentin.obsidium.block.ThanatosExecutionerMenu> THANATOS_EXECUTIONER_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "thanatos_executioner_menu"),
			new MenuType<>(net.valentin.obsidium.block.ThanatosExecutionerMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<net.valentin.obsidium.block.VolcaniteCrafterInterfaceMenu> VOLCANITE_CRAFTER_INTERFACE_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_crafter_interface_menu"),
			new MenuType<>(net.valentin.obsidium.block.VolcaniteCrafterInterfaceMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<net.valentin.obsidium.block.VolcaniteLiquidStorageTankMenu> VOLCANITE_LIQUID_STORAGE_TANK_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_liquid_storage_tank"),
			new MenuType<>(net.valentin.obsidium.block.VolcaniteLiquidStorageTankMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<net.valentin.obsidium.block.VolcaniteSortingStationMenu> VOLCANITE_SORTING_STATION_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_sorting_station"),
			new MenuType<>(net.valentin.obsidium.block.VolcaniteSortingStationMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<net.valentin.obsidium.menu.BasicBackpackMenu> BASIC_BACKPACK_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "basic_backpack"),
			new MenuType<>(net.valentin.obsidium.menu.BasicBackpackMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<net.valentin.obsidium.menu.AdvancedBackpackMenu> ADVANCED_BACKPACK_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "advanced_backpack"),
			new MenuType<>(net.valentin.obsidium.menu.AdvancedBackpackMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<net.valentin.obsidium.block.VolcaniteModularChestMenu> VOLCANITE_MODULAR_CHEST_MENU = Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_modular_chest"),
			new MenuType<>(net.valentin.obsidium.block.VolcaniteModularChestMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final CreativeModeTab OBSIDIUM_TAB = Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "obsidium_tab"),
			FabricItemGroup.builder()
					.icon(() -> new ItemStack(VOLCANIC_ALLOY))
					.title(Component.translatable("itemGroup.obsidium.obsidium_tab"))
					.displayItems(net.valentin.obsidium.registry.ModCreativeTabs::populateObsidiumTab)
					.build()
	);

	private static Item registerItem(String name, Item item) {
		return Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, name), item);
	}

	private static Block registerBlock(String name, Block block) {
		return Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(MOD_ID, name), block);
	}

	public static final net.minecraft.world.entity.EntityType<net.valentin.obsidium.entity.CinderStalker> CINDER_STALKER = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "cinder_stalker"),
		net.minecraft.world.entity.EntityType.Builder.of(net.valentin.obsidium.entity.CinderStalker::new, net.minecraft.world.entity.MobCategory.MONSTER)
			.sized(0.9F, 1.25F)
			.fireImmune()
			.clientTrackingRange(8)
			.build("cinder_stalker")
	);

	public static final net.minecraft.world.entity.EntityType<net.valentin.obsidium.entity.TartareanHound> TARTAREAN_HOUND = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "tartarean_hound"),
		net.minecraft.world.entity.EntityType.Builder.of(net.valentin.obsidium.entity.TartareanHound::new, net.minecraft.world.entity.MobCategory.CREATURE)
			.sized(0.7F, 0.9F)
			.fireImmune()
			.clientTrackingRange(10)
			.build("tartarean_hound")
	);
	public static final Item TARTAREAN_HOUND_SPAWN_EGG = registerItem("tartarean_hound_spawn_egg",
		new net.minecraft.world.item.SpawnEggItem(TARTAREAN_HOUND, 0x1F1418, 0xDC2626, new Item.Properties()));

	public static final net.minecraft.world.entity.EntityType<net.valentin.obsidium.entity.VolcanicBee> VOLCANIC_BEE = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanic_bee"),
		net.minecraft.world.entity.EntityType.Builder.of(net.valentin.obsidium.entity.VolcanicBee::new, net.minecraft.world.entity.MobCategory.CREATURE)
			.sized(0.7F, 0.6F)
			.fireImmune()
			.clientTrackingRange(8)
			.build("volcanic_bee")
	);
	public static final Item VOLCANIC_BEE_SPAWN_EGG = registerItem("volcanic_bee_spawn_egg",
		new net.minecraft.world.item.SpawnEggItem(VOLCANIC_BEE, 0xE11D48, 0xFBBF24, new Item.Properties().fireResistant()));

	public static final Block APOLLO_SOLAR_GENERATOR_BLOCK = registerBlock("apollo_solar_generator",
		new net.valentin.obsidium.block.ApolloSolarGeneratorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(4.5f, 1200.0f).noOcclusion()));
	public static final Item APOLLO_SOLAR_GENERATOR_ITEM = registerItem("apollo_solar_generator",
		new BlockItem(APOLLO_SOLAR_GENERATOR_BLOCK, new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

	public static final BlockEntityType<net.valentin.obsidium.block.ApolloSolarGeneratorBlockEntity> APOLLO_SOLAR_GENERATOR_BE = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "apollo_solar_generator_be"),
		BlockEntityType.Builder.of(net.valentin.obsidium.block.ApolloSolarGeneratorBlockEntity::new, APOLLO_SOLAR_GENERATOR_BLOCK).build(null)
	);

	public static final MenuType<net.valentin.obsidium.block.ApolloSolarGeneratorMenu> APOLLO_SOLAR_GENERATOR_MENU = Registry.register(
		BuiltInRegistries.MENU,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "apollo_solar_generator_menu"),
		new MenuType<>(net.valentin.obsidium.block.ApolloSolarGeneratorMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final Block AEOLUS_STORM_TURBINE_BLOCK = registerBlock("aeolus_storm_turbine",
		new net.valentin.obsidium.block.AeolusStormTurbineBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(4.5f, 1200.0f).noOcclusion().lightLevel(s -> s.getValue(net.valentin.obsidium.block.AeolusStormTurbineBlock.STORM) ? 13 : (s.getValue(net.valentin.obsidium.block.AeolusStormTurbineBlock.ACTIVE) ? 7 : 0))));
	public static final Item AEOLUS_STORM_TURBINE_ITEM = registerItem("aeolus_storm_turbine",
		new BlockItem(AEOLUS_STORM_TURBINE_BLOCK, new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.RARE)));

	public static final BlockEntityType<net.valentin.obsidium.block.AeolusStormTurbineBlockEntity> AEOLUS_STORM_TURBINE_BE = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "aeolus_storm_turbine_be"),
		BlockEntityType.Builder.of(net.valentin.obsidium.block.AeolusStormTurbineBlockEntity::new, AEOLUS_STORM_TURBINE_BLOCK).build(null)
	);

	public static final MenuType<net.valentin.obsidium.block.AeolusStormTurbineMenu> AEOLUS_STORM_TURBINE_MENU = Registry.register(
		BuiltInRegistries.MENU,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "aeolus_storm_turbine_menu"),
		new MenuType<>(net.valentin.obsidium.block.AeolusStormTurbineMenu::new, FeatureFlags.VANILLA_SET)
	);

	// Keraunos High-Voltage 3x3x3 Substation Multiblock
	public static final Block KERAUNOS_HV_CORE_BLOCK = registerBlock("keraunos_hv_core",
		new net.valentin.obsidium.block.KeraunosHVCoreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(6.0f, 2400.0f).noOcclusion().lightLevel(s -> s.getValue(net.valentin.obsidium.block.KeraunosHVCoreBlock.ACTIVE) ? 15 : 4)));
	public static final Item KERAUNOS_HV_CORE_ITEM = registerItem("keraunos_hv_core",
		new BlockItem(KERAUNOS_HV_CORE_BLOCK, new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC)));

	public static final Block KERAUNOS_HV_PORT_BLOCK = registerBlock("keraunos_hv_port",
		new net.valentin.obsidium.block.KeraunosHVPortBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(5.0f, 1800.0f)));
	public static final Item KERAUNOS_HV_PORT_ITEM = registerItem("keraunos_hv_port",
		new BlockItem(KERAUNOS_HV_PORT_BLOCK, new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.RARE)));

	public static final BlockEntityType<net.valentin.obsidium.block.KeraunosHVCoreBlockEntity> KERAUNOS_HV_CORE_BLOCK_ENTITY = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "keraunos_hv_core_be"),
		BlockEntityType.Builder.of(net.valentin.obsidium.block.KeraunosHVCoreBlockEntity::new, KERAUNOS_HV_CORE_BLOCK).build(null)
	);

	public static final MenuType<net.valentin.obsidium.block.KeraunosHVCoreMenu> KERAUNOS_HV_CORE_MENU = Registry.register(
		BuiltInRegistries.MENU,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "keraunos_hv_core_menu"),
		new MenuType<>(net.valentin.obsidium.block.KeraunosHVCoreMenu::new, FeatureFlags.VANILLA_SET)
	);

	// Olympian Hadron Collider (5x5 CERN Toroidal Particle Accelerator Multiblock)
	public static final Block OLYMPIAN_HADRON_COLLIDER_BLOCK = registerBlock("olympian_hadron_collider",
		new net.valentin.obsidium.block.OlympianHadronColliderBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK)
			.strength(5.5f, 1800.0f).lightLevel(s -> s.getValue(net.valentin.obsidium.block.OlympianHadronColliderBlock.ACTIVE) ? 15
				: (s.getValue(net.valentin.obsidium.block.OlympianHadronColliderBlock.FORMED) ? 10 : 3))));
	public static final Item OLYMPIAN_HADRON_COLLIDER_ITEM = registerItem("olympian_hadron_collider",
		new BlockItem(OLYMPIAN_HADRON_COLLIDER_BLOCK, new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC)));

	public static final Block SYNCHROTRON_RING_COIL_BLOCK = registerBlock("synchrotron_ring_coil",
		new net.valentin.obsidium.block.SynchrotronRingCoilBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK)
			.strength(4.5f, 1200.0f).lightLevel(s -> s.getValue(net.valentin.obsidium.block.SynchrotronRingCoilBlock.FORMED) ? 9 : 0)));
	public static final Item SYNCHROTRON_RING_COIL_ITEM = registerItem("synchrotron_ring_coil",
		new BlockItem(SYNCHROTRON_RING_COIL_BLOCK, new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.RARE)));

	public static final Block HADRON_ENERGY_PORT_BLOCK = registerBlock("hadron_energy_port",
		new net.valentin.obsidium.block.HadronColliderPortBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK)
			.strength(4.5f, 1200.0f).lightLevel(s -> s.getValue(net.valentin.obsidium.block.HadronColliderPortBlock.FORMED) ? 10 : 2),
			net.valentin.obsidium.block.HadronColliderPortBlock.PortType.ENERGY));
	public static final Item HADRON_ENERGY_PORT_ITEM = registerItem("hadron_energy_port",
		new BlockItem(HADRON_ENERGY_PORT_BLOCK, new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.RARE)));

	public static final Block HADRON_ITEM_PORT_BLOCK = registerBlock("hadron_item_port",
		new net.valentin.obsidium.block.HadronColliderPortBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK)
			.strength(4.5f, 1200.0f).lightLevel(s -> s.getValue(net.valentin.obsidium.block.HadronColliderPortBlock.FORMED) ? 10 : 2),
			net.valentin.obsidium.block.HadronColliderPortBlock.PortType.ITEM));
	public static final Item HADRON_ITEM_PORT_ITEM = registerItem("hadron_item_port",
		new BlockItem(HADRON_ITEM_PORT_BLOCK, new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.RARE)));

	public static final Block HADRON_FLUID_PORT_BLOCK = registerBlock("hadron_fluid_port",
		new net.valentin.obsidium.block.HadronColliderPortBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK)
			.strength(4.5f, 1200.0f).lightLevel(s -> s.getValue(net.valentin.obsidium.block.HadronColliderPortBlock.FORMED) ? 10 : 2),
			net.valentin.obsidium.block.HadronColliderPortBlock.PortType.FLUID));
	public static final Item HADRON_FLUID_PORT_ITEM = registerItem("hadron_fluid_port",
		new BlockItem(HADRON_FLUID_PORT_BLOCK, new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.RARE)));

	public static final Item SINGULARITY_HADRON_ALLOY = registerItem("singularity_hadron_alloy",
		new net.valentin.obsidium.item.SingularityHadronAlloyItem(new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC)));

	public static final BlockEntityType<net.valentin.obsidium.block.OlympianHadronColliderBlockEntity> OLYMPIAN_HADRON_COLLIDER_BE = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "olympian_hadron_collider_be"),
		BlockEntityType.Builder.of(net.valentin.obsidium.block.OlympianHadronColliderBlockEntity::new, OLYMPIAN_HADRON_COLLIDER_BLOCK).build(null)
	);

	public static final MenuType<net.valentin.obsidium.block.OlympianHadronColliderMenu> OLYMPIAN_HADRON_COLLIDER_MENU = Registry.register(
		BuiltInRegistries.MENU,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "olympian_hadron_collider_menu"),
		new MenuType<>(net.valentin.obsidium.block.OlympianHadronColliderMenu::new, FeatureFlags.VANILLA_SET)
	);

	// Poseidon Abyssal Pump (2x2x2 End-Game Fluid Extractor Multiblock)
	public static final Block POSEIDON_ABYSSAL_PUMP_BLOCK = registerBlock("poseidon_abyssal_pump",
		new net.valentin.obsidium.block.PoseidonAbyssalPumpBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK)
			.strength(5.0f, 1500.0f).lightLevel(s -> s.getValue(net.valentin.obsidium.block.PoseidonAbyssalPumpBlock.FORMED) ? 12 : 4)));
	public static final Item POSEIDON_ABYSSAL_PUMP_ITEM = registerItem("poseidon_abyssal_pump",
		new BlockItem(POSEIDON_ABYSSAL_PUMP_BLOCK, new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC)));

	public static final Block POSEIDON_ABYSSAL_CASING_BLOCK = registerBlock("poseidon_abyssal_casing",
		new net.valentin.obsidium.block.PoseidonAbyssalCasingBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK)
			.strength(4.5f, 1200.0f).noOcclusion().lightLevel(s -> s.getValue(net.valentin.obsidium.block.PoseidonAbyssalCasingBlock.FORMED) ? 8 : 2)));
	public static final Item POSEIDON_ABYSSAL_CASING_ITEM = registerItem("poseidon_abyssal_casing",
		new BlockItem(POSEIDON_ABYSSAL_CASING_BLOCK, new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.RARE)));

	public static final BlockEntityType<net.valentin.obsidium.block.PoseidonAbyssalPumpBlockEntity> POSEIDON_ABYSSAL_PUMP_BE = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "poseidon_abyssal_pump_be"),
		BlockEntityType.Builder.of(net.valentin.obsidium.block.PoseidonAbyssalPumpBlockEntity::new, POSEIDON_ABYSSAL_PUMP_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.PoseidonAbyssalCasingBlockEntity> POSEIDON_ABYSSAL_CASING_BE = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "poseidon_abyssal_casing_be"),
		BlockEntityType.Builder.of(net.valentin.obsidium.block.PoseidonAbyssalCasingBlockEntity::new, POSEIDON_ABYSSAL_CASING_BLOCK).build(null)
	);

	public static final MenuType<net.valentin.obsidium.block.PoseidonAbyssalPumpMenu> POSEIDON_ABYSSAL_PUMP_MENU = Registry.register(
		BuiltInRegistries.MENU,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "poseidon_abyssal_pump_menu"),
		new MenuType<>(net.valentin.obsidium.block.PoseidonAbyssalPumpMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<net.valentin.obsidium.block.AegisSanctuaryWardMenu> AEGIS_SANCTUARY_WARD_MENU = Registry.register(
		BuiltInRegistries.MENU,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "aegis_sanctuary_ward_menu"),
		new MenuType<>(net.valentin.obsidium.block.AegisSanctuaryWardMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final Block AMBROSIA_FEEDER = registerBlock("ambrosia_feeder",
		new net.valentin.obsidium.block.AmbrosiaFeederBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(4.0f, 1200.0f).requiresCorrectToolForDrops()));
	public static final Item AMBROSIA_FEEDER_ITEM = registerItem("ambrosia_feeder", new BlockItem(AMBROSIA_FEEDER, new Item.Properties().fireResistant()));
	public static final BlockEntityType<net.valentin.obsidium.block.AmbrosiaFeederBlockEntity> AMBROSIA_FEEDER_BE = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "ambrosia_feeder_be"),
		BlockEntityType.Builder.of(net.valentin.obsidium.block.AmbrosiaFeederBlockEntity::new, AMBROSIA_FEEDER).build(null)
	);
	public static final MenuType<net.valentin.obsidium.block.AmbrosiaFeederMenu> AMBROSIA_FEEDER_MENU = Registry.register(
		BuiltInRegistries.MENU,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "ambrosia_feeder_menu"),
		new MenuType<>(net.valentin.obsidium.block.AmbrosiaFeederMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final Block PAN_HARVESTER = registerBlock("pan_pastoral_harvester",
		new net.valentin.obsidium.block.PanHarvesterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(4.0f, 1200.0f).requiresCorrectToolForDrops()));
	public static final Item PAN_HARVESTER_ITEM = registerItem("pan_pastoral_harvester", new BlockItem(PAN_HARVESTER, new Item.Properties().fireResistant()));
	public static final BlockEntityType<net.valentin.obsidium.block.PanHarvesterBlockEntity> PAN_HARVESTER_BE = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "pan_pastoral_harvester_be"),
		BlockEntityType.Builder.of(net.valentin.obsidium.block.PanHarvesterBlockEntity::new, PAN_HARVESTER).build(null)
	);
	public static final MenuType<net.valentin.obsidium.block.PanHarvesterMenu> PAN_HARVESTER_MENU = Registry.register(
		BuiltInRegistries.MENU,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "pan_pastoral_harvester_menu"),
		new MenuType<>(net.valentin.obsidium.block.PanHarvesterMenu::new, FeatureFlags.VANILLA_SET)
	);

	// Advanced Volcanite Quantum Teleporter Pads
	public static final Block BASIC_VOLCANIC_TELEPORTER_BLOCK = registerBlock("basic_volcanic_teleporter",
		new net.valentin.obsidium.block.VolcanicTeleporterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK)
			.strength(4.5f, 1200.0f).noOcclusion().lightLevel(s -> s.getValue(net.valentin.obsidium.block.VolcanicTeleporterBlock.ACTIVE) ? 12 : 3), false));
	public static final Item BASIC_VOLCANIC_TELEPORTER_ITEM = registerItem("basic_volcanic_teleporter",
		new BlockItem(BASIC_VOLCANIC_TELEPORTER_BLOCK, new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.RARE)));

	public static final Block VOLCANIC_CORE_TELEPORTER_BLOCK = registerBlock("volcanic_core_teleporter",
		new net.valentin.obsidium.block.VolcanicTeleporterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK)
			.strength(5.5f, 1800.0f).noOcclusion().lightLevel(s -> s.getValue(net.valentin.obsidium.block.VolcanicTeleporterBlock.ACTIVE) ? 15 : 5), true));
	public static final Item VOLCANIC_CORE_TELEPORTER_ITEM = registerItem("volcanic_core_teleporter",
		new BlockItem(VOLCANIC_CORE_TELEPORTER_BLOCK, new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC)));

	public static final BlockEntityType<net.valentin.obsidium.block.VolcanicTeleporterBlockEntity> BASIC_VOLCANIC_TELEPORTER_BE = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "basic_volcanic_teleporter_be"),
		BlockEntityType.Builder.of((pos, state) -> new net.valentin.obsidium.block.VolcanicTeleporterBlockEntity(pos, state, false), BASIC_VOLCANIC_TELEPORTER_BLOCK).build(null)
	);

	public static final BlockEntityType<net.valentin.obsidium.block.VolcanicTeleporterBlockEntity> VOLCANIC_CORE_TELEPORTER_BE = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanic_core_teleporter_be"),
		BlockEntityType.Builder.of((pos, state) -> new net.valentin.obsidium.block.VolcanicTeleporterBlockEntity(pos, state, true), VOLCANIC_CORE_TELEPORTER_BLOCK).build(null)
	);

	public static final MenuType<net.valentin.obsidium.block.VolcaniteTeleporterMenu> VOLCANIC_TELEPORTER_MENU = Registry.register(
		BuiltInRegistries.MENU,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanic_teleporter_menu"),
		new MenuType<>(net.valentin.obsidium.block.VolcaniteTeleporterMenu::new, FeatureFlags.VANILLA_SET)
	);
	public static final net.minecraft.world.level.levelgen.feature.Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration> VOLCANIC_CONE_FEATURE = Registry.register(
		BuiltInRegistries.FEATURE,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanic_cone"),
		new net.valentin.obsidium.worldgen.VolcanicConeFeature()
	);
	public static final net.minecraft.world.level.levelgen.feature.Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration> VOLCANITE_THERMAL_ORE_FEATURE = Registry.register(
		BuiltInRegistries.FEATURE,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "volcanite_thermal_ore"),
		new net.valentin.obsidium.worldgen.VolcaniteThermalOreFeature()
	);

	public static final MenuType<net.valentin.obsidium.menu.MagmaForgeMenu> MAGMA_FORGE_MENU = Registry.register(
		BuiltInRegistries.MENU, ResourceLocation.fromNamespaceAndPath("obsidium", "magma_forge_menu"),
		new MenuType<>(net.valentin.obsidium.menu.MagmaForgeMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET)
	);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializare Obsidium Mod!");
		net.valentin.obsidium.config.ModConfig.load();
		net.valentin.obsidium.sound.ModSounds.init();
		net.valentin.obsidium.command.ObsidiumCommands.register();
		net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
			CINDER_STALKER, net.valentin.obsidium.entity.CinderStalker.createAttributes()
		);
		net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
			TARTAREAN_HOUND, net.valentin.obsidium.entity.TartareanHound.createAttributes()
		);
		net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
			VOLCANIC_BEE, net.valentin.obsidium.entity.VolcanicBee.createAttributes()
		);

		net.valentin.obsidium.registry.ModWorldgenAndFuels.register();
		net.valentin.obsidium.registry.ModTransferStorages.register();
		net.valentin.obsidium.registry.ModEventsAndNetworking.register();
	}
}
