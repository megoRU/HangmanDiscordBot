package main.hangman;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class language {

    private String language;

    @Override
    public String toString() {
        return "{" + "\"language\": \"" + language + "\"" + '}';
    }
}
