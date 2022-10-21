package main.hangman;

import main.model.repository.GamesRepository;
import main.model.repository.HangmanGameRepository;
import main.model.repository.PlayerRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

public interface HangmanBuilder {

    class Builder {

        //REPO
        private HangmanGameRepository hangmanGameRepository;
        private GamesRepository gamesRepository;
        private PlayerRepository playerRepository;

        //User|Guild|Channel data
        private long userId;
        private long secondPlayer;
        private Long guildId;
        private Long channelId;

        //For restoring
        private String guesses;
        private String word;
        private String wordHidden;
        private String currentHiddenWord;
        private int hangmanErrors;
        private LocalDateTime localDateTime;

        public Builder setGuesses(String guesses) {
            this.guesses = guesses;
            return this;
        }

        public Builder setWord(String word) {
            this.word = word;
            return this;
        }

        public Builder setWordHidden(String wordHidden) {
            this.wordHidden = wordHidden;
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

        public Builder setHangmanGameRepository(@NotNull HangmanGameRepository hangmanGameRepository) {
            this.hangmanGameRepository = hangmanGameRepository;
            return this;
        }

        public Builder setGamesRepository(@NotNull GamesRepository gamesRepository) {
            this.gamesRepository = gamesRepository;
            return this;
        }

        public Builder setPlayerRepository(@NotNull PlayerRepository playerRepository) {
            this.playerRepository = playerRepository;
            return this;
        }

        /**
         * @throws IllegalArgumentException if hangmanGameRepository, playerRepository, gamesRepository, channelId == null
         */
        public Hangman build() {

            if (hangmanGameRepository == null)
                throw new IllegalArgumentException("The provided hangmanGameRepository cannot be null!");

            if (playerRepository == null)
                throw new IllegalArgumentException("The provided playerRepository cannot be null!");

            if (gamesRepository == null)
                throw new IllegalArgumentException("The provided gamesRepository cannot be null!");

            if (channelId == null || channelId == 0L)
                throw new IllegalArgumentException("The provided channelId cannot be null!");

            if (guesses != null && word != null && currentHiddenWord != null && localDateTime != null) {
                return new Hangman(userId, secondPlayer, guildId, channelId, guesses, word, currentHiddenWord, hangmanErrors, localDateTime, hangmanGameRepository, gamesRepository, playerRepository);
            }

            if (secondPlayer != 0L) {
                return new Hangman(userId, secondPlayer, guildId, channelId, hangmanGameRepository, gamesRepository, playerRepository);
            }

            return new Hangman(userId, guildId, channelId, hangmanGameRepository, gamesRepository, playerRepository);
        }

    }
}
