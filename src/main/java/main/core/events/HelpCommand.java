package main.core.events;

import main.controller.UpdateController;
import main.game.utils.HangmanUtils;
import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class HelpCommand {

    //Language
    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    public void help(@NotNull Event event, UpdateController updateController) {
        var userIdLong = updateController.getUserId(event);

        EmbedBuilder info = new EmbedBuilder();
        info.setColor(Color.GREEN);
        info.addField("Slash Commands",

                "</play:1194760100833939491> - " + " " + jsonParsers.getLocale("help_hg", userIdLong)
                        + "\n</competitive:1194054216013066240> - " + " " + jsonParsers.getLocale("help_competitive", userIdLong)
                        + "\n</multi:1024084212762038352> - " + " " + jsonParsers.getLocale("help_multi", userIdLong)
                        + "\n</quit:1194760100833939492> - " + " " + jsonParsers.getLocale("help_stop", userIdLong)
                        + "\n</language:940560633504604160> - " + " " + jsonParsers.getLocale("help_language", userIdLong)
                        + "\n</statistics:1194760100833939493> - " + " " + jsonParsers.getLocale("help_stats", userIdLong)
                        + "\n</category:1029784705073168486> - " + " " + jsonParsers.getLocale("help_category", userIdLong)
                        + "\n</mystats:940560633504604165> - " + " " + jsonParsers.getLocale("help_mystats", userIdLong)
                        + "\n</allstats:940560633504604166> - " + " " + jsonParsers.getLocale("help_allstats", userIdLong)
                        + "\n</delete:940560633504604167> - " + " " + jsonParsers.getLocale("help_deleted", userIdLong)
                , false);

        String messagesEventsLinks = jsonParsers.getLocale("messages_events_Links", userIdLong);
        String messagesEventsSite = jsonParsers.getLocale("messages_events_Site", userIdLong);
        String messagesEventsAddMeToOtherGuilds = jsonParsers.getLocale("messages_events_Add_Me_To_Other_Guilds", userIdLong);
        String messagesEventsBotCreator = jsonParsers.getLocale("messages_events_Bot_Creator", userIdLong);
        String messagesEventsBotCreatorUrlSteam = jsonParsers.getLocale("messages_events_Bot_Creator_Url_Steam", userIdLong);

        info.addField(messagesEventsLinks, messagesEventsSite + messagesEventsAddMeToOtherGuilds, false);
        info.addField(messagesEventsBotCreator, messagesEventsBotCreatorUrlSteam, false);

        List<Button> buttons = new ArrayList<>();
        buttons.add(HangmanUtils.getButtonSupport(userIdLong));

        updateController.sendMessage(event, info.build(), buttons);
    }
}