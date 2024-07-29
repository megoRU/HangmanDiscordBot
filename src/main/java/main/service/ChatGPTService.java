package main.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import main.game.Hangman;
import main.game.HangmanInputs;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.gptunnel.ChatCompletion;
import main.gptunnel.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@AllArgsConstructor
public class ChatGPTService {
    private final static Logger LOGGER = LoggerFactory.getLogger(ChatGPTService.class.getName());

    private static final HangmanRegistry hangmanRegistry = HangmanRegistry.getInstance();
    private final HangmanInputs hangmanInputs;

    public void request() {

        Collection<Hangman> allGames = hangmanRegistry.getAllGames();
        allGames.stream().filter(Hangman::isChatGPT).forEach(hangman -> {
            Request request = new Request();
            String guesses = HangmanUtils.getGuesses(hangman.getGuesses()).replaceAll(" ", "");
            String wordHidden = hangman.getWORD_HIDDEN().replaceAll(" ", "");

            Request.Message systemMessage = new Request.Message(Request.Role.SYSTEM, Request.Message.howToPlay + guesses);
            Request.Message userMessage = new Request.Message(Request.Role.USER, Request.Message.guesses + wordHidden);

            System.out.println(Request.Message.guesses + wordHidden);

            List<Request.Message> messagesList = new ArrayList<>(List.of(systemMessage, userMessage));
            request.setMessages(messagesList);

            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String requestBody = objectMapper.writeValueAsString(request);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://gptunnel.ru/v1/chat/completions")) // замените на нужный URL
                        .header("Content-Type", "application/json")
                        .header("Authorization", "")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                // Отправка запроса и получение ответа
                HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    ChatCompletion chatCompletion = objectMapper.readValue(response.body(), ChatCompletion.class);
                    String letter = chatCompletion.getChoices()[0].getMessage().getContent().toLowerCase();

                    int completionTokens = chatCompletion.getUsage().getCompletionTokens();
                    double totalCost = chatCompletion.getUsage().getTotalCost();

                    System.out.println("totalCost " + totalCost);
                    System.out.println("completionTokens " + completionTokens);

                    long userId = HangmanUtils.getHangmanFirstPlayer(hangman.getHangmanPlayers());
                    hangmanInputs.handler(letter, userId, hangman);

                    System.out.println(chatCompletion.getChoices()[0].getMessage().getContent());
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        });

    }
}