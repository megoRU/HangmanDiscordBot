package main.hangman;

import lombok.Getter;

@Getter
public class HangmanPlayer {

    private final long userId;
    private final Long guildId;
    private final long channelId;

    public HangmanPlayer(long userId, Long guildId, long channelId) {
        this.userId = userId;
        this.guildId = guildId;
        this.channelId = channelId;
    }

    public Hangman getGame() {
        HangmanRegistry instance = HangmanRegistry.getInstance();
        return instance.getActiveHangman(userId);
    }
}