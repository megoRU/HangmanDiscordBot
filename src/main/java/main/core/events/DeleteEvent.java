package main.core.events;

import lombok.AllArgsConstructor;
import main.game.Hangman;
import main.game.HangmanPlayer;
import main.game.core.HangmanRegistry;
import main.model.repository.HangmanGameRepository;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class DeleteEvent {

    private final HangmanGameRepository hangmanGameRepository;
    private final static Logger LOGGER = LoggerFactory.getLogger(DeleteEvent.class.getName());

    public void handle(@NotNull MessageDeleteEvent event) {
        var messageId = event.getMessageIdLong();
        boolean fromGuild = event.isFromGuild();

        if (fromGuild) {
            HangmanRegistry instance = HangmanRegistry.getInstance();
            Hangman hangman = instance.isMessageIdHas(messageId);

            if (hangman != null) {
                HangmanPlayer[] hangmanPlayers = hangman.getHangmanPlayers();

                for (HangmanPlayer hangmanPlayer : hangmanPlayers) {
                    long userId = hangmanPlayer.getUserId();
                    hangmanGameRepository.deleteActiveGame(userId);
                    HangmanRegistry.getInstance().removeHangman(userId);
                }

                LOGGER.info("DeleteEvent: {}", messageId);
            }
        }
    }
}
