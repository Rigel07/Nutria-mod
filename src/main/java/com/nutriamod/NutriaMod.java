package com.nutriamod;

import com.nutriamod.entity.OtterEntity;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(NutriaMod.MODID)
public class NutriaMod {
    public static final String MODID = "nutriamod";

    public NutriaMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModEntities.ENTITY_TYPES.register(modBus);
        ModItems.ITEMS.register(modBus);

        modBus.addListener(this::registerAttributes);
        modBus.addListener(this::registerSpawnPlacements);
        modBus.addListener(this::addCreativeTabItems);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.OTTER.get(), OtterEntity.createAttributes().build());
    }

    private void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(
                ModEntities.OTTER.get(),
                SpawnPlacements.Type.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                OtterEntity::checkOtterSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.OTTER_SPAWN_EGG);
        }
    }
}
