package hangman;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.Button;
import startbot.BotStart;

import java.util.List;

public interface HangmanHelper {

    default void editMessage(EmbedBuilder embedBuilder, String guildId, Long userIdLong, Long channelId) {
        try {
            BotStart.getJda()
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
            BotStart.getJda()
                    .getGuildById(guildId)
                    .getTextChannelById(channelId)
                    .editMessageEmbedsById(HangmanRegistry.getInstance().getMessageId().get(userIdLong), embedBuilder.build())
                    .setActionRow(buttons)
                    .queue();
            embedBuilder.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
