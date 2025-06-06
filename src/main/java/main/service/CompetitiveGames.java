package main.service;

import lombok.AllArgsConstructor;
import main.game.HangmanPlayer;
import main.game.core.HangmanRegistry;
import main.model.entity.CompetitiveQueue;
import main.model.entity.UserSettings;
import main.model.repository.CompetitiveQueueRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class CompetitiveGames {

    private final CompetitiveQueueRepository competitiveQueueRepository;
    private final static HangmanRegistry instance = HangmanRegistry.getInstance();

    public void update() {
        List<CompetitiveQueue> competitiveQueueList = competitiveQueueRepository.findAll();
        competitiveQueueList.forEach(competitiveQueue -> {
            Long userIdLong = competitiveQueue.getUserIdLong();
            Long messageChannel = competitiveQueue.getMessageChannel();
            UserSettings.GameLanguage gameLanguage = competitiveQueue.getGameLanguage();
            HangmanPlayer hangmanPlayer = new HangmanPlayer(userIdLong, null, messageChannel, gameLanguage);
            instance.addCompetitiveQueue(hangmanPlayer);
        });
    }
}