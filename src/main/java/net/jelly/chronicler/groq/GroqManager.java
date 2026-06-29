package net.jelly.chronicler.groq;

import net.jelly.chronicler.ChroniclerMod;
import net.jelly.chronicler.config.GroqConfig;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = ChroniclerMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GroqManager {

    private static final Logger LOGGER = LogManager.getLogger();
    private static boolean available = false;

    public static boolean isAvailable() {
        return available;
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        initialize();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        available = false;
        LOGGER.info("[Groq] Shutting down.");
    }

    public static void initialize() {
        String apiKey = GroqConfig.GROQ_API_KEY.get();

        if (apiKey == null || apiKey.isBlank()) {
            LOGGER.warn("[Groq] No API key configured. Set groq.apiKey in your config. AI features disabled.");
            available = false;
            return;
        }

        LOGGER.info("[Groq] API key found. Testing connectivity...");

        try {
            String result = GroqClient.generate("Say 'OK' and nothing else.");
            LOGGER.info("[Groq] Connectivity test passed. Response: {}", result.trim());
            available = true;
            LOGGER.info("[Groq] Ready. Model: {}", GroqConfig.GROQ_MODEL.get());
        } catch (Exception e) {
            available = false;
            LOGGER.error("[Groq] Connectivity test failed: {}", e.getMessage());
        }
    }
}