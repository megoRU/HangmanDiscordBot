package main.eventlisteners;

import lombok.AllArgsConstructor;
import main.hangman.HangmanRegistry;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class GameHangmanListener extends ListenerAdapter {

    private static final String HG_ONE_LETTER = "[А-ЯЁа-яё]";
    private static final String HG_ONE_LETTER_ENG = "[A-Za-z]";
    private static final String HG_ONE_WORD = "[A-Za-zА-ЯЁа-яё]{3,24}+";

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        try {

            System.out.println("test");
            if (event.getAuthor().isBot()) return;

            if (event.isFromType(ChannelType.TEXT)) return;

            String message = event.getMessage().getContentRaw().trim().toLowerCase();
            long userIdLong = event.getAuthor().getIdLong();

            if (!HangmanRegistry.getInstance().hasHangman(userIdLong)) return;

            if (message.matches(HG_ONE_LETTER) || message.matches(HG_ONE_LETTER_ENG)) {
                HangmanRegistry.getInstance().getActiveHangman().get(userIdLong).logic(message, event.getMessage());
                return;
            }

            if (message.matches(HG_ONE_WORD)) {
                HangmanRegistry.getInstance().getActiveHangman().get(userIdLong).fullWord(message.toLowerCase());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
