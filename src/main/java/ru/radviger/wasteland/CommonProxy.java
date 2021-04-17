package com.legacy.wasteland;

import com.legacy.wasteland.common.block.BlockDeadGrass;
import com.legacy.wasteland.common.entity.EntityTumbleweed;
import com.legacy.wasteland.config.WastelandConfig;
import com.legacy.wasteland.network.MessageWeather;
import com.legacy.wasteland.util.CustomLootTableManager;
import com.legacy.wasteland.world.WastelandWorld;
import com.legacy.wasteland.world.WastelandWorldData;
import com.legacy.wasteland.world.WeatherType;
import net.minecraft.block.*;
import net.minecraft.block.BlockDirt.DirtType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntitySkeletonHorse;
import net.minecraft.entity.passive.EntityZombieHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static com.legacy.wasteland.WastelandVersion.MOD_ID;

public class CommonProxy {
    public final Map<DimensionType, WastelandWorldData> data = new HashMap<>();

    private static Field F_worldInfo;

    public void preInit() {}
    public void init() {
        IForgeRegistry<SoundEvent> soundRegistry = GameRegistry.findRegistry(SoundEvent.class);
        soundRegistry.register(Wasteland.SOUND_SANDSTORM);

        EntityRegistry.registerModEntity(new ResourceLocation(MOD_ID, "tumbleweed"), EntityTumbleweed.class, "tumbleweed", 0, Wasteland.INSTANCE, 80, 5, true);
    }
    public void postInit() {}

    @SubscribeEvent
    public void registerBlocks(RegistryEvent.Register<Block> event) {
        IForgeRegistry<Block> registry = event.getRegistry();
        registry.register(Wasteland.BLOCK_ACID_WATER);
        registry.register(Wasteland.BLOCK_DEAD_GRASS);
        registry.register(Wasteland.BLOCK_DEAD_SAPLING);
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();
        registry.register(
            new ItemBlock(Wasteland.BLOCK_DEAD_GRASS).setRegistryName(Wasteland.BLOCK_DEAD_GRASS.getRegistryName())
        );
        registry.register(
            new ItemBlock(Wasteland.BLOCK_DEAD_SAPLING).setRegistryName(Wasteland.BLOCK_DEAD_SAPLING.getRegistryName())
        );
    }

