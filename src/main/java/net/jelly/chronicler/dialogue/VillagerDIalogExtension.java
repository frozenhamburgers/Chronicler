package net.jelly.chronicler.dialogue;

import com.jelly.dialog_lib.api.entity.EntityDialogueExtension;
import com.jelly.dialog_lib.api.entity.IEntityDialogueExtension;
import com.jelly.dialog_lib.client.screen.DialogueScreen;
import com.jelly.dialog_lib.client.screen.TextInputDialogueScreen;
import com.jelly.dialog_lib.client.screen.builder.StreamDialogueScreenBuilder;
import net.jelly.chronicler.ChroniclerMod;
import net.jelly.chronicler.network.ChroniclerPacketHandler;
import net.jelly.chronicler.network.ChroniclerPacketRelay;
import net.jelly.chronicler.network.packet.serverbound.SendGroqQueryPacket;
import net.jelly.chronicler.network.packet.serverbound.SendTextInputPacket;
import net.jelly.chronicler.network.packet.serverbound.TestPacket;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@EntityDialogueExtension(modId = ChroniclerMod.MODID)
public class VillagerDialogExtension implements IEntityDialogueExtension<Villager> {

    @Override
    public EntityType<Villager> getEntityType() {
        return EntityType.VILLAGER;
    }

    @Override
    public boolean canInteractWith(Player player, Villager currentTalking) {
        return true;
    }

    @Override
    public void onPlayerInteract(Player player, Villager currentTalking, InteractionHand hand) {
        IEntityDialogueExtension.super.onPlayerInteract(player, currentTalking, hand);
//        player.level().playSound(null, currentTalking.getOnPos(), BountifulNpcSounds.ON_RECEPTIONIST_INTERACT.get(),
//                SoundSource.VOICE, 1.0F, 1.0F);
    }

    /**
     * Retrieve information for the client to construct the conversation.
     */
    @Override
    public CompoundTag getServerData(ServerPlayer player, Villager currentTalking, InteractionHand hand, CompoundTag senderData) {
//        if(player.getItemInHand(hand).is(BountifulContent.INSTANCE.getBOUNTY_ITEM())) {
//            senderData.putBoolean("isBounty", true);
//        }
//        if(player.getItemInHand(hand).is(BountifulContent.INSTANCE.getDECREE_ITEM())) {
//            senderData.putBoolean("isDecree", true);
//        }
        return senderData;
    }

    /**
     * Provide different dialogues for different situations.
     */
    @Override
    @OnlyIn(Dist.CLIENT)
    public DialogueScreen getDialogScreen(StreamDialogueScreenBuilder dialogueScreenBuilder, LocalPlayer localPlayer, Villager villager, CompoundTag serverData) {
        if(localPlayer.isCrouching()) {
            dialogueScreenBuilder.start(Component.literal("I see you!"))
                    .setLoopingTextInput(
                            Component.literal("Identify yourself!"),
                            Component.literal("Enter input..."),
                            // when using LoopingTextInput, it makes most sense in this situation to entirely delegate the reply to the callback.
                            (text, screen) -> {
                                screen.setBlocked(true);
                                screen.setDialogueAnswer(Component.literal("..."));
                                ChroniclerPacketRelay.sendToServer(new SendGroqQueryPacket(text));

                                if(text.equals(localPlayer.getDisplayName().getString())) {
                                    if (screen instanceof TextInputDialogueScreen tiScreen) {
                                        tiScreen.breakLoop();
                                    }
                                }
                            }
                    )
                    .addFinalOption(Component.literal("Goodbye"));
        }
        else {
            dialogueScreenBuilder.start(Component.literal("Hello there"))
                    .startWithTextInput(
                            Component.literal("What is your name?"),
                            Component.literal("Enter name..."),
                            (text, screen) -> {
                                screen.setDialogueAnswer(Component.literal("HUH?? I didn't hear that. My ears are failing me"));
                            }
                    )
                    .addTextInputOption(
                            Component.literal("Continue"),
                            Component.literal("What was your name again??"),
                            Component.literal("Enter name..."),
                            (text, screen) -> {
                                screen.setDialogueAnswer(Component.literal("You said: " + text));
                            }
                    )
                    .addOption(Component.literal("Continue"), Component.literal("DONT INTERRUPT ME"))
                    .addTextInputOption(
                            Component.literal("Continue"),
                            Component.literal("What was your name again?? What was your name again?? What was your name again?? What was your name again?? What was your name again?? What was your name again?? What was your name again?? What was your name again?? What was your name again?? What was your name again?? What was your name again?? What was your name again?? What was your name again?? What was your name again?? What was your name again?? What was your name again??\" What was your name again?? What was your name again?? What was your name again?? What was your name again??\" What was your name again?? What was your name again?? What was your name again?? What was your name again??\""),
                            Component.literal("Enter name..."),
                            (text, screen) -> {
                                screen.setDialogueAnswer(Component.literal("You said: " + text));
                            }
                    )
                    .addFinalOption(Component.literal("Goodbye"));
        }


        return dialogueScreenBuilder.build();
    }

    /**
     * Handling different callback values
     */
    @Override
    public void handleNpcInteraction(Villager villager, ServerPlayer serverPlayer, int i) {
            if(i == 1) {
                serverPlayer.level().playSound(null, villager.getOnPos(), SoundEvents.VILLAGER_YES,
                SoundSource.VOICE, 1.0F, 1.0F);
            }
            if(i == 2) {
                serverPlayer.level().playSound(null, villager.getOnPos(), SoundEvents.VILLAGER_NO,
                        SoundSource.VOICE, 1.0F, 1.0F);
            }

            removeConservingPlayer(villager);
    }

}
