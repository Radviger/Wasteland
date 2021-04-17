package com.legacy.wasteland.world.biome.decorations;

import com.legacy.wasteland.Wasteland;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockLog.EnumAxis;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenBigTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WorldGenDeadBigTree extends WorldGenBigTree {
    private final IBlockState trunk;
    private Random rand = new Random();
    private World world;
    private BlockPos basePos;
    private int heightLimit;
    private int height;
    private double heightAttenuation;
    private double branchSlope;
    private double scaleWidth;
    private double leafDensity;
    private int leafDistanceLimit;
    private List<Branch> branches;

    public WorldGenDeadBigTree(boolean notifyBlocks, IBlockState trunk) {
        super(notifyBlocks);
        this.trunk = trunk;
        this.basePos = BlockPos.ORIGIN;
        this.heightLimit = 0;
        this.heightAttenuation = 0.45D;
        this.branchSlope = 0.2D;
        this.scaleWidth = 1.0D;
        this.leafDensity = 1.0D;
        this.leafDistanceLimit = 4;
    }

    void generateLeafNodeList() {
        this.height = (int) ((double) this.heightLimit * this.heightAttenuation);
        if (this.height >= this.heightLimit) {
            this.height = this.heightLimit - 1;
        }

        int var1 = Math.min(1, (int) (1.382 + Math.pow(this.leafDensity * (double) this.heightLimit / 13.0, 2.0)));
        int treeHeight = this.heightLimit - this.leafDistanceLimit;
        int topY = this.basePos.getY() + treeHeight;
        List<Branch> nodes = new ArrayList<>(var1 * this.heightLimit);
        nodes.add(new Branch(new BlockPos(this.basePos.getX(), topY, this.basePos.getZ()), this.height));
        --topY;

        while (treeHeight >= 0) {
            float var8 = this.layerSize(treeHeight);
            if (var8 < 0.0F) {
                --topY;
                --treeHeight;
            } else {
                for (int var7 = 0; var7 < var1; ++var7) {
                    double var11 = this.scaleWidth * (double) var8 * ((double) this.rand.nextFloat() + 0.328D);
                    double var13 = (double) this.rand.nextFloat() * 2.0D * Math.PI;
                    int x = MathHelper.floor(var11 * Math.sin(var13) + (double) this.basePos.getX() + 0.5);
                    int z = MathHelper.floor(var11 * Math.cos(var13) + (double) this.basePos.getZ() + 0.5);
                    BlockPos branchStart = new BlockPos(x, topY, z);
                    BlockPos branchEnd = new BlockPos(x, topY + this.leafDistanceLimit, z);
                    if (this.checkBlockLine(branchStart, branchEnd) == -1) {
                        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(this.basePos);
                        double branchLength = Math.sqrt(Math.pow((double) Math.abs(this.basePos.getX() - x), 2.0) + Math.pow((double) Math.abs(this.basePos.getZ() - z), 2.0));
                        double branchHeight = branchLength * this.branchSlope;
                        int top = this.basePos.getY() + this.height;
                        if ((double) topY - branchHeight > (double) top) {
                            pos.setY(top);
                        } else {
                            pos.setY((int) ((double) topY - branchHeight));
                        }

                        if (this.checkBlockLine(pos, branchStart) == -1) {
                            nodes.add(new Branch(new BlockPos(x, topY, z), pos.getY()));
                        }
                    }
                }

                --topY;
                --treeHeight;
            }
        }

        this.branches = nodes;
    }

    private float layerSize(int par1) {
        if ((double) par1 < (double) this.heightLimit * 0.3D) {
            return -1.618F;
        } else {
            float var2 = (float) this.heightLimit / 2.0F;
            float var3 = (float) this.heightLimit / 2.0F - (float) par1;
            float var4;
            if (var3 == 0.0F) {
                var4 = var2;
            } else if (Math.abs(var3) >= var2) {
                var4 = 0.0F;
            } else {
                var4 = (float) Math.sqrt(Math.pow((double) Math.abs(var2), 2.0D) - Math.pow((double) Math.abs(var3), 2.0D));
            }

            var4 *= 0.5F;
            return var4;
        }
    }

    private void limb(BlockPos from, BlockPos to, IBlockState block) {
        BlockPos distance = to.add(-from.getX(), -from.getY(), -from.getZ());
        int i = this.getGreatestDistance(distance);
        float dx = (float)distance.getX() / (float)i;
        float dy = (float)distance.getY() / (float)i;
        float dz = (float)distance.getZ() / (float)i;

        for(int j = 0; j <= i; ++j) {
            BlockPos pos = from.add((double)(0.5F + (float)j * dx), (double)(0.5F + (float)j * dy), (double)(0.5F + (float)j * dz));
            EnumAxis axis = this.getLogAxis(from, pos);
            this.setBlockAndNotifyAdequately(this.world, pos, block.withProperty(BlockLog.LOG_AXIS, axis));
        }
    }

    public EnumAxis getLogAxis(BlockPos from, BlockPos to) {
        EnumAxis axis = EnumAxis.Y;
        int dx = Math.abs(to.getX() - from.getX());
        int dz = Math.abs(to.getZ() - from.getZ());
        int greatestDistance = Math.max(dx, dz);
        if (greatestDistance > 0) {
            if (dx == greatestDistance) {
                axis = EnumAxis.X;
            } else {
                axis = EnumAxis.Z;
            }
        }

        return axis;
    }

    public boolean leafNodeNeedsBase(int dy) {
        return (double) dy >= (double) this.heightLimit * 0.2D;
    }

    public void generateTrunk() {
        this.limb(this.basePos, this.basePos.add(0, this.height, 0), this.trunk);
    }

    public void generateLeafNodeBases() {
        for (Branch node : this.branches) {
            int i = node.branchBase;
            BlockPos pos = new BlockPos(this.basePos.getX(), i, this.basePos.getZ());
            if (!pos.equals(node.pos) && this.leafNodeNeedsBase(i - this.basePos.getY())) {
                this.limb(pos, node.pos, this.trunk);
            }
        }
    }

    public int checkBlockLine(BlockPos from, BlockPos to) {
        BlockPos distance = to.add(-from.getX(), -from.getY(), -from.getZ());
        int i = this.getGreatestDistance(distance);
        float dx = (float)distance.getX() / (float)i;
        float dy = (float)distance.getY() / (float)i;
        float dz = (float)distance.getZ() / (float)i;
        if (i == 0) {
            return -1;
        } else {
            for(int j = 0; j <= i; ++j) {
                BlockPos pos = from.add((double)(0.5F + (float)j * dx), (double)(0.5F + (float)j * dy), (double)(0.5F + (float)j * dz));
                if (!this.isReplaceable(this.world, pos)) {
                    return j;
                }
            }

            return -1;
        }
    }

    public int getGreatestDistance(BlockPos distance) {
        int dx = MathHelper.abs(distance.getX());
        int dy = MathHelper.abs(distance.getY());
        int dz = MathHelper.abs(distance.getZ());
        if (dz > dx && dz > dy) {
            return dz;
        } else {
            return dy > dx ? dy : dx;
        }
    }

    private boolean validTreeLocation() {
        if (this.world.getBlockState(this.basePos.down()) == Wasteland.SURFACE_BLOCK) {
            int i = this.checkBlockLine(this.basePos, this.basePos.up(this.heightLimit - 1));
            if (i == -1) {
                return true;
            } else if (i < 6) {
                return false;
            } else {
                this.heightLimit = i;
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean generate(World world, Random random, BlockPos pos) {
        this.world = world;
        this.rand.setSeed(random.nextLong());
        this.basePos = pos;
        if (this.heightLimit == 0) {
            this.heightLimit = 12 + random.nextInt(5);
        }

        if (this.validTreeLocation()) {
            this.generateLeafNodeList();
            this.generateTrunk();
            this.generateLeafNodeBases();
            return true;
        } else {
            return false;
        }
    }

    private static class Branch {
        private final BlockPos pos;
        private final int branchBase;

        public Branch(BlockPos pos, int branchBase) {
            this.pos = pos;
            this.branchBase = branchBase;
        }
    }
}
