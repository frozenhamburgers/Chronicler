package net.jelly.chronicler;

import com.mojang.logging.LogUtils;
import net.jelly.chronicler.config.GroqConfig;
import net.jelly.chronicler.config.OllamaConfig;
import net.jelly.chronicler.network.ChroniclerPacketHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ChroniclerMod.MODID)
public class ChroniclerMod
{
    // Define mod id i n a common place for everything to reference
    public static final String MODID = "chronicler";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public ChroniclerMod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Config
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, OllamaConfig.SPEC, "chronicler-ollama.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, GroqConfig.SPEC, "chronicler-groq.toml");

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    // common setup
    private void commonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(ChroniclerPacketHandler::register);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
        }
    }
}
