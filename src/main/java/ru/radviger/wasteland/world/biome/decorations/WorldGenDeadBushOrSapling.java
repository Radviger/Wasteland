package com.legacy.wasteland.world.biome.decorations;

import com.legacy.wasteland.Wasteland;
import net.minecraft.block.BlockBush;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import java.util.Random;

public class WorldGenDeadBushOrSapling extends WorldGenerator {
    @Override
    public boolean generate(World world, Random rand, BlockPos pos) {
        for(IBlockState state = world.getBlockState(pos); (state.getBlock().isAir(state, world, pos) || state.getBlock().isLeaves(state, world, pos)) && pos.getY() > 0; state = world.getBlockState(pos)) {
            pos = pos.down();
        }
        BlockBush block = rand.nextInt(16) == 0 ? Wasteland.BLOCK_DEAD_SAPLING : Blocks.DEADBUSH;

        for(int i = 0; i < 4; ++i) {
            BlockPos ofp = pos.add(rand.nextInt(8) - rand.nextInt(8), rand.nextInt(4) - rand.nextInt(4), rand.nextInt(8) - rand.nextInt(8));
            if (world.isAirBlock(ofp) && block.canBlockStay(world, ofp, block.getDefaultState())) {
                world.setBlockState(ofp, block.getDefaultState(), 2);
            }
        }

        return true;
    }
}
