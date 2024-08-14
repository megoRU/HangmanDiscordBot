package main.GPT;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GPTRequest {

    private String model = "gpt-4o-mini"; //gpt-3.5-turbo | gpt-4o-mini | gpt-4o
    private boolean useWalletBalance = true;
    @JsonProperty("max_tokens")
    private int maxTokens = 1000;
    private List<Message> messages;
    private int temperature = 1;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Message {

        private Role role;
        private List<Content> content;

        public String getRole() {
            return role.getRole();
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Content {

        private String type;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String text;
        @JsonProperty("image_url")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private ImageURL imageURL;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class ImageURL {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String url;
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