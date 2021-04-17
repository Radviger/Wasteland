package com.legacy.wasteland.coremod;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import javax.annotation.Nullable;
import java.util.Map;

import static com.legacy.wasteland.WastelandVersion.MOD_ID;

@IFMLLoadingPlugin.Name(MOD_ID + "-coremod")
@IFMLLoadingPlugin.SortingIndex(1001)
public class WastelandCoremod implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return new String[] {
            "com.legacy.wasteland.coremod.asm.Transformer"
        };
    }

    @Override
    public String getModContainerClass() {
        return "com.legacy.wasteland.coremod.WastelandModContainer";
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> map) {

    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
