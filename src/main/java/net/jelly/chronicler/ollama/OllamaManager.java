package net.jelly.chronicler.ollama;

import net.jelly.chronicler.ChroniclerMod;
import net.jelly.chronicler.config.OllamaConfig;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = ChroniclerMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class OllamaManager {

    private static final Logger LOGGER = LogManager.getLogger();
    private static Process ollamaProcess = null;
    private static boolean available = false;

    public static boolean isAvailable() {
        return available;
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        startOllama();
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (!event.getLevel().isClientSide()) {
            stopOllama();
        }
    }


    public static void startOllama() {
        LOGGER.info("[Ollama] startOllama() called. Checking for running instance...");

        if (OllamaClient.isServerRunning()) {
            LOGGER.info("[Ollama] isServerRunning() returned true. Setting available = true.");
            available = true;
            return;
        }

        LOGGER.info("[Ollama] isServerRunning() returned false. Last error: {}", OllamaClient.getServerError());

        String execPath = OllamaConfig.OLLAMA_PATH.get();

        try {
            if (OllamaClient.isServerRunning()) {
                LOGGER.info("[Ollama] Server already running externally, skipping spawn.");
                available = true;
                return;
            }

            ProcessBuilder pb = new ProcessBuilder(execPath, "serve");
            pb.redirectErrorStream(true);
            pb.redirectOutput(new File("ollama_startup.log"));
            ollamaProcess = pb.start();

//            pb.redirectErrorStream(true);
//            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
//            ollamaProcess = pb.start();

            LOGGER.info("[Ollama] Process spawned (PID {}), waiting for server to respond...", ollamaProcess.pid());

            // Retry up to 20 times, 500ms apart (10 seconds total)
            boolean responded = false;
            for (int i = 0; i < 20; i++) {
                Thread.sleep(500);

                // If the process died immediately, no point retrying
                if (!ollamaProcess.isAlive()) {
                    LOGGER.error("[Ollama] Process exited early with code {}. Check your executable path: '{}'",
                            ollamaProcess.exitValue(), execPath);
                    available = false;
                    return;
                }

                if (OllamaClient.isServerRunning()) {
                    responded = true;
                    LOGGER.info("[Ollama] Server responded after ~{}ms.", (i + 1) * 500);
                    break;
                }

                LOGGER.debug("[Ollama] Attempt {}/20 — not yet responding ({})", i + 1, OllamaClient.getServerError());
            }

            if (responded) {
                available = true;
                LOGGER.info("[Ollama] Ready. Model: {}", OllamaConfig.OLLAMA_MODEL.get());
            } else {
                available = false;
                LOGGER.error("[Ollama] Server did not respond after 10 seconds. Last error: {}", OllamaClient.getServerError());
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (ollamaProcess != null && ollamaProcess.isAlive()) {
                    ollamaProcess.destroy();
                }
            }));

        } catch (IOException e) {
            available = false;
            LOGGER.warn("[Ollama] Could not launch '{}'. Is Ollama installed? ({})", execPath, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void stopOllama() {
        available = false;
        // Only kill it if we spawned it — don't kill an externally running instance
        if (ollamaProcess != null && ollamaProcess.isAlive()) {
            ollamaProcess.destroy();
            LOGGER.info("[Ollama] Server stopped.");
            ollamaProcess = null;
        }
    }
}
