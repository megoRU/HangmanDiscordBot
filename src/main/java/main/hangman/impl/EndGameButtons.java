package main.hangman.impl;

import main.config.BotStartConfig;
import main.enums.Buttons;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.LinkedList;
import java.util.List;

public interface EndGameButtons {

    static List<Button> getListButtons(String userIdLong) {
        List<Button> buttonList = new LinkedList<>();

        buttonList.add(Button.success(Buttons.BUTTON_START_NEW_GAME.name(), "Play again"));

        if (BotStartConfig.getMapGameLanguages().get(userIdLong).equals("eng")) {
            buttonList.add(
                    Button.secondary(Buttons.BUTTON_CHANGE_GAME_LANGUAGE.name(), "Кириллица").withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
        } else {
            buttonList.add(
                    Button.secondary(Buttons.BUTTON_CHANGE_GAME_LANGUAGE.name(), "Latin").withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")));
        }

        buttonList.add(Button.primary(Buttons.BUTTON_MY_STATS.name(), "My stats"));

        return buttonList;
    }
}
