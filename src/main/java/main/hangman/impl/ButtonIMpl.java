package main.hangman.impl;

import main.enums.Buttons;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public interface ButtonIMpl {

    Button BUTTON_RUSSIAN = Button.secondary(Buttons.BUTTON_RUS.name(), "Кириллица").withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA"));
    Button BUTTON_ENGLISH = Button.secondary(Buttons.BUTTON_ENG.name(), "Latin").withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7"));
    Button BUTTON_MY_STATS = Button.primary(Buttons.BUTTON_MY_STATS.name(), "My stats");
    Button BUTTON_PLAY_AGAIN = Button.success(Buttons.BUTTON_START_NEW_GAME.name(), "Play again");
    Button BUTTON_SUPPORT = Button.link("https://discord.gg/UrWG3R683d", "Support");
    Button BUTTON_HELP = Button.success(Buttons.BUTTON_HELP.name(), "/help");
    Button BUTTON_STOP = Button.danger(Buttons.BUTTON_STOP.name(), "Stop game");

    static Button getButtonPlayAgainWithUsers(long userIdLong, long secondUser) {
        String multi = String.format("%s_%s_%s", Buttons.BUTTON_START_NEW_GAME.name(), userIdLong, secondUser);
        return Button.success(multi, "Play again");
    }
}
