package messagesevents;

import db.DataBase;
import jsonparser.JSONParsers;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;

import java.util.UUID;

public class DeleteAllMyData extends ListenerAdapter {

    private static final String DELETE = "!delete";
    private static final String DELETE_WITH_CODE = "!delete\\s.+";
    private final JSONParsers jsonParsers = new JSONParsers();

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        try {
            buildMessage(event, event.getMessage().getContentRaw(), event.getAuthor().getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void buildMessage(@NotNull MessageReceivedEvent event, String message, String authorId) {

        String prefix_DELETE = DELETE;
        String prefix_DELETE_WITH_CODE = DELETE_WITH_CODE;

        String[] split = message.split(" ", 2);

        if (BotStart.getMapPrefix().containsKey(authorId)) {
            prefix_DELETE = BotStart.getMapPrefix().get(authorId) + "delete";
            prefix_DELETE_WITH_CODE = BotStart.getMapPrefix().get(authorId) + split[1];
        }

        if (message.equals(prefix_DELETE)) {
            String code = UUID.randomUUID().toString().replaceAll("-", "");
            BotStart.getSecretCode().put(authorId, code);

            event.getChannel().sendMessage(jsonParsers.getLocale("restore_Data", authorId)).queue();

            event.getAuthor().openPrivateChannel()
                    .flatMap(channel -> channel.sendMessage(
                            jsonParsers.getLocale("restore_Data_PM", authorId).replaceAll("\\{0}", code)))
                    .queue();
            return;
        }

        if (message.matches(prefix_DELETE_WITH_CODE) && BotStart.getSecretCode().get(authorId) == null) {
            event.getChannel().sendMessage(jsonParsers.getLocale("restore_Data_Failure", authorId)).queue();
            return;
        }

        if (message.matches(prefix_DELETE_WITH_CODE) && !BotStart.getSecretCode().get(authorId).equals(split[1])) {
            event.getChannel().sendMessage(jsonParsers.getLocale("restore_Data_Failure", authorId)).queue();
            return;
        }

        if (split.length > 1 && message.matches(prefix_DELETE_WITH_CODE)
                && BotStart.getSecretCode().get(authorId) != null
                && BotStart.getSecretCode().get(authorId).equals(split[1])) {
            event.getChannel().sendMessage(jsonParsers.getLocale("restore_Data_Success", authorId)).queue();
            BotStart.getMapGameLanguages().remove(authorId);
            BotStart.getMapLanguages().remove(authorId);
            DataBase.getInstance().deleteAllMyData(authorId);
        }
    }
}