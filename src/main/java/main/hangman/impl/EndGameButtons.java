package main.hangman.impl;

import main.config.BotStartConfig;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.LinkedList;
import java.util.List;

public interface EndGameButtons {

    static List<Button> getListButtons(long userIdLong) {
        List<Button> buttonList = new LinkedList<>();

        buttonList.add(ButtonIMpl.BUTTON_PLAY_AGAIN);

        if (BotStartConfig.getMapGameLanguages().get(userIdLong).equals("eng")) {
            buttonList.add(ButtonIMpl.BUTTON_RUSSIAN);
        } else {
            buttonList.add(ButtonIMpl.BUTTON_ENGLISH);
        }

        buttonList.add(ButtonIMpl.BUTTON_MY_STATS);

        return buttonList;
    }

    static List<Button> getListButtons(long userIdLong, long secondUser) {
        List<Button> buttonList = new LinkedList<>();

        buttonList.add(ButtonIMpl.getButtonPlayAgainWithUsers(userIdLong, secondUser));

        if (BotStartConfig.getMapGameLanguages().get(userIdLong).equals("eng")) {
            buttonList.add(ButtonIMpl.BUTTON_RUSSIAN);
        } else {
            buttonList.add(ButtonIMpl.BUTTON_ENGLISH);
        }

        buttonList.add(ButtonIMpl.BUTTON_MY_STATS);

        return buttonList;
    }
}
