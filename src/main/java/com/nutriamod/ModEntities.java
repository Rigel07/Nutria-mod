package com.nutriamod;

import com.nutriamod.entity.OtterEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, NutriaMod.MODID);

    public static final RegistryObject<EntityType<OtterEntity>> OTTER = ENTITY_TYPES.register("otter",
            () -> EntityType.Builder.of(OtterEntity::new, MobCategory.CREATURE)
                    .sized(0.7F, 0.5F)
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(NutriaMod.MODID, "otter").toString()));
}
