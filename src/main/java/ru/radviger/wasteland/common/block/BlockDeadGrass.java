package com.legacy.wasteland.common.block;

import com.legacy.wasteland.Wasteland;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

import javax.annotation.Nullable;
import java.util.Random;

public class BlockDeadGrass extends BlockBush {
    private static final AxisAlignedBB BOUNDING = new AxisAlignedBB(0.1, 0, 0.1, 0.9, 0.8, 0.9);

    public BlockDeadGrass() {
        this.setUnlocalizedName("dead_grass");
        this.setRegistryName("dead_grass");
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
        if (rand.nextInt(10) == 0 && (this.hasWater(world, pos) || world.isRainingAt(pos.up()))) {
            for (MutableBlockPos p : BlockPos.getAllInBoxMutable(pos.add(-4, -1, -4), pos.add(4, 1, 4))) {
                IBlockState s = world.getBlockState(p);
                boolean dirt = s.getBlock() instanceof BlockDirt && s.getValue(BlockDirt.VARIANT) == BlockDirt.DirtType.DIRT;
                boolean grass = s.getBlock() instanceof BlockGrass;
                if ((grass || dirt) && world.isAirBlock(p.up()) && world.canBlockSeeSky(p.up())) {
                    if (grass) {
                        world.setBlockState(p.up(), Blocks.TALLGRASS.getDefaultState());
                        world.setBlockToAir(p);
                    } else {
                        world.setBlockState(p, Blocks.GRASS.getDefaultState());
                    }
                    break;
                }
            }
        }
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return null;
    }

    @Override
    public int quantityDroppedWithBonus(int fortune, Random rand) {
        return 1 + rand.nextInt(fortune * 2 + 1);
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity tile, ItemStack item) {
        if (!world.isRemote && item.getItem() == Items.SHEARS) {
            player.addStat(StatList.getBlockStats(this));
            spawnAsEntity(world, pos, new ItemStack(Wasteland.BLOCK_DEAD_GRASS));
        } else {
            super.harvestBlock(world, player, pos, state, tile, item);
        }

    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(this);
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
    public void getDrops(NonNullList<ItemStack> items, IBlockAccess blockAccessor, BlockPos pos, IBlockState state, int fortune) {
        if (RANDOM.nextInt(64) == 0) {
            ItemStack seed = ForgeHooks.getGrassSeed(RANDOM, fortune);
            if (!seed.isEmpty()) {
                items.add(seed);
            }
        }
    }

    @Override
    public EnumOffsetType getOffsetType() {
        return EnumOffsetType.XYZ;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess blockAccessor, BlockPos pos) {
        return BOUNDING;
    }
}
