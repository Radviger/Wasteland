package com.legacy.wasteland;

import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.legacy.wasteland.common.block.BlockAcidWater;
import com.legacy.wasteland.common.block.BlockDeadGrass;
import com.legacy.wasteland.common.block.BlockDeadSapling;
import com.legacy.wasteland.common.command.CommandWasteland;
import com.legacy.wasteland.common.command.wasteland.SubCommandWeather;
import com.legacy.wasteland.config.WastelandConfig;
import com.legacy.wasteland.network.MessageWeather;
import com.legacy.wasteland.util.CustomLootTableManager;
import com.legacy.wasteland.world.WastelandWorld;
import com.legacy.wasteland.world.WastelandWorldData;
import com.legacy.wasteland.world.WeatherType;
import com.legacy.wasteland.world.biome.decorations.template.WastelandStructure;
import com.legacy.wasteland.world.util.DirectResourceLocation;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.datafix.DataFixesManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static com.legacy.wasteland.WastelandVersion.*;

@Mod(name = MOD_NAME, modid = MOD_ID, version = VERSION, acceptedMinecraftVersions = "[1.12.2]")
public class Wasteland {
    @Mod.Instance(MOD_ID)
    public static Wasteland INSTANCE;

    @SidedProxy(clientSide = "com.legacy.wasteland.client.ClientProxy", serverSide = "com.legacy.wasteland.CommonProxy")
    public static CommonProxy PROXY;

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    public static SimpleNetworkWrapper NET;

    public static File CONFIG_DIR;
    public static TemplateManager TEMPLATE_MANAGER;
    public static CustomLootTableManager LOOT_TABLE_MANAGER;
    public static Map<String, WastelandStructure> STRUCTURES = new HashMap<>();

    public static Fluid FLUID_ACID_WATER = new Fluid("acid_water",
        new ResourceLocation("blocks/water_still"),
        new ResourceLocation("blocks/water_flow"),
        WeatherType.ACID_RAIN.getParticleColor()
    );
    static {
        FluidRegistry.enableUniversalBucket();
        FluidRegistry.registerFluid(FLUID_ACID_WATER);
        FluidRegistry.addBucketForFluid(FLUID_ACID_WATER);
    }

    public static Block BLOCK_ACID_WATER = new BlockAcidWater();
    public static BlockBush BLOCK_DEAD_GRASS = new BlockDeadGrass();
    public static BlockBush BLOCK_DEAD_SAPLING = new BlockDeadSapling();

    public static DamageSource DAMAGE_ACID_WATER = new DamageSource("acid_water");
    public static DamageSource DAMAGE_ACID_RAIN = new DamageSource("acid_rain");
    public static SoundEvent SOUND_SANDSTORM = new SoundEvent(new ResourceLocation(MOD_ID, "weather.sandstorm")).setRegistryName("weather.sandstorm");

    public static IBlockState SURFACE_BLOCK, FILLER_BLOCK;

    public static WastelandWorldData loadData(World world, boolean create) {
        MapStorage dataStorage = world.getPerWorldStorage();
        WastelandWorldData data = (WastelandWorldData) dataStorage.getOrLoadData(WastelandWorldData.class, WastelandWorldData.ID);
        if (data == null) {
            data = new WastelandWorldData();
            if (create) {
                data.markDirty();
                dataStorage.setData(WastelandWorldData.ID, data);
                dataStorage.saveAllData();
            }
        }
        return data;
    }

    public static void saveData(WastelandWorldData data, World world) {
        MapStorage dataStorage = world.getPerWorldStorage();
        data.markDirty();
        dataStorage.setData(WastelandWorldData.ID, data);
        dataStorage.saveAllData();
    }

