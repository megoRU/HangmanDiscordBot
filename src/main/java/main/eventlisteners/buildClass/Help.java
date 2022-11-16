package main.eventlisteners.buildClass;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.hangman.impl.ButtonIMpl;
import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class Help {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);
    private final InteractionHook interactionHook;
    private final long userIdLong;

    public void send() {
        EmbedBuilder info = new EmbedBuilder();
        info.setColor(Color.GREEN);
        info.addField("Slash Commands",
                """
                        </hg:940560633504604161>
                        </multi:1024084212762038352>
                        </stop:940560633504604162>
                        </language:940560633504604160>
                        </stats:940560633504604164>
                        </category:1029784705073168486>
                        </mystats:940560633504604165>
                        </allstats:940560633504604166>
                        </delete:940560633504604167>
                         """, false);

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
        interactionHook.sendMessageEmbeds(info.build()).setActionRow(buttons).queue();
    }
}