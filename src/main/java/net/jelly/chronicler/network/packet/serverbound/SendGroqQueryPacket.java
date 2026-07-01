package net.jelly.chronicler.network.packet.serverbound;

import net.jelly.chronicler.groq.GroqClient;
import net.jelly.chronicler.groq.GroqManager;
import net.jelly.chronicler.network.ChroniclerPacketRelay;
import net.jelly.chronicler.network.packet.BasePacket;
import net.jelly.chronicler.network.packet.clientbound.ReceiveTextResponsePacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.server.ServerLifecycleHooks;

;import java.util.concurrent.CompletableFuture;

public record SendGroqQueryPacket(String text, java.util.UUID sessionId) implements BasePacket {

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.text());
        buf.writeUUID(this.sessionId());
    }

    public static SendGroqQueryPacket decode(FriendlyByteBuf buf) {
        return new SendGroqQueryPacket(buf.readUtf(), buf.readUUID());
    }

    @Override
    public void execute(Player player) {

        if (!(player instanceof ServerPlayer serverPlayer))
            return;

        if (!GroqManager.isAvailable()) {
            ChroniclerPacketRelay.sendToPlayer(
                    new ReceiveTextResponsePacket(
                            "[Chronicler/Groq] AI is not available. Check that groq.apiKey is set in your config.",
                            sessionId
                    ),
                    serverPlayer
            );
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                return GroqClient.generate(text);
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        })
        .thenAccept(result -> {

            ServerLifecycleHooks.getCurrentServer().execute(() -> {

                ChroniclerPacketRelay.sendToPlayer(
                        new ReceiveTextResponsePacket(result, sessionId),
                        serverPlayer
                );
            });
        });

    }
}
