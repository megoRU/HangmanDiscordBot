package messagesevents;

import db.DataBase;
import jsonparser.JSONParsers;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;

public class GameLanguageChange extends ListenerAdapter {

  private static final String LANG_RUS = "!game rus";
  private static final String LANG_ENG = "!game eng";
  private static final String LANG_RESET = "!game reset";
  private final JSONParsers jsonParsers = new JSONParsers();

  @Override
  public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
    if (event.getAuthor().isBot()) {
      return;
    }
    if (!event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_WRITE)) {
      return;
    }

    String message = event.getMessage().getContentRaw().toLowerCase().trim();
    String[] messages = message.split(" ", 2);
    String prefix_LANG_RUS = LANG_RUS;
    String prefix_LANG_ENG = LANG_ENG;
    String prefix_LANG_RESET = LANG_RESET;

    if (BotStart.getMapPrefix().containsKey(event.getAuthor().getId())) {
      prefix_LANG_RUS = BotStart.getMapPrefix().get(event.getAuthor().getId()) + "game rus";
      prefix_LANG_ENG = BotStart.getMapPrefix().get(event.getAuthor().getId()) + "game eng";
      prefix_LANG_RESET = BotStart.getMapPrefix().get(event.getAuthor().getId()) + "game reset";
    }

    if ((message.equals(prefix_LANG_RUS)
        || message.equals(prefix_LANG_RESET)
        || message.equals(prefix_LANG_ENG))
        && !event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
      event.getChannel()
          .sendMessage(jsonParsers.getLocale("language_change_Not_Admin", event.getAuthor().getId()))
          .queue();
      return;
    }

    if (message.equals(prefix_LANG_RUS) || message.equals(prefix_LANG_ENG)) {
      BotStart.getMapLanguages().put(event.getAuthor().getId(), messages[1]);

      DataBase.getInstance().removeGameLanguageFromDB(event.getAuthor().getId());
      DataBase.getInstance().addGameLanguageToDB(event.getAuthor().getId(), messages[1]);

      event.getChannel()
          .sendMessage(jsonParsers
              .getLocale("language_change_lang", event.getAuthor().getId())
              + "`" + messages[1].toUpperCase() + "`")
          .queue();

      return;
    }

    if (message.equals(prefix_LANG_RESET)) {
      BotStart.getMapLanguages().remove(event.getAuthor().getId());

      DataBase.getInstance().removeGameLanguageFromDB(event.getAuthor().getId());

      event.getChannel()
          .sendMessage(jsonParsers.getLocale("language_change_lang_reset", event.getAuthor().getId()))
          .queue();
    }
  }
}
