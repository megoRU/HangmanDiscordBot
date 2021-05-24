package Hangman;

import net.dv8tion.jda.api.EmbedBuilder;
import startbot.BotStart;

public interface HangmanHelper {

  default void editMessage(EmbedBuilder embedBuilder, long guildId, long userIdLong, long channelId) {
    try {
      BotStart.getJda()
          .getGuildById(guildId)
          .getTextChannelById(channelId)
          .editMessageById(HangmanRegistry.getInstance().getMessageId().get(userIdLong), embedBuilder.build())
          .queue();
      embedBuilder.clear();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
