package net.jelly.chronicler.network.packet.serverbound;;

import net.jelly.chronicler.network.ChroniclerPacketRelay;
import net.jelly.chronicler.network.packet.BasePacket;
import net.jelly.chronicler.network.packet.clientbound.ReceiveTextResponsePacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public record SendTextInputPacket(String text) implements BasePacket {

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.text());
    }

    public static SendTextInputPacket decode(FriendlyByteBuf buf) {
        return new SendTextInputPacket(buf.readUtf());
    }

    @Override
    public void execute(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            // echo back to client
            ChroniclerPacketRelay.sendToPlayer(
                    new ReceiveTextResponsePacket(text),
                    serverPlayer
            );
        }
    }
}
