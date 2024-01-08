package main.hangman;

import lombok.Getter;
import main.model.entity.UserSettings;

@Getter
public class HangmanPlayer {

    private final long userId;
    private final Long guildId;
    private final long channelId;
    private UserSettings.GameLanguage gameLanguage;

    public HangmanPlayer(long userId, Long guildId, long channelId) {
        this.userId = userId;
        this.guildId = guildId;
        this.channelId = channelId;
    }

    public HangmanPlayer(long userId, Long guildId, long channelId, UserSettings.GameLanguage gameLanguage) {
        this.userId = userId;
        this.guildId = guildId;
        this.channelId = channelId;
        this.gameLanguage = gameLanguage;
    }

    public Hangman getGame() {
        HangmanRegistry instance = HangmanRegistry.getInstance();
        return instance.getActiveHangman(userId);
    }

    public boolean isFromGuild() {
        return guildId != null;
    }
}