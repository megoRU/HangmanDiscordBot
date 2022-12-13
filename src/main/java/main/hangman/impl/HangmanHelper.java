package main.hangman.impl;

import main.config.BotStartConfig;
import main.hangman.Hangman;
import main.hangman.HangmanRegistry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.util.List;
import java.util.logging.Logger;

public interface HangmanHelper {

    Logger LOGGER = Logger.getLogger(HangmanHelper.class.getName());

    static void editMessage(EmbedBuilder embedBuilder, Long userIdLong) {
        try {
            if (HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                Hangman hangman = HangmanRegistry.getInstance().getActiveHangman(userIdLong);
                if (hangman == null) return;
                Long guildId = hangman.getGuildId();
                Long channelId = hangman.getChannelId();
                long messageId = hangman.getMessageId();

                if (guildId != null) {
                    Guild guildById = BotStartConfig.jda.getGuildById(guildId);
                    if (guildById != null) {
                        TextChannel textChannelById = guildById.getTextChannelById(channelId);
                        if (textChannelById != null) {
                            textChannelById.retrieveMessageById(messageId).queue((message) ->
                                    message.editMessageEmbeds(embedBuilder.build())
                                            .queue(), (failure) -> {
                                if (failure instanceof ErrorResponseException) {
                                    ErrorResponseException ex = (ErrorResponseException) failure;
                                    if (ex.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) {
                                        LOGGER.info("editMessageWithButtons(): UNKNOWN_MESSAGE");
                                    }
                                }
                            });
                        }
                    }
                } else {
                    try {
                        PrivateChannel privateChannelById = BotStartConfig.jda.getPrivateChannelById(channelId);
                        if (privateChannelById == null) {
                            BotStartConfig
                                    .jda.retrieveUserById(userIdLong).complete()
                                    .openPrivateChannel()
                                    .flatMap(channel -> channel.editMessageEmbedsById(messageId, embedBuilder.build()))
                                    .queue();
                        } else {
                            privateChannelById.editMessageEmbedsById(messageId, embedBuilder.build()).queue();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("editMessage(): " + "Скорее всего бот в чс!");
        }
    }

    static void editMessageWithButtons(EmbedBuilder embedBuilder, Long userIdLong, List<Button> buttons) {
        try {
            if (HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                Hangman hangman = HangmanRegistry.getInstance().getActiveHangman(userIdLong);
                if (hangman == null) return;
                Long guildId = hangman.getGuildId();
                Long channelId = hangman.getChannelId();
                long messageId = hangman.getMessageId();

                if (guildId != null) {
                    Guild guildById = BotStartConfig.jda.getGuildById(guildId);
                    if (guildById != null) {
                        TextChannel textChannelById = guildById.getTextChannelById(channelId);
                        if (textChannelById != null) {
                            textChannelById.retrieveMessageById(messageId).queue((message) ->
                                    message.editMessageEmbeds(embedBuilder.build())
                                            .setActionRow(buttons)
                                            .queue(), (failure) -> {
                                if (failure instanceof ErrorResponseException) {
                                    ErrorResponseException ex = (ErrorResponseException) failure;
                                    if (ex.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) {
                                        LOGGER.info("editMessageWithButtons(): UNKNOWN_MESSAGE");
                                    }
                                }
                            });
                        }
                    }
                } else {
                    PrivateChannel privateChannelById = BotStartConfig.jda.getPrivateChannelById(channelId);
                    if (privateChannelById == null) {
                        BotStartConfig
                                .jda.retrieveUserById(userIdLong).complete()
                                .openPrivateChannel()
                                .flatMap(channel -> channel.editMessageEmbedsById(messageId, embedBuilder.build())
                                        .setActionRow(buttons))
                                .queue();
                    } else {
                        privateChannelById.editMessageEmbedsById(messageId, embedBuilder.build())
                                .setActionRow(buttons)
                                .queue();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("editMessageWithButtons(): " + "Скорее всего бот в чс!");
        }
    }
}