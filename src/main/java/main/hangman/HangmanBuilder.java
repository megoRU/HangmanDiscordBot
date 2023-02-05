package main.hangman;

import main.controller.UpdateController;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

public interface HangmanBuilder {

    class Builder {

        //REPO
        private UpdateController updateController;

        //User|Guild|Channel data
        private long userId;
        private long secondPlayer;
        private Long guildId;
        private Long channelId;
        private long messageId;

        //For restoring
        private String guesses;
        private String word;
        private String currentHiddenWord;
        private int hangmanErrors;
        private LocalDateTime localDateTime;

        public Builder setMessageId(long messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder setGuesses(String guesses) {
            this.guesses = guesses;
            return this;
        }

        public Builder setWord(String word) {
            this.word = word;
            return this;
        }

        public Builder setCurrentHiddenWord(String currentHiddenWord) {
            this.currentHiddenWord = currentHiddenWord;
            return this;
        }

        public Builder setHangmanErrors(int hangmanErrors) {
            this.hangmanErrors = hangmanErrors;
            return this;
        }

        public Builder setLocalDateTime(LocalDateTime localDateTime) {
            this.localDateTime = localDateTime;
            return this;
        }

        public long getSecondPlayer() {
            return secondPlayer;
        }

        public Builder setUserIdLong(long userId) {
            this.userId = userId;
            return this;
        }

        public Builder setSecondUserIdLong(long secondPlayer) {
            this.secondPlayer = secondPlayer;
            return this;
        }

        public Builder setGuildIdLong(@Nullable Long guildId) {
            this.guildId = guildId;
            return this;
        }

        public Builder setChannelId(@NotNull Long channelId) {
            this.channelId = channelId;
            return this;
        }

        public Builder setUpdateController(UpdateController updateController) {
            this.updateController = updateController;
            return this;
        }

        /**
         * @throws IllegalArgumentException if hangmanGameRepository, playerRepository, gamesRepository, channelId == null
         */
        public Hangman build() {

            if (updateController == null)
                throw new IllegalArgumentException("The provided updateController cannot be null!");

            if (channelId == null || channelId == 0L)
                throw new IllegalArgumentException("The provided channelId cannot be null!");

            if (word != null && currentHiddenWord != null) {
                return new Hangman(userId,
                        secondPlayer,
                        guildId,
                        channelId,
                        messageId,
                        guesses,
                        word,
                        currentHiddenWord,
                        hangmanErrors,
                        localDateTime,
                        updateController);
            }

            if (secondPlayer != 0L) {
                return new Hangman(userId, secondPlayer, guildId, channelId, updateController);
            }

            return new Hangman(userId, guildId, channelId, updateController);
        }

    }
}
