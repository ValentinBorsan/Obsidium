package net.valentin.obsidium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.valentin.obsidium.entity.TartareanHound;

public class TartareanHoundRenderer extends MobRenderer<TartareanHound, WolfModel<TartareanHound>> {
    private static final ResourceLocation WILD_TEXTURE = ResourceLocation.parse("minecraft:textures/entity/wolf/wolf_rusty.png");
    private static final ResourceLocation TAME_TEXTURE = ResourceLocation.parse("minecraft:textures/entity/wolf/wolf_ashen.png");

    public TartareanHoundRenderer(EntityRendererProvider.Context context) {
        super(context, new WolfModel<>(context.bakeLayer(ModelLayers.WOLF)), 0.6F);
    }

    @Override
    protected void scale(TartareanHound hound, PoseStack poseStack, float partialTickTime) {
        // Tartarean hound is 1.2x bigger and more imposing than a normal wolf
        float scale = hound.isBaby() ? 0.65F : 1.22F;
        poseStack.scale(scale, scale, scale);
        super.scale(hound, poseStack, partialTickTime);
    }

    @Override
    public ResourceLocation getTextureLocation(TartareanHound entity) {
        return entity.isTame() ? TAME_TEXTURE : WILD_TEXTURE;
    }
}

