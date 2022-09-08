package main.hangman.impl;

import main.config.BotStartConfig;
import main.hangman.HangmanRegistry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.List;

public interface HangmanHelper {

    static void editMessage(EmbedBuilder embedBuilder, Long userIdLong) {
        try {
            if (HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                Long guildId = HangmanRegistry.getInstance().getActiveHangman(userIdLong).getGuildId();
                Long channelId = HangmanRegistry.getInstance().getActiveHangman(userIdLong).getChannelId();
                String messageId = HangmanRegistry.getInstance().getMessageId(userIdLong);

                if (guildId != null) {
                    Guild guildById = BotStartConfig.jda.getGuildById(guildId);
                    if (guildById != null) {
                        TextChannel textChannelById = guildById.getTextChannelById(channelId);

                        if (textChannelById != null) {
                            boolean hasPermission = guildById.getSelfMember()
                                    .hasPermission(
                                            textChannelById,
                                            Permission.MESSAGE_SEND,
                                            Permission.MESSAGE_MANAGE,
                                            Permission.VIEW_CHANNEL);
                            if (!hasPermission) return;
                            textChannelById.editMessageEmbedsById(messageId, embedBuilder.build()).queue();
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
        }
    }

    static void editMessageWithButtons(EmbedBuilder embedBuilder, Long userIdLong, List<Button> buttons) {
        try {
            if (HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                Long guildId = HangmanRegistry.getInstance().getActiveHangman(userIdLong).getGuildId();
                Long channelId = HangmanRegistry.getInstance().getActiveHangman(userIdLong).getChannelId();
                String messageId = HangmanRegistry.getInstance().getMessageId(userIdLong);

                if (guildId != null) {
                    Guild guildById = BotStartConfig.jda.getGuildById(guildId);
                    if (guildById != null) {
                        TextChannel textChannelById = guildById.getTextChannelById(channelId);
                        if (textChannelById != null) {
                            textChannelById
                                    .editMessageEmbedsById(messageId, embedBuilder.build())
                                    .setActionRow(buttons)
                                    .queue();
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
            System.out.println("Скорее всего бот в чс!");
        }
    }
}