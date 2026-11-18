package net.valentin.mythicrails;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.MinecartRenderer;

public final class MythicRailsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        net.minecraft.client.gui.screens.MenuScreens.register(MythicRails.FREIGHT_MENU, FreightCartScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(MythicRails.STATION_MENU, FreightStationScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(MythicRails.UTILITY_STATION_MENU, RailUtilityStationScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(MythicRails.RAIL_CONTROL_MENU, RailControlScreen::new);
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlocks(
                net.minecraft.client.renderer.RenderType.cutout(),
                MythicRails.OBSIDIAN_RAIL,
                MythicRails.EMBER_RAIL,
                MythicRails.VOLCANITE_SPEED_RAIL,
                MythicRails.OBSIDIAN_DETECTOR_RAIL,
                MythicRails.SWITCH_RAIL);
        EntityRendererRegistry.register(MythicRails.FREIGHT_CART, context ->
                new MinecartRenderer<HermesFreightCart>(context, ModelLayers.CHEST_MINECART) {
                    @Override
                    public net.minecraft.resources.ResourceLocation getTextureLocation(HermesFreightCart cart) {
                        return MythicRails.id("textures/entity/hermes_freight_cart.png");
                    }
                });
    }
}
