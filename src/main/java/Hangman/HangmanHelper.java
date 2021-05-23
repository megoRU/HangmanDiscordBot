package Hangman;

import jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import startbot.BotStart;

public interface HangmanHelper {

//  JSONParsers jsonParsers = new JSONParsers();

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

//  default void updateGiveawayMessage(String endingWord, long guildId, long channelId, int count) {
//    try {
//      EmbedBuilder edit = new EmbedBuilder();
//      edit.setColor(0x00FF00);
//      edit.setTitle(HangmanRegistry.getInstance().getTitle().get(guildId));
//
//      edit.setDescription(jsonParsers.getLocale("gift_React_With_Gift", String.valueOf(guildId))
//          .replaceAll("\\{0}", GiveawayRegistry.getInstance().getCountWinners().get(guildId) == null ? "TBA"
//              : GiveawayRegistry.getInstance().getCountWinners().get(guildId))
//          .replaceAll("\\{1}", setEndingWord(endingWord, guildId)) + count + "`");
//
//      //Если есть время окончания включить в EmbedBuilder
//      if (!GiveawayRegistry.getInstance().getEndGiveawayDate().get(guildId).equals("null")) {
//        edit.setTimestamp(OffsetDateTime.parse(String.valueOf(GiveawayRegistry.getInstance().getEndGiveawayDate().get(guildId))));
//        edit.setFooter(jsonParsers.getLocale("gift_Ends_At", String.valueOf(guildId)));
//      }
//      //Отправляет сообщение и если нельзя редактировать то отправляет ошибку
//      BotStart.getJda().getGuildById(guildId)
//          .getTextChannelById(channelId)
//          .editMessageById(GiveawayRegistry.getInstance().getMessageId().get(guildId),
//              edit.build()).queue(null, (exception) ->
//          BotStart.getJda().getTextChannelById(channelId).sendMessage(GiveawayRegistry.getInstance().removeGiftExceptions(guildId))
//              .queue());
//      edit.clear();
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }

//  default String setEndingWord(Object num, long guildId) {
//    String language = "eng";
//    if (BotStart.getMapLanguages().get(String.valueOf(guildId)) != null) {
//      language = BotStart.getMapLanguages().get(String.valueOf(guildId));
//    }
//    if (num == null) {
//      num = "1";
//    }
//
//    if (num.equals("TBA")) {
//      return language.equals("eng") ? "Winners" : "Победителей";
//    }
//
//    return switch (Integer.parseInt((String) num) % 10) {
//      case 1 -> language.equals("eng") ? "Winner" : "Победитель";
//      case 2, 3, 4 -> language.equals("eng") ? "Winners" : "Победителя";
//      default -> language.equals("eng") ? "Winners" : "Победителей";
//    };
//  }


}
