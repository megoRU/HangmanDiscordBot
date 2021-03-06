package main.hangman;

import main.model.entity.Game;
import main.model.entity.Player;
import main.model.repository.GamesRepository;
import main.model.repository.HangmanGameRepository;
import main.model.repository.PlayerRepository;

import java.sql.Timestamp;
import java.time.Instant;

public class ResultGame {

    //Repo
    private final HangmanGameRepository hangmanGameRepository;
    private final GamesRepository gamesRepository;
    private final PlayerRepository playerRepository;

    //Data
    private final int idGame;
    private final String userId;
    private final boolean result;


    public ResultGame(HangmanGameRepository hangmanGameRepository,
                      GamesRepository gamesRepository,
                      PlayerRepository playerRepository,
                      String userId,
                      boolean result) {
        this.hangmanGameRepository = hangmanGameRepository;
        this.gamesRepository = gamesRepository;
        this.playerRepository = playerRepository;
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

            Player player = new Player();
            player.setId(idGame);
            player.setUserIdLong(Long.valueOf(userId));
            player.setGames_id(game);

            gamesRepository.saveAndFlush(game);
            playerRepository.saveAndFlush(player);

            hangmanGameRepository.deleteActiveGame(Long.valueOf(userId));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
