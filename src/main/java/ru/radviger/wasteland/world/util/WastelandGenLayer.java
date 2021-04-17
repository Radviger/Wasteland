package com.legacy.wasteland.world.util;

import com.legacy.wasteland.config.WastelandConfig;
import com.legacy.wasteland.world.WastelandWorld;
import net.minecraft.util.WeightedRandom;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;
import net.minecraftforge.common.BiomeManager.BiomeEntry;

import java.util.ArrayList;
import java.util.List;

public class WastelandGenLayer extends GenLayer {
    private List<BiomeEntry> biomes = new ArrayList<>();

    public WastelandGenLayer(long worldGenSeed, GenLayer parentLayer) {
        super(worldGenSeed);
        this.parent = parentLayer;

        if (WastelandConfig.worldgen.shouldSpawnCities) {
            for (int i = 0; i < 1; ++i) {
                this.biomes.add(new BiomeEntry(WastelandWorld.BIOME_CITY, 10));
            }
        }

        for (int i = 0; i < 10; ++i) {
            if (WastelandConfig.biomes.wastelandForestEnabled) {
                this.biomes.add(new BiomeEntry(WastelandWorld.BIOME_FOREST, 10));
            }
            if (WastelandConfig.biomes.wastelandMountainsEnabled) {
                this.biomes.add(new BiomeEntry(WastelandWorld.BIOME_MOUNTAINS, 10));
            }
        }

        for (int i = 0; i < 40; ++i) {
            this.biomes.add(new BiomeEntry(WastelandWorld.BIOME_GENERAL, 10));
        }

    }

    @Override
    public int[] getInts(int x, int z, int width, int depth) {
        int[] cache = IntCache.getIntCache(width * depth);

        for (int dx = 0; dx < depth; ++dx) {
            for (int dz = 0; dz < width; ++dz) {
                this.initChunkSeed((long) (dz + x), (long) (dx + z));
                BiomeEntry biomeEntry = WeightedRandom.getRandomItem(this.biomes, (int) (this.nextLong((long) (WeightedRandom.getTotalWeight(this.biomes) / 10)) * 10L));
                cache[dz + dx * width] = Biome.getIdForBiome(biomeEntry.biome);
            }
        }

        return cache;
    }
}
