package com.legacy.wasteland.world;

import com.legacy.wasteland.config.WastelandConfig;
import com.legacy.wasteland.world.biome.BiomeWasteland;
import com.legacy.wasteland.world.biome.BiomeWastelandCity;
import com.legacy.wasteland.world.biome.BiomeWastelandForest;
import com.legacy.wasteland.world.biome.BiomeWastelandMountains;
import com.legacy.wasteland.world.type.WorldTypeWasteland;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.BiomeProperties;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Arrays;

public class WastelandWorld {
    public static WorldType WORLD_TYPE;
    public static Biome BIOME_GENERAL, BIOME_MOUNTAINS, BIOME_FOREST, BIOME_CITY;

    public static void init() {
        BIOME_GENERAL = registerBiome("wasteland", new BiomeWasteland(new BiomeProperties("Wasteland").setTemperature(2.5F).setBaseHeight(0.1F).setHeightVariation(0.05F).setWaterColor(14728553).setRainfall(0.5F)));
        if (WastelandConfig.biomes.wastelandMountainsEnabled) {
            BIOME_MOUNTAINS = registerBiome("wasteland_mountains", new BiomeWastelandMountains(new BiomeProperties("Wasteland Mountains").setTemperature(2F).setBaseHeight(1F).setHeightVariation(0.5F).setWaterColor(0x9c7c13).setRainfall(0.5F)));
        }
        if (WastelandConfig.biomes.wastelandForestEnabled) {
            BIOME_FOREST = registerBiome("wasteland_forest", new BiomeWastelandForest(new BiomeProperties("Wasteland Forest").setTemperature(2.2F).setBaseHeight(0.1F).setHeightVariation(0.05F).setWaterColor(0xa4b34f).setRainfall(0.5F)));
        }
        if (WastelandConfig.biomes.wastelandCityEnabled) {
            BIOME_CITY = registerBiome("wasteland_city", new BiomeWastelandCity(new BiomeProperties("Wasteland City").setTemperature(2.5F).setBaseHeight(0.09F).setHeightVariation(0.05F).setWaterColor(0x8f98b3).setRainfall(0.5F)));
        }
        WORLD_TYPE = new WorldTypeWasteland();
    }

    private static Biome registerBiome(String name, Biome biome) {
        biome.setRegistryName(name);
        ForgeRegistries.BIOMES.register(biome);
        BiomeDictionary.addTypes(biome, BiomeDictionary.Type.DEAD, BiomeDictionary.Type.DRY, BiomeDictionary.Type.WASTELAND);
        return biome;
    }

    public static boolean isWasteland(World world) {
        return Arrays.binarySearch(WastelandConfig.general.enabledWorlds, world.provider.getDimensionType().getName()) >= 0;
    }
}
