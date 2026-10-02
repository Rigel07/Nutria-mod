package com.nutriamod;

import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, NutriaMod.MODID);

    public static final RegistryObject<Item> OTTER_SPAWN_EGG = ITEMS.register("otter_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.OTTER, 0x6B4A2B, 0xE2CEA8, new Item.Properties()));
}
