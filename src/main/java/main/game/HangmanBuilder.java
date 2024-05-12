package main.game;

import main.game.api.HangmanAPI;
import main.model.repository.HangmanGameRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public interface HangmanBuilder {

    class Builder {

        //Service
        private HangmanGameRepository hangmanGameRepository;
        private HangmanDataSaving hangmanDataSaving;
        private HangmanResult hangmanResult;
        private HangmanAPI hangmanAPI;

        //User|Guild|Channel data

        private final List<HangmanPlayer> hangmanPlayerList = new ArrayList<>();
        private long messageId;

        //For restoring
        private String guesses;
        private String word;
        private String currentHiddenWord;
        private int hangmanErrors;
        private LocalDateTime localDateTime;
        private boolean isCompetitive;
        private Long againstPlayerId;

        private HangmanPlayer[] hangmanPlayersArrays() {
            List<HangmanPlayer> list = hangmanPlayerList.stream().distinct().toList();
            int size = list.size();
            HangmanPlayer[] hangmanPlayers = new HangmanPlayer[size];
            for (int i = 0; i < list.size(); i++) {
                HangmanPlayer hangmanPlayer = list.get(i);
                hangmanPlayers[i] = hangmanPlayer;
            }
            return hangmanPlayers;
        }

        public Builder setHangmanAPI(HangmanAPI hangmanAPI) {
            this.hangmanAPI = hangmanAPI;
            return this;
        }

        public Builder setHangmanGameRepository(HangmanGameRepository hangmanGameRepository) {
            this.hangmanGameRepository = hangmanGameRepository;
            return this;
        }

        public Builder setHangmanResult(HangmanResult hangmanResult) {
            this.hangmanResult = hangmanResult;
            return this;
        }

        public Builder setHangmanDataSaving(HangmanDataSaving hangmanDataSaving) {
            this.hangmanDataSaving = hangmanDataSaving;
            return this;
        }

        public Builder setMessageId(long messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder setCompetitive(boolean isCompetitive) {
            this.isCompetitive = isCompetitive;
            return this;
        }

        public Builder setAgainstPlayerId(Long againstPlayerId) {
            this.againstPlayerId = againstPlayerId;
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

        public Builder addHangmanPlayer(HangmanPlayer... hangmanPlayer) {
            hangmanPlayerList.addAll(List.of(hangmanPlayer));
            return this;
        }

        /**
         * @throws IllegalArgumentException if hangmanGameRepository, playerRepository, gamesRepository, channelId == null
         */
        public Hangman build() {
            if (hangmanGameRepository == null)
                throw new IllegalArgumentException("The provided hangmanGameRepository cannot be null!");
            else if (hangmanDataSaving == null)
                throw new IllegalArgumentException("The provided hangmanDataSaving cannot be null!");
            else if (hangmanResult == null)
                throw new IllegalArgumentException("The provided hangmanResult cannot be null!");
            else if (hangmanAPI == null)
                throw new IllegalArgumentException("The provided hangmanAPI cannot be null!");

            Hangman hangman = new Hangman(hangmanGameRepository, hangmanDataSaving, hangmanResult, hangmanAPI);

            if (word != null && currentHiddenWord != null) {
                return hangman.update(
                        messageId,
                        guesses,
                        word,
                        currentHiddenWord,
                        hangmanErrors,
                        localDateTime,
                        isCompetitive,
                        againstPlayerId,
                        hangmanPlayersArrays());
            } else {
                hangman.setHangmanPlayers(hangmanPlayersArrays());
                hangman.setCompetitive(isCompetitive);
                hangman.setAgainstPlayerId(againstPlayerId);
                return hangman;
            }
        }
    }
}
