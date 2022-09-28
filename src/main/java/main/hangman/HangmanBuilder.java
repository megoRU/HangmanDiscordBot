package main.hangman;

import main.model.repository.GamesRepository;
import main.model.repository.HangmanGameRepository;
import main.model.repository.PlayerRepository;

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

        public Builder setUserIdLong(long userId) {
            this.userId = userId;
            return this;
        }

        public Builder setSecondUserIdLong(long secondPlayer) {
            this.secondPlayer = secondPlayer;
            return this;
        }

        public Builder setGuildIdLong(Long guildId) {
            this.guildId = guildId;
            return this;
        }

        public Builder setChannelId(Long channelId) {
            this.channelId = channelId;
            return this;
        }

        public Builder setHangmanGameRepository(HangmanGameRepository hangmanGameRepository) {
            this.hangmanGameRepository = hangmanGameRepository;
            return this;
        }

        public Builder setGamesRepository(GamesRepository gamesRepository) {
            this.gamesRepository = gamesRepository;
            return this;
        }

        public Builder setPlayerRepository(PlayerRepository playerRepository) {
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

            if (secondPlayer != 0L) {
                return new Hangman(userId, secondPlayer, guildId, channelId, hangmanGameRepository, gamesRepository, playerRepository);
            }

            return new Hangman(userId, guildId, channelId, hangmanGameRepository, gamesRepository, playerRepository);
        }

    }
}
