package com.legacy.wasteland.client.render;

import com.legacy.wasteland.Wasteland;
import com.legacy.wasteland.world.WastelandWorldData;
import com.legacy.wasteland.world.WeatherType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.Random;

public class RenderWeather extends IRenderHandler {
    private static final ResourceLocation RAIN_TEXTURES = new ResourceLocation("textures/environment/rain.png");
    private static final ResourceLocation SNOW_TEXTURES = new ResourceLocation("textures/environment/snow.png");

    private static Field F_rendererUpdateCount, F_rainSoundCounter;

    private final float[] rainXCoords = new float[1024];
    private final float[] rainZCoords = new float[1024];

    public RenderWeather() {
        for(int x = 0; x < 32; ++x) {
            for(int z = 0; z < 32; ++z) {
                float dx = (float)(z - 16);
                float dz = (float)(x - 16);
                float d = MathHelper.sqrt(dx * dx + dz * dz);
                this.rainXCoords[x << 5 | z] = -dz / d;
                this.rainZCoords[x << 5 | z] = dx / d;
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void render(float partialTicks, WorldClient world, Minecraft client) {
        WastelandWorldData data = Wasteland.PROXY.getData(world);
        float weatherStrength = data.getInterpolatedWeatherStrength(partialTicks);
        WeatherType weatherType = data.getInterpolatedWeatherType();
        if (weatherStrength > 0) {
            EntityRenderer entityRenderer = client.entityRenderer;
            int rendererUpdateCount = getRendererUpdateCount(entityRenderer);
            entityRenderer.enableLightmap();
            Entity entity = client.getRenderViewEntity();
            Random rand = world.rand;
            int blockX = MathHelper.floor(entity.posX);
            int blockY = MathHelper.floor(entity.posY);
            int blockZ = MathHelper.floor(entity.posZ);
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.getBuffer();
            GlStateManager.disableCull();
            GlStateManager.glNormal3f(0, 1, 0);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
            GlStateManager.alphaFunc(516, 0.1F);
            double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double)partialTicks;
            double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double)partialTicks;
            double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double)partialTicks;
            int lastViewerBlockY = MathHelper.floor(y);
            int range = client.gameSettings.fancyGraphics ? 10 : 5;

            int j1 = -1;
            float dustDelta = (float)rendererUpdateCount + partialTicks;
            buf.setTranslation(-x, -y, -z);
            GlStateManager.color(1F, 1F, 1F, 1F);
            MutableBlockPos pos = new MutableBlockPos();

            int particleColor = weatherType.getParticleColor();
            float r = ((particleColor >> 16) & 255) / 255F;
            float g = ((particleColor >> 8) & 255) / 255F;
            float b = (particleColor & 255) / 255F;

            double dustSpeedMod = (weatherType.isSandstorm() ? 0.09 : 0.01);

            for(int ofz = blockZ - range; ofz <= blockZ + range; ++ofz) {
                for(int ofx = blockX - range; ofx <= blockX + range; ++ofx) {
                    int idx = (ofz - blockZ + 16) * 32 + ofx - blockX + 16;
                    double rx = (double)this.rainXCoords[idx] * 0.5;
                    double rz = (double)this.rainZCoords[idx] * 0.5;
                    pos.setPos(ofx, 0, ofz);
                    Biome biome = world.getBiome(pos);
                    if (biome.canRain() || biome.getEnableSnow()) {
                        int precipitationHeight = world.getPrecipitationHeight(pos).getY();
                        int yMin = Math.max(blockY - range, precipitationHeight);
                        int yMax = Math.max(blockY + range, precipitationHeight);
                        int midY = Math.max(precipitationHeight, lastViewerBlockY);

                        if (yMin != yMax) {
                            rand.setSeed((long)(ofx * ofx * 3121 + ofx * 45238971 ^ ofz * ofz * 418711 + ofz * 13761));
                            pos.setPos(ofx, yMin, ofz);
                            float biomeTemperature = biome.getTemperature(pos);
                            float temperature = world.getBiomeProvider().getTemperatureAtHeight(biomeTemperature, precipitationHeight);
                            if (temperature >= 0.15 && !weatherType.isSandstorm()) {
                                if (j1 != 0) {
                                    if (j1 >= 0) {
                                        tess.draw();
                                    }

                                    j1 = 0;
                                    client.getTextureManager().bindTexture(RAIN_TEXTURES);
                                    buf.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
                                }

                                double d5 = -((double)(rendererUpdateCount + ofx * ofx * 3121 + ofx * 45238971 + ofz * ofz * 418711 + ofz * 13761 & 31) + (double)partialTicks) / 32 * (3 + rand.nextDouble());
                                double dx = (double)((float)ofx + 0.5F) - entity.posX;
                                double dz = (double)((float)ofz + 0.5F) - entity.posZ;
                                float mod = MathHelper.sqrt(dx * dx + dz * dz) / (float)range;
                                float alpha = ((1F - mod * mod) * 0.5F + 0.5F) * weatherStrength;
                                pos.setPos(ofx, midY, ofz);
                                int light = world.getCombinedLight(pos, 0);
                                int lx = light >> 16 & 0xFFFF;
                                int ly = light & 0xFFFF;
                                buf.pos((double)ofx - rx + 0.5, (double)yMax, (double)ofz - rz + 0.5).tex(0, (double)yMin * 0.25D + d5).color(r, g, b, alpha).lightmap(lx, ly).endVertex();
                                buf.pos((double)ofx + rx + 0.5, (double)yMax, (double)ofz + rz + 0.5).tex(1, (double)yMin * 0.25D + d5).color(r, g, b, alpha).lightmap(lx, ly).endVertex();
                                buf.pos((double)ofx + rx + 0.5, (double)yMin, (double)ofz + rz + 0.5).tex(1, (double)yMax * 0.25D + d5).color(r, g, b, alpha).lightmap(lx, ly).endVertex();
                                buf.pos((double)ofx - rx + 0.5, (double)yMin, (double)ofz - rz + 0.5).tex(0, (double)yMax * 0.25D + d5).color(r, g, b, alpha).lightmap(lx, ly).endVertex();
                            } else {
                                if (j1 != 1) {
                                    if (j1 >= 0) {
                                        tess.draw();
                                    }

                                    j1 = 1;
                                    client.getTextureManager().bindTexture(SNOW_TEXTURES);
                                    buf.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
                                }

                                double d5 = (double)(-((float)(rendererUpdateCount & 511) + partialTicks) / 512F);
                                double d6 = rand.nextDouble() + (double)dustDelta * dustSpeedMod * (double)((float) rand.nextGaussian());
                                double d7 = rand.nextDouble() + (double)(dustDelta * (float) rand.nextGaussian()) * dustSpeedMod / 10.0;
                                double dx = (double)((float)ofx + 0.5F) - entity.posX;
                                double dz = (double)((float)ofz + 0.5F) - entity.posZ;
                                float mod = MathHelper.sqrt(dx * dx + dz * dz) / (float)range;
                                float alpha = ((1F - mod * mod) * 0.3F + 0.5F) * weatherStrength;
                                pos.setPos(ofx, midY, ofz);
                                int light = (world.getCombinedLight(pos, 0) * 3 + 0xF000F0) / 4;
                                int lx = light >> 16 & 0xFFFF;
                                int ly = light & 0xFFFF;
                                buf.pos((double)ofx - rx + 0.5, (double)yMax, (double)ofz - rz + 0.5).tex(0 + d6, (double)yMin * 0.25 + d5 + d7).color(r, g, b, alpha).lightmap(lx, ly).endVertex();
                                buf.pos((double)ofx + rx + 0.5, (double)yMax, (double)ofz + rz + 0.5).tex(1 + d6, (double)yMin * 0.25 + d5 + d7).color(r, g, b, alpha).lightmap(lx, ly).endVertex();
                                buf.pos((double)ofx + rx + 0.5, (double)yMin, (double)ofz + rz + 0.5).tex(1 + d6, (double)yMax * 0.25 + d5 + d7).color(r, g, b, alpha).lightmap(lx, ly).endVertex();
                                buf.pos((double)ofx - rx + 0.5, (double)yMin, (double)ofz - rz + 0.5).tex(0 + d6, (double)yMax * 0.25 + d5 + d7).color(r, g, b, alpha).lightmap(lx, ly).endVertex();
                            }
                        }
                    }
                }
            }

            if (j1 >= 0) {
                tess.draw();
            }

            buf.setTranslation(0, 0, 0);
            GlStateManager.enableCull();
            GlStateManager.disableBlend();
            GlStateManager.alphaFunc(516, 0.1F);
            entityRenderer.disableLightmap();
        }
    }

