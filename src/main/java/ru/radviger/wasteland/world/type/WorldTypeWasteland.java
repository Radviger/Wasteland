package com.legacy.wasteland.world.type;

import com.legacy.wasteland.world.ChunkGeneratorWasteland;
import com.legacy.wasteland.world.util.WastelandGenLayer;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.ChunkGeneratorSettings;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.GenLayerBiomeEdge;
import net.minecraft.world.gen.layer.GenLayerZoom;

public class WorldTypeWasteland extends WorldType {
    public WorldTypeWasteland() {
        super("wasteland");
    }

    @Override
    public BiomeProvider getBiomeProvider(World world) {
        return new BiomeProvider(world.getWorldInfo());
    }

    @Override
    public boolean isCustomizable() {
        return false;
    }

    @Override
    public IChunkGenerator getChunkGenerator(World world, String generatorOptions) {
        return new ChunkGeneratorWasteland(world, world.getSeed(), world.getWorldInfo().isMapFeaturesEnabled(), generatorOptions);
    }

    @Override
    public GenLayer getBiomeLayer(long worldSeed, GenLayer parentLayer, ChunkGeneratorSettings chunkSettings) {
        WastelandGenLayer ret = new WastelandGenLayer(200L, parentLayer);
        GenLayer zoom = GenLayerZoom.magnify(1000L, ret, 2);
        return new GenLayerBiomeEdge(1000L, zoom);
    }
}
