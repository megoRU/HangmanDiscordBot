package main.hangman.impl;

import main.enums.Buttons;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.List;

public interface SetGameLanguageButtons {

    List<Button> getList = List.of(
            Button.secondary(Buttons.BUTTON_RUS.name(), "Кириллица").withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")),
            Button.secondary(Buttons.BUTTON_ENG.name(), "Latin").withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")),
            Button.success(Buttons.BUTTON_START_NEW_GAME.name(), "Play"));
}
