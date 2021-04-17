package com.legacy.wasteland.world;

import com.legacy.wasteland.Wasteland;
import com.legacy.wasteland.config.WastelandConfig;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.*;
import net.minecraft.world.gen.ChunkGeneratorSettings.Factory;
import net.minecraft.world.gen.feature.WorldGenDungeons;
import net.minecraft.world.gen.structure.MapGenMineshaft;
import net.minecraft.world.gen.structure.MapGenStronghold;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.terraingen.InitMapGenEvent.EventType;
import net.minecraftforge.event.terraingen.InitNoiseGensEvent.ContextOverworld;
import net.minecraftforge.event.terraingen.TerrainGen;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

import static net.minecraftforge.event.terraingen.PopulateChunkEvent.Populate;

public class ChunkGeneratorWasteland implements IChunkGenerator {
    private MinecraftServer server;
    private IBlockState stoneBlock = Blocks.STONE.getDefaultState();
    private IBlockState surfaceBlock = Wasteland.SURFACE_BLOCK;
    private NoiseGeneratorOctaves minLimitPerlinNoise;
    private NoiseGeneratorOctaves maxLimitPerlinNoise;
    private NoiseGeneratorOctaves mainPerlinNoise;
    private NoiseGeneratorPerlin surfaceNoise;
    private NoiseGeneratorOctaves scaleNoise;
    private NoiseGeneratorOctaves depthNoise;
    private NoiseGeneratorOctaves forestNoise;
    private final World world;
    private final Random rand;
    private final boolean enableFeatures;
    private final WorldType terrainType;
    private final double[] heightMap;
    private final float[] biomeWeights;
    private double[] depthBuffer = new double[256];
    private ChunkGeneratorSettings settings;
    private MapGenBase caveGenerator;
    private MapGenBase ravineGenerator;
    private MapGenStructure strongholdGenerator;
    private MapGenStructure villageGenerator;
    private MapGenStructure mineshaftGenerator;
    private Biome[] biomesForGeneration;
    private double[] mainNoiseRegion;
    private double[] minLimitRegion;
    private double[] maxLimitRegion;
    private double[] depthRegion;

    public ChunkGeneratorWasteland(World world, long seed, boolean enableFeatures, String settings) {
        this.server = world.getMinecraftServer();
        this.caveGenerator = TerrainGen.getModdedMapGen(new MapGenCaves(), EventType.CAVE);
        this.ravineGenerator = TerrainGen.getModdedMapGen(new MapGenRavine(), EventType.RAVINE);
        this.strongholdGenerator = (MapGenStructure) TerrainGen.getModdedMapGen(new MapGenStronghold(), EventType.STRONGHOLD);
        this.villageGenerator = (MapGenStructure) TerrainGen.getModdedMapGen(new MapGenVillage(), EventType.VILLAGE);
        this.mineshaftGenerator = (MapGenStructure) TerrainGen.getModdedMapGen(new MapGenMineshaft(), EventType.MINESHAFT);
        this.world = world;
        this.enableFeatures = enableFeatures;
        this.terrainType = world.getWorldInfo().getTerrainType();
        this.rand = new Random(seed);
        this.minLimitPerlinNoise = new NoiseGeneratorOctaves(this.rand, 16);
        this.maxLimitPerlinNoise = new NoiseGeneratorOctaves(this.rand, 16);
        this.mainPerlinNoise = new NoiseGeneratorOctaves(this.rand, 8);
        this.surfaceNoise = new NoiseGeneratorPerlin(this.rand, 4);
        this.scaleNoise = new NoiseGeneratorOctaves(this.rand, 10);
        this.depthNoise = new NoiseGeneratorOctaves(this.rand, 16);
        this.forestNoise = new NoiseGeneratorOctaves(this.rand, 8);
        this.heightMap = new double[825];
        this.biomeWeights = new float[25];

        for (int ctx = -2; ctx <= 2; ++ctx) {
            for (int j = -2; j <= 2; ++j) {
                float f = 10.0F / MathHelper.sqrt((float) (ctx * ctx + j * j) + 0.2F);
                this.biomeWeights[ctx + 2 + (j + 2) * 5] = f;
            }
        }

        if (settings != null) {
            this.settings = Factory.jsonToFactory(settings).build();
            world.setSeaLevel(this.settings.seaLevel);
        }

        ContextOverworld ctx = new ContextOverworld(this.minLimitPerlinNoise, this.maxLimitPerlinNoise, this.mainPerlinNoise, this.surfaceNoise, this.scaleNoise, this.depthNoise, this.forestNoise);
        ctx = TerrainGen.getModdedNoiseGenerators(world, this.rand, ctx);
        this.minLimitPerlinNoise = ctx.getLPerlin1();
        this.maxLimitPerlinNoise = ctx.getLPerlin2();
        this.mainPerlinNoise = ctx.getPerlin();
        this.surfaceNoise = ctx.getHeight();
        this.scaleNoise = ctx.getScale();
        this.depthNoise = ctx.getDepth();
        this.forestNoise = ctx.getForest();
    }

