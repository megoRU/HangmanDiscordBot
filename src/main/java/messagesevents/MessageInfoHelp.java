package messagesevents;

import jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;

import java.util.ArrayList;
import java.util.List;

public class MessageInfoHelp extends ListenerAdapter implements SenderMessage {

    private static final String HELP = "!help";
    private static final String PREFIX = "!";
    private final JSONParsers jsonParsers = new JSONParsers();

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {

        if (!event.getGuild().getSelfMember().hasPermission(event.getMessage().getTextChannel(), Permission.MESSAGE_WRITE)) {
            return;
        }

        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentDisplay().trim();

        if (message.equals("")) return;

        String prefix = HELP;
        String p = PREFIX;

        if (BotStart.getMapPrefix().containsKey(event.getGuild().getId())) {
            prefix = BotStart.getMapPrefix().get(event.getGuild().getId()) + "help";
            p = BotStart.getMapPrefix().get(event.getGuild().getId());
        }


        if (message.equals(prefix)) {
            buildMessage(
                    p,
                    event.getChannel(),
                    event.getAuthor().getAvatarUrl(),
                    event.getAuthor().getId(),
                    event.getAuthor().getName());
        }

    }

    public void buildMessage(String p, TextChannel textChannel, String avatarUrl, String userIdLong, String name) {

        String avatar = null;

        if (avatarUrl == null) {
            avatar = "https://cdn.discordapp.com/avatars/754093698681274369/dc4b416065569253bc6323efb6296703.png";
        }
        if (avatarUrl != null) {
            avatar = avatarUrl;
        }

        EmbedBuilder info = new EmbedBuilder();
        info.setColor(0xa224db);
        info.setAuthor(name, null, avatar);
        info.addField(jsonParsers.getLocale("messages_events_Prefix", userIdLong),
                jsonParsers.getLocale("messages_events_Changes_Prefix", userIdLong) +
                        jsonParsers.getLocale("messages_events_Reset_Prefix", userIdLong), false);

        info.addField(jsonParsers.getLocale("messages_events_Language_Title", userIdLong), "`"
                + p + jsonParsers.getLocale("messages_events_Language", userIdLong) + "`"
                + p + jsonParsers.getLocale("messages_events_Game_Language", userIdLong), false);

        info.addField(jsonParsers.getLocale("messages_events_Title", userIdLong), "`"
                + p + jsonParsers.getLocale("messages_events_Start_Hangman", userIdLong) + "`"
                + p + jsonParsers.getLocale("messages_events_Stop_Hangman", userIdLong) + "`"
                + p + jsonParsers.getLocale("messages_events_Stats_Hangman", userIdLong) + "`"
                + p + jsonParsers.getLocale("messages_events_My_Stats_Hangman", userIdLong) + "`"
                + p + jsonParsers.getLocale("messages_events_All_Stats_Hangman", userIdLong),
                false);

        info.addField(jsonParsers.getLocale("help_delete_Title", userIdLong), "`"
                + p + jsonParsers.getLocale("help_delete", userIdLong), false);

        info.addField(jsonParsers.getLocale("messages_events_Links", userIdLong),
                jsonParsers.getLocale("messages_events_Site", userIdLong) +
                        jsonParsers.getLocale("messages_events_Add_Me_To_Other_Guilds", userIdLong) +
                        jsonParsers.getLocale("messages_events_Vote_For_This_Bot", userIdLong), false);

        info.addField(
                jsonParsers.getLocale("messages_events_Bot_Creator", userIdLong),
                jsonParsers.getLocale("messages_events_Bot_Creator_Url_Steam", userIdLong), false);

        info.addField(
                jsonParsers.getLocale("messages_events_Support", userIdLong),
                jsonParsers.getLocale("messages_events_Support_Url_Discord", userIdLong), false);

        sendMessage(info, textChannel);
    }
}