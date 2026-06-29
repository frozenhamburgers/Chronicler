package net.jelly.chronicler.network.packet.serverbound;

import com.jelly.dialog_lib.events.ServerCustomInteractEvent;
import net.jelly.chronicler.network.packet.BasePacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public record TestPacket(int integer) implements BasePacket {

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.integer());
    }

    public static TestPacket decode(FriendlyByteBuf buf) {
        return new TestPacket(buf.readInt());
    }

    @Override
    public void execute(@Nullable Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            System.out.println("Packet received on server, id: " + integer);
        }
    }
}
