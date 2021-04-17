package com.legacy.wasteland.world.util;

import net.minecraft.util.ResourceLocation;

public class DirectResourceLocation extends ResourceLocation {
    private final boolean hasDomain;

    public DirectResourceLocation(String id) {
        super(id);
        this.hasDomain = id.contains(":");
    }

    @Override
    public String toString() {
        return this.hasDomain ? super.toString() : this.getResourcePath();
    }
}