    public static void addRainParticles() {
        Minecraft client = Minecraft.getMinecraft();
        WorldClient world = client.world;
        WastelandWorldData data = Wasteland.PROXY.getData(world);
        if (data == null) {
            return;
        }
        if (!(world.provider.getWeatherRenderer() instanceof RenderWeather)) {
            return;
        }
        float partialTicks = client.getRenderPartialTicks();
        EntityRenderer entityRenderer = client.entityRenderer;
        GameSettings settings = client.gameSettings;

        WeatherType weatherType = data.getInterpolatedWeatherType();
        int particleColor = weatherType.getParticleColor();

        Random random = new Random((long) getRendererUpdateCount(entityRenderer) * 312987231L);
        Entity entity = client.getRenderViewEntity();
        BlockPos pos = new BlockPos(entity);

        float weatherStrength = data.getInterpolatedWeatherStrength(partialTicks);
        if (!settings.fancyGraphics) {
            weatherStrength /= 2.0F;
        }

        if (weatherStrength != 0) {
            double dx = 0;
            double dy = 0;
            double dz = 0;
            int particleSources = 0;
            int k = (int) (100.0F * weatherStrength * weatherStrength);
            if (settings.particleSetting == 1) {
                k >>= 1;
            } else if (settings.particleSetting == 2) {
                k = 0;
            }

            for (int l = 0; l < k; ++l) {
                BlockPos upperLiquidPos = world.getPrecipitationHeight(pos.add(random.nextInt(10) - random.nextInt(10), 0, random.nextInt(10) - random.nextInt(10)));
                Biome liquidBiome = world.getBiome(upperLiquidPos);
                BlockPos liquidPos = upperLiquidPos.down();
                IBlockState liquidState = world.getBlockState(liquidPos);
                if (upperLiquidPos.getY() <= pos.getY() + 10 && upperLiquidPos.getY() >= pos.getY() - 10 && liquidBiome.canRain() && liquidBiome.getTemperature(upperLiquidPos) >= 0.15F) {
                    double d3 = random.nextDouble();
                    double d4 = random.nextDouble();
                    AxisAlignedBB bounding = liquidState.getBoundingBox(world, liquidPos);
                    if (liquidState.getMaterial() != Material.LAVA && liquidState.getBlock() != Blocks.MAGMA || !weatherType.isRain()) {
                        if (liquidState.getMaterial() != Material.AIR) {
                            ++particleSources;
                            if (random.nextInt(particleSources) == 0) {
                                dx = (double) liquidPos.getX() + d3;
                                dy = (double) ((float) liquidPos.getY() + 0.1F) + bounding.maxY - 1.0;
                                dz = (double) liquidPos.getZ() + d4;
                            }

                            if (weatherType.isRain()) {
                                world.spawnParticle(EnumParticleTypes.WATER_DROP, (double) liquidPos.getX() + d3, (double) ((float) liquidPos.getY() + 0.1F) + bounding.maxY, (double) liquidPos.getZ() + d4, 0, 0, 0, particleColor);
                            }
                        }
                    } else {
                        world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, (double) upperLiquidPos.getX() + d3, (double) ((float) upperLiquidPos.getY() + 0.1F) - bounding.minY, (double) upperLiquidPos.getZ() + d4, 0, 0, 0);
                    }
                }
            }

            if (particleSources > 0) {
                if (weatherType.isRain() && random.nextInt(3) < postIncrRainSoundCounter(entityRenderer)) {
                    setRainSoundCounter(entityRenderer, 0);
                    if (dy > (double) (pos.getY() + 1) && world.getPrecipitationHeight(pos).getY() > MathHelper.floor((float) pos.getY())) {
                        world.playSound(dx, dy, dz, SoundEvents.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, 0.1F, 0.5F, false);
                    } else {
                        world.playSound(dx, dy, dz, SoundEvents.WEATHER_RAIN, SoundCategory.WEATHER, 0.2F, 1F, false);
                    }
                }
                if (weatherType.isSandstorm() && random.nextInt(100) < postIncrRainSoundCounter(entityRenderer)) {
                    setRainSoundCounter(entityRenderer, 0);
                    world.playSound(entity.posX, entity.posY, entity.posZ, Wasteland.SOUND_SANDSTORM, SoundCategory.WEATHER, 0.7F, 1F, false);
                }
            }
        }
    }

    private static int getRendererUpdateCount(EntityRenderer entityRenderer) {
        try {
            return (int) F_rendererUpdateCount.get(entityRenderer);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static int postIncrRainSoundCounter(EntityRenderer entityRenderer) {
        int result = getRainSoundCounter(entityRenderer);
        setRainSoundCounter(entityRenderer, result + 1);
        return result;
    }

    private static int getRainSoundCounter(EntityRenderer entityRenderer) {
        try {
            return (int) F_rainSoundCounter.get(entityRenderer);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setRainSoundCounter(EntityRenderer entityRenderer, int value) {
        try {
            F_rainSoundCounter.set(entityRenderer, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static {
        try {
            F_rendererUpdateCount = EntityRenderer.class.getDeclaredField("rendererUpdateCount");
        } catch (NoSuchFieldException e) {
            try {
                F_rendererUpdateCount = EntityRenderer.class.getDeclaredField("field_78529_t");
            } catch (NoSuchFieldException e1) {
                e1.printStackTrace();
            }
        }
        try {
            F_rainSoundCounter = EntityRenderer.class.getDeclaredField("rainSoundCounter");
        } catch (NoSuchFieldException e) {
            try {
                F_rainSoundCounter = EntityRenderer.class.getDeclaredField("field_78534_ac");
            } catch (NoSuchFieldException e1) {
                e1.printStackTrace();
            }
        }
        F_rendererUpdateCount.setAccessible(true);
        F_rainSoundCounter.setAccessible(true);
    }
}
