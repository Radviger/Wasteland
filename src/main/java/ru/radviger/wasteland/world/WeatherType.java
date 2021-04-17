package com.legacy.wasteland.world;

import com.legacy.wasteland.Wasteland;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import java.util.Random;

public enum  WeatherType {
    CLEAR,
    RAIN {
        @Override
        public Fluid getParticleFluid() {
            return FluidRegistry.WATER;
        }
    },
    ACID_RAIN(0xFF_B3FF1A) {
        @Override
        public Fluid getParticleFluid() {
            return Wasteland.FLUID_ACID_WATER;
        }
    },
    SANDSTORM(0xFF_A95821);

    private final int particleColor;

    WeatherType() {
        this(0xFF_FFFFFF);
    }

    WeatherType(int particleColor) {
        this.particleColor = particleColor;
    }

    public int getParticleColor() {
        return particleColor;
    }

    public Fluid getParticleFluid() {
        return null;
    }

    public static WeatherType random(long time, Random rand) {
        if (rand.nextInt(7) == 0 || time >= 13000 && rand.nextInt(5) == 0) {
            return SANDSTORM;
        } else if (rand.nextInt(3) == 0) {
            return ACID_RAIN;
        }
        return RAIN;
    }

    public boolean isClear() {
        return this == CLEAR;
    }

    public boolean isRain() {
        return this == RAIN || this == ACID_RAIN;
    }

    public boolean isSandstorm() {
        return this == SANDSTORM;
    }
}