    @Mod.EventHandler
    public static void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(PROXY);
        PROXY.preInit();
        NET = NetworkRegistry.INSTANCE.newSimpleChannel(MOD_ID);
        NET.registerMessage(new MessageWeather.Handler(), MessageWeather.class, 0, Side.CLIENT);
        CONFIG_DIR = new File(event.getModConfigurationDirectory(), "wasteland");
    }

    @Mod.EventHandler
    public static void init(FMLInitializationEvent event) {
        SURFACE_BLOCK = parseBlockState(WastelandConfig.worldgen.surfaceBlock);
        FILLER_BLOCK = parseBlockState(WastelandConfig.worldgen.fillerBlock);
        WastelandWorld.init();
        PROXY.init();
    }

    @Mod.EventHandler
    public static void postInit(FMLPostInitializationEvent event) {
        PROXY.postInit();
    }

    @Mod.EventHandler
    public void serverStart(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandWasteland());
        if (WastelandConfig.general.replaceWeatherCommand) {
            event.registerServerCommand(new SubCommandWeather());
        }

        load(new File(CONFIG_DIR, "structures"), new File(CONFIG_DIR, "loot_tables"));
    }

    public static boolean load(File templatesDir, File lootTablesDir) {
        LOGGER.info("Loading structures...");
        TEMPLATE_MANAGER = new TemplateManager(templatesDir.toString(), DataFixesManager.createFixer());
        boolean error = false;
        File[] templates = templatesDir.listFiles(n -> n.isFile() && n.getName().endsWith(".json"));
        STRUCTURES.clear();
        if (templates != null) {
            for (File f : templates) {
                String id = f.getName().substring(0, f.getName().length() - 5);
                try (FileReader reader = new FileReader(f)) {
                    JsonObject root = GSON.fromJson(reader, JsonObject.class);
                    LOGGER.info(id);
                    STRUCTURES.put(id, new WastelandStructure(id, root));
                } catch (IOException e) {
                    LOGGER.error("Error loading template " + id, e);
                    error = true;
                }
            }
        }
        LOGGER.info("Loading loot tables...");
        LOOT_TABLE_MANAGER = new CustomLootTableManager(lootTablesDir);
        LOOT_TABLE_MANAGER.load(LOGGER);

        return !error;
    }

    public static Template findTemplate(String name, MinecraftServer server, TemplateManager fallbackManager) {
        ResourceLocation id = new DirectResourceLocation(name);
        Template t = TEMPLATE_MANAGER.get(server, id);
        if (t == null) {
            System.out.println("Unknown template: " + id);
            if (fallbackManager != null) {
                t = fallbackManager.get(server, id);
            }
        }
        return t;
    }

    public static LootTable findLootTable(ResourceLocation id) {
        CustomLootTableManager mgr = LOOT_TABLE_MANAGER;
        return mgr != null && id.getResourceDomain().equals(MOD_ID) ? mgr.getLootTableFromLocation(id) : null;
    }

    public static WastelandStructure findRandomStructure(Random rand) {
        if (STRUCTURES.isEmpty()) {
            return null;
        }
        List<WastelandStructure> values = new ArrayList<>(STRUCTURES.values());
        int index = rand.nextInt(values.size());
        return values.get(index);
    }

    public static ITextComponent formatCoordinates(BlockPos pos) {
        ITextComponent message = new TextComponentString("(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")");
        Style style = message.getStyle();
        style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation("commands.wasteland.generic.teleport_tooltip")));
        style.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + pos.getX() + " " + pos.getY() + " " + pos.getZ()));
        style.setColor(TextFormatting.BLUE);
        return message;
    }

    public static IBlockState parseBlockState(String string) {
        int firstBracket = string.indexOf('[');
        boolean hasBracket = firstBracket >= 0;
        String id = hasBracket ? string.substring(0, firstBracket) : string;
        Block block = Block.REGISTRY.getObject(new ResourceLocation(id));
        IBlockState state = block.getDefaultState();
        if (hasBracket) {
            String args = string.substring(firstBracket + 1, string.length() - 1);
            Map<String, IProperty<?>> properties = new HashMap<>();
            for (IProperty<?> property : state.getPropertyKeys()) {
                properties.put(property.getName(), property);
            }
            if (!args.trim().isEmpty()) {
                for (String kv : args.split(",")) {
                    String[] parts = kv.split("=");
                    if (parts.length == 2) {
                        String k = parts[0];
                        String v = parts[1];
                        if (properties.containsKey(k)) {
                            IProperty<?> property = properties.get(k);
                            Optional<?> value = property.parseValue(v);
                            if (value.isPresent()) {
                                state = state.withProperty((IProperty) property, (Comparable)value.get());
                            } else {
                                throw new IllegalArgumentException("Invalid property value: " + k + "=" + v + " for block " + id);
                            }
                        } else {
                            throw new IllegalArgumentException("Invalid property " + k + " for block " + id);
                        }
                    } else {
                        throw new IllegalArgumentException("Invalid property=value pair: " + parts + " for block " + id);
                    }
                }
            }
        }
        return state;
    }
}