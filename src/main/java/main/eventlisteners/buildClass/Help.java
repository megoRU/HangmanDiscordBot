package main.eventlisteners.buildClass;

import main.config.BotStartConfig;
import main.hangman.impl.ButtonIMpl;
import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.ArrayList;
import java.util.List;

public class Help {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    public void send(MessageChannel messageChannel, SlashCommandInteractionEvent event, String avatar, long userIdLong, String name) {
        if (avatar == null) {
            avatar = "https://cdn.discordapp.com/avatars/754093698681274369/dc4b416065569253bc6323efb6296703.png";
        }

        EmbedBuilder info = new EmbedBuilder();
        info.setColor(0xa224db);
        info.setAuthor(name, null, avatar);

        info.addField("Slash Commands", "`/hg`, `/multi`, `/stop`, `/language`, `/stats`, \n`/mystats`, `/allstats`, `/delete`", false);

        String messagesEventsLinks = jsonParsers.getLocale("messages_events_Links", userIdLong);
        String messagesEventsSite = jsonParsers.getLocale("messages_events_Site", userIdLong);
        String messagesEventsAddMeToOtherGuilds = jsonParsers.getLocale("messages_events_Add_Me_To_Other_Guilds", userIdLong);
        String messagesEventsVoteForThisBot = jsonParsers.getLocale("messages_events_Vote_For_This_Bot", userIdLong);
        String messagesEventsBotCreator = jsonParsers.getLocale("messages_events_Bot_Creator", userIdLong);
        String messagesEventsBotCreatorUrlSteam = jsonParsers.getLocale("messages_events_Bot_Creator_Url_Steam", userIdLong);

        info.addField(messagesEventsLinks, messagesEventsSite + messagesEventsAddMeToOtherGuilds + messagesEventsVoteForThisBot, false);
        info.addField(messagesEventsBotCreator, messagesEventsBotCreatorUrlSteam, false);

        List<Button> buttons = new ArrayList<>();
        buttons.add(ButtonIMpl.BUTTON_SUPPORT);

        if (BotStartConfig.getMapLanguages().get(userIdLong) != null) {

            if (BotStartConfig.getMapLanguages().get(userIdLong).equals("eng")) {

                buttons.add(ButtonIMpl.BUTTON_RUSSIAN_CHANGE);
            } else {
                buttons.add(ButtonIMpl.BUTTON_ENGLISH_CHANGE);
            }
        } else {
            buttons.add(ButtonIMpl.BUTTON_RUSSIAN_CHANGE);
        }

        if (messageChannel != null) {
            messageChannel.sendMessageEmbeds(info.build()).setActionRow(buttons).queue();
        } else {
            event.replyEmbeds(info.build()).addActionRow(buttons).queue();
        }
    }
}