package com.legacy.wasteland.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.legacy.wasteland.WastelandVersion.MOD_ID;

@Config(modid = MOD_ID, name = "wasteland")
@Config.LangKey("wastelands.config.title")
public class WastelandConfig {
    public static General general = new General();

    public static Worldgen worldgen = new Worldgen();

    public static Biomes biomes = new Biomes();

    public static class General {
        @Config.Comment("Allow vanilla '/weather' command replacement?")
        public boolean replaceWeatherCommand = true;

        @Config.Comment("Wasteland worlds")
        public String[] enabledWorlds = {"overworld"};
    }

    public static class Worldgen {

        @Config.Comment("Dead Tree Rarity")
        public int wastelandTreeSpawnRate = 2;

        @Config.Comment("Wasteland fires per chunk")
        public int randomFirePerChunk = 1;

        @Config.Comment("Should vanilla structures spawn?")
        public boolean shouldSpawnStructures = true;

        @Config.Comment("Wasteland Top Block")
        public String surfaceBlock = "minecraft:sand[variant=red_sand]";

        @Config.Comment("Wasteland Fill Block")
        public String fillerBlock = "minecraft:red_sandstone";

        @Config.Comment("Enable cities")
        public boolean shouldSpawnCities = true;

        @Config.Comment("Allow husks to spawn in daylight")
        public boolean shouldSpawnDayHusks = true;

        @Config.Comment("Allow spiders to spawn in caves")
        public boolean shouldSpawnCaveSpiders = true;

        @Config.Comment("Should ores generate?")
        public boolean shouldSpawnOres = true;
    }

    public static class Biomes {

        @Config.Comment("Should the Wasteland Mountains biome be enabled?")
        public boolean wastelandMountainsEnabled = true;

        @Config.Comment("Should the Wasteland Forest biome be enabled?")
        public boolean wastelandForestEnabled = true;

        @Config.Comment("Should the Wasteland City biome be enabled?")
        public boolean wastelandCityEnabled = true;

        @Config.Comment("Should the Oasis structure be enabled?")
        public boolean oasisEnabled = true;
    }

    @Mod.EventBusSubscriber(modid = MOD_ID)
    private static class EventHandler {
        @SubscribeEvent
        public static void onConfigChangedEvent(final ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(MOD_ID)) {
                ConfigManager.sync(MOD_ID, Config.Type.INSTANCE);
            }
        }
    }
}