    @SubscribeEvent
    public void monsterOnFire(LivingHurtEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (event.getSource() == DamageSource.ON_FIRE && WastelandWorld.isWasteland(entity.world) && entity instanceof EntityMob) {
            entity.extinguish();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void monsterUpdateFire(LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity.isBurning() && WastelandWorld.isWasteland(entity.world) && entity instanceof EntityMob) {
            entity.extinguish();
        }
    }

    @SubscribeEvent
    public void monsterSpawn(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        World world = event.getWorld();
        if (!world.isRemote && WastelandWorld.isWasteland(world)) {
            BlockPos pos = new BlockPos(entity.posX, entity.getEntityBoundingBox().minY, entity.posZ);
            int blockLight = event.getWorld().getLightFor(EnumSkyBlock.BLOCK, pos);
            int skyLight = event.getWorld().getLightFor(EnumSkyBlock.SKY, pos);
            if (entity.getClass() == EntityZombie.class && !((EntityZombie)entity).isChild() && world.rand.nextInt(10) == 0) {
                if (skyLight == 15) {
                    EntityZombieHorse horse = new EntityZombieHorse(world);
                    horse.setHorseTamed(true);
                    horse.setPosition(entity.posX, entity.posY, entity.posZ);
                    horse.setGrowingAge(0);
                    world.spawnEntity(horse);
                    entity.startRiding(horse, true);
                    event.setResult(Event.Result.ALLOW);
                }
            } else if (entity.getClass() == EntitySkeleton.class && world.rand.nextInt(10) == 0) {
                if (skyLight == 15) {
                    EntitySkeletonHorse horse = new EntitySkeletonHorse(world);
                    horse.setHorseTamed(true);
                    horse.setPosition(entity.posX, entity.posY, entity.posZ);
                    horse.setGrowingAge(0);
                    world.spawnEntity(horse);
                    entity.startRiding(horse, true);
                    event.setResult(Event.Result.ALLOW);
                }
            }
        }
    }

    @SubscribeEvent
    public void monsterCheckSpawn(LivingSpawnEvent.CheckSpawn event) {
        EntityLivingBase entity = event.getEntityLiving();
        World world = entity.world;
        if (!event.isSpawner() && WastelandWorld.isWasteland(world)) {
            if (entity instanceof EntitySpider && WastelandConfig.worldgen.shouldSpawnCaveSpiders) {
                BlockPos pos = new BlockPos(entity.posX, entity.getEntityBoundingBox().minY, entity.posZ);
                int blockLight = event.getWorld().getLightFor(EnumSkyBlock.BLOCK, pos);
                int skyLight = event.getWorld().getLightFor(EnumSkyBlock.SKY, pos);
                if (blockLight >= 5 || skyLight >= 5 || event.getWorld().canSeeSky(pos)) {
                    event.setResult(Event.Result.DENY);
                } else {
                    List<Entity> spiders = world.getEntitiesInAABBexcluding(entity,
                        new AxisAlignedBB(pos.add(-10, -10, -10), pos.add(10, 10, 10)),
                        e -> e instanceof EntitySpider
                    );
                    if (spiders.size() > 1) {
                        event.setResult(Event.Result.DENY);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void worldLoad(WorldEvent.Load event) {
        World world = event.getWorld();
        if (WastelandWorld.isWasteland(world)) {
            WastelandWorldData data = Wasteland.loadData(world, !world.isRemote);
            this.data.put(world.provider.getDimensionType(), data);
        }
    }

    @SubscribeEvent
    public void worldSave(WorldEvent.Save event) {
        World world = event.getWorld();
        if (WastelandWorld.isWasteland(world)) {
            WastelandWorldData data = getData(world);
            if (data != null) {
                Wasteland.saveData(data, world);
            }
        }
    }

    public WastelandWorldData getData(World world) {
        return this.data.get(world.provider.getDimensionType());
    }

    private int randomTickSeed = new Random().nextInt();

    @SubscribeEvent
    public void worldTick(TickEvent.WorldTickEvent event) {
        WorldServer world = (WorldServer) event.world;
        WastelandWorldData data = getData(world);
        if (data != null) {
            boolean raining = world.isRaining();
            if (data.isRaining() != raining) {
                data.setRaining(raining);
                if (!world.isRemote) {
                    WeatherType lastWeatherType = data.getWeatherType();
                    WeatherType weatherType = raining ? WeatherType.random(world.getWorldTime(), world.rand) : WeatherType.CLEAR;
                    data.setWeatherType(weatherType);

                    float windX = data.getWindX();
                    float windZ = data.getWindZ();

                    if (weatherType != WeatherType.CLEAR) {
                        windX = (world.rand.nextFloat() * 2F - 1F) / 10F;
                        windZ = (world.rand.nextFloat() * 2F - 1F) / 10F;
                        data.setWind(windX, windZ);
                    }

                    MessageWeather msg = new MessageWeather(lastWeatherType, weatherType, windX, windZ);
                    Wasteland.NET.sendToDimension(msg, world.provider.getDimension());
                }
            }
            WeatherType weatherType = data.getWeatherType();
            int randomTickSpeed = world.getGameRules().getInt("randomTickSpeed");

            Iterator iterator = world.getPersistentChunkIterable(world.getPlayerChunkMap().getChunkIterator());

            while (iterator.hasNext()) {
                Chunk chunk = (Chunk)iterator.next();
                int j = chunk.x * 16;
                int k = chunk.z * 16;

                if (randomTickSpeed > 0) {
                    for (ExtendedBlockStorage blockStorage : chunk.getBlockStorageArray()) {
                        if (blockStorage != Chunk.NULL_BLOCK_STORAGE && blockStorage.needsRandomTick()) {
                            for (int i = 0; i < randomTickSpeed; ++i) {
                                randomTickSeed = randomTickSeed * 3 + 1013904223;
                                int rawPos = randomTickSeed >> 2;
                                int x = rawPos & 15;
                                int z = rawPos >> 8 & 15;
                                int y = rawPos >> 16 & 15;
                                IBlockState state = blockStorage.get(x, y, z);
                                world.profiler.startSection("randomTickWasteland");
                                BlockPos pos = new BlockPos(x + j, y + blockStorage.getYLocation(), z + k);

                                this.tickBlock(weatherType, world, pos, state);

                                world.profiler.endSection();
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void entityUpdate(LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        World world = entity.getEntityWorld();
        WastelandWorldData data = getData(world);
        if (data != null) {
            WeatherType weatherType = data.getWeatherType();
            BlockPos pos = entity.getPosition();
            if (world.isRainingAt(pos) || world.isRainingAt(pos.up()) && (world.getBlockState(pos.up()) instanceof BlockFluidBase || world.getBlockState(pos.up()) instanceof BlockLiquid)) {
                if (weatherType == WeatherType.ACID_RAIN) {
                    if (!world.isRemote) {
                        if (entity instanceof EntityZombie || entity instanceof EntityZombieHorse) {
                            /*if (world.rand.nextInt(200) == 0) {
                                entity.setDead();
                                if (entity instanceof EntityZombieHorse) {
                                    EntitySkeletonHorse skeletonHorse = new EntitySkeletonHorse(world);
                                    skeletonHorse.setPositionAndRotation(
                                        entity.posX, entity.posY, entity.posZ,
                                        entity.rotationYaw, entity.rotationPitch
                                    );
                                    skeletonHorse.prevRotationYaw = entity.prevRotationYaw;
                                    skeletonHorse.prevRotationPitch = entity.prevRotationPitch;
                                    skeletonHorse.setGrowingAge(((EntityZombieHorse) entity).getGrowingAge());
                                    world.spawnEntity(skeletonHorse);
                                } else if (!entity.isChild()) {
                                    EntitySkeleton skeleton = new EntitySkeleton(world);
                                    skeleton.setPositionAndRotation(
                                        entity.posX, entity.posY, entity.posZ,
                                        entity.rotationYaw, entity.rotationPitch
                                    );
                                    skeleton.prevRotationYaw = entity.prevRotationYaw;
                                    skeleton.prevRotationPitch = entity.prevRotationPitch;
                                    world.spawnEntity(skeleton);
                                }
                            }*/
                        } else if (entity instanceof AbstractSkeleton || entity instanceof EntitySkeletonHorse) {

                        } else if (entity instanceof EntityCreeper) {

                        } else if (!(entity instanceof EntityPlayer) || !((EntityPlayer) entity).capabilities.isCreativeMode) {
                            entity.addPotionEffect(new PotionEffect(MobEffects.POISON, 20));
                            entity.attackEntityFrom(Wasteland.DAMAGE_ACID_RAIN, 1F);
                        }
                    }
                } else if (weatherType == WeatherType.SANDSTORM) {

                }
            }
        }
    }

    private void tickBlock(WeatherType weatherType, World world, BlockPos pos, IBlockState state) {
        if (world.isRainingAt(pos.up())) {
            Block block = state.getBlock();
            Material material = state.getMaterial();
            if (weatherType == WeatherType.ACID_RAIN) {
                if (!(block instanceof BlockDeadBush) && !(block instanceof BlockDeadGrass)) {
                    if (block instanceof BlockSapling) {
                        world.playEvent(2001, pos, Block.getStateId(state));
                        world.setBlockState(pos, Blocks.DEADBUSH.getDefaultState());
                    } else if (block == Blocks.GRASS) {
                        world.playEvent(2001, pos, Block.getStateId(state));
                        world.setBlockState(pos, Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, DirtType.COARSE_DIRT));
                    } else if (material == Material.LEAVES || material == Material.PLANTS
                            || material == Material.VINE || block instanceof IGrowable && !material.isSolid()) {
                        world.playEvent(2001, pos, Block.getStateId(state));
                        world.setBlockToAir(pos);
                    } else if (block == Blocks.WATER && state.getValue(BlockLiquid.LEVEL) == 15) {
                        world.setBlockState(pos, Wasteland.BLOCK_ACID_WATER.getDefaultState());
                    }
                }
            } else if (weatherType == WeatherType.SANDSTORM) {
                if (block instanceof BlockDeadBush && world.rand.nextInt(10) == 0) {
                    BlockPos spawnPoint = world.getSpawnPoint();
                    int x = pos.getX();
                    int y = pos.getY();
                    int z = pos.getZ();
                    if (!world.isAnyPlayerWithinRangeAt((double) x, (double) y, (double) z, 24) && spawnPoint.distanceSq(pos) >= 24 * 24) {
                        EntityTumbleweed entity = new EntityTumbleweed(world);
                        entity.setLocationAndAngles((double) x + 0.5, (double) y + 0.5, (double) z + 0.5, 0F, 0F);

                        if (entity.isNotColliding()) {
                            world.spawnEntity(entity);
                            world.setBlockToAir(pos);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void monsterDespawn(LivingSpawnEvent.AllowDespawn event) {
        EntityLivingBase entity = event.getEntityLiving();
        if ((entity instanceof EntityZombieHorse || entity instanceof EntitySkeletonHorse)
                && ((AbstractHorse) entity).isTame() && !entity.isBeingRidden()) {

            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public void waterSourceCreate(BlockEvent.CreateFluidSourceEvent event) {
        IBlockState state = event.getState();
        if (state.getMaterial() == Material.WATER) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void playerJoinEvent(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        World world = player.world;
        WastelandWorldData data = getData(world);
        if (!world.isRemote && data != null) {
            data.sync((EntityPlayerMP)player);
        }
    }

    @SubscribeEvent
    public void lootTableLoad(LootTableLoadEvent event) {
        ResourceLocation name = event.getName();
        if (name.getResourceDomain().equals(MOD_ID) && !(event.getLootTableManager() instanceof CustomLootTableManager)) {
            event.setTable(Wasteland.LOOT_TABLE_MANAGER.getLootTableFromLocation(name));
        }
    }

    public static void fillWithRain(Block block, World world, BlockPos pos) {
        WastelandWorldData data = Wasteland.PROXY.getData(world);
        if (data != null) {
            WeatherType weatherType = data.getWeatherType();
            Fluid particleFluid = weatherType.getParticleFluid();
            if (particleFluid != null) {
                try {
                    Method fillWithRain = block.getClass().getMethod("fillWithRain", World.class, BlockPos.class, Fluid.class);
                    fillWithRain.invoke(block, world, pos, particleFluid);
                } catch (ReflectiveOperationException e) {
                    if (weatherType == WeatherType.RAIN && !(block instanceof BlockCauldron)) {
                        block.fillWithRain(world, pos);
                    }
                }
            }
        }
    }

    static {
        try {
            F_worldInfo = World.class.getDeclaredField("worldInfo");
        } catch (NoSuchFieldException e) {
            try {
                F_worldInfo = World.class.getDeclaredField("field_72986_A");
            } catch (NoSuchFieldException e1) {
                e1.printStackTrace();
            }
        }
        F_worldInfo.setAccessible(true);
    }
}
