package net.jelly.chronicler.network.packet.clientbound;
import com.jelly.dialog_lib.client.screen.DialogueScreen;
import net.jelly.chronicler.network.packet.BasePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

// General clientbound packet to set dialogue reply
public record ReceiveTextResponsePacket(String text, java.util.UUID sessionId) implements BasePacket {

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.text());
        buf.writeUUID(this.sessionId());
    }

    public static ReceiveTextResponsePacket decode(FriendlyByteBuf buf) {
        return new ReceiveTextResponsePacket(buf.readUtf(), buf.readUUID());
    }

    @Override
    public void execute(Player player) {
        Minecraft.getInstance().execute(() -> {
            Screen current = Minecraft.getInstance().screen;
            if (current instanceof DialogueScreen dialogueScreen
                    && dialogueScreen.getSessionId().equals(sessionId)) {
                dialogueScreen.setBlocked(false);
                dialogueScreen.setDialogueAnswer(Component.literal(text));
            }
        });
    }
}
