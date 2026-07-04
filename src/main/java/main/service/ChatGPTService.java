package main.service;

import api.megoru.ru.entity.HangmanChatAI;
import api.megoru.ru.impl.MegoruAPI;
import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.enums.GameStatus;
import main.game.Hangman;
import main.game.HangmanGameEndHandler;
import main.game.HangmanInputs;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.model.entity.UserSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

@Service
@AllArgsConstructor
public class ChatGPTService {
    private final static Logger LOGGER = LoggerFactory.getLogger(ChatGPTService.class.getName());

    private static final HangmanRegistry hangmanRegistry = HangmanRegistry.getInstance();
    private final HangmanInputs hangmanInputs;
    private final HangmanGameEndHandler hangmanGameEndHandler;
    private final MegoruAPI megoruAPI = new MegoruAPI.Builder().build();

    //TODO: NPE Language
    public void request() {
        Collection<Hangman> allGames = hangmanRegistry.getAllGames();
        allGames.stream().filter(Hangman::isChatGPT).forEach(hangman -> {
            String guesses = HangmanUtils.getGuesses(hangman.getGuesses());
            String wordHidden = hangman.getWORD_HIDDEN().replace(" ", "");

            long againstPlayerEmbedded = hangman.getAgainstPlayerEmbedded();

            Map<Long, UserSettings> userSettingsMap = BotStartConfig.userSettingsMap;
            UserSettings userSettings = userSettingsMap.get(againstPlayerEmbedded);

            UserSettings.GameLanguage gameLanguage = userSettings.getGameLanguage();
            UserSettings.Category category = userSettings.getCategory();

            String gptPrompt = HangmanUtils.getGPTPrompt(gameLanguage, category, guesses, wordHidden);

            HangmanChatAI hangmanChatAI = new HangmanChatAI();
            hangmanChatAI.setLanguage(gameLanguage.name().toLowerCase());
            hangmanChatAI.setPrompt(gptPrompt);

            try {
                String hangmanLetter = megoruAPI.getHangmanLetter(hangmanChatAI);

                String letter = hangmanLetter.toLowerCase();
                boolean contains = hangman.getGuesses().contains(letter);

                if (contains) {
                    hangman.setGameStatus(GameStatus.LOSE_GAME);
                    hangmanGameEndHandler.handleGameEnd(hangman, false);
                    return;
                }

                long userId = HangmanUtils.getHangmanFirstPlayer(hangman.getHangmanPlayers());
                hangmanInputs.handler(letter, userId, hangman);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        });
    }
}