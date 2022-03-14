package main.hangman.impl;

import main.config.BotStartConfig;
import main.hangman.language;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public interface GetWord {
    String URL = "http://193.163.203.77:8085/api/word";

    static String get(String userIdLong) {
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
            return response.body();
        } catch (Exception e) {
            System.out.println("Скорее всего API не работает");
            e.printStackTrace();
        }
        return null;
    }
}
