package net.jelly.chronicler.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Consider removal of BasePacket in delegation to DialgoueLib's identical BasePacket
public interface BasePacket {
    void encode(FriendlyByteBuf var1);

    default boolean handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            this.execute(context.get().getSender());
        });
        return true;
    }

    void execute(Player var1);
}
