package net.jelly.chronicler.ollama;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.jelly.chronicler.ChroniclerMod;
import net.jelly.chronicler.config.OllamaConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = ChroniclerMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class OllamaCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("queryollama")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.argument("prompt", StringArgumentType.greedyString())
                                        .executes(OllamaCommand::runQuery)
                        )
                        .executes(ctx -> {
                            ctx.getSource().sendFailure(Component.literal("Usage: /queryollama <prompt>"));
                            return 0;
                        })
        );
    }

    private static int runQuery(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String prompt = StringArgumentType.getString(ctx, "prompt");

        LOGGER.info("[Ollama] Command fired. available={}, isServerRunning()={}",
                OllamaManager.isAvailable(), OllamaClient.isServerRunning());

        if (!OllamaManager.isAvailable()) {
            source.sendFailure(Component.literal("[Ollama] AI is not available. Check that Ollama is installed and the path is configured correctly."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("[Ollama] Querying... (this may take a moment)"), false);

        String model = OllamaConfig.OLLAMA_MODEL.get();

        CompletableFuture.supplyAsync(() -> {
            try {
                return OllamaClient.generate(model, prompt);
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }).thenAccept(result -> {
            ServerLifecycleHooks.getCurrentServer().execute(() -> {
                source.sendSuccess(() -> Component.literal("[Ollama] " + result), false);
            });
        });

        return 1;
    }
}