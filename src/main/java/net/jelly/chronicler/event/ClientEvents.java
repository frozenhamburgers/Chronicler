package net.jelly.chronicler.event;

import net.jelly.chronicler.ChroniclerMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;


public class ClientEvents {
    // FORGE CLIENT EVENTS

    @Mod.EventBusSubscriber(modid = ChroniclerMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeClientEvents {
    }





    // MOD CLIENT EVENTS
    @Mod.EventBusSubscriber(modid= ChroniclerMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModClientEvents {
    }
}
