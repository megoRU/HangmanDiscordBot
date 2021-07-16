package Hangman;

import jsonparser.JSONParsers;
import messagesevents.GameLanguageChange;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class ReactionsButton extends ListenerAdapter {

    private final JSONParsers jsonParsers = new JSONParsers();
    public static final String START_NEW_GAME = "NEW_GAME";
    public static final String START_CHANGE_GAME_LANGUAGE = "CHANGE_GAME_LANGUAGE";

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        try {
            if (event.getButton() == null) return;

            if (event.getGuild() == null || event.getMember() == null) return;

            if (event.getUser().isBot()) return;

            if (HangmanRegistry.getInstance().hasHangman(event.getUser().getIdLong())) return;

            long userIdLong = event.getUser().getIdLong();
            if (Objects.equals(event.getButton().getId(), event.getMember().getId() + ":" + START_NEW_GAME)) {
                event.deferEdit().queue();
                HangmanRegistry.getInstance().setHangman(userIdLong, new Hangman(event.getUser().getId(), event.getGuild().getId(), event.getTextChannel()));
                HangmanRegistry.getInstance().getActiveHangman().get(userIdLong).startGame(event.getTextChannel());
                return;
            }

            if (Objects.equals(event.getButton().getId(), event.getMember().getId() + ":" + START_CHANGE_GAME_LANGUAGE)) {
                event.deferEdit().queue();
                new GameLanguageChange().changeGameLanguage(event.getButton().getLabel().contains("rus") ? "rus" : "eng", event.getMember().getId());
                event.getHook().sendMessage(jsonParsers
                        .getLocale("ReactionsButton_Save", event.getMember().getId())
                        .replaceAll("\\{0}", event.getButton().getLabel().contains("rus") ? "rus" : "eng"))
                        .setEphemeral(true).queue();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}