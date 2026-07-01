package net.jelly.chronicler.network;

import net.jelly.chronicler.ChroniclerMod;
import net.jelly.chronicler.network.packet.BasePacket;
import net.jelly.chronicler.network.packet.clientbound.ReceiveTextResponsePacket;
import net.jelly.chronicler.network.packet.serverbound.SendGroqQueryPacket;
import net.jelly.chronicler.network.packet.serverbound.SendTextInputPacket;
import net.jelly.chronicler.network.packet.serverbound.TestPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Function;

public class ChroniclerPacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ChroniclerMod.MODID, "main"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals
    );

    private static int index;

    private static <MSG extends BasePacket> void register(final Class<MSG> packet, Function<FriendlyByteBuf, MSG> decoder) {
        INSTANCE.messageBuilder(packet, index++).encoder(BasePacket::encode).decoder(decoder).consumerMainThread(BasePacket::handle).add();
    }

    public static synchronized void register() {
        register(TestPacket.class, TestPacket::decode);

        register(SendTextInputPacket.class, SendTextInputPacket::decode);
        register(SendGroqQueryPacket.class, SendGroqQueryPacket::decode);

        register(ReceiveTextResponsePacket.class, ReceiveTextResponsePacket::decode);

    }
}
