package main.hangman.impl;

import main.config.BotStartConfig;
import main.hangman.Hangman;
import main.hangman.HangmanRegistry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

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
                        GuildMessageChannel textChannelById = guildById.getTextChannelById(channelId);
                        if (textChannelById == null) textChannelById = guildById.getNewsChannelById(channelId);
                        if (textChannelById == null) textChannelById = guildById.getThreadChannelById(channelId);
                        if (textChannelById != null) {
                            try {
                                textChannelById.editMessageEmbedsById(messageId, embedBuilder.build()).queue();
                            } catch (Exception e) {
                                if (e.getMessage().contains("UNKNOWN_MESSAGE")) {
                                    LOGGER.info("editMessage(): UNKNOWN_MESSAGE");
                                } else if (e.getMessage().contains("MISSING_ACCESS")) {
                                    LOGGER.info("editMessage(): MISSING_ACCESS");
                                } else if (e.getMessage().contains("UNKNOWN_CHANNEL")) {
                                    LOGGER.info("editMessage(): UNKNOWN_CHANNEL");
                                }
                            }
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
        } catch (
                Exception e) {
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
                        GuildMessageChannel textChannelById = guildById.getTextChannelById(channelId);
                        if (textChannelById == null) textChannelById = guildById.getNewsChannelById(channelId);
                        if (textChannelById == null) textChannelById = guildById.getThreadChannelById(channelId);
                        if (textChannelById != null) {
                            try {
                                textChannelById
                                        .editMessageEmbedsById(messageId, embedBuilder.build())
                                        .setActionRow(buttons)
                                        .queue();
                            } catch (Exception e) {
                                if (e.getMessage().contains("UNKNOWN_MESSAGE")) {
                                    LOGGER.info("editMessage(): UNKNOWN_MESSAGE");
                                } else if (e.getMessage().contains("MISSING_ACCESS")) {
                                    LOGGER.info("editMessage(): MISSING_ACCESS");
                                } else if (e.getMessage().contains("UNKNOWN_CHANNEL")) {
                                    LOGGER.info("editMessage(): UNKNOWN_CHANNEL");
                                }
                            }
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