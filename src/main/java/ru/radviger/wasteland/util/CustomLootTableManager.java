package com.legacy.wasteland.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.legacy.wasteland.WastelandVersion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.*;
import net.minecraft.world.storage.loot.LootContext.EntityTarget;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.conditions.LootConditionManager;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraft.world.storage.loot.functions.LootFunctionManager;
import net.minecraftforge.common.ForgeHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class CustomLootTableManager extends LootTableManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON_INSTANCE = (new GsonBuilder()).registerTypeAdapter(RandomValueRange.class, new RandomValueRange.Serializer()).registerTypeAdapter(LootPool.class, new LootPool.Serializer()).registerTypeAdapter(LootTable.class, new LootTable.Serializer()).registerTypeHierarchyAdapter(LootEntry.class, new LootEntry.Serializer()).registerTypeHierarchyAdapter(LootFunction.class, new LootFunctionManager.Serializer()).registerTypeHierarchyAdapter(LootCondition.class, new LootConditionManager.Serializer()).registerTypeHierarchyAdapter(LootContext.EntityTarget.class, new EntityTarget.Serializer()).create();
    private final LoadingCache<ResourceLocation, LootTable> registeredLootTables = CacheBuilder.newBuilder().build(new CustomLootTableManager.Loader());
    private final File baseFolder;

    public CustomLootTableManager(@Nullable File baseFolder) {
        super(baseFolder);
        this.baseFolder = baseFolder;
    }

    public boolean containsKey(ResourceLocation id) {
        return this.registeredLootTables.asMap().containsKey(id);
    }

    @Override
    public LootTable getLootTableFromLocation(ResourceLocation id) {
        return this.registeredLootTables.getUnchecked(id);
    }

    @Deprecated
    @Override
    public void reloadLootTables() {
        //this.registeredLootTables.invalidateAll();
    }

    public void load(Logger logger) {
        this.registeredLootTables.invalidateAll();
        File[] files = this.baseFolder.listFiles(f -> f.isFile() && f.getName().endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                String id = file.getName().substring(0, file.getName().length() - 5);
                logger.info(id);
                this.getLootTableFromLocation(new ResourceLocation(WastelandVersion.MOD_ID, id));
            }
        }
    }

    public class Loader extends CacheLoader<ResourceLocation, LootTable> {
        private Loader() {
        }

        @Override
        public LootTable load(ResourceLocation id) throws Exception {
            if (id.getResourcePath().contains(".")) {
                LOGGER.debug("Invalid loot table name '{}' (can't contain periods)", id);
                return LootTable.EMPTY_LOOT_TABLE;
            } else {
                LootTable table = this.loadLootTable(id);
                if (table == null) {
                    table = this.loadBuiltinLootTable(id);
                }

                if (table == null) {
                    table = LootTable.EMPTY_LOOT_TABLE;
                    LOGGER.warn("Couldn't find resource table {}", id);
                }

                return table;
            }
        }

        @Nullable
        private LootTable loadLootTable(ResourceLocation id) {
            if (CustomLootTableManager.this.baseFolder == null) {
                return null;
            } else {
                File file = new File(CustomLootTableManager.this.baseFolder, id.getResourcePath() + ".json");
                if (file.exists()) {
                    if (file.isFile()) {
                        String s;
                        try {
                            s = Files.toString(file, StandardCharsets.UTF_8);
                        } catch (IOException exc) {
                            LOGGER.warn("Couldn't load loot table {} from {}", id, file, exc);
                            return LootTable.EMPTY_LOOT_TABLE;
                        }

                        try {
                            return ForgeHooks.loadLootTable(GSON_INSTANCE, id, s, true, CustomLootTableManager.this);
                        } catch (JsonParseException | IllegalArgumentException exc) {
                            LOGGER.error("Couldn't load loot table {} from {}", id, file, exc);
                            return LootTable.EMPTY_LOOT_TABLE;
                        }
                    } else {
                        LOGGER.warn("Expected to find loot table {} at {} but it was a folder.", id, file);
                        return LootTable.EMPTY_LOOT_TABLE;
                    }
                } else {
                    return null;
                }
            }
        }

        @Nullable
        private LootTable loadBuiltinLootTable(ResourceLocation location) {
            URL url = LootTableManager.class.getResource("/assets/" + location.getResourceDomain() + "/loot_tables/" + location.getResourcePath() + ".json");
            if (url != null) {
                String s;
                try {
                    s = Resources.toString(url, StandardCharsets.UTF_8);
                } catch (IOException var6) {
                    LOGGER.warn("Couldn't load loot table {} from {}", location, url, var6);
                    return LootTable.EMPTY_LOOT_TABLE;
                }

                try {
                    return ForgeHooks.loadLootTable(GSON_INSTANCE, location, s, false, CustomLootTableManager.this);
                } catch (JsonParseException exc) {
                    LOGGER.error("Couldn't load loot table {} from {}", location, url, exc);
                    return LootTable.EMPTY_LOOT_TABLE;
                }
            } else {
                return null;
            }
        }
    }
}
