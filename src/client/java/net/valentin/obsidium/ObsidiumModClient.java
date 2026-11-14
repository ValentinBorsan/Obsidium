package net.valentin.obsidium;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.valentin.obsidium.client.VolcaniteFurnaceScreen;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.valentin.obsidium.network.GeyserJumpPayload;

public class ObsidiumModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		net.valentin.obsidium.client.ModTooltips.register();
		ClientTickEvents.END_CLIENT_TICK.register(net.valentin.obsidium.client.WaypointMiniMapHud::tick);
		HudRenderCallback.EVENT.register((graphics, tickCounter) ->
			net.valentin.obsidium.client.WaypointMiniMapHud.render(graphics, net.minecraft.client.Minecraft.getInstance()));

		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(ObsidiumMod.CINDER_STALKER, net.valentin.obsidium.client.CinderStalkerRenderer::new);
		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(ObsidiumMod.TARTAREAN_HOUND, net.valentin.obsidium.client.TartareanHoundRenderer::new);
		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(ObsidiumMod.VOLCANIC_BEE, net.valentin.obsidium.client.VolcanicBeeRenderer::new);
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlocks(
			net.minecraft.client.renderer.RenderType.cutoutMipped(),
			ObsidiumMod.MIDAS_GOLD_LEAVES, ObsidiumMod.ARES_IRON_LEAVES, ObsidiumMod.HERMES_COPPER_LEAVES, ObsidiumMod.HEPHAESTUS_VOLCANITE_LEAVES, ObsidiumMod.ELECTRUM_LEAVES, ObsidiumMod.TARTAREAN_NETHERITE_LEAVES,
			ObsidiumMod.CRONUS_LEAD_LEAVES, ObsidiumMod.ZEUS_TIN_LEAVES, ObsidiumMod.ARTEMIS_SILVER_LEAVES, ObsidiumMod.PYRETIC_TITAN_LEAVES
		);
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlocks(
			net.minecraft.client.renderer.RenderType.cutout(),
			ObsidiumMod.MIDAS_GOLD_SAPLING, ObsidiumMod.ARES_IRON_SAPLING, ObsidiumMod.HERMES_COPPER_SAPLING, ObsidiumMod.HEPHAESTUS_VOLCANITE_SAPLING, ObsidiumMod.ELECTRUM_SAPLING, ObsidiumMod.TARTAREAN_NETHERITE_SAPLING,
			ObsidiumMod.CRONUS_LEAD_SAPLING, ObsidiumMod.ZEUS_TIN_SAPLING, ObsidiumMod.ARTEMIS_SILVER_SAPLING, ObsidiumMod.PYRETIC_TITAN_SAPLING,
			ObsidiumMod.HADES_COAL_SAPLING, ObsidiumMod.ASTRAPE_REDSTONE_SAPLING, ObsidiumMod.POSEIDON_LAPIS_SAPLING, ObsidiumMod.APOLLO_QUARTZ_SAPLING,
			ObsidiumMod.DIONYSIAN_AMETHYST_SAPLING, ObsidiumMod.HYPERION_GLOWSTONE_SAPLING, ObsidiumMod.PLOUTON_EMERALD_SAPLING, ObsidiumMod.ADAMANTINE_DIAMOND_SAPLING,
			ObsidiumMod.GRAFTED_SAPLING_BLOCK, ObsidiumMod.DEMETRIAN_BONSAI_PLANTER_BLOCK, ObsidiumMod.VOLCANIC_BEEHIVE_BLOCK
		);
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(ObsidiumMod.SUPREME_VOLCANITE_GLASS_BLOCK, net.minecraft.client.renderer.RenderType.translucent());
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(ObsidiumMod.REINFORCED_TINTED_GLASS_BLOCK, net.minecraft.client.renderer.RenderType.translucent());
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(ObsidiumMod.VOLCANITE_STORAGE_PIPE_BLOCK, net.minecraft.client.renderer.RenderType.cutout());
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(ObsidiumMod.VOLCANITE_UNIVERSAL_CONDUIT_BLOCK, net.minecraft.client.renderer.RenderType.cutout());
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(ObsidiumMod.HERMES_ITEM_PIPE_BLOCK, net.minecraft.client.renderer.RenderType.cutout());
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(ObsidiumMod.NAIAD_FLUID_PIPE_BLOCK, net.minecraft.client.renderer.RenderType.cutout());
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(ObsidiumMod.VOLCANITE_ENERGY_WIRE_BLOCK, net.minecraft.client.renderer.RenderType.cutout());
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(ObsidiumMod.ADVANCED_VOLCANITE_ENERGY_WIRE_BLOCK, net.minecraft.client.renderer.RenderType.cutout());
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(ObsidiumMod.SUPREME_VOLCANITE_ENERGY_WIRE_BLOCK, net.minecraft.client.renderer.RenderType.cutout());
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(ObsidiumMod.SINGULARITY_VOLCANITE_ENERGY_WIRE_BLOCK, net.minecraft.client.renderer.RenderType.cutout());
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(ObsidiumMod.HEPHAESTEAN_CHARGING_PLATE_BLOCK, net.minecraft.client.renderer.RenderType.cutout());
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(ObsidiumMod.OLYMPIAN_ALTAR_BLOCK, net.minecraft.client.renderer.RenderType.cutout());
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(ObsidiumMod.ASTRAPE_RITUAL_OBELISK_BLOCK, net.minecraft.client.renderer.RenderType.cutout());
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(ObsidiumMod.ASTRAPE_ENERGY_PYLON_BLOCK, net.minecraft.client.renderer.RenderType.cutout());
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(ObsidiumMod.KERAUNOS_HV_CORE_BLOCK, net.minecraft.client.renderer.RenderType.cutout());
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(ObsidiumMod.VOLCANITE_LIQUID_STORAGE_TANK_BLOCK, net.minecraft.client.renderer.RenderType.cutout());
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(ObsidiumMod.VOLCANITE_SORTING_STATION_BLOCK, net.minecraft.client.renderer.RenderType.cutout());
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(ObsidiumMod.SUPREME_LIQUID_TANK_BLOCK, net.minecraft.client.renderer.RenderType.translucent());
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(ObsidiumMod.CREATIVE_LAVA_TANK, net.minecraft.client.renderer.RenderType.translucent());
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(ObsidiumMod.CREATIVE_WATER_TANK, net.minecraft.client.renderer.RenderType.translucent());
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.VOLCANITE_STORAGE_PIPE_BE, net.valentin.obsidium.client.VolcaniteStoragePipeRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.VOLCANITE_UNIVERSAL_CONDUIT_BE, net.valentin.obsidium.client.VolcaniteStoragePipeRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.HERMES_ITEM_PIPE_BE, net.valentin.obsidium.client.VolcaniteStoragePipeRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.NAIAD_FLUID_PIPE_BE, net.valentin.obsidium.client.NaiadFluidPipeRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.VOLCANITE_ENERGY_WIRE_BE, net.valentin.obsidium.client.VolcaniteEnergyWireRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.VOLCANITE_SORTING_STATION_BE, net.valentin.obsidium.client.VolcaniteSortingStationRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.SUPREME_LIQUID_TANK_BE, net.valentin.obsidium.client.LiquidTankRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.CREATIVE_LAVA_TANK_BE, net.valentin.obsidium.client.LiquidTankRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.CREATIVE_WATER_TANK_BE, net.valentin.obsidium.client.LiquidTankRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.VOLCANITE_ENERGY_CORE_BE, net.valentin.obsidium.client.VolcaniteEnergyCoreRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.KERAUNOS_HV_CORE_BLOCK_ENTITY, net.valentin.obsidium.client.KeraunosHVCoreRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.ASTRAPE_ENERGY_PYLON_BE, net.valentin.obsidium.client.AstrapeEnergyPylonRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.OLYMPIAN_ALTAR_BE, net.valentin.obsidium.client.OlympianAltarRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.ASTRAPE_RITUAL_OBELISK_BE, net.valentin.obsidium.client.AstrapeRitualObeliskRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.BASIC_VOLCANITE_ITEM_BARREL_BE, net.valentin.obsidium.client.VolcaniteItemBarrelRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.ADVANCED_VOLCANITE_ITEM_BARREL_BE, net.valentin.obsidium.client.VolcaniteItemBarrelRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.SUPREME_VOLCANITE_ITEM_BARREL_BE, net.valentin.obsidium.client.VolcaniteItemBarrelRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.DEMETRIAN_SAPLING_BARREL_BE, net.valentin.obsidium.client.DemetrianSaplingBarrelRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.DEMETRIAN_LUMBER_DEPOT_BE, net.valentin.obsidium.client.DemetrianLumberDepotRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.DEMETRIAN_BONSAI_PLANTER_BE, net.valentin.obsidium.client.DemetrianBonsaiPlanterRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.DIONYSIAN_DISTILLERY_BE, net.valentin.obsidium.client.DionysianDistilleryRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.VOLCANIC_DIESEL_GENERATOR_BE, net.valentin.obsidium.client.VolcanicDieselGeneratorRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.VOLCANITE_ENERGY_CELL_BE, net.valentin.obsidium.client.VolcaniteEnergyCellRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.VOLCANITE_ENERGY_UNIT_BE, net.valentin.obsidium.client.VolcaniteEnergyCellRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.OLYMPIAN_HADRON_COLLIDER_BE, net.valentin.obsidium.client.OlympianHadronColliderRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.BASIC_VOLCANIC_TELEPORTER_BE, net.valentin.obsidium.client.VolcanicTeleporterRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ObsidiumMod.VOLCANIC_CORE_TELEPORTER_BE, net.valentin.obsidium.client.VolcanicTeleporterRenderer::new);

		// Dynamic Pipe Color according to ITEMS (Green) or LIQUIDS (Cyan) mode
		net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
			if (world != null && pos != null) {
				net.minecraft.world.level.block.entity.BlockEntity be = world.getBlockEntity(pos);
				if (be instanceof net.valentin.obsidium.block.VolcaniteStoragePipeBlockEntity pipeBe) {
					if (pipeBe.hasAnyModule()) {
						int type = pipeBe.getPrimaryTransferType();
						return (type == 1) ? 0x00D4FF : 0x00FF66;
					}
				}
			}
			return -1;
		}, ObsidiumMod.VOLCANITE_STORAGE_PIPE_BLOCK);

		// Cutout render layer for Volcanic Ore Crops
		net.minecraft.client.renderer.RenderType cutout = net.minecraft.client.renderer.RenderType.cutout();
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlocks(cutout,
			ObsidiumMod.VOLCANITE_CROP,
			ObsidiumMod.IRON_CROP,
			ObsidiumMod.GOLD_CROP,
			ObsidiumMod.COPPER_CROP,
			ObsidiumMod.COAL_CROP,
			ObsidiumMod.LAPIS_CROP,
			ObsidiumMod.REDSTONE_CROP,
			ObsidiumMod.DIAMOND_CROP,
			ObsidiumMod.NETHERITE_CROP,
			ObsidiumMod.TIN_CROP,
			ObsidiumMod.LEAD_CROP,
			ObsidiumMod.SILVER_CROP,
			ObsidiumMod.VOLCANITE_MODULAR_CHEST_BLOCK,
			ObsidiumMod.VOLCANITE_NETWORK_NODE_EMITTER_BLOCK,
			ObsidiumMod.VOLCANITE_NETWORK_NODE_RECEIVER_BLOCK
		);
		
		MenuScreens.register(ObsidiumMod.VOLCANITE_FURNACE_MENU, VolcaniteFurnaceScreen::new);
		MenuScreens.register(ObsidiumMod.VOLCANITE_WORKTABLE_MENU, net.valentin.obsidium.client.VolcaniteWorktableScreen::new);
		MenuScreens.register(ObsidiumMod.LIQUID_TANK_MENU, net.valentin.obsidium.client.LiquidTankScreen::new);
		MenuScreens.register(ObsidiumMod.CREATIVE_LAVA_TANK_MENU, net.valentin.obsidium.client.CreativeLavaTankScreen::new);
		MenuScreens.register(ObsidiumMod.CREATIVE_WATER_TANK_MENU, net.valentin.obsidium.client.CreativeWaterTankScreen::new);
		MenuScreens.register(ObsidiumMod.MAGMA_FORGE_MENU, net.valentin.obsidium.screen.MagmaForgeScreen::new);
		MenuScreens.register(ObsidiumMod.GEOTHERMAL_ARBORIST_MENU, net.valentin.obsidium.client.GeothermalArboristScreen::new);
		MenuScreens.register(ObsidiumMod.VOLCANITE_STEAM_GENERATOR_MENU, net.valentin.obsidium.client.VolcaniteSteamGeneratorScreen::new);
		MenuScreens.register(ObsidiumMod.VOLCANITE_GEOTHERMAL_GENERATOR_MENU, net.valentin.obsidium.client.VolcaniteGeothermalGeneratorScreen::new);
		MenuScreens.register(ObsidiumMod.SUPREME_VOLCANIC_GENERATOR_MENU, net.valentin.obsidium.client.SupremeVolcanicGeneratorScreen::new);
		MenuScreens.register(ObsidiumMod.VOLCANIC_DISTILLATION_CHAMBER_MENU, net.valentin.obsidium.client.VolcanicDistillationChamberScreen::new);
		MenuScreens.register(ObsidiumMod.DIONYSIAN_DISTILLERY_MENU, net.valentin.obsidium.client.DionysianDistilleryScreen::new);
		MenuScreens.register(ObsidiumMod.VOLCANIC_DIESEL_GENERATOR_MENU, net.valentin.obsidium.client.VolcanicDieselGeneratorScreen::new);
		MenuScreens.register(ObsidiumMod.DIONYSIAN_PRESS_MENU, net.valentin.obsidium.client.DionysianPressScreen::new);
		MenuScreens.register(ObsidiumMod.ADVANCED_DIONYSIAN_PRESS_MENU, net.valentin.obsidium.client.AdvancedDionysianPressScreen::new);
		MenuScreens.register(ObsidiumMod.DEMETRIAN_BIOMASS_GENERATOR_MENU, net.valentin.obsidium.client.DemetrianBiomassGeneratorScreen::new);
		MenuScreens.register(ObsidiumMod.DEMETRIAN_COMPOSTER_MENU, net.valentin.obsidium.client.DemetrianComposterScreen::new);
		MenuScreens.register(ObsidiumMod.DIONYSIAN_AGING_BARREL_MENU, net.valentin.obsidium.client.DionysianAgingBarrelScreen::new);
		MenuScreens.register(ObsidiumMod.DEMETRIAN_SAPLING_BARREL_MENU, net.valentin.obsidium.client.DemetrianSaplingBarrelScreen::new);
		MenuScreens.register(ObsidiumMod.DEMETRIAN_LUMBER_DEPOT_MENU, net.valentin.obsidium.client.DemetrianLumberDepotScreen::new);
		MenuScreens.register(ObsidiumMod.VOLCANITE_STORAGE_UNIT_MENU, net.valentin.obsidium.client.VolcaniteStorageUnitScreen::new);
		MenuScreens.register(ObsidiumMod.VOLCANITE_STORAGE_DRIVE_MENU, net.valentin.obsidium.client.VolcaniteStorageDriveScreen::new);
		MenuScreens.register(ObsidiumMod.VOLCANITE_PIPE_MODULE_MENU, net.valentin.obsidium.client.VolcanitePipeModuleScreen::new);
		MenuScreens.register(ObsidiumMod.HERMES_FILTER_MENU, net.valentin.obsidium.client.HermesFilterScreen::new);
		MenuScreens.register(ObsidiumMod.VOLCANITE_STORAGE_TERMINAL_MENU, net.valentin.obsidium.client.VolcaniteStorageTerminalScreen::new);
		MenuScreens.register(ObsidiumMod.VOLCANITE_LIQUID_STORAGE_TANK_MENU, net.valentin.obsidium.client.VolcaniteLiquidStorageTankScreen::new);
		MenuScreens.register(ObsidiumMod.VOLCANITE_SORTING_STATION_MENU, net.valentin.obsidium.client.VolcaniteSortingStationScreen::new);
		MenuScreens.register(ObsidiumMod.VOLCANITE_BATTERY_MENU, net.valentin.obsidium.client.VolcaniteBatteryScreen::new);
		MenuScreens.register(ObsidiumMod.KERAUNOS_HV_CORE_MENU, net.valentin.obsidium.client.KeraunosHVCoreScreen::new);
		MenuScreens.register(ObsidiumMod.ADVANCED_VOLCANITE_FURNACE_MENU, net.valentin.obsidium.client.AdvancedVolcaniteFurnaceScreen::new);
		MenuScreens.register(ObsidiumMod.VOLCANITE_GRINDER_MENU, net.valentin.obsidium.client.VolcaniteGrinderScreen::new);
		MenuScreens.register(ObsidiumMod.VOLCANITE_GENERATOR_MENU, net.valentin.obsidium.client.VolcaniteGeneratorScreen::new);
		MenuScreens.register(ObsidiumMod.VOLCANITE_LAVA_PUMP_MENU, net.valentin.obsidium.client.VolcaniteLavaPumpScreen::new);
		MenuScreens.register(ObsidiumMod.BASIC_BACKPACK_MENU, net.valentin.obsidium.client.BasicBackpackScreen::new);
		MenuScreens.register(ObsidiumMod.ADVANCED_BACKPACK_MENU, net.valentin.obsidium.client.AdvancedBackpackScreen::new);
		MenuScreens.register(ObsidiumMod.HEPHAESTEAN_ARMORY_MENU, net.valentin.obsidium.screen.HephaesteanArmoryScreen::new);
		MenuScreens.register(ObsidiumMod.TARTARUS_PISTON_ENGINE_MENU, net.valentin.obsidium.client.TartarusPistonEngineScreen::new);
		MenuScreens.register(ObsidiumMod.DIONYSIAN_BIO_CALDERA_MENU, net.valentin.obsidium.client.DionysianBioCalderaScreen::new);
		MenuScreens.register(ObsidiumMod.HYDROTHERMAL_VITRIFIER_MENU, net.valentin.obsidium.client.HydrothermalVitrifierScreen::new);
		MenuScreens.register(ObsidiumMod.MAGMATIC_CRUCIBLE_MENU, net.valentin.obsidium.client.MagmaticCrucibleScreen::new);
		MenuScreens.register(ObsidiumMod.HYDROTHERMAL_CONDENSER_MENU, net.valentin.obsidium.client.HydrothermalCondenserScreen::new);
		MenuScreens.register(ObsidiumMod.HYDROTHERMAL_LITHOFIER_MENU, net.valentin.obsidium.client.HydrothermalLithofierScreen::new);
		MenuScreens.register(ObsidiumMod.VOLCANITE_ITEM_BARREL_MENU, net.valentin.obsidium.client.VolcaniteItemBarrelScreen::new);
		MenuScreens.register(ObsidiumMod.VOLCANITE_MODULAR_CHEST_MENU, net.valentin.obsidium.client.VolcaniteModularChestScreen::new);
		MenuScreens.register(ObsidiumMod.VOLCANITE_CRAFTER_INTERFACE_MENU, net.valentin.obsidium.client.VolcaniteCrafterInterfaceScreen::new);
		MenuScreens.register(ObsidiumMod.THANATOS_EXECUTIONER_MENU, net.valentin.obsidium.client.ThanatosExecutionerScreen::new);
		MenuScreens.register(ObsidiumMod.APOLLO_SOLAR_GENERATOR_MENU, net.valentin.obsidium.client.ApolloSolarGeneratorScreen::new);
		MenuScreens.register(ObsidiumMod.AEOLUS_STORM_TURBINE_MENU, net.valentin.obsidium.client.AeolusStormTurbineScreen::new);
		MenuScreens.register(ObsidiumMod.OLYMPIAN_HADRON_COLLIDER_MENU, net.valentin.obsidium.client.OlympianHadronColliderScreen::new);
		MenuScreens.register(ObsidiumMod.VOLCANIC_TELEPORTER_MENU, net.valentin.obsidium.client.VolcaniteTeleporterScreen::new);

		ClientPlayNetworking.registerGlobalReceiver(net.valentin.obsidium.network.TeleportNetworkSyncPayload.TYPE, (payload, context) -> {
			context.client().execute(() -> net.valentin.obsidium.client.ClientTeleportCache.update(payload));
		});
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			net.valentin.obsidium.client.ClientTeleportCache.clear();
		});

		KeyMapping geyserJumpKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.obsidium.geyser_jump",
			com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_G,
			"category.obsidium.keys"
		));

		KeyMapping muteSoundsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.obsidium.mute_machine_sounds",
			com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_M,
			"category.obsidium.keys"
		));

		KeyMapping sortChestKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.obsidium.sort_chest",
			com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_R,
			"category.obsidium.keys"
		));

		net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) {
				net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents.afterKeyPress(screen).register((scr, key, scancode, modifiers) -> {
					if (sortChestKey.matches(key, scancode)) {
						// Do not sort if typing in a text box (e.g. search bar)
						if (scr.getFocused() instanceof net.minecraft.client.gui.components.EditBox editBox && editBox.canConsumeInput()) {
							return;
						}
						boolean descending = !net.minecraft.client.gui.screens.Screen.hasShiftDown();
						ClientPlayNetworking.send(new net.valentin.obsidium.network.SortContainerPayload(descending));
					}
				});
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (geyserJumpKey.consumeClick()) {
				if (client.player != null && !client.player.onGround()) {
					ClientPlayNetworking.send(new GeyserJumpPayload());
				}
			}
			while (muteSoundsKey.consumeClick()) {
				net.valentin.obsidium.sound.MachineSoundConfig.toggleMute();
			}
		});

		// Dynamic Model Predicates: Bow of Artemis & Aegis of Athena
		net.minecraft.client.renderer.item.ItemProperties.register(
				ObsidiumMod.BOW_OF_ARTEMIS,
				net.minecraft.resources.ResourceLocation.withDefaultNamespace("pull"),
				(stack, level, entity, seed) -> {
					if (entity == null) return 0.0F;
					return entity.getUseItem() != stack ? 0.0F : (float)(stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F;
				}
		);
		net.minecraft.client.renderer.item.ItemProperties.register(
				ObsidiumMod.BOW_OF_ARTEMIS,
				net.minecraft.resources.ResourceLocation.withDefaultNamespace("pulling"),
				(stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F
		);
		net.minecraft.client.renderer.item.ItemProperties.register(
				ObsidiumMod.AEGIS_OF_ATHENA,
				net.minecraft.resources.ResourceLocation.withDefaultNamespace("blocking"),
				(stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F
		);
	}
}
