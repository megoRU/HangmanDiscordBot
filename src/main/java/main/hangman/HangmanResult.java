package main.hangman;

import main.controller.UpdateController;
import main.model.entity.Game;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class HangmanResult {

    //Repo
    private final UpdateController updateController;

    //Data
    private final HangmanPlayer[] hangmanPlayers;
    private final boolean result;
    private final boolean isCompetitive;

    public HangmanResult(HangmanPlayer[] hangmanPlayers, boolean result, boolean isCompetitive, UpdateController updateController) {
        this.isCompetitive = isCompetitive;
        this.hangmanPlayers = hangmanPlayers;
        this.result = result;
        this.updateController = updateController;
    }

    public void save() {
        try {
            HangmanRegistry instance = HangmanRegistry.getInstance();
            List<Game> gameList = new ArrayList<>();
            for (HangmanPlayer player : hangmanPlayers) {
                Game game = new Game();
                game.setId(instance.getIdGame());
                game.setResult(result);
                game.setIsCompetitive(isCompetitive);
                game.setGameDate(new Timestamp(Instant.now().toEpochMilli()));
                game.setUserIdLong(player.getUserId());

                gameList.add(game);
            }

            updateController.getGamesRepository().saveAllAndFlush(gameList);

            HangmanPlayer hangmanPlayer = hangmanPlayers[0];
            updateController.getHangmanGameRepository().deleteActiveGame(hangmanPlayer.getUserId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}