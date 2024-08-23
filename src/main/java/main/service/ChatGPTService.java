package main.service;

import lombok.AllArgsConstructor;
import main.GPT.ChatCompletion;
import main.GPT.GPTRequest;
import main.GPT.Request;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static main.config.BotStartConfig.mapGameCategory;
import static main.config.BotStartConfig.mapGameLanguages;

@Service
@AllArgsConstructor
public class ChatGPTService {
    private final static Logger LOGGER = LoggerFactory.getLogger(ChatGPTService.class.getName());

    private static final HangmanRegistry hangmanRegistry = HangmanRegistry.getInstance();
    private final HangmanInputs hangmanInputs;
    private final HangmanGameEndHandler hangmanGameEndHandler;

    public void request() {
        Collection<Hangman> allGames = hangmanRegistry.getAllGames();
        allGames.stream().filter(Hangman::isChatGPT).forEach(hangman -> {
            GPTRequest request = new GPTRequest();
            String guesses = HangmanUtils.getGuesses(hangman.getGuesses());
            String wordHidden = hangman.getWORD_HIDDEN().replaceAll(" ", "");

            long againstPlayerEmbedded = hangman.getAgainstPlayerEmbedded();
            UserSettings.GameLanguage gameLanguage = mapGameLanguages.get(againstPlayerEmbedded);
            UserSettings.Category gameCategory = mapGameCategory.get(againstPlayerEmbedded);

            String gptPrompt = HangmanUtils.getGPTPrompt(gameLanguage, gameCategory, guesses, wordHidden);

            List<GPTRequest.Content> content = new ArrayList<>();

            GPTRequest.Content gptContent = new GPTRequest.Content("text", gptPrompt, null);
            content.add(gptContent);

            GPTRequest.Message userMessage = new GPTRequest.Message(GPTRequest.Role.USER, content);
            userMessage.setContent(content);

            request.setMessages(List.of(userMessage));
            try {
                ChatCompletion chatCompletion = Request.send(request);
                String letter = chatCompletion.getChoices()[0].getMessage().getContent().toLowerCase();
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