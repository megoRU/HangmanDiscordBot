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

        if (BotStart.getMapPrefix().containsKey(event.getAuthor().getId())) {
            prefix_LANG_RUS = BotStart.getMapPrefix().get(event.getAuthor().getId()) + "game rus";
            prefix_LANG_ENG = BotStart.getMapPrefix().get(event.getAuthor().getId()) + "game eng";
        }

        if (message.equals(prefix_LANG_RUS) || message.equals(prefix_LANG_ENG)) {
            BotStart.getMapGameLanguages().put(event.getAuthor().getId(), messages[1]);

            DataBase.getInstance().addGameLanguageToDB(event.getAuthor().getId(), messages[1]);

            event.getChannel()
                    .sendMessage(jsonParsers.getLocale("language_change_lang", event.getAuthor().getId())
                    + "`" + messages[1].toUpperCase() + "`").queue();
        }
    }
}
