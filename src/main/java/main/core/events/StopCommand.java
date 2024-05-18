package main.core.events;

import main.controller.UpdateController;
import main.game.*;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.jsonparser.JSONParsers;
import main.model.repository.HangmanGameRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
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
                instance.removeHangman(userIdLong);
            } else {
                HangmanPlayer[] hangmanPlayers = hangman.getHangmanPlayers();
                long userId = HangmanUtils.getHangmanFirstPlayer(hangmanPlayers);
                String hangmanEngGame = jsonParsers.getLocale("Hangman_Eng_game", userId);
                String hangmanEngGame1 = jsonGameParsers.getLocale("Hangman_Eng_game", userId);

                if (hangmanPlayers.length > 1) {
                    HangmanPlayer hangmanPlayerSecond = hangmanPlayers[1];
                    long secondUserId = hangmanPlayerSecond.getUserId();
                    Button buttonPlayAgainWithUsers = HangmanUtils.getButtonPlayAgainWithUsers(userId, secondUserId);
                    updateController.sendMessage(event, hangmanEngGame, buttonPlayAgainWithUsers);
                } else {
                    updateController.sendMessage(event, hangmanEngGame, HangmanUtils.getButtonPlayAgain(userId));
                }
                EmbedBuilder embedBuilder = HangmanEmbedUtils.hangmanLayout(userId, hangmanEngGame1);

                HangmanEmbedUtils.editMessage(embedBuilder, userId, hangmanGameRepository);
                instance.removeHangman(userId);
                hangmanGameRepository.deleteActiveGame(userId);
            }
            //Если игрок не играет, а хочет завершить игру, то нужно ему это прислать уведомление, что он сейчас не играет
        } else {
            String hangmanYouAreNotPlay = jsonParsers.getLocale("Hangman_You_Are_Not_Play", userIdLong);
            updateController.sendMessage(event, hangmanYouAreNotPlay, HangmanUtils.getButtonPlayAgain(userIdLong));
        }
    }

    private void cancelCompetitiveGame(@NotNull Event event, long userId, UpdateController updateController) {
        GenericInteractionCreateEvent genericCommandInteractionEvent = (GenericInteractionCreateEvent) event;
        long userIdFromEvent = genericCommandInteractionEvent.getUser().getIdLong();
        if (userIdFromEvent == userId) {
            String hangmanEngGame = jsonParsers.getLocale("Hangman_Eng_game", userId);
            EmbedBuilder embedBuilder = HangmanEmbedUtils.hangmanLayout(userId, hangmanEngGame);
            updateController.sendMessage(event, hangmanEngGame);
            HangmanEmbedUtils.editMessage(embedBuilder, userId, hangmanGameRepository);
        } else {
            String opponentCanselGame = jsonParsers.getLocale("hangman_opponent_cansel_game", userId);
            EmbedBuilder embedBuilder = HangmanEmbedUtils.hangmanLayout(userId, opponentCanselGame);
            HangmanEmbedUtils.editMessage(embedBuilder, userId, hangmanGameRepository);
        }

        hangmanGameRepository.deleteActiveGame(userId);
    }
}