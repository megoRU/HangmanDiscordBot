package main.hangman.impl;

import main.config.BotStartConfig;
import main.hangman.HangmanRegistry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.List;

public interface HangmanHelper {

    static void editMessage(EmbedBuilder embedBuilder, Long userIdLong) {
        try {
            if (HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                String guildId = HangmanRegistry.getInstance().getActiveHangman().get(userIdLong).getGuildId();
                Long channelId = HangmanRegistry.getInstance().getActiveHangman().get(userIdLong).getChannelId();
                String messageId = HangmanRegistry.getInstance().getMessageId().get(userIdLong);

                if (guildId != null) {
                    BotStartConfig
                            .jda
                            .getGuildById(guildId)
                            .getTextChannelById(channelId)
                            .editMessageEmbedsById(messageId, embedBuilder.build())
                            .queue();
                } else {
                    BotStartConfig
                            .jda
                            .getPrivateChannelById(channelId)
                            .editMessageEmbedsById(messageId, embedBuilder.build())
                            .queue();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Скорее всего бот в чс!");
        }
    }

    static void editMessageWithButtons(EmbedBuilder embedBuilder, Long userIdLong, List<Button> buttons) {
        try {
            if (HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                String guildId = HangmanRegistry.getInstance().getActiveHangman().get(userIdLong).getGuildId();
                Long channelId = HangmanRegistry.getInstance().getActiveHangman().get(userIdLong).getChannelId();
                String messageId = HangmanRegistry.getInstance().getMessageId().get(userIdLong);

                if (guildId != null) {
                    BotStartConfig
                            .jda
                            .getGuildById(guildId)
                            .getTextChannelById(channelId)
                            .editMessageEmbedsById(messageId, embedBuilder.build())
                            .setActionRow(buttons)
                            .queue();
                } else {
                    BotStartConfig
                            .jda
                            .getPrivateChannelById(channelId)
                            .editMessageEmbedsById(messageId, embedBuilder.build())
                            .setActionRow(buttons)
                            .queue();
                }
            }
        } catch (Exception e) {
            System.out.println("Скорее всего бот в чс!");
        }
    }
}