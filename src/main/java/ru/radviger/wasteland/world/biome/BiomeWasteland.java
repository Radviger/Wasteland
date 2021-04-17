package com.legacy.wasteland.world.biome;

import com.legacy.wasteland.Wasteland;
import com.legacy.wasteland.config.WastelandConfig;
import com.legacy.wasteland.world.biome.decorations.BiomeDecoratorWasteland;
import net.minecraft.block.BlockSand;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

public class BiomeWasteland extends Biome {
    public BiomeWasteland(BiomeProperties properties) {
        super(properties);
        this.spawnableCreatureList.clear();
        this.spawnableMonsterList.clear();
        this.spawnableWaterCreatureList.clear();
        this.spawnableCaveCreatureList.clear();
        this.decorator = new BiomeDecoratorWasteland();

        if (WastelandConfig.worldgen.shouldSpawnDayHusks) {
            this.spawnableCreatureList.add(new SpawnListEntry(EntityHusk.class, 45/*%*/, 1,/*<-->*/ 3));
        }
        this.spawnableMonsterList.add(new SpawnListEntry(EntityHusk.class, 90/*%*/, 2,/*<-->*/ 5));
        this.spawnableMonsterList.add(new SpawnListEntry(EntityZombie.class, 20, 1, 1));
        this.spawnableMonsterList.add(new SpawnListEntry(EntityZombieVillager.class, 5, 1, 1));
        this.spawnableMonsterList.add(new SpawnListEntry(EntitySkeleton.class, 30, 1, 1));
        this.spawnableMonsterList.add(new SpawnListEntry(EntityCreeper.class, 40, 1, 1));
        this.spawnableMonsterList.add(new SpawnListEntry(EntityEnderman.class, 40, 1, 1));

        this.spawnableCaveCreatureList.add(new SpawnListEntry(EntityBat.class, 5, 1, 2));
        if (WastelandConfig.worldgen.shouldSpawnCaveSpiders) {
            this.spawnableCaveCreatureList.add(new SpawnListEntry(EntitySpider.class, 1, 1, 1));
        }

        this.decorator.deadBushPerChunk = 5;
        this.decorator.flowersPerChunk = -999;
        this.decorator.generateFalls = false;
        this.decorator.grassPerChunk = 10;
        this.decorator.treesPerChunk = -999;
        this.topBlock = Wasteland.SURFACE_BLOCK;
        this.fillerBlock = Wasteland.FILLER_BLOCK;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getSkyColorByTemp(float temperature) {
        float mod = MathHelper.clamp((temperature / 3F) + 1F, 0.25F, 1F);
        int r = (int) (mod * 134F);
        int g = (int) (mod * 104F);
        int b = (int) (mod * 54F);
        return r << 16 | g << 8 | b;
    }

    @Override
    public BiomeDecorator getModdedBiomeDecorator(BiomeDecorator original) {
        return new BiomeDecoratorWasteland();
    }

    @Override
    public void genTerrainBlocks(World world, Random rand, ChunkPrimer chunkPrimer, int x, int z, double noise) {
        int seaLevel = world.getSeaLevel();
        IBlockState topBlock = this.topBlock;
        IBlockState fillerBlock = this.fillerBlock;
        int currentSurfaceLevel = -1;
        int surfaceDepth = (int)(noise / 3.0 + 3.0 + rand.nextDouble() * 0.25);
        int localX = x & 15;
        int localZ = z & 15;
        MutableBlockPos pos = new MutableBlockPos();

        for(int y = 255; y >= 0; --y) {
            if (y <= rand.nextInt(5)) {
                chunkPrimer.setBlockState(localZ, y, localX, BEDROCK);
            } else {
                IBlockState state = chunkPrimer.getBlockState(localZ, y, localX);
                if (state.getMaterial() == Material.AIR) {
                    currentSurfaceLevel = -1;
                } else if (state.getBlock() == Blocks.STONE) {
                    if (currentSurfaceLevel == -1) {
                        if (surfaceDepth <= 0) {
                            topBlock = AIR;
                            fillerBlock = STONE;
                        } else if (y >= seaLevel - 4 && y <= seaLevel + 1) {
                            topBlock = this.topBlock;
                            fillerBlock = this.fillerBlock;
                        }

                        if (y < seaLevel && (topBlock == null || topBlock.getMaterial() == Material.AIR)) {
                            topBlock = Blocks.CLAY.getDefaultState(); //FIXME: RED CLAY
                        }

                        currentSurfaceLevel = surfaceDepth;
                        if (y >= seaLevel - 1) {
                            chunkPrimer.setBlockState(localZ, y, localX, topBlock);
                        } else if (y < seaLevel - 7 - surfaceDepth) {
                            topBlock = AIR;
                            fillerBlock = STONE;
                            chunkPrimer.setBlockState(localZ, y, localX, GRAVEL);
                        } else {
                            chunkPrimer.setBlockState(localZ, y, localX, fillerBlock);
                        }
                    } else if (currentSurfaceLevel > 0) {
                        --currentSurfaceLevel;
                        chunkPrimer.setBlockState(localZ, y, localX, fillerBlock);
                        if (currentSurfaceLevel == 0 && fillerBlock.getBlock() == Blocks.SAND && surfaceDepth > 1) {
                            currentSurfaceLevel = rand.nextInt(4) + Math.max(0, y - 63);
                            fillerBlock = fillerBlock.getValue(BlockSand.VARIANT) == BlockSand.EnumType.RED_SAND ? RED_SANDSTONE : SANDSTONE;
                        }
                    }
                }
            }
        }
    }
}
