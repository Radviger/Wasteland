package com.legacy.wasteland.world.biome.decorations;

import com.legacy.wasteland.Wasteland;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import java.util.Random;

public class WorldGenDeadGrass extends WorldGenerator {
    protected final IBlockState state;

    public WorldGenDeadGrass() {
        this.state = Wasteland.BLOCK_DEAD_GRASS.getDefaultState();
    }

    @Override
    public boolean generate(World world, Random rand, BlockPos pos) {
        for(IBlockState s = world.getBlockState(pos); (s.getBlock().isAir(s, world, pos) || s.getBlock().isLeaves(s, world, pos)) && pos.getY() > 0; s = world.getBlockState(pos)) {
            pos = pos.down();
        }

        for(int i = 0; i < 4; ++i) {
            BlockPos p = pos.add(rand.nextInt(8) - rand.nextInt(8), rand.nextInt(4) - rand.nextInt(4), rand.nextInt(8) - rand.nextInt(8));
            if (world.isAirBlock(p) && Wasteland.BLOCK_DEAD_GRASS.canBlockStay(world, p, this.state)) {
                world.setBlockState(p, this.state, 2);
            }
        }

        return true;
    }
}
