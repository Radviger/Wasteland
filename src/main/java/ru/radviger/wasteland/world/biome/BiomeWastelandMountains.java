package com.legacy.wasteland.world.biome;

import net.minecraft.block.BlockSilverfish;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.pattern.BlockMatcher;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.OreGenEvent;
import net.minecraftforge.event.terraingen.OreGenEvent.GenerateMinable.EventType;
import net.minecraftforge.event.terraingen.TerrainGen;

import java.util.Random;

public class BiomeWastelandMountains extends BiomeWasteland {
    private final WorldGenerator silverfishSpawner = new WorldGenMinable(Blocks.MONSTER_EGG.getDefaultState().withProperty(BlockSilverfish.VARIANT, BlockSilverfish.EnumType.STONE), 9);

    public BiomeWastelandMountains(BiomeProperties properties) {
        super(properties);
    }

    @Override
    public void decorate(World world, Random rand, BlockPos pos) {
        super.decorate(world, rand, pos);
        MinecraftForge.ORE_GEN_BUS.post(new OreGenEvent.Pre(world, rand, pos));
        WorldGenerator emeralds = new EmeraldGenerator();
        if (TerrainGen.generateOre(world, rand, emeralds, pos, EventType.EMERALD)) {
            emeralds.generate(world, rand, pos);
        }

        for(int j1 = 0; j1 < 7; ++j1) {
            int k1 = rand.nextInt(16);
            int l1 = rand.nextInt(64);
            int i2 = rand.nextInt(16);
            if (TerrainGen.generateOre(world, rand, this.silverfishSpawner, pos.add(j1, k1, l1), EventType.SILVERFISH)) {
                this.silverfishSpawner.generate(world, rand, pos.add(k1, l1, i2));
            }
        }

        MinecraftForge.ORE_GEN_BUS.post(new OreGenEvent.Post(world, rand, pos));
    }

    private static class EmeraldGenerator extends WorldGenerator {
        private EmeraldGenerator() {}

        @Override
        public boolean generate(World world, Random rand, BlockPos pos) {
            int count = 3 + rand.nextInt(6);

            for(int i = 0; i < count; ++i) {
                int offset = ForgeModContainer.fixVanillaCascading ? 8 : 0;
                BlockPos blockpos = pos.add(rand.nextInt(16) + offset, rand.nextInt(28) + 4, rand.nextInt(16) + offset);
                IBlockState state = world.getBlockState(blockpos);
                if (state.getBlock().isReplaceableOreGen(state, world, blockpos, BlockMatcher.forBlock(Blocks.STONE))) {
                    world.setBlockState(blockpos, Blocks.EMERALD_ORE.getDefaultState(), 18);
                }
            }

            return true;
        }
    }
}
