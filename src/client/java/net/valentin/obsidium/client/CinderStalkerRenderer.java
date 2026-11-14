package net.valentin.obsidium.client;

import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.valentin.obsidium.entity.CinderStalker;

public class CinderStalkerRenderer extends MobRenderer<CinderStalker, SlimeModel<CinderStalker>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("minecraft:textures/entity/slime/magmacube.png");

    public CinderStalkerRenderer(EntityRendererProvider.Context context) {
        super(context, new SlimeModel<>(context.bakeLayer(ModelLayers.SLIME)), 0.45F);
    }

    @Override
    public ResourceLocation getTextureLocation(CinderStalker entity) {
        return TEXTURE;
    }
}
