package main.eventlisteners;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.hangman.ReactionsButton;
import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
public class MessageInfoHelp extends ListenerAdapter implements SenderMessage {

    private static final String HELP = "!help";
    private static final String PREFIX = "!";
    private final JSONParsers jsonParsers = new JSONParsers();

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        if (!event.isFromType(ChannelType.TEXT)) return;

        if (CheckPermissions.isHasPermissionsWriteAndEmbedLinks(event.getTextChannel())) return;

        String message = event.getMessage().getContentDisplay().trim();

        if (message.equals("")) return;

        String prefix = HELP;
        String p = PREFIX;

        if (BotStartConfig.getMapPrefix().containsKey(event.getGuild().getId())) {
            prefix = BotStartConfig.getMapPrefix().get(event.getGuild().getId()) + "help";
            p = BotStartConfig.getMapPrefix().get(event.getGuild().getId());
        }


        if (message.equals(prefix)) {
            buildMessage(
                    p,
                    event.getTextChannel(),
                    null,
                    event.getAuthor().getAvatarUrl(),
                    event.getAuthor().getId(),
                    event.getAuthor().getName());
        }

    }

    public void buildMessage(String p, TextChannel textChannel, SlashCommandEvent event, String avatarUrl, String userIdLong, String name) {

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

        List<Button> buttons = new ArrayList<>();
        buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));

        if (BotStartConfig.getMapLanguages().get(userIdLong) != null) {

            if (BotStartConfig.getMapLanguages().get(userIdLong).equals("eng")) {

                buttons.add(Button.secondary(ReactionsButton.BUTTON_CHANGE_LANGUAGE,
                                "Сменить язык")
                        .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
            } else {
                buttons.add(Button.secondary(ReactionsButton.BUTTON_CHANGE_LANGUAGE,
                                "Change language")
                        .withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")));
            }
        } else {
            buttons.add(Button.secondary(ReactionsButton.BUTTON_CHANGE_LANGUAGE,
                            "Сменить язык")
                    .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
        }

        if (textChannel != null) {
            sendMessage(info, textChannel, buttons);
        } else {
            sendMessage(info, event, buttons);
        }
    }
}