package com.legacy.wasteland.world.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.legacy.wasteland.Wasteland;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.JsonUtils;

import java.util.function.Predicate;

public class BlockReplacement {
    public final Predicate<IBlockState> from;
    public final IBlockState to;
    public final NBTTagCompound nbt;
    public float chance;

    public BlockReplacement(JsonObject root) {
        JsonElement from = root.get("from");
        Predicate<IBlockState> filter;
        if (from.isJsonPrimitive()) {
            filter = s -> s.getBlock() == Wasteland.parseBlockState(from.getAsString()).getBlock();
        } else {
            JsonObject o = from.getAsJsonObject();
            if (JsonUtils.getBoolean(o, "exact_match", false)) {
                filter = s -> s == Wasteland.parseBlockState(from.getAsString());
            } else {
                filter = s -> s.getBlock() == Wasteland.parseBlockState(from.getAsString()).getBlock();
            }
        }
        JsonElement to = root.get("to");
        IBlockState replacement;
        NBTTagCompound nbt = new NBTTagCompound();
        if (to.isJsonPrimitive()) {
            replacement = Wasteland.parseBlockState(to.getAsString());
        } else {
            JsonObject o = to.getAsJsonObject();
            replacement = Wasteland.parseBlockState(o.get("block").getAsString());
            if (o.has("nbt")) {
                try {
                    nbt = JsonToNBT.getTagFromJson(o.get("nbt").getAsString());
                } catch (NBTException e) {
                    e.printStackTrace();
                }
            }
        }
        float chance = JsonUtils.getFloat(root, "chance", 1F);
        this.from = filter;
        this.to = replacement;
        this.nbt = nbt;
        this.chance = chance;
    }
}