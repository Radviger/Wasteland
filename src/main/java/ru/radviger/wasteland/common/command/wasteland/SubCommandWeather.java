package com.legacy.wasteland.common.command.wasteland;

import com.legacy.wasteland.Wasteland;
import com.legacy.wasteland.common.command.CommandBaseRecursive;
import com.legacy.wasteland.world.WastelandWorld;
import com.legacy.wasteland.world.WastelandWorldData;
import com.legacy.wasteland.world.WeatherType;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.WorldInfo;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.util.Collections.emptyList;

public class SubCommandWeather extends CommandBaseRecursive {
    public SubCommandWeather() {
        super(true);
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getName() {
        return "weather";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.wasteland.weather.usage";
    }

    @Override
    protected boolean run(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length >= 1) {
            WeatherType weatherType;
            try {
                weatherType = WeatherType.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException exc) {
                throw new CommandException("commands.wasteland.weather.unknown_type", args[0]);
            }
            World world = null;
            if (args.length == 2) {
                for (WorldServer w : server.worlds) {
                    if (w.provider.getDimensionType().getName().equals(args[1])) {
                        world = w;
                        break;
                    }
                }
                if (world == null) {
                    throw new CommandException("commands.wasteland.generic.unknown_world", args[1]);
                }
            } else if (args.length > 2) {
                throw new WrongUsageException(this.getUsage(sender));
            } else {
                world = sender.getEntityWorld();
            }
            int time = (300 + (new Random()).nextInt(600)) * 20;

            String worldName = world.provider.getDimensionType().getName();
            WorldInfo worldInfo = world.getWorldInfo();
            WastelandWorldData data = Wasteland.PROXY.getData(world);
            WeatherType oldWeatherType = data.getWeatherType();
            if (weatherType != oldWeatherType) {
                if (weatherType != WeatherType.CLEAR && weatherType != WeatherType.RAIN && !WastelandWorld.isWasteland(world)) {
                    throw new CommandException("commands.wasteland.generic.other_world_type");
                }
                boolean update = oldWeatherType == WeatherType.CLEAR || weatherType == WeatherType.CLEAR;
                if (update) {
                    worldInfo.setRaining(weatherType != WeatherType.CLEAR);
                }
                data.setWeatherType(weatherType);
                switch (weatherType) {
                    case CLEAR:
                        worldInfo.setCleanWeatherTime(time);
                        worldInfo.setRainTime(0);
                        worldInfo.setThunderTime(0);
                        worldInfo.setThundering(false);
                        break;
                    case RAIN:
                    case ACID_RAIN:
                    case SANDSTORM:
                        worldInfo.setCleanWeatherTime(0);
                        worldInfo.setRainTime(time);
                        worldInfo.setThunderTime(time);
                        worldInfo.setThundering(false);
                        break;
                }
                notifyCommandListener(sender, this, "commands.wasteland.weather." + weatherType.name().toLowerCase(), worldName);
            } else {
                throw new CommandException("commands.wasteland.weather.same_type", worldName);
            }
            return true;
        } else {
            throw new WrongUsageException(this.getUsage(sender));
        }
    }

    @Override
    protected List<String> complete(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos lookingSpot) {
        switch (args.length) {
            case 1:
                List<String> weatherTypes = new ArrayList<>();
                for (WeatherType weatherType : WeatherType.values()) {
                    weatherTypes.add(weatherType.name().toLowerCase());
                }
                return getListOfStringsMatchingLastWord(args, weatherTypes);
            case 2: {
                List<String> worldNames = new ArrayList<>();
                for (DimensionType dimension : DimensionType.values()) {
                    worldNames.add(dimension.getName());
                }
                return getListOfStringsMatchingLastWord(args, worldNames);
            }
        }
        return emptyList();
    }
}
