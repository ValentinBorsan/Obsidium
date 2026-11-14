package net.valentin.obsidium.client;

import net.minecraft.client.model.BeeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.entity.VolcanicBee;

public class VolcanicBeeRenderer extends MobRenderer<VolcanicBee, BeeModel<VolcanicBee>> {
    private static final ResourceLocation ANGRY_NECTAR = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/entity/volcanic_bee_angry_nectar.png");
    private static final ResourceLocation ANGRY = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/entity/volcanic_bee_angry.png");
    private static final ResourceLocation NECTAR = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/entity/volcanic_bee_nectar.png");
    private static final ResourceLocation NORMAL = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "textures/entity/volcanic_bee.png");

    public VolcanicBeeRenderer(EntityRendererProvider.Context context) {
        super(context, new BeeModel<>(context.bakeLayer(ModelLayers.BEE)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(VolcanicBee bee) {
        if (bee.isAngry()) {
            return bee.hasNectar() ? ANGRY_NECTAR : ANGRY;
        } else {
            return bee.hasNectar() ? NECTAR : NORMAL;
        }
    }
}