    public void setBlocksInChunk(int x, int z, ChunkPrimer primer) {
        this.biomesForGeneration = this.world.getBiomeProvider().getBiomesForGeneration(this.biomesForGeneration, x * 4 - 2, z * 4 - 2, 10, 10);
        this.generateHeightmap(x * 4, 0, z * 4);

        for (int i = 0; i < 4; ++i) {
            int j = i * 5;
            int k = (i + 1) * 5;

            for (int l = 0; l < 4; ++l) {
                int i1 = (j + l) * 33;
                int j1 = (j + l + 1) * 33;
                int k1 = (k + l) * 33;
                int l1 = (k + l + 1) * 33;

                for (int i2 = 0; i2 < 32; ++i2) {
                    double d1 = this.heightMap[i1 + i2];
                    double d2 = this.heightMap[j1 + i2];
                    double d3 = this.heightMap[k1 + i2];
                    double d4 = this.heightMap[l1 + i2];
                    double d5 = (this.heightMap[i1 + i2 + 1] - d1) * 0.125D;
                    double d6 = (this.heightMap[j1 + i2 + 1] - d2) * 0.125D;
                    double d7 = (this.heightMap[k1 + i2 + 1] - d3) * 0.125D;
                    double d8 = (this.heightMap[l1 + i2 + 1] - d4) * 0.125D;

                    for (int j2 = 0; j2 < 8; ++j2) {
                        double d10 = d1;
                        double d11 = d2;
                        double d12 = (d3 - d1) * 0.25D;
                        double d13 = (d4 - d2) * 0.25D;

                        for (int k2 = 0; k2 < 4; ++k2) {
                            double d16 = (d11 - d10) * 0.25D;
                            double lvt_45_1_ = d10 - d16;

                            for (int l2 = 0; l2 < 4; ++l2) {
                                int localX = i * 4 + k2;
                                int localY = i2 * 8 + j2;
                                int localZ = l * 4 + l2;
                                if ((lvt_45_1_ += d16) > 0) {
                                    primer.setBlockState(localX, localY, localZ, stoneBlock);
                                } else if (localY < this.settings.seaLevel) {
                                    primer.setBlockState(localX, localY, localZ, surfaceBlock);
                                }
                            }

                            d10 += d12;
                            d11 += d13;
                        }

                        d1 += d5;
                        d2 += d6;
                        d3 += d7;
                        d4 += d8;
                    }
                }
            }
        }

    }

    public void replaceBiomeBlocks(int x, int z, ChunkPrimer primer, Biome[] biomes) {
        if (!net.minecraftforge.event.ForgeEventFactory.onReplaceBiomeBlocks(this, x, z, primer, this.world)) return;
        double d0 = 0.03125D;
        this.depthBuffer = this.surfaceNoise.getRegion(this.depthBuffer, (double) (x * 16), (double) (z * 16), 16, 16, 0.0625D, 0.0625D, 1.0D);

        for (int dx = 0; dx < 16; ++dx) {
            for (int dz = 0; dz < 16; ++dz) {
                Biome biome = biomes[dz + dx * 16];
                biome.genTerrainBlocks(this.world, this.rand, primer, x * 16 + dx, z * 16 + dz, this.depthBuffer[dz + dx * 16]);
            }
        }
    }

    @Override
    public Chunk generateChunk(int x, int z) {
        this.rand.setSeed((long) x * 341873128712L + (long) z * 132897987541L);
        ChunkPrimer primer = new ChunkPrimer();
        this.setBlocksInChunk(x, z, primer);
        this.biomesForGeneration = this.world.getBiomeProvider().getBiomes(this.biomesForGeneration, x * 16, z * 16, 16, 16);

        for (int chunk = 0; chunk < this.biomesForGeneration.length; ++chunk) {
            if (this.biomesForGeneration[chunk] == Biomes.OCEAN || this.biomesForGeneration[chunk] == Biomes.RIVER) {
                this.biomesForGeneration[chunk] = WastelandWorld.BIOME_GENERAL;
            }
        }

        this.replaceBiomeBlocks(x, z, primer, this.biomesForGeneration);
        if (this.settings.useCaves) {
            this.caveGenerator.generate(this.world, x, z, primer);
        }

        if (this.enableFeatures) {
            if (this.settings.useMineShafts) {
                this.mineshaftGenerator.generate(this.world, x, z, primer);
            }

            if (this.settings.useVillages) {
                this.villageGenerator.generate(this.world, x, z, primer);
            }

            if (this.settings.useStrongholds) {
                this.strongholdGenerator.generate(this.world, x, z, primer);
            }
            if (this.settings.useRavines) {
                this.ravineGenerator.generate(this.world, x, z, primer);
            }
        }

        Chunk chunk = new Chunk(this.world, primer, x, z);
        byte[] bytes = chunk.getBiomeArray();

        for (int i = 0; i < bytes.length; ++i) {
            bytes[i] = (byte) Biome.getIdForBiome(this.biomesForGeneration[i]);
        }

        chunk.generateSkylightMap();
        return chunk;
    }

