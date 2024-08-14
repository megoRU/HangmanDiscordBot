package main.game;

import lombok.Getter;
import main.game.core.HangmanRegistry;
import main.model.entity.UserSettings;
import org.jetbrains.annotations.Nullable;

@Getter
public class HangmanPlayer {

    private final long userId;
    @Nullable
    private final Long guildId;
    private final Long channelId;
    private UserSettings.GameLanguage gameLanguage;

    public HangmanPlayer(long userId, @Nullable Long guildId, Long channelId) {
        this.userId = userId;
        this.guildId = guildId;
        this.channelId = channelId;
    }

    public HangmanPlayer(long userId, @Nullable Long guildId, Long channelId, UserSettings.GameLanguage gameLanguage) {
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