package main.hangman.impl;

import main.config.BotStartConfig;
import main.hangman.HangmanRegistry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.List;

public interface HangmanHelper {

    default void editMessage(EmbedBuilder embedBuilder, Long userIdLong, Long channelId) {
        try {
            BotStartConfig
                    .jda
                    .getPrivateChannelById(channelId)
                    .editMessageEmbedsById(HangmanRegistry.getInstance().getMessageId().get(userIdLong), embedBuilder.build())
                    .queue();
            embedBuilder.clear();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Скорее всего бот в чс!");
        }
    }

    default void editMessage(EmbedBuilder embedBuilder, String guildId, Long userIdLong, Long channelId) {
        try {
            BotStartConfig
                    .jda
                    .getGuildById(guildId)
                    .getTextChannelById(channelId)
                    .editMessageEmbedsById(HangmanRegistry.getInstance().getMessageId().get(userIdLong), embedBuilder.build())
                    .queue();
            embedBuilder.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    default void editMessageWithButtons(EmbedBuilder embedBuilder, String guildId, Long userIdLong, Long channelId, List<Button> buttons) {
        try {
            BotStartConfig
                    .jda
                    .getGuildById(guildId)
                    .getTextChannelById(channelId)
                    .editMessageEmbedsById(HangmanRegistry.getInstance().getMessageId().get(userIdLong), embedBuilder.build())
                    .setActionRow(buttons)
                    .queue();
            embedBuilder.clear();
        } catch (Exception e) {
            System.out.println("Скорее всего бота удалили из гильдии!");
        }
    }

    default void editMessageWithButtons(EmbedBuilder embedBuilder, Long userIdLong, Long channelId, List<Button> buttons) {
        try {
            BotStartConfig
                    .jda
                    .getPrivateChannelById(channelId)
                    .editMessageEmbedsById(HangmanRegistry.getInstance().getMessageId().get(userIdLong), embedBuilder.build())
                    .setActionRow(buttons)
                    .queue();
            embedBuilder.clear();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Скорее всего бот в чс!");
        }
    }

}