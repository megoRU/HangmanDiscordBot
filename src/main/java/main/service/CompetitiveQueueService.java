package main.service;

import lombok.AllArgsConstructor;
import main.game.HangmanPlayer;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.model.entity.CompetitiveQueue;
import main.model.entity.UserSettings;
import main.model.repository.CompetitiveQueueRepository;
import net.dv8tion.jda.api.JDA;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class CompetitiveQueueService {

    private final CompetitiveQueueRepository competitiveQueueRepository;

    public void queue(JDA jda) {
        List<CompetitiveQueue> competitiveQueueList = competitiveQueueRepository.findAll();
        HangmanRegistry instance = HangmanRegistry.getInstance();

        for (CompetitiveQueue competitiveQueue : competitiveQueueList) {
            Long userIdLong = competitiveQueue.getUserIdLong();
            Long messageChannel = competitiveQueue.getMessageChannel();
            UserSettings.GameLanguage gameLanguage = competitiveQueue.getGameLanguage();
            HangmanPlayer hangmanPlayer = new HangmanPlayer(userIdLong, null, messageChannel, gameLanguage);
            instance.addCompetitiveQueue(hangmanPlayer);
        }
        HangmanUtils.updateActivity(jda);
        System.out.println("getCompetitiveQueue()");
    }
}