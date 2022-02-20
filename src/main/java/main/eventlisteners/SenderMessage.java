package main.eventlisteners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.List;

public interface SenderMessage {

    default void sendMessage(EmbedBuilder embedBuilder, TextChannel textChannel, List<Button> buttons) {
        try {
            textChannel.sendMessageEmbeds(embedBuilder.build()).setActionRow(buttons).queue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    default void sendMessage(EmbedBuilder embedBuilder, TextChannel textChannel) {
        try {
            textChannel.sendMessageEmbeds(embedBuilder.build()).queue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    default void sendMessage(EmbedBuilder embedBuilder, MessageChannel textChannel) {
        try {
            textChannel.sendMessageEmbeds(embedBuilder.build()).queue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    default void sendMessage(EmbedBuilder embedBuilder, SlashCommandInteractionEvent event, List<Button> buttons) {
        try {
            event.replyEmbeds(embedBuilder.build()).addActionRow(buttons).queue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    default void sendMessage(EmbedBuilder embedBuilder, SlashCommandInteractionEvent event) {
        try {
            event.replyEmbeds(embedBuilder.build()).queue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
