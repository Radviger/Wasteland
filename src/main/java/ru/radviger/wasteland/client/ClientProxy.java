package com.legacy.wasteland.client;

import com.legacy.wasteland.CommonProxy;
import com.legacy.wasteland.Wasteland;
import com.legacy.wasteland.client.particle.ParticleColoredDroplet;
import com.legacy.wasteland.client.particle.ParticleColoredSplash;
import com.legacy.wasteland.client.render.RenderClouds;
import com.legacy.wasteland.client.render.RenderTumbleweed;
import com.legacy.wasteland.client.render.RenderWeather;
import com.legacy.wasteland.common.entity.EntityTumbleweed;
import com.legacy.wasteland.world.WastelandWorld;
import com.legacy.wasteland.world.WastelandWorldData;
import com.legacy.wasteland.world.WeatherType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.item.Item;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import static com.legacy.wasteland.WastelandVersion.MOD_ID;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        super.preInit();
        RenderingRegistry.registerEntityRenderingHandler(EntityTumbleweed.class, RenderTumbleweed::new);
    }

    @Override
    public void init() {
        super.init();
        Minecraft client = Minecraft.getMinecraft();
        ParticleManager particleManager = client.effectRenderer;

        particleManager.registerParticle(EnumParticleTypes.WATER_SPLASH.getParticleID(), new ParticleColoredSplash.Factory());
        particleManager.registerParticle(EnumParticleTypes.WATER_DROP.getParticleID(), new ParticleColoredDroplet.Factory());
    }

    /*@SubscribeEvent
    public void blockColorsInit(ColorHandlerEvent.Block event) {
        event.getBlockColors().registerBlockColorHandler(
            (state, blockAccessor, pos, tintIndex) -> 0x946428,
            Wasteland.BLOCK_DEAD_GRASS
        );
    }

    @SubscribeEvent
    public void itemColorsInit(ColorHandlerEvent.Item event) {
        event.getItemColors().registerItemColorHandler(
            (item, tintIndex) -> 0x946428,
            Wasteland.BLOCK_DEAD_GRASS
        );
    }*/

    @SubscribeEvent
    public void onModelsRegister(ModelRegistryEvent event) {
        ModelLoader.setCustomStateMapper(Wasteland.BLOCK_ACID_WATER, new StateMapperBase() {
            @Override
            protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
                return new ModelResourceLocation(MOD_ID + ":acid_water", "fluid");
            }
        });
        ModelLoader.setCustomStateMapper(Wasteland.BLOCK_DEAD_GRASS, new StateMapperBase() {
            @Override
            protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
                return new ModelResourceLocation(MOD_ID + ":dead_grass", "normal");
            }
        });
        Item deadGrass = Item.getItemFromBlock(Wasteland.BLOCK_DEAD_GRASS);
        ModelLoader.setCustomModelResourceLocation(deadGrass, 0, new ModelResourceLocation(MOD_ID + ":dead_grass", "inventory"));
        ModelLoader.registerItemVariants(deadGrass, new ResourceLocation(MOD_ID, "dead_grass"));
        Item deadSapling = Item.getItemFromBlock(Wasteland.BLOCK_DEAD_SAPLING);
        ModelLoader.setCustomModelResourceLocation(deadSapling, 0, new ModelResourceLocation(MOD_ID + ":dead_sapling", "inventory"));
        ModelLoader.registerItemVariants(deadSapling, new ResourceLocation(MOD_ID, "dead_sapling"));
    }

    @SubscribeEvent
    public void monsterRenderOnFire(RenderLivingEvent.Pre<EntityLivingBase> event) {
        EntityLivingBase entity = event.getEntity();

        if (entity.isBurning() && WastelandWorld.isWasteland(entity.world) && entity instanceof EntityMob) {
            entity.extinguish();
        }
    }

    @SubscribeEvent
    public void fogRenderColor(EntityViewRenderEvent.FogColors event) {
        Entity entity = event.getEntity();
        World world = entity.world;
        WastelandWorldData data = getData(world);
        if (data != null) {
            WeatherType weatherType = data.getInterpolatedWeatherType();

            float partialTicks = (float) event.getRenderPartialTicks();
            float celestialAngle = world.getCelestialAngle(partialTicks);

            event.setRed((134F / 255F) * celestialAngle);
            event.setGreen((104F / 255F) * celestialAngle);
            event.setBlue((54F / 255F) * celestialAngle);

            float fogDensity = data.getInterpolatedFogDensity(partialTicks);

            if (weatherType.isSandstorm() && fogDensity > 0) {
                float weatherStrength = data.getInterpolatedWeatherStrength(partialTicks);
                int rgb = weatherType.getParticleColor();
                float fr = event.getRed();
                float fg = event.getGreen();
                float fb = event.getBlue();
                float tr = ((rgb >> 16) & 255) / 255F;
                float tg = ((rgb >> 8) & 255) / 255F;
                float tb = (rgb & 255) / 255F;
                event.setRed(fr + (tr - fr) * weatherStrength);
                event.setGreen(fg + (tg - fg) * weatherStrength);
                event.setBlue(fb + (tb - fb) * weatherStrength);
            }
        }
    }

    @SubscribeEvent
    public void fogRenderDensity(EntityViewRenderEvent.FogDensity event) {
        Entity entity = event.getEntity();
        World world = entity.world;
        WastelandWorldData data = getData(world);
        if (data != null) {
            WeatherType weatherType = data.getInterpolatedWeatherType();
            float partialTicks = (float) event.getRenderPartialTicks();
            float fogDensity = data.getInterpolatedFogDensity(partialTicks);
            if (weatherType.isSandstorm() && fogDensity > 0) {
                float weatherStrength = data.getInterpolatedWeatherStrength(partialTicks);
                GlStateManager.setFog(GlStateManager.FogMode.EXP);
                event.setDensity(0.2F * weatherStrength * fogDensity);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void worldLoad(WorldEvent.Load event) {
        super.worldLoad(event);
        World world = event.getWorld();
        if (world.isRemote && WastelandWorld.isWasteland(world)) {
            world.provider.setWeatherRenderer(new RenderWeather());
            world.provider.setCloudRenderer(new RenderClouds());
        }
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.side == Side.CLIENT) {
            Minecraft client = Minecraft.getMinecraft();
            World world = client.world;
            if (world != null) {
                WastelandWorldData data = getData(world);
                if (data != null) {
                    Entity entity = client.getRenderViewEntity();
                    if (entity != null) {
                        float partialTicks = client.getRenderPartialTicks();
                        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
                        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
                        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
                        int skyLight = 0;
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dy = -1; dy <= 1; dy++) {
                                for (int dz = -1; dz <= 1; dz++) {
                                    BlockPos pos = new BlockPos(x + dx, y + dy, z + dz);
                                    skyLight += world.getLightFor(EnumSkyBlock.SKY, pos);
                                }
                            }
                        }
                        data.setFogDensity((float) skyLight / (9 * 15F));
                    }
                    data.tickWeather();
                }
            }
        }
    }
}
