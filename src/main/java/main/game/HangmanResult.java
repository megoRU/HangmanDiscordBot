package main.game;

import lombok.AllArgsConstructor;
import main.model.entity.Game;
import main.model.repository.GamesRepository;
import main.model.repository.HangmanGameRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class HangmanResult {

    //Repo
    private final HangmanGameRepository hangmanGameRepository;
    private final GamesRepository gamesRepository;

    public void saveGame(HangmanPlayer[] hangmanPlayers, boolean result, boolean isCompetitive) {
        List<Game> gameList = new ArrayList<>();
        for (HangmanPlayer player : hangmanPlayers) {
            Game game = new Game();
            game.setResult(result);
            game.setIsCompetitive(isCompetitive);
            game.setGameDate(Instant.now());
            game.setUserIdLong(player.getUserId());

            gameList.add(game);
        }

        gamesRepository.saveAllAndFlush(gameList);

        HangmanPlayer hangmanPlayer = hangmanPlayers[0];
        hangmanGameRepository.deleteActiveGame(hangmanPlayer.getUserId());
    }

    public void saveGame(long userId, boolean result, boolean isCompetitive) {
        Game game = new Game();
        game.setResult(result);
        game.setIsCompetitive(isCompetitive);
        game.setGameDate(Instant.now());
        game.setUserIdLong(userId);
        gamesRepository.saveAndFlush(game);
        hangmanGameRepository.deleteActiveGame(userId);
    }
}