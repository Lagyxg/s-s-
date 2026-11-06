package com.example.chaosmod;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(ChaosMod.MODID)
public class ChaosMod {
    public static final String MODID = "chaosmod";

    public ChaosMod() {
        MinecraftForge.EVENT_BUS.register(new ChaosEvents());
        MinecraftForge.EVENT_BUS.register(new RebelItemsEvents());
    }
}
