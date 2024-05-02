package main.service;

import lombok.AllArgsConstructor;
import main.game.HangmanPlayer;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.model.entity.UserSettings;
import net.dv8tion.jda.api.JDA;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class CompetitiveQueueService {

    private static int countPlayers = 0;

    public void queue(JDA jda) {
        HangmanRegistry instance = HangmanRegistry.getInstance();
        List<HangmanPlayer> competitiveQueueList = instance.getCompetitiveQueue();

        for (HangmanPlayer competitiveQueue : competitiveQueueList) {
            long userIdLong = competitiveQueue.getUserId();
            long messageChannel = competitiveQueue.getChannelId();
            UserSettings.GameLanguage gameLanguage = competitiveQueue.getGameLanguage();
            HangmanPlayer hangmanPlayer = new HangmanPlayer(userIdLong, null, messageChannel, gameLanguage);
            instance.addCompetitiveQueue(hangmanPlayer);
        }

        long countRU = competitiveQueueList.stream().filter(
                hangmanPlayer ->
                        hangmanPlayer.getGameLanguage()
                                .equals(UserSettings.GameLanguage.RU)).count();

        long countEN = competitiveQueueList.stream().filter(
                hangmanPlayer ->
                        hangmanPlayer.getGameLanguage()
                                .equals(UserSettings.GameLanguage.EN)).count();

        int size = competitiveQueueList.size();

        if (countPlayers != size) {
            countPlayers = size;

            if (countRU >= 1 && countEN >= 1) {
                HangmanUtils.updateActivity(jda, "[RU, EN]");
            } else if (countRU >= 1) {
                HangmanUtils.updateActivity(jda, "[RU]");
            } else if (countEN >= 1) {
                HangmanUtils.updateActivity(jda, "[EN]");
            } else {
                HangmanUtils.updateActivity(jda);
            }
        }
    }
}