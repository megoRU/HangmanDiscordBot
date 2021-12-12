package messagesevents;

import db.DataBase;
import jsonparser.JSONParsers;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;

public class PrefixChange extends ListenerAdapter {

    private static final String PREFIX = "\\*prefix\\s.";
    private static final String PREFIX_RESET = "*prefix reset";
    private final JSONParsers jsonParsers = new JSONParsers();

    @SneakyThrows
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        if (!event.isFromType(ChannelType.TEXT)) return;

        if (CheckPermissions.isHasPermissionToWrite(event.getTextChannel())) return;

        String message = event.getMessage().getContentRaw().toLowerCase().trim();
        String[] messages = message.split(" ", 2);

        if ((message.equals(PREFIX_RESET) || message.matches(PREFIX))
                && !event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            event.getChannel()
                    .sendMessage(jsonParsers.getLocale("prefix_change_Must_have_Permission", event.getGuild().getId()))
                    .queue();
            return;
        }

        if (message.matches(PREFIX) && messages[1].equals("!")) {
            event.getChannel()
                    .sendMessage(jsonParsers.getLocale("prefix_change_Its_Standard_Prefix", event.getGuild().getId()))
                    .queue();
            return;
        }

        if (message.matches(PREFIX) && event.getMember().hasPermission(Permission.MANAGE_SERVER)
                && BotStart.getMapPrefix().get(event.getMessage().getGuild().getId()) != null) {
            BotStart.getMapPrefix().put(event.getGuild().getId(), messages[1]);

            DataBase.getInstance().removePrefixFromDB(event.getGuild().getId());
            DataBase.getInstance().addPrefixToDB(event.getGuild().getId(), messages[1]);

            event.getChannel()
                    .sendMessage(
                            jsonParsers.getLocale("prefix_change_Now_Prefix", event.getGuild().getId())
                                    .replaceAll("\\{0}", "\\" + messages[1])).queue();
            return;
        }

        if (message.matches(PREFIX) && event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            BotStart.getMapPrefix().put(event.getGuild().getId(), messages[1]);

            DataBase.getInstance().addPrefixToDB(event.getGuild().getId(), messages[1]);

            event.getChannel()
                    .sendMessage(jsonParsers.getLocale("prefix_change_Now_Prefix", event.getGuild().getId())
                            .replaceAll("\\{0}", "\\" + messages[1])).queue();
            return;
        }

        if (message.equals(PREFIX_RESET) && event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            BotStart.getMapPrefix().remove(event.getGuild().getId());

            DataBase.getInstance().removePrefixFromDB(event.getGuild().getId());

            event.getChannel()
                    .sendMessage(jsonParsers.getLocale("prefix_change_Prefix_Now_Standard", event.getGuild().getId())).queue();
        }
    }
}
