package main.game;

import main.model.entity.ActiveHangman;
import main.model.repository.HangmanGameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class HangmanDataSaving {

    private final HangmanGameRepository hangmanGameRepository;

    @Autowired
    public HangmanDataSaving(HangmanGameRepository hangmanGameRepository) {
        this.hangmanGameRepository = hangmanGameRepository;
    }

    public void saveGame(Hangman hangman) {
        HangmanPlayer[] hangmanPlayers = hangman.getHangmanPlayers();
        long messageId = hangman.getMessageId();
        String word = hangman.getWORD();
        String wordHidden = hangman.getWORD_HIDDEN();
        int hangmanErrors = hangman.getHangmanErrors();
        boolean competitive = hangman.isCompetitive();
        Long againstPlayerId = hangman.getAgainstPlayerId();

        HangmanPlayer hangmanPlayer = hangmanPlayers[0];
        long userId = hangmanPlayer.getUserId();
        Long guildId = hangmanPlayer.getGuildId(); //Nullable is fine
        long channelId = hangmanPlayer.getChannelId();

        ActiveHangman activeHangman = new ActiveHangman();
        activeHangman.setUserIdLong(userId);
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now().atZone(ZoneOffset.UTC).toLocalDateTime());

        if (hangmanPlayers.length > 1) {
            StringBuilder stringBuilder = new StringBuilder();
            for (HangmanPlayer player : hangmanPlayers) {
                if (player.getUserId() == userId) continue;
                stringBuilder.append(player.getUserId()).append(" ");
            }
            activeHangman.setPlayersList(stringBuilder.toString().trim());
        }
        activeHangman.setMessageId(messageId);
        activeHangman.setChannelId(channelId);
        activeHangman.setGuildId(guildId);
        activeHangman.setWord(word);
        activeHangman.setCurrentHiddenWord(wordHidden);
        activeHangman.setHangmanErrors(hangmanErrors);
        activeHangman.setIsCompetitive(competitive);
        activeHangman.setAgainstPlayerId(againstPlayerId);
        activeHangman.setGameCreatedTime(timestamp);
        hangmanGameRepository.saveAndFlush(activeHangman);
    }
}