    private void generateHeightmap(int x, int y, int z) {
        this.depthRegion = this.depthNoise.generateNoiseOctaves(this.depthRegion, x, z, 5, 5, (double) this.settings.depthNoiseScaleX, (double) this.settings.depthNoiseScaleZ, (double) this.settings.depthNoiseScaleExponent);
        float f = this.settings.coordinateScale;
        float f1 = this.settings.heightScale;
        this.mainNoiseRegion = this.mainPerlinNoise.generateNoiseOctaves(this.mainNoiseRegion, x, y, z, 5, 33, 5, (double) (f / this.settings.mainNoiseScaleX), (double) (f1 / this.settings.mainNoiseScaleY), (double) (f / this.settings.mainNoiseScaleZ));
        this.minLimitRegion = this.minLimitPerlinNoise.generateNoiseOctaves(this.minLimitRegion, x, y, z, 5, 33, 5, (double) f, (double) f1, (double) f);
        this.maxLimitRegion = this.maxLimitPerlinNoise.generateNoiseOctaves(this.maxLimitRegion, x, y, z, 5, 33, 5, (double) f, (double) f1, (double) f);
        int i = 0;
        int j = 0;

        for (int k = 0; k < 5; ++k) {
            for (int l = 0; l < 5; ++l) {
                float f2 = 0.0F;
                float f3 = 0.0F;
                float f4 = 0.0F;
                Biome biome = this.biomesForGeneration[k + 2 + (l + 2) * 10];

                for (int d7 = -2; d7 <= 2; ++d7) {
                    for (int k1 = -2; k1 <= 2; ++k1) {
                        Biome d8 = this.biomesForGeneration[k + d7 + 2 + (l + k1 + 2) * 10];
                        float f5 = this.settings.biomeDepthOffSet + d8.getBaseHeight() * this.settings.biomeDepthWeight;
                        float d9 = this.settings.biomeScaleOffset + d8.getHeightVariation() * this.settings.biomeScaleWeight;
                        if (this.terrainType == WorldType.AMPLIFIED && f5 > 0.0F) {
                            f5 = 1.0F + f5 * 2.0F;
                            d9 = 1.0F + d9 * 4.0F;
                        }

                        float f7 = this.biomeWeights[d7 + 2 + (k1 + 2) * 5] / (f5 + 2.0F);
                        if (d8.getBaseHeight() > biome.getBaseHeight()) {
                            f7 /= 2.0F;
                        }

                        f2 += d9 * f7;
                        f3 += f5 * f7;
                        f4 += f7;
                    }
                }

                f2 /= f4;
                f3 /= f4;
                f2 = f2 * 0.9F + 0.1F;
                f3 = (f3 * 4.0F - 1.0F) / 8.0F;
                double var35 = this.depthRegion[j] / 8000.0D;
                if (var35 < 0.0D) {
                    var35 = -var35 * 0.3D;
                }

                var35 = var35 * 3.0D - 2.0D;
                if (var35 < 0.0D) {
                    var35 /= 2.0D;
                    if (var35 < -1.0D) {
                        var35 = -1.0D;
                    }

                    var35 /= 1.4D;
                    var35 /= 2.0D;
                } else {
                    if (var35 > 1.0D) {
                        var35 = 1.0D;
                    }

                    var35 /= 8.0D;
                }

                ++j;
                double var36 = (double) f3;
                double var37 = (double) f2;
                var36 += var35 * 0.2D;
                var36 = var36 * (double) this.settings.baseSize / 8.0D;
                double d0 = (double) this.settings.baseSize + var36 * 4.0D;

                for (int l1 = 0; l1 < 33; ++l1) {
                    double d1 = ((double) l1 - d0) * (double) this.settings.stretchY * 128.0D / 256.0D / var37;
                    if (d1 < 0.0D) {
                        d1 *= 4.0D;
                    }

                    double d2 = this.minLimitRegion[i] / (double) this.settings.lowerLimitScale;
                    double d3 = this.maxLimitRegion[i] / (double) this.settings.upperLimitScale;
                    double d4 = (this.mainNoiseRegion[i] / 10.0D + 1.0D) / 2.0D;
                    double d5 = MathHelper.clamp(d2, d3, d4) - d1;
                    if (l1 > 29) {
                        double d6 = (double) ((float) (l1 - 29) / 3.0F);
                        d5 = d5 * (1.0D - d6) + -10.0D * d6;
                    }

                    this.heightMap[i] = d5;
                    ++i;
                }
            }
        }

    }

