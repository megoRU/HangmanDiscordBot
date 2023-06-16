package main.hangman;

import main.model.entity.Game;
import main.model.repository.GamesRepository;
import main.model.repository.HangmanGameRepository;

import java.sql.Timestamp;
import java.time.Instant;

public class ResultGame {

    //Repo
    private final HangmanGameRepository hangmanGameRepository;
    private final GamesRepository gamesRepository;

    //Data
    private final int idGame;
    private final long userId;
    private final boolean result;

    public ResultGame(HangmanGameRepository hangmanGameRepository,
                      GamesRepository gamesRepository,
                      long userId,
                      boolean result) {
        this.hangmanGameRepository = hangmanGameRepository;
        this.gamesRepository = gamesRepository;
        this.idGame = HangmanRegistry.getInstance().getIdGame();
        this.userId = userId;
        this.result = result;
    }

    public void send() {
        try {
            Game game = new Game();
            game.setId(idGame);
            game.setResult(result);
            game.setGameDate(new Timestamp(Instant.now().toEpochMilli()));
            game.setUserIdLong(userId);

            gamesRepository.saveAndFlush(game);
            hangmanGameRepository.deleteActiveGame(userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
