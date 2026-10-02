package com.nutriamod.client;

import com.nutriamod.NutriaMod;
import com.nutriamod.entity.OtterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class OtterRenderer extends MobRenderer<OtterEntity, OtterModel<OtterEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(NutriaMod.MODID, "textures/entity/otter.png");

    public OtterRenderer(EntityRendererProvider.Context context) {
        super(context, new OtterModel<>(context.bakeLayer(OtterModel.LAYER_LOCATION)), 0.35F);
    }

    @Override
    public ResourceLocation getTextureLocation(OtterEntity entity) {
        return TEXTURE;
    }
}
