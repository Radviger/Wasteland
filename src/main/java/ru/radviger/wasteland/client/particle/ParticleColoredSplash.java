package com.legacy.wasteland.client.particle;

import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleSplash;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class ParticleColoredSplash extends ParticleSplash {
    protected ParticleColoredSplash(World world, double x, double y, double z, double mx, double my, double mz, float r, float g, float b) {
        super(world, x, y, z, mx, my, mz);
        this.particleRed = r;
        this.particleGreen = g;
        this.particleBlue = b;
    }

    public static class Factory implements IParticleFactory {
        @Nullable
        @Override
        public Particle createParticle(int id, World world, double x, double y, double z, double mx, double my, double mz, int... data) {
            float r = 1F;
            float g = 1F;
            float b = 1F;
            if (data.length >= 1) {
                int rgb = data[0];
                r = ((rgb >> 16) & 255) / 255F;
                g = ((rgb >> 8) & 255) / 255F;
                b = ((rgb >> 0) & 255) / 255F;
            }
            return new ParticleColoredSplash(world, x, y, z, mx, my, mz, r, g, b);
        }
    }
}
