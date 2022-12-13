package main.eventlisteners;

import lombok.AllArgsConstructor;
import main.hangman.Hangman;
import main.hangman.HangmanRegistry;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class GameHangmanListener extends ListenerAdapter {

    private static final String HG_ONE_LETTER = "[A-Za-zА-ЯЁа-яё]";
    private static final String HG_ONE_WORD = "[-A-Za-zА-ЯЁа-яё\\s]{3,24}+";

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        try {
            if (event.getAuthor().isBot()) return;

            boolean permission = ChecksClass.canSendHG(event.getChannel(), event);
            if (!permission) return;

            String message = event.getMessage().getContentRaw().trim().toLowerCase();
            long userIdLong = event.getAuthor().getIdLong();

            if (!HangmanRegistry.getInstance().hasHangman(userIdLong)) return;

            Hangman hangman = HangmanRegistry.getInstance().getActiveHangman(userIdLong);
            Message messageEvent = event.getMessage();

            if (hangman != null) {
                if (message.matches(HG_ONE_LETTER) || message.matches(HG_ONE_WORD)) {
                    hangman.inputHandler(message, messageEvent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
