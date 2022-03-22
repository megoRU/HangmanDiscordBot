package main.hangman.impl;

import main.config.BotStartConfig;
import main.hangman.language;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Logger;

public interface GetWord {
    Logger LOGGER = Logger.getLogger(GetWord.class.getName());
    String URL = "http://193.163.203.77:8085/api/word";

    /**
     * @param userIdLong String user id long
     * @return String word
     * @throws Exception
     */
    static String get(String userIdLong) throws Exception {
        String body;
        try {
            long time = System.currentTimeMillis();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL))
                    .POST(HttpRequest.BodyPublishers.ofString(new language(BotStartConfig.getMapGameLanguages().get(userIdLong)).toString()))
                    .header("Content-Type", "application/json")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(System.currentTimeMillis() - time + " ms getWord()");

            body = response.body();
            LOGGER.info("User ID: " + userIdLong + "\nbody: " + body);

            if (body == null) {
                throw new UnsuccessfulHttpException(response.statusCode(), null);
            }

            if (response.statusCode() != 200) {
                throw new UnsuccessfulHttpException(response.statusCode(), response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Скорее всего API не работает");
        }
        return body;
    }
}