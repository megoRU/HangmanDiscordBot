package main.eventlisteners;

import lombok.AllArgsConstructor;
import main.hangman.Hangman;
import main.hangman.HangmanRegistry;
import main.hangman.impl.HangmanHelper;
import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.awt.*;

@AllArgsConstructor
@Service
public class GameHangmanListener extends ListenerAdapter {

    private static final String HG_ONE_LETTER = "[A-Za-zА-ЯЁа-яё]";
    private static final String HG_ONE_WORD = "[A-Za-zА-ЯЁа-яё]{3,24}+";
    private static final JSONParsers jsonGameParsers = new JSONParsers(JSONParsers.Locale.GAME);

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        try {
            if (event.getAuthor().isBot()) return;

            String message = event.getMessage().getContentRaw().trim().toLowerCase();
            long userIdLong = event.getAuthor().getIdLong();
            int length = message.length();

            if (!HangmanRegistry.getInstance().hasHangman(userIdLong)) return;

            Hangman hangman = HangmanRegistry.getInstance().getActiveHangman(userIdLong);

            if (message.matches(HG_ONE_LETTER)) {
                Message messageEvent = event.getMessage();
                hangman.logic(message, messageEvent);
                return;
            }

            if (length == hangman.getLengthWord() && message.matches(HG_ONE_WORD)) {
                hangman.fullWord(message);
            } else {
                String wrongLengthJson = jsonGameParsers.getLocale("wrongLength", userIdLong);
                EmbedBuilder wrongLength = hangman.embedBuilder(
                        Color.GREEN,
                        wrongLengthJson,
                        false,
                        false,
                        message);

                HangmanHelper.editMessage(wrongLength, userIdLong);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
