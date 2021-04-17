package com.legacy.wasteland.coremod;

import com.google.common.eventbus.EventBus;
import com.legacy.wasteland.WastelandVersion;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModMetadata;

public class WastelandModContainer extends DummyModContainer {
    private final ModMetadata md = new ModMetadata();

    public WastelandModContainer() {
        super("Wasteland Coremod");
        this.md.modId = WastelandVersion.MOD_ID + "-coremod";
        this.md.name = "Wasteland Coremod";
        this.md.version = "0.0.1-ALPHA";
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller) {
        bus.register(this);
        return true;
    }

    @Override
    public String getModId() {
        return this.md.modId;
    }

    @Override
    public String getName() {
        return this.md.name;
    }

    @Override
    public String getVersion() {
        return this.md.version;
    }

    @Override
    public String getDisplayVersion() {
        return this.md.version;
    }

    @Override
    public ModMetadata getMetadata() {
        return this.md;
    }
}