    @Override
    public void populate(int x, int z) {
        BlockFalling.fallInstantly = true;
        int blockX = x * 16;
        int blockY = z * 16;
        BlockPos pos = new BlockPos(blockX, 0, blockY);
        Biome biome = this.world.getBiome(pos.add(16, 0, 16));
        this.rand.setSeed(this.world.getSeed());
        long k = this.rand.nextLong() / 2L * 2L + 1L;
        long l = this.rand.nextLong() / 2L * 2L + 1L;
        this.rand.setSeed((long) x * k + (long) z * l ^ this.world.getSeed());
        boolean hasVillage = false;
        ChunkPos chunkPos = new ChunkPos(x, z);
        ForgeEventFactory.onChunkPopulate(true, this, this.world, this.rand, x, z, hasVillage);
        if (this.enableFeatures) {
            if (this.settings.useMineShafts) {
                this.mineshaftGenerator.generateStructure(this.world, this.rand, chunkPos);
            }

            if (this.settings.useVillages) {
                hasVillage = this.villageGenerator.generateStructure(this.world, this.rand, chunkPos);
            }

            if (this.settings.useStrongholds) {
                this.strongholdGenerator.generateStructure(this.world, this.rand, chunkPos);
            }
        }

        if (this.settings.useDungeons && TerrainGen.populate(this, this.world, this.rand, x, z, hasVillage, Populate.EventType.DUNGEON)) {
            for (int j2 = 0; j2 < this.settings.dungeonChance; ++j2) {
                int i3 = this.rand.nextInt(16) + 8;
                int l3 = this.rand.nextInt(256);
                int l1 = this.rand.nextInt(16) + 8;
                (new WorldGenDungeons()).generate(this.world, this.rand, pos.add(i3, l3, l1));
            }
        }

        biome.decorate(this.world, this.rand, new BlockPos(blockX, 0, blockY));
        if (TerrainGen.populate(this, this.world, this.rand, x, z, hasVillage, Populate.EventType.ANIMALS)) {
            WorldEntitySpawner.performWorldGenSpawning(this.world, biome, blockX + 8, blockY + 8, 16, 16, this.rand);
        }

        ForgeEventFactory.onChunkPopulate(false, this, this.world, this.rand, x, z, hasVillage);
        BlockFalling.fallInstantly = false;
    }

    @Override
    public boolean generateStructures(Chunk chunk, int x, int z) {
        return WastelandConfig.worldgen.shouldSpawnStructures;
    }

    @Override
    public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
        Biome biome = this.world.getBiome(pos);
        return biome.getSpawnableList(creatureType);
    }

    @Nullable
    public BlockPos getNearestStructurePos(World world, String structureName, BlockPos pos, boolean findUnexplored) {
        if (!this.enableFeatures) {
            return null;
        } else if ("Stronghold".equals(structureName) && this.strongholdGenerator != null) {
            return this.strongholdGenerator.getNearestStructurePos(world, pos, findUnexplored);
        } else if ("Village".equals(structureName) && this.villageGenerator != null) {
            return this.villageGenerator.getNearestStructurePos(world, pos, findUnexplored);
        } else if ("Mineshaft".equals(structureName) && this.mineshaftGenerator != null) {
            return this.mineshaftGenerator.getNearestStructurePos(world, pos, findUnexplored);
        }
        return null;
    }

    @Override
    public void recreateStructures(Chunk chunk, int x, int z) {
        if (this.enableFeatures) {
            if (this.settings.useMineShafts) {
                this.mineshaftGenerator.generate(this.world, x, z, null);
            }

            if (this.settings.useVillages) {
                this.villageGenerator.generate(this.world, x, z, null);
            }

            if (this.settings.useStrongholds) {
                this.strongholdGenerator.generate(this.world, x, z, null);
            }
        }
    }

    @Override
    public boolean isInsideStructure(World world, String structureName, BlockPos pos) {
        if (!this.enableFeatures) {
            return false;
        } else if ("Stronghold".equals(structureName) && this.strongholdGenerator != null) {
            return this.strongholdGenerator.isInsideStructure(pos);
        } else if ("Village".equals(structureName) && this.villageGenerator != null) {
            return this.villageGenerator.isInsideStructure(pos);
        } else if ("Mineshaft".equals(structureName) && this.mineshaftGenerator != null) {
            return this.mineshaftGenerator.isInsideStructure(pos);
        }
        return false;
    }
}
