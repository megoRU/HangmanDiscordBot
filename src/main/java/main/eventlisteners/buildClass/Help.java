package main.eventlisteners.buildClass;

import main.config.BotStartConfig;
import main.enums.Buttons;
import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.ArrayList;
import java.util.List;

public class Help {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    public void send(MessageChannel messageChannel, SlashCommandInteractionEvent event, String avatar, String userIdLong, String name) {
        if (avatar == null) {
            avatar = "https://cdn.discordapp.com/avatars/754093698681274369/dc4b416065569253bc6323efb6296703.png";
        }

        EmbedBuilder info = new EmbedBuilder();
        info.setColor(0xa224db);
        info.setAuthor(name, null, avatar);

        info.addField("Slash Commands", "`/hg`, `/stop`, `/language`, `/stats`,\n`/mystats`, `/allstats`, `/delete`", false);


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

                buttons.add(Button.secondary(Buttons.BUTTON_CHANGE_LANGUAGE.name(),
                                "Сменить язык")
                        .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
            } else {
                buttons.add(Button.secondary(Buttons.BUTTON_CHANGE_LANGUAGE.name(),
                                "Change language")
                        .withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")));
            }
        } else {
            buttons.add(Button.secondary(Buttons.BUTTON_CHANGE_LANGUAGE.name(),
                            "Сменить язык")
                    .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
        }

        if (messageChannel != null) {
            messageChannel.sendMessageEmbeds(info.build()).setActionRow(buttons).queue();
        } else {
            event.replyEmbeds(info.build()).addActionRow(buttons).queue();
        }
    }
}