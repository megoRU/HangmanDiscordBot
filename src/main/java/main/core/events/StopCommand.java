package main.core.events;

import main.controller.UpdateController;
import main.hangman.*;
import main.jsonparser.JSONParsers;
import main.model.repository.HangmanGameRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
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
                Long againstPlayerId = hangman.getAgainstPlayerId();
                cancelCompetitiveGame(event, againstPlayerId, updateController);
            } else {
                HangmanPlayer[] hangmanPlayers = hangman.getHangmanPlayers();
                HangmanPlayer hangmanPlayer = hangmanPlayers[0];
                long userId = hangmanPlayer.getUserId();

                String hangmanEngGame = jsonParsers.getLocale("Hangman_Eng_game", userId);
                String hangmanEngGame1 = jsonGameParsers.getLocale("Hangman_Eng_game", userId);

                if (hangmanPlayers.length > 1) {
                    HangmanPlayer hangmanPlayerSecond = hangmanPlayers[1];
                    long secondUserId = hangmanPlayerSecond.getUserId();
                    Button buttonPlayAgainWithUsers = HangmanUtils.getButtonPlayAgainWithUsers(userId, secondUserId);
                    updateController.sendMessage(event, hangmanEngGame, buttonPlayAgainWithUsers);
                } else {
                    updateController.sendMessage(event, hangmanEngGame, HangmanUtils.BUTTON_PLAY_AGAIN);
                }
                EmbedBuilder embedBuilder = HangmanEmbedUtils.hangmanPattern(userId, hangmanEngGame1);

                HangmanEmbedUtils.editMessage(embedBuilder, userId, hangmanGameRepository);
                instance.removeHangman(userId);
                hangmanGameRepository.deleteActiveGame(userId);
            }
            //Если игрок не играет, а хочет завершить игру, то нужно ему это прислать уведомление, что он сейчас не играет
        } else {
            String hangmanYouAreNotPlay = jsonParsers.getLocale("Hangman_You_Are_Not_Play", userIdLong);
            updateController.sendMessage(event, hangmanYouAreNotPlay, HangmanUtils.BUTTON_PLAY_AGAIN);
        }
    }

    private void cancelCompetitiveGame(@NotNull Event event, long userIdLong, UpdateController updateController) {
        HangmanRegistry instance = HangmanRegistry.getInstance();

        GenericCommandInteractionEvent genericCommandInteractionEvent = (GenericCommandInteractionEvent) event;
        long userId = genericCommandInteractionEvent.getUser().getIdLong();

        String hangmanEngGame = jsonParsers.getLocale("Hangman_Eng_game", userIdLong);

        if (userId == userIdLong) {
            updateController.sendMessage(event, hangmanEngGame);
        }

        EmbedBuilder embedBuilder = HangmanEmbedUtils.hangmanPattern(userIdLong, hangmanEngGame);
        HangmanEmbedUtils.editMessage(embedBuilder, userIdLong, hangmanGameRepository);
        hangmanGameRepository.deleteActiveGame(userIdLong);

        instance.removeHangman(userIdLong);
    }
}