package main.gptunnel;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Request {

    private String model = "gpt-4o-mini"; //gpt-3.5-turbo | gpt-4o-mini | gpt-4o
    private boolean useWalletBalance = true;
    @JsonProperty("max_tokens")
    private int maxTokens = 200;
    private List<Message> messages;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Message {

        public static final String guesses = "Угадай букву в загаданном слове: ";
        public static final String howToPlay = "Как играть: " +
                "1. Не используй уже использованные буквы. " +
                "2. Черточки представляют собой буквы. " +
                "3. Используй только кириллические буквы. Отвечай одной буквой без дополнительного текста. " +
                "4. Выбирай буквы, исходя из того, какие еще используются в слове и какие могут подойти. " +
                "5. Используемые буквы: ";

        private Role role;
        private String content;

        public String getRole() {
            return role.getRole();
        }
    }

    @Getter
    public enum Role {
        USER("user"),
        SYSTEM("system");

        private final String role;

        Role(String role) {
            this.role = role;
        }
    }
}