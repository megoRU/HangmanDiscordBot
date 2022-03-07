package main.eventlisteners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.List;

public interface SenderMessage {

    static void sendMessage(EmbedBuilder embedBuilder, SlashCommandInteractionEvent event, List<Button> buttons) {
        try {
            if (buttons == null) {
                event.replyEmbeds(embedBuilder.build()).queue();
            } else {
                event.replyEmbeds(embedBuilder.build()).addActionRow(buttons).queue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void sendMessage(EmbedBuilder embedBuilder, MessageChannel messageChannel, List<Button> buttons) {
        try {
            if (buttons == null) {
                messageChannel.sendMessageEmbeds(embedBuilder.build()).queue();
            } else {
                messageChannel.sendMessageEmbeds(embedBuilder.build()).setActionRow(buttons).queue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
