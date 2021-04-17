package com.legacy.wasteland.world;

import com.legacy.wasteland.Wasteland;
import com.legacy.wasteland.WastelandVersion;
import com.legacy.wasteland.network.MessageWeather;
import com.legacy.wasteland.world.biome.decorations.template.WastelandStructure;
import jline.internal.Nullable;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;

public class WastelandWorldData extends WorldSavedData {
    public static final String ID = WastelandVersion.MOD_ID;

    private static float weatherStrengthStep = 0.005F;

    private float lastWeatherStrength, weatherStrength;
    @SideOnly(Side.CLIENT)
    private float lastFogDensity, fogDensity;
    private float windX, windZ;
    private boolean raining;
    private WeatherType lastWeatherType = WeatherType.CLEAR, weatherType = WeatherType.CLEAR;
    private Map<String, List<WastelandStructure.SavedStructure>> structures = new HashMap<>();
    private Tuple<BlockPos, WastelandStructure> currentGeneratingStructure;

    public WastelandWorldData() {
        this(ID);
    }

    public WastelandWorldData(String id) {
        super(id);
    }

    public WeatherType getWeatherType() {
        return weatherType;
    }

    public float getWindX() {
        return windX;
    }

    public float getWindZ() {
        return windZ;
    }

    public void setWind(float x, float z) {
        this.windX = x;
        this.windZ = z;
    }

    public float getWeatherStrength() {
        return weatherStrength;
    }

    public Map<String, List<WastelandStructure.SavedStructure>> getStructures() {
        return this.structures;
    }

    public boolean isRaining() {
        return raining;
    }

    public void setRaining(boolean raining) {
        this.raining = raining;
    }

    public void setWeatherType(WeatherType weatherType) {
        this.weatherType = weatherType;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        this.weatherStrength = compound.getFloat("weatherStrength");
        this.weatherType = WeatherType.values()[compound.getInteger("weatherType")];
        this.structures.clear();
        NBTTagCompound structures = compound.getCompoundTag("structures");
        for (String id : structures.getKeySet()) {
            List<WastelandStructure.SavedStructure> data = new ArrayList<>();
            NBTTagList list = structures.getTagList(id, 10);
            for (NBTBase nbt : list) {
                data.add(WastelandStructure.SavedStructure.readFromNBT((NBTTagCompound)nbt));
            }
            this.structures.put(id, data);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setFloat("weatherStrength", this.weatherStrength);
        compound.setInteger("weatherType", this.weatherType.ordinal());
        NBTTagCompound structures = new NBTTagCompound();
        for (Map.Entry<String, List<WastelandStructure.SavedStructure>> entry : this.structures.entrySet()) {
            NBTTagList list = new NBTTagList();

            for (WastelandStructure.SavedStructure data : entry.getValue()) {
                list.appendTag(data.writeToNBT(new NBTTagCompound()));
            }

            structures.setTag(entry.getKey(), list);
        }
        compound.setTag("structures", structures);
        return compound;
    }

    public void sync(EntityPlayerMP player) {
        MessageWeather msg = new MessageWeather(this.lastWeatherType, this.weatherType, this.windX, this.windZ);
        Wasteland.NET.sendTo(msg, player);
    }

    @Nullable
    public Tuple<BlockPos, WastelandStructure> getCurrentGeneratingStructure() {
        return currentGeneratingStructure;
    }

    public void setCurrentGeneratingStructure(Tuple<BlockPos, WastelandStructure> currentGeneratingStructure) {
        this.currentGeneratingStructure = currentGeneratingStructure;
    }

    public Map<String, List<WastelandStructure.SavedStructure>> getAllStructures() {
        return this.structures;
    }

    public List<WastelandStructure.SavedStructure> getStructures(String id) {
        return this.structures.getOrDefault(id, Collections.emptyList());
    }

    public void addStructure(String id, WastelandStructure.SavedStructure data) {
        this.structures.computeIfAbsent(id, k -> new ArrayList<>()).add(data);
    }

    @Nullable
    public WastelandStructure.SavedStructure getNearestStructure(BlockPos pos) {
        return this.getNearestStructure((String)null, pos);
    }

    @Nullable
    public WastelandStructure.SavedStructure getNearestStructure(String id, BlockPos pos) {
        if (id == null) {
            List<WastelandStructure.SavedStructure> cache = new ArrayList<>();
            for (Map.Entry<String, List<WastelandStructure.SavedStructure>> entry : this.structures.entrySet()) {
                WastelandStructure.SavedStructure structure = this.getNearestStructure(entry.getValue(), pos);
                if (structure != null) {
                    cache.add(structure);
                }
            }
            return this.getNearestStructure(cache, pos);
        } else {
            List<WastelandStructure.SavedStructure> structures = this.structures.getOrDefault(id, Collections.emptyList());
            return this.getNearestStructure(structures, pos);
        }
    }

    @Nullable
    private WastelandStructure.SavedStructure getNearestStructure(List<WastelandStructure.SavedStructure> list, BlockPos pos) {
        WastelandStructure.SavedStructure nearest = null;
        for (WastelandStructure.SavedStructure structure : list) {
            if (nearest != null) {
                double last = nearest.pos.distanceSq(pos);
                double current = structure.pos.distanceSq(pos);
                if (current < last) {
                    nearest = structure;
                }
            } else {
                nearest = structure;
            }
        }
        return nearest;
    }

    public void tickWeather() {
        if (this.lastWeatherType != this.weatherType) {
            this.lastWeatherStrength = this.weatherStrength;
            if (this.weatherType == WeatherType.CLEAR) {
                this.weatherStrength -= weatherStrengthStep;
                if (this.weatherStrength <= 0) {
                    this.lastWeatherType = this.weatherType;
                }
            } else if (this.lastWeatherType == WeatherType.CLEAR) {
                this.weatherStrength += weatherStrengthStep;
                if (this.weatherStrength >= 1) {
                    this.lastWeatherType = this.weatherType;
                }
            }
            this.weatherStrength = MathHelper.clamp(this.weatherStrength, 0F, 1F);
        } else {
            if (this.weatherType == WeatherType.CLEAR) {
                if (this.weatherStrength != 0) {
                    this.weatherStrength = 0;
                }
                if (this.windX != 0) {
                    this.windX = 0;
                }
                if (this.windZ != 0) {
                    this.windZ = 0;
                }
            } else if (this.weatherStrength != 1) {
                this.weatherStrength = 1;
            }
        }
    }

    public float getInterpolatedWeatherStrength(float partialTicks) {
        return lastWeatherStrength + (weatherStrength - lastWeatherStrength) * partialTicks;
    }

    @SideOnly(Side.CLIENT)
    public void setFogDensity(float fogDensity) {
        this.lastFogDensity = this.fogDensity;
        this.fogDensity = fogDensity;
    }

    @SideOnly(Side.CLIENT)
    public float getInterpolatedFogDensity(float partialTicks) {
        return lastFogDensity + (fogDensity - lastFogDensity) * partialTicks;
    }

    @Deprecated
    @SideOnly(Side.CLIENT)
    public void setWeatherType(WeatherType oldType, WeatherType newType) {
        this.lastWeatherType = oldType;
        this.weatherType = newType;
    }

    public WeatherType getInterpolatedWeatherType() {
        if (this.lastWeatherType != WeatherType.CLEAR) {
            return this.lastWeatherType;
        }
        if (this.weatherType != WeatherType.CLEAR) {
            return this.weatherType;
        }
        return WeatherType.CLEAR;
    }
}
