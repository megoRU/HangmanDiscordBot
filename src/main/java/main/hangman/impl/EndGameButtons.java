package main.hangman.impl;

import main.config.BotStartConfig;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

public interface EndGameButtons {

    static List<Button> getListButtons(long userIdLong) {
        List<Button> buttonList = new LinkedList<>();
        buttonList.add(ButtonIMpl.BUTTON_PLAY_AGAIN);
        return getButtons(userIdLong, buttonList);
    }

    static List<Button> getListButtons(long userIdLong, long secondUser) {
        List<Button> buttonList = new LinkedList<>();
        buttonList.add(ButtonIMpl.getButtonPlayAgainWithUsers(userIdLong, secondUser));
        return getButtons(userIdLong, buttonList);
    }

    @NotNull
    private static List<Button> getButtons(long userIdLong, List<Button> buttonList) {
        String language = BotStartConfig.getMapGameLanguages().get(userIdLong);
        if (language != null && language.equals("eng")) {
            buttonList.add(ButtonIMpl.BUTTON_RUSSIAN);
        } else {
            buttonList.add(ButtonIMpl.BUTTON_ENGLISH);
        }
        buttonList.add(ButtonIMpl.BUTTON_MY_STATS);
        return buttonList;
    }
}
