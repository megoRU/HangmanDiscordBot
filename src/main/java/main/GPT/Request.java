package main.GPT;

import com.fasterxml.jackson.databind.ObjectMapper;
import main.config.Config;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Request {

    public static ChatCompletion send(GPTRequest gptRequest) throws IOException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        try (HttpClient client = HttpClient.newHttpClient()) {
            String requestBody = objectMapper.writeValueAsString(gptRequest);
//            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
//            String json = ow.writeValueAsString(gptRequest);
//            System.out.println(json);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://gptunnel.ru/v1/chat/completions")) // замените на нужный URL
                    .header("Content-Type", "application/json")
                    .header("Authorization", Config.getGPT_TOKEN())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // Отправка запроса и получение ответа
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), ChatCompletion.class);
            } else {
                throw new IllegalArgumentException(response.body());
            }
        }
    }
}