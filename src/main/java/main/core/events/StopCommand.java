package main.core.events;

import main.controller.UpdateController;
import main.game.Hangman;
import main.game.HangmanEmbedUtils;
import main.game.HangmanPlayer;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.jsonparser.JSONParsers;
import main.model.repository.HangmanGameRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StopCommand {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);
    private final JSONParsers jsonGameParsers = new JSONParsers(JSONParsers.Locale.GAME);

    private final HangmanGameRepository hangmanGameRepository;

    @Autowired
    public StopCommand(HangmanGameRepository hangmanGameRepository) {
        this.hangmanGameRepository = hangmanGameRepository;
    }

    //TODO: Переделать всё
    public void stop(@NotNull Event event, UpdateController updateController) {
        var userIdLong = updateController.getUserId(event);

        //Проверяем играет ли сейчас игрок. Если да удаляем игру.
        HangmanRegistry instance = HangmanRegistry.getInstance();
        Hangman hangman = instance.getActiveHangman(userIdLong);
        if (hangman != null) {
            boolean competitive = hangman.isCompetitive();
            if (competitive) {
                //First player
                cancelCompetitiveGame(event, userIdLong, updateController);
                long againstPlayerId = hangman.getAgainstPlayerEmbedded();
                cancelCompetitiveGame(event, againstPlayerId, updateController);
                instance.removeHangman(userIdLong);
            } else {
                HangmanPlayer[] hangmanPlayers = hangman.getHangmanPlayers();
                long userId = HangmanUtils.getHangmanFirstPlayer(hangmanPlayers);
                //Мы не знаем что это такое
                String hangmanEngGame = jsonParsers.getLocale("Hangman_Eng_game", userId);
                String hangmanEngGame1 = jsonGameParsers.getLocale("Hangman_Eng_game", userId);

                updateController.sendMessage(event, hangmanEngGame);
                EmbedBuilder embedBuilder = HangmanEmbedUtils.hangmanLayout(userId, hangmanEngGame1);

                HangmanEmbedUtils.editMessage(embedBuilder, userId, hangmanGameRepository);
                instance.removeHangman(userId);
                hangmanGameRepository.deleteActiveGame(userId);
            }
            //Если игрок не играет, а хочет завершить игру, то нужно ему это прислать уведомление, что он сейчас не играет
        } else {
            String hangmanYouAreNotPlay = jsonParsers.getLocale("Hangman_You_Are_Not_Play", userIdLong);
            updateController.sendMessage(event, hangmanYouAreNotPlay);
        }
    }

    private void cancelCompetitiveGame(@NotNull Event event, long userId, UpdateController updateController) {
        GenericInteractionCreateEvent genericCommandInteractionEvent = (GenericInteractionCreateEvent) event;
        long userIdFromEvent = genericCommandInteractionEvent.getUser().getIdLong();
        if (userIdFromEvent == userId) {
            String hangmanEngGame = jsonParsers.getLocale("Hangman_Eng_game", userId);
            EmbedBuilder embedBuilder = HangmanEmbedUtils.hangmanLayout(userId, hangmanEngGame);
            updateController.sendMessage(event, hangmanEngGame);
            HangmanEmbedUtils.editMessageWithButtons(embedBuilder, userId, hangmanGameRepository);
        } else {
            String opponentCanselGame = jsonParsers.getLocale("hangman_opponent_cansel_game", userId);
            EmbedBuilder embedBuilder = HangmanEmbedUtils.hangmanLayout(userId, opponentCanselGame);
            HangmanEmbedUtils.editMessageWithButtons(embedBuilder, userId, hangmanGameRepository);
        }

        hangmanGameRepository.deleteActiveGame(userId);
    }
}