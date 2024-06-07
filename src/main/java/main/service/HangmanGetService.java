package main.service;

import lombok.AllArgsConstructor;
import main.game.*;
import main.game.core.HangmanRegistry;
import main.model.entity.ActiveHangman;
import main.model.repository.HangmanGameRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class HangmanGetService {

    private final HangmanGameRepository hangmanGameRepository;
    private final HangmanDataSaving hangmanDataSaving;
    private final HangmanResult hangmanResult;

    public void update() {
        List<ActiveHangman> activeHangmanList = hangmanGameRepository.findAll();
        HangmanRegistry instance = HangmanRegistry.getInstance();

        for (ActiveHangman activeHangman : activeHangmanList) {
            Long userIdLong = activeHangman.getUserIdLong();
            String playersList = activeHangman.getPlayersList();
            Long messageIdLong = activeHangman.getMessageIdLong();
            Long channelIdLong = activeHangman.getChannelIdLong();
            Long guildLongId = activeHangman.getGuildLongId();
            String word = activeHangman.getWord();
            String currentHiddenWord = activeHangman.getCurrentHiddenWord();
            String guesses = activeHangman.getGuesses();
            Integer hangmanErrors = activeHangman.getHangmanErrors();
            LocalDateTime gameCreatedTime = activeHangman.getGameCreatedTime().toLocalDateTime();
            Boolean isCompetitive = activeHangman.getIsCompetitive();
            Long againstPlayerId = activeHangman.getAgainstPlayerId();

            HangmanPlayer hangmanPlayer = new HangmanPlayer(userIdLong, guildLongId, channelIdLong);

            HangmanBuilder.Builder hangmanBuilder = new HangmanBuilder.Builder()
                    .setHangmanGameRepository(hangmanGameRepository)
                    .setHangmanDataSaving(hangmanDataSaving)
                    .setHangmanResult(hangmanResult)
                    .addHangmanPlayer(hangmanPlayer)
                    .setHangmanErrors(hangmanErrors)
                    .setWord(word)
                    .setGuesses(guesses)
                    .setCurrentHiddenWord(currentHiddenWord)
                    .setLocalDateTime(gameCreatedTime)
                    .setCompetitive(isCompetitive)
                    .setAgainstPlayerId(againstPlayerId)
                    .setMessageId(messageIdLong);

            if (playersList == null) {
                instance.setHangman(userIdLong, hangmanBuilder.build());
            } else {
                String[] userList = playersList.split(" ");

                for (String userId : userList) {
                    HangmanPlayer hangmanPlayerSecond = new HangmanPlayer(Long.parseLong(userId), guildLongId, channelIdLong);
                    hangmanBuilder.addHangmanPlayer(hangmanPlayerSecond);
                }

                Hangman hangman = hangmanBuilder.build();
                //Заполнение коллекции
                HangmanPlayer[] hangmanPlayers = hangman.getHangmanPlayers();
                for (HangmanPlayer player : hangmanPlayers) {
                    instance.setHangman(player.getUserId(), hangman);
                }
            }
        }
        System.out.println("setHangmanGetService()");
    }
}