package com.legacy.wasteland.common.block;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

public class BlockDeadSapling extends BlockBush {
    private static final AxisAlignedBB BOUNDING = new AxisAlignedBB(0.1, 0, 0.1, 0.9, 0.8, 0.9);

    public BlockDeadSapling() {
        this.setUnlocalizedName("dead_sapling");
        this.setRegistryName("dead_sapling");
        this.setSoundType(SoundType.PLANT);
    }

    private boolean hasWater(World world, BlockPos pos) {
        for (MutableBlockPos p : BlockPos.getAllInBoxMutable(pos.add(-4, 0, -4), pos.add(4, 1, 4))) {
            if (world.getBlockState(p).getMaterial() == Material.WATER) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (rand.nextInt(15) == 0 && this.hasWater(world, pos) || world.isRainingAt(pos.up())) {
            IBlockState s = world.getBlockState(pos.down());
            boolean dirt = s.getBlock() instanceof BlockDirt && s.getValue(BlockDirt.VARIANT) == BlockDirt.DirtType.DIRT;
            boolean grass = s.getBlock() instanceof BlockGrass;
            if (grass || dirt) {
                world.setBlockState(pos, Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.TYPE, BlockPlanks.EnumType.OAK));
            }
        }
    }

    @Override
    public boolean isReplaceable(IBlockAccess blockAccessor, BlockPos pos) {
        return true;
    }

    @Override
    protected boolean canSustainBush(IBlockState state) {
        return super.canSustainBush(state) || state.getBlock() == Blocks.SAND;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess blockAccessor, BlockPos pos) {
        return BOUNDING;
    }
}
