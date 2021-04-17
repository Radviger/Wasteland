package com.legacy.wasteland.world.biome.decorations.template;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.legacy.wasteland.Wasteland;
import com.legacy.wasteland.world.util.BlockReplacement;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.template.ITemplateProcessor;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.Template.BlockInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class WastelandStructure implements ITemplateProcessor {
    public final String id;
    public final int rarity;
    public final List<ResourceLocation> biomeFilter;
    public final String templateName;
    public final SurfaceVerification mode;
    public final IBlockState surfaceBlock, fillerBlock;
    public final List<BlockReplacement> replacements = new ArrayList<>();
    public final boolean rotated, mirrored;
    public final int minDistance;
    public final Vec3i offset;
    
    public WastelandStructure(String id, JsonObject root) {
        this.id = id;
        this.rarity = JsonUtils.getInt(root, "rarity");
        if (root.has("biomes")) {
            List<ResourceLocation> ids = new ArrayList<>();
            for (JsonElement b : root.getAsJsonArray("biomes")) {
                ids.add(new ResourceLocation(b.getAsString()));
            }
            this.biomeFilter = ids;
        } else {
            this.biomeFilter = Collections.emptyList();
        }
        this.templateName = JsonUtils.getString(root, "id");
        this.mode = SurfaceVerification.valueOf(JsonUtils.getString(root, "verification", SurfaceVerification.ALL_CORNERS.name()).toUpperCase());
        this.rotated = JsonUtils.getBoolean(root, "rotate", true);
        this.mirrored = JsonUtils.getBoolean(root, "mirror", true);
        this.minDistance = JsonUtils.getInt(root, "min_distance", 200);
        if (root.has("offset")) {
            JsonObject offset = JsonUtils.getJsonObject(root, "offset");
            int offsetX = JsonUtils.getInt(offset, "x", 0);
            int offsetY = JsonUtils.getInt(offset, "y", 0);
            int offsetZ = JsonUtils.getInt(offset, "z", 0);
            this.offset = new Vec3i(offsetX, offsetY, offsetZ);
        } else {
            this.offset = Vec3i.NULL_VECTOR;
        }
        this.surfaceBlock = Wasteland.parseBlockState(JsonUtils.getString(root, "surface", Wasteland.SURFACE_BLOCK.toString()));
        this.fillerBlock = Wasteland.parseBlockState(JsonUtils.getString(root, "filler", Wasteland.FILLER_BLOCK.toString()));
        if (root.has("replacements")) {
            for (JsonElement e : root.getAsJsonArray("replacements")) {
                this.replacements.add(new BlockReplacement(e.getAsJsonObject()));
            }
        }
    }

    public SavedStructure generate(World world, Random rand, BlockPos pos, Rotation rotation, Mirror mirror) {
        if (world.getBlockState(pos).getMaterial().isSolid()) {
            return null;
        }

        for (EnumFacing side : EnumFacing.values()) {
            BlockPos p = pos.offset(side);
            IBlockState s = world.getBlockState(p);
            if (side == EnumFacing.DOWN) {
                if (s != Wasteland.SURFACE_BLOCK) {
                    return null;
                }
            } else if (s.getMaterial().isSolid()) {
                return null;
            }
        }

        Template template = Wasteland.findTemplate(this.templateName, world.getMinecraftServer(), world.getSaveHandler().getStructureTemplateManager());

        pos = pos.down();

        Vec3i endOffset = template.getSize();

        switch (rotation) {
            case CLOCKWISE_90:
                endOffset = new Vec3i(-endOffset.getZ(), endOffset.getY(), endOffset.getX());
                break;
            case CLOCKWISE_180:
                endOffset = new Vec3i(-endOffset.getZ(), endOffset.getY(), -endOffset.getX());
                break;
            case COUNTERCLOCKWISE_90:
                endOffset = new Vec3i(endOffset.getZ(), endOffset.getY(), -endOffset.getX());
                break;
        }

        BlockPos size = template.transformedSize(rotation);

        if (isValidSurface(world, pos, this.mode, endOffset)) {
            PlacementSettings settings = new PlacementSettings().setRotation(rotation).setMirror(mirror);
            template.addBlocksToWorld(world, pos.add(this.offset), this, settings, 2);
            return new SavedStructure(pos, size, rotation, mirror, this.minDistance, new NBTTagCompound());
        }
        return null;
    }

    @Nullable
    @Override
    public BlockInfo processBlock(World world, BlockPos pos, BlockInfo blockInfo) {
        if (blockInfo.blockState == this.surfaceBlock) {
            return new BlockInfo(blockInfo.pos, Wasteland.SURFACE_BLOCK, blockInfo.tileentityData);
        }
        if (blockInfo.blockState == this.fillerBlock) {
            return new BlockInfo(blockInfo.pos, Wasteland.FILLER_BLOCK, blockInfo.tileentityData);
        }
        for (BlockReplacement replacement : this.replacements) {
            if (replacement.from.test(blockInfo.blockState) && new Random().nextFloat() <= replacement.chance) {
                return new BlockInfo(blockInfo.pos, replacement.to, replacement.nbt);
            }
        }
        return blockInfo;
    }

    private static boolean isValidSurface(World world, BlockPos pos, SurfaceVerification mode, Vec3i endOffset) {
        switch (mode) {
            case DIAGONAL_CORNERS:
                return isValidPos(world, pos) && isValidPos(world, pos.add(endOffset.getX(), 0, endOffset.getZ()));
            case ALL_CORNERS:
                return isValidPos(world, pos) && isValidPos(world, pos.add(endOffset.getX(), 0, endOffset.getZ()))
                        && isValidPos(world, pos.add(endOffset.getX(), 0, 0)) && isValidPos(world, pos.add(0, 0, endOffset.getZ()));
            case FULL:
                for (BlockPos.MutableBlockPos p : BlockPos.getAllInBoxMutable(pos, pos.add(endOffset.getX(), 0, endOffset.getZ()))) {
                    if (!isValidPos(world, p)) {
                        return false;
                    }
                }
                return true;
        }
        return false;
    }

    private static boolean isValidPos(World world, BlockPos pos) {
        return world.getBlockState(pos) == Wasteland.SURFACE_BLOCK
                && world.getBlockState(pos.up()).getMaterial().isReplaceable();
    }

    public enum SurfaceVerification {
        DIAGONAL_CORNERS,
        ALL_CORNERS,
        FULL
    }

    public static class SavedStructure {
        public final BlockPos pos;
        public final Vec3i size;
        public final Rotation rotation;
        public final Mirror mirror;
        public final int minDistance;
        public final NBTTagCompound extra;

        public SavedStructure(BlockPos pos, Vec3i size, Rotation rotation, Mirror mirror, int minDistance, NBTTagCompound extra) {
            this.pos = pos;
            this.size = size;
            this.rotation = rotation;
            this.mirror = mirror;
            this.minDistance = minDistance;
            this.extra = extra;
        }

        public BlockPos getStartPos() {
            return pos;
        }

        public Vec3i getEndOffset() {
            Vec3i endOffset = this.size;

            switch (rotation) {
                case CLOCKWISE_90:
                    endOffset = new Vec3i(-endOffset.getZ(), endOffset.getY(), endOffset.getX());
                    break;
                case CLOCKWISE_180:
                    endOffset = new Vec3i(-endOffset.getZ(), endOffset.getY(), -endOffset.getX());
                    break;
                case COUNTERCLOCKWISE_90:
                    endOffset = new Vec3i(endOffset.getZ(), endOffset.getY(), -endOffset.getX());
                    break;
            }
            return endOffset;
        }

        public BlockPos getEndPos() {
            return getStartPos().add(getEndOffset());
        }

        public NBTTagCompound writeToNBT(NBTTagCompound compound) {
            compound.setInteger("x", this.pos.getX());
            compound.setInteger("y", this.pos.getY());
            compound.setInteger("z", this.pos.getZ());
            compound.setInteger("width", this.size.getX());
            compound.setInteger("height", this.size.getY());
            compound.setInteger("depth", this.size.getZ());
            compound.setString("rotation", this.rotation.name().toLowerCase());
            compound.setString("mirror", this.mirror.name().toLowerCase());
            compound.setInteger("minDistance", this.minDistance);
            compound.setTag("extra", this.extra);
            return compound;
        }

        public static SavedStructure readFromNBT(NBTTagCompound compound) {
            BlockPos pos = new BlockPos(
                compound.getInteger("x"),
                compound.getInteger("y"),
                compound.getInteger("z")
            );
            Vec3i size = new Vec3i(
                compound.getInteger("width"),
                compound.getInteger("height"),
                compound.getInteger("depth")
            );
            int minDistance = compound.getInteger("minDistance");
            Rotation rotation = Rotation.valueOf(compound.getString("rotation").toUpperCase());
            Mirror mirror = Mirror.valueOf(compound.getString("mirror").toUpperCase());
            NBTTagCompound extra = compound.getCompoundTag("extra");
            return new SavedStructure(pos, size, rotation, mirror, minDistance, extra);
        }

        public double distanceTo(@Nonnull BlockPos pos) {
            return Math.sqrt(this.pos.distanceSq(pos));
        }
    }
}
