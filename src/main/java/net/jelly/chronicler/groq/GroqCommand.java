package net.jelly.chronicler.groq;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.jelly.chronicler.ChroniclerMod;
import net.jelly.chronicler.backend.graph.debug.GraphTestCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = ChroniclerMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GroqCommand {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("querygroq")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.argument("prompt", StringArgumentType.greedyString())
                                        .executes(GroqCommand::runQuery)
                        )
                        .executes(ctx -> {
                            ctx.getSource().sendFailure(Component.literal("Usage: /querygroq <prompt>"));
                            return 0;
                        })
        );
    }

    private static int runQuery(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String prompt = StringArgumentType.getString(ctx, "prompt");

        if (!GroqManager.isAvailable()) {
            source.sendFailure(Component.literal(
                    "[Chronicler/Groq] AI is not available. Check that groq.apiKey is set in your config."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("[Chronicler/Groq] Querying..."), false);

        CompletableFuture.supplyAsync(() -> {
            try {
                return GroqClient.generate(prompt);
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }).thenAccept(result -> {
            ServerLifecycleHooks.getCurrentServer().execute(() -> {
                source.sendSuccess(() -> Component.literal("[Chronicler/Groq] " + result), false);
            });
        });

        return 1;
    }
}
