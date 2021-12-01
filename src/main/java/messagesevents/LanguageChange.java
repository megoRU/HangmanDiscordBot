package messagesevents;

import db.DataBase;
import jsonparser.JSONParsers;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;

public class LanguageChange extends ListenerAdapter {

    private static final String LANG_RUS = "!lang rus";
    private static final String LANG_ENG = "!lang eng";
    private final JSONParsers jsonParsers = new JSONParsers();

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        if (!event.isFromGuild()) return;

        if (CheckPermissions.isHasPermissionToWrite(event.getTextChannel())) {
            return;
        }

        String message = event.getMessage().getContentRaw().toLowerCase().trim();
        String[] messages = message.split(" ", 2);
        String prefix_LANG_RUS = LANG_RUS;
        String prefix_LANG_ENG = LANG_ENG;

        if (BotStart.getMapPrefix().containsKey(event.getGuild().getId())) {
            prefix_LANG_RUS = BotStart.getMapPrefix().get(event.getGuild().getId()) + "lang rus";
            prefix_LANG_ENG = BotStart.getMapPrefix().get(event.getGuild().getId()) + "lang eng";
        }

        if (message.equals(prefix_LANG_RUS) || message.equals(prefix_LANG_ENG)) {
            BotStart.getMapLanguages().put(event.getAuthor().getId(), messages[1]);

            DataBase.getInstance().addLanguageToDB(event.getAuthor().getId(), messages[1]);

            String language;

            if (messages[1].equals("rus")) {
                language = "Русский";
            } else {
                language = "English";
            }

            event.getChannel()
                    .sendMessage(jsonParsers.getLocale("language_change_lang", event.getAuthor().getId())
                                    .replaceAll("\\{0}", language)).queue();
        }
    }
}
