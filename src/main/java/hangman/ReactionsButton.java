package hangman;

import jsonparser.JSONParsers;
import messagesevents.GameLanguageChange;
import messagesevents.MessageInfoHelp;
import messagesevents.MessageStats;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;

import java.util.Objects;

public class ReactionsButton extends ListenerAdapter {

    private final JSONParsers jsonParsers = new JSONParsers();
    public static final String START_NEW_GAME = "NEW_GAME";
    public static final String START_CHANGE_GAME_LANGUAGE = "CHANGE_GAME_LANGUAGE";
    public static final String MY_STATS = "MY_STATS";
    public static final String BUTTON_HELP = "BUTTON_HELP";
    public static final String BUTTON_RUS = "LANGUAGE_RUS";
    public static final String BUTTON_ENG = "LANGUAGE_ENG";

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        try {
            if (event.getButton() == null) return;

            if (event.getGuild() == null || event.getMember() == null) return;

            if (event.getUser().isBot()) return;

            if (HangmanRegistry.getInstance().hasHangman(event.getUser().getIdLong()) &&
                    (Objects.equals(event.getButton().getId(), event.getMember().getUser().getId() + ":" + START_NEW_GAME)
                            || Objects.equals(event.getButton().getId(), event.getMember().getUser().getId() + ":" + START_CHANGE_GAME_LANGUAGE))) {
                event.deferEdit().queue();
                event.getHook().sendMessage(jsonParsers
                                .getLocale("ReactionsButton_When_Play", event.getMember().getId()))
                        .setEphemeral(true).queue();
                return;
            }

            if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + BUTTON_RUS)) {
                event.deferEdit().queue();
                new GameLanguageChange().changeGameLanguage("rus", event.getUser().getId());
                event.getHook().sendMessage(jsonParsers
                                .getLocale("language_change_lang", event.getMember().getId()) + "Кириллица")
                        .setEphemeral(true).queue();
                return;
            }

            if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + BUTTON_ENG)) {
                event.deferEdit().queue();
                new GameLanguageChange().changeGameLanguage("eng", event.getUser().getId());
                event.getHook().sendMessage(jsonParsers
                                .getLocale("language_change_lang", event.getMember().getId()) + "Latin")
                        .setEphemeral(true).queue();
                return;
            }

            if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + BUTTON_HELP)) {

                event.deferEdit().queue();
                MessageInfoHelp messageInfoHelp = new MessageInfoHelp();
                messageInfoHelp.buildMessage(
                        BotStart.getMapPrefix().get(event.getGuild().getId()) == null ? "!" : BotStart.getMapPrefix().get(event.getGuild().getId()),
                        event.getTextChannel(),
                        event.getUser().getAvatarUrl(),
                        event.getGuild().getId(),
                        event.getUser().getName()
                );
                return;
            }

            long userIdLong = event.getUser().getIdLong();
            if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + START_NEW_GAME)) {
                event.deferEdit().queue();
                HangmanRegistry.getInstance().setHangman(userIdLong, new Hangman(event.getUser().getId(), event.getGuild().getId(), event.getTextChannel()));
                HangmanRegistry.getInstance().getActiveHangman().get(userIdLong).startGame(event.getTextChannel());
                return;
            }

            if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + START_CHANGE_GAME_LANGUAGE)) {
                event.deferEdit().queue();
                new GameLanguageChange().changeGameLanguage(event.getButton().getLabel().contains("rus") ? "rus" : "eng", event.getMember().getId());
                event.getHook().sendMessage(jsonParsers
                                .getLocale("ReactionsButton_Save", event.getMember().getId())
                                .replaceAll("\\{0}", event.getButton().getLabel().contains("rus") ? "rus" : "eng"))
                        .setEphemeral(true).queue();
            }

            if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + MY_STATS)) {
                event.deferEdit().queue();
                new MessageStats().sendStats(
                        event.getTextChannel(),
                        event.getMember().getUser().getAvatarUrl(),
                        event.getUser().getId(),
                        event.getMember().getUser().getName());
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}